package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

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
    
    public static final int[] ALIGN = {Align.left, Align.center, Align.right};

    private FreeTypeFontGenerator generator;
    private FreeTypeFontGenerator.FreeTypeFontParameter parameter;

    private int id = -1;
    
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
    	if(generator == null) {
    		return;
    	}
        
        if(id == -1) {
        	return;
        }
       final String  value = state.getTextValue(id);
        if(value == null || value.length() == 0) {
            return;
        }        	
        Rectangle r = this.getDestination(time,state);
        if(r != null) {
            if(!value.equals(parameter.characters)) {
                parameter.characters = value;
                if(font != null) {
                    font.dispose();                	
                }
                font = generator.generateFont(parameter);
                layout = new GlyphLayout(font, value);
            }
            if(font != null) {
                Color c = getColor(time,state);
                font.getData().setScale(r.height / parameter.size);
                final float x = (align == 2 ? r.x - r.width : (align == 1 ? r.x - r.width / 2 : r.x));
                if(shadow > 0) {
                    layout.setText(font, value, new Color(c.r / 2, c.g / 2, c.b / 2, c.a), r.getWidth(),ALIGN[align], false);
                    font.draw(sprite, layout, x + shadow, r.y - shadow);
                }                
                layout.setText(font, value, c, r.getWidth(),ALIGN[align], false);
                font.draw(sprite, layout, x, r.y);
            }
        }
    }

    public void dispose() {
        if(generator != null) {
        	generator.dispose();
        	generator = null;
        }
        if(font != null) {
            font.dispose();;
            font = null;
        }
    }

	public void setReferenceID(int id) {
		this.id = id;
	}
}
