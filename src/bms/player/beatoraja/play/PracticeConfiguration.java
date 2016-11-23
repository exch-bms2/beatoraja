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

	private int startsection = 5;
	private int endsection = 10;
	private int gaugetype;
	private int startgauge = 20;
	
	private int cursorpos = 0;
	
	private BMSModel model;

	private static final String[] GAUGE = { "ASSIST EASY", "EASY", "NORMAL", "HARD", "EX-HARD", "HAZARD" ,"GRADE", "EX GRADE", "EXHARD GRADE"};
	public void create(BMSModel model) {
		this.model = model;
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
		titlefont = generator.generateFont(parameter);
	}


	public int getStartSection() {
		return startsection;
	}

	public int getStartTime() {
		for(TimeLine tl : model.getAllTimeLines()) {
			if(startsection == tl.getSection()) {
				return tl.getTime();
			}
		}
		return 0;
	}

	public int getEndSection() {
		return endsection;
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
			cursorpos = (cursorpos + 3) % 4;
		}
		if (cursor[1]) {
			cursor[1] = false;
			cursorpos = (cursorpos + 1) % 4;
		}
		if (cursor[2]) {
			cursor[2] = false;
			switch(cursorpos) {
			case 0:
				if(startsection > 0) {
					startsection--;
				}
				break;
			case 1:
				if(endsection > startsection + 1) {
					endsection--;					
				}
				break;
			case 2:
				gaugetype = (gaugetype + 8) % 9;
				break;
			case 3:
				if(startgauge > 1) {
					startgauge--;
				}
				break;
			}
		}
		if (cursor[3]) {
			cursor[3] = false;
			TimeLine[] tl = model.getAllTimeLines();
			switch(cursorpos) {
			case 0:
				if(startsection + 2 <= tl[tl.length - 1].getSection()) {
					startsection++;					
				}
				if(startsection == endsection) {
					endsection++;					
				}
				break;
			case 1:
				if(endsection + 1 <= tl[tl.length - 1].getSection()) {
					endsection++;					
				}
				break;
			case 2:
				gaugetype = (gaugetype + 1) % 9;
				break;
			case 3:
				if(startgauge < 100) {
					startgauge++;
				}
				break;
			}
		}		
	}

	public void draw(Rectangle r, SpriteBatch sprite, long time, MainState state) {
		float x = r.x;
		float y = r.y + r.height;
		titlefont.setColor(cursorpos == 0 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "START SECTION : " + startsection, x, y);
		titlefont.setColor(cursorpos == 1 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "END SECTION : " + endsection, x, y - 28);
		titlefont.setColor(cursorpos == 2 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "GAUGE TYPE : " + GAUGE[gaugetype], x, y - 56);
		titlefont.setColor(cursorpos == 3 ? Color.YELLOW : Color.CYAN);
		titlefont.draw(sprite, "GAUGE VALUE : " + startgauge, x, y - 84);

	}
}
