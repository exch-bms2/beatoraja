package bms.player.beatoraja.input.controller;

import bms.player.beatoraja.input.BMSPlayerInputProcessor;

import bms.player.beatoraja.PlayModeConfig.ControllerConfig;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.controllers.*;
import com.badlogic.gdx.utils.Array;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Shared SDL3 FIFO/lifecycle; sources expose either raw channels or Jamepad-compatible channels. */
public class SdlRawControllerManager implements ControllerManager, ControllerBackendManager {
    private final Array<Controller> controllers = new Array<>();
    private final Array<ControllerListener> listeners = new Array<>();
    private final Thread owner = Thread.currentThread();
    private final boolean async;
    private final boolean mapped;
    private final String backend;
    private final Consumer<Runnable> dispatch;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final PollRunner pollRunner = new PollRunner();
    private final SdlRawSource.Event event = new SdlRawSource.Event();
    private SdlRawSource source;
    private volatile BooleanSupplier pollingShutdown = () -> true;
    private volatile boolean disposed, hasListeners;
    private boolean workerStarted;
    private long inventoryVersion = -1;
    private long controllerVersion;
    private volatile Controller current;

    public SdlRawControllerManager() {
        this(false);
    }

    protected SdlRawControllerManager(boolean async) {
        this(async ? ControllerBackend.SDL_RAW_ASYNC : ControllerBackend.SDL_RAW);
    }

    protected SdlRawControllerManager(ControllerBackend backend) {
        this(open(backend), Gdx.app::postRunnable, backend);
        BMSPlayerInputProcessor.controllerPollRunner = pollRunner;
        Gdx.app.addLifecycleListener(new LifecycleListener() {
            public void pause() { }
            public void resume() { }
            public void dispose() { SdlRawControllerManager.this.dispose(); }
        });
        if (!async) Gdx.app.postRunnable(new Runnable() {
            public void run() {
                if (disposed) return;
                poll();
                if (!disposed) Gdx.app.postRunnable(this);
            }
        });
    }

    SdlRawControllerManager(SdlRawSource source, Consumer<Runnable> dispatch) {
        this(source, dispatch, false);
    }

    SdlRawControllerManager(SdlRawSource source, Consumer<Runnable> dispatch, boolean async) {
        this(source, dispatch, async ? ControllerBackend.SDL_RAW_ASYNC : ControllerBackend.SDL_RAW);
    }

    SdlRawControllerManager(SdlRawSource source, Consumer<Runnable> dispatch, ControllerBackend selected) {
        if (selected != ControllerBackend.SDL_RAW && selected != ControllerBackend.SDL_RAW_ASYNC && selected != ControllerBackend.SDL_ASYNC)
            throw new IllegalArgumentException("Not an SDL3 backend: " + selected);
        this.source = source;
        this.dispatch = dispatch;
        async = selected != ControllerBackend.SDL_RAW;
        mapped = selected == ControllerBackend.SDL_ASYNC;
        backend = selected.name();
        try {
            if (source != null) {
                refreshDevices();
                Logger.getGlobal().info("Controller backend=" + backend + ", devices=" + controllers.size
                        + (mapped ? "; SDL3 Gamepad, Jamepad-compatible logical IDs" : "; SDL3 raw joystick events")
                        + "; acquisition=" + (async ? "input-thread" : "render-thread"));
                poll();
            }
        } catch (RuntimeException | Error failure) {
            dispose();
            throw failure;
        }
        if (controllers.notEmpty()) current = controllers.first();
    }

    private static SdlRawSource open(ControllerBackend backend) {
        try { return backend == ControllerBackend.SDL_ASYNC ? new SdlGamepadNative() : new SdlRawNative(backend == ControllerBackend.SDL_RAW_ASYNC); }
        catch (RuntimeException | LinkageError failure) {
            Logger.getGlobal().log(Level.WARNING, backend + " initialization failed; keyboard remains available; no backend fallback", failure);
            return null;
        }
    }

    synchronized void poll() {
        if (!async && Thread.currentThread() != owner) throw new IllegalStateException("SDL_RAW update requires its owner thread");
        if (disposed || source == null) return;
        if (async && !workerStarted && Thread.currentThread() != owner) {
            workerStarted = true;
            Logger.getGlobal().info(backend + " input polling started: thread=" + Thread.currentThread().getName()
                    + "; native update/read independent of rendering; judgment clock unchanged");
        }
        try {
            for (int i = 0; i < controllers.size; i++) {
                var raw = (RawController) controllers.get(i);
                if (raw.diagnostics != null) raw.diagnostics.beginPoll(System.nanoTime());
            }
            source.update();
            while (source.next(event)) {
                if (event.type == SdlRawSource.Event.ADDED) refreshDevices();
                else for (int i = 0; i < controllers.size; i++) {
                    var raw = (RawController) controllers.get(i);
                    if (raw.device.id() == event.device) {
                        if (event.type == SdlRawSource.Event.REMOVED) raw.disconnect();
                        else raw.event(event);
                        break;
                    }
                }
            }
            for (int i = 0; i < controllers.size; i++) ((RawController) controllers.get(i)).snapshot();
        } catch (RuntimeException | LinkageError failure) {
            Logger.getGlobal().log(Level.WARNING, backend + " polling failed; disabling controller input", failure);
            // The native call has returned. Exclude further polls and close on the owner,
            // without stopping the shared keyboard/MIDI polling thread on a backend failure.
            disable();
            scheduleClose();
        }
    }

    private void refreshDevices() {
        if (inventoryVersion == source.inventoryVersion()) return;
        var devices = source.devices();
        for (int i = 0; i < devices.length; i++) {
            if (i == controllers.size) {
                controllers.add(new RawController(devices[i], i));
                controllerVersion++;
            }
            else {
                var raw = (RawController) controllers.get(i);
                if (raw.device.id() != devices[i].id()) {
                    raw.disconnect();
                    raw.device = devices[i];
                    raw.snapshot();
                }
            }
        }
        inventoryVersion = source.inventoryVersion();
    }

    private final class PollRunner implements ControllerBackendManager.PollRunner {
        public void run() { if (async) poll(); }
        public ControllerBackendManager owner() { return SdlRawControllerManager.this; }
    }
    public void setPollingShutdown(BooleanSupplier shutdown) { pollingShutdown = shutdown; }
    public void dispose() {
        if (!pollingShutdown.getAsBoolean()) return;
        disable();
        scheduleClose();
    }
    private synchronized void disable() {
        if (!disposed) {
            disposed = true;
            if (BMSPlayerInputProcessor.controllerPollRunner == pollRunner) BMSPlayerInputProcessor.controllerPollRunner = null;
            for (int i = 0; i < controllers.size; i++) ((RawController) controllers.get(i)).disconnect();
            current = null;
            clearListeners();
        }
    }
    private void scheduleClose() {
        if (Thread.currentThread() == owner) closeNative();
        else dispatch.accept(this::closeNative);
    }
    private synchronized void closeNative() {
        if (Thread.currentThread() != owner) throw new IllegalStateException(backend + " shutdown requires its owner thread");
        if (!closed.compareAndSet(false, true)) return;
        try {
            if (source != null) {
                source.close();
                Logger.getGlobal().info(backend + " controller native shutdown complete");
            }
        }
        finally {
            source = null;
            pollingShutdown = () -> true;
            for (int i = 0; i < controllers.size; i++) {
                var raw = (RawController) controllers.get(i);
                raw.clearListeners();
                if (raw.diagnostics != null) raw.diagnostics.close();
            }
        }
    }
    public synchronized Array<Controller> getControllers() { return new Array<>(controllers); }
    public synchronized long controllerVersion() { return controllerVersion; }
    public Controller getCurrentController() { return current; }
    public Array<ControllerListener> getListeners() { return listeners; }
    public synchronized void addListener(ControllerListener listener) { listeners.add(listener); hasListeners = listeners.notEmpty(); }
    public synchronized void removeListener(ControllerListener listener) { listeners.removeValue(listener, true); hasListeners = listeners.notEmpty(); }
    public synchronized void clearListeners() { listeners.clear(); hasListeners = false; }

    private static final ControllerMapping RAW_MAPPING = new ControllerMapping(-1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 29, 31, 28, 30) { };

    final class RawController implements QueuedController {
        private volatile SdlRawSource.Device device;
        private final int slot;
        private final boolean[] physical, buttons = new boolean[32];
        private final float[] physicalAxes, axes = new float[8];
        private int pov;
        private SdlRawMapping mapping;
        private final ControllerEventQueue events = new ControllerEventQueue();
        private final ControllerInputDiagnostics diagnostics;
        private final Array<ControllerListener> localListeners = new Array<>();
        private volatile boolean connected, hasLocalListeners;
        private boolean capture, overflowReported, discardPendingEvents, previouslyConnected;
        private long generation;
        private long connectionGeneration;

        RawController(SdlRawSource.Device device, int slot) {
            this.device = device; this.slot = slot;
            mapping = new SdlRawMapping(new ControllerConfig(), device.buttons(), device.axes());
            physical = new boolean[device.buttons()]; physicalAxes = new float[device.axes()];
            diagnostics = Boolean.getBoolean("beatoraja.inputDiagnostics")
                    ? new ControllerInputDiagnostics(backend + ":" + slot, 32, 8, true) : null;
            Logger.getGlobal().info("Controller backend=" + backend + ", name=" + device.name() + ", buttons=" + device.buttons()
                    + ", axes=" + device.axes() + ", hats=" + device.hats()
                    + (mapped ? "; SDL bindings shared; Jamepad-compatible logical IDs; verify SDL2/SDL3 device name/mapping"
                            : "; JInput-compatible IDs; same-name settings inherited; verify physical order/direction") + "; acquisition="
                    + (async ? "input-thread" : "render-thread"));
        }

        public synchronized void configure(ControllerConfig config) {
            if (mapped) return; // Raw conversion profiles must not reinterpret saved SDL/Jamepad IDs.
            var replacement = new SdlRawMapping(config, device.buttons(), device.axes());
            if (mapping.same(replacement)) return;
            mapping = replacement;
            publish(false);
            resetEvents(capture);
        }
        synchronized void event(SdlRawSource.Event event) {
            if (diagnostics != null) {
                switch (event.type) {
                    case SdlRawSource.Event.BUTTON -> diagnostics.trace("button", event.index, event.value, "received");
                    case SdlRawSource.Event.AXIS -> diagnostics.trace("axis", event.index, event.value, "received");
                    case SdlRawSource.Event.HAT -> diagnostics.trace("pov", event.index, event.value, "received");
                    default -> { }
                }
            }
            // Native events may predate focus regain even after the Java FIFO was reset.
            if (discardPendingEvents) return;
            if (event.type == SdlRawSource.Event.BUTTON && event.index >= 0 && event.index < physical.length) {
                physical[event.index] = event.value != 0;
                long targets = mapping.buttonTargets[event.index];
                int alias = mapped ? SdlGamepadCompatibility.dpadAlias(event.index) : -1;
                if (alias >= 0) {
                    targets |= 1L << alias;
                    if (diagnostics != null) diagnostics.trace("pov", alias - 28, event.value, "mapped-dpad");
                }
                while (targets != 0) {
                    int i = Long.numberOfTrailingZeros(targets); targets &= targets - 1;
                    boolean value = digital(i);
                    if (diagnostics != null) diagnostics.event(i, value ? 1 : 0);
                    if (connected) events.offer(i, value);
                }
            } else if (event.type == SdlRawSource.Event.HAT && event.index == 0) {
                pov = (int) event.value;
                for (int i = 28; i < 32; i++) {
                    if (diagnostics != null) diagnostics.event(i, digital(i) ? 1 : 0);
                    if (connected) events.offer(i, digital(i));
                }
            }
            // Axes intentionally remain final snapshots: preserve JKOC/scratch update frequency.
        }
        private boolean digital(int i) {
            int dpad = mapped ? SdlGamepadCompatibility.dpadButton(i) : -1;
            return (mapping.buttons[i] >= 0 && physical[mapping.buttons[i]]) || SdlRawMapping.hat(i, pov)
                    || (dpad >= 0 && dpad < physical.length && physical[dpad]);
        }
        synchronized void snapshot() {
            if (!source.connected(slot)) {
                disconnect();
                if (diagnostics != null) diagnostics.endPoll(System.nanoTime(), false);
                return;
            }
            boolean continuous = connected;
            for (int i = 0; i < physical.length; i++) physical[i] = source.button(slot, i);
            for (int i = 0; i < physicalAxes.length; i++) {
                float value = source.axis(slot, i);
                physicalAxes[i] = Float.isFinite(value) ? Math.max(-1, Math.min(1, value)) : 0;
            }
            pov = source.hat(slot);
            publish(continuous || previouslyConnected);
            discardPendingEvents = false;
            if (!continuous && previouslyConnected) connectionGeneration++;
            if (!continuous && !previouslyConnected) resetEvents(capture);
            connected = true;
            previouslyConnected = true;
            if (diagnostics != null) diagnostics.endPoll(System.nanoTime(), true);
            if (events.isOverflowed() && !overflowReported) {
                overflowReported = true;
                Logger.getGlobal().severe(backend + " controller input queue overflow; stopping to avoid invalid scores");
                if (Gdx.app != null) Gdx.app.exit();
            }
        }
        private void publish(boolean continuous) {
            for (int i = 0; i < 32; i++) {
                boolean down = digital(i);
                if (continuous) events.offer(i, down);
                if (buttons[i] != down) { buttons[i] = down; notifyChange(i, true, down, 0); }
            }
            for (int i = 0; i < 8; i++) {
                float value = mapping.axes[i] < 0 ? 0 : physicalAxes[mapping.axes[i]];
                if (mapping.inverted[i]) value = -value;
                if (axes[i] != value) {
                    axes[i] = value;
                    if (diagnostics != null) diagnostics.event(32 + i, value);
                    notifyChange(i, false, false, value);
                }
            }
        }
        synchronized void disconnect() {
            boolean wasConnected = connected;
            connected = false;
            Arrays.fill(buttons, false); Arrays.fill(axes, 0); Arrays.fill(physical, false); Arrays.fill(physicalAxes, 0); pov = 0;
            if (wasConnected) {
                connectionGeneration++;
                // Keep preceding native edges in order, followed by unconditional releases.
                for (int i = 0; i < buttons.length; i++) events.offer(i, false);
                generation++;
            }
            if (current == this) current = null;
        }
        public synchronized void resetEvents(boolean active) { capture = active; events.reset(active, buttons); generation++; }
        public synchronized void windowActivityChanged() { discardPendingEvents = true; }
        public synchronized long inputGeneration() { return generation; }
        public synchronized long connectionGeneration() { return connectionGeneration; }
        public ControllerEventQueue events() { return events; }
        public ControllerInputDiagnostics diagnostics() { return diagnostics; }
        private void notifyChange(int index, boolean button, boolean down, float value) {
            current = this;
            if (!hasListeners && !hasLocalListeners) return;
            dispatch.accept(() -> {
                synchronized (SdlRawControllerManager.this) { if (!disposed && connected) notifyList(listeners, index, button, down, value); }
                synchronized (this) { if (!disposed && connected) notifyList(localListeners, index, button, down, value); }
            });
        }
        private void notifyList(Array<ControllerListener> list, int index, boolean button, boolean down, float value) {
            for (int i = 0; i < list.size; i++) {
                var listener = list.get(i);
                if (button ? (down ? listener.buttonDown(this, index) : listener.buttonUp(this, index))
                        : listener.axisMoved(this, index, value)) break;
            }
        }
        public synchronized boolean getButton(int i) { return connected && i >= 0 && i < 32 && buttons[i]; }
        public synchronized float getAxis(int i) { return connected && i >= 0 && i < 8 ? axes[i] : 0; }
        public String getName() { return device.name(); }
        public String getUniqueId() { return (mapped ? "SDL:" : "SDL_RAW:") + device.id(); }
        public boolean isConnected() { return connected; }
        public int getMinButtonIndex() { return 0; }
        public int getMaxButtonIndex() { return 31; }
        public int getAxisCount() { return mapped ? device.axes() : 8; }
        public ControllerMapping getMapping() { return mapped ? SdlGamepadCompatibility.MAPPING : RAW_MAPPING; }
        public ControllerPowerLevel getPowerLevel() { return ControllerPowerLevel.POWER_UNKNOWN; }
        public boolean canVibrate() { return false; }
        public boolean isVibrating() { return false; }
        public void startVibration(int duration, float strength) { }
        public void cancelVibration() { }
        public boolean supportsPlayerIndex() { return false; }
        public int getPlayerIndex() { return PLAYER_IDX_UNSET; }
        public void setPlayerIndex(int index) { }
        public synchronized void addListener(ControllerListener listener) { if (!localListeners.contains(listener, true)) localListeners.add(listener); hasLocalListeners = localListeners.notEmpty(); }
        public synchronized void removeListener(ControllerListener listener) { localListeners.removeValue(listener, true); hasLocalListeners = localListeners.notEmpty(); }
        private synchronized void clearListeners() { localListeners.clear(); hasLocalListeners = false; }
    }
}
