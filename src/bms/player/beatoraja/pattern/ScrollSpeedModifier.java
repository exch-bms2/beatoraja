package bms.player.beatoraja.pattern;

import bms.model.BMSModel;
import bms.model.TimeLine;

/**
 * スクロールスピード変更に関するオプション
 *
 * @author exch
 */
public class ScrollSpeedModifier extends PatternModifier {

    private Mode mode = Mode.REMOVE;

    /**
     * スクロールを変更する小節単位
     */
    private int section = 4;

    /**
     * 変更するスクロール幅
     */
    private double rate = 0.5;
    
    public ScrollSpeedModifier() {
    }

    public ScrollSpeedModifier(int mode, int section, double scrollrate) {
        this.mode = Mode.values()[mode];
        this.section = section;
        this.rate = scrollrate;
    }

    @Override
    public void modify(BMSModel model) {
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
        } else {
            final double base = model.getAllTimeLines()[0].getScroll();
            double current = base;
            int sectioncount = 0;
            for (TimeLine tl : model.getAllTimeLines()) {
                if(tl.getSectionLine()) {
                	sectioncount++;
                	if(section == sectioncount) {
                        current = base * (1.0 + Math.random() * rate * 2 - rate);
                        sectioncount = 0;
                	}
                }
                tl.setScroll(current);
            }
        }
    }

    public enum Mode {
        REMOVE, ADD;
    }
}
