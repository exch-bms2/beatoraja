package bms.player.beatoraja;

import java.nio.file.Path;

import bms.player.beatoraja.SkinConfig.Offset;
import bms.player.beatoraja.audio.AudioDriver;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
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

	private final IntMap<String> soundmap = new IntMap<String>();
	private final IntMap<Boolean> soundloop = new IntMap<Boolean>();

	private final ScoreDataProperty score = new ScoreDataProperty();

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

	public void executeEvent(int id) {
		executeEvent(id, 0, 0);
	}

	public void executeEvent(int id, int arg) {
		executeEvent(id, arg, 0);
	}

	public void executeEvent(int id, int arg1, int arg2) {
		if (SkinPropertyMapper.isCustomEventId(id)) {
			skin.executeCustomEvent(this, id, arg1, arg2);
		}
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
				if(offset == null || e.value == null) {
					continue;
				}
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
		for(Path p :getSoundPaths(path, type)) {
			String newpath = p.toString();
			String oldpath = soundmap.get(id);
			if (newpath.equals(oldpath)) {
				return;
			}
			if (oldpath != null) {
				main.getAudioProcessor().dispose(oldpath);
			}
			soundmap.put(id, newpath);
			soundloop.put(id, loop);
			return;
		}
	}
	
	public boolean setSoundFile(int id, String path, SoundType type, boolean loop) {		
		for(Path p : AudioDriver.getPaths(path)) {
			String newpath = p.toString();
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
		return false;
	}
	
	public Path[] getSoundPaths(String filename, SoundType type) {
		Path p = null;
		switch (type) {
		case BGM:
			p = main.getSoundManager().getBGMPath();
			break;
		case SOUND:
			p = main.getSoundManager().getSoundPath();
			break;
		}
		
		Array<Path> paths = new Array();
		if(p != null) {
			paths.addAll(AudioDriver.getPaths(p.resolve(filename).toString()));			
		}
		paths.addAll(AudioDriver.getPaths("defaultsound/" + filename.substring(filename.contains("/") || filename.contains("\\") ? Math.max(filename.lastIndexOf('/'),filename.lastIndexOf('\\')) + 1 : 0)));
		return paths.toArray(Path.class);
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
	
	public enum MainStateType {
		MUSICSELECT,DECIDE,PLAY,RESULT,COURSERESULT,CONFIG,SKINCONFIG;
	}
}
