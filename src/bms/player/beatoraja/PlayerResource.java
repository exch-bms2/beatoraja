package bms.player.beatoraja;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import bms.model.*;
import bms.player.beatoraja.gauge.GrooveGauge;
import bms.player.beatoraja.play.audio.AudioProcessor;
import bms.player.beatoraja.play.audio.SoundProcessor;
import bms.player.beatoraja.play.bga.BGAProcessor;

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
	
	private SongData songdata;
	
	private Config config;
	private int auto;

	private List<Integer> constraint = new ArrayList<Integer>();

	private int bgashow;
	/**
	 * BMSの音源リソース
	 */
	private SoundProcessor audio;
	/**
	 * BMSのBGAリソース
	 */
	private BGAProcessor bga;

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
	private List<Float> gauge;

	private List<List<Float>> coursegauge = new ArrayList<List<Float>>();

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

	private int combo;

	private int maxcombo;

	public PlayerResource(Config config) {
		this.config = config;
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
		String bmspath = model != null ? model.getPath() : null;
		model = loadBMSModel(f);
		if(model == null) {
			Logger.getGlobal().warning("楽曲が存在しないか、解析時にエラーが発生しました:" + f.toString());
			return false;
		}
		if (model.getAllTimeLines().length == 0) {
			return false;
		}
		if (bmspath == null || !f.toAbsolutePath().toString().equals(bmspath) || bgashow != config.getBga()) {
			// 前回と違うbmsファイルを読み込んだ場合、BGAオプション変更時はリソースのロード
			// 同フォルダの違うbmsファイルでも、WAV/,BMP定義が違う可能性があるのでロード
			this.bgashow = config.getBga();
			if (audio != null) {
				audio.dispose();
			}
			audio = new SoundProcessor();
			if (bga != null) {
				bga.dispose();
			}
			bga = new BGAProcessor(config);
			Thread medialoader = new Thread() {
				@Override
				public void run() {
					try {
						if (config.getBga() == Config.BGA_ON || (config.getBga() == Config.BGA_AUTO && (auto != 0))) {
							bga.setModel(model);
						} else {
							bga.forceFinish();
						}
						audio.setModel(model);
					} catch (Exception e) {
						Logger.getGlobal().severe(e.getClass().getName() + " : " + e.getMessage());
						e.printStackTrace();
					} catch (Error e) {
						Logger.getGlobal().severe(e.getClass().getName() + " : " + e.getMessage());
					} finally {
						bga.forceFinish();
						audio.forceFinish();
					}
				}
			};
			medialoader.start();
		}
		return true;
	}

	private BMSModel loadBMSModel(Path f) {
		BMSModel model;
		if (f.toString().toLowerCase().endsWith(".bmson")) {
			BMSONDecoder decoder = new BMSONDecoder(BMSModel.LNTYPE_CHARGENOTE);
			model = decoder.decode(f.toFile());
			if(model == null) {
				return null;
			}
			if (model.getTotal() <= 0.0) {
				model.setTotal(100.0);
			}
			int totalnotes = model.getTotalNotes();
			model.setTotal(model.getTotal() / 100.0 * 7.605 * totalnotes / (0.01 * totalnotes + 6.5));
		} else {
			BMSDecoder decoder = new BMSDecoder(BMSModel.LNTYPE_CHARGENOTE);
			model = decoder.decode(f.toFile());
			if(model == null) {
				return null;
			}
			// JUDGERANKをbmson互換に変換
			if (model.getJudgerank() < 0 || model.getJudgerank() > 2) {
				model.setJudgerank(100);
			} else {
				final int[] judgetable = { 40, 70, 90 };
				model.setJudgerank(judgetable[model.getJudgerank()]);
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

	public AudioProcessor getAudioProcessor() {
		return audio;
	}

	public BGAProcessor getBGAManager() {
		return bga;
	}

	public boolean mediaLoadFinished() {
		return audio != null && audio.getProgress() == 1 && bga != null && bga.getProgress() == 1;
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
			BMSModel model = loadBMSModel(f);
			if(model == null) {
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
		if(model != null) {
			model = loadBMSModel(Paths.get(model.getPath()));
		}
		clear();
	}

	public List<Float> getGauge() {
		return gauge;
	}

	public void setGauge(List<Float> gauge) {
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

	public List<List<Float>> getCourseGauge() {
		return coursegauge;
	}

	public void addCourseGauge(List<Float> gauge) {
		coursegauge.add(new ArrayList<Float>(gauge));
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
		int[] result = new int[constraint.size()];
		for(int i = 0;i < result.length;i++) {
			result[i] = constraint.get(i);
		}
		return result;
	}

	public void addConstraint(int constraint) {
		this.constraint.add(constraint);
	}

	public void dispose() {
		if(audio != null) {
			audio.dispose();
			audio = null;
		}
		if(bga != null) {
			bga.dispose();
			bga = null;
		}
	}

	public SongData getSongdata() {
		return songdata;
	}

	public void setSongdata(SongData songdata) {
		this.songdata = songdata;
	}
}