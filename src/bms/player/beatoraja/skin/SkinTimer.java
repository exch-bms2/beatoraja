package bms.player.beatoraja.skin;

import static bms.player.beatoraja.skin.SkinProperty.OPTION_1P_100;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_1P_BORDER_OR_MORE;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_MUSIC_END;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PM_CHARA_1P_BAD;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PM_CHARA_1P_FEVER;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PM_CHARA_1P_GOOD;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PM_CHARA_1P_GREAT;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PM_CHARA_1P_NEUTRAL;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PM_CHARA_2P_BAD;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PM_CHARA_2P_GREAT;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PM_CHARA_2P_NEUTRAL;

public class SkinTimer {
	int timer;
	int setMotion;
	public SkinTimer(int timer, int setMotion) {
		this.timer = timer;
		this.setMotion = setMotion;
	}
	
	void TimerSetting(int motion, SkinOption skinOption, int side, int dsttimer) {
		if(setMotion != Integer.MIN_VALUE && setMotion == motion) {
			timer = dsttimer;
		} else if(setMotion == Integer.MIN_VALUE) {
			if(side != 2) {
				if(motion == 1) timer = TIMER_PM_CHARA_1P_NEUTRAL;
				else if(motion == 6) timer = TIMER_PM_CHARA_1P_FEVER;
				else if(motion == 7) timer = TIMER_PM_CHARA_1P_GREAT;
				else if(motion == 8) timer = TIMER_PM_CHARA_1P_GOOD;
				else if(motion == 10) timer = TIMER_PM_CHARA_1P_BAD;
				else if(motion >= 15 && motion <= 17) {
					timer = TIMER_MUSIC_END;
					if(motion == 15) {
						skinOption.setDstOpt1(OPTION_1P_BORDER_OR_MORE);
						skinOption.setDstOpt2( -OPTION_1P_100);
					}
					else if(motion == 16) skinOption.setDstOpt1(-OPTION_1P_BORDER_OR_MORE);	//LOSE
					else if(motion == 17) skinOption.setDstOpt1(OPTION_1P_100);	//FEVERWIN
				}
			} else {
				if(motion == 1) timer = TIMER_PM_CHARA_2P_NEUTRAL;
				else if(motion == 7) timer = TIMER_PM_CHARA_2P_GREAT;
				else if(motion == 10) timer = TIMER_PM_CHARA_2P_BAD;
				else if(motion == 15 || motion == 16) {
					timer = TIMER_MUSIC_END;
					if(motion == 15) skinOption.setDstOpt1(-OPTION_1P_BORDER_OR_MORE);	//WIN
					else if(motion == 16) skinOption.setDstOpt1( OPTION_1P_BORDER_OR_MORE);	//LOSE
				}
			}
		}
	}
	
}
