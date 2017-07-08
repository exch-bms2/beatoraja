package bms.player.beatoraja.input;

public class BMSPlayerInputDevice {

	public enum Type {
		KEYBOARD,
		BM_CONTROLLER,
		MIDI
	}

	protected Type type;
	protected int player = 0;
	protected boolean enabled = true;

	public Type getType() {
		return type;
	}

	public int getPlayer() {
		return player;
	}

	public void setPlayer(int player) {
		this.player = player;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void clear() {
	}

	protected BMSPlayerInputDevice(Type type, int player) {
		this.type = type;
		this.player = player;
	}

	protected BMSPlayerInputDevice(Type type) {
		this.type = type;
	}
}
