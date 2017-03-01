package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

import static bms.player.beatoraja.skin.SkinProperty.NUMBER_SCRATCHANGLE_1P;
import static bms.player.beatoraja.skin.SkinProperty.NUMBER_SCRATCHANGLE_2P;

/**
 * テキストオブジェクト
 *
 * @author exch
 */
public abstract class SkinText extends SkinObject {

    private int align = ALIGN_LEFT;
	public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_CENTER = 1;
    public static final int ALIGN_RIGHT = 2;
    
    public static final int[] ALIGN = {Align.left, Align.center, Align.right};

    private int id = -1;
    
    private String text;
    
    public int getAlign() {
		return align;
	}

    public void setAlign(int align) {
        this.align = align;
    }
    
    public String getText() {
    	return text;
    }

    public void setText(String text) {
        if(text == null || text.length() == 0) {
            text = " ";
        }
    	this.text = text;
        prepareText(text);
    }
    
    protected abstract void prepareText(String text);

    public void draw(SpriteBatch sprite, long time, MainState state) {
        if(id == -1) {
        	return;
        }
       final String  value = state.getTextValue(id);
        if(value == null || value.length() == 0) {
            return;
        }        	
        Rectangle r = this.getDestination(time,state);
        if(r != null) {
            if(!value.equals(text)) {
                setText(value);
            }
            draw(sprite, time, state, 0,0);
        }
    }

    public abstract void draw(SpriteBatch sprite, long time, MainState state, int offsetX, int offsetY);
    
	public void setReferenceID(int id) {
		this.id = id;
	}
	
	public int getReferenceID() {
		return id;
	}
}
