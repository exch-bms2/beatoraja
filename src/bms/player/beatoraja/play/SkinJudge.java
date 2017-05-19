package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;

/**
 * Created by exch on 2016/12/11.
 */
public class SkinJudge extends SkinObject {

    private SkinImage[] judge;
    private SkinNumber[] count;
    private int index;
    private boolean shift;

    private BitmapFont font;

    public SkinJudge(int index, boolean shift) {
        this(new SkinImage[6], new SkinNumber[6], index, shift);
    }

    public SkinJudge(SkinImage[] judge, SkinNumber[] count, int index, boolean shift) {
        this.judge = judge;
        this.count = count;
        this.index = index;
        this.shift = shift;
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
                Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 18;
        font = generator.generateFont(parameter);
        generator.dispose();
        
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
                final Rectangle nr = count[judgenow].getDestination(time, state);
                if (nr != null) {
                    TextureRegion[] ntr = count[judgenow].getValue(time, judgecombo, 0, state);
                    int index = 0;
                    int length = 0;
                    for (; index < ntr.length && ntr[index] == null; index++)
                        ;
                    for (int i = 0; i < ntr.length; i++) {
                        if (ntr[i] != null) {
                            length++;
                        }
                    }
                    shift = (int) (length * nr.width / 2);
                    // コンボカウント描画
                    for (int i = index; i < index + length; i++) {
                        if (ntr[i] != null) {
                            sprite.draw(ntr[i], r.x + nr.x + (i - index) * nr.width - shift, r.y + nr.y,
                                    nr.width, nr.height);
                        }
                    }
                }
            }
            judge[judgenow].draw(sprite, time, state, this.shift ? -shift : 0, 0);

        }
    }

    @Override
    public void dispose() {
    	disposeAll(judge);
    	disposeAll(count);
        font.dispose();
    }
}
