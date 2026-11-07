package bms.player.beatoraja.input;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.studiohartman.jamepad.Configuration;
import com.studiohartman.jamepad.ControllerAxis;
import com.studiohartman.jamepad.ControllerButton;
import net.java.games.input.ControllerEnvironment;

/** Stable SDL IDs/defaults and bundled native formats; real device enumeration is explicitly opt-in. */
public final class ControllerLibraryCompatibilityTest {
    public static void main(String[] args) throws Exception {
        if (Arrays.asList(args).contains("--jinput-only")) {
            verifyJInputNativeBackend();
            System.exit(0);
        }
        verifyMappings();
        verifyNatives();
        verifyEmbeddedJutils();
        if (Arrays.asList(args).contains("--native")) {
            verifyNativeBackends();
            System.out.println("ControllerLibraryCompatibilityTest native probe passed (host enumeration and SDL init/update/quit; NO physical-input validation)");
            // JInput owns its environment until JVM shutdown; no public close API exists.
            System.exit(0);
        }
        System.out.println("ControllerLibraryCompatibilityTest passed (SDL enum/default parity, native formats/architectures and embedded JUtils; NO device initialization)");
    }

    private static void verifyMappings() {
        check(Arrays.stream(ControllerButton.values()).map(Enum::name).toList().equals(List.of(
                "A", "B", "X", "Y", "BACK", "GUIDE", "START", "LEFTSTICK", "RIGHTSTICK",
                "LEFTBUMPER", "RIGHTBUMPER", "DPAD_UP", "DPAD_DOWN", "DPAD_LEFT", "DPAD_RIGHT",
                "BUTTON_MISC1", "BUTTON_PADDLE1", "BUTTON_PADDLE2", "BUTTON_PADDLE3", "BUTTON_PADDLE4", "BUTTON_TOUCHPAD")),
                "Preserve Jamepad 2.26.5 button ordinals");
        check(Arrays.stream(ControllerAxis.values()).map(Enum::name).toList().equals(List.of(
                "LEFTX", "LEFTY", "RIGHTX", "RIGHTY", "TRIGGERLEFT", "TRIGGERRIGHT")), "Preserve SDL axis ordinals");
        var defaults = new Configuration();
        check(defaults.maxNumControllers == 4 && !defaults.useRawInput && defaults.loadNativeLibrary
                && defaults.loadDatabaseInMemory, "Preserve Jamepad configuration defaults");
    }

    private static void verifyNatives() throws IOException {
        for (String name : List.of("jinput-dx8_64.dll", "jinput-raw_64.dll", "jinput-wintab.dll", "jamepad64.dll"))
            verifyPe64(name);
        for (String name : List.of("libjinput-linux64.so", "libjamepad64.so")) verifyElf64(name);
        var mac = ByteBuffer.wrap(resource("libjinput-osx.jnilib")).order(ByteOrder.BIG_ENDIAN);
        check(mac.getInt(0) == 0xcafebabe && mac.getInt(4) == 2, "Universal macOS JInput library");
        Set<Integer> architectures = new HashSet<>();
        for (int i = 0; i < mac.getInt(4); i++) architectures.add(mac.getInt(8 + i * 20));
        check(architectures.equals(Set.of(0x01000007, 0x0100000c)), "JInput macOS x86_64/arm64 slices");
        for (String name : List.of("libjamepad64.dylib", "libjamepadarm64.dylib")) {
            var header = ByteBuffer.wrap(resource(name)).order(ByteOrder.LITTLE_ENDIAN);
            check(header.getInt(0) == 0xfeedfacf, "64-bit Mach-O: " + name);
            check(header.getInt(4) == (name.contains("arm64") ? 0x0100000c : 0x01000007), "Mach-O CPU: " + name);
        }
        for (String retired : List.of("jinput-dx8.dll", "jinput-raw.dll", "libjinput-linux.so"))
            check(ControllerLibraryCompatibilityTest.class.getClassLoader().getResource(retired) == null,
                    "Do not mix retired JInput natives: " + retired);
    }

    private static void verifyPe64(String name) throws IOException {
        var data = ByteBuffer.wrap(resource(name)).order(ByteOrder.LITTLE_ENDIAN);
        check(data.getShort(0) == 0x5a4d, "PE DOS signature: " + name);
        int pe = data.getInt(0x3c);
        check(data.getInt(pe) == 0x00004550 && Short.toUnsignedInt(data.getShort(pe + 4)) == 0x8664,
                "PE x86_64: " + name);
    }

    private static void verifyElf64(String name) throws IOException {
        var data = ByteBuffer.wrap(resource(name)).order(ByteOrder.LITTLE_ENDIAN);
        check(data.getInt(0) == 0x464c457f && data.get(4) == 2 && data.get(5) == 1
                && data.getShort(18) == 62, "ELF64 x86_64: " + name);
    }

    private static void verifyEmbeddedJutils() throws IOException {
        var resources = ControllerLibraryCompatibilityTest.class.getClassLoader()
                .getResources("net/java/games/util/plugins/Plugins.class");
        check(resources.hasMoreElements(), "Embedded JUtils");
        resources.nextElement();
        check(!resources.hasMoreElements(), "No duplicate external JUtils JAR");
    }

    private static void verifyNativeBackends() throws Exception {
        verifyJInputNativeBackend();
        for (int cycle = 0; cycle < 2; cycle++) {
            var manager = new com.studiohartman.jamepad.ControllerManager(new Configuration());
            boolean initialized = false;
            try {
                manager.initSDLGamepad(); initialized = true;
                manager.update();
                System.out.println("Jamepad 2.30.0.0 native cycle=" + cycle + ", attached joysticks=" + manager.getNumControllers());
            } finally {
                if (initialized) manager.quitSDLGamepad();
            }
        }
    }

    private static void verifyJInputNativeBackend() throws Exception {
        JInputControllerManager.prepareNativeLibraries();
        String os = System.getProperty("os.name");
        String plugin = os.startsWith("Windows") ? "DirectAndRawInputEnvironmentPlugin"
                : os.startsWith("Mac") ? "OSXEnvironmentPlugin" : "LinuxEnvironmentPlugin";
        var environment = (ControllerEnvironment) Class.forName("net.java.games.input." + plugin).getConstructor().newInstance();
        check(environment.isSupported(), "JInput host native plugin must load, not silently fall back");
        System.out.println("JInput native plugin supported; source="
                + ControllerEnvironment.class.getProtectionDomain().getCodeSource().getLocation()
                + "; enumerated devices=" + environment.getControllers().length);
    }

    private static byte[] resource(String name) throws IOException {
        try (var stream = ControllerLibraryCompatibilityTest.class.getClassLoader().getResourceAsStream(name)) {
            check(stream != null, "Missing native resource: " + name);
            return stream.readAllBytes();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
