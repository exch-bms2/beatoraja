package bms.player.beatoraja.play.gauge;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import bms.player.beatoraja.play.PlaySkin;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * プレイゲージの抽象クラス
 * 
 * @author exch
 */
public abstract class GrooveGauge {
	/**
	 * ゲージ量
	 */
	private float value;
	/**
	 * ゲージ最小値
	 */
	private float minValue;
	/**
	 * ゲージ最大値
	 */
	private float maxValue;
	/**
	 * クリアノルマ
	 */
	private float norm;

	private int cleartype;

	private float[] gauge;

	public static final int CLEARTYPE_NOPLAY = 0;
	public static final int CLEARTYPE_FAILED = 1;
	public static final int CLEARTYPE_ASSTST = 2;
	public static final int CLEARTYPE_LIGHT_ASSTST = 3;
	public static final int CLEARTYPE_EASY = 4;
	public static final int CLEARTYPE_NORMAL = 5;
	public static final int CLEARTYPE_HARD = 6;
	public static final int CLEARTYPE_EXHARD = 7;
	public static final int CLEARTYPE_FULLCOMBO = 8;
	public static final int CLEARTYPE_PERFECT = 9;
	public static final int CLEARTYPE_MAX = 10;

	public GrooveGauge() {
		
	}
	
	public GrooveGauge(float minValue, float maxValue, float startValue, float norm, int cleartype, float[] gauge) {
		init(minValue, maxValue, startValue, norm, cleartype, gauge);
	}

	protected void init(float minValue, float maxValue, float startValue, float norm, int cleartype, float[] gauge) {
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.value = startValue;
		this.norm = norm;
		this.cleartype = cleartype;
		this.gauge = gauge;
	}

	/**
	 * 判定に応じてゲージを更新する
	 * 
	 * @param judge
	 */
	public void update(int judge) {
		this.update(judge, 1);
	}

	/**
	 * 判定に応じてゲージを更新する
	 * 
	 * @param judge
	 */
	public void update(int judge, float rate) {
		this.setValue(this.getValue() + this.getGaugeValue(judge) * rate);
	}

	public void addValue(float value) {
		this.setValue(this.getValue() + value);
	}

	protected float getGaugeValue(int judge) {
		return gauge[judge];
	}

	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		if (value > maxValue) {
			this.value = maxValue;
		} else if (value < minValue) {
			this.value = minValue;
		} else {
			this.value = value;
		}
	}
	
	public boolean isQualified() {
		return value >= norm;
	}

	public int getClearType() {
		return cleartype;
	}

	public float getMaxValue() {
		return maxValue;
	}

	public float getMinValue() {
		return minValue;
	}

	public float getBorder() {
		return norm;
	}

}
