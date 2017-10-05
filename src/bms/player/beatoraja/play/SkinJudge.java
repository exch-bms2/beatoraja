package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.*;
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
    
    public boolean isSheft() {
    	return shift;
    }

    @Override
    public void draw(SpriteBatch sprite, long time, MainState state) {
        final int judgenow = ((BMSPlayer)state).getJudgeManager().getNowJudge()[index] - 1;
        final int judgecombo = ((BMSPlayer)state).getJudgeManager().getNowCombo()[index];

        if(judgenow < 0) {
            return;
        }
        final Rectangle r = judge[judgenow].getDestination(time, state);
        if (r != null) {
            int shift = 0;
            if (judgenow < 3) {
            	count[judgenow].draw(sprite, time, judgecombo, state, r.x, r.y);
            	shift = count[judgenow].getLength() / 2;
//                final Rectangle nr = count[judgenow].getDestination(time, state);
//                if (nr != null) {
//                    TextureRegion[] ntr = count[judgenow].getValue(time, judgecombo, 0, state);
//                    int index = 0;
//                    int length = 0;
//                    for (; index < ntr.length && ntr[index] == null; index++)
//                        ;
//                    for (int i = 0; i < ntr.length; i++) {
//                        if (ntr[i] != null) {
//                            length++;
//                        }
//                    }
//                    shift = (int) (length * nr.width / 2);
//                    // コンボカウント描画
//                    for (int i = index; i < index + length; i++) {
//                        if (ntr[i] != null) {
//                            sprite.draw(ntr[i], r.x + nr.x + (i - index) * nr.width - shift, r.y + nr.y,
//                                    nr.width, nr.height);
//                        }
//                    }
//                }
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
