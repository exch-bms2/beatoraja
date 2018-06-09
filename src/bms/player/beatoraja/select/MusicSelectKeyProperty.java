package bms.player.beatoraja.select;

import static bms.player.beatoraja.select.MusicSelectKeyProperty.MusicSelectKey.*;

import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.keyData;

public enum MusicSelectKeyProperty {

	BEAT_7K(new MusicSelectKey[][]{
        {PLAY, FOLDER_OPEN, OPTION1_DOWN, JUDGEWINDOW_UP, BGA_DOWN}, 
        {FOLDER_CLOSE, OPTION1_UP, CONSTANT}, 
        {PRACTICE, FOLDER_OPEN, GAUGE_DOWN, JUDGEAREA}, 
        {FOLDER_CLOSE, OPTIONDP_DOWN, LEGACYNOTE, DURATION_DOWN}, 
        {FOLDER_OPEN, AUTO, HSFIX_DOWN, MARKNOTE, JUDGETIMING_DOWN}, 
        {NEXT_REPLAY, OPTION2_UP, BPMGUIDE ,DURATION_UP},
        {FOLDER_OPEN, REPLAY, OPTION2_DOWN, NOMINE, JUDGETIMING_UP}, 
        {UP, TARGET_UP}, 
        {DOWN, TARGET_DOWN}    		
	}),
	POPN_9K(new MusicSelectKey[][]{
        {AUTO, OPTION1_DOWN, JUDGEWINDOW_UP, BGA_DOWN}, 
        {OPTION1_UP, CONSTANT}, 
        {FOLDER_CLOSE, GAUGE_DOWN, JUDGEAREA}, 
        {DOWN, OPTIONDP_DOWN, LEGACYNOTE, DURATION_DOWN}, 
        {PLAY, FOLDER_OPEN, HSFIX_DOWN, MARKNOTE, JUDGETIMING_DOWN}, 
        {UP, OPTION2_UP, BPMGUIDE ,DURATION_UP},
        {PRACTICE, FOLDER_OPEN, OPTION2_DOWN, NOMINE, JUDGETIMING_UP}, 
        {TARGET_UP, NEXT_REPLAY}, 
        {REPLAY, TARGET_DOWN}
	}),
	BEAT_14K(new MusicSelectKey[][]{
        {PLAY, FOLDER_OPEN, OPTION1_DOWN, JUDGEWINDOW_UP, BGA_DOWN}, 
        {FOLDER_CLOSE, OPTION1_UP, CONSTANT}, 
        {PRACTICE, FOLDER_OPEN, GAUGE_DOWN, JUDGEAREA}, 
        {FOLDER_CLOSE, OPTIONDP_DOWN, LEGACYNOTE, DURATION_DOWN}, 
        {FOLDER_OPEN, AUTO, HSFIX_DOWN, MARKNOTE, JUDGETIMING_DOWN}, 
        {NEXT_REPLAY, OPTION2_UP, BPMGUIDE ,DURATION_UP},
        {FOLDER_OPEN, REPLAY, OPTION2_DOWN, NOMINE, JUDGETIMING_UP}, 
        {UP, TARGET_UP}, 
        {DOWN, TARGET_DOWN},
        {PLAY, FOLDER_OPEN, OPTION1_DOWN, JUDGEWINDOW_UP, BGA_DOWN}, 
        {FOLDER_CLOSE, OPTION1_UP, CONSTANT}, 
        {PRACTICE, FOLDER_OPEN, GAUGE_DOWN, JUDGEAREA}, 
        {FOLDER_CLOSE, OPTIONDP_DOWN, LEGACYNOTE, DURATION_DOWN}, 
        {FOLDER_OPEN, AUTO, HSFIX_DOWN, MARKNOTE, JUDGETIMING_DOWN}, 
        {NEXT_REPLAY, OPTION2_UP, BPMGUIDE ,DURATION_UP},
        {FOLDER_OPEN, REPLAY, OPTION2_DOWN, NOMINE, JUDGETIMING_UP}, 
        {UP, TARGET_UP}, 
        {DOWN, TARGET_DOWN}    		
	}),
	;
	
	private final MusicSelectKey[][] assign;
	
	private MusicSelectKeyProperty(MusicSelectKey[][] assign) {
		this.assign = assign;
	}
	
	public boolean isPressed(BMSPlayerInputProcessor input, MusicSelectKey code, boolean resetState) {
		for (int i = 0; i < assign.length; i++) {
			for (MusicSelectKey index : assign[i]) {
				if (code == index && keyData.getKeyState(i)) {
					if (resetState) {
						if (keyData.getKeyTime(i) != 0) {
							keyData.resetKeyTime(i);
							return true;
						}
						return false;
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
        BGA_UP, BGA_DOWN, DURATION_UP, DURATION_DOWN, JUDGETIMING_UP, JUDGETIMING_DOWN, NEXT_REPLAY
        ;;    	
    }
}
