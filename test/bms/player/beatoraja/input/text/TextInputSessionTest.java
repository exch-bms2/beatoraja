package bms.player.beatoraja.input.text;

import static bms.player.beatoraja.input.text.ImeBridge.Command.*;

/** No GL or native IME required: state transitions, callback ordering and session isolation. */
public final class TextInputSessionTest {
    public static void main(String[] args) throws Exception {
        var bridge = new MockImeBridge();
        var session = new TextInputSession(bridge, new TextInputSession.EditorState("abc", 1, 2));
        Thread nativeThread = new Thread(() -> bridge.preedit("\u304b\u306a"));
        nativeThread.start(); nativeThread.join();
        check(!session.isComposing(), "Callback cannot mutate UI state");
        session.drain();
        check(session.isComposing() && session.state().text().equals("abc") && session.displayText().equals("a\u304b\u306ac"), "Preedit overlays selection only");
        session.updateEditor(new TextInputSession.EditorState("wrong", 0, 0));
        check(session.state().text().equals("abc"), "Displayed preedit cannot leak into committed text");
        bridge.command(CANCEL, false); session.drain();
        check(bridge.cancels == 1 && session.isComposing(), "Cancel waits for native response");
        bridge.sink.cancelled(bridge.id); session.drain();
        check(!session.isComposing() && session.displayText().equals("abc") && session.state().anchor() == 1, "Cancel restores committed selection");
        bridge.preedit("\u304b\u306a"); bridge.command(SUBMIT, false); session.drain();
        check(bridge.confirms == 1 && !session.isFinishing(), "Enter during composition confirms only");
        bridge.commit("a\u4eeec"); bridge.command(SUBMIT, true); session.drain();
        check(session.state().text().equals("a\u4eeec") && !session.isFinishing(), "Consumed Enter after commit cannot submit");
        bridge.command(SUBMIT, false); session.drain();
        check(session.isFinishing() && !session.isFinished() && bridge.finishes == 1, "Distinct Enter requests finish");
        session.requestFinish();
        bridge.commit("final\ud842\udfb7"); bridge.done(); session.drain();
        check(session.isFinished() && bridge.closes == 1 && bridge.finishes == 1 && session.state().text().equals("final\ud842\udfb7"), "Final text precedes terminal acknowledgement");
        var oldSink = bridge.sink; long oldId = session.id();
        var next = new TextInputSession(bridge, new TextInputSession.EditorState("next", 4, 4));
        check(next.id() != oldId, "IDs unique across reused transports");
        oldSink.committed(oldId, new TextInputSession.EditorState("late", 4, 4));
        bridge.sink.committed(oldId, new TextInputSession.EditorState("wrong-session", 0, 0));
        session.drain(); next.drain();
        check(next.state().text().equals("next") && session.state().text().equals("final\ud842\udfb7"), "Old sink and wrong ID ignored");
        bridge.preedit(""); next.drain();
        check(next.isComposing(), "Empty preedit can still be an active composition");
        next.close(); next.close(); bridge.commit("after disposal"); next.drain();
        oldSink.committed(oldId, null);
        oldSink.preedit(oldId, null, -1);
        check(!next.isFinished() && next.state().text().equals("next") && bridge.closes == 2, "Disposal aborts without waiting or accepting late messages");
        var raw = new TextInputSession(ImeBridge.NONE, new TextInputSession.EditorState("raw", 3, 3));
        raw.updateEditor(new TextInputSession.EditorState("typed", 5, 5)); raw.requestFinish();
        check(raw.isFinished() && raw.state().text().equals("typed"), "Legacy finish remains synchronous");
        try { new TextInputSession.EditorState("\ud842\udfb7", 1, 1); throw new AssertionError("Split surrogate accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("TextInputSessionTest passed (mock IME, commit/cancel, ordered finish, stale events, UTF-16 and legacy path)");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
