package bms.player.beatoraja;

import java.io.File;
import java.nio.file.Path;

import bms.player.beatoraja.SkinConfig.Offset;
import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;
import bms.player.beatoraja.song.SongData;

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
				return song != null ? resource.getTableFullname() : "";
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
