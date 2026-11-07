package bms.player.beatoraja.input;

import bms.player.beatoraja.input.text.ImeBridge;
import bms.player.beatoraja.input.text.TextInputSession;

import static org.lwjgl.glfw.GLFW.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/** Synthetic XIM semantics; no Linux/display/IME server is needed. */
public final class X11ImeBridgeTest {
    public static void main(String[] args) throws Exception {
        String flag = System.getProperty("beatoraja.x11Ime");
        try {
            System.clearProperty("beatoraja.x11Ime"); check(X11ImeBridge.create() == ImeBridge.NONE,"Default opt-out");
            System.setProperty("beatoraja.x11Ime","true"); check(X11ImeBridge.create() == ImeBridge.NONE,"Unavailable without GLFW graphics");
        } finally { if (flag == null) System.clearProperty("beatoraja.x11Ime"); else System.setProperty("beatoraja.x11Ime",flag); }
        var support = new X11ImeTestSupport();
        var session = new TextInputSession(support.bridge,new TextInputSession.EditorState("abc",1,2));
        var p = support.current;
        p.preedit("\u304b\ud842\udfb7",1);
        check(!session.isComposing(),"Callbacks enqueue only"); session.drain();
        check(session.displayText().equals("a\u304b\ud842\udfb7c") && session.displayCursor() == 2,"Preedit selection/caret map code points to UTF-16");
        p.end(); session.drain(); check(session.state().text().equals("abc") && !session.isComposing(),"End does not insert cancelled preedit");
        p.preedit("\u304b"); p.end(); p.commit("\u6f22\ud842\udfb7");
        p.key(GLFW_KEY_ENTER,0,true); session.drain();
        check(session.state().text().equals("a\u6f22\ud842\udfb7c") && !session.isFinishing(),"Commit/end before consumed Enter cannot submit");
        p.key(GLFW_KEY_ENTER,0,false); session.drain(); check(!session.isFinishing(),"Consumed Enter repeats guarded through release");
        p.release(GLFW_KEY_ENTER);
        p.preedit("cancel"); p.key(GLFW_KEY_ESCAPE,0,true); p.end(); session.drain();
        check(!session.isComposing() && !session.isFinished(),"IME Escape cancels without closing editor"); p.release(GLFW_KEY_ESCAPE);
        p.commit("old"); p.preedit("next"); session.drain();
        check(session.isComposing() && session.displayText().contains("oldnext"),"Commit followed by next preedit remains visible");
        p.focusLost(); session.drain();
        check(!session.isComposing() && !session.isFinished() && !session.state().text().contains("next"),"Window focus loss cancels only unconfirmed text");
        p.preedit("discard"); session.drain(); session.requestFinish(); session.drain();
        check(session.isFinished() && !session.state().text().contains("discard") && p.detached == 1,"Field transfer saves committed draft and releases XIC");
        var next = new TextInputSession(support.bridge,new TextInputSession.EditorState("next",4,4));
        p.commit("stale"); p.preedit("late"); p.key(GLFW_KEY_ENTER,0,false); next.drain();
        check(next.state().text().equals("next") && !next.isFinished(),"Old native listener cannot mutate next session");
        next.close(); next.close(); check(support.current.detached == 1,"Close is idempotent");
        editing(); geometry(); failures();
        System.out.println("X11ImeBridgeTest passed (fake XIM, Unicode, commands/focus/stale events, editing, HiDPI; NO X11 runtime)");
    }
    private static void editing() {
        var support = new X11ImeTestSupport();
        var session = new TextInputSession(support.bridge,new TextInputSession.EditorState("",0,0));
        var p = support.current;
        p.commit("\ud842\udfb7"); p.key(GLFW_KEY_BACKSPACE,0,false); session.drain(); check(session.state().text().isEmpty(),"Code point deletion");
        p.key(GLFW_KEY_Z,GLFW_MOD_CONTROL,false); session.drain(); check(session.state().text().equals("\ud842\udfb7"),"Undo");
        p.key(GLFW_KEY_A,GLFW_MOD_CONTROL,false); p.failWrite = true; p.key(GLFW_KEY_X,GLFW_MOD_CONTROL,false); session.drain();
        check(session.state().text().equals("\ud842\udfb7"),"Failed cut preserves text");
        p.failRead = true; p.key(GLFW_KEY_V,GLFW_MOD_CONTROL,false); session.drain(); check(session.state().text().equals("\ud842\udfb7"),"Failed paste preserves selection");
        p.failRead = false; p.clipboard = "\u65e5\r\nabc"; p.key(GLFW_KEY_V,GLFW_MOD_CONTROL,false); session.drain();
        check(session.state().text().equals("\u65e5abc"),"Single-line clipboard policy");
        p.key(GLFW_KEY_HOME,0,false); p.key(GLFW_KEY_RIGHT,GLFW_MOD_SHIFT,false); p.commit("X"); session.drain();
        check(session.state().text().equals("Xabc"),"Selection editing");
        p.key(GLFW_KEY_ENTER,0,false); session.drain(); check(session.isFinished(),"Separate unconsumed Enter submits");
    }
    private static void geometry() {
        var support = new X11ImeTestSupport();
        var session = new TextInputSession(support.bridge,new TextInputSession.EditorState("",0,0));
        support.bridge.caret(session.id(),0,0,2,20,0,0); check(support.current.geometry == null,"Minimized/zero client ignored");
        support.bridge.caret(session.id(),80,40,2,20,640,480); check(support.current.geometry[4] == 640,"Caret remains logical");
        var caret = new X11ImePlatform.Caret(80,40,2,20,640,480);
        check(Arrays.equals(caret.nativeRectangle(640,480),new int[]{80,40,82,60}),"100% native client");
        check(Arrays.equals(caret.nativeRectangle(800,600),new int[]{100,50,103,75}),"125% native client");
        check(Arrays.equals(caret.nativeRectangle(1280,960),new int[]{160,80,164,120}),"200% exactly once");
        session.close();
    }
    private static void failures() throws Exception {
        var support = new X11ImeTestSupport(); support.failNextAttach = true;
        try { new TextInputSession(support.bridge,new TextInputSession.EditorState("",0,0)); throw new AssertionError("Expected failed attach"); }
        catch (IllegalStateException expected) { check(support.current.detached == 1,"Failed attach releases platform"); }
        support = new X11ImeTestSupport();
        var session = new TextInputSession(support.bridge,new TextInputSession.EditorState("safe",4,4));
        support.current.send(X11ImePlatform.COMMIT,new byte[]{(byte)0xc0,(byte)0xaf},0,0,false); session.drain();
        check(session.state().text().equals("safe"),"Malformed UTF-8 cannot corrupt draft");
        session.requestFinish(); session.drain(); check(session.isFinished(),"Faulted context can finish");
        support = new X11ImeTestSupport();
        session = new TextInputSession(support.bridge,new TextInputSession.EditorState("safe",4,4));
        support.current.preedit("discard"); support.current.lost(); session.drain();
        check(session.state().text().equals("safe") && !session.isComposing(),"IM server loss preserves draft");
        var failure = new AtomicReference<Throwable>(); var bridge = support.bridge;
        Thread thread = new Thread(() -> { try { bridge.close(0); } catch (Throwable e) { failure.set(e); } });
        thread.start(); thread.join(); check(failure.get() instanceof IllegalStateException,"Owner-thread confinement"); session.close();
    }
    private static void check(boolean value,String message) { if (!value) throw new AssertionError(message); }
}
