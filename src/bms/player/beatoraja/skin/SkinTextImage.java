package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * テキストオブジェクト
 *
 * @author exch
 */
public class SkinTextImage extends SkinObject {

	private SkinTextImageSource source;

	private int align = ALIGN_LEFT;
	public static final int ALIGN_LEFT = 0;
	public static final int ALIGN_CENTER = 1;
	public static final int ALIGN_RIGHT = 2;

	public static final int[] ALIGN = { Align.left, Align.center, Align.right };

	private int id = -1;

	private String text;

	public SkinTextImage(SkinTextImageSource source) {
		this.source = source;
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
	}

	public void draw(SpriteBatch sprite, long time, MainState state) {
		if (id == -1) {
			return;
		}
		final String value = state.getTextValue(id);
		if (value == null || value.length() == 0) {
			return;
		}
		Rectangle r = this.getDestination(time, state);
		if (r != null) {
			Color c = getColor(time, state);
			final float x = (align == 2 ? r.x - r.width : (align == 1 ? r.x - r.width / 2 : r.x));
			drawText(sprite, value, align, r, c);
		}
	}

	public void draw(SpriteBatch sprite, long time, MainState state, String value, int offsetX, int offsetY) {
		Rectangle r = this.getDestination(time, state);
		if (r != null) {
			Color c = getColor(time, state);
			drawText(sprite, value, align, r, c);
		}
	}

	private void drawText(SpriteBatch sprite, String text, int align, Rectangle r, Color c) {
		try {
			// System.out.println("SkinTextImage描画:" + text + " - " + x + " " +
			// y +
			// " " + w + " " + h);
			float width = 0;
			byte[] b = text.getBytes("utf-16le");
			for (int i = 0; i < b.length;) {
				int code = 0;
				code |= (b[i++] & 0xff);
				code |= (b[i++] & 0xff) << 8;
				if (code >= 0xdc00) {
					code |= (b[i++] & 0xff) << 16;
					code |= (b[i++] & 0xff) << 24;
				}
				final TextureRegion ch = source.getImage(code);
				if (ch != null) {
					width += ch.getRegionWidth() * r.height / source.getSize() + source.getMargin();
				}
			}

			final float scale = r.width < width ? r.width / width : 1;
			final float x = (align == 2 ? r.x - (r.width - width * scale)
					: (align == 1 ? r.x - width * scale / 2 : r.x));
			float dx = 0;
			for (int i = 0; i < b.length;) {
				int code = 0;
				code |= (b[i++] & 0xff);
				code |= (b[i++] & 0xff) << 8;
				if (code >= 0xdc00) {
					code |= (b[i++] & 0xff) << 16;
					code |= (b[i++] & 0xff) << 24;
				}
				final TextureRegion ch = source.getImage(code);
				if (ch != null) {
					final float tw = ch.getRegionWidth() * scale * r.height / source.getSize();
					// System.out.println("SkinTextImage描画:" + text.charAt(i) +
					// " -
					// " + (x + dx) + " " + y + " " + tw + " " + h);
					draw(sprite, ch, x + dx, r.y, tw, r.height, c, 0);
					dx += tw + source.getMargin() * scale;
				} else {
					// System.out.println("undefined char : " + text.charAt(i));
				}
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void dispose() {
		source.dispose();
	}

	public void setReferenceID(int id) {
		this.id = id;
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
