package bms.player.beatoraja.skin.property;

import static bms.player.beatoraja.skin.SkinProperty.*;

import bms.player.beatoraja.BMSResource;
import bms.player.beatoraja.ScoreData;
import bms.player.beatoraja.PlayConfig;
import bms.player.beatoraja.config.SkinConfiguration;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.select.bar.Bar;
import bms.player.beatoraja.select.bar.SongBar;
import bms.player.beatoraja.song.SongData;

public class FloatPropertyFactory {

	public static FloatProperty getFloatProperty(int optionid) {
		FloatProperty result = null;
		if (optionid == SLIDER_MUSICSELECT_POSITION) {
			result = (state) -> (state instanceof MusicSelector
					? ((MusicSelector) state).getBarRender().getSelectedPosition()
					: 0);
		}
		if (optionid == BARGRAPH_SCORERATE) {
			result = (state) -> (state.getScoreDataProperty().getRate());
		}
		if (optionid == BARGRAPH_SCORERATE_FINAL) {
			result = (state) -> (state.getScoreDataProperty().getNowRate());
		}
		if (optionid == BARGRAPH_BESTSCORERATE_NOW) {
			result = (state) -> (state.getScoreDataProperty().getNowBestScoreRate());
		}
		if (optionid == BARGRAPH_BESTSCORERATE) {
			result = (state) -> (state.getScoreDataProperty().getBestScoreRate());
		}
		if (optionid == BARGRAPH_TARGETSCORERATE_NOW) {
			result = (state) -> (state.getScoreDataProperty().getNowRivalScoreRate());
		}
		if (optionid == BARGRAPH_TARGETSCORERATE) {
			result = (state) -> (state.getScoreDataProperty().getRivalScoreRate());
		}
		if (optionid == BARGRAPH_LOAD_PROGRESS) {
			result = (state) -> {
				final BMSResource resource = state.main.getPlayerResource().getBMSResource();
				return resource.isBGAOn()
						? (resource.getBGAProcessor().getProgress() + resource.getAudioDriver().getProgress()) / 2
						: resource.getAudioDriver().getProgress();
			};
		}
		if (optionid == SLIDER_MUSIC_PROGRESS || optionid == BARGRAPH_MUSIC_PROGRESS) {
			result = (state) -> {
				if (state instanceof BMSPlayer) {
					if (state.main.isTimerOn(TIMER_PLAY)) {
						return Math.min((float) state.main.getNowTime(TIMER_PLAY) / ((BMSPlayer) state).getPlaytime(),
								1);
					}
				}
				return 0;
			};
		}
		if (optionid == SLIDER_LANECOVER || optionid == SLIDER_LANECOVER2) {
			result = (state) -> {
				if (state instanceof BMSPlayer) {
					final PlayConfig pc = ((BMSPlayer) state).getLanerender().getPlayConfig();
					if (pc.isEnablelanecover()) {
						float lane = pc.getLanecover();
						if (pc.isEnablelift()) {
							lane = lane * (1 - pc.getLift());
						}
						return lane;
					}
				}
				return 0;
			};
		}

		if (result == null) {
			result = getFloatProperty0(optionid);
		}

		return result;
	}

	private static FloatProperty getFloatProperty0(int optionid) {
		switch (optionid) {
		case SLIDER_MUSICSELECT_POSITION:
			return (state) -> ((state instanceof MusicSelector)
					? ((MusicSelector) state).getBarRender().getSelectedPosition()
					: 0);
		case SLIDER_SKINSELECT_POSITION:
			return (state) -> ((state instanceof SkinConfiguration)
					? ((SkinConfiguration) state).getSkinSelectPosition() : 0);
		case SLIDER_MASTER_VOLUME:
			return (state) -> (state.main.getConfig().getSystemvolume());
		case SLIDER_KEY_VOLUME:
			return (state) -> (state.main.getConfig().getKeyvolume());
		case SLIDER_BGM_VOLUME:
			return (state) -> (state.main.getConfig().getBgvolume());
		case BARGRAPH_LEVEL:
			return getLevelRate(-1);
		case BARGRAPH_LEVEL_BEGINNER:
			return getLevelRate(1);
		case BARGRAPH_LEVEL_NORMAL:
			return getLevelRate(2);
		case BARGRAPH_LEVEL_HYPER:
			return getLevelRate(3);
		case BARGRAPH_LEVEL_ANOTHER:
			return getLevelRate(4);
		case BARGRAPH_LEVEL_INSANE:
			return getLevelRate(5);
		case BARGRAPH_RATE_PGREAT:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
					if (selected instanceof SongBar) {
						ScoreData score = selected.getScore();
						return score != null
								? ((float) (score.getEpg() + score.getLpg()))
										/ ((SongBar) selected).getSongData().getNotes()
								: 0;
					}
				}
				return 0;
			};
		case BARGRAPH_RATE_GREAT:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
					if (selected instanceof SongBar) {
						ScoreData score = selected.getScore();
						return score != null
								? ((float) (score.getEgr() + score.getLgr()))
										/ ((SongBar) selected).getSongData().getNotes()
								: 0;
					}
				}
				return 0;
			};
		case BARGRAPH_RATE_GOOD:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
					if (selected instanceof SongBar) {
						ScoreData score = selected.getScore();
						return score != null
								? ((float) (score.getEgd() + score.getLgd()))
										/ ((SongBar) selected).getSongData().getNotes()
								: 0;
					}
				}
				return 0;
			};
		case BARGRAPH_RATE_BAD:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
					if (selected instanceof SongBar) {
						ScoreData score = selected.getScore();
						return score != null
								? ((float) (score.getEbd() + score.getLbd()))
										/ ((SongBar) selected).getSongData().getNotes()
								: 0;
					}
				}
				return 0;
			};
		case BARGRAPH_RATE_POOR:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
					if (selected instanceof SongBar) {
						ScoreData score = selected.getScore();
						return score != null
								? ((float) (score.getEpr() + score.getLpr()))
										/ ((SongBar) selected).getSongData().getNotes()
								: 0;
					}
				}
				return 0;
			};
		case BARGRAPH_RATE_MAXCOMBO:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
					if (selected instanceof SongBar) {
						ScoreData score = selected.getScore();
						return score != null
								? ((float) score.getCombo()) / ((SongBar) selected).getSongData().getNotes()
								: 0;
					}
				}
				return 0;
			};
		case BARGRAPH_RATE_EXSCORE:
			return (state) -> {
				if (state instanceof MusicSelector) {
					final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
					if (selected instanceof SongBar) {
						ScoreData score = selected.getScore();
						return score != null
								? ((float) score.getExscore()) / ((SongBar) selected).getSongData().getNotes() / 2
								: 0;
					}
				}
				return 0;
			};
		}
		return null;
	}
	
	public static FloatWriter getFloatWriter(int optionid) {
		FloatWriter result = null;
		
		if(result == null) {
			result = getFloatWriter0(optionid);
		}
		
		return result;
	}
	
	private static FloatWriter getFloatWriter0(int optionid) {
		switch(optionid) {
		case SLIDER_MUSICSELECT_POSITION:
			return (state, value) -> {
				if(state instanceof MusicSelector) {
					final MusicSelector select = (MusicSelector) state;
					select.selectedBarMoved();
					select.getBarRender().setSelectedPosition(value);
				}
			};
			case SLIDER_MASTER_VOLUME:
				return (state, value) -> {
					state.main.getConfig().setSystemvolume(value);					
				};
			case SLIDER_KEY_VOLUME:
				return (state, value) -> {
					state.main.getConfig().setKeyvolume(value);					
				};
			case SLIDER_BGM_VOLUME:
				return (state, value) -> {
					state.main.getConfig().setBgvolume(value);					
				};
		}
		return null;		
	}


	private static FloatProperty getLevelRate(final int difficulty) {
		return (state) -> {
			if (state instanceof MusicSelector) {
				final Bar selected = ((MusicSelector) state).getBarRender().getSelected();
				if (selected instanceof SongBar && ((SongBar) selected).getSongData() != null) {
					SongData sd = ((SongBar) selected).getSongData();
					if (difficulty >= 0 && sd.getDifficulty() != difficulty) {
						return 0;
					}
					int maxLevel = 0;
					switch (sd.getMode()) {
					case 5:
					case 10:
						maxLevel = 9;
					case 7:
					case 14:
						maxLevel = 12;
					case 9:
						maxLevel = 50;
					case 25:
					case 50:
						maxLevel = 10;
					}
					if (maxLevel > 0) {
						return (float) sd.getLevel() / maxLevel;
					}
				}
			}
			return 0;
		};
	}

}
