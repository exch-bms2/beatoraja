package bms.player.beatoraja.skin;

import bms.player.beatoraja.input.keyboard.KeyBoardInputProcesseor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.input.*;
import bms.player.beatoraja.input.text.ImeBridge;
import bms.player.beatoraja.input.text.MockImeBridge;
import bms.player.beatoraja.input.text.TextInputSession;
import bms.player.beatoraja.input.text.ime.WindowsImeTestSupport;
import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Clipboard;
import org.lwjgl.glfw.GLFW;

/** Committed-character callbacks, not an OS IME automation; never reads/writes the user's clipboard. */
public final class Lwjgl3TextInputTest {
    public static void main(String[] args) {
        var cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("beatoraja Unicode/editor test"); cfg.setWindowedMode(480, 270);
        cfg.disableAudio(true); cfg.setForegroundFPS(60);
        new Lwjgl3Application(bms.player.beatoraja.build.GlTestDiagnostics.wrap("text-input", new ApplicationAdapter() {
            int frames;
            @Override public void render() {
                if (++frames != 10) return;
                try { test((PollingLwjgl3Input)Gdx.input); }
                catch (Exception e) { throw new AssertionError(e); }
                Gdx.app.exit();
            }
        }), cfg) {
            @Override public Lwjgl3Input createInput(Lwjgl3Window window) { return new PollingLwjgl3Input(window); }
        };
        System.out.println("Lwjgl3TextInputTest passed (Japanese/supplementary callbacks, focus, deferred Enter, isolated clipboard and cleanup)");
    }

    private static void test(PollingLwjgl3Input source) throws Exception {
        long handle = ((Lwjgl3Graphics)Gdx.graphics).getWindow().getWindowHandle();
        var focus = GLFW.glfwSetWindowFocusCallback(handle, null); GLFW.glfwSetWindowFocusCallback(handle, focus); focus.invoke(handle, true);
        var main = KeyboardQueueTestSupport.allocate(MainController.class);
        var config = new Config(); config.setResolution(Resolution.SD);
        var input = KeyboardQueueTestSupport.input(new PlayModeConfig.KeyboardConfig(Mode.BEAT_7K, true));
        KeyboardQueueTestSupport.set(main, MainController.class, "config", config);
        KeyboardQueueTestSupport.set(main, MainController.class, "input", input);
        KeyboardQueueTestSupport.set(main, MainController.class, "timer", new TimerManager());
        var state = new MainState(main, MainState.MainStateType.MUSICSELECT) {
            @Override public void create() { }
            @Override public void render() { }
        };
        var editor = new SkinTextInput(ImeBridge.NONE);
        var first = new AtomicReference<String>(); var second = new AtomicReference<String>();
        var writes = new AtomicInteger();
        var a = text("", 32, (s, value) -> { first.set(value); writes.incrementAndGet(); });
        var b = text("B", 48, (s, value) -> second.set(value));
        try {
            editor.focus(state, a);
            TextField field = (TextField)state.getStage().getActors().first();
            characters(handle, "\u65e5\u672c\u8a9e"); source.update();
            check(field.getText().equals("\u65e5\u672c\u8a9e") && first.get() == null, "Committed Japanese is editable, not yet submitted");
            field.setSelection(1, 2); editor.focus(state, a);
            check(field.getSelection().equals("\u672c") && field.getText().equals("\u65e5\u672c\u8a9e"), "Reclick preserves draft/cursor/selection");
            float height = field.getPrefHeight();
            editor.focus(state, b);
            check(first.get().equals("\u65e5\u672c\u8a9e") && writes.get() == 1, "Switch commits previous target exactly once");
            check(field.getText().equals("B") && field.getPrefHeight() > height, "Font metrics refresh with field size");
            key(handle, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_PRESS);
            characters(handle, "\ud842\udfb7"); // supplementary Japanese code point, after Enter in the same queue batch
            source.update();
            check(second.get() == null && field.isVisible(), "Enter submission waits for the complete input batch");
            state.getStage().act(1 / 60f);
            check(second.get().equals("B\ud842\udfb7") && !field.isVisible(), "Late committed text is included without UTF-32 truncation");
            key(handle, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_RELEASE); source.update();

            editor.focus(state, a);
            key(handle, GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_PRESS); source.update();
            input.getKeyBoardInputProcesseor().poll(100000);
            check(!input.isControlKeyPressed(KeyBoardInputProcesseor.ControlKeys.ESCAPE) && editor.isFocused(a),
                    "Escape without committed text cannot navigate game or submit editor");
            key(handle, GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_RELEASE); source.update();

            var memory = new MemoryClipboard();
            field.setClipboard(new SkinTextClipboard(() -> memory));
            memory.value = "\u65e5\u672c\u8a9e\r\nabc"; field.selectAll(); shortcut(source, handle, GLFW.GLFW_KEY_V);
            check(field.getText().equals("\u65e5\u672c\u8a9eabc"), "Single-line Japanese paste retains existing newline policy");
            field.selectAll(); shortcut(source, handle, GLFW.GLFW_KEY_C);
            check(memory.value.equals(field.getText()), "Platform copy shortcut copies exact text");
            memory.failWrite = true; shortcut(source, handle, GLFW.GLFW_KEY_X);
            check(field.getText().equals("\u65e5\u672c\u8a9eabc"), "Failed cut cannot delete selection");
            memory.failRead = true; shortcut(source, handle, GLFW.GLFW_KEY_V);
            check(field.getText().equals("\u65e5\u672c\u8a9eabc"), "Failed paste cannot delete selection or crash");
            memory.failRead = memory.failWrite = false; shortcut(source, handle, GLFW.GLFW_KEY_X);
            check(field.getText().isEmpty(), "Successful cut retains behavior");
            characters(handle, "\ud842\udfb7"); source.update();
            key(handle, GLFW.GLFW_KEY_A, GLFW.GLFW_REPEAT); source.update();
            check(field.getText().equals("\ud842\udfb7\ud842\udfb7"), "Supplementary repeat retains both UTF-16 units");
            key(handle, GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_PRESS); source.update();
            check(field.getText().equals("\ud842\udfb7"), "Backspace removes one code point");
            key(handle, GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_RELEASE); source.update();
            key(handle, GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_PRESS); source.update();
            check(field.getText().isEmpty(), "Backspace removes supplementary character as a pair");
            key(handle, GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_RELEASE); source.update();
            field.setText("focus-out"); state.getStage().setKeyboardFocus(null);
            check(first.get().equals("focus-out") && !field.isVisible(), "Scene2D focus-out commits");

            var failed = text("unchanged", 32, (s, value) -> { throw new IllegalStateException("Expected writer failure"); });
            editor.focus(state, failed);
            try { editor.commit(); throw new AssertionError("Expected failure"); }
            catch (IllegalStateException expected) { }
            check(!editor.isFocused(failed) && !field.isVisible() && state.getStage().getKeyboardFocus() == null,
                    "Writer failure cannot leave a stuck editor");
            key(handle, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_PRESS); source.update(); input.getKeyBoardInputProcesseor().poll(200000);
            check(input.getControlKeyState(KeyBoardInputProcesseor.ControlKeys.DOWN), "Gameplay controls restored after failure");
            key(handle, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_RELEASE); source.update();
            mockIme(state, source, handle);
            windowsIme(state);
            x11Ime(state, source, handle, input);
            if (Boolean.getBoolean("beatoraja.test.macosIme")) macIme(state);
        } finally { editor.dispose(); state.dispose(); }
    }

    private static void x11Ime(MainState state, PollingLwjgl3Input source, long handle, BMSPlayerInputProcessor input) {
        var support = new X11ImeTestSupport();
        var editor = new SkinTextInput(support.bridge);
        var first = new AtomicReference<String>(); var second = new AtomicReference<String>();
        var a = text("base",32,(s,value)->first.set(value));
        var b = text("next",32,(s,value)->second.set(value));
        try {
            editor.focus(state,a);
            var old = support.current;
            TextField field = (TextField)state.getStage().getActors().peek();
            check(old.geometry != null && old.geometry[3] > 0,"X11 initial logical caret published before first draw");
            old.preedit("\u304b\ud842\udfb7"); state.getStage().act(0); state.getStage().draw();
            check(field.getText().equals("base\u304b\ud842\udfb7") && first.get()==null,"X11 preedit displayed without property write");
            check(old.geometry[4]==Gdx.graphics.getWidth() && old.geometry[5]==Gdx.graphics.getHeight(),"X11 caret uses logical client units");
            key(handle,GLFW.GLFW_KEY_ESCAPE,GLFW.GLFW_PRESS); source.update(); input.getKeyBoardInputProcesseor().poll(250000);
            check(!input.isControlKeyPressed(KeyBoardInputProcesseor.ControlKeys.ESCAPE),"X11 editing reserves game Escape");
            key(handle,GLFW.GLFW_KEY_ESCAPE,GLFW.GLFW_RELEASE); source.update();
            characters(handle,"duplicate"); source.update();
            check(field.getText().equals("base\u304b\ud842\udfb7"),"Native mode ignores duplicate GLFW character stream");
            old.end(); old.commit("\u6f22\u5b57"); old.key(GLFW.GLFW_KEY_ENTER,0,true); state.getStage().act(0);
            check(editor.isFocused(a) && first.get()==null && field.getText().equals("base\u6f22\u5b57"),"X11 conversion Return does not submit");
            old.release(GLFW.GLFW_KEY_ENTER);
            old.preedit("cancel"); state.getStage().act(0); old.end(); state.getStage().act(0);
            check(field.getText().equals("base\u6f22\u5b57"),"X11 cancellation restores committed draft");
            old.preedit("discard"); state.getStage().act(0); editor.focus(state,b);
            check(editor.isFocused(b) && first.get().equals("base\u6f22\u5b57") && old.detached==1,"X11 focus transfer excludes unresolved preedit and releases context");
            old.commit("stale"); old.preedit("late"); state.getStage().act(0);
            check(field.getText().equals("next") && second.get()==null,"Old XIM listener cannot enter new field");
            support.current.preedit("lost"); support.current.focusLost(); state.getStage().act(0);
            check(editor.isFocused(b) && field.getText().equals("next"),"Window focus loss cancels preedit without submitting");
            support.current.commit("!"); state.getStage().act(0); state.getStage().setKeyboardFocus(null);
            check(second.get().equals("next!") && !field.isVisible(),"X11 field focus-out writes committed document");
            editor.focus(state,b); var next=support.current; next.preedit("discard"); state.getStage().act(0); editor.dispose();
            check(next.detached==1 && second.get().equals("next!"),"X11 disposal releases context without saving preedit");
        } finally { editor.dispose(); }
        support = new X11ImeTestSupport(); support.failNextAttach = true;
        editor = new SkinTextInput(support.bridge);
        try {
            editor.focus(state,a);
            TextField field=(TextField)state.getStage().getActors().peek(); field.setText("fallback"); editor.commit();
            check(first.get().equals("fallback") && support.current.detached==1,"Unsupported XIM style restores legacy editor");
        } finally { editor.dispose(); }
    }

    private static void windowsIme(MainState state) {
        var support=new WindowsImeTestSupport();
        var editor=new SkinTextInput(support.bridge);
        var first=new AtomicReference<String>(); var second=new AtomicReference<String>();
        var a=text("base",32,(s,value)->first.set(value));
        var b=text("next",32,(s,value)->second.set(value));
        try {
            editor.focus(state,a);
            var old=support.current;
            TextField field=(TextField)state.getStage().getActors().peek();
            old.start(); old.preedit("\u304b\u306a"); state.getStage().act(0); state.getStage().draw();
            check(field.getText().equals("base\u304b\u306a") && first.get()==null,"Windows preedit displayed without property write");
            double[] caret=old.geometryValues();
            check(caret[3]>0 && caret[4]==Gdx.graphics.getWidth() && caret[5]==Gdx.graphics.getHeight(),"Windows caret uses logical client units");
            old.key(13,true); old.result("\u6f22\u5b57"); old.end(); old.text("\r"); state.getStage().act(0);
            check(editor.isFocused(a) && first.get()==null && field.getText().equals("base\u6f22\u5b57"),"Windows conversion Return does not submit");
            old.key(13,false);
            old.start(); old.preedit("cancel"); state.getStage().act(0);
            old.end(); state.getStage().act(0);
            check(field.getText().equals("base\u6f22\u5b57"),"Windows cancel restores committed draft");
            old.start(); old.preedit("final"); state.getStage().act(0); old.synchronous=false;
            editor.focus(state,b);
            check(editor.isFinishing() && first.get()==null,"Windows focus transfer waits for native completion");
            old.result("final"); old.end(); state.getStage().act(0);
            check(editor.isFocused(b) && first.get().equals("base\u6f22\u5b57final"),"Windows focus transfer saves final old document");
            old.text("late"); old.result("stale"); state.getStage().act(0);
            check(field.getText().equals("next") && second.get()==null,"Old HWND callback cannot mutate new Windows session");
            var next=support.current; next.start(); next.preedit("focus-out"); state.getStage().act(0);
            state.getStage().setKeyboardFocus(null);
            check(second.get().equals("nextfocus-out") && !field.isVisible(),"Windows focus-out saves resolved composition");
            editor.focus(state,b); next=support.current; next.start(); next.preedit("discard"); state.getStage().act(0);
            editor.dispose();
            check(second.get().equals("nextfocus-out") && next.detached==1,"Windows disposal does not save preedit or wait");
        } finally { editor.dispose(); }
        support=new WindowsImeTestSupport(); support.failNextAttach=true;
        editor=new SkinTextInput(support.bridge);
        try {
            editor.focus(state,a);
            TextField field=(TextField)state.getStage().getActors().peek();
            field.setText("fallback"); editor.commit();
            check(first.get().equals("fallback") && support.current.detached==1,"Failed Windows hook restores legacy editor");
        } finally { editor.dispose(); }
    }

    private static void macIme(MainState state) throws Exception {
        String previous = System.getProperty("beatoraja.macosIme");
        System.setProperty("beatoraja.macosIme", "true");
        ImeBridge created = MacOsImeBridge.create();
        check(created instanceof MacOsImeBridge, "Opt-in JNI bridge must load for this native test");
        var bridge = (MacOsImeBridge)created;
        var editor = new SkinTextInput(bridge);
        var first = new AtomicReference<String>(); var second = new AtomicReference<String>();
        var a = text("base", 32, (s, value) -> first.set(value));
        var b = text("next", 32, (s, value) -> second.set(value));
        long old = 0, next = 0, empty = 0;
        try {
            editor.focus(state, a);
            old = MacOsImeNativeTest.retain(bridge);
            TextField field = (TextField)state.getStage().getActors().peek();
            MacOsImeNativeTest.inject(old, 1, "\ud842\udfb7"); state.getStage().act(0);
            check(field.getText().equals("base\ud842\udfb7"), "AppKit supplementary character commit");
            MacOsImeNativeTest.inject(old, 8, ""); state.getStage().act(0);
            check(field.getText().equals("base"), "AppKit backspace removes a complete supplementary character");
            MacOsImeNativeTest.inject(old, 9, "");
            MacOsImeNativeTest.inject(old, 11, "\u65e5\r\nabc"); state.getStage().act(0);
            check(field.getText().equals("\u65e5abc"), "Private native pasteboard preserves single-line paste policy");
            MacOsImeNativeTest.inject(old, 9, ""); MacOsImeNativeTest.inject(old, 12, ""); state.getStage().act(0);
            check(field.getText().isEmpty(), "Native cut uses the private pasteboard");
            MacOsImeNativeTest.inject(old, 1, "base"); state.getStage().act(0);
            MacOsImeNativeTest.inject(old, 9, ""); MacOsImeNativeTest.inject(old, 13, ""); state.getStage().act(0);
            check(field.getText().equals("base"), "Native failed cut preserves selected text");
            MacOsImeNativeTest.inject(old, 14, ""); state.getStage().act(0);
            check(field.getText().equals("base"), "Native failed paste preserves selected text");
            MacOsImeNativeTest.inject(old, 1, "base"); state.getStage().act(0);
            MacOsImeNativeTest.inject(old, 0, "\u304b\u306a"); state.getStage().act(0);
            check(field.getText().equals("base\u304b\u306a") && first.get() == null, "AppKit preedit reaches SkinTextInput, not property");
            state.getStage().draw();
            double[] actual = MacOsImeNativeTest.geometry(old);
            check(actual[3] > 0 && actual[11] == 1, "Caret geometry and native first responder connected");
            MacOsImeNativeTest.position(old, 80, 40, 2, 20, 480, 270);
            double[] one = MacOsImeNativeTest.geometry(old);
            check(Math.abs(one[0] - one[4] - 80 * one[6] / 480) < .01
                    && Math.abs(one[1] - one[5] - (270 - 40 - 20) * one[7] / 270) < .01, "Client top-left to Cocoa screen points");
            MacOsImeNativeTest.position(old, 80, 40, 2, 20, 960, 540);
            double[] two = MacOsImeNativeTest.geometry(old);
            check(Math.abs((one[0] - one[4]) - 2 * (two[0] - two[4])) < .01
                    && Math.abs(one[3] - 2 * two[3]) < .01, "Logical scaling applied once, not framebuffer DPI twice");
            MacOsImeNativeTest.inject(old, 2, ""); state.getStage().act(0);
            check(field.getText().equals("base") && editor.isFocused(a), "AppKit cancel restores draft without closing field");
            MacOsImeNativeTest.inject(old, 0, "\u6f22\u5b57");
            MacOsImeNativeTest.inject(old, 3, ""); state.getStage().act(0);
            check(field.getText().equals("base\u6f22\u5b57") && first.get() == null, "Commit then consumed Return does not submit");
            MacOsImeNativeTest.inject(old, 0, "\u672b"); state.getStage().act(0);
            editor.focus(state, b);
            check(first.get().equals("base\u6f22\u5b57\u672b") && editor.isFocused(b), "Focus transfer resolves AppKit composition before property write");
            double[] closed = MacOsImeNativeTest.geometry(old);
            check(closed[9] == 0 && closed[10] == 0, "Old global reference and event monitor released");
            MacOsImeNativeTest.inject(old, 1, "late"); state.getStage().act(0);
            check(field.getText().equals("next") && second.get() == null, "Detached native session cannot enter new field");
            next = MacOsImeNativeTest.retain(bridge);
            check(MacOsImeNativeTest.inject(next, 5, ""), "Native window's Return key is consumed");
            state.getStage().act(0);
            check(second.get().equals("next") && !field.isVisible(), "Separate Return submits editor");
            check(MacOsImeNativeTest.geometry(next)[9] == 0 && MacOsImeNativeTest.geometry(next)[10] == 1,
                    "Global reference freed while a held-key guard remains");
            check(MacOsImeNativeTest.inject(next, 6, ""), "Held Return repeat cannot navigate game after close");
            check(MacOsImeNativeTest.inject(next, 7, "") && MacOsImeNativeTest.geometry(next)[10] == 0,
                    "Release removes held-key guard");
            editor.focus(state, text("", 32, (s, value) -> {}));
            empty = MacOsImeNativeTest.retain(bridge);
            check(MacOsImeNativeTest.geometry(empty)[3] > 0, "Empty editor publishes caret before first draw");
            editor.commit();
            check(MacOsImeNativeTest.geometry(empty)[9] == 0 && MacOsImeNativeTest.geometry(empty)[10] == 0,
                    "Empty session closes without native residue");
            macImeSearchDiagnostics(state);
            System.out.println("macOS AppKit IME JNI test passed: synthetic marked text/commit/cancel, focus, guard, screen coordinates; backingScale=" + actual[8]);
        } finally {
            editor.dispose();
            if (old != 0) MacOsImeNativeTest.release(old);
            if (next != 0) MacOsImeNativeTest.release(next);
            if (empty != 0) MacOsImeNativeTest.release(empty);
            if (previous == null) System.clearProperty("beatoraja.macosIme"); else System.setProperty("beatoraja.macosIme", previous);
        }
    }

    private static void macImeSearchDiagnostics(MainState owner) throws Exception {
        String previous = System.getProperty("beatoraja.textInputDiagnostics");
        System.setProperty("beatoraja.textInputDiagnostics", "true");
        var state = KeyboardQueueTestSupport.allocate(bms.player.beatoraja.select.MusicSelector.class);
        KeyboardQueueTestSupport.set(state, MainState.class, "main", owner.main);
        KeyboardQueueTestSupport.set(state, MainState.class, "timer", owner.timer);
        KeyboardQueueTestSupport.set(state, MainState.class, "resource", owner.resource);
        var query = new AtomicReference<String>();
        var db = (bms.player.beatoraja.song.SongDatabaseAccessor)java.lang.reflect.Proxy.newProxyInstance(
                Lwjgl3TextInputTest.class.getClassLoader(), new Class<?>[]{bms.player.beatoraja.song.SongDatabaseAccessor.class},
                (proxy, method, args) -> {
                    if (!method.getName().equals("getSongDatasByText")) throw new AssertionError(method);
                    query.set((String)args[0]); return bms.player.beatoraja.song.SongData.EMPTY;
                });
        KeyboardQueueTestSupport.set(state, state.getClass(), "songdb", db);
        var bridge = (MacOsImeBridge)MacOsImeBridge.create();
        var editor = new SkinTextInput(bridge);
        var search = text("", 32, bms.player.beatoraja.skin.property.StringPropertyFactory.getStringWriter(30));
        long pointer = 0;
        try {
            editor.focus(state, search);
            pointer = MacOsImeNativeTest.retain(bridge);
            TextField field = (TextField)state.getStage().getActors().peek();
            MacOsImeNativeTest.inject(pointer, 1, "as"); state.getStage().act(0);
            check(field.getText().equals("as"), "Native text reaches initially empty search field");
            MacOsImeNativeTest.inject(pointer, 5, ""); state.getStage().act(0);
            check("as".equals(query.get()) && !field.isVisible(), "Real search writer receives native committed ASCII exactly");
            MacOsImeNativeTest.inject(pointer, 7, ""); MacOsImeNativeTest.release(pointer); pointer = 0;

            query.set(null); search.setText(""); editor.focus(state, search);
            pointer = MacOsImeNativeTest.retain(bridge);
            MacOsImeNativeTest.inject(pointer, 0, "\u3042\u3059"); state.getStage().act(0);
            check(query.get() == null && field.getText().equals("\u3042\u3059"), "Search preedit is visible but never queried");
            MacOsImeNativeTest.inject(pointer, 3, ""); state.getStage().act(0);
            check(query.get() == null && field.isVisible(), "Composition Return does not run search");
            MacOsImeNativeTest.inject(pointer, 5, ""); state.getStage().act(0);
            check("\u3042\u3059".equals(query.get()) && !field.isVisible(), "Distinct Return sends confirmed Japanese to search");
            MacOsImeNativeTest.inject(pointer, 7, ""); MacOsImeNativeTest.release(pointer); pointer = 0;

            query.set(null); search.setText(""); editor.focus(state, search);
            pointer = MacOsImeNativeTest.retain(bridge);
            MacOsImeNativeTest.inject(pointer, 1, "as");
            MacOsImeNativeTest.inject(pointer, 5, ""); state.getStage().act(0);
            check("as".equals(query.get()), "Native commit and Return in one batch keep the final query even before display refresh");
            MacOsImeNativeTest.inject(pointer, 7, "");
        } finally {
            try { editor.dispose(); }
            finally {
                if (state.getStage() != null) { state.getStage().dispose(); state.setStage(null); }
                if (pointer != 0) MacOsImeNativeTest.release(pointer);
                if (previous == null) System.clearProperty("beatoraja.textInputDiagnostics");
                else System.setProperty("beatoraja.textInputDiagnostics", previous);
            }
        }
    }

    private static void mockIme(MainState state, PollingLwjgl3Input source, long handle) {
        var bridge = new MockImeBridge();
        var editor = new SkinTextInput(bridge);
        var first = new AtomicReference<String>(); var second = new AtomicReference<String>();
        var a = text("base", 32, (s, value) -> first.set(value));
        var b = text("next", 32, (s, value) -> second.set(value));
        try {
            editor.focus(state, a);
            TextField field = (TextField)state.getStage().getActors().peek();
            bridge.preedit("\u304b\u306a"); state.getStage().act(0);
            check(field.getText().equals("base\u304b\u306a") && first.get() == null, "Preedit displays without writing property");
            key(handle, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_PRESS); characters(handle, "duplicate"); source.update();
            bridge.commit("base\u4eee"); bridge.command(ImeBridge.Command.SUBMIT, true); state.getStage().act(0);
            check(field.getText().equals("base\u4eee") && first.get() == null && editor.isFocused(a), "IME Enter confirms only; raw character path cannot duplicate");
            key(handle, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_RELEASE); source.update();
            key(handle, GLFW.GLFW_KEY_F6, GLFW.GLFW_PRESS); source.update();
            state.main.getInputProcessor().getKeyBoardInputProcesseor().poll(300000);
            check(!state.main.getInputProcessor().isControlKeyPressed(KeyBoardInputProcesseor.ControlKeys.F6), "IME function key cannot trigger game control");
            bridge.preedit("cancel-me"); state.getStage().act(0);
            bridge.sink.cancelled(bridge.id); state.getStage().act(0);
            check(field.getText().equals("base\u4eee") && editor.isFocused(a), "Composition cancel keeps editor and committed text");
            var oldSink = bridge.sink; long oldId = bridge.id;
            editor.focus(state, b);
            check(editor.isFinishing() && editor.isFocused(a) && first.get() == null && bridge.begins == 1, "Focus transfer waits for final native text");
            bridge.commit("final old"); bridge.done(); state.getStage().act(0);
            check(first.get().equals("final old") && editor.isFocused(b) && field.getText().equals("next"), "Finish writes old target before opening next");
            oldSink.committed(oldId, new TextInputSession.EditorState("late", 4, 4));
            bridge.sink.committed(oldId, new TextInputSession.EditorState("wrong", 5, 5));
            state.getStage().act(0);
            check(field.getText().equals("next") && second.get() == null, "Late old events cannot enter the next field");
            state.getStage().setKeyboardFocus(null);
            check(editor.isFinishing() && second.get() == null, "Scene2D focus loss waits for IME acknowledgement");
            bridge.commit("focus-out final"); bridge.done(); state.getStage().act(0);
            check(second.get().equals("focus-out final") && !field.isVisible(), "Focus-out final commit writes exactly the resolved document");
            editor.focus(state, b);
            bridge.preedit("discard on dispose"); state.getStage().act(0);
            editor.dispose();
            check(second.get().equals("focus-out final") && !field.isVisible(), "Disposal saves committed draft only without waiting");
            bridge.commit("after dispose");
            state.main.getInputProcessor().getKeyBoardInputProcesseor().poll(400000);
            check(!state.main.getInputProcessor().isControlKeyPressed(KeyBoardInputProcesseor.ControlKeys.F6), "Held IME key suppressed after exit");
            key(handle, GLFW.GLFW_KEY_F6, GLFW.GLFW_RELEASE); source.update();
            state.main.getInputProcessor().getKeyBoardInputProcesseor().poll(500000);
            key(handle, GLFW.GLFW_KEY_F6, GLFW.GLFW_PRESS); source.update();
            state.main.getInputProcessor().getKeyBoardInputProcesseor().poll(600000);
            check(state.main.getInputProcessor().isControlKeyPressed(KeyBoardInputProcesseor.ControlKeys.F6), "IME key restored after release/new press");
            key(handle, GLFW.GLFW_KEY_F6, GLFW.GLFW_RELEASE); source.update();
        } finally { editor.dispose(); }
        var failedBridge = new MockImeBridge(); failedBridge.failFinish = true;
        var failedEditor = new SkinTextInput(failedBridge);
        try {
            failedEditor.focus(state, a);
            TextField field = (TextField)state.getStage().getActors().peek();
            try { failedEditor.commit(); throw new AssertionError("Expected bridge finish failure"); }
            catch (IllegalStateException expected) { }
            check(!failedEditor.isFocused(a) && !field.isVisible() && failedBridge.closes == 1,
                    "Bridge failure cannot leave focus/input capture stuck");
        } finally { failedEditor.dispose(); }
    }

    private static SkinText text(String initial, int height, bms.player.beatoraja.skin.property.StringWriter writer) {
        var result = new SkinText((bms.player.beatoraja.skin.property.StringProperty)null) {
            @Override public void prepareFont(String value) { }
            @Override protected void prepareText(String value) { }
            @Override public void draw(Skin.SkinObjectRenderer renderer, float x, float y) { }
            @Override public void dispose() { }
        };
        result.region.set(80, 60, 200, height); result.color.set(com.badlogic.gdx.graphics.Color.WHITE);
        result.setText(initial); result.setEditable(true); result.setWriter(writer); return result;
    }
    private static void characters(long handle, String value) {
        var callback = GLFW.glfwSetCharCallback(handle, null); GLFW.glfwSetCharCallback(handle, callback);
        value.codePoints().forEach(codepoint -> callback.invoke(handle, codepoint));
    }
    private static void key(long handle, int code, int action) {
        var callback = GLFW.glfwSetKeyCallback(handle, null); GLFW.glfwSetKeyCallback(handle, callback);
        callback.invoke(handle, code, 0, action, 0);
    }
    private static void shortcut(PollingLwjgl3Input source, long handle, int code) {
        int modifier = System.getProperty("os.name").startsWith("Mac") ? GLFW.GLFW_KEY_LEFT_SUPER : GLFW.GLFW_KEY_LEFT_CONTROL;
        key(handle, modifier, GLFW.GLFW_PRESS); source.update(); key(handle, code, GLFW.GLFW_PRESS); source.update();
        key(handle, code, GLFW.GLFW_RELEASE); key(handle, modifier, GLFW.GLFW_RELEASE); source.update();
    }
    private static final class MemoryClipboard implements Clipboard {
        String value = ""; boolean failRead, failWrite;
        @Override public boolean hasContents() { return !value.isEmpty(); }
        @Override public String getContents() { if (failRead) throw new IllegalStateException("Unavailable"); return value; }
        @Override public void setContents(String text) { if (failWrite) throw new IllegalStateException("Unavailable"); value = text; }
    }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
