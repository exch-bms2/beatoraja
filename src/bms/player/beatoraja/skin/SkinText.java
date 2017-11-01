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
    
    private String text = "";

    /**
     * 0 : Nearest neighbor
     * 1 : Linear filtering
     * SkinObject の dstfilter は private のでテキスト用の filter 変数です
     */
	protected int textfilter;
    
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
        if(text == null) {
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

    @Override
	public void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
			int blend, int filter, int angle, int center, int loop, int timer, int[] op) {
        textfilter = filter;
        super.setDestination(time, x, y, w, h, acc, a, r, g, b, blend, filter, angle, center, loop, timer, op);
    }
}
