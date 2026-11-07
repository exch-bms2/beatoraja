package bms.player.beatoraja.input.text.ime;

import java.lang.invoke.MethodHandles;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.Callback;
import org.lwjgl.system.CallbackI;
import org.lwjgl.system.Pointer;
import static org.lwjgl.system.libffi.LibFFI.*;
import static org.lwjgl.system.MemoryUtil.*;

/** LWJGL 3.4 descriptor and marshaling for the six-argument Win32 SUBCLASSPROC. */
@FunctionalInterface
interface WindowsImeCallbackI extends CallbackI {
    Callback.Descriptor DESCRIPTOR = new Callback.Descriptor(WindowsImeCallbackI.class, MethodHandles.lookup(),
            APIUtil.apiCreateCIF(APIUtil.apiStdcall(), ffi_type_pointer,
                    ffi_type_pointer, ffi_type_uint32, ffi_type_pointer, ffi_type_pointer, ffi_type_pointer, ffi_type_pointer));

    @Override default Callback.Descriptor getDescriptor() { return DESCRIPTOR; }

    @Override default void callback(long result,long args) {
        APIUtil.apiClosureRetP(result, invoke(address(args,0),
                memGetInt(memGetAddress(args+Pointer.POINTER_SIZE)), address(args,2), address(args,3), address(args,4), address(args,5)));
    }

    long invoke(long hwnd,int message,long w,long l,long id,long data);

    private static long address(long args,int n) { return memGetAddress(memGetAddress(args+(long)n*Pointer.POINTER_SIZE)); }
}
