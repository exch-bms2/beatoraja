package bms.player.beatoraja.build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Comparator;

/** Real SQLite native operations and SLF4J API compatibility without player data or a log provider. */
public final class DatabaseRuntimeCompatibilityTest {
    public static void main(String[] args) throws Exception {
        verifySlf4j();
        Path root = Files.createTempDirectory("database-runtime-compatibility-");
        try {
            verifySqlite(root);
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
        System.out.println("DatabaseRuntimeCompatibilityTest passed (SQLite native/WAL/attach/rollback/reopen and SLF4J legacy/fluent API)");
    }

    private static void verifySlf4j() throws Exception {
        var factory = Class.forName("org.slf4j.LoggerFactory");
        var loggerType = Class.forName("org.slf4j.Logger");
        Object logger = factory.getMethod("getLogger", String.class).invoke(null, "compatibility-test");
        Object implementation = factory.getMethod("getILoggerFactory").invoke(null);
        check(implementation.getClass().getName().equals("org.slf4j.helpers.NOPLoggerFactory"),
                "Keep the previous no-provider logging behavior");
        loggerType.getMethod("info", String.class, Object.class).invoke(logger, "Legacy {}", "API");
        loggerType.getMethod("debug", String.class, Object[].class).invoke(logger, "Legacy {} {}", new Object[] {1, 2});
        loggerType.getMethod("error", String.class, Throwable.class).invoke(logger, "Expected ignored error", new Exception("fixture"));
        Object builder = loggerType.getMethod("atInfo").invoke(logger);
        Class.forName("org.slf4j.spi.LoggingEventBuilder").getMethod("log", String.class).invoke(builder, "Fluent API");
        var providers = factory.getClassLoader().getResources("META-INF/services/org.slf4j.spi.SLF4JServiceProvider");
        check(!providers.hasMoreElements(), "Do not introduce a logging provider through transitive dependencies");
    }

    private static void verifySqlite(Path root) throws Exception {
        Path file = root.resolve("score \u65e5\u672c\u8a9e.db");
        Path info = root.resolve("info.db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                var statement = connection.createStatement()) {
            check(connection.getMetaData().getDriverVersion().equals("3.54.1.0"), "Selected SQLite JDBC version");
            try (var rows = statement.executeQuery("SELECT sqlite_version()")) {
                check(rows.next() && rows.getString(1).equals("3.54.1"), "Bundled SQLite native version");
            }
            try (var rows = statement.executeQuery("PRAGMA journal_mode=WAL")) {
                check(rows.next() && rows.getString(1).equalsIgnoreCase("wal"), "WAL mode");
            }
            statement.executeUpdate("CREATE TABLE score (sha256 TEXT PRIMARY KEY, avgjudge INTEGER, comment TEXT, ghost BLOB)");
            try (var attach = connection.prepareStatement("ATTACH DATABASE ? AS songinfo")) {
                attach.setString(1, info.toString());
                attach.execute();
            }
            statement.executeUpdate("CREATE TABLE songinfo.info (sha256 TEXT PRIMARY KEY, notes INTEGER)");
            connection.setAutoCommit(false);
            try (var insert = connection.prepareStatement("INSERT INTO score VALUES (?, ?, ?, ?)")) {
                insert.setString(1, "hash");
                insert.setLong(2, Long.MAX_VALUE);
                insert.setString(3, null);
                insert.setBytes(4, new byte[] {0, -1, 127});
                check(insert.executeUpdate() == 1, "Prepared insert");
            }
            connection.commit();
            statement.executeUpdate("UPDATE score SET avgjudge = 1");
            connection.rollback();
            connection.setAutoCommit(true);
            statement.executeUpdate("INSERT INTO songinfo.info VALUES ('hash', 1600)");
            try (var rows = statement.executeQuery("SELECT avgjudge, comment, ghost, notes FROM score JOIN songinfo.info USING (sha256)")) {
                check(rows.next() && rows.getLong(1) == Long.MAX_VALUE && rows.getString(2) == null
                        && Arrays.equals(rows.getBytes(3), new byte[] {0, -1, 127}) && rows.getInt(4) == 1600,
                        "Rollback, attached DB join, 64-bit values, NULL and BLOB");
            }
            statement.execute("DETACH DATABASE songinfo");
            try (var rows = statement.executeQuery("PRAGMA integrity_check")) {
                check(rows.next() && rows.getString(1).equals("ok"), "Database integrity");
            }
        }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                var statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT avgjudge FROM score WHERE sha256 = 'hash'")) {
            check(rows.next() && rows.getLong(1) == Long.MAX_VALUE, "Reopen committed database");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
