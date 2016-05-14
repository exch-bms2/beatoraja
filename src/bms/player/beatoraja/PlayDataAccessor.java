package bms.player.beatoraja;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.model.TimeLine;
import bms.player.beatoraja.gauge.GrooveGauge;
import bms.player.lunaticrave2.LunaticRave2ScoreDatabaseManager;

import bms.player.lunaticrave2.PlayerData;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

/**
 * プレイデータアクセス用クラス
 * 
 * @author exch
 */
public class PlayDataAccessor {

	/**
	 * プレイヤー名
	 */
	private String player;
	/**
	 * スコアデータベースアクセサ
	 */
	private LunaticRave2ScoreDatabaseManager scoredb;

	public PlayDataAccessor(String player) {
		this.player = player;

		try {
			Class.forName("org.sqlite.JDBC");
			scoredb = new LunaticRave2ScoreDatabaseManager(new File(".").getAbsoluteFile().getParent(), "/", "/");
			scoredb.createTable(player);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public PlayerData readPlayerData() {
		return scoredb.getPlayerDatas(player);
	}

	public void updatePlayerData(IRScoreData score, long time) {
		PlayerData pd = readPlayerData();
		pd.setPerfect(pd.getPerfect() + score.getPg());
		pd.setGreat(pd.getGreat() + score.getGr());
		pd.setGood(pd.getGood() + score.getGd());
		pd.setBad(pd.getBad()+ score.getBd());
		pd.setPoor(pd.getPoor() + score.getPr());

		pd.setPlaycount(pd.getPlaycount() + 1);
		if(score.getClear() > GrooveGauge.CLEARTYPE_FAILED) {
			pd.setClear(pd.getClear() + 1);
		} else {
			pd.setFail(pd.getFail() + 1);
		}
		pd.setPlaytime(pd.getPlaytime() + time);
		scoredb.setPlayerData(player, pd);
	}

	/**
	 * スコアデータを読み込む
	 * @param model　対象のモデル
	 * @param lnmode LNモード
     * @return スコアデータ
     */
	public IRScoreData readScoreData(BMSModel model, int lnmode) {
		String hash = model.getHash();
		boolean ln = model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
				+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		return readScoreData(hash, ln, lnmode);
	}

	/**
	 * スコアデータを読み込む
	 * @param ln 対象のbmsがLNを含む場合はtrueを入れる
	 * @param lnmode LNモード
     * @return スコアデータ
     */
	public IRScoreData readScoreData(String hash, boolean ln, int lnmode) {
		if (ln && lnmode > 0) {
			hash = "C" + hash;
		}
		return scoredb.getScoreData(player, hash, false);
	}

	/**
	 * スコアデータを書き込む
	 * @param newscore スコアデータ
	 * @param model　対象のモデル
	 * @param lnmode LNモード
	 * @param updateScore プレイ回数のみ反映する場合はfalse
     */
	public void writeScoreDara(IRScoreData newscore, BMSModel model, int lnmode, boolean updateScore) {
		String hash = model.getHash();
		boolean ln = model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
				+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		if (ln && lnmode > 0) {
			hash = "C" + hash;
		}
		if (newscore == null) {
			return;
		}
		IRScoreData score = scoredb.getScoreData(player, hash, false);
		if (score == null) {
			score = new IRScoreData();
		}
		int clear;
		if (ln && lnmode == 2) {
			clear = score.getExclear();
		} else {
			clear = score.getClear();
		}
		score.setHash(hash);
		score.setNotes(model.getTotalNotes());

		if (newscore.getClear() > GrooveGauge.CLEARTYPE_FAILED) {
			score.setClearcount(score.getClearcount() + 1);
		}
		if (clear < newscore.getClear()) {
			if (ln && lnmode == 2) {
				score.setExclear(newscore.getClear());
			} else {
				score.setClear(newscore.getClear());
			}
			score.setOption(newscore.getOption());
		}

		final int pgreat = newscore.getPg();
		final int great = newscore.getGr();
		final int good = newscore.getGd();
		final int bad = newscore.getBd();
		final int poor = newscore.getPr();
		int exscore = pgreat * 2 + great;
		if (score.getExscore() < exscore && updateScore) {
			score.setPg(pgreat);
			score.setGr(great);
			score.setGd(good);
			score.setBd(bad);
			score.setPr(poor);
		}
		if (score.getMinbp() > newscore.getMinbp() && updateScore) {
			score.setMinbp(newscore.getMinbp());
		}
		score.setPlaycount(score.getPlaycount() + 1);
		score.setLastupdate(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis() / 1000L);
		scoredb.setScoreData(player, score);

		int time = 0;
		for(TimeLine tl : model.getAllTimeLines()) {
			for(int i = 0;i < 18;i++) {
				if(tl.getNote(i) != null && tl.getNote(i).getState() != 0) {
					time =tl.getTime() / 1000;
				}
			}
		}
		updatePlayerData(newscore, time);
		Logger.getGlobal().info("スコアデータベース更新完了 ");

	}

	public IRScoreData readScoreData(String hash, boolean ln, int lnmode, boolean mirror) {
		if (ln && lnmode > 0) {
			hash = "C" + hash;
		}
		if (mirror) {
			hash = "M" + hash;
		}
		return scoredb.getScoreData(player, hash, false);
	}

	public IRScoreData readScoreData(BMSModel[] models, int lnmode, boolean mirror) {
		String hash = "";
		boolean ln = false;
		for (BMSModel model : models) {
			hash += model.getHash();
			ln |= model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
					+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		}
		return readScoreData(hash, ln, lnmode, mirror);
	}

	public void writeScoreDara(IRScoreData newscore, BMSModel[] models, int lnmode, boolean mirror, boolean updateScore) {
		String hash = "";
		int totalnotes = 0;
		boolean ln = false;
		for (BMSModel model : models) {
			hash += model.getHash();
			totalnotes += model.getTotalNotes();
			ln |= model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
					+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		}
		if (ln && lnmode > 0) {
			hash = "C" + hash;
		}
		if (mirror) {
			hash = "M" + hash;
		}
		if (newscore == null) {
			return;
		}
		IRScoreData score = scoredb.getScoreData(player, hash, false);
		if (score == null) {
			score = new IRScoreData();
		}
		int clear;
		if (ln && lnmode == 2) {
			clear = score.getExclear();
		} else {
			clear = score.getClear();
		}
		score.setHash(hash);
		score.setNotes(totalnotes);

		if (newscore.getClear() != GrooveGauge.CLEARTYPE_FAILED) {
			score.setClearcount(score.getClearcount() + 1);
		}
		if (clear < newscore.getClear()) {
			if (ln && lnmode == 2) {
				score.setExclear(newscore.getClear());
			} else {
				score.setClear(newscore.getClear());
			}
			score.setOption(newscore.getOption());
		}

		final int pgreat = newscore.getPg();
		final int great = newscore.getGr();
		final int good = newscore.getGd();
		final int bad = newscore.getBd();
		final int poor = newscore.getPr();
		int exscore = pgreat * 2 + great;
		if (score.getExscore() < exscore && updateScore) {
			score.setPg(pgreat);
			score.setGr(great);
			score.setGd(good);
			score.setBd(bad);
			score.setPr(poor);
		}
		if (score.getMinbp() > newscore.getMinbp() && updateScore) {
			score.setMinbp(newscore.getMinbp());
		}
		score.setPlaycount(score.getPlaycount() + 1);
		score.setLastupdate(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis() / 1000L);
		scoredb.setScoreData(player, score);

		Logger.getGlobal().info("スコアデータベース更新完了 ");

	}

	private static final String[] replay = {"", "C", "H"};

	public boolean existsReplayData(BMSModel model, int lnmode) {
		boolean ln = model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
				+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		return new File(this.getReplayDataFilePath(model.getHash(), ln, lnmode)).exists();
	}

	public boolean existsReplayData(String hash, boolean ln, int lnmode) {
		return new File(this.getReplayDataFilePath(hash, ln, lnmode)).exists();
	}

	/**
	 * リプレイデータを読み込む
	 * @param model 対象のBMS
	 * @param lnmode LNモード
     * @return リプレイデータ
     */
	public ReplayData readReplayData(BMSModel model, int lnmode) {
		if (existsReplayData(model, lnmode)) {
			Json json = new Json();
			try {
				return json.fromJson(ReplayData.class,
						new FileReader(this.getReplayDataFilePath(model, lnmode)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * リプレイデータを書き込む
	 * @param rd リプレイデータ
	 * @param model 対象のBMS
	 * @param lnmode LNモード
     */
	public void wrireReplayData(ReplayData rd, BMSModel model, int lnmode) {
		File replaydir = new File("replay");
		if (!replaydir.exists()) {
			replaydir.mkdirs();
		}
		Json json = new Json();
		json.setOutputType(OutputType.json);
		try {
			FileWriter fw = new FileWriter(this.getReplayDataFilePath(model, lnmode));
			fw.write(json.prettyPrint(rd));
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private String getReplayDataFilePath(BMSModel model, int lnmode) {
		boolean ln = model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
				+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		return getReplayDataFilePath(model.getHash(), ln, lnmode);
	}

	private String getReplayDataFilePath(String hash, boolean ln, int lnmode) {
		return "replay" + File.separatorChar +  (ln ? replay[lnmode] : "") + hash + ".json";
	}
}
