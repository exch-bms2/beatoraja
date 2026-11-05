package bms.player.beatoraja.ir;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarFile;

/** Opaque IR archives from the application's own code source, never a parent class-loader input. */
final class BundledIR {
    static final String ROOT = "META-INF/beatoraja/bundled-ir/";
    static final String INDEX = ROOT + "index.bin";
    private record Entry(String name, long size, String hash) { }

    private BundledIR() { }

    static boolean isPresent() throws IOException {
        try (var source = Source.open(IRProcessSupport.applicationLocation())) {
            return !entries(source).isEmpty();
        }
    }

    static Prepared prepare() throws IOException { return prepare(IRProcessSupport.applicationLocation()); }

    /** Each worker gets its own unpublished directory; no shared-cache files are ever reused. */
    static Prepared prepare(Path core) throws IOException { return prepare(core, Path.of(System.getProperty("java.io.tmpdir"))); }

    static Prepared prepare(Path core, Path temporaryRoot) throws IOException {
        try (var source = Source.open(core)) {
            var entries = entries(source);
            if (entries.isEmpty()) return new Prepared(null, List.of());
            var attributes = java.nio.file.FileSystems.getDefault().supportedFileAttributeViews().contains("posix")
                    ? new java.nio.file.attribute.FileAttribute<?>[]{PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))}
                    : new java.nio.file.attribute.FileAttribute<?>[0];
            Path directory = Files.createTempDirectory(temporaryRoot, "beatoraja-ir-", attributes);
            var files = new ArrayList<Path>();
            var prepared = new Prepared(directory, files);
            try {
                for (Entry entry : entries) {
                    Path file = directory.resolve(entry.name());
                    files.add(file);
                    MessageDigest digest = sha256();
                    long length = 0;
                    try (var input = source.read(ROOT + "jars/" + entry.name());
                            var output = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                        if (input == null) throw new IOException("Missing bundled IR JAR: " + entry.name());
                        byte[] buffer = new byte[8192];
                        for (int n; (n = input.read(buffer)) != -1;) {
                            length += n;
                            if (length > entry.size()) throw new IOException("Oversized bundled IR JAR: " + entry.name());
                            digest.update(buffer, 0, n); output.write(buffer, 0, n);
                        }
                    }
                    if (length != entry.size() || !entry.hash().equals(HexFormat.of().formatHex(digest.digest())))
                        throw new IOException("Bundled IR checksum/size mismatch: " + entry.name());
                    try (var jar = new JarFile(file.toFile())) { jar.size(); }
                }
                return prepared;
            } catch (IOException | RuntimeException | Error failure) {
                try { prepared.close(); } catch (IOException cleanup) { failure.addSuppressed(cleanup); }
                throw failure;
            }
        }
    }

    private static List<Entry> entries(Source source) throws IOException {
        try (var input = source.read(INDEX)) {
            if (input == null) return List.of();
            byte[] bytes = input.readNBytes(65537);
            if (bytes.length > 65536) throw new IOException("Oversized bundled IR index");
            try (var data = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (data.readInt() != 0x42495231) throw new IOException("Invalid bundled IR index version");
                int count = data.readInt();
                if (count < 1 || count > 128) throw new IOException("Invalid bundled IR count");
                var entries = new ArrayList<Entry>();
                var names = new HashSet<String>();
                long total = 0;
                for (int i = 0; i < count; i++) {
                    String name = data.readUTF(); long size = data.readLong(); String hash = data.readUTF();
                    total += size;
                    if (!name.matches("[A-Za-z0-9][A-Za-z0-9._ ()-]{0,123}\\.[jJ][aA][rR]")
                            || !names.add(name.toLowerCase(Locale.ROOT)) || size <= 0 || size > 512L * 1024 * 1024
                            || total > 1024L * 1024 * 1024 || !hash.matches("[0-9a-f]{64}"))
                        throw new IOException("Invalid bundled IR entry: " + name);
                    entries.add(new Entry(name, size, hash));
                }
                if (data.read() != -1) throw new IOException("Trailing bundled IR index data");
                return entries;
            }
        }
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }

    private record Source(Path directory, JarFile jar) implements AutoCloseable {
        static Source open(Path path) throws IOException {
            return Files.isDirectory(path) ? new Source(path, null) : new Source(null, new JarFile(path.toFile()));
        }
        InputStream read(String resource) throws IOException {
            if (jar == null) {
                Path file = directory.resolve(resource);
                return Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) ? Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS) : null;
            }
            var entry = jar.getJarEntry(resource);
            return entry == null ? null : jar.getInputStream(entry);
        }
        public void close() throws IOException { if (jar != null) jar.close(); }
    }

    /** Owned by one Process; call close only after its class loaders have exited. */
    static final class Prepared implements AutoCloseable {
        private final Path directory;
        private final List<Path> files;
        Prepared(Path directory, List<Path> files) { this.directory = directory; this.files = files; }
        List<Path> files() { return List.copyOf(files); }
        public synchronized void close() throws IOException {
            IOException failure = null;
            for (Path file : files) {
                try { Files.deleteIfExists(file); }
                catch (IOException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
            }
            if (directory != null) {
                try { Files.deleteIfExists(directory); }
                catch (IOException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
            }
            if (failure != null) throw failure;
        }
    }
}
