package bms.player.beatoraja.input;

/** Owner-thread XIM boundary. Each attachment belongs to exactly one editor session. */
interface X11ImePlatform {
    int COMMIT = 0, PREEDIT = 1, END = 2, KEY = 3, RELEASE = 4, FOCUS_LOST = 5, LOST = 6;
    interface Listener { void event(int kind, byte[] utf8, int first, int second, boolean consumed); }
    record Caret(double x, double y, double width, double height, double clientWidth, double clientHeight) {
        public Caret {
            if (!Double.isFinite(x + y + width + height + clientWidth + clientHeight)
                    || width < 0 || height <= 0 || clientWidth <= 0 || clientHeight <= 0)
                throw new IllegalArgumentException("Invalid X11 IME caret");
        }
        int[] nativeRectangle(int width, int height) {
            double sx = width / clientWidth, sy = height / clientHeight;
            return new int[] {bounded(Math.floor(x * sx)), bounded(Math.floor(y * sy)),
                    bounded(Math.ceil((x + this.width) * sx)), bounded(Math.ceil((y + this.height) * sy))};
        }
        private static int bounded(double value) { return (int)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value)); }
    }
    void attach(Listener listener);
    void reset();
    void caret(Caret caret);
    void close();
    String clipboard();
    void clipboard(String value);
}
