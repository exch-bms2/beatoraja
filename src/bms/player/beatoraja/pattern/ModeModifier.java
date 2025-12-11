package bms.player.beatoraja.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.Mode;
import bms.model.Note;
import bms.model.TimeLine;
import bms.player.beatoraja.PlayerConfig;
/**
 * 譜面のレーン数を変更するクラス。
 * NoteShuffleModifierから分離。
 * @author keh
 */
public class ModeModifier extends PatternModifier {

	// TODO 7to9ほぼそのままのコードのため、要リファクタリング

	private final PlayerConfig config;

	/**
	 * 連打しきい値(ms)(H-RANDOM用)
	 */
	private int hranThreshold = 125;
	
	private final Mode beforeMode;
	private final Mode afterMode;

	public ModeModifier(Mode beforeMode, Mode afterMode, PlayerConfig config) {
		super(AssistLevel.LIGHT_ASSIST);		
		this.beforeMode = beforeMode;
		this.afterMode = afterMode;
		this.config = config;
	}

	@Override
	public void modify(BMSModel model) {
		model.setMode(afterMode);
		final Algorithm algorithm = Algorithm.get(beforeMode, afterMode);
		int lanes = afterMode.key;
		int[] random = new int[0];
		int[] ln = new int[lanes];
		int[] lastNoteTime = new int[lanes];
		int[] endLnNoteTime = new int[lanes];
		Arrays.fill(ln, -1);
		Arrays.fill(lastNoteTime, -100);
		Arrays.fill(endLnNoteTime, -1);
		if(config.getHranThresholdBPM() <= 0) hranThreshold = 0;
		else hranThreshold = (int) (Math.ceil(15000.0f / config.getHranThresholdBPM()));
		for (TimeLine tl : model.getAllTimeLines()) {
			if (tl.existNote() || tl.existHiddenNote()) {
				Note[] notes = new Note[lanes];
				Note[] hnotes = new Note[lanes];
				for (int i = 0; i < lanes; i++) {
					notes[i] = tl.getNote(i);
					hnotes[i] = tl.getHiddenNote(i);
				}
				int[] keys;
				keys = getKeys(afterMode, 0, true);
				random = algorithm != null && keys.length > 0 ? algorithm.modify(keys, ln,
						notes, lastNoteTime, tl.getTime(), hranThreshold, config)
						: keys;

				for (int i = 0; i < lanes; i++) {
					final int mod = i < random.length ? random[i] : i;
					Note n = notes[mod];
					Note hn = hnotes[mod];
					if (n instanceof LongNote) {
						LongNote ln2 = (LongNote) n;
						if (ln2.isEnd() && tl.getTime() == endLnNoteTime[i]) {
							tl.setNote(i, n);
							ln[i] = -1;
							endLnNoteTime[i] = -1;
						} else {
							tl.setNote(i, n);
							ln[i] = mod;
							if (!ln2.isEnd()) {
								endLnNoteTime[i] = ln2.getPair().getTime();
							}
							lastNoteTime[i] = tl.getTime();
						}
					} else {
						tl.setNote(i, n);
						if (n != null) {
							lastNoteTime[i] = tl.getTime();
						}
					}
					tl.setHiddenNote(i, hn);
				}
			}
		}
	}
	
	enum Algorithm {
		SEVEN_TO_NINE(Mode.BEAT_7K, Mode.POPN_9K) {
			@Override
			public int[] modify(int[] keys, int[] activeln, Note[] notes, int[] lastNoteTime, int now, int duration, PlayerConfig config) {
				/**
				 * 7to9 スクラッチ鍵盤位置関係 0:OFF 1:SC1KEY2~8 2:SC1KEY3~9 3:SC2KEY3~9 4:SC8KEY1~7 5:SC9KEY1~7 6:SC9KEY2~8
				 */
				int keyLane = 2;
				int scLane = 1;
				int restLane = 0;
				switch(config.getSevenToNinePattern()) {
					case 1:
						scLane = 1 - 1;
						keyLane = 2 - 1;
						restLane = 9 - 1;
						break;
					case 2:
						scLane = 1 - 1;
						keyLane = 3 - 1;
						restLane = 2 - 1;
						break;
					case 4:
						scLane = 8 - 1;
						keyLane = 1 - 1;
						restLane = 9 - 1;
						break;
					case 5:
						scLane = 9 - 1;
						keyLane = 1 - 1;
						restLane = 8 - 1;
						break;
					case 6:
						scLane = 9 - 1;
						keyLane = 2 - 1;
						restLane = 1 - 1;
						break;
					case 3:
					default:
						scLane = 2 - 1;
						keyLane = 3 - 1;
						restLane = 1 - 1;
						break;
				}

				int[] result = new int[9];
				for (int i = 0; i < 7; i++) {
					result[i + keyLane] = i;
				}

				if (activeln != null && (activeln[scLane] != -1 || activeln[restLane] != -1)) {
					if(activeln[scLane] == 7) {
						result[scLane] = 7;
						result[restLane] = 8;
					} else {
						result[scLane] = 8;
						result[restLane] = 7;
					}
				} else {
					/**
					 * 7to9スクラッチ処理タイプ 0:そのまま 1:連打回避 2:交互
					 */
					switch(config.getSevenToNineType()) {
						case 1:
							if(now - lastNoteTime[scLane] > duration || now - lastNoteTime[scLane] >= now - lastNoteTime[restLane]) {
								result[scLane] = 7;
								result[restLane] = 8;
							} else {
								result[scLane] = 8;
								result[restLane] = 7;
							}
							break;
						case 2:
							if(now - lastNoteTime[scLane] >= now - lastNoteTime[restLane]) {
								result[scLane] = 7;
								result[restLane] = 8;
							} else {
								result[scLane] = 8;
								result[restLane] = 7;
							}
							break;
						case 0:
						default:
							result[scLane] = 7;
							result[restLane] = 8;
							break;
					}
				}

				return result;
			}
		};
		
		/**
		 * 変更前モード
		 */
		private final Mode beforeMode;
		/**
		 * 変更後モード
		 */
		private final Mode afterMode;
		
		public abstract int[] modify(int[] keys, int[] activeln, Note[] notes, int[] lastNoteTime, int now, int duration, PlayerConfig config);
		
		private Algorithm(Mode beforeMode, Mode afterMode) {
			this.beforeMode = beforeMode;
			this.afterMode = afterMode;
		}

		public static Algorithm get(Mode beforeMode, Mode afterMode) {
			for(Algorithm algorithm : Algorithm.values()) {
				if(algorithm.beforeMode == beforeMode && algorithm.afterMode == afterMode) {
					return algorithm;
				}
			}
			return null;
		}
	}
}
