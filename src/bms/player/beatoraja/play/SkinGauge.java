package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;

import static bms.player.beatoraja.play.GrooveGauge.*;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.graphics.Color;

/**
 * ゲージオブジェクト
 *
 * @author exch
 */
public class SkinGauge extends SkinObject {

	public static final int ANIMATION_RANDOM = 0;
	public static final int ANIMATION_INCLEASE = 1;
	public static final int ANIMATION_DECLEASE = 2;
	public static final int ANIMATION_FLICKERING = 3; //PMSゲージ明滅用
	
	/**
	 * イメージ
	 */
	private SkinSource image;
	/**
	 * アニメーションの種類(未実装)
	 */
	private int animationType = 0;
	/**
	 * アニメーションする範囲
	 */
	private int animationRange = 4;
	/**
	 * アニメーション間隔(ms)
	 */
	private long duration = 33;
	/**
	 * ゲージの粒の数
	 */
	private int parts = 50;
	
	private int animation;
	private long atime;

	public SkinGauge(TextureRegion[][] image, int timer, int cycle, int parts, int type, int range, int duration) {
		this.image = new SkinSourceImage(image, timer, cycle);
		this.parts = parts;
		this.animationType = type;
		this.animationRange = range;
		this.duration = duration;
	}

	@Override
	public void draw(SkinObjectRenderer sprite, long time, MainState state) {
		final Rectangle gr = getDestination(time, state);
		final GrooveGauge gauge = ((BMSPlayer) state).getGauge();
		if (gauge == null || gr == null) {
			return;
		}

		if (atime < time) {
			switch(animationType) {
			case ANIMATION_RANDOM:
				animation = (int) (Math.random() * (animationRange + 1));
				break;
			case ANIMATION_INCLEASE:				
				animation = (animation + animationRange) % (animationRange + 1);
				break;
			case ANIMATION_DECLEASE:				
				animation = (animation + 1) % (animationRange + 1);
				break;
			case ANIMATION_FLICKERING:
				animation = 0;
				break;
			}
			atime = time + duration;
		}
		final float max = gauge.getMaxValue();
		final float value = gauge.getValue();
		final TextureRegion[] images = image.getImages(time, state);

		int exgauge = 0;
		final int type = gauge.getType();
		if (type == ASSISTEASY || type == EASY || type == EXHARD || type == HAZARD || type == EXCLASS || type == EXHARDCLASS) {
			exgauge = 4;
		}

		Color orgColor = new Color(sprite.getColor());

		final int notes = (type == HARD || type == EXHARD || type == HAZARD || type ==GrooveGauge.CLASS || type == EXCLASS || type == EXHARDCLASS)
						&& value > 0 && ((int) (value * parts / max)) < 1
						? 1 : (int) (value * parts / max);
		for (int i = 1; i <= parts; i++) {
			final float border = i * max / parts;
			sprite.draw(
					images[exgauge + (notes == i || notes - animation > i ? 0 : 2)
							+ (border < gauge.getBorder() ? 1 : 0)],
					gr.x + gr.width * (i - 1) / parts, gr.y, gr.width / parts, gr.height);

			if(animationType == ANIMATION_FLICKERING && images.length == 12 && i == notes) {
				float alpha = orgColor.a * ((time % duration) < duration / 2 ? (time % duration) / ((float) duration / 2 - 1) : ((duration - 1) - (time % duration)) / ((float) duration / 2 - 1));
				sprite.setColor(new Color(orgColor.r, orgColor.g, orgColor.b, alpha));
				sprite.draw(
						images[8 + exgauge / 2 + (border < gauge.getBorder() ? 1 : 0)],
						gr.x + gr.width * (i - 1) / parts, gr.y, gr.width / parts, gr.height);
				sprite.setColor(orgColor);
			}
		}
	}

	public int getAnimationType() {
		return animationType;
	}

	public void setAnimationType(int animationType) {
		this.animationType = animationType;
	}

	public int getAnimationRange() {
		return animationRange;
	}

	public void setAnimationRange(int animationRange) {
		this.animationRange = animationRange;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public int getParts() {
		return parts;
	}

	public void setParts(int parts) {
		this.parts = parts;
	}

	@Override
	public void dispose() {
		if (image != null) {
			image.dispose();
			image = null;
		}
	}
}
