package bms.player.beatoraja.select;

import static bms.player.beatoraja.select.MusicSelectKeyProperty.MusicSelectKey.*;

public enum MusicSelectKeyProperty {

	BEAT_7K(new MusicSelectKey[][]{
        {PLAY, FOLDER_OPEN}, {FOLDER_CLOSE}, {PRACTICE, FOLDER_OPEN}, {FOLDER_CLOSE}
        , {FOLDER_OPEN, AUTO}, {FOLDER_CLOSE},{FOLDER_OPEN, REPLAY}, {UP}, {DOWN}    		
	},new MusicSelectKey[][]{},new MusicSelectKey[][]{},new MusicSelectKey[][]{}),
	POPN_9K(new MusicSelectKey[][]{
        {AUTO}, {}, {FOLDER_CLOSE}, {DOWN}
        , {PLAY}, {UP},{PRACTICE, FOLDER_OPEN}, {}, {REPLAY}
	},new MusicSelectKey[][]{},new MusicSelectKey[][]{},new MusicSelectKey[][]{}),
	BEAT_14K(new MusicSelectKey[][]{
        {PLAY, FOLDER_OPEN}, {FOLDER_CLOSE}, {PRACTICE, FOLDER_OPEN}, {FOLDER_CLOSE}
        , {FOLDER_OPEN, AUTO}, {FOLDER_CLOSE},{FOLDER_OPEN, REPLAY}, {UP}, {DOWN},
        {PLAY, FOLDER_OPEN}, {FOLDER_CLOSE}, {PRACTICE, FOLDER_OPEN}, {FOLDER_CLOSE}
        , {FOLDER_OPEN, AUTO}, {FOLDER_CLOSE},{FOLDER_OPEN, REPLAY}, {UP}, {DOWN},
	},new MusicSelectKey[][]{},new MusicSelectKey[][]{},new MusicSelectKey[][]{}),
	;
	
	private final MusicSelectKey[][] select;
	private final MusicSelectKey[][] option;
	private final MusicSelectKey[][] assist;
	private final MusicSelectKey[][] config;
	
	private MusicSelectKeyProperty(MusicSelectKey[][] select,MusicSelectKey[][] option,MusicSelectKey[][] assist,MusicSelectKey[][] config) {
		this.select = select;
		this.option = option;
		this.assist =assist;
		this.config = config;
	}
	
	public boolean isPressed(boolean[] keystate, long[] keytime, MusicSelectKey code, boolean resetState) {
		final MusicSelectKey[][] keyassign = select;
		for (int i = 0; i < keyassign.length; i++) {
			for (MusicSelectKey index : keyassign[i]) {
				if (code == index && keystate[i]) {
					if (resetState) {
						if (keytime[i] != 0) {
							keytime[i] = 0;
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
        PLAY,AUTO,REPLAY,UP,DOWN,FOLDER_OPEN,FOLDER_CLOSE,PRACTICE;;    	
    }
}
