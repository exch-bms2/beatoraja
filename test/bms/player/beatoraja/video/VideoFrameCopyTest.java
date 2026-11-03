package bms.player.beatoraja.video;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.GdxNativesLoader;
import org.bytedeco.javacv.Frame;
import bms.player.beatoraja.song.SongResources;

/** Padded/odd-width frame copies without an OpenGL context or external video. */
public final class VideoFrameCopyTest {
    public static void main(String[] args) throws Exception {
        GdxNativesLoader.load();
        var processor = new FFmpegProcessor(1);
        var worker = processor.new MovieSeekThread(SongResources.fromPath(Path.of("unused.avi")));
        var copy = worker.getClass().getDeclaredMethod("copyFrameToPixmap", Frame.class, Pixmap.class);
        copy.setAccessible(true);
        var prepare = worker.getClass().getDeclaredMethod("preparePixmapForDraw", Pixmap.class);
        prepare.setAccessible(true);
        for (int channels : new int[] {3, 4}) {
            var frame = new Frame();
            frame.imageWidth = 66; frame.imageHeight = 7; frame.imageChannels = channels;
            frame.imageStride = 66 * channels + 14;
            var bytes = ByteBuffer.allocateDirect(frame.imageStride * frame.imageHeight);
            frame.image = new java.nio.Buffer[] {bytes};
            for (int i = 0; i < bytes.capacity(); i++) bytes.put(i, (byte) 0xee);
            for (int y = 0; y < frame.imageHeight; y++) for (int x = 0; x < frame.imageWidth; x++) {
                int p = y * frame.imageStride + x * channels;
                bytes.put(p, (byte) x); bytes.put(p + 1, (byte) y); bytes.put(p + 2, (byte) 123);
            }
            var pixmap = new Pixmap(66, 7, Pixmap.Format.RGB888);
            try {
                for (int repeat = 0; repeat < 5; repeat++) {
                    copy.invoke(worker, frame, pixmap);
                    pixmap.getPixels().position(13);
                    prepare.invoke(worker, pixmap);
                    var pixels = pixmap.getPixels();
                    check(pixels.position() == 0 && pixels.remaining() == 66 * 7 * 3, "Upload bounds reset on every copy");
                    for (int y = 0; y < 7; y++) for (int x = 0; x < 66; x++) {
                        int p = (y * 66 + x) * 3;
                        check((pixels.get(p) & 255) == x && (pixels.get(p + 1) & 255) == y
                                && (pixels.get(p + 2) & 255) == 123, "No stride/padding bleed at " + x + "," + y);
                    }
                }
            } finally { pixmap.dispose(); }
        }
        processor.dispose();
        System.out.println("VideoFrameCopyTest passed (odd width, padded RGB/RGBA stride, repeated upload bounds)");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
