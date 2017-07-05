package bms.player.beatoraja;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import bms.model.BMSModel;
import bms.model.TimeLine;
import static bms.player.beatoraja.ClearType.*;
import static bms.player.beatoraja.CourseData.CourseDataConstraint.*;

import bms.player.beatoraja.ScoreLogDatabaseAccessor.ScoreLog;
import bms.player.beatoraja.song.SongData;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.badlogic.gdx.utils.StringBuilder;

/**
 * プレイデータアクセス用クラス
 * 
 * @author exch
 */
public class PlayDataAccessor {

	// TODO スコアハッシュを付与するかどうかの判定(前のスコアハッシュの正当性を確認できなかった時)
	// TODO リプレイ暗号、復号化

	private final String hashkey;

	/**
	 * プレイヤー名
	 */
	private String player;
	/**
	 * スコアデータベースアクセサ
	 */
	private ScoreDatabaseAccessor scoredb;
	/**
	 * スコアログアクセサ
	 */
	private ScoreLogDatabaseAccessor scorelogdb;

	private static final String[] replay = { "", "C", "H" };

	public PlayDataAccessor(String player) {
		this.player = player;

		try {
			Class.forName("org.sqlite.JDBC");
			scoredb = new ScoreDatabaseAccessor(player);
			scoredb.createTable();
			scorelogdb = new ScoreLogDatabaseAccessor("player/" + player + "/scorelog.db");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		hashkey = "";
	}

	public PlayerData readPlayerData() {
		return scoredb.getPlayerData();
	}

	public PlayerData readTodayPlayerData() {
		PlayerData[] pd = scoredb.getPlayerDatas(2);
		if (pd.length > 1) {
			pd[0].setPlaycount(pd[0].getPlaycount() - pd[1].getPlaycount());
			pd[0].setClear(pd[0].getClear() - pd[1].getClear());
			pd[0].setEpg(pd[0].getEpg() - pd[1].getEpg());
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
		} else if (pd.length == 1) {
			return pd[0];
		}
		return null;
	}

	/**
	 * 指定されたスコアデータを元にプレイヤーデータを更新する
	 * 
	 * @param score
	 * @param time
	 */
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
		if (score.getClear() > Failed.id) {
			pd.setClear(pd.getClear() + 1);
		}
		pd.setPlaytime(pd.getPlaytime() + time);
		scoredb.setPlayerData(pd);
	}

	/**
	 * スコアデータを読み込む
	 * 
	 * @param model
	 *            対象のモデル
	 * @param lnmode
	 *            LNモード
	 * @return スコアデータ
	 */
	public IRScoreData readScoreData(BMSModel model, int lnmode) {
		String hash = model.getSHA256();
		boolean ln = model.containsUndefinedLongNote();
		return readScoreData(hash, ln, lnmode);
	}

	/**
	 * スコアデータを読み込む
	 * 
	 * @param ln
	 *            対象のbmsが未定義LNを含む場合はtrueを入れる
	 * @param lnmode
	 *            LNモード
	 * @return スコアデータ
	 */
	public IRScoreData readScoreData(String hash, boolean ln, int lnmode) {
		return scoredb.getScoreData(hash, ln ? lnmode : 0);
	}

	public Map<String, IRScoreData> readScoreDatas(SongData[] songs, int lnmode) {
		List<String> noln = new ArrayList<String>();
		List<String> ln = new ArrayList<String>();
		for (SongData song : songs) {
			if (song.hasUndefinedLongNote()) {
				ln.add(song.getSha256());
			} else {
				noln.add(song.getSha256());
			}
		}
		Map<String, IRScoreData> result = scoredb.getScoreDatas(noln.toArray(new String[0]), 0);
		result.putAll(scoredb.getScoreDatas(ln.toArray(new String[0]), lnmode));
		return result;
	}

	public List<IRScoreData> readScoreDatas(String sql) {
		return scoredb.getScoreDatas(sql);
	}

	/**
	 * スコアデータを書き込む
	 * 
	 * @param newscore
	 *            スコアデータ
	 * @param model
	 *            対象のモデル
	 * @param lnmode
	 *            LNモード
	 * @param updateScore
	 *            プレイ回数のみ反映する場合はfalse
	 */
	public void writeScoreDara(IRScoreData newscore, BMSModel model, int lnmode, boolean updateScore) {
		String hash = model.getSHA256();
		if (newscore == null) {
			return;
		}
		IRScoreData score = scoredb.getScoreData(hash, model.containsUndefinedLongNote() ? lnmode : 0);

		if (score == null) {
			score = new IRScoreData();
			score.setMode(model.containsUndefinedLongNote() ? lnmode : 0);
		}
		score.setSha256(hash);
		score.setNotes(model.getTotalNotes());

		if (newscore.getClear() > Failed.id) {
			score.setClearcount(score.getClearcount() + 1);
		}

		ScoreLog log = updateScore(score, newscore, hash, updateScore);
		
		if (model.getMode().player == 1) {
			int history = score.getHistory();
			for (int i = 0; i < newscore.getOption(); i++) {
				history /= 10;
			}
			if (history % 10 < newscore.getClear() - LightAssistEasy.id) {
				int add = newscore.getClear() - LightAssistEasy.id - (history % 10);
				for (int i = 0; i < newscore.getOption(); i++) {
					add *= 10;
				}
				score.setHistory(score.getHistory() + add);
			}
		} else {
			// TODO DPのhistoryはどうする？
		}

		score.setPlaycount(score.getPlaycount() + 1);
		score.setDate(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis() / 1000L);
		score.setScorehash(getScoreHash(score));
		scoredb.setScoreData(score);
		if (log.getSha256() != null && scorelogdb != null) {
			log.setMode(score.getMode());
			log.setDate(score.getDate());
			scorelogdb.setScoreLog(log);
		}

		int time = 0;
		for (TimeLine tl : model.getAllTimeLines()) {
			for (int i = 0; i < model.getMode().key; i++) {
				if (tl.getNote(i) != null && tl.getNote(i).getState() != 0) {
					time = tl.getTime() / 1000;
				}
			}
		}
		updatePlayerData(newscore, time);
		Logger.getGlobal().info("スコアデータベース更新完了 ");
	}
	
	public IRScoreData readScoreData(String hash, boolean ln, int lnmode, int option,
			CourseData.CourseDataConstraint[] constraint) {
		int hispeed = 0;
		int judge = 0;
		for (CourseData.CourseDataConstraint c : constraint) {
			if (c == NO_SPEED) {
				hispeed = 1;
			}
			if (c == NO_GOOD) {
				judge = 1;
			}
			if (c == NO_GREAT) {
				judge = 2;
			}
		}
		return scoredb.getScoreData(hash, (ln ? lnmode : 0) + option * 10 + hispeed * 100 + judge * 1000);
	}

	public IRScoreData readScoreData(BMSModel[] models, int lnmode, int option,
			CourseData.CourseDataConstraint[] constraint) {
		String[] hash = new String[models.length];
		boolean ln = false;
		for (int i = 0; i < models.length; i++) {
			hash[i] = models[i].getSHA256();
			ln |= models[i].containsUndefinedLongNote();
		}
		return readScoreData(hash, ln, lnmode, option, constraint);
	}

	public IRScoreData readScoreData(String[] hashes, boolean ln, int lnmode, int option,
			CourseData.CourseDataConstraint[] constraint) {
		String hash = "";
		for (String s : hashes) {
			hash += s;
		}
		return readScoreData(hash, ln, lnmode, option, constraint);
	}

	public void writeScoreDara(IRScoreData newscore, BMSModel[] models, int lnmode, int option,
			CourseData.CourseDataConstraint[] constraint, boolean updateScore) {
		String hash = "";
		int totalnotes = 0;
		boolean ln = false;
		for (BMSModel model : models) {
			hash += model.getSHA256();
			totalnotes += model.getTotalNotes();
			ln |= model.containsUndefinedLongNote();
		}
		if (newscore == null) {
			return;
		}
		int hispeed = 0;
		int judge = 0;
		for (CourseData.CourseDataConstraint c : constraint) {
			if (c == NO_SPEED) {
				hispeed = 1;
			}
			if (c == NO_GOOD) {
				judge = 1;
			}
			if (c == NO_GREAT) {
				judge = 2;
			}
		}
		IRScoreData score = scoredb.getScoreData(hash, (ln ? lnmode : 0) + option * 10 + hispeed * 100 + judge * 1000);

		if (score == null) {
			score = new IRScoreData();
			score.setMode((ln ? lnmode : 0) + option * 10 + hispeed * 100 + judge * 1000);
		}
		score.setSha256(hash);
		score.setNotes(totalnotes);

		if (newscore.getClear() != Failed.id) {
			score.setClearcount(score.getClearcount() + 1);
		}
		
		ScoreLog log = updateScore(score, newscore, hash, updateScore);

		score.setPlaycount(score.getPlaycount() + 1);
		score.setDate(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis() / 1000L);
		score.setScorehash(getScoreHash(score));
		scoredb.setScoreData(score);
		if (log.getSha256() != null && scorelogdb != null) {
			log.setMode(score.getMode());
			log.setDate(score.getDate());
			scorelogdb.setScoreLog(log);
		}

		Logger.getGlobal().info("スコアデータベース更新完了 ");

	}

	private String getScoreHash(IRScoreData score) {
		byte[] cipher_byte;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update((hashkey + score.getSha256() + "," + score.getExscore() + "," + score.getEpg() + ","
					+ score.getLpg() + "," + score.getEgr() + "," + score.getLgr() + "," + score.getEgd() + ","
					+ score.getLgd() + "," + score.getEbd() + "," + score.getLbd() + "," + score.getEpr() + ","
					+ score.getLpr() + "," + score.getEms() + "," + score.getLms() + "," + score.getClear() + ","
					+ score.getMinbp() + "," + score.getCombo() + "," + score.getMode() + "," + score.getClearcount()
					+ "," + score.getPlaycount() + "," + score.getOption() + "," + score.getRandom() + ","
					+ score.getHistory() + "," + score.getDate()).getBytes());
			cipher_byte = md.digest();
			StringBuilder sb = new StringBuilder(2 * cipher_byte.length);
			for (byte b : cipher_byte) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return "035" + sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private ScoreLog updateScore(IRScoreData score, IRScoreData newscore, String hash, boolean updateScore) {
		ScoreLog log = new ScoreLog();
		
		final int clear = score.getClear();
		log.setOldclear(clear);
		log.setClear(clear);
		if (clear < newscore.getClear()) {
			score.setClear(newscore.getClear());
			score.setOption(newscore.getOption());
			log.setSha256(hash);
			log.setClear(newscore.getClear());
		}
		log.setOldscore(score.getExscore());
		log.setScore(score.getExscore());
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
			log.setSha256(hash);
			log.setScore(newscore.getExscore());
		}
		log.setOldminbp(score.getMinbp());
		log.setMinbp(score.getMinbp());
		if (score.getMinbp() > newscore.getMinbp() && updateScore) {
			score.setMinbp(newscore.getMinbp());
			score.setOption(newscore.getOption());
			log.setSha256(hash);
			log.setMinbp(newscore.getMinbp());
		}
		log.setOldcombo(score.getCombo());
		log.setCombo(score.getCombo());
		if (score.getCombo() < newscore.getCombo() && updateScore) {
			score.setCombo(newscore.getCombo());
			score.setOption(newscore.getOption());
			log.setSha256(hash);
			log.setCombo(newscore.getCombo());
		}

		return log;
	}

	public boolean existsReplayData(BMSModel model, int lnmode, int index) {
		boolean ln = model.containsUndefinedLongNote();
		return Files.exists(Paths.get(this.getReplayDataFilePath(model.getSHA256(), ln, lnmode, index) + ".brd"))
				|| Files.exists(Paths.get(this.getReplayDataFilePath(model.getSHA256(), ln, lnmode, index) + ".json"));
	}

	public boolean existsReplayData(String hash, boolean ln, int lnmode, int index) {
		return Files.exists(Paths.get(this.getReplayDataFilePath(hash, ln, lnmode, index) + ".brd"))
				|| Files.exists(Paths.get(this.getReplayDataFilePath(hash, ln, lnmode, index) + ".json"));
	}

	public boolean existsReplayData(BMSModel[] models, int lnmode, int index,
			CourseData.CourseDataConstraint[] constraint) {
		String[] hash = new String[models.length];
		boolean ln = false;
		for (int i = 0; i < models.length; i++) {
			BMSModel model = models[i];
			hash[i] = model.getSHA256();
			ln |= model.containsUndefinedLongNote();
		}
		return Files.exists(Paths.get(this.getReplayDataFilePath(hash, ln, lnmode, index, constraint) + ".brd"))
				|| Files.exists(Paths.get(this.getReplayDataFilePath(hash, ln, lnmode, index, constraint) + ".json"));
	}

	public boolean existsReplayData(String[] hash, boolean ln, int lnmode, int index,
			CourseData.CourseDataConstraint[] constraint) {
		return Files.exists(Paths.get(this.getReplayDataFilePath(hash, ln, lnmode, index, constraint) + ".brd"))
				|| Files.exists(Paths.get(this.getReplayDataFilePath(hash, ln, lnmode, index, constraint) + ".json"));
	}

	/**
	 * リプレイデータを読み込む
	 * 
	 * @param model
	 *            対象のBMS
	 * @param lnmode
	 *            LNモード
	 * @return リプレイデータ
	 */
	public ReplayData readReplayData(BMSModel model, int lnmode, int index) {
		if (existsReplayData(model, lnmode, index)) {
			Json json = new Json();
			json.setIgnoreUnknownFields(true);
			try {
				String path = this.getReplayDataFilePath(model, lnmode, index);
				if (Files.exists(Paths.get(path + ".brd"))) {
					return json.fromJson(ReplayData.class, new BufferedInputStream(
							new GZIPInputStream(Files.newInputStream(Paths.get(path + ".brd")))));
				}
				if (Files.exists(Paths.get(path + ".json"))) {
					return json.fromJson(ReplayData.class, new FileReader(path + ".json"));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * リプレイデータを書き込む
	 * 
	 * @param rd
	 *            リプレイデータ
	 * @param model
	 *            対象のBMS
	 * @param lnmode
	 *            LNモード
	 */
	public void wrireReplayData(ReplayData rd, BMSModel model, int lnmode, int index) {
		File replaydir = new File("replay");
		if (!replaydir.exists()) {
			replaydir.mkdirs();
		}
		Json json = new Json();
		json.setOutputType(OutputType.json);
		try {
			String path = this.getReplayDataFilePath(model, lnmode, index) + ".brd";
			OutputStreamWriter fw = new OutputStreamWriter(
					new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(path))), "UTF-8");
			fw.write(json.prettyPrint(rd));
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public ReplayData[] readReplayData(BMSModel[] models, int lnmode, int index,
			CourseData.CourseDataConstraint[] constraint) {
		String[] hashes = new String[models.length];
		boolean ln = false;
		for (int i = 0; i < models.length; i++) {
			hashes[i] = models[i].getSHA256();
			ln |= models[i].containsUndefinedLongNote();
		}
		return this.readReplayData(hashes, ln, lnmode, index, constraint);
	}

	/**
	 * コースリプレイデータを読み込む
	 * 
	 * @param hash
	 *            対象のBMSハッシュ群
	 * @param lnmode
	 *            LNモード
	 * @return リプレイデータ
	 */
	public ReplayData[] readReplayData(String[] hash, boolean ln, int lnmode, int index,
			CourseData.CourseDataConstraint[] constraint) {
		if (existsReplayData(hash, ln, lnmode, index, constraint)) {
			Json json = new Json();
			json.setIgnoreUnknownFields(true);
			try {
				String path = this.getReplayDataFilePath(hash, ln, lnmode, index, constraint);
				if (Files.exists(Paths.get(path + ".brd"))) {
					return json.fromJson(ReplayData[].class, new BufferedInputStream(
							new GZIPInputStream(Files.newInputStream(Paths.get(path + ".brd")))));
				}
				if (Files.exists(Paths.get(path + ".json"))) {
					return json.fromJson(ReplayData[].class, new FileReader(path + ".json"));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public void wrireReplayData(ReplayData[] rd, BMSModel[] models, int lnmode, int index,
			CourseData.CourseDataConstraint[] constraint) {
		String[] hashes = new String[models.length];
		boolean ln = false;
		for (int i = 0; i < models.length; i++) {
			hashes[i] = models[i].getSHA256();
			ln |= models[i].getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
					+ models[i].getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		}
		this.wrireReplayData(rd, hashes, ln, lnmode, index, constraint);

	}

	/**
	 * コースリプレイデータを書き込む
	 * 
	 * @param rd
	 *            リプレイデータ
	 * @param hash
	 *            対象のBMSハッシュ群
	 * @param lnmode
	 *            LNモード
	 */
	public void wrireReplayData(ReplayData[] rd, String[] hash, boolean ln, int lnmode, int index,
			CourseData.CourseDataConstraint[] constraint) {
		Json json = new Json();
		json.setOutputType(OutputType.json);
		try {
			String path = this.getReplayDataFilePath(hash, ln, lnmode, index, constraint) + ".brd";
			OutputStreamWriter fw = new OutputStreamWriter(
					new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(path))), "UTF-8");
			fw.write(json.prettyPrint(rd));
			fw.flush();
			fw.close();
			Files.deleteIfExists(Paths.get(this.getReplayDataFilePath(hash, ln, lnmode, index, constraint) + ".json"));
			Logger.getGlobal().info("コースリプレイを保存:" + path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getReplayDataFilePath(BMSModel model, int lnmode, int index) {
		return getReplayDataFilePath(model.getSHA256(), model.containsUndefinedLongNote(), lnmode, index);
	}

	private String getReplayDataFilePath(String hash, boolean ln, int lnmode, int index) {
		return "player" + File.separatorChar + player + File.separatorChar + "replay" + File.separatorChar
				+ (ln ? replay[lnmode] : "") + hash + (index > 0 ? "_" + index : "");
	}

	private String getReplayDataFilePath(String[] hashes, boolean ln, int lnmode, int index,
			CourseData.CourseDataConstraint[] constraint) {
		StringBuilder hash = new StringBuilder();
		for (String s : hashes) {
			hash.append(s.substring(0, 10));
		}
		StringBuilder sb = new StringBuilder();
		for (CourseData.CourseDataConstraint c : constraint) {
			if (c != CLASS && c != MIRROR && c != RANDOM) {
				sb.append(String.format("%02d", c.id));
			}
		}
		return "player" + File.separatorChar + player + File.separatorChar + "replay" + File.separatorChar
				+ (ln ? replay[lnmode] : "") + hash + (sb.length() > 0 ? "_" + sb.toString() : "")
				+ (index > 0 ? "_" + index : "");
	}

}
