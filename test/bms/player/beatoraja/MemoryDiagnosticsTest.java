package bms.player.beatoraja;

import java.nio.file.*;
import java.util.*;

public final class MemoryDiagnosticsTest {
    public static void main(String[] args) throws Exception {
        var folder = Files.createTempDirectory("memory-diagnostics-");
        try {
            var csv = folder.resolve("samples.csv");
            var diagnostics = new MemoryDiagnostics(csv);
            var categories = new LinkedHashMap<String, MemoryDiagnostics.Category>();
            categories.put("pool", new MemoryDiagnostics.Category(2, 100, "quotes \" , newline\nend"));
            var initial = new MemoryDiagnostics.Snapshot(categories, 3, 4);
            categories.clear();
            check(initial.categories().size() == 1, "Snapshot is a defensive copy");
            try { initial.categories().clear(); throw new AssertionError(); } catch (UnsupportedOperationException expected) { }
            diagnostics.record("SELECT", "settled", 0, 10, 20, initial);
            diagnostics.record("PLAY", "settled", 0, 30, 40,
                    new MemoryDiagnostics.Snapshot(Map.of("pool", new MemoryDiagnostics.Category(100, 999, "")), 3, 4));
            diagnostics.record("SELECT", "settled", 1, 50, 60,
                    new MemoryDiagnostics.Snapshot(Map.of("pool", new MemoryDiagnostics.Category(3, 150, "")), 4, 5));
            String text = Files.readString(csv);
            check(text.startsWith("run,sample,context,event,iteration,"), "CSV schema");
            check(text.contains("\"quotes \"\" , newline\nend\""), "RFC4180 quote/newline escaping");
            check(text.contains(",3,150,1,50,\"\""), "Delta uses preceding same state, not PLAY");
            check(MemoryDiagnostics.sample().categories().containsKey("jvm-heap"), "Standalone snapshot needs no controller or GL");
            var broken = new MemoryDiagnostics(folder); // Opening a directory must not crash diagnostics/gameplay.
            broken.record("SELECT", "settled", 1, 0, 0, initial);
            broken.record("SELECT", "settled", 2, 0, 0, initial);
            var protectedFile = folder.resolve("not-a-report.txt"); Files.writeString(protectedFile, "existing content");
            new MemoryDiagnostics(protectedFile).record("SELECT", "settled", 0, 0, 0, initial);
            check(Files.readString(protectedFile).equals("existing content"), "Do not append CSV to unrelated files");
            var main = bms.player.beatoraja.input.KeyboardQueueTestSupport.allocate(MainController.class);
            var config = new Config();
            bms.player.beatoraja.input.KeyboardQueueTestSupport.set(main, MainController.class, "config", config);
            var timingFile = folder.resolve("transitions.csv");
            var timing = new MemoryDiagnostics(timingFile, 0);
            timing.transitionComplete(main, "PLAY", System.nanoTime()); timing.capture(main, "PLAY");
            check(!Files.exists(timingFile), "Disabled diagnostics never create output");
            config.setMemoryDiagnostics(true);
            timing.transitionComplete(main, "PLAY", System.nanoTime());
            long before = Files.size(timingFile);
            timing.frameRendered(main, "RESULT"); check(Files.size(timingFile) == before, "Do not mark an unrelated frame");
            timing.frameRendered(main, "PLAY"); long after = Files.size(timingFile);
            check(after > before, "Matching first frame is captured");
            timing.frameRendered(main, "PLAY"); check(Files.size(timingFile) == after, "First frame captured once per transition");
            check(Files.readString(timingFile).contains("\"settled\""), "Stable state sample captured once");
            System.out.println("MemoryDiagnosticsTest passed (immutable snapshots, CSV, per-state comparison, output failure)");
        } finally {
            try (var files = Files.walk(folder)) {
                for (var file : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        }
    }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
