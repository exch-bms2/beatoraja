package bms.player.beatoraja;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import bms.player.beatoraja.input.PollingLwjgl3Input;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/** Real GLFW scheduling, with an injected render stall; no player files, sound or physical latency claim. */
public final class Lwjgl3FramePacingSmokeTest {
    public static void main(String[] args) {
        boolean vsync = Boolean.getBoolean("beatoraja.frameTest.vsync");
        int fps = Integer.getInteger("beatoraja.frameTest.fps", 60);
        var frames = new FramePacingDiagnostics(vsync, fps, 60000, 50_000_000);
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("beatoraja frame pacing diagnostics test");
        config.setWindowedMode(320, 200);
        config.useVsync(vsync); config.setForegroundFPS(fps); config.setIdleFPS(Math.max(1, fps));
        config.setPauseWhenMinimized(false); config.disableAudio(true);
        var running = new AtomicBoolean(true);
        Thread worker = new Thread(() -> {
            while (running.get()) { frames.workerPoll(System.nanoTime()); LockSupport.parkNanos(1_000_000); }
        }, "Synthetic input poll probe");
        worker.start();
        try {
            new Lwjgl3Application(FramePacingDiagnostics.wrap(new ApplicationAdapter() {
                int rendered;
                @Override public void render() {
                    if (++rendered == 5) {
                        try { Thread.sleep(60); } catch (InterruptedException e) { throw new AssertionError(e); }
                    }
                    if (rendered >= 24) Gdx.app.exit();
                }
            }, frames, () -> "SMOKE"), config) {
                @Override public Lwjgl3Input createInput(Lwjgl3Window window) {
                    return new PollingLwjgl3Input(window, frames);
                }
            };
        } finally {
            running.set(false); worker.interrupt();
            try { worker.join(2000); } catch (InterruptedException e) { throw new AssertionError(e); }
            frames.close();
        }
        var s = frames.snapshot();
        check(s.frames() >= 24 && s.failures() == 0 && s.longFrames() > 0, "Observe real renders and injected stall");
        check(Math.max(s.focused().render().maxMs(), s.unfocused().render().maxMs()) >= 50, "Render stall measured");
        check(s.inputUpdateGap().maxMs() >= 50 && s.workerPollGap().count() > 0,
                "Observe delayed owner-thread publication alongside independent synthetic polling");
        check(!worker.isAlive(), "Probe terminates");
        System.out.println("Lwjgl3FramePacingSmokeTest passed: requestedVsync=" + vsync + ", fps=" + fps
                + "; synthetic stall and poll probe, not GPU/physical input/audio latency measurements");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
