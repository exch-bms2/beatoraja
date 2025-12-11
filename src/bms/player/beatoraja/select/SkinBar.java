package bms.player.beatoraja.select;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import java.util.Optional;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * 楽曲バー描画用スキンオブジェクト
 * 
 * @author exch
 */
public final class SkinBar extends SkinObject {

    /**
     * 選択時のBarのSkinImage
     */
    private SkinImage[] barimageon = new SkinImage[BAR_COUNT];
    /**
     * 非選択時のBarのSkinImage
     */
    private SkinImage[] barimageoff = new SkinImage[BAR_COUNT];
    
    public static final int BAR_COUNT = 60;

    /**
     * トロフィーのSkinImage。描画位置はBarの相対座標
     */
    private final SkinImage[] trophy = new SkinImage[BARTROPHY_COUNT];
    
    public static final int BARTROPHY_COUNT = 3;

    /**
     * BarのSkinText。描画位置はBarの相対座標。
     * Indexは0:通常 1:新規 2:SongBar(通常) 3:SongBar(新規) 4:FolderBar(通常) 5:FolderBar(新規) 6:TableBar or HashBar
     * 7:GradeBar(曲所持) 8:(SongBar or GradeBar)(曲未所持) 9:CommandBar or ContainerBar 10:SearchWordBar
     * 3以降で定義されてなければ0か1を用いる
     */
    private final SkinText[] text = new SkinText[BARTEXT_COUNT];

    public static final int BARTEXT_NORMAL = 0;
    public static final int BARTEXT_NEW = 1;
    public static final int BARTEXT_SONG_NORMAL = 2;
    public static final int BARTEXT_SONG_NEW = 3;
    public static final int BARTEXT_FOLDER_NORMAL = 4;
    public static final int BARTEXT_FOLDER_NEW = 5;
    public static final int BARTEXT_TABLE = 6;
    public static final int BARTEXT_GRADE = 7;
    public static final int BARTEXT_NO_SONGS = 8;
    public static final int BARTEXT_COMMAND = 9;
    public static final int BARTEXT_SEARCH = 10;
    public static final int BARTEXT_COUNT = 11;
    /**
     * レベルのSkinNumber。描画位置はBarの相対座標
     */
    private final SkinNumber[] barlevel = new SkinNumber[BARLEVEL_COUNT];
    
    public static final int BARLEVEL_COUNT = 7;

    /**
     * 譜面ラベルのSkinImage。描画位置はBarの相対座標
     */
    private final SkinImage[] label = new SkinImage[BARLABEL_COUNT];
    
    public static final int BARLABEL_COUNT = 5;

    private SkinDistributionGraph graph;

    private int position = 0;

    /**
     * ランプ画像
     */
    private final SkinImage[] lamp = new SkinImage[BARLAMP_COUNT];
    /**
     * ライバルランプ表示時のプレイヤーランプ画像
     */
    private final SkinImage[] mylamp = new SkinImage[BARLAMP_COUNT];
    /**
     * ライバルランプ表示時のライバルランプ画像
     */
    private final SkinImage[] rivallamp = new SkinImage[BARLAMP_COUNT];

    public static final int BARLAMP_COUNT = 11;
    
    private BarRenderer render;
    /**
     * 描画不可スキンオブジェクト
     */
    private Array<SkinObject> removes = new Array<SkinObject>();

    public SkinBar(int position) {
        this.position = position;
        this.setDestination(0, 0, 0, 0, 0, 0, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, new int[0]);
    }

    public void setBarImage(SkinImage[] onimage, SkinImage[] offimage) {
    	barimageon = onimage;
    	barimageoff = offimage;
    }

    public SkinImage getBarImages(boolean on, int index) {
    	return index >= 0 && index < barimageoff.length ? (on ? barimageon[index] : barimageoff[index]) : null;
    }

    public SkinImage getLamp(int id) {
        return id >= 0 && id < this.lamp.length ? this.lamp[id] : null;
    }

    public SkinImage getPlayerLamp(int id) {
        return id >= 0 && id < this.mylamp.length ? this.mylamp[id] : null;
    }

    public SkinImage getRivalLamp(int id) {
        return id >= 0 && id < rivallamp.length ? rivallamp[id] : null;
    }

    public SkinImage getTrophy(int id) {
        return id >= 0 && id < trophy.length ? trophy[id] : null;
    }

    public SkinText getText(int id) {
        return id >= 0 && id < text.length ? text[id] : null;
    }

    public void setTrophy(int id, SkinImage trophy) {
        if(id >= 0 && id < this.trophy.length) {
            this.trophy[id] = trophy;
        }
    }

    public void setLamp(int id, SkinImage lamp) {
        if(id >= 0 && id < this.lamp.length) {
            this.lamp[id] = lamp;
        }
    }

    public void setPlayerLamp(int id, SkinImage mylamp) {
        if(id >= 0 && id < this.mylamp.length) {
            this.mylamp[id] = mylamp;
        }
    }

    public void setText(int id, SkinText text) {
        if(id >= 0 && id < this.text.length) {
            this.text[id] = text;
        }
    }

    public void setRivalLamp(int id, SkinImage rivallamp) {
        if(id >= 0 && id < this.rivallamp.length) {
            this.rivallamp[id] = rivallamp;
        }
    }

    public boolean validate() {
    	for(int i = 0;i < barimageon.length;i++) {
    		if(barimageon[i] != null && !barimageon[i].validate()) {
    			removes.add(barimageon[i]);
    			barimageon[i] = null;
    		}
    	}
    	for(int i = 0;i < barimageoff.length;i++) {
    		if(barimageoff[i] != null && !barimageoff[i].validate()) {
    			removes.add(barimageoff[i]);
    			barimageoff[i] = null;
    		}
    	}
    	for(int i = 0;i < trophy.length;i++) {
    		if(trophy[i] != null && !trophy[i].validate()) {
    			removes.add(trophy[i]);
    			trophy[i] = null;
    		}
    	}
    	for(int i = 0;i < label.length;i++) {
    		if(label[i] != null && !label[i].validate()) {
    			removes.add(label[i]);
    			label[i] = null;
    		}
    	}
    	for(int i = 0;i < lamp.length;i++) {
    		if(lamp[i] != null && !lamp[i].validate()) {
    			removes.add(lamp[i]);
    			lamp[i] = null;
    		}
    	}
    	for(int i = 0;i < mylamp.length;i++) {
    		if(mylamp[i] != null && !mylamp[i].validate()) {
    			removes.add(mylamp[i]);
    			mylamp[i] = null;
    		}
    	}
    	for(int i = 0;i < rivallamp.length;i++) {
    		if(rivallamp[i] != null && !rivallamp[i].validate()) {
    			removes.add(rivallamp[i]);
    			rivallamp[i] = null;
    		}
    	}
    	for(int i = 0;i < text.length;i++) {
    		if(text[i] != null && !text[i].validate()) {
    			removes.add(text[i]);
    			text[i] = null;
    		}
    	}
    	return super.validate();
    }
    
    @Override
    public void prepare(long time, MainState state) {
    	if(render == null) {
    		render = ((MusicSelector) state).getBarRender();
    		if(render == null) {
    			draw = false;
    			return;
    		}
    	}
    	super.prepare(time, state);
    	for(SkinImage bar : barimageon) {
    		if(bar != null) {
    			bar.prepare(time, state);
    		}
    	}
    	for(SkinImage bar : barimageoff) {
    		if(bar != null) {
    			bar.prepare(time, state);
    		}
    	}
    	for(SkinImage trophy : trophy) {
    		if(trophy != null) {
    			trophy.prepare(time, state);
    		}
    	}
    	for(SkinText text : text) {
    		if(text != null) {
    			text.prepare(time, state);
    		}
    	}
    	for(SkinNumber barlevel : barlevel) {
    		if(barlevel != null) {
    			barlevel.prepare(time, state);
    		}
    	}
    	for(SkinImage label : label) {
    		if(label != null) {
    			label.prepare(time, state);
    		}
    	}
    	for(SkinImage lamp : lamp) {
    		if(lamp != null) {
    			lamp.prepare(time, state);
    		}
    	}
    	for(SkinImage mylamp : mylamp) {
    		if(mylamp != null) {
    			mylamp.prepare(time, state);
    		}
    	}
    	for(SkinImage rivallamp : rivallamp) {
    		if(rivallamp != null) {
    			rivallamp.prepare(time, state);
    		}
    	}
    	
    	if(graph != null) {
    		graph.prepare(time, state);
    	}

        render.prepare(this, time);
    }

    public void draw(SkinObjectRenderer sprite) {
    	render.render(sprite, this);
    }

    @Override
    public void dispose() {
    	disposeAll(removes.toArray(SkinObject.class));
    	disposeAll(barimageon);
    	disposeAll(barimageoff);
    	disposeAll(trophy);
    	disposeAll(text);
    	disposeAll(barlevel);
    	disposeAll(label);
        disposeAll(lamp);
        disposeAll(mylamp);
        disposeAll(rivallamp);
        Optional.ofNullable(graph).ifPresent(Disposable::dispose);
        setDisposed();
    }

    public SkinNumber getBarlevel(int id) {
        return id >= 0 && id < barlevel.length ? barlevel[id] : null;
    }

    public void setBarlevel(int id, SkinNumber barlevel) {
        if(id >= 0 && id < this.barlevel.length) {
            this.barlevel[id] = barlevel;
        }
    }

    public int getPosition() {
        return position;
    }
    
    @Override
	protected boolean mousePressed(MainState state, int button, int x, int y) {
        return ((MusicSelector) state).getBarRender().mousePressed(this, button, x, y);
	}

    public SkinImage getLabel(int id) {
        return id >= 0 && id < label.length ? label[id] : null;
    }

    public void setLabel(int id, SkinImage label) {
        if(id >= 0 && id < this.label.length) {
            this.label[id] = label;
        }
    }

	public SkinDistributionGraph getGraph() {
		return graph;
	}

	public void setGraph(SkinDistributionGraph graph) {
		this.graph = graph;
	}
}