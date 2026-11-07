package bms.player.beatoraja.audio;

import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MP3;

/** Optional offline generator for synthetic MP3 fixtures; never run by distZip or regression tests. */
public final class Mp3TestAssets {
    private Mp3TestAssets() { }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        Files.createDirectories(root);
        for (int[] format : new int[][] {{44100, 1}, {48000, 2}, {22050, 2}}) {
            int rate = format[0], channels = format[1];
            Path path = root.resolve("tone-" + rate + "-" + channels + ".mp3");
            if (Files.exists(path)) throw new IllegalArgumentException("Refuse to overwrite " + path);
            try (var recorder = new FFmpegFrameRecorder(path.toFile(), channels)) {
                recorder.setFormat("mp3");
                recorder.setAudioCodec(AV_CODEC_ID_MP3);
                recorder.setSampleRate(rate);
                recorder.setAudioChannels(channels);
                recorder.setAudioBitrate(rate == 22050 ? 64000 : 128000);
                // No seek-dependent Xing frame; these fixtures exercise the decoder's raw frame duration.
                recorder.setOption("write_xing", "0");
                if (rate == 48000) recorder.setAudioQuality(4);
                recorder.start();
                var samples = ShortBuffer.allocate(rate / 5 * channels);
                for (int frame = 0; frame < rate / 5; frame++) {
                    for (int channel = 0; channel < channels; channel++) {
                        double phase = 2 * StrictMath.PI * (channel == 0 ? 440 : 880) * frame / rate;
                        samples.put((short) StrictMath.round(4000 * StrictMath.sin(phase)));
                    }
                }
                samples.flip();
                recorder.recordSamples(rate, channels, samples);
                recorder.stop();
            }
            System.out.println(path + " (" + Files.size(path) + " bytes)");
        }
    }
}
