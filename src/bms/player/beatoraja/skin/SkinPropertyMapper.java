package bms.player.beatoraja.skin;

import static bms.player.beatoraja.skin.SkinProperty.*;

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

	public static boolean isCustomEventId(int id) {
		return id >= EVENT_CUSTOM_BEGIN && id <= EVENT_CUSTOM_END;
	}

	public static boolean isEventRunnableBySkin(int id) {
		if (isCustomEventId(id))
			return true;
		// スキンから実行できては困るイベントがあればここでフィルタリングする
		return true;
	}

	public static boolean isCustomTimerId(int id) {
		return id >= TIMER_CUSTOM_BEGIN && id <= TIMER_CUSTOM_END;
	}

	public static boolean isTimerWritableBySkin(int id) {
		// スキンからはカスタムタイマーのみ書き込み可能とする(組み込みタイマーはゲームプレイに影響するため)
		return isCustomTimerId(id);
	}
}
