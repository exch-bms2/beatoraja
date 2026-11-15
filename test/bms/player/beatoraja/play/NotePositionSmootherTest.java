package bms.player.beatoraja.play;

import bms.player.beatoraja.Config;
import com.badlogic.gdx.utils.Json;

public final class NotePositionSmootherTest {
    public static void main(String[] args) {
        checkConfiguration();
        checkJitter();
        checkCappedCadence();
        checkReset();
        checkGeometry();
    }

    private static void checkConfiguration() {
        var json = new Json();
        json.setUsePrototypes(false);
        var config = json.fromJson(Config.class, "{vsync:true}");
        String property = "beatoraja.notePositionSmoothing";
        String original = System.getProperty(property);
        try {
            System.clearProperty(property);
            check(!new Config().isNotePositionSmoothingEnabled(), "new config defaults to off");
            check(!config.isNotePositionSmoothingEnabled(), "old config defaults to off even with VSync enabled");
            System.setProperty(property, "true");
            check(config.isNotePositionSmoothingEnabled(), "VM option enables a saved off value");
            check(!config.isNotePositionSmoothing(), "VM enable must not change the saved value");
            config.setNotePositionSmoothing(true);
            config = json.fromJson(Config.class, json.toJson(config));
            System.clearProperty(property);
            check(config.isNotePositionSmoothingEnabled(), "saved on value survives a reload");
            System.setProperty(property, "false");
            check(!config.isNotePositionSmoothingEnabled(), "VM option disables a saved on value");
            check(config.isNotePositionSmoothing(), "VM disable must not change the saved value");
            System.setProperty(property, "invalid");
            check(!config.isNotePositionSmoothingEnabled(), "non-true values keep the old VM option semantics");
        } finally {
            if (original == null) System.clearProperty(property);
            else System.setProperty(property, original);
        }
    }

    private static void checkJitter() {
        var smoother = new NotePositionSmoother();
        long time = 100_000;
        long previousDrawTime = time;
        long rawError = 0;
        long drawError = 0;
        check(smoother.sample(time, 0, 60) == 0, "initial sample must be unshifted");
        for (int i = 0; i < 100; i++) {
            long delta = i % 2 == 0 ? 15_000 : 18_333;
            time += delta;
            long offset = smoother.sample(time, 0, 60);
            check(Math.abs(offset) <= NotePositionSmoother.MAX_OFFSET_MICROS, "offset exceeded cap");
            long drawTime = time + offset;
            check(drawTime > previousDrawTime, "corrected time moved backwards");
            rawError += Math.abs(delta - 16_667);
            drawError += Math.abs(drawTime - previousDrawTime - 16_667);
            previousDrawTime = drawTime;
        }
        check(drawError < rawError / 4, "regular VSync jitter should be reduced");

        smoother.reset();
        smoother.sample(0, 0, 60);
        check(smoother.sample(10_000, 0, 60) == 2_000, "positive cap");
        smoother.reset();
        smoother.sample(0, 0, 60);
        check(smoother.sample(24_000, 0, 60) == -2_000, "negative cap");
    }

    private static void checkCappedCadence() {
        var smoother = new NotePositionSmoother();
        long time = 100_000;
        long drawTime = time;
        long rawError = 0, drawError = 0;
        smoother.sampleCapped(time, 0, 60, time);
        for (int i = 0; i < 48; i++) {
            long delta = i % 2 == 0 ? 15_000 : 18_333;
            time += delta;
            long offset = smoother.sampleCapped(time, 0, 60, time);
            check(smoother.sampleCapped(time, 0, 60, time) == offset, "duplicate draws do not advance the window");
            if (i < 8) check(offset == 0, "no correction before a stable cadence is established");
            check(Math.abs(offset) <= NotePositionSmoother.MAX_OFFSET_MICROS, "capped mode keeps the offset limit");
            if (i >= 8) {
                rawError += Math.abs(delta - 16_667);
                drawError += Math.abs(time + offset - drawTime - 16_667);
            }
            drawTime = time + offset;
        }
        check(drawError < rawError / 4, "stable capped jitter should be reduced");
        time += 33_333;
        check(smoother.sampleCapped(time, 0, 60, time) == 0, "dropped frame stops correction immediately");
        time += 15_000;
        check(smoother.sampleCapped(time, 0, 60, time) == 0, "dropped frame restarts warmup");
        check(smoother.sampleCapped(time, 0, 0, time) == 0, "unlimited FPS disables capped smoothing");

        for (long interval : new long[]{11_111, 8_800}) {
            // Hold the chart clock near target to prove the gate checks the actual draw cadence.
            smoother.reset();
            for (int i = 0; i < 24; i++) {
                check(smoother.sampleCapped(i * 8_333L, 0, 120, i * interval) == 0,
                        "sustained rate below the cap must not be corrected: " + interval);
            }
        }
        check(smoother.sampleCapped(210_000, 1, 120, 215_000) == 0, "seek restarts capped warmup");
        check(smoother.sampleCapped(225_000, 1, 60, 230_000) == 0, "cap change restarts warmup");
        check(smoother.sample(230_000, 1, 60) == 0, "switch to VSync resets the correction");
        check(smoother.sample(245_000, 1, 60) > 0, "VSync keeps its existing immediate correction");
    }

    private static void checkReset() {
        var smoother = new NotePositionSmoother();
        smoother.sample(0, 0, 60);
        check(smoother.sample(15_000, 0, 60) > 0, "should have a correction to clear");
        check(smoother.sample(55_000, 0, 60) == 0, "missed frame");
        check(smoother.sample(70_000, 1, 60) == 0, "clock origin changed");
        check(smoother.sample(40_000, 1, 60) == 0, "backward seek");
        check(smoother.sample(55_000, 1, 120) == 0, "display rate changed");
        long resumedOffset = smoother.sample(62_500, 1, 120);
        check(resumedOffset > 0, "resume after display change");
        check(smoother.sample(62_500, 1, 120) == resumedOffset, "same prepared time must draw the same geometry");
        check(smoother.sample(70_000, 1, 0) == 0, "unknown display rate");
        check(smoother.sample(77_500, 1, 120) == 0, "first frame after invalid display rate");
        smoother.reset();
        check(smoother.sample(85_000, 1, 120) == 0, "first frame after leaving play or moving monitor");
    }

    private static void checkGeometry() {
        check(NotePositionSmoother.positionOffset(50_000, 2_000, 10_000, 110_000, 400) == -8,
                "positive correction moves future notes towards the judge line");
        check(NotePositionSmoother.positionOffset(50_000, -2_000, 10_000, 110_000, -400) == -8,
                "negative scroll keeps its sign");
        check(NotePositionSmoother.positionOffset(50_000, 0, 10_000, 110_000, 400) == 0,
                "disabled path leaves positions alone");
        check(NotePositionSmoother.positionOffset(8_000, 2_000, 10_000, 110_000, 400) == 0,
                "don't move during STOP");
        check(NotePositionSmoother.positionOffset(110_000, -2_000, 10_000, 110_000, 400) == 0,
                "don't shift the timeline boundary");
        check(NotePositionSmoother.positionOffset(109_000, 2_000, 10_000, 110_000, 400) == -2,
                "taper correction near the timeline boundary");
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
