package bms.player.beatoraja.skin;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;

import org.lwjgl.opengl.GL11;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

public class SkinTextFont extends SkinText {

    /**
     * ビットマップフォント
     */
    private BitmapFont font;

    private GlyphLayout layout;

    private int shadow = 0;

    private FreeTypeFontGenerator generator;
    private FreeTypeFontGenerator.FreeTypeFontParameter parameter;
    private String preparedFonts;

    public SkinTextFont(String fontpath, int cycle, int size) {
        this(fontpath, cycle, size, 0);
    }

    public SkinTextFont(String fontpath, int cycle, int size, int shadow) {
        generator = new FreeTypeFontGenerator(Gdx.files.internal(fontpath));
        parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.characters = "";
//        this.setCycle(cycle);
        parameter.size = size;
        this.shadow = shadow;
    }

    public void prepareFont(String text) {
        if(font != null) {
            font.dispose();                	
        }
        preparedFonts = parameter.characters = text;        	
        font = generator.generateFont(parameter);
    }    

	@Override
	protected void prepareText(String text) {
        if(preparedFonts == null) {
            if(font != null) {
                font.dispose();                	
            }
            parameter.characters = text;        	
            font = generator.generateFont(parameter);
        }
        layout = new GlyphLayout(font, text);        	    	
	}
	
	@Override
    public void draw(SkinObjectRenderer sprite, long time, MainState state, int offsetX, int offsetY) {
        if(generator == null) {
            return;
        }
        Rectangle r = this.getDestination(time,state);
        if(r != null) {
            if(font != null) {
                Color c = getColor();
                font.getData().setScale(r.height / parameter.size);
                
                sprite.setType(textfilter != 0 ? SkinObjectRenderer.TYPE_LINEAR : SkinObjectRenderer.TYPE_NORMAL);

                final float x = (getAlign() == 2 ? r.x - r.width : (getAlign() == 1 ? r.x - r.width / 2 : r.x));
                if(shadow > 0) {
                    layout.setText(font, getText(), new Color(c.r / 2, c.g / 2, c.b / 2, c.a), r.getWidth(),ALIGN[getAlign()], false);
                    sprite.draw(font, layout, x + shadow + offsetX, r.y - shadow + offsetY + r.getHeight());
                }
                layout.setText(font, getText(), c, r.getWidth(),ALIGN[getAlign()], false);

                sprite.draw(font, layout, x + offsetX, r.y + offsetY + r.getHeight());
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
}
