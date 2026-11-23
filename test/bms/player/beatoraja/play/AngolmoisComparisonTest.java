package bms.player.beatoraja.play;

import bms.model.BMSDecoder;
import bms.model.BMSModelUtils;
import bms.model.Mode;
import bms.player.beatoraja.input.JudgeEventQueue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import static bms.player.beatoraja.play.AngolmoisEngine.*;

/** Differential execution of the pinned extracted C playback loop and the production Java engine. */
public final class AngolmoisComparisonTest {
    private record Edge(long us, int key, boolean down) {}
    private static Path reference;
    private static int scenarios, checkedFrames;

    public static void main(String[] args) throws Exception {
        var work = Files.createTempDirectory("angolmois-comparison-");
        reference = work.resolve(System.getProperty("os.name").startsWith("Windows") ? "reference.exe" : "reference");
        var compile = new ProcessBuilder(System.getenv().getOrDefault("CC", "cc"), "-std=c99", "-O0",
                "test/resources/play/angolmois/reference.c", "-o", reference.toString()).inheritIO().start();
        check(compile.waitFor() == 0, "Compile the extracted C oracle");
        boundaries();
        longNotes();
        clocks();
        gauges();
        mixed();
        callbackCadence();
        integration(work);
        System.out.printf("AngolmoisComparisonTest passed (%d scenarios, %d C/Java frame snapshots)%n", scenarios, checkedFrames);
    }

    private static ChartObject note(double position, int lane) { return new ChartObject(position, lane, NOTE, 0); }
    private static Edge down(long us) { return new Edge(us, 0, true); }
    private static Edge up(long us) { return new Edge(us, 0, false); }

    private static void boundaries() throws Exception {
        for (int rank : new int[]{-1, 0, 1, 2, 3, 4, 5, 6}) {
            for (double boundary : new double[]{14.4, 48, 84, 144}) {
                for (int side : new int[]{-1, 1}) for (int extra : new int[]{-1, 0, 1}) {
                    long at = 2_000_000 + side * ((long) (boundary / (1.5 - Math.min(rank, 5) * .25) * 1000) + extra);
                    compare("rank=" + rank + " edge=" + at, List.of(note(1, 0)), rank, 120, new double[0],
                            List.of(down(at)), 3000);
                }
            }
        }
        compare("early input does not consume", List.of(note(1, 0)), 2, 120, new double[0],
                List.of(down(1_800_000), up(1_801_000), down(2_000_000)), 2300);
        compare("dense processed blocker and midpoint", List.of(note(1, 0), note(1.02, 0)), 2, 120, new double[0],
                List.of(down(2_000_000), up(2_001_000), down(2_010_000), up(2_011_000), down(2_020_000)), 2300);
        compare("scratch two keys hold one lane", List.of(note(1, 7), note(1.025, 7)), 2, 120, new double[0],
                List.of(new Edge(2_000_000, 7, true), new Edge(2_001_000, 8, true),
                        new Edge(2_002_000, 7, false), new Edge(2_050_000, 8, false)), 2300);
    }

    private static void longNotes() throws Exception {
        var notes = List.of(new ChartObject(1, 0, HEAD, 0), new ChartObject(1.5, 0, END, 0), note(1.6, 0));
        for (long release : new long[]{2_500_000, 2_855_999, 2_856_000, 2_999_999, 3_000_000, 3_143_999, 3_144_000, 3_145_000}) {
            compare("LN release " + release, notes, 2, 120, new double[0], List.of(down(2_000_000), up(release)), 3500);
        }
        compare("LN head missed", notes, 2, 120, new double[0], List.of(), 3500);
        compare("LN held past tail", notes, 2, 120, new double[0], List.of(down(2_000_000)), 3500);
        compare("LN tail blocks following note", notes, 2, 120, new double[0],
                List.of(down(2_000_000), up(3_000_000), down(3_050_000)), 3500);
    }

    private static void clocks() throws Exception {
        compare("BPM changes", List.of(note(.99, 0), new ChartObject(1, BPM, 1, 240), note(1, 1), note(1.05, 2),
                new ChartObject(1.1, BPM, 1, 90), note(1.12, 0)), 2, 120, new double[0],
                List.of(new Edge(4_001_000, 1, true), new Edge(4_050_000, 2, true)), 5000);
        double[] lengths = {1, .5, .1, 2, 1};
        compare("measure changes / miss cursor", List.of(note(.99, 0), note(1, 1), note(1.05, 0), note(2.5, 2)),
                2, 120, lengths, List.of(new Edge(4_001_000, 1, true), down(4_050_000), new Edge(5_100_000, 2, true)), 6500);
        compare("STOP / LN release / BPM", List.of(new ChartObject(1, 0, HEAD, 0), new ChartObject(1.5, STOP, 0, .5),
                new ChartObject(1.5, 0, END, 0), new ChartObject(1.5, BPM, 1, 240), note(1.6, 1)),
                2, 120, new double[0], List.of(down(2_000_000), up(3_500_000), new Edge(4_101_000, 1, true)), 5000);
        compare("fractional STOP truncation", List.of(new ChartObject(1, STOP, 0, 1.0 / 192), note(1, 0), note(1.1, 1)),
                2, 137, new double[0], List.of(down(1_760_001)), 2500);
        compare("FIFO score bonus within frame", List.of(note(1, 0), note(1.005, 1)), 2, 120, new double[0],
                List.of(new Edge(2_019_999, 1, true), new Edge(2_019_001, 0, true)), 2300);
    }

    private static void gauges() throws Exception {
        var notes = new ArrayList<ChartObject>();
        var inputs = new ArrayList<Edge>();
        for (int i = 0; i < 180; i++) {
            notes.add(note(1 + i / 16.0, 0));
            inputs.add(down(2_000_000 + i * 125_000L));
            inputs.add(up(2_001_000 + i * 125_000L));
        }
        compare("combo 50/100 and upper clamp", notes, 2, 120, new double[0], inputs, 25000);
        var debt = new ArrayList<ChartObject>();
        for (int i = 0; i < 12; i++) debt.add(note(1 + i / 8.0, 0));
        for (int i = 0; i < 120; i++) debt.add(note(3 + i / 16.0, 0));
        inputs.clear();
        for (int i = 0; i < 120; i++) {
            inputs.add(down(6_000_000 + i * 125_000L)); inputs.add(up(6_001_000 + i * 125_000L));
        }
        compare("negative gauge and recovery", debt, 2, 120, new double[0], inputs, 21500);
        for (int damage : new int[]{30, 512, -1}) {
            compare("mine " + damage, List.of(note(1, 0), new ChartObject(1.1, 0, MINE, damage), note(2, 1)),
                    2, 120, new double[0], List.of(down(2_000_000)), 4500);
            compare("mine release on crossing frame " + damage,
                    List.of(note(1, 0), new ChartObject(1.1, 0, MINE, damage), note(2, 1)),
                    2, 120, new double[0], List.of(down(2_000_000), up(2_200_001)), 4500);
        }
        for (int damage : new int[]{108, 109, 110}) {
            var objects = List.of(note(1, 0), new ChartObject(1.1, 0, MINE, damage));
            compare("clear border damage " + damage, objects, 2, 120, new double[0], List.of(down(2_000_000)), 2500);
            var engine = new AngolmoisEngine(objects.toArray(ChartObject[]::new), 120, new double[]{1, 1}, 2);
            var run = new AngolmoisSecondary(engine, 2_500_000, 0, new LaneProperty(Mode.BEAT_7K).getKeyLaneAssign());
            var batch = new JudgeEventQueue.Batch();
            batch.size = 1; batch.keys[0] = 0; batch.times[0] = 2_000_000; batch.pressed[0] = true;
            run.advance(2_500_001, batch, false);
            check((run.finish(true).clear() == bms.player.beatoraja.ClearType.Normal) == (damage <= 109), "Clear threshold at raw150");
        }
    }

    private static void mixed() throws Exception {
        for (int seed = 0; seed < 12; seed++) {
            var random = new Random(seed);
            var notes = new ArrayList<ChartObject>();
            var inputs = new ArrayList<Edge>();
            for (int i = 0; i < 80; i++) {
                int lane = random.nextInt(8);
                double position = 1 + i / 32.0;
                notes.add(note(position, lane));
                long at = 2_000_000 + i * 62_500L + random.nextInt(400_000) - 200_000;
                inputs.add(new Edge(at, lane, true)); inputs.add(new Edge(at + 1_000, lane, false));
            }
            inputs.sort(Comparator.comparingLong(Edge::us));
            compare("mixed " + seed, notes, seed % 4, 120, new double[0], inputs, 7500);
        }
    }

    private static void callbackCadence() {
        var objects = new ChartObject[]{note(1, 0), note(1.125, 1), new ChartObject(1.2, STOP, 0, .125), note(1.5, 2)};
        var edges = List.of(new Edge(2_000_000, 0, true), new Edge(2_000_001, 0, false),
                new Edge(2_249_001, 1, true), new Edge(2_249_999, 1, false), new Edge(3_200_000, 2, true));
        var expected = new AngolmoisEngine(objects, 120, new double[]{1, 1}, 2);
        for (var edge : edges) expected.offer(edge.key, edge.down, edge.us);
        expected.advance(4_000_001);
        for (long callback : new long[]{1, 999, 1000, 17003, 500000}) {
            var engine = new AngolmoisEngine(objects, 120, new double[]{1, 1}, 2);
            int cursor = 0;
            for (long now = callback; now < 4_000_001; now += callback) {
                while (cursor < edges.size() && edges.get(cursor).us <= now) {
                    var edge = edges.get(cursor++);
                    engine.offer(edge.key, edge.down, edge.us);
                }
                engine.advance(now);
            }
            while (cursor < edges.size()) {
                var edge = edges.get(cursor++);
                engine.offer(edge.key, edge.down, edge.us);
            }
            engine.advance(4_000_001);
            check(engine.score == expected.score && engine.gauge == expected.gauge && Arrays.equals(engine.counts, expected.counts),
                    "Callback cadence does not change the 1 ms virtual frames: " + callback);
        }
    }

    private static void compare(String label, List<ChartObject> notes, int rank, double bpm, double[] lengths,
                                List<Edge> inputs, int frames) throws Exception {
        var objects = notes.toArray(ChartObject[]::new);
        double[] rates = new double[Math.max(1000, lengths.length)];
        Arrays.fill(rates, 1);
        System.arraycopy(lengths, 0, rates, 0, lengths.length);
        var engine = new AngolmoisEngine(objects, bpm, rates, rank);
        compare(label, objects, rank, bpm, lengths, inputs, frames, engine);
    }

    private static void compare(String label, ChartObject[] objects, int rank, double bpm, double[] lengths,
                                List<Edge> inputs, int frames, AngolmoisEngine engine) throws Exception {
        // C consumes each frame's events in arrival order, including distinct timestamps in one frame.
        var ordered = new ArrayList<>(inputs);
        ordered.sort(Comparator.comparingLong(e -> (e.us + FRAME_US - 1) / FRAME_US));
        for (var edge : ordered) engine.offer(edge.key, edge.down, edge.us);
        var process = new ProcessBuilder(reference.toString()).start();
        try (var writer = process.outputWriter(StandardCharsets.UTF_8)) {
            writer.write(String.format(Locale.ROOT, "%d %.17g %d %d %d%n%d%n", objects.length, bpm, rank, frames, ordered.size(), lengths.length));
            for (int i = 0; i < lengths.length; i++) writer.write(i + " " + lengths[i] + "\n");
            for (var o : objects) writer.write(String.format(Locale.ROOT, "%.17g %d %d %.17g%n", o.position, o.lane, o.type, o.value));
            for (var edge : ordered) writer.write(edge.us + " " + edge.key + " " + (edge.down ? 1 : 0) + "\n");
        }
        try (var reader = process.inputReader(StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                int[] c = Arrays.stream(line.split(" ")).mapToInt(Integer::parseInt).toArray();
                engine.advance(c[0] * FRAME_US + 1);
                int[] java = {c[0], engine.score, engine.gauge, engine.combo, engine.maxCombo,
                        engine.counts[8] + engine.counts[9], engine.counts[6] + engine.counts[7],
                        engine.counts[4] + engine.counts[5], engine.counts[2] + engine.counts[3],
                        engine.counts[0] + engine.counts[1], engine.failed ? 1 : 0, engine.maxScore};
                if (!Arrays.equals(c, java)) throw new AssertionError(label + " frame " + c[0]
                        + " C=" + Arrays.toString(c) + " Java=" + Arrays.toString(java));
                checkedFrames++;
            }
            check(process.waitFor() == 0, label + " C oracle exit");
        } finally { if (process.isAlive()) process.destroyForcibly(); }
        scenarios++;
    }

    private static void integration(Path work) throws Exception {
        Path chart = work.resolve("tap.bme");
        Files.writeString(chart, "#PLAYER 1\n#BPM 120\n#RANK 2\n#TOTAL 400\n#WAV01 none.wav\n#00119:01\n#00202:0.5\n#00219:01\n#00319:01\n");
        var model = new BMSDecoder().decode(chart);
        var f = new KeyboardJudgeCompatibilityTest.Fixture(model, true, BMSPlayerRule.Beatoraja_7,
                BMSPlayerRule.ChartParameters.fromBms(model), AbstractGrooveGauge.NORMAL);
        f.playerConfig.setSecondaryScoringRuleIds(new String[]{"angolmois", "lr2"});
        var group = SecondaryJudgingGroup.create(f.player, model, f.resource, 7_000_000);
        check(group != null, "Normal entry point creates Angolmois");
        var batch = new JudgeEventQueue.Batch();
        batch.size = 2; batch.keys[0] = batch.keys[1] = 6;
        batch.pressed[0] = true; batch.pressed[1] = false;
        batch.times[0] = 2_000_000; batch.times[1] = 2_001_000;
        group.advance(7_000_001, batch, false);
        var results = group.finish(true);
        var result = results.stream().filter(r -> r.rule() == SecondaryScoringRule.ANGOLMOIS).findFirst().orElseThrow().judgment();
        check(result.nativeScore() == 300 && result.nativeMaxScore() == 1199 && result.passed() == 3,
                "Native score and decoded measure lengths: " + result);
        check(model.getAllTimeLines()[1].getNote(6).getState() == 0, "Secondary does not consume primary notes");
        check(results.size() == 2, "LR2 still runs alongside Angolmois");
        check(result.toLocalAttempt(model).toScoreData(Mode.BEAT_7K).getNativeScore() == 300, "Native result propagation");

        // Primary's start margin shifts both time and cumulative sections. Reverse that shift in the adapter.
        Files.writeString(chart, "#PLAYER 1\n#BPM 120\n#WAV01 none.wav\n#00019:01\n#00119:01\n");
        model = new BMSDecoder().decode(chart);
        long margin = BMSModelUtils.setStartNoteTime(model, 1000);
        var engine = AngolmoisChart.create(model);
        var run = new AngolmoisSecondary(engine, 5_000_000, engine.originDelayUs - margin * 1000,
                new LaneProperty(Mode.BEAT_7K).getKeyLaneAssign());
        batch.size = 1; batch.keys[0] = 6; batch.times[0] = 1_000_000; batch.pressed[0] = true;
        run.advance(5_000_001, batch, false);
        check(run.finish(true).nativeScore() == 300, "Early chart and primary margin align with C's leading measure");

        Files.writeString(chart, "#PLAYER 1\n#BPM 120\n#WAV01 none.wav\n#00119:01\n#002D9:02\n");
        model = new BMSDecoder().decode(chart);
        engine = AngolmoisChart.create(model);
        engine.offer(6, true, 2_000_000);
        engine.advance(4_500_001);
        check(engine.gauge == 254, "Base-36 mine damage uses C's half-percent unit and integer truncation");

        Files.writeString(chart, "#PLAYER 1\n#BPM 120\n#RANK 2\n#TOTAL 400\n#WAV01 none.wav\n"
                + "#BPM01 240\n#STOP01 48\n#00102:0.5\n#00119:01\n#00108:0001\n#00219:01\n#00209:01\n#00359:0101\n");
        model = new BMSDecoder().decode(chart);
        engine = AngolmoisChart.create(model);
        var literal = new ChartObject[]{new ChartObject(0, BGA, 3, 0), note(1, 6), new ChartObject(1.5, BPM, 1, 240),
                note(2, 6), new ChartObject(2, STOP, 0, .25), new ChartObject(3, 6, HEAD, 0), new ChartObject(3.5, 6, END, 0)};
        compare("decoded BPM/measure/STOP/LN", literal, 2, 120, new double[]{1, .5, 1, 1},
                List.of(new Edge(2_000_000, 6, true), new Edge(2_001_000, 6, false), new Edge(2_750_000, 6, true),
                        new Edge(2_751_000, 6, false), new Edge(4_000_000, 6, true), new Edge(4_500_000, 6, false)), 5500, engine);
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
