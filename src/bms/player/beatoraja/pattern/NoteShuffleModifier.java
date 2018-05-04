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
import bms.player.beatoraja.PlayerConfig;

/**
 * �궭�궎�깲�꺀�궎�꺍�뜕鵝띲겎�깕�꺖�깂�굮�뀯�굦�쎘�걟�굥�걼�굙�겗�궚�꺀�궧竊�
 *
 * @author exch
 */
public class NoteShuffleModifier extends PatternModifier {
	private static final PlayerConfig config = playerConfig;
	/**
	 * �궭�궎�깲�꺀�궎�꺍驪롢겓�깕�꺖�깂�굮�꺀�꺍���깲�겓�뀯�굦�쎘�걟�굥
	 */
	public static final int S_RANDOM = 0;
	/**
	 * �닜�쐿�겗訝╉겧�쎘�걟�굮�깧�꺖�궧�겓�곮왉�뿃�듁�겓訝╉겧�쎘�걟�굥
	 */
	public static final int SPIRAL = 1;
	/**
	 * �깕�꺖�깂�굮�궧�궚�꺀�긿�긽�꺃�꺖�꺍�겓�썓榮꾠걲�굥
	 */
	public static final int ALL_SCR = 2;
	/**
	 * S-RANDOM�겓潁��ｃ걣璵드뒟�씎�겒�걚�굠�걝�겓�뀓營��걲�굥
	 */
	public static final int H_RANDOM = 3;
	/**
	 * �궧�궚�꺀�긿�긽�꺃�꺖�꺍�굮�맜�굙�걼S-RANDOM
	 */
	public static final int S_RANDOM_EX = 4;

	/**
	 * 7to9
	 */
	public static final int SEVEN_TO_NINE = 100;

	private int type;
	/**
	 * 轝▲겗TimeLine罌쀥뒥�늽(SPIRAL�뵪)
	 */
	private int inc;

	/**
	 * �ｆ돀�걮�걤�걚��(ms)(H-RANDOM�뵪)
	 */
	private int hranThreshold = 125;

	public NoteShuffleModifier(int type) {
		super(type >= ALL_SCR ? 1 : 0);
		this.type = type;
	}

	/**
	 * �ｆ돀�썮�빊(PMS ALLSCR�뵪)
	 */
	private static int[] laneRendaCount;

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
		laneRendaCount = new int[lanes];
		Arrays.fill(laneRendaCount, 0);
		
		calculateHranThreshold();
		
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
						// �닜�쐿�ㅳ겗鵝쒏닇
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
						Logger.getGlobal().info("SPIRAL - �뼀冶뗤퐤營�:" + index + " 罌쀥늽:" + inc);
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
					if(mode == Mode.POPN_9K) {
						keys = getKeys(mode, false);
						random = keys.length > 0 ? rendaShuffle(keys, ln, notes, lastNoteTime, tl.getTime(), hranThreshold, 60)
								: keys;
						break;
					}
					// �궧�궚�꺀�긿�긽�꺃�꺖�꺍�걣�꽒�걚�겒�굢鵝뺛굚�걮�겒�걚
					if (mode.scratchKey.length == 0) {
						break;
					}

					random = new int[mode.key];
					for (int i = 0; i < random.length; i++) {
						random[i] = i;
					}

					/*
					 * �ｇ슼�걮�걤�걚��
					 */
					int scratchInterval = 40;

					// Scratch�꺃�꺖�꺍�걣筽뉑빊�걗�굥�졃�릦�겘�젂濚겹굤�겓�뀓營��걬�굦�굥�굠�걝�겓 (24key野얍퓶)
					if (mode.player == 1) {
						// �궥�꺍�궛�꺂�깤�꺃�꺖�셽
						keys = getKeys(mode, true);
						int keyInterval = hranThreshold;
						ArrayList<Integer> original, assign, note, other, primary, tate, sckey;
						original = new ArrayList<Integer>(keys.length);
						assign = new ArrayList<Integer>(keys.length);
						note = new ArrayList<Integer>(keys.length);
						other = new ArrayList<Integer>(keys.length);
						primary = new ArrayList<Integer>(keys.length);
						tate = new ArrayList<Integer>(keys.length);
						sckey = new ArrayList<Integer>(mode.scratchKey.length);

						for (int lane = 0; lane < keys.length; lane++) {
							original.add(keys[lane]);
							assign.add(keys[lane]);
						}
						
						for (int sc = 0; sc < mode.scratchKey.length; sc++) {
							sckey.add(mode.scratchKey[sc]);
						}

						// LN�걣�궋�궚�깇�궍�깣�겒�꺃�꺖�꺍�굮�궋�궢�궎�꺍�걮�겍�걢�굢�솮鸚�
						for (int lane = 0; lane < keys.length; lane++) {
							if (ln[keys[lane]] != -1) {
								random[keys[lane]] = ln[keys[lane]];
								assign.remove((Integer) keys[lane]);
								original.remove((Integer) ln[keys[lane]]);
							}
						}

						// �뀇�겗�꺃�꺖�꺍�굮�깕�꺖�깂�겗耶섇쑉�겎�늽窈�
						while (!original.isEmpty()) {
							if (notes[original.get(0)] != null) {
								note.add(original.get(0));
							} else {
								other.add(original.get(0));
							}
							original.remove(0);
						}
						
						// 

						// �쑋�궋�궢�궎�꺍�꺃�꺖�꺍�굮�늽窈� 1.轝▲겓�뀓營��걲�굥�궧�궚�꺀�긿�긽�꺃�꺖�꺍�겎�겒�걚 2.潁��ｃ걣�쇇�뵟�걲�굥
						while (!assign.isEmpty()) {
							if ((
									sckey.contains(assign.get(0)) && assign.get(0) != sckey.get(scratchIndex))
									|| tl.getTime() - lastNoteTime[assign.get(0)]
											< (sckey.contains(assign.get(0)) ? scratchInterval : keyInterval)) {
								tate.add(assign.get(0));
							} else {
								primary.add(assign.get(0));
							}
							
							assign.remove(0);
						}
						
						// primary�겓�궧�궚�꺀�긿�긽�꺃�꺖�꺍�걣�걗�굦�겙�깕�꺖�깂�걣�걗�굥�꺃�꺖�꺍�굮�뀓營�
						if (primary.contains(sckey.get(scratchIndex)) && !note.isEmpty()) {
							random[sckey.get(scratchIndex)] = note.get(0);
							primary.remove(sckey.get(scratchIndex));
							note.remove(0);
							// �궧�궚�꺀�긿�긽�꺃�꺖�꺍�굮�젂濚겹굤�겓
							scratchIndex = ++scratchIndex == sckey.size() ? 0 : scratchIndex;
						}

						// �깕�꺖�깂�걣�걗�굥�꺃�꺖�꺍�굮潁��ｃ걣�쇇�뵟�걮�겒�걚�꺃�꺖�꺍�겓�꺀�꺍���깲�겓�뀓營�
						while (!(note.isEmpty() || primary.isEmpty())) 
							makeOtherLaneRandom(random, note, primary, -1);

						// noteLane�걣令뷩겎�겒�걢�겂�걼�굢
						// lastNoteTime�걣弱뤵걬�걚�꺃�꺖�꺍�걢�굢�젂�빁�겓營��걚�겍�걚�걦
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
						// 餘뗣굤�굮�꺀�꺍���깲�겓
						while (!other.isEmpty()) {
							int r = (int) (Math.random() * primary.size());
							random[primary.get(r)] = other.get(0);
							primary.remove(r);
							other.remove(0);
						}
						


					} else if (mode.player == 2) {
						if (mode == Mode.KEYBOARD_24K_DOUBLE) {
							// TODO 24k-DP�겓野얍퓶
							break;
						}
						// ���깣�꺂�깤�꺃�꺖�셽
						// �궧�궚�꺀�긿�긽�겢�겗�뜷�썶�겓�꽛�뀍�쉪�겓�궋�궢�궎�꺍�걬�굦�굥�굠�걝�겓�걲�굥
						// �ｆ돀�겘�눣�씎�겒�걚�굠�걝�겓 sc:40ms key:�궠�꺍�깢�궍�궛�걢�굢沃��겳�눣�걮
						keys = getKeys(mode, true);
						int keyInterval = hranThreshold;
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

						// scLane�굮�뀍�젺�겓
						if (!isRightSide) {
							assign.remove((Integer) scLane);
							assign.add(0, scLane);
						}

						// LN�걣�궋�궚�깇�궍�깣�겒�꺃�꺖�꺍�굮�궋�궢�궎�꺍�걮�겍�걢�굢�솮鸚�
						for (int lane = 0; lane < keys.length; lane++) {
							if (ln[keys[lane]] != -1) {
								random[keys[lane]] = ln[keys[lane]];
								assign.remove((Integer) keys[lane]);
								original.remove((Integer) ln[keys[lane]]);
							}
						}

						// �뀇�겗�꺃�꺖�꺍�굮�깕�꺖�깂�겗耶섇쑉�겎�늽窈�
						while (!original.isEmpty()) {
							if (notes[original.get(0)] != null) {
								note.add(original.get(0));
							} else {
								other.add(original.get(0));
							}
							original.remove(0);
						}

						// �쑋�궋�궢�궎�꺍�꺃�꺖�꺍�굮潁��ｇ쇇�뵟�걢�겑�걝�걢�겎�늽窈�
						while (!assign.isEmpty()) {
							if (tl.getTime() - lastNoteTime[assign.get(0)] < (assign.get(0) == scLane ? scratchInterval
									: keyInterval)) {
								tate.add(assign.get(0));
							} else {
								primary.add(assign.get(0));
							}
							assign.remove(0);
						}

						// �깕�꺖�깂�걣�걗�굥�꺃�꺖�꺍�굮潁��ｃ걣�쇇�뵟�걮�겒�걚�꺃�꺖�꺍�겓�뀓營�
						while (!(note.isEmpty() || primary.isEmpty())) {
							random[primary.get(0)] = note.get(0);
							primary.remove(0);
							note.remove(0);
						}

						// noteLane�걣令뷩겎�겒�걢�겂�걼�굢
						// lastNoteTime�걣弱뤵걬�걚�꺃�꺖�꺍�걢�굢�젂�빁�겓營��걚�겍�걚�걦
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
						// 餘뗣굤�굮營��걚�겍�걚�걦
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
							notes, lastNoteTime, tl.getTime(), hranThreshold)
							: keys;
					break;
				case S_RANDOM_EX:
					keys = getKeys(mode, true);
					if(mode == Mode.POPN_9K) {
						random = keys.length > 0 ? noMurioshiShuffle(keys, ln,
								notes, lastNoteTime, tl.getTime(), hranThreshold)
								: keys;
					} else {
						random = keys.length > 0 ? timeBasedShuffle(keys, ln,
								notes, lastNoteTime, tl.getTime(), 40)
								: keys;
					}
					break;
				case SEVEN_TO_NINE:
					keys = getKeys(mode, true);
					random = keys.length > 0 ? sevenToNine(keys, ln,
							notes, lastNoteTime, tl.getTime(), hranThreshold)
							: keys;
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


	// �쎍�뎺�깕�꺖�깂�겏�겗�셽�뼋�껅뀸shuffle duration[ms]�셽�뼋�쑋繹��겗潁��ｆ돀�걣�눣�씎�굥�걽�걨�쇇�뵟�걮�겒�걚�굠�걝�겓shuffle�굮�걢�걨�굥
	private static int[] timeBasedShuffle(int[] keys, int[] activeln,
			Note[] notes, int[] lastNoteTime, int now, int duration) {
		List<Integer> assignLane = new ArrayList<Integer>(keys.length);
		List<Integer> originalLane = new ArrayList<Integer>(keys.length);
		int max = 0;
		int[] result = new int[max + 1];
		
		initLanes(keys, assignLane, originalLane, max, result);

		removeActivedLane(keys, activeln, assignLane, originalLane, null, result);
		
		List<Integer> noteLane, otherLane;
		noteLane = new ArrayList<Integer>(keys.length);
		otherLane = new ArrayList<Integer>(keys.length);

		// �뀇�겗�꺃�꺖�꺍�굮�깕�꺖�깂�겗耶섇쑉�겎�늽窈�
		while (!originalLane.isEmpty()) {
			if (notes[originalLane.get(0)] != null) {
				noteLane.add(originalLane.get(0));
			} else {
				otherLane.add(originalLane.get(0));
			}
			originalLane.remove(0);
		}

		// �쑋�궋�궢�궎�꺍�꺃�꺖�꺍�굮潁��ｇ쇇�뵟�걢�겑�걝�걢�겎�늽窈�
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

		// �깕�꺖�깂�걣�걗�굥�꺃�꺖�꺍�굮潁��ｃ걣�쇇�뵟�걮�겒�걚�꺃�꺖�꺍�겓�뀓營�
		while (!(noteLane.isEmpty() || primaryLane.isEmpty()))
			makeOtherLaneRandom(result, noteLane, primaryLane, -1);

		// noteLane�걣令뷩겎�겒�걢�겂�걼�굢
		// lastNoteTime�걣弱뤵걬�걚�꺃�꺖�꺍�걢�굢�젂�빁�겓營��걚�겍�걚�걦
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
			makeOtherLaneRandom(result, noteLane, minLane, -1);
		}

		primaryLane.addAll(rendaLane);
		laneRemover(primaryLane, result, otherLane);

		return result;
	}

	// �꽒�릤�듉�걮�겏duration[ms]�셽�뼋�쑋繹��겗潁��ｆ돀�걣�겒�굥�겧�걦�씎�겒�걚�굠�걝�겓shuffle�굮�걢�걨�굥
	private static int[] noMurioshiShuffle(int[] keys, int[] activeln,
		Note[] notes, int[] lastNoteTime, int now, int duration) {
		List<Integer> assignedLane = new ArrayList<Integer>(keys.length);
		List<Integer> noAssignedLane = new ArrayList<Integer>(keys.length);
		List<Integer> originalLane = new ArrayList<Integer>(keys.length);

		int max = 0;
		int[] result = new int[max + 1];
		
		initLanes(keys, noAssignedLane, originalLane, max, result);

		// LN�걣�궋�궚�깇�궍�깣�겒�꺃�꺖�꺍�굮�궋�궢�궎�꺍�걮�겍�걢�굢�솮鸚�
		removeActivedLane(keys, activeln, assignedLane, originalLane, noAssignedLane, result);
		
		List<Integer> noteLane, otherLane;
		noteLane = new ArrayList<Integer>(keys.length);
		otherLane = new ArrayList<Integer>(keys.length);

		checkOriginalLane(notes, originalLane, noteLane, otherLane);

		//�꽒�릤�듉�걮�겓�겒�굢�겒�걚�굠�걝�겓�꺀�꺍���깲�겓營��걚�겍�걚�걦
		//7�뗦듉�걮餓δ툓�겎�겘�꽒�릤�듉�걮�걮�걢耶섇쑉�걮�겒�걚�겗�겎�솮鸚�
		if(assignedLane.size() + noteLane.size() <= 6) {
			preventMoreThanSevenKeys(keys, lastNoteTime, now, duration, assignedLane, noAssignedLane, max, result,
					noteLane);
		}

		laneRemover(noAssignedLane, result, noteLane);

		// 餘뗣굤�굮�꺀�꺍���깲�겓營��걚�겍�걚�걦
		laneRemover(noAssignedLane, result, otherLane);

		return result;
	}

	// duration2[ms]�셽�뼋�쑋繹��겗潁��ｆ돀�굮�눣�씎�굥�걽�걨�겳�걨�겇�겇duration1[ms]�셽�뼋�쑋繹��겗潁��ｆ돀�걣�눣�씎�굥�걽�걨�빓�걦�쇇�뵟�걲�굥�굠�걝�겓shuffle�굮�걢�걨�굥
	private static int[] rendaShuffle(int[] keys, int[] activeln,
			Note[] notes, int[] lastNoteTime, int now, int duration1, int duration2) {
		List<Integer> assignLane = new ArrayList<Integer>(keys.length);
		List<Integer> originalLane = new ArrayList<Integer>(keys.length);
		int max = 0;
		int[] result = new int[max + 1];
		
		initLanes(keys, assignLane, originalLane, max, result);
		removeActivedLane(keys, activeln, assignLane, originalLane, null, result);
		
		List<Integer> noteLane, otherLane;
		noteLane = new ArrayList<Integer>(keys.length);
		otherLane = new ArrayList<Integer>(keys.length);

		checkOriginalLane(notes, originalLane, noteLane, otherLane);

		// �쑋�궋�궢�궎�꺍�꺃�꺖�꺍�굮潁��ｆ돀�쇇�뵟�걢�겑�걝�걢�겎�늽窈�
		List<Integer> rendaLane,mainRendaLane, noRendaLane;
		rendaLane = new ArrayList<Integer>(keys.length);
		mainRendaLane = new ArrayList<Integer>(keys.length);
		noRendaLane = new ArrayList<Integer>(keys.length);
		while (!assignLane.isEmpty()) {
			if (now - lastNoteTime[assignLane.get(0)] < duration2) {
				rendaLane.add(assignLane.get(0));
			} else if(now - lastNoteTime[assignLane.get(0)] < duration1) {
				mainRendaLane.add(assignLane.get(0));
			} else {
				noRendaLane.add(assignLane.get(0));
			}
			assignLane.remove(0);
		}

		// �깕�꺖�깂�걣�걗�굥�꺃�꺖�꺍�굮潁��ｆ돀�걣�쇇�뵟�걲�굥�꺃�꺖�꺍�겓�빓�걚�젂�겓�뀓營�
		while (!(noteLane.isEmpty() || mainRendaLane.isEmpty())) {
			int maxRenda = Integer.MIN_VALUE;
		
			for (int i = 0; i < mainRendaLane.size(); i++) 
				if (maxRenda < laneRendaCount[mainRendaLane.get(i)])
					maxRenda = laneRendaCount[mainRendaLane.get(i)];

			ArrayList<Integer> maxLane = new ArrayList<Integer>(mainRendaLane.size());
			for (int i = 0; i < mainRendaLane.size(); i++) 
				if (maxRenda == laneRendaCount[mainRendaLane.get(i)]) 
					maxLane.add(mainRendaLane.get(i));

			makeOtherLaneRandom(result, noteLane, mainRendaLane, 1);
		}

		// noteLane�걣令뷩겎�겒�걢�겂�걼�굢餘뗣굤�겗�깕�꺖�깉�굮潁��ｆ돀�겓�겒�굢�겒�걚�꺃�꺖�꺍�걢�굢�꺀�꺍���깲�겓營��걚�겍�걚�걦
		while (!(noteLane.isEmpty() || noRendaLane.isEmpty()))
			makeOtherLaneRandom(result, noteLane, noRendaLane, 0);

		// noteLane�걣令뷩겎�겒�걢�겂�걼�굢餘뗣굤�겗�깕�꺖�깉�굮�꺀�꺍���깲�겓營��걚�겍�걚�걦
		while (!(noteLane.isEmpty() || rendaLane.isEmpty()))
			makeOtherLaneRandom(result, noteLane, rendaLane, 1);

		// 餘뗣굤�굮�꺀�꺍���깲�겓營��걚�겍�걚�걦
		noRendaLane.addAll(rendaLane);
		noRendaLane.addAll(mainRendaLane);
		
		while (!otherLane.isEmpty()) {
			int r = (int) (Math.random() * noRendaLane.size());
			result[noRendaLane.get(r)] = otherLane.get(0);
			if(rendaLane.indexOf(noRendaLane.get(r)) == -1)
				laneRendaCount[noRendaLane.get(r)] = 0;
			noRendaLane.remove(r);
			otherLane.remove(0);
		}

		return result;
	}


	//7to9
	private static int[] sevenToNine(int[] keys, int[] activeln, Note[] notes, int[] lastNoteTime, int now, int duration) {
		/**
		 * 7to9 �궧�궚�꺀�긿�긽�뜷�썶鵝띸쉰�뼟岳� 0:OFF 1:SC1KEY2~8 2:SC1KEY3~9 3:SC2KEY3~9 4:SC8KEY1~7 5:SC9KEY1~7 6:SC9KEY2~8
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
			 * 7to9�궧�궚�꺀�긿�긽�눇�릤�궭�궎�깤 0:�걹�겗�겲�겲 1:�ｆ돀�썮�겳 2:雅ㅴ틨
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

	private static void laneRemover(List<Integer> noAssignedLane, int[] result, List<Integer> Lane) {
		while (!Lane.isEmpty()) {
			int r = (int) (Math.random() * noAssignedLane.size());
			result[noAssignedLane.get(r)] = Lane.get(0);
			noAssignedLane.remove(r);
			Lane.remove(0);
		}
	}

	private static void initLanes(int[] keys, List<Integer> laneFirst, List<Integer> laneSecond, int max,
			int[] result) {
		for (int key : keys) {
			laneFirst.add(key);
			laneSecond.add(key);
		}
		for (int key : keys) 
			max = Math.max(max, key);
		for (int i = 0; i < result.length; i++) 
			result[i] = i;
	}
	
	private void calculateHranThreshold() {
		if(config.getHranThresholdBPM() <= 0)
			hranThreshold = 0;
		else
			hranThreshold = (int) (Math.ceil(15000.0f / config.getHranThresholdBPM()));
	}
	
	private static void removeActivedLane(int[] keys, int[] activeln, List<Integer> assignLane,
			List<Integer> originalLane, List<Integer> noAssignedLane, int[] result) {
		for (int lane = 0; lane < keys.length; lane++) {
			if (activeln != null && activeln[keys[lane]] != -1) {
				result[keys[lane]] = activeln[keys[lane]];
				assignLane.remove((Integer) keys[lane]);
				originalLane.remove((Integer) activeln[keys[lane]]);
				if(noAssignedLane != null)
					noAssignedLane.remove((Integer) keys[lane]);
			}
		}
	}

	private static void makeOtherLaneRandom(int[] result, List<Integer> noteLane, List<Integer> toRandomLane, int lineCountBias) {
		int r = (int) (Math.random() * toRandomLane.size());
		result[toRandomLane.get(r)] = noteLane.get(0);
		if(lineCountBias == 0)
			laneRendaCount[toRandomLane.get(r)] = 0;
		else if(lineCountBias == 1)
			laneRendaCount[toRandomLane.get(r)]++;
		toRandomLane.remove(r);
		noteLane.remove(0);

	}

	private static void checkOriginalLane(Note[] notes, List<Integer> originalLane, List<Integer> noteLane,
			List<Integer> otherLane) {
		while (!originalLane.isEmpty()) {
			if (notes[originalLane.get(0)] != null && (notes[originalLane.get(0)] instanceof NormalNote || notes[originalLane.get(0)] instanceof LongNote)) {
				noteLane.add(originalLane.get(0));
			} else {
				otherLane.add(originalLane.get(0));
			}
			originalLane.remove(0);
		}
	}

	private static void preventMoreThanSevenKeys(int[] keys, int[] lastNoteTime, int now, int duration,
			List<Integer> assignedLane, List<Integer> noAssignedLane, int max, int[] result, List<Integer> noteLane) {
		List<Integer> kouhoLane = new ArrayList<Integer>(keys.length); //營��걨�굥�숃짒
		List<Integer> rendaLane = new ArrayList<Integer>(keys.length); //營��걦�겏潁��ｆ돀�겓�겒�굥�꺃�꺖�꺍
		while (!(noteLane.isEmpty() || noAssignedLane.isEmpty())) {
			kouhoLane.clear();
			rendaLane.clear();
			if(assignedLane.size() <= 1) {
				kouhoLane.addAll(noAssignedLane); //�뿢�겓�깕�꺖�깉�걣營��걢�굦�겍�걚�굥�꺃�꺖�꺍�걣1�뗤빳訝뗣겎�걗�굦�겙�뀲�깿�걣�숃짒
			} else {
				int[] referencePoint = new int[2]; //�뿢�겓�깕�꺖�깉�걣營��걢�굦�겍�걚�굥�꺃�꺖�꺍�겗訝��겎藥�塋��겗�꺃�꺖�꺍�겏�뤂塋��겗�꺃�꺖�꺍
				referencePoint[0] = max;
				referencePoint[1] = 0;
				for(int i = 0; i < assignedLane.size(); i++){
					referencePoint[0] = Math.min(referencePoint[0] , assignedLane.get(i));
					referencePoint[1] = Math.max(referencePoint[1] , assignedLane.get(i));
				}
				if(referencePoint[1] - referencePoint[0] <= 2) {
					kouhoLane.addAll(noAssignedLane); //�뿢�겓�깕�꺖�깉�걣營��걢�굦�겍�걚�굥�꺃�꺖�꺍�걣�뎴�뎸�겎�듉�걵�굥影꾢쎊�겎�걗�굦�겙�뀲�깿�걣�숃짒
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
			if(kouhoLane.size() > rendaLane.size())
				kouhoLane.removeAll(rendaLane); //潁��ｆ돀�겓�겒�굥�꺃�꺖�꺍�굮�솮鸚뽧�귙걼�걽�걮�숃짒�뀲�깿�걣潁��ｆ돀�겓�겒�굥�졃�릦�꽒�릤�듉�걮�겎�겒�걚�걪�겏�겗�뼶�굮�꽛�뀍
			if(kouhoLane.isEmpty())
				break;
			
			int r = (int) (Math.random() * kouhoLane.size());
			result[kouhoLane.get(r)] = noteLane.get(0);
			assignedLane.add(kouhoLane.get(r));
			noAssignedLane.remove(kouhoLane.get(r));
			noteLane.remove(0);
		}
	}

}
