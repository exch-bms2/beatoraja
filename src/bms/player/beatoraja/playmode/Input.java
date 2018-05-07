package bms.player.beatoraja.playmode;

public class Input {
    public enum Type {
        NOTE, PITCH_BEND, CONTROL_CHANGE,
    }

    public Type type;
    public int value;

    public Input() {
        this.type = Type.NOTE;
        this.value = 0;
    }

    public Input(Input input) {
        this.type = input.type;
        this.value = input.value;
    }

    public Input(Type type, int value) {
        this.type = type;
        this.value = value;
    }

    public String toString() {
        switch (type) {
            case NOTE:
                return "NOTE " + value;
            case PITCH_BEND:
                return "PITCH " + (value > 0 ? "+" : "-");
            case CONTROL_CHANGE:
                return "CC " + value;
            default:
                return null;
        }
    }
}