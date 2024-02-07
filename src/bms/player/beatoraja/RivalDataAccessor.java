package bms.player.beatoraja;

import java.io.File;
import java.nio.file.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.badlogic.gdx.utils.Array;

import bms.player.beatoraja.ScoreDatabaseAccessor.ScoreDataCollector;
import bms.player.beatoraja.external.ScoreDataImporter;
import bms.player.beatoraja.ir.*;
import bms.player.beatoraja.select.ScoreDataCache;
import bms.player.beatoraja.song.SongData;

/**
 * ライバルデータ管理用
 * 
 * @author exch
 */
public final class RivalDataAccessor {

	/**
	 * ライバル情報
	 */
	private PlayerInformation[] rivals = new PlayerInformation[0];
	/**
	 * ライバルスコアデータキャッシュ
	 */
	private ScoreDataCache[] rivalcaches = new ScoreDataCache[0];

	/**
	 * ライバル情報を取得する
	 * 
	 * @param index インデックス
	 * @return ライバル情報
	 */
	public PlayerInformation getRivalInformation(int index) {
		return index >= 0 && index < rivals.length ? rivals[index] : null;
	}
	
	/**
	 * ライバルスコアデータキャッシュを取得する
	 * 
	 * @param index インデックス
	 * @return ライバルスコアデータキャッシュ
	 */
	public ScoreDataCache getRivalScoreDataCache(int index) {
		return index >= 0 && index < rivalcaches.length ? rivalcaches[index] : null;
	}
	
	/**
	 * ライバル数を取得する
	 * 
	 * @return ライバル数
	 */
	public int getRivalCount() {
		return rivals.length;
	}

	public void update(MainController main) {
		if(main.getIRStatus().length > 0) {
			if(main.getIRStatus()[0].config.isImportscore()) {
				main.getIRStatus()[0].config.setImportscore(false);
				try {
					IRResponse<IRScoreData[]> scores = main.getIRStatus()[0].connection.getPlayData(main.getIRStatus()[0].player, null);
					if(scores.isSucceeded()) {
						ScoreDataImporter scoreimport = new ScoreDataImporter(new ScoreDatabaseAccessor(main.getConfig().getPlayerpath() + File.separatorChar + main.getConfig().getPlayername() + File.separatorChar + "score.db"));
						scoreimport.importScores(convert(scores.getData()), main.getIRStatus()[0].config.getIrname());

						Logger.getGlobal().info("IRからのスコアインポート完了");
					} else {
						Logger.getGlobal().warning("IRからのスコアインポート失敗 : " + scores.getMessage());
					}					
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
			
			IRResponse<IRPlayerData[]> response = main.getIRStatus()[0].connection.getRivals();
			if(response.isSucceeded()) {
				try {
					
					// ライバルスコアデータベース作成
					// TODO 別のクラスに移動
					if(!Files.exists(Paths.get("rival"))) {
						Files.createDirectory(Paths.get("rival"));
					}

					// ライバルキャッシュ作成
					Array<PlayerInformation> rivals = new Array<PlayerInformation>();
					Array<ScoreDataCache> rivalcaches = new Array<ScoreDataCache>();
					
					if(main.getIRStatus()[0].config.isImportrival()) {
						for(IRPlayerData irplayer : response.getData()) {
							final PlayerInformation rival = new PlayerInformation();
							rival.setId(irplayer.id);
							rival.setName(irplayer.name);
							rival.setRank(irplayer.rank);
							final ScoreDatabaseAccessor scoredb = new ScoreDatabaseAccessor("rival/" + main.getIRStatus()[0].config.getIrname() + rival.getId() + ".db");
							
							rivals.add(rival);
							rivalcaches.add(new ScoreDataCache() {

								@Override
								protected ScoreData readScoreDatasFromSource(SongData song, int lnmode) {
									return scoredb.getScoreData(song.getSha256(), song.hasUndefinedLongNote() ? lnmode : 0);
								}

								protected void readScoreDatasFromSource(ScoreDataCollector collector, SongData[] songs, int lnmode) {
									scoredb.getScoreDatas(collector,songs, lnmode);
								}
							});
							new Thread(() -> {
								scoredb.createTable();
								scoredb.setInformation(rival);
								IRResponse<IRScoreData[]> scores = main.getIRStatus()[0].connection.getPlayData(irplayer, null);
								if(scores.isSucceeded()) {
									scoredb.setScoreData(convert(scores.getData()));
									Logger.getGlobal().info("IRからのライバルスコア取得完了 : " + rival.getName());
								} else {
									Logger.getGlobal().warning("IRからのライバルスコア取得失敗 : " + scores.getMessage());
								}
							}).start();
						}
					}
					
					try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get("rival"))) {
						for (Path p : paths) {
							boolean exists = false;
							for(PlayerInformation info : rivals) {
								if(p.getFileName().toString().equals(main.getIRStatus()[0].config.getIrname() + info.getId() + ".db")) {
									exists = true;
									break;
								}
							}
							if(exists) {
								continue;
							}
							
							if(p.toString().endsWith(".db")) {
								final ScoreDatabaseAccessor scoredb = new ScoreDatabaseAccessor(p.toString());
								PlayerInformation info = scoredb.getInformation();
								if(info != null) {
									rivals.add(info);
									rivalcaches.add(new ScoreDataCache() {

										@Override
										protected ScoreData readScoreDatasFromSource(SongData song, int lnmode) {
											return scoredb.getScoreData(song.getSha256(), song.hasUndefinedLongNote() ? lnmode : 0);
										}

										protected void readScoreDatasFromSource(ScoreDataCollector collector, SongData[] songs, int lnmode) {
											scoredb.getScoreDatas((song, score) -> {
												if(score != null) {
													score.setPlayer(info.getName());
												}
												collector.collect(song, score);
											},songs, lnmode);
										}
									});
									Logger.getGlobal().info("ローカルに保存されているライバルスコア取得完了 : " + info.getName());
								}
							}
						}
					} catch (Throwable e) {
						e.printStackTrace();
					}
					this.rivals = rivals.toArray(PlayerInformation.class);
					this.rivalcaches = rivalcaches.toArray(ScoreDataCache.class);
					
//					Array<String> targets = new Array<String>(TargetProperty.getTargets());
//					for(int i = 0;i < this.rivals.length;i++) {
//						targets.add("RIVAL_" + (i + 1));
//					}
//					TargetProperty.setTargets(targets.toArray(String.class));

				} catch (Throwable e) {
					e.printStackTrace();
				}
			} else {
				Logger.getGlobal().warning("IRからのライバル取得失敗 : " + response.getMessage());
			}
		}
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
}
