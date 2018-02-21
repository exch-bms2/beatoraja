package bms.player.beatoraja.play;

import java.util.*;

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
	private float[] value;
	/**
	 * ゲージの仕様
	 */
	private GaugeProperty property;
	/**
	 * ゲージのクリアタイプ
	 */
	private ClearType cleartype;

	private float[][] gauge;

	private boolean[] stopUpdate;

	public GrooveGauge(BMSModel model, int type, GaugeProperty propertyset) {
		this.type = type;
		property = propertyset;
		this.value = new float[property.values.length];
		for(int i = 0; i < property.values.length; i++) {
			this.value[i] = property.values[i].init;
		}
		this.cleartype = ClearType.getClearTypeByGauge(type);
		this.gauge = new float[property.values.length][];
		for(int j = 0; j < property.values.length; j++) {
			this.gauge[j] = property.values[j].value.clone();
			if(property.values[j].type != null) {
				for(int i = 0;i < gauge[j].length;i++) {
					gauge[j][i] = property.values[j].type.modify(gauge[j][i], model);
				}
			}
		}
		this.stopUpdate = new boolean[property.values.length];
		Arrays.fill(stopUpdate, false);
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
		for(int i = 0; i < property.values.length; i++) {
			float inc = this.getGaugeValue(i, judge) * rate;
			if(inc < 0) {
				for(float[] gut : property.values[i].guts) {
					if(value[i] < gut[0]) {
						inc *= gut[1];
						break;
					}
				}
			}
			if(!stopUpdate[i]) this.setValue(i, value[i] + inc);
		}
	}

	public void addValue(float value) {
		for(int type = 0; type < property.values.length; type++) {
			this.setValue(type, getValue(type) + value);
		}
	}

	protected float getGaugeValue(int type, int judge) {
		return gauge[type][judge];
	}

	public float getValue() {
		return value[type];
	}

	public float getValue(int type) {
		return value[type];
	}

	public void setValue(float value) {
		for(int type = 0; type < property.values.length; type++) {
			if (value > property.values[type].max) {
				this.value[type] = property.values[type].max;
			} else if (value < property.values[type].min) {
				this.value[type] = property.values[type].min;
			} else {
				this.value[type] = value;
			}
			if((this.value[type] == 0) &&(type == HARD || type == EXHARD || type == HAZARD || type == CLASS || type == EXCLASS || type == EXHARDCLASS)) {
				stopUpdate[type] = true;
			}
		}
	}

	public void setValue(int type, float value) {
		if (value > property.values[type].max) {
			this.value[type] = property.values[type].max;
		} else if (value < property.values[type].min) {
			this.value[type] = property.values[type].min;
		} else {
			this.value[type] = value;
		}
		if((this.value[type] == 0) &&(type == HARD || type == EXHARD || type == HAZARD || type == CLASS || type == EXCLASS || type == EXHARDCLASS)) {
			stopUpdate[type] = true;
		}
	}

	public boolean isQualified() {
		return value[type] >= property.values[type].border;
	}

	public int getType() {
		return  type;
	}

	public void downType() {
		type = type > 0 ? type - 1 : 0;
		cleartype = ClearType.getClearTypeByGauge(type);
		return;
	}

	public int changeTypeOfClear(int type) {
		if(type >= ASSISTEASY && type <= EXHARD) {
			for(int i = ASSISTEASY; i <= EXHARD; i++) {
				if(value[i] >= property.values[i].border && value[i] != 0) {
					this.type = i;
					cleartype = ClearType.getClearTypeByGauge(this.type);
				}
			}
		}
		return (this.type - type);
	}

	public void setStopUpdate(int type, boolean stopUpdate) {
		this.stopUpdate[type] = stopUpdate;
	}

	public int getGaugeTypeLength() {
		return property.values.length;
	}

	public ClearType getClearType() {
		return cleartype;
	}

	public float getMaxValue() {
		return property.values[type].max;
	}

	public float getMaxValue(int type) {
		return property.values[type].max;
	}

	public float getMinValue() {
		return property.values[type].min;
	}

	public float getMinValue(int type) {
		return property.values[type].min;
	}

	public float getBorder() {
		return property.values[type].border;
	}

	public float getBorder(int type) {
		return property.values[type].border;
	}

	public static GrooveGauge create(BMSModel model, int type, int grade, GaugeProperty gauge) {
		int id = -1;
		if (grade > 0) {
			// 段位ゲージ
			id = type <= 2 ? 6 : (type == 3 ? 7 : 8);
		} else {
			id = type;
		}
		if(id >= 0) {
			if(gauge == null) {
				Mode mode = model.getMode();
				gauge = BMSPlayerRule.getBMSPlayerRule(mode).gauge;
			}
			if(gauge != null) {
				return create(model, id, gauge);
			}
		}
		return null;
	}

	public static GrooveGauge create(BMSModel model, int id, GaugeProperty gauge) {
		return new GrooveGauge(model, id, gauge);
	}

	public static int getGaugeID(GrooveGauge gauge) {
		return gauge.type;
	}

	public enum GaugeType {
		/**
		 * 回復量にTOTALを使用
		 */
		TOTAL {
			@Override
			public float modify(float f, BMSModel model) {
				if(f > 0) {
					return (float) (f * model.getTotal() / model.getTotalNotes());
				}
				return f;
			}
		},
		/**
		 * TOTAL値によって回復量に制限をかける
		 */
		LIMIT_INCREMENT {
			@Override
			public float modify(float f, BMSModel model) {
				// TODO EXHARD依存の計算式なので後で汎用化する
				final float pg = (float) Math.max(Math.min(0.15f, (2.5 * model.getTotal() - 250) / model.getTotalNotes()), 0);
				if(f > 0) {
					f *= pg / 0.15f;
				}
				return f;
			}
		},
		/**
		 * TOTAL値、総ノート数によってダメージ量を増加させる
		 */
		MODIFY_DAMAGE {
			@Override
			public float modify(float f, BMSModel model) {
				float fix1=1.0f;
				float fix2=1.0f;
				if(f < 0) {
					if(model.getTotal()>=240.0){
						fix1=1.0f;
					}else if(model.getTotal()>=230.0){
						fix1=1.11f;
					}else if(model.getTotal()>=210.0){
						fix1=1.25f;
					}else if(model.getTotal()>=200.0){
						fix1=1.5f;
					}else if(model.getTotal()>=180.0){
						fix1=1.666f;
					}else if(model.getTotal()>=160.0){
						fix1=2.0f;
					}else if(model.getTotal()>=150.0){
						fix1=2.5f;
					}else if(model.getTotal()>=130.0){
						fix1=33.33f;
					}else if(model.getTotal()>=120.0){
						fix1=5.0f;
					}else{
						fix1=10.0f;
					}
					int note=1000;
					float mod=0.002f;
					while(note>model.getTotalNotes()||note>1){
						fix2 += mod * (float)(note - Math.max(model.getTotalNotes(), note/2));
						note/=2;
						mod*=2.0f;
					}
					f *= Math.max(fix1, fix2);
				}
				return f;
			}
		},
		;

		public abstract float modify(float f, BMSModel model);
	}
}
