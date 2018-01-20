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
		int[] endLnNoteTime = new int[lanes];
		int scratchIndex = 0;
		Arrays.fill(ln, -1);
		Arrays.fill(lastNoteTime, -100);
		Arrays.fill(endLnNoteTime, -1);
		for (TimeLine tl : model.getAllTimeLines()) {
			if (tl.existNote() || tl.existHiddenNote()) {
				Note[] notes = new Note[lanes];
				Note[] hnotes = new Note[lanes];
				for (int i = 0; i < lanes; i++) {
					notes[i] = tl.getNote(i);
					hnotes[i] = tl.getHiddenNote(i);
				}
				int[] keys;
				switch (type) {
				case S_RANDOM:
					keys = getKeys(mode, false);
					if(mode == Mode.POPN_9K) {
						random = keys.length > 0 ? timeBasedShuffle(keys, ln, notes, lastNoteTime, tl.getTime(), 0)
								: keys;
					} else {
						random = keys.length > 0 ? timeBasedShuffle(keys, ln, notes, lastNoteTime, tl.getTime(), 40)
								: keys;
					}
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
					// スクラッチレーンが無いなら何もしない
					if (mode.scratchKey.length == 0) {
						break;
					}

					random = new int[mode.key];
					for (int i = 0; i < random.length; i++) {
						random[i] = i;
					}

					/*
					 * 連皿しきい値
					 */
					int scratchInterval = 40;

					// Scratchレーンが複数ある場合は順繰りに配置されるように (24key対応)
					if (mode.player == 1) {
						// シングルプレー時
						int[] sckey = mode.scratchKey;
						boolean existsScratch = false;
						boolean sclnisactive = false;
						int lastScNoteTime = Integer.MIN_VALUE;
						for (int sc : sckey) {
							existsScratch = existsScratch || (notes[sc] != null);
							sclnisactive = sclnisactive || (ln[sc] != -1);
							lastScNoteTime = lastScNoteTime > lastNoteTime[sc] ? lastScNoteTime : lastNoteTime[sc];
						}
						// sckeyがln activeでなく、sckeyにノーツがなく、最後にスクラッチを置いてから十分時間が経っていれば
						if (!(sclnisactive || existsScratch)
								&& tl.getTime() - lastScNoteTime > scratchInterval) {
							// normalnoteをリストアップしてそこからランダムに入れ替える
							ArrayList<Integer> l = new ArrayList<Integer>(mode.key);
							for (int i = 0; i < mode.key; i++) {
								if (notes[i] != null && notes[i] instanceof NormalNote) {
									l.add(i);
								}
							}
							if (!l.isEmpty()) {
								int r = (int) (Math.random() * l.size());
								random[sckey[scratchIndex]] = l.get(r);
								random[l.get(r)] = sckey[scratchIndex];
								scratchIndex = ++scratchIndex == sckey.length ? 0 : scratchIndex;
							}

						}
					} else if (mode.player == 2) {
						if (mode == Mode.KEYBOARD_24K_DOUBLE) {
							// TODO 24k-DPに対応
							break;
						}
						// ダブルプレー時
						// スクラッチ側の鍵盤に優先的にアサインされるようにする
						// 連打は出来ないように sc:40ms key:110ms
						keys = getKeys(mode, true);
						int keyInterval = 110;
						boolean isRightSide = (getModifyTarget() == SIDE_2P);
						int scLane = isRightSide ? mode.scratchKey[1] : mode.scratchKey[0];
						ArrayList<Integer> original, assign, note, other, primary, tate;
						original = new ArrayList<Integer>(keys.length);
						assign = new ArrayList<Integer>(keys.length);
						note = new ArrayList<Integer>(keys.length);
						other = new ArrayList<Integer>(keys.length);
						primary = new ArrayList<Integer>(keys.length);
						tate = new ArrayList<Integer>(keys.length);

						for (int lane = 0; lane < keys.length; lane++) {
							original.add(keys[lane]);
							if (isRightSide) {
								assign.add(keys[keys.length - lane - 1]);
							} else {
								assign.add(keys[lane]);
							}
						}

						// scLaneを先頭に
						if (!isRightSide) {
							assign.remove((Integer) scLane);
							assign.add(0, scLane);
						}

						// LNがアクティブなレーンをアサインしてから除外
						for (int lane = 0; lane < keys.length; lane++) {
							if (ln[keys[lane]] != -1) {
								random[keys[lane]] = ln[keys[lane]];
								assign.remove((Integer) keys[lane]);
								original.remove((Integer) ln[keys[lane]]);
							}
						}

						// 元のレーンをノーツの存在で分類
						while (!original.isEmpty()) {
							if (notes[original.get(0)] != null) {
								note.add(original.get(0));
							} else {
								other.add(original.get(0));
							}
							original.remove(0);
						}

						// 未アサインレーンを縦連発生かどうかで分類
						while (!assign.isEmpty()) {
							if (tl.getTime() - lastNoteTime[assign.get(0)] < (assign.get(0) == scLane ? scratchInterval
									: keyInterval)) {
								tate.add(assign.get(0));
							} else {
								primary.add(assign.get(0));
							}
							assign.remove(0);
						}

						// ノーツがあるレーンを縦連が発生しないレーンに配置
						while (!(note.isEmpty() || primary.isEmpty())) {
							random[primary.get(0)] = note.get(0);
							primary.remove(0);
							note.remove(0);
						}

						// noteLaneが空でなかったら
						// lastNoteTimeが小さいレーンから順番に置いていく
						while (!note.isEmpty()) {
							int min = Integer.MAX_VALUE;
							int minLane = tate.get(0);
							for (int i = 0; i < tate.size(); i++) {
								if (min > lastNoteTime[tate.get(i)]) {
									min = lastNoteTime[tate.get(i)];
									minLane = tate.get(i);
								}
							}
							random[minLane] = note.get(0);
							tate.remove((Integer) minLane);
							note.remove(0);
						}

						primary.addAll(tate);
						// 残りを置いていく
						while (!other.isEmpty()) {
							random[primary.get(0)] = other.get(0);
							primary.remove(0);
							other.remove(0);
						}

					}
					break;

				case H_RANDOM:
					keys = getKeys(mode, false);
					random = keys.length > 0 ? timeBasedShuffle(keys, ln,
							notes, lastNoteTime, tl.getTime(), 110)
							: keys;
					break;
				case S_RANDOM_EX:
					keys = getKeys(mode, true);
					if(mode == Mode.POPN_9K) {
						random = keys.length > 0 ? noMurioshiShuffle(keys, ln,
								notes, lastNoteTime, tl.getTime(), 125)
								: keys;
					} else {
						random = keys.length > 0 ? timeBasedShuffle(keys, ln,
								notes, lastNoteTime, tl.getTime(), 40)
								: keys;
					}
					break;

				}

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
			int r = (int) (Math.random() * primaryLane.size());
			result[primaryLane.get(r)] = noteLane.get(0);
			primaryLane.remove(r);
			noteLane.remove(0);
		}

		// noteLaneが空でなかったら
		// lastNoteTimeが小さいレーンから順番に置いていく
		while (!noteLane.isEmpty()) {
			int min = Integer.MAX_VALUE;
			int r = rendaLane.get(0);
			for (int i = 0; i < rendaLane.size(); i++) {
				if (min > lastNoteTime[rendaLane.get(i)]) {
					min = lastNoteTime[rendaLane.get(i)];
				}
			}
			ArrayList<Integer> minLane = new ArrayList<Integer>(rendaLane.size());
			for (int i = 0; i < rendaLane.size(); i++) {
				if (min == lastNoteTime[rendaLane.get(i)]) {
					minLane.add(rendaLane.get(i));
				}
			}
			r = (int) (Math.random() * minLane.size());
			result[minLane.get(r)] = noteLane.get(0);
			rendaLane.remove((Integer) minLane.get(r));
			noteLane.remove(0);
		}

		primaryLane.addAll(rendaLane);
		// 残りをランダムに置いていく
		while (!otherLane.isEmpty()) {
			int r = (int) (Math.random() * primaryLane.size());
			result[primaryLane.get(r)] = otherLane.get(0);
			primaryLane.remove(r);
			otherLane.remove(0);
		}

		return result;
	}
	
	// 無理押しとduration[ms]時間未満の縦連打がなるべく来ないようにshuffleをかける
	private static int[] noMurioshiShuffle(int[] keys, int[] activeln,
		Note[] notes, int[] lastNoteTime, int now, int duration) {
		List<Integer> assignedLane = new ArrayList<Integer>(keys.length);
		List<Integer> noAssignedLane = new ArrayList<Integer>(keys.length);
		List<Integer> originalLane = new ArrayList<Integer>(keys.length);

		for (int key : keys) {
			noAssignedLane.add(key);
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
				assignedLane.add((Integer) keys[lane]);
				noAssignedLane.remove((Integer) keys[lane]);
				originalLane.remove((Integer) activeln[keys[lane]]);
			}
		}
		List<Integer> noteLane, otherLane;
		noteLane = new ArrayList<Integer>(keys.length);
		otherLane = new ArrayList<Integer>(keys.length);

		// 元のレーンをノーツの存在で分類
		while (!originalLane.isEmpty()) {
			if (notes[originalLane.get(0)] != null && (notes[originalLane.get(0)] instanceof NormalNote || notes[originalLane.get(0)] instanceof LongNote)) {
				noteLane.add(originalLane.get(0));
			} else {
				otherLane.add(originalLane.get(0));
			}
			originalLane.remove(0);
		}

		//無理押しにならないようにランダムに置いていく
		//7個押し以上では無理押ししか存在しないので除外
		if(assignedLane.size() + noteLane.size() <= 6) {
			List<Integer> kouhoLane = new ArrayList<Integer>(keys.length); //置ける候補
			List<Integer> rendaLane = new ArrayList<Integer>(keys.length); //置くと縦連打になるレーン
			while (!(noteLane.isEmpty() || noAssignedLane.isEmpty())) {
				kouhoLane.clear();
				rendaLane.clear();
				if(assignedLane.size() <= 1) {
					kouhoLane.addAll(noAssignedLane); //既にノートが置かれているレーンが1個以下であれば全部が候補
				} else {
					int[] referencePoint = new int[2]; //既にノートが置かれているレーンの中で左端のレーンと右端のレーン
					referencePoint[0] = max;
					referencePoint[1] = 0;
					for(int i = 0; i < assignedLane.size(); i++){
						referencePoint[0] = Math.min(referencePoint[0] , assignedLane.get(i));
						referencePoint[1] = Math.max(referencePoint[1] , assignedLane.get(i));
					}
					if(referencePoint[1] - referencePoint[0] <= 2) {
						kouhoLane.addAll(noAssignedLane); //既にノートが置かれているレーンが片手で押せる範囲であれば全部が候補
					} else if(referencePoint[1] - referencePoint[0] == 3) {
						if(noAssignedLane.indexOf(referencePoint[0] - 2) != -1) kouhoLane.add(referencePoint[0] - 2);
						if(noAssignedLane.indexOf(referencePoint[0] - 1) != -1) kouhoLane.add(referencePoint[0] - 1);
						if(noAssignedLane.indexOf(referencePoint[0] + 1) != -1) kouhoLane.add(referencePoint[0] + 1);
						if(noAssignedLane.indexOf(referencePoint[0] + 2) != -1) kouhoLane.add(referencePoint[0] + 2);
						if(noAssignedLane.indexOf(referencePoint[1] + 2) != -1) kouhoLane.add(referencePoint[1] + 2);
						if(noAssignedLane.indexOf(referencePoint[1] + 1) != -1) kouhoLane.add(referencePoint[1] + 1);
						if(noAssignedLane.indexOf(referencePoint[1] - 1) != -1) kouhoLane.add(referencePoint[1] - 1);
						if(noAssignedLane.indexOf(referencePoint[1] - 2) != -1) kouhoLane.add(referencePoint[1] - 2);
					} else if(referencePoint[1] - referencePoint[0] == 4) {
						if(noAssignedLane.indexOf(referencePoint[0] - 2) != -1 && noAssignedLane.indexOf(referencePoint[0] + 1) != -1) kouhoLane.add(referencePoint[0] - 2);
						if(noAssignedLane.indexOf(referencePoint[0] - 1) != -1) kouhoLane.add(referencePoint[0] - 1);
						if(noAssignedLane.indexOf(referencePoint[0] + 1) != -1) kouhoLane.add(referencePoint[0] + 1);
						if(noAssignedLane.indexOf(referencePoint[0] + 2) != -1) kouhoLane.add(referencePoint[0] + 2);
						if(noAssignedLane.indexOf(referencePoint[1] + 2) != -1 && noAssignedLane.indexOf(referencePoint[1] - 1) != -1) kouhoLane.add(referencePoint[1] + 2);
						if(noAssignedLane.indexOf(referencePoint[1] + 1) != -1) kouhoLane.add(referencePoint[1] + 1);
						if(noAssignedLane.indexOf(referencePoint[1] - 1) != -1) kouhoLane.add(referencePoint[1] - 1);
						if(noAssignedLane.indexOf(referencePoint[1] - 2) != -1) kouhoLane.add(referencePoint[1] - 2);
					} else if(referencePoint[1] - referencePoint[0] >= 5) {
						if(noAssignedLane.indexOf(referencePoint[0] - 2) != -1 && noAssignedLane.indexOf(referencePoint[0] + 1) != -1 && noAssignedLane.indexOf(referencePoint[0] + 2) != -1) kouhoLane.add(referencePoint[0] - 2);
						if(noAssignedLane.indexOf(referencePoint[0] - 1) != -1 && noAssignedLane.indexOf(referencePoint[0] + 2) != -1) kouhoLane.add(referencePoint[0] - 1);
						if(noAssignedLane.indexOf(referencePoint[0] + 1) != -1) kouhoLane.add(referencePoint[0] + 1);
						if(noAssignedLane.indexOf(referencePoint[0] + 2) != -1) kouhoLane.add(referencePoint[0] + 2);
						if(noAssignedLane.indexOf(referencePoint[1] + 2) != -1 && noAssignedLane.indexOf(referencePoint[1] - 1) != -1 && noAssignedLane.indexOf(referencePoint[1] - 2) != -1) kouhoLane.add(referencePoint[1] + 2);
						if(noAssignedLane.indexOf(referencePoint[1] + 1) != -1 && noAssignedLane.indexOf(referencePoint[1] - 2) != -1) kouhoLane.add(referencePoint[1] + 1);
						if(noAssignedLane.indexOf(referencePoint[1] - 1) != -1) kouhoLane.add(referencePoint[1] - 1);
						if(noAssignedLane.indexOf(referencePoint[1] - 2) != -1) kouhoLane.add(referencePoint[1] - 2);
					}
				}
				for(int i = 0; i < kouhoLane.size(); i++){
					if (now - lastNoteTime[kouhoLane.get(i)] < duration) {
						rendaLane.add(kouhoLane.get(i));
					}
				}
				if(kouhoLane.size() > rendaLane.size()) kouhoLane.removeAll(rendaLane); //縦連打になるレーンを除外。ただし候補全部が縦連打になる場合無理押しでないことの方を優先
				if(kouhoLane.isEmpty()) break;
				int r = (int) (Math.random() * kouhoLane.size());
				result[kouhoLane.get(r)] = noteLane.get(0);
				assignedLane.add(kouhoLane.get(r));
				noAssignedLane.remove(kouhoLane.get(r));
				noteLane.remove(0);
			}
		}

		// noteLaneが空でなかったら残りのノートをランダムに置いていく
		while (!noteLane.isEmpty()) {
			int r = (int) (Math.random() * noAssignedLane.size());
			result[noAssignedLane.get(r)] = noteLane.get(0);
			noAssignedLane.remove(r);
			noteLane.remove(0);
		}

		// 残りをランダムに置いていく
		while (!otherLane.isEmpty()) {
			int r = (int) (Math.random() * noAssignedLane.size());
			result[noAssignedLane.get(r)] = otherLane.get(0);
			noAssignedLane.remove(r);
			otherLane.remove(0);
		}

		return result;
	}

}
