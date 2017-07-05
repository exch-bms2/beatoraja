package bms.player.beatoraja.input;

public class BMSPlayerInputDevice {

	public enum Type {
		KEYBOARD,
		BM_CONTROLLER,
		MIDI
	}

	public Type getType() {
		return type;
	}

	public int getPlayer() {
		return player;
	}

	public void setPlayer(int player) {
		this.player = player;
	}

	public boolean equals(Object other) {
		if (!(other instanceof BMSPlayerInputDevice))
			return false;

		BMSPlayerInputDevice device = (BMSPlayerInputDevice)other;
		return this.type == device.type && this.player == device.player;
	}

	public int hashCode() {
		return (type.ordinal() << 16) | player;
	}

	public String toString() {
		return String.format("%s-%d", type.toString(), player);
	}

	protected BMSPlayerInputDevice(Type type, int player) {
		this.type = type;
		this.player = player;
	}

	protected BMSPlayerInputDevice(Type type) {
		this.player = 0;
	}

	protected Type type;
	protected int player;
}
