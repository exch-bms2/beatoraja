package bms.player.beatoraja.skin;

import static bms.player.beatoraja.skin.SkinProperty.*;

import bms.model.Mode;
import bms.player.beatoraja.BMSResource;
import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayConfig;
import bms.player.beatoraja.ScoreDataProperty;
import bms.player.beatoraja.IRScoreData.SongTrophy;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.play.GrooveGauge.Gauge;
import bms.player.beatoraja.play.JudgeManager;
import bms.player.beatoraja.play.LaneRenderer;
import bms.player.beatoraja.result.AbstractResult;
import bms.player.beatoraja.result.CourseResult;
import bms.player.beatoraja.result.MusicResult;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.select.bar.Bar;
import bms.player.beatoraja.select.bar.GradeBar;
import bms.player.beatoraja.skin.SkinObject.BooleanProperty;
import bms.player.beatoraja.skin.SkinObject.FloatProperty;
import bms.player.beatoraja.skin.SkinObject.FloatWriter;
import bms.player.beatoraja.skin.SkinObject.IntegerProperty;
import bms.player.beatoraja.skin.SkinObject.StringProperty;
import bms.player.beatoraja.song.SongData;

public class SkinPropertyMapper {

	public static int bombTimerId(int player, int key) {
		if (player < 2) {
			if (key < 10) {
				return TIMER_BOMB_1P_SCRATCH + key + player * 10;
			} else if (key < 100) {
				return TIMER_BOMB_1P_KEY10 + key - 10 + player * 100;
			}
		}
		return -1;
	}
	
	public static int holdTimerId(int player, int key) {
		if (player < 2) {
			if (key < 10) {
				return TIMER_HOLD_1P_SCRATCH + key + player * 10;
			} else if (key < 100) {
				return TIMER_HOLD_1P_KEY10 + key - 10 + player * 100;
			}
		}
		return -1;
	}

	public static int hcnActiveTimerId(int player, int key) {
		if (player < 2) {
			if (key < 10) {
				return TIMER_HCN_ACTIVE_1P_SCRATCH + key + player * 10;
			} else if (key < 100) {
				return TIMER_HCN_ACTIVE_1P_KEY10 + key - 10 + player * 100;
			}
		}
		return -1;
	}

	public static int hcnDamageTimerId(int player, int key) {
		if (player < 2) {
			if (key < 10) {
				return TIMER_HCN_DAMAGE_1P_SCRATCH + key + player * 10;
			} else if (key < 100) {
				return TIMER_HCN_DAMAGE_1P_KEY10 + key - 10 + player * 100;
			}
		}
		return -1;
	}

	public static int keyOnTimerId(int player, int key) {
		if (player < 2) {
			if (key < 10) {
				return TIMER_KEYON_1P_SCRATCH + key + player * 10;
			} else if (key < 100) {
				return TIMER_KEYON_1P_KEY10 + key - 10 + player * 100;
			}
		}
		return -1;
	}

	public static int keyOffTimerId(int player, int key) {
		if (player < 2) {
			if (key < 10) {
				return TIMER_KEYOFF_1P_SCRATCH + key + player * 10;
			} else if (key < 100) {
				return TIMER_KEYOFF_1P_KEY10 + key - 10 + player * 100;
			}
		}
		return -1;
	}


	public static int getKeyJudgeValuePlayer(int valueId) {
		if (valueId >= VALUE_JUDGE_1P_SCRATCH && valueId <= VALUE_JUDGE_2P_KEY9) {
			return (valueId - VALUE_JUDGE_1P_SCRATCH) / 10;
		} else {
			return (valueId - VALUE_JUDGE_1P_KEY10) / 100;
		}
	}

	public static int getKeyJudgeValueOffset(int valueId) {
		if (valueId >= VALUE_JUDGE_1P_SCRATCH && valueId <= VALUE_JUDGE_2P_KEY9) {
			return (valueId - VALUE_JUDGE_1P_SCRATCH) % 10;
		} else {
			return (valueId - VALUE_JUDGE_1P_KEY10) % 100 + 10;
		}
	}

	public static int keyJudgeValueId(int player, int key) {
		if (player < 2) {
			if (key < 10) {
				return VALUE_JUDGE_1P_SCRATCH + key + player * 10;
			} else if (key < 100) {
				return VALUE_JUDGE_1P_KEY10 + key - 10 + player * 100;
			}
		}
		return -1;
	}

	public static boolean isSkinSelectTypeId(int id) {
		return (id >= BUTTON_SKINSELECT_7KEY && id <= BUTTON_SKINSELECT_COURSE_RESULT)
				|| (id >= BUTTON_SKINSELECT_24KEY && id <= BUTTON_SKINSELECT_24KEY_BATTLE);
	}

	public static SkinType getSkinSelectType(int id) {
		if (id >= BUTTON_SKINSELECT_7KEY && id <= BUTTON_SKINSELECT_COURSE_RESULT) {
			return SkinType.getSkinTypeById(id - BUTTON_SKINSELECT_7KEY);
		} else if (id >= BUTTON_SKINSELECT_24KEY && id <= BUTTON_SKINSELECT_24KEY_BATTLE) {
			return SkinType.getSkinTypeById(id - BUTTON_SKINSELECT_24KEY + 16);
		} else {
			return null;
		}
	}

	public static int skinSelectTypeId(SkinType type) {
		if (type.getId() <= 15) {
			return BUTTON_SKINSELECT_7KEY + type.getId();
		} else {
			return BUTTON_SKINSELECT_24KEY + type.getId() - 16;
		}
	}

	public static boolean isSkinCustomizeButton(int id) {
		return id >= BUTTON_SKIN_CUSTOMIZE1 && id < BUTTON_SKIN_CUSTOMIZE10;
	}

	public static int getSkinCustomizeIndex(int id) {
		return id - BUTTON_SKIN_CUSTOMIZE1;
	}

	public static boolean isSkinCustomizeCategory(int id) {
		return id >= STRING_SKIN_CUSTOMIZE_CATEGORY1 && id <= STRING_SKIN_CUSTOMIZE_CATEGORY10;
	}

	public static int getSkinCustomizeCategoryIndex(int id) {
		return id - STRING_SKIN_CUSTOMIZE_CATEGORY1;
	}

	public static boolean isSkinCustomizeItem(int id) {
		return id >= STRING_SKIN_CUSTOMIZE_ITEM1 && id <= STRING_SKIN_CUSTOMIZE_ITEM10;
	}

	public static int getSkinCustomizeItemIndex(int id) {
		return id - STRING_SKIN_CUSTOMIZE_ITEM1;
	}
	
	public static BooleanProperty getBooleanProperty(int optionid) {
		// TODO 各Skinに分散するべき？
		BooleanProperty result = null;
		final int id = Math.abs(optionid);
		
		if(id == OPTION_GAUGE_GROOVE) {
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_NO_STATIC) {
				@Override
				public boolean get(MainState state) {
					int type = Integer.MIN_VALUE;
					if(state instanceof BMSPlayer) {
						type = ((BMSPlayer) state).getGauge().getType();
					} else if(state instanceof AbstractResult) {
						type = ((AbstractResult) state).getGaugeType();
					}
					if(type != Integer.MIN_VALUE) return type <= 2;
					return false;
				}
			};			
		}
		if(id == OPTION_GAUGE_HARD) {
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_NO_STATIC) {
				@Override
				public boolean get(MainState state) {
					int type = Integer.MIN_VALUE;
					if(state instanceof BMSPlayer) {
						type = ((BMSPlayer) state).getGauge().getType();
					} else if(state instanceof AbstractResult) {
						type = ((AbstractResult) state).getGaugeType();
					}
					if(type != Integer.MIN_VALUE) return type >= 3;
					return false;
				}
			};			
		}
		if(id == OPTION_GAUGE_EX) {
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_NO_STATIC) {
				@Override
				public boolean get(MainState state) {
					int type = Integer.MIN_VALUE;
					if(state instanceof BMSPlayer) {
						type = ((BMSPlayer) state).getGauge().getType();
					} else if(state instanceof AbstractResult) {
						type = ((AbstractResult) state).getGaugeType();
					}
					if(type != Integer.MIN_VALUE) return type == 0 || type == 1 || type == 4 || type == 5 || type == 7 || type == 8;
					return false;
				}
			};			
		}

		if(id == OPTION_BGAOFF || id == OPTION_BGAON) {
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					return id == OPTION_BGAON ? state.main.getPlayerResource().getBMSResource().isBGAOn() : !state.main.getPlayerResource().getBMSResource().isBGAOn();
				}
			};
		}
		if (id >= OPTION_7KEYSONG && id <= OPTION_9KEYSONG) {	
			final Mode[] modes = {Mode.BEAT_7K, Mode.BEAT_5K, Mode.BEAT_14K, Mode.BEAT_10K, Mode.POPN_9K};
			result = new ModeDrawCondition(modes[id - OPTION_7KEYSONG]);
		}
		if (id == OPTION_24KEYSONG) {
			result = new ModeDrawCondition(Mode.KEYBOARD_24K);
		}
		if (id == OPTION_24KEYDPSONG) {
			result = new ModeDrawCondition(Mode.KEYBOARD_24K_DOUBLE);
		}

		if(id >= OPTION_JUDGE_VERYHARD && id <= OPTION_JUDGE_VERYEASY) {
			final int judgerank = id - OPTION_JUDGE_VERYHARD;
			final int[] judges = {10,35,60,85,110,Integer.MAX_VALUE};
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_WITHOUT_MUSICSELECT) {
				@Override
				public boolean get(MainState state) {
					final SongData model = state.main.getPlayerResource().getSongdata();
					return model != null && (model.getJudge() == judgerank
							|| (model.getJudge() >= judges[judgerank] && model.getJudge() < judges[judgerank + 1]));
				}
			};
		}

		if (id >= OPTION_1P_0_9 && id <= OPTION_1P_100) {
			final float low =(id - OPTION_1P_0_9) * 0.1f;
			final float high =(id - OPTION_1P_0_9 + 1) * 0.1f;
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_ON_RESULT) {
				@Override
				public boolean get(MainState state) {
					if(state instanceof BMSPlayer) {
						final Gauge gauge = ((BMSPlayer) state).getGauge().getGauge();
						return gauge.getValue() >= low * gauge.getProperty().max && gauge.getValue() < high * gauge.getProperty().max ;
					}
					return false;
				}
			};
		}

		if (id >= OPTION_AAA && id <= OPTION_F) {			
			final int[] values = { 0, 6, 9, 12, 15, 18, 21, 24};
			final int low =values[OPTION_F - id];
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_ON_RESULT) {
				@Override
				public boolean get(MainState state) {
					return state.getScoreDataProperty().qualifyRank(low);
				}
			};
		}
		if (id >= OPTION_BEST_AAA_1P && id <= OPTION_BEST_F_1P) {			
			final int[] values = { 0, 6, 9, 12, 15, 18, 21, 24, 28 };
			final int low =values[OPTION_BEST_F_1P - id];
			final int high =values[OPTION_BEST_F_1P - id + 1];
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
		if(id >= OPTION_PERFECT_EXIST && id <= OPTION_MISS_EXIST) {
			result = new DrawConditionProperty(DrawConditionProperty.TYPE_STATIC_ON_RESULT) {
				private final int judge = id - OPTION_PERFECT_EXIST;
				@Override
				public boolean get(MainState state) {
					return state.getJudgeCount(judge, true) + state.getJudgeCount(judge, false) > 0;
				}
			};
		}
		
		if (id == OPTION_1P_PERFECT) {			
			result = new NowJudgeDrawCondition(0,0);
		}		
		if (id == OPTION_1P_EARLY) {			
			result = new NowJudgeDrawCondition(0,1);
		}
		if (id == OPTION_1P_LATE) {			
			result = new NowJudgeDrawCondition(0,2);
		}
		if (id == OPTION_2P_PERFECT) {			
			result = new NowJudgeDrawCondition(1,0);
		}		
		if (id == OPTION_2P_EARLY) {			
			result = new NowJudgeDrawCondition(1,1);
		}
		if (id == OPTION_2P_LATE) {			
			result = new NowJudgeDrawCondition(1,2);
		}
		if (id == OPTION_3P_PERFECT) {			
			result = new NowJudgeDrawCondition(2,0);
		}		
		if (id == OPTION_3P_EARLY) {			
			result = new NowJudgeDrawCondition(2,1);
		}
		if (id == OPTION_3P_LATE) {			
			result = new NowJudgeDrawCondition(2,2);
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
		
		if(result != null && optionid < 0) {
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
	
	public static IntegerProperty getIntegerProperty(int optionid) {
		IntegerProperty result = null;
		if(optionid == NUMBER_MINBPM) {
			result = (state) -> (state.main.getPlayerResource().getSongdata() != null ? 
					state.main.getPlayerResource().getSongdata().getMinbpm() : Integer.MIN_VALUE);
		}
		if(optionid == NUMBER_MAXBPM) {
			result = (state) -> (state.main.getPlayerResource().getSongdata() != null ? 
					state.main.getPlayerResource().getSongdata().getMaxbpm() : Integer.MIN_VALUE);
		}		
		if(optionid == NUMBER_MAINBPM) {
			result = (state) -> (state.main.getPlayerResource().getSongdata() != null ? 
					state.main.getPlayerResource().getSongdata().getMainbpm() : Integer.MIN_VALUE);
		}
		if(optionid >= NUMBER_DURATION_LANECOVER_ON && optionid <= NUMBER_MAXBPM_DURATION_GREEN_LANECOVER_OFF) {
			final boolean green = (optionid - NUMBER_DURATION_LANECOVER_ON) % 2 == 1;
			final boolean cover = (optionid - NUMBER_DURATION_LANECOVER_ON) % 4 < 2;
			final int mode = (optionid - NUMBER_DURATION_LANECOVER_ON) / 4;
			result = (state) -> {
				if(state instanceof BMSPlayer) {
					final LaneRenderer lanerender = ((BMSPlayer) state).getLanerender();
					double bpm = 0;
					switch(mode) {
					case 0:
						bpm = lanerender.getNowBPM();
						break;
					case 1:
						bpm = lanerender.getMainBPM();
						break;
					case 2:
						bpm = lanerender.getMinBPM();
						break;
					case 3:
						bpm = lanerender.getMaxBPM();
						break;
					}
					return (int) Math.round( (240000 / bpm / lanerender.getHispeed()) * (cover ? 1 - lanerender.getLanecover() : 1) * (green ? 1 : 0.6));
				}
				return 0;
			};
		}
		
		if(optionid == NUMBER_LOADING_PROGRESS) {
			result = (state) -> {
				final BMSResource resource = state.main.getPlayerResource().getBMSResource();
				return (int) ((resource.isBGAOn() ? (resource.getBGAProcessor().getProgress() + resource.getAudioDriver().getProgress()) / 2 : 
					resource.getAudioDriver().getProgress()) * 100);
			};			
		}
		
		if(optionid >= VALUE_JUDGE_1P_DURATION && optionid <= VALUE_JUDGE_3P_DURATION) {
			final int player = optionid - VALUE_JUDGE_1P_DURATION;
			result = (state) -> {
				if(state instanceof BMSPlayer) {
					final JudgeManager judge =((BMSPlayer) state).getJudgeManager();
					return (int) (judge.getRecentJudgeTiming().length > player ? judge.getRecentJudgeTiming()[player] : judge.getRecentJudgeTiming()[0]);
				}
				return 0;
			};
		}
		
		return result;
	}

	public static IntegerProperty getImageIndexProperty(int optionid) {
		IntegerProperty result = null;
		
		if(optionid == BUTTON_GAUGE_1P) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().getGauge());
		}
		if(optionid == BUTTON_RANDOM_1P) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().getRandom());
		}		
		if(optionid == BUTTON_RANDOM_2P) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().getRandom2());
		}		
		if(optionid == BUTTON_DPOPTION) {
			result = (state) -> (state.main.getPlayerResource().getPlayerConfig().getDoubleoption());
		}		

		if((optionid >= VALUE_JUDGE_1P_SCRATCH && optionid <= VALUE_JUDGE_2P_KEY9)
					|| (optionid >= VALUE_JUDGE_1P_KEY10 && optionid <= VALUE_JUDGE_2P_KEY99)) {
			result = (state) -> {
				if(state instanceof BMSPlayer) {
					return ((BMSPlayer) state).getJudgeManager().getJudge(optionid);
				}
				return 0;
			};			
		}

		return result;
	}
	
	public static FloatProperty getFloatProperty(int optionid) {
		FloatProperty result = null;
		if(optionid == SLIDER_MUSICSELECT_POSITION) {
			result = (state) -> (state instanceof MusicSelector ? ((MusicSelector) state).getBarRender().getSelectedPosition() : 0);
		}
		if(optionid == BARGRAPH_SCORERATE) {
			result = (state) -> (state.getScoreDataProperty().getRate());
		}
		if(optionid == BARGRAPH_SCORERATE_FINAL) {
			result = (state) -> (state.getScoreDataProperty().getNowRate());
		}		
		if(optionid == BARGRAPH_BESTSCORERATE_NOW) {
			result = (state) -> (state.getScoreDataProperty().getNowBestScoreRate());
		}		
		if(optionid == BARGRAPH_BESTSCORERATE) {
			result = (state) -> (state.getScoreDataProperty().getBestScoreRate());
		}		
		if(optionid == BARGRAPH_TARGETSCORERATE_NOW) {
			result = (state) -> (state.getScoreDataProperty().getNowRivalScoreRate());
		}		
		if(optionid == BARGRAPH_TARGETSCORERATE) {
			result = (state) -> (state.getScoreDataProperty().getRivalScoreRate());
		}
		if(optionid == BARGRAPH_LOAD_PROGRESS) {
			result = (state) -> {
				final BMSResource resource = state.main.getPlayerResource().getBMSResource();
				return resource.isBGAOn() ? (resource.getBGAProcessor().getProgress() + resource.getAudioDriver().getProgress()) / 2 : 
					resource.getAudioDriver().getProgress();
			};			
		}
		if(optionid == SLIDER_MUSIC_PROGRESS || optionid == BARGRAPH_MUSIC_PROGRESS) {
			result = (state) -> {
				if(state instanceof BMSPlayer) {
					if (state.main.isTimerOn(TIMER_PLAY)) {
						return Math.min((float) state.main.getNowTime(TIMER_PLAY) / ((BMSPlayer)state).getPlaytime() , 1);
					}					
				}
				return 0;
			};			
		}
		if(optionid == SLIDER_LANECOVER || optionid == SLIDER_LANECOVER2) {
			result = (state) -> {
				if(state instanceof BMSPlayer) {
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
		
		return result;
	}
	
	public static FloatWriter getFloatWriter(int optionid) {
		FloatWriter result = null;
		
		if(optionid == SLIDER_MUSICSELECT_POSITION) {
			result = (state, value) -> {
				if(state instanceof MusicSelector) {
					final MusicSelector select = (MusicSelector) state;
					select.selectedBarMoved();
					select.getBarRender().setSelectedPosition(value);
				}
			};
		}
		
		return result;
	}

	public static StringProperty getTextProperty(final int optionid) {
		StringProperty result = null;
		if(optionid >= STRING_COURSE1_TITLE && optionid <= STRING_COURSE10_TITLE) {
			result = new StringProperty() {
				private final int index = optionid - STRING_COURSE1_TITLE;
				@Override
				public String get(MainState state) {
					if(state instanceof MusicSelector) {
						final Bar bar = ((MusicSelector)state).getSelectedBar();
						if (bar instanceof GradeBar) {
							if (((GradeBar) bar).getSongDatas().length > index) {
								SongData song = ((GradeBar) bar).getSongDatas()[index];
								final String songname = song != null && song.getTitle() != null ? song.getTitle() : "----";
								return song != null && song.getPath() != null ? songname : "(no song) " + songname;
							}
						}				
					} else {
						CourseData course = state.main.getPlayerResource().getCourseData();
						if(course != null && course.getSong().length > index && course.getSong()[index] != null) {
							return course.getSong()[index].getTitle();
						}
					}
					return "";
				}
			};
		}
		
		return result;
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
			switch(type) {
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
			if(state instanceof BMSPlayer) {
				JudgeManager judge = ((BMSPlayer) state).getJudgeManager();
				if(type == 0) {
					return judge.getNowJudge().length > player && judge.getNowJudge()[player] == 1;
				} else if(type == 1) {
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
			low =values[rank];
			high =values[rank + 1];
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
}
