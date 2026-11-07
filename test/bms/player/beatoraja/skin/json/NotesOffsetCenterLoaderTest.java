package bms.player.beatoraja.skin.json;

import java.nio.file.Path;
import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.play.SkinNote;
import bms.player.beatoraja.skin.Skin;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinType;
import bms.player.beatoraja.skin.lua.LuaSkinLoader;
import bms.player.beatoraja.skin.lua.SkinLuaAccessor;
import com.badlogic.gdx.utils.Json;
import org.luaj.vm2.LuaValue;

/** Real JSON skin object loading and the same Lua-table conversion used by LuaSkinLoader. */
public final class NotesOffsetCenterLoaderTest extends JSONSkinLoader {
    public static void main(String[] args) throws Exception {
        var loader = new NotesOffsetCenterLoaderTest();
        var json = new Json();
        json.setIgnoreUnknownFields(true);
        try {
            JsonSkin.NoteSet.class.getField("offsetcenter");
            throw new AssertionError("NoteSet must not define offsetcenter");
        } catch (NoSuchFieldException expected) { }
        var fromLua = LuaSkinLoader.class.getDeclaredMethod("fromLuaValue", Class.class, LuaValue.class);
        fromLua.setAccessible(true);
        var luaLoader = new LuaSkinLoader();
        var lua = new SkinLuaAccessor(false);
        try {
            for (int configured : new int[]{2, 5, 8, 0}) {
                var property = new SkinConfig.Property(); property.setNotesOffsetCenter(configured);
                for (int destinationCenter : new int[]{-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}) {
                    String text = "{note:{id:notes},destination:[{id:notes,offsetcenter:" + destinationCenter + "}]}";
                    var definition = json.fromJson(JsonSkin.Skin.class, text);
                    verify(loader, definition, property, configured, destinationCenter);
                    var luaDefinition = (JsonSkin.Skin) fromLua.invoke(luaLoader, JsonSkin.Skin.class,
                            lua.exec("return {note={id='notes'},destination={{id='notes',offsetcenter=" + destinationCenter + "}}}"));
                    verify(loader, luaDefinition, property, configured, destinationCenter);
                }
            }
            var old = json.fromJson(JsonSkin.Skin.class, "{note:{id:notes},destination:[{id:notes}]}");
            verify(loader, old, new SkinConfig.Property(), 0, 0);
            // Ignore the removed, briefly supported field just like other unknown JSON/Lua fields.
            var removedJson = json.fromJson(JsonSkin.Skin.class, "{note:{id:notes,offsetcenter:2},destination:[{id:notes,offsetcenter:8}]}");
            var removedLua = (JsonSkin.Skin) fromLua.invoke(luaLoader, JsonSkin.Skin.class,
                    lua.exec("return {note={id='notes',offsetcenter=2},destination={{id='notes',offsetcenter=8}}}"));
            for (var definition : new JsonSkin.Skin[]{removedJson, removedLua}) {
                var override = new SkinConfig.Property(); override.setNotesOffsetCenter(5);
                verify(loader, definition, override, 5, 8);
                verify(loader, definition, new SkinConfig.Property(), 0, 8);
            }
        } finally { lua.dispose(); luaLoader.lua.dispose(); }
        System.out.println("NotesOffsetCenterLoaderTest passed (JSON/Lua destination only, config override/default/invalid, removed field ignored, loader reuse, other objects)");
    }

    private static void verify(NotesOffsetCenterLoaderTest loader, JsonSkin.Skin definition,
            SkinConfig.Property property, int configured, int destinationCenter) {
        int expected = configured != 0 ? configured
                : destinationCenter >= 1 && destinationCenter <= 9 ? destinationCenter : 0;
        Skin skin = loader.loadJsonSkin(new SkinHeader(), definition, SkinType.PLAY_7KEYS, property, Path.of("skin/test.json"));
        check(skin != null, "Play skin loaded");
        try {
            var note = skin.getAllSkinObjects()[0];
            check(note instanceof SkinNote && note.getOffsetCenter() == expected, "Root note destination anchor");
            new JsonPlaySkinObjectLoader(loader).setDestination(skin, note, definition.destination[0]);
            check(note.getOffsetCenter() == expected, "Nested note destination anchor");
            var image = new bms.player.beatoraja.skin.SkinImage(new bms.player.beatoraja.skin.SkinSourceImage[0], 0);
            var imageDestination = new JsonSkin.Destination(); imageDestination.offsetcenter = 4;
            new JsonPlaySkinObjectLoader(loader).setDestination(skin, image, imageDestination);
            check(image.getOffsetCenter() == 4, "Other objects retain destination anchor");
            image.dispose();
            check(definition.destination[0].offsetcenter == destinationCenter, "Do not mutate reusable destination definitions");
        } finally { skin.dispose(); }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
