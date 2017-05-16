package bms.player.beatoraja.play;

import bms.model.Mode;
import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.play.GaugeProperty.GaugeElementProperty;
import bms.model.BMSModel;

/**
 * プレイゲージの抽象クラス
 * 
 * @author exch
 */
public class GrooveGauge {

	public static final int ASSISTEASY = 0;
	public static final int EASY = 1;
	public static final int NORMAL = 2;
	public static final int HARD = 3;
	public static final int EXHARD = 4;
	public static final int HAZARD = 5;
	public static final int CLASS = 6;
	public static final int EXCLASS = 7;
	public static final int EXHARDCLASS = 8;

	private int type = -1;
	/**
	 * ゲージ量
	 */
	private float value;
	/**
	 * ゲージの仕様
	 */
	private GaugeElementProperty property;
	/**
	 * ゲージのクリアタイプ
	 */
	private ClearType cleartype;

	private float[] gauge;

	public GrooveGauge(BMSModel model, int type, GaugeProperty propertyset) {
		this.type = type;
		property = propertyset.values[type];
		this.value = property.init;
		this.cleartype = ClearType.getClearTypeByGauge(type);
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
			for(float[] gut : property.guts) {
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
		if (value > property.max) {
			this.value = property.max;
		} else if (value < property.min) {
			this.value = property.min;
		} else {
			this.value = value;
		}
	}
	
	public boolean isQualified() {
		return value >= property.border;
	}

	public int getType() {
		return  type;
	}

	public ClearType getClearType() {
		return cleartype;
	}

	public float getMaxValue() {
		return property.max;
	}

	public float getMinValue() {
		return property.min;
	}

	public float getBorder() {
		return property.border;
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
