package bms.player.beatoraja.skin.lr2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.skin.MockTextureEnvironment;
import bms.player.beatoraja.skin.Skin;
import bms.player.beatoraja.skin.SkinHeader;
import com.badlogic.gdx.utils.IntIntMap;

/** Tests cleanup when an LR2 skin file cannot be read. */
public final class LR2SkinResourceLifecycleTest {
    public static void main(String[] args) throws IOException {
        Path directory = Files.createTempDirectory("lr2-skin-resource-test");
        try (var gl = new MockTextureEnvironment()) {
            var header = new SkinHeader();
            header.setSourceResolution(Resolution.HD);
            header.setDestinationResolution(Resolution.HD);
            var skin = new Skin(header);
            var texture = gl.createTexture();
            int handle = texture.getTextureObjectHandle();
            skin.addResource(texture);
            var loader = new LR2SkinCSVLoader<Skin>(Resolution.HD, new Config()) {
                @Override
                public Skin loadSkin(MainState state, SkinHeader ignored, IntIntMap options) throws IOException {
                    loadSkin0(skin, directory.resolve("missing.lr2skin"), state, options);
                    return skin;
                }
            };
            loader.skin = skin;
            try {
                loader.loadSkin(null, header, new IntIntMap());
                throw new AssertionError("Missing LR2 file must fail loading");
            } catch (IOException expected) {
            }
            gl.assertDeletedOnce(handle);
        } finally {
            Files.delete(directory);
        }
        System.out.println("LR2SkinResourceLifecycleTest passed");
    }
}
