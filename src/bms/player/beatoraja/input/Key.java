package bms.player.beatoraja.input;

public class Key {
	private boolean isPressed;
	private long pressTime;
	
	Key() {
		isPressed = false;
		pressTime = 0;
	}
	
	boolean checkIfPressed() {
		return isPressed && pressTime != 0;
	}
	
	// get, set methods
	void setState(boolean state) {
		isPressed = state;
	}
	
	boolean getIsPressed() {
		return isPressed;
	}
	
	void setTime(long time) {
		pressTime = time;
	}
	
	void resetTime() {
		pressTime = 0;
	}
	
	long getPressTime() {
		return pressTime;
	}
}
