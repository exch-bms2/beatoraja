package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.play.gauge.*;
import bms.player.beatoraja.skin.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * ゲージオブジェクト
 *
 * @author exch
 */
public class SkinGauge extends SkinObject {

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
	
	private int animation;
	private long atime;

	public SkinGauge(TextureRegion[][] image, int timer, int cycle) {
		this.image = new SkinSourceImage(image, timer, cycle);
	}

	@Override
	public void draw(SpriteBatch sprite, long time, MainState state) {
		final Rectangle gr = getDestination(time, state);
		final GrooveGauge gauge = ((BMSPlayer) state).getGauge();
		if (gauge == null || gr == null) {
			return;
		}

		if (atime < time) {
			animation = (int) (Math.random() * (animationRange + 1));
			atime = time + duration;
		}
		final float max = gauge.getMaxValue();
		final float value = gauge.getValue();
		final TextureRegion[] images = image.getImages(time, state);

		int count = max > 100 ? 24 : 50;
		int exgauge = 0;
		if (gauge instanceof AssistEasyGrooveGauge || gauge instanceof EasyGrooveGauge
				|| gauge instanceof ExhardGrooveGauge || gauge instanceof ExgradeGrooveGauge
				|| gauge instanceof ExhardGradeGrooveGauge || gauge instanceof HazardGrooveGauge) {
			exgauge = 4;
		}

		final int notes = (int) (value * count / max);
		for (int i = 1; i <= count; i++) {
			final float border = i * max / count;
			sprite.draw(
					images[exgauge + (notes == i || notes - animation > i ? 0 : 2)
							+ (border < gauge.getBorder() ? 1 : 0)],
					gr.x + gr.width * (i - 1) / count, gr.y, gr.width / count, gr.height);
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

	@Override
	public void dispose() {
		if (image != null) {
			image.dispose();
			image = null;
		}
	}
}
