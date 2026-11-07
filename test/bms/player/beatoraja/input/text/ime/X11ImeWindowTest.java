package bms.player.beatoraja.input.text.ime;

import bms.player.beatoraja.input.text.ImeBridge;
import bms.player.beatoraja.input.text.TextInputSession;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/** Real X11 window/XIC lifetime smoke. Does not synthesize real Japanese IME conversion. */
public final class X11ImeWindowTest {
    public static void main(String[] args) {
        if (!System.getProperty("os.name","").startsWith("Linux") || !Boolean.getBoolean("beatoraja.x11Ime")) {
            System.out.println("SKIP X11ImeWindowTest: requires Linux/X11 and explicit patched GLFW opt-in"); return;
        }
        var cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("beatoraja X11 IME lifecycle smoke"); cfg.setWindowedMode(640,480);
        cfg.disableAudio(true); cfg.setForegroundFPS(60);
        new Lwjgl3Application(new ApplicationAdapter() {
            int frames;
            @Override public void render() {
                if (++frames != 10) return;
                ImeBridge bridge = X11ImeBridge.create();
                check(bridge instanceof X11ImeBridge,"Patched GLFW/XIM bridge must be available");
                for (int i=0; i<30; i++) {
                    var session = new TextInputSession(bridge,new TextInputSession.EditorState("draft",5,5));
                    try {
                        bridge.caret(session.id(),80,40,2,20,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
                        session.requestFinish(); session.drain();
                        check(session.isFinished() && session.state().text().equals("draft"),"XIC focus/finish/restore preserves committed draft");
                    } finally { session.close(); }
                }
                Gdx.app.exit();
            }
        },cfg);
        System.out.println("X11ImeWindowTest passed (real X11 window and 30 XIC lifetimes; NO Japanese conversion automation)");
    }
    private static void check(boolean value,String message) { if (!value) throw new AssertionError(message); }
}
