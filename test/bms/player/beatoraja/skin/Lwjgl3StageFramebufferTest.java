package bms.player.beatoraja.skin;

import java.lang.reflect.Method;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.config.SkinConfiguration;
import bms.player.beatoraja.config.SkinPreview;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.StretchViewport;

/** Real production editor/preview paths, isolated from user databases, audio and external skins. */
public final class Lwjgl3StageFramebufferTest {
    public static void main(String[] args) {
        var cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("beatoraja Stage/FBO HiDPI test"); cfg.setWindowedMode(480, 270);
        cfg.disableAudio(true); cfg.setForegroundFPS(60);
        new Lwjgl3Application(bms.player.beatoraja.build.GlTestDiagnostics.wrap("stage-framebuffer", new Harness()), cfg);
        System.out.println("Lwjgl3StageFramebufferTest passed (editor pixels/hitboxes, resize/fullscreen, nested FBO/scissors and failure restoration)");
    }

    private static final class Harness extends ApplicationAdapter {
        MainState state;
        SkinTextInput editor;
        SkinText text;
        SpriteBatch batch;
        Texture white;
        Object windowMode;
        Method switching, toggle;
        int frames, verified;
        final AtomicReference<String> written = new AtomicReference<>();

        @Override public void create() {
            try {
                var main = KeyboardQueueTestSupport.allocate(MainController.class);
                var config = new Config(); config.setResolution(Resolution.SD);
                var input = KeyboardQueueTestSupport.input(new PlayModeConfig.KeyboardConfig(Mode.BEAT_7K, true));
                KeyboardQueueTestSupport.set(main, MainController.class, "config", config);
                KeyboardQueueTestSupport.set(main, MainController.class, "input", input);
                KeyboardQueueTestSupport.set(main, MainController.class, "timer", new TimerManager());
                state = new MainState(main, MainState.MainStateType.MUSICSELECT) {
                    @Override public void create() { }
                    @Override public void render() { }
                };
                text = new SkinText((bms.player.beatoraja.skin.property.StringProperty)null) {
                    @Override public void prepareFont(String value) { }
                    @Override protected void prepareText(String value) { }
                    @Override public void draw(Skin.SkinObjectRenderer renderer) { }
                    @Override public void draw(Skin.SkinObjectRenderer renderer, float x, float y) { }
                    @Override public void dispose() { }
                };
                text.region.set(80, 60, 160, 40); text.color.set(Color.WHITE);
                text.setText("HELLO"); text.setEditable(true); text.setWriter((s, value) -> written.set(value));
                editor = new SkinTextInput(bms.player.beatoraja.input.text.ImeBridge.NONE); editor.focus(state, text);
                check(state.getStage().getViewport() instanceof StretchViewport, "Production editor uses full stretched skin space");
                var processors = ((InputMultiplexer)Gdx.input.getInputProcessor()).getProcessors();
                check(processors.first() == input.getSkinMouseInput(), "Editor preserves skin mouse edge observer");
                batch = new SpriteBatch();
                var pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
                try { pixmap.setColor(Color.WHITE); pixmap.fill(); white = new Texture(pixmap); }
                finally { pixmap.dispose(); }
                var type = Class.forName("bms.player.beatoraja.WindowModeController");
                var create = type.getDeclaredMethod("create", Lwjgl3Graphics.class, Resolution.class); create.setAccessible(true);
                switching = type.getDeclaredMethod("isSwitching"); switching.setAccessible(true);
                toggle = type.getDeclaredMethod("toggle"); toggle.setAccessible(true);
                windowMode = create.invoke(null, (Lwjgl3Graphics)Gdx.graphics, Resolution.SD);
            } catch (Exception e) { throw new AssertionError(e); }
        }

        @Override public void resize(int w, int h) { if (state != null) state.resize(w, h); }

        @Override public void render() {
            if (windowMode == null || call(switching)) return;
            if (frames % 20 == 10) {
                try { verify(); } catch (Exception e) { throw new AssertionError(e); }
            }
            if (frames == 20) check(Gdx.graphics.setWindowedMode(400, 300), "Nonuniform resize");
            if (frames == 40) {
                if (Boolean.getBoolean("beatoraja.viewportTest.fullscreen")) check(call(toggle), "Fullscreen entry");
                else check(Gdx.graphics.setWindowedMode(520, 260), "Wide resize");
            }
            if (frames == 60) {
                if (Boolean.getBoolean("beatoraja.viewportTest.fullscreen")) check(call(toggle), "Window restoration");
                else check(Gdx.graphics.setWindowedMode(480, 270), "Window resize restoration");
            }
            if (++frames == 80) { check(verified == 4, "All viewport transitions verified"); Gdx.app.exit(); }
        }

        private void verify() throws Exception {
            System.out.println("Stage/FBO viewport : logical=" + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight()
                    + ", framebuffer=" + Gdx.graphics.getBackBufferWidth() + "x" + Gdx.graphics.getBackBufferHeight()
                    + ", fullscreen=" + Gdx.graphics.isFullscreen());
            var stage = state.getStage();
            state.updateStageViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            int screenWidth = stage.getViewport().getScreenWidth();
            state.resize(0, 0);
            check(stage.getViewport().getScreenWidth() == screenWidth, "Minimized zero size retains valid viewport");
            TextField field = (TextField)stage.getActors().first();
            check(field.isVisible() && stage.getKeyboardFocus() == field, "Focus survives resize");
            for (int skinX : new int[] {84, 236}) {
                int x = Math.round(skinX * Gdx.graphics.getWidth() / 640f);
                int y = Math.round(Gdx.graphics.getHeight() * (1 - 80 / 480f));
                var hit = stage.screenToStageCoordinates(new Vector2(x, y));
                check(stage.hit(hit.x, hit.y, true) == field, "Visible field edges match click range");
                check(stage.touchDown(x, y, 0, Input.Buttons.LEFT), "Field receives actual Stage click");
                stage.touchUp(x, y, 0, Input.Buttons.LEFT);
            }
            var outside = stage.screenToStageCoordinates(new Vector2(Math.round(72 * Gdx.graphics.getWidth() / 640f),
                    Math.round(Gdx.graphics.getHeight() * (1 - 80 / 480f))));
            check(stage.hit(outside.x, outside.y, true) != field, "Outside skin bounds is not a field hit");
            Gdx.gl.glClearColor(0, 0, 0, 1); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            stage.getViewport().apply(); stage.act(1 / 60f); stage.draw();
            int bw = Gdx.graphics.getBackBufferWidth(), bh = Gdx.graphics.getBackBufferHeight();
            var scissors = new com.badlogic.gdx.math.Rectangle();
            stage.calculateScissors(text.getInputBounds(), scissors);
            check(ScissorStack.pushScissors(scissors), "Stage field scissor push");
            try {
                int[] box = query(GL20.GL_SCISSOR_BOX);
                check(Math.abs(box[0] - bw * 80f / 640) <= 2 && Math.abs(box[1] - bh * 60f / 480) <= 2
                        && Math.abs(box[2] - bw * 160f / 640) <= 2 && Math.abs(box[3] - bh * 40f / 480) <= 2,
                        "Stage scissor agrees with physical field pixels, without double DPI scaling");
            } finally { ScissorStack.popScissors(); }
            Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, bw, bh);
            try {
                int ink = 0;
                for (int y = bh * 60 / 480; y < bh * 100 / 480; y++)
                    for (int x = bw * 80 / 640; x < bw * 240 / 640; x++) if ((pixels.getPixel(x, y) >>> 24) > 100) ink++;
                check(ink > 10, "Production font/cursor draw in expected input bounds");
            } finally { pixels.dispose(); }
            verifyPreview();
            check(Gdx.gl.glGetError() == GL20.GL_NO_ERROR, "No GL errors");
            field.setText("changed"); editor.commit();
            check("changed".equals(written.get()), "Property writer remains functional");
            text.setText("HELLO"); editor.focus(state, text);
            verified++;
        }

        private void verifyPreview() throws Exception {
            var header = new SkinHeader(); header.setSourceResolution(Resolution.SD); header.setDestinationResolution(Resolution.SD);
            var camera = new OrthographicCamera(); camera.setToOrtho(false, 640, 480);
            var inner = new Skin.SkinObjectRenderer(batch, camera);
            var previewSkin = new Skin(header) {
                @Override public void updateCustomObjects(MainState s) { }
                @Override public void drawAllObjectsSafely(SpriteBatch sprite, MainState s) {
                    var renderer = new Skin.SkinObjectRenderer(sprite, camera);
                    check(renderer.pushClip(160, 120, 320, 240), "FBO outer clip");
                    sprite.setColor(Color.RED); sprite.draw(white, 0, 0, 640, 480);
                    check(renderer.pushClip(240, 180, 80, 60), "FBO nested clip"); renderer.popClip();
                    check(!renderer.pushClip(500, 400, 10, 10), "Empty FBO intersection is rejected");
                    sprite.setColor(Color.GREEN); sprite.draw(white, 400, 250, 20, 20);
                    renderer.popClip(); sprite.setColor(Color.WHITE);
                }
            };
            var configuration = KeyboardQueueTestSupport.allocate(SkinConfiguration.class);
            KeyboardQueueTestSupport.set(configuration, SkinConfiguration.class, "selectedSkin", previewSkin);
            var preview = new SkinPreview();
            KeyboardQueueTestSupport.set(preview, SkinPreview.class, "configuration", configuration);
            Method render = SkinPreview.class.getDeclaredMethod("renderPreview", Skin.class); render.setAccessible(true);
            var parent = new FrameBuffer(Pixmap.Format.RGBA8888, 96, 64, false);
            try {
                batch.setProjectionMatrix(camera.combined); batch.begin();
                check(inner.pushClip(80, 60, 160, 120), "Outer window clip"); batch.end();
                var outerClip = ScissorStack.peekScissors();
                parent.begin(); Gdx.gl.glViewport(7, 9, 70, 40); Gdx.gl.glScissor(11, 8, 50, 25);
                Gdx.gl.glClearColor(.25f, .5f, .75f, 1);
                int[] viewport = query(GL20.GL_VIEWPORT), scissors = query(GL20.GL_SCISSOR_BOX);
                int binding = query(GL20.GL_FRAMEBUFFER_BINDING)[0];
                render.invoke(preview, previewSkin);
                check(Arrays.equals(query(GL20.GL_VIEWPORT), viewport) && query(GL20.GL_FRAMEBUFFER_BINDING)[0] == binding,
                        "Preview restores parent framebuffer and non-default viewport");
                check(Gdx.gl.glIsEnabled(GL20.GL_SCISSOR_TEST) && Arrays.equals(query(GL20.GL_SCISSOR_BOX), scissors)
                        && ScissorStack.peekScissors() == outerClip, "Preview isolates clipping and preserves outer ScissorStack");
                var field = SkinPreview.class.getDeclaredField("frameBuffer"); field.setAccessible(true);
                var framebuffer = (FrameBuffer)field.get(preview);
                try (var scope = new SkinRenderTarget().begin()) {
                    scope.bind(framebuffer);
                    Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, 640, 480);
                    try {
                        check((pixels.getPixel(320, 240) >>> 24) > 240, "FBO clip center red, independent of screen DPI");
                        check(pixels.getPixel(80, 240) == 0, "FBO outside clip transparent");
                        check(((pixels.getPixel(410, 260) >>> 16) & 255) > 240, "FBO nested clip restores parent");
                    } finally { pixels.dispose(); }
                }
                var badSkin = new Skin(header) {
                    @Override public void updateCustomObjects(MainState s) { throw new IllegalStateException("Expected render failure"); }
                };
                try { render.invoke(preview, badSkin); throw new AssertionError("Expected failure"); }
                catch (java.lang.reflect.InvocationTargetException expected) { check(expected.getCause() instanceof IllegalStateException, "Injected failure"); }
                check(Arrays.equals(query(GL20.GL_VIEWPORT), viewport) && query(GL20.GL_FRAMEBUFFER_BINDING)[0] == binding
                        && Arrays.equals(query(GL20.GL_SCISSOR_BOX), scissors), "Render failure restores GL target and clipping");
                var clear = BufferUtils.newFloatBuffer(4); Gdx.gl.glGetFloatv(GL20.GL_COLOR_CLEAR_VALUE, clear);
                check(clear.get(0) == .25f && clear.get(1) == .5f && clear.get(2) == .75f, "Preview restores caller clear color");
                badSkin.dispose();
                parent.end(); stageViewport();
                batch.begin(); inner.popClip(); batch.end();
            } finally { preview.dispose(); previewSkin.dispose(); parent.dispose(); }
        }

        private void stageViewport() { state.getStage().getViewport().apply(); }
        private boolean call(Method method) {
            try { return (boolean)method.invoke(windowMode); }
            catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        }
        @Override public void dispose() {
            editor.dispose(); state.dispose(); white.dispose(); batch.dispose(); ShaderManager.dispose();
        }
    }

    private static int[] query(int parameter) {
        IntBuffer values = BufferUtils.newIntBuffer(4); Gdx.gl.glGetIntegerv(parameter, values);
        return new int[] {values.get(0), values.get(1), values.get(2), values.get(3)};
    }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
