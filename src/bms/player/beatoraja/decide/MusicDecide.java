package bms.player.beatoraja.decide;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;

import bms.player.beatoraja.Config.SkinConfig;
import bms.player.beatoraja.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.lr2.*;

import com.badlogic.gdx.math.Rectangle;

import static bms.player.beatoraja.Resolution.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

/**
 * 曲決定部分。
 * 
 * @author exch
 */
public class MusicDecide extends MainState {

	private boolean cancel;

	public static final int SOUND_DECIDE = 0;
	
	public MusicDecide(MainController main) {
		super(main);
	}

	public void create() {
		cancel = false;
		final PlayerResource resource = getMainController().getPlayerResource();
		
		setSound(SOUND_DECIDE, resource.getConfig().getBgmpath() + File.separatorChar + "decide.wav", false);
		play(SOUND_DECIDE);

		try {
			SkinConfig sc = resource.getConfig().getSkin()[6];
			if (sc.getPath().endsWith(".json")) {
				SkinLoader sl = new SkinLoader(RESOLUTION[resource.getConfig().getResolution()]);
				setSkin(sl.loadDecideSkin(Paths.get(sc.getPath()), sc.getProperty()));
			} else {
				LR2SkinHeaderLoader loader = new LR2SkinHeaderLoader();
				SkinHeader header = loader.loadSkin(Paths.get(sc.getPath()), this, sc.getProperty());
				Rectangle srcr = RESOLUTION[header.getResolution()];
				Rectangle dstr = RESOLUTION[resource.getConfig().getResolution()];
				LR2DecideSkinLoader dloader = new LR2DecideSkinLoader(srcr.width, srcr.height, dstr.width, dstr.height);
				setSkin(dloader.loadMusicDecideSkin(Paths.get(sc.getPath()).toFile(), this, header, loader.getOption(),
						sc.getProperty()));
			}
		} catch (Throwable e) {
			e.printStackTrace();
			SkinLoader sl = new SkinLoader(RESOLUTION[resource.getConfig().getResolution()]);
			setSkin(sl.loadDecideSkin(Paths.get("skin/default/decide.json"), new HashMap()));
		}
	}

	public void render() {
		long nowtime = getNowTime();

		if (getTimer()[TIMER_FADEOUT] != Long.MIN_VALUE) {
			if (nowtime > getTimer()[TIMER_FADEOUT] + getSkin().getFadeout()) {
				getMainController()
						.changeState(cancel ? MainController.STATE_SELECTMUSIC : MainController.STATE_PLAYBMS);
			}
		} else {
			if (nowtime > getSkin().getScene()) {
				getTimer()[TIMER_FADEOUT] = nowtime;
			}
		}
	}

	public void input() {
		long nowtime = getNowTime();

		if (getTimer()[TIMER_FADEOUT] == Long.MIN_VALUE && nowtime > getSkin().getInput()) {
			BMSPlayerInputProcessor input = getMainController().getInputProcessor();
			if (input.getKeystate()[0] || input.getKeystate()[2] || input.getKeystate()[4] || input.getKeystate()[6]) {
				getTimer()[TIMER_FADEOUT] = nowtime;
			}
			if (input.isExitPressed()) {
				cancel = true;
				getTimer()[TIMER_FADEOUT] = nowtime;
			}
		}
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	public String getTextValue(int id) {
		final PlayerResource resource = getMainController().getPlayerResource();
		if (resource.getCourseBMSModels() != null) {
			switch (id) {
			case STRING_TITLE:
			case STRING_FULLTITLE:
				return resource.getCoursetitle();
			}
			return "";
		}
		return super.getTextValue(id);
	}
}
