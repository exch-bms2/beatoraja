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
}
