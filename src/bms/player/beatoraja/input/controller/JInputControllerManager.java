package bms.player.beatoraja.input.controller;

import bms.player.beatoraja.input.BMSPlayerInputProcessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.ControllerManager;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.controllers.ControllerPowerLevel;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import net.java.games.input.Component;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;

/**
 * gdx-controllers 2.x manager reading JInput directly with LWJGL2-compatible IDs.
 * No SDL gamepad remapping is applied. Enumeration is performed at startup.
 */
public final class JInputControllerManager implements ControllerManager, ControllerBackendManager {
    static final int DEVICE_EVENT_QUEUE_CAPACITY = 1024;
    private final Array<Controller> controllers = new Array<>();
    private final Array<ControllerListener> listeners = new Array<>();
    private final Runnable pollDevices;
    private final Runnable closeDevices;
    private final Consumer<Runnable> dispatch;
    private final Runnable pollRunner = new PollRunner();
    private volatile BooleanSupplier pollingShutdown = () -> true;
    private volatile Controller current;
    private volatile boolean disposed;
    private volatile boolean hasListeners;

    /** Must be selected before the first call to Controllers.getControllers(). */
    public static void configure() {
        Controllers.preferredManager = JInputControllerManager.class.getName();
    }

    /** Used by gdx-controllers' preferredManager factory on the application thread. */
    public JInputControllerManager() {
        this(openDevices(), Gdx.app::postRunnable);
        BMSPlayerInputProcessor.controllerPollRunner = pollRunner;
        Gdx.app.addLifecycleListener(new LifecycleListener() {
            @Override public void pause() { }
            @Override public void resume() { }
            @Override public void dispose() { JInputControllerManager.this.dispose(); }
        });
    }

    private JInputControllerManager(Devices devices, Consumer<Runnable> dispatch) {
        this(devices.controllers(), devices.poll(), devices.close(), dispatch);
    }

    JInputControllerManager(net.java.games.input.Controller[] devices, Runnable poll, Runnable close,
            Consumer<Runnable> dispatch) {
        this.pollDevices = poll;
        this.closeDevices = close;
        this.dispatch = dispatch;
        try {
            var leaves = leafDevices(devices);
            for (int i = 0; i < leaves.size(); i++) controllers.add(new RawController(leaves.get(i), i));
        } catch (RuntimeException | LinkageError error) {
            dispose();
            throw error;
        }
        if (controllers.notEmpty()) current = controllers.first();
    }

    private static Devices openDevices() {
        try {
            prepareNativeLibraries();
            var devices = ControllerEnvironment.getDefaultEnvironment().getControllers();
            // JInput owns native handles until its JVM shutdown hooks run;
            // it exposes no public close/destroy operation for the default environment.
            return new Devices(devices, () -> { }, () -> { });
        } catch (Exception | LinkageError error) {
            Logger.getGlobal().log(Level.WARNING, "JInput controller initialization failed; keyboard remains available", error);
            return new Devices(new net.java.games.input.Controller[0], () -> { }, () -> { });
        }
    }

    private static List<net.java.games.input.Controller> leafDevices(net.java.games.input.Controller[] devices) {
        List<net.java.games.input.Controller> result = new ArrayList<>();
        for (var device : devices) {
            if (device.getType() != net.java.games.input.Controller.Type.KEYBOARD
                    && device.getType() != net.java.games.input.Controller.Type.MOUSE) collectLeaves(device, result);
        }
        return result;
    }

    private static void collectLeaves(net.java.games.input.Controller device, List<net.java.games.input.Controller> result) {
        var children = device.getControllers();
        if (children.length == 0) result.add(device);
        else for (var child : children) collectLeaves(child, result);
    }

    /** JInput does not extract its own native JARs; honor explicitly supplied paths. */
    static void prepareNativeLibraries() throws IOException {
        if (System.getProperty("net.java.games.input.librarypath") != null) return;
        List<String> libraries = nativeLibraries(System.getProperty("os.name"));
        var loader = new SharedLibraryLoader();
        Path first = loader.extractFile(libraries.get(0), null).toPath();
        var directory = first.getParent().toFile();
        for (int i = 1; i < libraries.size(); i++) loader.extractFileTo(libraries.get(i), directory);
        if (libraries.get(0).equals("libjinput-osx.jnilib")) {
            // JInput ships a universal .jnilib, while Java 17 maps the name to .dylib.
            Path mapped = first.resolveSibling(System.mapLibraryName("jinput-osx"));
            if (!Files.exists(mapped)) Files.copy(first, mapped);
        }
        System.setProperty("net.java.games.input.librarypath", directory.getAbsolutePath());
    }

    static List<String> nativeLibraries(String os) {
        if (os.startsWith("Windows")) return List.of("jinput-dx8_64.dll", "jinput-raw_64.dll", "jinput-wintab.dll");
        if (os.startsWith("Linux")) return List.of("libjinput-linux64.so");
        if (os.startsWith("Mac")) return List.of("libjinput-osx.jnilib");
        throw new IllegalStateException("Unsupported JInput platform: " + os);
    }

    /** Called at the input polling rate, not the rendering frame rate. */
    public synchronized void poll() {
        if (disposed) return;
        try {
            pollDevices.run();
            for (Controller controller : controllers) ((RawController) controller).poll();
        } catch (RuntimeException | LinkageError error) {
            Logger.getGlobal().log(Level.WARNING, "JInput controller polling failed; controller input disabled", error);
            // The failed native call has returned, and this monitor excludes other controller polls.
            // Keep keyboard/MIDI polling alive when only the controller backend fails.
            disposeStopped();
        }
    }

    @Override public Array<Controller> getControllers() { return controllers; }
    @Override public Controller getCurrentController() { return current; }
    @Override public Array<ControllerListener> getListeners() { return listeners; }

    @Override public void addListener(ControllerListener listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener, true)) listeners.add(listener);
            hasListeners = listeners.notEmpty();
        }
    }

    @Override public void removeListener(ControllerListener listener) {
        synchronized (listeners) {
            listeners.removeValue(listener, true);
            hasListeners = listeners.notEmpty();
        }
    }

    @Override public void clearListeners() {
        synchronized (listeners) {
            listeners.clear();
            hasListeners = false;
        }
    }

    private final class PollRunner implements ControllerBackendManager.PollRunner {
        @Override public void run() { poll(); }
        @Override public JInputControllerManager owner() { return JInputControllerManager.this; }
    }

    static JInputControllerManager ownerOf(Runnable runner) {
        return runner instanceof JInputControllerManager.PollRunner owned ? owned.owner() : null;
    }

    public void setPollingShutdown(BooleanSupplier shutdown) { pollingShutdown = shutdown; }

    @Override public void dispose() {
        // Backend lifecycle listeners run before MainController.dispose(). Join outside poll's monitor.
        if (!pollingShutdown.getAsBoolean()) return;
        synchronized (this) { disposeStopped(); }
    }

    private void disposeStopped() {
        if (disposed) return;
        disposed = true;
        if (BMSPlayerInputProcessor.controllerPollRunner == pollRunner) BMSPlayerInputProcessor.controllerPollRunner = null;
        for (Controller controller : controllers) {
            RawController raw = (RawController) controller;
            raw.releaseEvents();
            if (raw.diagnostics != null) raw.diagnostics.close();
            raw.connected = false;
            raw.device = null;
            Arrays.fill(raw.buttonComponents, null);
            Arrays.fill(raw.axisComponents, null);
            raw.pov = null;
            raw.buttonIndices.clear();
            raw.axisIndices.clear();
            raw.event.set(null, 0, 0);
            synchronized (raw.localListeners) {
                raw.localListeners.clear();
                raw.hasLocalListeners = false;
            }
        }
        current = null;
        clearListeners();
        try {
            closeDevices.run();
        } catch (RuntimeException | LinkageError error) {
            Logger.getGlobal().log(Level.WARNING, "JInput controller cleanup failed", error);
        } finally {
            pollingShutdown = () -> true;
        }
    }

    private record Devices(net.java.games.input.Controller[] controllers, Runnable poll, Runnable close) { }

    /** A neutral semantic mapping: only the legacy HAT IDs have known meanings. */
    private static final ControllerMapping RAW_MAPPING = new ControllerMapping(
            -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1,
            BMControllerInputProcessor.BMKeys.HAT1_TOP, BMControllerInputProcessor.BMKeys.HAT1_BOTTOM,
            BMControllerInputProcessor.BMKeys.HAT1_LEFT, BMControllerInputProcessor.BMKeys.HAT1_RIGHT) { };

    final class RawController implements QueuedController {
        final ControllerInputDiagnostics diagnostics;
        final ControllerEventQueue events = new ControllerEventQueue();
        private boolean captureEvents;
        private boolean overflowReported;
        private boolean reconciliationReported;
        private long inputGeneration;
        private net.java.games.input.Controller device;
        private final String name;
        private final String id;
        private final boolean[] buttons;
        private final int rawButtonCount;
        private final float[] axes;
        private final Component[] buttonComponents;
        private final Component[] axisComponents;
        private final IdentityHashMap<Component, Integer> buttonIndices = new IdentityHashMap<>();
        private final IdentityHashMap<Component, Integer> axisIndices = new IdentityHashMap<>();
        private final boolean[] rawButtons;
        private final float[] axisValues;
        private final float[] axisMaximum;
        private final boolean[] buttonEvents, axisEvents;
        private final int deviceQueueCapacity;
        private final Event event = new Event();
        private Component pov;
        private float povValue;
        private final Array<ControllerListener> localListeners = new Array<>();
        private volatile boolean connected = true;
        private volatile boolean hasLocalListeners;

        RawController(net.java.games.input.Controller device, int index) {
            this.device = device;
            this.name = device.getName();
            this.id = name + ":" + index;
            var previousQueue = device.getEventQueue();
            device.setEventQueueSize(DEVICE_EVENT_QUEUE_CAPACITY);
            // AbstractController swallows native resize IOExceptions and retains the old queue.
            deviceQueueCapacity = device.getEventQueue() != previousQueue ? DEVICE_EVENT_QUEUE_CAPACITY : 0;
            if (deviceQueueCapacity == 0) Logger.getGlobal().warning(
                    "JInput event queue resize was not confirmed: " + name
                    + "; requestedCapacity=" + DEVICE_EVENT_QUEUE_CAPACITY);
            List<Component> buttonList = new ArrayList<>();
            List<Component> axisList = new ArrayList<>();
            for (var component : device.getComponents()) {
                if (component.getIdentifier() instanceof Component.Identifier.Button) buttonList.add(component);
                else if (component.getIdentifier().equals(Component.Identifier.Axis.POV)) {
                    if (pov == null) pov = component;
                } else axisList.add(component);
            }
            buttonComponents = buttonList.toArray(Component[]::new);
            axisComponents = axisList.toArray(Component[]::new);
            this.rawButtonCount = buttonComponents.length;
            this.buttons = new boolean[Math.max(32, rawButtonCount)];
            this.axes = new float[axisComponents.length];
            rawButtons = new boolean[rawButtonCount];
            axisValues = new float[axes.length];
            axisMaximum = new float[axes.length];
            buttonEvents = new boolean[rawButtonCount];
            axisEvents = new boolean[axes.length];
            Arrays.fill(axisMaximum, 1);
            for (int i = 0; i < rawButtonCount; i++) {
                buttonIndices.put(buttonComponents[i], i);
                rawButtons[i] = buttonComponents[i].getPollData() != 0;
            }
            for (int i = 0; i < axes.length; i++) {
                axisIndices.put(axisComponents[i], i);
                axisValues[i] = axisComponents[i].getPollData();
            }
            if (pov != null) povValue = pov.getPollData();
            diagnostics = Boolean.getBoolean("beatoraja.inputDiagnostics")
                    ? new ControllerInputDiagnostics(Integer.toString(index), rawButtonCount, axes.length, true) : null;
            if (diagnostics != null) {
                for (int i = 0; i < rawButtonCount; i++) diagnostics.seed(i, rawButtons[i] ? 1 : 0);
                for (int i = 0; i < axes.length; i++) diagnostics.seed(rawButtonCount + i, axisValues[i]);
                diagnostics.seed(rawButtonCount + axes.length, povValue);
            }
            Logger.getGlobal().info("Controller backend=JInput direct, name=" + name + ", buttons="
                    + rawButtonCount + ", axes=" + axes.length
                    + ", eventQueueRequested=" + DEVICE_EVENT_QUEUE_CAPACITY
                    + ", eventQueueResizeConfirmed=" + (deviceQueueCapacity != 0)
                    + "; legacy key IDs retained; verify assignments after OIS migration");
        }

        public synchronized void resetEvents(boolean active) {
            captureEvents = active;
            events.reset(active, buttons);
            inputGeneration++;
        }

        synchronized void releaseEvents() {
            Arrays.fill(buttons, false);
            Arrays.fill(axes, 0);
            connected = false;
            resetEvents(captureEvents);
        }

        public synchronized long inputGeneration() { return inputGeneration; }
        public ControllerEventQueue events() { return events; }
        public ControllerInputDiagnostics diagnostics() { return diagnostics; }

        private boolean buttonPressed(int i) {
            return (i < rawButtonCount && rawButtons[i]) || switch (i) {
                case BMControllerInputProcessor.BMKeys.HAT1_LEFT -> povValue == Component.POV.LEFT
                        || povValue == Component.POV.UP_LEFT || povValue == Component.POV.DOWN_LEFT;
                case BMControllerInputProcessor.BMKeys.HAT1_TOP -> povValue == Component.POV.UP
                        || povValue == Component.POV.UP_LEFT || povValue == Component.POV.UP_RIGHT;
                case BMControllerInputProcessor.BMKeys.HAT1_RIGHT -> povValue == Component.POV.RIGHT
                        || povValue == Component.POV.UP_RIGHT || povValue == Component.POV.DOWN_RIGHT;
                case BMControllerInputProcessor.BMKeys.HAT1_BOTTOM -> povValue == Component.POV.DOWN
                        || povValue == Component.POV.DOWN_LEFT || povValue == Component.POV.DOWN_RIGHT;
                default -> false;
            };
        }

        synchronized void poll() {
            if (device == null) return;
            if (diagnostics != null) diagnostics.beginPoll(System.nanoTime());
            if (!device.poll()) {
                if (diagnostics != null) diagnostics.endPoll(System.nanoTime(), false);
                boolean wasConnected = connected;
                connected = false;
                Arrays.fill(buttons, false);
                Arrays.fill(axes, 0);
                if (wasConnected) resetEvents(captureEvents);
                if (current == this) current = null;
                if (wasConnected) Logger.getGlobal().warning("JInput controller polling failed; retrying: " + name);
                return;
            }
            // Preserve LWJGL2's event-driven normalization, without allocating its
            // per-poll Event and per-change ControllerEvent objects.
            var queue = device.getEventQueue();
            boolean continuous = connected;
            Arrays.fill(buttonEvents, false);
            Arrays.fill(axisEvents, false);
            boolean povEvent = false;
            int eventCount = 0;
            while (queue.getNextEvent(event)) {
                eventCount++;
                Component component = event.getComponent();
                Integer button = buttonIndices.get(component);
                if (button != null) {
                    buttonEvents[button] = true;
                    if (diagnostics != null) diagnostics.event(button, event.getValue() != 0 ? 1 : 0);
                    rawButtons[button] = event.getValue() != 0;
                    if (continuous && button < 32) events.offer(button, buttonPressed(button));
                } else if (component == pov) {
                    povEvent = true;
                    if (diagnostics != null) diagnostics.event(rawButtonCount + axes.length, event.getValue());
                    povValue = event.getValue();
                    if (continuous) for (int i = 28; i < 32; i++) events.offer(i, buttonPressed(i));
                }
                else {
                    Integer axis = axisIndices.get(component);
                    if (axis != null) {
                        axisEvents[axis] = true;
                        if (diagnostics != null) diagnostics.event(rawButtonCount + axis, event.getValue());
                        axisValues[axis] = normalizedAxis(axis);
                    }
                }
            }
            if (diagnostics != null) diagnostics.jinputQueue(eventCount, deviceQueueCapacity);
            // DirectInput captures its snapshot before collecting events. Do not undo a newer
            // event with that older snapshot; reconcile that channel on an event-free poll.
            for (int i = 0; i < rawButtonCount; i++) {
                boolean pressed = buttonComponents[i].getPollData() != 0;
                if (rawButtons[i] != pressed) {
                    if (continuous) mismatch(i, pressed ? 1 : 0, buttonEvents[i]);
                    if (!continuous || !buttonEvents[i]) rawButtons[i] = pressed;
                }
            }
            if (pov != null) {
                float value = pov.getPollData();
                if (Float.compare(povValue, value) != 0) {
                    if (continuous) mismatch(rawButtonCount + axes.length, value, povEvent);
                    if (!continuous || !povEvent) povValue = value;
                }
            }
            for (int i = 0; i < axes.length; i++) {
                // Relative-axis deltas reset each JInput poll; retain legacy event-driven handling.
                if (!axisEvents[i] && !axisComponents[i].isRelative()) {
                    float value = normalizedAxis(i);
                    if (Float.compare(axisValues[i], value) != 0) {
                        if (continuous) mismatch(rawButtonCount + i, value, false);
                        axisValues[i] = value;
                    }
                }
            }
            for (int i = 0; i < axes.length; i++) {
                float value = axisValues[i];
                value = Float.isFinite(value) ? Math.max(-1, Math.min(1, value)) : 0;
                if (axes[i] != value) {
                    axes[i] = value;
                    current = this;
                    notifyChange(i, false, false, value);
                }
            }
            for (int i = 0; i < buttons.length; i++) {
                boolean pressed = buttonPressed(i);
                if (continuous && i < 32) events.offer(i, pressed);
                if (buttons[i] != pressed) {
                    buttons[i] = pressed;
                    current = this;
                    notifyChange(i, true, pressed, 0);
                }
            }
            if (!continuous) resetEvents(captureEvents);
            connected = true; // Publish the updated polling snapshot to rendering/Lua readers.
            if (diagnostics != null) diagnostics.endPoll(System.nanoTime(), true);
            if (events.isOverflowed() && !overflowReported) {
                overflowReported = true;
                Logger.getGlobal().severe("Controller input queue overflow; stopping to avoid invalid scores");
                if (Gdx.app != null) Gdx.app.exit();
            }
        }

        private float normalizedAxis(int axis) {
            Component component = axisComponents[axis];
            float value = component.getPollData();
            if (!Float.isFinite(value)) value = 0;
            if (Math.abs(value) < component.getDeadZone()) value = 0;
            axisMaximum[axis] = Math.max(axisMaximum[axis], Math.abs(value));
            return value / axisMaximum[axis];
        }

        private void mismatch(int channel, float value, boolean deferred) {
            if (diagnostics != null) diagnostics.snapshotMismatch(channel, value, deferred);
            if (!deferred && !reconciliationReported) {
                reconciliationReported = true;
                Logger.getGlobal().warning("JInput snapshot reconciliation: device=" + id
                        + ", channel=" + channel + ", snapshot=" + value
                        + "; event/snapshot mismatch is not proof of native overflow;"
                        + " enable -Dbeatoraja.inputDiagnostics=true for cumulative counts");
            }
        }

        private void notifyChange(int index, boolean button, boolean pressed, float value) {
            if (!hasListeners && !hasLocalListeners) return;
            dispatch.accept(() -> {
                if (!connected || disposed) return;
                notifyList(listeners, index, button, pressed, value);
                notifyList(localListeners, index, button, pressed, value);
            });
        }

        private void notifyList(Array<ControllerListener> list, int index, boolean button, boolean pressed, float value) {
            synchronized (list) {
                for (int i = 0; i < list.size; i++) {
                    ControllerListener listener = list.get(i);
                    boolean handled = button ? (pressed ? listener.buttonDown(this, index) : listener.buttonUp(this, index))
                            : listener.axisMoved(this, index, value);
                    if (handled) break;
                }
            }
        }

        @Override public boolean getButton(int index) { return connected && index >= 0 && index < buttons.length && buttons[index]; }
        @Override public float getAxis(int index) { return connected && index >= 0 && index < axes.length ? axes[index] : 0; }
        @Override public int getMinButtonIndex() { return 0; }
        @Override public int getMaxButtonIndex() { return buttons.length - 1; }
        @Override public int getAxisCount() { return axes.length; }
        @Override public String getName() { return name; }
        @Override public String getUniqueId() { return id; }
        @Override public boolean isConnected() { return connected; }
        @Override public ControllerMapping getMapping() { return RAW_MAPPING; }
        @Override public ControllerPowerLevel getPowerLevel() { return ControllerPowerLevel.POWER_UNKNOWN; }
        @Override public boolean canVibrate() { return false; }
        @Override public boolean isVibrating() { return false; }
        @Override public void startVibration(int duration, float strength) { }
        @Override public void cancelVibration() { }
        @Override public boolean supportsPlayerIndex() { return false; }
        @Override public int getPlayerIndex() { return PLAYER_IDX_UNSET; }
        @Override public void setPlayerIndex(int index) { }
        @Override public void addListener(ControllerListener listener) {
            synchronized (localListeners) {
                if (!localListeners.contains(listener, true)) localListeners.add(listener);
                hasLocalListeners = localListeners.notEmpty();
            }
        }
        @Override public void removeListener(ControllerListener listener) {
            synchronized (localListeners) {
                localListeners.removeValue(listener, true);
                hasLocalListeners = localListeners.notEmpty();
            }
        }
    }
}
