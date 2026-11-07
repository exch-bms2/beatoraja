package bms.player.beatoraja.input.controller;

import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;

import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerAdapter;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Json;
import net.java.games.input.Component;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

import bms.player.beatoraja.PlayModeConfig.ControllerConfig;
import bms.player.beatoraja.input.controller.BMControllerInputProcessor.BMKeys;

/** No windows or physical devices: exercise the real raw adapter and BMS input translation. */
public final class JInputControllerCompatibilityTest {
    public static void main(String[] args) throws Exception {
        String previous = Controllers.preferredManager;
        try {
            JInputControllerManager.configure();
            check(Controllers.preferredManager.equals(JInputControllerManager.class.getName()), "Raw manager selection");
        } finally {
            Controllers.preferredManager = previous;
        }
        testRawInputs();
        testScratchAndAssignments();
        testListenersAndCleanup();
        testFailureCleanup();
        testNames();
        testNativeExtraction();
        testEnumeration();
        if (!Arrays.asList(args).contains("--packaged")) testLwjgl2Parity();
        testDisconnection();
        testJkocFourthAxis();
        testDiagnostics(!Arrays.asList(args).contains("--packaged"));
        System.out.println("JInputControllerCompatibilityTest passed");
    }

    private static void testRawInputs() {
        FakeDevice fake = new FakeDevice(32, 2);
        int[] polls = {0};
        var manager = new JInputControllerManager(new net.java.games.input.Controller[]{fake.device},
                () -> polls[0]++, () -> { }, runnable -> { throw new AssertionError("No listeners, no dispatch"); });
        try {
            Controller controller = manager.getControllers().first();
            if (!Boolean.getBoolean("beatoraja.inputDiagnostics"))
                check(((JInputControllerManager.RawController) controller).diagnostics == null,
                        "Diagnostics allocate nothing by default");
            fake.buttons[0] = fake.buttons[27] = true;
            fake.axes[0] = 0.008f;
            fake.axes[1] = -0.75f;
            float[][] directions = {{0,0}, {-1,0}, {-1,-1}, {0,-1}, {1,-1}, {1,0}, {1,1}, {0,1}, {-1,1}};
            for (float[] direction : directions) {
                fake.povX = direction[0];
                fake.povY = direction[1];
                manager.poll();
                check(controller.getButton(0) && controller.getButton(27), "Physical button IDs retained");
                check(controller.getButton(28) == (direction[0] < 0), "POV left");
                check(controller.getButton(29) == (direction[1] < 0), "POV top");
                check(controller.getButton(30) == (direction[0] > 0), "POV right");
                check(controller.getButton(31) == (direction[1] > 0), "POV bottom");
                check(controller.getAxis(0) == 0.008f && controller.getAxis(1) == -0.75f, "Small/raw axis values retained");
            }
            fake.povX = fake.povY = 0;
            fake.buttons[28] = true;
            manager.poll();
            check(controller.getButton(28), "Physical button 28 ORed with POV, as before");
            check(!controller.getButton(-1) && !controller.getButton(32), "Out-of-range buttons are neutral");
            check(controller.getAxis(-1) == 0 && controller.getAxis(2) == 0, "Absent axes are neutral");
            fake.axes[0] = Float.NaN;
            fake.axes[1] = 2;
            manager.poll();
            check(controller.getAxis(0) == 0 && controller.getAxis(1) == 1, "Invalid axes sanitized");
            check(polls[0] == directions.length + 2, "Backend polled once per input tick");
            check(controller.getMapping().buttonDpadLeft == BMKeys.HAT1_LEFT, "Legacy HAT mapping");
        } finally {
            manager.dispose();
        }
    }

    private static void testScratchAndAssignments() throws Exception {
        check(BMKeys.HAT1_LEFT == 28 && BMKeys.HAT1_BOTTOM == 31
                && BMKeys.AXIS1_PLUS == 32 && BMKeys.AXIS8_MINUS == 47 && BMKeys.MAXID == 48, "Stable saved IDs");
        var saved = new ControllerConfig(new int[]{0, 27, 28, 29, 30, 31, 32, 33, 47}, 8, 9);
        saved.setName("Dedicated controller");
        var json = new Json();
        var restored = json.fromJson(ControllerConfig.class, json.toJson(saved));
        check(Arrays.equals(saved.getKeyAssign(), restored.getKeyAssign())
                && restored.getStart() == 8 && restored.getSelect() == 9, "Saved assignments round trip unchanged");

        FakeDevice fake = new FakeDevice(2, 1);
        var manager = new JInputControllerManager(new net.java.games.input.Controller[]{fake.device}, () -> { }, () -> { }, Runnable::run);
        try {
            var config = new ControllerConfig(new int[]{-1}, -1, -1);
            config.setDuration(0);
            var input = new BMControllerInputProcessor(null, "test", manager.getControllers().first(), config);
            input.setEnable(true);
            input.clear();
            var stateField = BMControllerInputProcessor.class.getDeclaredField("buttonstate");
            stateField.setAccessible(true);
            boolean[] state = (boolean[]) stateField.get(input);
            fake.povY = -1;
            fake.axes[0] = 1;
            manager.poll();
            input.poll(0);
            check(state[29] && state[32] && !state[33], "POV and digital axis reach BMS input");
            fake.axes[0] = -1;
            manager.poll();
            input.poll(1000);
            check(!state[32] && state[33] && !state[47], "Negative axis and absent eighth axis");

            config.setJKOC(true);
            input.setConfig(config);
            manager.poll();
            input.poll(2000);
            check(state[33], "JKOC axis fallback tolerates missing fourth axis");
            config.setJKOC(false);
            config.setAnalogScratch(true);
            long time = 10000;
            for (int version : new int[]{ControllerConfig.ANALOG_SCRATCH_VER_1, ControllerConfig.ANALOG_SCRATCH_VER_2}) {
                config.setAnalogScratchMode(version);
                input.setConfig(config);
                fake.axes[0] = 0;
                manager.poll();
                input.poll(time);
                fake.axes[0] = 0.03f;
                manager.poll();
                input.poll(time + 1000);
                check(state[32] && !state[33], "Analog scratch positive direction, version " + version);
                fake.axes[0] = -0.03f;
                manager.poll();
                input.poll(time + 2000);
                fake.axes[0] = -0.06f;
                manager.poll();
                input.poll(time + 3000);
                check(!state[32] && state[33], "Analog scratch reversal, version " + version);
                var value = BMControllerInputProcessor.class.getDeclaredMethod("getAnalogValue", int.class);
                value.setAccessible(true);
                check((float) value.invoke(input, 32) == -0.06f && (float) value.invoke(input, 33) == 0.06f,
                        "Signed analog values use the current snapshot");
                time += 10000;
            }
            check(BMControllerInputProcessor.computeAnalogDiff(0.99f, -0.99f) > 0
                    && BMControllerInputProcessor.computeAnalogDiff(-0.99f, 0.99f) < 0, "Scratch wraparound retained");
        } finally {
            manager.dispose();
        }
    }

    private static void testListenersAndCleanup() {
        FakeDevice first = new FakeDevice(1, 0);
        FakeDevice second = new FakeDevice(1, 0);
        List<Runnable> queued = new ArrayList<>();
        int[] closed = {0};
        int[] notifications = {0};
        var manager = new JInputControllerManager(new net.java.games.input.Controller[]{first.device, second.device},
                () -> { }, () -> closed[0]++, queued::add);
        var controller = manager.getControllers().get(1);
        var listener = new ControllerAdapter() {
            @Override public boolean buttonDown(Controller controller, int button) { notifications[0]++; return false; }
            @Override public boolean buttonUp(Controller controller, int button) { notifications[0]++; return false; }
        };
        manager.addListener(listener);
        controller.addListener(listener);
        second.buttons[0] = true;
        manager.poll();
        check(manager.getCurrentController() == controller && notifications[0] == 0, "Dispatch on app thread");
        queued.remove(0).run();
        check(notifications[0] == 2, "Global and local listeners notified");
        manager.poll();
        check(queued.isEmpty(), "No event for unchanged inputs");
        second.buttons[0] = false;
        manager.poll();
        manager.dispose();
        manager.dispose();
        manager.poll();
        queued.remove(0).run();
        check(closed[0] == 1 && notifications[0] == 2, "Close once, suppress pending events after disposal");
        check(!controller.isConnected() && !controller.getButton(0) && controller.getAxis(0) == 0
                && manager.getCurrentController() == null && manager.getListeners().isEmpty(), "Disposed input neutral");
    }

    private static void testFailureCleanup() {
        int[] closed = {0};
        var manager = new JInputControllerManager(new net.java.games.input.Controller[0],
                () -> { throw new IllegalStateException("Expected test polling failure"); }, () -> closed[0]++, Runnable::run);
        manager.poll();
        manager.poll();
        manager.dispose();
        check(closed[0] == 1, "Polling failure closes once");
        FakeDevice invalid = new FakeDevice(1, 1);
        invalid.failInitialization = true;
        try {
            new JInputControllerManager(new net.java.games.input.Controller[]{invalid.device}, () -> { },
                    () -> closed[0]++, Runnable::run);
            throw new AssertionError("Expected initialization failure");
        } catch (IllegalStateException expected) {
            check(closed[0] == 2, "Adapter initialization failure also releases backend");
        }
    }

    private static void testNames() {
        String japanese = "\u5c02\u7528\u30b3\u30f3\u30c8\u30ed\u30fc\u30e9\u30fc";
        check(KeyboardQueueTestSupport.matchesControllerName(japanese, japanese), "Unicode names");
        String legacy = new String(japanese.getBytes(Charset.forName("EUC_JP")), StandardCharsets.UTF_8);
        check(KeyboardQueueTestSupport.matchesControllerName(legacy, japanese), "Legacy encoding fallback");
        check(KeyboardQueueTestSupport.matchesControllerName("Controller-2", "Controller-2"), "Duplicate-name suffix");
        check(!KeyboardQueueTestSupport.matchesControllerName("a", "b")
                && !KeyboardQueueTestSupport.matchesControllerName(null, "a"), "Different/missing names");
    }

    private static void testNativeExtraction() throws Exception {
        String property = "net.java.games.input.librarypath";
        String previous = System.getProperty(property);
        try {
            check(JInputControllerManager.nativeLibraries("Windows 11").equals(
                    List.of("jinput-dx8_64.dll", "jinput-raw_64.dll", "jinput-wintab.dll")), "JInput 2.0.10 Windows natives");
            check(JInputControllerManager.nativeLibraries("Linux").equals(List.of("libjinput-linux64.so")),
                    "JInput 2.0.10 Linux native");
            for (String os : List.of("Windows 11", "Linux", "Mac OS X")) {
                for (String name : JInputControllerManager.nativeLibraries(os)) {
                    check(JInputControllerCompatibilityTest.class.getClassLoader().getResource(name) != null,
                            "Packaged JInput native: " + name);
                }
            }
            System.setProperty(property, "/explicit-jinput-path");
            JInputControllerManager.prepareNativeLibraries();
            check(System.getProperty(property).equals("/explicit-jinput-path"), "Honor native path override");
            System.clearProperty(property);
            JInputControllerManager.prepareNativeLibraries();
            Path directory = Path.of(System.getProperty(property));
            for (String name : JInputControllerManager.nativeLibraries(System.getProperty("os.name"))) {
                check(Files.isRegularFile(directory.resolve(name)), "Extracted JInput native: " + name);
                try (var source = JInputControllerCompatibilityTest.class.getClassLoader().getResourceAsStream(name)) {
                    check(Arrays.equals(source.readAllBytes(), Files.readAllBytes(directory.resolve(name))),
                            "Extracted native matches selected JAR: " + name);
                }
            }
            if (System.getProperty("os.name").startsWith("Mac")) {
                check(Files.isRegularFile(directory.resolve(System.mapLibraryName("jinput-osx"))), "Java 17 dylib alias");
                check(Files.mismatch(directory.resolve("libjinput-osx.jnilib"),
                        directory.resolve(System.mapLibraryName("jinput-osx"))) == -1, "Universal macOS alias content");
            }
        } finally {
            if (previous == null) System.clearProperty(property);
            else System.setProperty(property, previous);
        }
    }

    private static void testEnumeration() {
        FakeDevice keyboard = new FakeDevice(1, 0);
        keyboard.type = net.java.games.input.Controller.Type.KEYBOARD;
        FakeDevice mouse = new FakeDevice(1, 0);
        mouse.type = net.java.games.input.Controller.Type.MOUSE;
        FakeDevice parent = new FakeDevice(0, 0);
        FakeDevice first = new FakeDevice(2, 1);
        FakeDevice second = new FakeDevice(3, 2);
        FakeDevice third = new FakeDevice(4, 3);
        parent.children = new net.java.games.input.Controller[]{first.device, second.device};
        parent.failInitialization = true;
        var manager = new JInputControllerManager(new net.java.games.input.Controller[]{
                keyboard.device, parent.device, mouse.device, third.device}, () -> { }, () -> { }, Runnable::run);
        try {
            check(manager.getControllers().size == 3, "Keyboard/mouse excluded and child devices expanded");
            for (int i = 0; i < 3; i++) {
                var controller = manager.getControllers().get(i);
                check(controller.getAxisCount() == i + 1, "Leaf enumeration order retained");
                check(controller.getUniqueId().equals("Raw device:" + i), "Stable indices for duplicate device names");
            }
        } finally {
            manager.dispose();
        }
    }

    /** Compare with the actual previous LWJGL2 wrapper, without loading hardware natives. */
    private static void testLwjgl2Parity() throws Exception {
        FakeDevice reference = new FakeDevice(32, 8);
        FakeDevice direct = new FakeDevice(32, 8);
        reference.axisComponents[1].deadZone = direct.axisComponents[1].deadZone = 0.1f;
        var type = Class.forName("org.lwjgl.input.JInputController");
        var constructor = type.getDeclaredConstructor(int.class, net.java.games.input.Controller.class);
        constructor.setAccessible(true);
        var legacy = (org.lwjgl.input.Controller) constructor.newInstance(0, reference.device);
        for (int i = 0; i < 8; i++) legacy.setDeadZone(i, 0);
        var manager = new JInputControllerManager(new net.java.games.input.Controller[]{direct.device},
                () -> { }, () -> { }, Runnable::run);
        try {
            var controller = manager.getControllers().first();
            float[] values = {0, 0.008f, 0.05f, 0.1f, 0.5f, 0.99f, -0.99f, -0.5f, -0.03f, 2, 1, -2, -1, 0};
            float[][] directions = {{0,0}, {-1,0}, {-1,-1}, {0,-1}, {1,-1}, {1,0}, {1,1}, {0,1}, {-1,1}};
            for (int frame = 0; frame < values.length * directions.length; frame++) {
                float[] direction = directions[frame % directions.length];
                reference.povX = direct.povX = direction[0];
                reference.povY = direct.povY = direction[1];
                for (int i = 0; i < 32; i++) reference.buttons[i] = direct.buttons[i] = (frame + i) % 3 == 0;
                for (int i = 0; i < 8; i++) reference.axes[i] = direct.axes[i] = values[(frame + i) % values.length];
                // Secondary hats must not affect the first-POV IDs or become axes.
                reference.update(reference.secondPov, frame % 2 == 0 ? Component.POV.UP : Component.POV.DOWN);
                direct.update(direct.secondPov, frame % 2 == 0 ? Component.POV.UP : Component.POV.DOWN);
                legacy.poll();
                manager.poll();
                for (int i = 0; i < 8; i++) check(controller.getAxis(i) == legacy.getAxisValue(i),
                        "LWJGL2 axis normalization parity, frame=" + frame + ", axis=" + i);
                for (int i = 0; i < 32; i++) {
                    boolean pressed = legacy.isButtonPressed(i) || switch (i) {
                        case 28 -> legacy.getPovX() < 0;
                        case 29 -> legacy.getPovY() < 0;
                        case 30 -> legacy.getPovX() > 0;
                        case 31 -> legacy.getPovY() > 0;
                        default -> false;
                    };
                    check(controller.getButton(i) == pressed, "LWJGL2 button/POV parity, frame=" + frame + ", button=" + i);
                }
                check(direct.queue.getNextEvent(direct.event) == false, "JInput events drained every tick");
                org.lwjgl.input.Controllers.clearEvents();
            }
            int polls = direct.pollCount;
            manager.poll();
            check(direct.pollCount == polls + 1, "Poll exactly once even without changed events");
        } finally {
            manager.dispose();
            org.lwjgl.input.Controllers.clearEvents();
        }
    }

    private static void testDisconnection() {
        FakeDevice failed = new FakeDevice(1, 1);
        FakeDevice other = new FakeDevice(1, 1);
        var manager = new JInputControllerManager(new net.java.games.input.Controller[]{failed.device, other.device},
                () -> { }, () -> { }, Runnable::run);
        try {
            failed.buttons[0] = other.buttons[0] = true;
            failed.axes[0] = 1;
            manager.poll();
            var disconnected = manager.getControllers().first();
            failed.pollSuccess = false;
            manager.poll();
            check(!disconnected.isConnected() && !disconnected.getButton(0) && disconnected.getAxis(0) == 0,
                    "Failed poll releases held buttons and axes");
            other.buttons[0] = false;
            manager.poll();
            check(manager.getControllers().get(1).isConnected() && !manager.getControllers().get(1).getButton(0),
                    "Other controller continues after one device fails");
            failed.pollSuccess = true;
            failed.buttons[0] = false;
            manager.poll();
            check(disconnected.isConnected() && !disconnected.getButton(0) && disconnected.getAxis(0) == 1,
                    "Temporary poll failure recovers without losing held axis state");
        } finally {
            manager.dispose();
        }
    }

    private static void testJkocFourthAxis() throws Exception {
        FakeDevice fake = new FakeDevice(0, 4);
        var manager = new JInputControllerManager(new net.java.games.input.Controller[]{fake.device}, () -> { }, () -> { }, Runnable::run);
        try {
            var config = new ControllerConfig(new int[]{-1}, -1, -1);
            config.setJKOC(true);
            config.setDuration(0);
            var input = new BMControllerInputProcessor(null, "test", manager.getControllers().first(), config);
            input.setEnable(true);
            input.clear();
            var field = BMControllerInputProcessor.class.getDeclaredField("buttonstate");
            field.setAccessible(true);
            boolean[] state = (boolean[]) field.get(input);
            fake.axes[3] = 1;
            manager.poll();
            input.poll(0);
            check(state[32] && !state[33] && !state[38], "JKOC fourth axis positive fallback");
            fake.axes[3] = -1;
            manager.poll();
            input.poll(1000);
            check(!state[32] && state[33] && !state[39], "JKOC fourth axis negative fallback");
        } finally {
            manager.dispose();
        }
    }

    private static void testDiagnostics(boolean compareLegacy) throws Exception {
        String previous = System.getProperty("beatoraja.inputDiagnostics");
        System.setProperty("beatoraja.inputDiagnostics", "true");
        try {
            testRawInputs();
            testScratchAndAssignments();
            testJkocFourthAxis();
            if (compareLegacy) testLwjgl2Parity();
            FakeDevice fake = new FakeDevice(1, 1);
            var manager = new JInputControllerManager(new net.java.games.input.Controller[]{fake.device},
                    () -> { }, () -> { }, Runnable::run);
            try {
                var raw = (JInputControllerManager.RawController) manager.getControllers().first();
                var d = raw.diagnostics;
                fake.update(fake.buttonComponents[0], 1);
                fake.update(fake.buttonComponents[0], 0);
                fake.update(fake.pov, Component.POV.UP);
                fake.update(fake.pov, Component.POV.OFF);
                fake.update(fake.axisComponents[0], .5f);
                fake.update(fake.axisComponents[0], -.5f);
                fake.update(fake.axisComponents[0], 0);
                manager.poll();
                check(!raw.getButton(0) && !raw.getButton(29) && raw.getAxis(0) == 0,
                        "Diagnostics do not recover or alter collapsed input");
                check(d.snapshot().buttonCollapsed() == 2 && d.snapshot().povCollapsed() == 2
                        && d.snapshot().axisCollapsed() == 3, "Real adapter instruments intermediate events");
                var keyboard = new bms.player.beatoraja.PlayModeConfig.KeyboardConfig(bms.model.Mode.BEAT_7K, false);
                var target = KeyboardQueueTestSupport.input(keyboard);
                var config = new ControllerConfig(new int[]{0}, -1, -1);
                config.setDuration(10);
                var input = new BMControllerInputProcessor(target, "test", raw, config);
                input.setEnable(true);
                input.clear();
                fake.buttons[0] = true;
                manager.poll();
                input.poll(0);
                fake.buttons[0] = false;
                manager.poll();
                input.poll(1000);
                check(target.getKeyState(0), "Duration retains old state");
                input.poll(10000);
                check(!target.getKeyState(0), "Release accepted at duration boundary");
                check(d.snapshot().gatedSamples() >= 1 && d.snapshot().forwarded() == 2
                        && d.snapshot().gameWrites() == 2 && d.snapshot().gameSlotReplaced() == 1,
                        "Observe duration, forwarding and unconsumed judgment slot");
                fake.pollSuccess = false;
                manager.poll();
                check(d.snapshot().failures() == 1, "Poll failure observed");
                manager.dispose();
                var finalSnapshot = d.snapshot();
                manager.poll();
                check(d.snapshot().equals(finalSnapshot), "Disposal stops observations");
            } finally { manager.dispose(); }
        } finally {
            if (previous == null) System.clearProperty("beatoraja.inputDiagnostics");
            else System.setProperty("beatoraja.inputDiagnostics", previous);
        }
    }

    public static JInputControllerManager createManager(net.java.games.input.Controller[] devices, Runnable poll,
            Runnable close, java.util.function.Consumer<Runnable> dispatch) {
        return new JInputControllerManager(devices, poll, close, dispatch);
    }

    public static final class FakeDevice {
        public final boolean[] buttons;
        public final float[] axes;
        public final net.java.games.input.Controller device;
        final FakeComponent[] buttonComponents;
        final FakeComponent[] axisComponents;
        final FakeComponent pov = new FakeComponent(Component.Identifier.Axis.POV);
        final FakeComponent secondPov = new FakeComponent(Component.Identifier.Axis.POV);
        EventQueue queue = new EventQueue(1024);
        final Event event = new Event();
        final Method addEvent;
        public String name = "Raw device";
        net.java.games.input.Controller.Type type = net.java.games.input.Controller.Type.STICK;
        net.java.games.input.Controller[] children = new net.java.games.input.Controller[0];
        Component[] components;
        public float povX;
        float povY;
        boolean failInitialization;
        boolean pollSuccess = true;
        int pollCount;

        public FakeDevice(int buttonCount, int axisCount) {
            buttons = new boolean[buttonCount];
            axes = new float[axisCount];
            buttonComponents = new FakeComponent[buttonCount];
            axisComponents = new FakeComponent[axisCount];
            for (int i = 0; i < buttonCount; i++) buttonComponents[i] = new FakeComponent(Component.Identifier.Button._0);
            for (int i = 0; i < axisCount; i++) axisComponents[i] = new FakeComponent(Component.Identifier.Axis.X);
            List<Component> all = new ArrayList<>();
            // Interleave components to detect accidental axis/button sorting.
            for (int i = 0; i < Math.max(buttonCount, axisCount); i++) {
                if (i < axisCount) all.add(axisComponents[i]);
                if (i < buttonCount) all.add(buttonComponents[i]);
                if (i == 0) all.add(pov);
            }
            if (all.isEmpty()) all.add(pov);
            all.add(secondPov);
            components = all.toArray(Component[]::new);
            try {
                addEvent = EventQueue.class.getDeclaredMethod("add", Event.class);
                addEvent.setAccessible(true);
            } catch (ReflectiveOperationException error) {
                throw new AssertionError(error);
            }
            device = (net.java.games.input.Controller) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{net.java.games.input.Controller.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getName" -> name;
                        case "getType" -> type;
                        case "getControllers" -> children;
                        case "getComponents" -> {
                            if (failInitialization) throw new IllegalStateException("Expected initialization failure");
                            yield components;
                        }
                        case "getRumblers" -> new net.java.games.input.Rumbler[0];
                        case "getEventQueue" -> queue;
                        case "setEventQueueSize" -> { queue = new EventQueue((int) args[0]); yield null; }
                        case "poll" -> {
                            pollCount++;
                            if (!pollSuccess) yield false;
                            for (int i = 0; i < buttons.length; i++) update(buttonComponents[i], buttons[i] ? 1 : 0);
                            for (int i = 0; i < axes.length; i++) update(axisComponents[i], axes[i]);
                            float value = povY < 0 ? (povX < 0 ? Component.POV.UP_LEFT
                                    : povX > 0 ? Component.POV.UP_RIGHT : Component.POV.UP)
                                    : povY > 0 ? (povX < 0 ? Component.POV.DOWN_LEFT
                                    : povX > 0 ? Component.POV.DOWN_RIGHT : Component.POV.DOWN)
                                    : povX < 0 ? Component.POV.LEFT : povX > 0 ? Component.POV.RIGHT : Component.POV.OFF;
                            update(pov, value);
                            yield true;
                        }
                        default -> throw new AssertionError("Unexpected JInput method: " + method.getName());
                    });
        }

        void update(FakeComponent component, float value) {
            if (Float.floatToIntBits(component.value) == Float.floatToIntBits(value)) return;
            component.value = value;
            event.set(component, value, System.nanoTime());
            try {
                addEvent.invoke(queue, event);
            } catch (ReflectiveOperationException error) {
                throw new AssertionError(error);
            }
        }
    }

    static final class FakeComponent implements Component {
        final Identifier identifier;
        float value, deadZone;
        FakeComponent(Identifier identifier) { this.identifier = identifier; }
        @Override public Identifier getIdentifier() { return identifier; }
        @Override public boolean isRelative() { return false; }
        @Override public boolean isAnalog() { return !(identifier instanceof Identifier.Button); }
        @Override public float getDeadZone() { return deadZone; }
        @Override public float getPollData() { return value; }
        @Override public String getName() { return identifier.getName(); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
