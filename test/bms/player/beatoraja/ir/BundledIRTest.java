package bms.player.beatoraja.ir;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.jar.JarOutputStream;

/** Index validation and private extraction; no IR class is loaded. */
public final class BundledIRTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("bundled-ir-validation-");
        try {
            Path core = Files.createDirectory(root.resolve("core")), temp = Files.createDirectory(root.resolve("temp"));
            try (var prepared = BundledIR.prepare(core, temp)) { check(prepared.files().isEmpty(), "No index must not enable bundling"); }
            Path index = core.resolve(BundledIR.INDEX); Files.createDirectories(index.getParent());
            Path source = core.resolve(BundledIR.ROOT + "jars/sample.jar"); Files.createDirectories(source.getParent());
            try (var jar = new JarOutputStream(Files.newOutputStream(source))) { jar.putNextEntry(new java.util.jar.JarEntry("data.txt")); jar.write(42); jar.closeEntry(); }
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source)));
            write(index, "sample.jar", Files.size(source), hash);
            Path extracted;
            try (var prepared = BundledIR.prepare(core, temp)) {
                extracted = prepared.files().get(0);
                check(Files.mismatch(source, extracted) == -1 && extracted.startsWith(temp), "Private original JAR extraction");
                if (Files.getFileStore(extracted).supportsFileAttributeView("posix"))
                    check(Files.getPosixFilePermissions(extracted.getParent()).equals(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")), "Private POSIX directory");
                try (var second = BundledIR.prepare(core, temp)) {
                    check(!second.files().get(0).getParent().equals(extracted.getParent()), "Workers must not share extracted files");
                }
            }
            check(!Files.exists(extracted.getParent()), "Normal close removes archive/directory");
            write(index, "sample.jar", Files.size(source), "0".repeat(64)); rejects(core, temp);
            write(index, "sample.jar", Files.size(source) - 1, hash); rejects(core, temp);
            write(index, "sample.jar", Files.size(source) + 1, hash); rejects(core, temp);
            write(index, "missing.jar", 10, hash); rejects(core, temp);
            for (String name : new String[]{"../escaped.jar", "/absolute.jar", "C:\\escape.jar", "bad/name.jar"}) {
                write(index, name, 10, hash); rejects(core, temp);
            }
            write(index, "sample.jar", -1, hash); rejects(core, temp);
            write(index, "sample.jar", 512L * 1024 * 1024 + 1, hash); rejects(core, temp);
            write(index, "sample.jar", 10, "bad hash"); rejects(core, temp);
            write(index, "sample.jar", Files.size(source), hash);
            try (var out = new DataOutputStream(Files.newOutputStream(index, java.nio.file.StandardOpenOption.APPEND))) { out.write(1); }
            rejects(core, temp);
            Files.write(index, new byte[65537]); rejects(core, temp);
            for (int count : new int[]{0, -1, 129}) {
                try (var out = new DataOutputStream(Files.newOutputStream(index))) { out.writeInt(0x42495231); out.writeInt(count); }
                rejects(core, temp);
            }
            try (var out = new DataOutputStream(Files.newOutputStream(index))) {
                out.writeInt(0x42495231); out.writeInt(2);
                for (String name : new String[]{"sample.jar", "SAMPLE.jar"}) { out.writeUTF(name); out.writeLong(10); out.writeUTF(hash); }
            }
            rejects(core, temp);
            write(index, "sample.jar", Files.size(source), hash);
            Files.writeString(source, "corrupted archive"); rejects(core, temp);
            write(index, "sample.jar", Files.size(source), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source))));
            rejects(core, temp);
            check(!Files.exists(root.resolve("escaped.jar")), "Traversal never written");
            System.out.println("BundledIRTest passed (bounded index, names/count/size/checksum, missing/corrupt resources, private directories, separate workers and cleanup)");
        } finally { try (var files = Files.walk(root)) { for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(path); } }
    }
    private static void write(Path index, String name, long size, String hash) throws Exception {
        try (var out = new DataOutputStream(Files.newOutputStream(index))) {
            out.writeInt(0x42495231); out.writeInt(1); out.writeUTF(name); out.writeLong(size); out.writeUTF(hash);
        }
    }
    private static void rejects(Path core, Path temp) throws Exception {
        try (var prepared = BundledIR.prepare(core, temp)) { throw new AssertionError("Invalid bundle accepted"); }
        catch (IOException expected) { }
        try (var files = Files.list(temp)) { check(files.findAny().isEmpty(), "Failed extraction leaked temporary files"); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
