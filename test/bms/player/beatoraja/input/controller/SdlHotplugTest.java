package bms.player.beatoraja.input.controller;

import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.JudgeEventQueue;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;

import bms.player.beatoraja.*;
import bms.player.beatoraja.PlayModeConfig.ControllerConfig;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/** Late attachment and instance-ID changes through production BM, judgment and replay paths. */
public final class SdlHotplugTest {
    public static void main(String[] args) throws Exception {
        focusRegainDuringSdlUpdate();
        for (var backend : new ControllerBackend[]{ControllerBackend.SDL_RAW, ControllerBackend.SDL_RAW_ASYNC, ControllerBackend.SDL_ASYNC}) {
            reconnect(backend);
        }
        System.out.println("SdlHotplugTest passed (focus reentry, inventory snapshots, reconnect, saved bindings, FIFO/replay)");
    }

    private static void focusRegainDuringSdlUpdate() throws Exception {
        var sdlUpdating = new CountDownLatch(1);
        var inputLocked = new CountDownLatch(1);
        var owner = new FutureTask<Void>(() -> {
            var oldInput = Gdx.input;
            var oldRunner = BMSPlayerInputProcessor.controllerPollRunner;
            var source = new Source();
            int slot = source.add(101, "serial-A");
            var manager = new SdlRawControllerManager(source, Runnable::run);
            try {
                Gdx.input = fakeInput();
                BMSPlayerInputProcessor.controllerPollRunner = SdlRawAsyncControllerTest.runner(manager);
                var player = new PlayerConfig();
                player.selectControllerBackend(ControllerBackend.SDL_RAW);
                var play = player.getMode14();
                Arrays.fill(play.getKeyboardConfig().getKeyAssign(), -1);
                configure(play.getController()[0], "Hotplug device", 0);
                play.getController()[0].setDuration(0); // Isolate focus handling from the debounce interval.
                var config = new Config(); config.setControllerBackend(ControllerBackend.SDL_RAW);
                var input = KeyboardQueueTestSupport.input(config, player, manager.getControllers());
                try {
                    input.setPlayConfig(play); input.setStartTime(1); input.beginInputJudge(null, null, false);
                    source.press(slot, 3, true); manager.poll(); input.poll();
                    check(input.getKeyState(0), "Held controller input before focus loss");
                    var batch = new JudgeEventQueue.Batch(); input.getJudgeEventQueue().drain(batch);
                    KeyboardQueueTestSupport.windowActivityChanged(input, false);
                    input.getJudgeEventQueue().drain(batch);
                    check(!input.getKeyState(0) && batch.size == 1 && !batch.pressed[0], "Focus loss releases the held key");

                    var worker = new FutureTask<Void>(() -> {
                        await(sdlUpdating);
                        synchronized (input.getKeyBoardInputProcesseor()) {
                            inputLocked.countDown();
                            input.poll();
                        }
                        return null;
                    });
                    startDaemon(worker, "test-input-polling");
                    source.onUpdate = () -> {
                        sdlUpdating.countDown();
                        await(inputLocked);
                        // Model SDL calling back into GLFW before releasing the manager monitor.
                        KeyboardQueueTestSupport.windowActivityChanged(input, true);
                    };
                    manager.poll();
                    source.onUpdate = () -> { };
                    worker.get(5, TimeUnit.SECONDS);
                    input.poll(); input.getJudgeEventQueue().drain(batch);
                    check(!input.getKeyState(0) && batch.size == 0, "Still-held input is suppressed on regain");
                    source.press(slot, 3, false); manager.poll(); input.poll();
                    source.press(slot, 3, true); manager.poll(); input.poll();
                    check(input.getKeyState(0), "Fresh controller press works after regain");
                } finally { input.dispose(); }
            } finally {
                manager.dispose();
                Gdx.input = oldInput; BMSPlayerInputProcessor.controllerPollRunner = oldRunner;
            }
            return null;
        });
        startDaemon(owner, "test-sdl-owner");
        owner.get(10, TimeUnit.SECONDS);
    }

    private static void startDaemon(Runnable task, String name) {
        var thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }

    private static void await(CountDownLatch latch) {
        try { check(latch.await(5, TimeUnit.SECONDS), "Timed out arranging SDL focus reentry"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }

    private static Input fakeInput() {
        return (Input) Proxy.newProxyInstance(SdlHotplugTest.class.getClassLoader(), new Class<?>[]{Input.class},
                (p, m, a) -> m.getReturnType() == boolean.class ? false : m.getReturnType() == int.class ? 0 : null);
    }

    private static void reconnect(ControllerBackend backend) throws Exception {
        var oldInput = Gdx.input;
        var oldRunner = BMSPlayerInputProcessor.controllerPollRunner;
        var source = new Source();
        var posted = new java.util.concurrent.ConcurrentLinkedQueue<Runnable>();
        var manager = new SdlRawControllerManager(source, posted::add, backend);
        BMSPlayerInputProcessor input = null;
        try {
            Gdx.input = fakeInput();
            BMSPlayerInputProcessor.controllerPollRunner = SdlRawAsyncControllerTest.runner(manager);
            var player = new PlayerConfig();
            player.selectControllerBackend(backend);
            var play = player.getMode14();
            Arrays.fill(play.getKeyboardConfig().getKeyAssign(), -1);
            configure(play.getController()[0], "Hotplug device", 0);
            configure(play.getController()[1], "Hotplug device-2", 1);
            var config = new Config(); config.setControllerBackend(backend);
            input = KeyboardQueueTestSupport.input(config, player, manager.getControllers());
            input.setPlayConfig(play); input.setStartTime(1); input.beginInputJudge(null, null, false);
            // Pending unrelated judgment events must survive registration of an additional device.
            KeyboardQueueTestSupport.includeJudgeKeys(input.getJudgeEventQueue(), new int[]{8}, new boolean[256]);
            input.setKeyState(8, true, 10);
            var empty = manager.controllerInventory();
            int slot = source.add(101, "serial-A");
            poll(manager, input, backend);
            var first = manager.controllerInventory();
            check(empty.controllers().isEmpty() && first.version() == empty.version() + 1 && first.controllers().size() == 1,
                    "Each publication pairs its version with an unchanged device list");
            manager.getControllers().clear();
            check(manager.controllerInventory() == first && first.controllers().size() == 1,
                    "The mutable ControllerManager view does not alter the published inventory");
            check(input.getBMInputProcessor().length == 1, "Device connected after BMS startup registered");
            var processor = input.getBMInputProcessor()[0];
            var controller = manager.getControllers().first();
            var batch = new JudgeEventQueue.Batch(); input.getJudgeEventQueue().drain(batch);
            check(batch.size == 1 && batch.keys[0] == 8 && batch.pressed[0], "Hotplug preserves preexisting judgment FIFO");
            source.press(slot, 3, true); source.press(slot, 6, true); source.press(slot, 4, true);
            poll(manager, input, backend);
            check(input.getKeyState(0) && input.startPressed() && input.isSelectPressed(), "Saved mode/bank assignments restored on late attach");
            input.getJudgeEventQueue().drain(batch);
            check(batch.size == 1 && batch.pressed[0] && input.getControllerReplayKeys()[0] == 0, "Late controller opts into judgment and replay FIFO");
            source.remove(slot);
            poll(manager, input, backend);
            check(!input.getKeyState(0) && !input.startPressed() && !input.isSelectPressed(), "Unplug releases keys and START/SELECT despite 100ms duration");
            input.getJudgeEventQueue().drain(batch);
            check(batch.size == 1 && !batch.pressed[0], "Unplug produces one ordered judgment release");
            check(source.closed.size() == 1, "Unplug closes native handle once");
            slot = source.add(202, "serial-A");
            poll(manager, input, backend);
            check(manager.controllerInventory() == first, "Reconnecting an existing slot does not republish the inventory");
            check(input.getBMInputProcessor()[0] == processor && manager.getControllers().size == 1
                    && controller == manager.getControllers().first(), "Instance-ID change reuses binding slot and processor");
            source.press(slot, 3, true); poll(manager, input, backend);
            check(input.getKeyState(0), "Reconnected saved button works");
            input.getJudgeEventQueue().drain(batch);
            source.remove(slot); slot = source.add(303, "serial-A");
            // Producer unplug/replug can occur before the input consumer gets another turn.
            poll(manager, input, backend);
            input.getJudgeEventQueue().drain(batch);
            check(!input.getKeyState(0) && batch.size == 1 && !batch.pressed[0], "Same-poll reconnect cannot hide the release");
            source.press(slot, 3, true); poll(manager, input, backend);
            check(input.getKeyState(0), "Fresh input after same-poll reconnect works");
            input.getJudgeEventQueue().drain(batch);
            int second = source.add(404, "serial-B"); poll(manager, input, backend);
            check(input.getBMInputProcessor().length == 2 && input.getBMInputProcessor()[1].getName().equals("Hotplug device-2"),
                    "Same-name devices have distinct stable binding slots");
            source.press(second, 3, true); poll(manager, input, backend);
            check(input.getKeyState(0) && input.getKeyState(1), "Second attachment does not reset first device's keys");
            input.getJudgeEventQueue().drain(batch);
            source.remove(slot); poll(manager, input, backend);
            check(!input.getKeyState(0) && input.getKeyState(1), "Unplug affects only owning controller");
            int restored = source.add(505, "serial-A"); poll(manager, input, backend);
            source.press(restored, 3, true); poll(manager, input, backend);
            check(input.getKeyState(0) && input.getKeyState(1) && input.getBMInputProcessor().length == 2,
                    "First duplicate reconnect keeps both original assignments");
            var replay = new ReplayData(); replay.keylog = input.getKeyInputLog(); replay.controllerEventKeys = input.getControllerReplayKeys();
            var expected = replay.keylog;
            input.beginInputJudge(null, replay.controllerEventKeys, true);
            for (var event : expected) input.setKeyState(event.getKeycode(), event.isPressed(), event.getTime());
            input.getJudgeEventQueue().drain(batch);
            check(batch.size == expected.length, "Hotplug press/release log round-trips through replay FIFO");
            for (int i = 0; i < expected.length; i++) check(batch.keys[i] == expected[i].getKeycode()
                    && batch.pressed[i] == expected[i].isPressed() && batch.times[i] == expected[i].getTime(), "Replay edge order and clock");
            input.endInputJudge(); input.resetAllKeyState();
            source.remove(restored); source.remove(second); poll(manager, input, backend);
            restored = source.add(606, "serial-A"); poll(manager, input, backend);
            input.beginInputJudge(null, null, false);
            source.press(restored, 3, true); source.press(restored, 3, false); source.remove(restored);
            poll(manager, input, backend); input.getJudgeEventQueue().drain(batch);
            check(batch.size == 2 && batch.pressed[0] && !batch.pressed[1], "Native short tap before unplug is preserved in order");
        } finally {
            if (input != null) input.dispose();
            manager.dispose();
            Runnable task; while ((task = posted.poll()) != null) task.run();
            Gdx.input = oldInput; BMSPlayerInputProcessor.controllerPollRunner = oldRunner;
        }
    }

    private static void configure(ControllerConfig config, String name, int key) {
        config.setName(name); Arrays.fill(config.getKeyAssign(), -1);
        config.getKeyAssign()[key] = 3; config.setStart(6); config.setSelect(4); config.setDuration(100);
    }

    private static void poll(SdlRawControllerManager manager, BMSPlayerInputProcessor input, ControllerBackend backend) {
        if (backend == ControllerBackend.SDL_RAW) manager.poll();
        input.poll();
    }

    private static final class Source implements SdlRawSource {
        final ArrayList<Long> closed = new ArrayList<>();
        final SdlDeviceRegistry registry = new SdlDeviceRegistry(closed::add);
        final ArrayList<boolean[]> buttons = new ArrayList<>();
        final ArrayDeque<Event> pending = new ArrayDeque<>();
        Runnable onUpdate = () -> { };
        int add(int id, String identity) {
            registry.add(new Device(id, "Hotplug device", 32, 4, 1, identity), id);
            var devices = registry.devices();
            while (buttons.size() < devices.length) buttons.add(new boolean[32]);
            for (int i = 0; i < devices.length; i++) if (devices[i].id() == id) {
                Arrays.fill(buttons.get(i), false); enqueue(Event.ADDED, id, 0, 0); return i;
            }
            throw new AssertionError("No new slot");
        }
        void remove(int slot) { int id = registry.device(slot).id(); registry.remove(id); enqueue(Event.REMOVED, id, 0, 0); }
        void press(int slot, int button, boolean down) { buttons.get(slot)[button] = down; enqueue(Event.BUTTON, registry.device(slot).id(), button, down ? 1 : 0); }
        void enqueue(int type, int id, int index, float value) {
            var event = new Event(); event.type = type; event.device = id; event.index = index; event.value = value; pending.add(event);
        }
        public Device[] devices() { return registry.devices(); }
        public long inventoryVersion() { return registry.version(); }
        public void update() { onUpdate.run(); }
        public boolean next(Event target) {
            var event = pending.poll(); if (event == null) return false;
            target.type = event.type; target.device = event.device; target.index = event.index; target.value = event.value; return true;
        }
        public boolean connected(int slot) { return registry.handle(slot) != 0; }
        public boolean button(int slot, int button) { return buttons.get(slot)[button]; }
        public float axis(int slot, int axis) { return 0; }
        public int hat(int slot) { return 0; }
        public void close() { registry.close(); }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
