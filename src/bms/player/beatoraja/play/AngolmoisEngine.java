package bms.player.beatoraja.play;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/** C Angolmois 2.0 alpha 3 (625f843), with SDL polls replaced by fixed 1 ms frames. */
final class AngolmoisEngine {
    static final long FRAME_US = 1_000;
    static final int END = 0, HEAD = 1, NOTE = 2, HIDDEN = 3, MINE = 4;
    static final int BGM = 72, BGA = 73, BPM = 74, STOP = 75;

    static final class ChartObject {
        final double position;
        final int lane, type;
        final double value;
        boolean consumed;

        ChartObject(double position, int lane, int type, double value) {
            this.position = position;
            this.lane = lane;
            this.type = type;
            this.value = value;
        }
    }

    record Input(int key, boolean down, long us, long order) {
        long frameUs() { return Math.max(0, (us + FRAME_US - 1) / FRAME_US * FRAME_US); }
    }

    final ChartObject[] objects;
    final int notes, maxScore;
    final long originDelayUs;
    final int[] counts = new int[12];
    private final double[] lengths;
    private final double factor;
    private final PriorityQueue<Input> pending = new PriorityQueue<>(Comparator.comparingLong(Input::frameUs)
            .thenComparingLong(Input::order));
    private long inputOrder;
    private final boolean[] keys = new boolean[9];
    private final int[] held = new int[8], through = new int[8];
    private int current, check, startMs, stopMs;
    private double bpm, base, baseLength, line;
    long nextFrameUs, processedUs;
    int score, gauge = 256, combo, maxCombo, passed;
    boolean failed;

    AngolmoisEngine(ChartObject[] source, double initialBpm, double[] measureLengths, int rank) {
        objects = Arrays.stream(source).map(o -> new ChartObject(o.position, o.lane, o.type, o.value))
                .sorted(Comparator.comparingDouble((ChartObject o) -> o.position).thenComparingInt(o -> o.type))
                .toArray(ChartObject[]::new);
        lengths = measureLengths.clone();
        bpm = initialBpm;
        factor = 1.5 - Math.min(rank, 5) * .25;
        notes = (int) Arrays.stream(objects).filter(o -> o.lane < BGM && (o.type == HEAD || o.type == NOTE)).count();
        int maximum = 0;
        for (int i = 0; i < notes; i++) maximum += (int) (300 * (1 + 1.0 * i / notes));
        maxScore = maximum;
        // C inserts a leading measure only when a scoring note occurs before measure 1.
        base = Arrays.stream(objects).anyMatch(o -> o.lane < BGM && (o.type == HEAD || o.type == NOTE)
                && o.position < 1) ? -1 : 0;
        baseLength = length(base);
        originDelayUs = base < 0 ? (long) (240_000_000 / initialBpm) : 0;
    }

    private double length(double position) {
        return position < 0 || position >= lengths.length ? 1 : lengths[(int) position];
    }

    void offer(int key, boolean down, long us) {
        if (key < 0 || key >= keys.length) return;
        var input = new Input(key, down, us, inputOrder++);
        if (input.frameUs() < nextFrameUs) throw new IllegalArgumentException("Late Angolmois input");
        pending.add(input);
    }

    void advance(long nowUs) {
        // The caller supplies a complete input prefix strictly before nowUs.
        // Frame N remains open until nowUs > N so equal-time events in another batch are included.
        while (!failed && nextFrameUs < nowUs) {
            frame((int) (nextFrameUs / FRAME_US));
            processedUs = nextFrameUs;
            nextFrameUs += FRAME_US;
        }
    }

    private double milliseconds(double position) {
        return position * 240_000 / bpm;
    }

    private void frame(int now) {
        double bottom;
        if (now < stopMs) {
            bottom = base;
        } else {
            if (stopMs != 0) {
                startMs = stopMs;
                stopMs = 0;
            }
            bottom = base + (now - startMs) * bpm / 240_000 / baseLength;
        }
        int measure = bottom < 0 ? -1 - (int) -bottom : (int) bottom;
        if (baseLength != length(measure)) {
            startMs += (int) (milliseconds(measure - base) * baseLength);
            base = measure;
            baseLength = length(measure);
        }
        line = bottom;
        int previous = current;
        for (; current < objects.length && objects[current].position < line; current++) {
            var o = objects[current];
            if (o.lane == BPM) {
                startMs += (int) (milliseconds(o.position - base) * baseLength);
                base = o.position;
                bpm = o.value;
            } else if (o.lane == STOP) {
                if (now >= stopMs) stopMs = now;
                stopMs += o.type == 1 ? (int) o.value : (int) milliseconds(o.value);
                base = o.position;
            }
        }
        for (; check < current; check++) {
            var o = objects[check];
            double gap = milliseconds(line - o.position) * length(o.position) * factor;
            if (gap < 144) break;
            if (o.lane >= BGM || o.consumed || o.type >= HIDDEN
                    || (o.type == END && through[o.lane] == 0)) continue;
            grade(0, 0, 0, false);
            if (o.type != END) passed++;
            through[o.lane] = 0;
        }
        while (!pending.isEmpty() && pending.peek().frameUs() <= nextFrameUs) input(pending.remove());
        for (int i = previous; i < current; i++) {
            var o = objects[i];
            if (o.lane >= BGM || o.type != MINE || held[o.lane] == 0) continue;
            through[o.lane] = 0;
            if (o.value < 0) {
                gauge = 0;
                failed = true;
                return;
            }
            grade(0, 0, (int) o.value, false);
        }
        gauge = Math.min(512, gauge);
    }

    private void input(Input input) {
        int key = input.key, lane = Math.min(key, 7);
        if (input.down == keys[key]) return;
        keys[key] = input.down;
        if (!input.down) {
            if (--held[lane] == 0 && through[lane] != 0) {
                int end = through[lane];
                while (end < objects.length && !(objects[end].lane == lane && objects[end].type == END)) end++;
                through[lane] = 0;
                if (end == objects.length) throw new IllegalStateException("Unpaired Angolmois LN");
                double gap = milliseconds(objects[end].position - line) * length(line) * factor;
                if (-144 < gap && gap < 144) objects[end].consumed = true;
                else grade(0, 0, 0, gap >= 0);
            }
            return;
        }
        // SDL's keyboard mappings count multiple physical keys per lane. Both scratch keys share a lane.
        if (held[lane]++ != 0 || objects.length == 0) return;
        int future = current;
        while (future < objects.length && !(objects[future].lane == lane && objects[future].type < HIDDEN)) future++;
        int past = current - 1;
        while (past >= 0 && !(objects[past].lane == lane && objects[past].type < HIDDEN)) past--;
        double distance = past >= 0 ? line - objects[past].position : Double.MAX_VALUE;
        if (future < objects.length && distance > objects[future].position - line) past = future;
        if (past < check || past < 0 || objects[past].consumed || objects[past].type == END) return;
        var o = objects[past];
        double signed = milliseconds(o.position - line) * length(line) * factor;
        double gap = Math.abs(signed);
        if (gap >= 144) return;
        if (o.type == HEAD) through[lane] = past + 1;
        o.consumed = true;
        passed++;
        grade((gap < 14.4 ? 1 : 0) + (gap < 48 ? 1 : 0) + (gap < 84 ? 1 : 0) + 1,
                (int) ((1 - gap / 144) * 300), 0, signed >= 0);
    }

    private void grade(int grade, int delta, int damage, boolean early) {
        counts[(4 - grade) * 2 + (early ? 0 : 1)]++;
        score += delta + delta * combo / notes;
        if (grade == 0) {
            combo = 0;
            gauge -= damage != 0 ? damage : 30;
        } else if (grade == 1) {
            combo = 0;
            gauge -= 15;
        } else if (grade >= 3) {
            combo++;
            gauge += (grade < 4 ? 2 : 3) + Math.min(combo, 100) / 50;
        }
        maxCombo = Math.max(maxCombo, combo);
    }

    boolean settled() {
        return passed == notes && Arrays.stream(through).allMatch(i -> i == 0);
    }
}
