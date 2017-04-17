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

	private static final int[][] MIRROR_LANE = { { 4, 3, 2, 1, 0, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 },
			{ 6, 5, 4, 3, 2, 1, 0, 7, 8, 9, 10, 11, 12, 13, 14, 15 },
			{ 0, 1, 2, 3, 4, 5, 10, 9, 8, 7, 6, 11, 12, 13, 14, 15 },
			{ 0, 1, 2, 3, 4, 5, 6, 7, 14, 13, 12, 11, 10, 9, 8, 15 },
			{ 8, 7, 6, 5, 4, 3, 2, 1, 0, 9, 10, 11, 12, 13, 14, 15 } };

	public LaneShuffleModifier(int type) {
		super(type == RANDOM_EX ? 1 : 0);
		this.type = type;
	}

	private void makeRandom(Mode mode) {
		random = new int[mode.key];
		for (int i = 0; i < random.length; i++) {
			random[i] = i;
		}

		switch (type) {
		case MIRROR:
			random = MIRROR_LANE[getModifyTarget()];
			break;
		case R_RANDOM:
			int i, j;
			random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
			switch (getModifyTarget()) {
			case PLAYER1_5KEYS:
				i = (int) (Math.random() * 4);
				j = (int) (Math.random() * 2);
				for (int lane = 0; lane < 5; lane++) {
					i = (i + 1) % 5;
					random[lane] = (j == 0 ? i : 4 - i);
				}
				break;
			case PLAYER1_7KEYS:
				i = (int) (Math.random() * 6);
				j = (int) (Math.random() * 2);
				for (int lane = 0; lane < 7; lane++) {
					i = (i + 1) % 7;
					random[lane] = (j == 0 ? i : 6 - i);
				}
				break;
			case PLAYER2_5KEYS:
				i = (int) (Math.random() * 4);
				j = (int) (Math.random() * 2);
				for (int lane = 6; lane < 11; lane++) {
					i = (i + 1) % 5;
					random[lane] = (j == 0 ? i + 6 : 10 - i);
				}
				break;
			case PLAYER2_7KEYS:
				i = (int) (Math.random() * 6);
				j = (int) (Math.random() * 2);
				for (int lane = 8; lane < 15; lane++) {
					i = (i + 1) % 7;
					random[lane] = (j == 0 ? i + 8 : 14 - i);
				}
				break;
			case NINEKEYS:
				i = (int) (Math.random() * 8);
				j = (int) (Math.random() * 2);
				for (int lane = 0; lane < 9; lane++) {
					i = (i + 1) % 9;
					random[lane] = (j == 0 ? i : 8 - i);
				}
				break;
			}
			break;
		case RANDOM:
			List<Integer> l;
			random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
			switch (getModifyTarget()) {
			case PLAYER1_5KEYS:
				l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4));
				for (int lane = 0; lane < 5; lane++) {
					int r = (int) (Math.random() * l.size());
					random[lane] = l.get(r);
					l.remove(r);
				}
				break;
			case PLAYER1_7KEYS:
				l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6));
				for (int lane = 0; lane < 7; lane++) {
					int r = (int) (Math.random() * l.size());
					random[lane] = l.get(r);
					l.remove(r);
				}
				break;
			case PLAYER2_5KEYS:
				l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4));
				for (int lane = 0; lane < 5; lane++) {
					int r = (int) (Math.random() * l.size());
					random[lane + 6] = l.get(r) + 6;
					l.remove(r);
				}
				break;
			case PLAYER2_7KEYS:
				l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6));
				for (int lane = 0; lane < 7; lane++) {
					int r = (int) (Math.random() * l.size());
					random[lane + 8] = l.get(r) + 8;
					l.remove(r);
				}
				break;
			case NINEKEYS:
				l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
				for (int lane = 0; lane < 9; lane++) {
					int r = (int) (Math.random() * l.size());
					random[lane] = l.get(r);
					l.remove(r);
				}
				break;
			}
			break;
		case RANDOM_EX:
			List<Integer> le = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
			random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
			switch (getModifyTarget()) {
			case PLAYER1_5KEYS:
				le = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5));
				for (int lane = 0; lane < 6; lane++) {
					int r = (int) (Math.random() * le.size());
					random[lane] = le.get(r);
					le.remove(r);
				}
				break;
			case PLAYER1_7KEYS:
				le = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
				for (int lane = 0; lane < 8; lane++) {
					int r = (int) (Math.random() * le.size());
					random[lane] = le.get(r);
					le.remove(r);
				}
				break;
			case PLAYER2_5KEYS:
				le = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5));
				for (int lane = 0; lane < 6; lane++) {
					int r = (int) (Math.random() * le.size());
					random[lane + 6] = le.get(r) + 6;
					le.remove(r);
				}
				break;
			case PLAYER2_7KEYS:
				le = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
				for (int lane = 0; lane < 8; lane++) {
					int r = (int) (Math.random() * le.size());
					random[lane + 8] = le.get(r) + 8;
					le.remove(r);
				}
				break;
			case NINEKEYS:
				// 9keyにはSCがないため、EX-RANDOMがない。そもそもここには飛んでこない
				break;
			}
			break;
		case FLIP:
			switch (getModifyTarget()) {
			case PLAYER1_5KEYS:
			case PLAYER2_5KEYS:
				random = new int[] { 6, 7, 8, 9, 10, 11, 0, 1, 2, 3, 4, 5, 12, 13, 14, 15};
				break;
			case PLAYER1_7KEYS:
			case PLAYER2_7KEYS:
				random = new int[] { 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7};
				break;
			}
			break;
		case BATTLE:
			switch (getModifyTarget()) {
			case PLAYER1_5KEYS:
				random = new int[] { 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 12,13,14,15 };
				break;
			case PLAYER1_7KEYS:
				random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3, 4, 5, 6, 7};
				break;
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
					if (clone[random[i]]) {
						if (notes[random[i]] != null) {
							if (notes[random[i]] instanceof LongNote
									&& ((LongNote) notes[random[i]]).getEndnote().getSection() == tl.getSection()) {
								LongNote ln = (LongNote) model
										.getTimeLine(notes[random[i]].getSection(), notes[random[i]].getSectiontime())
										.getNote(i);
								tl.setNote(i, ln);
							} else {
								tl.setNote(i, (Note) notes[random[i]].clone());
							}
						} else {
							tl.setNote(i, null);
						}
						if (hnotes[random[i]] != null) {
							tl.setHiddenNote(i, (Note) hnotes[random[i]].clone());
						} else {
							tl.setHiddenNote(i, null);
						}
					} else {
						tl.setNote(i, notes[random[i]]);
						tl.setHiddenNote(i, hnotes[random[i]]);
						clone[random[i]] = true;
					}
				}
				log.add(new PatternModifyLog(tl.getTime(), random));
			}
		}
		return log;
	}

}
