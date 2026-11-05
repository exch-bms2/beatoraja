package bms.player.beatoraja.video;

import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.BufferUtils;
import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.play.SkinBGA;
import bms.player.beatoraja.play.bga.BGAProcessor;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.song.SongResources;

/** Real FFmpeg/GL integration, isolated from MainController, player databases and external skins. */
public final class Lwjgl3BgaSkinIntegrationTest {
    private static final int WIDTH = 640, HEIGHT = 480;
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]).toAbsolutePath();
        Files.createDirectories(output);
        Path fixture = output.resolve("generated-colors.avi");
        generateMovie(fixture, false);
        Path compressed = output.resolve("generated-compressed.avi");
        generateMovie(compressed, true);
        Path narrow = output.resolve("generated-narrow-compressed.avi");
        generateMovie(narrow, true, 66);
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("beatoraja BGA/skin resource integration test");
        config.setWindowedMode(WIDTH, HEIGHT); config.disableAudio(true); config.setForegroundFPS(120);
        var test = new Harness(new Path[] {fixture, compressed, narrow}, output, Integer.getInteger("beatoraja.glIntegration.cycles", 8));
        new Lwjgl3Application(bms.player.beatoraja.build.GlTestDiagnostics.wrap("bga-skin", test), config);
        if (test.failure != null) throw new AssertionError("GL integration failed", test.failure);
        check(test.completed, "Integration test must complete all phases");
        System.out.println("Lwjgl3BgaSkinIntegrationTest passed: " + test.cycles
                + " synthetic skin/BGA lifetimes; real pixels, restart/forward seek/loop, queued-upload disposal, GL ledger and native references");
    }

    static void generateMovie(Path path, boolean compressed) throws Exception {
        generateMovie(path, compressed, compressed ? 80 : 66);
    }

    private static void generateMovie(Path path, boolean compressed, int width) throws Exception {
        var recorder = new FFmpegFrameRecorder(path.toFile(), width, 48);
        recorder.setFormat("avi"); recorder.setVideoCodec(compressed ? AV_CODEC_ID_MPEG4 : AV_CODEC_ID_RAWVIDEO);
        recorder.setPixelFormat(compressed ? AV_PIX_FMT_YUV420P : AV_PIX_FMT_BGR24);
        recorder.setFrameRate(20); recorder.setVideoBitrate(compressed ? 400_000 : 2_000_000);
        try {
            recorder.start();
            var frame = new Frame(width, 48, Frame.DEPTH_UBYTE, 3);
            var pixels = (ByteBuffer) frame.image[0];
            for (int index = 0; index < 24; index++) {
                int channel = index < 8 ? 2 : index < 16 ? 1 : 0; // BGR red -> green -> blue.
                for (int y = 0; y < 48; y++) for (int x = 0; x < width; x++) {
                    int p = y * frame.imageStride + x * 3;
                    for (int c = 0; c < 3; c++) pixels.put(p + c, (byte) (c == channel ? 230 : 15));
                }
                recorder.record(frame);
            }
            recorder.stop();
        } finally { recorder.release(); }
    }


    private static final class Harness extends ApplicationAdapter {
        final Path[] fixtures;
        final Path output;
        final int cycles;
        final ConcurrentLinkedQueue<Runnable> held = new ConcurrentLinkedQueue<>();
        final AtomicBoolean holdUploads = new AtomicBoolean();
        SpriteBatch batch;
        SkinObjectRenderer renderer;
        Ledger ledger;
        Application app;
        Scene scene;
        int cycle, phase, baseline, inputStreamBaseline;
        long movieTime, deadline;
        boolean completed;
        boolean disposed;
        Throwable failure;

        Harness(Path[] fixtures, Path output, int cycles) {
            this.fixtures = fixtures; this.output = output; this.cycles = Math.max(2, Math.min(100, cycles));
        }
        @Override public void create() {
            try { initialize(); }
            catch (Throwable e) { failure = e; dispose(); throw new AssertionError(e); }
        }
        private void initialize() {
            app = Gdx.app;
            Gdx.app = (Application) Proxy.newProxyInstance(Application.class.getClassLoader(), new Class<?>[] {Application.class},
                    (proxy, method, values) -> {
                        if (method.getName().equals("postRunnable") && holdUploads.get()) { held.add((Runnable) values[0]); return null; }
                        return invoke(method, app, values);
                    });
            ledger = new Ledger();
            batch = new SpriteBatch();
            batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, WIDTH, HEIGHT));
            var camera = new OrthographicCamera(); camera.setToOrtho(false, WIDTH, HEIGHT);
            renderer = new SkinObjectRenderer(batch, camera);
            check(ShaderManager.getShader("bilinear") != null, "Bilinear shader compiles on the real GL backend");
            var transparent = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
            try {
                transparent.setColor(0, 0, 0, 0); transparent.fill();
                PixmapIO.writePNG(new com.badlogic.gdx.files.FileHandle(output.resolve("generated-transparent.png").toFile()), transparent);
            } finally { transparent.dispose(); }
            baseline = ledger.liveCount();
            inputStreamBaseline = inputStreamCount();
            newScene();
        }
        private void newScene() {
            scene = new Scene();
            try { scene.initialize(fixtures[cycle % fixtures.length], renderer); }
            catch (Exception e) { throw new AssertionError(e); }
            movieTime = 0; phase = 0; deadline = System.nanoTime() + 15_000_000_000L;
        }
        @Override public void render() {
            try { renderChecks(); }
            catch (Throwable e) {
                failure = e;
                try { capture("failure.png"); } catch (Throwable captureFailure) { e.addSuppressed(captureFailure); }
                try { dispose(); } catch (Throwable cleanup) { e.addSuppressed(cleanup); }
                app.exit();
            }
        }
        private void renderChecks() {
            check(System.nanoTime() < deadline, "Timed out: cycle=" + cycle + ", phase=" + phase + ", worker=" + scene.worker.getState());
            Gdx.gl.glClearColor(0, 0, 0, 1); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            scene.draw(batch, renderer, movieTime);
            check(Gdx.gl.glGetError() == GL20.GL_NO_ERROR, "No GL upload/draw errors");
            check(ScissorStack.peekScissors() == null && !Gdx.gl.glIsEnabled(GL20.GL_SCISSOR_TEST), "Scissor state restored");
            int wanted = switch (phase) { case 0, 3, 5, 7 -> 0; case 1 -> 1; default -> 2; };
            if (phase == 4) {
                if (scene.movie.getFrame(movieTime) == null) {
                    scene.movie.play(0, true); movieTime = 0; next(5);
                }
                return;
            }
            if (phase == 8) {
                if (held.isEmpty()) { movieTime += 100; return; }
                cleanupScene();
                if (++cycle == cycles) { completed = true; Gdx.app.exit(); }
                else newScene();
                return;
            }
            if (!hasColor(80, 85, wanted) || !hasColor(260, 85, wanted)) return;
            check(hasColor(138, 85, wanted), "Right edge has no RGB stride noise: cycle=" + cycle
                    + ", phase=" + phase + ", edge=" + Arrays.toString(pixel(138, 85))
                    + ", center=" + Arrays.toString(pixel(80, 85)));
            verifySkinPixels();
            Texture texture = scene.movie.getFrame(movieTime);
            check(texture != null && texture == scene.movie.getFrame(movieTime), "Multiple regions share the same decoded texture");
            var grabber = (org.bytedeco.javacv.FFmpegFrameGrabber)get(scene.worker, "grabber");
            check(texture.getWidth() == grabber.getImageWidth() && texture.getHeight() == grabber.getImageHeight(),
                    "Buffer alignment must not resize or pad the displayed texture");
            if (phase == 0 && (cycle == 0 || cycle == cycles - 1)) capture("scene-" + cycle + ".png");
            switch (phase) {
                case 0 -> { movieTime = 550; next(1); }
                case 1 -> { movieTime = 950; next(2); }
                case 2 -> { scene.bga.prepare(null); scene.bga.prepareBGA(-1); movieTime = 0; next(3); }
                case 3 -> { scene.bga.stop(); next(4); }
                case 5 -> { movieTime = 950; next(6); }
                case 6 -> { movieTime = 1600; next(7); }
                case 7 -> { holdUploads.set(true); movieTime += 550; next(8); }
            }
        }
        private void next(int phase) { this.phase = phase; deadline = System.nanoTime() + 10_000_000_000L; }
        private void verifySkinPixels() {
            check(pixel(15, 290)[0] < 10, "Outside destination clip stays black");
            check(pixel(50, 290)[0] > 230, "Destination clip interior visible");
            check(pixel(145, 290)[0] < 10, "Destination clip right edge respected");
            check(pixel(190, 290)[0] > 230, "Pop restores unrestricted drawing");
            check(hasColor(325, 290, 0) && hasColor(375, 290, 2), "Bilinear shader receives texture dimensions");
            check(nonBlack(20, 370, 180, 40) > 30, "Latin/Japanese/supplementary font pixels visible");
            check(nonBlack(300, 370, 180, 40) > 30, "Cached font layout pixels visible");
            int maxGreen = 0, maxRed = 0;
            for (int y = 370; y < 410; y += 2) for (int x = 300; x < 480; x += 2) {
                int[] p = pixel(x, y); maxRed = Math.max(maxRed, p[0]); maxGreen = Math.max(maxGreen, p[1]);
            }
            check(maxGreen > 90 && maxGreen < 170 && maxRed < 20, "Cached glyph tint/alpha reaches real framebuffer");
        }
        private void cleanupScene() {
            int[] textures = ledger.textureNames();
            scene.skin.dispose(); scene.skin.dispose();
            scene.bga.dispose(); scene.bga.dispose();
            scene.movie.dispose();
            try { scene.worker.join(5000); } catch (InterruptedException e) { throw new AssertionError(e); }
            check(!scene.worker.isAlive(), "Movie worker exits on transition");
            check(get(scene.worker, "grabber") == null && get(scene.worker, "pixmap") == null
                    && get(scene.worker, "movieBytes") == null && get(scene.worker, "frameRow") == null
                    && get(scene.worker, "converter") == null,
                    "Native grabber/Pixmap and compressed/copy buffers released");
            for (Pixmap p : scene.fontPixmaps) check(p.isDisposed(), "Font atlas backing pixmap freed");
            check(scene.imagePixmap.isDisposed(), "BGA image-cache backing Pixmap freed");
            check(!scene.fontSource.isAvailable(), "Shared font source released by skin owner");
            check(scene.movie.getRetainedMovieBytes() == 0 && scene.movie.getFrame(0) == null, "Disposed movie is inactive");
            check(inputStreamCount() == inputStreamBaseline, "JavaCV's static input-stream registry returns to baseline");
            holdUploads.set(false);
            Runnable upload;
            while ((upload = held.poll()) != null) upload.run();
            for (int texture : textures) check(!Gdx.gl.glIsTexture(texture), "Deleted texture no longer exists in GL: " + texture);
            check(ledger.liveCount() == baseline, "Transition leaks GL objects: " + ledger.live);
            check(Gdx.gl.glGetError() == GL20.GL_NO_ERROR, "Queued upload after disposal is harmless");
            System.out.println("GL transition verified : cycle=" + cycle + ", liveObjects=" + ledger.liveCount());
        }
        private boolean hasColor(int x, int y, int channel) {
            int[] p = pixel(x, y);
            return p[channel] > 170 && p[(channel + 1) % 3] < 70 && p[(channel + 2) % 3] < 70;
        }
        private int[] pixel(int x, int y) {
            var p = BufferUtils.newByteBuffer(4);
            Gdx.gl.glReadPixels(x * Gdx.graphics.getBackBufferWidth() / WIDTH,
                    y * Gdx.graphics.getBackBufferHeight() / HEIGHT, 1, 1, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, p);
            return new int[] {p.get(0) & 255, p.get(1) & 255, p.get(2) & 255};
        }
        private int nonBlack(int x, int y, int width, int height) {
            int count = 0;
            for (int py = y; py < y + height; py += 2) for (int px = x; px < x + width; px += 2) {
                int[] p = pixel(px, py); if (p[0] + p[1] + p[2] > 100) count++;
            }
            return count;
        }
        private void capture(String name) {
            var p = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
            try { PixmapIO.writePNG(new com.badlogic.gdx.files.FileHandle(output.resolve(name).toFile()), p, -1, true); }
            finally { p.dispose(); }
        }
        @Override public void dispose() {
            if (disposed) return;
            disposed = true;
            try {
                if (scene != null) {
                    if (scene.skin != null) scene.skin.dispose();
                    if (scene.bga != null) scene.bga.dispose();
                    if (scene.worker != null) {
                        try { scene.worker.join(5000); } catch (InterruptedException e) { throw new AssertionError(e); }
                        check(!scene.worker.isAlive(), "Failure cleanup also stops movie worker");
                    }
                }
                holdUploads.set(false);
                Runnable runnable; while ((runnable = held.poll()) != null) runnable.run();
                if (batch != null) batch.dispose();
                ShaderManager.dispose();
                if (completed) check(ledger.liveCount() == 0, "Final test-owned GL objects all released: " + ledger.live);
            } finally { if (ledger != null) ledger.close(); Gdx.app = app; }
        }
    }

    static final class Scene {
        Skin skin;
        BGAProcessor bga;
        FFmpegProcessor movie;
        Thread worker;
        SkinFontSource fontSource;
        final List<Pixmap> fontPixmaps = new ArrayList<>();
        Pixmap imagePixmap;
        SkinTextFont text;
        SkinText.CachedTextLayout cached;
        final SkinBGA first = new SkinBGA(Config.BGAEXPAND_FULL), second = new SkinBGA(Config.BGAEXPAND_FULL);
        SkinImage clipped;
        Texture white;
        Texture bilinearPattern;
        Method drawObject;
        void initialize(Path fixture, SkinObjectRenderer renderer) throws Exception {
            var config = new Config(); config.setFrameskip(1);
            bga = new BGAProcessor(config, new PlayerConfig());
            var model = new BMSModel(); model.setBgaList(new String[] {fixture.getFileName().toString(), "generated-transparent.png"});
            var line = new TimeLine(0, 0, Mode.BEAT_7K.key); line.setBGA(0); line.setLayer(1);
            model.setAllTimeLine(new TimeLine[] {line});
            bga.setModel(model, SongResources.fromPath(fixture.resolveSibling("synthetic.bms")));
            bga.prepare(null);
            bga.prepareBGA(-1);
            imagePixmap = ((Pixmap[]) get(get(bga, "cache"), "bgamap"))[1];
            check(imagePixmap != null && !imagePixmap.isDisposed(), "Production image cache owns a native Pixmap");
            movie = (FFmpegProcessor) ((VideoProcessor[]) get(bga, "movies"))[0];
            check(movie != null, "Fixture loaded by production BGA resource path");
            worker = (Thread) get(movie, "movieseek");
            var header = new SkinHeader(); header.setSourceResolution(Resolution.SD); header.setDestinationResolution(Resolution.SD);
            skin = new Skin(header);
            set(skin, "renderer", renderer);
            drawObject = Skin.class.getDeclaredMethod("drawObject", SkinObject.class); drawObject.setAccessible(true);
            fontSource = new SkinFontSource("font/VL-Gothic-Regular.ttf", new String[0]); skin.addResource(fontSource);
            text = new SkinTextFont(fontSource, 0, 28, 1, state -> "AB\u3042\uD83D\uDE00");
            text.setDestination(0, 20, 370, 200, 28, 0, 255, 255, 255, 255, 1, 0, 0, 0, 0, 0, new int[0]);
            text.prepareFont("AB\u3042\uD83D\uDE00"); skin.add(text); text.prepare(0, null);
            BitmapFont font = (BitmapFont) get(text, "font");
            Object packer = get(font.getData(), "packer");
            if (packer instanceof PixmapPacker p) for (var page : p.getPages()) fontPixmaps.add(page.getPixmap());
            text.setText("AB\u3042\uD83D\uDE00"); cached = text.createCachedTextLayout(text.getText());
            var pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            try { pixmap.setColor(Color.WHITE); pixmap.fill(); white = new Texture(pixmap); }
            finally { pixmap.dispose(); }
            var pattern = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
            try {
                pattern.setColor(Color.RED); pattern.fill();
                pattern.setColor(Color.BLUE); pattern.fillRectangle(2, 0, 2, 4);
                bilinearPattern = new Texture(pattern); skin.addResource(bilinearPattern);
            } finally { pattern.dispose(); }
            clipped = new SkinImage(new TextureRegion(white)); skin.add(clipped); skin.addResource(white);
            clipped.setDestination(0, 0, 270, 160, 60, 0, 255, 255, 255, 255, 1, 0, 0, 0, 0, 0, new int[0]);
            clipped.setDestinationClip(0, new Rectangle(30, 270, 100, 60)); clipped.prepare(0, null);
            first.color.set(Color.WHITE); first.region.set(20, 40, 120, 90);
            second.color.set(Color.WHITE); second.region.set(200, 40, 120, 90);
        }
        void draw(SpriteBatch batch, SkinObjectRenderer renderer, long time) {
            batch.begin();
            try {
                bga.prepareBGA(time); bga.drawBGA(first, renderer, first.region); bga.drawBGA(second, renderer, second.region);
                renderer.setType(SkinObjectRenderer.TYPE_NORMAL); renderer.setColor(Color.WHITE);
                invoke(drawObject, skin, new Object[] {clipped});
                check(renderer.pushClip(180, 270, 50, 60), "Outer nested scissor");
                check(renderer.pushClip(200, 280, 10, 10), "Inner nested scissor");
                renderer.popClip(); renderer.draw(white, 180, 270, 50, 60); renderer.popClip();
                renderer.setType(SkinObjectRenderer.TYPE_BILINEAR); renderer.draw(bilinearPattern, 320, 270, 60, 60);
                text.draw(renderer);
                text.color.set(0, 1, 0, .5f);
                check(text.drawCachedTextLayout(renderer, cached, 280, 0), "Production cached layout still valid");
                text.color.set(Color.WHITE);
            } finally { batch.end(); }
        }
    }

    /** Forwarding real GL, not a mock. GL names can be reused, so track live ownership rather than historic IDs. */
    static final class Ledger implements AutoCloseable {
        final GL20 original = Gdx.gl, original20 = Gdx.gl20;
        final Set<String> live = new HashSet<>();
        Ledger() {
            GL20 forwarded = (GL20) Proxy.newProxyInstance(GL20.class.getClassLoader(), new Class<?>[] {GL20.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        String kind = switch (name) {
                            case "glGenTexture", "glDeleteTexture" -> "texture";
                            case "glGenBuffer", "glDeleteBuffer" -> "buffer";
                            case "glCreateProgram", "glDeleteProgram" -> "program";
                            case "glCreateShader", "glDeleteShader" -> "shader";
                            case "glGenFramebuffer", "glDeleteFramebuffer" -> "framebuffer";
                            case "glGenRenderbuffer", "glDeleteRenderbuffer" -> "renderbuffer";
                            default -> null;
                        };
                        if (kind != null && name.startsWith("glDelete") && args.length == 1 && args[0] instanceof Integer id && id > 0)
                            check(live.remove(kind + id), "Duplicate/unowned GL delete: " + kind + id);
                        Object result = invoke(method, original, args);
                        if (kind != null && (name.startsWith("glGen") || name.startsWith("glCreate")) && result instanceof Integer id && id > 0)
                            check(live.add(kind + id), "Duplicate live GL name: " + kind + id);
                        return result;
                    });
            Gdx.gl = forwarded; Gdx.gl20 = forwarded;
        }
        int liveCount() { return live.size(); }
        int[] textureNames() {
            return live.stream().filter(s -> s.startsWith("texture")).mapToInt(s -> Integer.parseInt(s.substring(7))).toArray();
        }
        @Override public void close() { Gdx.gl = original; Gdx.gl20 = original20; }
    }
    static Object get(Object target, String name) {
        try { var f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    static int inputStreamCount() {
        try {
            var f = org.bytedeco.javacv.FFmpegFrameGrabber.class.getDeclaredField("inputStreams");
            f.setAccessible(true);
            return ((Map<?, ?>) f.get(null)).size();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void set(Object target, String name, Object value) throws Exception {
        var f = target.getClass().getDeclaredField(name); f.setAccessible(true); f.set(target, value);
    }
    private static Object invoke(Method method, Object target, Object[] args) {
        try { return method.invoke(target, args); }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) throw r;
            if (e.getCause() instanceof Error error) throw error;
            throw new AssertionError(e.getCause());
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
