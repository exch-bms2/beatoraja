package bms.player.beatoraja.video;

import java.nio.ByteBuffer;
import org.bytedeco.javacpp.avutil.AVFrame;
import org.bytedeco.javacv.Frame;
import static org.bytedeco.javacpp.avutil.*;

/** Native conversion regression: no display/audio, but requires matching existing FFmpeg natives. */
public final class BgrFrameConverterTest {
    public static void main(String[] args) throws Exception {
        try (var converter = new BgrFrameConverter()) {
            for (int width : new int[] {1, 2, 7, 9, 16, 17, 31, 33, 64, 66, 67, 70, 80, 641})
                for (int format : new int[] {AV_PIX_FMT_RGB24, AV_PIX_FMT_BGR24, AV_PIX_FMT_BGRA, AV_PIX_FMT_GRAY8})
                    checkPacked(converter, width, 5, format, false);
            checkPacked(converter, 66, 7, AV_PIX_FMT_RGB24, true);
            for (int width = 2; width <= 80; width += 2) checkYuv(converter, width, AV_PIX_FMT_YUV420P);
            checkYuv(converter, 66, AV_PIX_FMT_YUV444P);
            checkYuv(converter, 66, AV_PIX_FMT_YUVJ420P);
            checkYuv(converter, 66, AV_PIX_FMT_NV12);
        }
        var closed = new BgrFrameConverter(); closed.close(); closed.close();
        try { closed.convert(new Frame()); throw new AssertionError(); } catch (IllegalStateException expected) { }
        try (var invalid = new BgrFrameConverter()) {
            try { invalid.convert(new Frame()); throw new AssertionError(); } catch (IllegalArgumentException expected) { }
            AVFrame raw = av_frame_alloc();
            try {
                raw.width(Integer.MAX_VALUE).height(Integer.MAX_VALUE).format(AV_PIX_FMT_BGR24);
                var source = new Frame(); source.opaque = raw;
                try { invalid.convert(source); throw new AssertionError(); } catch (IllegalArgumentException expected) { }
            } finally { av_frame_free(raw); }
        }
        System.out.println("BgrFrameConverterTest passed (small/odd sizes, packed/gray/YUV, negative stride, reuse, format changes and close)");
    }

    private static AVFrame allocate(int width, int height, int format) {
        AVFrame source = av_frame_alloc(); source.width(width).height(height).format(format);
        if (av_frame_get_buffer(source, 32) < 0) throw new AssertionError("Input allocation");
        return source;
    }
    private static void checkPacked(BgrFrameConverter converter, int width, int height, int format, boolean flipped) {
        AVFrame source = allocate(width, height, format), view = null;
        try {
            int channels = format == AV_PIX_FMT_BGRA ? 4 : format == AV_PIX_FMT_GRAY8 ? 1 : 3;
            ByteBuffer input = source.data(0).capacity((long)source.linesize(0) * height).asByteBuffer();
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                int p = y * source.linesize(0) + x * channels, red = x % 180, green = y * 20, blue = 96;
                if (format == AV_PIX_FMT_GRAY8) input.put(p, (byte)red);
                else {
                    input.put(p, (byte)(format == AV_PIX_FMT_RGB24 ? red : blue)); input.put(p + 1, (byte)green);
                    input.put(p + 2, (byte)(format == AV_PIX_FMT_RGB24 ? blue : red));
                    if (channels == 4) input.put(p + 3, (byte)127);
                }
            }
            AVFrame borrowed = source;
            if (flipped) {
                view = av_frame_alloc(); view.width(width).height(height).format(format);
                view.data(0, source.data(0).position((long)(height - 1) * source.linesize(0))).linesize(0, -source.linesize(0));
                borrowed = view;
            }
            var frame = new Frame(); frame.opaque = borrowed; frame.timestamp = 123456; frame.keyFrame = true;
            Frame result = converter.convert(frame); var output = (ByteBuffer)result.image[0];
            check(result.imageWidth == width && result.imageHeight == height && result.imageChannels == 3, "Dimensions/channels unchanged");
            check(result.timestamp == 123456 && result.keyFrame, "Metadata unchanged");
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                int p = y * result.imageStride + x * 3, red = x % 180, green = (flipped ? height - 1 - y : y) * 20;
                int blue = format == AV_PIX_FMT_GRAY8 ? red : 96;
                if (format == AV_PIX_FMT_GRAY8) green = red;
                check((output.get(p) & 255) == blue && (output.get(p + 1) & 255) == green && (output.get(p + 2) & 255) == red,
                        "Packed conversion: width=" + width + ", format=" + format + ", x=" + x + ", y=" + y);
            }
            check(converter.convert(frame) == result && result.image[0] == output, "Reuse frame/buffer for stable layout");
        } finally { if (view != null) av_frame_free(view); av_frame_free(source); }
    }
    private static void checkYuv(BgrFrameConverter converter, int width, int format) {
        AVFrame source = allocate(width, 48, format);
        try {
            for (int plane = 0; plane < (format == AV_PIX_FMT_NV12 ? 2 : 3); plane++) {
                int height = plane == 0 || format == AV_PIX_FMT_YUV444P ? 48 : 24, value = plane == 0 ? 81 : plane == 1 ? 90 : 240;
                var bytes = source.data(plane).capacity((long)source.linesize(plane) * height).asByteBuffer();
                for (int p = 0; p < bytes.capacity(); p++) bytes.put(p, (byte)(format == AV_PIX_FMT_NV12 && plane == 1 ? p % 2 == 0 ? 90 : 240 : value));
            }
            var frame = new Frame(); frame.opaque = source;
            var result = converter.convert(frame); var bytes = (ByteBuffer)result.image[0];
            for (int y = 0; y < 48; y++) for (int x = 0; x < width; x++) {
                int p = y * result.imageStride + x * 3;
                check((bytes.get(p) & 255) < 20 && (bytes.get(p + 1) & 255) < 20 && (bytes.get(p + 2) & 255) > 220,
                        "YUV conversion: width=" + width + ", stride=" + result.imageStride + ", x=" + x);
            }
        } finally { av_frame_free(source); }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
