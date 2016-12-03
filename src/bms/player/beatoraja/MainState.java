package bms.player.beatoraja;

import java.util.Arrays;
import java.util.Calendar;

import bms.player.beatoraja.skin.Skin;
import bms.player.beatoraja.song.SongData;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;

import static bms.player.beatoraja.skin.SkinProperty.*;

public abstract class MainState {

	private final MainController main;

	private long starttime;

	private long[] timer = new long[256];

	private Skin skin;

	private Stage stage;


	public MainState() {
		this(null);
	}

	public MainState(MainController main) {
		this.main = main;
		Arrays.fill(timer, Long.MIN_VALUE);
		Pixmap bp = new Pixmap(1,1, Pixmap.Format.RGBA8888);
		bp.drawPixel(0,0, Color.toIntBits(255,0,0,0));
		black = new TextureRegion(new Texture(bp));
		Pixmap hp = new Pixmap(1,1, Pixmap.Format.RGBA8888);
		hp.drawPixel(0,0, Color.toIntBits(255,255,255,255));
		white = new TextureRegion(new Texture(hp));
	}

	public MainController getMainController() {
		return main;
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

	public abstract void dispose();

	public long getStartTime() {
		return starttime;
	}

	public void setStartTime(long starttime) {
		this.starttime = starttime;
	}

	public int getNowTime() {
		return (int) (System.currentTimeMillis() - starttime);
	}

	public long[] getTimer() {
		return timer;
	}

	public void executeClickEvent(int id) {

	}

	public boolean getBooleanValue(int id) {
		final SongData model = getMainController().getPlayerResource().getSongdata();
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
		case OPTION_BGAEXTEND:
			return true;
		case OPTION_SCOREGRAPHOFF:
			return false;
		case OPTION_SCOREGRAPHON:
			return true;
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
		case OPTION_JUDGE_EASY:
			return model != null && (model.getJudge() == 3 || model.getJudge() >= 100);
		case OPTION_JUDGE_NORMAL:
			return model != null && (model.getJudge() == 2 || (model.getJudge() >= 80 && model.getJudge() < 100));
		case OPTION_JUDGE_HARD:
			return model != null && (model.getJudge() == 1 || (model.getJudge() >= 50 && model.getJudge() < 80));
		case OPTION_JUDGE_VERYHARD:
			return model != null && (model.getJudge() == 0 || (model.getJudge() >= 10 && model.getJudge() < 50));
		case OPTION_5KEYSONG:
			return model != null && model.getMode() == 5;
		case OPTION_7KEYSONG:
			return model != null && model.getMode() == 7;
		case OPTION_9KEYSONG:
			return model != null && model.getMode() == 9;
		case OPTION_10KEYSONG:
			return model != null && model.getMode() == 10;
		case OPTION_14KEYSONG:
			return model != null && model.getMode() == 14;
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

		}
		return false;
	}

	public Skin getSkin() {
		return skin;
	}

	public void setSkin(Skin skin) {
		this.skin = skin;
	}

	public int getJudgeCount(int judge, boolean fast) {
		return 0;
	}
	
	private Calendar cl = Calendar.getInstance();

	public int getNumberValue(int id) {
		switch (id) {
		case NUMBER_JUDGETIMING:
			return getMainController().getPlayerResource().getConfig().getJudgetiming();
		case NUMBER_TIME_YEAR:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.YEAR);
		case NUMBER_TIME_MONTH:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.MONTH) + 1;
		case NUMBER_TIME_DAY:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.DATE);
		case NUMBER_TIME_HOUR:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.HOUR_OF_DAY);
		case NUMBER_TIME_MINUTE:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.MINUTE);
		case NUMBER_TIME_SECOND:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.SECOND);
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
		case BUTTON_GAUGE_1P:
			return getMainController().getPlayerResource().getConfig().getGauge();
		case BUTTON_RANDOM_1P:
			return getMainController().getPlayerResource().getConfig().getRandom();
		case BUTTON_RANDOM_2P:
			return getMainController().getPlayerResource().getConfig().getRandom2();
		case BUTTON_DPOPTION:
			return getMainController().getPlayerResource().getConfig().getDoubleoption();
		case BUTTON_HSFIX:
			return getMainController().getPlayerResource().getConfig().getFixhispeed();
		case BUTTON_BGA:
			return getMainController().getPlayerResource().getConfig().getBga();
		case BUTTON_JUDGEDETAIL:
			return getMainController().getPlayerResource().getConfig().getJudgedetail();
		case BUTTON_ASSIST_EXJUDGE:
			return getMainController().getPlayerResource().getConfig().isExpandjudge() ? 1 : 0;
		case BUTTON_ASSIST_CONSTANT:
			return getMainController().getPlayerResource().getConfig().isConstant() ? 1 : 0;
		case BUTTON_ASSIST_JUDGEAREA:
			return getMainController().getPlayerResource().getConfig().isShowjudgearea() ? 1 : 0;
		case BUTTON_ASSIST_LEGACY:
			return getMainController().getPlayerResource().getConfig().isLegacynote() ? 1 : 0;
		case BUTTON_ASSIST_MARKNOTE:
			return getMainController().getPlayerResource().getConfig().isMarkprocessednote() ? 1 : 0;
		case BUTTON_ASSIST_BPMGUIDE:
			return getMainController().getPlayerResource().getConfig().isBpmguide() ? 1 : 0;
		case BUTTON_ASSIST_NOMINE:
			return getMainController().getPlayerResource().getConfig().isNomine() ? 1 : 0;
		case NUMBER_TOTALNOTES:
		case NUMBER_TOTALNOTES2:
			if (getMainController().getPlayerResource().getSongdata() != null) {
				return getMainController().getPlayerResource().getSongdata().getNotes();
			}
			return Integer.MIN_VALUE;
		case NUMBER_MINBPM:
			if (getMainController().getPlayerResource().getSongdata() != null) {
				return getMainController().getPlayerResource().getSongdata().getMinbpm();
			}
			return Integer.MIN_VALUE;
		case NUMBER_MAXBPM:
			if (getMainController().getPlayerResource().getSongdata() != null) {
				return getMainController().getPlayerResource().getSongdata().getMaxbpm();
			}
			return Integer.MIN_VALUE;
		case NUMBER_PLAYLEVEL:
		case NUMBER_FOLDER_BEGINNER:
		case NUMBER_FOLDER_NORMAL:
		case NUMBER_FOLDER_HYPER:
		case NUMBER_FOLDER_ANOTHER:
		case NUMBER_FOLDER_INSANE:
			if (getMainController().getPlayerResource().getSongdata() != null) {
				return getMainController().getPlayerResource().getSongdata().getLevel();
			}
			return Integer.MIN_VALUE;

		}
		return 0;
	}

	public float getSliderValue(int id) {
		return 0;
	}

	public void setSliderValue(int id, float value) {
	}

	public String getTextValue(int id) {
		if (getMainController().getPlayerResource() != null) {
			SongData song = getMainController().getPlayerResource().getSongdata();
			switch (id) {
			case STRING_TITLE:
				return song != null ? song.getTitle() : "";
			case STRING_SUBTITLE:
				return song != null ? song.getSubtitle() : "";
			case STRING_FULLTITLE:
				return song != null ? song.getTitle() + " " + song.getSubtitle() : "";
			case STRING_ARTIST:
				return song != null ? song.getArtist() : "";
			case STRING_SUBARTIST:
				return song != null ? song.getSubartist() : "";
			case STRING_GENRE:
				return song != null ? song.getGenre() : "";
			}
		}
		return "";
	}

	private TextureRegion black;
	private TextureRegion white;

	public TextureRegion getImage(int imageid) {
		if (getMainController().getPlayerResource().getBGAManager() != null) {
			if (imageid == IMAGE_BACKBMP) {
				return getMainController().getPlayerResource().getBGAManager().getBackbmpData();
			}
			if (imageid == IMAGE_STAGEFILE) {
				return getMainController().getPlayerResource().getBGAManager().getStagefileData();
			}
		}
		if(imageid == IMAGE_BLACK) {
			return black;
		}
		if(imageid == IMAGE_WHITE) {
			return white;
		}
		return null;
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}
}
