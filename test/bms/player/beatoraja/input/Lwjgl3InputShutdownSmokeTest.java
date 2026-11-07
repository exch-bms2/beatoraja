package bms.player.beatoraja.input;

import bms.player.beatoraja.input.controller.JInputControllerCompatibilityTest;
import bms.player.beatoraja.input.controller.JInputControllerManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayerConfig;
import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;

/** Real backend lifecycle order with fake devices; no MIDI/controller enumeration. */
public final class Lwjgl3InputShutdownSmokeTest {
    public static void main(String[] args) {
        Runnable oldRunner = BMSPlayerInputProcessor.controllerPollRunner;
        var input = new AtomicReference<BMSPlayerInputProcessor>();
        var worker = new AtomicReference<Thread>();
        var polls = new AtomicInteger();
        var closed = new AtomicInteger();
        var disposed = new AtomicInteger();
        var manager = JInputControllerCompatibilityTest.createManager(new net.java.games.input.Controller[0], () -> {
            worker.set(Thread.currentThread()); polls.incrementAndGet();
        }, () -> {
            check(worker.get() != null && !worker.get().isAlive(), "Controller release must follow join");
            closed.incrementAndGet();
        }, Runnable::run);
        try {
            var runner = JInputControllerManager.class.getDeclaredField("pollRunner");
            runner.setAccessible(true);
            BMSPlayerInputProcessor.controllerPollRunner = (Runnable) runner.get(manager);
            var config = new Lwjgl3ApplicationConfiguration();
            config.setTitle("beatoraja input shutdown test");
            config.setWindowedMode(320, 240);
            config.disableAudio(true);
            config.setForegroundFPS(60);
            new Lwjgl3Application(new ApplicationAdapter() {
                int frames;
                @Override public void create() {
                    input.set(new BMSPlayerInputProcessor(new Config(), new PlayerConfig(), manager.getControllers(),
                            owner -> new MidiInputProcessor(owner, List.of())));
                    Gdx.app.addLifecycleListener(new LifecycleListener() {
                        @Override public void pause() { }
                        @Override public void resume() { }
                        @Override public void dispose() { manager.dispose(); }
                    });
                    input.get().startPolling();
                }
                @Override public void render() {
                    if (++frames > 120) throw new AssertionError("Input polling did not start");
                    if (frames > 5 && polls.get() > 0) Gdx.app.exit();
                }
                @Override public void dispose() {
                    check(closed.get() == 1, "Backend lifecycle listener must dispose before application");
                    input.get().dispose(); input.get().dispose();
                    check(!worker.get().isAlive(), "No input worker left at application disposal");
                    disposed.incrementAndGet();
                }
            }, config) {
                @Override public Lwjgl3Input createInput(Lwjgl3Window window) { return new PollingLwjgl3Input(window); }
            };
            check(disposed.get() == 1 && closed.get() == 1, "One completed backend/application shutdown");
            check(BMSPlayerInputProcessor.controllerPollRunner == null, "No stale controller poll runner");
            System.out.println("Lwjgl3InputShutdownSmokeTest passed (real GLFW lifecycle, joined worker, idempotent cleanup)");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        } finally {
            if (input.get() != null) input.get().dispose();
            manager.dispose();
            BMSPlayerInputProcessor.controllerPollRunner = oldRunner;
        }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
