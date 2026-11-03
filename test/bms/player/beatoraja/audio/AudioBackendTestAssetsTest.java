package bms.player.beatoraja.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Independent WAV layout/timing/signal checks; no native or audio hardware required. */
public final class AudioBackendTestAssetsTest {
    public static void main(String[] args) throws Exception {
        Path parent = Files.createTempDirectory("audio-fixture-regression-");
        Path root = parent.resolve("assets");
        try {
            AudioBackendTestAssets.generate(root);
            try { AudioBackendTestAssets.generate(root); throw new AssertionError("Existing output overwritten"); }
            catch (java.io.IOException expected) { }
            for (int rate : new int[] {44100, 48000}) {
                Path folder = root.resolve(Integer.toString(rate));
                byte[] bytes = Files.readAllBytes(folder.resolve("timeline.wav"));
                ByteBuffer pcm = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
                check(pcm.getInt(4) == bytes.length - 8 && pcm.getInt(40) == rate * 32 * 4, "RIFF length");
                check(pcm.getShort(20) == 1 && pcm.getShort(22) == 2 && pcm.getInt(24) == rate
                        && pcm.getInt(28) == rate * 4 && pcm.getShort(32) == 4 && pcm.getShort(34) == 16, "PCM16 stereo units");
                check(new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF"), "RIFF magic");
                for (int frame = 0; frame < rate * 32; frame++) {
                    for (int channel = 0; channel < 2; channel++) {
                        short sample = pcm.getShort(44 + frame * 4 + channel * 2);
                        check(Math.abs((int)sample) < 3200, "Headroom/no clipping");
                    }
                }
                // Count zero crossings well away from marker bursts: channels must not be swapped or interleaved incorrectly.
                for (int channel = 0; channel < 2; channel++) {
                    int crossings = 0;
                    for (int frame = rate / 4 + 1; frame < rate * 3 / 4; frame++) {
                        short previous = pcm.getShort(44 + (frame - 1) * 4 + channel * 2);
                        short current = pcm.getShort(44 + frame * 4 + channel * 2);
                        if (previous <= 0 && current > 0) crossings++;
                    }
                    check(Math.abs(crossings - (channel == 0 ? 220 : 440)) <= 1, "Frequency/channel identity");
                }
                for (int second = 1; second <= 30; second++) {
                    int peak = 0;
                    for (int i = rate / 200; i < rate / 50; i++) peak = Math.max(peak,
                            Math.abs((int)pcm.getShort(44 + (second * rate + i) * 4)));
                    check(peak > 2100, "One marker per second");
                }
                check(Files.readAllLines(folder.resolve("markers.csv")).size() == 31, "30 markers/29-second span");
                check(Files.readString(folder.resolve("timeline.bms")).contains("#00001:01"), "BGM at time zero");
                check(!Files.readString(folder.resolve("trigger.bms")).contains("#00001:"), "Manual latency has no BGM");
                for (String chart : new String[] {"timeline.bms", "trigger.bms"}) {
                    var model = new bms.model.BMSDecoder().decode(folder.resolve(chart));
                    check(model != null && model.getTotalNotes() == 16 && model.getBpm() == 120,
                            "Playable BMS fixture: " + chart);
                    for (String wav : model.getWavList())
                        if (wav != null && !wav.isEmpty()) check(Files.isRegularFile(folder.resolve(wav)), "Resolved chart audio");
                }
                byte[] trigger = Files.readAllBytes(folder.resolve("trigger.wav"));
                check(trigger.length == 44 + (rate / 20) * 4, "50-ms trigger");
                byte[] silence = Files.readAllBytes(folder.resolve("silence.wav"));
                for (int i = 44; i < silence.length; i++) check(silence[i] == 0, "Autoplay keys stay silent");
            }
            for (String line : Files.readAllLines(root.resolve("SHA256SUMS"))) {
                String[] fields = line.split("  ");
                check(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(root.resolve(fields[1]))))
                        .equals(fields[0]), "Manifest hashes");
            }
            System.out.println("AudioBackendTestAssetsTest passed (WAV units, duration, markers, stereo pitch, headroom, hashes, safe output)");
        } finally {
            try (var paths = Files.walk(parent)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
