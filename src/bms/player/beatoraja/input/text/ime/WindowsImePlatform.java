package bms.player.beatoraja.input.text.ime;

/** Injectable Win32 boundary; implementations must release each borrowed HIMC. */
interface WindowsImePlatform {
    record Composition(String result, String preedit, int caret) { }
    record Caret(double x, double y, double width, double height, double clientWidth, double clientHeight) {
        public Caret {
            if (!Double.isFinite(x + y + width + height + clientWidth + clientHeight)
                    || width < 0 || height <= 0 || clientWidth <= 0 || clientHeight <= 0)
                throw new IllegalArgumentException("Invalid IME caret");
        }
        int[] nativeRectangle(int width, int height) {
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid Win32 client size");
            return new int[] {pixel(x * width / clientWidth), pixel(y * height / clientHeight),
                    pixel((x + this.width) * width / clientWidth), pixel((y + this.height) * height / clientHeight)};
        }
        private static int pixel(double n) {
            if (!Double.isFinite(n) || n < Integer.MIN_VALUE || n > Integer.MAX_VALUE)
                throw new IllegalArgumentException("Oversized IME caret");
            return (int)Math.round(n);
        }
    }
    void attach(WindowsImeSession bridge);
    long forward(int message, long wParam, long lParam);
    Composition composition(long flags);
    boolean complete(boolean cancel);
    boolean keyDown(int key);
    int physicalKey(int key, long lParam);
    String clipboard();
    void clipboard(String text);
    void caret(Caret rectangle);
    /** Detach; callback storage must not be freed while its native invocation is on the stack. */
    void detach(boolean destroyed);
}
