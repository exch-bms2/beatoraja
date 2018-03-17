package bms.player.beatoraja.select;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.select.bar.Bar;
import bms.player.beatoraja.select.bar.DirectoryBar;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * フォルダのランプ分布、ランク分布に使用される棒グラフ
 */
public class SkinDistributionGraph extends SkinObject {

    // TODO 分布計算はフォルダ選択時にのみ行う

    private SkinSource[] lampimage;

    private int type;

    private static final String[] LAMP = { "ff404040", "ff000080", "ff800080", "ffff00ff", "ff40ff40", "ff00c0f0", "ffffffff",
            "ff88ffff", "ffffff88", "ff8888ff", "ff0000ff" };

    private static final String[] RANK = { "ff404040", "ff400040", "ff400040", "ff400040", "ff400040", "ff400040", "ff000040",
            "ff000040", "ff000040", "ff004040", "ff004040", "ff004040", "ff00c000", "ff00c000", "ff00c000", "ff80c000", "ff80c000",
            "ff80c000", "ff0080f0", "ff0080f0", "ff0080f0", "ffe0e0e0", "ffe0e0e0", "ffe0e0e0", "ff44ffff", "ff44ffff", "ff44ffff",
            "ffccffff" };


    public SkinDistributionGraph(int type) {
    	this(type, createDefaultImages(type), 0, 0);
    }
    
    private static TextureRegion[][] createDefaultImages(int type) {
        if(type == 0) {
            Pixmap lampp = new Pixmap(11,1, Pixmap.Format.RGBA8888);
            for(int i = 0;i < LAMP.length;i++) {
                lampp.drawPixel(i,0, Color.valueOf(LAMP[i]).toIntBits());
            }
            Texture lampt = new Texture(lampp);
            TextureRegion[][] result = new TextureRegion[11][1];
            for(int i = 0;i < LAMP.length;i++) {
            	result[i] = new TextureRegion[]{new TextureRegion(lampt,i,0,1,1)};
            }
            return result;
        } else {
            Pixmap rankp = new Pixmap(28,1, Pixmap.Format.RGBA8888);
            for(int i = 0;i < RANK.length;i++) {
                rankp.drawPixel(i,0, Color.valueOf(RANK[i]).toIntBits());
            }
            Texture rankt = new Texture(rankp);
            TextureRegion[][] result = new TextureRegion[28][1];
            for(int i = 0;i < RANK.length;i++) {
            	result[i] = new TextureRegion[]{new TextureRegion(rankt,i,0,1,1)};
            }
            return result;
        }
    }

    public SkinDistributionGraph(int type, TextureRegion[][] image, int timer, int cycle) {
        this.type = type;
        if(type == 0) {
            lampimage = new SkinSource[11];
            for(int i = 0;i < lampimage.length;i++) {
                lampimage[i] = new SkinSourceImage(image[i],timer,cycle);
            }
        } else {
            lampimage = new SkinSource[28];
            for(int i = 0;i < lampimage.length;i++) {
                lampimage[i] = new SkinSourceImage(image[i],timer,cycle);
            }
        }
    }

    @Override
    public void draw(SkinObjectRenderer sprite, long time, MainState state) {
    	final Bar bar = ((MusicSelector)state).getSelectedBar();
    	if(!(bar instanceof DirectoryBar)) {
    		return;
    	}
    	draw(sprite, time, state, (DirectoryBar)bar, 0, 0);
    }

    public void draw(SkinObjectRenderer sprite, long time, MainState state, DirectoryBar current, int offsetx, int offsety) {
        final Rectangle r = getDestination(time, state);
        if (r != null && state.main.getPlayerResource().getConfig().isFolderLamp()) {
            int[] lamps = current.getLamps();
            int[] ranks = current.getRanks();
            int count = 0;
            for (int lamp : lamps) {
                count += lamp;
            }

            if (count != 0) {
                if(type == 0) {
                    for (int i = 10, x = 0; i >= 0; i--) {
                        sprite.draw(lampimage[i].getImage(time,state), r.x + x * r.width / count + offsetx, r.y + offsety, lamps[i] * r.width / count, r.height);                    		
                        x += lamps[i];
                    }
                } else {
                    for (int i = 27, x = 0; i >= 0; i--) {
                        sprite.draw(lampimage[i].getImage(time,state), r.x + x * r.width / count + offsetx, r.y + offsety, ranks[i] * r.width / count, r.height);                    		
                        x += ranks[i];
                    }
                }
            }
        }
    }

    @Override
    public void dispose() {
    	disposeAll(lampimage);
    }
}
