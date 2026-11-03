package bms.player.beatoraja.audio;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import bms.player.beatoraja.AudioConfig;
import bms.player.beatoraja.Config;

/** Real OpenAL context and zero-valued PCM only. Does not measure acoustic latency. */
public final class Lwjgl3AudioDiagnosticsSmokeTest {
    public static void main(String[] args) {
        System.setProperty("beatoraja.audioDiagnostics", "true");
        var window = new Lwjgl3ApplicationConfiguration();
        window.setTitle("beatoraja silent audio diagnostics test");
        window.setWindowedMode(320, 180);
        window.setForegroundFPS(60);
        window.setAudioConfig(16, 4096, 9);
        new Lwjgl3Application(new ApplicationAdapter() {
            GdxSoundDriver sounds;
            Sound silence;
            GdxAudioDeviceDriver device;
            int frames, rate = 44100;
            @Override public void create() {
                check(org.lwjgl.openal.ALC10.alcGetCurrentContext() != 0, "A real OpenAL context is required");
                var config = configuration(rate);
                sounds = new GdxSoundDriver(config);
                silence = sounds.getKeySound(new ShortPCM(2, rate, 0, 8820, new short[8820]));
                check(silence != null, "Zero PCM sound decoded");
                sounds.play(silence, 0, 1, 1);
                check(diagnostics(sounds).snapshot().playRejected() == 0, "Native sound source accepted");
                device = new GdxAudioDeviceDriver(config);
            }
            @Override public void render() {
                if (++frames % 60 != 0) return;
                var snapshot = diagnostics(device).snapshot();
                check(snapshot.returned() > 2 && snapshot.failed() == 0, "Native writes returned at " + rate + " Hz");
                device.dispose();
                check(!mixer(device).isAlive(), "Native mixer exits before device disposal completes");
                device = null;
                if (rate == 44100) {
                    rate = 48000;
                    device = new GdxAudioDeviceDriver(configuration(rate));
                } else Gdx.app.exit();
            }
            @Override public void dispose() {
                if (device != null) device.dispose();
                if (silence != null) silence.dispose();
                if (sounds != null) sounds.dispose();
            }
        }, window);
        System.out.println("Lwjgl3AudioDiagnosticsSmokeTest passed (silent native Sound/AudioDevice, 44100/48000 Hz; no acoustic latency measurement)");
    }

    private static Config configuration(int rate) {
        var config = new Config();
        config.setAudioConfig(new AudioConfig());
        config.getAudioConfig().setSampleRate(rate);
        config.getAudioConfig().setDeviceBufferSize(384);
        config.getAudioConfig().setDeviceSimultaneousSources(16);
        return config;
    }
    private static AudioDiagnostics diagnostics(Object driver) {
        return (AudioDiagnostics) field(driver, "diagnostics");
    }
    private static Thread mixer(Object driver) { return (Thread) field(driver, "mixer"); }
    private static Object field(Object driver, String name) {
        try {
            var field = driver.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(driver);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
