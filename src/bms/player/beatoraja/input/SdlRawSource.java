package bms.player.beatoraja.input;

/** Single-consumer raw joystick boundary; lifecycle stays on the owner, buffers are reused per poll. */
interface SdlRawSource extends AutoCloseable {
    record Device(int id, String name, int buttons, int axes, int hats) { }
    final class Event {
        static final int BUTTON = 1, HAT = 2, AXIS = 3;
        int device, type, index;
        float value;
    }
    Device[] devices();
    void update();
    boolean next(Event target);
    boolean connected(int slot);
    boolean button(int slot, int button);
    float axis(int slot, int axis);
    int hat(int slot);
    @Override void close();
}
