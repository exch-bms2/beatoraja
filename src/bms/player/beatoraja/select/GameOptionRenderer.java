package bms.player.beatoraja.select;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import bms.player.beatoraja.Config;

/**
 * ゲームオプション描画、入力受付用クラス
 * 
 * @author exch
 */
public class GameOptionRenderer {

	private static final String[] SCOREOP = { "OFF", "MIRROR", "RANDOM", "R-RANDOM", "S-RANDOM", "SPIRAL", "H-RANDOM",
			"ALL-SCR", "RANDOM-EX", "S-RANDOM-EX" };

	private static final String[] DOUBLEOP = { "OFF", "FLIP", "BATTLE" };

	private static final String[] GAUGEOP = { "ASSIST EASY", "EASY", "NORMAL", "HARD", "EX-HARD", "HAZARD" };

	private static final String[] FIXHISPEEDOP = { "OFF", "STARTBPM", "MAXBPM", "MAINBPM" };

	private ShapeRenderer shape;
	private SpriteBatch sprite;
	private BitmapFont titlefont;
	
	private Config config;
	
	public GameOptionRenderer(ShapeRenderer shape, SpriteBatch sprite, BitmapFont titlefont, Config config) {
		this.sprite = sprite;
		this.shape = shape;
		this.titlefont = titlefont;
		this.config = config;
	}
	
	public void render(boolean[] keystate, long[] keytime) {
		if (keystate[0] && keytime[0] != 0) {
			keytime[0] = 0;
			config.setGauge(config.getGauge() + 1 < GAUGEOP.length ? config.getGauge() + 1 : 0);
		}
		if (keystate[1] && keytime[1] != 0) {
			keytime[1] = 0;
			config.setRandom(config.getRandom() + 1 < SCOREOP.length ? config.getRandom() + 1 : 0);
		}
		if (keystate[3] && keytime[3] != 0) {
			keytime[3] = 0;
			config.setDoubleoption(
					config.getDoubleoption() + 1 < DOUBLEOP.length ? config.getDoubleoption() + 1 : 0);
		}
		if (keystate[5] && keytime[5] != 0) {
			keytime[5] = 0;
			config.setRandom2(config.getRandom2() + 1 < SCOREOP.length ? config.getRandom2() + 1 : 0);
		}
		if (keystate[6] && keytime[6] != 0) {
			keytime[6] = 0;
			config.setFixhispeed(config.getFixhispeed() + 1 < FIXHISPEEDOP.length ? config.getFixhispeed() + 1 : 0);
		}
		
		shape.begin(ShapeType.Filled);
		shape.setColor(Color.BLACK);
		shape.rect(800, 200, 400, 400);
		shape.end();
		shape.begin(ShapeType.Line);
		shape.setColor(Color.WHITE);
		shape.rect(800, 200, 400, 400);
		shape.rect(850, 250, 55, 95);
		shape.rect(880, 350, 55, 95);
		shape.rect(910, 250, 55, 95);
		shape.rect(940, 350, 55, 95);
		shape.rect(970, 250, 55, 95);
		shape.rect(1000, 350, 55, 95);
		shape.rect(1030, 250, 55, 95);
		shape.end();

		sprite.begin();
		titlefont.draw(sprite, SCOREOP[config.getRandom()], 810, 520);
		titlefont.draw(sprite, DOUBLEOP[config.getDoubleoption()], 920, 520);
		titlefont.draw(sprite, GAUGEOP[config.getGauge()], 810, 220);
		titlefont.draw(sprite, SCOREOP[config.getRandom2()], 1030, 520);
		titlefont.draw(sprite, FIXHISPEEDOP[config.getFixhispeed()], 1000, 220);
		sprite.end();

	}
}
