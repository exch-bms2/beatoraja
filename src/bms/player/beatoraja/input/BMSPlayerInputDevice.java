package bms.player.beatoraja.input;

public class BMSPlayerInputDevice {

	public enum Type {
		KEYBOARD,
		BM_CONTROLLER,
		MIDI
	}

	protected Type type;

	public Type getType() {
		return type;
	}

	public void clear() {
	}

	protected BMSPlayerInputDevice(Type type) {
		this.type = type;
	}
}
