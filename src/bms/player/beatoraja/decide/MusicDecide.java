package bms.player.beatoraja.decide;

import bms.player.beatoraja.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.keyData;
import bms.player.beatoraja.skin.*;

import static bms.player.beatoraja.skin.SkinProperty.*;

/**
 * �쎊黎뷴츣�깿�늽��
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

		main.getPlayerResource().setOrgGaugeOption(main.getPlayerResource().getPlayerConfig().getGauge());
	}

	public void render() {
		long nowtime = main.getNowTime();
        if(nowtime >getSkin().getInput()){
        	main.switchTimer(TIMER_STARTINPUT, true);
        }
		if (main.isTimerOn(TIMER_FADEOUT)) {
			if (main.getNowTime(TIMER_FADEOUT) > getSkin().getFadeout()) {
				main.changeState(cancel ? MainController.STATE_SELECTMUSIC : MainController.STATE_PLAYBMS);
			}
		} else {
			if (nowtime > getSkin().getScene()) {
				main.setTimerOn(TIMER_FADEOUT);
			}
		}
	}

	public void input() {
		if (!main.isTimerOn(TIMER_FADEOUT) && main.isTimerOn(TIMER_STARTINPUT)) {
			BMSPlayerInputProcessor input = main.getInputProcessor();
			if (keyData.getKeyState(0) || keyData.getKeyState(2) || keyData.getKeyState(4) || keyData.getKeyState(6) || input.isEnterPressed()) {
				input.setEnterPressed(false);
				main.setTimerOn(TIMER_FADEOUT);
			}
			if (input.isExitPressed() || (input.startPressed() && input.isSelectPressed())) {
				cancel = true;
				main.setTimerOn(TIMER_FADEOUT);
			}
		}
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	public String getTextValue(int id) {
		final PlayerResource resource = main.getPlayerResource();
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
