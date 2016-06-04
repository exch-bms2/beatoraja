package bms.player.beatoraja.decide;

import java.io.File;
import java.io.IOException;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.skin.LR2DecideSkinLoader;
import bms.player.beatoraja.skin.SkinImage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * 曲決定部分。
 * 
 * @author exch
 */
public class MusicDecide extends MainState {

	private MainController main;
	private PlayerResource resource;

	public MusicDecide(MainController main) {
		this.main = main;
	}

	public void create(PlayerResource resource) {
		this.resource = resource;

		if (new File("skin/decide.wav").exists()) {
			Gdx.audio.newSound(Gdx.files.internal("skin/decide.wav")).play();
		}

		if (resource.getConfig().getLr2decideskin() != null) {
			LR2DecideSkinLoader loader = new LR2DecideSkinLoader();
			try {
				setSkin(loader.loadMusicDecideSkin(new File(resource.getConfig().getLr2decideskin()), resource
						.getConfig().getLr2decideskinoption()));
			} catch (IOException e) {
				e.printStackTrace();
				setSkin(new MusicDecideSkin());
			}
		} else {
			setSkin(new MusicDecideSkin());
		}

		if(resource.getCourseBMSModels() != null) {
			getSkin().getTitle().setText(resource.getCoursetitle());
			getSkin().getGenre().setText("");
			getSkin().getArtist().setText("");
		} else {
			getSkin().setText(resource.getBMSModel());
		}
	}

	public void render() {
		SpriteBatch sprite = main.getSpriteBatch();
		long nowtime = getNowTime();
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
		getSkin().drawAllObjects(sprite, nowtime);
		sprite.end();

		sprite.begin();
		for (SkinImage part : getSkin().getSkinPart()) {
			int[] op = part.getOption();
			if (true) {
				Rectangle r = part.getDestination(nowtime);
				if (r != null) {
					sprite.draw(part.getImage(nowtime), r.x, r.y, r.width, r.height);
				}
			}
		}
		sprite.end();

		if (nowtime > 1500) {
			main.changeState(MainController.STATE_PLAYBMS);
		}
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
}
