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
import com.badlogic.gdx.utils.Disposable;

import java.io.UnsupportedEncodingException;

import static bms.player.beatoraja.skin.SkinProperty.NUMBER_SCRATCHANGLE_1P;
import static bms.player.beatoraja.skin.SkinProperty.NUMBER_SCRATCHANGLE_2P;

/**
 * テキストオブジェクト
 *
 * @author exch
 */
public class SkinTextImage extends SkinObject {

    private SkinTextImageSource source;

    private int align = ALIGN_LEFT;
    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_CENTER = 1;
    public static final int ALIGN_RIGHT = 2;

    public static final int[] ALIGN = {Align.left, Align.center, Align.right};

    private int id = -1;

    private String text;
    private String encodetext;

    public SkinTextImage(SkinTextImageSource source) {
        this.source = source;
    }

    public int getAlign() {
        return align;
    }

    public void setAlign(int align) {
        this.align = align;
    }

    public void setText(String text) {
        if(text == null || text.length() == 0) {
            text = " ";
        }
    }

    public void draw(SpriteBatch sprite, long time, MainState state) {
        if(id == -1) {
            return;
        }
        final String value = state.getTextValue(id);
        if(value == null || value.length() == 0) {
            return;
        }
        if(!value.equals(text)) {
            try {
                encodetext = new String(value.getBytes("Shift_JIS"), "Shift_JIS");
                text = value;
            } catch (UnsupportedEncodingException e) {
            }
        }
        Rectangle r = this.getDestination(time,state);
        if(r != null && encodetext != null) {
            Color c = getColor(time,state);
            final float x = (align == 2 ? r.x - r.width : (align == 1 ? r.x - r.width / 2 : r.x));
            drawText(sprite, encodetext, align, r ,c);
        }
    }

    public void draw(SpriteBatch sprite, long time, MainState state, String value, int offsetX, int offsetY) {
        Rectangle r = this.getDestination(time,state);
        if(r != null) {
            Color c = getColor(time,state);
            drawText(sprite, value, align, r, c);
        }
    }

    private void drawText(SpriteBatch sprite, String text, int align, Rectangle r, Color c) {
        float width = 0;
//        System.out.println("SkinTextImage描画:" + text + " - " + x + " " + y + " " + w + " " + h);
        for(int i = 0, len = text.length();i < len;i++) {
            final TextureRegion ch = source.getImages()[text.charAt(i)];
            if(ch != null) {
                width += ch.getRegionWidth() * r.height / source.getSize() + source.getMargin();
            }
        }
        final float scale = r.width < width ? r.width / width : 1;
        final float x = (align == 2 ? r.x - (r.width - width * scale) : (align == 1 ? r.x - width * scale / 2 : r.x));
        float dx = 0;
        for(int i = 0, len = text.length();i < len;i++) {
            final TextureRegion ch = source.getImages()[text.charAt(i)];
            if(ch != null) {
                final float tw = ch.getRegionWidth() * scale * r.height / source.getSize();
//                System.out.println("SkinTextImage描画:" + text.charAt(i) + " - " + (x + dx) + " " + y + " " + tw + " " + h);
                draw(sprite, ch, x + dx, r.y, tw, r.height, c, 0);
                dx += tw + source.getMargin() * scale;
            } else {
                System.out.println("undefined char : " + text.charAt(i));
            }
        }
    }

    public void dispose() {
        source.dispose();
    }

    public void setReferenceID(int id) {
        this.id = id;
    }


    public static class SkinTextImageSource implements Disposable{

        private int size = 0;
        private int margin = 0;
        private TextureRegion[] images = new TextureRegion[65536];

        public int getMargin() {
            return margin;
        }

        public void setMargin(int margin) {
            this.margin = margin;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public TextureRegion[] getImages() {
            return images;
        }

        @Override
        public void dispose() {
            for(int i = 0;i < images.length;i++) {
                if(images[i] != null) {
                    images[i].getTexture().dispose();
                    images[i] = null;
                }
            }
        }
    }
}
