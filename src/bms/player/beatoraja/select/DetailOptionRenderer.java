package bms.player.beatoraja.select;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import bms.player.beatoraja.Config;

public class DetailOptionRenderer {

	private ShapeRenderer shape;
	private SpriteBatch sprite;
	private BitmapFont titlefont;

	private Config config;

	private static final String[] BGA = { "BGA ON", "BGA AUTO", "BGA OFF" };

	public DetailOptionRenderer(ShapeRenderer shape, SpriteBatch sprite, BitmapFont titlefont, Config config) {
		this.sprite = sprite;
		this.shape = shape;
		this.titlefont = titlefont;
		this.config = config;
	}

	public void render(boolean[] keystate, long[] keytime) {
		if (keystate[0] && keytime[0] != 0) {
			keytime[0] = 0;
			config.setBga((config.getBga() + 1) % BGA.length);
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
		shape.begin(ShapeType.Filled);
		shape.setColor(Color.BLACK);
		shape.rect(100, 200, 400, 400);
		shape.end();
		shape.begin(ShapeType.Line);
		shape.setColor(Color.PINK);
		shape.rect(100, 200, 400, 400);
		shape.rect(150, 250, 55, 95);
		shape.rect(180, 350, 55, 95);
		shape.rect(210, 250, 55, 95);
		shape.rect(240, 350, 55, 95);
		shape.rect(270, 250, 55, 95);
		shape.rect(300, 350, 55, 95);
		shape.rect(330, 250, 55, 95);
		shape.end();

		sprite.begin();

		titlefont.setColor(Color.WHITE);
		titlefont.draw(sprite, BGA[config.getBga()], 130, 250);
		titlefont.draw(sprite, "- DURATION + ", 240, 480);
		titlefont.draw(sprite, String.valueOf(config.getGreenvalue()), 300, 510);
		titlefont.draw(sprite, "- JUDGE TIMING + ", 270, 250);
		titlefont.draw(sprite, config.getJudgetiming() + " ms", 330, 220);
		sprite.end();
	}

}
