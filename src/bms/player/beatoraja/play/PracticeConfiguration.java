package bms.player.beatoraja.play;

import java.io.*;
import java.nio.file.*;
import java.util.function.*;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.model.TimeLine;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyBoardInputProcesseor.ControlKeys;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.SerializationException;

/**
 * プラクティスモードの設定表示/編集用クラス
 *
 * @author exch
 */
public final class PracticeConfiguration {


	private int cursorpos = 0;
	private int itemOffset;
	private int visibleItemCount = 10;

	private BMSModel model;

	private static final String[] GAUGE = { "ASSIST EASY", "EASY", "NORMAL", "HARD", "EX-HARD", "HAZARD", "GRADE",
			"EX GRADE", "EXHARD GRADE"};
	private static final String[] RANDOM = { "NORMAL", "MIRROR", "RANDOM", "R-RANDOM", "S-RANDOM", "SPIRAL", "H-RANDOM",
			"ALL-SCR", "RANDOM-EX", "S-RANDOM-EX" };
	private static final String[] DPRANDOM = { "NORMAL", "FLIP" };
	private static final String[] GRAPHTYPESTR = {"NOTETYPE", "JUDGE", "EARLYLATE"};

	private PracticeProperty property = new PracticeProperty();

	public PracticeConfiguration() {
		// TODO 描画位置、使用テキスト等をスキン定義できるように
		// TODO スキン定義がない場合のデフォルト配置の定義
	}

	public static final PracticeElement[] elements = PracticeElement.values();

	private PracticeModeControls controls = new PracticeModeControls();

	public void create(BMSModel model, Config config) {
		cursorpos = 0;
		itemOffset = 0;
		controls.initialize(config);
		property.judgerank = model.getJudgerank();
		property.endtime = model.getLastTime() + 1000;
		Path p = Paths.get("practice/" + model.getSHA256() + ".json");
		if (Files.exists(p)) {
			Json json = new Json();
			try {
				property = json.fromJson(PracticeProperty.class, new FileReader(p.toFile()));
			} catch (FileNotFoundException | SerializationException e) {
				e.printStackTrace();
			}
		}

		if(property.gaugecategory == null) {
			property.gaugecategory = BMSPlayerRule.getBMSPlayerRule(model.getMode()).gauge;
		}
		this.model = model;
		if(property.total == 0) {
			property.total = model.getTotal();
		}
	}

	public void saveProperty() {
		try {
			Files.createDirectory(Paths.get("practice"));
		} catch (IOException e1) {
		}
		try (FileWriter fw = new FileWriter("practice/" + model.getSHA256() + ".json")) {
			Json json = new Json();
			fw.write(json.prettyPrint(property));
			fw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public PracticeProperty getPracticeProperty() {
		return property;
	}

	/**
	 * Configures how many Practice settings are exposed through the skin slots.
	 */
	public void setVisibleItemCount(int visibleItemCount) {
		this.visibleItemCount = Math.max(1, Math.min(visibleItemCount, elements.length));
		ensureCursorVisible();
	}

	public String getVisibleItemText(int index) {
		PracticeElement element = getVisibleElement(index);
		return element != null ? element.text.apply(property) : "";
	}

	public String getVisibleItemLabel(int index) {
		return getVisibleItemTextPart(index, true);
	}

	public String getVisibleItemValue(int index) {
		return getVisibleItemTextPart(index, false);
	}

	private String getVisibleItemTextPart(int index, boolean label) {
		String text = getVisibleItemText(index);
		int separator = text.indexOf(" : ");
		if (separator < 0) {
			return label ? text : "";
		}
		return label ? text.substring(0, separator) : text.substring(separator + 3);
	}

	public boolean isVisibleItemAvailable(int index) {
		return getVisibleElement(index) != null;
	}

	public boolean isVisibleItemSelected(int index) {
		return getVisibleElement(index) == elements[cursorpos];
	}

	public void selectVisibleItem(int index) {
		PracticeElement element = getVisibleElement(index);
		if (element != null) {
			cursorpos = element.ordinal();
		}
	}

	public void changeVisibleItem(int index, boolean increment) {
		PracticeElement element = getVisibleElement(index);
		if (element != null) {
			cursorpos = element.ordinal();
			element.action.run(this, increment, false, false);
		}
	}

	public float getItemScrollPosition() {
		int maxOffset = getMaxItemOffset();
		return maxOffset > 0 ? (float) itemOffset / maxOffset : 0;
	}

	public void setItemScrollPosition(float position) {
		itemOffset = Math.round(MathUtils.clamp(position, 0, 1) * getMaxItemOffset());
		PracticeElement firstVisibleElement = getVisibleElement(0);
		if (firstVisibleElement != null) {
			cursorpos = firstVisibleElement.ordinal();
		}
	}

	private PracticeElement getVisibleElement(int index) {
		if (index < 0 || index >= visibleItemCount || model == null) {
			return null;
		}
		int availableIndex = itemOffset + index;
		for (PracticeElement element : elements) {
			if (element.optionAvailable.test(this) && availableIndex-- == 0) {
				return element;
			}
		}
		return null;
	}

	private int getAvailableItemCount() {
		if (model == null) {
			return 0;
		}
		int count = 0;
		for (PracticeElement element : elements) {
			if (element.optionAvailable.test(this)) {
				count++;
			}
		}
		return count;
	}

	private int getMaxItemOffset() {
		return Math.max(0, getAvailableItemCount() - visibleItemCount);
	}

	private void ensureCursorVisible() {
		if (model == null) {
			return;
		}
		int selectedIndex = 0;
		for (PracticeElement element : elements) {
			if (!element.optionAvailable.test(this)) {
				continue;
			}
			if (element.ordinal() == cursorpos) {
				if (selectedIndex < itemOffset) {
					itemOffset = selectedIndex;
				} else if (selectedIndex >= itemOffset + visibleItemCount) {
					itemOffset = selectedIndex - visibleItemCount + 1;
				}
				itemOffset = Math.min(itemOffset, getMaxItemOffset());
				return;
			}
			selectedIndex++;
		}
	}

	public GrooveGauge getGauge(BMSModel model) {
		GrooveGauge gauge = GrooveGauge.create(model, property.gaugetype, property.gaugecategory);
		gauge.setValue(property.startgauge);
		return gauge;
	}
	
	public void processInput(BMSPlayerInputProcessor input) {
		controls.readInput(input, model.getMode());
		boolean turbo = controls.getInputTurbo();

		int yTicks = controls.extractYTicks();
		while (yTicks > 0) {
			do {
			cursorpos = (cursorpos + 1) % elements.length;
			} while(!elements[cursorpos].optionAvailable.test(this));
			--yTicks;
		}
		while (yTicks < 0) {
			do {
				cursorpos = (cursorpos + elements.length - 1) % elements.length;
			} while(!elements[cursorpos].optionAvailable.test(this));
			++yTicks;
		}
		ensureCursorVisible();

		int nonAnalogXTicks = controls.extractNonAnalogXTicks();
		if (nonAnalogXTicks != 0) {
			boolean inc = nonAnalogXTicks > 0;
			nonAnalogXTicks = Math.abs(nonAnalogXTicks);
			for (int i = 0; i < nonAnalogXTicks ; ++i) {
				elements[cursorpos].action.run(this, inc, false, turbo);
			}
		} 
		int analogXTicks = controls.extractAnalogXTicks(elements[cursorpos].useFineAnalogTicks);
		if (analogXTicks != 0) {
			boolean inc = analogXTicks > 0;
			analogXTicks = Math.abs(analogXTicks);
			for (int i = 0; i < analogXTicks ; ++i) {
				elements[cursorpos].action.run(this, inc, true, turbo);
			}
		}
	}

	BMSModel getModel() {
		return model;
	}

	int getCursorPosition() {
		return cursorpos;
	}

	boolean isElementAvailable(int index) {
		return elements[index].optionAvailable.test(this);
	}

	boolean isHorizontalInputMode() {
		return controls.isHorizontalMode();
	}

	boolean isInputTurbo() {
		return controls.getInputTurbo();
	}

	private final class PracticeModeControls {
		private boolean inputUp;
		private boolean inputDown;
		private boolean inputTurbo;
		private boolean inputLeft;
		private boolean inputRight;

		private long nextHoldTick;
		private int nonAnalogYTicks;
		private int nonAnalogXTicks;

		private boolean turntableHorizontalModePrev;

		private int inputXAnalogAmount;
		private int inputYAnalogAmount;

		private int scrollDurationLow;
		private int scrollDurationHigh;
		private int analogTicksPerScroll;

		public void initialize(Config config) {
	        scrollDurationLow = config.getScrollDurationLow();
	        scrollDurationHigh = config.getScrollDurationHigh();
        	analogTicksPerScroll = config.getAnalogTicksPerScroll();
	    }

		public void readInput(BMSPlayerInputProcessor input, Mode mode) {
			final boolean inputUpPrev = inputUp;
			final boolean inputDownPrev = inputDown;
			final boolean inputLeftPrev = inputLeft;
			final boolean inputRightPrev = inputRight;

			if (mode == Mode.POPN_9K) {
				//  1 3 5 7
				// 0 2 4 6 8
				inputUp = input.getControlKeyState(ControlKeys.UP) || input.getKeyState(1) || input.getKeyState(7);
				inputDown = input.getControlKeyState(ControlKeys.DOWN) || input.getKeyState(2) || input.getKeyState(6);
				inputLeft = input.getControlKeyState(ControlKeys.LEFT) || input.getKeyState(3);
				inputRight = input.getControlKeyState(ControlKeys.RIGHT) || input.getKeyState(5);
				inputTurbo = input.getKeyState(4);
				inputXAnalogAmount = 0;
				inputYAnalogAmount = 0;
			} else {
				//  1 3 5
				// 0 2 4 6
				inputUp = input.getControlKeyState(ControlKeys.UP);
				inputDown = input.getControlKeyState(ControlKeys.DOWN);
				inputLeft = input.getControlKeyState(ControlKeys.LEFT);
				inputRight = input.getControlKeyState(ControlKeys.RIGHT);
				inputTurbo = input.getKeyState(3);
				boolean turntableHorizontalMode = input.getKeyState(1) || inputTurbo;

				if (mode == Mode.BEAT_10K) {
					//  1 3    8 10
					// 0 2 4  7 9 11
					inputUp = inputUp || input.getKeyState(8) || input.getKeyState(10);
					inputDown = inputDown || input.getKeyState(9);
					inputLeft = inputLeft || input.getKeyState(7);
					inputRight = inputRight || input.getKeyState(11);
				} else if (mode == Mode.BEAT_14K) {
					//  1 3 5    10 12 14
					// 0 2 4 6  9 11 13 15
					inputUp = inputUp || input.getKeyState(10) || input.getKeyState(14);
					inputDown = inputDown || input.getKeyState(11) || input.getKeyState(13);
					inputLeft = inputLeft || input.getKeyState(9);
					inputRight = inputRight || input.getKeyState(15);
					inputTurbo = inputTurbo || input.getKeyState(12);
				} else if (mode == Mode.KEYBOARD_24K || mode == Mode.KEYBOARD_24K_DOUBLE) {
					//  1 3   6 8 10
					// 0 2 4 5 7 9 11
					inputUp = inputUp || input.getKeyState(6) || input.getKeyState(10);
					inputDown = inputDown || input.getKeyState(7) || input.getKeyState(9);
					inputLeft = inputLeft || input.getKeyState(5);
					inputRight = inputRight || input.getKeyState(11);
					inputTurbo = inputTurbo || input.getKeyState(8);
				}

				int turntableUpIndex1 = -1;
				int turntableDownIndex1 = -1;
				int turntableUpIndex2 = -1;
				int turntableDownIndex2 = -1;
				switch (mode) {
					case BEAT_5K: {
						turntableUpIndex1 = 6;
						turntableDownIndex1 = 5;
						break;
					}
					case BEAT_7K: {
						turntableUpIndex1 = 8;
						turntableDownIndex1 = 7;
						break;
					}
					case BEAT_10K: {
						turntableUpIndex1 = 6;
						turntableDownIndex1 = 5;
						turntableUpIndex2 = 13;
						turntableDownIndex2 = 12;
						break;
					}
					case BEAT_14K: {
						turntableUpIndex1 = 8;
						turntableDownIndex1 = 7;
						turntableUpIndex2 = 17;
						turntableDownIndex2 = 16;
						break;
					}
					case KEYBOARD_24K: {
						turntableUpIndex1 = 25;
						turntableDownIndex1 = 24;
						break;
					}
					case KEYBOARD_24K_DOUBLE: {
						turntableUpIndex1 = 25;
						turntableDownIndex1 = 24;
						turntableUpIndex2 = 51;
						turntableDownIndex2 = 50;
						break;
					}
				}
				final boolean resetAnalogInput = (turntableHorizontalMode != turntableHorizontalModePrev);
				if (resetAnalogInput) {
					inputXAnalogAmount = 0;
					inputYAnalogAmount = 0;
					turntableHorizontalModePrev = turntableHorizontalMode;
				}
				processTurntableInput(input, turntableUpIndex1, true, turntableHorizontalMode, resetAnalogInput);
				processTurntableInput(input, turntableDownIndex1, false, turntableHorizontalMode, resetAnalogInput);
				processTurntableInput(input, turntableUpIndex2, true, turntableHorizontalMode, resetAnalogInput);
				processTurntableInput(input, turntableDownIndex2, false, turntableHorizontalMode, resetAnalogInput);
			}

			if (inputUpPrev != inputUp || inputDownPrev != inputDown || inputLeftPrev != inputLeft || inputRightPrev != inputRight) {
				nextHoldTick = 0;
			}
			if (inputUp || inputDown || inputLeft || inputRight) {
				long currentTimeMillis = System.currentTimeMillis();
				if (nextHoldTick <= currentTimeMillis) {
					if (nextHoldTick == 0) {
						nextHoldTick = currentTimeMillis + scrollDurationLow;
					} else {
						nextHoldTick = currentTimeMillis + scrollDurationHigh;
					}
					if (inputRight) nonAnalogXTicks += 1;
					else if (inputLeft) nonAnalogXTicks -= 1;
					else if (inputDown) nonAnalogYTicks += 1;
					else if (inputUp) nonAnalogYTicks -= 1;
				}
			}
		}

		private void processTurntableInput(BMSPlayerInputProcessor input, int index, boolean scratchUp, boolean turntableHorizontalMode, boolean resetAnalogInput) {
			if (index == -1) return;
			if (input.isAnalogInput(index)) {
				if (resetAnalogInput) {
					input.resetAnalogInput(index);
				}
				int analogDiff = input.getAnalogDiffAndReset(index, 200);
				if (turntableHorizontalMode) {
					inputXAnalogAmount += (scratchUp ? analogDiff : -analogDiff);
				} else {
					inputYAnalogAmount += (scratchUp ? -analogDiff : analogDiff);
				}
			} else {
				if (turntableHorizontalMode) {
					if (scratchUp) {
						inputRight = inputRight || input.getKeyState(index);
					} else {
						inputLeft = inputLeft || input.getKeyState(index);
					}
				} else {
					if (scratchUp) {
						inputUp = inputUp || input.getKeyState(index);
					} else {
						inputDown = inputDown || input.getKeyState(index);
					}
				}
			}
		}

		public int extractAnalogXTicks(boolean useFineAnalogTicks) {
			int ticksPerScroll = useFineAnalogTicks ? 1 : analogTicksPerScroll;
			int actualTicks = controls.inputXAnalogAmount / ticksPerScroll;
			controls.inputXAnalogAmount %= ticksPerScroll; // Note: -15 % 12 = -3
			return actualTicks;
		}

		public int extractNonAnalogXTicks() {
			int actualTicks = nonAnalogXTicks;
			nonAnalogXTicks = 0;
			return actualTicks;
		}

		public int extractYTicks() {
			int actualTicks = controls.inputYAnalogAmount / analogTicksPerScroll;
			controls.inputYAnalogAmount %= analogTicksPerScroll; // Note: -15 % 12 = -3
			actualTicks += nonAnalogYTicks;
			nonAnalogYTicks = 0;
			return actualTicks;
		}

		public boolean getInputTurbo() {
			return inputTurbo;
		}

		public boolean isHorizontalMode() {
			return turntableHorizontalModePrev;
		}
	}

	private static int roundDownTo(int value, int base) {
		return value/base*base;
	}

	@FunctionalInterface
	interface PracticeAction {
		void run(PracticeConfiguration practice, boolean inc, boolean isAnalog, boolean turboSpeed);
	}

	enum PracticeElement {

		STARTTIME((practice, inc, isAnalog, turboSpeed) -> {
			final TimeLine[] tl = practice.model.getAllTimeLines();
			final PracticeProperty property = practice.property;
			final int maxStartTime = roundDownTo(tl[tl.length - 1].getTime() - 2000, 100);
			final int minStartTime = 0;
			int change = turboSpeed ? (isAnalog ? 1000 : 2500) : 100;
			if (inc) {
				property.starttime = Math.min(property.starttime + change, maxStartTime);
				property.endtime = Math.max(property.endtime, property.starttime + 1000);
			} else {
				property.starttime = Math.max(property.starttime - change, minStartTime);
			}
		}, true, property -> String.format("START TIME : %2d:%02d.%1d", property.starttime / 60000,
				(property.starttime / 1000) % 60, (property.starttime / 100) % 10)),
		ENDTIME((practice, inc, isAnalog, turboSpeed) -> {
			final TimeLine[] tl = practice.model.getAllTimeLines();
			final PracticeProperty property = practice.property;
			final int maxEndTime = roundDownTo(tl[tl.length - 1].getTime() + 1000, 100);
			final int minEndTime = roundDownTo(property.starttime + 1000, 100);
			int change = turboSpeed ? (isAnalog ? 1000 : 2500) : 100;
			if (inc) {
				property.endtime = Math.min(property.endtime + change, maxEndTime);
			} else {
				property.endtime = Math.max(property.endtime - change, minEndTime);
			}
		}, true, property -> String.format("END TIME : %2d:%02d.%1d", property.endtime / 60000,
				(property.endtime / 1000) % 60, (property.endtime / 100) % 10)),
		GAUGETYPE((practice, inc, isAnalog, turboSpeed) -> {
			final PracticeProperty property = practice.property;
			property.gaugetype = (property.gaugetype + (inc ? 1 : 8)) % 9;
			if ((practice.model.getMode() == Mode.POPN_5K || practice.model.getMode() == Mode.POPN_9K) && property.gaugetype >= 3 && property.startgauge > 100) {
				property.startgauge = 100;
			}
		}, false, property -> "GAUGE TYPE : " + GAUGE[property.gaugetype]),
		GAUGECATEGORY((practice, inc, isAnalog, turboSpeed) -> {
			final PracticeProperty property = practice.property;
			GaugeProperty[] categories = GaugeProperty.values();
			for(int i = 0;i < categories.length;i++) {
				if(property.gaugecategory == categories[i]) {
					property.gaugecategory = categories[(i + (inc ? 1 : (categories.length - 1))) % categories.length];
					break;
				}
			}
			property.startgauge = (int) property.gaugecategory.values[property.gaugetype].init;
		}, false, property -> "GAUGE CATEGORY : " + property.gaugecategory.name()),
		GAUGEVALUE((practice, inc, isAnalog, turboSpeed) -> {
			final PracticeProperty property = practice.property;
			int change = turboSpeed ? 10 : 1;
			int maxValue = (int)property.gaugecategory.values[property.gaugetype].max;
			if (inc && turboSpeed && property.startgauge == 1) {
				property.startgauge = Math.min(change, maxValue);
			} else {
				property.startgauge = MathUtils.clamp(property.startgauge + (inc ? change : -change), 1, maxValue);
			}
		}, true, property -> "GAUGE VALUE : " + property.startgauge),
		JUDGERANK((practice, inc, isAnalog, turboSpeed) -> {
			final PracticeProperty property = practice.property;
			int change = turboSpeed ? 25 : 1;
			if (inc && turboSpeed && property.judgerank == 1) {
				property.judgerank = Math.min(change, 400);
			} else {
				property.judgerank = MathUtils.clamp(property.judgerank + (inc ? change : -change), 1, 400);
			}
		}, true, property -> "JUDGERANK : " + property.judgerank),
		TOTAL((practice, inc, isAnalog, turboSpeed) -> {
			int change = turboSpeed ? 25 : 5;
			if (isAnalog) {
				change = turboSpeed ? 20 : 1;
			}
			practice.property.total = MathUtils.clamp(practice.property.total + (inc ? change : -change), 10, 5000);
		}, true, property -> "TOTAL : " + (int)property.total),
		FREQ((practice, inc, isAnalog, turboSpeed) -> {
			int change = turboSpeed ? 25 : 5;
			if (isAnalog) {
				change = turboSpeed ? 10 : 1;
			}
			practice.property.freq = MathUtils.clamp(practice.property.freq + (inc ? change : -change), 50, 200);
		}, true, property -> "FREQUENCY : " + property.freq),
		GRAPHTYPE((practice, inc, isAnalog, turboSpeed) -> {
			practice.property.graphtype = (practice.property.graphtype + (inc ? 1 : 2)) % 3;
		}, false, property -> "GRAPHTYPE : " + GRAPHTYPESTR[property.graphtype]),
		OPTION1P((practice, inc, isAnalog, turboSpeed) -> {
			final int options = (practice.model.getMode() == Mode.POPN_5K || practice.model.getMode() == Mode.POPN_9K ? 7 : 10);
			practice.property.random = (practice.property.random + (inc ? 1 : (options -1))) % options;
		}, false, property -> "OPTION-1P : " + RANDOM[property.random]),
		OPTION2P((practice, inc, isAnalog, turboSpeed) -> {
			practice.property.random2 = (practice.property.random2 + (inc ? 1 : 9)) % 10;
		}, false, property -> "OPTION-2P : " + RANDOM[property.random2], practice -> practice.model.getMode().player == 2),
		OPTIONDP((practice, inc, isAnalog, turboSpeed) -> {
			practice.property.doubleop = (practice.property.doubleop + 1) % 2;
		}, false, property -> "OPTION-DP : " + DPRANDOM[property.doubleop], practice -> practice.model.getMode().player == 2);

		public final PracticeAction action;

		public final boolean useFineAnalogTicks;

		public final Function<PracticeProperty, String> text;

		public final Predicate<PracticeConfiguration> optionAvailable;

		private PracticeElement(PracticeAction action, boolean useFineAnalogTicks, Function<PracticeProperty, String> text) {
			this(action, useFineAnalogTicks, text, property -> true);
		}

		private PracticeElement(PracticeAction action, boolean useFineAnalogTicks, Function<PracticeProperty, String> text, Predicate<PracticeConfiguration> optionAvailable) {
			this.action = action;
			this.useFineAnalogTicks = useFineAnalogTicks;
			this.text = text;
			this.optionAvailable = optionAvailable;
		}
	}

	/**
	 * プラクティスの各種設定値
	 *
	 * @author exch
	 */
	public static class PracticeProperty {

		/**
		 * 演奏開始時間
		 */
		public int starttime = 0;
		/**
		 * 演奏終了時間
		 */
		public int endtime = 10000;
		/**
		 * 選択ゲージカテゴリ
		 */
		public GaugeProperty gaugecategory;
		/**
		 * 選択ゲージタイプ
		 */
		public int gaugetype = 2;
		/**
		 * 開始ゲージ量
		 */
		public int startgauge = 20;
		/**
		 * 1P側オプション
		 */
		public int random = 0;
		/**
		 * 2P側オプション
		 */
		public int random2 = 0;
		/**
		 * DPオプション
		 */
		public int doubleop = 0;
		/**
		 * 判定幅
		 */
		public int judgerank = 100;
		/**
		 * 再生速度倍率
		 */
		public int freq = 100;
		/**
		 * TOTAL値
		 */
		public double total = 0;
		/**
		 *
		 */
		public int graphtype = 0;
	}
}
