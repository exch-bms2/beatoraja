package bms.player.beatoraja.input;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

import bms.player.beatoraja.ReplayData;
import com.badlogic.gdx.utils.Json;

public final class KeyboardEventQueueTest {
    public static void main(String[] args) throws Exception {
        testPublicationAndDuration();
        testLifecycleAndOverflow();
        testJudgmentAndReplay();
        testConcurrentQueue();
        System.out.println("KeyboardEventQueueTest passed");
    }

    private static void testPublicationAndDuration() {
        var q = new KeyboardEventQueue(4);
        var state = new AtomicIntegerArray(256);
        q.configure(new int[] {29, 30}, state);
        q.publish(true, state);
        check(q.pollReady(29, false, 0, Long.MIN_VALUE, 16) == -1, "Discard duplicate baseline");
        q.offer(29, true);
        q.offer(29, false);
        check(q.peek(29) == -1, "Do not expose callbacks before the original snapshot boundary");
        q.publish(true, state);
        check(q.pollReady(29, false, 1000, Long.MIN_VALUE, 16) == 1, "Keep short press");
        check(q.pollReady(29, true, 1000, 1000, 16) == -2, "Keep release pending for duration");
        q.offer(30, true);
        state.set(30, 1);
        q.publish(true, state);
        check(q.pollReady(30, false, 2000, Long.MIN_VALUE, 16) == 1, "Deferred key does not block another key");
        check(q.pollReady(29, true, 16999, 1000, 16) == -2, "Do not shorten duration");
        check(q.pollReady(29, true, 17000, 1000, 16) == 0, "Release stamped at the eligible poll, not callback time");
        q.offer(29, true);
        q.offer(29, false);
        q.publish(true, state);
        check(q.pollReady(29, false, 20000, 17000, 0) == 1
                && q.pollReady(29, true, 20000, 20000, 0) == 0, "Zero duration preserves both edges at the same time");
    }

    private static void testLifecycleAndOverflow() {
        var q = new KeyboardEventQueue(2);
        var state = new AtomicIntegerArray(256);
        q.configure(new int[] {29}, state);
        q.publish(true, state);
        q.pollReady(29, false, 0, Long.MIN_VALUE, 0);
        q.offer(29, true);
        q.publish(false, state);
        check(q.pollReady(29, true, 1000, Long.MIN_VALUE, 0) == 0, "Focus loss discards stale presses and releases held keys");
        q.offer(29, true);
        q.configure(new int[] {30}, state);
        check(q.peek(29) == -1, "Assignment changes clear old-key events");
        q.offer(30, true);
        q.offer(30, false);
        check(q.isOverflowed(), "Bounded queue reports overflow instead of silently losing a release");
        check(q.peek(30) == -1, "Faulted stream is not partly replayed");
        var alias = new KeyboardEventQueue(4);
        alias.configure(new int[] {29, 29}, state);
        alias.publish(true, state);
        alias.pollReady(29, false, 0, Long.MIN_VALUE, 0);
        alias.offer(29, true);
        state.set(29, 1);
        alias.publish(true, state);
        check(alias.pollReady(29, false, 0, Long.MIN_VALUE, 0) == 1 && alias.peek(29) == -1,
                "Duplicate physical assignments do not duplicate captured callbacks");
        state.set(29, 0);
        alias.reset(state);
        state.set(29, 1);
        alias.publish(true, state);
        check(alias.pollReady(29, false, 0, Long.MIN_VALUE, 0) == 1,
                "Reconcile a held key whose callback preceded a reset but whose snapshot followed it");
    }

    private static void testJudgmentAndReplay() {
        var q = new KeyboardJudgeQueue(4);
        q.begin(new int[] {0, 1}, new boolean[256]);
        q.offer(0, true, 123456);
        q.offer(0, false, 123456);
        q.offer(1, true, 123456);
        q.offer(2, true, 123456);
        var batch = new KeyboardJudgeQueue.Batch();
        q.drain(batch);
        check(batch.size == 3 && batch.pressed[0] && !batch.pressed[1] && batch.keys[2] == 1,
                "Keep same-time sequence and exclude other devices' keys");
        for (int i = 0; i < batch.size; i++) check(batch.times[i] == 123456, "Never invent microsecond offsets");
        ReplayData replay = new ReplayData();
        replay.keyboardEventKeys = new int[] {0, 1};
        replay.keylog = new KeyInputLog[] {new KeyInputLog(123456, 0, true), new KeyInputLog(123456, 0, false)};
        replay.shrink();
        ReplayData loaded = new Json().fromJson(ReplayData.class, new Json().toJson(replay));
        check(loaded.validate() && loaded.keyboardEventKeys.length == 2, "Keep keyboard metadata in compressed replay");
        check(loaded.keylog[0].getTime() == loaded.keylog[1].getTime()
                && loaded.keylog[0].isPressed() && !loaded.keylog[1].isPressed(), "Compressed edge order and timestamps unchanged");
        ReplayData old = new Json().fromJson(ReplayData.class, "{}");
        check(old.keyboardEventKeys == null, "Old replay selects the legacy path");
        q.begin(null, new boolean[256]);
        q.offer(0, true, 1000);
        q.drain(batch);
        check(batch.size == 0 && !q.isQueued(0), "Legacy replay does not enter the new judgment stream");
        q.begin(new int[] {0}, new boolean[256]);
        for (int i = 0; i < 5; i++) q.offer(0, (i & 1) == 0, i);
        check(q.isOverflowed(), "Judgment overflow is explicit");
        q.end();
        q.drain(batch);
        check(batch.size == 0, "No events survive end of play");
    }

    private static void testConcurrentQueue() throws Exception {
        var q = new KeyboardJudgeQueue(4096);
        q.begin(new int[] {0}, new boolean[256]);
        var failure = new AtomicReference<Throwable>();
        Thread producer = new Thread(() -> {
            for (int i = 0; i < 2000; i++) q.offer(0, (i & 1) == 0, i);
        });
        Thread consumer = new Thread(() -> {
            try {
                int count = 0;
                var batch = new KeyboardJudgeQueue.Batch();
                long deadline = System.nanoTime() + 3_000_000_000L;
                while (count < 2000 && System.nanoTime() < deadline) {
                    q.drain(batch);
                    for (int i = 0; i < batch.size; i++, count++) {
                        check(batch.times[i] == count && batch.pressed[i] == ((count & 1) == 0), "Concurrent FIFO order");
                    }
                    Thread.yield();
                }
                check(count == 2000, "Drain every event");
            } catch (Throwable e) { failure.set(e); }
        });
        producer.start();
        consumer.start();
        producer.join(5000);
        consumer.join(5000);
        check(!producer.isAlive() && !consumer.isAlive() && failure.get() == null && !q.isOverflowed(),
                "No deadlock/loss: " + failure.get());
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
