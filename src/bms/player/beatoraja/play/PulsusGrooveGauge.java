package bms.player.beatoraja.play;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.NormalNote;
import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.play.GaugeProperty.GaugeElementProperty;

/** Pulsus's three BMS gauges. Other beatoraja gauge choices keep their existing behavior. */
final class PulsusGrooveGauge extends AbstractGrooveGauge {
	PulsusGrooveGauge(BMSModel model, int type, BMSPlayerRule.ChartParameters source) {
		super(type, createGauges(model, source));
	}

	@Override
	void updateJudgment(int judge, long noteMinusInputUs) {
		// An early LN release is scored as a miss but can use the early POOR gauge penalty.
		int pulsusJudge = judge == 4 && noteMinusInputUs > PulsusNoteRules.LATE_LIMIT_US
				&& noteMinusInputUs < PulsusNoteRules.EARLY_LIMIT_US ? 5 : judge;
		for (int i = 0; i < getGaugeTypeLength(); i++) {
			var gauge = getGauge(i);
			gauge.update(gauge instanceof PulsusGauge ? pulsusJudge : judge, 1);
		}
	}

	private static Gauge[] createGauges(BMSModel model, BMSPlayerRule.ChartParameters source) {
		int count = 0;
		for (var timeline : model.getAllTimeLines()) {
			for (int lane = 0; lane < model.getMode().key; lane++) {
				var note = timeline.getNote(lane);
				if (note instanceof NormalNote || note instanceof LongNote ln && !ln.isEnd() && ln.getPair() != null) {
					count++;
				}
			}
		}
		// Unlike EX score, TOTAL recovery is divided by taps plus hold heads only.
		double total = source != null ? source.total() : model.getTotal();
		double defaultTotal = Math.max(260.0, 7.605 * count / (0.01 * count + 6.5));
		// In-game models have already had bmson TOTAL converted by BMSPlayerRule.validate.
		if (model.getTotalType() == BMSModel.TotalType.BMSON) {
			total = total > 0 ? defaultTotal * total / 100.0 : defaultTotal;
		}
		double gain = count == 0 ? 0 : (total > 0 ? total : defaultTotal) / count;
		var properties = GaugeProperty.SEVENKEYS.values;
		var gauges = new Gauge[properties.length];
		for (int i = 0; i < gauges.length; i++) {
			var clear = ClearType.getClearTypeByGauge(i);
			gauges[i] = switch (i) {
				case NORMAL -> new PulsusGauge(GaugeElementProperty.PULSUS_DEFAULT, clear, gain);
				case EASY -> new PulsusGauge(GaugeElementProperty.PULSUS_EASY, clear, gain);
				case HARD -> new PulsusGauge(GaugeElementProperty.PULSUS_HARD, clear, 1);
				default -> GrooveGauge.standardGauge(model, properties[i], clear);
			};
		}
		return gauges;
	}

	private static final class PulsusGauge extends Gauge {
		private final double gain;

		private PulsusGauge(GaugeElementProperty element, ClearType clear, double gain) {
			super(element, clear);
			this.gain = gain;
		}

		@Override
		public void update(int judge, float rate) {
			double change = element.value[judge] * (judge < 3 ? gain : 1);
			setClampedValue(value + change * rate);
		}

		@Override
		boolean isFailed() {
			// Upstream does not implement instant failure. Only final qualification uses the border.
			return false;
		}
	}
}
