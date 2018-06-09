package bms.player.beatoraja.input;

public class keyData {
	public static Key[] key = new Key[256];
	private Key[] numberKey = new Key[10];
	private Key[] functionKey = new Key[12];

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

	
}