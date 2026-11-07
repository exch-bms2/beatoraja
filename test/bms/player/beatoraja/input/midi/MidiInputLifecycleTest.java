package bms.player.beatoraja.input.midi;

import javax.sound.midi.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Fake MIDI transmitters/devices only; never opens the host's MIDI devices. */
public final class MidiInputLifecycleTest {
    public static void main(String[] args) throws Exception {
        orderedClose(); openFailure(); activeCallback();
        System.out.println("MidiInputLifecycleTest passed (callback gate, transmitter/device order, partial initialization and cleanup failures)");
    }
    private static void orderedClose() throws Exception {
        var events = new ArrayList<String>();
        var a = new Device("a", events);
        var b = new Device("b", events);
        a.failTransmitClose = true; a.failDeviceClose = true;
        var midi = new MidiInputProcessor(null, List.of(a, b)); midi.open(); midi.open();
        Receiver receiver = a.transmitter.receiver;
        receiver.send(note(), 0); check(midi.hasLastPressedKey(), "Normal MIDI input remains available");
        midi.close(); midi.close(); midi.open();
        midi.clearLastPressedKey(); receiver.send(note(), 0);
        check(!midi.hasLastPressedKey(), "Late callbacks after close cannot update input");
        check(events.equals(List.of("open-a", "open-b", "tx-a", "tx-b", "device-a", "device-b")),
                "Close all transmitters before devices, despite individual failures: " + events);
    }
    private static void openFailure() {
        var events = new ArrayList<String>();
        var unavailable = new Device("unavailable", events); unavailable.unavailable = true;
        var valid = new Device("valid", events);
        var midi = new MidiInputProcessor(null, List.of(unavailable, valid)); midi.open(); midi.close();
        check(Collections.frequency(events, "device-unavailable") == 1 && events.contains("tx-valid"),
                "Unavailable devices close their partial state once; other devices still open");
        events.clear();
        var bad = new Device("bad", events); bad.failReceiver = true;
        var failed = new MidiInputProcessor(null, List.of(valid, bad));
        try { failed.open(); throw new AssertionError(); } catch (IllegalStateException expected) { }
        failed.close();
        check(events.containsAll(List.of("tx-valid", "tx-bad", "device-valid", "device-bad")), "Failure attaching a receiver releases all acquired resources");
    }
    private static void activeCallback() throws Exception {
        var events = new CopyOnWriteArrayList<String>();
        var device = new Device("active", events);
        var midi = new MidiInputProcessor(null, List.of(device)); midi.open();
        Receiver receiver = device.transmitter.receiver;
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread callback = new Thread(() -> {
            try {
                synchronized (receiver) {
                    receiver.send(note(), 0); entered.countDown();
                    check(release.await(5, TimeUnit.SECONDS), "Callback release timed out");
                }
            } catch (Throwable e) { failure.set(e); }
        });
        callback.start(); check(entered.await(5, TimeUnit.SECONDS), "Callback entered");
        var closing = new AtomicBoolean();
        Thread close = new Thread(() -> { closing.set(true); midi.close(); }); close.start();
        try {
            while (!closing.get()) Thread.yield();
            Thread.sleep(20);
            check(!events.contains("tx-active") && !events.contains("device-active"), "Wait for callback before native close");
        } finally { release.countDown(); callback.join(5000); close.join(5000); }
        check(!callback.isAlive() && !close.isAlive() && failure.get() == null, "Callback/close complete without deadlock");
        midi.clearLastPressedKey(); receiver.send(note(), 0);
        check(!midi.hasLastPressedKey(), "Receiver remains gated after shutdown");
    }
    private static ShortMessage note() {
        try { var msg = new ShortMessage(); msg.setMessage(ShortMessage.NOTE_ON, 0, 60, 100); return msg; }
        catch (InvalidMidiDataException e) { throw new AssertionError(e); }
    }
    private static final class Device implements MidiDevice {
        final Info info;
        final List<String> events;
        final String name;
        final Sender transmitter = new Sender();
        boolean open, unavailable, failReceiver, failTransmitClose, failDeviceClose;
        Device(String name, List<String> events) { this.name = name; this.events = events; info = new Info(name, "test", "test", "1") { }; }
        @Override public Info getDeviceInfo() { return info; }
        @Override public void open() throws MidiUnavailableException {
            events.add("open-" + name); if (unavailable) throw new MidiUnavailableException(); open = true;
        }
        @Override public void close() {
            events.add("device-" + name); open = false;
            if (failDeviceClose) throw new IllegalStateException("Expected device close failure");
        }
        @Override public boolean isOpen() { return open; }
        @Override public long getMicrosecondPosition() { return -1; }
        @Override public int getMaxReceivers() { return 0; }
        @Override public int getMaxTransmitters() { return -1; }
        @Override public Receiver getReceiver() throws MidiUnavailableException { throw new MidiUnavailableException(); }
        @Override public List<Receiver> getReceivers() { return List.of(); }
        @Override public Transmitter getTransmitter() { return transmitter; }
        @Override public List<Transmitter> getTransmitters() { return List.of(transmitter); }
        final class Sender implements Transmitter {
            Receiver receiver;
            @Override public void setReceiver(Receiver receiver) { this.receiver = receiver; if (failReceiver) throw new IllegalStateException("Attach failed"); }
            @Override public Receiver getReceiver() { return receiver; }
            @Override public void close() { events.add("tx-" + name); if (failTransmitClose) throw new IllegalStateException("Expected transmitter failure"); }
        }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
