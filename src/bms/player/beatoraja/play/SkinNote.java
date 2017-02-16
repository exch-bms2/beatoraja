package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * ノーツオブジェクト
 * 
 * @author exch
 */
public class SkinNote extends SkinObject {

	// TODO Lane毎にスキンオブジェクト化してLaneRegionをDestination化

	private SkinLane[] lanes;

	private float scale;
	
	private TextureRegion[] cnote;
	private TextureRegion[][] clongnote;
	private TextureRegion[] cminenote;
	private TextureRegion[] chiddennote;
	private TextureRegion[] cprocessednote;
	private Rectangle[] claneregion;

	public SkinNote(TextureRegion[][] note, TextureRegion[][][] longnote, TextureRegion[][] minenote, float scale) {
		this(note, longnote, minenote, 0, scale);
	}

	public SkinNote(TextureRegion[][] note, TextureRegion[][][] longnote, TextureRegion[][] minenote,
			int cycle, float scale) {
		this.scale = scale;
		lanes = new SkinLane[note.length];
		for(int i = 0;i < lanes.length;i++) {
			TextureRegion[][] ln = new TextureRegion[10][];
			for(int t = 0;t < 10;t++) {
				ln[t] = longnote[t][i];
			}
			lanes[i] = new SkinLane(note[i],ln, minenote[i], cycle, scale);
		}
		cnote = new TextureRegion[note.length];
		clongnote = new TextureRegion[10][note.length];
		cminenote = new TextureRegion[note.length];
		chiddennote = new TextureRegion[note.length];
		cprocessednote = new TextureRegion[note.length];
		claneregion = new Rectangle[note.length];

		Pixmap hn = new Pixmap(note[0][0].getRegionWidth(), 8, Pixmap.Format.RGBA8888);
		hn.setColor(Color.ORANGE);
		hn.drawRectangle(0, 0, hn.getWidth(), hn.getHeight());
		hn.drawRectangle(1, 1, hn.getWidth() - 2, hn.getHeight() - 2);
		Pixmap pn = new Pixmap(note[0][0].getRegionWidth(), note[0][0].getRegionHeight(), Pixmap.Format.RGBA8888);
		pn.setColor(Color.CYAN);
		pn.drawRectangle(0, 0, hn.getWidth(), hn.getHeight());
		pn.drawRectangle(1, 1, hn.getWidth() - 2, hn.getHeight() - 2);
	}

	public void setLaneRegion(Rectangle[] region) {
		for(int i = 0;i < lanes.length;i++) {
			lanes[i].setDestination(0,region[i].x, region[i].y, region[i].width, region[i].height, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		}
	}

	@Override
	public void draw(SpriteBatch sprite, long time, MainState state) {
		final BMSPlayer player = (BMSPlayer) state;
		if (player.getLanerender() != null) {
			for (int i = 0; i < lanes.length; i++) {
				if (lanes[i].note != null) {
					cnote[i] = lanes[i].note.getImage(time, state);
				}
				for (int type = 0; type < 10; type++) {
					if (lanes[i].longnote[type] != null) {
						clongnote[type][i] = lanes[i].longnote[type].getImage(time, state);
					}
				}
				if (lanes[i].minenote != null) {
					cminenote[i] = lanes[i].minenote.getImage(time, state);
				}
				if (lanes[i].hiddennote != null) {
					chiddennote[i] = lanes[i].hiddennote.getImage(time, state);
				}
				if (lanes[i].processednote != null) {
					cprocessednote[i] = lanes[i].processednote.getImage(time, state);
				}
				claneregion[i] = lanes[i].getDestination(time, state);
			}
			player.getLanerender().drawLane(time, cnote, clongnote, cminenote, cprocessednote, chiddennote, claneregion, scale);
		}
	}

	@Override
	public void dispose() {
		if (lanes != null) {
			disposeAll(lanes);
			lanes = null;
		}
	}

	class SkinLane extends SkinObject {
		/**
		 * ノーツ画像
		 */
		private SkinSource note;
		/**
		 * ロングノーツ画像
		 */
		private SkinSource[] longnote = new SkinSource[10];
		/**
		 * 地雷ノーツ画像
		 */
		private SkinSource minenote;

		private float scale;

		private TextureRegion[] cnote;
		private TextureRegion[][] clongnote;
		private TextureRegion[] cminenote;
		private TextureRegion[] chiddennote;
		private TextureRegion[] cprocessednote;

		/**
		 * 不可視ノーツ画像
		 */
		private SkinSource hiddennote;
		/**
		 * 処理済ノーツ画像
		 */
		private SkinSource processednote;

		public SkinLane(TextureRegion[] note, TextureRegion[][] longnote, TextureRegion[] minenote, float scale) {
			this(note, longnote, minenote, 0, scale);
		}

		public SkinLane(TextureRegion[] note, TextureRegion[][] longnote, TextureRegion[] minenote,
						int cycle, float scale) {
			this.scale = scale;
			this.note = new SkinSourceImage(note, 0, cycle);
			for(int type = 0;type < 10;type++) {
				this.longnote[type] = new SkinSourceImage(longnote[type], 0, cycle);
			}
			this.minenote = new SkinSourceImage(minenote, 0, cycle);
			cnote = new TextureRegion[note.length];
			clongnote = new TextureRegion[10][note.length];
			cminenote = new TextureRegion[note.length];
			chiddennote = new TextureRegion[note.length];
			cprocessednote = new TextureRegion[note.length];

			Pixmap hn = new Pixmap(note[0].getRegionWidth(), 8, Pixmap.Format.RGBA8888);
			hn.setColor(Color.ORANGE);
			hn.drawRectangle(0, 0, hn.getWidth(), hn.getHeight());
			hn.drawRectangle(1, 1, hn.getWidth() - 2, hn.getHeight() - 2);
			Pixmap pn = new Pixmap(note[0].getRegionWidth(), note[0].getRegionHeight(), Pixmap.Format.RGBA8888);
			pn.setColor(Color.CYAN);
			pn.drawRectangle(0, 0, hn.getWidth(), hn.getHeight());
			pn.drawRectangle(1, 1, hn.getWidth() - 2, hn.getHeight() - 2);
			hiddennote = new SkinSourceImage(new TextureRegion(new Texture(hn)));
			processednote = new SkinSourceImage(new TextureRegion(new Texture(pn)));
		}

		@Override
		public void draw(SpriteBatch sprite, long time, MainState state) {
		}

		@Override
		public void dispose() {
			if (note != null) {
				note.dispose();
				note = null;
			}
			if (longnote != null) {
				disposeAll(longnote);
				longnote = null;
			}
			if (minenote != null) {
				minenote.dispose();
				minenote = null;
			}
			if (hiddennote != null) {
				hiddennote.dispose();
				hiddennote = null;
			}
			if (processednote != null) {
				processednote.dispose();
				processednote = null;
			}
		}
	}
}

