package bms.player.beatoraja.play;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.SkinObject;
import bms.player.beatoraja.skin.SkinSource;
import bms.player.beatoraja.skin.SkinSourceImage;

import bms.player.beatoraja.skin.property.TimerProperty;
import bms.player.beatoraja.skin.property.TimerPropertyFactory;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import static bms.player.beatoraja.skin.SkinProperty.*;

public class SkinHidden extends SkinObject {

	MainState state;

	/**
	 * イメージ
	 */
	private TextureRegion[] originalImages;
	private TextureRegion[] trimmedImages;

	/**
	 * 消失ラインのy座標(スキン設定値) この座標以下の部分はトリミングする 負の値の場合はトリミングしない
	 */
	private float disapearLine = -1;
	/**
	 * 消失ラインのy座標(計算用)
	 */
	private float disapearLineAddedLift = -1;
	/**
	 * 消失ラインのy座標とリフトを連動させるかどうか
	 */
	private boolean isDisapearLineLinkLift = true;

	private float previousY = Float.MIN_VALUE;
	private float previousLift = Float.MIN_VALUE;

	private TimerProperty timer;
	private int cycle;
	
	private int imageindex;

	public SkinHidden(TextureRegion[] image, int timer, int cycle) {
		this.timer = timer > 0 ? TimerPropertyFactory.getTimerProperty(timer) : null;
		this.cycle = cycle;
		originalImages = image;
		trimmedImages = new TextureRegion[originalImages.length];
		for(int i = 0; i < trimmedImages.length; i++) {
			trimmedImages[i] = new TextureRegion(originalImages[i]);
		}
	}

	public SkinHidden(TextureRegion[] image, TimerProperty timer, int cycle) {
		this.timer = timer;
		this.cycle = cycle;
		originalImages = image;
		trimmedImages = new TextureRegion[originalImages.length];
		for(int i = 0; i < trimmedImages.length; i++) {
			trimmedImages[i] = new TextureRegion(originalImages[i]);
		}
	}

	@Override
	public void prepare(long time, MainState state) {
		if(originalImages == null) {
			draw = false;
			return;
		}
		if(this.state != state) {
			this.state = state;
			disapearLineAddedLift = disapearLine;
		}
		super.prepare(time, state);
		if(isDisapearLineLinkLift && disapearLine >= 0 && previousLift != state.getOffsetValue(OFFSET_LIFT).y) {
			disapearLineAddedLift = disapearLine + state.getOffsetValue(OFFSET_LIFT).y;
			previousLift = state.getOffsetValue(OFFSET_LIFT).y;
		}
		
		imageindex = getImageIndex(originalImages.length, time, state);
	}

	public void draw(SkinObjectRenderer sprite) {
		//描画領域上端が消失ラインより下なら描画処理を行わない
		if (((region.y + region.height > disapearLineAddedLift && disapearLine >= 0) || disapearLine < 0)) {
			//描画領域と消失ラインが重なっている場合
			if(region.y < disapearLineAddedLift && disapearLine >= 0) {
				//前回と位置が異なる場合は画像加工処理を行う
				if(previousY != region.y) {
					for(int i = 0; i < trimmedImages.length; i++) {
						trimmedImages[i] = new TextureRegion(originalImages[i]);
					}
					for(int i = 0; i < trimmedImages.length; i++) {
						trimmedImages[i].setRegionHeight( (int) Math.round(originalImages[i].getRegionHeight() * (region.y + region.height - disapearLineAddedLift) / region.height));
					}
					previousY = region.y;
				}
				draw(sprite, trimmedImages[imageindex], region.x, disapearLineAddedLift, region.width, region.y + region.height - disapearLineAddedLift);
			//画像加工処理が必要ない場合
			} else {
				draw(sprite, originalImages[imageindex], region.x, region.y, region.width, region.height);
			}
		}
	}
	
	public void draw(SkinObjectRenderer sprite, long time, MainState state) {
		draw(sprite);
	}

	public void dispose() {
		if(originalImages != null) {
			for(TextureRegion tex : originalImages) {
				tex.getTexture().dispose();
			}
			originalImages = null;
		}
		if(trimmedImages != null) {
			for(TextureRegion tex : trimmedImages) {
				tex.getTexture().dispose();
			}
			trimmedImages = null;
		}
	}

	private int getImageIndex(int length, long time, MainState state) {
		if (cycle == 0) {
			return 0;
		}

		if (timer != null) {
			if (timer.isOff(state)) {
				return 0;
			}
			time -= timer.get(state);
		}
		if (time < 0) {
			return 0;
		}
		return (int) ((time * length / cycle) % length);
	}

	public float getDisapearLine() {
		return disapearLine;
	}

	public void setDisapearLine(float disapearLine) {
		this.disapearLine = disapearLine;
	}

	public boolean isDisapearLineLinkLift() {
		return isDisapearLineLinkLift;
	}

	public void setDisapearLineLinkLift(boolean isDisapearLineLinkLift) {
		this.isDisapearLineLinkLift = isDisapearLineLinkLift;
	}
}
