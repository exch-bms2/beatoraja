package bms.player.beatoraja.pattern;

import bms.model.BMSModel;
import bms.model.TimeLine;

import java.util.List;

/**
 * スクロールスピード変更に関するオプション
 *
 * @author exch
 */
public class ScrollSpeedModifier extends PatternModifier {

    private Mode mode = Mode.REMOVE;

    private double rate = 0.5;

    public ScrollSpeedModifier() {
    }

    public ScrollSpeedModifier(int mode) {
        this.mode = Mode.values()[mode];
    }

    @Override
    public List<PatternModifyLog> modify(BMSModel model) {
        if(mode == Mode.REMOVE) {
            // スクロールスピード変更、ストップシーケンス無効化
            AssistLevel assist = AssistLevel.NONE;
            TimeLine starttl = model.getAllTimeLines()[0];

            for (TimeLine tl : model.getAllTimeLines()) {
                if(tl.getBPM() != starttl.getBPM() || tl.getScroll() != starttl.getScroll() || tl.getStop() != 0) {
                    assist = AssistLevel.LIGHT_ASSIST;
                }
                tl.setSection(starttl.getBPM() * tl.getMicroTime() / 240000000);
                tl.setStop(0);
                tl.setBPM(starttl.getBPM());
                tl.setScroll(starttl.getScroll());
            }
            setAssistLevel(assist);
            return null;
        }

        final double base = model.getAllTimeLines()[0].getScroll();
        double current = base;
        for (TimeLine tl : model.getAllTimeLines()) {
            if(tl.getSectionLine()) {
                current = base * (1.0 + Math.random() * rate * 2 - rate);
            }
            tl.setScroll(current);
        }
        return null;
    }

    public enum Mode {
        REMOVE, ADD;
    }
}
