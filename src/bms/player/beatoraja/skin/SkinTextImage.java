package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * テキストオブジェクト
 *
 * @author exch
 */
public class SkinTextImage extends SkinText {

	// TODO SkinTextと共通化して選曲で使用できるように

	private SkinTextImageSource source;

	private TextureRegion[] texts;
	private float textwidth;

	public SkinTextImage(SkinTextImageSource source) {
		this.source = source;
	}
	
    public void prepareFont(String chars) {
    	
    }    

	@Override
	protected void prepareText(String text) {
		try {
			byte[] b = getText().getBytes("utf-16le");
			textwidth = 0;
			List<TextureRegion> l = new ArrayList<TextureRegion>(b.length / 2);
			for (int i = 0; i < b.length;) {
				int code = 0;
				code |= (b[i++] & 0xff);
				code |= (b[i++] & 0xff) << 8;
				if (code >= 0xdc00 && i < b.length) {
					code |= (b[i++] & 0xff) << 16;
					code |= (b[i++] & 0xff) << 24;
				}
				final TextureRegion ch = source.getImage(code);
				if (ch != null) {
					l.add(ch);
					textwidth += ch.getRegionWidth();
				}
			}
			texts = l.toArray(new TextureRegion[l.size()]);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public void draw(SpriteBatch sprite, long time, MainState state, int offsetX, int offsetY) {
		Rectangle r = this.getDestination(time, state);
		if (r != null) {
			Color c = getColor(time, state);
			// System.out.println("SkinTextImage描画:" + text + " - " + x + " " +
			// y +
			// " " + w + " " + h);
			float width = textwidth * r.height / source.getSize() + source.getMargin() * texts.length;

			final float scale = r.width < width ? r.width / width : 1;
			final float x = (getAlign() == 2 ? r.x - (r.width - width * scale)
					: (getAlign() == 1 ? r.x - width * scale / 2 : r.x));
			float dx = 0;
			for (TextureRegion ch : texts) {
				final float tw = ch.getRegionWidth() * scale * r.height / source.getSize();
				// System.out.println("SkinTextImage描画:" + text.charAt(i) +
				// " -
				// " + (x + dx) + " " + y + " " + tw + " " + h);
				draw(sprite, ch, x + dx + offsetX, r.y + offsetY, tw, r.height, c, 0);
				dx += tw + source.getMargin() * scale;
			}
		}
	}

	public void dispose() {
		source.dispose();
	}

	public static class SkinTextImageSource implements Disposable {

		private int size = 0;
		private int margin = 0;
		private Map<Integer, TextureRegion> images = new TreeMap<Integer, TextureRegion>();

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

		public TextureRegion getImage(int index) {
			return images.get(index);
		}

		public void setImage(int index, TextureRegion tex) {
			images.put(index, tex);
		}

		@Override
		public void dispose() {
			for (TextureRegion tr : images.values()) {
				tr.getTexture().dispose();
			}
		}
	}
}
