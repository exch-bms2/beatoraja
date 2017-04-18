package bms.player.beatoraja.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.Mode;
import bms.model.Note;
import bms.model.TimeLine;

/**
 * レーン単位でノーツを入れ替えるオプション MIRROR、RANDOM、R-RANDOMが該当する
 * 
 * @author exch
 */
public class LaneShuffleModifier extends PatternModifier {

	/**
	 * 各レーンの移動先
	 */
	private int[] random;
	/**
	 * ランダムのタイプ
	 */
	private int type;
	/**
	 * ミラー
	 */
	public static final int MIRROR = 0;
	/**
	 * ローテート
	 */
	public static final int R_RANDOM = 1;
	/**
	 * ランダム
	 */
	public static final int RANDOM = 2;
	/**
	 * スクラッチレーンを含むランダム
	 */
	public static final int RANDOM_EX = 3;
	/**
	 * 1P-2Pを入れ替える
	 */
	public static final int FLIP = 4;
	/**
	 * 1Pの譜面を2Pにコピーする
	 */
	public static final int BATTLE = 5;

	public LaneShuffleModifier(int type) {
		super(type == RANDOM_EX ? 1 : 0);
		this.type = type;
	}

	private void makeRandom(Mode mode) {
		
		int[] keys;
		switch (type) {
		case MIRROR:
			keys = getKeys(mode, false);
			random = keys.length > 0 ? rotate(keys, keys.length - 1, false) : keys;
			break;
		case R_RANDOM:
			keys = getKeys(mode, false);
			random = keys.length > 0 ? rotate(keys) : keys;
			break;
		case RANDOM:
			keys = getKeys(mode, false);
			random = keys.length > 0 ? shuffle(keys) : keys;
			break;
		case RANDOM_EX:
			keys = getKeys(mode, true);
			random = keys.length > 0 ? shuffle(keys) : keys;
			break;
		case FLIP:
			if(mode.player == 2) {
				random = new int[mode.key];
				for(int i = 0;i < random.length;i++) {
					random[i] = (i + (mode.key / mode.player)) % mode.key;
				}
			} else {
				random = new int[0];
			}			
			break;
		case BATTLE:
			if(mode.player == 1) {
				random = new int[0];
			} else {
				keys = getKeys(mode, true);
				random = new int[keys.length * 2];
				System.arraycopy(keys, 0, random, 0, keys.length);
				System.arraycopy(keys, 0, random, keys.length, keys.length);
			}
			break;

		}
	}
	
	@Override
	public List<PatternModifyLog> modify(BMSModel model) {
		List<PatternModifyLog> log = new ArrayList();
		makeRandom(model.getMode());
		int lanes = model.getMode().key;
		for (TimeLine tl : model.getAllTimeLines()) {
			if (tl.existNote() || tl.existHiddenNote()) {
				Note[] notes = new Note[lanes];
				Note[] hnotes = new Note[lanes];
				for (int i = 0; i < lanes; i++) {
					notes[i] = tl.getNote(i);
					hnotes[i] = tl.getHiddenNote(i);
				}
				boolean[] clone = new boolean[lanes];
				for (int i = 0; i < lanes; i++) {
					final int mod = i < random.length ? random[i] : i;
					if (clone[mod]) {
						if (notes[mod] != null) {
							if (notes[mod] instanceof LongNote
									&& ((LongNote) notes[mod]).getEndnote().getSection() == tl.getSection()) {
								LongNote ln = (LongNote) model
										.getTimeLine(notes[mod].getSection(), notes[mod].getSectiontime()).getNote(i);
								tl.setNote(i, ln);
							} else {
								tl.setNote(i, (Note) notes[mod].clone());
							}
						} else {
							tl.setNote(i, null);
						}
						if (hnotes[mod] != null) {
							tl.setHiddenNote(i, (Note) hnotes[mod].clone());
						} else {
							tl.setHiddenNote(i, null);
						}
					} else {
						tl.setNote(i, notes[mod]);
						tl.setHiddenNote(i, hnotes[mod]);
						clone[mod] = true;
					}
				}
				log.add(new PatternModifyLog(tl.getTime(), random));
			}
		}
		return log;
	}

}
