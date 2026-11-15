package bms.player.beatoraja.play;

import bms.model.BMSModel;
import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.play.GaugeProperty.GaugeElementProperty;

/** LunaticRave2 single-song gauge rules. Unsupported gauges retain their standard calculation. */
public final class LunaticRave2GrooveGauge extends AbstractGrooveGauge {

	LunaticRave2GrooveGauge(BMSModel model, int type, PlayerResource resource) {
		super(type, createGauges(model, resource));
	}

	private static Gauge[] createGauges(BMSModel model, PlayerResource resource) {
		var rules = new LunaticRave2Rules(resource.getChartRuleParameters(), model.getTotalNotes());
		var properties = BMSPlayerRule.LunaticRave2.gauge.values;
		var gauges = new Gauge[properties.length];
		for (int i = 0; i < gauges.length; i++) {
			var clearType = ClearType.getClearTypeByGauge(i);
			double[] changes = rules.gaugeChanges(i, model.getTotalNotes());
			gauges[i] = changes != null
					? new LunaticRave2Gauge(properties[i], clearType, changes, i == HARD)
					: GrooveGauge.standardGauge(model, properties[i], clearType);
		}
		return gauges;
	}

	private static final class LunaticRave2Gauge extends Gauge {
		private final double[] changes;
		private final boolean hard;

		LunaticRave2Gauge(GaugeElementProperty element, ClearType clearType, double[] changes, boolean hard) {
			super(element, clearType);
			this.changes = changes;
			this.hard = hard;
		}

		@Override
		public void update(int judge, float rate) {
			double hp = value;
			if (hp < 2) {
				value = 0;
				return;
			}
			double change = changes[judge] * rate;
			// HARD's low-health correction uses the lower 2% step.
			if (hard && change < 0 && hp < 32) change *= 0.6;
			setClampedValue(hp + change);
		}

		@Override
		void applyMine(double damage) {
			// Mine damage bypasses the HARD low-health correction.
			setClampedValue(value - damage);
		}

		@Override
		boolean isFailed() {
			return hard ? value < 2 : super.isFailed();
		}
	}
}
