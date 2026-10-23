package bms.player.beatoraja.input;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Consumer;
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
public final class JInputControllerManager implements ControllerManager, Disposable {
    private final Array<Controller> controllers = new Array<>();
    private final Array<ControllerListener> listeners = new Array<>();
    private final Runnable pollDevices;
    private final Runnable closeDevices;
    private final Consumer<Runnable> dispatch;
    private final Runnable pollRunner = this::poll;
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
            // JInput 2.0.5 owns native handles until its JVM shutdown hooks run;
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
            // JInput 2.0.5 ships .jnilib, while Java 17 maps the name to .dylib.
            Path mapped = first.resolveSibling(System.mapLibraryName("jinput-osx"));
            if (!Files.exists(mapped)) Files.copy(first, mapped);
        }
        System.setProperty("net.java.games.input.librarypath", directory.getAbsolutePath());
    }

    static List<String> nativeLibraries(String os) {
        if (os.startsWith("Windows")) return List.of("jinput-dx8.dll", "jinput-dx8_64.dll",
                "jinput-raw.dll", "jinput-raw_64.dll", "jinput-wintab.dll");
        if (os.startsWith("Linux")) return List.of("libjinput-linux.so", "libjinput-linux64.so");
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
            dispose();
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

    @Override public synchronized void dispose() {
        if (disposed) return;
        disposed = true;
        if (BMSPlayerInputProcessor.controllerPollRunner == pollRunner) BMSPlayerInputProcessor.controllerPollRunner = null;
        for (Controller controller : controllers) {
            RawController raw = (RawController) controller;
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

    final class RawController implements Controller {
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
            Arrays.fill(axisMaximum, 1);
            for (int i = 0; i < rawButtonCount; i++) {
                buttonIndices.put(buttonComponents[i], i);
                rawButtons[i] = buttonComponents[i].getPollData() != 0;
            }
            for (int i = 0; i < axes.length; i++) {
                axisIndices.put(axisComponents[i], i);
                axisValues[i] = axisComponents[i].getPollData();
            }
            Logger.getGlobal().info("Controller backend=JInput direct, name=" + name + ", buttons="
                    + rawButtonCount + ", axes=" + axes.length
                    + "; legacy key IDs retained; verify assignments after OIS migration");
        }

        void poll() {
            if (device == null) return;
            if (!device.poll()) {
                boolean wasConnected = connected;
                connected = false;
                Arrays.fill(buttons, false);
                Arrays.fill(axes, 0);
                if (current == this) current = null;
                if (wasConnected) Logger.getGlobal().warning("JInput controller polling failed; retrying: " + name);
                return;
            }
            // Preserve LWJGL2's event-driven normalization, without allocating its
            // per-poll Event and per-change ControllerEvent objects.
            var queue = device.getEventQueue();
            while (queue.getNextEvent(event)) {
                Component component = event.getComponent();
                Integer button = buttonIndices.get(component);
                if (button != null) rawButtons[button] = event.getValue() != 0;
                else if (component == pov) povValue = event.getValue();
                else {
                    Integer axis = axisIndices.get(component);
                    if (axis != null) {
                        float value = component.getPollData();
                        if (!Float.isFinite(value)) value = 0;
                        if (Math.abs(value) < component.getDeadZone()) value = 0;
                        axisMaximum[axis] = Math.max(axisMaximum[axis], Math.abs(value));
                        axisValues[axis] = value / axisMaximum[axis];
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
                boolean pressed = i < rawButtonCount && rawButtons[i];
                pressed |= switch (i) {
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
                if (buttons[i] != pressed) {
                    buttons[i] = pressed;
                    current = this;
                    notifyChange(i, true, pressed, 0);
                }
            }
            connected = true; // Publish the updated polling snapshot to rendering/Lua readers.
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
