package bms.player.beatoraja.build;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/** Linux artifact/ABI verification only: no display, GL context, audio or real IME server. */
public final class X11ImeArtifactTest {
    private static final String LIBRARY = "libglfw-beatoraja-x11-ime.so";
    private static final String REVISION = "d9d6f0f1f967807ffade6598ea9a631ebaf37a56";
    private static final Set<String> EXPORTS = Set.of("glfwInit", "glfwGetVersionString", "glfwGetPlatform",
            "Java_bms_player_beatoraja_input_X11ImeNative_versionNative",
            "Java_bms_player_beatoraja_input_X11ImeNative_openNative",
            "Java_bms_player_beatoraja_input_X11ImeNative_resetNative",
            "Java_bms_player_beatoraja_input_X11ImeNative_caretNative",
            "Java_bms_player_beatoraja_input_X11ImeNative_closeNative");

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--self-test")) { selfTest(); return; }
        check(args.length == 4,"Expected native output directory, distribution ZIP, Java executable and report directory");
        check(System.getProperty("os.name","").startsWith("Linux"),"Artifact verification requires Linux, not a skip/pass on another OS");
        check(Set.of("amd64","x86_64").contains(System.getProperty("os.arch","").toLowerCase(java.util.Locale.ROOT)),
                "Current distribution supports Linux x64; another architecture is not validated");
        Path directory=Path.of(args[0]), archive=Path.of(args[1]), output=Path.of(args[3]);
        Files.createDirectories(output);
        Path library=directory.resolve(LIBRARY);
        byte[] header;
        try (var stream=Files.newInputStream(library)) { header=stream.readNBytes(64); }
        verifyElf(header);
        check(Files.readString(directory.resolve("GLFW-REVISION.txt")).equals(REVISION+" (altered: opt-in beatoraja X11 IME)\n"),"Wrong native source revision");
        check(Files.size(directory.resolve("GLFW-LICENSE.md"))>100,"Missing upstream license");
        String exports=command(List.of("nm","-D","--defined-only",library.toAbsolutePath().toString()),output.resolve("exports.txt"));
        verifyExports(exports);
        String dependencies=command(List.of("ldd","-r",library.toAbsolutePath().toString()),output.resolve("dependencies.txt"));
        verifyDependencies(dependencies);
        var hashes=new LinkedHashMap<String,String>();
        for (String name : List.of(LIBRARY,"GLFW-LICENSE.md","GLFW-REVISION.txt"))
            hashes.put(name,verifyPackagedFile(archive,directory.resolve(name),"natives/"+name));
        // This fresh child sees only the extracted application's lib/*, never development/test classes.
        PackagedZipSmokeTest.main(new String[]{archive.toString(),args[2],output.resolve("zip-probe").toString(),"--x11-ime"});
        Map<String,Object> report=new LinkedHashMap<>();
        report.put("status","PASS"); report.put("os",System.getProperty("os.name")); report.put("arch",System.getProperty("os.arch"));
        report.put("revision",REVISION); report.put("filesSha256",hashes);
        report.put("checks",List.of("ELF64 little-endian x86-64 shared object","GLFW/JNI exports","ldd -r dependencies",
                "ZIP byte identity/license/revision","extracted-ZIP-only JNI ABI load and child exit"));
        report.put("notTested",List.of("X11/Wayland display initialization","GL window/rendering","XIC attachment",
                "Japanese IME conversion/candidate positioning","physical devices/latency","ARM-native"));
        var json=new Json(); json.setOutputType(JsonWriter.OutputType.json);
        Files.writeString(output.resolve("artifact-report.json"),json.prettyPrint(report)+"\n");
        System.out.println("X11ImeArtifactTest passed (ELF/exports/dependencies/ZIP/JNI load only; NO real display/IME)");
    }
    private static void verifyElf(byte[] header) {
        check(header.length>=64 && header[0]==0x7f && header[1]=='E' && header[2]=='L' && header[3]=='F',"Not an ELF file");
        check(header[4]==2 && header[5]==1 && header[6]==1,"Expected ELF64 little-endian version 1");
        var data=ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        check(data.getShort(16)==3 && data.getShort(18)==62,"Expected x86-64 shared object");
    }
    private static void verifyExports(String text) {
        var found=new java.util.HashSet<String>();
        for (String line : text.lines().toList()) {
            String[] fields=line.trim().split("\\s+");
            if (fields.length>=3) found.add(fields[fields.length-1]);
        }
        check(found.containsAll(EXPORTS),"Missing GLFW/JNI exports: "+EXPORTS.stream().filter(s->!found.contains(s)).toList());
    }
    private static void verifyDependencies(String text) {
        check(!text.isBlank() && !text.contains("not found") && !text.contains("undefined symbol"),"Unresolved shared-library dependency");
    }
    private static String verifyPackagedFile(Path archive,Path file,String suffix) throws Exception {
        byte[] sourceHash;
        try (var input=Files.newInputStream(file)) { sourceHash=digest(input); }
        try (var zip=new ZipFile(archive.toFile())) {
            var entries=zip.stream().filter(e->!e.isDirectory() && e.getName().endsWith("/"+suffix)).toList();
            check(entries.size()==1,"Missing/duplicate packaged native entry: "+suffix);
            try (var input=zip.getInputStream(entries.get(0))) { check(MessageDigest.isEqual(sourceHash,digest(input)),"ZIP native differs from verified build output: "+suffix); }
        }
        return HexFormat.of().formatHex(sourceHash);
    }
    private static byte[] digest(java.io.InputStream input) throws Exception {
        var digest=MessageDigest.getInstance("SHA-256");
        byte[] buffer=new byte[16384]; int count;
        while ((count=input.read(buffer))!=-1) digest.update(buffer,0,count);
        return digest.digest();
    }
    private static String command(List<String> args,Path log) throws Exception {
        var builder=new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("LC_ALL","C");
        var child=builder.start();
        try {
            check(child.waitFor(30,TimeUnit.SECONDS),"Native inspection timeout: "+args.get(0));
            String text=Files.readString(log);
            check(child.exitValue()==0,"Native inspection failed: "+args.get(0)+"; see "+log);
            return text;
        } finally { if (child.isAlive()) { child.destroyForcibly(); child.waitFor(10,TimeUnit.SECONDS); } }
    }
    private static void selfTest() throws Exception {
        try (var source=X11ImeArtifactTest.class.getClassLoader().getResourceAsStream(
                "META-INF/linux/x64/org/lwjgl/glfw/libglfw.so.git")) {
            check(source!=null && new String(source.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim().equals(REVISION),
                    "X11 IME must match the selected LWJGL GLFW native source");
        }
        for (String script:List.of("scripts/prepare-x11-ime.py","scripts/build-x11-ime.sh"))
            check(Files.readString(Path.of(script)).contains("REVISION="+REVISION)
                    || Files.readString(Path.of(script)).contains("REVISION = \""+REVISION+"\""),"Stale GLFW source pin: "+script);
        byte[] elf=new byte[64]; elf[0]=0x7f; elf[1]='E'; elf[2]='L'; elf[3]='F'; elf[4]=2; elf[5]=1; elf[6]=1; elf[16]=3; elf[18]=62;
        verifyElf(elf);
        for (int position : new int[]{0,4,5,6,16,18}) { byte[] broken=elf.clone(); broken[position]=0; rejects(()->verifyElf(broken)); }
        rejects(()->verifyElf(new byte[12]));
        String symbols=EXPORTS.stream().map(s->"0000000000010000 T "+s).collect(java.util.stream.Collectors.joining("\n"));
        verifyExports(symbols); rejects(()->verifyExports(symbols.replace("glfwInit","glfwInitWrong")));
        verifyDependencies("libX11.so.6 => /usr/lib/libX11.so.6 (0x1)");
        rejects(()->verifyDependencies("libX11.so.6 => not found")); rejects(()->verifyDependencies("undefined symbol: XSetICValues"));
        Path root=Files.createTempDirectory("x11 artifact inspector ");
        try {
            Path nativeFile=root.resolve(LIBRARY), zipFile=root.resolve("fixture.zip"); Files.write(nativeFile,elf);
            try (var zip=new java.util.zip.ZipOutputStream(Files.newOutputStream(zipFile))) {
                zip.putNextEntry(new java.util.zip.ZipEntry("beatoraja/natives/"+LIBRARY)); zip.write(elf); zip.closeEntry();
            }
            check(verifyPackagedFile(zipFile,nativeFile,"natives/"+LIBRARY).length()==64,"SHA-256 inspection");
            Files.write(nativeFile,new byte[]{1}); rejects(()->verifyPackagedFile(zipFile,nativeFile,"natives/"+LIBRARY));
            rejects(()->verifyPackagedFile(zipFile,nativeFile,"natives/missing.so"));
            try (var zip=new java.util.zip.ZipOutputStream(Files.newOutputStream(zipFile))) {
                for (String prefix:List.of("first/","second/")) {
                    zip.putNextEntry(new java.util.zip.ZipEntry(prefix+"natives/"+LIBRARY)); zip.write(elf); zip.closeEntry();
                }
            }
            Files.write(nativeFile,elf);
            rejects(()->verifyPackagedFile(zipFile,nativeFile,"natives/"+LIBRARY));
        } finally {
            try (var files=Files.walk(root)) { for (var path:files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
        }
        System.out.println("X11ImeArtifactTest self-test passed (synthetic headers/symbols/dependencies/ZIP; NO Linux artifact load)");
    }
    @FunctionalInterface private interface Check { void run() throws Exception; }
    private static void rejects(Check action) throws Exception {
        try { action.run(); } catch (AssertionError expected) { return; }
        throw new AssertionError("Invalid native artifact accepted");
    }
    private static void check(boolean value,String message) { if (!value) throw new AssertionError(message); }
}
