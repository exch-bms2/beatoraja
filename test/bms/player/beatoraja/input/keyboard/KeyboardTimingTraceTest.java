package bms.player.beatoraja.input.keyboard;

import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicIntegerArray;
import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import bms.player.beatoraja.input.JudgeEventQueue;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;

/** Portable coverage of the correlation and queues, without installing a Windows hook. */
public final class KeyboardTimingTraceTest {
    public static void main(String[] args) throws Exception {
        matchingAndWrap();
        roundTrip();
        System.out.println("KeyboardTimingTraceTest passed");
    }

    private static void matchingAndWrap() throws Exception {
        var trace = new KeyboardTimingTrace(3);
        var message = new WindowsKeyboardMessageClock.Message();
        check(WindowsKeyboardMessageClock.Message.isKey(0x105)
                && !WindowsKeyboardMessageClock.Message.isKey(0x102), "Only queued key messages can match");
        message.set(0x101, 0x135L << 16, 0xfffffff0, 0x10, 10_000);
        var matched = message.match(trace, 0x135, false, 12_000);
        matched.publishedNanos = 15_000;
        new KeyboardTimingTrace.Accepted(matched, 20_000).dispatch(false, 47, 40_000);
        var missing = message.match(trace, 0x135, false, 12_000);
        missing.publishedNanos = 15_000;
        new KeyboardTimingTrace.Accepted(missing, 20_000).dispatch(false, 48, 40_000);
        message.set(0x100, 0x135L << 16, 34, 36, 10_000);
        var different = message.match(trace, 0x35, true, 12_000);
        different.publishedNanos = 15_000;
        new KeyboardTimingTrace.Accepted(different, 20_000).dispatch(true, 49, 40_000);
        new KeyboardTimingTrace.Accepted(matched, 20_000).dispatch(false, 50, 40_000);
        var rows = rows(trace);
        check(rows[1].equals("UP\t32\t2\t3\t5\t20\t4294967280\t10000\t12000\t15000\t20000\t40000\t47"), rows[1]);
        check(rows[2].startsWith("UP\tNA\tNA\t"), "One Win32 message must not match twice");
        check(rows[3].startsWith("DOWN\tNA\tNA\t"), "Extended and non-extended keys must not match");
        check(rows[4].equals("# dropped_after_capacity=1"), "Trace capacity is bounded");
    }

    private static void roundTrip() throws Exception {
        var queue = new KeyboardEventQueue(4, false, true);
        var snapshot = new AtomicIntegerArray(256);
        queue.configure(new int[]{30}, snapshot);
        queue.publish(true, snapshot);
        var trace = new KeyboardTimingTrace(4);
        queue.offer(30, true, 0, trace.unmatched(System.nanoTime() - 10_000));
        queue.offer(30, false, 0, trace.unmatched(System.nanoTime() - 5_000));
        check(queue.pollReady(30, false, 100, Long.MIN_VALUE, 0) == -1, "Tracing must not publish early");
        queue.publish(true, snapshot);

        var input = KeyboardQueueTestSupport.input(new KeyboardConfig());
        var judge = input.getJudgeEventQueue();
        judge.begin(new int[]{2}, new boolean[256]);
        var batch = new JudgeEventQueue.Batch();
        check(queue.pollReady(30, false, 101, Long.MIN_VALUE, 0) == 1, "Short press is retained");
        var down = queue.lastTrace(30);
        input.keyboardKeyChanged(101, 2, true, new KeyboardTimingTrace.Accepted(down, System.nanoTime()));
        check(queue.pollReady(30, true, 102, 101, 1) == -2, "Tracing must not bypass duration");
        check(queue.pollReady(30, true, 1_101, 101, 1) == 0, "Release stays queued");
        var up = queue.lastTrace(30);
        check(down != up && down.publishedNanos == up.publishedNanos, "Two edges share one publication");
        input.keyboardKeyChanged(1_101, 2, false, new KeyboardTimingTrace.Accepted(up, System.nanoTime()));
        judge.drain(batch);
        check(batch.size == 2 && batch.keys[0] == 2 && batch.times[0] == 101 && batch.times[1] == 1_101,
                "Trace does not replace judgment timestamps");
        batch.traceKeyboardDispatch(0);
        batch.traceKeyboardDispatch(1);
        judge.offer(2, true, 2_101);
        judge.drain(batch);
        batch.traceKeyboardDispatch(0);
        var rows = rows(trace);
        check(rows.length == 4 && rows[1].startsWith("DOWN\t") && rows[1].endsWith("\t101")
                && rows[2].startsWith("UP\t") && rows[2].endsWith("\t1101"), "No leaked metadata into untraced edges");

        queue.offer(30, true, 0, trace.unmatched(System.nanoTime()));
        queue.reset(snapshot);
        check(queue.pollReady(30, true, 2_000, 0, 0) == 0 && queue.lastTrace(30) == null,
                "A focus reset must not reuse the timestamp of an old or synthetic edge");
    }

    private static String[] rows(KeyboardTimingTrace trace) throws Exception {
        var output = new StringWriter();
        trace.write(output);
        return output.toString().split("\n");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
