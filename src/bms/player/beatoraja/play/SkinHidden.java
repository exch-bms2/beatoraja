package bms.player.beatoraja.play;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.SkinObject;
import bms.player.beatoraja.skin.SkinSource;
import bms.player.beatoraja.skin.SkinSourceImage;

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

	private int timer;
	private int cycle;

	public SkinHidden(TextureRegion[] image, int timer, int cycle) {
		this.timer = timer;
		this.cycle = cycle;
		originalImages = image;
		trimmedImages = new TextureRegion[originalImages.length];
		for(int i = 0; i < trimmedImages.length; i++) {
			trimmedImages[i] = new TextureRegion(originalImages[i]);
		}
	}

	public void draw(SkinObjectRenderer sprite, long time, MainState state) {
		if(originalImages == null) {
			return;
		}
		if(this.state != state) {
			this.state = state;
			disapearLineAddedLift = disapearLine;
		}
		if(isDisapearLineLinkLift && disapearLine >= 0 && previousLift != state.getOffsetValue(OFFSET_LIFT).y) {
			disapearLineAddedLift = disapearLine + state.getOffsetValue(OFFSET_LIFT).y;
			previousLift = state.getOffsetValue(OFFSET_LIFT).y;
		}
		Rectangle r = this.getDestination(time,state);
		//描画領域上端が消失ラインより下なら描画処理を行わない
		if (r != null && ((r.y + r.height > disapearLineAddedLift && disapearLine >= 0) || disapearLine < 0)) {
			//描画領域と消失ラインが重なっている場合
			if(r.y < disapearLineAddedLift && disapearLine >= 0) {
				//前回と位置が異なる場合は画像加工処理を行う
				if(previousY != r.y) {
					for(int i = 0; i < trimmedImages.length; i++) {
						trimmedImages[i] = new TextureRegion(originalImages[i]);
					}
					for(int i = 0; i < trimmedImages.length; i++) {
						trimmedImages[i].setRegionHeight( (int) Math.round(originalImages[i].getRegionHeight() * (r.y + r.height - disapearLineAddedLift) / r.height));
					}
					previousY = r.y;
				}
				draw(sprite, trimmedImages[getImageIndex(trimmedImages.length, time, state)], r.x, disapearLineAddedLift, r.width, r.y + r.height - disapearLineAddedLift, state);
			//画像加工処理が必要ない場合
			} else {
				draw(sprite, originalImages[getImageIndex(originalImages.length, time, state)], r.x, r.y, r.width, r.height, state);
			}
		}
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

		if (timer != 0 && timer < MainController.timerCount) {
			if (!state.main.isTimerOn(timer)) {
				return 0;
			}
			time -= state.main.getTimer(timer);
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
