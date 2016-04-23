package bms.player.beatoraja.skin;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;

/**
 * テキストオブジェクト
 *
 * @author exch
 */
public class SkinText extends SkinObject {
    /**
     * ビットマップフォント
     */
    private BitmapFont font;

    private int cycle;

    private FreeTypeFontGenerator generator;
    private FreeTypeFontGenerator.FreeTypeFontParameter parameter;

    public SkinText(String fontpath, int cycle, int size) {
        generator = new FreeTypeFontGenerator(Gdx.files.internal(fontpath));
        parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        this.cycle = cycle;
        parameter.size = size;
    }

    public void setText(String text) {
        if(text == null || text.length() == 0) {
            text = " ";
        }
        parameter.characters = text;
        font = generator.generateFont(parameter);
    }

    public void draw(SpriteBatch sprite, long time) {
        Rectangle r = this.getDestination(time);
        if(r != null && font != null) {
            font.setColor(getColor(time));
            font.draw(sprite, parameter.characters, r.x, r.y);
        }
    }
}
