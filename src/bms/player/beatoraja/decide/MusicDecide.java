package bms.player.beatoraja.decide;

import java.io.File;
import java.io.IOException;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.skin.LR2DecideSkinLoader;
import bms.player.beatoraja.skin.SkinObject;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.Rectangle;

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
	
	private MusicDecideSkin skin;

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
		parameter.characters += title + "段位認定 " + resource.getCoursetitle();
		titlefont = generator.generateFont(parameter);
		time = System.currentTimeMillis();
		
		if(new File("skin/decide.wav").exists()) {
			Gdx.audio.newSound(Gdx.files.internal("skin/decide.wav")).play();
		}
		
		skin = new MusicDecideSkin();
//		LR2DecideSkinLoader loader = new LR2DecideSkinLoader();
//		try {
//			skin = loader.loadMusicDecideSkin(new File("skin/Seraphic/Decide/[+]decide.csv"));
//		} catch(IOException e) {
//			e.printStackTrace();
//		}
	}
	
	public void render() {
		SpriteBatch sprite = main.getSpriteBatch();
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		final float w = 1280;
		final float h = 720;

		if(resource.getCourseBMSModels() != null) {
			sprite.begin();
			titlefont.setColor(Color.WHITE);
			titlefont.draw(sprite, "段位認定 " + resource.getCoursetitle(),  w /2, h / 2);
			sprite.end();						
		} else {
			if(resource.getBGAManager().getStagefileData() != null) {
				sprite.begin();
				sprite.draw(resource.getBGAManager().getStagefileData(), 0, 0, w, h);
				sprite.end();
			}
			sprite.begin();
			titlefont.setColor(Color.WHITE);
			titlefont.draw(sprite,title,  w /2, h / 2);
			sprite.end();			
		}
		
		sprite.begin();
		for (SkinObject part : skin.getSkinPart()) {
			int[] op = part.getOption();
 			if (part.getTiming() != 3 && (op.length == 0 || op[0] == 910)) {
 				Rectangle r = part.getDestination(System.currentTimeMillis() - time);
 				if(r != null) {
 					sprite.draw(part.getImage(), r.x, r.y, r.width, r.height);
 				}
			}
		}
		sprite.end();
		
		if(System.currentTimeMillis() > time + 1500) {
			main.changeState(MainController.STATE_PLAYBMS, resource);			
		}
	}
}
