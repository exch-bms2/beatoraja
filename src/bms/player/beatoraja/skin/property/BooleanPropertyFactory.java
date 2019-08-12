package bms.player.beatoraja.skin.property;

import static bms.player.beatoraja.ClearType.AssistEasy;
import static bms.player.beatoraja.ClearType.Easy;
import static bms.player.beatoraja.ClearType.ExHard;
import static bms.player.beatoraja.ClearType.Failed;
import static bms.player.beatoraja.ClearType.FullCombo;
import static bms.player.beatoraja.ClearType.Hard;
import static bms.player.beatoraja.ClearType.LightAssistEasy;
import static bms.player.beatoraja.ClearType.Max;
import static bms.player.beatoraja.ClearType.NoPlay;
import static bms.player.beatoraja.ClearType.Normal;
import static bms.player.beatoraja.ClearType.Perfect;
import static bms.player.beatoraja.skin.SkinProperty.*;

import bms.model.Mode;
import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.ScoreDataProperty;
import bms.player.beatoraja.IRScoreData.SongTrophy;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.play.JudgeManager;
import bms.player.beatoraja.play.GrooveGauge.Gauge;
import bms.player.beatoraja.result.AbstractResult;
import bms.player.beatoraja.result.CourseResult;
import bms.player.beatoraja.result.MusicResult;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.select.bar.Bar;
import bms.player.beatoraja.select.bar.DirectoryBar;
import bms.player.beatoraja.select.bar.GradeBar;
import bms.player.beatoraja.select.bar.SelectableBar;
import bms.player.beatoraja.select.bar.SongBar;
import bms.player.beatoraja.skin.SkinObject;
import bms.player.beatoraja.song.SongData;

public class BooleanPropertyFactory {

	public static BooleanProperty getBooleanProperty(int optionid) {
		BooleanProperty result = null;
		final int id = Math.abs(optionid);

		if (id == OPTION_GAUGE_GROOVE) {
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_NO_STATIC) {
				@Override
				public boolean get(MainState state) {
					int type = Integer.MIN_VALUE;
					if (state instanceof BMSPlayer) {
						type = ((BMSPlayer) state).getGauge().getType();
					} else if (state instanceof AbstractResult) {
						type = ((AbstractResult) state).getGaugeType();
					}
					if (type != Integer.MIN_VALUE)
						return type <= 2;
					return false;
				}
			};
		}
		if (id == OPTION_GAUGE_HARD) {
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_NO_STATIC) {
				@Override
				public boolean get(MainState state) {
					int type = Integer.MIN_VALUE;
					if (state instanceof BMSPlayer) {
						type = ((BMSPlayer) state).getGauge().getType();
					} else if (state instanceof AbstractResult) {
						type = ((AbstractResult) state).getGaugeType();
					}
					if (type != Integer.MIN_VALUE)
						return type >= 3;
					return false;
				}
			};
		}
		if (id == OPTION_GAUGE_EX) {
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_NO_STATIC) {
				@Override
				public boolean get(MainState state) {
					int type = Integer.MIN_VALUE;
					if (state instanceof BMSPlayer) {
						type = ((BMSPlayer) state).getGauge().getType();
					} else if (state instanceof AbstractResult) {
						type = ((AbstractResult) state).getGaugeType();
					}
					if (type != Integer.MIN_VALUE)
						return type == 0 || type == 1 || type == 4 || type == 5 || type == 7 || type == 8;
					return false;
				}
			};
		}

		if (id == OPTION_BGAOFF || id == OPTION_BGAON) {
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					return id == OPTION_BGAON ? state.main.getPlayerResource().getBMSResource().isBGAOn()
							: !state.main.getPlayerResource().getBMSResource().isBGAOn();
				}
			};
		}
		if (id >= OPTION_7KEYSONG && id <= OPTION_9KEYSONG) {
			final Mode[] modes = { Mode.BEAT_7K, Mode.BEAT_5K, Mode.BEAT_14K, Mode.BEAT_10K, Mode.POPN_9K };
			result = new ModeDrawCondition(modes[id - OPTION_7KEYSONG]);
		}
		if (id == OPTION_24KEYSONG) {
			result = new ModeDrawCondition(Mode.KEYBOARD_24K);
		}
		if (id == OPTION_24KEYDPSONG) {
			result = new ModeDrawCondition(Mode.KEYBOARD_24K_DOUBLE);
		}

		if (id >= OPTION_JUDGE_VERYHARD && id <= OPTION_JUDGE_VERYEASY) {
			final int judgerank = id - OPTION_JUDGE_VERYHARD;
			final int[] judges = { 10, 35, 60, 85, 110, Integer.MAX_VALUE };
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					final SongData model = state.main.getPlayerResource().getSongdata();
					return model != null && (model.getJudge() == judgerank
							|| (model.getJudge() >= judges[judgerank] && model.getJudge() < judges[judgerank + 1]));
				}
			};
		}

		if (id >= OPTION_DIFFICULTY0 && id <= OPTION_DIFFICULTY5) {
			final int difficulty = id - OPTION_DIFFICULTY0;
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					final SongData model = state.main.getPlayerResource().getSongdata();
					return model != null && (difficulty == 0 ? model.getDifficulty() <= 0 || model.getDifficulty() > 5
							: model.getDifficulty() == difficulty);
				}
			};
		}

		if (id >= OPTION_1P_0_9 && id <= OPTION_1P_100) {
			final float low = (id - OPTION_1P_0_9) * 0.1f;
			final float high = (id - OPTION_1P_0_9 + 1) * 0.1f;
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_ON_RESULT) {
				@Override
				public boolean get(MainState state) {
					if (state instanceof BMSPlayer) {
						final Gauge gauge = ((BMSPlayer) state).getGauge().getGauge();
						return gauge.getValue() >= low * gauge.getProperty().max
								&& gauge.getValue() < high * gauge.getProperty().max;
					}
					return false;
				}
			};
		}

		if (id >= OPTION_AAA && id <= OPTION_F) {
			final int[] values = { 0, 6, 9, 12, 15, 18, 21, 24 };
			final int low = values[OPTION_F - id];
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_ON_RESULT) {
				@Override
				public boolean get(MainState state) {
					return state.getScoreDataProperty().qualifyRank(low);
				}
			};
		}
		if (id >= OPTION_BEST_AAA_1P && id <= OPTION_BEST_F_1P) {
			final int[] values = { 0, 6, 9, 12, 15, 18, 21, 24, 28 };
			final int low = values[OPTION_BEST_F_1P - id];
			final int high = values[OPTION_BEST_F_1P - id + 1];
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_ON_RESULT) {

				@Override
				public boolean get(MainState state) {
					final ScoreDataProperty score = state.getScoreDataProperty();
					return score.qualifyBestRank(low) && (high > 27 ? true : !score.qualifyBestRank(high));
				}

			};
		}
		if (id >= OPTION_1P_AAA && id <= OPTION_1P_F) {
			result = new NowRankDrawCondition(OPTION_1P_F - id);
		}
		if (id >= OPTION_RESULT_AAA_1P && id <= OPTION_RESULT_F_1P) {
			result = new NowRankDrawCondition(OPTION_RESULT_F_1P - id);
		}
		if (id >= OPTION_NOW_AAA_1P && id <= OPTION_NOW_F_1P) {
			result = new NowRankDrawCondition(OPTION_NOW_F_1P - id);
		}
		if (id >= OPTION_PERFECT_EXIST && id <= OPTION_MISS_EXIST) {
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_ON_RESULT) {
				private final int judge = id - OPTION_PERFECT_EXIST;

				@Override
				public boolean get(MainState state) {
					return state.getJudgeCount(judge, true) + state.getJudgeCount(judge, false) > 0;
				}
			};
		}

		if (id == OPTION_1P_PERFECT) {
			result = new NowJudgeDrawCondition(0, 0);
		}
		if (id == OPTION_1P_EARLY) {
			result = new NowJudgeDrawCondition(0, 1);
		}
		if (id == OPTION_1P_LATE) {
			result = new NowJudgeDrawCondition(0, 2);
		}
		if (id == OPTION_2P_PERFECT) {
			result = new NowJudgeDrawCondition(1, 0);
		}
		if (id == OPTION_2P_EARLY) {
			result = new NowJudgeDrawCondition(1, 1);
		}
		if (id == OPTION_2P_LATE) {
			result = new NowJudgeDrawCondition(1, 2);
		}
		if (id == OPTION_3P_PERFECT) {
			result = new NowJudgeDrawCondition(2, 0);
		}
		if (id == OPTION_3P_EARLY) {
			result = new NowJudgeDrawCondition(2, 1);
		}
		if (id == OPTION_3P_LATE) {
			result = new NowJudgeDrawCondition(2, 2);
		}

		if (id == OPTION_CLEAR_EASY) {
			result = new TrophyDrawCondition(SongTrophy.EASY);
		}
		if (id == OPTION_CLEAR_GROOVE) {
			result = new TrophyDrawCondition(SongTrophy.GROOVE);
		}
		if (id == OPTION_CLEAR_HARD) {
			result = new TrophyDrawCondition(SongTrophy.HARD);
		}
		if (id == OPTION_CLEAR_EXHARD) {
			result = new TrophyDrawCondition(SongTrophy.EXHARD);
		}
		if (id == OPTION_CLEAR_NORMAL) {
			result = new TrophyDrawCondition(SongTrophy.NORMAL);
		}
		if (id == OPTION_CLEAR_MIRROR) {
			result = new TrophyDrawCondition(SongTrophy.MIRROR);
		}
		if (id == OPTION_CLEAR_RANDOM) {
			result = new TrophyDrawCondition(SongTrophy.RANDOM);
		}
		if (id == OPTION_CLEAR_RRANDOM) {
			result = new TrophyDrawCondition(SongTrophy.R_RANDOM);
		}
		if (id == OPTION_CLEAR_SRANDOM) {
			result = new TrophyDrawCondition(SongTrophy.S_RANDOM);
		}
		if (id == OPTION_CLEAR_SPIRAL) {
			result = new TrophyDrawCondition(SongTrophy.SPIRAL);
		}
		if (id == OPTION_CLEAR_HRANDOM) {
			result = new TrophyDrawCondition(SongTrophy.H_RANDOM);
		}
		if (id == OPTION_CLEAR_ALLSCR) {
			result = new TrophyDrawCondition(SongTrophy.ALL_SCR);
		}
		if (id == OPTION_CLEAR_EXRANDOM) {
			result = new TrophyDrawCondition(SongTrophy.EX_RANDOM);
		}
		if (id == OPTION_CLEAR_EXSRANDOM) {
			result = new TrophyDrawCondition(SongTrophy.EX_S_RANDOM);
		}
		if (id == OPTION_REPLAYDATA) {
			result = new ReplayDrawCondition(0, 0);
		} else if (id == OPTION_REPLAYDATA2) {
			result = new ReplayDrawCondition(1, 0);
		} else if (id == OPTION_REPLAYDATA3) {
			result = new ReplayDrawCondition(2, 0);
		} else if (id == OPTION_REPLAYDATA4) {
			result = new ReplayDrawCondition(3, 0);
		} else if (id == OPTION_NO_REPLAYDATA) {
			result = new ReplayDrawCondition(0, 1);
		} else if (id == OPTION_NO_REPLAYDATA2) {
			result = new ReplayDrawCondition(1, 1);
		} else if (id == OPTION_NO_REPLAYDATA3) {
			result = new ReplayDrawCondition(2, 1);
		} else if (id == OPTION_NO_REPLAYDATA4) {
			result = new ReplayDrawCondition(3, 1);
		} else if (id == OPTION_REPLAYDATA_SAVED) {
			result = new ReplayDrawCondition(0, 2);
		} else if (id == OPTION_REPLAYDATA2_SAVED) {
			result = new ReplayDrawCondition(1, 2);
		} else if (id == OPTION_REPLAYDATA3_SAVED) {
			result = new ReplayDrawCondition(2, 2);
		} else if (id == OPTION_REPLAYDATA4_SAVED) {
			result = new ReplayDrawCondition(3, 2);
		}

		if (result == null) {
			result = getBooleanProperty0(id);
		}

		if (result != null && optionid < 0) {
			final BooleanProperty dc = result;
			result = new BooleanProperty() {
				@Override
				public boolean isStatic(MainState state) {
					return dc.isStatic(state);
				}

				public boolean get(MainState state) {
					return !dc.get(state);
				}
			};
		}
		
		return result;
	}

	private static BooleanProperty getBooleanProperty0(int optionid) {
		switch (optionid) {
		case OPTION_STAGEFILE:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					return state.main.getPlayerResource().getBMSResource().getStagefile() != null;
				}
			};
		case OPTION_NO_STAGEFILE:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					return state.main.getPlayerResource().getBMSResource().getStagefile() == null;
				}
			};
		case OPTION_BACKBMP:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					return state.main.getPlayerResource().getBMSResource().getBackbmp() != null;
				}
			};
		case OPTION_NO_BACKBMP:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					return state.main.getPlayerResource().getBMSResource().getBackbmp() == null;
				}
			};
		case OPTION_BANNER:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					return state.main.getPlayerResource().getBMSResource().getBanner() != null;
				}
			};
		case OPTION_NO_BANNER:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					return state.main.getPlayerResource().getBMSResource().getBanner() == null;
				}
			};
		case OPTION_NO_TEXT:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					final SongData model = state.main.getPlayerResource().getSongdata();
					return model != null && !model.hasDocument();
				}
			};
		case OPTION_TEXT:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					final SongData model = state.main.getPlayerResource().getSongdata();
					return model != null && model.hasDocument();
				}
			};
		case OPTION_NO_LN:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					final SongData model = state.main.getPlayerResource().getSongdata();
					return model != null && !model.hasAnyLongNote();
				}
			};
		case OPTION_LN:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					final SongData model = state.main.getPlayerResource().getSongdata();
					return model != null && model.hasAnyLongNote();
				}
			};
		case OPTION_NO_BGA:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					final SongData model = state.main.getPlayerResource().getSongdata();
					return model != null && !model.hasBGA();
				}
			};
		case OPTION_BGA:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					final SongData model = state.main.getPlayerResource().getSongdata();
					return model != null && model.hasBGA();
				}
			};
		case OPTION_NO_RANDOMSEQUENCE:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					final SongData model = state.main.getPlayerResource().getSongdata();
					return model != null && !model.hasRandomSequence();
				}
			};
		case OPTION_RANDOMSEQUENCE:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					final SongData model = state.main.getPlayerResource().getSongdata();
					return model != null && model.hasRandomSequence();
				}
			};
		case OPTION_NO_BPMCHANGE:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					final SongData model = state.main.getPlayerResource().getSongdata();
					return model != null && model.getMinbpm() == model.getMaxbpm();
				}
			};
		case OPTION_BPMCHANGE:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					final SongData model = state.main.getPlayerResource().getSongdata();
					return model != null && model.getMinbpm() < model.getMaxbpm();
				}
			};
		case OPTION_BPMSTOP:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					final SongData model = state.main.getPlayerResource().getSongdata();
					return model != null && model.isBpmstop();
				}
			};
		case OPTION_OFFLINE:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_ALL) {
				@Override
				public boolean get(MainState state) {
					return state.main.getIRStatus().length == 0;
				}
			};
		case OPTION_ONLINE:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_ALL) {
				@Override
				public boolean get(MainState state) {
					return state.main.getIRStatus().length > 0;
				}
			};
		case OPTION_TABLE_SONG:
			return new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					return state.main.getPlayerResource().getTablename().length() != 0;
				}
			};
		case OPTION_PANEL1:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getPanelState() == 1 : false));
		case OPTION_PANEL2:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getPanelState() == 2 : false));
		case OPTION_PANEL3:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getPanelState() == 3 : false));
		case OPTION_SONGBAR:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSelectedBar() instanceof SongBar : false));
		case OPTION_FOLDERBAR:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSelectedBar() instanceof DirectoryBar : false));
		case OPTION_GRADEBAR:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSelectedBar() instanceof GradeBar : false));
		case OPTION_PLAYABLEBAR:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> {
						if(state instanceof MusicSelector) {
							Bar selected = ((MusicSelector) state).getSelectedBar();
							return ((selected instanceof SongBar) && ((SongBar)selected).getSongData().getPath() != null) ||
									((selected instanceof GradeBar) && ((GradeBar)selected).existsAllSongs()); 
						}
						return false;
					});
		case OPTION_SELECT_REPLAYDATA:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSelectedReplay() == 0 : false));
		case OPTION_SELECT_REPLAYDATA2:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSelectedReplay() == 1 : false));
		case OPTION_SELECT_REPLAYDATA3:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSelectedReplay() == 2 : false));
		case OPTION_SELECT_REPLAYDATA4:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getSelectedReplay() == 3 : false));
		case OPTION_GRADEBAR_CLASS:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).existsConstraint(CourseData.CourseDataConstraint.CLASS) : false));
		case OPTION_GRADEBAR_MIRROR:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).existsConstraint(CourseData.CourseDataConstraint.MIRROR) : false));
		case OPTION_GRADEBAR_RANDOM:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).existsConstraint(CourseData.CourseDataConstraint.RANDOM) : false));
		case OPTION_GRADEBAR_NOSPEED:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).existsConstraint(CourseData.CourseDataConstraint.NO_SPEED) : false));
		case OPTION_GRADEBAR_NOGOOD:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).existsConstraint(CourseData.CourseDataConstraint.NO_GOOD) : false));
		case OPTION_GRADEBAR_NOGREAT:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).existsConstraint(CourseData.CourseDataConstraint.NO_GREAT) : false));
		case OPTION_GRADEBAR_GAUGE_LR2:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).existsConstraint(CourseData.CourseDataConstraint.GAUGE_LR2) : false));
		case OPTION_GRADEBAR_GAUGE_5KEYS:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).existsConstraint(CourseData.CourseDataConstraint.GAUGE_5KEYS) : false));
		case OPTION_GRADEBAR_GAUGE_7KEYS:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).existsConstraint(CourseData.CourseDataConstraint.GAUGE_7KEYS) : false));
		case OPTION_GRADEBAR_GAUGE_9KEYS:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).existsConstraint(CourseData.CourseDataConstraint.GAUGE_9KEYS) : false));
		case OPTION_GRADEBAR_GAUGE_24KEYS:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).existsConstraint(CourseData.CourseDataConstraint.GAUGE_24KEYS) : false));
		case OPTION_GRADEBAR_LN:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).existsConstraint(CourseData.CourseDataConstraint.LN) : false));
		case OPTION_GRADEBAR_CN:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).existsConstraint(CourseData.CourseDataConstraint.CN) : false));
		case OPTION_GRADEBAR_HCN:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).existsConstraint(CourseData.CourseDataConstraint.HCN) : false));
		case OPTION_NOT_COMPARE_RIVAL:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getRival() == null : false));
		case OPTION_COMPARE_RIVAL:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof MusicSelector) ? ((MusicSelector) state).getRival() != null : false));
		case OPTION_SELECT_BAR_NOT_PLAYED:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> {
						if(state instanceof MusicSelector) {
							Bar current = ((MusicSelector) state).getSelectedBar();
							IRScoreData score = ((MusicSelector) state).getSelectedBar().getScore();
							return (current instanceof SongBar || current instanceof GradeBar)
									&& (score == null || (score != null && score.getClear() == NoPlay.id));
						}
						return false;
					});
		case OPTION_SELECT_BAR_FAILED:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> {
						if(state instanceof MusicSelector) {
							IRScoreData score = ((MusicSelector) state).getSelectedBar().getScore();
							return score != null ? score.getClear() == Failed.id : false; 
						}
						return false;
					});
		case OPTION_SELECT_BAR_ASSIST_EASY_CLEARED:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> {
						if(state instanceof MusicSelector) {
							IRScoreData score = ((MusicSelector) state).getSelectedBar().getScore();
							return score != null ? score.getClear() == AssistEasy.id : false; 
						}
						return false;
					});
		case OPTION_SELECT_BAR_LIGHT_ASSIST_EASY_CLEARED:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> {
						if(state instanceof MusicSelector) {
							IRScoreData score = ((MusicSelector) state).getSelectedBar().getScore();
							return score != null ? score.getClear() == LightAssistEasy.id : false; 
						}
						return false;
					});
		case OPTION_SELECT_BAR_EASY_CLEARED:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> {
						if(state instanceof MusicSelector) {
							IRScoreData score = ((MusicSelector) state).getSelectedBar().getScore();
							return score != null ? score.getClear() == Easy.id : false; 
						}
						return false;
					});
		case OPTION_SELECT_BAR_NORMAL_CLEARED:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> {
						if(state instanceof MusicSelector) {
							IRScoreData score = ((MusicSelector) state).getSelectedBar().getScore();
							return score != null ? score.getClear() == Normal.id : false; 
						}
						return false;
					});
		case OPTION_SELECT_BAR_HARD_CLEARED:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> {
						if(state instanceof MusicSelector) {
							IRScoreData score = ((MusicSelector) state).getSelectedBar().getScore();
							return score != null ? score.getClear() == Hard.id : false; 
						}
						return false;
					});
		case OPTION_SELECT_BAR_EXHARD_CLEARED:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> {
						if(state instanceof MusicSelector) {
							IRScoreData score = ((MusicSelector) state).getSelectedBar().getScore();
							return score != null ? score.getClear() == ExHard.id : false; 
						}
						return false;
					});
		case OPTION_SELECT_BAR_FULL_COMBO_CLEARED:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> {
						if(state instanceof MusicSelector) {
							IRScoreData score = ((MusicSelector) state).getSelectedBar().getScore();
							return score != null ? score.getClear() == FullCombo.id : false; 
						}
						return false;
					});
		case OPTION_SELECT_BAR_PERFECT_CLEARED:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> {
						if(state instanceof MusicSelector) {
							IRScoreData score = ((MusicSelector) state).getSelectedBar().getScore();
							return score != null ? score.getClear() == Perfect.id : false; 
						}
						return false;
					});
		case OPTION_SELECT_BAR_MAX_CLEARED:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> {
						if(state instanceof MusicSelector) {
							IRScoreData score = ((MusicSelector) state).getSelectedBar().getScore();
							return score != null ? score.getClear() == Max.id : false; 
						}
						return false;
					});
		case OPTION_AUTOPLAYON:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).main.getPlayerResource().getPlayMode() == PlayMode.AUTOPLAY : false));
		case OPTION_AUTOPLAYOFF:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).main.getPlayerResource().getPlayMode() != PlayMode.AUTOPLAY : false));
		case OPTION_REPLAY_OFF:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).main.getPlayerResource().getPlayMode() == PlayMode.PLAY || ((BMSPlayer) state).main.getPlayerResource().getPlayMode() == PlayMode.PRACTICE : false));
		case OPTION_REPLAY_PLAYING:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).main.getPlayerResource().getPlayMode().isReplayMode() : false));
		case OPTION_STATE_PRACTICE:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).getState() == BMSPlayer.STATE_PRACTICE : false));
		case OPTION_NOW_LOADING:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).getState() == BMSPlayer.STATE_PRELOAD : false));
		case OPTION_LOADED:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).getState() != BMSPlayer.STATE_PRELOAD : false));
		case OPTION_LANECOVER1_CHANGING:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> (state.main.getInputProcessor().startPressed() || state.main.getInputProcessor().isSelectPressed()));
		case OPTION_LANECOVER1_ON:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).getLanerender().getPlayConfig().isEnablelanecover() : false));
		case OPTION_LIFT1_ON:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).getLanerender().getPlayConfig().isEnablelift() : false));
		case OPTION_HIDDEN1_ON:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).getLanerender().getPlayConfig().isEnablehidden() : false));
		case OPTION_1P_BORDER_OR_MORE:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof BMSPlayer) ? ((BMSPlayer) state).getGauge().getGauge().isQualified() : false));
		case OPTION_UPDATE_SCORE:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof AbstractResult) ? ((AbstractResult) state).getNewScore().getExscore() > ((AbstractResult) state).getOldScore().getExscore() : false));
		case OPTION_DRAW_SCORE:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof AbstractResult) ? ((AbstractResult) state).getNewScore().getExscore() == ((AbstractResult) state).getOldScore().getExscore() : false));
		case OPTION_UPDATE_MAXCOMBO:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof AbstractResult) ? ((AbstractResult) state).getNewScore().getCombo() > ((AbstractResult) state).getOldScore().getCombo() : false));
		case OPTION_DRAW_MAXCOMBO:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof AbstractResult) ? ((AbstractResult) state).getNewScore().getCombo() == ((AbstractResult) state).getOldScore().getCombo() : false));
		case OPTION_UPDATE_MISSCOUNT:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof AbstractResult) ? ((AbstractResult) state).getNewScore().getMinbp() < ((AbstractResult) state).getOldScore().getMinbp() : false));
		case OPTION_DRAW_MISSCOUNT:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof AbstractResult) ? ((AbstractResult) state).getNewScore().getMinbp() == ((AbstractResult) state).getOldScore().getMinbp() : false));
		case OPTION_UPDATE_SCORERANK:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> (state.getScoreDataProperty().getNowRate() > state.getScoreDataProperty().getBestScoreRate()));
		case OPTION_DRAW_SCORERANK:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> (state.getScoreDataProperty().getNowRate() == state.getScoreDataProperty().getBestScoreRate()));
		case OPTION_UPDATE_TARGET:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof AbstractResult) ? state.main.getPlayerResource().getScoreData().getExscore() > state.getScoreDataProperty().getRivalScore() : false));
		case OPTION_DRAW_TARGET:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC,
					(state) -> ((state instanceof AbstractResult) ? state.main.getPlayerResource().getScoreData().getExscore() == state.getScoreDataProperty().getRivalScore() : false));
		case OPTION_DISABLE_SAVE_SCORE:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC, (state) -> (!state.main.getPlayerResource().isUpdateScore()));
		case OPTION_ENABLE_SAVE_SCORE:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC, (state) -> (state.main.getPlayerResource().isUpdateScore()));
		case OPTION_RESULT_CLEAR:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC, (state) -> {
				final IRScoreData score = state.main.getPlayerResource().getScoreData();
				final IRScoreData cscore = state.main.getPlayerResource().getCourseScoreData();
				return score.getClear() != Failed.id && (cscore == null || cscore.getClear() != Failed.id);
			});
		case OPTION_RESULT_FAIL:
			return new DrawProperty(DrawProperty.TYPE_NO_STATIC, (state) -> {
				final IRScoreData score = state.main.getPlayerResource().getScoreData();
				final IRScoreData cscore = state.main.getPlayerResource().getCourseScoreData();
				return score.getClear() == Failed.id || (cscore != null && cscore.getClear() == Failed.id);
			});
		}
		
		return null;
	}

	private static class DrawProperty implements BooleanProperty {

		public final int type;
		public final BProperty bool;

		public static final int TYPE_NO_STATIC = 0;
		public static final int TYPE_STATIC_WITHOUT_MUSICSELECT = 1;
		public static final int TYPE_STATIC_ON_RESULT = 2;
		public static final int TYPE_STATIC_ALL = 3;

		public DrawProperty(int type, BProperty bool) {
			this.type = type;
			this.bool = bool;
		}

		@Override
		public boolean isStatic(MainState state) {
			switch (type) {
			case TYPE_NO_STATIC:
				return false;
			case TYPE_STATIC_WITHOUT_MUSICSELECT:
				return !(state instanceof MusicSelector);
			case TYPE_STATIC_ON_RESULT:
				return (state instanceof MusicResult) || (state instanceof CourseResult);
			case TYPE_STATIC_ALL:
				return true;
			}
			return false;
		}

		@Override
		public boolean get(MainState state) {
			return bool.get(state);
		}
	}

	private interface BProperty {
		public boolean get(MainState state);
	}

	private static abstract class DrawConditionProperty implements BooleanProperty {

		public final int type;

		public static final int TYPE_NO_STATIC = 0;
		public static final int TYPE_STATIC_WITHOUT_MUSICSELECT = 1;
		public static final int TYPE_STATIC_ON_RESULT = 2;
		public static final int TYPE_STATIC_ALL = 3;

		public DrawConditionProperty(int type) {
			this.type = type;
		}

		@Override
		public boolean isStatic(MainState state) {
			switch (type) {
			case TYPE_NO_STATIC:
				return false;
			case TYPE_STATIC_WITHOUT_MUSICSELECT:
				return !(state instanceof MusicSelector);
			case TYPE_STATIC_ON_RESULT:
				return (state instanceof MusicResult) || (state instanceof CourseResult);
			case TYPE_STATIC_ALL:
				return true;
			}
			return false;
		}

	}

	private static class NowJudgeDrawCondition extends DrawConditionProperty {

		private final int player;
		private final int type;

		public NowJudgeDrawCondition(int player, int type) {
			super(TYPE_NO_STATIC);
			this.player = player;
			this.type = type;
		}

		@Override
		public boolean get(MainState state) {
			if (state instanceof BMSPlayer) {
				JudgeManager judge = ((BMSPlayer) state).getJudgeManager();
				if (type == 0) {
					return judge.getNowJudge().length > player && judge.getNowJudge()[player] == 1;
				} else if (type == 1) {
					return judge.getNowJudge().length > player && judge.getNowJudge()[player] > 1
							&& judge.getRecentJudgeTiming()[player] > 0;
				} else {
					return judge.getNowJudge().length > player && judge.getNowJudge()[player] > 1
							&& judge.getRecentJudgeTiming()[player] < 0;
				}
			}
			return false;
		}

	}

	private static class ModeDrawCondition extends DrawConditionProperty {

		private final Mode mode;

		public ModeDrawCondition(Mode mode) {
			super(TYPE_STATIC_WITHOUT_MUSICSELECT);
			this.mode = mode;
		}

		@Override
		public boolean get(MainState state) {
			final SongData model = state.main.getPlayerResource().getSongdata();
			return model != null && model.getMode() == mode.id;
		}

	}

	private static class NowRankDrawCondition extends DrawConditionProperty {

		private final int low;
		private final int high;

		public NowRankDrawCondition(int rank) {
			super(TYPE_STATIC_ON_RESULT);
			final int[] values = { 0, 6, 9, 12, 15, 18, 21, 24, 28 };
			low = values[rank];
			high = values[rank + 1];
		}

		@Override
		public boolean get(MainState state) {
			final ScoreDataProperty score = state.getScoreDataProperty();
			return score.qualifyNowRank(low) && (high > 27 ? true : !score.qualifyNowRank(high));
		}

	}

	private static class TrophyDrawCondition extends DrawConditionProperty {

		private final SongTrophy trophy;

		public TrophyDrawCondition(SongTrophy trophy) {
			super(TYPE_STATIC_WITHOUT_MUSICSELECT);
			this.trophy = trophy;
		}

		@Override
		public boolean get(MainState state) {
			final IRScoreData score = state.getScoreDataProperty().getScoreData();
			return score != null && score.getTrophy() != null && score.getTrophy().indexOf(trophy.character) >= 0;
		}

	}

	private static class ReplayDrawCondition extends DrawConditionProperty {

		private final int index;
		private final int type;

		public ReplayDrawCondition(int index, int type) {
			super(TYPE_NO_STATIC);
			this.index = index;
			this.type = type;
		}

		@Override
		public boolean get(MainState state) {
			if (state instanceof MusicSelector) {
				final Bar current = ((MusicSelector) state).getSelectedBar();
				return (current instanceof SelectableBar)
						&& ((SelectableBar) current).getExistsReplayData().length > index
						&& (type == 0 ? ((SelectableBar) current).getExistsReplayData()[index]
								: !((SelectableBar) current).getExistsReplayData()[index]);
			} else if (state instanceof AbstractResult) {
				return ((AbstractResult) state).getReplayStatus(index) == (type == 0 ? AbstractResult.ReplayStatus.EXIST
						: (type == 1 ? AbstractResult.ReplayStatus.NOT_EXIST : AbstractResult.ReplayStatus.SAVED));
			}
			return false;
		}
	}
}
