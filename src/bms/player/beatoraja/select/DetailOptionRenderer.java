package bms.player.beatoraja.select;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayConfig;

public class DetailOptionRenderer {
	
	// TODO レーンカバー、リフト

	private Config config;

	private static final String[] BGA = { "BGA ON", "BGA AUTO", "BGA OFF" };

	private static final String[] JUDGE = { "JUDGE OFF", "JUDGE F/S", "JUDGE +-ms" };

	public DetailOptionRenderer(Config config) {
		this.config = config;
	}

	public void render(boolean[] keystate, long[] keytime, Bar bar) {
		PlayConfig pc = null;
		if(bar instanceof SongBar) {
			SongBar song = (SongBar)bar;
			pc = (song.getSongData().getMode() == 5 || song.getSongData().getMode() == 7 ? config.getMode7()
					: (song.getSongData().getMode() == 10 || song.getSongData().getMode() == 14 ? config.getMode14() : config.getMode9()));			
		}
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
			if (pc != null && pc.getDuration() > 1) {
				pc.setDuration(pc.getDuration() - 1);
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
			if (pc != null && pc.getDuration() < 2000) {
				pc.setDuration(pc.getDuration() + 1);
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
