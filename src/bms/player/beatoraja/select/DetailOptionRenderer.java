package bms.player.beatoraja.select;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import bms.player.beatoraja.Config;

public class DetailOptionRenderer {
	
	// TODO レーンカバー、リフト

	private Config config;

	private static final String[] BGA = { "BGA ON", "BGA AUTO", "BGA OFF" };

	private static final String[] JUDGE = { "JUDGE OFF", "JUDGE F/S", "JUDGE +-ms" };

	public DetailOptionRenderer(Config config) {
		this.config = config;
	}

	public void render(boolean[] keystate, long[] keytime) {
		if (keystate[0] && keytime[0] != 0) {
			keytime[0] = 0;
			config.setBga((config.getBga() + 1) % BGA.length);
		}
		if (keystate[1] && keytime[1] != 0) {
			keytime[1] = 0;
			config.setJudgedetail((config.getJudgedetail() + 1) % JUDGE.length);
		}
		if (keystate[3] && keytime[3] != 0) {
			keytime[3] = 0;
			if (config.getGreenvalue() > 1) {
				config.setGreenvalue(config.getGreenvalue() - 1);
			}
		}
		if (keystate[4] && keytime[4] != 0) {
			keytime[4] = 0;
			if (config.getJudgetiming() > -99) {
				config.setJudgetiming(config.getJudgetiming() - 1);
			}
		}
		if (keystate[5] && keytime[5] != 0) {
			keytime[5] = 0;
			if (config.getGreenvalue() < 2000) {
				config.setGreenvalue(config.getGreenvalue() + 1);
			}
		}
		if (keystate[6] && keytime[6] != 0) {
			keytime[6] = 0;
			if (config.getJudgetiming() < 99) {
				config.setJudgetiming(config.getJudgetiming() + 1);
			}
		}
	}

}
