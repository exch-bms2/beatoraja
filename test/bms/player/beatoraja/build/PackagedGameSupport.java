package bms.player.beatoraja.build;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import bms.player.beatoraja.*;
import bms.player.beatoraja.song.SQLiteSongDatabaseAccessor;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/** Isolated fixture and validation helpers; never shipped in the application. */
public final class PackagedGameSupport {
    static boolean shouldObserve(String command) {
        String entry = command.split("\\s+", 2)[0];
        return !entry.equals("bms.player.beatoraja.ir.IRWorkerMain")
                && !command.contains("--display-modes") && !command.contains("--runtime-smoke");
    }

    static final String MARKER = "beatoraja-generated-packaged-game-test-v1";
    private PackagedGameSupport() { }

    static Path extract(Path archive, Path destination) throws Exception {
        Files.createDirectory(destination);
        try (var zip = new ZipFile(archive.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                Path target = destination.resolve(entry.getName()).normalize();
                check(target.startsWith(destination) && !entry.getName().contains("\\"), "ZIP traversal/ambiguous path");
                if (entry.isDirectory()) Files.createDirectories(target);
                else {
                    Files.createDirectories(target.getParent());
                    try (var input = zip.getInputStream(entry)) { Files.copy(input, target); }
                }
            }
        }
        try (var children = Files.list(destination)) {
            var roots = children.toList();
            check(roots.size() == 1 && Files.isDirectory(roots.get(0)) && Files.isDirectory(roots.get(0).resolve("lib")), "One distribution root required");
            return roots.get(0);
        }
    }

    static void fixture(Path root) throws Exception {
        check(!Files.exists(root.resolve("config_sys.json")) && !Files.exists(root.resolve("player")), "Refuse existing player configuration");
        Files.createDirectory(root.resolve("test-evidence"));
        Files.writeString(root.resolve("test-evidence/marker"), MARKER);
        Path songs = Files.createDirectory(root.resolve("test-song"));
        byte[] wave = new byte[44 + 4410 * 2];
        var data = ByteBuffer.wrap(wave).order(ByteOrder.LITTLE_ENDIAN);
        data.put(new byte[]{'R','I','F','F'}).putInt(wave.length - 8).put(new byte[]{'W','A','V','E','f','m','t',' '})
                .putInt(16).putShort((short)1).putShort((short)1).putInt(44100).putInt(88200).putShort((short)2).putShort((short)16)
                .put(new byte[]{'d','a','t','a'}).putInt(wave.length - 44);
        Files.write(songs.resolve("silence.wav"), wave);
        Files.copy(root.resolve("skin/default/system.png"), songs.resolve("stage.png"));
        Files.writeString(songs.resolve("smoke.bms"), "#PLAYER 1\n#TITLE Packaged game smoke\n#ARTIST Generated fixture\n"
                + "#BPM 240\n#PLAYLEVEL 1\n#RANK 2\n#TOTAL 100\n#WAV01 silence.wav\n#BMP01 stage.png\n"
                + "#00004:01\n#00111:01010101\n#00219:01\n");
        var config = new Config();
        config.setControllerBackend(bms.player.beatoraja.input.ControllerBackend.valueOf(System.getProperty("beatoraja.packagedGameTest.controllerBackend", "JINPUT")));
        config.setPlayername("smoke"); config.setPlayerpath("player");
        config.setBmsroot(new String[]{songs.toString()}); config.setTableURL(new String[0]);
        config.setSongpath(root.resolve("songdata.db").toString()); config.setTablepath("table");
        config.setUseSongInfo(false); config.setUpdatesong(false); config.setEnableIpfs(false); config.setUseDiscordRPC(false);
        config.setDisplaymode(Config.DisplayMode.WINDOW); config.setResolution(Resolution.SD);
        config.setMaxFramePerSecond(60); config.setVsync(false); config.setCacheSkinImage(false);
        config.setSongPreview(Config.SongPreview.NONE);
        config.setAudioConfig(new AudioConfig());
        config.getAudioConfig().setSystemvolume(0); config.getAudioConfig().setKeyvolume(0); config.getAudioConfig().setBgvolume(0);
        var player = new PlayerConfig(); player.setId("smoke"); player.setName("Generated test player");
        player.setIrconfig(new IRConfig[0]); player.setRequestEnable(false);
        var json = new Json(); json.setOutputType(JsonWriter.OutputType.json); json.setUsePrototypes(false);
        Files.writeString(root.resolve("config_sys.json"), json.prettyPrint(config));
        Files.createDirectories(root.resolve("player/smoke"));
        Files.writeString(root.resolve("player/smoke/config_player.json"), json.prettyPrint(player));
        var database = new SQLiteSongDatabaseAccessor(config.getSongpath(), config.getBmsroot());
        database.updateSongDatas(null, config.getBmsroot(), true, null);
        check(database.getSongDatas("sha256", new bms.model.BMSDecoder().decode(songs.resolve("smoke.bms")).getSHA256()).length == 1,
                "Generated chart registered in isolated DB");
    }

    static Path useFatJar(Path root, Path archive) throws Exception {
        try (var files = Files.list(root.resolve("lib"))) {
            for (Path file : files.toList()) Files.delete(file);
        }
        return Files.copy(archive, root.resolve("lib/beatoraja-all.jar"));
    }

    static Path verifyRoot(String value) throws Exception {
        Path root = Path.of(value).toRealPath();
        check(root.equals(Path.of("").toRealPath()), "Agent must run inside its generated distribution root");
        check(Files.readString(root.resolve("test-evidence/marker")).equals(MARKER), "Missing generated-test marker");
        return root;
    }

    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
