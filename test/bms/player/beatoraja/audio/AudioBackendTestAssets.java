package bms.player.beatoraja.audio;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/** Deterministic offline fixtures. Never opens an audio device or changes player settings. */
public final class AudioBackendTestAssets {
    static final int SECONDS = 32;
    private AudioBackendTestAssets() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Specify a new output directory");
        generate(Path.of(args[0]));
    }

    static void generate(Path root) throws Exception {
        // Refuse existing targets rather than replacing user recordings or player data.
        if (Files.exists(root)) throw new IOException("Output already exists: " + root);
        Files.createDirectories(root);
        StringBuilder hashes = new StringBuilder();
        for (int rate : new int[] {44100, 48000}) {
            Path folder = Files.createDirectory(root.resolve(Integer.toString(rate)));
            wave(folder.resolve("timeline.wav"), rate, rate * SECONDS, 0);
            wave(folder.resolve("silence.wav"), rate, rate / 100, 1);
            wave(folder.resolve("trigger.wav"), rate, rate / 20, 2);
            String header = "#PLAYER 1\n#GENRE AUDIO TEST\n#ARTIST beatoraja diagnostic fixture\n#BPM 120\n#PLAYLEVEL 1\n#RANK 2\n#TOTAL 100\n";
            StringBuilder speed = new StringBuilder(header).append("#TITLE Audio timeline ").append(rate)
                    .append(" Hz - autoplay\n#WAV01 timeline.wav\n#WAV02 silence.wav\n#00001:01\n");
            StringBuilder latency = new StringBuilder(header).append("#TITLE Audio trigger ").append(rate)
                    .append(" Hz - manual input\n#WAV01 trigger.wav\n");
            for (int bar = 1; bar <= 16; bar++) {
                speed.append(String.format(Locale.ROOT, "#%03d11:02\n", bar));
                latency.append(String.format(Locale.ROOT, "#%03d11:01\n", bar));
            }
            Files.writeString(folder.resolve("timeline.bms"), speed, StandardCharsets.US_ASCII);
            Files.writeString(folder.resolve("trigger.bms"), latency, StandardCharsets.US_ASCII);
            StringBuilder markers = new StringBuilder("marker,source_frame,source_seconds\n");
            for (int second = 1; second <= 30; second++)
                markers.append(second).append(',').append(second * rate).append(',').append(second).append('\n');
            Files.writeString(folder.resolve("markers.csv"), markers, StandardCharsets.US_ASCII);
            for (String name : new String[] {"timeline.wav", "silence.wav", "trigger.wav", "timeline.bms", "trigger.bms", "markers.csv"}) {
                Path file = folder.resolve(name);
                hashes.append(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))))
                        .append("  ").append(rate).append('/').append(name).append('\n');
            }
        }
        Files.writeString(root.resolve("SHA256SUMS"), hashes, StandardCharsets.US_ASCII);
        Files.writeString(root.resolve("measurements.csv"),
                "build,os,jvm_arch,backend,device,source_rate,configured_rate,actual_rate,configured_buffer,actual_buffer,trial,recording_rate,first_marker_frame,last_marker_frame,observed_span_seconds,speed_ratio,dropout_count,latency_median_ms,latency_p95_ms,diagnostics,notes\n",
                StandardCharsets.US_ASCII);
        Files.writeString(root.resolve("README.txt"),
                "Start at low volume. Generated PCM16 stereo WAV; no external recordings.\n"
                + "See docs/audio-backend-measurement.md in the distribution root.\n"
                + "timeline.bms: autoplay, 32-second BGM; source markers at 1..30 seconds (29-second span).\n"
                + "L=440 Hz, R=880 Hz at low level; 1800 Hz marker bursts on both channels.\n"
                + "trigger.bms: manual lane-1 input, 50-ms trigger, no BGM. Do NOT autoplay for input latency.\n"
                + "WAV duration is not chart completion time. No physical latency/underrun claim from these files alone.\n",
                StandardCharsets.US_ASCII);
    }

    private static void wave(Path path, int rate, int frames, int kind) throws IOException {
        try (var out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)))) {
            out.writeBytes("RIFF"); little(out, 36 + frames * 4, 4); out.writeBytes("WAVEfmt ");
            little(out, 16, 4); little(out, 1, 2); little(out, 2, 2); little(out, rate, 4);
            little(out, rate * 4, 4); little(out, 4, 2); little(out, 16, 2);
            out.writeBytes("data"); little(out, frames * 4, 4);
            for (int frame = 0; frame < frames; frame++) {
                for (int channel = 0; channel < 2; channel++) little(out, sample(rate, frame, channel, kind), 2);
            }
        }
    }

    static short sample(int rate, int frame, int channel, int kind) {
        double t = (double)frame / rate;
        double value = 0;
        if (kind == 0) {
            double fade = Math.min(1, Math.min(t / .01, (SECONDS - t) / .01));
            value = .015 * fade * StrictMath.sin(2 * StrictMath.PI * (channel == 0 ? 440 : 880) * t);
            int second = frame / rate, position = frame % rate;
            int length = rate / 50;
            if (second >= 1 && second <= 30 && position < length) {
                double envelope = .5 - .5 * StrictMath.cos(2 * StrictMath.PI * position / (length - 1));
                value += .08 * envelope * StrictMath.sin(2 * StrictMath.PI * 1800 * position / rate);
            }
        } else if (kind == 2) {
            int length = rate / 20;
            double envelope = Math.min(1, Math.min(frame / (rate * .001), (length - 1 - frame) / (rate * .005)));
            value = .08 * envelope * StrictMath.sin(2 * StrictMath.PI * (channel == 0 ? 1200 : 1800) * t);
        }
        return (short)StrictMath.round(value * 32767);
    }

    private static void little(DataOutputStream out, int value, int bytes) throws IOException {
        for (int i = 0; i < bytes; i++) out.writeByte(value >>> (i * 8));
    }
}
