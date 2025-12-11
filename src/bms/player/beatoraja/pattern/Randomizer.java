package bms.player.beatoraja.pattern;

import java.util.*;
import java.util.stream.Collectors;

import bms.model.*;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.pattern.PatternModifier.AssistLevel;

/**
 * TimeLine毎にレーンを置換するクラス
 * 
 * @author KEH
 */
public abstract class Randomizer {

	protected Mode mode;

	protected static final int SRAN_THRESHOLD = 40;

	protected static final int DEFAULT_HRAN_THRESHOLD = 100;

	/**
	 * 変更対象レーン
	 */
	protected int[] modifyLanes;
	
	protected java.util.Random random = new java.util.Random((long) (Math.random() * 65536 * 65536 * 65536));

	/**
	 * LNが配置されているレーン
	 * <移動後, 元>
	 */
	private Map<Integer, Integer> LNactive = new HashMap<>();

	/**
	 * 変更可能な移動元レーン
	 */
	private List<Integer> changeableLane = new ArrayList<>();

	/**
	 * 変更可能な移動先レーン
	 */
	private List<Integer> assignableLane = new ArrayList<>();

	/**
	 * changeableLaneからassignableLaneへの対応を返す。
	 * キー:changeableLane, 値:assignableLane とすること。
	 * [1->3]なら3バスになるということ。
	 * Listは変更してもよい。TimeLineは変更してはならない。
	 */
	abstract Map<Integer, Integer> randomize(TimeLine tl, List<Integer> changeableLane, List<Integer> assignableLane);
	/**
	 * 譜面変更のアシストレベル
	 */
	private AssistLevel assist = AssistLevel.NONE;

	/**
	 * TimeLineのノーツをrandomizeで定義された方法で入れ替える
	 */
	public int[] permutate(TimeLine tl) {
		Map<Integer, Integer> permutationMap = randomize(tl, new ArrayList<>(changeableLane),
				new ArrayList<>(assignableLane));

		// LNアクティブなレーンのアサイン
		permutationMap.putAll(LNactive);

		// permutationMapに従ってtlのノーツを再配置
		int[] permutation = new int[mode.key];
		for (int i = 0; i < permutation.length; i++) {
			permutation[i] = i;
		}
		Note[] notes = new Note[mode.key];
		Note[] hnotes = new Note[mode.key];
		for (int i = 0; i < modifyLanes.length; i++) {
			notes[modifyLanes[i]] = tl.getNote(modifyLanes[i]);
			hnotes[modifyLanes[i]] = tl.getHiddenNote(modifyLanes[i]);
		}
		for (Map.Entry<Integer, Integer> e : permutationMap.entrySet()) {
			int x = e.getKey();
			int y = e.getValue();
			Note n = notes[x];
			Note hn = hnotes[x];
			if (n instanceof LongNote) {
				LongNote ln2 = (LongNote) n;
				if (ln2.isEnd() && LNactive.containsKey(x) && tl.getTime() == ln2.getTime()) {
					LNactive.remove(x);
					changeableLane.add(x);
					assignableLane.add(y);
				} else {
					if (!ln2.isEnd()) {
						LNactive.put(x, y);
						changeableLane.remove((Integer) x);
						assignableLane.remove((Integer) y);
					}
				}
			}
			tl.setNote(y, n);
			tl.setHiddenNote(y, hn);

			permutation[y] = x;
		}
		return permutation;
	}

	public void setModifyLanes(int[] lanes) {
		for (int lane : lanes) {
			changeableLane.add(lane);
			assignableLane.add(lane);
		}
		this.modifyLanes = lanes;
	}

	protected void setMode(Mode m) {
		this.mode = m;
	}

	protected Collection<Integer> getLNLane() {
		return LNactive.values();
	}

	public AssistLevel getAssistLevel() {
		return assist;
	}

	protected void setAssistLevel(AssistLevel assist) {
		this.assist = (assist != null ? assist : AssistLevel.NONE);
	}

	public void setRandomSeed(long seed) {
		if(seed >= 0) {
			random.setSeed(seed);
		}
	}

	/**
	 * 対応するランダマイザ生成
	 */
	public static Randomizer create(Random r, Mode mode, PlayerConfig config) {
		return create(r, 0, mode, config);
	}

	public static Randomizer create(Random r, int playSide, Mode mode, PlayerConfig config) {
		Randomizer randomizer = null;
		int thresholdBPM = config.getHranThresholdBPM();
		int thresholdMillis;
		if (thresholdBPM > 0) {
			thresholdMillis = (int) (Math.ceil(15000.0f / thresholdBPM));
		} else if (thresholdBPM == 0) {
			thresholdMillis = 0;
		} else {
			thresholdMillis = DEFAULT_HRAN_THRESHOLD;
		}

		randomizer = switch (r) {
			case ALL_SCR -> new AllScratchRandomizer(SRAN_THRESHOLD, thresholdMillis, playSide);
			case CONVERGE -> new ConvergeRandomizer(thresholdMillis, thresholdMillis * 2);
			case H_RANDOM -> new SRandomizer(thresholdMillis, AssistLevel.LIGHT_ASSIST);
			case SPIRAL -> new SpiralRandomizer();
			case S_RANDOM -> new SRandomizer(SRAN_THRESHOLD, AssistLevel.NONE);
			case S_RANDOM_NO_THRESHOLD -> new SRandomizer(0, AssistLevel.NONE);
			case S_RANDOM_EX -> new SRandomizer(SRAN_THRESHOLD, AssistLevel.LIGHT_ASSIST);
			case S_RANDOM_PLAYABLE -> new NoMurioshiRandomizer(thresholdMillis);
			default -> throw new IllegalArgumentException("Unexpected value: " + r);
		};
		randomizer.setMode(mode);
		return randomizer;
	}

}

/**
 * 時間に依存するランダムに必要な機能を実装する抽象クラス
 * 
 * @author KEH
 */
abstract class TimeBasedRandomizer extends Randomizer {

	protected int threshold;
	protected Map<Integer, Integer> lastNoteTime;

	public TimeBasedRandomizer(int threshold) {
		this.threshold = threshold;
		lastNoteTime = new HashMap<>();
	}

	@Override
	public void setModifyLanes(int[] lanes) {
		super.setModifyLanes(lanes);
		for (int lane : lanes) {
			lastNoteTime.put(lane, -10000);
		}
	}

	/**
	 * threshold[ms]以下の連打ができるだけ発生しないようにレーンを入れ替える
	 */
	protected Map<Integer, Integer> timeBasedShuffle(TimeLine tl, List<Integer> changeableLane,
			List<Integer> assignableLane) {
		Map<Integer, Integer> randomMap = new HashMap<>();
		List<Integer> noteLane, emptyLane, primaryLane, inferiorLane;
		noteLane = new ArrayList<>();
		emptyLane = new ArrayList<>();
		primaryLane = new ArrayList<>();
		inferiorLane = new ArrayList<>();
		for (Integer cl : changeableLane) {
			if (tl.getNote(cl) == null || tl.getNote(cl) instanceof MineNote) {
				emptyLane.add(cl);
			} else {
				noteLane.add(cl);
			}
		}
		for (Integer al : assignableLane) {
			if (tl.getTime() - lastNoteTime.get(al) > threshold) {
				primaryLane.add(al);
			} else {
				inferiorLane.add(al);
			}
		}

		// ノーツがあるレーンを縦連が発生しないレーンに配置
		while (!(noteLane.isEmpty() || primaryLane.isEmpty())) {
			int r = selectLane(primaryLane);
			randomMap.put(noteLane.remove(0), primaryLane.remove(r));
		}
		// nが空でなかったら
		// lastNoteTimeが小さいレーンから順番に置いていく
		while (!noteLane.isEmpty()) {
			int min = inferiorLane.stream()
					.mapToInt(lastNoteTime::get)
					.min()
					.getAsInt();
			List<Integer> minLane = inferiorLane.stream()
					.filter(l -> {return lastNoteTime.get(l) == min;})
					.collect(Collectors.toList());
			Integer m = minLane.get(random.nextInt(minLane.size()));
			randomMap.put(noteLane.remove(0), m);
			inferiorLane.remove(m);
		}

		// 残りをランダムに置いていく
		primaryLane.addAll(inferiorLane);
		while (!emptyLane.isEmpty()) {
			int r = random.nextInt(primaryLane.size());
			randomMap.put(emptyLane.remove(0), primaryLane.remove(r));
		}

		return randomMap;
	}

	protected void updateNoteTime(TimeLine tl, Map<Integer, Integer> randomMap) {
		for (Map.Entry<Integer, Integer> el : randomMap.entrySet()) {
			if (tl.getNote(el.getKey()) != null && !(tl.getNote(el.getKey()) instanceof MineNote)) {
				lastNoteTime.put(el.getValue(), tl.getTime());
			}
		}
	}

	/**
	 *  ノーツがあるレーンの配置方法を定義
	 * @param lane レーン候補
	 * @return laneのindex(候補から選ぶ)
	 */
	abstract int selectLane(List<Integer> lane);
}

/**
 * S-RANDOM、H-RANDOMランダマイザ
 * 
 * @author KEH
 */
class SRandomizer extends TimeBasedRandomizer {

	public SRandomizer(int threshold, AssistLevel assist) {
		super(threshold);
		setAssistLevel(assist);
	}

	@Override
	Map<Integer, Integer> randomize(TimeLine tl, List<Integer> changeableLane, List<Integer> assignableLane) {
		Map<Integer, Integer> randomMap = timeBasedShuffle(tl, changeableLane, assignableLane);

		updateNoteTime(tl, randomMap);

		return randomMap;
	}

	@Override
	int selectLane(List<Integer> lane) {
		return random.nextInt(lane.size());
	}
}

/**
 * SPIRALランダマイザ
 * 
 * @author KEH
 */
class SpiralRandomizer extends Randomizer {

	private int increment;
	private int head;
	private int cycle;

	public SpiralRandomizer() {
		setAssistLevel(AssistLevel.LIGHT_ASSIST);
	}
	@Override
	public void setModifyLanes(int[] lanes) {
		super.setModifyLanes(lanes);
		this.increment = random.nextInt(lanes.length - 1) + 1;
		this.head = 0;
		this.cycle = lanes.length;
	}

	// modifiLaneから直接Mapを生成する
	// LNアクティブの時はheadを変化させない
	@Override
	Map<Integer, Integer> randomize(TimeLine tl, List<Integer> changeableLane, List<Integer> assignableLane) {
		Map<Integer, Integer> rotateMap = new HashMap<>();
		if (changeableLane.size() == cycle) {
			head = (head + increment) % cycle;
			for (int i = 0; i < modifyLanes.length; i++) {
				rotateMap.put(modifyLanes[i], modifyLanes[(i + head) % cycle]);
			}

		} else {
			for (int i = 0; i < modifyLanes.length; i++) {
				if (changeableLane.contains(modifyLanes[i])) {
					rotateMap.put(modifyLanes[i], modifyLanes[(i + head) % cycle]);
				}
			}
		}
		return rotateMap;
	}

}

/**
 * ALL-SCRランダマイザ
 * 
 * @author KEH
 */
class AllScratchRandomizer extends TimeBasedRandomizer {

	private int scratchThreshold;
	private int[] scratchLane;
	private int scratchIndex;
	private int modifySide;
	private boolean isDoublePlay;
	private static final int SIDE_1P = 0, SIDE_2P = 1;

	public AllScratchRandomizer(int s, int k, int modifySide) {
		super(k);
		this.scratchThreshold = s;
		this.scratchIndex = 0;
		this.modifySide = modifySide; // 1P側は0,2P側は1
		setAssistLevel(AssistLevel.LIGHT_ASSIST);
	}

	// scratchLaneの決定
	@Override
	protected void setMode(Mode m) {
		super.setMode(m);
		this.isDoublePlay = m.player == 2;
		if (isDoublePlay) {
			int[] tempLane = new int[m.scratchKey.length / 2];
			for (int i = 0; i < tempLane.length; i++) {
				tempLane[i] = m.scratchKey[(modifySide * m.scratchKey.length / 2) + i];
			}
			this.scratchLane = tempLane;
		} else {
			this.scratchLane = m.scratchKey;
		}
	}

	@Override
	Map<Integer, Integer> randomize(TimeLine tl, List<Integer> changeableLane, List<Integer> assignableLane) {
		Map<Integer, Integer> randomMap = new HashMap<>();

		// スクラッチレーンにアサインできれば先にアサインする
		if (assignableLane.contains(scratchLane[scratchIndex])
				&& tl.getTime() - lastNoteTime.get(scratchLane[scratchIndex]) > scratchThreshold) {

			Integer l = -1;
			for (Integer cl : changeableLane) {
				if (tl.getNote(cl) != null && !(tl.getNote(cl) instanceof MineNote)) {
					l = cl;
					break;
				}
			}
			if (l != -1) {
				randomMap.put(l, scratchLane[scratchIndex]);
				changeableLane.remove(l);
				assignableLane.remove((Integer) scratchLane[scratchIndex]);
				scratchIndex = ++scratchIndex == scratchLane.length ? 0 : scratchIndex;
			}
		}

		// 残りをアサインする
		randomMap.putAll(timeBasedShuffle(tl, changeableLane, assignableLane));

		updateNoteTime(tl, randomMap);

		return randomMap;
	}

	// DPならスクラッチレーン寄りに、SPならランダムに
	@Override
	int selectLane(List<Integer> lane) {
		if (isDoublePlay) {
			int index = -1;
			switch (modifySide) {
				case SIDE_1P -> {
					int min = Integer.MAX_VALUE;
					for (int i = 0; i < lane.size(); i++) {
						if (lane.get(i) < min) {
							min = lane.get(i);
							index = i;
						}
					}
				}
				case SIDE_2P -> {
					int max = Integer.MIN_VALUE;
					for (int i = 0; i < lane.size(); i++) {
						if (lane.get(i) > max) {
							max = lane.get(i);
							index = i;
						}
					}					
				}
			}
			return index;
		}
		return random.nextInt(lane.size());
	}
}

/**
 * PMSでの無理押し防止S-RANDOMランダマイザ
 * 
 * @author KEH
 */
class NoMurioshiRandomizer extends TimeBasedRandomizer {

	static final List<List<Integer>> buttonCombinationTable;
	private List<Integer> buttonCombination;
	private boolean flag;

	// 無理押しが存在しない6個押しは10パターン
	static {
		buttonCombinationTable = new ArrayList<>();
		buttonCombinationTable.add(Arrays.asList(0, 1, 2, 3, 4, 5));
		buttonCombinationTable.add(Arrays.asList(0, 1, 2, 4, 5, 6));
		buttonCombinationTable.add(Arrays.asList(0, 1, 2, 5, 6, 7));
		buttonCombinationTable.add(Arrays.asList(0, 1, 2, 6, 7, 8));
		buttonCombinationTable.add(Arrays.asList(1, 2, 3, 4, 5, 6));
		buttonCombinationTable.add(Arrays.asList(1, 2, 3, 5, 6, 7));
		buttonCombinationTable.add(Arrays.asList(1, 2, 3, 6, 7, 8));
		buttonCombinationTable.add(Arrays.asList(2, 3, 4, 5, 6, 7));
		buttonCombinationTable.add(Arrays.asList(2, 3, 4, 6, 7, 8));
		buttonCombinationTable.add(Arrays.asList(3, 4, 5, 6, 7, 8));
	}

	public NoMurioshiRandomizer(int threshold) {
		super(threshold);
		setAssistLevel(AssistLevel.LIGHT_ASSIST);
	}

	@Override
	Map<Integer, Integer> randomize(TimeLine tl, List<Integer> changeableLane, List<Integer> assignableLane) {
		int noteCount = noteCount(tl);
		Map<Integer, Integer> randomMap;
		// タイムラインのノーツ(LNアクティブを含む)が2個以下or7個以上のとき無理押しを考慮しない
		flag = (2 < noteCount && noteCount < 7);
		if (flag) {
			List<List<Integer>> candidate;
			if (getLNLane().size() == 0) {
				candidate = buttonCombinationTable;
			} else {
				// LNアクティブを含む同時押しパターンをフィルタ
				candidate = buttonCombinationTable.stream()
						.filter(l -> {return l.containsAll(getLNLane());})
						.collect(Collectors.toList());
			}
			if (candidate.size() != 0) {
				// 候補から縦連打になるレーンを除外する
				List<Integer> rendaLane = lastNoteTime.keySet().stream()
						.filter(lane -> {return tl.getTime() - lastNoteTime.get(lane) < threshold;})
						.collect(Collectors.toList());
				List<List<Integer>> candidate2 = candidate.stream()
						.map(lanes -> {
							return lanes.stream()
									.filter(lane -> {return !rendaLane.contains(lane);})
									.collect(Collectors.toList());
							})
						.filter(lanes -> {return lanes.size() >= noteCount;})
						.collect(Collectors.toList());
				if (candidate2.size() != 0) {
					// 候補の長さがTLのノート数以上のものが残れば、それを選ぶ
					buttonCombination = candidate2.get(random.nextInt(candidate2.size()));
				} else {
					// 縦連打が発生しないことより、無理押しが発生しないことを優先する
					randomMap = new HashMap<>();
					buttonCombination = candidate.get(random.nextInt(candidate2.size())).stream()
							.filter(assignableLane::contains).collect(Collectors.toList());
					List<Integer> e = getNoteExistLane(tl).stream()
							.filter(changeableLane::contains).collect(Collectors.toList());
					e.stream().forEach(lane -> {
						int i = random.nextInt(buttonCombination.size());
						randomMap.put(lane, buttonCombination.get(i));
						changeableLane.remove((Integer)lane);
						assignableLane.remove(buttonCombination.remove(i));
					});
					flag = false;
					randomMap.putAll(timeBasedShuffle(tl, changeableLane, assignableLane));
					return randomMap;
				}
			} else {
				// 無理押ししかありえないので通常H乱処理
				flag = false;
			}
		}
		randomMap = timeBasedShuffle(tl, changeableLane, assignableLane);
		updateNoteTime(tl, randomMap);
		return randomMap;
	}

	// 無理押しを考慮する(flag=true)の時、選ばれた6個押しの中から優先的に選ぶ
	@Override
	int selectLane(List<Integer> lane) {
		if (flag) {
			List<Integer> l = lane.stream().filter(buttonCombination::contains).collect(Collectors.toList());
			if (l.size() != 0) {
				return lane.indexOf(l.get(random.nextInt(l.size())));
			}
		}
		return random.nextInt(lane.size());
	}

	// LNアクティブも含めたタイムラインのノート数
	private int noteCount(TimeLine tl) {
		return getNoteExistLane(tl).size() + getLNLane().size();
	}

	// タイムラインにノーツが存在するレーンのリスト
	private List<Integer> getNoteExistLane(TimeLine tl) {
		List<Integer> l = new ArrayList<>();
		for (int i = 0; i < modifyLanes.length; i++) {
			// nullでない、地雷ノートでない
			if (tl.getNote(modifyLanes[i]) != null && !(tl.getNote(modifyLanes[i]) instanceof MineNote)) {
				l.add(modifyLanes[i]);
			}
		}
		return l;
	}

}

/**
 * threshold1以上threshold2以下の間隔の連打ができるだけ長く発生するように配置する
 * 
 * @author KEH
 */
class ConvergeRandomizer extends TimeBasedRandomizer {

	private final int threshold2;
	private Map<Integer, Integer> rendaCount;

	public ConvergeRandomizer(int threshold1, int threshold2) {
		super(threshold1);
		this.threshold2 = threshold2;
		this.rendaCount = new HashMap<>();
		setAssistLevel(AssistLevel.LIGHT_ASSIST);
	}

	@Override
	public void setModifyLanes(int[] lanes) {
		super.setModifyLanes(lanes);
		for (int lane : lanes) {
			rendaCount.put(lane, 0);
		}
	}

	@Override
	Map<Integer, Integer> randomize(TimeLine tl, List<Integer> changeableLane, List<Integer> assignableLane) {
		// 連打とならないレーンは連打カウントをリセットする
		rendaCount.entrySet().stream().forEach(e -> {
			if (tl.getTime() - lastNoteTime.get(e.getKey()) > threshold2) {
				e.setValue(0);
			}
		});
		Map<Integer, Integer> randomMap = timeBasedShuffle(tl, changeableLane, assignableLane);
		updateNoteTime(tl, randomMap);
		return randomMap;
	}

	// できるだけ連打が長いレーンに優先的に配置
	@Override
	int selectLane(List<Integer> lane) {
		int max = lane.stream()
				.mapToInt(rendaCount::get)
				.max()
				.getAsInt();
		List<Integer> gya = lane.stream()
				.filter(l -> {return rendaCount.get(l) == max;})
				.collect(Collectors.toList());
		int l = gya.get(random.nextInt(gya.size()));
		rendaCount.put(l, rendaCount.get(l) + 1);
		return lane.indexOf(l);
	}

}