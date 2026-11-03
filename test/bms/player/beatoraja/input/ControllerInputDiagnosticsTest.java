package bms.player.beatoraja.input;

/** Deterministic observations, not a hardware latency benchmark. */
public final class ControllerInputDiagnosticsTest {
    public static void main(String[] args) throws Exception {
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
            check(d.snapshot().equals(s), "Closed collector is inert and close is idempotent");
        }
        System.out.println("ControllerInputDiagnosticsTest passed (synthetic timings)");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
