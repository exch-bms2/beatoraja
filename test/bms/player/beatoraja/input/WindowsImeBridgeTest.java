package bms.player.beatoraja.input;

import static bms.player.beatoraja.input.WindowsImeSession.*;
import java.util.Arrays;

/** Synthetic IMM ordering/model/lifecycle tests, runnable on every supported OS. */
public final class WindowsImeBridgeTest {
    public static void main(String[] args) throws Exception {
        String flag=System.getProperty("beatoraja.windowsIme");
        try {
            System.clearProperty("beatoraja.windowsIme"); check(WindowsImeBridge.create()==ImeBridge.NONE,"Default opt-out");
            System.setProperty("beatoraja.windowsIme","true"); check(WindowsImeBridge.create()==ImeBridge.NONE,"Headless/other OS fallback");
        } finally { if (flag==null) System.clearProperty("beatoraja.windowsIme"); else System.setProperty("beatoraja.windowsIme",flag); }
        var support=new WindowsImeTestSupport();
        var session=new TextInputSession(support.bridge,new TextInputSession.EditorState("abc",1,2));
        var nativeIme=support.current;
        nativeIme.start(); nativeIme.preedit("\u304b\u306a");
        check(!session.isComposing(),"Callback queues only"); session.drain();
        check(session.displayText().equals("a\u304b\u306ac") && session.state().text().equals("abc"),"Preedit replaces selection visually");
        nativeIme.end(); session.drain(); check(session.displayText().equals("abc"),"Cancel preserves committed selection");
        nativeIme.start(); nativeIme.preedit("\u304b\u306a");
        nativeIme.send(KEYDOWN,PROCESS,RETURN); nativeIme.result("\u4eee"); nativeIme.end(); nativeIme.text("\r"); session.drain();
        check(!session.isFinishing() && session.state().text().equals("a\u4eeec"),"Conversion Return cannot submit after END");
        nativeIme.key(RETURN,false);
        nativeIme.start(); nativeIme.down.set(RETURN); nativeIme.result("x"); nativeIme.end();
        nativeIme.key(RETURN,true); nativeIme.text("\r"); session.drain();
        check(!session.isFinishing(),"Physical Return guards result/end preceding keydown");
        nativeIme.key(RETURN,false);
        nativeIme.start(); nativeIme.resultAndPreedit("A","B"); session.drain();
        check(session.isComposing() && session.displayText().contains("AB"),"Result and next composition in one message");
        nativeIme.end(); session.drain();
        nativeIme.key(RETURN,true); nativeIme.text("\r"); session.drain();
        check(session.isFinished(),"Separate Return submits");
        check(nativeIme.detached==0,"Held Return keeps passive callback alive");
        check(nativeIme.send(KEYDOWN,RETURN,1L<<30)==0 && nativeIme.send(CHAR,13,0)==0,"Post-close repeat cannot reach game");
        check(nativeIme.send(KEYDOWN,'B',0)==123 && nativeIme.send(CHAR,'b',0)==123,"Passive guard does not swallow fresh unrelated input");
        nativeIme.key(RETURN,false); check(nativeIme.detached==1,"Release detaches passive callback");
        String saved=session.state().text();
        var next=new TextInputSession(support.bridge,new TextInputSession.EditorState("next",4,4));
        nativeIme.result("old message"); next.drain(); session.drain();
        check(next.state().text().equals("next") && session.state().text().equals(saved),"Per-focus hook isolates late messages");
        next.close();
        editing(); asynchronousFinish(); inheritedKeys(); geometry();
        System.out.println("WindowsImeBridgeTest passed (synthetic IMM, Enter/cancel, editing, focus/stale callbacks, bounded finish, HiDPI math; NO Win32 runtime)");
    }
    private static void editing() {
        var support=new WindowsImeTestSupport();
        var session=new TextInputSession(support.bridge,new TextInputSession.EditorState("",0,0));
        var p=support.current;
        p.text("\ud842\udfb7"); session.drain(); check(session.state().text().equals("\ud842\udfb7"),"UTF-16 pair assembled once");
        p.key(8,true); p.key(8,false); session.drain(); check(session.state().text().isEmpty(),"Backspace removes code point");
        p.down.set(CONTROL); p.key('Z',true); p.key('Z',false); session.drain(); check(session.state().text().equals("\ud842\udfb7"),"Undo restores text");
        p.key('A',true); p.key('A',false); p.failWrite=true; p.key('X',true); p.key('X',false); session.drain();
        check(session.state().text().equals("\ud842\udfb7"),"Failed cut preserves selection");
        p.failRead=true; p.key('V',true); p.key('V',false); session.drain(); check(session.state().text().equals("\ud842\udfb7"),"Failed paste preserves selection");
        p.failRead=p.failWrite=false; p.clipboard="\u65e5\r\nabc"; p.key('V',true); p.key('V',false); session.drain();
        check(session.state().text().equals("\u65e5abc"),"Paste retains single-line behavior"); p.down.clear(CONTROL);
        p.start(); p.preedit("discard"); p.send(KILLFOCUS,0,0); session.drain();
        check(!session.isComposing() && !session.isFinished() && session.state().text().equals("\u65e5abc"),"Window focus loss cancels only preedit");
        check(p.send(SETCONTEXT,1,0x8000000fL)==123 && p.lastForwardL==15,"Hide native composition UI, preserve candidates");
        p.send(DESTROY,0,0); session.close(); check(p.detached==1,"Window destruction does not use a dead native context");
    }
    private static void asynchronousFinish() {
        var support=new WindowsImeTestSupport();
        var session=new TextInputSession(support.bridge,new TextInputSession.EditorState("base",4,4));
        var p=support.current; p.synchronous=false; p.start(); p.preedit("\u304b"); session.drain();
        session.requestFinish(); session.drain(); check(!session.isFinished(),"Focus transfer waits for native completion");
        p.result("\u304b"); p.end(); session.drain(); check(session.isFinished() && session.state().text().equals("base\u304b"),"Final result before finish acknowledgement");
        var timed=new TextInputSession(support.bridge,new TextInputSession.EditorState("draft",5,5));
        p=support.current; p.synchronous=false; p.start(); p.preedit("lost"); timed.drain(); timed.requestFinish();
        support.clock.addAndGet(2_000_000_001L); support.bridge.poll(timed.id()); timed.drain();
        check(timed.isFinished() && timed.state().text().equals("draft"),"Timeout saves committed draft, not preedit");
        var failed=new TextInputSession(support.bridge,new TextInputSession.EditorState("safe",4,4));
        p=support.current; p.start(); p.preedit("x"); failed.drain(); p.throwComplete=true;
        failed.requestFinish(); failed.drain(); check(failed.isFinished() && p.detached==1,"Completion failure releases session");
        var refused=new TextInputSession(support.bridge,new TextInputSession.EditorState("base",4,4));
        p=support.current; p.start(); p.preedit("discard"); refused.drain(); p.refuse=true;
        refused.requestFinish(); refused.drain(); check(refused.isFinished() && refused.state().text().equals("base"),"Completion refusal preserves committed draft");
    }
    private static void inheritedKeys() {
        var p=new WindowsImeTestSupport.Fake();
        p.down.set(1); p.down.set(CONTROL); p.down.set(162); p.down.set(RETURN);
        var bridge=new WindowsImeBridge(()->p,()->0L);
        var session=new TextInputSession(bridge,new TextInputSession.EditorState("",0,0));
        p.text("\r"); session.drain(); check(!session.isFinished(),"Return already down before focus cannot submit");
        session.close(); p.key(RETURN,false); p.key(162,false);
        check(p.detached==1,"Mouse and aggregate modifier states cannot strand a passive hook");
    }
    private static void geometry() {
        var support=new WindowsImeTestSupport();
        var session=new TextInputSession(support.bridge,new TextInputSession.EditorState("",0,0));
        support.bridge.caret(session.id(),0,0,2,20,0,0);
        check(support.current.geometry==null,"Zero-size/minimized client skips native positioning");
        session.close();
        var rect=new WindowsImePlatform.Caret(80,40,2,20,640,480);
        check(Arrays.equals(rect.nativeRectangle(640,480),new int[]{80,40,82,60}),"100% client coordinates");
        check(Arrays.equals(rect.nativeRectangle(800,600),new int[]{100,50,103,75}),"125% client coordinates");
        check(Arrays.equals(rect.nativeRectangle(1280,960),new int[]{160,80,164,120}),"200%, no additional DPI factor");
        try { new WindowsImePlatform.Caret(Double.NaN,0,1,1,640,480); throw new AssertionError("NaN accepted"); }
        catch (IllegalArgumentException expected) { }
    }
    private static void check(boolean value,String message) { if (!value) throw new AssertionError(message); }
}
