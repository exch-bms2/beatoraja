package bms.player.beatoraja.decide;

import java.io.File;
import java.io.IOException;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.skin.LR2DecideSkinLoader;
import bms.player.beatoraja.skin.SkinImage;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * 曲決定部分。
 * 
 * @author exch
 */
public class MusicDecide extends ApplicationAdapter {

	private MainController main;
	private PlayerResource resource;

	private MusicDecideSkin skin;

	public MusicDecide(MainController main) {
		this.main = main;
	}

	private long time = 0;

	public void create(PlayerResource resource) {
		this.resource = resource;
		time = System.currentTimeMillis();

		if (new File("skin/decide.wav").exists()) {
			Gdx.audio.newSound(Gdx.files.internal("skin/decide.wav")).play();
		}

		if (resource.getConfig().getLr2decideskin() != null) {
			LR2DecideSkinLoader loader = new LR2DecideSkinLoader();
			try {
				skin = loader.loadMusicDecideSkin(new File(resource.getConfig().getLr2decideskin()), resource
						.getConfig().getLr2decideskinoption());
			} catch (IOException e) {
				e.printStackTrace();
				skin = new MusicDecideSkin();
			}
		} else {
			skin = new MusicDecideSkin();
		}

		if(resource.getCourseBMSModels() != null) {
			skin.getTitle().setText(resource.getCoursetitle());
			skin.getGenre().setText("");
			skin.getArtist().setText("");
		} else {
			skin.setText(resource.getBMSModel());
		}
	}

	public void render() {
		SpriteBatch sprite = main.getSpriteBatch();
		long nowtime = System.currentTimeMillis() - time;
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		final float w = 1280;
		final float h = 720;

		if (resource.getCourseBMSModels() == null && resource.getBGAManager().getStagefileData() != null) {
			sprite.begin();
			sprite.draw(resource.getBGAManager().getStagefileData(), 0, 0, w, h);
			sprite.end();
		}
		sprite.begin();
		skin.drawAllObjects(sprite, nowtime);
		sprite.end();

		sprite.begin();
		for (SkinImage part : skin.getSkinPart()) {
			int[] op = part.getOption();
			if (true) {
				Rectangle r = part.getDestination(System.currentTimeMillis() - time);
				if (r != null) {
					sprite.draw(part.getImage(System.currentTimeMillis() - time), r.x, r.y, r.width, r.height);
				}
			}
		}
		sprite.end();

		if (System.currentTimeMillis() > time + 1500) {
			main.changeState(MainController.STATE_PLAYBMS);
		}
	}
}
