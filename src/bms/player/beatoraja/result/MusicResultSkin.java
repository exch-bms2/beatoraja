package bms.player.beatoraja.result;

import bms.player.beatoraja.skin.Skin;
import com.badlogic.gdx.math.Rectangle;

/**
 * リサルトスキン
 */
public class MusicResultSkin extends Skin{

    private Rectangle gaugeregion;

    private Rectangle judgeregion;

    public MusicResultSkin() {
        gaugeregion = new Rectangle(20, 500, 400, 200);
        judgeregion = new Rectangle(500,500,700,200);
    }

    public Rectangle getGaugeRegion() {
        return gaugeregion;
    }

    public Rectangle getJudgeRegion() {
        return judgeregion;
    }

}
