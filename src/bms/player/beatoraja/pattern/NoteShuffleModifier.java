package bms.player.beatoraja.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.NormalNote;
import bms.model.Note;
import bms.model.TimeLine;

/**
 * タイムライン単位でノーツを入れ替えるためのクラス．
 *
 * @author exch
 */
public class NoteShuffleModifier extends PatternModifier {

	/**
	 * タイムライン毎にノーツをランダムに入れ替える
	 */
	public static final int S_RANDOM = 0;
	/**
	 * 初期の並べ替えをベースに、螺旋状に並べ替える
	 */
	public static final int SPIRAL = 1;
	/**
	 * ノーツをスクラッチレーンに集約する
	 */
	public static final int ALL_SCR = 2;
	/**
	 * S-RANDOMに縦連が極力来ないように配置する
	 */
	public static final int H_RANDOM = 3;
	/**
	 * スクラッチレーンを含めたS-RANDOM
	 */
	public static final int S_RANDOM_EX = 4;

	private int type;
	/**
	 * 次のTimeLine増加分(SPIRAL用)
	 */
	private int inc;
	/**
	 * ノーツが存在する直前のTimeLine(H-RANDOM用)
	 */
	private TimeLine prev;

	public NoteShuffleModifier(int type) {
		super(type >= ALL_SCR ? 1 : 0);
		this.type = type;
	}

	@Override
	public List<PatternModifyLog> modify(BMSModel model) {
		// TODO 未修正

		List<PatternModifyLog> log = new ArrayList<PatternModifyLog>();
		int lanes = model.getMode().key;
		int[] random = new int[0];
		int[] ln = new int[lanes];
		Arrays.fill(ln, -1);
		for (TimeLine tl : model.getAllTimeLines()) {
			if (tl.existNote() || tl.existHiddenNote()) {
				Note[] notes = new Note[lanes];
				Note[] hnotes = new Note[lanes];
				for (int i = 0; i < lanes; i++) {
					notes[i] = tl.getNote(i);
					hnotes[i] = tl.getHiddenNote(i);
				}
				List<Integer> l;
				switch (type) {
				case S_RANDOM:
					switch (getModifyTarget()) {
					case PLAYER1_5KEYS:
						l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4));
						random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
						for (int lane = 0; lane < 5; lane++) {
							if (ln[lane] != -1) {
								random[lane] = ln[lane];
								l.remove((Integer) ln[lane]);
							}
						}
						for (int lane = 0; lane < 5; lane++) {
							if (ln[lane] == -1) {
								int r = (int) (Math.random() * l.size());
								random[lane] = l.get(r);
								l.remove(r);
							}
						}
						break;
					case PLAYER1_7KEYS:
						l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6));
						random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
						for (int lane = 0; lane < 7; lane++) {
							if (ln[lane] != -1) {
								random[lane] = ln[lane];
								l.remove((Integer) ln[lane]);
							}
						}
						for (int lane = 0; lane < 7; lane++) {
							if (ln[lane] == -1) {
								int r = (int) (Math.random() * l.size());
								random[lane] = l.get(r);
								l.remove(r);
							}
						}
						break;
					case PLAYER2_5KEYS:
						l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4));
						random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
						for (int lane = 0; lane < 5; lane++) {
							if (ln[lane + 6] != -1) {
								random[lane + 6] = ln[lane + 6];
								l.remove((Integer) ln[lane + 6] - 6);
							}
						}
						for (int lane = 0; lane < 5; lane++) {
							if (ln[lane + 6] == -1) {
								int r = (int) (Math.random() * l.size());
								random[lane + 6] = l.get(r) + 6;
								l.remove(r);
							}
						}
						break;
					case PLAYER2_7KEYS:
						l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6));
						random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
						for (int lane = 0; lane < 7; lane++) {
							if (ln[lane + 8] != -1) {
								random[lane + 8] = ln[lane + 8];
								l.remove((Integer) ln[lane + 8] - 8);
							}
						}
						for (int lane = 0; lane < 8; lane++) {
							if (ln[lane + 8] == -1) {
								int r = (int) (Math.random() * l.size());
								random[lane + 8] = l.get(r) + 8;
								l.remove(r);
							}
						}
						break;
					case NINEKEYS:
						l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
						random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
						for (int lane = 0; lane < 9; lane++) {
							if (ln[lane] != -1) {
								random[lane] = ln[lane];
								l.remove((Integer) (ln[lane]));
							}
						}
						for (int lane = 0; lane < 9; lane++) {
							if (ln[lane] == -1) {
								int r = (int) (Math.random() * l.size());
								random[lane] = l.get(r);
								l.remove(r);
							}
						}
						break;
					}
					break;
				case SPIRAL:
					switch (getModifyTarget()) {
					case PLAYER1_5KEYS:
						if (random.length == 0) {
							// 初期値の作成
							random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
							int index = (int) (Math.random() * 5);
							int j = (int) (Math.random() * 2) >= 1 ? 1 : 4;
							for (int i = 0; i < 5; i++) {
								random[i] = index;
								index = (index + j) % 5;
							}
							inc = (int) (Math.random() * 4) + 1;
						} else {
							boolean cln = false;
							for (int lane = 0; lane < 5; lane++) {
								if (ln[lane] != -1) {
									cln = true;
								}
							}
							if (!cln) {
								int[] nrandom = Arrays.copyOf(random, random.length);
								int index = inc;
								for (int i = 0; i < 5; i++) {
									nrandom[i] = random[index];
									index = (index + 1) % 5;
								}
								random = nrandom;
							}
						}
						break;
					case PLAYER1_7KEYS:
						if (random.length == 0) {
							// 初期値の作成
							random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
							int index = (int) (Math.random() * 7);
							int j = (int) (Math.random() * 2) >= 1 ? 1 : 6;
							for (int i = 0; i < 7; i++) {
								random[i] = index;
								index = (index + j) % 7;
							}
							inc = (int) (Math.random() * 6) + 1;
						} else {
							boolean cln = false;
							for (int lane = 0; lane < 7; lane++) {
								if (ln[lane] != -1) {
									cln = true;
								}
							}
							if (!cln) {
								int[] nrandom = Arrays.copyOf(random, random.length);
								int index = inc;
								for (int i = 0; i < 7; i++) {
									nrandom[i] = random[index];
									index = (index + 1) % 7;
								}
								random = nrandom;
							}
						}
						break;
					case PLAYER2_5KEYS:
						if (random.length == 0) {
							// 初期値の作成
							random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
							int index = (int) (Math.random() * 5);
							int j = (int) (Math.random() * 2) >= 1 ? 1 : 4;
							for (int i = 0; i < 5; i++) {
								random[i + 6] = index + 6;
								index = (index + j) % 5;
							}
							inc = (int) (Math.random() * 4) + 1;
						} else {
							boolean cln = false;
							for (int lane = 0; lane < 5; lane++) {
								if (ln[lane + 6] != -1) {
									cln = true;
								}
							}
							if (!cln) {
								int[] nrandom = Arrays.copyOf(random, random.length);
								int index = inc;
								for (int i = 0; i < 5; i++) {
									nrandom[i + 6] = random[index + 6];
									index = (index + 1) % 5;
								}
								random = nrandom;
							}
						}
						break;
					case PLAYER2_7KEYS:
						if (random.length == 0) {
							// 初期値の作成
							random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
							int index = (int) (Math.random() * 7);
							int j = (int) (Math.random() * 2) >= 1 ? 1 : 6;
							for (int i = 0; i < 7; i++) {
								random[i + 8] = index + 8;
								index = (index + j) % 7;
							}
							inc = (int) (Math.random() * 6) + 1;
						} else {
							boolean cln = false;
							for (int lane = 0; lane < 7; lane++) {
								if (ln[lane + 8] != -1) {
									cln = true;
								}
							}
							if (!cln) {
								int[] nrandom = Arrays.copyOf(random, random.length);
								int index = inc;
								for (int i = 0; i < 7; i++) {
									nrandom[i + 8] = random[index + 8];
									index = (index + 1) % 7;
								}
								random = nrandom;
							}
						}
						break;
					case NINEKEYS:
						if (random.length == 0) {
							// 初期値の作成
							random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
							int index = (int) (Math.random() * 9);
							int j = (int) (Math.random() * 2) >= 1 ? 1 : 8;
							for (int i = 0; i < 9; i++) {
								random[i] = index;
								index = (index + j) % 9;
							}
							inc = (int) (Math.random() * 8) + 1;
						} else {
							boolean cln = false;
							for (int lane = 0; lane < 9; lane++) {
								if (ln[lane] != -1) {
									cln = true;
								}
							}
							if (!cln) {
								int[] nrandom = Arrays.copyOf(random, random.length);
								int index = inc;
								for (int i = 0; i < 9; i++) {
									nrandom[i] = random[index];
									index = (index + 1) % 9;
								}
								random = nrandom;
							}
						}
						break;
					}
					break;
				case ALL_SCR:
					random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
					switch (getModifyTarget()) {
					case PLAYER1_5KEYS:
						if (ln[5] == -1 && notes[5] == null) {
							for (int lane = 0; lane < 5; lane++) {
								if (notes[lane] != null && notes[lane] instanceof NormalNote) {
									random[5] = lane;
									random[lane] = 5;
									break;
								}
							}
						}
						break;
					case PLAYER1_7KEYS:
						if (ln[7] == -1 && notes[7] == null) {
							for (int lane = 0; lane < 7; lane++) {
								if (notes[lane] != null && notes[lane] instanceof NormalNote) {
									random[7] = lane;
									random[lane] = 7;
									break;
								}
							}
						}
						break;
					case PLAYER2_5KEYS:
						if (ln[11] == -1 && notes[11] == null) {
							for (int lane = 0; lane < 5; lane++) {
								if (notes[lane + 6] != null && notes[lane + 6] instanceof NormalNote) {
									random[11] = lane + 6;
									random[lane + 6] = 11;
									break;
								}
							}
						}
						break;
					case PLAYER2_7KEYS:
						if (ln[15] == -1 && notes[15] == null) {
							for (int lane = 0; lane < 7; lane++) {
								if (notes[lane + 8] != null && notes[lane + 8] instanceof NormalNote) {
									random[15] = lane + 8;
									random[lane + 8] = 15;
									break;
								}
							}
						}
						break;
					case NINEKEYS:
						break;
					}
					break;
				case H_RANDOM:
					// TODO ノーツのあるレーンを先行して優先配置する方式へ変更
					switch (getModifyTarget()) {
					case PLAYER1_5KEYS:
						l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4));
						random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
						for (int lane = 0; lane < 5; lane++) {
							if (ln[lane] != -1) {
								random[lane] = ln[lane];
								l.remove((Integer) ln[lane]);
							}
						}
						for (int lane = 0; lane < 5; lane++) {
							if (ln[lane] == -1) {
								int r = -1;
								int count = l.size() - tl.getTotalNotes() - (prev != null ? prev.getTotalNotes() : 0);
								for (int i = 0; i < 100; i++) {
									r = (int) (Math.random() * l.size());
									if (prev == null || (prev.existNote(lane) && !tl.existNote(l.get(r)))
											|| (!prev.existNote(lane) && tl.existNote(l.get(r)))
											|| (count > 0 && !prev.existNote(lane) && !tl.existNote(l.get(r)))) {
										if (prev != null && !prev.existNote(lane) && !tl.existNote(l.get(r))) {
											count--;
										}
										break;
									}
								}
								random[lane] = l.get(r);
								l.remove(r);
							}
						}
						break;
					case PLAYER1_7KEYS:
						l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6));
						random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
						for (int lane = 0; lane < 7; lane++) {
							if (ln[lane] != -1) {
								random[lane] = ln[lane];
								l.remove((Integer) ln[lane]);
							}
						}
						for (int lane = 0; lane < 7; lane++) {
							if (ln[lane] == -1) {
								int r = -1;
								int count = l.size() - tl.getTotalNotes() - (prev != null ? prev.getTotalNotes() : 0);
								for (int i = 0; i < 100; i++) {
									r = (int) (Math.random() * l.size());
									if (prev == null || (prev.existNote(lane) && !tl.existNote(l.get(r)))
											|| (!prev.existNote(lane) && tl.existNote(l.get(r)))
											|| (count > 0 && !prev.existNote(lane) && !tl.existNote(l.get(r)))) {
										if (prev != null && !prev.existNote(lane) && !tl.existNote(l.get(r))) {
											count--;
										}
										break;
									}
								}
								random[lane] = l.get(r);
								l.remove(r);
							}
						}
						break;
					case PLAYER2_5KEYS:
						l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4));
						random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
						for (int lane = 0; lane < 5; lane++) {
							if (ln[lane + 6] != -1) {
								random[lane + 6] = ln[lane + 6];
								l.remove((Integer) ln[lane + 6] - 6);
							}
						}
						for (int lane = 0; lane < 5; lane++) {
							if (ln[lane + 6] == -1) {
								int r = -1;
								int count = l.size() - tl.getTotalNotes() - (prev != null ? prev.getTotalNotes() : 0);
								for (int i = 0; i < 100; i++) {
									r = (int) (Math.random() * l.size());
									if (prev == null || (prev.existNote(lane) && !tl.existNote(l.get(r)))
											|| (!prev.existNote(lane) && tl.existNote(l.get(r)))
											|| (count > 0 && !prev.existNote(lane) && !tl.existNote(l.get(r)))) {
										if (prev != null && !prev.existNote(lane) && !tl.existNote(l.get(r))) {
											count--;
										}
										break;
									}
								}
								random[lane + 6] = l.get(r) + 6;
								l.remove(r);
							}
						}
						break;
					case PLAYER2_7KEYS:
						l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6));
						random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
						for (int lane = 0; lane < 7; lane++) {
							if (ln[lane + 8] != -1) {
								random[lane + 8] = ln[lane + 8];
								l.remove((Integer) ln[lane + 8] - 8);
							}
						}
						for (int lane = 0; lane < 7; lane++) {
							if (ln[lane + 8] == -1) {
								int r = -1;
								int count = l.size() - tl.getTotalNotes() - (prev != null ? prev.getTotalNotes() : 0);
								for (int i = 0; i < 100; i++) {
									r = (int) (Math.random() * l.size());
									if (prev == null || (prev.existNote(lane) && !tl.existNote(l.get(r)))
											|| (!prev.existNote(lane) && tl.existNote(l.get(r)))
											|| (count > 0 && !prev.existNote(lane) && !tl.existNote(l.get(r)))) {
										if (prev != null && !prev.existNote(lane) && !tl.existNote(l.get(r))) {
											count--;
										}
										break;
									}
								}
								random[lane + 8] = l.get(r) + 8;
								l.remove(r);
							}
						}
						break;
					case NINEKEYS:
						l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
						random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
						for (int lane = 0; lane < 9; lane++) {
							if (ln[lane] != -1) {
								random[lane] = ln[lane];
								l.remove((Integer) (ln[lane]));
							}
						}
						for (int lane = 0; lane < 9; lane++) {
							if (ln[lane] == -1) {
								int r = -1;
								int count = l.size() - tl.getTotalNotes() - (prev != null ? prev.getTotalNotes() : 0);
								for (int i = 0; i < 100; i++) {
									r = (int) (Math.random() * l.size());
									int plane2 = l.get(r);
									if (prev == null || (prev.existNote(lane) && !tl.existNote(plane2))
											|| (!prev.existNote(lane) && tl.existNote(plane2)
													|| (count > 0 && !prev.existNote(lane) && !tl.existNote(plane2)))) {
										if (prev != null && !prev.existNote(lane) && !tl.existNote(plane2)) {
											count--;
										}
										break;
									}
								}
								random[lane] = l.get(r);
								l.remove(r);
							}
						}
						break;
					}
					if (tl.getTotalNotes(BMSModel.LNTYPE_HELLCHARGENOTE) > 0) {
						prev = tl;
					}
					break;
				case S_RANDOM_EX:
					switch (getModifyTarget()) {
					case PLAYER1_5KEYS:
						l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5));
						random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
						for (int lane = 0; lane < 6; lane++) {
							if (ln[lane] != -1) {
								random[lane] = ln[lane];
								l.remove((Integer) ln[lane]);
							}
						}
						for (int lane = 0; lane < 6; lane++) {
							if (ln[lane] == -1) {
								int r = (int) (Math.random() * l.size());
								random[lane] = l.get(r);
								l.remove(r);
							}
						}
						break;
					case PLAYER1_7KEYS:
						l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
						random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
						for (int lane = 0; lane < 8; lane++) {
							if (ln[lane] != -1) {
								random[lane] = ln[lane];
								l.remove((Integer) ln[lane]);
							}
						}
						for (int lane = 0; lane < 8; lane++) {
							if (ln[lane] == -1) {
								int r = (int) (Math.random() * l.size());
								random[lane] = l.get(r);
								l.remove(r);
							}
						}
						break;
					case PLAYER2_5KEYS:
						l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5));
						random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
						for (int lane = 0; lane < 6; lane++) {
							if (ln[lane + 6] != -1) {
								random[lane + 6] = ln[lane + 6];
								l.remove((Integer) ln[lane + 6] - 6);
							}
						}
						for (int lane = 0; lane < 6; lane++) {
							if (ln[lane + 6] == -1) {
								int r = (int) (Math.random() * l.size());
								random[lane + 6] = l.get(r) + 6;
								l.remove(r);
							}
						}
						break;
					case PLAYER2_7KEYS:
						l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
						random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
						for (int lane = 0; lane < 8; lane++) {
							if (ln[lane + 8] != -1) {
								random[lane + 8] = ln[lane + 8];
								l.remove((Integer) ln[lane + 8] - 8);
							}
						}
						for (int lane = 0; lane < 8; lane++) {
							if (ln[lane + 8] == -1) {
								int r = (int) (Math.random() * l.size());
								random[lane + 8] = l.get(r) + 8;
								l.remove(r);
							}
						}
						break;
					case NINEKEYS:
						// S-RANDOMと同じ
						l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
						random = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
						for (int lane = 0; lane < 9; lane++) {
							if (ln[lane] != -1) {
								random[lane] = ln[lane];
								l.remove((Integer) (ln[lane]));
							}
						}
						for (int lane = 0; lane < 9; lane++) {
							if (ln[lane] == -1) {
								int r = (int) (Math.random() * l.size());
								random[lane] = l.get(r);
								l.remove(r);
							}
						}
						break;
					}

					break;

				}

				for (int i = 0; i < lanes; i++) {
					Note n = notes[random[i]];
					Note hn = hnotes[random[i]];
					if (n instanceof LongNote) {
						LongNote ln2 = (LongNote) n;
						if (ln2.getSection() == tl.getSection()) {
							tl.setNote(i, n);
							ln[i] = random[i];
						} else {
							tl.setNote(i, n);
							ln[i] = -1;
						}
					} else {
						tl.setNote(i, n);
					}
					tl.setHiddenNote(i, hn);
				}
				log.add(new PatternModifyLog(tl.getTime(), random));
			}
		}
		return log;
	}
}
