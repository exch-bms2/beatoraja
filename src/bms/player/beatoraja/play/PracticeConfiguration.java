package bms.player.beatoraja.play;

import java.io.*;
import java.nio.file.*;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.model.TimeLine;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;

import bms.player.beatoraja.skin.SkinNoteDistributionGraph;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Json;

/**
 * プラクティスモードの設定表示/編集用クラス
 *
 * @author exch
 */
public class PracticeConfiguration {

	private BitmapFont titlefont;

	private int cursorpos = 0;
	private long presscount = 0;

	private BMSModel model;

	private static final String[] GAUGE = { "ASSIST EASY", "EASY", "NORMAL", "HARD", "EX-HARD", "HAZARD", "GRADE",
			"EX GRADE", "EXHARD GRADE" };
	private static final String[] RANDOM = { "NORMAL", "MIRROR", "RANDOM", "R-RANDOM", "S-RANDOM", "SPIRAL", "H-RANDOM",
			"ALL-SCR", "RANDOM-EX", "S-RANDOM-EX" };
	private static final String[] DPRANDOM = { "NORMAL", "FLIP" };

	private PracticeProperty property = new PracticeProperty();

	private SkinNoteDistributionGraph graph = new SkinNoteDistributionGraph();

	public void create(BMSModel model) {
		property.judgerank = model.getJudgerank();
		property.endtime = model.getLastTime() + 1000;
		Path p = Paths.get("practice/" + model.getSHA256() + ".json");
		if (Files.exists(p)) {
			Json json = new Json();
			try {
				property = json.fromJson(PracticeProperty.class, new FileReader(p.toFile()));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		this.model = model;
		if(property.total == 0) {
			property.total = model.getTotal();
		}
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
				Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 18;
		titlefont = generator.generateFont(parameter);
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
		GrooveGauge gauge = GrooveGauge.create(model, property.gaugetype);
		gauge.setValue(property.startgauge);
		return gauge;
	}

	public void processInput(BMSPlayerInputProcessor input) {
		final int values = model.getMode().player == 2 ? 10 : 8;
		boolean[] cursor = input.getCursorState();
		long[] cursortime = input.getCursorTime();
		if (cursor[0] && cursortime[0] != 0) {
			cursortime[0] = 0;
			cursorpos = (cursorpos + values - 1) % values;
		}
		if (cursor[1] && cursortime[1] != 0) {
			cursortime[1] = 0;
			cursorpos = (cursorpos + 1) % values;
		}
		if (cursor[2] && (presscount == 0 || presscount + 10 < System.currentTimeMillis())) {
			if (presscount == 0) {
				presscount = System.currentTimeMillis() + 500;
			} else {
				presscount = System.currentTimeMillis();
			}
			switch (cursorpos) {
			case 0:
				if (property.starttime >= 100) {
					property.starttime -= 100;
				}
				break;
			case 1:
				if (property.endtime > property.starttime + 1000) {
					property.endtime -= 100;
				}
				break;
			case 2:
				property.gaugetype = (property.gaugetype + 8) % 9;
				if ((model.getMode() == Mode.POPN_5K || model.getMode() == Mode.POPN_9K) && property.gaugetype >= 3
						&& property.startgauge > 100) {
					property.startgauge = 100;
				}
				break;
			case 3:
				if (property.startgauge > 1) {
					property.startgauge--;
				}
				break;
			case 4:
				if (property.judgerank > 10) {
					property.judgerank -= 10;
				}
				break;
				case 5:
					if (property.total > 20) {
						property.total -= 10;
					}
					break;
			case 6:
				if (property.freq > 50) {
					property.freq -= 5;
				}
				break;
			case 7:
				property.random = (property.random + (model.getMode() == Mode.POPN_5K || model.getMode() == Mode.POPN_9K ? 6 : 9))
						% (model.getMode() == Mode.POPN_5K || model.getMode() == Mode.POPN_9K ? 7 : 10);
				break;
			case 8:
				property.random2 = (property.random2 + 9) % 10;
				break;
			case 9:
				property.doubleop = (property.doubleop + 1) % 2;
				break;
			}
		} else if (cursor[3] && (presscount == 0 || presscount + 10 < System.currentTimeMillis())) {
			if (presscount == 0) {
				presscount = System.currentTimeMillis() + 500;
			} else {
				presscount = System.currentTimeMillis();
			}
			TimeLine[] tl = model.getAllTimeLines();
			switch (cursorpos) {
			case 0:
				if (property.starttime + 2000 <= tl[tl.length - 1].getTime()) {
					property.starttime += 100;
				}
				if (property.starttime + 900 >= property.endtime) {
					property.endtime += 100;
				}
				break;
			case 1:
				if (property.endtime <= tl[tl.length - 1].getTime() + 1000) {
					property.endtime += 100;
				}
				break;
			case 2:
				property.gaugetype = (property.gaugetype + 1) % 9;
				if ((model.getMode() == Mode.POPN_5K || model.getMode() == Mode.POPN_9K) && property.gaugetype >= 3 && property.startgauge > 100) {
					property.startgauge = 100;
				}
				break;
			case 3:
				if (property.startgauge < 100
						|| ((model.getMode() == Mode.POPN_5K || model.getMode() == Mode.POPN_9K) && property.gaugetype <= 2 && property.startgauge < 120)) {
					property.startgauge++;
				}
				break;
			case 4:
				if (property.judgerank < 400) {
					property.judgerank += 10;
				}
				break;
				case 5:
					if (property.total < 5000) {
						property.total += 10;
					}
					break;
			case 6:
				if (property.freq < 200) {
					property.freq += 5;
				}
				break;
			case 7:
				property.random = (property.random + 1) % (model.getMode() == Mode.POPN_5K || model.getMode() == Mode.POPN_9K ? 7 : 10);
				break;
			case 8:
				property.random2 = (property.random2 + 1) % 10;
				break;
			case 9:
				property.doubleop = (property.doubleop + 1) % 2;
				break;

			}
		} else if (!(cursor[2] || cursor[3])) {
			presscount = 0;
		}
	}

	public void draw(Rectangle r, SpriteBatch sprite, long time, MainState state) {
		float x = r.x + r.width / 8;
		float y = r.y + r.height * 7 / 8;
		titlefont.setColor(cursorpos == 0 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, String.format("START TIME : %2d:%02d.%1d", property.starttime / 60000,
				(property.starttime / 1000) % 60, (property.starttime / 100) % 10), x, y);
		titlefont.setColor(cursorpos == 1 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, String.format("END TIME : %2d:%02d.%1d", property.endtime / 60000,
				(property.endtime / 1000) % 60, (property.endtime / 100) % 10), x, y - 22);
		titlefont.setColor(cursorpos == 2 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "GAUGE TYPE : " + GAUGE[property.gaugetype], x, y - 44);
		titlefont.setColor(cursorpos == 3 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "GAUGE VALUE : " + property.startgauge, x, y - 66);
		titlefont.setColor(cursorpos == 4 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "JUDGERANK : " + property.judgerank, x, y - 88);
		titlefont.setColor(cursorpos == 5 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "TOTAL : " + (int)property.total, x, y - 110);
		titlefont.setColor(cursorpos == 6 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "FREQENCY : " + property.freq, x, y - 132);
		titlefont.setColor(cursorpos == 7 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "OPTION-1P : " + RANDOM[property.random], x, y - 154);
		if (model.getMode().player == 2) {
			titlefont.setColor(cursorpos == 8 ? Color.YELLOW : Color.CYAN);
			titlefont.draw(sprite, "OPTION-2P : " + RANDOM[property.random2], x, y - 176);
			titlefont.setColor(cursorpos == 9 ? Color.YELLOW : Color.CYAN);
			titlefont.draw(sprite, "OPTION-DP : " + DPRANDOM[property.doubleop], x, y - 198);
		}

		if (state.getMainController().getPlayerResource().mediaLoadFinished()) {
			titlefont.setColor(Color.ORANGE);
			titlefont.draw(sprite, "PRESS 1KEY TO PLAY", x, y - 232);
		}

		graph.draw(sprite, time, state, new Rectangle(r.x, r.y, r.width, r.height / 4), property.starttime,
				property.endtime);
	}

	/**
	 * プラクティスの各種設定値
	 *
	 * @author exch
	 */
	public static class PracticeProperty {
		public int starttime = 0;
		public int endtime = 10000;
		public int gaugetype = 2;
		public int startgauge = 20;
		public int random = 0;
		public int random2 = 0;
		public int doubleop = 0;
		public int judgerank = 100;
		public int freq = 100;
		public double total = 0;
	}
}
