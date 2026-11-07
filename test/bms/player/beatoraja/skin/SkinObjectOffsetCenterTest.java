package bms.player.beatoraja.skin;

import java.util.ArrayList;
import java.util.List;

import bms.player.beatoraja.*;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;
import bms.player.beatoraja.skin.property.TimerProperty;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Rectangle;

/** Geometry and captured production draw calls, without a window or a real GL context. */
public final class SkinObjectOffsetCenterTest {
    public static void main(String[] args) throws Exception {
        var controller = KeyboardQueueTestSupport.allocate(MainController.class);
        var offsets = new SkinOffset[3];
        for (int i = 0; i < offsets.length; i++) offsets[i] = new SkinOffset();
        KeyboardQueueTestSupport.set(controller, MainController.class, "offset", offsets);
        var state = KeyboardQueueTestSupport.allocate(State.class);
        KeyboardQueueTestSupport.set(state, MainState.class, "main", controller);
        for (boolean animated : new boolean[]{false, true}) {
            for (boolean relative : new boolean[]{false, true}) {
                for (int center = 0; center <= 9; center++) {
                    for (int sign : new int[]{-1, 1}) {
                        offsets[1].x = 3; offsets[1].y = -4; offsets[1].w = 8 * sign; offsets[1].h = 4 * sign;
                        offsets[2].x = 2; offsets[2].y = 1; offsets[2].w = 4 * sign; offsets[2].h = 2 * sign;
                        var object = new Probe();
                        destination(object, 0, 100, 200, 40, 20, 0, 5);
                        object.setDestinationClip(0, new Rectangle(100, 200, 40, 20));
                        if (animated) {
                            destination(object, 100, 120, 240, 60, 40, 0, 5);
                            object.setDestinationClip(100, new Rectangle(120, 240, 60, 40));
                        }
                        object.setOffsetID(new int[]{1, 2});
                        object.setOffsetCenter(center); object.setRelative(relative);
                        float ax = center == 0 ? 0.5f : ((center - 1) % 3) * 0.5f;
                        float ay = center == 0 ? 0.5f : ((center - 1) / 3) * 0.5f;
                        float x = (animated ? 110 : 100) + (relative ? 0 : 5);
                        float y = (animated ? 220 : 200) + (relative ? 0 : -3);
                        if (!relative || center != 0) { x -= 12 * sign * ax; y -= 6 * sign * ay; }
                        for (int repeat = 0; repeat < 3; repeat++) {
                            object.prepare(50, state, 7, 9);
                            rectangle(object.region, x + 7, y + 9, (animated ? 50 : 40) + 12 * sign,
                                    (animated ? 30 : 20) + 6 * sign);
                            check(object.hasClip(), "Positive clip");
                            rectangle(object.getClip(), object.region.x, object.region.y, object.region.width, object.region.height);
                        }
                    }
                }
            }
        }
        var object = new Probe();
        destination(object, 0, 100, 200, 40, 20, 30, 9);
        object.setOffsetID(1); object.setOffsetCenter(2);
        object.prepare(0, state);
        try (var gl = new MockTextureEnvironment()) {
            var texture = gl.createTexture();
            try {
                object.image = new TextureRegion(texture);
                var batch = recordingBatch();
                object.draw(renderer(batch));
                var draw = batch.draws.get(0);
                near(draw.x, object.region.x); near(draw.y, object.region.y);
                near(draw.originX, object.region.width); near(draw.originY, object.region.height);
                near(draw.angle, 30);
            } finally { texture.dispose(); }
        }
        for (int invalid : new int[]{-1, 10, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            object.setOffsetCenter(invalid);
            check(object.getOffsetCenter() == 0, "Invalid centers fall back to legacy");
        }
        System.out.println("SkinObjectOffsetCenterTest passed (keypad anchors, legacy/relative, growth/shrink, summed offsets, interpolation, clips and independent rotation)");
    }

    public static void destination(SkinObject object, long time, float x, float y, float w, float h, int angle, int center) {
        object.setDestination(time, x, y, w, h, 0, 255, 255, 255, 255, 0, 0, angle, center,
                0, (TimerProperty) null, new int[0]);
    }

    public static RecordingBatch recordingBatch() throws Exception {
        var batch = KeyboardQueueTestSupport.allocate(RecordingBatch.class);
        batch.draws = new ArrayList<>(); batch.color = new Color(Color.WHITE);
        return batch;
    }

    public static SkinObjectRenderer renderer(RecordingBatch batch) throws Exception {
        var renderer = KeyboardQueueTestSupport.allocate(SkinObjectRenderer.class);
        KeyboardQueueTestSupport.set(renderer, SkinObjectRenderer.class, "sprite", batch);
        KeyboardQueueTestSupport.set(renderer, SkinObjectRenderer.class, "shaders", new ShaderProgram[6]);
        KeyboardQueueTestSupport.set(renderer, SkinObjectRenderer.class, "color", new Color(Color.WHITE));
        return renderer;
    }

    public record Draw(TextureRegion image, float x, float y, float width, float height,
                       float originX, float originY, float angle) { }

    public static final class RecordingBatch extends SpriteBatch {
        public List<Draw> draws;
        private Color color;
        @Override public Color getColor() { return color; }
        @Override public void setColor(Color value) { color.set(value); }
        @Override public void setShader(ShaderProgram shader) { }
        @Override public void setBlendFunction(int src, int dst) { }
        @Override public void draw(TextureRegion image, float x, float y, float w, float h) {
            draws.add(new Draw(image, x - 0.01f, y - 0.01f, w, h, 0, 0, 0));
        }
        @Override public void draw(TextureRegion image, float x, float y, float ox, float oy,
                                   float w, float h, float sx, float sy, float rotation) {
            draws.add(new Draw(image, x - 0.01f, y - 0.01f, w, h, ox, oy, rotation));
        }
    }

    private static final class Probe extends SkinObject {
        TextureRegion image;
        @Override public void draw(SkinObjectRenderer renderer) { draw(renderer, image); }
        @Override public void dispose() { }
    }
    private static final class State extends MainState {
        private State() { super(null, null); }
        @Override public void create() { }
        @Override public void render() { }
    }
    private static void rectangle(Rectangle r, float x, float y, float w, float h) {
        near(r.x, x); near(r.y, y); near(r.width, w); near(r.height, h);
    }
    public static void near(float value, float expected) {
        if (!Float.isFinite(value) || Math.abs(value - expected) > 0.001f) throw new AssertionError(value + " != " + expected);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
