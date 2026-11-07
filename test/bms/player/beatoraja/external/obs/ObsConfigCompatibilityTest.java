package bms.player.beatoraja.external.obs;

import bms.player.beatoraja.Config;
import com.badlogic.gdx.utils.JsonReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/** Exercises the actual system config reader and writer in an isolated working directory. */
public final class ObsConfigCompatibilityTest {
    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            checkRoundTrip();
            return;
        }

        Path directory = Files.createTempDirectory("beatoraja-obs-config-");
        try {
            Files.writeString(directory.resolve("config.json"), """
                    {
                      "useObsWs": true,
                      "obsWsHost": "127.0.0.1",
                      "obsWsPort": 4460,
                      "obsWsPass": "fixture-password",
                      "obsWsRecStopWait": 1500,
                      "obsWsRecMode": 2,
                      "obsCommands": [
                        {"timing": "PLAY_START", "action": "SetScene", "targetScene": "",
                         "targetSource": "", "transitionScene": "Gameplay"},
                        {"timing": "PLAY_END", "action": "HideSource", "targetScene": "Gameplay",
                         "targetSource": "Overlay", "transitionScene": ""}
                      ]
                    }
                    """);
            String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            Process child = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                    ObsConfigCompatibilityTest.class.getName(), "round-trip")
                    .directory(directory.toFile()).inheritIO().start();
            if (!child.waitFor(30, TimeUnit.SECONDS)) {
                child.destroyForcibly().waitFor();
                throw new AssertionError("OBS config round trip timed out");
            }
            check(child.exitValue() == 0, "OBS config round trip failed: " + child.exitValue());
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("ObsConfigCompatibilityTest passed (legacy config.json and config_sys.json round trip)");
    }

    private static void checkRoundTrip() throws Exception {
        Config config = Config.read();
        checkSettings(config);
        Config.write(config);
        var saved = new JsonReader().parse(Files.readString(Path.of("config_sys.json")));
        for (var command : saved.get("obsCommands"))
            check(!command.has("class"), "OBS command must not depend on a Java package name");
        Files.delete(Path.of("config.json"));
        checkSettings(Config.read());
    }

    private static void checkSettings(Config config) {
        check(config.isUseObsWs() && config.getObsWsHost().equals("127.0.0.1") && config.getObsWsPort() == 4460
                && config.getObsWsPass().equals("fixture-password") && config.getObsWsRecStopWait() == 1500
                && config.getObsWsRecMode() == 2, "Existing OBS connection and recording settings must survive");
        var commands = config.getObsCommands();
        check(commands.size() == 2, "Both existing OBS commands must survive validation");
        var scene = commands.get(0);
        check(scene.getTiming().equals("PLAY_START") && scene.getAction().equals("SetScene")
                && scene.getTransitionScene().equals("Gameplay"), "Existing scene command");
        var source = commands.get(1);
        check(source.getTiming().equals("PLAY_END") && source.getAction().equals("HideSource")
                && source.getTargetScene().equals("Gameplay") && source.getTargetSource().equals("Overlay"),
                "Existing source command");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
