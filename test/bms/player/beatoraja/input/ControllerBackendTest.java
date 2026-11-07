package bms.player.beatoraja.input;

import bms.model.Mode;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlayModeConfig;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Json;
import java.nio.file.Files;
import java.util.Arrays;

/** Configuration migration and serialization, without opening either native backend. */
public final class ControllerBackendTest {
    public static void main(String[] args) throws Exception {
        var json = new Json();
        var config = json.fromJson(Config.class, "{}");
        check(config.getControllerBackend() == ControllerBackend.JINPUT, "Old config defaults to JInput");
        config.setControllerBackend(null); check(config.getControllerBackend() == ControllerBackend.JINPUT, "Null fallback");
        config = json.fromJson(Config.class, "{controllerBackend:SDL}");
        check(config.getControllerBackend() == ControllerBackend.SDL && json.toJson(config).contains("SDL"), "Config persistence");
        String previous = Controllers.preferredManager;
        try {
            ControllerBackend.SDL.configure(); check(Controllers.preferredManager.equals(SdlControllerManager.class.getName()), "SDL startup selection");
            ControllerBackend.SDL_ASYNC.configure(); check(Controllers.preferredManager.equals(SdlAsyncControllerManager.class.getName()), "SDL3 mapped async selection");
            ControllerBackend.JINPUT.configure(); check(Controllers.preferredManager.equals(JInputControllerManager.class.getName()), "JInput startup selection");
        } finally { Controllers.preferredManager = previous; }

        var player = new PlayerConfig();
        for (Mode mode : new Mode[]{Mode.BEAT_5K, Mode.BEAT_7K, Mode.BEAT_10K, Mode.BEAT_14K, Mode.POPN_9K, Mode.KEYBOARD_24K, Mode.KEYBOARD_24K_DOUBLE}) {
            var play = player.getPlayConfig(mode);
            Arrays.fill(play.getKeyboardConfig().getKeyAssign(), -1);
        }
        var legacy = player.getMode7().getController()[0];
        legacy.setName("PS2 adapter"); legacy.getKeyAssign()[0] = 3; legacy.setStart(8); legacy.setSelect(9);
        legacy.setDuration(7); legacy.setJKOC(true); legacy.setAnalogScratch(true); legacy.setAnalogScratchMode(1); legacy.setAnalogScratchThreshold(17);
        player.selectControllerBackend(ControllerBackend.SDL); player.validate();
        var sdl = player.getMode7().getController()[0];
        check(sdl != legacy && sdl.getName().isEmpty() && Arrays.stream(sdl.getKeyAssign()).allMatch(i -> i == -1)
                && sdl.getStart() == -1 && sdl.getSelect() == -1, "SDL starts unassigned, never inherits raw IDs");
        sdl.setName("SDL adapter"); sdl.getKeyAssign()[0] = 5; sdl.setStart(4); sdl.setSelect(6); sdl.setDuration(2);
        sdl.setAnalogScratch(true); sdl.setAnalogScratchThreshold(23);
        var bank = player.getMode7().getController();
        player.selectControllerBackend(ControllerBackend.SDL_ASYNC); player.validate();
        check(player.getMode7().getController() == bank && player.getMode7().getController()[0] == sdl, "SDL_ASYNC shares exact SDL bank");
        player.getMode7().setController(bank.clone());
        player.selectControllerBackend(ControllerBackend.SDL);
        check(player.getMode7().getController() != bank && player.getMode7().getController()[0] == sdl, "Async setter updates the shared SDL bank");
        check(json.fromJson(Config.class, "{controllerBackend:SDL_ASYNC}").getControllerBackend() == ControllerBackend.SDL_ASYNC, "Config selector persistence");
        String saved = json.toJson(player);
        check(saved.contains("sdlController") && !saved.contains("controllerBackend"), "Both banks persisted, active selection remains machine-local/transient");
        var copy = json.fromJson(PlayerConfig.class, saved);
        copy.selectControllerBackend(ControllerBackend.SDL); copy.validate();
        check(copy.getMode7().getController()[0].getKeyAssign()[0] == 5 && copy.getMode7().getController()[0].getStart() == 4, "SDL round trip");
        copy.selectControllerBackend(ControllerBackend.JINPUT); copy.validate();
        var restored = copy.getMode7().getController()[0];
        check(restored.getName().equals("PS2 adapter") && restored.getKeyAssign()[0] == 3 && restored.getStart() == 8
                && restored.getDuration() == 7 && restored.getJKOC() && restored.isAnalogScratch()
                && restored.getAnalogScratchMode() == 1 && restored.getAnalogScratchThreshold() == 17, "Legacy bank unchanged");
        copy.selectControllerBackend(ControllerBackend.SDL);
        check(copy.getMode14().getController().length == 2 && copy.getMode24double().getController().length == 2, "Independent DP banks");
        var damaged = json.fromJson(PlayModeConfig.class, "{controller:null,sdlController:[null],version:1}");
        damaged.selectControllerBackend(ControllerBackend.SDL, Mode.BEAT_14K); damaged.validate(Mode.BEAT_14K);
        check(damaged.getController().length == 2 && damaged.getController()[0].getKeyAssign().length == 18, "Corrupt SDL bank repair");
        var migration = json.fromJson(PlayModeConfig.class, "{controller:[{keys:[16]}],sdlController:[{keys:[16]}],version:0}");
        Arrays.fill(migration.getKeyboardConfig().getKeyAssign(), -1);
        migration.selectControllerBackend(ControllerBackend.SDL, Mode.BEAT_7K); migration.validate(Mode.BEAT_7K);
        check(migration.getController()[0].getKeyAssign()[0] == 16, "Old axis-ID migration never rewrites SDL IDs");
        migration.selectControllerBackend(ControllerBackend.JINPUT, Mode.BEAT_7K);
        check(migration.getController()[0].getKeyAssign()[0] == 32, "Legacy axis-ID migration preserved");
        var replacement = new PlayerConfig(); replacement.selectControllerBackend(ControllerBackend.SDL);
        replacement.setMode7(new PlayModeConfig(Mode.BEAT_7K));
        check(replacement.getMode7().getController()[0].getStart() == -1, "Replacement mode keeps active backend");
        replacement.setMode7(null);
        check(replacement.getMode7().getController()[0].getStart() == -1, "Lazy mode repair keeps active backend");
        var root = Files.createTempDirectory("backend-config-");
        try {
            Files.createDirectory(root.resolve("test")); copy.setId("test"); PlayerConfig.write(root.toString(), copy);
            var disk = PlayerConfig.readPlayerConfig(root.toString(), "test", ControllerBackend.SDL);
            check(disk.getMode7().getController()[0].getKeyAssign()[0] == 5, "Disk read selects SDL before validation");
            disk = PlayerConfig.readPlayerConfig(root.toString(), "test", ControllerBackend.SDL_ASYNC);
            check(disk.getMode7().getController()[0].getKeyAssign()[0] == 5 && disk.getMode7().getController()[0].getStart() == 4
                    && disk.getMode14().getController().length == 2, "Async disk read retains SDL keys/START/DP lengths");
            disk = PlayerConfig.readPlayerConfig(root.toString(), "test");
            check(disk.getMode7().getController()[0].getKeyAssign()[0] == 3, "Old read API remains JInput-compatible");
        } finally { try (var paths = Files.walk(root)) { for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path); } }
        System.out.println("ControllerBackendTest passed (startup/default/null, separate banks, migration, DP, corrupted data and JSON/disk round trips)");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
