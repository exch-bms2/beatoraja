package bms.player.beatoraja.pattern;

import java.util.List;
import java.util.stream.IntStream;

import bms.model.*;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.pattern.LaneShuffleModifier.*;

/**
 * 譜面オプションの抽象クラス
 *
 * @author exch
 */
public abstract class PatternModifier {

	/**
	 * 譜面変更のアシストレベル
	 */
	private AssistLevel assist = AssistLevel.NONE;
	
	private long seed = (long) (Math.random() * 65536 * 256);

	public final int player;

	public PatternModifier() {
		this(0);
	}

	public PatternModifier(int player) {
		this.player = player;
	}

	public PatternModifier(AssistLevel assist) {
		this(0);
		this.assist = assist;
	}

	public abstract void modify(BMSModel model);

	/**
	 * 譜面変更ログの通りに譜面オプションをかける
	 *
	 * @param model
	 *            譜面オプションをかける対象のBMSModel
	 * @param log
	 *            譜面変更ログ
	 */
	public static void modify(BMSModel model, List<PatternModifyLog> log) {
		for (TimeLine tl : model.getAllTimeLines()) {
			PatternModifyLog pm = null;
			for (PatternModifyLog pms : log) {
				if (pms.section == tl.getSection()) {
					pm = pms;
					break;
				}
			}
			if (pm != null) {
				int lanes = model.getMode().key;
				Note[] notes = new Note[lanes];
				Note[] hnotes = new Note[lanes];
				for (int i = 0; i < lanes; i++) {
					final int mod = i < pm.modify.length ? pm.modify[i] : i;
					notes[i] = tl.getNote(mod);
					hnotes[i] = tl.getHiddenNote(mod);
				}
				for (int i = 0; i < lanes; i++) {
					tl.setNote(i, notes[i]);
					tl.setHiddenNote(i, hnotes[i]);
				}
			}
		}
	}

	public AssistLevel getAssistLevel() {
		return assist;
	}

	protected void setAssistLevel(AssistLevel assist) {
		this.assist = (assist != null ? assist : AssistLevel.NONE);
	}

	public long getSeed() {
		return seed;
	}

	public void setSeed(long seed) {
		if(seed >= 0) {
			this.seed = seed;			
		}
	}

    /**
     * 譜面オプションに対応したPatternModifierを生成する
     *
     * @param id 譜面オプションID
     * @param side 譜面オプションサイド(1P or 2P)
     * @param mode 譜面のモード
     * @return
     */
    public static PatternModifier create(int id, int player, Mode mode, PlayerConfig config) {
		final Random chartOprion = Random.getRandom(id, mode);
		PatternModifier pm = switch (chartOprion) {
			case IDENTITY -> new PatternModifier() {
			@Override
				public void modify(BMSModel model) {
				}
			};
			case MIRROR -> new LaneMirrorShuffleModifier(player, false);
			case MIRROR_EX -> new LaneMirrorShuffleModifier(player, true);
			case ROTATE -> new LaneRotateShuffleModifier(player, false);
			case ROTATE_EX -> new LaneRotateShuffleModifier(player, true);
			case RANDOM -> new LaneRandomShuffleModifier(player, false);
			case RANDOM_EX -> new LaneRandomShuffleModifier(player, true);
			case CROSS -> new LaneCrossShuffleModifier(player, false);
			case RANDOM_PLAYABLE -> new LanePlayableRandomShuffleModifier(player, false);

			case FLIP -> new PlayerFlipModifier();
			// TODO BATTLEはModeModifierの方がいいかも
			case BATTLE -> new PlayerBattleModifier();

			default -> switch (chartOprion.unit) {
				case NOTE -> new NoteShuffleModifier(chartOprion, player, mode, config);
				default -> throw new IllegalArgumentException("Unexpected value: " + chartOprion.unit);
			};
		};
		
		return pm;
	}

	/**
	 * 変更対象のレーン番号が格納された配列を返す
	 * @param mode
	 * プレイモード
	 * @param containsScratch
	 * スクラッチレーンを含むか
	 * @return レーン番号の配列
	 */
	protected int[] getKeys(Mode mode, int player, boolean containsScratch) {
		if(player >= mode.player) {
			return new int[0];
		}
		final int startkey = mode.key * player / mode.player;
		return IntStream.range(startkey, startkey + mode.key / mode.player).filter(i -> containsScratch || !mode.isScratchKey(i)).toArray();
	}

	protected static void moveToBackground(TimeLine[] tls, TimeLine tl, int lane) {
		Note n = tl.getNote(lane);
		if(n == null) {
			return;
		}
		if(n instanceof LongNote) {
			LongNote pln = ((LongNote) tl.getNote(lane)).getPair();
			for(TimeLine tl2 : tls) {
				if(tl2.getNote(lane) == pln) {
					tl2.addBackGroundNote(pln);
					tl2.setNote(lane, null);
					break;
				}
			}
		}

		if(!(n instanceof MineNote)) {
			tl.addBackGroundNote(tl.getNote(lane));
		}
		tl.setNote(lane, null);

	}

	public enum AssistLevel {
		NONE, LIGHT_ASSIST, ASSIST;
	}
}
