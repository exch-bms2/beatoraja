package bms.player.beatoraja.input.text.ime;

import bms.player.beatoraja.input.X11ImeNative;

import static bms.player.beatoraja.input.X11ImeNativeTest.*;

import bms.player.beatoraja.input.text.TextInputSession;

import java.nio.charset.StandardCharsets;

/** Production C callbacks/JNI with fake Xlib server functions, NOT an actual Linux IME. */
public final class X11ImeNativeIntegrationTest {
    public static void main(String[] args) {
        System.load(args[0]);
        check(locale(),"UTF-8 test locale");
        long pointer=window();
        var bridge=new X11ImeBridge(()->X11ImeNative.forWindow(pointer));
        var session=new TextInputSession(bridge,new TextInputSession.EditorState("base",4,4));
        draw(0,0,"\u304b\ud842\udfb7",2,true); session.drain();
        check(session.displayText().equals("base\u304b\ud842\udfb7") && session.displayCursor()==7,"Wide XIM callback handles supplementary code points");
        draw(0,1,"\u6f22",1,false); session.drain();
        check(session.displayText().equals("base\u6f22\ud842\udfb7"),"Multibyte XIM delta replaces, not appends");
        draw(1,1,null,1,true); session.drain(); check(session.displayText().equals("base\u6f22"),"NULL XIMText deletes range");
        key("\u78ba\u5b9a".getBytes(StandardCharsets.UTF_8)); session.drain();
        check(session.state().text().equals("base\u78ba\u5b9a") && !session.isFinished(),"Actual C filter/commit/key ordering consumes conversion Enter");
        draw(0,0,"discard",7,true); session.drain(); session.requestFinish(); session.drain();
        check(session.isFinished() && session.state().text().equals("base\u78ba\u5b9a"),"Xutf8ResetIC return is not treated as confirmed text");
        check(state(0)==1 && state(1)==1 && state(2)>=1,"XIC destroyed once and original GLFW context restored, including destroy callback");
        window(); session=new TextInputSession(bridge,new TextInputSession.EditorState("safe",4,4));
        draw(9,0,"bad",0,true); session.drain(); check(session.state().text().equals("safe") && !session.isComposing(),"Invalid native delta is rejected"); session.close();
        window(); session=new TextInputSession(bridge,new TextInputSession.EditorState("safe",4,4));
        lost(); session.drain(); session.close(); check(state(3)==1,"New GLFW context after IM restart is not overwritten or freed");
        window(); session=new TextInputSession(bridge,new TextInputSession.EditorState("safe",4,4));
        flow(2); session.drain(); check(!session.isFinished(),"Initially filtered non-composing Return is deferred");
        flow(3); session.drain(); check(session.isFinished(),"Forwarded unconsumed Return still submits");
        window(); session=new TextInputSession(bridge,new TextInputSession.EditorState("safe",4,4));
        draw(0,0,"discard",7,true); session.drain(); flow(0); session.drain();
        check(!session.isComposing() && !session.isFinished(),"Native focus loss cancels only preedit");
        flow(4); session.drain(); check(!session.isFinished(),"Physically held Return after focus gain is guarded until release");
        flow(3); session.drain(); check(session.isFinished(),"Fresh Return after release can submit");
        window(); session=new TextInputSession(bridge,new TextInputSession.EditorState("safe",4,4));
        flow(1); session.close(); check(state(0)==1 && state(1)==1,"Window destruction then late close cannot double-destroy XIC");
        System.out.println("X11ImeNativeTest passed (production C/JNI, real Xlib headers, FAKE server/GLFW)");
    }
    private static void check(boolean value,String message) { if (!value) throw new AssertionError(message); }
}
