package bms.player.beatoraja.input.text.ime;

import bms.player.beatoraja.input.text.ImeBridge;
import bms.player.beatoraja.input.text.TextInputSession;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.logging.Logger;

/** Opt-in IMM prototype. The state machine is testable without loading a Windows DLL. */
final class WindowsImeSession implements ImeBridge {
    static final int KEYDOWN=0x100, KEYUP=0x101, CHAR=0x102, SYSKEYDOWN=0x104, SYSKEYUP=0x105,
            UNICHAR=0x109, START=0x10d, END=0x10e, COMPOSITION=0x10f, SETCONTEXT=0x281,
            IMECHAR=0x286, NOTIFY=0x282, SETFOCUS=7, KILLFOCUS=8, DESTROY=0x82;
    static final int RESULT=0x800, PREEDIT=8, CURSOR=0x80;
    static final int RETURN=13, ESCAPE=27, SHIFT=16, CONTROL=17, PROCESS=0xe5;
    private final WindowsImePlatform platform;
    private final Thread owner = Thread.currentThread();
    private final LongSupplier clock;
    private final BitSet held = new BitSet(256), consumed = new BitSet(256), inherited = new BitSet(256);
    private final ArrayDeque<TextInputSession.EditorState> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private TextInputSession.EditorState document;
    private Sink sink;
    private long session, deadline;
    private boolean active, composing, finishing, acknowledged, fault;
    private boolean blockCharacter;
    private char highSurrogate;
    private WindowsImePlatform.Caret latestCaret;

    WindowsImeSession(WindowsImePlatform platform, LongSupplier clock) {
        this.platform = Objects.requireNonNull(platform); this.clock = Objects.requireNonNull(clock);
    }
    static void warn(String message, Throwable cause) {
        Logger.getGlobal().warning("Windows IME prototype " + message + (cause == null ? "" : ": " + cause.getClass().getSimpleName()));
    }
    public boolean isAvailable() { return true; }
    public void begin(long id, TextInputSession.EditorState initial, Sink sink) {
        checkThread();
        if (active) throw new IllegalStateException("IME already active");
        session=id; document=initial; this.sink=sink;
        composing=finishing=acknowledged=fault=blockCharacter=false; highSurrogate=0;
        held.clear(); consumed.clear(); inherited.clear(); undo.clear(); redo.clear();
        active=true;
        try {
            trace("hook attach begin");
            try {
                platform.attach(this);
                trace("hook attach succeeded");
            } catch (RuntimeException | Error e) {
                trace("hook attach failed");
                throw e;
            }
            // Mouse buttons never produce WM_KEYUP. Generic modifier states duplicate left/right VKs.
            for (int key=8; key<256; key++) if (key!=SHIFT && key!=CONTROL && key!=18 && platform.keyDown(key)) {
                held.set(key); inherited.set(key);
                if (key==RETURN || key==ESCAPE) consumed.set(key);
            }
        } catch (RuntimeException | Error e) {
            active=false; this.sink=null;
            try { detach(false); } catch (RuntimeException | Error cleanup) { e.addSuppressed(cleanup); }
            throw e;
        }
    }
    public void update(long id, TextInputSession.EditorState value) {
        checkThread();
        // Native messages can be ahead of the actor's displayed snapshot. Never overwrite newer native text.
        if (current(id) && !composing && !finishing && document.text().equals(value.text())) document=value;
    }
    public void caret(long id, double x, double y, double w, double h, double cw, double ch) {
        checkThread();
        if (current(id) && !fault && Double.isFinite(x+y+w+h+cw+ch) && w>=0 && h>0 && cw>0 && ch>0) {
            latestCaret=new WindowsImePlatform.Caret(x,y,w,h,cw,ch); platform.caret(latestCaret);
        }
    }
    public void confirmComposition(long id) {
        checkThread();
        if (current(id) && composing && !platform.complete(false)) cancelComposition(id);
    }
    public void cancelComposition(long id) {
        checkThread();
        if (!current(id)) return;
        if (composing) platform.complete(true);
        composing=false; sink.cancelled(id);
    }
    public void finish(long id) {
        checkThread();
        if (!current(id) || finishing) return;
        finishing=true; deadline=clock.getAsLong()+2_000_000_000L;
        try {
            if (composing && !fault && !platform.complete(false)) {
                platform.complete(true); composing=false; sink.cancelled(id);
                warn("completion refused; saving committed draft only", null);
            }
        } catch (RuntimeException | LinkageError e) { fault=true; composing=false; sink.cancelled(id); warn("completion failed", e); }
        acknowledge();
    }
    public void poll(long id) {
        checkThread();
        if (current(id) && finishing && !acknowledged && clock.getAsLong()-deadline>=0) {
            try { platform.complete(true); } catch (RuntimeException | LinkageError e) { warn("cancel failed", e); }
            composing=false; sink.cancelled(id); acknowledge();
            warn("completion timed out; saving committed draft only", null);
        }
    }
    private void acknowledge() {
        if (active && finishing && !acknowledged && (!composing || fault)) { acknowledged=true; sink.finished(session); }
    }
    public void close(long id) {
        checkThread();
        if (!current(id)) return;
        trace(held.get(RETURN) ? "session close begin (Enter held)" : "session close begin");
        try { if (composing && !fault) platform.complete(true); }
        finally {
            active=false; sink=null; composing=false; highSurrogate=0;
            blockCharacter=!held.isEmpty();
            if (held.isEmpty()) detach(false);
            else trace("session closed; hook retained for key release or focus loss");
        }
    }
    private void trace(String event) { WindowsImeDiagnostics.event(session, event); }

    private void detach(boolean destroyed) {
        trace("hook detach begin");
        try {
            platform.detach(destroyed);
            trace("hook detach returned");
        } catch (RuntimeException | Error e) {
            trace("hook detach failed");
            throw e;
        }
    }

    void nativeRemovalResult(boolean removed) {
        trace(removed ? "RemoveWindowSubclass succeeded" : "RemoveWindowSubclass returned false");
    }
    private boolean current(long id) { return active && id==session && sink!=null; }
    private void checkThread() { if (Thread.currentThread()!=owner) throw new IllegalStateException("IME requires GLFW owner thread"); }

    /** Called only by the per-HWND subclass on its owner thread. */
    long message(int message, long w, long l) {
        checkThread();
        if (message == SETFOCUS) trace("Win32 focus gained");
        if (message == KILLFOCUS) trace(active ? "Win32 focus lost (active)" : "Win32 focus lost (passive)");
        if (message==DESTROY) {
            try { return platform.forward(message,w,l); }
            finally { active=false; composing=false; fault=true; sink=null; held.clear(); detach(true); }
        }
        if (!active) {
            if (message==KILLFOCUS) { held.clear(); detach(false); }
            if (message==KEYUP || message==SYSKEYUP) {
                int key=platform.physicalKey((int)w,l); held.clear(key);
                if (key==RETURN) trace("Enter released (passive)");
                blockCharacter=false;
                if (held.isEmpty()) detach(false);
                return platform.forward(message,w,l);
            }
            if (message==KEYDOWN || message==SYSKEYDOWN) {
                blockCharacter=held.get(platform.physicalKey((int)w,l));
                if (blockCharacter) return 0;
            }
            if ((message==CHAR || message==UNICHAR) && blockCharacter) return 0;
            return platform.forward(message,w,l);
        }
        switch (message) {
            case START -> {
                composing=true; sink.preedit(session,"",0);
                if (latestCaret!=null) platform.caret(latestCaret);
                return 0;
            }
            case COMPOSITION -> {
                var value=platform.composition(l);
                if (value.result()!=null) { markImeKeys(); insert(value.result()); composing=false; }
                if (value.preedit()!=null) {
                    composing=true; sink.preedit(session,value.preedit(),boundary(value.preedit(),value.caret()));
                } else if (l==0) { markImeKeys(); composing=false; sink.cancelled(session); }
                acknowledge(); return 0; // Do not generate a second WM_CHAR stream for GCS_RESULTSTR.
            }
            case END -> { markImeKeys(); composing=false; sink.cancelled(session); acknowledge(); return platform.forward(message,w,l); }
            case IMECHAR -> { return 0; }
            case SETCONTEXT -> { return platform.forward(message,w,l & ~0x80000000L); }
            case NOTIFY -> { if ((w==3 || w==5) && latestCaret!=null) platform.caret(latestCaret); }
            case KILLFOCUS -> {
                if (composing) cancelComposition(session);
                held.clear(); consumed.clear(); inherited.clear();
            }
            case KEYUP, SYSKEYUP -> {
                int key=platform.physicalKey((int)w,l);
                if (key==RETURN) trace("Enter released (active)");
                held.clear(key); consumed.clear(key); inherited.clear(key); blockCharacter=false;
            }
            case KEYDOWN, SYSKEYDOWN -> {
                int key=platform.physicalKey((int)w,l); if (key>=8) held.set(key);
                blockCharacter=inherited.get(key) && key!=SHIFT && key!=CONTROL && key!=18 && !(key>=160 && key<=165);
                if (composing || w==PROCESS) { if (key==RETURN || key==ESCAPE) consumed.set(key); }
                else if (!blockCharacter && !finishing) editKey(key);
            }
            case CHAR -> {
                if (composing || finishing || blockCharacter) return 0;
                char ch=(char)w;
                if (ch=='\r') { if (!consumed.get(RETURN)) sink.command(session,Command.SUBMIT,false); return 0; }
                if (ch<32 || ch==127) return 0;
                if (Character.isHighSurrogate(ch)) { highSurrogate=ch; return 0; }
                if (Character.isLowSurrogate(ch)) {
                    if (highSurrogate!=0) insert(new String(new char[]{highSurrogate,ch}));
                    highSurrogate=0; return 0;
                }
                highSurrogate=0; insert(String.valueOf(ch)); return 0;
            }
            case UNICHAR -> {
                if (w==0xffff) return 1;
                int cp=(int)w;
                if (!composing && !finishing && !blockCharacter && cp>=32 && Character.isValidCodePoint(cp)
                        && !(cp>=0xd800 && cp<=0xdfff)) insert(new String(Character.toChars(cp)));
                return 0;
            }
            default -> { }
        }
        return platform.forward(message,w,l);
    }
    void nativeFailure(Throwable e) {
        warn("message handling failed; closing the editor with its committed draft", e);
        fault=true; composing=false;
        if (active && sink!=null) {
            sink.cancelled(session);
            if (finishing) acknowledge(); else sink.command(session,Command.SUBMIT,false);
        }
    }
    private void markImeKeys() {
        if (held.get(RETURN) || platform.keyDown(RETURN)) consumed.set(RETURN);
        if (held.get(ESCAPE) || platform.keyDown(ESCAPE)) consumed.set(ESCAPE);
    }
    private static int boundary(String value,int n) {
        n=Math.max(0,Math.min(n,value.length()));
        return n>0 && n<value.length() && Character.isSurrogatePair(value.charAt(n-1),value.charAt(n)) ? n-1 : n;
    }
    private void replace(TextInputSession.EditorState next, boolean remember) {
        if (remember && !document.equals(next)) { if (undo.size()==64) undo.removeFirst(); undo.addLast(document); redo.clear(); }
        document=next; sink.committed(session,next);
    }
    private void insert(String value) {
        int a=Math.min(document.anchor(),document.cursor()), b=Math.max(document.anchor(),document.cursor());
        replaceRange(a,b,value);
    }
    private void replaceRange(int a,int b,String value) {
        value=value.replace("\r","").replace("\n","");
        String text=document.text().substring(0,a)+value+document.text().substring(b);
        replace(new TextInputSession.EditorState(text,a+value.length(),a+value.length()),true);
    }
    private void editKey(int key) {
        boolean ctrl=platform.keyDown(CONTROL) && !platform.keyDown(18), shift=platform.keyDown(SHIFT);
        String text=document.text(); int cursor=document.cursor();
        if (ctrl) {
            if (key=='A') { replace(new TextInputSession.EditorState(text,0,text.length()),false); return; }
            if (key=='Z' || key=='Y') {
                var source=key=='Y' || shift ? redo : undo;
                var target=source==undo ? redo : undo;
                if (!source.isEmpty()) { target.addLast(document); replace(source.removeLast(),false); }
                return;
            }
            if (key=='C' || key=='X' || key=='V') {
                try {
                    if (key=='V') { String paste=platform.clipboard(); if (paste!=null) insert(paste); }
                    else if (document.anchor()!=cursor) {
                        platform.clipboard(text.substring(Math.min(document.anchor(),cursor),Math.max(document.anchor(),cursor)));
                        if (key=='X') insert("");
                    }
                } catch (RuntimeException e) { warn("clipboard unavailable", e); }
                return;
            }
        }
        if (key==8 || key==46) {
            if (document.anchor()!=cursor) insert("");
            else {
                int end=cursor;
                if (key==8 && cursor>0) cursor=text.offsetByCodePoints(cursor,-1);
                if (key==46 && end<text.length()) end=text.offsetByCodePoints(end,1);
                replaceRange(cursor,end,"");
            }
        } else if (key==37 || key==39 || key==36 || key==35) {
            int next=cursor;
            if (key==36) next=0;
            else if (key==35) next=text.length();
            else if (!shift && document.anchor()!=cursor) next=key==37 ? Math.min(document.anchor(),cursor) : Math.max(document.anchor(),cursor);
            else if (key==37 && cursor>0) {
                next=text.offsetByCodePoints(cursor,-1);
                if (ctrl) {
                    while (next>0 && !Character.isLetterOrDigit(text.codePointAt(next))) next=text.offsetByCodePoints(next,-1);
                    while (next>0 && Character.isLetterOrDigit(text.codePointBefore(next))) next=text.offsetByCodePoints(next,-1);
                }
            } else if (key==39 && cursor<text.length()) {
                next=text.offsetByCodePoints(cursor,1);
                if (ctrl) {
                    while (next<text.length() && Character.isLetterOrDigit(text.codePointAt(next))) next=text.offsetByCodePoints(next,1);
                    while (next<text.length() && !Character.isLetterOrDigit(text.codePointAt(next))) next=text.offsetByCodePoints(next,1);
                }
            }
            replace(new TextInputSession.EditorState(text,shift?document.anchor():next,next),false);
        } else if (key==ESCAPE) sink.command(session,Command.CANCEL,false);
    }
}
