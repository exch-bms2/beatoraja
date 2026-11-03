package bms.player.beatoraja.input;

/** Hooks exist only in the separately compiled test dylib, never the distribution library. */
public final class MacOsImeNativeTest {
    private MacOsImeNativeTest() { }
    public static long retain(MacOsImeBridge bridge) { return retainNative(bridge.nativeHandle()); }
    public static boolean inject(long pointer, int op, String text) { return injectNative(pointer, op, text); }
    public static double[] geometry(long pointer) { return geometryNative(pointer); }
    public static void release(long pointer) { releaseNative(pointer); }
    public static void position(long pointer, double x, double y, double w, double h, double cw, double ch) {
        positionNative(pointer, x, y, w, h, cw, ch);
    }
    private static native long retainNative(long pointer);
    private static native void releaseNative(long pointer);
    private static native boolean injectNative(long pointer, int op, String text);
    private static native double[] geometryNative(long pointer);
    private static native void positionNative(long pointer, double x, double y, double w, double h, double cw, double ch);
}
