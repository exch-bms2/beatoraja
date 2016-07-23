package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
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

    private GlyphLayout layout;

    private int cycle;

    private int shadow = 0;

    private int align = ALIGN_LEFT;
    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_CENTER = 1;
    public static final int ALIGN_RIGHT = 2;

    private FreeTypeFontGenerator generator;
    private FreeTypeFontGenerator.FreeTypeFontParameter parameter;

    private TextResourceAccessor resource;

    public SkinText(String fontpath, int cycle, int size) {
        this(fontpath, cycle, size, 0);
    }

    public SkinText(String fontpath, int cycle, int size, int shadow) {
        generator = new FreeTypeFontGenerator(Gdx.files.internal(fontpath));
        parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        this.cycle = cycle;
        parameter.size = size;
        this.shadow = shadow;
    }

    public void setTextResourceAccessor(TextResourceAccessor resource) {
        this.resource = resource;
    }

    public void setAlign(int align) {
        this.align = align;
    }

    public void setText(String text) {
        if(text == null || text.length() == 0) {
            text = " ";
        }
        parameter.characters = text;
        font = generator.generateFont(parameter);
    }

    public void draw(SpriteBatch sprite, long time, MainState state) {
        if(resource == null) {
            return;
        }
        final String value = resource.getValue(state);
        if(value == null || value.length() == 0) {
            return;
        }
        Rectangle r = this.getDestination(time);
        if(r != null) {
            if(value != parameter.characters) {
                parameter.characters = value;
                if(font != null) {
                    font.dispose();                	
                }
                font = generator.generateFont(parameter);
                layout = new GlyphLayout(font, value);
            }
            if(font != null) {
                final float x = r.x - (align == ALIGN_CENTER ? layout.width / 2 : (align == ALIGN_RIGHT ? layout.width : 0));
                Color c = getColor(time);
                if(shadow > 0) {
                    font.setColor(new Color(c.r / 4, c.g / 4, c.b / 4, c.a));
                    font.draw(sprite, parameter.characters, x + shadow, r.y - shadow);
                }
                font.setColor(getColor(time));
                font.draw(sprite, parameter.characters, x, r.y);
            }
        }
    }
}
