package bms.player.beatoraja;

import bms.player.beatoraja.SkinConfig.Offset;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;
import bms.player.beatoraja.skin.property.EventFactory.EventType;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.IntMap;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Optional;

/**
 * プレイヤー内の各状態の抽象クラス
 *
 * @author exch
 */
public abstract class MainState {
	
	public final MainController main;
	
	public final MainStateType type;

	/**
	 * スキン
	 */
	private Skin skin;

	private Stage stage;
	
	public final TimerManager timer;
	
	public final PlayerResource resource;

	private final ScoreDataProperty score = new ScoreDataProperty();

	public MainState(MainController main, MainStateType type) {
		this.main = main;
		this.type = type;
		timer = main.getTimer();
		resource = main.getPlayerResource();
	}

	public abstract void create();

	public void prepare() {

	}

	public void shutdown() {

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
		Optional.ofNullable(skin).ifPresent(skin -> skin.dispose());
		skin = null;
		Optional.ofNullable(stage).ifPresent(skin -> skin.dispose());
		stage = null;
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

	public void executeEvent(EventType e) {
		executeEvent(e, 0, 0);
	}

	public void executeEvent(EventType e, int arg) {
		executeEvent(e, arg, 0);
	}

	public void executeEvent(EventType e, int arg1, int arg2) {
		e.event.exec(this, arg1, arg2);
	}
	
	public final ScoreDataProperty getScoreDataProperty() {
		return score;
	}

	public final Skin getSkin() {
		return skin;
	}

	public final void setSkin(Skin skin) {
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

	public final void loadSkin(SkinType skinType) {
		setSkin(SkinLoader.load(this, skinType));
	}

	public int getJudgeCount(int judge, boolean fast) {
		ScoreData sd = score.getScoreData();
		return sd != null ? sd.getJudgeCount(judge, fast) : 0;
	}

	public final SkinOffset getOffsetValue(int id) {
		return main.getOffset(id);
	}

	public final TextureRegion getImage(int imageid) {
		return switch (imageid) {
			case IMAGE_BACKBMP -> resource.getBMSResource().getBackbmp();
			case IMAGE_STAGEFILE -> resource.getBMSResource().getStagefile();
			case IMAGE_BANNER -> resource.getBMSResource().getBanner();
			case IMAGE_BLACK -> main.black;
			case IMAGE_WHITE -> main.white;
			default -> null;
		};
	}

	public final Stage getStage() {
		return stage;
	}

	public final void setStage(Stage stage) {
		this.stage = stage;
	}

	public String getSound(SystemSoundManager.SoundType sound) {
		return main.getSoundManager().getSound(sound);
	}
	
	public void play(SystemSoundManager.SoundType sound) {
		play(sound, false);
	}
	
	public void play(SystemSoundManager.SoundType sound, boolean loop) {
		main.getSoundManager().play(sound, loop);
	}
	
	public void stop(SystemSoundManager.SoundType sound) {
		main.getSoundManager().stop(sound);
	}
	
	public enum MainStateType {
		MUSICSELECT,
		DECIDE,
		PLAY,
		RESULT,
		COURSERESULT,
		CONFIG,
		SKINCONFIG;
	}
}
