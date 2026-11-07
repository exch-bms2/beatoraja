package bms.player.beatoraja.input;

/** Single-consumer SDL3 channels (raw or legacy mapped IDs); lifecycle stays on the owner. */
interface SdlRawSource extends AutoCloseable {
    record Device(int id, String name, int buttons, int axes, int hats, String identity) {
        Device(int id, String name, int buttons, int axes, int hats) {
            this(id, name, buttons, axes, hats, name);
        }
    }
    final class Event {
        static final int BUTTON = 1, HAT = 2, AXIS = 3, ADDED = 4, REMOVED = 5;
        int device, type, index;
        float value;
    }
    Device[] devices();
    default long inventoryVersion() { return 0; }
    void update();
    boolean next(Event target);
    boolean connected(int slot);
    boolean button(int slot, int button);
    float axis(int slot, int axis);
    int hat(int slot);
    @Override void close();
}
