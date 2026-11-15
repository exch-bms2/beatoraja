package bms.player.beatoraja.play;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.play.GaugeProperty.GaugeElementProperty;
import com.badlogic.gdx.utils.FloatArray;

/** Standard beatoraja gauge bank, including course gauge profiles. */
public final class GrooveGauge extends AbstractGrooveGauge {

	public GrooveGauge(BMSModel model, int type, GaugeProperty property) {
		super(type, createGauges(model, property));
	}

	private static Gauge[] createGauges(BMSModel model, GaugeProperty property) {
		Gauge[] gauges = new Gauge[property.values.length];
		for (int i = 0; i < gauges.length; i++) {
			gauges[i] = standardGauge(model, property.values[i], ClearType.getClearTypeByGauge(i));
		}
		return gauges;
	}

	static Gauge standardGauge(BMSModel model, GaugeElementProperty element, ClearType clearType) {
		return new StandardGauge(model, element, clearType);
	}

	public static GrooveGauge create(BMSModel model, int type, PlayerResource resource) {
		int coursetype = 0;
		GaugeProperty gauges = null;
		if(resource.getCourseBMSModels() != null){
			coursetype = 1;
			for (CourseData.CourseDataConstraint i : resource.getConstraint()) {
				switch(i) {
				case GAUGE_5KEYS:
					gauges = GaugeProperty.FIVEKEYS;
					break;
				case GAUGE_7KEYS:
					gauges = GaugeProperty.SEVENKEYS;
					break;
				case GAUGE_9KEYS:
					gauges = GaugeProperty.PMS;
					break;
				case GAUGE_24KEYS:
					gauges = GaugeProperty.KEYBOARD;
					break;
				case GAUGE_LR2:
					gauges = GaugeProperty.LR2;
					break;
				default:
					break;
				}
			}
		}
		GrooveGauge gauge = create(model, type, coursetype, gauges);
		FloatArray[] f = resource.getGauge();
		if (f != null) {
			for(int i = 0; i < f.length; i++) {
				gauge.setValue(i, f[i].get(f[i].size - 1));
			}
		}
		return gauge;
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

	private static final class StandardGauge extends Gauge {
		private final float[] changes;

		StandardGauge(BMSModel model, GaugeElementProperty element, ClearType clearType) {
			super(element, clearType);
			changes = element.value.clone();
			if (element.modifier != null) {
				for (int i = 0; i < changes.length; i++) {
					changes[i] = element.modifier.modify(changes[i], model);
				}
			}
		}

		@Override
		public void update(int judge, float rate) {
			float change = changes[judge] * rate;
			if (change < 0) {
				for (float[] gut : element.guts) {
					if (value < gut[0]) {
						change *= gut[1];
						break;
					}
				}
			}
			setValue(getValue() + change);
		}
	}

	public interface GaugeModifier {
		
		/**
		 * 回復量にTOTALを使用
		 */
		public static final GaugeModifier TOTAL = (f, model) -> (f > 0 ? (float) (f * model.getTotal() / model.getTotalNotes()) : f);
		/**
		 * TOTAL値によって回復量に制限をかける
		 */
		public static final GaugeModifier LIMIT_INCREMENT = (f, model) -> {
			final float pg = (float) Math.max(Math.min(0.15f, (2 * model.getTotal() - 320) / model.getTotalNotes()), 0);
			if(f > 0) {
				f *= pg / 0.15f;
			}
			return f;
		};
		/**
		 * TOTAL値、総ノート数によってダメージ量を増加させる
		 */
		public static final GaugeModifier MODIFY_DAMAGE = (f, model) -> {			
			if(f < 0) {
				float fix2=1.0f;
				final double[] fix1total = {240.0, 230.0, 210.0, 200.0, 180.0, 160.0, 150.0, 130.0, 120.0, 0};
				final float[] fix1table = {1.0f, 1.11f, 1.25f, 1.5f, 1.666f, 2.0f, 2.5f, 3.333f, 5.0f, 10.0f};
				int i = 0;
				for(;i < fix1total.length - 1 && model.getTotal() < fix1total[i];i++);
				int note=1000;
				float mod=0.002f;
				while(note>model.getTotalNotes()||note>1){
					fix2 += mod * (float)(note - Math.max(model.getTotalNotes(), note/2));
					note/=2;
					mod*=2.0f;
				}
				f *= Math.max(fix1table[i], fix2);
			}
			return f;
		};
		
		public abstract float modify(float f, BMSModel model);		
	}
}
