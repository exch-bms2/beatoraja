package bms.player.beatoraja.play;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.input.*;
import bms.player.beatoraja.song.SongData;
import com.badlogic.gdx.utils.Json;
import static bms.player.beatoraja.input.KeyboardQueueTestSupport.*;

/** Real START/SELECT handling with synthetic analog state, without audio, graphics or player files. */
public final class ControlInputProcessorTest {
    public static void main(String[] args) throws Exception {
        persistence();
        for (Mode mode : new Mode[]{Mode.BEAT_5K, Mode.BEAT_7K, Mode.BEAT_10K, Mode.BEAT_14K,
                Mode.POPN_9K, Mode.KEYBOARD_24K, Mode.KEYBOARD_24K_DOUBLE}) {
            int up = switch (mode) {
                case BEAT_5K, BEAT_10K -> 5;
                case KEYBOARD_24K, KEYBOARD_24K_DOUBLE -> 25;
                default -> 7;
            };
            int down = up == 25 ? 24 : up + 1;
            var fixture = new Fixture(mode, true, 0);
            fixture.start();
            fixture.tick(up, 1); near(1.99f, fixture.renderer.getHispeed());
            fixture.tick(down, 1); near(2f, fixture.renderer.getHispeed());
            fixture.tick(up, 4); near(1.96f, fixture.renderer.getHispeed());
            // Idle frames must not repeat consumed motion.
            fixture.controls.input(); near(1.96f, fixture.renderer.getHispeed());
        }
        for (int condition = 0; condition <= 4; condition++) {
            var fixture = new Fixture(Mode.BEAT_7K, condition != 0, condition);
            fixture.start(); fixture.tick(7, 1);
            near(2, fixture.renderer.getHispeed());
            float cover = condition == 2 ? fixture.runtime.getLift() : condition == 3 ? fixture.runtime.getHidden() : fixture.runtime.getLanecover();
            near(condition == 2 || condition == 3 ? .199f : .201f, cover);
        }
        var fixed = new Fixture(Mode.BEAT_7K, true, 0);
        fixed.saved.setFixhispeed(PlayConfig.FIX_HISPEED_MAINBPM);
        fixed.runtime.setFixhispeed(PlayConfig.FIX_HISPEED_MAINBPM);
        fixed.runtime.setDuration(1000);
        fixed.start(); fixed.tick(8, 1);
        near(2.01f, fixed.renderer.getHispeed());
        check(fixed.runtime.getDuration() == 995, "Fixed-HS green number follows absolute 0.01 adjustment");
        var save = BMSPlayer.class.getDeclaredMethod("saveConfig"); save.setAccessible(true); save.invoke(fixed.player);
        check(fixed.saved.getDuration() == 995 && fixed.saved.isAnalogScratchHispeed(), "Fixed-HS setting survives normal end-of-play save");
        var free = new Fixture(Mode.BEAT_7K, true, 0); free.start(); free.tick(8, 1); save.invoke(free.player);
        near(2.01f, free.saved.getHispeed());
        free.runtime.setHispeed(PlayConfig.HISPEED_MAX); free.tick(8, 4); near(PlayConfig.HISPEED_MAX, free.renderer.getHispeed());
        free.runtime.setHispeed(PlayConfig.HISPEED_MIN); free.tick(7, 4); near(PlayConfig.HISPEED_MIN, free.renderer.getHispeed());
        var select = new Fixture(Mode.BEAT_7K, true, 0);
        select.input.setSelectPressed(true); select.controls.input();
        select.runtime.setDuration(500); select.tick(7, 1);
        check(select.runtime.getDuration() == 501, "SELECT + analog scratch still adjusts green number");
        near(2, select.renderer.getHispeed());
        var digital = new Fixture(Mode.BEAT_7K, true, 0); digital.start();
        digital.input.setKeyState(7, true, 1000); digital.controls.input();
        near(2, digital.renderer.getHispeed()); near(.201f, digital.runtime.getLanecover());
        digital.input.setKeyState(0, true, 2000); digital.controls.input();
        near(1.75f, digital.renderer.getHispeed());
        System.out.println("ControlInputProcessorTest passed (mode-specific START/scratch, opt-in guards, directions/ticks/limits, SELECT/keyboard, fixed-HS save, JSON/clone)");
    }

    private static void persistence() {
        var json = new Json();
        check(!json.fromJson(PlayConfig.class, "{}").isAnalogScratchHispeed(), "Old configuration defaults to existing cover controls");
        var player = new PlayerConfig(); player.getMode7().getPlayconfig().setAnalogScratchHispeed(true);
        var restored = json.fromJson(PlayerConfig.class, json.toJson(player)); restored.validate();
        check(restored.getMode7().getPlayconfig().isAnalogScratchHispeed() && !restored.getMode14().getPlayconfig().isAnalogScratchHispeed(),
                "Setting round-trips independently per mode");
        check(restored.getMode7().getPlayconfig().clone().isAnalogScratchHispeed(), "Runtime/replay clone preserves selection");
    }

    private static final class Fixture {
        final PlayerConfig settings = new PlayerConfig();
        final PlayConfig saved;
        final PlayConfig runtime;
        final LaneRenderer renderer = allocate(LaneRenderer.class);
        final BMSPlayer player = allocate(BMSPlayer.class);
        final BMSPlayerInputProcessor input;
        final ControlInputProcessor controls;

        Fixture(Mode mode, boolean enabled, int condition) throws Exception {
            saved = settings.getPlayConfig(mode).getPlayconfig();
            saved.setAnalogScratchHispeed(enabled); saved.setHispeed(2); saved.setFixhispeed(PlayConfig.FIX_HISPEED_OFF);
            saved.setHispeedMargin(.25f); saved.setHispeedAutoAdjust(condition == 4);
            saved.setEnablelanecover(condition == 1); saved.setEnablelift(condition == 2); saved.setEnablehidden(condition == 3);
            saved.setLanecover(.2f); saved.setLift(.2f); saved.setHidden(.2f);
            runtime = saved.clone();
            set(renderer, LaneRenderer.class, "playconfig", runtime);
            set(renderer, LaneRenderer.class, "basebpm", 120d);
            set(renderer, LaneRenderer.class, "nowbpm", 120d);
            set(renderer, LaneRenderer.class, "basehispeed", 2f);
            set(renderer, LaneRenderer.class, "hispeedmargin", .25f);
            input = input(settings.getPlayConfig(mode).getKeyboardConfig());
            set(input, BMSPlayerInputProcessor.class, "analogScroll", true);
            set(input, BMSPlayerInputProcessor.class, "lastAnalogValue", new float[256]);
            set(input, BMSPlayerInputProcessor.class, "analogLastResetTime", new long[256]);
            var main = allocate(MainController.class);
            set(main, MainController.class, "input", input); set(main, MainController.class, "player", settings);
            var resource = allocate(PlayerResource.class); set(resource, PlayerResource.class, "pconfig", settings);
            var song = new SongData(); song.setNotes(1); set(resource, PlayerResource.class, "songdata", song);
            var model = new BMSModel(); model.setMode(mode);
            set(player, MainState.class, "main", main); set(player, MainState.class, "resource", resource);
            set(player, MainState.class, "timer", new TimerManager()); set(player, BMSPlayer.class, "model", model);
            var judge = allocate(JudgeManager.class); set(judge, JudgeManager.class, "score", new ScoreData());
            set(player, BMSPlayer.class, "players", new BMSPlayer.PlayerProperty[]{
                    new BMSPlayer.PlayerProperty(renderer, judge, null, null)});
            controls = new ControlInputProcessor(player, BMSPlayerMode.PLAY);
        }

        void start() {
            input.startChanged(true); controls.input();
        }

        void tick(int key, int ticks) {
            input.setAnalogState(key, true, 0); input.resetAnalogInput(key);
            input.setAnalogState(key, true, ticks * .0085f); controls.input();
        }
    }

    private static void near(float expected, float actual) {
        if (Math.abs(expected - actual) > .00001f) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
