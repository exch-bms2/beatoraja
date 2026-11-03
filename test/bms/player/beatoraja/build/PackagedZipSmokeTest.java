package bms.player.beatoraja.build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

/** Explicit native test using ONLY the extracted ZIP runtime, never test/development classes. */
public final class PackagedZipSmokeTest {
    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4 || args.length == 4 && !args[3].equals("--x11-ime"))
            throw new IllegalArgumentException("Expected ZIP, Java executable, output directory and optional --x11-ime");
        boolean x11 = args.length == 4;
        Path output = Path.of(args[2]);
        Files.createDirectories(output);
        Path extracted = Files.createTempDirectory(output, "zip runtime with spaces-");
        try (var zip = new ZipFile(args[0])) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                Path target = extracted.resolve(entry.getName()).normalize();
                if (!target.startsWith(extracted)) throw new AssertionError("ZIP traversal");
                if (entry.isDirectory()) Files.createDirectories(target);
                else {
                    Files.createDirectories(target.getParent());
                    try (var input = zip.getInputStream(entry)) { Files.copy(input, target); }
                }
            }
        }
        Path root;
        try (var children = Files.list(extracted)) { root = children.filter(Files::isDirectory).findFirst().orElseThrow(); }
        Path log = output.resolve("native-probe.log");
        var command = new java.util.ArrayList<String>();
        command.add(args[1]);
        command.add("-Dbeatoraja.distributionDiagnostics=true"); command.add("-Djava.library.path=./natives");
        if (x11) {
            Path library = root.resolve("natives/libglfw-beatoraja-x11-ime.so").toAbsolutePath();
            if (!Files.isRegularFile(library)) throw new AssertionError("X11 IME native missing from ZIP");
            command.add("-Dorg.lwjgl.glfw.libname=" + library);
            command.add("-Dbeatoraja.x11Ime.library=" + library);
        }
        command.addAll(java.util.List.of("-cp", "lib/*", "bms.player.beatoraja.DesktopLauncher", "--runtime-smoke"));
        if (x11) command.add("--x11-ime");
        Process child = new ProcessBuilder(command)
                .directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            if (!child.waitFor(90, TimeUnit.SECONDS)) throw new AssertionError("Packaged native probe timeout; see " + log);
            String text = Files.readString(log);
            System.out.print(text);
            if (child.exitValue() != 0 || !text.contains("Native probe failures=0")) throw new AssertionError("Packaged native probe failed");
            if (x11 && !text.contains("PASS X11 IME prototype (load/ABI only)"))
                throw new AssertionError("X11 IME ABI probe was not executed");
            if (!Files.readString(root.resolve("diagnostics/process-" + child.pid() + ".log")).contains("shutdown-hook"))
                throw new AssertionError("Missing shutdown evidence");
            if (Files.exists(root.resolve("config_sys.json")) || Files.exists(root.resolve("player")))
                throw new AssertionError("Probe touched player configuration");
            System.out.println("PackagedZipSmokeTest passed: pid=" + child.pid() + ", exited=true; extracted=" + root
                    + "; Launcher/UI/device checks remain manual");
        } finally {
            if (child.isAlive()) { child.destroyForcibly(); child.waitFor(10, TimeUnit.SECONDS); }
        }
    }
}
