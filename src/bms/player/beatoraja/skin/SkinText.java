package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.property.StringProperty;
import bms.player.beatoraja.skin.property.StringPropertyFactory;

import com.badlogic.gdx.graphics.Color;
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
    private String constantText;

    private boolean editable;

    private boolean wrapping;
    private int overflow;
    private Color outlineColor;
    private float outlineWidth;
    private Color shadowColor;
    private Vector2 shadowOffset;
    private float shadowSmoothness;
    
    private String currentText;

    public SkinText(int id) {
    	ref = StringPropertyFactory.getStringProperty(id);
    }

    public SkinText(StringProperty property) {
        ref = property;
    }

    public final int getAlign() {
		return align;
	}

    public final void setAlign(int align) {
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

    public void setConstantText(String constantText) {
        this.constantText = constantText;
    }

    public abstract void prepareFont(String text);

    protected abstract void prepareText(String text);
    
    public void prepare(long time, MainState state) {
    	super.prepare(time, state);
        currentText = ref != null ? ref.get(state) : (constantText != null ? constantText : null);
        if(currentText == null || currentText.length() == 0) {
        	draw = false;
            return;
        }
    }

    public void draw(SkinObjectRenderer sprite) {
        if(!currentText.equals(text)) {
            setText(currentText);
        }
        draw(sprite, 0,0);
    }

    public abstract void draw(SkinObjectRenderer sprite, float offsetX, float offsetY);
    
    public final boolean isEditable() {
        return editable;
    }

    public final void setEditable(boolean editable) {
        this.editable = editable;
    }

    public final boolean isWrapping() {
        return wrapping;
    }

    public final void setWrapping(boolean value) {
        wrapping = value;
    }

    public final int getOverflow() {
        return overflow;
    }

    public final void setOverflow(int value) {
        overflow = value;
    }

    public final Color getOutlineColor() {
        return outlineColor;
    }

    public final void setOutlineColor(Color color) {
        outlineColor = color;
    }

    public final float getOutlineWidth() {
        return outlineWidth;
    }

    public final void setOutlineWidth(float value) {
        outlineWidth = value;
    }

    public final Color getShadowColor() {
        return shadowColor;
    }

    public final void setShadowColor(Color color) {
        shadowColor = color;
    }

    public final Vector2 getShadowOffset() {
        return shadowOffset;
    }

    public final void setShadowOffset(Vector2 offset) {
        shadowOffset = offset;
    }

    public final float getShadowSmoothness() {
        return shadowSmoothness;
    }

    public final void setShadowSmoothness(float value) {
        shadowSmoothness = value;
    }
}
