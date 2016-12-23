package bms.player.beatoraja;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import bms.model.*;
import bms.player.beatoraja.audio.AudioDriver;
import bms.player.beatoraja.play.audio.AudioDeviceProcessor;
import bms.player.beatoraja.play.audio.AudioProcessor;
import bms.player.beatoraja.play.audio.SoundProcessor;
import bms.player.beatoraja.play.bga.BGAProcessor;
import bms.player.beatoraja.play.gauge.GrooveGauge;
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

	private List<Integer> constraint = new ArrayList<Integer>();

	private int bgashow;
	/**
	 * BMSの音源リソース
	 */
	private AudioDriver audio;
	private boolean audioLoaded;
	/**
	 * BMSのBGAリソース
	 */
	private BGAProcessor bga;
	private boolean bgaLoaded;

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
	/**
	 * コンボ数。コースプレイ時の引継ぎに使用
	 */
	private int combo;
	/**
	 * 最大コンボ数。コースプレイ時の引継ぎに使用
	 */
	private int maxcombo;

	public PlayerResource(AudioDriver audio, Config config) {
		this.audio = audio;
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
		if (model == null) {
			Logger.getGlobal().warning("楽曲が存在しないか、解析時にエラーが発生しました:" + f.toString());
			return false;
		}
		if (model.getAllTimeLines().length == 0) {
			return false;
		}
		if (bmspath == null || !f.toAbsolutePath().toString().equals(bmspath) || bgashow != config.getBga()
				|| (model.getRandom() != null && model.getRandom().length > 0)) {
			// 前回と違うbmsファイルを読み込んだ場合、BGAオプション変更時はリソースのロード
			// 同フォルダの違うbmsファイルでも、WAV/,BMP定義が違う可能性があるのでロード
			// RANDOM定義がある場合はリロード
			this.bgashow = config.getBga();
			
			audioLoaded = false;
			
			if (bga != null) {
				bga.dispose();
			}
			bga = new BGAProcessor(config);
			bgaLoaded = false;
			
			if (config.getBga() == Config.BGA_ON || (config.getBga() == Config.BGA_AUTO && (auto == 1 || auto >= 3))) {
				Thread bgaloader = new Thread() {
					@Override
					public void run() {
						try {
							bga.setModel(model);
						} catch (Throwable e) {
							Logger.getGlobal().severe(e.getClass().getName() + " : " + e.getMessage());
							e.printStackTrace();
						} finally {
							bgaLoaded = true;
						}
					}
				};
				bgaloader.start();					
			} else {
				bgaLoaded = true;
			}
			Thread audioloader = new Thread() {
				@Override
				public void run() {
					try {
						audio.setModel(model);
					} catch (Throwable e) {
						Logger.getGlobal().severe(e.getClass().getName() + " : " + e.getMessage());
						e.printStackTrace();
					} finally {
						audioLoaded = true;
					}
				}
			};
			audioloader.start();
		} else {
			// windowsだけ動画を含むBGAがあれば読み直す(ffmpegがエラー終了する。今後のupdateで直れば外す)
			if ("\\".equals(System.getProperty("file.separator"))) {
				Logger.getGlobal().info("WindowsのためBGA再読み込み");
				if (bga != null) {
					bga.dispose();
				}
				bga = new BGAProcessor(config);
				bgaLoaded = false;
				
				if (config.getBga() == Config.BGA_ON || (config.getBga() == Config.BGA_AUTO && (auto == 1 || auto >= 3))) {
					Thread bgaloader = new Thread() {
						@Override
						public void run() {
							try {
								bga.setModel(model);
							} catch (Throwable e) {
								Logger.getGlobal().severe(e.getClass().getName() + " : " + e.getMessage());
								e.printStackTrace();
							} finally {
								bgaLoaded = true;
							}
						}
					};
					bgaloader.start();					
				} else {
					bgaLoaded = true;
				}
			}
		}
		return true;
	}

	private BMSModel loadBMSModel(Path f) {
		BMSModel model;
		if (f.toString().toLowerCase().endsWith(".bmson")) {
			BMSONDecoder decoder = new BMSONDecoder(config.getLnmode());
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
			BMSDecoder decoder = new BMSDecoder(config.getLnmode());
			model = decoder.decode(f.toFile());
			if (model == null) {
				return null;
			}
			generator = decoder.getBMSGenerator();
			// JUDGERANKをbmson互換に変換
			if (model.getJudgerank() >= 0 && model.getJudgerank() < 4) {
				final int[] judgetable = { 40, 70, 90, 100 };
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
		return bga;
	}

	public boolean mediaLoadFinished() {
		return audioLoaded && bgaLoaded;
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
		for (int i = 0; i < result.length; i++) {
			result[i] = constraint.get(i);
		}
		return result;
	}

	public void addConstraint(int constraint) {
		this.constraint.add(constraint);
	}

	public void dispose() {
		if (audio != null) {
			audio.dispose();
			audio = null;
		}
		if (bga != null) {
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

	public BMSGenerator getGenerator() {
		return generator;
	}

	public int getPlayDevice() {
		return playDevice;
	}

	public void setPlayDevice(int playDevice) {
		this.playDevice = playDevice;
	}
}