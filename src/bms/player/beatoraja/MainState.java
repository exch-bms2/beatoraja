package bms.player.beatoraja;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

import bms.model.Mode;
import bms.player.beatoraja.SkinConfig.Offset;
import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;
import bms.player.beatoraja.skin.lr2.LR2SkinCSVLoader;
import bms.player.beatoraja.skin.lr2.LR2SkinHeaderLoader;
import bms.player.beatoraja.song.SongData;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.IntMap;

import static bms.player.beatoraja.skin.SkinProperty.*;

/**
 * プレイヤー内の各状態の抽象クラス
 * 
 * @author exch
 */
public abstract class MainState {

	public final MainController main;
	
	/**
	 * スキン
	 */
	private Skin skin;

	private Stage stage;
	
	private IntMap<String> soundmap = new IntMap<String>();
	private IntMap<Boolean> soundloop = new IntMap<Boolean>();

	private ScoreDataProperty score = new ScoreDataProperty();

	public MainState(MainController main) {
		this.main = main;
	}
	
	public abstract void create();

	public abstract void render();

	public void input() {
		
	}
	
	public void pause() {

	}

	public void resume() {

	}

	public void resize(int width, int height) {

	}

	public void dispose() {
		if(skin != null) {
			skin.dispose();
			skin = null;
		}
		if(stage != null) {
			stage.dispose();
			stage = null;
		}
	}

	public void executeClickEvent(int id) {

	}

	public ScoreDataProperty getScoreDataProperty() {
		return score;
	}

	public boolean getBooleanValue(int id) {
		final SongData model = main.getPlayerResource().getSongdata();
		switch (id) {
		case OPTION_STAGEFILE:
			return model != null && model.getStagefile().length() > 0;
		case OPTION_NO_STAGEFILE:
			return model != null && model.getStagefile().length() == 0;
		case OPTION_BACKBMP:
			return model != null && model.getBackbmp().length() > 0;
		case OPTION_NO_BACKBMP:
			return model != null && model.getBackbmp().length() == 0;
		case OPTION_BANNER:
			return model != null && model.getBanner().length() > 0;
		case OPTION_NO_BANNER:
			return model != null && model.getBanner().length() == 0;
		case OPTION_DIFFICULTY0:
			return model != null && (model.getDifficulty() <= 0 || model.getDifficulty() > 5);
		case OPTION_DIFFICULTY1:
			return model != null && model.getDifficulty() == 1;
		case OPTION_DIFFICULTY2:
			return model != null && model.getDifficulty() == 2;
		case OPTION_DIFFICULTY3:
			return model != null && model.getDifficulty() == 3;
		case OPTION_DIFFICULTY4:
			return model != null && model.getDifficulty() == 4;
		case OPTION_DIFFICULTY5:
			return model != null && model.getDifficulty() == 5;
		case OPTION_JUDGE_VERYEASY:
			return model != null && (model.getJudge() == 4 || model.getJudge() >= 110);
		case OPTION_JUDGE_EASY:
			return model != null && (model.getJudge() == 3 || (model.getJudge() >= 90 && model.getJudge() < 110));
		case OPTION_JUDGE_NORMAL:
			return model != null && (model.getJudge() == 2 || (model.getJudge() >= 70 && model.getJudge() < 90));
		case OPTION_JUDGE_HARD:
			return model != null && (model.getJudge() == 1 || (model.getJudge() >= 50 && model.getJudge() < 70));
		case OPTION_JUDGE_VERYHARD:
			return model != null && (model.getJudge() == 0 || (model.getJudge() >= 10 && model.getJudge() < 50));
		case OPTION_5KEYSONG:
			return model != null && model.getMode() == Mode.BEAT_5K.id;
		case OPTION_7KEYSONG:
			return model != null && model.getMode() == Mode.BEAT_7K.id;
		case OPTION_9KEYSONG:
			return model != null && model.getMode() == Mode.POPN_9K.id;
		case OPTION_10KEYSONG:
			return model != null && model.getMode() == Mode.BEAT_10K.id;
		case OPTION_14KEYSONG:
			return model != null && model.getMode() == Mode.BEAT_14K.id;
		case OPTION_24KEYSONG:
			return model != null && model.getMode() == Mode.KEYBOARD_24K.id;
		case OPTION_24KEYDPSONG:
			return model != null && model.getMode() == Mode.KEYBOARD_24K_DOUBLE.id;
		case OPTION_NO_TEXT:
			return model != null && !model.hasDocument();
		case OPTION_TEXT:
			return model != null && model.hasDocument();
		case OPTION_NO_LN:
			return model != null && !model.hasLongNote();
		case OPTION_LN:
			return model != null && model.hasLongNote();
		case OPTION_NO_BGA:
			return model != null && !model.hasBGA();
		case OPTION_BGA:
			return model != null && model.hasBGA();
		case OPTION_NO_RANDOMSEQUENCE:
			return model != null && !model.hasRandomSequence();
		case OPTION_RANDOMSEQUENCE:
			return model != null && model.hasRandomSequence();
		case OPTION_NO_BPMCHANGE:
			return model != null && model.getMinbpm() == model.getMaxbpm();
		case OPTION_BPMCHANGE:
			return model != null && model.getMinbpm() < model.getMaxbpm();
		case OPTION_OFFLINE:
			return main.getIRConnection() == null;
			case OPTION_ONLINE:
				return main.getIRConnection() != null;
			case OPTION_F:
				return score.qualifyRank(0);
			case OPTION_E:
				return score.qualifyRank(6);
			case OPTION_D:
				return score.qualifyRank(9);
			case OPTION_C:
				return score.qualifyRank(12);
			case OPTION_B:
				return score.qualifyRank(15);
			case OPTION_A:
				return score.qualifyRank(18);
			case OPTION_AA:
				return score.qualifyRank(21);
			case OPTION_AAA:
				return score.qualifyRank(24);
			case OPTION_1P_F:
			case OPTION_RESULT_F_1P:
			case OPTION_NOW_F_1P:
				return score.qualifyNowRank(0) && !score.qualifyNowRank(6);
			case OPTION_1P_E:
			case OPTION_RESULT_E_1P:
			case OPTION_NOW_E_1P:
				return score.qualifyNowRank(6) && !score.qualifyNowRank(9);
			case OPTION_RESULT_D_1P:
			case OPTION_NOW_D_1P:
			case OPTION_1P_D:
				return score.qualifyNowRank(9) && !score.qualifyNowRank(12);
			case OPTION_RESULT_C_1P:
			case OPTION_NOW_C_1P:
			case OPTION_1P_C:
				return score.qualifyNowRank(12) && !score.qualifyNowRank(15);
			case OPTION_1P_B:
			case OPTION_RESULT_B_1P:
			case OPTION_NOW_B_1P:
				return score.qualifyNowRank(15) && !score.qualifyNowRank(18);
			case OPTION_1P_A:
			case OPTION_RESULT_A_1P:
			case OPTION_NOW_A_1P:
				return score.qualifyNowRank(18) && !score.qualifyNowRank(21);
			case OPTION_1P_AA:
			case OPTION_RESULT_AA_1P:
			case OPTION_NOW_AA_1P:
				return score.qualifyNowRank(21) && !score.qualifyNowRank(24);
			case OPTION_1P_AAA:
			case OPTION_RESULT_AAA_1P:
			case OPTION_NOW_AAA_1P:
				return score.qualifyNowRank(24);
			case OPTION_BEST_F_1P:
				return score.qualifyBestRank(0) && !score.qualifyBestRank(6);
			case OPTION_BEST_E_1P:
				return score.qualifyBestRank(6) && !score.qualifyBestRank(9);
			case OPTION_BEST_D_1P:
				return score.qualifyBestRank(9) && !score.qualifyBestRank(12);
			case OPTION_BEST_C_1P:
				return score.qualifyBestRank(12) && !score.qualifyBestRank(15);
			case OPTION_BEST_B_1P:
				return score.qualifyBestRank(15) && !score.qualifyBestRank(18);
			case OPTION_BEST_A_1P:
				return score.qualifyBestRank(18) && !score.qualifyBestRank(21);
			case OPTION_BEST_AA_1P:
				return score.qualifyBestRank(21) && !score.qualifyBestRank(24);
			case OPTION_BEST_AAA_1P:
				return score.qualifyBestRank(24);
			case OPTION_TABLE_SONG:
				return main.getPlayerResource().getTablename().length() != 0;
		}
		return false;
	}

	public Skin getSkin() {
		return skin;
	}

	public void setSkin(Skin skin) {
		if(this.skin != null) {
			this.skin.dispose();
		}
		this.skin = skin;
		if(skin != null) {
			for(Entry<Integer, Offset> e : skin.getOffset().entrySet()) {
				SkinOffset offset = main.getOffset(e.getKey());
				offset.x = e.getValue().x;
				offset.y = e.getValue().y;
				offset.w = e.getValue().w;
				offset.h = e.getValue().h;
				offset.r = e.getValue().r;
				offset.a = e.getValue().a;
			}			
		}
	}
	
	public void loadSkin(SkinType skinType) {
		final PlayerResource resource = main.getPlayerResource();
		try {
			SkinConfig sc = resource.getPlayerConfig().getSkin()[skinType.getId()];
			if (sc.getPath().endsWith(".json")) {
				JSONSkinLoader sl = new JSONSkinLoader(resource.getConfig());
				setSkin(sl.loadSkin(Paths.get(sc.getPath()), skinType, sc.getProperties()));
			} else {
				LR2SkinHeaderLoader loader = new LR2SkinHeaderLoader();
				SkinHeader header = loader.loadSkin(Paths.get(sc.getPath()), this, sc.getProperties());
				LR2SkinCSVLoader dloader = LR2SkinCSVLoader.getSkinLoader(skinType,  header.getResolution(), resource.getConfig());
				setSkin(dloader.loadSkin(Paths.get(sc.getPath()).toFile(), this, header, loader.getOption(),
						sc.getProperties()));
			}
		} catch (Throwable e) {
			e.printStackTrace();
			JSONSkinLoader sl = new JSONSkinLoader(resource.getConfig());
			setSkin(sl.loadSkin(Paths.get(SkinConfig.defaultSkinPathMap.get(skinType)), skinType, new SkinConfig.Property()));
		}
	}

	public int getJudgeCount(int judge, boolean fast) {
		IRScoreData sd = score.getScoreData();
		return sd != null ? sd.getJudgeCount(judge, fast) : 0;
	}

	public int getNumberValue(int id) {
		switch (id) {
			case NUMBER_JUDGETIMING:
				return main.getPlayerResource().getPlayerConfig().getJudgetiming();
			case NUMBER_CURRENT_FPS:
				return Gdx.graphics.getFramesPerSecond();
			case NUMBER_TIME_YEAR:
				return main.getCurrnetTime().get(Calendar.YEAR);
			case NUMBER_TIME_MONTH:
				return main.getCurrnetTime().get(Calendar.MONTH) + 1;
			case NUMBER_TIME_DAY:
				return main.getCurrnetTime().get(Calendar.DATE);
			case NUMBER_TIME_HOUR:
				return main.getCurrnetTime().get(Calendar.HOUR_OF_DAY);
			case NUMBER_TIME_MINUTE:
				return main.getCurrnetTime().get(Calendar.MINUTE);
			case NUMBER_TIME_SECOND:
				return main.getCurrnetTime().get(Calendar.SECOND);
			case NUMBER_OPERATING_TIME_HOUR:
				return (int) (main.getPlayTime() / 3600000);
			case NUMBER_OPERATING_TIME_MINUTE:
				return (int) (main.getPlayTime() / 60000) % 60;
			case NUMBER_OPERATING_TIME_SECOND:
				return (int) (main.getPlayTime() / 1000) % 60;
			case NUMBER_PERFECT:
				return getJudgeCount(0, true) + getJudgeCount(0, false);
			case NUMBER_EARLY_PERFECT:
				return getJudgeCount(0, true);
			case NUMBER_LATE_PERFECT:
				return getJudgeCount(0, false);
			case NUMBER_GREAT:
				return getJudgeCount(1, true) + getJudgeCount(1, false);
			case NUMBER_EARLY_GREAT:
				return getJudgeCount(1, true);
			case NUMBER_LATE_GREAT:
				return getJudgeCount(1, false);
			case NUMBER_GOOD:
				return getJudgeCount(2, true) + getJudgeCount(2, false);
			case NUMBER_EARLY_GOOD:
				return getJudgeCount(2, true);
			case NUMBER_LATE_GOOD:
				return getJudgeCount(2, false);
			case NUMBER_BAD:
				return getJudgeCount(3, true) + getJudgeCount(3, false);
			case NUMBER_EARLY_BAD:
				return getJudgeCount(3, true);
			case NUMBER_LATE_BAD:
				return getJudgeCount(3, false);
			case NUMBER_POOR:
				return getJudgeCount(4, true) + getJudgeCount(4, false);
			case NUMBER_EARLY_POOR:
				return getJudgeCount(4, true);
			case NUMBER_LATE_POOR:
				return getJudgeCount(4, false);
			case NUMBER_MISS:
				return getJudgeCount(5, true) + getJudgeCount(5, false);
			case NUMBER_EARLY_MISS:
				return getJudgeCount(5, true);
			case NUMBER_LATE_MISS:
				return getJudgeCount(5, false);
			case NUMBER_POOR_PLUS_MISS:
				return getJudgeCount(4, true) + getJudgeCount(4, false) + getJudgeCount(5, true) + getJudgeCount(5, false);
			case NUMBER_BAD_PLUS_POOR_PLUS_MISS:
				return getJudgeCount(3, true) + getJudgeCount(3, false) + getJudgeCount(4, true) + getJudgeCount(4, false) + getJudgeCount(5, true) + getJudgeCount(5, false);
			case NUMBER_TOTALEARLY:
				int ecount = 0;
				for (int i = 1; i < 6; i++) {
					ecount += getJudgeCount(i, true);
				}
				return ecount;
			case NUMBER_TOTALLATE:
				int count = 0;
				for (int i = 1; i < 6; i++) {
					count += getJudgeCount(i, false);
				}
				return count;
			case NUMBER_COMBOBREAK:
				return getJudgeCount(3, true) + getJudgeCount(3, false) + getJudgeCount(4, true) + getJudgeCount(4, false);
			case NUMBER_TOTALNOTES:
			case NUMBER_TOTALNOTES2:
				if (main.getPlayerResource().getSongdata() != null) {
					return main.getPlayerResource().getSongdata().getNotes();
				}
				return Integer.MIN_VALUE;
			case NUMBER_MINBPM:
				if (main.getPlayerResource().getSongdata() != null) {
					return main.getPlayerResource().getSongdata().getMinbpm();
				}
				return Integer.MIN_VALUE;
			case NUMBER_MAXBPM:
				if (main.getPlayerResource().getSongdata() != null) {
					return main.getPlayerResource().getSongdata().getMaxbpm();
				}
				return Integer.MIN_VALUE;
			case NUMBER_HISPEED_LR2:
				if (main.getPlayerResource().getSongdata() != null) {
					SongData song = main.getPlayerResource().getSongdata();
					PlayConfig pc = main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode());
					return (int) (pc.getHispeed() * 100);
				}
				return Integer.MIN_VALUE;
			case NUMBER_HISPEED:
				if (main.getPlayerResource().getSongdata() != null) {
					SongData song = main.getPlayerResource().getSongdata();
					PlayConfig pc = main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode());
					return (int) pc.getHispeed();
				}
				return Integer.MIN_VALUE;
			case NUMBER_HISPEED_AFTERDOT:
				if (main.getPlayerResource().getSongdata() != null) {
					SongData song = main.getPlayerResource().getSongdata();
					PlayConfig pc = main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode());
					return (int) (pc.getHispeed() * 100) % 100;
				}
				return Integer.MIN_VALUE;
			case NUMBER_DURATION:
				if (main.getPlayerResource().getSongdata() != null) {
					SongData song = main.getPlayerResource().getSongdata();
					PlayConfig pc = main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode());
					return pc.getDuration();
				}
				return Integer.MIN_VALUE;
			case NUMBER_DURATION_GREEN:
				if (main.getPlayerResource().getSongdata() != null) {
					SongData song = main.getPlayerResource().getSongdata();
					PlayConfig pc = main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode());
					return pc.getDuration() * 3 / 5;
				}
				return Integer.MIN_VALUE;
			case NUMBER_PLAYLEVEL:
			case NUMBER_FOLDER_BEGINNER:
			case NUMBER_FOLDER_NORMAL:
			case NUMBER_FOLDER_HYPER:
			case NUMBER_FOLDER_ANOTHER:
			case NUMBER_FOLDER_INSANE:
				if (main.getPlayerResource().getSongdata() != null) {
					return main.getPlayerResource().getSongdata().getLevel();
				}
				return Integer.MIN_VALUE;
			case NUMBER_POINT:
				return score.getNowScore();
			case NUMBER_SCORE:
			case NUMBER_SCORE2:
			case NUMBER_SCORE3:
				return score.getScoreData() != null ? score.getNowEXScore() : Integer.MIN_VALUE;
			case NUMBER_MAXSCORE:
				return score.getScoreData() != null ? score.getScoreData().getNotes() : 0;
			case NUMBER_DIFF_NEXTRANK:
				return score.getNextRank();
			case NUMBER_SCORE_RATE:
				return score.getScoreData() != null ? score.getNowRateInt() : Integer.MIN_VALUE;
			case NUMBER_SCORE_RATE_AFTERDOT:
				return score.getScoreData() != null ? score.getNowRateAfterDot() : Integer.MIN_VALUE;
			case NUMBER_TOTAL_RATE:
			case NUMBER_SCORE_RATE2:
				return score.getScoreData() != null ? score.getRateInt() : Integer.MIN_VALUE;
			case NUMBER_TOTAL_RATE_AFTERDOT:
			case NUMBER_SCORE_RATE_AFTERDOT2:
				return score.getScoreData() != null ? score.getRateAfterDot() : Integer.MIN_VALUE;
			case NUMBER_HIGHSCORE:
				return score.getBestScore();
			case NUMBER_BEST_RATE:
				return score.getBestRateInt();
			case NUMBER_BEST_RATE_AFTERDOT:
				return score.getBestRateAfterDot();
			case NUMBER_TARGET_SCORE:
			case NUMBER_TARGET_SCORE2:
			case NUMBER_RIVAL_SCORE:
				return score.getRivalScore();
			case NUMBER_TARGET_SCORE_RATE:
			case NUMBER_TARGET_TOTAL_RATE:
			case NUMBER_TARGET_SCORE_RATE2:
				return score.getRivalRateInt();
			case NUMBER_TARGET_SCORE_RATE_AFTERDOT:
			case NUMBER_TARGET_TOTAL_RATE_AFTERDOT:
			case NUMBER_TARGET_SCORE_RATE_AFTERDOT2:
				return score.getRivalRateAfterDot();
			case NUMBER_DIFF_HIGHSCORE:
				return score.getNowEXScore() - score.getNowBestScore();
			case NUMBER_DIFF_EXSCORE:
			case NUMBER_DIFF_EXSCORE2:
			case NUMBER_DIFF_TARGETSCORE:
				return score.getNowEXScore() - score.getNowRivalScore();

			case NUMBER_TOTALNOTE_NORMAL: {
				final SongData song = main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return song.getInformation().getN();
				}
				return Integer.MIN_VALUE;
			}
			case NUMBER_TOTALNOTE_LN: {
				final SongData song = main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return song.getInformation().getLn();
				}
				return Integer.MIN_VALUE;
			}
			case NUMBER_TOTALNOTE_SCRATCH: {
				final SongData song = main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return song.getInformation().getS();
				}
				return Integer.MIN_VALUE;
			}
			case NUMBER_TOTALNOTE_BSS: {
				final SongData song = main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return song.getInformation().getLs();
				}
				return Integer.MIN_VALUE;
			}
			case NUMBER_DENSITY_ENDPEAK: {
				final SongData song = main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return (int) song.getInformation().getDensity();
				}
				return Integer.MIN_VALUE;
			}
			case NUMBER_DENSITY_ENDPEAK_AFTERDOT: {
				final SongData song = main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return ((int) (song.getInformation().getDensity() * 100)) % 100;
				}
				return Integer.MIN_VALUE;
			}
			case NUMBER_SONGGAUGE_TOTAL: {
				final SongData song = main.getPlayerResource().getSongdata();
				if (song != null && song.getInformation() != null) {
					return (int) song.getInformation().getTotal();
				}
				return Integer.MIN_VALUE;
			}
		}
		return 0;
	}

	public float getSliderValue(int id) {
		switch (id) {
			case BARGRAPH_SCORERATE:
				return score.getRate();
			case BARGRAPH_SCORERATE_FINAL:
				return score.getNowRate();
			case BARGRAPH_BESTSCORERATE_NOW:
				return score.getNowBestScoreRate();
			case BARGRAPH_BESTSCORERATE:
				return score.getBestScoreRate();
			case BARGRAPH_TARGETSCORERATE_NOW:
				return score.getNowRivalScoreRate();
			case BARGRAPH_TARGETSCORERATE:
				return score.getRivalScoreRate();
		}
		return 0;
	}

	public void setSliderValue(int id, float value) {
	}

	public SkinOffset getOffsetValue(int id) {
		return main.getOffset(id);
	}

	public String getTextValue(int id) {
		if (main.getPlayerResource() != null) {
			PlayerResource resource = main.getPlayerResource();
			SongData song = resource.getSongdata();
			switch (id) {
				case STRING_RIVAL:
					return TargetProperty.getAllTargetProperties()[main.getPlayerResource().getPlayerConfig().getTarget()].getName();
				case STRING_PLAYER:
					return main.getPlayerConfig().getName();
			case STRING_TITLE:
				return song != null ? song.getTitle() : "";
			case STRING_SUBTITLE:
				return song != null ? song.getSubtitle() : "";
			case STRING_FULLTITLE:
				return song != null ? song.getFullTitle() : "";
			case STRING_ARTIST:
				return song != null ? song.getArtist() : "";
			case STRING_SUBARTIST:
				return song != null ? song.getSubartist() : "";
				case STRING_FULLARTIST:
					return song != null ? song.getFullArtist() : "";
			case STRING_GENRE:
				return song != null ? song.getGenre() : "";
				case STRING_TABLE_NAME:
					return song != null ? resource.getTablename() : "";
				case STRING_TABLE_LEVEL:
					return song != null ? resource.getTablelevel() : "";
			}
		}
		return "";
	}

	public TextureRegion getImage(int imageid) {
		switch(imageid) {
		case IMAGE_BACKBMP:
			return main.getPlayerResource().getBMSResource().getBackbmp();
		case IMAGE_STAGEFILE:
			return main.getPlayerResource().getBMSResource().getStagefile();
			case IMAGE_BANNER:
				return main.getPlayerResource().getBMSResource().getBanner();
		case IMAGE_BLACK:
			return main.black;
		case IMAGE_WHITE:
			return main.white;			
		}
		return null;
	}

	public int getImageIndex(int id) {
		switch(id) {
			case BUTTON_GAUGE_1P:
				return main.getPlayerResource().getPlayerConfig().getGauge();
			case BUTTON_RANDOM_1P:
				return main.getPlayerResource().getPlayerConfig().getRandom();
			case BUTTON_RANDOM_2P:
				return main.getPlayerResource().getPlayerConfig().getRandom2();
			case BUTTON_DPOPTION:
				return main.getPlayerResource().getPlayerConfig().getDoubleoption();
			case BUTTON_HSFIX:
				return main.getPlayerResource().getPlayerConfig().getFixhispeed();
			case BUTTON_BGA:
				return main.getPlayerResource().getConfig().getBga();
			case BUTTON_ASSIST_EXJUDGE:
				return main.getPlayerResource().getPlayerConfig().getJudgewindowrate() > 100 ? 1 : 0;
			case BUTTON_ASSIST_CONSTANT:
				return main.getPlayerResource().getPlayerConfig().isConstant() ? 1 : 0;
			case BUTTON_ASSIST_JUDGEAREA:
				return main.getPlayerResource().getPlayerConfig().isShowjudgearea() ? 1 : 0;
			case BUTTON_ASSIST_LEGACY:
				return main.getPlayerResource().getPlayerConfig().isLegacynote() ? 1 : 0;
			case BUTTON_ASSIST_MARKNOTE:
				return main.getPlayerResource().getPlayerConfig().isMarkprocessednote() ? 1 : 0;
			case BUTTON_ASSIST_BPMGUIDE:
				return main.getPlayerResource().getPlayerConfig().isBpmguide() ? 1 : 0;
			case BUTTON_ASSIST_NOMINE:
				return main.getPlayerResource().getPlayerConfig().isNomine() ? 1 : 0;
			case BUTTON_LNMODE:
				return main.getPlayerResource().getPlayerConfig().getLnmode();
		}
		return Integer.MIN_VALUE;
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public enum SoundType {
		BGM, SOUND
	}

	public void setSound(int id, String path, SoundType type, boolean loop) {
		Path p = null;
		switch(type) {
			case BGM:
				p = main.getSoundManager().getBGMPath();
				break;
			case SOUND:
				p = main.getSoundManager().getSoundPath();
				break;
		}
		if(p == null) {
			return;
		}
		path = p.resolve(path).toString();
		path = path.substring(0, path.lastIndexOf('.'));

		for(File f : new File[]{new File(path + ".wav"), new File(path + ".ogg"), new File(path + ".mp3")}) {
			if(f.exists()) {
				String newpath = f.getPath();
				String oldpath = soundmap.get(id);
				if(newpath.equals(oldpath)) {
					return;
				}
				if(oldpath != null) {
					main.getAudioProcessor().dispose(oldpath);
				}
				soundmap.put(id, newpath);
				soundloop.put(id, loop);
				break;
			}
		}
	}

	public String getSound(int id) {
		return soundmap.get(id);
	}
	
	public void play(int id) {
		final String path = soundmap.get(id);
		if(path != null) {
			main.getAudioProcessor().play(path, main.getPlayerResource().getConfig().getSystemvolume(), soundloop.get(id));
		}
	}
	
	public void stop(int id) {
		final String path = soundmap.get(id);
		if(path != null) {
			main.getAudioProcessor().stop(path);
		}		
	}
}
