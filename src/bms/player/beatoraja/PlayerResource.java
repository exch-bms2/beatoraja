package bms.player.beatoraja;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.*;

import bms.model.*;
import bms.player.beatoraja.audio.AudioDriver;
import bms.player.beatoraja.play.bga.BGAProcessor;
import bms.player.beatoraja.play.GrooveGauge;
import bms.player.beatoraja.song.SongData;

/**
 * プレイヤーのコンポーネント間でデータをやり取りするためのクラス
 * 
 * @author exch
 */
public class PlayerResource {
	/**
	 * 選曲中のBMS
	 */
	private BMSModel model;

	private BMSGenerator generator;
	/**
	 * 選択中のBMSの情報
	 */
	private SongData songdata;

	private Config config;
	private int auto;

	private int playDevice;

	private IntArray constraint = new IntArray();

	private BMSResource bmsresource;
	/**
	 * backbmp
	 */
	private TextureRegion backbmp;
	/**
	 * stagefile
	 */
	private TextureRegion stagefile;

	/**
	 * スコア
	 */
	private IRScoreData score;
	/**
	 * ライバルスコア
	 */
	private int rscore;
	/**
	 * スコア更新するかどうか
	 */
	private boolean updateScore = true;
	private GrooveGauge grooveGauge;
	/**
	 * ゲージの遷移ログ
	 */
	private FloatArray gauge;

	private List<FloatArray> coursegauge = new ArrayList<FloatArray>();

	/**
	 * コースタイトル
	 */
	private String coursetitle;
	/**
	 * コースのBMSモデル
	 */
	private BMSModel[] course;
	/**
	 * コース何曲目
	 */
	private int courseindex;

	private ReplayData replay;

	private List<ReplayData> courseReplay = new ArrayList<ReplayData>();
	/**
	 * コーススコア
	 */
	private IRScoreData cscore;
	/**
	 * コンボ数。コースプレイ時の引継ぎに使用
	 */
	private int combo;
	/**
	 * 最大コンボ数。コースプレイ時の引継ぎに使用
	 */
	private int maxcombo;

	public PlayerResource(AudioDriver audio, Config config) {
		this.config = config;
		this.bmsresource = new BMSResource(audio, config);
	}

	public void clear() {
		coursetitle = null;
		course = null;
		courseindex = 0;
		cscore = null;
		score = null;
		rscore = 0;
		gauge = null;
		courseReplay.clear();
		coursegauge.clear();
		combo = 0;
		maxcombo = 0;
		constraint.clear();
	}

	public boolean setBMSFile(final Path f, final Config config, int autoplay) {
		this.config = config;
		this.auto = autoplay;
		replay = new ReplayData();
		model = loadBMSModel(f, config.getLnmode());
		if (model == null) {
			Logger.getGlobal().warning("楽曲が存在しないか、解析時にエラーが発生しました:" + f.toString());
			return false;
		}
		if (model.getAllTimeLines().length == 0) {
			return false;
		}
		
		if(stagefile != null) {
			stagefile.getTexture().dispose();
			stagefile = null;
		}
		
		Pixmap pix = PixmapResourcePool.loadPicture(f.getParent().resolve(model.getStagefile()).toString());
		if(pix != null) {
			stagefile = new TextureRegion(new Texture(pix));
			pix.dispose();
		}
		if(backbmp != null) {
			backbmp.getTexture().dispose();
			backbmp = null;
		}
		pix = PixmapResourcePool.loadPicture(f.getParent().resolve(model.getBackbmp()).toString());
		if(pix != null) {
			backbmp = new TextureRegion(new Texture(pix));
			pix.dispose();
		}
		bmsresource.setBMSFile(model, f, config, autoplay);
		return true;
	}

	public BMSModel loadBMSModel(Path f, int lnmode) {
		BMSModel model;
		if (f.toString().toLowerCase().endsWith(".bmson")) {
			BMSONDecoder decoder = new BMSONDecoder(lnmode);
			model = decoder.decode(f.toFile());
			if (model == null) {
				return null;
			}
			if (model.getTotal() <= 0.0) {
				model.setTotal(100.0);
			}
			int totalnotes = model.getTotalNotes();
			model.setTotal(model.getTotal() / 100.0 * 7.605 * totalnotes / (0.01 * totalnotes + 6.5));
		} else {
			BMSDecoder decoder = new BMSDecoder(lnmode);
			model = decoder.decode(f.toFile());
			if (model == null) {
				return null;
			}
			generator = decoder.getBMSGenerator();
			// JUDGERANKをbmson互換に変換
			if (model.getJudgerank() >= 0 && model.getJudgerank() < 5) {
				final int[] judgetable = { 40, 60, 80, 100, 120 };
				model.setJudgerank(judgetable[model.getJudgerank()]);
			} else if (model.getJudgerank() < 0) {
				model.setJudgerank(100);
			}
			// TOTAL未定義の場合
			if (model.getTotal() <= 0.0) {
				int totalnotes = model.getTotalNotes();
				model.setTotal(7.605 * totalnotes / (0.01 * totalnotes + 6.5));
			}
		}

		return model;
	}

	public BMSModel getBMSModel() {
		return model;
	}

	public int getAutoplay() {
		return auto;
	}

	public Config getConfig() {
		return config;
	}

	public BGAProcessor getBGAManager() {
		return bmsresource.getBGAProcessor();
	}

	public boolean mediaLoadFinished() {
		return bmsresource.mediaLoadFinished();
	}

	public IRScoreData getScoreData() {
		return score;
	}

	public void setScoreData(IRScoreData score) {
		this.score = score;
	}

	public int getRivalScoreData() {
		return rscore;
	}

	public void setRivalScoreData(int rscore) {
		this.rscore = rscore;
	}

	public boolean setCourseBMSFiles(Path[] files) {
		List<BMSModel> models = new ArrayList();
		for (Path f : files) {
			BMSModel model = loadBMSModel(f, config.getLnmode());
			if (model == null) {
				return false;
			}
			models.add(model);
		}
		course = models.toArray(new BMSModel[0]);
		return true;
	}

	public BMSModel[] getCourseBMSModels() {
		return course;
	}

	public boolean nextCourse() {
		courseindex++;
		if (courseindex == course.length) {
			return false;
		} else {
			setBMSFile(Paths.get(course[courseindex].getPath()), config, auto);
			return true;
		}
	}

	public void reloadBMSFile() {
		if (model != null) {
			model = loadBMSModel(Paths.get(model.getPath()), config.getLnmode());
		}
		clear();
	}

	public FloatArray getGauge() {
		return gauge;
	}

	public void setGauge(FloatArray gauge) {
		this.gauge = gauge;
	}

	public GrooveGauge getGrooveGauge() {
		return grooveGauge;
	}

	public void setGrooveGauge(GrooveGauge grooveGauge) {
		this.grooveGauge = grooveGauge;
	}

	public ReplayData getReplayData() {
		return replay;
	}

	public void setReplayData(ReplayData replay) {
		this.replay = replay;
	}

	public IRScoreData getCourseScoreData() {
		return cscore;
	}

	public void setCourseScoreData(IRScoreData cscore) {
		this.cscore = cscore;
	}

	public boolean isUpdateScore() {
		return updateScore;
	}

	public void setUpdateScore(boolean b) {
		this.updateScore = b;
	}

	public String getCoursetitle() {
		return coursetitle;
	}

	public void setCoursetitle(String coursetitle) {
		this.coursetitle = coursetitle;
	}

	public ReplayData[] getCourseReplay() {
		return courseReplay.toArray(new ReplayData[0]);
	}

	public void addCourseReplay(ReplayData rd) {
		courseReplay.add(rd);
	}

	public List<FloatArray> getCourseGauge() {
		return coursegauge;
	}

	public void addCourseGauge(FloatArray gauge) {
		coursegauge.add(gauge);
	}

	public int getCombo() {
		return combo;
	}

	public void setCombo(int combo) {
		this.combo = combo;
	}

	public int getMaxcombo() {
		return maxcombo;
	}

	public void setMaxcombo(int maxcombo) {
		this.maxcombo = maxcombo;
	}

	public int[] getConstraint() {
		return constraint.toArray();
	}

	public void addConstraint(int constraint) {
		this.constraint.add(constraint);
	}

	public void dispose() {
		if(bmsresource != null) {
			bmsresource.dispose();
			bmsresource = null;
		}
		if(stagefile != null) {
			stagefile.getTexture().dispose();
			stagefile = null;
		}
		if(backbmp != null) {
			backbmp.getTexture().dispose();
			backbmp = null;
		}
	}

	public SongData getSongdata() {
		return songdata;
	}

	public void setSongdata(SongData songdata) {
		this.songdata = songdata;
	}

	public BMSGenerator getGenerator() {
		return generator;
	}
	
	public TextureRegion getBackbmpData() {
		return backbmp;
	}

	public TextureRegion getStagefileData() {
		return stagefile;
	}

	public int getPlayDevice() {
		return playDevice;
	}

	public void setPlayDevice(int playDevice) {
		this.playDevice = playDevice;
	}
}