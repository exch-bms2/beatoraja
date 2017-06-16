package bms.player.beatoraja.input;

public final class BMSPlayerInputDevice {

	public static final BMSPlayerInputDevice Keyboard = new BMSPlayerInputDevice(Type.KEYBOARD, 0);

	public static BMSPlayerInputDevice BMController(int index) {
		return new BMSPlayerInputDevice(Type.BM_CONTROLLER, index);
	}

	public static BMSPlayerInputDevice Midi(int index) {
		return new BMSPlayerInputDevice(Type.MIDI, index);
	}

	public enum Type {
		KEYBOARD,
		BM_CONTROLLER,
		MIDI
	}

	public Type getType() {
		return type;
	}

	public int getIndex() {
		return index;
	}

	public boolean equals(Object other) {
		if (!(other instanceof BMSPlayerInputDevice))
			return false;

		BMSPlayerInputDevice device = (BMSPlayerInputDevice)other;
		return this.type == device.type && this.index == device.index;
	}

	public int hashCode() {
		return (type.ordinal() << 16) | index;
	}

	public String toString() {
		return String.format("%s-%d", type.toString(), index);
	}

	private BMSPlayerInputDevice(Type type, int index) {
		this.type = type;
		this.index = index;
	}

	private final Type type;
	private final int index;
}
