package bms.player.beatoraja.select;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

/**
 * 楽曲バー描画用スキンオブジェクト
 */
public class SkinBar extends SkinObject {

    /**
     * 選択時のBarのSkinImage
     */
    private SkinImage[] barimageon = new SkinImage[60];
    /**
     * 非選択時のBarのSkinImage
     */
    private SkinImage[] barimageoff = new SkinImage[60];
    /**
     * トロフィーのSkinImage。描画位置はBarの相対座標
     */
    private SkinImage[] trophy = new SkinImage[3];

    private SkinBarText[] text = new SkinBarText[2];
    /**
     * レベルのSkinNumber。描画位置はBarの相対座標
     */
    private SkinNumber[] barlevel = new SkinNumber[7];
    /**
     * 譜面ラベルのSkinImage。描画位置はBarの相対座標
     */
    private SkinImage[] label = new SkinImage[3];

    private int position = 0;

    private TextureRegion[][] images;
    private int cycle;

    /**
     * ランプ画像
     */
    private SkinImage[] lamp = new SkinImage[11];

    public SkinBar(int position) {
        this.position = position;
    }

    public SkinBar(int position, TextureRegion[][] images, int cycle) {
        setBarImages(images, cycle);
    }

    public void setBarImages(TextureRegion[][] images, int cycle) {
        this.images = images;
        this.cycle = cycle;
    }

    public SkinImage makeBarImages(boolean on, int index) {
        if ((on ? barimageon[index] : barimageoff[index]) == null) {
            if (on) {
                barimageon[index] = new SkinImage(images, 0, cycle);
            } else {
                barimageoff[index] = new SkinImage(images, 0, cycle);
            }
        }
        return on ? barimageon[index] : barimageoff[index];
    }

    public SkinImage getBarImages(boolean on, int index) {
        return on ? barimageon[index] : barimageoff[index];
    }

    public SkinImage[] getLamp() {
        return lamp;
    }

    public SkinImage[] getTrophy() {
        return trophy;
    }

    public SkinBarText[] getBarText() {
        return text;
    }

    public void setTrophy(SkinImage[] trophy) {
        this.trophy = trophy;
    }

    public void setLamp(SkinImage[] lamp) {
        this.lamp = lamp;
    }

    @Override
    public void draw(SpriteBatch sprite, long time, MainState state) {
        ((MusicSelector) state).renderBar(this, (int) time);
    }

    @Override
    public void dispose() {
    	disposeAll(barimageon);
    	disposeAll(barimageoff);
    	disposeAll(trophy);
    	disposeAll(text);
    	disposeAll(barlevel);
    	disposeAll(label);
    }

    public SkinNumber[] getBarlevel() {
        return barlevel;
    }

    public void setBarlevel(SkinNumber[] barlevel) {
        this.barlevel = barlevel;
    }

    public int getPosition() {
        return position;
    }
    
    @Override
	protected boolean mousePressed(MainState state, int button, int x, int y) {
        return ((MusicSelector) state).getBarRender().mousePressed(this, button, x, y);
	}

    public SkinImage[] getLabel() {
        return label;
    }

    public void setLabel(SkinImage[] label) {
        this.label = label;
    }

    public static class SkinBarText extends SkinObject {
        /**
         * ビットマップフォント
         */
        private BitmapFont font;

        private GlyphLayout layout;

        private int shadow = 0;

        private int align = ALIGN_LEFT;
        public static final int ALIGN_LEFT = 0;
        public static final int ALIGN_CENTER = 1;
        public static final int ALIGN_RIGHT = 2;

        public static final int[] ALIGN = {Align.left, Align.center, Align.right};

        private FreeTypeFontGenerator generator;
        private FreeTypeFontGenerator.FreeTypeFontParameter parameter;

        public SkinBarText(String fontpath, int cycle, int size) {
            this(fontpath, cycle, size, 0);
        }

        public SkinBarText(String fontpath, int cycle, int size, int shadow) {
            generator = new FreeTypeFontGenerator(Gdx.files.internal(fontpath));
            parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
//            this.setCycle(cycle);
            parameter.size = size;
            this.shadow = shadow;
        }

        public int getAlign() {
            return align;
        }

        public void setAlign(int align) {
            this.align = align;
        }

        public void setText(String text) {
            if (text == null || text.length() == 0) {
                text = " ";
            }
            parameter.characters = text;
            if (font != null) {
                font.dispose();
            }
            font = generator.generateFont(parameter);
            layout = new GlyphLayout(font, text);
        }

        public void draw(SpriteBatch sprite, long time, MainState state, String value, int offsetX, int offsetY) {
            if (generator == null) {
                return;
            }
            Rectangle r = this.getDestination(time, state);
            if (r != null) {
                if (font != null) {
                    Color c = getColor(time, state);
                    font.getData().setScale(r.height / parameter.size);
                    final float x = (align == 2 ? r.x - r.width : (align == 1 ? r.x - r.width / 2 : r.x));
                    if (shadow > 0) {
                        layout.setText(font, value, new Color(c.r / 2, c.g / 2, c.b / 2, c.a), r.getWidth(), ALIGN[align], false);
                        font.draw(sprite, layout, x + shadow + offsetX, r.y - shadow + offsetY);
                    }
                    layout.setText(font, value, c, r.getWidth(), ALIGN[align], false);
                    font.draw(sprite, layout, x + offsetX, r.y + offsetY);
                }
            }
        }

        @Override
        public void draw(SpriteBatch sprite, long time, MainState state) {

        }
        
		public void dispose() {
            if (generator != null) {
                generator.dispose();
                generator = null;
            }
            if (font != null) {
                font.dispose();
                font = null;
            }
        }

    }
}