package bms.player.beatoraja.input;

public class keyData {
	public static Key[] key = new Key[256];
	public static Key[] numberKey = new Key[10];
	public static Key[] functionKey = new Key[12];

	
	// methods for key
	public static long getKeyTime(int i) {
		return key[i].getPressTime();
	}

	public static void setKeyTime(int i, long time) {
		key[i].setTime(time);
	}
	
	public static void resetKeyTime(int i) {
		key[i].resetTime();
	}
	
	public static void resetKeyTime() {
		for (int i = 0; i < getKeyLength(); i++)
			key[i].resetTime();
	}

	public static boolean getKeyState(int i) {
		return key[i].getIsPressed();
	}

	public static void setKeyState(int i, boolean state) {
		key[i].setState(state);
	}
	
	public static void resetKeyState() {
		for (int i = 0; i < getKeyLength(); i++)
			key[i].setState(false);
	}
	
	public static boolean checkIfKeyPressed(int i) {
		return key[i].checkIfPressed();
	}
	
	public static int getKeyLength() {
		return key.length;
	}

	
	// methods for numberKey
	public static boolean getNumberState(int i) {
		return numberKey[i].getIsPressed();
	}

	public static long getNumberTime(int i) {
		return numberKey[i].getPressTime();
	}
	
	public static void resetNumberTime(int i) {
		numberKey[i].resetTime();
	}
	
	public static void setNumberState(int i, boolean state, long time) {
		numberKey[i].setState(state);
		numberKey[i].setTime(time);
	}
	
	public static boolean checkIfNumberPressed(int i) {
		return numberKey[i].checkIfPressed();
	}

	
	// methods for functionKey
	public static boolean getFunctionstate(int i) {
		return functionKey[i].getIsPressed();
	}

	public static void setFunctionstate(int i, boolean state) {
		functionKey[i].setState(state);
	}

	public static long getFunctiontime(int i) {
		return functionKey[i].getPressTime();
	}

	public static void setFunctiontime(int i, long time) {
		functionKey[i].setTime(time);
	}
	
	public static void resetFunctionTime(int i) {
		functionKey[i].resetTime();
	}
	
	public static void setFunction(int i, boolean state, long time) {
		functionKey[i].setState(state);
		functionKey[i].setTime(time);
	}

	public static boolean checkIfFunctionPressed(int i) {
		return functionKey[i].checkIfPressed();
	}
}