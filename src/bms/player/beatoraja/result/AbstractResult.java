package bms.player.beatoraja.result;

import static bms.player.beatoraja.ClearType.NoPlay;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_NO_REPLAYDATA;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_NO_REPLAYDATA2;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_NO_REPLAYDATA3;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_NO_REPLAYDATA4;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_REPLAYDATA;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_REPLAYDATA2;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_REPLAYDATA2_SAVED;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_REPLAYDATA3;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_REPLAYDATA3_SAVED;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_REPLAYDATA4;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_REPLAYDATA4_SAVED;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_REPLAYDATA_SAVED;

import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainState;

public abstract class AbstractResult extends MainState {

	/**
	 * 状態
	 */
	protected int state;

	public static final int STATE_OFFLINE = 0;
	public static final int STATE_IR_PROCESSING = 1;
	public static final int STATE_IR_FINISHED = 2;

	protected int next;

	protected int irrank;
	protected int irprevrank;
	protected int irtotal;

	protected ReplayStatus[] saveReplay = new ReplayStatus[REPLAY_SIZE];
	protected static final int REPLAY_SIZE = 4;

	public static final int SOUND_CLEAR = 0;
	public static final int SOUND_FAIL = 1;
	public static final int SOUND_CLOSE = 2;
	
	protected int gaugeType;

	public AbstractResult(MainController main) {
		super(main);
	}
	
	public boolean getBooleanValue(int id) {
		switch(id) {
		case OPTION_NO_REPLAYDATA:
			return saveReplay[0] == ReplayStatus.NOT_EXIST;
		case OPTION_NO_REPLAYDATA2:
			return saveReplay[1] == ReplayStatus.NOT_EXIST;
		case OPTION_NO_REPLAYDATA3:
			return saveReplay[2] == ReplayStatus.NOT_EXIST;
		case OPTION_NO_REPLAYDATA4:
			return saveReplay[3] == ReplayStatus.NOT_EXIST;
		case OPTION_REPLAYDATA:
			return saveReplay[0] == ReplayStatus.EXIST;
		case OPTION_REPLAYDATA2:
			return saveReplay[1] == ReplayStatus.EXIST;
		case OPTION_REPLAYDATA3:
			return saveReplay[2] == ReplayStatus.EXIST;
		case OPTION_REPLAYDATA4:
			return saveReplay[3] == ReplayStatus.EXIST;
		case OPTION_REPLAYDATA_SAVED:
			return saveReplay[0] == ReplayStatus.SAVED;
		case OPTION_REPLAYDATA2_SAVED:
			return saveReplay[1] == ReplayStatus.SAVED;
		case OPTION_REPLAYDATA3_SAVED:
			return saveReplay[2] == ReplayStatus.SAVED;
		case OPTION_REPLAYDATA4_SAVED:
			return saveReplay[3] == ReplayStatus.SAVED;
		}
		return super.getBooleanValue(id);
	}

	
	public enum ReplayAutoSaveConstraint {

		NOTHING {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return false;
			}
		},
		SCORE_UPDATE {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getExscore() > oldscore.getExscore();
			}
		},
		SCORE_UPDATE_OR_EQUAL {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getExscore() >= oldscore.getExscore();
			}
		},
		MISSCOUNT_UPDATE {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getMinbp() < oldscore.getMinbp() || oldscore.getClear() == NoPlay.id;
			}
		},
		MISSCOUNT_UPDATE_OR_EQUAL {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getMinbp() <= oldscore.getMinbp() || oldscore.getClear() == NoPlay.id;
			}
		},
		MAXCOMBO_UPDATE {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getCombo() > oldscore.getCombo();
			}
		},
		MAXCOMBO_UPDATE_OR_EQUAL {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getCombo() >= oldscore.getCombo();
			}
		},
		CLEAR_UPDATE {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getClear() > oldscore.getClear();
			}
		},
		CLEAR_UPDATE_OR_EQUAL {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getClear() >= oldscore.getClear();
			}
		},
		ANYONE_UPDATE {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getClear() > oldscore.getClear() || newscore.getCombo() > oldscore.getCombo() ||
						newscore.getMinbp() < oldscore.getMinbp() || newscore.getExscore() > oldscore.getExscore();
			}
		},
		ALWAYS {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return true;
			}
		};

		public abstract boolean isQualified(IRScoreData oldscore, IRScoreData newscore);

		public static ReplayAutoSaveConstraint get(int index) {
			if (index < 0 || index >= values().length) {
				return NOTHING;
			}
			return values()[index];
		}
	}
	
	public enum ReplayStatus {
		EXIST, NOT_EXIST, SAVED;
	}

	public int getGaugeType() {
		return gaugeType;
	}
}
