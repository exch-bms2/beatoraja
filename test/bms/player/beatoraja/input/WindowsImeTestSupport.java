package bms.player.beatoraja.input;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicLong;

/** Test-only IMM transport. Neither a Windows DLL nor the user's clipboard is accessed. */
public final class WindowsImeTestSupport {
    public final AtomicLong clock=new AtomicLong();
    public Fake current;
    public boolean failNextAttach;
    public final ImeBridge bridge=new WindowsImeBridge(()-> {
        current=new Fake(); current.failAttach=failNextAttach; failNextAttach=false; return current;
    }, clock::get);
    public static final class Fake implements WindowsImePlatform {
        WindowsImeSession handler;
        Composition value=new Composition(null,null,0);
        public final BitSet down=new BitSet(256);
        public boolean synchronous=true, refuse, throwComplete, failWrite, failRead, failAttach;
        public int detached, forwards, completes, cancels;
        public long lastForwardL;
        public String clipboard="", preedit="";
        public Caret geometry;
        public double[] geometryValues() {
            return new double[]{geometry.x(),geometry.y(),geometry.width(),geometry.height(),geometry.clientWidth(),geometry.clientHeight()};
        }
        public void attach(WindowsImeSession handler) { this.handler=handler; if (failAttach) throw new IllegalStateException("Injected attach failure"); }
        public long forward(int m,long w,long l) { forwards++; lastForwardL=l; return 123; }
        public Composition composition(long flags) { return value; }
        public boolean complete(boolean cancel) {
            if (throwComplete) throw new IllegalStateException("Injected IMM failure");
            if (cancel) { cancels++; end(); return true; }
            completes++;
            if (refuse) return false;
            if (synchronous) { result(preedit); end(); }
            return true;
        }
        public boolean keyDown(int key) { return down.get(key); }
        public int physicalKey(int key,long l) { return key==WindowsImeSession.PROCESS ? (int)(l & 255) : key; }
        public String clipboard() { if (failRead) throw new IllegalStateException("Injected clipboard failure"); return clipboard; }
        public void clipboard(String text) { if (failWrite) throw new IllegalStateException("Injected clipboard failure"); clipboard=text; }
        public void caret(Caret rectangle) { geometry=rectangle; }
        public void detach(boolean destroyed) { detached++; }
        public long send(int message,long w,long l) { return handler.message(message,w,l); }
        public void start() { send(WindowsImeSession.START,0,0); }
        public void preedit(String text) {
            preedit=text; value=new Composition(null,text,text.length());
            send(WindowsImeSession.COMPOSITION,0,WindowsImeSession.PREEDIT|WindowsImeSession.CURSOR);
        }
        public void result(String text) {
            value=new Composition(text,null,0); send(WindowsImeSession.COMPOSITION,0,WindowsImeSession.RESULT);
        }
        public void resultAndPreedit(String result,String text) {
            preedit=text; value=new Composition(result,text,text.length());
            send(WindowsImeSession.COMPOSITION,0,WindowsImeSession.RESULT|WindowsImeSession.PREEDIT);
        }
        public void end() { send(WindowsImeSession.END,0,0); }
        public void key(int key,boolean pressed) {
            down.set(key,pressed); send(pressed?WindowsImeSession.KEYDOWN:WindowsImeSession.KEYUP,key,0);
        }
        public void text(String text) { for (char ch : text.toCharArray()) send(WindowsImeSession.CHAR,ch,0); }
    }
}
