package bms.player.beatoraja.skin.lr2;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.IRConfig;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlayDataAccessor;
import bms.player.beatoraja.ScoreData;
import bms.player.beatoraja.ScoreDatabaseAccessor;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import bms.player.beatoraja.ir.IRResponse;
import bms.player.beatoraja.ir.IRReview;
import bms.player.beatoraja.ir.IRReviewSync;
import bms.player.beatoraja.ir.TestIRConnectionBase;
import bms.player.beatoraja.select.BarManager;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.select.bar.Bar;
import bms.player.beatoraja.select.bar.SongBar;
import bms.player.beatoraja.skin.Skin;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinText;
import bms.player.beatoraja.skin.json.JsonSkin;
import bms.player.beatoraja.skin.json.JsonSkinSerializer;
import bms.player.beatoraja.skin.property.StringPropertyFactory;
import bms.player.beatoraja.song.SQLiteSongDatabaseAccessor;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongDatabaseAccessor;
import bms.player.beatoraja.song.SongReview;
import bms.player.beatoraja.song.SongReviewAccessor;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.Json;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static bms.player.beatoraja.skin.SkinProperty.STRING_LEVELREVIEW;

public final class PlayerReviewLevelTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("player-review-level");
        try {
            verify(directory);
        } finally {
            try (var files = Files.walk(directory)) {
                for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        }
        System.out.println("PlayerReviewLevelTest passed (LR2 ID 29, named property, edit binding, per-player SQLite persistence)");
    }

    private static void verify(Path directory) throws Exception {
        var songs = directory.resolve("songdata.db").toString();
        var reviews = directory.resolve("songreview.db").toString();
        var database = new SQLiteSongDatabaseAccessor(songs, new String[0], reviews);
        var first = chart(directory, "a");
        var second = chart(directory, "b");
        database.setSongDatas(new SongData[]{first, second});
        var accessor = new SongReviewAccessor(reviews);
        var existing = first.getSongReview();
        existing.setLevelreview(12.5);
        existing.setTag("tag");
        existing.setComment("comment");
        existing.setFavorite(SongReview.FAVORITE_CHART);
        accessor.setSongReview(existing);
        first = database.getSongDatas("sha256", first.getSha256())[0];
        second = database.getSongDatas("sha256", second.getSha256())[0];

        var selector = KeyboardQueueTestSupport.allocate(MusicSelector.class);
        var bars = KeyboardQueueTestSupport.allocate(BarManager.class);
        KeyboardQueueTestSupport.set(selector, MusicSelector.class, "manager", bars);
        KeyboardQueueTestSupport.set(selector, MusicSelector.class, "songdb", database);
        KeyboardQueueTestSupport.set(bars, BarManager.class, "currentsongs", new Bar[]{
                new SongBar(first), new SongBar(second), null, new SongBar(new SongData())});

        var property = StringPropertyFactory.getStringProperty(STRING_LEVELREVIEW);
        var writer = StringPropertyFactory.getStringWriter(STRING_LEVELREVIEW);
        check(STRING_LEVELREVIEW == 29 && property == StringPropertyFactory.getStringProperty("levelreview")
                && writer == StringPropertyFactory.getStringWriter("levelreview"), "ID and name resolve the same property");
        check(property.get(selector).equals("12.5"), "Existing REAL value is displayed");
        verifyLoaders(selector, directory);

        var edit = writer.bind(selector);
        select(bars, 1);
        check(property.get(selector).isEmpty(), "Unrated chart is blank");
        edit.set(selector, " 16.250 ");
        var persisted = accessor.getSongReview(first.getSha256());
        check(persisted.getLevelreview() == 16.25 && first.getSongReview().getLevelreviewText().equals("16.25")
                && second.getSongReview().getLevelreview() == 0, "Commit updates the originally edited chart");
        check(persisted.getFavorite() == SongReview.FAVORITE_CHART && persisted.getTag().equals("tag")
                && persisted.getComment().equals("comment"), "Only levelreview is changed");
        check(database.getSongDatas("sha256", first.getSha256())[0].getSongReview().getLevelreview() == 16.25,
                "Previously loaded review cache sees update");
        check(new SongReviewAccessor(directory.resolve("other-player.db").toString())
                .getSongReview(first.getSha256()) == null, "Another player's DB is untouched");

        select(bars, 0);
        for (String invalid : new String[]{null, "-1", "NaN", "Infinity", "1e999", "1e-999", "1,2", "text"}) {
            writer.set(selector, invalid);
            check(property.get(selector).equals("16.25"), "Invalid input leaves the previous rating intact");
        }
        writer.set(selector, " ");
        check(property.get(selector).isEmpty() && accessor.getSongReview(first.getSha256()).getLevelreview() == 0,
                "Blank input clears the saved review level");
        for (int index : new int[]{2, 3}) {
            select(bars, index);
            check(property.get(selector).isEmpty() && writer.bind(selector) == null, "No editor without a local chart");
        }
        select(bars, 0);
        verifySync(selector, database, accessor, first.getSha256(), directory);
    }

    private static void verifySync(MusicSelector selector, SongDatabaseAccessor database, SongReviewAccessor accessor,
            String hash, Path directory) throws Exception {
        var received = new LinkedBlockingQueue<IRReview>();
        var primary = new IRConfig();
        primary.setSyncReviewLevel(true);
        var connection = new TestIRConnectionBase() {
            @Override public IRResponse<Object> setReview(IRReview review) {
                check(accessor.getSongReview(hash).getLevelreview() == review.level(), "Database commit precedes upload");
                received.add(review);
                return null; // Even a failed response must not roll back the saved level.
            }
        };
        try (var sync = IRReviewSync.forPrimary(new IRConfig[]{primary},
                new MainController.IRStatus[]{new MainController.IRStatus(primary, connection, null)})) {
            var main = KeyboardQueueTestSupport.allocate(MainController.class);
            var player = new PlayerConfig();
            player.setLnmode(2);
            KeyboardQueueTestSupport.set(main, MainController.class, "player", player);
            var scoreDb = new ScoreDatabaseAccessor(directory.resolve("score.db").toString());
            scoreDb.createTable();
            var playData = KeyboardQueueTestSupport.allocate(PlayDataAccessor.class);
            KeyboardQueueTestSupport.set(playData, PlayDataAccessor.class, "scoredb", scoreDb);
            KeyboardQueueTestSupport.set(main, MainController.class, "playdata", playData);
            KeyboardQueueTestSupport.set(main, MainController.class, "reviewSync", sync);
            KeyboardQueueTestSupport.set(selector, MainState.class, "main", main);
            var writer = StringPropertyFactory.getStringWriter(STRING_LEVELREVIEW);
            writer.set(selector, "8.5");
            check(new IRReview(hash, 8.5, null, null, 0).equals(received.poll(5, TimeUnit.SECONDS)),
                    "Commit queues only SHA-256 and level, not existing private comment or favorite");
            var selected = ((SongBar) selector.getSelectedBar()).getSongData();
            selected.setFeature(selected.getFeature() | SongData.FEATURE_UNDEFINEDLN);
            player.setLnmode(1);
            check(main.getReviewLnMode(selected) == 1, "Without a local play record use the configured mode");
            recordPlay(scoreDb, hash, 2, 10);
            var bound = writer.bind(selector);
            recordPlay(scoreDb, hash, 1, 20);
            bound.set(selector, "9.5");
            check(new IRReview(hash, 9.5, null, null, 2).equals(received.poll(5, TimeUnit.SECONDS)),
                    "The play LN mode is captured at edit start when the chart has undefined long notes");
            writer.set(selector, "9.50");
            writer.set(selector, "invalid");
            var failing = (SongDatabaseAccessor) Proxy.newProxyInstance(PlayerReviewLevelTest.class.getClassLoader(),
                    new Class<?>[]{SongDatabaseAccessor.class}, (proxy, method, arguments) -> {
                        check(method.getName().equals("setSongReviewLevel"), "Only level persistence should be attempted");
                        return false;
                    });
            KeyboardQueueTestSupport.set(selector, MusicSelector.class, "songdb", failing);
            writer.set(selector, "10");
            check(received.poll(250, TimeUnit.MILLISECONDS) == null && accessor.getSongReview(hash).getLevelreview() == 9.5,
                    "Unchanged, invalid or unsuccessfully saved values are not uploaded; failed IR does not undo local save");
            KeyboardQueueTestSupport.set(selector, MusicSelector.class, "songdb", database);
            writer.set(selector, "");
            check(new IRReview(hash, 0.0, null, null, 1).equals(received.poll(5, TimeUnit.SECONDS)),
                    "Clearing a committed review sends a level-zero update");
        }
    }

    private static void recordPlay(ScoreDatabaseAccessor database, String hash, int mode, long date) {
        var score = new ScoreData();
        score.setSha256(hash);
        score.setMode(mode);
        score.setPlaycount(1);
        score.setDate(date);
        database.setScoreData(score);
    }

    private static void verifyLoaders(MusicSelector selector, Path directory) throws IOException {
        var header = new SkinHeader();
        header.setSourceResolution(Resolution.HD);
        header.setDestinationResolution(Resolution.HD);
        var fixture = new Skin(header);
        var csv = directory.resolve("text.csv");
        Files.writeString(csv, """
                #SRC_TEXT,0,0,29,0,1,0
                #DST_TEXT,0,0,10,10,100,20,0,255,255,255,255
                #SRC_TEXT,1,0,29,0,0,0
                #DST_TEXT,1,0,10,40,100,20,0,255,255,255,255
                """);
        var loader = new LR2SkinCSVLoader<Skin>(Resolution.HD, new Config()) {
            @Override
            public Skin loadSkin(MainState state, SkinHeader ignored, IntIntMap options) throws IOException {
                this.skin = fixture;
                loadSkin0(fixture, csv, state, options);
                return fixture;
            }
        };
        try {
            loader.loadSkin(selector, header, new IntIntMap());
            var editable = (SkinText) fixture.getAllSkinObjects()[0];
            var readOnly = (SkinText) fixture.getAllSkinObjects()[1];
            editable.prepare(0, selector);
            check(editable.getCurrentText().equals("12.5") && editable.isEditable() && editable.getWriter() != null,
                    "LR2 ID 29 uses the same property even with the fallback font");
            check(!readOnly.isEditable() && readOnly.getWriter() == null, "LR2 edit flag still controls editing");
        } finally {
            fixture.dispose();
        }
        var json = new Json();
        new JsonSkinSerializer(null, File::new).setSerializers(json, null, directory.resolve("skin.json"));
        var named = json.fromJson(JsonSkin.Text.class, "{value:levelreview,event:levelreview,editable:true}");
        check(named.value.get(selector).equals("12.5") && named.event == StringPropertyFactory.getStringWriter(29),
                "JSON can reference the named reader and writer");
    }

    private static SongData chart(Path directory, String id) {
        var song = new SongData();
        song.setSha256(id.repeat(64));
        song.setMd5(id.repeat(32));
        song.setTitle(id);
        song.setPath(directory.resolve(id + ".bms").toString());
        return song;
    }

    private static void select(BarManager manager, int index) throws Exception {
        KeyboardQueueTestSupport.set(manager, BarManager.class, "selectedindex", index);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
