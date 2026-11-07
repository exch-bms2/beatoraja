package bms.player.beatoraja.audio;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import com.badlogic.gdx.backends.lwjgl3.audio.Mp3;
import com.badlogic.gdx.backends.lwjgl3.audio.OpenALLwjgl3Audio;
import com.badlogic.gdx.files.FileHandle;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.alcGetCurrentContext;

/** Opt-in native test. All playback is muted; no physical latency or acoustic quality claim. */
public final class Mp3OpenALSmokeTest {
    public static void main(String[] args) throws Exception {
        var audio = new OpenALLwjgl3Audio(8, 3, 512);
        try {
            check(alcGetCurrentContext() != 0, "An actual OpenAL context is required");
            for (var sample : Mp3CompatibilityTest.SAMPLES) {
                Path path = Files.createTempFile("mp3-openal-", ".mp3");
                try {
                    Files.write(path, Mp3CompatibilityTest.fixture(sample.name()));
                    verifySound(audio, new FileHandle(path.toFile()), sample);
                    verifyMusic(audio, new FileHandle(path.toFile()), sample);
                } finally { Files.delete(path); }
            }
        } finally { audio.dispose(); }
        check(alcGetCurrentContext() == 0, "Release OpenAL context after MP3 tests");
        System.out.println("Mp3OpenALSmokeTest passed (muted Sound/Music, golden streaming PCM, reset/loop/seek/dispose)");
    }

    private static void verifySound(OpenALLwjgl3Audio audio, FileHandle file,
            Mp3CompatibilityTest.Sample sample) {
        var sound = audio.newSound(file);
        try {
            check(sound instanceof Mp3.Sound, "Native MP3 Sound decoder");
            check(sound.getRate() == sample.rate() && sound.getChannels() == sample.channels(), "Sound format");
            float seconds = sample.bytes() / (float) (sample.rate() * sample.channels() * 2);
            check(Math.abs(sound.duration() - seconds) < .00001f, "Sound sample count/duration");
            check(sound.play(0) != -1 && sound.loop(0) != -1, "Muted key sound and looping preview accepted");
            check(alGetError() == AL_NO_ERROR, "MP3 Sound upload/play");
            sound.stop();
        } finally { sound.dispose(); }
    }

    private static void verifyMusic(OpenALLwjgl3Audio audio, FileHandle file,
            Mp3CompatibilityTest.Sample sample) throws Exception {
        var music = (Mp3.Music) audio.newMusic(file);
        try {
            for (int pass = 0; pass < 3; pass++) {
                var decoded = new ByteArrayOutputStream();
                byte[] buffer = new byte[16384];
                int size;
                while ((size = music.read(buffer)) > 0) decoded.write(buffer, 0, size);
                String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(decoded.toByteArray()));
                check(decoded.size() == sample.bytes() && hash.equals(sample.hash()), "Music streaming PCM, pass " + pass);
                music.reset();
            }
            music.setVolume(0);
            music.setLooping(true);
            music.play();
            for (int i = 0; i < 35; i++) {
                Thread.sleep(10);
                audio.update();
            }
            check(music.isPlaying(), "Muted preview stays active across EOF/loop");
            music.setPosition(.05f);
            audio.update();
            check(music.isPlaying() && alGetError() == AL_NO_ERROR, "Preview seek without native error");
            music.stop();
            check(!music.isPlaying(), "Preview stop");
        } finally { music.dispose(); }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
