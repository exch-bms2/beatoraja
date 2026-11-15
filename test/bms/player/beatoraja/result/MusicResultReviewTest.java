package bms.player.beatoraja.result;

import bms.model.BMSModel;
import bms.model.ChartInformation;
import bms.player.beatoraja.BMSPlayerMode;
import bms.player.beatoraja.IRConfig;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.ScoreData;
import bms.player.beatoraja.TimerManager;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import bms.player.beatoraja.ir.IRChartData;
import bms.player.beatoraja.ir.IRResponse;
import bms.player.beatoraja.ir.IRReview;
import bms.player.beatoraja.ir.IRReviewSync;
import bms.player.beatoraja.ir.IRScoreData;
import bms.player.beatoraja.ir.TestIRConnectionBase;
import bms.player.beatoraja.skin.property.BooleanPropertyFactory;
import bms.player.beatoraja.skin.property.StringPropertyFactory;
import bms.player.beatoraja.song.SQLiteSongDatabaseAccessor;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongReview;
import bms.player.beatoraja.song.SongReviewAccessor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static bms.player.beatoraja.skin.SkinProperty.TIMER_FADEOUT;

public final class MusicResultReviewTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("result-level-review");
        try {
            verify(directory);
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("MusicResultReviewTest passed (eligibility, IR wait, actual LN mode and offline persistence)");
    }

    private static void verify(Path directory) throws Exception {
        String hash = "a".repeat(64);
        var model = new BMSModel();
        model.setSHA256(hash);
        model.setChartInformation(new ChartInformation(directory.resolve("chart.bms"), 2, null));
        var song = new SongData();
        song.setSha256(hash);
        song.setFeature(SongData.FEATURE_UNDEFINEDLN);
        var resource = KeyboardQueueTestSupport.allocate(PlayerResource.class);
        resource.setSongdata(song);
        resource.setScoreData(new ScoreData());
        resource.setPlayMode(BMSPlayerMode.PLAY);
        KeyboardQueueTestSupport.set(resource, PlayerResource.class, "model", model);

        String reviewPath = directory.resolve("songreview.db").toString();
        var db = new SQLiteSongDatabaseAccessor(directory.resolve("songdata.db").toString(), new String[0], reviewPath);
        var reviews = new SongReviewAccessor(reviewPath);
        var existing = new SongReview();
        existing.setSha256(hash);
        existing.setLevelreview(12.5);
        existing.setComment("preserve me");
        reviews.setSongReview(existing);
        var main = KeyboardQueueTestSupport.allocate(MainController.class);
        var timer = new TimerManager();
        timer.setTimerOff(TIMER_FADEOUT);
        KeyboardQueueTestSupport.set(main, MainController.class, "timer", timer);
        KeyboardQueueTestSupport.set(main, MainController.class, "resource", resource);
        KeyboardQueueTestSupport.set(main, MainController.class, "songdb", db);
        var result = new MusicResult(main);

        var visible = BooleanPropertyFactory.getBooleanProperty("result_level_review_editable");
        var value = StringPropertyFactory.getStringProperty("levelreview");
        var writer = StringPropertyFactory.getStringWriter("levelreview");
        result.prepareLevelReview();
        result.state = AbstractResult.STATE_IR_PROCESSING;
        check(!visible.get(result) && writer.bind(result) == null, "Cannot edit while IR score upload is in progress");
        result.state = AbstractResult.STATE_IR_FINISHED;
        check(visible.get(result) && value.get(result).equals("12.5"), "Show persisted review after IR processing");
        check(result.getEditableLevelReview().lnmode() == 2, "Undefined LN uses the actual loaded chart's mode");
        timer.setTimerOn(TIMER_FADEOUT);
        check(!visible.get(result) && writer.bind(result) == null, "Cannot start editing during fadeout");
        timer.setTimerOff(TIMER_FADEOUT);
        result.state = AbstractResult.STATE_OFFLINE;
        check(visible.get(result), "No IR connection is required to edit");
        writer.set(result, "16.25");
        var saved = reviews.getSongReview(hash);
        check(saved.getLevelreview() == 16.25 && saved.getComment().equals("preserve me")
                && value.get(result).equals("16.25"), "Offline edit saves and refreshes only the local level");

        var reviewsSent = new LinkedBlockingQueue<IRReview>();
        var submitted = new IRChartData[1];
        var ir = new TestIRConnectionBase() {
            @Override public IRResponse<Object> sendPlayData(IRChartData chart, IRScoreData score) {
                submitted[0] = chart;
                return success();
            }
            @Override public IRResponse<Object> setReview(IRReview review) {
                reviewsSent.add(review);
                return success();
            }
        };
        var primary = new IRConfig();
        primary.setSyncReviewLevel(true);
        try (var sync = IRReviewSync.forPrimary(new IRConfig[]{primary},
                new MainController.IRStatus[]{new MainController.IRStatus(primary, ir, null)})) {
            KeyboardQueueTestSupport.set(main, MainController.class, "reviewSync", sync);
            verifyModeRouting(directory, hash, model, song, result, writer, ir, submitted, reviewsSent);
        }
        KeyboardQueueTestSupport.set(main, MainController.class, "reviewSync", null);
        song.setFeature(0);
        KeyboardQueueTestSupport.set(result, MusicResult.class, "resultChart", MusicResult.createResultChart(song, model));
        result.prepareLevelReview();
        check(result.getEditableLevelReview().lnmode() == 0, "Fixed LN charts use the shared score bucket");
        for (var mode : new BMSPlayerMode[]{BMSPlayerMode.REPLAY_1, BMSPlayerMode.AUTOPLAY, BMSPlayerMode.PRACTICE}) {
            resource.setPlayMode(mode);
            result.prepareLevelReview();
            check(!visible.get(result) && writer.bind(result) == null, "Non-normal plays are excluded");
        }
        resource.setPlayMode(BMSPlayerMode.PLAY);
        KeyboardQueueTestSupport.set(resource, PlayerResource.class, "course", new BMSModel[]{model});
        result.prepareLevelReview();
        check(!visible.get(result), "Course-stage results are excluded too");
    }

    private static void verifyModeRouting(Path directory, String hash, BMSModel model, SongData song, MusicResult result,
            bms.player.beatoraja.skin.property.StringWriter writer, TestIRConnectionBase ir, IRChartData[] submitted,
            LinkedBlockingQueue<IRReview> reviewsSent) throws Exception {
        for (int mode = 0; mode <= 2; mode++) {
            var played = new BMSModel();
            played.setMode(bms.model.Mode.BEAT_7K);
            played.setSHA256(hash);
            played.setChartInformation(new ChartInformation(directory.resolve("chart.bms"), mode, null));
            // The played branch may differ from the model retained by PlayerResource.
            model.setChartInformation(new ChartInformation(directory.resolve("chart.bms"), (mode + 1) % 3, null));
            KeyboardQueueTestSupport.set(song, SongData.class, "model", played);
            var chart = MusicResult.createResultChart(song, model);
            KeyboardQueueTestSupport.set(result, MusicResult.class, "resultChart", chart);
            result.prepareLevelReview();
            check(new MusicResult.IRSendStatus(ir, chart, new ScoreData()).send(), "Fixture score upload");
            writer.set(result, Integer.toString(20 + mode));
            IRReview review = reviewsSent.poll(5, TimeUnit.SECONDS);
            check(submitted[0].hasUndefinedLN && submitted[0].lntype == mode
                    && review != null && review.sha256().equals(hash) && review.lnmode() == mode,
                    "LN/CN/HCN review upload matches the submitted score");
        }
    }

    private static IRResponse<Object> success() {
        return new IRResponse<>() {
            public boolean isSucceeded() { return true; }
            public String getMessage() { return ""; }
            public Object getData() { return null; }
        };
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
