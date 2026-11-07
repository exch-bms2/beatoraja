package bms.player.beatoraja.input;

import bms.player.beatoraja.input.controller.JInputControllerCompatibilityTest;
import bms.player.beatoraja.input.controller.JInputControllerManager;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlayModeConfig.ControllerConfig;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/** Hardware-free window boundary tests through the real BMS/JInput translation. */
public final class WindowInputLifecycleTest {
    public static void main(String[] args) throws Exception {
        Input old = Gdx.input;
        boolean[] keys = new boolean[256];
        Gdx.input = (Input) Proxy.newProxyInstance(WindowInputLifecycleTest.class.getClassLoader(),
                new Class<?>[] {Input.class}, (proxy, method, values) -> {
                    if (method.getName().equals("isKeyPressed")) return keys[(int) values[0]];
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    return null;
                });
        try {
            for (int mode = 0; mode < 4; mode++) controllerBoundaries(mode);
            replayBoundary();
            System.out.println("WindowInputLifecycleTest passed (button/POV, JKOC, analog v1/v2, FIFO boundaries, replay isolation)");
        } finally { Gdx.input = old; }
    }

    private static void controllerBoundaries(int mode) throws Exception {
        try (var f = new Fixture(mode, false)) {
            f.input.poll();
            f.fake.buttons[0] = true; f.fake.povX = -1;
            f.input.poll();
            check(f.input.getKeyState(1) && f.input.getKeyState(5), "Normal button and POV assignment");
            f.consume();
            f.input.windowActivityChanged(false);
            check(!f.input.getKeyState(1) && !f.input.getKeyState(5), "Loss releases live held states");
            var releases = f.consume();
            check(releases.size == 2 && !releases.pressed[0] && !releases.pressed[1], "Release queued for held LN/POV");
            f.fake.axes[0] = 0.95f; f.fake.axes[3] = -0.95f;
            for (int i = 0; i < 4; i++) f.input.poll();
            check(f.input.getJudgeEventQueue().available() == 0, "Background polls never forward game edges");
            f.input.windowActivityChanged(true);
            f.input.poll(); f.input.poll();
            check(f.input.getJudgeEventQueue().available() == 0 && !f.input.getKeyState(1), "Held controls/axis jumps do not press on resume");
            f.fake.buttons[0] = false; f.fake.povX = 0; f.fake.axes[0] = f.fake.axes[3] = 0;
            f.input.poll(); f.consume();
            f.fake.buttons[0] = true; f.input.poll();
            check(f.input.getKeyState(1), "Fresh button press after release works");
            f.consume();
            f.fake.buttons[0] = false; f.input.poll(); f.consume();
            // Queue raw edges without translating them; a loss/regain between polls must discard them.
            f.fake.buttons[0] = true; f.manager.poll();
            f.fake.buttons[0] = false; f.manager.poll();
            f.input.windowActivityChanged(false); f.consume(); f.input.windowActivityChanged(true);
            f.input.poll(); f.input.poll();
            check(!f.input.getKeyState(1) && f.input.getJudgeEventQueue().available() == 0, "No raw FIFO tap survives a window boundary");
            var log = f.input.getKeyInputLog();
            check(log.length >= 6, "Focus release and real input remain in replay log");
            for (int i = 1; i < log.length; i++) check(log[i].getTime() >= log[i - 1].getTime(), "Replay times remain monotonic");
        }
    }

    private static void replayBoundary() throws Exception {
        try (var f = new Fixture(0, false)) {
            f.input.beginInputJudge(new int[] {0}, new int[] {1}, null, true);
            var q = f.input.getJudgeEventQueue();
            q.offer(1, true, 12345);
            f.input.windowActivityChanged(false); f.input.windowActivityChanged(true);
            var batch = new JudgeEventQueue.Batch(); q.drain(batch);
            check(batch.size == 1 && batch.keys[0] == 1 && batch.times[0] == 12345 && batch.pressed[0],
                    "Window focus cannot discard, shift or inject replay events");
        }
    }

    static final class Fixture implements AutoCloseable {
        final Runnable previous = BMSPlayerInputProcessor.controllerPollRunner;
        final JInputControllerCompatibilityTest.FakeDevice fake = new JInputControllerCompatibilityTest.FakeDevice(2, 4);
        final JInputControllerManager manager;
        final BMSPlayerInputProcessor input;
        Fixture(int mode, boolean mouse) throws Exception {
            manager = JInputControllerCompatibilityTest.createManager(new net.java.games.input.Controller[] {fake.device}, () -> { }, () -> { }, Runnable::run);
            var runner = JInputControllerManager.class.getDeclaredField("pollRunner"); runner.setAccessible(true);
            BMSPlayerInputProcessor.controllerPollRunner = (Runnable) runner.get(manager);
            var player = new PlayerConfig();
            var keyboard = player.getMode14().getKeyboardConfig();
            Arrays.fill(keyboard.getKeyAssign(), -1); keyboard.getKeyAssign()[0] = Input.Keys.Z;
            keyboard.setDuration(0);
            var ms = keyboard.getMouseScratchConfig();
            ms.setMouseScratchEnabled(mouse); ms.setMouseScratchDistance(12);
            ms.setMouseScratchMode(mode % 2);
            Arrays.fill(ms.getKeyAssign(), -1); ms.getKeyAssign()[2] = 0; ms.getKeyAssign()[3] = 1;
            var controller = new ControllerConfig();
            controller.setName(fake.name); controller.setDuration(0); controller.setJKOC(mode == 1);
            controller.setAnalogScratch(mode >= 2); controller.setAnalogScratchMode(mode - 2);
            Arrays.fill(controller.getKeyAssign(), -1);
            controller.getKeyAssign()[1] = 0; controller.getKeyAssign()[5] = 28; controller.getKeyAssign()[6] = 32;
            input = new BMSPlayerInputProcessor(new Config(), player, manager.getControllers(), owner -> new MidiInputProcessor(owner, List.of()));
            input.setControllerConfig(new ControllerConfig[] {controller});
            input.setStartTime(1);
            input.beginInputJudge(null, null, false);
        }
        JudgeEventQueue.Batch consume() {
            var batch = new JudgeEventQueue.Batch(); var q = input.getJudgeEventQueue(); q.drain(batch);
            for (int i = 0; i < batch.size; i++) q.apply(batch.keys[i], batch.pressed[i]);
            return batch;
        }
        @Override public void close() {
            input.dispose(); manager.dispose();
            BMSPlayerInputProcessor.controllerPollRunner = previous;
        }
    }
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
