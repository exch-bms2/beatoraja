package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.SkinObject;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * ノーツオブジェクト
 *
 * @author exch
 */
public class SkinNote extends SkinObject {

    /**
     * ノーツ画像
     */
    private TextureRegion[][] note;
    /**
     * ロングノーツ画像
     */
    private TextureRegion[][][] longnote;
    /**
     * 地雷ノーツ画像
     */
    private TextureRegion[][] minenote;

    private TextureRegion[] cnote;
    private TextureRegion[][] clongnote;
    private TextureRegion[] cminenote;
    private TextureRegion[] chiddennote;
    private TextureRegion[] cprocessednote;
    private PlaySkin skin;

    /**
     * 不可視ノーツ画像
     */
    private TextureRegion[][] hiddennote;
    /**
     * 処理済ノーツ画像
     */
    private TextureRegion[][] processednote;

    public SkinNote(PlaySkin skin, TextureRegion[][] note, TextureRegion[][][] longnote,
                    TextureRegion[][] minenote) {
        this.skin = skin;
        this.note = note;
        this.longnote = longnote;
        this.minenote = minenote;
        cnote = new TextureRegion[note.length];
        clongnote = new TextureRegion[10][note.length];
        cminenote = new TextureRegion[note.length];
        chiddennote = new TextureRegion[note.length];
        cprocessednote = new TextureRegion[note.length];
        
		Pixmap hn = new Pixmap(note[0][0].getRegionWidth(), 8 , Pixmap.Format.RGBA8888);
		hn.setColor(Color.ORANGE);
		hn.drawRectangle(0, 0, hn.getWidth(), hn.getHeight());
		hn.drawRectangle(1, 1, hn.getWidth() - 2 , hn.getHeight() - 2);
		Pixmap pn = new Pixmap(note[0][0].getRegionWidth(), note[0][0].getRegionHeight(), Pixmap.Format.RGBA8888);
		pn.setColor(Color.CYAN);
		pn.drawRectangle(0, 0, hn.getWidth(), hn.getHeight());
		pn.drawRectangle(1, 1, hn.getWidth() - 2 , hn.getHeight() - 2);
        hiddennote = new TextureRegion[note.length][];
        processednote = new TextureRegion[note.length][];
        for(int i = 0;i < note.length;i++) {
        	hiddennote[i] = new TextureRegion[] {new TextureRegion(new Texture(hn)) };
        	processednote[i] = new TextureRegion[] {new TextureRegion(new Texture(pn)) };
        }
    }

    public TextureRegion getNoteImage(MainState state, long time, int lane) {
        if (note[lane] != null) {
            return note[lane][getImageIndex(note[lane].length, time, state)];
        }
        return null;
    }

    public TextureRegion getLongNoteImage(MainState state, long time, int lane, int type) {
        if (longnote[type][lane] != null) {
            return longnote[type][lane][getImageIndex(longnote[type][lane].length, time, state)];
        }
        return null;
    }

    public TextureRegion getMineNoteImage(MainState state, long time, int lane) {
        if (minenote[lane] != null) {
            return minenote[lane][getImageIndex(minenote[lane].length, time, state)];
        }
        return null;
    }

    public TextureRegion getProcessedNoteImage(MainState state, long time, int lane) {
        if (processednote[lane] != null) {
            return processednote[lane][getImageIndex(processednote[lane].length, time, state)];
        }
        return null;
    }

    public TextureRegion getHiddenNoteImage(MainState state, long time, int lane) {
        if (hiddennote[lane] != null) {
            return hiddennote[lane][getImageIndex(hiddennote[lane].length, time, state)];
        }
        return null;
    }

    @Override
    public void draw(SpriteBatch sprite, long time, MainState state) {
        if (skin.player.getLanerender() != null) {
            for (int i = 0; i < note.length; i++) {
                cnote[i] = getNoteImage(state, time, i);
            }
            for (int type = 0; type < 10; type++) {
                for (int i = 0; i < longnote[0].length; i++) {
                    clongnote[type][i] = getLongNoteImage(state, time, i, type);
                }
            }
            for (int i = 0; i < minenote.length; i++) {
                cminenote[i] = getMineNoteImage(state, time, i);
            }
            for (int i = 0; i < hiddennote.length; i++) {
            	chiddennote[i] = getHiddenNoteImage(state, time, i);
            }
            for (int i = 0; i < processednote.length; i++) {
            	cprocessednote[i] = getProcessedNoteImage(state, time, i);
            }
            skin.player.getLanerender().drawLane(cnote, clongnote, cminenote, cprocessednote, chiddennote);
        }
    }

    @Override
    public void dispose() {
    	if(note != null) {
    		for(TextureRegion[] trs : note) {
    			for(TextureRegion tr : trs) {
    				tr.getTexture().dispose();
    			}
    		}
    		note = null;
    	}
    	if(longnote != null) {
    		for(TextureRegion[][] trss : longnote) {
    			for(TextureRegion[] trs : trss) {
        			for(TextureRegion tr : trs) {
        				tr.getTexture().dispose();
        			}
    			}
    		}
    		longnote = null;
    	}
    	if(minenote != null) {
    		for(TextureRegion[] trs : minenote) {
    			if(trs != null) {
        			for(TextureRegion tr : trs) {
        				tr.getTexture().dispose();
        			}    				
    			}
    		}
    		minenote = null;
    	}
    	if(hiddennote != null) {
    		for(TextureRegion[] trs : hiddennote) {
    			for(TextureRegion tr : trs) {
    				tr.getTexture().dispose();
    			}
    		}
    		hiddennote = null;
    	}
    	if(processednote != null) {
    		for(TextureRegion[] trs : processednote) {
    			for(TextureRegion tr : trs) {
    				tr.getTexture().dispose();
    			}
    		}
    		processednote = null;
    	}
    }
}
