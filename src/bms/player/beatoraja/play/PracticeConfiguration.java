package bms.player.beatoraja.play;

import bms.model.BMSModel;
import bms.model.TimeLine;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.play.gauge.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.Rectangle;

public class PracticeConfiguration {

	private BitmapFont titlefont;

	private int starttime = 0;
	private int endtime = 10000;
	private int gaugetype = 2;
	private int startgauge = 20;
	private int random = 0;
	private int random2 = 0;
	private int doubleop = 0;
	private int judgerank = 100;
	
	private int cursorpos = 0;
	private long presscount = 0;

	private BMSModel model;

	private static final String[] GAUGE = { "ASSIST EASY", "EASY", "NORMAL", "HARD", "EX-HARD", "HAZARD" ,"GRADE", "EX GRADE", "EXHARD GRADE"};
	private static final String[] RANDOM = { "NORMAL", "MIRROR", "RANDOM", "R-RANDOM", "S-RANDOM", "SPIRAL" ,"H-RANDOM", "ALL-SCR", "RANDOM-EX","S-RANDOM-EX"};
	private static final String[] DPRANDOM = { "NORMAL", "FLIP"};

	public void create(BMSModel model) {
		this.model = model;
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 18;
		titlefont = generator.generateFont(parameter);
		judgerank = model.getJudgerank();
		endtime = model.getLastTime() + 1000;
	}


	public int getStartTime() {
		return starttime;
	}

	public int getEndTime() {
		return endtime;
	}
	
	public int getOption() {
		return random;
	}

	public int getOption2() {
		return random2;
	}

	public int getDoubleOption() {
		return doubleop;
	}

	public int getJudgerank() {
		return judgerank;
	}

	public GrooveGauge getGauge(BMSModel model) {
		GrooveGauge gauge = null;
		switch(gaugetype) {
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
		
		gauge.setValue(startgauge);
		return gauge;
	}
	
	public void processInput(BMSPlayerInputProcessor input) {
		boolean[] cursor = input.getCursorState();
		if (cursor[0]) {
			cursor[0] = false;
			cursorpos = (cursorpos + (model.getUseKeys() >= 10 ? 7 : 5)) % (model.getUseKeys() >= 10 ? 8 : 6);
		}
		if (cursor[1]) {
			cursor[1] = false;
			cursorpos = (cursorpos + 1) % (model.getUseKeys() >= 10 ? 8 : 6);
		}
		if (cursor[2] && (presscount == 0 || presscount + 10 < System.currentTimeMillis())) {
			if(presscount == 0) {
				presscount = System.currentTimeMillis() + 500;
			} else {
				presscount = System.currentTimeMillis();
			}
			switch(cursorpos) {
			case 0:
				if(starttime >= 100) {
					starttime -= 100;
				}
				break;
			case 1:
				if(endtime > starttime + 1000) {
					endtime -= 100;
				}
				break;
			case 2:
				gaugetype = (gaugetype + 8) % 9;
				if(model.getUseKeys() == 9 && gaugetype >= 3 && startgauge > 100) {
					startgauge = 100;
				}
				break;
			case 3:
				if(startgauge > 1) {
					startgauge--;
				}
				break;
			case 5:
				random = (random + (model.getUseKeys() == 9 ? 6 : 9)) % (model.getUseKeys() == 9 ? 7 : 10);
				break;
				case 6:
					random2 = (random2 + 9) % 10;
					break;
				case 7:
					doubleop = (doubleop + 1) % 2;
					break;
				case 4:
					if(judgerank > 10) {
						judgerank -= 10;
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
				if(starttime + 2000 <= tl[tl.length - 1].getTime()) {
					starttime += 100;
				}
				if(starttime + 900 >= endtime) {
					endtime += 100;
				}
				break;
			case 1:
				if(endtime <= tl[tl.length - 1].getTime() + 1000) {
					endtime += 100;
				}
				break;
			case 2:
				gaugetype = (gaugetype + 1) % 9;
				if(model.getUseKeys() == 9 && gaugetype >= 3 && startgauge > 100) {
					startgauge = 100;
				}
				break;
			case 3:
				if(startgauge < 100 || (model.getUseKeys() == 9 && gaugetype <= 2 && startgauge < 120)) {
					startgauge++;
				}
				break;
			case 5:
				random = (random + 1) % (model.getUseKeys() == 9 ? 7 : 10);
				break;
				case 6:
					random2 = (random2 + 1) % 10;
					break;
				case 7:
					doubleop = (doubleop + 1) % 2;
					break;
				case 4:
					judgerank += 10;
					break;

			}
		} else if(!(cursor[2] || cursor[3])){
			presscount = 0;
		}
	}

	public void draw(Rectangle r, SpriteBatch sprite, long time, MainState state) {
		float x = r.x + r.width / 8;
		float y = r.y + r.height * 7 / 8;
		titlefont.setColor(cursorpos == 0 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, String.format("START TIME : %2d:%02d.%1d", starttime / 60000, (starttime / 1000) % 60, (starttime / 100) % 10), x, y);
		titlefont.setColor(cursorpos == 1 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, String.format("END TIME : %2d:%02d.%1d", endtime / 60000, (endtime / 1000) % 60, (endtime / 100) % 10), x, y - 22);
		titlefont.setColor(cursorpos == 2 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "GAUGE TYPE : " + GAUGE[gaugetype], x, y - 44);
		titlefont.setColor(cursorpos == 3 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "GAUGE VALUE : " + startgauge, x, y - 66);
		titlefont.setColor(cursorpos == 4 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "JUDGERANK : " + judgerank, x, y - 88);
		titlefont.setColor(cursorpos == 5 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "OPTION-1P : " + RANDOM[random], x, y - 110);
		if(model.getUseKeys() >= 10) {
			titlefont.setColor(cursorpos == 6 ? Color.YELLOW : Color.CYAN);
			titlefont.draw(sprite, "OPTION-2P : " + RANDOM[random2], x, y - 132);
			titlefont.setColor(cursorpos == 7 ? Color.YELLOW : Color.CYAN);
			titlefont.draw(sprite, "OPTION-DP : " + DPRANDOM[doubleop], x, y - 154);
		}

		if(state.getMainController().getPlayerResource().mediaLoadFinished()) {
			titlefont.setColor(Color.ORANGE);
			titlefont.draw(sprite, "PRESS 1KEY TO PLAY", x, y - 198);
		}
	}
}
