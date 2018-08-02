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

	public SkinOffset getOffsetValue(int id) {
		return main.getOffset(id);
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
