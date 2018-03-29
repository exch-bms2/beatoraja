package bms.player.beatoraja.skin;

import static bms.player.beatoraja.skin.SkinProperty.*;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.ScoreDataProperty;
import bms.player.beatoraja.skin.SkinObject.BooleanProperty;

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
		if (id >= OPTION_BEST_AAA_1P && id <= OPTION_BEST_F_1P) {			
			final int[] values = { 0, 6, 9, 12, 15, 18, 21, 24, 28 };
			final int low =values[OPTION_BEST_F_1P - id];
			final int high =values[OPTION_BEST_F_1P - id + 1];
			result = new BooleanProperty() {				
				@Override
				public boolean get(MainState state) {
					final ScoreDataProperty score = state.getScoreDataProperty();
					return score.qualifyBestRank(low) && !score.qualifyBestRank(high);
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
				public boolean get(MainState state) {
					return !dc.get(state);
				}				
			};
		}
		
		return result;
	}
	
	private static class NowRankDrawCondition implements BooleanProperty {
		
		private final int low;
		private final int high;
		
		public NowRankDrawCondition(int rank) {
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
