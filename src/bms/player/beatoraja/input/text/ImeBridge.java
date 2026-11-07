package bms.player.beatoraja.input.text;

/**
 * Per-editor IME transport. Lifecycle methods run on the UI thread; sinks may be
 * called from another thread. Text/ranges use UTF-16 boundaries (never half a surrogate pair).
 * A native adapter owns character delivery while available: it must not also
 * deliver the same committed characters through GLFW. OS adapters are opt-in; NONE is the default.
 */
public interface ImeBridge {
    enum Command { SUBMIT, CANCEL }

    interface Sink {
        /** Full committed document, not an insertion delta; excludes preedit text. */
        void committed(long session, TextInputSession.EditorState state);
        /** Preedit replaces the current selection visually, without modifying committed text. */
        void preedit(long session, String text, int caret);
        void cancelled(long session);
        /** Consumed key identity must survive composition-end/commit ordering. */
        void command(long session, Command command, boolean consumedByIme);
        /** Sent only after finish has resolved composition and delivered final committed text. */
        void finished(long session);
    }

    boolean isAvailable();
    void begin(long session, TextInputSession.EditorState initial, Sink sink);
    void update(long session, TextInputSession.EditorState state);
    /** Top-left client logical coordinates, NOT framebuffer pixels. */
    default void caret(long session, double x, double y, double width, double height, double clientWidth, double clientHeight) { }
    /** Optional owner-thread work (e.g. a bounded asynchronous native finish). */
    default void poll(long session) { }
    void confirmComposition(long session);
    void cancelComposition(long session);
    void finish(long session);
    /** Detach callbacks without waiting. Late callbacks must retain their original session ID. */
    void close(long session);

    ImeBridge NONE = new ImeBridge() {
        public boolean isAvailable() { return false; }
        public void begin(long id, TextInputSession.EditorState state, Sink sink) { }
        public void update(long id, TextInputSession.EditorState state) { }
        public void confirmComposition(long id) { }
        public void cancelComposition(long id) { }
        public void finish(long id) { }
        public void close(long id) { }
    };
}
