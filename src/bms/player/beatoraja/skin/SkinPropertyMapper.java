package bms.player.beatoraja.skin;

import static bms.player.beatoraja.skin.SkinProperty.*;

public class SkinPropertyMapper {

	public static int bombTimerId(int player, boolean scratch, int index) {
		if (player < 2) {
			if (scratch) {
				if (index == 0) {
					return TIMER_BOMB_1P_SCRATCH + player * 10;
				} else if (index < 11) {
					return TIMER_BOMB_1P_SCRATCH2 + index - 1 + player * 100;
				}
			} else {
				if (index < 9) {
					return TIMER_BOMB_1P_KEY1 + index + player * 10;
				} else if (index < 99) {
					return TIMER_BOMB_1P_KEY10 + index - 9 + player * 100;
				}
			}
		}
		return -1;
	}

	public static int holdTimerId(int player, boolean scratch, int index) {
		if (player < 2) {
			if (scratch) {
				if (index == 0) {
					return TIMER_HOLD_1P_SCRATCH + player * 10;
				} else if (index < 11) {
					return TIMER_HOLD_1P_SCRATCH2 + index - 1 + player * 100;
				}
			} else {
				if (index < 9) {
					return TIMER_HOLD_1P_KEY1 + index + player * 10;
				} else if (index < 99) {
					return TIMER_HOLD_1P_KEY10 + index - 9 + player * 100;
				}
			}
		}
		return -1;
	}

	public static int keyOnTimerId(int player, boolean scratch, int index) {
		if (player < 2) {
			if (scratch) {
				if (index == 0) {
					return TIMER_KEYON_1P_SCRATCH + player * 10;
				} else if (index < 11) {
					return TIMER_KEYON_1P_SCRATCH2 + index - 1 + player * 100;
				}
			} else {
				if (index < 9) {
					return TIMER_KEYON_1P_KEY1 + index + player * 10;
				} else if (index < 99) {
					return TIMER_KEYON_1P_KEY10 + index - 9 + player * 100;
				}
			}
		}
		return -1;
	}

	public static int keyOffTimerId(int player, boolean scratch, int index) {
		if (player < 2) {
			if (scratch) {
				if (index == 0) {
					return TIMER_KEYOFF_1P_SCRATCH + player * 10;
				} else if (index < 11) {
					return TIMER_KEYOFF_1P_SCRATCH2 + index - 1 + player * 100;
				}
			} else {
				if (index < 9) {
					return TIMER_KEYOFF_1P_KEY1 + index + player * 10;
				} else if (index < 99) {
					return TIMER_KEYOFF_1P_KEY10 + index - 9 + player * 100;
				}
			}
		}
		return -1;
	}


	public static boolean isKeyJudgeValueId(int valueId) {
		return (valueId >= VALUE_JUDGE_1P_SCRATCH && valueId <= VALUE_JUDGE_2P_KEY9)
				|| (valueId >= VALUE_JUDGE_1P_SCRATCH2 && valueId <= VALUE_JUDGE_2P_KEY_MAX);
	}

	public static int keyJudgeValueId(int player, boolean scratch, int index) {
		if (player < 2) {
			if (scratch) {
				if (index == 0) {
					return VALUE_JUDGE_1P_SCRATCH + player * 10;
				} else if (index < 11) {
					return VALUE_JUDGE_1P_SCRATCH2 + index - 1 + player * 100;
				}
			} else {
				if (index < 9) {
					return VALUE_JUDGE_1P_KEY1 + index + player * 10;
				} else if (index < 99) {
					return VALUE_JUDGE_1P_KEY10 + index - 9 + player * 100;
				}
			}
		}
		return -1;
	}
}
