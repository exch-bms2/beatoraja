package bms.player.beatoraja.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.Mode;
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
		List<PatternModifyLog> log = new ArrayList<PatternModifyLog>();
		Mode mode = model.getMode();
		int lanes = mode.key;
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
				int[] keys;
				switch (type) {
				case S_RANDOM:
					keys = getKeys(mode, false);
					random = keys.length > 0 ? shuffle(keys, ln) : keys;
					break;
				case SPIRAL:
					// TODO 譜面アルゴリズムの共通化(今後のモード増加へ対応するため)
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
					random = new int[mode.key];
					for (int i = 0; i < random.length; i++) {
						random[i] = i;
					}
					boolean rightside = (getModifyTarget() == PLAYER2_5KEYS ||  getModifyTarget() == PLAYER2_7KEYS);
					if(!rightside || mode.player == 2) {
						int sckey = rightside ? mode.scratchKey[1] : mode.scratchKey[0];
						if (ln[sckey] == -1 && notes[sckey] == null) {
							for (int i = 0; i < mode.key / mode.player; i++) {
								int lane = i + (rightside ? mode.key / mode.player : 0);
								if (lane != sckey && notes[lane] != null && notes[lane] instanceof NormalNote) {
									random[sckey] = lane;
									random[lane] = sckey;
									break;
								}
							}
						}						
					}
					break;
				case H_RANDOM:
					// TODO ノーツのあるレーンを先行して優先配置する方式へ変更
					// TODO 譜面アルゴリズムの共通化(今後のモード増加へ対応するため)
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
					keys = getKeys(mode, true);
					random = keys.length > 0 ? shuffle(keys, ln) : keys;
					break;

				}

				for (int i = 0; i < lanes; i++) {
					final int mod = i < random.length ? random[i] : i;
					Note n = notes[mod];
					Note hn = hnotes[mod];
					if (n instanceof LongNote) {
						LongNote ln2 = (LongNote) n;
						if (ln2.getSection() == tl.getSection()) {
							tl.setNote(i, n);
							ln[i] = mod;
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
