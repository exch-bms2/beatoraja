package bms.player.beatoraja.skin;

import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.play.PlaySkin;
import bms.player.beatoraja.skin.lua.LuaSkinLoader;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.badlogic.gdx.utils.Json;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public final class SkinCustomizationTest {
    public static void main(String[] args) throws Exception {
        var dir = Files.createTempDirectory("skin-selection-");
        try {
            var red = Files.writeString(dir.resolve("red.csv"), "red");
            var blue = Files.writeString(dir.resolve("blue.CSV"), "blue");
            Files.createDirectory(dir.resolve("folder.csv"));
            Files.writeString(dir.resolve("other.txt"), "other");
            var header = new SkinHeader();
            header.setSourceResolution(Resolution.HD);
            header.setDestinationResolution(Resolution.HD);
            var file = new SkinHeader.CustomFile("notes", dir.toString().replace('\\', '/') + "/*.csv|", "red");
            header.setCustomFiles(new SkinHeader.CustomFile[]{file});
            header.setCustomOptions(new SkinHeader.CustomOption[]{
                    new SkinHeader.CustomOption("layout", new int[]{1, 2}, new String[]{"Old", "New"})});
            check(SkinCustomization.fileChoices(file).equals(List.of("blue.CSV", "red.csv")), "Declared regular files only");
            check(SkinCustomization.defaultFile(file, SkinCustomization.fileChoices(file)).equals("red.csv"), "Default without extension");
            var prefixed = new com.badlogic.gdx.utils.ObjectMap<String, String>();
            prefixed.put(dir.toString().replace('\\', '/') + "/red*.csv", "red.csv");
            check(SkinLoader.getPath(prefixed.keys().next(), prefixed).toPath().equals(red), "Full names do not duplicate a wildcard prefix");

            var config = new SkinConfig("skin/play.luaskin");
            config.validate();
            var property = config.getProperties();
            property.setOption(new SkinConfig.Option[]{option(1)});
            property.setFile(new SkinConfig.FilePath[]{file("red.csv")});
            property.setNoteAdjustment(new int[]{4, 0, 0, 0});
            var json = new Json();
            property.setPendingSelection(selection(2, "blue.CSV"));
            config = json.fromJson(SkinConfig.class, json.toJson(config));
            config.validate();
            property = config.getProperties();
            check(property.getOption()[0].value == 1 && property.getFile()[0].path.equals("red.csv"), "Save leaves active settings intact");
            check(SkinLoader.loadPending(config, candidate -> {
                header.setSkinConfigProperty(candidate.getProperties());
                return null; // Lua/JSON loader rejected the candidate.
            }) == null, "Rejected candidate");
            check(property.getPendingSelection() == null && property.getOption()[0].value == 1
                    && property.getFile()[0].path.equals("red.csv"), "Load failure preserves active settings");

            property.setPendingSelection(selection(2, "blue.CSV"));
            var loaded = SkinLoader.loadPending(config, candidate -> {
                header.setSkinConfigProperty(candidate.getProperties());
                check(header.getCustomOptions()[0].getSelectedOption() == 2
                        && file.getSelectedFilename().equals("blue.CSV"), "Candidate choices reach the loader");
                return new PlaySkin(header);
            });
            check(loaded != null && property.getPendingSelection() == null && property.getOption()[0].value == 2
                    && property.getFile()[0].path.equals("blue.CSV") && property.getNoteAdjustment()[0] == 4, "Successful load commits only selected fields");
            loaded.dispose();

            Files.delete(blue);
            expectInvalid(header, property, selection(1, "blue.CSV"));
            expectInvalid(header, property, selection(3, "red.csv"));
            expectInvalid(header, property, selection(1, "../red.csv"));
            var random = property.candidateFor(selection(1, "Random"));
            header.setSkinConfigProperty(random);
            check(file.getSelectedFilename().equals(red.getFileName().toString()), "Random resolves only to valid candidates");
            verifyImageDecode(dir, header, property);
            verifyLuaRollback(dir);
        } finally {
            try (var paths = Files.walk(dir)) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("SkinCustomizationTest passed");
    }

    private static void verifyImageDecode(Path dir, SkinHeader header, SkinConfig.Property property) throws Exception {
        var oldFiles = Gdx.files;
        GdxNativesLoader.load();
        Gdx.files = new Lwjgl3Files();
        try {
            var file = new SkinHeader.CustomFile("notes", dir.toString().replace('\\', '/') + "/*.png", null);
            header.setCustomFiles(new SkinHeader.CustomFile[]{file});
            Files.writeString(dir.resolve("broken.png"), "not a PNG");
            expectInvalid(header, property, selection(1, "broken.png"));
            var pixmap = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
            try { PixmapIO.writePNG(new FileHandle(dir.resolve("good.png").toFile()), pixmap); }
            finally { pixmap.dispose(); }
            header.setSkinConfigProperty(property.candidateFor(selection(1, "good.png")));
            check(file.getSelectedFilename().equals("good.png"), "Valid image is decodable");
        } finally { Gdx.files = oldFiles; }
    }

    private static void verifyLuaRollback(Path dir) throws Exception {
        var path = Files.writeString(dir.resolve("play.luaskin"), """
                if skin_config and skin_config.option.layout == 2 then error('candidate rejected') end
                return {type=0, name='fixture', w=1280, h=720, destination={},
                        property={{name='layout', item={{name='Old', op=1}, {name='New', op=2}}}}}
                """);
        var config = new SkinConfig(path.toString());
        config.validate();
        var property = config.getProperties();
        property.setOption(new SkinConfig.Option[]{option(1)});
        property.setPendingSelection(new SkinConfig.Selection(new SkinConfig.Option[]{option(2)}, new SkinConfig.FilePath[0]));
        check(SkinLoader.loadPending(config, candidate -> new LuaSkinLoader().loadSkin(path, SkinType.PLAY_7KEYS,
                candidate.getProperties())) == null, "Actual Lua failure rejects pending choices");
        check(property.getOption()[0].value == 1 && property.getPendingSelection() == null, "Lua failure leaves original options");
        Skin original = new LuaSkinLoader().loadSkin(path, SkinType.PLAY_7KEYS, property);
        check(original instanceof PlaySkin, "Original Lua settings still load");
        original.dispose();
    }

    private static void expectInvalid(SkinHeader header, SkinConfig.Property property, SkinConfig.Selection selection) {
        try {
            header.setSkinConfigProperty(property.candidateFor(selection));
            throw new AssertionError("Invalid pending selection accepted");
        } catch (IllegalArgumentException expected) {
            // The active settings were not replaced.
        }
    }

    private static SkinConfig.Selection selection(int option, String file) {
        return new SkinConfig.Selection(new SkinConfig.Option[]{option(option)}, new SkinConfig.FilePath[]{file(file)});
    }

    private static SkinConfig.Option option(int value) {
        var option = new SkinConfig.Option();
        option.name = "layout";
        option.value = value;
        return option;
    }

    private static SkinConfig.FilePath file(String path) {
        var file = new SkinConfig.FilePath();
        file.name = "notes";
        file.path = path;
        return file;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
