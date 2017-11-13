package bms.player.beatoraja.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

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
		int[] lastNoteTime = new int[lanes];
		Arrays.fill(ln, -1);
		Arrays.fill(lastNoteTime, -100);
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
					random = keys.length > 0 ? timeBasedShuffle(keys, ln, notes, lastNoteTime, tl.getTime(), 40)
							: keys;
					break;
				case SPIRAL:
					keys = getKeys(mode, false);
					if (random.length == 0) {
						// 初期値の作成
						int max = 0;
						for (int key : keys) {
							max = Math.max(max, key);
						}
						random = new int[max + 1];
						for (int i = 0; i < random.length; i++) {
							random[i] = i;
						}

						int index = (int) (Math.random() * keys.length);
						int j = (int) (Math.random() * 2) >= 1 ? 1 : keys.length - 1;
						for (int i = 0; i < keys.length; i++) {
							random[keys[i]] = keys[index];
							index = (index + j) % keys.length;
						}
						inc = (int) (Math.random() * (keys.length - 1)) + 1;
						Logger.getGlobal().info("SPIRAL - 開始位置:" + index + " 増分:" + inc);
					} else {
						boolean cln = false;
						for (int lane = 0; lane < keys.length; lane++) {
							if (ln[keys[lane]] != -1) {
								cln = true;
							}
						}
						if (!cln) {
							int[] nrandom = Arrays.copyOf(random, random.length);
							int index = inc;
							for (int i = 0; i < keys.length; i++) {
								nrandom[keys[i]] = random[keys[index]];
								index = (index + 1) % keys.length;
							}
							random = nrandom;
						}
					}

					break;
				case ALL_SCR:
					random = new int[mode.key];
					for (int i = 0; i < random.length; i++) {
						random[i] = i;
					}
					boolean rightside = (getModifyTarget() == SIDE_2P);
					if (!rightside || mode.player == 2) {
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
					keys = getKeys(mode, false);
					random = keys.length > 0 ?timeBasedShuffle(keys, ln,
							notes, lastNoteTime, tl.getTime(), 110)
							: keys;
					break;
				case S_RANDOM_EX:
					keys = getKeys(mode, true);
					random = keys.length > 0 ? timeBasedShuffle(keys, ln,
							notes, lastNoteTime, tl.getTime(), 40)
							: keys;
					break;

				}

				for (int i = 0; i < lanes; i++) {
					final int mod = i < random.length ? random[i] : i;
					Note n = notes[mod];
					Note hn = hnotes[mod];
					if (n instanceof LongNote) {
						LongNote ln2 = (LongNote) n;
						if (ln2.isEnd()) {
							tl.setNote(i, n);
							ln[i] = -1;
						} else {
							tl.setNote(i, n);
							ln[i] = mod;
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
				log.add(new PatternModifyLog(tl.getSection(), random));
			}
		}
		return log;
	}

	// 直前ノーツとの時間考慮shuffle duration[ms]時間未満の縦連打が出来るだけ発生しないようにshuffleをかける
	private static int[] timeBasedShuffle(int[] keys, int[] activeln,
			Note[] notes, int[] lastNoteTime, int now, int duration) {
		List<Integer> assignLane = new ArrayList<Integer>(keys.length);
		List<Integer> originalLane = new ArrayList<Integer>(keys.length);
		for (int key : keys) {
			assignLane.add(key);
			originalLane.add(key);
		}
		int max = 0;
		for (int key : keys) {
			max = Math.max(max, key);
		}
		int[] result = new int[max + 1];
		for (int i = 0; i < result.length; i++) {
			result[i] = i;
		}

		// LNがアクティブなレーンをアサインしてから除外
		for (int lane = 0; lane < keys.length; lane++) {
			if (activeln != null && activeln[keys[lane]] != -1) {
				result[keys[lane]] = activeln[keys[lane]];
				assignLane.remove((Integer) keys[lane]);
				originalLane.remove((Integer) activeln[keys[lane]]);
			}
		}
		List<Integer> noteLane, otherLane;
		noteLane = new ArrayList<Integer>(keys.length);
		otherLane = new ArrayList<Integer>(keys.length);

		// 元のレーンをノーツの存在で分類
		while (!originalLane.isEmpty()) {
			if (notes[originalLane.get(0)] != null) {
				noteLane.add(originalLane.get(0));
			} else {
				otherLane.add(originalLane.get(0));
			}
			originalLane.remove(0);
		}

		// 未アサインレーンを縦連発生かどうかで分類
		List<Integer> rendaLane, primaryLane;
		rendaLane = new ArrayList<Integer>(keys.length);
		primaryLane = new ArrayList<Integer>(keys.length);
		while (!assignLane.isEmpty()) {
			if (now - lastNoteTime[assignLane.get(0)] < duration) {
				rendaLane.add(assignLane.get(0));
			} else {
				primaryLane.add(assignLane.get(0));
			}
			assignLane.remove(0);
		}

		// ノーツがあるレーンを縦連が発生しないレーンに配置
		while (!(noteLane.isEmpty() || primaryLane.isEmpty())) {
			int r = (int)(Math.random() * primaryLane.size());
			result[primaryLane.get(r)] = noteLane.get(0);
			primaryLane.remove(r);
			noteLane.remove(0);
		}

		// noteLaneが空でなかったら
		// lastNoteTimeが小さいレーンから順番に置いていく
		while (!noteLane.isEmpty()) {
			int min = Integer.MAX_VALUE;
			int r = rendaLane.get(0);
			for ( int i = 0; i < rendaLane.size(); i++) {
				if (min > lastNoteTime[rendaLane.get(i)]) {
					min = lastNoteTime[rendaLane.get(i)];
				}
			}
			ArrayList<Integer> minLane = new ArrayList<Integer>(rendaLane.size());
			for ( int i = 0; i < rendaLane.size(); i++) {
				if ( min == lastNoteTime[rendaLane.get(i)]) {
					minLane.add(rendaLane.get(i));
				}
			}
			r = (int)(Math.random() * minLane.size());
			result[minLane.get(r)] = noteLane.get(0);
			rendaLane.remove((Integer)minLane.get(r));
			noteLane.remove(0);
		}

		primaryLane.addAll(rendaLane);
		// 残りをランダムに置いていく
		while (!otherLane.isEmpty()) {
			int r = (int)(Math.random() * primaryLane.size());
			result[primaryLane.get(r)] = otherLane.get(0);
			primaryLane.remove(r);
			otherLane.remove(0);
		}


		return result;
	}

}
