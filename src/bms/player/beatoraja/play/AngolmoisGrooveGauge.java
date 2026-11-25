package bms.player.beatoraja.play;

import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.PlayerConfig;

/** The single Angolmois gauge occupies the NORMAL slot in play skins. */
final class AngolmoisGrooveGauge extends AbstractGrooveGauge {
    AngolmoisGrooveGauge() { super(NORMAL, new Gauge[]{new Value(), new Value(), new Value()}); }
    void sync(AngolmoisEngine engine) {
        for (int i = 0; i < getGaugeTypeLength(); i++) {
            var gauge = (Value) getGauge(i);
            gauge.value = engine.gauge * 100.0 / 512;
            gauge.failed = engine.failed;
        }
    }
    @Override void selectAutoShiftGauge(PlayerConfig config, boolean bestClear) {}
    private static final class Value extends Gauge {
        boolean failed;
        Value() { super(GaugeProperty.GaugeElementProperty.ANGOLMOIS, ClearType.Normal); }
        @Override public void update(int judge, float rate) {}
        @Override boolean isFailed() { return failed; }
    }
}
