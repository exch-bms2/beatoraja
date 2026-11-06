package bms.player.beatoraja.input;

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

/** Raw SDL joystick producer on the render thread; the input thread consumes only common FIFOs/state. */
public final class SdlRawControllerManager implements ControllerManager, ControllerBackendManager {
    private final Array<Controller> controllers = new Array<>();
    private final Array<ControllerListener> listeners = new Array<>();
    private final Thread owner = Thread.currentThread();
    private final Consumer<Runnable> dispatch;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final PollRunner pollRunner = new PollRunner();
    private final SdlRawSource.Event event = new SdlRawSource.Event();
    private SdlRawSource source;
    private volatile BooleanSupplier pollingShutdown = () -> true;
    private volatile boolean disposed, hasListeners;
    private volatile Controller current;

    public SdlRawControllerManager() {
        this(open(), Gdx.app::postRunnable);
        BMSPlayerInputProcessor.controllerPollRunner = pollRunner;
        Gdx.app.addLifecycleListener(new LifecycleListener() {
            public void pause() { }
            public void resume() { }
            public void dispose() { SdlRawControllerManager.this.dispose(); }
        });
        Gdx.app.postRunnable(new Runnable() {
            public void run() {
                if (disposed) return;
                poll();
                if (!disposed) Gdx.app.postRunnable(this);
            }
        });
    }

    SdlRawControllerManager(SdlRawSource source, Consumer<Runnable> dispatch) {
        this.source = source;
        this.dispatch = dispatch;
        try {
            if (source != null) {
                var devices = source.devices();
                for (int i = 0; i < devices.length; i++) controllers.add(new RawController(devices[i], i));
                Logger.getGlobal().info("Controller backend=SDL_RAW, devices=" + devices.length + "; SDL3 raw joystick events; render-thread native capture");
                poll();
            }
        } catch (RuntimeException | Error failure) {
            dispose();
            throw failure;
        }
        if (controllers.notEmpty()) current = controllers.first();
    }

    private static SdlRawSource open() {
        try { return new SdlRawNative(); }
        catch (RuntimeException | LinkageError failure) {
            Logger.getGlobal().log(Level.WARNING, "SDL_RAW initialization failed; keyboard remains available; no backend fallback", failure);
            return null;
        }
    }

    void poll() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("SDL_RAW update requires its owner thread");
        if (disposed || source == null) return;
        try {
            for (int i = 0; i < controllers.size; i++) {
                var raw = (RawController) controllers.get(i);
                if (raw.diagnostics != null) raw.diagnostics.beginPoll(System.nanoTime());
            }
            source.update();
            while (source.next(event)) for (int i = 0; i < controllers.size; i++) {
                var raw = (RawController) controllers.get(i);
                if (raw.device.id() == event.device) { raw.event(event); break; }
            }
            for (int i = 0; i < controllers.size; i++) ((RawController) controllers.get(i)).snapshot();
        } catch (RuntimeException | LinkageError failure) {
            Logger.getGlobal().log(Level.WARNING, "SDL_RAW polling failed; disabling controller input", failure);
            dispose();
        }
    }

    private final class PollRunner implements ControllerBackendManager.PollRunner {
        public void run() { }
        public ControllerBackendManager owner() { return SdlRawControllerManager.this; }
    }
    public void setPollingShutdown(BooleanSupplier shutdown) { pollingShutdown = shutdown; }
    public void dispose() {
        if (!pollingShutdown.getAsBoolean()) return;
        synchronized (this) {
            if (!disposed) {
                disposed = true;
                if (BMSPlayerInputProcessor.controllerPollRunner == pollRunner) BMSPlayerInputProcessor.controllerPollRunner = null;
                for (int i = 0; i < controllers.size; i++) ((RawController) controllers.get(i)).disconnect();
                current = null;
                clearListeners();
            }
        }
        if (Thread.currentThread() == owner) closeNative();
        else dispatch.accept(this::closeNative);
    }
    private void closeNative() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("SDL_RAW shutdown requires its owner thread");
        if (!closed.compareAndSet(false, true)) return;
        try {
            if (source != null) {
                source.close();
                Logger.getGlobal().info("SDL_RAW controller native shutdown complete");
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
    public Array<Controller> getControllers() { return controllers; }
    public Controller getCurrentController() { return current; }
    public Array<ControllerListener> getListeners() { return listeners; }
    public synchronized void addListener(ControllerListener listener) { listeners.add(listener); hasListeners = listeners.notEmpty(); }
    public synchronized void removeListener(ControllerListener listener) { listeners.removeValue(listener, true); hasListeners = listeners.notEmpty(); }
    public synchronized void clearListeners() { listeners.clear(); hasListeners = false; }

    private static final ControllerMapping RAW_MAPPING = new ControllerMapping(-1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 29, 31, 28, 30) { };

    final class RawController implements QueuedController {
        private final SdlRawSource.Device device;
        private final int slot;
        private final boolean[] physical, buttons = new boolean[32];
        private final float[] physicalAxes, axes = new float[8];
        private int pov;
        private SdlRawMapping mapping;
        private final ControllerEventQueue events = new ControllerEventQueue();
        private final ControllerInputDiagnostics diagnostics;
        private final Array<ControllerListener> localListeners = new Array<>();
        private volatile boolean connected, hasLocalListeners;
        private boolean capture, overflowReported, discardPendingEvents;
        private long generation;

        RawController(SdlRawSource.Device device, int slot) {
            this.device = device; this.slot = slot;
            mapping = new SdlRawMapping(new ControllerConfig(), device.buttons(), device.axes());
            physical = new boolean[device.buttons()]; physicalAxes = new float[device.axes()];
            diagnostics = Boolean.getBoolean("beatoraja.inputDiagnostics")
                    ? new ControllerInputDiagnostics("SDL_RAW:" + slot, 32, 8, true) : null;
            Logger.getGlobal().info("Controller backend=SDL_RAW, name=" + device.name() + ", buttons=" + device.buttons()
                    + ", axes=" + device.axes() + ", hats=" + device.hats()
                    + "; JInput-compatible IDs; same-name settings inherited; verify physical order/direction; render-thread native event capture");
        }

        public synchronized void configure(ControllerConfig config) {
            var replacement = new SdlRawMapping(config, device.buttons(), device.axes());
            if (mapping.same(replacement)) return;
            mapping = replacement;
            publish(false);
            resetEvents(capture);
        }
        synchronized void event(SdlRawSource.Event event) {
            // Native events may predate focus regain even after the Java FIFO was reset.
            if (discardPendingEvents) return;
            if (event.type == SdlRawSource.Event.BUTTON && event.index >= 0 && event.index < physical.length) {
                physical[event.index] = event.value != 0;
                long targets = mapping.buttonTargets[event.index];
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
            return (mapping.buttons[i] >= 0 && physical[mapping.buttons[i]]) || SdlRawMapping.hat(i, pov);
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
            publish(continuous);
            discardPendingEvents = false;
            if (!continuous) resetEvents(capture);
            connected = true;
            if (diagnostics != null) diagnostics.endPoll(System.nanoTime(), true);
            if (events.isOverflowed() && !overflowReported) {
                overflowReported = true;
                Logger.getGlobal().severe("SDL_RAW controller input queue overflow; stopping to avoid invalid scores");
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
            if (wasConnected) resetEvents(capture);
            if (current == this) current = null;
        }
        public synchronized void resetEvents(boolean active) { capture = active; events.reset(active, buttons); generation++; }
        public synchronized void windowActivityChanged() { discardPendingEvents = true; }
        public synchronized long inputGeneration() { return generation; }
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
        public String getUniqueId() { return "SDL_RAW:" + device.id(); }
        public boolean isConnected() { return connected; }
        public int getMinButtonIndex() { return 0; }
        public int getMaxButtonIndex() { return 31; }
        public int getAxisCount() { return 8; }
        public ControllerMapping getMapping() { return RAW_MAPPING; }
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
