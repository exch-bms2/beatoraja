package bms.player.beatoraja.input.controller;

import bms.player.beatoraja.input.BMSPlayerInputProcessor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.controllers.*;
import com.badlogic.gdx.controllers.desktop.support.JamepadController;
import com.badlogic.gdx.utils.Array;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** SDL/Jamepad is sampled on its creating thread; the judgment thread reads only cached state/FIFOs. */
public final class SdlControllerManager implements ControllerManager, ControllerBackendManager {
    private final Array<Controller> controllers = new Array<>();
    private final Array<ControllerListener> listeners = new Array<>();
    private final Thread owner = Thread.currentThread();
    private final Runnable update, close;
    private final Consumer<Runnable> dispatch;
    private final AtomicBoolean nativeClosed = new AtomicBoolean();
    private final PollRunner pollRunner = new PollRunner();
    private volatile BooleanSupplier pollingShutdown = () -> true;
    private volatile boolean disposed;
    private volatile boolean hasListeners;
    private volatile Controller current;

    public SdlControllerManager() {
        this(openDevices(), Gdx.app::postRunnable);
        BMSPlayerInputProcessor.controllerPollRunner = pollRunner;
        Gdx.app.addLifecycleListener(new LifecycleListener() {
            public void pause() { }
            public void resume() { }
            public void dispose() { SdlControllerManager.this.dispose(); }
        });
        Gdx.app.postRunnable(new Runnable() {
            public void run() {
                if (disposed) return;
                poll();
                if (!disposed) Gdx.app.postRunnable(this);
            }
        });
    }

    private SdlControllerManager(Devices devices, Consumer<Runnable> dispatch) {
        this(devices.controllers(), devices.update(), devices.close(), dispatch);
    }

    SdlControllerManager(Controller[] devices, Runnable update, Runnable close, Consumer<Runnable> dispatch) {
        this.update = update; this.close = close; this.dispatch = dispatch;
        try {
            for (int i = 0; i < devices.length; i++) controllers.add(new CachedController(devices[i], i));
            if (controllers.notEmpty()) current = controllers.first();
        } catch (RuntimeException | Error failure) {
            disposed = true;
            try { closeNative(); } catch (RuntimeException | Error cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    private record Devices(Controller[] controllers, Runnable update, Runnable close) { }

    private static Devices openDevices() {
        com.studiohartman.jamepad.ControllerManager manager = null;
        boolean initialized = false;
        try {
            var settings = new com.studiohartman.jamepad.Configuration();
            manager = new com.studiohartman.jamepad.ControllerManager(settings);
            manager.initSDLGamepad(); initialized = true;
            manager.update();
            var found = new java.util.ArrayList<Controller>();
            for (int i = 0; i < settings.maxNumControllers; i++) {
                var index = manager.getControllerIndex(i);
                if (index.isConnected()) found.add(new JamepadController(index));
            }
            Logger.getGlobal().info("Controller backend=SDL/Jamepad, devices=" + found.size()
                    + "; render-thread sampling, no sub-frame history; separate SDL bindings; no automatic fallback");
            if (found.isEmpty()) Logger.getGlobal().warning("SDL found no mapped gamepads; this adapter may need an SDL mapping; JInput was not selected automatically");
            return new Devices(found.toArray(Controller[]::new), manager::update, manager::quitSDLGamepad);
        } catch (RuntimeException | LinkageError failure) {
            if (initialized) try { manager.quitSDLGamepad(); } catch (RuntimeException | LinkageError cleanup) { failure.addSuppressed(cleanup); }
            Logger.getGlobal().log(Level.WARNING, "SDL controller initialization failed; keyboard remains available; no automatic fallback", failure);
            return new Devices(new Controller[0], () -> { }, () -> { });
        }
    }

    /** Called only by the render-thread producer, never by the input polling runner. */
    void poll() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("SDL update requires its creating thread");
        if (disposed) return;
        try {
            update.run();
            for (Controller controller : controllers) ((CachedController) controller).sample();
        } catch (RuntimeException | LinkageError failure) {
            Logger.getGlobal().log(Level.WARNING, "SDL polling failed; disabling controller input", failure);
            dispose();
        }
    }

    private final class PollRunner implements ControllerBackendManager.PollRunner {
        public void run() { /* Native SDL pumping belongs to the rendering thread. */ }
        public ControllerBackendManager owner() { return SdlControllerManager.this; }
    }

    public void setPollingShutdown(BooleanSupplier shutdown) { pollingShutdown = shutdown; }

    public void dispose() {
        if (!pollingShutdown.getAsBoolean()) return;
        synchronized (this) {
            if (!disposed) {
                disposed = true;
                if (BMSPlayerInputProcessor.controllerPollRunner == pollRunner) BMSPlayerInputProcessor.controllerPollRunner = null;
                for (Controller controller : controllers) ((CachedController) controller).disconnect();
                listeners.clear(); hasListeners = false; current = null;
                for (Controller controller : controllers) {
                    var cached = (CachedController) controller;
                    synchronized (cached) { cached.localListeners.clear(); cached.hasLocalListeners = false; }
                }
            }
        }
        if (Thread.currentThread() == owner) closeNative();
        else dispatch.accept(this::closeNative);
    }

    private void closeNative() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("SDL shutdown requires its creating thread");
        if (!nativeClosed.compareAndSet(false, true)) return;
        try { close.run(); Logger.getGlobal().info("SDL controller native shutdown complete"); }
        finally { for (Controller controller : controllers) {
            var cached = (CachedController) controller;
            synchronized (cached) { cached.delegate = null; if (cached.diagnostics != null) cached.diagnostics.close(); }
        } }
    }

    public Array<Controller> getControllers() { return controllers; }
    public Controller getCurrentController() { return current; }
    public Array<ControllerListener> getListeners() { return listeners; }
    public synchronized void addListener(ControllerListener listener) { listeners.add(listener); hasListeners = listeners.notEmpty(); }
    public synchronized void removeListener(ControllerListener listener) { listeners.removeValue(listener, true); hasListeners = listeners.notEmpty(); }
    public synchronized void clearListeners() { listeners.clear(); hasListeners = false; }

    final class CachedController implements QueuedController {
        private Controller delegate;
        private final String name, id;
        private final ControllerMapping mapping;
        private final boolean[] buttons = new boolean[32], sample = new boolean[32];
        private final float[] axes;
        private final ControllerEventQueue events = new ControllerEventQueue();
        private final ControllerInputDiagnostics diagnostics;
        private final Array<ControllerListener> localListeners = new Array<>();
        private boolean capture, overflowReported;
        private volatile boolean connected;
        private volatile boolean hasLocalListeners;
        private long generation;

        CachedController(Controller device, int slot) {
            delegate = device; name = device.getName(); id = "SDL:" + name + ":" + slot;
            mapping = device.getMapping(); axes = new float[Math.min(8, Math.max(0, device.getAxisCount()))];
            diagnostics = Boolean.getBoolean("beatoraja.inputDiagnostics") ? new ControllerInputDiagnostics("SDL:" + slot, 32, axes.length, true) : null;
            Logger.getGlobal().info("Controller backend=SDL/Jamepad, name=" + name + ", axes=" + axes.length + "; bindings are separate from JInput");
            sample();
        }

        synchronized void sample() {
            if (delegate == null) return;
            if (diagnostics != null) diagnostics.beginPoll(System.nanoTime());
            if (!delegate.isConnected()) { disconnect(); if (diagnostics != null) diagnostics.endPoll(System.nanoTime(), false); return; }
            boolean continuous = connected;
            for (int i = 0; i < 32; i++) sample[i] = i <= delegate.getMaxButtonIndex() && delegate.getButton(i);
            if (mapping != null) {
                sample[28] |= pressed(mapping.buttonDpadLeft); sample[29] |= pressed(mapping.buttonDpadUp);
                sample[30] |= pressed(mapping.buttonDpadRight); sample[31] |= pressed(mapping.buttonDpadDown);
            }
            for (int i = 0; i < 32; i++) {
                boolean value = sample[i];
                if (continuous) events.offer(i, value);
                if (buttons[i] != value) {
                    buttons[i] = value;
                    if (diagnostics != null) diagnostics.event(i, value ? 1 : 0);
                    notifyChange(i, true, value, 0);
                }
            }
            for (int i = 0; i < axes.length; i++) {
                float value = delegate.getAxis(i); value = Float.isFinite(value) ? Math.max(-1, Math.min(1, value)) : 0;
                if (axes[i] != value) {
                    axes[i] = value; if (diagnostics != null) diagnostics.event(32 + i, value);
                    notifyChange(i, false, false, value);
                }
            }
            if (!continuous) resetEvents(capture);
            connected = true;
            if (diagnostics != null) diagnostics.endPoll(System.nanoTime(), true);
            if (events.isOverflowed() && !overflowReported) {
                overflowReported = true;
                Logger.getGlobal().severe("SDL controller input queue overflow; stopping to avoid invalid scores");
                if (Gdx.app != null) Gdx.app.exit();
            }
        }

        private boolean pressed(int button) { return button >= 0 && button <= delegate.getMaxButtonIndex() && delegate.getButton(button); }
        synchronized void disconnect() {
            boolean wasConnected = connected; connected = false; Arrays.fill(buttons, false); Arrays.fill(axes, 0);
            if (current == this) current = null;
            if (wasConnected) resetEvents(capture);
        }
        public synchronized void resetEvents(boolean active) { capture = active; events.reset(active, buttons); generation++; }
        public synchronized long inputGeneration() { return generation; }
        public ControllerEventQueue events() { return events; }
        public ControllerInputDiagnostics diagnostics() { return diagnostics; }

        private void notifyChange(int index, boolean button, boolean down, float value) {
            current = this;
            if (!hasListeners && !hasLocalListeners) return;
            dispatch.accept(() -> {
                synchronized (SdlControllerManager.this) {
                    if (disposed || !connected) return;
                    notifyList(listeners, index, button, down, value);
                }
                synchronized (this) { if (!disposed && connected) notifyList(localListeners, index, button, down, value); }
            });
        }
        private void notifyList(Array<ControllerListener> list, int index, boolean button, boolean down, float value) {
            for (int i = 0; i < list.size; i++) {
                ControllerListener listener = list.get(i);
                if (button ? (down ? listener.buttonDown(this, index) : listener.buttonUp(this, index)) : listener.axisMoved(this, index, value)) break;
            }
        }
        public synchronized boolean getButton(int index) { return connected && index >= 0 && index < 32 && buttons[index]; }
        public synchronized float getAxis(int index) { return connected && index >= 0 && index < axes.length ? axes[index] : 0; }
        public String getName() { return name; }
        public String getUniqueId() { return id; }
        public boolean isConnected() { return connected; }
        public int getMinButtonIndex() { return 0; }
        public int getMaxButtonIndex() { return 31; }
        public int getAxisCount() { return axes.length; }
        public ControllerMapping getMapping() { return mapping; }
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
    }
}
