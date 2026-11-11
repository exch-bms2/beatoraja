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
	private final Array<TextRun> currentRuns = new Array<>(64);
	/** {@link #currentRuns} の幅を計算したときの描画高さ。 / Region height the widths in {@link #currentRuns} were computed for. */
	private float preparedRunHeight = Float.NaN;

	/** 画像にない文字を描く代替文字({@link FallbackText})。 / Fallback text for characters without an image ({@link FallbackText}). */
	private final FallbackText fallback = new FallbackText();

	public SkinTextImage(SkinTextImageSource source) {
		this(source, -1);
	}

	public SkinTextImage(SkinTextImageSource source, int id) {
		super(id);
		this.source = source;
		if (this.source != null) {
			this.source.retain();
		}
	}

	/**
	 * 画像にない文字を描く代替文字。必要になったとき共通代替フォントの供給元で {@link SkinTextFont} を作り、この部品が解放する。
	 * 供給元はプログラム全体で共有するので、ここでは解放しない。
	 * Fallback text for characters without an image. A {@link SkinTextFont} is built on demand on the global fallback source and
	 * released by this object; the source itself is shared program-wide and is not released here.
	 */
	private final class FallbackText {
		private SkinTextFont font;

		/** 作ってあれば返す(作らない)。 / Returns the fallback text if already built (never builds it). */
		private SkinTextFont current() {
			return font;
		}

		/** キャッシュの有効性に使う字形版。 / Glyph revision used for cache validity. */
		private long revision() {
			return font != null ? font.getGlyphRevision() : -1L;
		}

		/**
		 * 必要なら作り、位置・色・縁取り・影などの表示設定を写して返す。共通代替フォントがなければ null。
		 * Builds it when needed and copies placement, color, outline and shadow settings; null without global fallback fonts.
		 */
		private SkinTextFont prepared() {
			if (font == null) {
				SkinFontSource globalFonts = SystemFallbackGlyphs.globalSource();
				if (globalFonts == null) {
					return null;
				}
				int fontSize = source != null && source.getSize() > 0 ? source.getSize() : 48;
				font = new SkinTextFont(globalFonts, 0, fontSize, 2,
						bms.player.beatoraja.skin.property.StringPropertyFactory.getStringProperty(-1));
			}
			font.region.set(region);
			font.color = color;
			font.setAlign(getAlign());
			font.setFilter(getFilter());
			font.setWrapping(isWrapping());
			font.setOverflow(getOverflow());
			font.setOutlineColor(getOutlineColor());
			font.setOutlineWidth(getOutlineWidth());
			font.setShadowColor(getShadowColor());
			if (getShadowOffset() != null) { font.setShadowOffset(getShadowOffset()); }
			font.setShadowSmoothness(getShadowSmoothness());
			return font;
		}

		/** 代替ランの幅。 / Width of a fallback run. */
		private float measure(String fullText, String fbStr) {
			if (fbStr == null || fbStr.isEmpty()) {
				return 0f;
			}
			SkinTextFont fb = prepared();
			if (fb == null) {
				return 0f;
			}
			if (fb.getFont() == null) {
				prepareFont(fullText);
				if (fb.getFont() == null) {
					fb.prepareFont(fbStr);
				}
			}
			return fb.getTextWidth(fbStr);
		}

		private void prepare(String missingChars) {
			SkinTextFont fb = prepared();
			if (fb != null) {
				fb.prepareFont(missingChars);
			}
		}

		/** 代替ランを幅 width に収めて left から描く。 / Draws a fallback run from left, fitted into width. */
		private void draw(SkinObjectRenderer sprite, String text, float width, boolean shrink, float left, float offsetX, float offsetY) {
			SkinTextFont fb = prepared();
			if (fb == null) {
				return;
			}
			fb.region.width = width;
			fb.setWrapping(false);
			fb.setOverflow(shrink ? OVERFLOW_SHRINK : OVERFLOW_OVERFLOW);
			fb.setAlign(0);
			fb.setText(text);
			if (sprite != null) {
				// 文字描画は合成方法を設定しないので、前の部品の合成方法を引き継がないようこの部品の設定を使う。
				// Text drawing does not set the blend mode, so apply this object's blend instead of inheriting the previous one.
				sprite.setBlend(getBlend());
				fb.draw(sprite, offsetX + (left - fb.region.x), offsetY);
			}
		}

		private void dispose() {
			if (font != null) {
				font.dispose();
				font = null;
			}
		}
	}

	/**
	 * 画像文字・空白・フォールバックテキストの個別描画ラン (T11/Section 5.3)
	 * Discrete rendering run for image glyphs, whitespace, and fallback text runs.
	 */
	private static final class TextRun {
		enum Type { IMAGE, SPACE, FALLBACK }
		final Type type;
		final TextureRegion image;
		final String text;
		final float baseWidth;

		TextRun(TextureRegion image, float baseWidth) {
			this.type = Type.IMAGE;
			this.image = image;
			this.text = null;
			this.baseWidth = baseWidth;
		}

		TextRun(float spaceXadvance) {
			this.type = Type.SPACE;
			this.image = null;
			this.text = null;
			this.baseWidth = spaceXadvance;
		}

		TextRun(String text, float baseWidth) {
			this.type = Type.FALLBACK;
			this.image = null;
			this.text = text;
			this.baseWidth = baseWidth;
		}
	}

	private Array<TextRun> buildRuns(String text) {
		Array<TextRun> runs = new Array<>(text != null ? text.length() : 0);
		if (text == null || text.isEmpty()) {
			return runs;
		}

		if (source == null) {
			float fw = fallback.measure(text, text);
			if (fallback.current() != null) {
				runs.add(new TextRun(text, fw));
			}
			return runs;
		}

		float baseScale = (float) region.height / (float) source.getSize();
		float defaultSpaceWidth = source.getSize() * 0.35f * baseScale;

		StringBuilder currentFallback = new StringBuilder();

		for (int i = 0; i < text.length();) {
			int code = text.codePointAt(i);
			int charCount = Character.charCount(code);
			i += charCount;

			if (source.hasImage(code)) {
				if (currentFallback.length() > 0) {
					String fbStr = currentFallback.toString();
					currentFallback.setLength(0);
					runs.add(new TextRun(fbStr, fallback.measure(text, fbStr)));
				}
				TextureRegion reg = source.getImage(code);
				if (reg != null) {
					runs.add(new TextRun(reg, reg.getRegionWidth() * baseScale));
				}
			} else if (code == ' ') {
				if (currentFallback.length() > 0) {
					String fbStr = currentFallback.toString();
					currentFallback.setLength(0);
					runs.add(new TextRun(fbStr, fallback.measure(text, fbStr)));
				}
				runs.add(new TextRun(defaultSpaceWidth));
			} else {
				currentFallback.appendCodePoint(code);
			}
		}

		if (currentFallback.length() > 0) {
			String fbStr = currentFallback.toString();
			runs.add(new TextRun(fbStr, fallback.measure(text, fbStr)));
		}

		return runs;
	}

	@Override
	public void prepareFont(String text) {
		if (text != null) {
			text = normalizeDisplay(text);
		}
		StringBuilder missingChars = new StringBuilder();
		for (int i = 0; i < (text != null ? text.length() : 0);) {
			int code = text.codePointAt(i);
			i += Character.charCount(code);
			if (source != null && source.hasImage(code)) {
				source.getImage(code);
			} else if (code != ' ') {
				missingChars.appendCodePoint(code);
			}
		}
		if (missingChars.length() > 0) {
			fallback.prepare(missingChars.toString());
		}
	}

	@Override
	public boolean requiresFontPreparation() {
		return true;
	}

	@Override
	protected void prepareText(String text) {
		texts.clear();
		currentRuns.clear();
		textwidth = 0;
		preparedRunHeight = region != null ? region.height : Float.NaN;
		if (text == null || text.isEmpty()) {
			return;
		}
		text = normalizeDisplay(text);
		currentRuns.addAll(buildRuns(text));
		for (int i = 0; i < currentRuns.size; i++) {
			TextRun run = currentRuns.get(i);
			textwidth += run.baseWidth;
			if (run.image != null) {
				texts.add(run.image);
			}
		}
	}

	public void draw(SkinObjectRenderer sprite, float offsetX, float offsetY) {
		// ラン幅は高さに比例する。文字が同じでも高さが変われば(アニメーション)作り直す。
		// Run widths scale with the height; rebuild them when the height changes (animation) even if the text does not.
		if (region != null && Float.compare(preparedRunHeight, region.height) != 0) {
			prepareText(getText());
		}
		drawTextImageRuns(sprite, currentRuns, textwidth, offsetX, offsetY);
	}

	@Override
	public CachedTextLayout createCachedTextLayout(String text) {
		if (text != null) {
			text = normalizeDisplay(text);
		}
		Array<TextRun> runs = buildRuns(text != null ? text : "");
		float totalBaseWidth = 0;
		for (int i = 0; i < runs.size; i++) {
			totalBaseWidth += runs.get(i).baseWidth;
		}
		int srcSize = source != null ? source.getSize() : -1;
		float srcMargin = source != null ? source.getMargin() : 0f;
		float regWidth = region != null ? region.width : 0f;
		float regHeight = region != null ? region.height : 0f;
		return new ImageTextLayout(runs, totalBaseWidth, srcSize, srcMargin, regWidth, regHeight,
				fallback.current(), fallback.revision());
	}

	@Override
	public boolean isCachedTextLayoutValid(CachedTextLayout layout) {
		if (!(layout instanceof ImageTextLayout itl) || source == null) {
			return false;
		}
		float currentRegionWidth = region != null ? region.width : 0f;
		float currentRegionHeight = region != null ? region.height : 0f;
		return itl.sourceSize == source.getSize()
				&& Float.compare(itl.sourceMargin, source.getMargin()) == 0
				&& Float.compare(itl.regionWidth, currentRegionWidth) == 0
				&& Float.compare(itl.regionHeight, currentRegionHeight) == 0
				&& itl.fallbackFont == fallback.current()
				&& itl.fallbackGlyphRevision == fallback.revision();
	}

	@Override
	public boolean drawCachedTextLayout(SkinObjectRenderer sprite, CachedTextLayout layout, float offsetX, float offsetY) {
		if (!(layout instanceof ImageTextLayout cached) || !isCachedTextLayoutValid(cached)) {
			return false;
		}
		drawTextImageRuns(sprite, cached.runs, cached.width, offsetX, offsetY);
		return true;
	}

	/**
	 * 分割されたラン（画像文字、空白、フォールバック）を順次描画します（V01/V02/F-01）。
	 * Sequentially renders segmented runs (image glyphs, whitespace, and fallback) preserving original regions.
	 */
	private void drawTextImageRuns(SkinObjectRenderer sprite, Array<TextRun> runs, float totalBaseWidth,
			float offsetX, float offsetY) {
		if (runs == null || runs.size == 0 || source == null) {
			return;
		}
		int marginCount = Math.max(0, runs.size - 1);
		float totalWidth = totalBaseWidth + source.getMargin() * marginCount;

		final float scale = (region.width < totalWidth && totalWidth > 0) ? region.width / totalWidth : 1;
		final float x = (getAlign() == 2 ? region.x - totalWidth * scale
				: (getAlign() == 1 ? region.x - totalWidth * scale / 2 : region.x));
		float dx = 0;

		for (int i = 0; i < runs.size; i++) {
			TextRun run = runs.get(i);
			switch (run.type) {
				case IMAGE -> {
					final float tw = run.baseWidth * scale;
					if (sprite != null) {
						draw(sprite, run.image, x + dx + offsetX, region.y + offsetY, tw, region.height, color, 0);
					}
					dx += tw + source.getMargin() * scale;
				}
				case SPACE -> {
					final float sw = run.baseWidth * scale;
					dx += sw + source.getMargin() * scale;
				}
				case FALLBACK -> {
					final float fw = run.baseWidth * scale;
					if (run.text != null) {
						fallback.draw(sprite, run.text, fw, scale < 1.0f, x + dx, offsetX, offsetY);
					}
					dx += fw + source.getMargin() * scale;
				}
			}
		}
	}

	/**
	 * 分割済みランとその作成条件。代替フォントやその字形版が変われば無効になる。
	 * Segmented runs plus the inputs they were built from; invalid once the fallback font or its glyph revision changes.
	 */
	private static final class ImageTextLayout implements CachedTextLayout {
		private final Array<TextRun> runs;
		private final float width;
		private final int sourceSize;
		private final float sourceMargin;
		private final float regionWidth;
		private final float regionHeight;
		private final SkinTextFont fallbackFont;
		private final long fallbackGlyphRevision;

		private ImageTextLayout(Array<TextRun> runs, float width, int sourceSize, float sourceMargin,
				float regionWidth, float regionHeight, SkinTextFont fallbackFont, long fallbackGlyphRevision) {
			this.runs = runs;
			this.width = width;
			this.sourceSize = sourceSize;
			this.sourceMargin = sourceMargin;
			this.regionWidth = regionWidth;
			this.regionHeight = regionHeight;
			this.fallbackFont = fallbackFont;
			this.fallbackGlyphRevision = fallbackGlyphRevision;
		}

		@Override
		public float getWidth() {
			return width;
		}
	}

	@Override
	public void dispose() {
		if (isDisposed()) {
			return;
		}
		setDisposed();
		texts.clear();
		currentRuns.clear();
		if (source != null) {
			source.release();
		}
		fallback.dispose();
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
		private int refCount = 1;

		public SkinTextImageSource(boolean usecim) {
			this.usecim = usecim;
		}

		public synchronized void retain() {
			refCount++;
		}

		public synchronized void release() {
			if (--refCount <= 0) {
				dispose();
			}
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

		boolean hasImage(int index) {
			return regions.containsKey(index);
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
		public synchronized void dispose() {
			if (refCount > 1) {
				refCount--;
				return;
			}
			refCount = 0;
			for (SkinTextImageSourceElement tr : elements.values()) {
				if (tr.texture != null) {
					tr.texture.dispose();
					tr.texture = null;
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
