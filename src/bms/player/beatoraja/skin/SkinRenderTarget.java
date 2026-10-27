package bms.player.beatoraja.skin;

import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.BufferUtils;

/** Reusable GL-thread scope for preview rendering, independent of window DPI and outer scissors. */
public final class SkinRenderTarget implements AutoCloseable {
    private static final ThreadLocal<SkinRenderTarget> CURRENT = new ThreadLocal<>();
    private final IntBuffer integers = BufferUtils.newIntBuffer(4);
    private final FloatBuffer floats = BufferUtils.newFloatBuffer(4);
    private final int[] viewport = new int[4], scissor = new int[4];
    private final float[] clearColor = new float[4];
    private SkinRenderTarget previous;
    private int framebuffer, width, height;
    private boolean clipping, active;

    public SkinRenderTarget begin() {
        if (active) throw new IllegalStateException("Render target already active");
        integers.clear(); Gdx.gl.glGetIntegerv(GL20.GL_FRAMEBUFFER_BINDING, integers); framebuffer = integers.get(0);
        read(GL20.GL_VIEWPORT, viewport); read(GL20.GL_SCISSOR_BOX, scissor);
        floats.clear(); Gdx.gl.glGetFloatv(GL20.GL_COLOR_CLEAR_VALUE, floats);
        for (int i = 0; i < 4; i++) clearColor[i] = floats.get(i);
        clipping = Gdx.gl.glIsEnabled(GL20.GL_SCISSOR_TEST);
        previous = CURRENT.get(); CURRENT.set(this); active = true;
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        return this;
    }

    public void bind(FrameBuffer buffer) {
        width = buffer.getWidth(); height = buffer.getHeight();
        buffer.begin();
    }

    static SkinRenderTarget current() { return CURRENT.get(); }
    int width() { return width; }
    int height() { return height; }

    private void read(int parameter, int[] values) {
        integers.clear(); Gdx.gl.glGetIntegerv(parameter, integers);
        for (int i = 0; i < 4; i++) values[i] = integers.get(i);
    }

    @Override public void close() {
        if (!active) return;
        Gdx.gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebuffer);
        Gdx.gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        Gdx.gl.glScissor(scissor[0], scissor[1], scissor[2], scissor[3]);
        if (clipping) Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST); else Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        CURRENT.set(previous); previous = null; active = false;
    }
}
