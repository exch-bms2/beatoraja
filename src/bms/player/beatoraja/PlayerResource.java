package bms.player.beatoraja;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import com.badlogic.gdx.utils.*;

import bms.model.*;
import bms.player.beatoraja.CourseData.CourseDataConstraint;
import bms.player.beatoraja.TableData.TableFolder;
import bms.player.beatoraja.audio.AudioDriver;
import bms.player.beatoraja.play.BMSPlayerRule;
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
	
	private long marginTime;
	/**
	 * 選択中のBMSの情報
	 */
	private SongData songdata;
	/**
	 * BMSModelの元々のモード
	 */
	private Mode orgmode;

	private PlayerData playerdata = new PlayerData();

	private Config config;
	private PlayerConfig pconfig;
	/**
	 * プレイモード
	 */
	private PlayMode mode;
	
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
	private FloatArray[] gauge;

	private ReplayData replay;

	private Path[] bmsPaths;
	private boolean loop;
	
	/**
	 * コース
	 */
	private CourseData coursedata;
	/**
	 * コースのBMSモデル
	 */
	private BMSModel[] course;
	/**
	 * コース何曲目
	 */
	private int courseindex;
	/**
	 * コースゲージ履歴
	 */
	private Array<FloatArray[]> coursegauge = new Array<FloatArray[]>();

	private Array<ReplayData> courseReplay = new Array<ReplayData>();
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
	/**
	 * 元々のゲージオプション
	 */
	private int orgGaugeOption = 0;

	private int assist = 0;

	/**
	 * 現在プレイしている楽曲を含む難易度表とレベル
	 */
	private String tablename = "";
	private String tablelevel = "";
	private String tablefull;

	public PlayerResource(AudioDriver audio, Config config, PlayerConfig pconfig) {
		this.config = config;
		this.pconfig = pconfig;
		this.bmsresource = new BMSResource(audio, config, pconfig);
		this.orgGaugeOption = pconfig.getGauge();
	}

	public void clear() {
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
		bmsPaths = null;
		setTablename("");
		setTablelevel("");
	}

	public boolean setBMSFile(final Path f, PlayMode mode) {
		// TODO play mode, リプレイデータでの読み込み分岐をここで行う
		this.mode = mode;
		replay = new ReplayData();
		model = loadBMSModel(f, pconfig.getLnmode());
		if (model == null) {
			Logger.getGlobal().warning("楽曲が存在しないか、解析時にエラーが発生しました:" + f.toString());
			return false;
		}
		if (model.getAllTimeLines().length == 0) {
			return false;
		}

		orgmode = model.getMode();
		bmsresource.setBMSFile(model, f, config, mode);
		if(songdata != null) {
			songdata.setBMSModel(model);
		} else {
			songdata = new SongData(model, false);			
		}
		if(tablename.length() == 0 || courseindex != 0){
			setTableinfo();
		}
		return true;
	}

	public BMSModel loadBMSModel(Path f, int lnmode) {
		return loadBMSModel(new ChartInformation(f, lnmode, null));
	}

	public BMSModel loadBMSModel(int[] selectedRandom) {
		if(model != null) {
			ChartInformation info = model.getChartInformation();
			return loadBMSModel(new ChartInformation(info.path, info.lntype, selectedRandom));			
		}
		return null;
	}

	public BMSModel loadBMSModel(ChartInformation info) {
		ChartDecoder decoder = ChartDecoder.getDecoder(info.path);
		if(decoder == null) {
			return null;
		}
		BMSModel model = decoder.decode(info);
		if (model == null) {
			return null;
		}

		marginTime = BMSModelUtils.setStartNoteTime(model, 1000);
		BMSPlayerRule.validate(model);
		return model;
	}

	public BMSModel getBMSModel() {
		return model;
	}
	
	public long getMarginTime() {
		return marginTime;
	}

	public PlayMode getPlayMode() {
		return mode;
	}

	public void setPlayMode(PlayMode mode) {
		this.mode = mode;
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
		Array<BMSModel> models = new Array();
		for (Path f : files) {
			BMSModel model = loadBMSModel(f, pconfig.getLnmode());
			if (model == null) {
				return false;
			}
			models.add(model);
		}
		course = models.toArray(BMSModel.class);
		return true;
	}

	public BMSModel[] getCourseBMSModels() {
		return course;
	}

	public void setAutoPlaySongs(Path[] paths, boolean loop) {
		this.bmsPaths = paths;
		this.loop = loop;
	}
	
	public boolean nextSong() {
		if(bmsPaths == null) {
			return false;
		}
		final int orgindex = courseindex;
		do {
			if(courseindex == bmsPaths.length) {
				if(loop) {
					courseindex = 0;
				} else {
					return false;
				}
			}
			songdata = null;
			if(setBMSFile(bmsPaths[courseindex++], PlayMode.AUTOPLAY)) {
				return true;
			};
		} while(orgindex != courseindex);
		return false;
	}
	
	public boolean nextCourse() {
		courseindex++;
		if (courseindex == course.length) {
			return false;
		} else {
			songdata = null;
			setBMSFile(Paths.get(course[courseindex].getPath()), mode);
			return true;
		}
	}

	public int getCourseIndex() {
		return courseindex;
	}

	public void reloadBMSFile() {
		if (model != null) {
			model = loadBMSModel(Paths.get(model.getPath()), pconfig.getLnmode());
		}
		final String name = tablename;
		final String lev = tablelevel;
		clear();
		tablename = name;
		tablelevel = lev;
	}

	public FloatArray[] getGauge() {
		return gauge;
	}

	public void setGauge(FloatArray[] gauge) {
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

	public CourseData getCourseData() {
		return coursedata;
	}

	public void setCourseData(CourseData coursedata) {
		this.coursedata = coursedata;
	}
	
	public String getCoursetitle() {
		return coursedata != null ? coursedata.getName() : null;
	}
	
	public CourseDataConstraint[] getConstraint() {
		return coursedata != null ? coursedata.getConstraint() : new CourseDataConstraint[0];
	}

	public ReplayData[] getCourseReplay() {
		return courseReplay.toArray(ReplayData.class);
	}

	public void addCourseReplay(ReplayData rd) {
		courseReplay.add(rd);
	}

	public Array<FloatArray[]> getCourseGauge() {
		return coursegauge;
	}

	public void addCourseGauge(FloatArray[] gauge) {
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

	public BMSResource getBMSResource() {
		return bmsresource;
	}
	
	public int getOrgGaugeOption() {
		return orgGaugeOption;
	}

	public void setOrgGaugeOption(int orgGaugeOption) {
		this.orgGaugeOption = orgGaugeOption;
	}

	public int getAssist() {
		return assist;
	}

	public void setAssist(int assist) {
		this.assist = assist;
	}

	public String getTablename() {
		return tablename;
	}

	public void setTablename(String tablename) {
		this.tablename = tablename;
		this.tablefull = null;
	}

	public String getTablelevel() {
		return tablelevel;
	}

	public void setTablelevel(String tablelevel) {
		this.tablelevel = tablelevel;
		this.tablefull = null;
	}

	public String getTableFullname() {
		if(tablefull == null) {
			tablefull = tablelevel + tablename;
		}
		return tablefull;
	}

	public PlayerData getPlayerData() {
		return playerdata;
	}

	public void setPlayerData(PlayerData playerdata) {
		this.playerdata = playerdata;
	}

	private void setTableinfo(){
		final String[] urls = this.getConfig().getTableURL();
		final TableDataAccessor tdaccessor = new TableDataAccessor(config.getTablepath());
		final TableData[] tds = tdaccessor.readAll();
		for(String url: urls){
			for(TableData td: tds){
				if(td.getUrl().equals(url)){
					final TableFolder[] tfs = td.getFolder();
					for(TableFolder tf: tfs){
						final SongData[] tss = tf.getSong();
						for(SongData ts: tss){
							if((ts.getMd5().length() != 0 && this.getSongdata().getMd5().length() != 0 &&
									ts.getMd5().equals(this.getSongdata().getMd5()))||
									(ts.getMd5().length() != 0 && this.getSongdata().getMd5().length() != 0 &&
									ts.getSha256().equals(this.getSongdata().getSha256()))){
								setTablename(td.getName());
								setTablelevel(tf.getName());
								return;
							}
						}
					}
				}
			}
		}
		setTablename("");
		setTablelevel("");
	}

	public Mode getOriginalMode() {
		return orgmode;
	}

	public void setOriginalMode(Mode orgmode) {
		this.orgmode = orgmode;
	}

	public enum PlayMode {
		PLAY,
		PRACTICE,
		AUTOPLAY,
		AUTOPLAY_LOOP,
		REPLAY_1,
		REPLAY_2,
		REPLAY_3,
		REPLAY_4;
		
		public boolean isAutoPlayMode() {
			return this == AUTOPLAY || this == AUTOPLAY_LOOP; 
		}
		
		public boolean isReplayMode() {
			return this == REPLAY_1 || this == REPLAY_2 || this == REPLAY_3 || this == REPLAY_4; 
		}
		
		public static PlayMode getReplayMode(int index) {
			switch(index) {
			case 0:
				return REPLAY_1;
			case 1:
				return REPLAY_2;
			case 2:
				return REPLAY_3;
			case 3:
				return REPLAY_4;
			default:
				return null;
			}			
		}
		
		public int getReplayIndex() {
			switch(this) {
			case REPLAY_1:
				return 0;
			case REPLAY_2:
				return 1;
			case REPLAY_3:
				return 2;
			case REPLAY_4:
				return 3;
			default:
				return 0;
			}
		}
	}
}