package bms.player.beatoraja.decide;

import java.io.File;

import bms.player.beatoraja.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.skin.*;

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
		
		setSound(SOUND_DECIDE, "decide.wav", SoundType.BGM, false);
		play(SOUND_DECIDE);

		loadSkin(SkinType.DECIDE);
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
