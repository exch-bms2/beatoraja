package bms.player.beatoraja.skin.json;

import java.nio.file.Path;

import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.lua.LuaSkinLoader;
import bms.player.beatoraja.skin.lua.SkinLuaAccessor;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.luaj.vm2.LuaValue;

/** Both destination setters must propagate the field, including Lua's reflective conversion. */
public final class OffsetCenterLoaderTest {
    public static void main(String[] args) throws Exception {
        com.badlogic.gdx.utils.GdxNativesLoader.load();
        var json = new Json();
        new JsonSkinSerializer(null, java.io.File::new).setSerializers(json, null, Path.of("skin/test.json"));
        var fromLua = LuaSkinLoader.class.getDeclaredMethod("fromLuaValue", Class.class, LuaValue.class);
        fromLua.setAccessible(true);
        var lua = new SkinLuaAccessor(false);
        var luaLoader = new LuaSkinLoader();
        try (var gl = new MockTextureEnvironment()) {
            for (int center : new int[]{-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}) {
                String fields = "\"id\":\"image\",\"center\":9,\"offsetcenter\":" + center
                        + ",\"dst\":[{\"time\":0,\"x\":100,\"y\":200,\"w\":40,\"h\":20}]";
                var jsonDst = json.fromJson(JsonSkin.Destination.class, "{" + fields + "}");
                var luaDst = (JsonSkin.Destination) fromLua.invoke(luaLoader, JsonSkin.Destination.class,
                        lua.exec("return {id='image',center=9,offsetcenter=" + center + ",dst={{time=0,x=100,y=200,w=40,h=20}}}"));
                verify(jsonDst, center >= 1 && center <= 9 ? center : 0, gl);
                verify(luaDst, center >= 1 && center <= 9 ? center : 0, gl);
            }
            verify(json.fromJson(JsonSkin.Destination.class, "{\"id\":\"image\"}"), 0, gl);
            verify((JsonSkin.Destination) fromLua.invoke(luaLoader, JsonSkin.Destination.class,
                    lua.exec("return {id='image'}")), 0, gl);
        } finally { lua.dispose(); luaLoader.lua.dispose(); }
        System.out.println("OffsetCenterLoaderTest passed (JSON/Lua conversion, root/nested destination setters, missing/invalid values)");
    }

    private static void verify(JsonSkin.Destination destination, int expected, MockTextureEnvironment gl) {
        var texture = gl.createTexture();
        var loader = new ExposedLoader(texture);
        var definition = new JsonSkin.Skin();
        var image = new JsonSkin.Image(); image.id = "image"; image.src = "source"; image.w = image.h = 16;
        definition.image = new JsonSkin.Image[]{image};
        definition.destination = new JsonSkin.Destination[]{destination};
        var header = new SkinHeader();
        header.setCustomOptions(new SkinHeader.CustomOption[0]);
        header.setCustomOffsets(new SkinHeader.CustomOffset[0]);
        Skin skin = loader.loadJsonSkin(header, definition, SkinType.DECIDE, new SkinConfig.Property(), Path.of("skin/test.json"));
        if (skin == null) throw new AssertionError("Skin failed to load");
        try {
            if (skin.getAllSkinObjects()[0].getOffsetCenter() != expected) throw new AssertionError("Root destination lost offsetcenter");
            var nested = new SkinImage(new TextureRegion(texture));
            new JsonDecideSkinObjectLoader(loader).setDestination(skin, nested, destination);
            if (nested.getOffsetCenter() != expected) throw new AssertionError("Nested destination lost offsetcenter");
            nested.dispose();
        } finally { skin.dispose(); }

        texture = gl.createTexture();
        loader = new ExposedLoader(texture);
        destination.id = "notes";
        var notes = new JsonSkin.NoteSet(); notes.id = "notes";
        for (String field : new String[]{"note", "mine", "lnstart", "lnend", "lnbody", "lnactive",
                "hcnstart", "hcnend", "hcnbody", "hcnactive", "hcndamage", "hcnreactive"}) {
            try { JsonSkin.NoteSet.class.getField(field).set(notes, new String[]{"image"}); }
            catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        }
        var lane = new JsonSkin.Animation(); lane.x = 100; lane.y = 100; lane.w = 40; lane.h = 600;
        notes.dst = new JsonSkin.Animation[]{lane}; notes.size = new float[]{8}; definition.note = notes;
        loader.sk = definition;
        var source = new JSONSkinLoader.SourceData("unused"); source.loaded = true; source.data = texture;
        loader.sourceMap = new java.util.HashMap<>(); loader.sourceMap.put("source", source);
        var playLoader = new JsonPlaySkinObjectLoader(loader);
        var playSkin = playLoader.getSkin(header);
        try {
            var note = playLoader.loadSkinObject(playSkin, definition, destination, Path.of("skin/test.json"));
            playSkin.add(note); playLoader.setDestination(playSkin, note, destination);
            if (!(note instanceof bms.player.beatoraja.play.SkinNote) || note.getOffsetCenter() != expected)
                throw new AssertionError("Note destination lost offsetcenter");
        } finally { playSkin.dispose(); texture.dispose(); }
    }
    private static final class ExposedLoader extends JSONSkinLoader {
        private final Texture texture;
        ExposedLoader(Texture texture) { this.texture = texture; }
        @Override protected Object getSource(String id, Path path) { registerResource(texture); return texture; }
    }
}
