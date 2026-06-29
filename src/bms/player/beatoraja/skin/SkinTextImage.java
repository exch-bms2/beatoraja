package bms.player.beatoraja.skin;

import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntMap;


/**
 * イメージデータをソースとして持つスキン用テキスト
 * 
 * @author exch
 */
public final class SkinTextImage extends SkinText {
	/**
	 * テキストイメージ
	 */
	private final SkinTextImageSource source;
	/**
	 * 現在のテキスト
	 */
	private final Array<TextureRegion> texts = new Array<TextureRegion>(256);
	/**
	 * 現在のテキスト長
	 */
	private float textwidth;

	public SkinTextImage(SkinTextImageSource source) {
		this(source, -1);
	}

	public SkinTextImage(SkinTextImageSource source, int id) {
		super(id);
		this.source = source;
	}

	@Override
	public void prepareFont(String text) {
		for (int i = 0; i < text.length();) {
			int code = text.codePointAt(i);
			i += Character.charCount(code);
			source.getImage(code);
		}
	}

	@Override
	protected void prepareText(String text) {
		textwidth = 0;
		texts.clear();
		for (int i = 0; i < text.length();) {
			int code = text.codePointAt(i);
			i += Character.charCount(code);
			final TextureRegion ch = source.getImage(code);
			if (ch != null) {
				texts.add(ch);
				textwidth += ch.getRegionWidth();
			} else {
				// System.out.println(text + " -> " + Arrays.toString(b) +
				// "code not found : " + Integer.toHexString(code));
			}
		}
	}

	public void draw(SkinObjectRenderer sprite, float offsetX, float offsetY) {
		// System.out.println("SkinTextImage描画:" + text + " - " + x + " " +
		// y +
		// " " + w + " " + h);
		float width = textwidth * region.height / source.getSize() + source.getMargin() * texts.size;

		final float scale = region.width < width ? region.width / width : 1;
		final float x = (getAlign() == 2 ? region.x - width * scale : (getAlign() == 1 ? region.x - width * scale / 2 : region.x));
		float dx = 0;
		for (int i = 0;i < texts.size;i++) {
			final TextureRegion ch = texts.get(i);
			final float tw = ch.getRegionWidth() * scale * region.height / source.getSize();
			// System.out.println("SkinTextImage描画:" + text.charAt(i) +
			// " -
			// " + (x + dx) + " " + y + " " + tw + " " + h);
			draw(sprite, ch, x + dx + offsetX, region.y + offsetY, tw, region.height, color, 0);
			dx += tw + source.getMargin() * scale;
		}
	}

	public void dispose() {
		source.dispose();
	}

	/**
	 * テキストイメージ
	 * 
	 * @author exch
	 */
	public static final class SkinTextImageSource implements Disposable {
		/**
		 * サイズ
		 */
		private int size = 0;
		/**
		 * 文字間のマージン
		 */
		private int margin = 0;
		private final IntMap<SkinTextImageSourceElement> elements = new IntMap<SkinTextImageSourceElement>(400);
		private final boolean usecim;
		private final IntMap<SkinTextImageSourceRegion> regions = new IntMap<SkinTextImageSourceRegion>(10000);

		public SkinTextImageSource(boolean usecim) {
			this.usecim = usecim;
		}

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
			SkinTextImageSourceRegion region = regions.get(index);
			if (region == null) {
				return null;
			}
			if (region.image == null) {
				SkinTextImageSourceElement element = elements.get(region.id);
				if (element == null) {
					return null;
				}
				if (element.texture == null) {
					element.texture = SkinLoader.getTexture(element.path, usecim);
					if (element.texture == null) {
						return null;
					}
				}
				region.image = new TextureRegion(element.texture, region.x, region.y, region.w, region.h);
			}
			return region.image;
		}

		public void setImage(int index, int id, int x, int y, int w, int h) {
			regions.put(index, new SkinTextImageSourceRegion(id, x, y, w, h));
		}

		public String getPath(int index) {
			SkinTextImageSourceElement element = elements.get(index);
			if (element != null) {
				return element.path;
			}
			return null;
		}

		public void setPath(int index, String p) {
			SkinTextImageSourceElement element = new SkinTextImageSourceElement();
			element.path = p;
			elements.put(index, element);
		}

		@Override
		public void dispose() {
			for (SkinTextImageSourceElement tr : elements.values()) {
				if (tr.texture != null) {
					tr.texture.dispose();
				}
			}
		}
	}

	private static final class SkinTextImageSourceElement {

		private String path;
		private Texture texture;
	}

	private static final class SkinTextImageSourceRegion {

		private final int id;
		private final int x;
		private final int y;
		private final int w;
		private final int h;
		private TextureRegion image;

		public SkinTextImageSourceRegion(int id, int x, int y, int w, int h) {
			this.id = id;
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}
	}
}
