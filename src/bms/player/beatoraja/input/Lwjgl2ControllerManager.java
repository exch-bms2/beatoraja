package bms.player.beatoraja.input;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

/**
 * gdx-controllers 2.x manager retaining raw LWJGL2 buttons, axes and the first POV.
 * No SDL gamepad remapping is applied. Enumeration is performed at startup.
 */
public final class Lwjgl2ControllerManager implements ControllerManager, Disposable {
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
        Controllers.preferredManager = Lwjgl2ControllerManager.class.getName();
    }

    /** Used by gdx-controllers' preferredManager factory on the application thread. */
    public Lwjgl2ControllerManager() {
        this(openDevices(), Gdx.app::postRunnable);
        BMSPlayerInputProcessor.controllerPollRunner = pollRunner;
        Gdx.app.addLifecycleListener(new LifecycleListener() {
            @Override public void pause() { }
            @Override public void resume() { }
            @Override public void dispose() { Lwjgl2ControllerManager.this.dispose(); }
        });
    }

    private Lwjgl2ControllerManager(Devices devices, Consumer<Runnable> dispatch) {
        this(devices.controllers(), devices.poll(), devices.close(), dispatch);
    }

    Lwjgl2ControllerManager(org.lwjgl.input.Controller[] devices, Runnable poll, Runnable close,
            Consumer<Runnable> dispatch) {
        this.pollDevices = poll;
        this.closeDevices = close;
        this.dispatch = dispatch;
        try {
            for (var device : devices) controllers.add(new RawController(device));
        } catch (RuntimeException | LinkageError error) {
            dispose();
            throw error;
        }
        if (controllers.notEmpty()) current = controllers.first();
    }

    private static Devices openDevices() {
        boolean owned = !org.lwjgl.input.Controllers.isCreated();
        try {
            if (owned) prepareNativeLibraries();
            org.lwjgl.input.Controllers.create();
            var devices = new org.lwjgl.input.Controller[org.lwjgl.input.Controllers.getControllerCount()];
            for (int i = 0; i < devices.length; i++) devices[i] = org.lwjgl.input.Controllers.getController(i);
            return new Devices(devices, () -> {
                org.lwjgl.input.Controllers.poll();
                // This application consumes snapshots, not LWJGL's growing event queue.
                org.lwjgl.input.Controllers.clearEvents();
            }, () -> { if (owned) org.lwjgl.input.Controllers.destroy(); });
        } catch (Exception | LinkageError error) {
            try {
                if (owned && org.lwjgl.input.Controllers.isCreated()) org.lwjgl.input.Controllers.destroy();
            } catch (RuntimeException | LinkageError cleanupError) {
                error.addSuppressed(cleanupError);
            }
            Logger.getGlobal().log(Level.WARNING, "LWJGL2 raw controller initialization failed; keyboard remains available", error);
            return new Devices(new org.lwjgl.input.Controller[0], () -> { }, () -> { });
        }
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
            Logger.getGlobal().log(Level.WARNING, "LWJGL2 controller polling failed; controller input disabled", error);
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
            Logger.getGlobal().log(Level.WARNING, "LWJGL2 controller cleanup failed", error);
        }
    }

    private record Devices(org.lwjgl.input.Controller[] controllers, Runnable poll, Runnable close) { }

    /** A neutral semantic mapping: only the legacy HAT IDs have known meanings. */
    private static final ControllerMapping RAW_MAPPING = new ControllerMapping(
            -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1,
            BMControllerInputProcessor.BMKeys.HAT1_TOP, BMControllerInputProcessor.BMKeys.HAT1_BOTTOM,
            BMControllerInputProcessor.BMKeys.HAT1_LEFT, BMControllerInputProcessor.BMKeys.HAT1_RIGHT) { };

    final class RawController implements Controller {
        private final org.lwjgl.input.Controller device;
        private final String name;
        private final String id;
        private final boolean[] buttons;
        private final int rawButtonCount;
        private final float[] axes;
        private final Array<ControllerListener> localListeners = new Array<>();
        private volatile boolean connected = true;
        private volatile boolean hasLocalListeners;

        RawController(org.lwjgl.input.Controller device) {
            this.device = device;
            this.name = device.getName();
            this.id = name + ":" + device.getIndex();
            this.rawButtonCount = device.getButtonCount();
            this.buttons = new boolean[Math.max(32, rawButtonCount)];
            this.axes = new float[device.getAxisCount()];
            for (int i = 0; i < axes.length; i++) device.setDeadZone(i, 0);
            Logger.getGlobal().info("Controller backend=LWJGL2 raw, name=" + name + ", buttons="
                    + device.getButtonCount() + ", axes=" + axes.length
                    + "; legacy key IDs retained; verify assignments after OIS migration");
        }

        void poll() {
            for (int i = 0; i < axes.length; i++) {
                float value = device.getAxisValue(i);
                value = Float.isFinite(value) ? Math.max(-1, Math.min(1, value)) : 0;
                if (axes[i] != value) {
                    axes[i] = value;
                    current = this;
                    notifyChange(i, false, false, value);
                }
            }
            float povX = device.getPovX();
            float povY = device.getPovY();
            for (int i = 0; i < buttons.length; i++) {
                boolean pressed = i < rawButtonCount && device.isButtonPressed(i);
                pressed |= switch (i) {
                    case BMControllerInputProcessor.BMKeys.HAT1_LEFT -> povX < 0;
                    case BMControllerInputProcessor.BMKeys.HAT1_TOP -> povY < 0;
                    case BMControllerInputProcessor.BMKeys.HAT1_RIGHT -> povX > 0;
                    case BMControllerInputProcessor.BMKeys.HAT1_BOTTOM -> povY > 0;
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
