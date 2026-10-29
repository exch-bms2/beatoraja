package bms.player.beatoraja.input;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.badlogic.gdx.Gdx;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.SharedLibrary;
import org.lwjgl.system.windows.WindowsLibrary;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.JNI.*;

/** Win32/IMM calls using existing LWJGL stubs only; initialized on Windows opt-in. */
final class WindowsImeNative implements WindowsImePlatform {
    private static final Set<WindowsImeNative> ROOTS = ConcurrentHashMap.newKeySet();
    private final long hwnd;
    private final ArrayList<SharedLibrary> libraries = new ArrayList<>();
    private SharedLibrary user, imm, controls;
    private WindowsImeCallback callback;
    private boolean installed, disposed, destroyed, deferred;
    private boolean candidateWarning;
    private int depth;
    private final long getContext, releaseContext, getComposition, notify, candidate;
    private final long setSubclass, removeSubclass, defSubclass, getClient, keyState, mapKey;

    WindowsImeNative(long hwnd) {
        this.hwnd=hwnd;
        try {
            if (hwnd==0 || org.lwjgl.system.Pointer.POINTER_SIZE!=8) throw new IllegalStateException("A 64-bit HWND is required");
            // kernel32 is a Windows KnownDLL. All other DLL paths come from the OS, not cwd/PATH.
            SharedLibrary kernel=load("kernel32.dll");
            String directory;
            try (var stack=MemoryStack.stackPush()) {
                ByteBuffer buffer=stack.calloc(520);
                int count=callPI(memAddress(buffer),260,function(kernel,"GetSystemDirectoryW"));
                if (count<=0 || count>=260) throw new IllegalStateException("Cannot resolve System32");
                directory=memUTF16(memAddress(buffer),count);
            }
            user=load(Path.of(directory,"user32.dll").toString());
            imm=load(Path.of(directory,"imm32.dll").toString());
            controls=load(Path.of(directory,"comctl32.dll").toString());
            int thread=callPPI(hwnd,0L,function(user,"GetWindowThreadProcessId"));
            if (thread!=callI(function(kernel,"GetCurrentThreadId"))) throw new IllegalStateException("HWND belongs to another thread");
            getContext=function(imm,"ImmGetContext"); releaseContext=function(imm,"ImmReleaseContext");
            getComposition=function(imm,"ImmGetCompositionStringW"); notify=function(imm,"ImmNotifyIME");
            candidate=function(imm,"ImmSetCandidateWindow");
            setSubclass=function(controls,"SetWindowSubclass"); removeSubclass=function(controls,"RemoveWindowSubclass");
            defSubclass=function(controls,"DefSubclassProc"); getClient=function(user,"GetClientRect");
            keyState=function(user,"GetAsyncKeyState"); mapKey=function(user,"MapVirtualKeyW");
        } catch (RuntimeException | Error e) { freeLibraries(); throw e; }
    }
    private SharedLibrary load(String name) { var library=new WindowsLibrary(name); libraries.add(library); return library; }
    private static long function(SharedLibrary library,String name) {
        long address=library.getFunctionAddress(name);
        if (address==0) throw new UnsatisfiedLinkError("Missing Win32 API: "+name);
        return address;
    }
    public void attach(WindowsImeSession bridge) {
        if (disposed || installed) throw new IllegalStateException("IME native hook already used");
        long himc=callPP(hwnd,getContext);
        if (himc==0) throw new IllegalStateException("No IMM context on this window");
        callPPI(hwnd,himc,releaseContext);
        callback=new WindowsImeCallback((window,message,w,l,id,data)-> {
            depth++;
            try { return bridge.message(message,w,l); }
            catch (Throwable error) {
                try { bridge.nativeFailure(error); } catch (Throwable ignored) { WindowsImeSession.warn("failure reporting failed",ignored); }
                return forward(message,w,l);
            } finally { depth--; }
        });
        ROOTS.add(this);
        if (callPPPPI(hwnd,callback.address(),callback.address(),0L,setSubclass)==0) {
            ROOTS.remove(this); callback.free(); callback=null;
            throw new IllegalStateException("SetWindowSubclass failed");
        }
        installed=true;
    }
    public long forward(int message,long w,long l) { return callPPPP(hwnd,message,w,l,defSubclass); }
    public Composition composition(long flags) {
        long context=callPP(hwnd,getContext);
        if (context==0) throw new IllegalStateException("IMM context lost");
        try {
            String result=(flags & WindowsImeSession.RESULT)!=0 ? string(context,WindowsImeSession.RESULT) : null;
            String text=(flags & WindowsImeSession.PREEDIT)!=0 ? string(context,WindowsImeSession.PREEDIT) : null;
            int cursor=text==null ? 0 : callPPI(context,WindowsImeSession.CURSOR,0L,0,getComposition);
            return new Composition(result,text,cursor);
        } finally { callPPI(hwnd,context,releaseContext); }
    }
    private String string(long context,int flag) {
        int bytes=callPPI(context,flag,0L,0,getComposition);
        if (bytes<0 || bytes>2*1024*1024 || (bytes & 1)!=0) throw new IllegalStateException("Invalid IMM string size");
        if (bytes==0) return "";
        ByteBuffer data=memAlloc(bytes);
        try {
            int actual=callPPI(context,flag,memAddress(data),bytes,getComposition);
            if (actual<0 || actual>bytes || (actual & 1)!=0) throw new IllegalStateException("IMM string changed while reading");
            return memUTF16(memAddress(data),actual/2);
        } finally { memFree(data); }
    }
    public boolean complete(boolean cancel) {
        if (disposed || destroyed) return false;
        long context=callPP(hwnd,getContext);
        if (context==0) return false;
        try { return callPI(context,0x15,cancel?4:1,0,notify)!=0; }
        finally { callPPI(hwnd,context,releaseContext); }
    }
    public boolean keyDown(int key) { return (callI(key,keyState) & 0x8000)!=0; }
    public int physicalKey(int key,long l) {
        int scan=(int)((l>>>16)&0xff);
        int physical=key==WindowsImeSession.PROCESS || key==16 ? callI(scan,3,mapKey) : key;
        if (physical==17) physical=(l & (1L<<24))!=0 ? 163 : 162;
        if (physical==18) physical=(l & (1L<<24))!=0 ? 165 : 164;
        return physical>=0 && physical<256 ? physical : 0;
    }
    public String clipboard() { return Gdx.app.getClipboard().getContents(); }
    public void clipboard(String text) { Gdx.app.getClipboard().setContents(text); }
    public void caret(Caret value) {
        if (disposed || destroyed) return;
        try (var stack=MemoryStack.stackPush()) {
            ByteBuffer rect=stack.calloc(16);
            if (callPPI(hwnd,memAddress(rect),getClient)==0) return;
            int width=rect.getInt(8)-rect.getInt(0), height=rect.getInt(12)-rect.getInt(4);
            if (width<=0 || height<=0) return;
            int[] pixels=value.nativeRectangle(width,height);
            ByteBuffer form=stack.calloc(32); // CANDIDATEFORM: DWORD,DWORD,POINT,RECT.
            form.putInt(0,0).putInt(4,0x80).putInt(8,pixels[0]).putInt(12,pixels[3]);
            for (int i=0; i<4; i++) form.putInt(16+i*4,pixels[i]);
            long context=callPP(hwnd,getContext);
            if (context==0) return;
            try {
                if (callPPI(context,memAddress(form),candidate)==0 && !candidateWarning) {
                    candidateWarning=true; WindowsImeSession.warn("candidate position request refused",null);
                }
            } finally { callPPI(hwnd,context,releaseContext); }
        }
    }
    public void detach(boolean windowDestroyed) {
        if (disposed) return;
        destroyed |= windowDestroyed;
        if (installed) {
            boolean removed=callPPPI(hwnd,callback.address(),callback.address(),removeSubclass)!=0;
            if (!removed && !destroyed) { WindowsImeSession.warn("detach refused; retaining callback until window destruction",null); return; }
            installed=false;
        }
        if (depth==0) dispose();
        else if (!deferred) {
            deferred=true;
            // A libffi trampoline must survive until the native callback has actually returned.
            Gdx.app.postRunnable(this::dispose);
        }
    }
    private void dispose() {
        if (disposed || installed) return;
        if (depth!=0) throw new IllegalStateException("Cannot free an executing SUBCLASSPROC");
        disposed=true; ROOTS.remove(this);
        if (callback!=null) { callback.free(); callback=null; }
        freeLibraries();
    }
    private void freeLibraries() { for (int i=libraries.size()-1; i>=0; i--) libraries.get(i).free(); libraries.clear(); }
}
