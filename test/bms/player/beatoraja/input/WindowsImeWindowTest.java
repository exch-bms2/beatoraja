package bms.player.beatoraja.input;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import org.lwjgl.system.SharedLibrary;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.JNI.*;

/** Windows-only HWND/API smoke test. SendMessage is synthetic, not real Japanese IME automation. */
public final class WindowsImeWindowTest {
    public static void main(String[] args) {
        if (!System.getProperty("os.name").startsWith("Windows")) throw new IllegalStateException("Windows required");
        System.setProperty("beatoraja.windowsIme","true");
        var config=new Lwjgl3ApplicationConfiguration();
        config.setTitle("Windows IMM prototype smoke test"); config.setWindowedMode(480,270);
        config.disableAudio(true); config.setForegroundFPS(60);
        new Lwjgl3Application(new ApplicationAdapter() {
            int frames; TextInputSession session;
            @Override public void render() {
                if (++frames==10) {
                    ImeBridge bridge=WindowsImeBridge.create();
                    check(bridge instanceof WindowsImeBridge,"Windows adapter selected");
                    session=new TextInputSession(bridge,new TextInputSession.EditorState("",0,0));
                    Object nativeAccess=roots().iterator().next();
                    SharedLibrary user=(SharedLibrary)field(nativeAccess,"user");
                    long hwnd=(long)field(nativeAccess,"hwnd");
                    long send=user.getFunctionAddress("SendMessageW");
                    message(hwnd,WindowsImeSession.CHAR,'a',0,send);
                    message(hwnd,WindowsImeSession.CHAR,0xd842,0,send);
                    message(hwnd,WindowsImeSession.CHAR,0xdfb7,0,send);
                    session.drain(); check(session.state().text().equals("a\ud842\udfb7"),"HWND callback receives Unicode without duplicate characters");
                    bridge.caret(session.id(),80,40,2,20,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
                    candidate(nativeAccess);
                    message(hwnd,WindowsImeSession.START,0,0,send);
                    message(hwnd,WindowsImeSession.KEYDOWN,13,0,send);
                    message(hwnd,WindowsImeSession.END,0,0,send);
                    message(hwnd,WindowsImeSession.CHAR,13,0,send);
                    session.drain(); check(!session.isFinished(),"IME Return cannot submit across END");
                    message(hwnd,WindowsImeSession.KEYUP,13,0,send);
                    message(hwnd,WindowsImeSession.KEYDOWN,13,0,send);
                    message(hwnd,WindowsImeSession.CHAR,13,0,send);
                    session.drain(); check(session.isFinished(),"Separate Return submits");
                    message(hwnd,WindowsImeSession.KEYDOWN,13,1L<<30,send);
                    message(hwnd,WindowsImeSession.KEYUP,13,0,send);
                    // Cleanup is posted, not freed from its own SUBCLASSPROC stack.
                } else if (frames==12) {
                    check(roots().isEmpty(),"Native hook, callback and DLL references released");
                    Gdx.app.exit();
                }
            }
            @Override public void dispose() { if (session!=null) session.close(); }
        },config);
        System.out.println("WindowsImeWindowTest passed (synthetic HWND/IMM context/candidate metadata, Unicode, callback cleanup; actual IME still untested)");
    }
    private static void message(long hwnd,int code,long w,long l,long function) { callPPPP(hwnd,code,w,l,function); }
    private static void candidate(Object access) {
        long hwnd=(long)field(access,"hwnd");
        SharedLibrary imm=(SharedLibrary)field(access,"imm");
        long context=callPP(hwnd,imm.getFunctionAddress("ImmGetContext"));
        check(context!=0,"Borrowed IMM context");
        try (var stack=MemoryStack.stackPush()) {
            var form=stack.calloc(32);
            check(callPPI(context,0,memAddress(form),imm.getFunctionAddress("ImmGetCandidateWindow"))!=0,"Candidate form query");
            check(form.getInt(4)==0x80 && form.getInt(24)>=form.getInt(16) && form.getInt(28)>form.getInt(20),"Candidate exclusion rectangle");
        } finally { callPPI(hwnd,context,imm.getFunctionAddress("ImmReleaseContext")); }
    }
    private static java.util.Set<?> roots() {
        try { var f=WindowsImeNative.class.getDeclaredField("ROOTS"); f.setAccessible(true); return (java.util.Set<?>)f.get(null); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static Object field(Object object,String name) {
        try { var f=object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void check(boolean value,String message) { if (!value) throw new AssertionError(message); }
}
