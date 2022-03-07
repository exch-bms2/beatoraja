package bms.player.beatoraja.decide;

import bms.player.beatoraja.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyBoardInputProcesseor.ControlKeys;
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
		
		loadSkin(SkinType.DECIDE);

		main.getPlayerResource().setOrgGaugeOption(main.getPlayerResource().getPlayerConfig().getGauge());
	}

	public void prepare() {
		super.prepare();
		setSound(SOUND_DECIDE, "decide.wav", SoundType.BGM, false);
		play(SOUND_DECIDE);
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
			if (input.getKeyState(0) || input.getKeyState(2) || input.getKeyState(4) || input.getKeyState(6) || input.isControlKeyPressed(ControlKeys.ENTER)) {
				main.setTimerOn(TIMER_FADEOUT);
			}
			if (input.isControlKeyPressed(ControlKeys.ESCAPE) || (input.startPressed() && input.isSelectPressed())) {
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
