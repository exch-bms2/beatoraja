package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * ノーツオブジェクト
 * 
 * @author exch
 */
public class SkinNote extends SkinObject {

	private SkinLane[] lanes;
	
	private LaneRenderer renderer;
	private long time;

	public SkinNote(SkinSource[] note, SkinSource[][] longnote, SkinSource[] minenote) {
		lanes = new SkinLane[note.length];
		for(int i = 0;i < lanes.length;i++) {
			lanes[i] = new SkinLane(note[i],longnote[i], minenote[i]);
		}
		
        this.setDestination(0, 0, 0, 0, 0, 0, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, new int[0]);
	}

	public void setLaneRegion(Rectangle[] region, float[] scale, int[] dstnote2, Skin skin) {
		for(int i = 0;i < lanes.length;i++) {
			lanes[i].setDestination(0,region[i].x, region[i].y, region[i].width, region[i].height, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			lanes[i].scale =  scale[i];
			lanes[i].dstnote2 =  dstnote2[i];
		}
	}

	@Override
	public void prepare(long time, MainState state) {
		if(renderer == null) {
			final BMSPlayer player = (BMSPlayer) state;
			if (player.getLanerender() == null) {
				draw = false;
				return;
			}
			renderer = player.getLanerender();
		}
		this.time = time;
		super.prepare(time, state);
		for(SkinLane lane : lanes) {
			lane.prepare(time, state);
		}
	}

	public void draw(SkinObjectRenderer sprite) {
		renderer.drawLane(sprite, time, lanes, this.getOffsets());
	}
	
	@Override
	public void dispose() {
		if (lanes != null) {
			disposeAll(lanes);
			lanes = null;
		}
	}

	static class SkinLane extends SkinObject {
		/**
		 * ノーツ画像
		 */
		SkinSource note;
		/**
		 * ロングノーツ画像
		 */
		SkinSource[] longnote = new SkinSource[10];
		/**
		 * 地雷ノーツ画像
		 */
		SkinSource minenote;

		float scale;
		int dstnote2;

		/**
		 * 不可視ノーツ画像
		 */
		SkinSource hiddennote;
		/**
		 * 処理済ノーツ画像
		 */
		SkinSource processednote;
		
		TextureRegion noteImage;
		TextureRegion[] longImage = new TextureRegion[10];
		TextureRegion mineImage;
		TextureRegion hiddenImage;
		TextureRegion processedImage;

		public SkinLane(SkinSource note, SkinSource[] longnote, SkinSource minenote) {
			init(note, longnote, minenote, null, null);
		}

		public SkinLane(SkinSource note, SkinSource[] longnote, SkinSource minenote, SkinSource hiddennote, SkinSource processednote) {
			init(note, longnote, minenote, hiddennote, processednote);
		}
		
		private void init(SkinSource note, SkinSource[] longnote, SkinSource minenote, SkinSource hiddennote, SkinSource processednote) {
			if(note == null) {
				Pixmap p = new Pixmap(32, 8, Pixmap.Format.RGBA8888);
				p.setColor(Color.WHITE);
				p.fillRectangle(0, 0, p.getWidth(), p.getHeight());
				note = new SkinSourceImage(new TextureRegion(new Texture(p)));
				p.dispose();	
			}
			this.note = note;
			
			for(int i = 0;i < this.longnote.length;i++) {
				
				if(longnote != null && i < longnote.length && longnote[i] != null) {
					this.longnote[i] = longnote[i];				
				} else {
					Pixmap p = new Pixmap(32, 8, Pixmap.Format.RGBA8888);
					p.setColor(Color.YELLOW);
					p.fillRectangle(0, 0, p.getWidth(), p.getHeight());
					this.longnote[i] = new SkinSourceImage(new TextureRegion(new Texture(p)));
					p.dispose();	
				}
			}
			
			if(minenote == null) {
				Pixmap p = new Pixmap(32, 8, Pixmap.Format.RGBA8888);
				p.setColor(Color.RED);
				p.fillRectangle(0, 0, p.getWidth(), p.getHeight());
				minenote = new SkinSourceImage(new TextureRegion(new Texture(p)));
				p.dispose();	
			}
			this.minenote = minenote;
			
			if(hiddennote == null) {
				Pixmap hn = new Pixmap(32, 8, Pixmap.Format.RGBA8888);
				hn.setColor(Color.ORANGE);
				hn.drawRectangle(0, 0, hn.getWidth(), hn.getHeight());
				hn.drawRectangle(1, 1, hn.getWidth() - 2, hn.getHeight() - 2);
				hiddennote = new SkinSourceImage(new TextureRegion(new Texture(hn)));
				hn.dispose();				
			}
			this.hiddennote = hiddennote;
			
			if(processednote == null) {
				Pixmap p = new Pixmap(32, 8, Pixmap.Format.RGBA8888);
				p.setColor(Color.CYAN);
				p.drawRectangle(0, 0, p.getWidth(), p.getHeight());
				p.drawRectangle(1, 1, p.getWidth() - 2, p.getHeight() - 2);
				processednote = new SkinSourceImage(new TextureRegion(new Texture(p)));
				p.dispose();	
			}
			this.processednote = processednote;
			
		}

		@Override
		public void prepare(long time, MainState state) {
			noteImage = note.getImage(time, state);
			for (int type = 0; type < 10; type++) {
				longImage[type] = longnote[type].getImage(time, state);
			}
			mineImage = minenote.getImage(time, state);
			hiddenImage = hiddennote.getImage(time, state);
			processedImage = processednote.getImage(time, state);
			super.prepare(time, state);
		}

		public void draw(SkinObjectRenderer sprite) {
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

