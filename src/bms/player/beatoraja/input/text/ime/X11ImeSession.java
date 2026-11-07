package bms.player.beatoraja.input.text.ime;

import bms.player.beatoraja.input.text.ImeBridge;
import bms.player.beatoraja.input.text.TextInputSession;

import static org.lwjgl.glfw.GLFW.*;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Objects;

/** Session-scoped editor state; XIM callbacks only enqueue immutable UI snapshots. */
final class X11ImeSession implements ImeBridge, X11ImePlatform.Listener {
    private static final int MAX_BYTES = 1 << 20;
    private final X11ImePlatform platform;
    private final Thread owner = Thread.currentThread();
    private final BitSet guarded = new BitSet();
    private final ArrayDeque<TextInputSession.EditorState> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private Sink sink;
    private TextInputSession.EditorState document;
    private long session;
    private boolean active, composing, finishing, resetting, fault;

    X11ImeSession(X11ImePlatform platform) { this.platform = Objects.requireNonNull(platform); }
    public boolean isAvailable() { return true; }
    public void begin(long id, TextInputSession.EditorState value, Sink sink) {
        checkThread();
        if (active) throw new IllegalStateException("IME already active");
        session = id; document = value; this.sink = Objects.requireNonNull(sink); active = true;
        try { platform.attach(this); }
        catch (RuntimeException | Error e) {
            active = false; this.sink = null;
            try { platform.close(); } catch (RuntimeException | Error cleanup) { e.addSuppressed(cleanup); }
            throw e;
        }
    }
    public void update(long id, TextInputSession.EditorState value) {
        checkThread();
        if (current(id) && !composing && !finishing && document.text().equals(value.text())) document = value;
    }
    public void caret(long id, double x, double y, double w, double h, double cw, double ch) {
        checkThread();
        if (current(id) && !fault && Double.isFinite(x+y+w+h+cw+ch) && w>=0 && h>0 && cw>0 && ch>0)
            platform.caret(new X11ImePlatform.Caret(x,y,w,h,cw,ch));
    }
    public void confirmComposition(long id) {
        checkThread();
        // XIM reset is not a portable conversion-confirm request. Native Return must resolve it.
    }
    public void cancelComposition(long id) {
        checkThread();
        if (!current(id)) return;
        resetting = true;
        try { if (!fault) platform.reset(); }
        catch (RuntimeException | LinkageError e) { fault = true; X11ImeBridge.warn("reset failed; preserving committed draft"); }
        finally { resetting = false; composing = false; sink.cancelled(id); }
    }
    public void finish(long id) {
        checkThread();
        if (!current(id) || finishing) return;
        finishing = true;
        cancelComposition(id);
        sink.finished(id);
    }
    public void close(long id) {
        checkThread();
        if (!current(id)) return;
        active = false; sink = null; composing = false;
        platform.close();
    }
    public void event(int kind, byte[] utf8, int first, int second, boolean consumed) {
        checkThread();
        if (!active || resetting || finishing) return;
        try {
            switch (kind) {
                case X11ImePlatform.COMMIT -> {
                    if (!fault) {
                        String value = text(utf8).replaceAll("[\\p{Cc}\\p{Zl}\\p{Zp}]", "");
                        if (!value.isEmpty()) insert(value);
                    }
                }
                case X11ImePlatform.PREEDIT -> {
                    if (fault) return;
                    String value = text(utf8);
                    if (first < 0 || first > value.codePointCount(0, value.length())) throw new IllegalArgumentException("XIM caret");
                    composing = true; sink.preedit(session, value, value.offsetByCodePoints(0, first));
                }
                case X11ImePlatform.END -> { composing = false; sink.cancelled(session); }
                case X11ImePlatform.KEY -> key(first, second, consumed);
                case X11ImePlatform.RELEASE -> { if (first >= 0) guarded.clear(first); }
                case X11ImePlatform.FOCUS_LOST -> { guarded.clear(); cancelComposition(session); }
                case X11ImePlatform.LOST -> {
                    fault = true; composing = false; sink.cancelled(session);
                    X11ImeBridge.warn("input context lost; finish and refocus the field to reconnect");
                }
                default -> throw new IllegalArgumentException("Unknown XIM event");
            }
        } catch (CharacterCodingException | IllegalArgumentException e) {
            fault = true; composing = false; sink.cancelled(session);
            X11ImeBridge.warn("invalid native notification; preserving committed draft");
        }
    }
    private static String text(byte[] utf8) throws CharacterCodingException {
        if (utf8 == null || utf8.length > MAX_BYTES) throw new IllegalArgumentException("XIM text length");
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(utf8)).toString();
    }
    private void key(int key, int mods, boolean consumed) {
        if (key < 0 || key > GLFW_KEY_LAST) return;
        if (consumed || composing) guarded.set(key);
        boolean used = guarded.get(key);
        if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) sink.command(session, Command.SUBMIT, used);
        else if (key == GLFW_KEY_ESCAPE) sink.command(session, Command.CANCEL, used);
        else if (!used && !fault) edit(key, mods);
    }
    private void edit(int key, int mods) {
        boolean ctrl = (mods & GLFW_MOD_CONTROL) != 0, shift = (mods & GLFW_MOD_SHIFT) != 0;
        if ((mods & (GLFW_MOD_ALT | GLFW_MOD_SUPER)) != 0) return;
        String text = document.text(); int cursor = document.cursor(), anchor = document.anchor();
        if (ctrl && key == GLFW_KEY_A) publish(new TextInputSession.EditorState(text, 0, text.length()));
        else if (ctrl && (key == GLFW_KEY_C || key == GLFW_KEY_X || key == GLFW_KEY_V)) {
            try {
                if (key == GLFW_KEY_V) {
                    String value = platform.clipboard();
                    if (value != null) insert(value.replaceAll("[\\p{Cc}\\p{Zl}\\p{Zp}]", ""));
                } else if (anchor != cursor) {
                    platform.clipboard(text.substring(Math.min(anchor,cursor), Math.max(anchor,cursor)));
                    if (key == GLFW_KEY_X) insert("");
                }
            } catch (RuntimeException e) { X11ImeBridge.warn("clipboard operation failed; preserving selection"); }
        } else if (ctrl && (key == GLFW_KEY_Z || key == GLFW_KEY_Y)) {
            var source = key == GLFW_KEY_Y || shift ? redo : undo;
            var target = source == undo ? redo : undo;
            if (!source.isEmpty()) { remember(target, document); publish(source.removeLast()); }
        } else if (!ctrl && (key == GLFW_KEY_BACKSPACE || key == GLFW_KEY_DELETE)) {
            if (anchor != cursor) insert("");
            else {
                int end = cursor;
                if (key == GLFW_KEY_BACKSPACE && cursor > 0) cursor = text.offsetByCodePoints(cursor,-1);
                if (key == GLFW_KEY_DELETE && end < text.length()) end = text.offsetByCodePoints(end,1);
                replace(cursor,end,"");
            }
        } else if (key == GLFW_KEY_LEFT || key == GLFW_KEY_RIGHT || key == GLFW_KEY_HOME || key == GLFW_KEY_END) {
            int next = cursor;
            if (key == GLFW_KEY_HOME) next = 0;
            else if (key == GLFW_KEY_END) next = text.length();
            else if (!shift && anchor != cursor) next = key == GLFW_KEY_LEFT ? Math.min(anchor,cursor) : Math.max(anchor,cursor);
            else if (key == GLFW_KEY_LEFT && cursor > 0) {
                next = text.offsetByCodePoints(cursor,-1);
                if (ctrl) {
                    while (next > 0 && !Character.isLetterOrDigit(text.codePointAt(next))) next = text.offsetByCodePoints(next,-1);
                    while (next > 0 && Character.isLetterOrDigit(text.codePointBefore(next))) next = text.offsetByCodePoints(next,-1);
                }
            } else if (key == GLFW_KEY_RIGHT && cursor < text.length()) {
                next = text.offsetByCodePoints(cursor,1);
                if (ctrl) {
                    while (next < text.length() && Character.isLetterOrDigit(text.codePointAt(next))) next = text.offsetByCodePoints(next,1);
                    while (next < text.length() && !Character.isLetterOrDigit(text.codePointAt(next))) next = text.offsetByCodePoints(next,1);
                }
            }
            publish(new TextInputSession.EditorState(text, shift ? anchor : next, next));
        }
    }
    private void insert(String value) { replace(Math.min(document.anchor(),document.cursor()), Math.max(document.anchor(),document.cursor()), value); }
    private void replace(int first, int last, String value) {
        String text = document.text();
        if (text.length() - (last-first) + value.length() > MAX_BYTES) return;
        String next = text.substring(0,first) + value + text.substring(last);
        remember(undo,document); redo.clear(); composing = false;
        publish(new TextInputSession.EditorState(next,first+value.length(),first+value.length()));
    }
    private static void remember(ArrayDeque<TextInputSession.EditorState> history, TextInputSession.EditorState value) {
        if (history.size() == 64) history.removeFirst();
        history.addLast(value);
    }
    private void publish(TextInputSession.EditorState value) { document = value; sink.committed(session,value); }
    private boolean current(long id) { return active && id == session; }
    private void checkThread() { if (Thread.currentThread() != owner) throw new IllegalStateException("IME requires GLFW owner thread"); }
}
