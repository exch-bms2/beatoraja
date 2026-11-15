package bms.player.beatoraja;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import bms.player.beatoraja.MainController.IRStatus;
import bms.player.beatoraja.ir.IRPlayerData;
import bms.player.beatoraja.ir.IRResponse;
import bms.player.beatoraja.ir.IRScoreData;
import bms.player.beatoraja.ir.IRScoreDataSyncResponse;
import bms.player.beatoraja.ir.TestIRConnectionBase;
import bms.player.beatoraja.song.SongData;

public final class RivalDataAccessorTest {
	private static final String CHART = "b".repeat(64);

	public static void main(String[] args) throws Exception {
		Path root = Files.createTempDirectory("beatoraja-rivals-");
		try {
			saveRival(root.resolve("FixtureIR42.db"), "42", "Cached IR rival", 12, 400);
			saveRival(root.resolve("local.db"), "local", "Local rival", -1, 500);
			Config config = new Config();
			IRConfig ir = new IRConfig();
			ir.setIrname("FixtureIR");

			checkOfflineRivals(root, config, null);

			ir.setImportrival(false);
			checkOfflineRivals(root, config, new IRStatus(ir, new TestIRConnectionBase() {
				@Override public IRResponse<IRPlayerData[]> getRivals() {
					throw new AssertionError("Disabled rival import must not request IR rivals");
				}
			}, null));

			ir.setImportrival(true);
			for (RivalResponse response : new RivalResponse[]{new RivalResponse(false, null), null, new RivalResponse(true, null)}) {
				checkOfflineRivals(root, config, new IRStatus(ir, new TestIRConnectionBase() {
					@Override public IRResponse<IRPlayerData[]> getRivals() {
						return response;
					}
				}, null));
			}

			checkOfflineRivals(root, config, new IRStatus(ir, new TestIRConnectionBase() {
				@Override public IRResponse<IRPlayerData[]> getRivals() {
					throw new IllegalStateException("Fixture connection failure");
				}
			}, null));
			try (var accessor = new RivalDataAccessor(root)) {
				accessor.updateInBackground(config, new IRStatus(ir, new TestIRConnectionBase() {
					@Override public IRResponse<IRPlayerData[]> getRivals() {
						return new RivalResponse(true, new IRPlayerData[]{new IRPlayerData("42", "Synced IR rival", "")});
					}
					@Override public IRScoreDataSyncResponse getPlayDataSince(IRPlayerData player, long revision) {
						check(player.id.equals("42") && revision == 12, "Incremental sync resumes from the cached revision");
						return new IRScoreDataSyncResponse(true, "", new IRScoreData[0], 13, false);
					}
				}, null));
				check(accessor.getRivalCount() == 2, "The synced DB is not listed a second time as a local rival");
				check(accessor.getRivalInformation(0).getSyncRevision() == 13, "New revision was applied");
				checkScore(accessor, "Synced IR rival", 800);
				checkScore(accessor, "Local rival", 1000);
			}
		} finally {
			try (var paths = Files.walk(root)) {
				for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
			}
		}
		System.out.println("RivalDataAccessorTest passed");
	}

	private static void checkOfflineRivals(Path root, Config config, IRStatus status) {
		try (var accessor = new RivalDataAccessor(root)) {
			accessor.updateInBackground(config, status);
			check(accessor.getRivalCount() == 2, "Both local DBs must be loaded");
			checkScore(accessor, "Cached IR rival", 800);
			checkScore(accessor, "Local rival", 1000);
		}
	}

	private static void checkScore(RivalDataAccessor accessor, String name, int exScore) {
		SongData song = new SongData();
		song.setSha256(CHART);
		for (int i = 0; i < accessor.getRivalCount(); i++) {
			if (name.equals(accessor.getRivalInformation(i).getName())) {
				ScoreData score = accessor.getRivalScoreDataCache(i).readScoreData(song, 0);
				check(score != null && score.getExscore() == exScore, "Expected stored score for " + name);
				return;
			}
		}
		throw new AssertionError("Missing rival " + name);
	}

	private static void saveRival(Path file, String id, String name, long revision, int epg) throws Exception {
		var db = new ScoreDatabaseAccessor(file.toString());
		db.createTable();
		var info = new PlayerInformation();
		info.setId(id);
		info.setName(name);
		info.setSyncRevision(revision);
		db.setInformation(info);
		var score = new ScoreData();
		score.setSha256(CHART);
		score.setNotes(500);
		score.setEpg(epg);
		db.setScoreData(score);
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	private record RivalResponse(boolean isSucceeded, IRPlayerData[] getData) implements IRResponse<IRPlayerData[]> {
		@Override public String getMessage() { return "Fixture response"; }
	}
}
