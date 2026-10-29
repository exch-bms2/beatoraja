package bms.player.beatoraja.input;

import org.lwjgl.system.APIUtil;
import org.lwjgl.system.Callback;
import org.lwjgl.system.CallbackI;
import org.lwjgl.system.Pointer;
import org.lwjgl.system.libffi.FFICIF;
import static org.lwjgl.system.libffi.LibFFI.*;
import static org.lwjgl.system.MemoryUtil.*;

/** Exact six-argument SUBCLASSPROC ABI, not the four-argument GLFW WindowProc ABI. */
final class WindowsImeCallback extends Callback implements CallbackI {
    interface Handler { long invoke(long hwnd,int message,long w,long l,long id,long data); }
    private static final FFICIF CIF = APIUtil.apiCreateCIF(APIUtil.apiStdcall(), ffi_type_pointer,
            ffi_type_pointer, ffi_type_uint32, ffi_type_pointer, ffi_type_pointer, ffi_type_pointer, ffi_type_pointer);
    private final Handler handler;
    WindowsImeCallback(Handler handler) { super(CIF); this.handler=handler; }
    public FFICIF getCallInterface() { return CIF; }
    public void callback(long result,long args) {
        APIUtil.apiClosureRetP(result, handler.invoke(address(args,0),
                memGetInt(memGetAddress(args+Pointer.POINTER_SIZE)), address(args,2), address(args,3), address(args,4), address(args,5)));
    }
    private static long address(long args,int n) { return memGetAddress(memGetAddress(args+(long)n*Pointer.POINTER_SIZE)); }
}
