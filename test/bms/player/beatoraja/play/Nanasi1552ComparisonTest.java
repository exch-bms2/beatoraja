package bms.player.beatoraja.play;

import bms.model.BMSDecoder;
import bms.model.Mode;
import bms.model.NormalNote;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;

/** Offline fixture checks and an optional comparison against recorded Nanasi 1.552 runs. */
public final class Nanasi1552ComparisonTest {
    private static final String ROOT = "/play/nanasi-1552/";
    private static final String GAUGE = "gauge-400.bms";
    private static final int CHANNEL = 19;
    private static final int LANE = 6;
    private static final long PULSE_US = 40_000;
    private static final String OBS_COLUMNS =
            "case\ttrial\tjudgments\tselected_note\tgauge_before_pct\tgauge_after_pct\tresult";

    // hitkey's EXTRA_OFF / GAMELEVEL_NORMAL measurement at 400 notes and TOTAL 400.
    // https://bms.ms/~hitkey/option.htm
    private enum Grade {
        COOL(2, 0L), GREAT(2, 25_000L), GOOD(1, 60_000L), BAD(-6, 110_000L), POOR(-10, null);
        final int halfPoints;
        final Long nominalOffsetUs;

        Grade(int halfPoints, Long nominalOffsetUs) {
            this.halfPoints = halfPoints;
            this.nominalOffsetUs = nominalOffsetUs;
        }
    }

    private record Chart(String name, byte[] bytes, long clockUs, List<Long> notes) {}
    private record Check(int firstNote, int before, int after, String result, List<Grade> grades) {}
    private record Scenario(String id, Chart chart, List<Long> presses, Check check) {}

    public static void main(String[] args) throws Exception {
        var cases = scenarios();
        check(cases.size() == 69, "Unexpected change in the number of experiment cases");
        validateInputs(cases);
        validateObservationComparison(cases);
        if (args.length == 2 && args[0].equals("--export")) {
            export(cases, Path.of(args[1]));
        } else if (args.length == 2 && args[0].equals("--observations")) {
            int compared = compareObservations(cases, Files.readAllLines(Path.of(args[1]), StandardCharsets.UTF_8));
            System.out.println(compared + " recorded observations compared with the published gauge reference");
        } else if (args.length != 0) {
            throw new IllegalArgumentException("Use --export DIR or --observations FILE");
        }
        System.out.println("Nanasi1552ComparisonTest passed (" + cases.size()
                + " input cases; unknown timing and selection results are not assumed)");
    }

    private static List<Scenario> scenarios() throws IOException {
        var cases = new ArrayList<Scenario>();
        int[] offsetsMs = {-400, -300, -250, -200, -150, -125, -100, -80, -60, -40, -30, -20, -10,
                0, 10, 20, 30, 40, 60, 80, 100, 125, 150, 200, 250, 300, 400};
        for (int bpm : new int[]{120, 240}) {
            var chart = load("tap-" + bpm + ".bms", bpm, 2_000_000, 1);
            check(chart.notes().equals(List.of(4_000_000L)), "BPM control must keep the same absolute note time");
            for (int offset : offsetsMs) {
                cases.add(new Scenario("tap-" + bpm + "-" + offset, chart,
                        List.of(4_000_000 + offset * 1_000L), null));
            }
            cases.add(new Scenario("tap-" + bpm + "-idle", chart, List.of(), null));
        }
        var dense = load("dense.bms", 120, 2_000_000, 2);
        check(dense.notes().equals(List.of(4_000_000L, 4_062_500L)), "Dense fixture must use 62.5 ms spacing");
        for (long offset : new long[]{-20_000, 15_000, 31_250, 50_000, 75_000}) {
            cases.add(new Scenario("dense-" + offset, dense, List.of(4_000_000 + offset), null));
        }

        var gauge = load(GAUGE, 120, 1_000_000, 400);
        var notes = gauge.notes();
        check(notes.get(0) == 2_000_000 && notes.get(1) - notes.get(0) == 1_000_000
                        && notes.get(399) - notes.get(392) == 7_000_000,
                "The first and final gauge judgment probes must be isolated by one second");
        for (var grade : Grade.values()) {
            var presses = grade.nominalOffsetUs == null ? List.<Long>of() : List.of(notes.get(0) + grade.nominalOffsetUs);
            int after = switch (grade) { case COOL, GREAT -> 52; case GOOD -> 51; case BAD -> 44; case POOR -> 40; };
            cases.add(new Scenario("gauge-" + grade.name().toLowerCase(Locale.ROOT), gauge, presses,
                    new Check(1, 50, after, "PLAYING", List.of(grade))));
        }
        addBoundary(cases, gauge, "below", 159, "FAILED", Grade.POOR, Grade.POOR, Grade.POOR, Grade.BAD, Grade.BAD, Grade.GOOD);
        addBoundary(cases, gauge, "exact", 160, "CLEAR", Grade.POOR, Grade.POOR, Grade.POOR, Grade.POOR);
        addBoundary(cases, gauge, "above", 161, "CLEAR", Grade.POOR, Grade.POOR, Grade.POOR, Grade.POOR, Grade.GOOD);
        return List.copyOf(cases);
    }

    private static void addBoundary(List<Scenario> cases, Chart chart, String name, int after, String result, Grade... suffix) {
        int prefix = chart.notes().size() - suffix.length;
        var presses = new ArrayList<>(chart.notes().subList(0, prefix));
        for (int i = 0; i < suffix.length; i++) {
            if (suffix[i].nominalOffsetUs != null) presses.add(chart.notes().get(prefix + i) + suffix[i].nominalOffsetUs);
        }
        cases.add(new Scenario("clear-" + name, chart, List.copyOf(presses),
                new Check(prefix + 1, 200, after, result, List.of(suffix))));
    }

    private static Chart load(String name, int bpm, long clock, int count) throws IOException {
        byte[] bytes;
        try (var input = Objects.requireNonNull(Nanasi1552ComparisonTest.class.getResourceAsStream(ROOT + name), name)) {
            bytes = input.readAllBytes();
        }
        var headers = new String(bytes, StandardCharsets.US_ASCII).lines().toList();
        check(headers.containsAll(List.of("#OPTION 774:EXTRA_OFF", "#OPTION 774:GAMELEVEL_NORMAL",
                "#RANK 2", "#TOTAL 400", "#BPM " + bpm)), name + ": reference options");
        var model = new BMSDecoder().decode(bytes, name, false, new int[0]);
        check(model != null && model.getMode() == Mode.BEAT_7K && model.getTotalNotes() == count
                && model.getTotal() == 400 && model.getJudgeType().bmsRank() == 2, name + ": chart metadata");
        var times = new ArrayList<Long>();
        var sounds = new ArrayList<Integer>();
        var clocks = new ArrayList<Long>();
        for (var line : model.getAllTimeLines()) {
            if (line.getBackGroundNotes().length != 0) clocks.add(line.getMicroTime());
            if (line.getNote(LANE) instanceof NormalNote note) {
                times.add(note.getMicroTime());
                sounds.add(note.getWav());
            }
        }
        check(clocks.equals(List.of(clock)) && times.size() == count, name + ": expected chart clock and lane " + CHANNEL);
        if (name.equals("dense.bms")) check(!sounds.get(0).equals(sounds.get(1)), "Dense notes need distinct sounds");
        return new Chart(name, bytes, clock, List.copyOf(times));
    }

    private static void validateInputs(List<Scenario> cases) {
        var ids = new HashMap<String, Scenario>();
        for (var scenario : cases) {
            check(ids.put(scenario.id(), scenario) == null, "Duplicate case " + scenario.id());
            long previousRelease = scenario.chart().clockUs();
            for (long press : scenario.presses()) {
                check(press > previousRelease, "Overlapping input or input before clock: " + scenario.id());
                previousRelease = press + PULSE_US;
            }
            var c = scenario.check();
            if (c == null) continue;
            int calculated = c.before() + c.grades().stream().mapToInt(g -> g.halfPoints).sum();
            check(calculated == c.after(), scenario.id() + ": published gauge arithmetic");
            check(c.result().equals("PLAYING") || c.result().equals(calculated >= 160 ? "CLEAR" : "FAILED"),
                    scenario.id() + ": published clear border");
        }
    }

    private static void validateObservationComparison(List<Scenario> cases) {
        // Synthetic observations exercise the importer, not the closed-source player.
        var good = List.of(OBS_COLUMNS, "gauge-good\t1\tGOOD\t-\t25\t25.5\tPLAYING",
                "clear-exact\t1\tPOOR POOR POOR POOR\t-\t100\t80\tCLEAR");
        check(compareObservations(cases, good) == 2, "Both synthetic observations must match");
        try {
            compareObservations(cases, List.of(OBS_COLUMNS, "clear-exact\t1\tPOOR POOR POOR POOR\t-\t100\t80\tFAILED"));
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("does not match"), "Unexpected rejection reason");
            return;
        }
        throw new AssertionError("A different observed clear outcome must fail the comparison");
    }

    private static int compareObservations(List<Scenario> cases, List<String> rows) {
        if (rows.isEmpty() || !rows.get(0).replace("\uFEFF", "").equals(OBS_COLUMNS)) {
            throw new IllegalArgumentException("Unexpected observations TSV header");
        }
        var byId = new HashMap<String, Scenario>();
        for (var scenario : cases) byId.put(scenario.id(), scenario);
        int compared = 0;
        for (int i = 1; i < rows.size(); i++) {
            if (rows.get(i).isBlank()) continue;
            var row = rows.get(i).split("\t", -1);
            if (row.length != 7 || !byId.containsKey(row[0]) || row[1].isBlank()) {
                throw new IllegalArgumentException("Invalid observation on row " + (i + 1));
            }
            var c = byId.get(row[0]).check();
            if (c == null) {
                System.out.printf("Observed only: %s trial %s, judgments=%s selected=%s%n", row[0], row[1], row[2], row[3]);
                continue;
            }
            if (!row[2].equals(grades(c)) || Double.parseDouble(row[4]) != c.before() / 2.0) {
                throw new IllegalArgumentException(row[0] + ": retry with the expected judgments and pre-probe gauge");
            }
            if (Double.parseDouble(row[5]) != c.after() / 2.0 || !row[6].equals(c.result())) {
                throw new IllegalArgumentException(row[0] + ": observation does not match the published reference"
                        + " (expected " + c.after() / 2.0 + "%, " + c.result() + ")");
            }
            compared++;
        }
        return compared;
    }

    private static String grades(Check c) {
        return String.join(" ", c.grades().stream().map(Grade::name).toList());
    }

    private static void export(List<Scenario> cases, Path directory) throws IOException {
        Files.createDirectories(directory);
        var charts = new TreeMap<String, Chart>();
        var inputs = new ArrayList<String>();
        inputs.add("case\tat_us\tchannel\tedge");
        var manifest = new ArrayList<String>();
        manifest.add("case\tchart\tclock_at_us\tprobe_note_1based\texpected_judgments\tgauge_before_pct\tgauge_after_pct\tresult");
        for (var scenario : cases) {
            var chart = scenario.chart();
            charts.put(chart.name(), chart);
            var c = scenario.check();
            manifest.add(scenario.id() + "\t" + chart.name() + "\t" + chart.clockUs() + "\t" + (c == null
                    ? "-\t-\t-\t-\t-" : c.firstNote() + "\t" + grades(c) + "\t" + c.before() / 2.0
                    + "\t" + c.after() / 2.0 + "\t" + c.result()));
            for (long press : scenario.presses()) {
                inputs.add(scenario.id() + "\t" + press + "\t" + CHANNEL + "\tDOWN");
                inputs.add(scenario.id() + "\t" + (press + PULSE_US) + "\t" + CHANNEL + "\tUP");
            }
        }
        for (var chart : charts.values()) Files.write(directory.resolve(chart.name()), chart.bytes());
        Files.write(directory.resolve("note-a.wav"), tone(440, 22));
        Files.write(directory.resolve("note-b.wav"), tone(880, 22));
        Files.write(directory.resolve("clock.wav"), tone(1_760, 45));
        Files.write(directory.resolve("cases.tsv"), manifest, StandardCharsets.UTF_8);
        Files.write(directory.resolve("inputs.tsv"), inputs, StandardCharsets.UTF_8);
        // Never overwrite measurement data when regenerating fixtures.
        var observations = directory.resolve("observations-template.tsv");
        if (Files.notExists(observations)) Files.writeString(observations, OBS_COLUMNS + "\n");
        System.out.println("Exported Nanasi fixtures: " + directory.toAbsolutePath());
    }

    private static byte[] tone(int frequency, int durationMs) {
        int rate = 44_100;
        int samples = rate * durationMs / 1_000;
        var wav = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN);
        wav.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + samples * 2);
        wav.put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16).putShort((short) 1).putShort((short) 1);
        wav.putInt(rate).putInt(rate * 2).putShort((short) 2).putShort((short) 16);
        wav.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(samples * 2);
        for (int i = 0; i < samples; i++) {
            wav.putShort((short) (8_000 * (samples - i) / samples * Math.sin(2 * Math.PI * frequency * i / rate)));
        }
        return wav.array();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
