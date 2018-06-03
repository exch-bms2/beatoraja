package bms.player.beatoraja;

import java.io.File;
import java.nio.file.Path;
import java.util.Calendar;

import bms.player.beatoraja.SkinConfig.Offset;
import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;
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

	public void prepare() {

	}

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
		if (skin != null) {
			skin.dispose();
			skin = null;
		}
		if (stage != null) {
			stage.dispose();
			stage = null;
		}
	}

	public void executeClickEvent(int id, int arg) {

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
		case OPTION_NO_TEXT:
			return model != null && !model.hasDocument();
		case OPTION_TEXT:
			return model != null && model.hasDocument();
		case OPTION_NO_LN:
			return model != null && !model.hasAnyLongNote();
		case OPTION_LN:
			return model != null && model.hasAnyLongNote();
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
		case OPTION_BPMSTOP:
			if (main.getPlayerResource().getSongdata() != null) {
				return main.getPlayerResource().getSongdata().isBpmstop();
			}
			return false;
		case OPTION_OFFLINE:
			return main.getIRConnection() == null;
		case OPTION_ONLINE:
			return main.getIRConnection() != null;
		case OPTION_TABLE_SONG:
			return main.getPlayerResource().getTablename().length() != 0;
		}
		return false;
	}

	public Skin getSkin() {
		return skin;
	}

	public void setSkin(Skin skin) {
		if (this.skin != null) {
			this.skin.dispose();
		}
		this.skin = skin;
		if (skin != null) {
			for (IntMap.Entry<Offset> e : skin.getOffset().entries()) {
				SkinOffset offset = main.getOffset(e.key);
				offset.x = e.value.x;
				offset.y = e.value.y;
				offset.w = e.value.w;
				offset.h = e.value.h;
				offset.r = e.value.r;
				offset.a = e.value.a;
			}
		}
	}

	public void loadSkin(SkinType skinType) {
		setSkin(SkinLoader.load(this, skinType));
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
			return getJudgeCount(3, true) + getJudgeCount(3, false) + getJudgeCount(4, true) + getJudgeCount(4, false)
					+ getJudgeCount(5, true) + getJudgeCount(5, false);
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
		case NUMBER_MAINBPM:
			if (main.getPlayerResource().getSongdata() != null) {
				return main.getPlayerResource().getSongdata().getMainbpm();
			}
			return Integer.MIN_VALUE;
		case NUMBER_HISPEED_LR2:
			if (main.getPlayerResource().getSongdata() != null) {
				SongData song = main.getPlayerResource().getSongdata();
				PlayConfig pc = main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode()).getPlayconfig();
				return (int) (pc.getHispeed() * 100);
			}
			return Integer.MIN_VALUE;
		case NUMBER_HISPEED:
			if (main.getPlayerResource().getSongdata() != null) {
				SongData song = main.getPlayerResource().getSongdata();
				PlayConfig pc = main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode()).getPlayconfig();
				return (int) pc.getHispeed();
			}
			return Integer.MIN_VALUE;
		case NUMBER_HISPEED_AFTERDOT:
			if (main.getPlayerResource().getSongdata() != null) {
				SongData song = main.getPlayerResource().getSongdata();
				PlayConfig pc = main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode()).getPlayconfig();
				return (int) (pc.getHispeed() * 100) % 100;
			}
			return Integer.MIN_VALUE;
		case NUMBER_DURATION:
			if (main.getPlayerResource().getSongdata() != null) {
				SongData song = main.getPlayerResource().getSongdata();
				PlayConfig pc = main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode()).getPlayconfig();
				return pc.getDuration();
			}
			return Integer.MIN_VALUE;
		case NUMBER_DURATION_GREEN:
			if (main.getPlayerResource().getSongdata() != null) {
				SongData song = main.getPlayerResource().getSongdata();
				PlayConfig pc = main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode()).getPlayconfig();
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
				return TargetProperty.getAllTargetProperties()[main.getPlayerResource().getPlayerConfig().getTarget()]
						.getName();
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
			case STRING_TABLE_FULL:
				return song != null ? resource.getTablename() + resource.getTablelevel() : "";
			}
		}
		return "";
	}

	public TextureRegion getImage(int imageid) {
		switch (imageid) {
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
		switch (id) {
		case BUTTON_GAUGE_1P:
			return main.getPlayerResource().getPlayerConfig().getGauge();
		case BUTTON_RANDOM_1P:
			return main.getPlayerResource().getPlayerConfig().getRandom();
		case BUTTON_RANDOM_2P:
			return main.getPlayerResource().getPlayerConfig().getRandom2();
		case BUTTON_DPOPTION:
			return main.getPlayerResource().getPlayerConfig().getDoubleoption();
		case BUTTON_HSFIX:
			if (main.getPlayerResource().getSongdata() != null) {
				SongData song = main.getPlayerResource().getSongdata();
				PlayConfig pc = main.getPlayerResource().getPlayerConfig().getPlayConfig(song.getMode()).getPlayconfig();
				return pc.getFixhispeed();
			} else if(main.getPlayerResource().getCourseData() != null) {
				PlayConfig pc = null;
				for(SongData song : main.getPlayerResource().getCourseData().getSong()) {
					if(song.getPath() == null) {
						pc = null;
						break;
					}
					PlayConfig pc2 = main.getPlayerConfig().getPlayConfig(song.getMode()).getPlayconfig();
					if(pc == null) {
						pc = pc2;
					}
					if(pc != pc2) {
						pc = null;
						break;
					}
				}
				if(pc != null) {
					return pc.getFixhispeed();
				}
			}
			return Integer.MIN_VALUE;
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
		switch (type) {
		case BGM:
			p = main.getSoundManager().getBGMPath();
			break;
		case SOUND:
			p = main.getSoundManager().getSoundPath();
			break;
		}
		if (p != null) {
			path = p.resolve(path).toString();
			path = path.substring(0, path.lastIndexOf('.'));
		} else {
			path = "defaultsound/" + path.substring(path.contains("/") || path.contains("\\") ? Math.max(path.lastIndexOf('/'),path.lastIndexOf('\\')) + 1 : 0, path.contains(".") ? path.lastIndexOf('.') : path.length());
		}

		if(!setSoundFile(id, path, type, loop)) {
			path = "defaultsound/" + path.substring(path.contains("/") || path.contains("\\") ? Math.max(path.lastIndexOf('/'),path.lastIndexOf('\\')) + 1 : 0, path.length());
			setSoundFile(id, path, type, loop);
		}
	}

	public boolean setSoundFile(int id, String path, SoundType type, boolean loop) {
		for (File f : new File[] { new File(path + ".wav"), new File(path + ".ogg"), new File(path + ".mp3"),
				new File(path + ".flac") }) {
			if (f.exists()) {
				String newpath = f.getPath();
				String oldpath = soundmap.get(id);
				if (newpath.equals(oldpath)) {
					return true;
				}
				if (oldpath != null) {
					main.getAudioProcessor().dispose(oldpath);
				}
				soundmap.put(id, newpath);
				soundloop.put(id, loop);
				return true;
			}
		}
		return false;
	}

	public String getSound(int id) {
		return soundmap.get(id);
	}

	public void play(int id) {
		final String path = soundmap.get(id);
		if (path != null) {
			main.getAudioProcessor().play(path, main.getPlayerResource().getConfig().getSystemvolume(),
					soundloop.get(id));
		}
	}

	public void stop(int id) {
		final String path = soundmap.get(id);
		if (path != null) {
			main.getAudioProcessor().stop(path);
		}
	}
}
