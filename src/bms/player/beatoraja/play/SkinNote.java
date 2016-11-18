package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.SkinObject;
import bms.player.beatoraja.skin.SkinSource;

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
	private SkinSource[] note;
	/**
	 * ロングノーツ画像
	 */
	private SkinSource[][] longnote;
	/**
	 * 地雷ノーツ画像
	 */
	private SkinSource[] minenote;

	private float scale;
	
	private TextureRegion[] cnote;
	private TextureRegion[][] clongnote;
	private TextureRegion[] cminenote;
	private TextureRegion[] chiddennote;
	private TextureRegion[] cprocessednote;
	private PlaySkin skin;

	/**
	 * 不可視ノーツ画像
	 */
	private SkinSource[] hiddennote;
	/**
	 * 処理済ノーツ画像
	 */
	private SkinSource[] processednote;

	public SkinNote(PlaySkin skin, TextureRegion[][] note, TextureRegion[][][] longnote, TextureRegion[][] minenote, float scale) {
		this(skin, note, longnote, minenote, 0, scale);
	}

	public SkinNote(PlaySkin skin, TextureRegion[][] note, TextureRegion[][][] longnote, TextureRegion[][] minenote,
			int cycle, float scale) {
		this.skin = skin;
		this.scale = scale;
		this.note = new SkinSource[note.length];
		for(int i = 0;i < note.length;i++) {
			this.note[i] = new SkinSource(note[i], 0, cycle);
		}
		this.longnote = new SkinSource[10][note.length];
		for(int type = 0;type < 10;type++) {
			for(int i = 0;i < note.length;i++) {
				this.longnote[type][i] = new SkinSource(longnote[type][i], 0, cycle);
			}			
		}
		this.minenote = new SkinSource[minenote.length];
		for(int i = 0;i < minenote.length;i++) {
			this.minenote[i] = new SkinSource(minenote[i], 0, cycle);
		}
		cnote = new TextureRegion[note.length];
		clongnote = new TextureRegion[10][note.length];
		cminenote = new TextureRegion[note.length];
		chiddennote = new TextureRegion[note.length];
		cprocessednote = new TextureRegion[note.length];

		Pixmap hn = new Pixmap(note[0][0].getRegionWidth(), 8, Pixmap.Format.RGBA8888);
		hn.setColor(Color.ORANGE);
		hn.drawRectangle(0, 0, hn.getWidth(), hn.getHeight());
		hn.drawRectangle(1, 1, hn.getWidth() - 2, hn.getHeight() - 2);
		Pixmap pn = new Pixmap(note[0][0].getRegionWidth(), note[0][0].getRegionHeight(), Pixmap.Format.RGBA8888);
		pn.setColor(Color.CYAN);
		pn.drawRectangle(0, 0, hn.getWidth(), hn.getHeight());
		pn.drawRectangle(1, 1, hn.getWidth() - 2, hn.getHeight() - 2);
		hiddennote = new SkinSource[note.length];
		processednote = new SkinSource[note.length];
		for (int i = 0; i < note.length; i++) {
			hiddennote[i] = new SkinSource(new TextureRegion(new Texture(hn)));
			processednote[i] = new SkinSource(new TextureRegion(new Texture(pn)));
		}
	}

	@Override
	public void draw(SpriteBatch sprite, long time, MainState state) {
		if (skin.player.getLanerender() != null) {
			for (int i = 0; i < note.length; i++) {
				if (note[i] != null) {
					cnote[i] = note[i].getImage(time, state);
				}
			}
			for (int type = 0; type < 10; type++) {
				for (int i = 0; i < longnote[0].length; i++) {
					if (longnote[type][i] != null) {
						clongnote[type][i] = longnote[type][i].getImage(time, state);
					}
				}
			}
			for (int i = 0; i < minenote.length; i++) {
				if (minenote[i] != null) {
					cminenote[i] = minenote[i].getImage(time, state);
				}
			}
			for (int i = 0; i < hiddennote.length; i++) {
				if (hiddennote[i] != null) {
					chiddennote[i] = hiddennote[i].getImage(time, state);
				}
			}
			for (int i = 0; i < processednote.length; i++) {
				if (processednote[i] != null) {
					cprocessednote[i] = processednote[i].getImage(time, state);
				}
			}
			skin.player.getLanerender().drawLane(cnote, clongnote, cminenote, cprocessednote, chiddennote, scale);
		}
	}

	@Override
	public void dispose() {
		if (note != null) {
			for (SkinSource ss : note) {
				ss.dispose();
			}
			note = null;
		}
		if (longnote != null) {
			for (SkinSource[] sss : longnote) {
				for (SkinSource ss : sss) {
					ss.dispose();
				}
			}
			longnote = null;
		}
		if (minenote != null) {
			for (SkinSource ss : minenote) {
				ss.dispose();
			}
			minenote = null;
		}
		if (hiddennote != null) {
			for (SkinSource trs : hiddennote) {
				trs.dispose();
			}
			hiddennote = null;
		}
		if (processednote != null) {
			for (SkinSource trs : processednote) {
				trs.dispose();
			}
			processednote = null;
		}
	}
}
