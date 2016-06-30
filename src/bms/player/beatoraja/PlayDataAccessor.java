package bms.player.beatoraja;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.model.TimeLine;
import bms.player.beatoraja.gauge.GrooveGauge;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

/**
 * プレイデータアクセス用クラス
 * 
 * @author exch
 */
public class PlayDataAccessor {
	
	// TODO スコアハッシュ付加
	// TODO リプレイ暗号、復号化
	
	/**
	 * プレイヤー名
	 */
	private String player;
	/**
	 * スコアデータベースアクセサ
	 */
	private ScoreDatabaseAccessor scoredb;

	private static final String[] replay = {"", "C", "H"};

	public PlayDataAccessor(String player) {
		this.player = player;

		try {
			Class.forName("org.sqlite.JDBC");
			scoredb = new ScoreDatabaseAccessor(new File(".").getAbsoluteFile().getParent(), "/", "/");
			scoredb.createTable(player);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public PlayerData readPlayerData() {
		return scoredb.getPlayerData(player);
	}

	public PlayerData readTodayPlayerData() {
		PlayerData[] pd = scoredb.getPlayerDatas(player, 2);
		if(pd.length > 1) {
			pd[0].setPlaycount(pd[0].getPlaycount() - pd[1].getPlaycount());
			pd[0].setClear(pd[0].getClear() - pd[1].getClear());
			pd[0].setEpg(pd[0].getEpg() - pd[1].getEpg() );
			pd[0].setLpg(pd[0].getLpg() - pd[1].getLpg());
			pd[0].setEgr(pd[0].getEgr() - pd[1].getEgr());
			pd[0].setLgr(pd[0].getLgr() - pd[1].getLgr());
			pd[0].setEgd(pd[0].getEgd() - pd[1].getEgd());
			pd[0].setLgd(pd[0].getLgd() - pd[1].getLgd());
			pd[0].setEbd(pd[0].getEbd() - pd[1].getEbd());
			pd[0].setLbd(pd[0].getLbd() - pd[1].getLbd());
			pd[0].setEpr(pd[0].getEpr() - pd[1].getEpr());
			pd[0].setLpr(pd[0].getLpr() - pd[1].getLpr());
			pd[0].setEms(pd[0].getEms() - pd[1].getEms());
			pd[0].setLms(pd[0].getLms() - pd[1].getLms());
			pd[0].setPlaytime(pd[0].getPlaytime() - pd[1].getPlaytime());
			return pd[0];
		} else if(pd.length == 1) {
			return pd[0];
		}
		return null;
	}

	public void updatePlayerData(IRScoreData score, long time) {
		PlayerData pd = readPlayerData();
		pd.setEpg(pd.getEpg() + score.getEpg());
		pd.setLpg(pd.getLpg() + score.getLpg());
		pd.setEgr(pd.getEgr() + score.getEgr());
		pd.setLgr(pd.getLgr() + score.getLgr());
		pd.setEgd(pd.getEgd() + score.getEgd());
		pd.setLgd(pd.getLgd() + score.getLgd());
		pd.setEbd(pd.getEbd() + score.getEbd());
		pd.setLbd(pd.getLbd() + score.getLbd());
		pd.setEpr(pd.getEpr() + score.getEpr());
		pd.setLpr(pd.getLpr() + score.getLpr());
		pd.setEms(pd.getEms() + score.getEms());
		pd.setLms(pd.getLms() + score.getLms());

		pd.setPlaycount(pd.getPlaycount() + 1);
		if(score.getClear() > GrooveGauge.CLEARTYPE_FAILED) {
			pd.setClear(pd.getClear() + 1);
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
		String hash = model.getSHA256();
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
		return scoredb.getScoreData(player, hash, ln ? lnmode : 0);
	}

	public Map<String, IRScoreData> readScoreDatas(SongData[] songs, int lnmode) {
		List<String> noln = new ArrayList<String>();
		List<String> ln = new ArrayList<String>();
		for(SongData song : songs) {
			if(song.hasLongNote()) {
				ln.add(song.getSha256());
			} else {
				noln.add(song.getSha256());
			}
		}
		Map<String, IRScoreData> result = scoredb.getScoreDatas(player, noln.toArray(new String[0]),0);
		result.putAll(scoredb.getScoreDatas(player, ln.toArray(new String[0]),lnmode));
		return result;
	}

	/**
	 * スコアデータを書き込む
	 * @param newscore スコアデータ
	 * @param model　対象のモデル
	 * @param lnmode LNモード
	 * @param updateScore プレイ回数のみ反映する場合はfalse
     */
	public void writeScoreDara(IRScoreData newscore, BMSModel model, int lnmode, boolean updateScore) {
		String hash = model.getSHA256();
		if (newscore == null) {
			return;
		}
		IRScoreData score = scoredb.getScoreData(player, hash, model.containsLongNote() ? lnmode : 0);
		if (score == null) {
			score = new IRScoreData();
			score.setMode(model.containsLongNote() ? lnmode : 0);
		}
		int clear = score.getClear();
		score.setSha256(hash);
		score.setNotes(model.getTotalNotes());

		if (newscore.getClear() > GrooveGauge.CLEARTYPE_FAILED) {
			score.setClearcount(score.getClearcount() + 1);
		}
		if (clear < newscore.getClear()) {
			score.setClear(newscore.getClear());
			score.setOption(newscore.getOption());
		}
		if(model.getUseKeys() < 10) {
			int history = score.getHistory();
			for(int i = 0;i < newscore.getOption();i++) {
				history /= 10;
			}
			if(history % 10 < newscore.getClear() - GrooveGauge.CLEARTYPE_LIGHT_ASSTST) {
				int add = newscore.getClear() - GrooveGauge.CLEARTYPE_LIGHT_ASSTST - (history % 10);
				for(int i = 0;i < newscore.getOption();i++) {
					add *= 10;
				}
				score.setHistory(score.getHistory() + add);
			}
		} else {
			// TODO DPのhistoryはどうする？			
		}

		if (score.getExscore() < newscore.getExscore() && updateScore) {
			score.setEpg(newscore.getEpg());
			score.setLpg(newscore.getLpg());
			score.setEgr(newscore.getEgr());
			score.setLgr(newscore.getLgr());
			score.setEgd(newscore.getEgd());
			score.setLgd(newscore.getLgd());
			score.setEbd(newscore.getEbd());
			score.setLbd(newscore.getLbd());
			score.setEpr(newscore.getEpr());
			score.setLpr(newscore.getLpr());
			score.setEms(newscore.getEms());
			score.setLms(newscore.getLms());
			score.setOption(newscore.getOption());
		}
		if (score.getMinbp() > newscore.getMinbp() && updateScore) {
			score.setMinbp(newscore.getMinbp());
			score.setOption(newscore.getOption());
		}
		if (score.getCombo() < newscore.getCombo() && updateScore) {
			score.setCombo(newscore.getCombo());
			score.setOption(newscore.getOption());
		}
		score.setPlaycount(score.getPlaycount() + 1);
		score.setDate(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis() / 1000L);
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

	public IRScoreData readScoreData(String hash, boolean ln, int lnmode, int option) {
		return scoredb.getScoreData(player, hash, (ln ? lnmode : 0) + option * 10);
	}

	public IRScoreData readScoreData(BMSModel[] models, int lnmode, int option) {
		String[] hash = new String[models.length];
		boolean ln = false;
		for (int i = 0;i < models.length;i++) {
			hash[i] = models[i].getSHA256();
			ln |= models[i].getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
					+ models[i].getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		}
		return readScoreData(hash, ln, lnmode, option);
	}

	public IRScoreData readScoreData(String[] hashes, boolean ln, int lnmode, int option) {
		String hash = "";
		for (String s : hashes) {
			hash += s;
		}
		return readScoreData(hash, ln, lnmode, option);
	}

	public void writeScoreDara(IRScoreData newscore, BMSModel[] models, int lnmode, int option, boolean updateScore) {
		String hash = "";
		int totalnotes = 0;
		boolean ln = false;
		for (BMSModel model : models) {
			hash += model.getSHA256();
			totalnotes += model.getTotalNotes();
			ln |= model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
					+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		}
		if (newscore == null) {
			return;
		}
		IRScoreData score = scoredb.getScoreData(player, hash, (ln ? lnmode : 0) + option * 10);
		if (score == null) {
			score = new IRScoreData();
			score.setMode((ln ? lnmode : 0) + option * 10);
		}
		int clear = score.getClear();
		score.setSha256(hash);
		score.setNotes(totalnotes);

		if (newscore.getClear() != GrooveGauge.CLEARTYPE_FAILED) {
			score.setClearcount(score.getClearcount() + 1);
		}
		if (clear < newscore.getClear()) {
			score.setClear(newscore.getClear());
			score.setOption(newscore.getOption());
		}

		if (score.getExscore() < newscore.getExscore() && updateScore) {
			score.setEpg(newscore.getEpg());
			score.setLpg(newscore.getLpg());
			score.setEgr(newscore.getEgr());
			score.setLgr(newscore.getLgr());
			score.setEgd(newscore.getEgd());
			score.setLgd(newscore.getLgd());
			score.setEbd(newscore.getEbd());
			score.setLbd(newscore.getLbd());
			score.setEpr(newscore.getEpr());
			score.setLpr(newscore.getLpr());
			score.setEms(newscore.getEms());
			score.setLms(newscore.getLms());
			score.setOption(newscore.getOption());
		}
		if (score.getMinbp() > newscore.getMinbp() && updateScore) {
			score.setMinbp(newscore.getMinbp());
			score.setOption(newscore.getOption());
		}
		if (score.getCombo() < newscore.getCombo() && updateScore) {
			score.setCombo(newscore.getCombo());
			score.setOption(newscore.getOption());
		}
		score.setPlaycount(score.getPlaycount() + 1);
		score.setDate(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis() / 1000L);
		scoredb.setScoreData(player, score);

		Logger.getGlobal().info("スコアデータベース更新完了 ");

	}

	public boolean existsReplayData(BMSModel model, int lnmode) {
		boolean ln = model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
				+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		return new File(this.getReplayDataFilePath(model.getSHA256(), ln, lnmode)).exists();
	}

	public boolean existsReplayData(String hash, boolean ln, int lnmode) {
		return new File(this.getReplayDataFilePath(hash, ln, lnmode)).exists();
	}

	public boolean existsReplayData(String[] hash, boolean ln, int lnmode) {
		String hashes = "";
		for(String s : hash) {
			hashes += s;
		}
		return new File(this.getReplayDataFilePath(hashes, ln, lnmode)).exists();
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

	public ReplayData[] readReplayData(BMSModel[] models, int lnmode) {
		String[] hashes = new String[models.length];
		boolean ln = false;
		for(int i = 0;i < models.length;i++) {
			hashes[i] = models[i].getSHA256();
			ln |= models[i].getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
					+ models[i].getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		}
		return this.readReplayData(hashes, ln, lnmode);
	}

	/**
	 * コースリプレイデータを読み込む
	 * @param hash 対象のBMSハッシュ群
	 * @param lnmode LNモード
	 * @return リプレイデータ
	 */
	public ReplayData[] readReplayData(String[] hash, boolean ln , int lnmode) {
		if (existsReplayData(hash, ln, lnmode)) {
			Json json = new Json();
			try {
				String hashes = "";
				for(String s : hash) {
					hashes += s;
				}
				return json.fromJson(ReplayData[].class,
						new FileReader(this.getReplayDataFilePath(hashes, ln, lnmode)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public void wrireReplayData(ReplayData[] rd, BMSModel[] models, int lnmode) {
		String[] hashes = new String[models.length];
		boolean ln = false;
		for(int i = 0;i < models.length;i++) {
			hashes[i] = models[i].getSHA256();
			ln |= models[i].getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
					+ models[i].getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		}
		this.wrireReplayData(rd, hashes, ln, lnmode);

	}

		/**
         * コースリプレイデータを書き込む
         * @param rd リプレイデータ
         * @param hash 対象のBMSハッシュ群
         * @param lnmode LNモード
         */
	public void wrireReplayData(ReplayData[] rd, String[] hash, boolean ln, int lnmode) {
		File replaydir = new File("replay");
		if (!replaydir.exists()) {
			replaydir.mkdirs();
		}
		Json json = new Json();
		json.setOutputType(OutputType.json);
		try {
			String hashes = "";
			for(String s : hash) {
				hashes += s;
			}
			FileWriter fw = new FileWriter(this.getReplayDataFilePath(hashes, ln, lnmode));
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
		return getReplayDataFilePath(model.getSHA256(), ln, lnmode);
	}

	private String getReplayDataFilePath(String hash, boolean ln, int lnmode) {
		return "replay" + File.separatorChar +  (ln ? replay[lnmode] : "") + hash + ".json";
	}
}
