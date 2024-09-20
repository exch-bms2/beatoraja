package bms.player.beatoraja.select;

import static bms.player.beatoraja.select.MusicSelectKeyProperty.MusicSelectKey.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;

public enum MusicSelectKeyProperty {

	BEAT_7K(new MusicSelectKey[][]{
        {PLAY, FOLDER_OPEN, OPTION1_DOWN, JUDGEWINDOW_UP, BGA_DOWN},
        {FOLDER_CLOSE, OPTION1_UP, CONSTANT, GAUGEAUTOSHIFT_DOWN},
        {PRACTICE, FOLDER_OPEN, GAUGE_DOWN, JUDGEAREA, NOTESDISPLAYTIMING_AUTOADJUST},
        {FOLDER_CLOSE, OPTIONDP_DOWN, LEGACYNOTE, DURATION_DOWN},
        {FOLDER_OPEN, AUTO, HSFIX_DOWN, MARKNOTE, NOTESDISPLAYTIMING_DOWN},
        {NEXT_REPLAY, OPTION2_UP, BPMGUIDE ,DURATION_UP},
        {FOLDER_OPEN, REPLAY, OPTION2_DOWN, NOMINE, NOTESDISPLAYTIMING_UP},
        {UP, TARGET_UP},
        {DOWN, TARGET_DOWN}
	}),
	POPN_9K(new MusicSelectKey[][]{
        {AUTO, OPTION1_DOWN, JUDGEWINDOW_UP, BGA_DOWN},
        {OPTION1_UP, CONSTANT, GAUGEAUTOSHIFT_DOWN},
        {FOLDER_CLOSE, GAUGE_DOWN, JUDGEAREA, NOTESDISPLAYTIMING_AUTOADJUST},
        {DOWN, OPTIONDP_DOWN, LEGACYNOTE, DURATION_DOWN},
        {PLAY, FOLDER_OPEN, HSFIX_DOWN, MARKNOTE, NOTESDISPLAYTIMING_DOWN},
        {UP, OPTION2_UP, BPMGUIDE ,DURATION_UP},
        {PRACTICE, FOLDER_OPEN, OPTION2_DOWN, NOMINE, NOTESDISPLAYTIMING_UP},
        {TARGET_UP, NEXT_REPLAY},
        {REPLAY, TARGET_DOWN}
	}),
	BEAT_14K(new MusicSelectKey[][]{
        {PLAY, FOLDER_OPEN, OPTION1_DOWN, JUDGEWINDOW_UP, BGA_DOWN},
        {FOLDER_CLOSE, OPTION1_UP, CONSTANT, GAUGEAUTOSHIFT_DOWN},
        {PRACTICE, FOLDER_OPEN, GAUGE_DOWN, JUDGEAREA, NOTESDISPLAYTIMING_AUTOADJUST},
        {FOLDER_CLOSE, OPTIONDP_DOWN, LEGACYNOTE, DURATION_DOWN},
        {FOLDER_OPEN, AUTO, HSFIX_DOWN, MARKNOTE, NOTESDISPLAYTIMING_DOWN},
        {NEXT_REPLAY, OPTION2_UP, BPMGUIDE ,DURATION_UP},
        {FOLDER_OPEN, REPLAY, OPTION2_DOWN, NOMINE, NOTESDISPLAYTIMING_UP},
        {UP, TARGET_UP},
        {DOWN, TARGET_DOWN},
        {PLAY, FOLDER_OPEN, OPTION1_DOWN, JUDGEWINDOW_UP, BGA_DOWN},
        {FOLDER_CLOSE, OPTION1_UP, CONSTANT, GAUGEAUTOSHIFT_DOWN},
        {PRACTICE, FOLDER_OPEN, GAUGE_DOWN, JUDGEAREA, NOTESDISPLAYTIMING_AUTOADJUST},
        {FOLDER_CLOSE, OPTIONDP_DOWN, LEGACYNOTE, DURATION_DOWN},
        {FOLDER_OPEN, AUTO, HSFIX_DOWN, MARKNOTE, NOTESDISPLAYTIMING_DOWN},
        {NEXT_REPLAY, OPTION2_UP, BPMGUIDE ,DURATION_UP},
        {FOLDER_OPEN, REPLAY, OPTION2_DOWN, NOMINE, NOTESDISPLAYTIMING_UP},
        {UP, TARGET_UP},
        {DOWN, TARGET_DOWN}
	}),
	;
	
	public static final MusicSelectKeyProperty[] values = MusicSelectKeyProperty.values();

	private final MusicSelectKey[][] assign;
	
	private MusicSelectKeyProperty(MusicSelectKey[][] assign) {
		this.assign = assign;
	}

    public int getAnalogChange(BMSPlayerInputProcessor input, MusicSelectKey code) {
        int dTicks = 0;
        for (int i = 0; i < assign.length; i++) {
            for (MusicSelectKey index : assign[i]) {
                if (code == index && input.isAnalogInput(i)) {
                    dTicks += input.getAnalogDiffAndReset(i, 200);
                }
            }
        }
        return dTicks;
    }

    public boolean isNonAnalogPressed(BMSPlayerInputProcessor input, MusicSelectKey code, boolean resetState) {
    	for (int i = 0; i < assign.length; i++) {
            for (MusicSelectKey index : assign[i]) {
                if (code == index && input.getKeyState(i)) {
                    if (input.isAnalogInput(i)) continue;
                    if (resetState) {
						return input.resetKeyChangedTime(i);
                    } else {
                        return true;
                    }

                }
            }
        }
        return false;
    }

	public boolean isPressed(BMSPlayerInputProcessor input, MusicSelectKey code, boolean resetState) {
		for (int i = 0; i < assign.length; i++) {
			for (MusicSelectKey index : assign[i]) {
				if (code == index && input.getKeyState(i)) {
					if (resetState) {
						return input.resetKeyChangedTime(i);
					} else {
						return true;
					}

				}
			}
		}
		return false;
	}

    public enum MusicSelectKey {
        PLAY,AUTO,REPLAY,UP,DOWN,FOLDER_OPEN,FOLDER_CLOSE,PRACTICE,
        OPTION1_UP, OPTION1_DOWN, GAUGE_UP, GAUGE_DOWN, OPTIONDP_UP, OPTIONDP_DOWN, HSFIX_UP, HSFIX_DOWN, OPTION2_UP, OPTION2_DOWN, TARGET_UP, TARGET_DOWN,
        JUDGEAREA, NOMINE, BPMGUIDE, LEGACYNOTE, CONSTANT, JUDGEWINDOW_UP, JUDGEWINDOW_DOWN, MARKNOTE,
        BGA_UP, BGA_DOWN, GAUGEAUTOSHIFT_UP, GAUGEAUTOSHIFT_DOWN, DURATION_UP, DURATION_DOWN, NOTESDISPLAYTIMING_UP, NOTESDISPLAYTIMING_DOWN, NOTESDISPLAYTIMING_AUTOADJUST, NEXT_REPLAY
        ;;
    }
}
