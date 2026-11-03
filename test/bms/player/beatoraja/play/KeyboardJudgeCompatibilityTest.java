package bms.player.beatoraja.play;

import java.lang.reflect.Proxy;
import java.nio.file.Path;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.audio.AudioDriver;
import bms.player.beatoraja.input.*;

/** Exercises the real JudgeManager with inert application/audio fixtures, never player data. */
public final class KeyboardJudgeCompatibilityTest {
    private static int eventSource;
    public static void main(String[] args) throws Exception {
        for (eventSource = 0; eventSource < 3; eventSource++) {
            testNormalAndShortTap();
            testTimingWindows();
            for (int type : new int[] {LongNote.TYPE_LONGNOTE, LongNote.TYPE_CHARGENOTE, LongNote.TYPE_HELLCHARGENOTE})
                testLongNote(type);
            testSameTimeChord();
            testScratchEnds();
            testReplayLegacy();
        }
        System.out.println("KeyboardJudgeCompatibilityTest passed (normal/LN/CN/HCN, chords, legacy/new replay paths)");
    }

    private static void testNormalAndShortTap() throws Exception {
        var legacy = normal(false);
        var queued = normal(true);
        for (var f : new Fixture[] {legacy, queued}) {
            f.input.setKeyState(0, true, 1_000_000);
            f.judge.update(1_000_000);
            f.input.setKeyState(0, false, 1_020_000);
            f.judge.update(1_020_000);
        }
        check(queued.judge.getPastNotes() == legacy.judge.getPastNotes()
                && queued.judge.getJudgeCount(0) == legacy.judge.getJudgeCount(0), "Ordinary input result unchanged");
        var shortTap = normal(true);
        shortTap.input.setKeyState(0, true, 1_000_000);
        shortTap.input.setKeyState(0, false, 1_000_000);
        shortTap.judge.update(1_200_000);
        check(shortTap.judge.getJudgeCount(0) == 1, "Same-poll press/release is judged once with its original time");
        shortTap.judge.update(1_500_000);
        check(shortTap.judge.getPastNotes() == 1, "No duplicate judgment from a lingering time slot");
    }

    private static void testLongNote(int type) throws Exception {
        var legacy = longNote(false, type);
        var queued = longNote(true, type);
        legacy.input.setKeyState(0, true, 1_000_000);
        legacy.judge.update(1_000_000);
        legacy.input.setKeyState(0, false, 2_000_000);
        legacy.judge.update(2_000_000);
        legacy.judge.update(3_000_000);
        queued.input.setKeyState(0, true, 1_000_000);
        queued.input.setKeyState(0, false, 2_000_000);
        queued.judge.update(3_000_000);
        check(queued.judge.getJudgeCount(0) == legacy.judge.getJudgeCount(0)
                && queued.judge.getJudgeCount(4) == legacy.judge.getJudgeCount(4)
                && queued.judge.getPastNotes() == legacy.judge.getPastNotes(), "Backlogged LN edges retain judgment times: " + type);

        var early = longNote(true, type);
        early.input.setKeyState(0, true, 1_000_000);
        early.judge.update(1_000_000);
        early.input.setKeyState(0, false, 1_500_000);
        early.input.setKeyState(0, true, 1_510_000);
        early.judge.update(1_510_000);
        early.input.setKeyState(0, false, 2_000_000);
        early.judge.update(2_000_000);
        early.judge.update(3_000_000);
        check(early.judge.getPastNotes() == legacy.judge.getPastNotes(), "Release/repress margin does not duplicate LN endpoints: " + type);
    }

    private static void testSameTimeChord() throws Exception {
        var model = model();
        var line = line(1_000_000);
        line.setNote(0, new NormalNote(0));
        line.setNote(1, new NormalNote(1));
        model.setAllTimeLine(new TimeLine[] {line});
        var f = new Fixture(model, true);
        if (eventSource == 2) f.input.beginInputJudge(new int[]{0}, null, new int[]{1}, true);
        else f.input.beginInputJudge(new int[]{0}, new int[]{1}, true);
        f.input.setKeyState(0, true, 1_000_000);
        f.input.setKeyState(1, true, 1_000_000);
        f.input.setKeyState(0, false, 1_000_000);
        f.input.setKeyState(1, false, 1_000_000);
        f.judge.update(1_000_000);
        check(f.judge.getJudgeCount(0) == 2 && f.judge.getPastNotes() == 2, "Same-time chord edges are neither reordered nor merged away");
    }

    private static void testReplayLegacy() throws Exception {
        var f = normal(false);
        f.input.setKeyState(0, true, 1_000_000);
        f.input.setKeyState(0, false, 1_000_000);
        f.judge.update(1_000_000);
        check(f.judge.getPastNotes() == 0, "Legacy replay keeps the original last-state semantics");
        f.judge.update(1_500_000);
        check(f.judge.getJudgeCount(4) == 1, "Legacy replay's original miss is not silently reinterpreted");
    }

    private static void testTimingWindows() throws Exception {
        for (long offset : new long[] {-120000, -30000, -1, 0, 1, 30000, 120000, 180000}) {
            var legacy = normal(false);
            var queued = normal(true);
            for (var f : new Fixture[] {legacy, queued}) {
                f.input.setKeyState(0, true, 1_000_000 + offset);
                f.judge.update(1_000_000 + offset);
                f.judge.update(1_500_000);
            }
            for (int rank = 0; rank <= 5; rank++) check(legacy.judge.getJudgeCount(rank) == queued.judge.getJudgeCount(rank),
                    "Keep judgment-window classification at offset=" + offset + ", rank=" + rank);
        }
        var future = normal(true);
        var oldFuture = normal(false);
        for (var f : new Fixture[] {future, oldFuture}) {
            f.input.setKeyState(0, true, 1_005_000);
            f.judge.update(1_000_000);
        }
        check(future.judge.getJudgeCount(0) == oldFuture.judge.getJudgeCount(0),
                "Keep poll-time judgment even if the gameplay clock is slightly behind");
    }

    private static void testScratchEnds() throws Exception {
        for (int type : new int[] {LongNote.TYPE_LONGNOTE, LongNote.TYPE_CHARGENOTE, LongNote.TYPE_HELLCHARGENOTE}) {
            Fixture[] fixtures = new Fixture[2];
            for (int i = 0; i < 2; i++) {
                BMSModel model = model();
                TimeLine a = line(1_000_000), b = line(2_000_000);
                LongNote start = new LongNote(0), end = new LongNote(0);
                start.setType(type);
                end.setType(type);
                a.setNote(7, start);
                b.setNote(7, end);
                start.setPair(end);
                model.setAllTimeLine(new TimeLine[] {a, b});
                var f = fixtures[i] = new Fixture(model, i == 1);
                f.input.setKeyState(7, true, 1_000_000);
                f.judge.update(1_000_000);
                f.input.setKeyState(7, false, 2_000_000);
                f.input.setKeyState(8, true, 2_000_000);
                f.judge.update(2_000_000);
                f.judge.update(3_000_000);
            }
            for (int rank = 0; rank <= 5; rank++) check(fixtures[0].judge.getJudgeCount(rank) == fixtures[1].judge.getJudgeCount(rank),
                    "Keep scratch-end and opposite-direction judgment rules: type=" + type + ", rank=" + rank);
        }
    }

    private static Fixture normal(boolean queued) throws Exception {
        BMSModel model = model();
        TimeLine line = line(1_000_000);
        line.setNote(0, new NormalNote(0));
        model.setAllTimeLine(new TimeLine[] {line});
        return new Fixture(model, queued);
    }

    private static Fixture longNote(boolean queued, int type) throws Exception {
        BMSModel model = model();
        TimeLine start = line(1_000_000), end = line(2_000_000);
        LongNote a = new LongNote(0), b = new LongNote(0);
        a.setType(type);
        b.setType(type);
        start.setNote(0, a);
        end.setNote(0, b);
        a.setPair(b);
        model.setAllTimeLine(new TimeLine[] {start, end});
        return new Fixture(model, queued);
    }

    private static BMSModel model() {
        var model = new BMSModel();
        model.setMode(Mode.BEAT_7K);
        model.setJudgerank(100);
        model.setTotal(300);
        model.setBpm(120);
        model.setSHA256("0".repeat(64));
        model.setChartInformation(new ChartInformation(Path.of("synthetic.bms"), BMSModel.LNTYPE_LONGNOTE, new int[0]));
        return model;
    }

    private static TimeLine line(long time) { return new TimeLine(0, time, Mode.BEAT_7K.key); }

    private static final class Fixture {
        final BMSPlayerInputProcessor input;
        final JudgeManager judge;
        Fixture(BMSModel model, boolean queued) throws Exception {
            var config = new Config();
            config.setAudioConfig(new AudioConfig());
            var playerConfig = new PlayerConfig();
            playerConfig.setNotesDisplayTimingAutoAdjust(false);
            input = KeyboardQueueTestSupport.input(playerConfig.getMode7().getKeyboardConfig());
            int[] keys = queued ? java.util.stream.IntStream.range(0, 9).toArray() : null;
            if (eventSource == 2) input.beginInputJudge(null, null, keys, true);
            else if (eventSource == 1) input.beginInputJudge(null, keys, true);
            else input.beginKeyboardJudge(keys, true);
            var audio = (AudioDriver) Proxy.newProxyInstance(AudioDriver.class.getClassLoader(), new Class<?>[] {AudioDriver.class},
                    (proxy, method, values) -> {
                        if (method.getReturnType() == boolean.class) return false;
                        if (method.getReturnType() == float.class) return 1f;
                        return null;
                    });
            var controller = KeyboardQueueTestSupport.allocate(MainController.class);
            set(controller, MainController.class, "input", input);
            set(controller, MainController.class, "audio", audio);
            set(controller, MainController.class, "config", config);
            set(controller, MainController.class, "player", playerConfig);
            var resource = KeyboardQueueTestSupport.allocate(PlayerResource.class);
            set(resource, PlayerResource.class, "config", config);
            set(resource, PlayerResource.class, "pconfig", playerConfig);
            set(resource, PlayerResource.class, "orgmode", Mode.BEAT_7K);
            set(resource, PlayerResource.class, "mode", BMSPlayerMode.PLAY);
            var player = KeyboardQueueTestSupport.allocate(TestPlayer.class);
            set(player, MainState.class, "main", controller);
            set(player, MainState.class, "timer", new TimerManager());
            set(player, MainState.class, "resource", resource);
            set(player, MainState.class, "skin", KeyboardQueueTestSupport.allocate(PlaySkin.class));
            player.lanes = new LaneProperty(Mode.BEAT_7K);
            player.gauge = new GrooveGauge(model, GrooveGauge.NORMAL, GaugeProperty.SEVENKEYS);
            player.input = KeyboardQueueTestSupport.allocate(TestKeyInput.class);
            judge = player.judge = new JudgeManager(player);
            judge.init(model, resource);
        }
    }

    private static final class TestPlayer extends BMSPlayer {
        LaneProperty lanes;
        GrooveGauge gauge;
        TestKeyInput input;
        JudgeManager judge;
        private TestPlayer() { super(null, null); }
        @Override public void update(int judge, long time) {}
        @Override public LaneProperty getLaneProperty() { return lanes; }
        @Override public GrooveGauge getGauge() { return gauge; }
        @Override public KeyInputProccessor getKeyinput() { return input; }
        @Override public JudgeManager getJudgeManager() { return judge; }
    }

    private static final class TestKeyInput extends KeyInputProccessor {
        private TestKeyInput() { super(null, null); }
        @Override public void inputKeyOn(int lane) {}
    }

    private static void set(Object target, Class<?> owner, String name, Object value) throws Exception {
        KeyboardQueueTestSupport.set(target, owner, name, value);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
