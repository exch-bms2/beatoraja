package bms.player.beatoraja.select;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import bms.player.beatoraja.Config;

/**
 * アシストオプション描画、入力受付用クラス
 * 
 * @author exch
 */
public class AssistOptionRenderer {
	
	private ShapeRenderer shape;
	private SpriteBatch sprite;
	private BitmapFont titlefont;

	private Config config;

	public AssistOptionRenderer(ShapeRenderer shape, SpriteBatch sprite, BitmapFont titlefont, Config config) {
		this.sprite = sprite;
		this.shape = shape;
		this.titlefont = titlefont;
		this.config = config;
	}

	public void render(boolean[] keystate, long[] keytime) {
		if (keystate[0] && keytime[0] != 0) {
			keytime[0] = 0;
			config.setExpandjudge(!config.isExpandjudge());
		}
		if (keystate[1] && keytime[1] != 0) {
			keytime[1] = 0;
			config.setConstant(!config.isConstant());
		}
		if (keystate[2] && keytime[2] != 0) {
			keytime[2] = 0;
			config.setShowjudgearea(!config.isShowjudgearea());
		}
		if (keystate[3] && keytime[3] != 0) {
			keytime[3] = 0;
			config.setLegacynote(!config.isLegacynote());
		}
		if (keystate[4] && keytime[4] != 0) {
			keytime[4] = 0;
			config.setMarkprocessednote(!config.isMarkprocessednote());
		}
		if (keystate[5] && keytime[5] != 0) {
			keytime[5] = 0;
			config.setBpmguide(!config.isBpmguide());
		}
		if (keystate[6] && keytime[6] != 0) {
			keytime[6] = 0;
			config.setNomine(!config.isNomine());
		}
		shape.begin(ShapeType.Filled);
		shape.setColor(Color.BLACK);
		shape.rect(800, 200, 400, 400);
		shape.end();
		shape.begin(ShapeType.Line);
		shape.setColor(Color.CYAN);
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

		titlefont.setColor(config.isConstant() ? Color.WHITE : Color.valueOf("444444"));
		titlefont.draw(sprite, "CONSTANT", 810, 490);
		titlefont.setColor(config.isLegacynote() ? Color.WHITE : Color.valueOf("444444"));
		titlefont.draw(sprite, "LEGACY NOTE", 900, 520);
		titlefont.setColor(config.isShowjudgearea() ? Color.WHITE : Color.valueOf("444444"));
		titlefont.draw(sprite, "JUDGE AREA", 850, 220);
		titlefont.setColor(config.isBpmguide() ? Color.WHITE : Color.valueOf("444444"));
		titlefont.draw(sprite, "BPM GUIDE", 1000, 490);
		titlefont.setColor(config.isMarkprocessednote() ? Color.WHITE : Color.valueOf("444444"));
		titlefont.draw(sprite, "MARK", 1000, 220);
		titlefont.setColor(config.isExpandjudge() ? Color.WHITE : Color.valueOf("444444"));
		titlefont.draw(sprite, "EXPAND JUDGE", 790, 250);
		titlefont.setColor(config.isNomine() ? Color.WHITE : Color.valueOf("444444"));
		titlefont.draw(sprite, "NO MINE", 1030, 250);
		sprite.end();
	}

}
