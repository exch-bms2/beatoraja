package bms.player.beatoraja.video;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.bytedeco.javacv.*;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.avutil.AVFrame;
import org.bytedeco.javacpp.swscale.SwsContext;
import static org.bytedeco.javacpp.avutil.*;
import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.swscale.*;

/** Each risky decoder/converter runs in its own JVM; no GL, Pixmap, texture or audio is initialized. */
public final class FFmpegDecoderIsolationTest {
    public static void main(String[] args) throws Exception {
        if (args[0].equals("child")) { decode(Path.of(args[1]), args[2], args[3].equals("stream")); return; }
        Path output = Path.of(args[0]).toAbsolutePath(); Files.createDirectories(output);
        var rows = new ArrayList<String>(); rows.add("width,mode,input,exit,timeout");
        boolean unsafe = Boolean.getBoolean("beatoraja.decoder.unsafeProbes");
        int selectedWidth = Integer.getInteger("beatoraja.decoder.width", 0);
        if (selectedWidth != 0 && (selectedWidth < 2 || selectedWidth > 4096 || selectedWidth % 2 != 0))
            throw new IllegalArgumentException("decoder width must be even, 2..4096, or 0 for the matrix");
        for (int width : selectedWidth == 0 ? new int[] {64, 66, 68, 70, 80} : new int[] {selectedWidth}) {
            Path movie = output.resolve("yuv-" + width + ".avi"); generate(movie, width);
            List<String> modes = unsafe ? List.of("raw", "raw-processed", "color", "guarded-tight", "guarded-aligned", "safe") : List.of("raw", "safe");
            for (String mode : modes) for (String input : List.of("file", "stream")) {
                String name = width + "-" + mode + "-" + input;
                Path log = output.resolve(name + ".log");
                var command = List.of(Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString(),
                        "-Xmx256m", "-XX:-CreateCoredumpOnCrash", "-XX:ErrorFile=" + output.resolve("hs_err_pid%p.log"),
                        "-cp", System.getProperty("java.class.path"), FFmpegDecoderIsolationTest.class.getName(), "child", movie.toString(), mode, input);
                var process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
                boolean finished;
                try { finished = process.waitFor(30, TimeUnit.SECONDS); }
                finally { if (process.isAlive()) { process.destroyForcibly(); process.waitFor(); } }
                int exit = process.exitValue(); rows.add(width + "," + mode + "," + input + "," + exit + "," + !finished);
                Files.write(output.resolve("results.csv"), rows);
                System.out.println("Decoder child : " + name + ", exit=" + exit + ", timeout=" + !finished);
                if ((!unsafe || mode.equals("raw") || mode.equals("safe") || mode.equals("guarded-aligned")) && (!finished || exit != 0))
                    throw new AssertionError("Safe/RAW probe failed; inspect " + log);
            }
        }
        System.out.println("FFmpegDecoderIsolationTest passed (isolated native processes); reports=" + output);
    }

    private static void generate(Path path, int width) throws Exception {
        // Feed planar YUV directly: fixture generation does not use a BGR->YUV conversion.
        var recorder = new FFmpegFrameRecorder(path.toFile(), width, 48);
        recorder.setFormat("avi"); recorder.setVideoCodec(AV_CODEC_ID_MPEG4); recorder.setPixelFormat(AV_PIX_FMT_YUV420P);
        recorder.setFrameRate(20); recorder.setVideoBitrate(2_000_000);
        try {
            recorder.start();
            int plane = width * 48; var data = ByteBuffer.allocateDirect(plane * 3 / 2 + 64);
            for (int i = 0; i < plane; i++) data.put(i, (byte)81);
            for (int i = plane; i < plane * 5 / 4; i++) data.put(i, (byte)90);
            for (int i = plane * 5 / 4; i < plane * 3 / 2; i++) data.put(i, (byte)240);
            for (int i = 0; i < 12; i++) { data.position(0); recorder.recordImage(width, 48, Frame.DEPTH_UBYTE, 1, width, AV_PIX_FMT_YUV420P, data); }
            recorder.stop();
        } finally { recorder.release(); }
    }

    private static void decode(Path movie, String mode, boolean stream) throws Exception {
        FFmpegFrameGrabber grabber = stream ? new FFmpegFrameGrabber(new ByteArrayInputStream(Files.readAllBytes(movie)))
                : new FFmpegFrameGrabber(movie.toFile());
        if (!mode.equals("color")) grabber.setImageMode(FrameGrabber.ImageMode.RAW);
        BgrFrameConverter safe = new BgrFrameConverter();
        try {
            grabber.start(); int count = 0;
            Frame frame;
            while ((frame = mode.equals("color") || mode.equals("raw-processed") ? grabber.grabImage()
                    : FFmpegProcessor.grabDecodedFrame(grabber)) != null) {
                AVFrame raw = (AVFrame)frame.opaque;
                if (mode.equals("raw-processed") && count == 0) {
                    var field = FFmpegFrameGrabber.class.getDeclaredField("image_ptr"); field.setAccessible(true);
                    var pointers = (BytePointer[])field.get(grabber);
                    System.out.println("legacyImagePointerBorrowsDecoderPlane=" + (pointers[0].address() == raw.data(0).address())
                            + ", decoderBufferReference=" + (raw.buf(0) != null && !raw.buf(0).isNull()));
                }
                if (mode.startsWith("raw")) {
                    for (int plane = 0; plane < 3; plane++) {
                        int width = plane == 0 ? raw.width() : (raw.width() + 1) / 2;
                        int height = plane == 0 ? raw.height() : (raw.height() + 1) / 2;
                        ByteBuffer bytes = raw.data(plane).capacity((long)raw.linesize(plane) * height).asByteBuffer();
                        int wanted = plane == 0 ? 81 : plane == 1 ? 90 : 240;
                        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++)
                            check(Math.abs((bytes.get(y * raw.linesize(plane) + x) & 255) - wanted) <= 3, "RAW plane corrupted");
                    }
                } else if (mode.startsWith("guarded")) guarded(raw, mode.equals("guarded-aligned"));
                else checkBgr(mode.equals("safe") ? safe.convert(frame) : frame);
                count++;
                System.out.println("frame=" + count + ", mode=" + mode + ", source=" + raw.width() + "x" + raw.height()
                        + ", pixelFormat=" + raw.format() + ", sourceStride=" + raw.linesize(0)); System.out.flush();
            }
            check(count == 12, "Frame count changed: " + count);
        } finally {
            safe.close();
            System.out.println("closing grabber"); System.out.flush();
            try { grabber.stop(); } finally { grabber.close(); }
            System.out.println("closed grabber");
        }
    }

    private static void checkBgr(Frame frame) {
        var bytes = (ByteBuffer)frame.image[0];
        for (int y = 0; y < frame.imageHeight; y++) for (int x = 0; x < frame.imageWidth; x++) {
            int p = y * frame.imageStride + x * 3;
            check((bytes.get(p) & 255) < 20 && (bytes.get(p + 1) & 255) < 20 && (bytes.get(p + 2) & 255) > 220,
                    "BGR corruption at x=" + x + ", y=" + y + ", stride=" + frame.imageStride + ", BGR="
                            + (bytes.get(p) & 255) + "/" + (bytes.get(p + 1) & 255) + "/" + (bytes.get(p + 2) & 255));
        }
    }

    private static void guarded(AVFrame raw, boolean aligned) {
        int row = raw.width() * 3, stride = aligned ? (row + 31) & ~31 : row;
        int size = stride * raw.height(), guard = 4096;
        BytePointer memory = new BytePointer(av_malloc(size + guard)).capacity(size + guard);
        AVFrame target = av_frame_alloc(); SwsContext context = null;
        try {
            for (int i = 0; i < size + guard; i++) memory.put(i, (byte)0x5a);
            target.data(0, memory).linesize(0, stride).width(raw.width()).height(raw.height()).format(AV_PIX_FMT_BGR24);
            context = sws_getCachedContext(null, raw.width(), raw.height(), raw.format(), raw.width(), raw.height(), AV_PIX_FMT_BGR24,
                    SWS_BILINEAR, null, null, (DoublePointer)null);
            check(sws_scale(context, raw.data(), raw.linesize(), 0, raw.height(), target.data(), target.linesize()) == raw.height(), "Conversion rows");
            int changed = 0;
            for (int i = size; i < size + guard; i++) if (memory.get(i) != (byte)0x5a) changed++;
            System.out.println("destinationStride=" + stride + ", tailGuardModified=" + changed); System.out.flush();
            var frame = new Frame(); frame.imageWidth = raw.width(); frame.imageHeight = raw.height();
            frame.imageStride = stride; frame.image = new java.nio.Buffer[] {memory.asByteBuffer()};
            checkBgr(frame); check(changed == 0, "Native color conversion wrote beyond tight image allocation");
        } finally { if (context != null) sws_freeContext(context); av_frame_free(target); av_free(memory); }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
