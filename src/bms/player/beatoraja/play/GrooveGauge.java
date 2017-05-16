package bms.player.beatoraja.play;

import bms.model.Mode;
import bms.player.beatoraja.ClearType;

import bms.model.BMSModel;

/**
 * プレイゲージの抽象クラス
 * 
 * @author exch
 */
public class GrooveGauge {

	private int type = -1;
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

	private float[][] guts = new float[0][2];

	public static final int ASSISTEASY = 0;
	public static final int EASY = 1;
	public static final int NORMAL = 2;
	public static final int HARD = 3;
	public static final int EXHARD = 4;
	public static final int HAZARD = 5;
	public static final int CLASS = 6;
	public static final int EXCLASS = 7;
	public static final int EXHARDCLASS = 8;

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

	public GrooveGauge(BMSModel model, int type, GaugeProperty propertyset) {
		this.type = type;
		GaugeProperty.GaugeElementProperty property = propertyset.values[type];
		this.minValue = property.min;
		this.maxValue = property.max;
		this.value = property.init;
		this.norm = property.border;
		this.cleartype = ClearType.getClearTypeByGauge(type).id;
		this.gauge = property.value.clone();
		switch(property.type) {
			case 0:
				for(int i = 0;i < gauge.length;i++) {
					if(gauge[i] > 0) {
						gauge[i] *= (model.getTotal() / model.getTotalNotes());
					}
				}
				break;
			case 1:
				break;
			case 2:
				final float pg = (float) Math.max(Math.min(0.15f, (2.5 * model.getTotal() - 250) / model.getTotalNotes()), 0);
				for(int i = 0;i < gauge.length;i++) {
					if(gauge[i] > 0) {
						gauge[i] *= pg / 0.15f;
					}
				}
				break;
		}
		this.guts = property.guts;
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
		float inc = this.getGaugeValue(judge) * rate;
		if(inc < 0) {
			for(float[] gut : guts) {
				if(value < gut[0]) {
					inc *= gut[1];
					break;
				}
			}
		}
		this.setValue(value + inc);
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

	public int getType() {
		return  type;
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
		Mode mode = model.getMode();
		for(BMSPlayerRule rule : BMSPlayerRule.values()) {
			if(rule.name().equals(mode.name())) {
				return new GrooveGauge(model, type, rule.gauge);
			}
		}
		return new GrooveGauge(model, type, BMSPlayerRule.Default.gauge);
	}
	
	public static int getGaugeID(GrooveGauge gauge) {
		return gauge.type;
	}
}
