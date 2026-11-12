package bms.player.beatoraja.input.controller;

import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** Deterministic observations, not a hardware latency benchmark. */
public final class ControllerInputDiagnosticsTest {
    public static void main(String[] args) throws Exception {
        checkAssignmentTrace();
        checkDeviceSelectionCapture();
        try (var d = new ControllerInputDiagnostics("test", 2, 1, false)) {
            d.seed(1, 1);
            d.beginPoll(0);
            d.event(0, 1);
            d.event(0, 1); // Duplicate state is not an edge.
            d.event(0, 0);
            d.event(1, 0);
            d.event(2, .5f);
            d.event(2, -.5f);
            d.event(2, 0);
            d.event(3, .25f);
            d.event(3, 0);
            d.endPoll(1_000_000, true);
            d.beginBm(1_250_000);
            d.endBm(1_500_000, 3, 1, 1);
            d.gameWrite(false);
            d.gameWrite(true);
            var s = d.snapshot();
            check(s.buttonEdges() == 3 && s.buttonCollapsed() == 2, "Short pulse plus visible release");
            check(s.povEdges() == 2 && s.povCollapsed() == 2, "POV round trip");
            check(s.axisEvents() == 3 && s.axisCollapsed() == 3, "Axis round trip");
            check(s.gatedSamples() == 3 && s.acceptedEdges() == 1 && s.forwarded() == 1, "BM stages");
            check(s.gameWrites() == 2 && s.gameSlotReplaced() == 1, "Legacy slot overwrite candidates");
            check(s.timings().get("pollDuration").maxUs() == 1000, "Native poll plus adapter duration");
            check(s.timings().get("snapshotToBm").maxUs() == 250, "Publication to translation");
            check(s.timings().get("bmDuration").maxUs() == 250, "Translation duration");
            d.jinputQueue(1024, 1024);
            d.jinputQueue(32, 0);
            d.snapshotMismatch(0, 1, true);
            d.snapshotMismatch(0, 1, false);
            d.snapshotMismatch(2, .5f, false);
            d.snapshotMismatch(3, .25f, false);
            s = d.snapshot();
            check(s.buttonSnapshotMismatches() == 2 && s.axisSnapshotMismatches() == 1
                    && s.povSnapshotMismatches() == 1 && s.snapshotCorrections() == 3
                    && s.snapshotDeferred() == 1, "Separate mismatch, deferred and correction counts");
            check(s.jinputEvents() == 1056 && s.jinputMaxEvents() == 1024 && s.jinputSaturatedPolls() == 1,
                    "Queue saturation candidates require confirmed capacity");
            check(s.buttonEdges() == 3 && s.axisEvents() == 3 && s.povEdges() == 2,
                    "Snapshot repairs are not observed native edges");
            d.beginPoll(2_000_000);
            d.endPoll(2_500_000, false);
            d.beginBm(2_750_000);
            d.endBm(3_000_000, 0, 0, 0);
            s = d.snapshot();
            check(s.polls() == 2 && s.failures() == 1, "Failed poll counted separately");
            check(s.timings().get("pollGap").meanUs() == 2000, "Poll gap");
            check(s.timings().get("bmGap").meanUs() == 1500, "Translation gap");
            d.close();
            d.close();
            d.beginPoll(9_000_000);
            d.event(0, 1);
            d.endPoll(10_000_000, true);
            d.beginBm(11_000_000);
            d.endBm(12_000_000, 99, 99, 99);
            d.gameWrite(true);
            d.jinputQueue(1024, 1024);
            d.snapshotMismatch(0, 1, false);
            check(d.snapshot().equals(s), "Closed collector is inert and close is idempotent");
        }
        System.out.println("ControllerInputDiagnosticsTest passed (synthetic timings)");
    }

    private static void checkAssignmentTrace() {
        var messages = new ArrayList<String>();
        var logger = Logger.getGlobal();
        var handler = new Handler() {
            @Override public void publish(LogRecord record) { messages.add(record.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        logger.addHandler(handler);
        try (var d = new ControllerInputDiagnostics("assignment-test", 32, 8, false)) {
            d.trace("pov", 0, 1, "received");
            d.traceLogicalPress(28);
            check(messages.isEmpty(), "Detailed tracing is opt-in per assignment");
            d.setAssignmentTrace(true);
            d.trace("button", 7, 1, "received");
            d.trace("axis", 1, -0.5f, "received");
            d.trace("pov", 0, 1, "received");
            d.traceLogicalPress(28);
            d.setAssignmentTrace(false);
            d.traceLogicalPress(29);
            check(messages.size() == 4 && messages.get(0).contains("device=assignment-test")
                    && messages.get(0).contains("kind=button, index=7")
                    && messages.get(1).contains("kind=axis, index=1")
                    && messages.get(2).contains("kind=pov, index=0")
                    && messages.get(3).contains("stage=logical, index=28"), "Trace exposes source and logical IDs");
        } finally {
            logger.removeHandler(handler);
        }
    }

    private static void checkDeviceSelectionCapture() {
        var capture = new DeviceButtonCapture();
        boolean[] physical = new boolean[256];
        var source = (QueuedController) java.lang.reflect.Proxy.newProxyInstance(
                QueuedController.class.getClassLoader(), new Class<?>[]{QueuedController.class},
                (proxy, method, args) -> {
                    if (!method.getName().equals("readSelectionButtons"))
                        throw new AssertionError("Selection must only observe physical buttons: " + method);
                    System.arraycopy(physical, 0, args[0], 0, physical.length);
                    return null;
                });
        physical[28] = true;
        capture.sample(source);
        capture.sample(source);
        check(capture.take() == -1, "Already-held buttons are ignored");
        physical[28] = false;
        capture.sample(source);
        check(capture.take() == -1, "Release is not a selection");
        physical[28] = true;
        capture.sample(source);
        check(capture.take() == 28 && capture.take() == -1, "Fresh physical press is consumed once");
        capture.reset();
        capture.sample(source);
        check(capture.take() == -1, "Cancel/rearm does not retain a stale press");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
