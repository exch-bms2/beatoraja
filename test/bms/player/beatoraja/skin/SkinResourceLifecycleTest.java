package bms.player.beatoraja.skin;

import java.util.concurrent.atomic.AtomicInteger;

import bms.player.beatoraja.Resolution;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Tests disposal of unused and shared loader resources. */
public final class SkinResourceLifecycleTest {
    public static void main(String[] args) {
        com.badlogic.gdx.utils.GdxNativesLoader.load();
        try (var gl = new MockTextureEnvironment()) {
            var header = new SkinHeader();
            header.setSourceResolution(Resolution.HD);
            header.setDestinationResolution(Resolution.HD);
            var skin = new Skin(header);
            var unused = gl.createTexture();
            int unusedHandle = unused.getTextureObjectHandle();
            skin.addResource(unused);
            var shared = gl.createTexture();
            int sharedHandle = shared.getTextureObjectHandle();
            skin.addResource(shared);
            skin.add(new SkinImage(new TextureRegion[] { new TextureRegion(shared) }, 0, 0));
            skin.addResource(() -> { throw new IllegalStateException("Expected disposal failure"); });
            AtomicInteger disposed = new AtomicInteger();
            skin.addResource(disposed::incrementAndGet);
            skin.dispose();
            skin.dispose();
            gl.assertDeletedOnce(unusedHandle);
            gl.assertDeletedOnce(sharedHandle);
            if (disposed.get() != 1) throw new AssertionError("Cleanup must continue after a failure");
            var bga = new bms.player.beatoraja.play.bga.BGAProcessor(
                    new bms.player.beatoraja.Config(), new bms.player.beatoraja.PlayerConfig());
            try {
                var field = bga.getClass().getDeclaredField("blanktex");
                field.setAccessible(true);
                int blankHandle = ((com.badlogic.gdx.graphics.Texture) field.get(bga)).getTextureObjectHandle();
                bga.dispose(); bga.dispose();
                gl.assertDeletedOnce(blankHandle);
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        }
        System.out.println("SkinResourceLifecycleTest passed");
    }
}
