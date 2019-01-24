package bms.player.beatoraja.decide;

import bms.player.beatoraja.*;
import bms.player.beatoraja.MainState.MainStateType;
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

		main.getPlayerResource().setOrgGaugeOption(main.getPlayerResource().getPlayerConfig().getGauge());
	}

	public void render() {
		long nowtime = main.getNowTime();
        if(nowtime >getSkin().getInput()){
        	main.switchTimer(TIMER_STARTINPUT, true);
        }
		if (main.isTimerOn(TIMER_FADEOUT)) {
			if (main.getNowTime(TIMER_FADEOUT) > getSkin().getFadeout()) {
				main.changeState(cancel ? MainStateType.MUSICSELECT : MainStateType.PLAY);
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
			if (input.getKeystate()[0] || input.getKeystate()[2] || input.getKeystate()[4] || input.getKeystate()[6] || input.isEnterPressed()) {
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
}
