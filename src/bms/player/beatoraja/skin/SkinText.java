package bms.player.beatoraja.skin;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * テキストオブジェクト
 *
 * @author exch
 */
public class SkinText extends AbstractSkinObject{

    private BitmapFont font;

    private int id;

    private int cycle;

    private FreeTypeFontGenerator generator;
    private FreeTypeFontGenerator.FreeTypeFontParameter parameter;

    public SkinText(String fontpath, int cycle, int size) {
        generator = new FreeTypeFontGenerator(Gdx.files.internal(fontpath));
        parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        this.cycle = cycle;
        parameter.size = size;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setText(String text) {
        parameter.characters = text;
        font = generator.generateFont(parameter);
    }

    public void draw(SpriteBatch sprite, long time) {
        Rectangle r = this.getDestination(time);
        font.draw(sprite, parameter.characters, r.x, r.y);
    }
}
