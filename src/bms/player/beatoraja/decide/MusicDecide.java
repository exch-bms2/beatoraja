package bms.player.beatoraja.decide;

import java.io.File;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainController.PlayerResource;
import bms.player.beatoraja.input.MusicSelectorInputProcessor;
import bms.player.lunaticrave2.SongData;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

/**
 * 曲決定部分。
 * 
 * @author exch
 */
public class MusicDecide extends ApplicationAdapter {
	
	// TODO 曲決定時からデータ読み込み開始

	private MainController main;
	private PlayerResource resource;

	private BitmapFont titlefont;
	private String title;

	public MusicDecide(MainController main, PlayerResource resource) {
		this.main = main;
		this.resource = resource;
	}

	private long time = 0;
	
	public void create() {
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
				Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();		
		parameter.size = 24;	
		title = "decide";
		parameter.characters = title;
		titlefont = generator.generateFont(parameter);
		time = System.currentTimeMillis();
	}
	
	public void render() {
		SpriteBatch sprite = main.getSpriteBatch();
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		final float w = 1280;
		final float h = 720;

		sprite.begin();
		if(resource.getBMSModel().getStagefile() != null) {
			
		}
		titlefont.setColor(Color.WHITE);
		titlefont.draw(sprite,title,  w /2, h / 2);
		sprite.end();
		
		if(System.currentTimeMillis() > time + 1500) {
			main.changeState(MainController.STATE_PLAYBMS, resource);			
		}
	}
}
