package bms.player.beatoraja.play;

import java.io.*;
import java.nio.file.*;
import java.util.function.*;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.model.TimeLine;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyBoardInputProcesseor.ControlKeys;
import bms.player.beatoraja.skin.SkinNoteDistributionGraph;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.SerializationException;

/**
 * プラクティスモードの設定表示/編集用クラス
 *
 * @author exch
 */
public final class PracticeConfiguration {


	private BitmapFont titlefont;

	private int cursorpos = 0;
	private long presscount = 0;

	private BMSModel model;

	private static final String[] GAUGE = { "ASSIST EASY", "EASY", "NORMAL", "HARD", "EX-HARD", "HAZARD", "GRADE",
			"EX GRADE", "EXHARD GRADE"};
	private static final String[] RANDOM = { "NORMAL", "MIRROR", "RANDOM", "R-RANDOM", "S-RANDOM", "SPIRAL", "H-RANDOM",
			"ALL-SCR", "RANDOM-EX", "S-RANDOM-EX" };
	private static final String[] DPRANDOM = { "NORMAL", "FLIP" };

	private PracticeProperty property = new PracticeProperty();

	public PracticeConfiguration() {
		// TODO 描画位置、使用テキスト等をスキン定義できるように
		// TODO スキン定義がない場合のデフォルト配置の定義
	}

	private SkinNoteDistributionGraph[] graph = { 
			new SkinNoteDistributionGraph(SkinNoteDistributionGraph.TYPE_NORMAL, 500, 0, 0, 0, 0),
			new SkinNoteDistributionGraph(SkinNoteDistributionGraph.TYPE_JUDGE, 500, 0, 0, 0, 0),
			new SkinNoteDistributionGraph(SkinNoteDistributionGraph.TYPE_EARLYLATE, 500, 0, 0, 0, 0),
	};
	
	private static final String[] GRAPHTYPESTR = {"NOTETYPE", "JUDGE", "EARLYLATE"};

	public static final PracticeElement[] elements = PracticeElement.values();

	public void create(BMSModel model, Config config) {
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
		try {
			FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
					Gdx.files.internal(config.getSystemfontpath()));
			FreeTypeFontParameter parameter = new FreeTypeFontParameter();
			parameter.size = 18;
			titlefont = generator.generateFont(parameter);
			generator.dispose();
		} catch (GdxRuntimeException e) {
			Logger.getGlobal().warning("Practice Font読み込み失敗");
		}
		
		for(int i = 0; i < graph.length; i++) {
			graph[i].setDestination(0, 0, 0, 0, 0, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, new int[0]);
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

	public GrooveGauge getGauge(BMSModel model) {
		GrooveGauge gauge = GrooveGauge.create(model, property.gaugetype, property.gaugecategory);
		gauge.setValue(property.startgauge);
		return gauge;
	}
	
	public void processInput(BMSPlayerInputProcessor input) {
		if (input.isControlKeyPressed(ControlKeys.UP)) {
			do {
				cursorpos = (cursorpos + elements.length - 1) % elements.length;
			} while(!elements[cursorpos].predicate.test(this));
		}
		if (input.isControlKeyPressed(ControlKeys.DOWN)) {
			do {
				cursorpos = (cursorpos + 1) % elements.length;
			} while(!elements[cursorpos].predicate.test(this));
		}
		if (input.getControlKeyState(ControlKeys.LEFT) && (presscount == 0 || presscount + 10 < System.currentTimeMillis())) {
			if (presscount == 0) {
				presscount = System.currentTimeMillis() + 500;
			} else {
				presscount = System.currentTimeMillis();
			}
			elements[cursorpos].action.accept(this, false);
		} else if (input.getControlKeyState(ControlKeys.RIGHT) && (presscount == 0 || presscount + 10 < System.currentTimeMillis())) {
			if (presscount == 0) {
				presscount = System.currentTimeMillis() + 500;
			} else {
				presscount = System.currentTimeMillis();
			}
			elements[cursorpos].action.accept(this, true);
		} else if (!(input.getControlKeyState(ControlKeys.LEFT) || input.getControlKeyState(ControlKeys.RIGHT))) {
			presscount = 0;
		}
	}

	public void draw(Rectangle r, SkinObjectRenderer sprite, long time, MainState state) {
		float x = r.x + r.width / 8;
		float y = r.y + r.height * 7 / 8;
		if(titlefont != null) {
			for(int i = 0;i < elements.length;i++) {
				if(elements[i].predicate.test(this)) {
					sprite.draw(titlefont, elements[i].text.apply(property), x, y - 22 * i, cursorpos == i ? Color.YELLOW : Color.CYAN);
				}
			}

			if (state.resource.mediaLoadFinished()) {
				sprite.draw(titlefont, "PRESS 1KEY TO PLAY", x, y - 276, Color.ORANGE);
			}
			
			String[] judge = {"PGREAT :","GREAT  :","GOOD   :", "BAD    :", "POOR   :", "KPOOR  :"};
			for(int i = 0; i < 6; i++) {
				sprite.draw(titlefont, String.format("%s %d %d %d",judge[i], state.getJudgeCount(i, true) + state.getJudgeCount(i, false), state.getJudgeCount(i, true), state.getJudgeCount(i, false)), x + 250, y - (i * 22), Color.WHITE);
			}			
		}

		graph[property.graphtype].draw(sprite, time, state, new Rectangle(r.x, r.y, r.width, r.height / 4), property.starttime,
				property.endtime, property.freq / 100f);
	}
	
	public void dispose() {
		if(titlefont != null) {
			titlefont.dispose();
			titlefont = null;
		}
	}

	enum PracticeElement {

		STARTTIME((practice, inc) -> {
			final TimeLine[] tl = practice.model.getAllTimeLines();
			final PracticeProperty property = practice.property;
			if(inc) {
				if (property.starttime + 2000 <= tl[tl.length - 1].getTime()) {
					property.starttime += 100;
				}
				if (property.starttime + 900 >= property.endtime) {
					property.endtime += 100;
				}
			} else {
				if (property.starttime >= 100) {
					property.starttime -= 100;
				}
			}
		}, property -> String.format("START TIME : %2d:%02d.%1d", property.starttime / 60000,
				(property.starttime / 1000) % 60, (property.starttime / 100) % 10)),
		ENDTIME((practice, inc) -> {
			final TimeLine[] tl = practice.model.getAllTimeLines();
			final PracticeProperty property = practice.property;
			if(inc) {
				if (property.endtime <= tl[tl.length - 1].getTime() + 1000) {
					property.endtime += 100;
				}
			} else {
				if (property.endtime > property.starttime + 1000) {
					property.endtime -= 100;
				}
			}
		}, property -> String.format("END TIME : %2d:%02d.%1d", property.endtime / 60000,
				(property.endtime / 1000) % 60, (property.endtime / 100) % 10)),
		GAUGETYPE((practice, inc) -> {
			final PracticeProperty property = practice.property;
			property.gaugetype = (property.gaugetype + (inc ? 1 : 8)) % 9;
			if ((practice.model.getMode() == Mode.POPN_5K || practice.model.getMode() == Mode.POPN_9K) && property.gaugetype >= 3 && property.startgauge > 100) {
				property.startgauge = 100;
			}
		}, property -> "GAUGE TYPE : " + GAUGE[property.gaugetype]),
		GAUGECATEGORY((practice, inc) -> {
			final PracticeProperty property = practice.property;
			GaugeProperty[] cateories = GaugeProperty.values();
			for(int i = 0;i < cateories.length;i++) {
				if(property.gaugecategory == cateories[i]) {
					property.gaugecategory = cateories[(i + (inc ? 1 : (cateories.length - 1))) % cateories.length];
					break;
				}
			}
			property.startgauge = (int) property.gaugecategory.values[property.gaugetype].init;
		}, property -> "GAUGE CATEGORY : " + property.gaugecategory.name()),
		GAUGEVALUE((practice, inc) -> {
			final PracticeProperty property = practice.property;
			property.startgauge = MathUtils.clamp(property.startgauge + (inc ? 1 : -1), 1, (int)property.gaugecategory.values[property.gaugetype].max);
		}, property -> "GAUGE VALUE : " + property.startgauge),
		JUDGERANK((practice, inc) -> {
			practice.property.judgerank = MathUtils.clamp(practice.property.judgerank + (inc ? 1 : -1), 1, 400);
		}, property -> "JUDGERANK : " + property.judgerank),
		TOTAL((practice, inc) -> {
			practice.property.total = MathUtils.clamp(practice.property.total + (inc ? 10 : -10), 20, 5000);
		}, property -> "TOTAL : " + (int)property.total),
		FREQ((practice, inc) -> {
			practice.property.freq = MathUtils.clamp(practice.property.freq + (inc ? 5 : -5), 50, 200);
		}, property -> "FREQUENCY : " + property.freq),
		GRAPHTYPE((practice, inc) -> {
			practice.property.graphtype = (practice.property.graphtype + (inc ? 1 : 2)) % 3;
		}, property -> "GRAPHTYPE : " + GRAPHTYPESTR[property.graphtype]),
		OPTION1P((practice, inc) -> {
			final int options = (practice.model.getMode() == Mode.POPN_5K || practice.model.getMode() == Mode.POPN_9K ? 7 : 10);
			practice.property.random = (practice.property.random + (inc ? 1 : (options -1))) % options;
		}, property -> "OPTION-1P : " + RANDOM[property.random]),
		OPTION2P((practice, inc) -> {
			practice.property.random2 = (practice.property.random2 + (inc ? 1 : 9)) % 10;
		}, property -> "OPTION-2P : " + RANDOM[property.random2], practice -> practice.model.getMode().player == 2),
		OPTIONDP((practice, inc) -> {
			practice.property.doubleop = (practice.property.doubleop + 1) % 2;
		}, property -> "OPTION-DP : " + DPRANDOM[property.doubleop], practice -> practice.model.getMode().player == 2);

		public final BiConsumer<PracticeConfiguration, Boolean> action;

		public final Function<PracticeProperty, String> text;

		public final Predicate<PracticeConfiguration> predicate;

		private PracticeElement(BiConsumer<PracticeConfiguration, Boolean> action, Function<PracticeProperty, String> text) {
			this(action, text, property -> true);
		}

		private PracticeElement(BiConsumer<PracticeConfiguration, Boolean> action, Function<PracticeProperty, String> text, Predicate<PracticeConfiguration> predicate) {
			this.action = action;
			this.text = text;
			this.predicate = predicate;
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
