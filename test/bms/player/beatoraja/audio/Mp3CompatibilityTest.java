package bms.player.beatoraja.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.MP3Decoder;
import javazoom.jl.decoder.OutputBuffer;

/** Golden PCM from JLayer 1.0.1-gdx; no audio device or FFmpeg required to run these tests. */
public final class Mp3CompatibilityTest {
    record Sample(int rate, int channels, int bytes, String hash) {
        String name() { return "tone-" + rate + "-" + channels + ".mp3"; }
    }

    static final List<Sample> SAMPLES = List.of(
            new Sample(44100, 1, 20736, "7e37aa752a0f384621ffc4601bf5232695eadd8266fc8b75bdadb3f83b6d344f"),
            new Sample(48000, 2, 46080, "9be56b99d6c57003510cba79c9e53181ef7e82467e2d2e5a9441337c2cac6e92"),
            new Sample(22050, 2, 23040, "f28dc5cd5a865e495a3702f4daab98faa15b968d8d6dd8827d74d9595d4281bf"));

    private Mp3CompatibilityTest() { }

    public static void main(String[] args) throws Exception {
        boolean baseline = args.length == 1 && args[0].equals("--baseline");
        for (Sample sample : SAMPLES) {
            byte[] mp3 = fixture(sample.name());
            byte[] decoded = decode(mp3, sample);
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(decoded));
            if (baseline) {
                System.out.println(sample.name() + " bytes=" + decoded.length + " sha256=" + hash);
            } else {
                check(decoded.length == sample.bytes && hash.equals(sample.hash), "Old decoder PCM: " + sample.name());
            }
            check(Arrays.equals(decoded, decode(mp3, sample)), "Fresh decoder/repeated key sound");
            verifyLoader(mp3, sample.name(), sample, decoded);
            verifyLoader(mp3Wave(mp3, sample), "mp3-in-wave.wav", sample, decoded);
            // Untagged frames must decode identically to the encoder's ID3-tagged input.
            check(mp3[0] == 'I' && mp3[1] == 'D' && mp3[2] == '3', "Tagged fixture");
            int tagLength = 10;
            int payload = 0;
            for (int i = 6; i < 10; i++) payload = (payload << 7) | (mp3[i] & 127);
            tagLength += payload;
            check(Arrays.equals(decoded, decode(Arrays.copyOfRange(mp3, tagLength, mp3.length), sample)), "ID3/no ID3 parity");
        }
        System.out.println("Mp3CompatibilityTest passed (MPEG1/2, CBR/VBR, mono/stereo, ID3, PCM/MP3-in-WAV)");
    }

    public static byte[] fixture(String name) throws IOException {
        try (var input = Mp3CompatibilityTest.class.getResourceAsStream("/audio/mp3/" + name)) {
            if (input == null) throw new IOException("Missing test fixture: " + name);
            return input.readAllBytes();
        }
    }

    /** Use the public SongResource loader, as the PCM backends and archived previews do. */
    public static void verifyResource(bms.player.beatoraja.song.SongResource resource) throws Exception {
        var sample = SAMPLES.get(1);
        var pcm = PCM.load(resource, null);
        check(pcm instanceof ShortDirectPCM && pcm.validate(), "Resource PCM16 decoded: " + resource.displayPath());
        check(pcm.channels == sample.channels && pcm.sampleRate == sample.rate, "Resource channels/rate");
        byte[] decoded = trimmed(decode(fixture(sample.name()), sample), sample.channels);
        check(pcm.len * 2 == decoded.length, "Resource duration/sample count");
        byte[] actual = new byte[pcm.len * 2];
        ((ByteBuffer) pcm.sample).duplicate().position(pcm.start * 2).get(actual);
        check(Arrays.equals(actual, decoded), "Resource PCM bytes");
    }

    private static byte[] decode(byte[] mp3, Sample sample) throws Exception {
        var stream = new Bitstream(new ByteArrayInputStream(mp3));
        var decoder = new MP3Decoder();
        var output = new OutputBuffer(sample.channels, false);
        decoder.setOutputBuffer(output);
        var bytes = new ByteArrayOutputStream();
        var bitrates = new HashSet<Integer>();
        int frames = 0;
        try {
            Header header;
            while ((header = stream.readFrame()) != null) {
                check(header.getSampleRate() == sample.rate, "MP3 frame sample rate");
                check((header.mode() == Header.SINGLE_CHANNEL ? 1 : 2) == sample.channels, "MP3 frame channels");
                bitrates.add(header.bitrate());
                decoder.decodeFrame(header, stream);
                bytes.write(output.getBuffer(), 0, output.reset());
                stream.closeFrame();
                frames++;
            }
        } finally { stream.close(); }
        check(frames > 2, "Multiple frames decoded without swallowed exceptions");
        check(sample.rate != 48000 || bitrates.size() > 1, "VBR fixture exercises different bitrates");
        check(bytes.size() > sample.rate * sample.channels / 5, "Nonempty PCM duration");
        check(!Arrays.equals(bytes.toByteArray(), new byte[bytes.size()]), "Non-silent decoder output");
        return bytes.toByteArray();
    }

    private static void verifyLoader(byte[] encoded, String name, Sample sample, byte[] decoded) throws Exception {
        var loader = new PCM.PCMLoader(null);
        loader.loadPCM(new ByteArrayInputStream(encoded), name);
        check(loader.channels == sample.channels && loader.sampleRate == sample.rate && loader.bitsPerSample == 16,
                "PCM loader format: " + name);
        byte[] expected = trimmed(decoded, sample.channels);
        byte[] actual = new byte[loader.pcm.limit()];
        loader.pcm.duplicate().rewind().get(actual);
        check(Arrays.equals(actual, expected), "PCM loader keeps decoder samples/tail trim: " + name);
        Path path = Files.createTempFile("mp3-compatibility-", name.endsWith(".wav") ? ".wav" : ".mp3");
        try {
            Files.write(path, encoded);
            var pcm = PCM.load(path, null);
            check(pcm instanceof ShortDirectPCM && pcm.validate() && pcm.len * 2 == expected.length,
                    "Public PCM loader for key sound/preview: " + name);
            byte[] restored = new byte[expected.length];
            ((ByteBuffer) pcm.sample).duplicate().rewind().get(restored);
            check(Arrays.equals(expected, restored), "PCM samples preserved: " + name);
        } finally { Files.delete(path); }
    }

    private static byte[] trimmed(byte[] decoded, int channels) {
        int size = decoded.length;
        while (size > channels * 2) {
            boolean zero = true;
            for (int i = size - channels * 2; i < size; i++) zero &= decoded[i] == 0;
            if (!zero) break;
            size -= channels * 2;
        }
        return Arrays.copyOf(decoded, size);
    }

    private static byte[] mp3Wave(byte[] mp3, Sample sample) {
        var bytes = ByteBuffer.allocate(44 + mp3.length).order(ByteOrder.LITTLE_ENDIAN);
        bytes.putInt(0x46464952).putInt(bytes.capacity() - 8).putInt(0x45564157).putInt(0x20746d66).putInt(16);
        bytes.putShort((short) 85).putShort((short) sample.channels).putInt(sample.rate).putInt(16000);
        bytes.putShort((short) 1).putShort((short) 0).putInt(0x61746164).putInt(mp3.length).put(mp3);
        return bytes.array();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
