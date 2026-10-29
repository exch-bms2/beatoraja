package bms.player.beatoraja.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import org.lwjgl.glfw.GLFWNativeWin32;

/** Opt-in Windows IMM adapter; each focus session owns its own native hook/state. */
public final class WindowsImeBridge implements ImeBridge {
    private final java.util.function.Supplier<WindowsImePlatform> platforms;
    private final java.util.function.LongSupplier clock;
    private final Thread owner=Thread.currentThread();
    private WindowsImeSession worker;
    private long session;
    private WindowsImeBridge(long hwnd) {
        platforms=()->new WindowsImeNative(hwnd); clock=System::nanoTime;
    }
    WindowsImeBridge(java.util.function.Supplier<WindowsImePlatform> platforms, java.util.function.LongSupplier clock) {
        this.platforms=platforms; this.clock=clock;
    }
    public static ImeBridge create() {
        if (!Boolean.getBoolean("beatoraja.windowsIme") || !System.getProperty("os.name", "").startsWith("Windows")
                || !(Gdx.graphics instanceof Lwjgl3Graphics graphics)) return NONE;
        try {
            long hwnd=GLFWNativeWin32.glfwGetWin32Window(graphics.getWindow().getWindowHandle());
            return hwnd==0 ? NONE : new WindowsImeBridge(hwnd);
        } catch (RuntimeException | LinkageError e) {
            WindowsImeSession.warn("unavailable; using GLFW text input",e); return NONE;
        }
    }
    public boolean isAvailable() { return true; }
    public void begin(long id,TextInputSession.EditorState state,Sink sink) {
        checkThread();
        if (worker!=null) throw new IllegalStateException("IME already active");
        session=id;
        worker=new WindowsImeSession(platforms.get(),clock);
        worker.begin(id,state,sink);
    }
    public void update(long id,TextInputSession.EditorState state) { if (worker!=null) worker.update(id,state); }
    public void caret(long id,double x,double y,double w,double h,double cw,double ch) {
        if (worker!=null) worker.caret(id,x,y,w,h,cw,ch);
    }
    public void poll(long id) { if (worker!=null) worker.poll(id); }
    public void confirmComposition(long id) { if (worker!=null) worker.confirmComposition(id); }
    public void cancelComposition(long id) { if (worker!=null) worker.cancelComposition(id); }
    public void finish(long id) { if (worker!=null) worker.finish(id); }
    public void close(long id) {
        checkThread();
        if (worker==null || session!=id) return;
        try { worker.close(id); } finally { worker=null; }
    }
    private void checkThread() { if (Thread.currentThread()!=owner) throw new IllegalStateException("IME requires GLFW owner thread"); }
}
