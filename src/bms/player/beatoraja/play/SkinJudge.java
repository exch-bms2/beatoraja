package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Rectangle;

/**
 * 判定オブジェクト
 * 
 * @author exch
 */
public class SkinJudge extends SkinObject {

	/**
	 * 文字イメージ
	 */
    private SkinImage[] judge;
    /**
     * 数字イメージ
     */
    private SkinNumber[] count;
    private int index;
    private boolean shift;

    public SkinJudge(int index, boolean shift) {
        this(new SkinImage[6], new SkinNumber[6], index, shift);
    }

    public SkinJudge(SkinImage[] judge, SkinNumber[] count, int index, boolean shift) {
        this.judge = judge;
        this.count = count;
        this.index = index;
        this.shift = shift;
        
        this.setDestination(0, 0, 0, 0, 0, 0, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, new int[0]);
    }

    public void setJudge(SkinImage[] judge) {
        this.judge = judge;
    }

    public SkinImage[] getJudge() {
        return  judge;
    }

    public void setJudgeCount(SkinNumber[] count) {
        this.count = count;
    }

    public SkinNumber[] getJudgeCount() {
        return count;
    }
    
    public boolean isShift() {
    	return shift;
    }

    @Override
    public void draw(SkinObjectRenderer sprite, long time, MainState state) {
        int judgenow = ((BMSPlayer)state).getJudgeManager().getNowJudge()[index] - 1;
        final int judgecombo = ((BMSPlayer)state).getJudgeManager().getNowCombo()[index];
	final GrooveGauge gauge = ((BMSPlayer)state).getGauge();

        if(judgenow < 0) {
            return;
        }
        final Rectangle r = judge[judgenow].getDestination(time, state);
        if (r != null) {
            int shift = 0;
            if (judgenow < 3) {
		if(judgenow == 0 && judge.length >= 7 && gauge.getValue() == gauge.getMaxValue()) {
                    judgenow = 6;
            	}
            	count[judgenow].draw(sprite, time, judgecombo, state, r.x, r.y);
            	shift = count[judgenow].getLength() / 2;
            }
            judge[judgenow].draw(sprite, time, state, this.shift ? -shift : 0, 0);

        }
    }

    @Override
    public void dispose() {
    	disposeAll(judge);
    	disposeAll(count);
    }
}
