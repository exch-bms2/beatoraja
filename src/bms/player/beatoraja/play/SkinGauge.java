package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.play.GaugeProperty.GaugeElementProperty;
import bms.player.beatoraja.result.AbstractResult;
import bms.player.beatoraja.result.MusicResult;

import static bms.player.beatoraja.play.GrooveGauge.*;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.FloatArray;
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

	/**
	 * リザルト用 ゲージが0から最終値まで増える演出の開始時間(ms)
	 */
	private int starttime = 0;
	/**
	 * リザルト用 ゲージが0から最終値まで増える演出の終了時間(ms)
	 */
	private int endtime = 500;

	/**
	 * 7to9時にボーダーが丁度割り切れるゲージ粒数になっているかがチェック済みかどうか
	 */
	private boolean isCheckedSevenToNine = false;

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
		GrooveGauge gauge = null;
		if(state instanceof BMSPlayer) {
			gauge = ((BMSPlayer) state).getGauge();
		} else if(state instanceof AbstractResult) {
			gauge = ((AbstractResult) state).main.getPlayerResource().getGrooveGauge();
		}
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

		if(!isCheckedSevenToNine && BMSPlayerRule.isSevenToNine()) {
			//7to9 ボーダーが丁度割り切れるゲージ粒数に変更
			int setParts = parts;
			for(int type = 0; type < gauge.getGaugeTypeLength(); type++) {
				final GaugeElementProperty element = gauge.getGauge(type).getProperty();
				for(int i = parts; i <= element.max; i++) {
					if(element.border % (element.max / i) == 0) {
						setParts = Math.max(setParts, i);
						break;
					}
				}
			}
			parts = setParts;
			isCheckedSevenToNine = true;
		}

		float value = gauge.getValue();
		final int type = state instanceof AbstractResult ? ((AbstractResult) state).getGaugeType() : gauge.getType();
		final float max = gauge.getGauge(type).getProperty().max;

		if(state instanceof AbstractResult) {
			PlayerResource resource = ((AbstractResult) state).main.getPlayerResource();
			FloatArray gaugeTransition;
			if(state instanceof MusicResult) {
				gaugeTransition = resource.getGauge()[type];
			} else {
				gaugeTransition = resource.getCourseGauge().get(resource.getCourseGauge().size - 1)[type];
			}
			value = gaugeTransition.get(gaugeTransition.size - 1);
			if(time < starttime) {
				value = gauge.getGauge(type).getProperty().min;
			} else if(time >= starttime && time < endtime) {
				value = Math.min(value, Math.max(max * (time - starttime) / (endtime - starttime), gauge.getGauge(type).getProperty().min));
			}
		}

		final TextureRegion[] images = image.getImages(time, state);

		int exgauge = 0;
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
							+ (border < gauge.getGauge(type).getProperty().border ? 1 : 0)],
					gr.x + gr.width * (i - 1) / parts, gr.y, gr.width / parts, gr.height);

			if(animationType == ANIMATION_FLICKERING && images.length == 12 && i == notes) {
				float alpha = orgColor.a * ((time % duration) < duration / 2 ? (time % duration) / ((float) duration / 2 - 1) : ((duration - 1) - (time % duration)) / ((float) duration / 2 - 1));
				sprite.setColor(new Color(orgColor.r, orgColor.g, orgColor.b, alpha));
				sprite.draw(
						images[8 + exgauge / 2 + (border < gauge.getGauge(type).getProperty().border ? 1 : 0)],
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

	public void setStarttime(int starttime) {
		this.starttime = starttime;
	}

	public void setEndtime(int endtime) {
		this.endtime = endtime;
	}

	@Override
	public void dispose() {
		if (image != null) {
			image.dispose();
			image = null;
		}
	}
}
