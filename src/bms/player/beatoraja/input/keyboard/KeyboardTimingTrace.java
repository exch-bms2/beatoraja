package bms.player.beatoraja.input.keyboard;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Bounded, opt-in trace. Never supplies a clock to the input or judgment code. */
public final class KeyboardTimingTrace implements AutoCloseable {
    private final ArrayList<Row> rows = new ArrayList<>();
    private final int capacity;
    private boolean closed;
    private int dropped;

    public KeyboardTimingTrace(int capacity) { this.capacity = capacity; }

    public static KeyboardTimingTrace createIfEnabled(boolean windows) {
        return windows && System.getProperty("os.name", "").startsWith("Windows")
                && Boolean.getBoolean("beatoraja.windowsKeyboardTiming") ? new KeyboardTimingTrace(32768) : null;
    }

    public static final class Edge {
        private final KeyboardTimingTrace trace;
        private final int messageTickMs, messageAgeMs;
        private final long peekNanos, callbackNanos;
        public long publishedNanos;

        Edge(KeyboardTimingTrace trace, int messageTickMs, int messageAgeMs, long peekNanos, long callbackNanos) {
            this.trace = trace;
            this.messageTickMs = messageTickMs;
            this.messageAgeMs = messageAgeMs;
            this.peekNanos = peekNanos;
            this.callbackNanos = callbackNanos;
        }
    }

    public Edge unmatched(long callbackNanos) { return new Edge(this, 0, 0, 0, callbackNanos); }

    public record Accepted(Edge edge, long readNanos) {
        public void dispatch(boolean down, long judgmentUs, long dispatchNanos) {
            edge.trace.record(new Row(this, down, judgmentUs, dispatchNanos));
        }
    }

    private record Row(Accepted input, boolean down, long judgmentUs, long dispatchNanos) {}

    private synchronized void record(Row row) {
        if (closed) return;
        if (rows.size() < capacity) rows.add(row);
        else dropped++;
    }

    synchronized void write(Writer output) throws IOException {
        output.write("edge\tmessage_to_peek_ms\tpeek_to_glfw_us\tglfw_to_publish_us\tpublish_to_read_us"
                + "\tread_to_judge_us\tmessage_tick_ms\tpeek_ns\tcallback_ns\tpublish_ns\tread_ns\tjudge_dispatch_ns\tjudgment_us\n");
        for (var row : rows) {
            var input = row.input();
            var edge = input.edge();
            String os = edge.peekNanos == 0 ? "NA\tNA"
                    : edge.messageAgeMs + "\t" + (edge.callbackNanos - edge.peekNanos) / 1000;
            output.write((row.down() ? "DOWN" : "UP") + "\t" + os
                    + "\t" + (edge.publishedNanos - edge.callbackNanos) / 1000
                    + "\t" + (input.readNanos() - edge.publishedNanos) / 1000
                    + "\t" + (row.dispatchNanos() - input.readNanos()) / 1000
                    + (edge.peekNanos == 0 ? "\tNA\tNA" : "\t" + Integer.toUnsignedLong(edge.messageTickMs) + "\t" + edge.peekNanos)
                    + "\t" + edge.callbackNanos + "\t" + edge.publishedNanos + "\t" + input.readNanos()
                    + "\t" + row.dispatchNanos() + "\t" + row.judgmentUs() + "\n");
        }
        output.write("# dropped_after_capacity=" + dropped + "\n");
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        try {
            Path file = Files.createTempFile(Path.of("."), "windows-keyboard-timing-", ".tsv");
            try (var output = Files.newBufferedWriter(file)) { write(output); }
            Logger.getGlobal().info("Windows keyboard timing: " + file.toAbsolutePath() + ", events=" + rows.size());
        } catch (IOException | RuntimeException e) {
            Logger.getGlobal().log(Level.WARNING, "Could not write Windows keyboard timing", e);
        } finally {
            rows.clear();
        }
    }
}
