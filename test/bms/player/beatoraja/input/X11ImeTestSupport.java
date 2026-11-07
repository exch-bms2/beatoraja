package bms.player.beatoraja.input;

import bms.player.beatoraja.input.text.ImeBridge;

import java.nio.charset.StandardCharsets;

/** Fake XIM boundary shared by standalone and real-GL SkinTextInput regressions. */
public final class X11ImeTestSupport {
    public Fake current;
    public boolean failNextAttach;
    public final ImeBridge bridge = new X11ImeBridge(() -> current = new Fake(failNextAttach));

    public static final class Fake implements X11ImePlatform {
        private Listener listener;
        private final boolean failAttach;
        public int detached, resets;
        public String clipboard = "";
        public boolean failRead, failWrite;
        public double[] geometry;
        Fake(boolean fail) { failAttach = fail; }
        public void attach(Listener listener) {
            this.listener = listener;
            if (failAttach) throw new IllegalStateException("Expected XIM attach failure");
        }
        public void reset() { resets++; end(); }
        public void close() { detached++; }
        public void caret(Caret value) { geometry = new double[]{value.x(),value.y(),value.width(),value.height(),value.clientWidth(),value.clientHeight()}; }
        public String clipboard() { if (failRead) throw new IllegalStateException("Expected clipboard failure"); return clipboard; }
        public void clipboard(String value) { if (failWrite) throw new IllegalStateException("Expected clipboard failure"); clipboard = value; }
        public void preedit(String value) { preedit(value,value.codePointCount(0,value.length())); }
        public void preedit(String value,int caret) { send(PREEDIT,value.getBytes(StandardCharsets.UTF_8),caret,0,false); }
        public void commit(String value) { send(COMMIT,value.getBytes(StandardCharsets.UTF_8),0,0,false); }
        public void end() { send(END,null,0,0,false); }
        public void key(int key,int mods,boolean consumed) { send(KEY,null,key,mods,consumed); }
        public void release(int key) { send(RELEASE,null,key,0,false); }
        public void focusLost() { send(FOCUS_LOST,null,0,0,false); }
        public void lost() { send(LOST,null,0,0,false); }
        public void send(int kind,byte[] text,int first,int second,boolean used) { listener.event(kind,text,first,second,used); }
    }
}
