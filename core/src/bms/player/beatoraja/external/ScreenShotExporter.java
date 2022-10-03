package bms.player.beatoraja.external;

import static bms.player.beatoraja.skin.SkinProperty.*;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.property.BooleanPropertyFactory;
import bms.player.beatoraja.skin.property.IntegerPropertyFactory;

public interface ScreenShotExporter {

	public boolean send(MainState state, byte[] pixels);

	public static String getClearTypeName(MainState currentState) {
		String[] clearTypeName = { "NO PLAY", "FAILED", "ASSIST EASY CLEAR", "LIGHT ASSIST EASY CLEAR", "EASY CLEAR",
				"CLEAR", "HARD CLEAR", "EXHARD CLEAR", "FULL COMBO", "PERFECT", "MAX" };

		int clear = IntegerPropertyFactory.getIntegerProperty(NUMBER_CLEAR).get(currentState);
		if(clear >= 0 && clear < clearTypeName.length) {
			return clearTypeName[clear];
		}

		return "";
	}

	public static String getRankTypeName(MainState currentState) {
		String rankTypeName = "";
		if(BooleanPropertyFactory.getBooleanProperty(OPTION_RESULT_AAA_1P).get(currentState)) rankTypeName += "AAA";
		else if(BooleanPropertyFactory.getBooleanProperty(OPTION_RESULT_AA_1P).get(currentState)) rankTypeName += "AA";
		else if(BooleanPropertyFactory.getBooleanProperty(OPTION_RESULT_A_1P).get(currentState)) rankTypeName += "A";
		else if(BooleanPropertyFactory.getBooleanProperty(OPTION_RESULT_B_1P).get(currentState)) rankTypeName += "B";
		else if(BooleanPropertyFactory.getBooleanProperty(OPTION_RESULT_C_1P).get(currentState)) rankTypeName += "C";
		else if(BooleanPropertyFactory.getBooleanProperty(OPTION_RESULT_D_1P).get(currentState)) rankTypeName += "D";
		else if(BooleanPropertyFactory.getBooleanProperty(OPTION_RESULT_E_1P).get(currentState)) rankTypeName += "E";
		else if(BooleanPropertyFactory.getBooleanProperty(OPTION_RESULT_F_1P).get(currentState)) rankTypeName += "F";
		return rankTypeName;
	}
}
