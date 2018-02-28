package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

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

    private boolean editable;

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

    public abstract void prepareFont(String text);

    protected abstract void prepareText(String text);

    public void draw(SkinObjectRenderer sprite, long time, MainState state) {
        if(id == -1) {
        	return;
        }
       final String  value = state.getTextValue(id);
        if(value == null || value.length() == 0) {
            return;
        }        	
        Rectangle r = this.getDestination(time,state);
        if(r != null) {
            if(value != text) {
                setText(value);
            }
            draw(sprite, time, state, 0,0);
        }
    }

    public abstract void draw(SkinObjectRenderer sprite, long time, MainState state, int offsetX, int offsetY);
    
	public void setReferenceID(int id) {
		this.id = id;
	}
	
	public int getReferenceID() {
		return id;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }
}
