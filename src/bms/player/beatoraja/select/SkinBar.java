package bms.player.beatoraja.select;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.*;
import com.badlogic.gdx.graphics.g2d.*;

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

    private SkinText[] text = new SkinText[2];
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
        this.setDestination(0, 0, 0, 0, 0, 0, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, new int[0]);
    }

    public SkinBar(int position, TextureRegion[][] images, int cycle) {
        setBarImages(images, cycle);
    }

    public void setBarImages(TextureRegion[][] images, int cycle) {
        this.images = images;
        this.cycle = cycle;
    }
    
    public void setBarImage(SkinImage[] onimage, SkinImage[] offimage) {
    	barimageon = onimage;
    	barimageoff = offimage;
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
    	if(index >= 0 && index < barimageoff.length) {
            return on ? barimageon[index] : barimageoff[index];    		
    	}
    	return null;
    }

    public SkinImage[] getLamp() {
        return lamp;
    }

    public SkinImage[] getTrophy() {
        return trophy;
    }

    public SkinText[] getText() {
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
        ((MusicSelector)state).getBarRender().render(sprite, (MusicSelectSkin) state.getSkin(), this, (int)time);
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
}