package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.property.StringProperty;
import bms.player.beatoraja.skin.property.StringPropertyFactory;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
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

    public static final int OVERFLOW_OVERFLOW = 0;
    public static final int OVERFLOW_SHRINK = 1;
    public static final int OVERFLOW_TRUNCATE = 2;
    
    public static final int[] ALIGN = {Align.left, Align.center, Align.right};

    private final StringProperty ref;
    
    private String text = "";

    private boolean editable;

    private boolean wrapping;
    private int overflow;
    private Color outlineColor;
    private float outlineWidth;
    private Color shadowColor;
    private Vector2 shadowOffset;
    private float shadowSmoothness;
    
    public SkinText(int id) {
    	ref = StringPropertyFactory.getStringProperty(id);
    }

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
       final String  value = ref != null ? ref.get(state) : null;
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
    
    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isWrapping() {
        return wrapping;
    }

    public void setWrapping(boolean value) {
        wrapping = value;
    }

    public int getOverflow() {
        return overflow;
    }

    public void setOverflow(int value) {
        overflow = value;
    }

    public Color getOutlineColor() {
        return outlineColor;
    }

    public void setOutlineColor(Color color) {
        outlineColor = color;
    }

    public float getOutlineWidth() {
        return outlineWidth;
    }

    public void setOutlineWidth(float value) {
        outlineWidth = value;
    }

    public Color getShadowColor() {
        return shadowColor;
    }

    public void setShadowColor(Color color) {
        shadowColor = color;
    }

    public Vector2 getShadowOffset() {
        return shadowOffset;
    }

    public void setShadowOffset(Vector2 offset) {
        shadowOffset = offset;
    }

    public float getShadowSmoothness() {
        return shadowSmoothness;
    }

    public void setShadowSmoothness(float value) {
        shadowSmoothness = value;
    }
}
