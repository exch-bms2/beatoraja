package bms.player.beatoraja;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.FloatArray;

import bms.model.Mode;
import bms.model.BMSDecoder;
import bms.model.BMSGenerator;
import bms.model.BMSModel;
import bms.model.BMSONDecoder;
import bms.player.beatoraja.audio.AudioDriver;
import bms.player.beatoraja.input.BMSPlayerInputDevice;
import bms.player.beatoraja.play.GrooveGauge;
import bms.player.beatoraja.play.bga.BGAProcessor;
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
	private PlayerConfig pconfig;

	private int auto;

	private List<CourseData.CourseDataConstraint> constraint = new ArrayList();

	private BMSResource bmsresource;

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

	public PlayerResource(AudioDriver audio, Config config, PlayerConfig pconfig) {
		this.config = config;
		this.pconfig = pconfig;
		this.bmsresource = new BMSResource(audio, config, pconfig);
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

	public boolean setBMSFile(final Path f, int autoplay) {
		this.auto = autoplay;
		replay = new ReplayData();
		model = loadBMSModel(f, pconfig.getLnmode());
		if (model == null) {
			Logger.getGlobal().warning("楽曲が存在しないか、解析時にエラーが発生しました:" + f.toString());
			return false;
		}
		if (model.getAllTimeLines().length == 0) {
			return false;
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
			model.setTotal(model.getTotal() / 100.0 * calculateDefaultTotal(model.getMode(), model.getTotalNotes()));
		} else {
			BMSDecoder decoder = new BMSDecoder(lnmode);
			model = decoder.decode(f.toFile());
			if (model == null) {
				return null;
			}
			generator = decoder.getBMSGenerator();
			// JUDGERANKをbmson互換に変換
			if (model.getJudgerank() >= 0 && model.getJudgerank() < 5) {
				int[] judgetable = new int[5];
				if(model.getMode() == Mode.POPN_9K) {
					judgetable[0] = 33;
					judgetable[1] = 50;
					judgetable[2] = 70;
					judgetable[3] = 100;
					judgetable[4] = 133;
				} else {
					judgetable[0] = 40;
					judgetable[1] = 60;
					judgetable[2] = 80;
					judgetable[3] = 100;
					judgetable[4] = 120;
				}
				model.setJudgerank(judgetable[model.getJudgerank()]);
			} else if (model.getJudgerank() < 0) {
				model.setJudgerank(100);
			}
			// TOTAL未定義の場合
			if (model.getTotal() <= 0.0) {
				model.setTotal(calculateDefaultTotal(model.getMode(), model.getTotalNotes()));
			}
		}

		return model;
	}

	double calculateDefaultTotal(Mode mode, int totalnotes) {
		switch (mode) {
		case BEAT_7K:
		case BEAT_5K:
		case BEAT_14K:
		case BEAT_10K:
		case POPN_9K:
		case POPN_5K:
			return Math.max(260.0, 7.605 * totalnotes / (0.01 * totalnotes + 6.5));
		case KEYBOARD_24K:
		case KEYBOARD_24K_DOUBLE:
			return Math.max(300.0, 7.605 * (totalnotes + 100) / (0.01 * totalnotes + 6.5));
		default:
			return Math.max(260.0, 7.605 * totalnotes / (0.01 * totalnotes + 6.5));
		}
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

	public PlayerConfig getPlayerConfig() {
		return pconfig;
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
			BMSModel model = loadBMSModel(f, pconfig.getLnmode());
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
			setBMSFile(Paths.get(course[courseindex].getPath()), auto);
			return true;
		}
	}

	public void reloadBMSFile() {
		if (model != null) {
			model = loadBMSModel(Paths.get(model.getPath()), pconfig.getLnmode());
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

	public CourseData.CourseDataConstraint[] getConstraint() {
		return constraint.toArray(new CourseData.CourseDataConstraint[constraint.size()]);
	}

	public void addConstraint(CourseData.CourseDataConstraint constraint) {
		this.constraint.add(constraint);
	}

	public void dispose() {
		if(bmsresource != null) {
			bmsresource.dispose();
			bmsresource = null;
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

	public BMSResource getBMSResource() {
		return bmsresource;
	}
}