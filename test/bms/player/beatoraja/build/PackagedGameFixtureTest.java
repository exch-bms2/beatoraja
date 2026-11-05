package bms.player.beatoraja.build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Offline contract/negative fixtures only; not Launcher/GL or real shutdown evidence. */
public final class PackagedGameFixtureTest {
    public static void main(String[] args) throws Exception {
        for (String command : new String[]{"bms.player.beatoraja.ir.IRWorkerMain", "bms.player.beatoraja.ir.IRWorkerMain --metadata",
                "bms.player.beatoraja.ir.IRWorkerMain mocha", "beatoraja-all.jar --display-modes", "beatoraja-all.jar --runtime-smoke"})
            check(!PackagedGameSupport.shouldObserve(command), "Worker/probe must not overwrite game evidence: " + command);
        for (String command : new String[]{"beatoraja-all.jar", "bms.player.beatoraja.DesktopLauncher", "bms.player.beatoraja.MainLoader -p smoke"})
            check(PackagedGameSupport.shouldObserve(command), "Observe actual Launcher/game: " + command);
        Path parent = Files.createTempDirectory("packaged game fixture ");
        try {
            Path root = Files.createDirectory(parent.resolve("runtime"));
            Files.createDirectories(root.resolve("skin/default"));
            Files.copy(Path.of("skin/default/system.png"), root.resolve("skin/default/system.png"));
            PackagedGameSupport.fixture(root);
            var model = new bms.model.BMSDecoder().decode(root.resolve("test-song/smoke.bms"));
            check(model != null && model.getTotalNotes() == 5, "Generated playable chart");
            byte[] wav = Files.readAllBytes(root.resolve("test-song/silence.wav"));
            check(wav.length == 8864 && wav[20] == 1 && wav[22] == 1, "Silent PCM fixture");
            rejects(() -> PackagedGameSupport.fixture(root));
            rejects(() -> PackagedGameSupport.verifyRoot(root.toString()));
            check(PackagedGameIntegrationTest.quoted(parent).startsWith("\""), "JVM paths with spaces quoted");
            rejects(() -> PackagedGameIntegrationTest.quoted(Path.of("invalid\"path")));

            Path zip = parent.resolve("traversal.zip");
            try (var out = new ZipOutputStream(Files.newOutputStream(zip))) {
                out.putNextEntry(new ZipEntry("../outside.txt")); out.write(1); out.closeEntry();
            }
            rejects(() -> PackagedGameSupport.extract(zip, parent.resolve("extract")));
            check(!Files.exists(parent.resolve("outside.txt")), "Traversal never written");

            Path agent = parent.resolve("agent.jar");
            var manifest = new java.util.jar.Manifest();
            manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
            manifest.getMainAttributes().putValue("Premain-Class", "bms.player.beatoraja.build.PackagedGameAgent");
            try (var out = new java.util.jar.JarOutputStream(Files.newOutputStream(agent), manifest)) {
                for (String name : new String[]{"build/PackagedGameAgent", "build/PackagedGameSupport", "MainController"}) {
                    out.putNextEntry(new ZipEntry("bms/player/beatoraja/" + name + ".class")); out.write(0); out.closeEntry();
                }
            }
            rejects(() -> PackagedGameIntegrationTest.verifyAgent(agent));

            Path evidence = root.resolve("test-evidence");
            Files.writeString(evidence.resolve("game.pid"), "2");
            for (String name : new String[]{"launcher.png", "select-before.png", "play.png", "result.png", "select-after.png"})
                Files.write(evidence.resolve(name), new byte[]{(byte)137,80,78,71,13,10,26,10});
            Files.writeString(evidence.resolve("launcher-ready.txt"), "fixture"); Files.writeString(evidence.resolve("game-ready.txt"), "fixture");
            Files.createDirectories(root.resolve("diagnostics"));
            Files.writeString(root.resolve("diagnostics/process-1.log"), "child-wait-complete role=game child=2 exit=0\n");
            Files.writeString(root.resolve("diagnostics/process-2.log"), "game-create-complete\ngame-dispose-complete\n");
            var report = new Properties();
            report.setProperty("inputObservedRunning", "true"); report.setProperty("inputThreadAlive", "false");
            report.setProperty("devicesReleased", "true"); report.setProperty("gameSequenceComplete", "true");
            save(report, evidence); PackagedGameIntegrationTest.validate(root, 1);
            for (String key : new String[]{"inputObservedRunning", "devicesReleased", "gameSequenceComplete"}) {
                report.remove(key); save(report, evidence); rejects(() -> PackagedGameIntegrationTest.validate(root, 1)); report.setProperty(key, "true");
            }
            report.setProperty("inputThreadAlive", "true"); save(report, evidence); rejects(() -> PackagedGameIntegrationTest.validate(root, 1));
            report.setProperty("inputThreadAlive", "false"); save(report, evidence);
            Files.delete(evidence.resolve("play.png"));
            try { PackagedGameIntegrationTest.validate(root, 1); throw new AssertionError("Missing image accepted"); }
            catch (java.nio.file.NoSuchFileException expected) { }
            Files.write(evidence.resolve("play.png"), new byte[]{(byte)137,80,78,71,13,10,26,10});
            Files.writeString(root.resolve("diagnostics/process-1.log"), "child-wait-complete role=game child=2 exit=7\n");
            rejects(() -> PackagedGameIntegrationTest.validate(root, 1));
            System.out.println("PackagedGameFixtureTest passed (isolation/traversal/chart/evidence/leaked-input/nonzero-exit contracts; NO GUI)");
        } finally {
            try (var files = Files.walk(parent)) { for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path); }
        }
    }
    private static void save(Properties report, Path evidence) throws Exception {
        try (var out = Files.newOutputStream(evidence.resolve("shutdown.properties"))) { report.store(out, "synthetic regression"); }
    }
    private static void rejects(Check action) throws Exception {
        try { action.run(); } catch (AssertionError | IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid fixture accepted");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    @FunctionalInterface private interface Check { void run() throws Exception; }
}
