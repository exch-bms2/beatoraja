package bms.player.beatoraja.input;

/** JNI hooks for the fake-Xlib test library; keep this name for its exported symbols. */
public final class X11ImeNativeTest {
    private X11ImeNativeTest() { }

    public static native long window();
    public static native void draw(int first, int removed, String text, int caret, boolean wide);
    public static native void key(byte[] commit);
    public static native int state(int what);
    public static native void lost();
    public static native void flow(int action);
    public static native boolean locale();
}
