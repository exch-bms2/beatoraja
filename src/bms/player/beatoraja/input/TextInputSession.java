package bms.player.beatoraja.input;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/** UI-thread editor state with ordered, session-scoped messages from an IME. */
public final class TextInputSession implements AutoCloseable {
    public record EditorState(String text, int anchor, int cursor) {
        public EditorState {
            Objects.requireNonNull(text);
            boundary(text, anchor); boundary(text, cursor);
        }
    }
    private enum Kind { COMMITTED, PREEDIT, CANCELLED, COMMAND, FINISHED }
    private record Event(Kind kind, EditorState state, String text, int caret,
                         ImeBridge.Command command, boolean consumed) { }
    private static final AtomicLong IDS = new AtomicLong();
    private final long id = IDS.incrementAndGet();
    private final ImeBridge bridge;
    private final boolean available;
    private final ConcurrentLinkedQueue<Event> events = new ConcurrentLinkedQueue<>();
    private EditorState state;
    private String preedit = "";
    private int preeditCaret;
    private boolean composing, finishing, finished;
    private volatile boolean closed;

    public TextInputSession(ImeBridge bridge, EditorState initial) {
        this.bridge = Objects.requireNonNull(bridge);
        state = Objects.requireNonNull(initial);
        available = bridge.isAvailable();
        if (available) {
            try { bridge.begin(id, initial, sink); }
            catch (RuntimeException | Error e) {
                closed = true; events.clear();
                try { bridge.close(id); } catch (RuntimeException | Error cleanup) { e.addSuppressed(cleanup); }
                throw e;
            }
        }
        TextInputDiagnostics.session("session-begin", this, bridge);
    }

    private final ImeBridge.Sink sink = new ImeBridge.Sink() {
        public void committed(long session, EditorState value) {
            if (!accepts(session)) return;
            post(session, new Event(Kind.COMMITTED, Objects.requireNonNull(value), null, 0, null, false));
        }
        public void preedit(long session, String text, int caret) {
            if (!accepts(session)) return;
            Objects.requireNonNull(text); boundary(text, caret);
            post(session, new Event(Kind.PREEDIT, null, text, caret, null, false));
        }
        public void cancelled(long session) { post(session, new Event(Kind.CANCELLED, null, null, 0, null, false)); }
        public void command(long session, ImeBridge.Command command, boolean consumed) {
            if (!accepts(session)) return;
            post(session, new Event(Kind.COMMAND, null, null, 0, Objects.requireNonNull(command), consumed));
        }
        public void finished(long session) { post(session, new Event(Kind.FINISHED, null, null, 0, null, false)); }
    };

    private boolean accepts(long session) { return session == id && !closed; }

    private synchronized void post(long session, Event event) {
        if (session == id && !closed) {
            traceEvent("ime-enqueue", event);
            events.add(event);
        }
    }

    private void traceEvent(String phase, Event event) {
        if (TextInputDiagnostics.isEnabled()) TextInputDiagnostics.event(phase, id, event.kind.name(),
                event.state, event.text, event.caret, event.command, event.consumed);
    }

    public long id() { return id; }
    public boolean usesIme() { return available; }
    public EditorState state() { return state; }
    public boolean isComposing() { return composing; }
    public boolean isFinishing() { return finishing; }
    public boolean isFinished() { return finished; }
    public String preedit() { return preedit; }
    public int preeditStart() { return Math.min(state.anchor, state.cursor); }
    public int displayCursor() { return composing ? Math.min(state.anchor, state.cursor) + preeditCaret : state.cursor; }
    public String displayText() {
        if (!composing) return state.text;
        return state.text.substring(0, Math.min(state.anchor, state.cursor)) + preedit
                + state.text.substring(Math.max(state.anchor, state.cursor));
    }

    /** Pull direct Scene2D edits only when no composition/finish transaction is in flight. */
    public void updateEditor(EditorState value) {
        if (closed || composing || finishing || state.equals(value)) return;
        state = Objects.requireNonNull(value);
        TextInputDiagnostics.session("scene2d-update", this, bridge);
        if (available) bridge.update(id, state);
    }

    public void requestFinish() {
        if (closed || finishing) return;
        finishing = true;
        TextInputDiagnostics.session("finish-request", this, bridge);
        if (available) bridge.finish(id);
        else { finished = true; close(); }
    }

    /** Native callbacks enqueue only; the UI applies them here, never on a native thread. */
    public void drain() {
        Event event;
        while (!closed && (event = events.poll()) != null) {
            traceEvent("ime-apply", event);
            switch (event.kind) {
                case COMMITTED -> { state = event.state; clearComposition(); }
                case PREEDIT -> { preedit = event.text; preeditCaret = event.caret; composing = true; }
                case CANCELLED -> clearComposition();
                case COMMAND -> {
                    if (event.consumed || finishing) break;
                    if (event.command == ImeBridge.Command.SUBMIT) {
                        if (composing) bridge.confirmComposition(id);
                        else requestFinish();
                    } else if (composing) bridge.cancelComposition(id);
                }
                case FINISHED -> {
                    if (finishing) { clearComposition(); finished = true; close(); }
                }
            }
            TextInputDiagnostics.session("ime-applied", this, bridge);
        }
    }

    private void clearComposition() { composing = false; preedit = ""; preeditCaret = 0; }

    /** Abort/detach on screen disposal; never block shutdown waiting for a native acknowledgement. */
    @Override public void close() {
        synchronized (this) {
            if (closed) return;
            closed = true; events.clear();
        }
        TextInputDiagnostics.session("session-close", this, bridge);
        clearComposition();
        if (available) bridge.close(id);
    }

    private static void boundary(String text, int offset) {
        if (offset < 0 || offset > text.length() || (offset > 0 && offset < text.length()
                && Character.isSurrogatePair(text.charAt(offset - 1), text.charAt(offset))))
            throw new IllegalArgumentException("Invalid UTF-16 text boundary");
    }
}
