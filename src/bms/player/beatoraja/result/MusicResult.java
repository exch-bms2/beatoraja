package bms.player.beatoraja.result;

import java.io.File;

import bms.player.beatoraja.MainController;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * リザルト
 * 
 * @author exch
 */
public class MusicResult extends ApplicationAdapter {

	private MainController main;

	private ShapeRenderer shape;
	private SpriteBatch sprite;
	private BitmapFont titlefont;
	private String title;

	public MusicResult(MainController main) {
		this.main = main;
	}

	private long time = 0;
	
	public void create() {
		shape = new ShapeRenderer();
		sprite = new SpriteBatch();
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
				Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();		
		parameter.size = 24;	
		title = "result";
		parameter.characters = title;
		titlefont = generator.generateFont(parameter);
		time = System.currentTimeMillis();
	}
	
	public void render() {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		final float w = 1280;
		final float h = 720;

		sprite.begin();
		titlefont.setColor(Color.WHITE);
		titlefont.draw(sprite,title,  w /2, h / 2);
		sprite.end();
		
		if(System.currentTimeMillis() > time + 1500) {
			main.changeState(MainController.STATE_SELECTMUSIC, null);			
		}
	}
}
