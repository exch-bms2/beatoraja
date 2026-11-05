package bms.player.beatoraja.input;

import org.lwjgl.system.Callback;

/** Exact six-argument SUBCLASSPROC ABI, not the four-argument GLFW WindowProc ABI. */
final class WindowsImeCallback extends Callback implements WindowsImeCallbackI {
    private final WindowsImeCallbackI handler;
    WindowsImeCallback(WindowsImeCallbackI handler) { super(DESCRIPTOR); this.handler=handler; }
    @Override public long invoke(long hwnd,int message,long w,long l,long id,long data) {
        return handler.invoke(hwnd,message,w,l,id,data);
    }
}
