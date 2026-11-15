package bms.player.beatoraja;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/** Uses a fresh JVM for each startup, without touching the user's configuration. */
public final class BmsRootConfigTest {
    private static final int EXISTING_ROOTS = 150;
    private static final String ADDED_ROOT = "library/new-folder";

    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            verifyStartup(args[0].equals("save"));
            return;
        }

        Path directory = Files.createTempDirectory("beatoraja-bms-root-config-");
        try {
            var paths = new ArrayList<>(Arrays.asList(existingRoots()));
            paths.add(0, null);
            paths.add(129, " ");
            paths.add("invalid\0path");
            paths.add("a".repeat(4097));
            var json = new Json();
            json.setTypeName(null);
            json.setOutputType(JsonWriter.OutputType.json);
            String fixture = "{\"bmsroot\":" + json.toJson(paths.toArray(String[]::new)) + "}";
            for (String filename : new String[]{"config.json", "config_sys.json"}) {
                Path home = Files.createDirectory(directory.resolve(filename.equals("config.json") ? "legacy" : "current"));
                Files.writeString(home.resolve(filename), fixture);
                run(home, "save");
                run(home, "restart");
            }
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("BmsRootConfigTest passed (150 existing roots, invalid paths rejected, save and restart)");
    }

    private static String[] existingRoots() {
        return IntStream.range(0, EXISTING_ROOTS).mapToObj(i -> "library/folder-" + i).toArray(String[]::new);
    }

    private static void verifyStartup(boolean save) {
        var expected = new ArrayList<>(Arrays.asList(existingRoots()));
        if (!save) expected.add(ADDED_ROOT);
        Config config = Config.read();
        if (!expected.equals(Arrays.asList(config.getBmsroot()))) {
            throw new AssertionError("Expected " + expected.size() + " valid BMS paths in their original order; got "
                    + config.getBmsroot().length);
        }
        if (save) {
            expected.add(ADDED_ROOT);
            config.setBmsroot(expected.toArray(String[]::new));
            Config.write(config);
        }
    }

    private static void run(Path home, String action) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process child = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                BmsRootConfigTest.class.getName(), action).directory(home.toFile()).inheritIO().start();
        if (!child.waitFor(30, TimeUnit.SECONDS)) {
            child.destroyForcibly().waitFor();
            throw new AssertionError(action + " timed out");
        }
        if (child.exitValue() != 0) throw new AssertionError(action + " failed: " + child.exitValue());
    }
}
