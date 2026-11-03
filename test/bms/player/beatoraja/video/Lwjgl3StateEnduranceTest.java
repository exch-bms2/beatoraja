package bms.player.beatoraja.video;

import java.nio.ByteBuffer;
import java.nio.file.*;
import java.nio.file.Files;
import java.util.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.BufferUtils;
import bms.player.beatoraja.*;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import static bms.player.beatoraja.video.Lwjgl3BgaSkinIntegrationTest.*;

/** Synthetic SELECT/PLAY/RESULT lifetime stress, not automation of the real game or user database. */
public final class Lwjgl3StateEnduranceTest {
    private static final String[] STATES = {"MUSICSELECT", "PLAY", "RESULT"};
    private static final int LIMIT = 10000;
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]).toAbsolutePath(); Files.createDirectories(output);
        Path[] movies = {output.resolve("raw.avi"), output.resolve("compressed.avi")};
        generateMovie(movies[0], false); generateMovie(movies[1], true);
        var cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("beatoraja synthetic state endurance"); cfg.setWindowedMode(640, 480);
        cfg.disableAudio(true); cfg.setResizable(false); cfg.setForegroundFPS(120);
        var harness = new Harness(output, movies);
        new Lwjgl3Application(bms.player.beatoraja.build.GlTestDiagnostics.wrap(
                harness.injectFailure ? "endurance-failure-detector" : "endurance", harness), cfg);
        if (harness.injectFailure) {
            check(harness.failure instanceof AssertionError && harness.failure.getMessage().contains("previously visible"),
                    "Injected missing frame must fail the pixel detector");
            check(Files.isRegularFile(output.resolve("failure.png")), "Failure screenshot produced");
            System.out.println("Lwjgl3StateEnduranceTest passed expected missing-frame detection and failure cleanup");
            return;
        }
        if (harness.failure != null) throw new AssertionError("Endurance failure; inspect " + output, harness.failure);
        check(harness.completed, "Endurance run must complete");
        System.out.println("Lwjgl3StateEnduranceTest passed: cycles=" + harness.cycle + ", reports=" + output
                + "; synthetic states, real GL/video/font/FBO; no forced GC or heap-leak verdict");
    }

    private static final class Harness extends ApplicationAdapter {
        final Path output; final Path[] movies;
        final int cycles = Math.max(2, Math.min(LIMIT, Integer.getInteger("beatoraja.endurance.cycles", 20)));
        final int warmup = Math.max(0, Math.min(cycles - 2, Integer.getInteger("beatoraja.endurance.warmup", 3)));
        final long minimumNanos = Math.max(0, Math.min(86400, Integer.getInteger("beatoraja.endurance.seconds", 0))) * 1_000_000_000L;
        final int settleFrames = Math.max(2, Math.min(600, Integer.getInteger("beatoraja.endurance.settleFrames", 4)));
        final boolean injectFailure = Boolean.getBoolean("beatoraja.endurance.injectMissingFrame");
        final Map<String, double[]> series = new LinkedHashMap<>();
        final SkinRenderTarget target = new SkinRenderTarget();
        final MemoryDiagnostics diagnostics;
        SpriteBatch batch; SkinObjectRenderer renderer; Ledger ledger; CycleScene screen;
        ByteBuffer pixels; int pixelWidth, pixelHeight, phase, cycle, stableFrames, baseline, streams, selectedObjects = -1;
        long started, transitionStart, transitionNanos, firstFrameNanos = -1, deadline;
        boolean completed, disposed; Throwable failure;

        Harness(Path output, Path[] movies) {
            this.output = output; this.movies = movies; diagnostics = new MemoryDiagnostics(output.resolve("memory.csv"));
            for (String metric : new String[] {"jvm-heap", "jvm-heap-committed", "jvm-direct-buffers", "jvm-nonheap",
                    "gl-live-count", "gc-count", "gc-time-ms", "return-select-ns"}) series.put(metric, new double[LIMIT]);
            for (String state : STATES) series.put("transition-" + state + "-ns", new double[LIMIT + 1]);
        }
        @Override public void create() {
            try {
                ledger = new Ledger(); batch = new SpriteBatch();
                var camera = new OrthographicCamera(); camera.setToOrtho(false, 640, 480); batch.setProjectionMatrix(camera.combined);
                renderer = new SkinObjectRenderer(batch, camera);
                pixelWidth = Gdx.graphics.getBackBufferWidth(); pixelHeight = Gdx.graphics.getBackBufferHeight();
                pixels = BufferUtils.newByteBuffer(pixelWidth * pixelHeight * 4);
                baseline = ledger.liveCount(); streams = inputStreamCount(); started = System.nanoTime();
                // Scene uses the production transparent BGA layer resource path.
                var transparent = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
                try { transparent.setColor(0, 0, 0, 0); transparent.fill();
                    PixmapIO.writePNG(new com.badlogic.gdx.files.FileHandle(output.resolve("generated-transparent.png").toFile()), transparent);
                } finally { transparent.dispose(); }
                enter(0);
            } catch (Throwable e) { failure = e; dispose(); throw new AssertionError(e); }
        }
        private void enter(int next) throws Exception {
            transitionStart = System.nanoTime();
            if (screen != null) { screen.close(); screen = null; }
            check(ledger.liveCount() == baseline, "GL ownership must return to baseline between states: " + ledger.live);
            check(inputStreamCount() == streams, "JavaCV input streams must return to baseline");
            phase = next; stableFrames = 0; firstFrameNanos = -1;
            screen = new CycleScene(); screen.initialize();
            transitionNanos = System.nanoTime() - transitionStart;
            series.get("transition-" + STATES[phase] + "-ns")[cycle] = transitionNanos;
            diagnostics.record(STATES[phase], "transition", cycle, transitionNanos, -1, snapshot());
            deadline = System.nanoTime() + 20_000_000_000L;
        }
        @Override public void render() {
            try {
                check(System.nanoTime() < deadline, "Timed out waiting for visible pixels: cycle=" + cycle + ", state=" + STATES[phase]);
                Gdx.gl.glClearColor(0, 0, 0, 1); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                if (!(injectFailure && cycle == 1 && phase == 1 && stableFrames == 1)) screen.draw();
                check(Gdx.gl.glGetError() == GL20.GL_NO_ERROR, "GL error during state draw");
                check(ScissorStack.peekScissors() == null && !Gdx.gl.glIsEnabled(GL20.GL_SCISSOR_TEST), "Clipping restored after draw");
                pixels.clear(); Gdx.gl.glReadPixels(0, 0, pixelWidth, pixelHeight, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);
                boolean ready = color(80, 85, phase == 1 ? 0 : phase == 0 ? 1 : 2);
                if (phase == 1) ready &= color(260, 85, 0);
                if (firstFrameNanos < 0) {
                    if (!ready) return; // Decode readiness is asynchronous, not a missing-frame failure yet.
                    firstFrameNanos = System.nanoTime() - transitionStart;
                    diagnostics.record(STATES[phase], "first-visible", cycle, transitionNanos, firstFrameNanos, snapshot());
                }
                check(ready, "A previously visible marker/BGA disappeared");
                check(channel(15, 85, 0) < 10 && channel(15, 85, 1) < 10, "Outside clip stays black");
                int ink = 0;
                for (int y = 370; y < 410; y += 2) for (int x = 20; x < 200; x += 2)
                    if (channel(x, y, 0) + channel(x, y, 1) + channel(x, y, 2) > 100) ink++;
                check(ink > 20, "Font pixels disappeared");
                if (++stableFrames < settleFrames) return;
                var sample = snapshot();
                diagnostics.record(STATES[phase], "settled", cycle, transitionNanos, firstFrameNanos, sample);
                if (phase == 0) {
                    if (selectedObjects < 0) selectedObjects = ledger.liveCount();
                    check(ledger.liveCount() == selectedObjects, "Returned SELECT retains extra GL objects");
                    if (cycle > 0) sampleReturn(sample);
                    if (cycle == 0 || (cycle >= cycles && System.nanoTime() - started >= minimumNanos)) capture("select-" + cycle + ".png");
                    if (cycle >= LIMIT || (cycle >= cycles && System.nanoTime() - started >= minimumNanos)) {
                        check(System.nanoTime() - started >= minimumNanos, "Cycle safety limit reached before requested duration; increase settleFrames");
                        writeSummary(); completed = true; Gdx.app.exit(); return;
                    }
                }
                if (phase == 2) { cycle++; enter(0); } else enter(phase + 1);
            } catch (Throwable e) {
                failure = e; try { capture("failure.png"); } catch (Throwable extra) { e.addSuppressed(extra); }
                try { Files.writeString(output.resolve("failure.txt"), "cycle=" + cycle + "\nstate=" + STATES[phase] + "\n" + e + "\n"); }
                catch (Exception extra) { e.addSuppressed(extra); }
                Gdx.app.exit();
            }
        }
        private MemoryDiagnostics.Snapshot snapshot() {
            var base = MemoryDiagnostics.sample();
            var categories = new LinkedHashMap<>(base.categories());
            categories.put("test-gl-live", new MemoryDiagnostics.Category(ledger.liveCount(), 0, "object count; GPU bytes not measured"));
            if (screen != null && screen.play != null && screen.play.movie != null)
                categories.put("test-movie-retained", new MemoryDiagnostics.Category(1, screen.play.movie.getRetainedMovieBytes(), "synthetic movie resource"));
            else categories.put("test-movie-retained", new MemoryDiagnostics.Category(0, 0, "synthetic movie resource"));
            return new MemoryDiagnostics.Snapshot(categories, base.gcCount(), base.gcTimeMs());
        }
        private void sampleReturn(MemoryDiagnostics.Snapshot snapshot) {
            int index = cycle - 1;
            for (String metric : new String[] {"jvm-heap", "jvm-heap-committed", "jvm-direct-buffers", "jvm-nonheap"}) {
                var category = snapshot.categories().get(metric);
                series.get(metric)[index] = category == null ? -1 : category.bytes();
            }
            series.get("gl-live-count")[index] = ledger.liveCount();
            series.get("gc-count")[index] = snapshot.gcCount(); series.get("gc-time-ms")[index] = snapshot.gcTimeMs();
            series.get("return-select-ns")[index] = firstFrameNanos;
            System.out.println("Endurance return SELECT : cycle=" + cycle + ", glLive=" + ledger.liveCount()
                    + ", firstVisibleMs=" + firstFrameNanos / 1_000_000.0);
        }
        private void writeSummary() throws Exception {
            try (var writer = Files.newBufferedWriter(output.resolve("summary.csv"))) {
                writer.write("metric,count,first,last,min,max,mean,p95,slope_per_iteration,early_mean,late_mean\n");
                for (var entry : series.entrySet()) {
                    int count = cycle; // Include completed play/result cycles; initial SELECT transition is warmup.
                    double[] values = entry.getKey().equals("transition-MUSICSELECT-ns")
                            ? Arrays.copyOfRange(entry.getValue(), 1, count + 1) : Arrays.copyOf(entry.getValue(), count);
                    var s = EnduranceStatistics.summarize(values, Math.min(warmup, count - 1));
                    writer.write(String.format(Locale.ROOT, "%s,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                            entry.getKey(), s.count(), s.first(), s.last(), s.min(), s.max(), s.mean(), s.p95(), s.slope(), s.earlyMean(), s.lateMean()));
                }
            }
            Files.writeString(output.resolve("run.txt"), "synthetic=true\ncycles=" + cycle + "\nwarmup=" + warmup
                    + "\nseconds=" + (System.nanoTime() - started) / 1_000_000_000.0 + "\nforcedGc=false\nrssMeasured=false\ngpuBytesMeasured=false\n"
                    + "os=" + System.getProperty("os.name") + "\narch=" + System.getProperty("os.arch")
                    + "\njava=" + System.getProperty("java.version") + "\nframebuffer=" + pixelWidth + "x" + pixelHeight + "\n");
        }
        private int channel(int x, int y, int c) { return pixels.get(((y * pixelHeight / 480) * pixelWidth + x * pixelWidth / 640) * 4 + c) & 255; }
        private boolean color(int x, int y, int c) { return channel(x, y, c) > 170 && channel(x, y, (c + 1) % 3) < 70 && channel(x, y, (c + 2) % 3) < 70; }
        private void capture(String name) {
            var image = Pixmap.createFromFrameBuffer(0, 0, pixelWidth, pixelHeight);
            try { PixmapIO.writePNG(new com.badlogic.gdx.files.FileHandle(output.resolve(name).toFile()), image, -1, true); }
            finally { image.dispose(); }
        }
        @Override public void dispose() {
            if (disposed) return; disposed = true;
            try {
                if (screen != null) screen.close();
                if (batch != null) batch.dispose(); ShaderManager.dispose();
                if (ledger != null) check(ledger.liveCount() == 0, "All test-owned GL objects released on final exit");
            } finally { if (ledger != null) ledger.close(); }
        }

        private final class CycleScene implements AutoCloseable {
            Scene play; Skin skin; SkinTextFont text; SkinText.CachedTextLayout cached; FrameBuffer image;
            SkinFontSource fontSource;
            final List<Pixmap> fontPixmaps = new ArrayList<>();
            void initialize() throws Exception {
                if (phase == 1) { play = new Scene(); play.initialize(movies[cycle % movies.length], renderer); return; }
                var header = new SkinHeader(); header.setSourceResolution(Resolution.SD); header.setDestinationResolution(Resolution.SD);
                skin = new Skin(header);
                image = new FrameBuffer(Pixmap.Format.RGBA8888, 64, 48, false); skin.addResource(image);
                try (var scope = target.begin()) {
                    scope.bind(image); Gdx.gl.glClearColor(0, phase == 0 ? 1 : 0, phase == 2 ? 1 : 0, 1);
                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                }
                fontSource = new SkinFontSource("font/VL-Gothic-Regular.ttf", new String[0]); skin.addResource(fontSource);
                text = new SkinTextFont(fontSource, 0, 28, 1, state -> STATES[phase] + " \u3042"); skin.add(text);
                text.setDestination(0, 20, 370, 200, 28, 0, 255, 255, 255, 255, 1, 0, 0, 0, 0, 0, new int[0]);
                text.prepareFont(STATES[phase] + " \u3042"); text.prepare(0, null); text.setText(STATES[phase] + " \u3042");
                cached = text.createCachedTextLayout(text.getText());
                BitmapFont font = (BitmapFont)get(text, "font");
                Object packer = get(font.getData(), "packer");
                if (packer instanceof PixmapPacker p) for (var page : p.getPages()) fontPixmaps.add(page.getPixmap());
            }
            void draw() {
                if (play != null) { play.draw(batch, renderer, 0); return; }
                batch.begin();
                try {
                    renderer.setType(SkinObjectRenderer.TYPE_NORMAL); renderer.setColor(Color.WHITE);
                    check(renderer.pushClip(20, 40, 120, 90), "Synthetic state clip");
                    renderer.draw(image.getColorBufferTexture(), 0, 0, 200, 200); renderer.popClip();
                    text.draw(renderer); check(text.drawCachedTextLayout(renderer, cached, 280, 0), "Cached font remains drawable");
                } finally { batch.end(); }
            }
            @Override public void close() {
                if (skin != null) { skin.dispose(); skin.dispose(); }
                for (Pixmap atlas : fontPixmaps) check(atlas.isDisposed(), "UI font atlas Pixmap disposed");
                if (fontSource != null) check(!fontSource.isAvailable(), "UI shared font source released");
                if (play == null) return;
                if (play.skin != null) play.skin.dispose(); if (play.bga != null) play.bga.dispose();
                if (play.movie != null) play.movie.dispose();
                if (play.worker != null) {
                    try { play.worker.join(5000); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                    check(!play.worker.isAlive(), "Movie worker stopped on screen transition");
                    check(get(play.worker, "grabber") == null && get(play.worker, "movieBytes") == null
                            && get(play.worker, "pixmap") == null, "Native movie references cleared");
                }
                for (Pixmap atlas : play.fontPixmaps) check(atlas.isDisposed(), "Font atlas Pixmap disposed");
                if (play.imagePixmap != null) check(play.imagePixmap.isDisposed(), "Image-cache Pixmap disposed");
            }
        }
    }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
