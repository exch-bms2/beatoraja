package bms.player.beatoraja.input;

/** Native libffi round-trip; validates marshaling, NOT the Windows OS or its IME. */
public final class WindowsImeCallbackTest {
    public static void main(String[] args) {
        long hwnd=0x123456789L, w=0x23456789aL, l=0x3456789abL, id=0x456789abcL, data=0x56789abcdL;
        try (var callback=new WindowsImeCallback((h,m,wp,lp,i,d)-> {
            if (h!=hwnd || m!=0x80000001 || wp!=w || lp!=l || i!=id || d!=data)
                throw new AssertionError("SUBCLASSPROC arguments truncated/reordered");
            return 0x123456789abcL;
        })) {
            long value=org.lwjgl.system.JNI.callPPPPPP(hwnd,0x80000001,w,l,id,data,callback.address());
            if (value!=0x123456789abcL) throw new AssertionError("SUBCLASSPROC result truncated");
        }
        System.out.println("WindowsImeCallbackTest passed (native six-argument FFI round-trip on host ABI; not Windows validation)");
    }
}
