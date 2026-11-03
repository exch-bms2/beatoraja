package bms.player.beatoraja.skin.json;

import java.nio.file.Path;

import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.skin.MockTextureEnvironment;
import bms.player.beatoraja.skin.Skin;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinType;
import bms.player.beatoraja.skin.lua.SkinLuaAccessor;
import com.badlogic.gdx.graphics.Texture;

/** Tests cleanup when an image loads but its destination is invalid. */
public final class JSONSkinResourceLifecycleTest {
    public static void main(String[] args) {
        assertLuaLifecycle();
        try (var gl = new MockTextureEnvironment()) {
            Texture texture = gl.createTexture();
            int handle = texture.getTextureObjectHandle();
            var loader = new JSONSkinLoader() {
                @Override
                protected Object getSource(String id, Path path) {
                    registerResource(texture);
                    return texture;
                }
            };
            var skin = new JsonSkin.Skin();
            var image = new JsonSkin.Image();
            image.id = "image";
            image.src = "source";
            skin.image = new JsonSkin.Image[] { image };
            var destination = new JsonSkin.Destination();
            destination.id = "image";
            destination.dst = null;
            skin.destination = new JsonSkin.Destination[] { destination };
            var header = new SkinHeader();
            header.setCustomOptions(new SkinHeader.CustomOption[0]);
            header.setCustomOffsets(new SkinHeader.CustomOffset[0]);
            Skin result = loader.loadJsonSkin(header, skin, SkinType.DECIDE,
                    new SkinConfig.Property(), Path.of("skin/test.json"));
            if (result != null) throw new AssertionError("Invalid destination must fail skin loading");
            gl.assertDeletedOnce(handle);
        }
        System.out.println("JSONSkinResourceLifecycleTest passed");
    }

    private static void assertLuaLifecycle() {
        int[] disposed = {0};
        var lua = new SkinLuaAccessor(false) {
            @Override public void dispose() { disposed[0]++; super.dispose(); }
        };
        var loader = new JSONSkinLoader(lua);
        var definition = new JsonSkin.Skin();
        definition.source = new JsonSkin.Source[0];
        definition.destination = new JsonSkin.Destination[0];
        var header = new SkinHeader();
        header.setCustomOptions(new SkinHeader.CustomOption[0]);
        header.setCustomOffsets(new SkinHeader.CustomOffset[0]);
        Skin loaded = loader.loadJsonSkin(header, definition, SkinType.DECIDE,
                new SkinConfig.Property(), Path.of("skin/test.json"));
        if (loaded == null) throw new AssertionError("Empty skin should load");
        if (disposed[0] != 0) throw new AssertionError("Lua must remain alive while skin is loaded");
        loaded.dispose();
        loaded.dispose();
        if (disposed[0] != 1) throw new AssertionError("Skin must release Lua exactly once");
    }
}
