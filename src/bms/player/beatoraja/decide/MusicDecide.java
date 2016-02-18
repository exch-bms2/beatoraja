package bms.player.beatoraja.decide;

import java.io.File;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainController.PlayerResource;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

/**
 * 曲決定部分。
 * 
 * @author exch
 */
public class MusicDecide extends ApplicationAdapter{
	
	private MainController main;
	private PlayerResource resource;

	private BitmapFont titlefont;
	private String title;

	public MusicDecide(MainController main) {
		this.main = main;
	}

	private long time = 0;
	
	public void create(PlayerResource resource) {
		this.resource = resource;
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
				Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();		
		parameter.size = 24;	
		title = resource.getBMSModel().getFullTitle();
		parameter.characters = title;
		titlefont = generator.generateFont(parameter);
		time = System.currentTimeMillis();
		
		if(new File("skin/decide.wav").exists()) {
			Gdx.audio.newSound(Gdx.files.internal("skin/decide.wav")).play();
		}
	}
	
	public void render() {
		SpriteBatch sprite = main.getSpriteBatch();
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		final float w = 1280;
		final float h = 720;

		if(resource.getBGAManager().getStagefileData() != null) {
			sprite.begin();
			Texture bgatex = new Texture(resource.getBGAManager().getStagefileData());
			sprite.draw(bgatex, 0, 0, w, h);
			sprite.end();
			bgatex.dispose();
		}
		sprite.begin();
		titlefont.setColor(Color.WHITE);
		titlefont.draw(sprite,title,  w /2, h / 2);
		sprite.end();
		
		if(System.currentTimeMillis() > time + 1500) {
			main.changeState(MainController.STATE_PLAYBMS, resource);			
		}
	}
}
