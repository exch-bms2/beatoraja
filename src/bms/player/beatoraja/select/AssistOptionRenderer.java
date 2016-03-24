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
	
	// TODO HELL CHARGEはアシストオプションに含めない方がいいかも

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
		if (keystate[3] && keytime[3] != 0) {
			keytime[3] = 0;
			config.setLegacynote(!config.isLegacynote());
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
		shape.rect(100, 200, 400, 400);
		shape.end();
		shape.begin(ShapeType.Line);
		shape.setColor(Color.CYAN);
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

		titlefont.setColor(config.isConstant() ? Color.WHITE : Color.valueOf("444444"));
		titlefont.draw(sprite, "CONSTANT", 110, 490);
		titlefont.setColor(config.isLegacynote() ? Color.WHITE : Color.valueOf("444444"));
		titlefont.draw(sprite, "LEGACY NOTE", 200, 520);
		titlefont.setColor(config.isBpmguide() ? Color.WHITE : Color.valueOf("444444"));
		titlefont.draw(sprite, "BPM GUIDE", 300, 490);
		titlefont.setColor(config.isExpandjudge() ? Color.WHITE : Color.valueOf("444444"));
		titlefont.draw(sprite, "EXPAND JUDGE", 90, 220);
		titlefont.setColor(config.isNomine() ? Color.WHITE : Color.valueOf("444444"));
		titlefont.draw(sprite, "NO MINE", 330, 220);
		sprite.end();
	}

}
