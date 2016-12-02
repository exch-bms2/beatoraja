package bms.player.beatoraja.play;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import bms.model.BMSModel;
import bms.model.TimeLine;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.TableData;
import bms.player.beatoraja.TableData.CourseData;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.play.gauge.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

public class PracticeConfiguration {

	private BitmapFont titlefont;
	
	private int cursorpos = 0;
	private long presscount = 0;

	private BMSModel model;

	private static final String[] GAUGE = { "ASSIST EASY", "EASY", "NORMAL", "HARD", "EX-HARD", "HAZARD" ,"GRADE", "EX GRADE", "EXHARD GRADE"};
	private static final String[] RANDOM = { "NORMAL", "MIRROR", "RANDOM", "R-RANDOM", "S-RANDOM", "SPIRAL" ,"H-RANDOM", "ALL-SCR", "RANDOM-EX","S-RANDOM-EX"};
	private static final String[] DPRANDOM = { "NORMAL", "FLIP"};

	private PracticeProperty property = new PracticeProperty();
	
	public void create(BMSModel model) {
		property.judgerank = model.getJudgerank();
		property.endtime = model.getLastTime();			
		Path p = Paths.get("practice/" + model.getSHA256() + ".json");
		if(Files.exists(p)) {
			Json json = new Json();
			try {
				property  = json.fromJson(PracticeProperty.class, new FileReader(p.toFile()));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}			
		}
		
		this.model = model;
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
		titlefont = generator.generateFont(parameter);
	}
	
	public void saveProperty() {
		try {
			Files.createDirectory(Paths.get("practice"));
		} catch (IOException e1) {
		}		
		try {
			Json json = new Json();
			FileWriter fw = new FileWriter("practice/" + model.getSHA256() + ".json");
			fw.write(json.prettyPrint(property));
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public PracticeProperty getPracticeProperty() {
		return property;
	}

	public GrooveGauge getGauge(BMSModel model) {
		GrooveGauge gauge = null;
		switch(property.gaugetype) {
		case 0:
			gauge = new AssistEasyGrooveGauge(model);
			break;
		case 1:
			gauge = new EasyGrooveGauge(model);
			break;
		case 2:
			gauge = new NormalGrooveGauge(model);
			break;
		case 3:
			gauge = new HardGrooveGauge(model);
			break;
		case 4:
			gauge = new ExhardGrooveGauge(model);
			break;
		case 5:
			gauge = new HazardGrooveGauge(model);
			break;
		case 6:
			gauge = new GradeGrooveGauge(model);
			break;
		case 7:
			gauge = new ExgradeGrooveGauge(model);
			break;
		case 8:
			gauge = new ExhardGradeGrooveGauge(model);
			break;
		}
		
		gauge.setValue(property.startgauge);
		return gauge;
	}
	
	public void processInput(BMSPlayerInputProcessor input) {
		boolean[] cursor = input.getCursorState();
		if (cursor[0]) {
			cursor[0] = false;
			cursorpos = (cursorpos + 7) % 8;
		}
		if (cursor[1]) {
			cursor[1] = false;
			cursorpos = (cursorpos + 1) % 8;
		}
		if (cursor[2] && (presscount == 0 || presscount + 10 < System.currentTimeMillis())) {
			if(presscount == 0) {
				presscount = System.currentTimeMillis() + 500;
			} else {
				presscount = System.currentTimeMillis();
			}
			switch(cursorpos) {
			case 0:
				if(property.starttime >= 100) {
					property.starttime -= 100;
				}
				break;
			case 1:
				if(property.endtime > property.starttime + 1000) {
					property.endtime -= 100;
				}
				break;
			case 2:
				property.gaugetype = (property.gaugetype + 8) % 9;
				if(model.getUseKeys() == 9 && property.gaugetype >= 3 && property.startgauge > 100) {
					property.startgauge = 100;
				}
				break;
			case 3:
				if(property.startgauge > 1) {
					property.startgauge--;
				}
				break;
			case 4:
				property.random = (property.random + 9) % 10;
				break;
				case 5:
					property.random2 = (property.random2 + 9) % 10;
					break;
				case 6:
					property.doubleop = (property.doubleop + 1) % 2;
					break;
				case 7:
					if(property.judgerank > 10) {
						property.judgerank -= 10;
					}
					break;
			}
		} else if (cursor[3] && (presscount == 0 || presscount + 10 < System.currentTimeMillis())) {
			if(presscount == 0) {
				presscount = System.currentTimeMillis() + 500;
			} else {
				presscount = System.currentTimeMillis();
			}
			TimeLine[] tl = model.getAllTimeLines();
			switch(cursorpos) {
			case 0:
				if(property.starttime + 2000 <= tl[tl.length - 1].getTime()) {
					property.starttime += 100;
				}
				if(property.starttime + 900 >= property.endtime) {
					property.endtime += 100;
				}
				break;
			case 1:
				if(property.endtime <= tl[tl.length - 1].getTime()) {
					property.endtime += 100;
				}
				break;
			case 2:
				property.gaugetype = (property.gaugetype + 1) % 9;
				if(model.getUseKeys() == 9 && property.gaugetype >= 3 && property.startgauge > 100) {
					property.startgauge = 100;
				}
				break;
			case 3:
				if(property.startgauge < 100 || (model.getUseKeys() == 9 && property.gaugetype <= 2 && property.startgauge < 120)) {
					property.startgauge++;
				}
				break;
			case 4:
				property.random = (property.random + 1) % 10;
				break;
				case 5:
					property.random2 = (property.random2 + 1) % 10;
					break;
				case 6:
					property.doubleop = (property.doubleop + 1) % 2;
					break;
				case 7:
					property.judgerank += 10;
					break;

			}
		} else if(!(cursor[2] || cursor[3])){
			presscount = 0;
		}
	}

	public void draw(Rectangle r, SpriteBatch sprite, long time, MainState state) {
		float x = r.x + r.width / 8;
		float y = r.y + r.height;
		titlefont.setColor(cursorpos == 0 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, String.format("START TIME : %2d:%02d.%1d", property.starttime / 60000, (property.starttime / 1000) % 60, (property.starttime / 100) % 10), x, y);
		titlefont.setColor(cursorpos == 1 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, String.format("END TIME : %2d:%02d.%1d", property.endtime / 60000, (property.endtime / 1000) % 60, (property.endtime / 100) % 10), x, y - 28);
		titlefont.setColor(cursorpos == 2 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "GAUGE TYPE : " + GAUGE[property.gaugetype], x, y - 56);
		titlefont.setColor(cursorpos == 3 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "GAUGE VALUE : " + property.startgauge, x, y - 84);
		titlefont.setColor(cursorpos == 4 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "OPTION-1P : " + RANDOM[property.random], x, y - 112);
		titlefont.setColor(cursorpos == 5 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "OPTION-2P : " + RANDOM[property.random2], x, y - 140);
		titlefont.setColor(cursorpos == 6 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "OPTION-DP : " + DPRANDOM[property.doubleop], x, y - 168);
		titlefont.setColor(cursorpos == 7 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "JUDGERANK : " + property.judgerank, x, y - 194);

		if(state.getMainController().getPlayerResource().mediaLoadFinished()) {
			titlefont.setColor(Color.ORANGE);
			titlefont.draw(sprite, "PRESS 1KEY TO PLAY", x, y - 250);
		}
	}
	
	public static class PracticeProperty {
		public int starttime = 0;
		public int endtime = 10000;
		public int gaugetype = 2;
		public int startgauge = 20;
		public int random = 0;
		public int random2 = 0;
		public int doubleop = 0;
		public int judgerank = 100;
	}
}
