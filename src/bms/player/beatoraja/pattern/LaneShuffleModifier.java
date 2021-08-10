package bms.player.beatoraja.pattern;

import bms.model.*;

import java.util.*;
import java.util.logging.Logger;

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
	private Random type;

	public LaneShuffleModifier(Random type) {
		this.type = type;
	}

	private void makeRandom(BMSModel model) {
		Mode mode = model.getMode();
		int[] keys;
		switch (type) {
		case MIRROR:
			keys = getKeys(mode, false);
			random = keys.length > 0 ? rotate(keys, keys.length - 1, false) : keys;
			break;
		case R_RANDOM:
			keys = getKeys(mode, false);
			random = keys.length > 0 ? rotate(keys, getSeed()) : keys;
			break;
		case RANDOM:
			keys = getKeys(mode, false);
			random = keys.length > 0 ? shuffle(keys, getSeed()) : keys;
			break;
		case CROSS:
			keys = getKeys(mode, false);
			random = new int[keys.length];
			for (int i = 0; i < keys.length / 2 - 1; i += 2) {
				random[i] = keys[i + 1];
				random[i + 1] = keys[i];
				random[keys.length - i - 1] = keys[keys.length - i - 2];
				random[keys.length - i - 2] = keys[keys.length - i - 1];
			}
			break;
		case RANDOM_EX:
			keys = getKeys(mode, true);
			if(mode == Mode.POPN_9K) {
				random = keys.length > 0 ? noMurioshiLaneShuffle(model) : keys;
			} else {
				random = keys.length > 0 ? shuffle(keys, getSeed()) : keys;
				setAssistLevel(AssistLevel.LIGHT_ASSIST);
			}
			break;
		case FLIP:
			if (mode.player == 2) {
				random = new int[mode.key];
				for (int i = 0; i < random.length; i++) {
					random[i] = (i + (mode.key / mode.player)) % mode.key;
				}
			} else {
				random = new int[0];
			}
			break;
		case BATTLE:
			if (mode.player == 1) {
				random = new int[0];
			} else {
				keys = getKeys(mode, true);
				random = new int[keys.length * 2];
				System.arraycopy(keys, 0, random, 0, keys.length);
				System.arraycopy(keys, 0, random, keys.length, keys.length);
				setAssistLevel(AssistLevel.LIGHT_ASSIST);
			}
			break;

		}
	}

	// 無理押しが来ないようにLaneShuffleをかける(ただし正規鏡を除く)。無理押しが来ない譜面が存在しない場合は正規か鏡でランダム
	private int[] noMurioshiLaneShuffle(BMSModel model) {
		Mode mode = model.getMode();
		int[] keys;
		keys = getKeys(mode, false);
		int lanes = mode.key;
		int[] ln = new int[lanes];
		int[] endLnNoteTime = new int[lanes];
		int max = 0;
		for (int key : keys) {
			max = Math.max(max, key);
		}
		boolean isImpossible = false; //7個押し以上が存在するかどうか
		Set<Integer> originalPatternList = new HashSet<>(); //3個押し以上の同時押しパターンのセット
		Arrays.fill(ln, -1);
		Arrays.fill(endLnNoteTime, -1);

		//3個押し以上の同時押しパターンのリストを作る
		for (TimeLine tl : model.getAllTimeLines()) {
			if (tl.existNote()) {
				//LN
				for (int i = 0; i < lanes; i++) {
					Note n = tl.getNote(i);
					if (n instanceof LongNote) {
						LongNote ln2 = (LongNote) n;
						if (ln2.isEnd() && tl.getTime() == endLnNoteTime[i]) {
							ln[i] = -1;
							endLnNoteTime[i] = -1;
						} else {
							ln[i] = i;
							if (!ln2.isEnd()) {
								endLnNoteTime[i] = ln2.getPair().getTime();
							}
						}
					}
				}
				//通常ノート
				List<Integer> noteLane = new ArrayList<>(keys.length);
				for (int i = 0; i < lanes; i++) {
					Note n = tl.getNote(i);
					if (n != null && n instanceof NormalNote || ln[i] != -1) {
						noteLane.add(i);
					}
				}
				//7個押し以上が一つでも存在すれば無理押しが来ない譜面は存在しない
				if (noteLane.size() >= 7) {
					isImpossible = true;
					break;
				} else if (noteLane.size() >= 3) {
					int pattern = 0;
					for (Integer i : noteLane) {
						pattern += (int) Math.pow(2, i);
					}
					originalPatternList.add(pattern);
				}
			}
		}

		List<List<Integer>> kouhoPatternList = new ArrayList<>(); //無理押しが来ない譜面のリスト
		if (!isImpossible) {
			kouhoPatternList = searchForNoMurioshiLaneCombinations(originalPatternList, keys);
		}

		Logger.getGlobal().info("無理押し無し譜面数 : "+(kouhoPatternList.size()));

		int[] result = new int[9];
		if (kouhoPatternList.size() > 0) {
			int r = (int) (Math.random() * kouhoPatternList.size());
			for (int i = 0; i < 9; i++) {
				result[kouhoPatternList.get(r).get(i)] = i;
			}
		//無理押しが来ない譜面が存在しない場合は正規か鏡でランダム
		} else {
			int mirror = (int) (Math.random() * 2);
			for (int i = 0; i < 9; i++) {
				result[i] = mirror == 0 ? i : 8 - i;
			}
		}
		return result;
	}

	private List<List<Integer>> searchForNoMurioshiLaneCombinations(Set<Integer> originalPatternList, int[] keys) {
		List<List<Integer>> noMurioshiLaneCombinations = new ArrayList<>(); // 無理押しが来ない譜面のリスト
		List<Integer> tempPattern = new ArrayList<>(keys.length);
		int[] indexes = new int[9];
		int[] laneNumbers = new int[9];
		for (int i = 0; i < 9; i++) {
			laneNumbers[i] = i;
			indexes[i] = 0;
		}

		List<List<Integer>> murioshiChords = Arrays.asList(
				Arrays.asList(1, 4, 7),
				Arrays.asList(1, 4, 8),
				Arrays.asList(1, 4, 9),
				Arrays.asList(1, 5, 8),
				Arrays.asList(1, 5, 9),
				Arrays.asList(1, 6, 9),
				Arrays.asList(2, 5, 8),
				Arrays.asList(2, 5, 9),
				Arrays.asList(2, 6, 9),
				Arrays.asList(3, 6, 9)
		);

		int i = 0;
		while (i < 9) {
			if (indexes[i] < i) {
				swap(laneNumbers, i % 2 == 0 ? 0 : indexes[i], i);

				boolean murioshiFlag = false;
				for (Integer pattern : originalPatternList) {
					tempPattern.clear();
					for (int j = 0; j < 9; j++) {
						if (((int) (pattern / Math.pow(2, j)) % 2) == 1) {
							tempPattern.add(laneNumbers[j] + 1);
						}
					}

					murioshiFlag = murioshiChords.stream().anyMatch(tempPattern::containsAll);
					if (murioshiFlag) {
						break;
					}
				}
				if (!murioshiFlag) {
					List<Integer> randomCombination = new ArrayList<>();
					for (int j = 0; j < 9; j++) {
						randomCombination.add(laneNumbers[j]);
					}
					noMurioshiLaneCombinations.add(randomCombination);
				}

				indexes[i]++;
				i = 0;
			} else {
				indexes[i] = 0;
				i++;
			}
		}

		noMurioshiLaneCombinations.remove(Arrays.asList(8, 7, 6, 5, 4, 3, 2, 1, 0));
		return noMurioshiLaneCombinations;
	}

	private void swap(int[] input, int a, int b) {
		int tmp = input[a];
		input[a] = input[b];
		input[b] = tmp;
	}

	@Override
	public List<PatternModifyLog> modify(BMSModel model) {
		List<PatternModifyLog> log = new ArrayList();
		makeRandom(model);
		int lanes = model.getMode().key;
		TimeLine[] timelines = model.getAllTimeLines();
		for (int index = 0; index < timelines.length; index++) {
			final TimeLine tl = timelines[index];
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
							if (notes[mod] instanceof LongNote && ((LongNote) notes[mod]).isEnd()) {
								for (int j = index - 1; j >= 0; j--) {
									if (((LongNote) notes[mod]).getPair().getSection() == timelines[j].getSection()) {
										LongNote ln = (LongNote) timelines[j].getNote(i);
										tl.setNote(i, ln.getPair());
										System.out.println(ln.toString() + " : " + ln.getPair().toString() + " == "
												+ ((LongNote) notes[mod]).getPair().toString() + " : "
												+ notes[mod].toString());
										break;
									}
								}
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
				log.add(new PatternModifyLog(tl.getSection(), random));
			}
		}
		return log;
	}

}
