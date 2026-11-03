package bms.player.beatoraja.ir;

import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import javax.tools.ToolProvider;

import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.ScoreData;
import com.badlogic.gdx.utils.Json;

/** Score conversion, IPC defaults, course totals, and binary linkage of an old-style IR JAR. */
public final class IRScorePlaytimeTest {
    public static void main(String[] args) throws Exception {
        ScoreData score = new ScoreData();
        equal(-1, score.getPlaytime());
        equal(-1, new IRScoreData(score).playtime);
        score.setPlaytime(123456);
        IRScoreData ir = new IRScoreData(score);
        equal(123456, ir.playtime);
        var dto = IRWorkerProtocol.Score.from(ir);
        Json json = new Json();
        var restored = json.fromJson(IRWorkerProtocol.Score.class, json.toJson(dto));
        equal(123456, restored.toIRScoreData().playtime);
        equal(-1, json.fromJson(IRWorkerProtocol.Score.class, "{}").toIRScoreData().playtime);
        equal(-1, json.fromJson(ScoreData.class, "{}").getPlaytime());
        equal(0, json.fromJson(IRWorkerProtocol.Score.class, "{\"playtime\":0}").toIRScoreData().playtime);
        equal(-1, legacy().playtime);

        ScoreData course = new ScoreData();
        course.setPlaytime(0);
        course.addPlaytime(120000);
        course.addPlaytime(30000);
        equal(150000, new IRScoreData(course).playtime);
        course.addPlaytime(-1);
        course.addPlaytime(10000);
        equal(-1, course.getPlaytime());
        course.setPlaytime(Long.MAX_VALUE);
        course.addPlaytime(1);
        equal(-1, course.getPlaytime());
        score.setPlaytime(-2);
        equal(-1, score.getPlaytime());
        assertLegacyJarLinkage();
        System.out.println("IRScorePlaytimeTest passed");
    }

    private static IRScoreData legacy() {
        return new IRScoreData("hash", 0, "id", "player", ClearType.Normal, 123,
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
                13L, 14, 15, 16, 17, 18, 19L, 20, 21, null, null, null, "skin");
    }

    private static void assertLegacyJarLinkage() throws Exception {
        Path root = Files.createTempDirectory("legacy-ir-score");
        try {
            var oldConstructor = Arrays.stream(IRScoreData.class.getConstructors())
                    .filter(c -> c.getParameterTypes()[c.getParameterCount() - 1] == String.class).findFirst().orElseThrow();
            Class<?>[] types = oldConstructor.getParameterTypes();
            String parameters = java.util.stream.IntStream.range(0, types.length)
                    .mapToObj(i -> types[i].getCanonicalName() + " p" + i).collect(Collectors.joining(","));
            Path stub = root.resolve("IRScoreData.java");
            Files.writeString(stub, "package bms.player.beatoraja.ir; public class IRScoreData { public IRScoreData("
                    + parameters + ") {} }");
            Path plugin = root.resolve("LegacyScorePlugin.java");
            Files.writeString(plugin, "package legacy; import bms.player.beatoraja.ir.IRScoreData; "
                    + "public class LegacyScorePlugin { public static IRScoreData create() { return new IRScoreData("
                    + "\"hash\",0,\"id\",\"player\",bms.player.beatoraja.ClearType.Normal,123,"
                    + "1,2,3,4,5,6,7,8,9,10,11,12,13L,14,15,16,17,18,19L,20,21,null,null,null,\"skin\"); } }");
            int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, "--release", "17", "-classpath",
                    System.getProperty("java.class.path"), "-d", root.toString(), stub.toString(), plugin.toString());
            if (result != 0) throw new AssertionError("Legacy plugin compilation failed");
            Path jar = root.resolve("legacy.jar");
            try (var output = new JarOutputStream(Files.newOutputStream(jar))) {
                output.putNextEntry(new JarEntry("legacy/LegacyScorePlugin.class"));
                Files.copy(root.resolve("legacy/LegacyScorePlugin.class"), output);
                output.closeEntry();
            }
            // The JAR was compiled against the old-only stub, but resolves the current API at runtime.
            try (var loader = new URLClassLoader(new java.net.URL[] {jar.toUri().toURL()}, IRScoreData.class.getClassLoader())) {
                IRScoreData score = (IRScoreData) loader.loadClass("legacy.LegacyScorePlugin").getMethod("create").invoke(null);
                equal(-1, score.playtime);
                equal(123, score.date);
                equal(15, score.notes);
                equal(1, score.epg);
            }
        } finally {
            try (var files = Files.walk(root)) {
                for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
            }
        }
    }

    private static void equal(long expected, long actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + " but was " + actual);
    }
}
