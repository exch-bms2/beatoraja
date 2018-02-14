package bms.player.beatoraja.decide;

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
		
		setSound(SOUND_DECIDE, "decide.wav", SoundType.BGM, false);
		play(SOUND_DECIDE);

		loadSkin(SkinType.DECIDE);
	}

	public void render() {
		long nowtime = getNowTime();
        if(nowtime >getSkin().getInput()){
			switchTimer(TIMER_STARTINPUT, true);
        }
		if (isTimerOn(TIMER_FADEOUT)) {
			if (getNowTime(TIMER_FADEOUT) > getSkin().getFadeout()) {
				getMainController()
						.changeState(cancel ? MainController.STATE_SELECTMUSIC : MainController.STATE_PLAYBMS);
			}
		} else {
			if (nowtime > getSkin().getScene()) {
				setTimerOn(TIMER_FADEOUT);
			}
		}
	}

	public void input() {
		if (!isTimerOn(TIMER_FADEOUT) && isTimerOn(TIMER_STARTINPUT)) {
			BMSPlayerInputProcessor input = getMainController().getInputProcessor();
			if (input.getKeystate()[0] || input.getKeystate()[2] || input.getKeystate()[4] || input.getKeystate()[6]) {
				setTimerOn(TIMER_FADEOUT);
			}
			if (input.isExitPressed() || (input.startPressed() && input.isSelectPressed())) {
				cancel = true;
				setTimerOn(TIMER_FADEOUT);
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
