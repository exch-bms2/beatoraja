package bms.player.beatoraja.input;

public class Key {
	private boolean isPressed;
	private long pressTime;
	
	Key() {
		isPressed = false;
		pressTime = 0;
	}
	
	void setState(boolean state) {
		isPressed = state;
	}
	
	boolean getIsPressed() {
		return isPressed;
	}
	
	void setTime(long time) {
		pressTime = time;
	}
	
	long getPressTime() {
		return pressTime;
	}
}
