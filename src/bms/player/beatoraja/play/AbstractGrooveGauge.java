package bms.player.beatoraja.play;

import bms.model.BMSModel;
import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.play.GaugeProperty.GaugeElementProperty;
import com.badlogic.gdx.math.MathUtils;

/** Common gauge bank and read API for play, skins and results. */
public abstract class AbstractGrooveGauge {

	public static final int ASSISTEASY = 0;
	public static final int EASY = 1;
	public static final int NORMAL = 2;
	public static final int HARD = 3;
	public static final int EXHARD = 4;
	public static final int HAZARD = 5;
	public static final int CLASS = 6;
	public static final int EXCLASS = 7;
	public static final int EXHARDCLASS = 8;

	private final int typeorg;
	private volatile int type;
	private final Gauge[] gauges;

	AbstractGrooveGauge(int type, Gauge[] gauges) {
		this.typeorg = this.type = type;
		this.gauges = gauges;
	}

	static AbstractGrooveGauge create(BMSModel model, int type, PlayerResource resource, BMSPlayerRule rule) {
		return rule == BMSPlayerRule.LunaticRave2
				? new LunaticRave2GrooveGauge(model, type, resource)
				: GrooveGauge.create(model, type, resource);
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
		for(Gauge gauge : gauges) {
			gauge.update(judge, rate);
		}
	}

	public void addValue(float value) {
		for(Gauge gauge : gauges) {
			gauge.setValue(gauge.getValue() + value);
		}
	}

	final void applyMine(double damage) {
		for (Gauge gauge : gauges) gauge.applyMine(damage);
	}

	public float getValue() {
		return gauges[type].getValue();
	}

	public float getValue(int type) {
		return gauges[type].getValue();
	}

	public void setValue(float value) {
		for(Gauge gauge : gauges) {
			gauge.setValue(value);
		}
	}

	public void setValue(int type, float value) {
		gauges[type].setValue(value);
	}

	public boolean isQualified() {
		return gauges[type].isQualified();
	}

	public boolean isFailed() {
		return gauges[type].isFailed();
	}

	public int getType() {
		return  type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public boolean isTypeChanged() {
		return typeorg != type;
	}

	public boolean isCourseGauge() {
		return type >= CLASS && type <= EXHARDCLASS;
	}

	public int getGaugeTypeLength() {
		return gauges.length;
	}

	public ClearType getClearType() {
		return gauges[type].cleartype;
	}

	public Gauge getGauge() {
		return gauges[type];
	}

	public Gauge getGauge(int type) {
		return gauges[type];
	}

	public abstract static class Gauge {
		volatile double value;
		final GaugeElementProperty element;
		private final ClearType cleartype;

		Gauge(GaugeElementProperty element, ClearType cleartype) {
			this.element = element;
			this.value = element.init;
			this.cleartype = cleartype;
		}

		public final float getValue() {
			return (float) value;
		}

		public final void setValue(float value) {
			if (this.value > 0f) setClampedValue(value);
		}

		final void setClampedValue(double value) {
			this.value = MathUtils.clamp(value, element.min, element.max);
		}

		public abstract void update(int judge, float rate);

		void applyMine(double damage) {
			setValue(getValue() - (float) damage);
		}

		public final GaugeElementProperty getProperty() {
			return element;
		}

		public final boolean isQualified() {
			return !isFailed() && value >= element.border;
		}

		boolean isFailed() {
			return value == 0;
		}

		public final boolean isMax() {
			return value == element.max;
		}
	}
}
