package bms.player.beatoraja.video;

import java.nio.Buffer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.avutil.AVFrame;
import org.bytedeco.javacpp.swscale.SwsContext;
import org.bytedeco.javacv.Frame;
import static org.bytedeco.javacpp.avutil.*;
import static org.bytedeco.javacpp.swscale.*;

/** Worker-owned, aligned BGR24 conversion. Input AVFrames are borrowed until the next grab. */
final class BgrFrameConverter implements AutoCloseable {
    private AVFrame destination;
    private SwsContext context;
    private final Frame result = new Frame();
    private boolean closed;

    Frame convert(Frame source) {
        if (closed) throw new IllegalStateException("Converter closed");
        if (source == null || !(source.opaque instanceof AVFrame raw) || raw.isNull())
            throw new IllegalArgumentException("Decoded AVFrame required");
        int width = raw.width(), height = raw.height(), format = raw.format();
        if (width <= 0 || height <= 0 || format < 0 || (long)width * height > Integer.MAX_VALUE / 3)
            throw new IllegalArgumentException("Invalid/oversized decoded image");
        if (destination == null || destination.width() != width || destination.height() != height) {
            freeImage();
            destination = av_frame_alloc();
            if (destination == null || destination.isNull()) throw new IllegalStateException("Cannot allocate output AVFrame");
            destination.width(width).height(height).format(AV_PIX_FMT_BGR24);
            if (av_frame_get_buffer(destination, 32) < 0) throw new IllegalStateException("Cannot allocate aligned output image");
            long capacity = (long)destination.linesize(0) * height;
            if (capacity <= 0 || capacity > Integer.MAX_VALUE) throw new IllegalArgumentException("Oversized output stride");
            result.image = new Buffer[] {destination.data(0).capacity(capacity).asByteBuffer()};
            result.imageWidth = width; result.imageHeight = height; result.imageDepth = Frame.DEPTH_UBYTE;
            result.imageChannels = 3; result.imageStride = destination.linesize(0); result.opaque = destination;
        }
        // Match JavaCV 1.3.3's format/size and SWS_BILINEAR policy; do not change CPU flags or scale the image.
        context = sws_getCachedContext(context, width, height, format, width, height, AV_PIX_FMT_BGR24,
                SWS_BILINEAR, null, null, (DoublePointer)null);
        if (context == null || context.isNull()) throw new IllegalStateException("Cannot create color conversion context");
        if (sws_scale(context, raw.data(), raw.linesize(), 0, height, destination.data(), destination.linesize()) != height)
            throw new IllegalStateException("Incomplete converted image");
        result.timestamp = source.timestamp; result.keyFrame = source.keyFrame;
        return result;
    }

    private void freeImage() {
        result.image = null; result.opaque = null;
        if (destination != null) { av_frame_free(destination); destination = null; }
    }
    @Override public void close() {
        if (closed) return; closed = true;
        try { if (context != null) { sws_freeContext(context); context = null; } }
        finally { freeImage(); }
    }
}
