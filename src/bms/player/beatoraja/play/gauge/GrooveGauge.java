package bms.player.beatoraja.play.gauge;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import bms.model.BMSModel;
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

	public static final int GAUGETYPE_ASSISTEASY = 0;
	public static final int GAUGETYPE_EASY = 1;
	public static final int GAUGETYPE_NORMAL = 2;
	public static final int GAUGETYPE_HARD = 3;
	public static final int GAUGETYPE_EXHARD = 4;
	public static final int GAUGETYPE_HAZARD = 5;
	public static final int GAUGETYPE_GRADE = 6;
	public static final int GAUGETYPE_EXGRADE = 7;
	public static final int GAUGETYPE_EXHARDGRADE = 8;

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

	public static GrooveGauge create(BMSModel model, int type, boolean grade) {
		int id = -1;
		if (grade) {	
			id = type <= 2 ? 6 : (type == 3 ? 7 : 8); 
			// 段位ゲージ
		} else {
			id = type;
		}
		if(id >= 0) {
			return create(model, id);
		}
		return null;
	}
	
	public static GrooveGauge create(BMSModel model, int type) {
		switch (type) {
		case GAUGETYPE_ASSISTEASY:
			return new AssistEasyGrooveGauge(model);
		case GAUGETYPE_EASY:
			return new EasyGrooveGauge(model);
		case GAUGETYPE_NORMAL:
			return new NormalGrooveGauge(model);
		case GAUGETYPE_HARD:
			return new HardGrooveGauge(model);
		case GAUGETYPE_EXHARD:
			return new ExhardGrooveGauge(model);
		case GAUGETYPE_HAZARD:
			return new HazardGrooveGauge(model);
		case GAUGETYPE_GRADE:
			return new GradeGrooveGauge(model);
		case GAUGETYPE_EXGRADE:
			return new ExgradeGrooveGauge(model);
		case GAUGETYPE_EXHARDGRADE:
			return new ExhardGradeGrooveGauge(model);
		}		
		return null;
	}
	
	public static int getGaugeID(GrooveGauge gauge) {
		if(gauge instanceof AssistEasyGrooveGauge) {
			return GAUGETYPE_ASSISTEASY;
		}
		if(gauge instanceof EasyGrooveGauge) {
			return GAUGETYPE_EASY;
		}
		if(gauge instanceof NormalGrooveGauge) {
			return GAUGETYPE_NORMAL;
		}
		if(gauge instanceof HardGrooveGauge) {
			return GAUGETYPE_HARD;
		}
		if(gauge instanceof ExhardGrooveGauge) {
			return GAUGETYPE_EXHARD;
		}
		if(gauge instanceof HazardGrooveGauge) {
			return GAUGETYPE_HAZARD;
		}
		if(gauge instanceof GradeGrooveGauge) {
			return GAUGETYPE_GRADE;
		}
		if(gauge instanceof ExgradeGrooveGauge) {
			return GAUGETYPE_EXGRADE;
		}
		if(gauge instanceof ExhardGradeGrooveGauge) {
			return GAUGETYPE_EXHARDGRADE;
		}
		return -1;
	}
}
