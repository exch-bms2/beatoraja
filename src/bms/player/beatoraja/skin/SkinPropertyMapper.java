package bms.player.beatoraja.skin;

import static bms.player.beatoraja.skin.SkinProperty.*;

import bms.model.Mode;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.ScoreDataProperty;
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


	public static boolean isKeyJudgeValueId(int valueId) {
		return (valueId >= VALUE_JUDGE_1P_SCRATCH && valueId <= VALUE_JUDGE_2P_KEY9)
				|| (valueId >= VALUE_JUDGE_1P_KEY10 && valueId <= VALUE_JUDGE_2P_KEY99);
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
		// TODO 各Skinに分散するるべき？
		BooleanProperty result = null;
		final int id = Math.abs(optionid);
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
}
