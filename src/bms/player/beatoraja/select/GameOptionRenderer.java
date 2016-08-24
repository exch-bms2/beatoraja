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

	private static final String[] FIXHISPEEDOP = { "OFF", "STARTBPM", "MAXBPM", "MAINBPM", "MINBPM" };

	private Config config;
	
	public GameOptionRenderer(Config config) {
		this.config = config;
	}
	
	public void render(boolean[] keystate, long[] keytime) {
		if (keystate[0] && keytime[0] != 0) {
			keytime[0] = 0;
			config.setRandom(config.getRandom() + 1 < SCOREOP.length ? config.getRandom() + 1 : 0);
		}
		if (keystate[2] && keytime[2] != 0) {
			keytime[2] = 0;
			config.setGauge(config.getGauge() + 1 < GAUGEOP.length ? config.getGauge() + 1 : 0);
		}
		if (keystate[3] && keytime[3] != 0) {
			keytime[3] = 0;
			config.setDoubleoption(
					config.getDoubleoption() + 1 < DOUBLEOP.length ? config.getDoubleoption() + 1 : 0);
		}
		if (keystate[6] && keytime[6] != 0) {
			keytime[6] = 0;
			config.setRandom2(config.getRandom2() + 1 < SCOREOP.length ? config.getRandom2() + 1 : 0);
		}
		if (keystate[4] && keytime[4] != 0) {
			keytime[4] = 0;
			config.setFixhispeed(config.getFixhispeed() + 1 < FIXHISPEEDOP.length ? config.getFixhispeed() + 1 : 0);
		}
	}
}
