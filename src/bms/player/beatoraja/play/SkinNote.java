package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.SkinObject;
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
    private PlaySkin skin;

    public SkinNote(PlaySkin skin, TextureRegion[][] note, TextureRegion[][][] longnote,
                    TextureRegion[][] minenote) {
        this.skin = skin;
        this.note = note;
        this.longnote = longnote;
        this.minenote = minenote;
        cnote = new TextureRegion[note.length];
        clongnote = new TextureRegion[10][note.length];
        cminenote = new TextureRegion[note.length];
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
            skin.player.getLanerender().drawLane(cnote, clongnote, cminenote);
        }
    }

    @Override
    public void dispose() {

    }
}
