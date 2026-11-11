package bms.player.beatoraja;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.badlogic.gdx.utils.Array;

import bms.player.beatoraja.MainController.IRStatus;
import bms.player.beatoraja.ScoreDatabaseAccessor.ScoreDataCollector;
import bms.player.beatoraja.external.ScoreDataImporter;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.ir.IRPlayerData;
import bms.player.beatoraja.ir.IRResponse;
import bms.player.beatoraja.ir.IRScoreData;
import bms.player.beatoraja.ir.IRScoreDataSyncResponse;
import bms.player.beatoraja.select.ScoreDataCache;
import bms.player.beatoraja.song.SongData;

/**
 * ライバルデータ管理用
 *
 * @author exch
 */
public final class RivalDataAccessor implements AutoCloseable {

	private final Path rivalDirectory;
	private final ExecutorService syncExecutor = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "rival-score-sync");
		thread.setDaemon(true);
		return thread;
	});
	private final AtomicBoolean syncScheduled = new AtomicBoolean();

	private volatile RivalData[] rivals = new RivalData[0];

	public RivalDataAccessor() {
		this(Paths.get("rival"));
	}

	RivalDataAccessor(Path rivalDirectory) {
		this.rivalDirectory = rivalDirectory;
	}

	public PlayerInformation getRivalInformation(int index) {
		RivalData[] current = rivals;
		return index >= 0 && index < current.length ? current[index].information() : null;
	}

	public ScoreDataCache getRivalScoreDataCache(int index) {
		RivalData[] current = rivals;
		return index >= 0 && index < current.length ? current[index].scores() : null;
	}

	public int getRivalCount() {
		return rivals.length;
	}

	/**
	 * ローカルライバルの読み込みとIRからのスコア同期をバックグラウンドで開始する。
	 * 同期中に重複して呼ばれても、新しい同期は追加しない。
	 */
	public void update(MainController main) {
		if (!syncScheduled.compareAndSet(false, true)) {
			return;
		}

		try {
			syncExecutor.execute(() -> {
				try {
					IRStatus[] statuses = main.getIRStatus();
					updateInBackground(main.getConfig(), statuses.length > 0 ? statuses[0] : null);
				} finally {
					syncScheduled.set(false);
				}
			});
		} catch (RejectedExecutionException exception) {
			syncScheduled.set(false);
		}
	}

	void updateInBackground(Config config, IRStatus status) {
		if (status != null && status.config.isImportscore()) {
			status.config.setImportscore(false);
			importPlayerScores(config, status.connection, status.player, status.config.getIrname());
		}

		try {
			Files.createDirectories(rivalDirectory);
			Array<RivalData> updatedRivals = new Array<>();
			var syncTargets = new ArrayList<RivalSyncTarget>();
			var syncedFiles = new HashSet<Path>();
			if (status != null && status.config.isImportrival()) {
				for (IRPlayerData irPlayer : fetchRivals(status.connection)) {
					if (irPlayer == null) {
						continue;
					}
					PlayerInformation rival = createRival(irPlayer);
					Path path = Paths.get(rivalDirectory.toString(), status.config.getIrname() + rival.getId() + ".db");
					ScoreDatabaseAccessor scoredb = new ScoreDatabaseAccessor(path.toString());
					updatedRivals.add(new RivalData(rival, createScoreCache(scoredb, null)));
					syncTargets.add(new RivalSyncTarget(irPlayer, rival, scoredb));
					syncedFiles.add(path);
				}
			}

			loadLocalRivals(syncedFiles, updatedRivals);
			rivals = updatedRivals.toArray(RivalData.class);

			for (RivalSyncTarget target : syncTargets) {
				if (Thread.currentThread().isInterrupted()) {
					return;
				}
				syncRivalScores(status.connection, target);
			}
		} catch (Exception exception) {
			Logger.getGlobal().warning("ライバルスコア同期の初期化失敗 : " + exception.getMessage());
		}
	}

	private IRPlayerData[] fetchRivals(IRConnection connection) {
		try {
			IRResponse<IRPlayerData[]> response = connection.getRivals();
			if (response != null && response.isSucceeded()) {
				IRPlayerData[] data = response.getData();
				return data != null ? data : new IRPlayerData[0];
			}
			Logger.getGlobal().warning("IRからのライバル取得失敗 : " + (response != null ? response.getMessage() : "応答なし"));
		} catch (RuntimeException exception) {
			Logger.getGlobal().warning("IRからのライバル取得失敗 : " + exception.getMessage());
		}
		return new IRPlayerData[0];
	}

	private void importPlayerScores(Config config, IRConnection connection, IRPlayerData player, String irName) {
		try {
			IRResponse<IRScoreData[]> scores = connection.getPlayData(player, null);
			if (scores.isSucceeded()) {
				var scoredb = new ScoreDatabaseAccessor(config.getPlayerpath() + File.separatorChar + config.getPlayername() + File.separatorChar + "score.db");
				new ScoreDataImporter(scoredb).importScores(convert(scores.getData()), irName);
				Logger.getGlobal().info("IRからのスコアインポート完了");
			} else {
				Logger.getGlobal().warning("IRからのスコアインポート失敗 : " + scores.getMessage());
			}
		} catch (Exception exception) {
			Logger.getGlobal().warning("IRからのスコアインポート失敗 : " + exception.getMessage());
		}
	}

	private PlayerInformation createRival(IRPlayerData irPlayer) {
		PlayerInformation rival = new PlayerInformation();
		rival.setId(irPlayer.id);
		rival.setName(irPlayer.name);
		rival.setRank(irPlayer.rank);
		return rival;
	}

	private ScoreDataCache createScoreCache(ScoreDatabaseAccessor scoredb, String playerName) {
		return new ScoreDataCache() {
			@Override
			protected ScoreData readScoreDatasFromSource(SongData song, int lnmode) {
				return scoredb.getScoreData(song.getSha256(), song.hasUndefinedLongNote() ? lnmode : 0);
			}

			@Override
			protected void readScoreDatasFromSource(ScoreDataCollector collector, SongData[] songs, int lnmode) {
				scoredb.getScoreDatas((song, score) -> {
					if (score != null && playerName != null) {
						score.setPlayer(playerName);
					}
					collector.collect(song, score);
				}, songs, lnmode);
			}
		};
	}

	private void syncRivalScores(IRConnection connection, RivalSyncTarget target) {
		try {
			target.scoredb.createTable();
			PlayerInformation savedRival = target.scoredb.getInformation();
			if (savedRival != null) {
				target.rival.setSyncRevision(savedRival.getSyncRevision());
			}
			target.scoredb.setInformation(target.rival);

			long revision = target.rival.getSyncRevision();
			boolean hasMore;
			do {
				IRScoreDataSyncResponse scores = connection.getPlayDataSince(target.irPlayer, revision);
				if (!scores.isSucceeded()) {
					Logger.getGlobal().warning("IRからのライバルスコア取得失敗 : " + scores.getMessage());
					return;
				}

				target.scoredb.setScoreData(convert(scores.getData()));
				long nextRevision = scores.getScoreRevision();
				hasMore = scores.hasMore();
				if (hasMore && nextRevision <= revision) {
					Logger.getGlobal().warning("IRからのライバルスコア同期を中断 : リビジョンが進みません (" + target.rival.getName() + ")");
					return;
				}

				revision = nextRevision;
				target.rival.setSyncRevision(revision);
				target.scoredb.setInformation(target.rival);
			} while (hasMore && !Thread.currentThread().isInterrupted());

			if (!Thread.currentThread().isInterrupted()) {
				Logger.getGlobal().info("IRからのライバルスコア同期完了 : " + target.rival.getName());
			}
		} catch (Exception exception) {
			Logger.getGlobal().warning("IRからのライバルスコア同期を中断 : " + target.rival.getName() + " : " + exception.getMessage());
		}
	}

	private void loadLocalRivals(Set<Path> syncedFiles, Array<RivalData> updatedRivals) throws Exception {
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(rivalDirectory, "*.db")) {
			for (Path path : paths) {
				if (syncedFiles.contains(path)) {
					continue;
				}

				ScoreDatabaseAccessor scoredb = new ScoreDatabaseAccessor(path.toString());
				PlayerInformation info = scoredb.getInformation();
				if (info != null) {
					updatedRivals.add(new RivalData(info, createScoreCache(scoredb, info.getName())));
					Logger.getGlobal().info("ローカルに保存されているライバルスコア取得完了 : " + info.getName());
				}
			}
		}
	}

	@Override
	public void close() {
		syncExecutor.shutdownNow();
	}

	private ScoreData[] convert(IRScoreData[] irscores) {
		return Stream.of(irscores).map(irscore -> {
			final ScoreData score = new ScoreData();
			score.setSha256(irscore.sha256);
			score.setMode(irscore.lntype);
			score.setPlayer(irscore.player);
			score.setClear(irscore.clear.id);
			score.setDate(irscore.date);
			score.setEpg(irscore.epg);
			score.setLpg(irscore.lpg);
			score.setEgr(irscore.egr);
			score.setLgr(irscore.lgr);
			score.setEgd(irscore.egd);
			score.setLgd(irscore.lgd);
			score.setEbd(irscore.ebd);
			score.setLbd(irscore.lbd);
			score.setEpr(irscore.epr);
			score.setLpr(irscore.lpr);
			score.setEms(irscore.ems);
			score.setLms(irscore.lms);
			score.setCombo(irscore.maxcombo);
			score.setNotes(irscore.notes);
			score.setPassnotes(irscore.passnotes != 0 ? irscore.notes : irscore.passnotes);
			score.setMinbp(irscore.minbp);
			score.setAvgjudge(irscore.avgjudge);
			score.setOption(irscore.option);
			score.setSeed(irscore.seed);
			score.setAssist(irscore.assist);
			score.setGauge(irscore.gauge);
			score.setDeviceType(irscore.deviceType);
			return score;
		}).toArray(ScoreData[]::new);
	}

	private record RivalSyncTarget(IRPlayerData irPlayer, PlayerInformation rival, ScoreDatabaseAccessor scoredb) {
	}

	private record RivalData(PlayerInformation information, ScoreDataCache scores) {
	}
}
