package bms.player.beatoraja.gauge;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import bms.player.beatoraja.play.PlaySkin;

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
	
	public GrooveGauge(float minValue, float maxValue, float startValue, float norm, int cleartype) {
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.value = startValue;
		this.norm = norm;
		this.cleartype = cleartype;
	}

	/**
	 * 判定に応じてゲージを更新する
	 * 
	 * @param judge
	 */
	public abstract void update(int judge);

	public void addValue(float value) {
		setValue(getValue() + value);
	}
	
	public abstract float getGaugeValue(int judge);
	
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
	
	public abstract void draw(PlaySkin skin, SpriteBatch sprite, float x, float y, float w, float h);

	public float getBorder() {
		return norm;
	}
}
