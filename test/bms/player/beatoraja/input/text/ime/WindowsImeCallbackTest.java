package bms.player.beatoraja.input.text.ime;

/** Native libffi round-trip; validates marshaling, NOT the Windows OS or its IME. */
public final class WindowsImeCallbackTest {
    public static void main(String[] args) {
        long hwnd=0x123456789L, w=0x23456789aL, l=0x3456789abL, id=0x456789abcL, data=0x56789abcdL;
        for (int cycle=0; cycle<64; cycle++) {
            long expected=(cycle&1)==0 ? 0x123456789abcL : -1L;
            try (var callback=new WindowsImeCallback((h,m,wp,lp,i,d)-> {
                if (h!=hwnd || m!=0x80000001 || wp!=w || lp!=l || i!=id || d!=data)
                    throw new AssertionError("SUBCLASSPROC arguments truncated/reordered");
                return expected;
            })) {
                if (org.lwjgl.system.Callback.get(callback.address())!=callback)
                    throw new AssertionError("SUBCLASSPROC closure registry changed");
                long value=org.lwjgl.system.JNI.callPPPPPP(hwnd,0x80000001,w,l,id,data,callback.address());
                if (value!=expected) throw new AssertionError("SUBCLASSPROC result truncated");
            }
        }
        System.out.println("WindowsImeCallbackTest passed (64 native six-argument FFI/registry/free round-trips on host ABI; not Windows validation)");
    }
}
