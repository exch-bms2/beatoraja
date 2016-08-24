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
	
	private Config config;

	public AssistOptionRenderer(Config config) {
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
	}

}
