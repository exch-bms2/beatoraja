package bms.player.beatoraja.skin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import bms.player.beatoraja.skin.property.StringProperty;
import bms.player.beatoraja.skin.property.StringPropertyFactory;
import bms.player.beatoraja.skin.BitmapFontCache.CacheableBitmapFont;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import java.util.function.Predicate;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.GlyphLayout.GlyphRun;
import bms.player.beatoraja.skin.BitmapFontLoader.BitmapFontDataCopy;
import bms.player.beatoraja.skin.MissingGlyphMapper.PuaSlotAllocator;
import bms.player.beatoraja.skin.FontRenderRetirement.FontResourceDisposer;

/**
 * ビットマップ(.fnt)スキン文字。元フォントと代替フォントを別々に描画し、計測済みの run 位置を共有する。
 * Bitmap (.fnt) skin text. Original and fallback glyphs are drawn by separate fonts that share measured run positions.
 */
public final class SkinTextBitmap extends SkinText {

	/**
	 * 欠落記号の候補 (□, ▢, ☐, ■, ?)。順序が優先順位。
	 * Missing-glyph marker candidates (□, ▢, ☐, ■, ?) in priority order.
	 */
	static final int[] MISSING_GLYPH_CANDIDATES = { 0x25a1, 0x25a2, 0x2610, 0x25a0, '?' };

	private final SkinTextBitmapSource source;
	private final BitmapFont.BitmapFontData fontData;
	private final Array<TextureRegion> regions;
	private final int basePageCount;
	private BitmapFont font;
	/**
	 * 通常描画用の最新レイアウト。null は再構築が必要という意味。
	 * Latest layout for the normal draw path; null means it must be rebuilt.
	 */
	private BitmapTextLayout drawLayout;
	private final BitmapFont.BitmapFontData originalMetrics = new BitmapFont.BitmapFontData();
	private final float size;
	private final int fontType;
	private final Vector2 shadowOffsetUniform = new Vector2();
	private final Color shadowDrawColor = new Color();
	private final Color previousSpriteColor = new Color();
	private final Consumer<ShaderProgram> distanceFieldUniformSetter = this::setDistanceFieldUniforms;
	/**
	 * 元フォントにない文字を描く代替の層。状態と寿命はすべてこの中にある({@link FallbackLayer})。
	 * The fallback layer that draws characters missing from the original font; all its state and lifetime live in {@link FallbackLayer}.
	 */
	private final FallbackLayer fallback = new FallbackLayer();

	public SkinTextBitmap(SkinTextBitmapSource source, float size) {
		this(source, size, StringPropertyFactory.getStringProperty(-1));
	}

	public SkinTextBitmap(SkinTextBitmapSource source, float size, StringProperty property) {
		this(source, null, size, property);
	}

	public SkinTextBitmap(SkinTextBitmapSource source, SkinFontSource fallbackSource, float size, StringProperty property) {
		super(property);
		this.source = source;
		this.size = size;
		if (getShadowOffset() == null) { setShadowOffset(new Vector2()); }

		if (source != null) {
			BitmapFont srcFont = source.getFont();
			BitmapFont.BitmapFontData srcData = source.getFontData() != null
					? source.getFontData()
					: (srcFont != null ? srcFont.getData() : null);
			this.fontData = BitmapFontDataCopy.copyFontData(srcData);
			this.regions = source.getRegions() != null
					? new Array<>(source.getRegions())
					: (srcFont != null && srcFont.getRegions() != null ? new Array<>(srcFont.getRegions()) : new Array<>());
			this.fontType = source.getType();
		} else {
			this.fontData = null;
			this.regions = new Array<>();
			this.fontType = SkinTextBitmapSource.TYPE_STANDARD;
		}
		this.basePageCount = this.regions.size;
		initializeFonts(fallbackSource);
	}

	public SkinTextBitmap(BitmapFont.BitmapFontData sharedData, Array<TextureRegion> sharedRegions, float size) {
		this(sharedData, sharedRegions, null, size, SkinTextBitmapSource.TYPE_STANDARD, StringPropertyFactory.getStringProperty(-1));
	}

	public SkinTextBitmap(BitmapFont.BitmapFontData sharedData, Array<TextureRegion> sharedRegions,
			SkinFontSource fallbackSource, float size, int type, StringProperty property) {
		super(property);
		this.source = null;
		this.size = size;
		this.fontType = type;
		if (getShadowOffset() == null) { setShadowOffset(new Vector2()); }
		this.fontData = BitmapFontDataCopy.copyFontData(sharedData);
		this.regions = sharedRegions != null ? new Array<>(sharedRegions) : new Array<>();
		this.basePageCount = this.regions.size;
		initializeFonts(fallbackSource);
	}

	/**
	 * 両コンストラクタ共通の後処理。元フォントを作り、代替の層を用意する。
	 * Shared constructor tail: builds the original font and prepares the fallback layer.
	 */
	private void initializeFonts(SkinFontSource fallbackSource) {
		if (fontData != null) {
			font = new BitmapFont(fontData, regions, false);
		}
		fallback.initialize(fallbackSource);
	}

	/**
	 * 元フォントが距離場(SDF)形式か。 / Whether the original font uses a distance-field (SDF) type.
	 */
	private boolean isDistanceField() {
		int effectiveType = source != null ? source.getType() : fontType;
		return effectiveType == SkinTextBitmapSource.TYPE_DISTANCE_FIELD
				|| effectiveType == SkinTextBitmapSource.TYPE_COLORED_DISTANCE_FIELD;
	}

	int getGlyphYOffset() {
		if (source != null) {
			return source.getGlyphYOffset();
		}
		if (fontData != null) {
			return SkinTextBitmapSource.getRepresentativeGlyphYOffset(fontData);
		}
		return 0;
	}

	public float getOriginalSize() {
		if (source != null) {
			return source.getOriginalSize();
		}
		return size > 0 ? size : (fontData != null ? fontData.lineHeight : 16f);
	}

	public BitmapFont getFont() {
		return font;
	}

	BitmapFont.BitmapFontData getFontData() {
		return fontData;
	}

	Array<TextureRegion> getRegions() {
		return regions;
	}

	int getBasePageCount() {
		return basePageCount;
	}

	/**
	 * 編集欄が引き継ぐのは拡大縮小できる代替フォント列だけで、ビットマップ資産は渡さない。
	 * The editor inherits only the scalable fallback chain, never the bitmap asset.
	 */
	@Override
	SkinFontSource getFontSource() {
		return fallback.systemFallback.sourceOrDefault();
	}

	/**
	 * 指定されたテキストの描画幅を現在のフォントスケールで計算して返します。
	 * Computes and returns the layout advance width for the given text under the current scale.
	 */
	float getTextWidth(String text) {
		if (text == null || text.isEmpty() || font == null) return 0;
		String remapped = remapMissingGlyphs(normalizeDisplay(text));
		BitmapFont.BitmapFontData data = fallback.measurementFont.getData();
		BitmapTextRuns.copyMetrics(fontData, data);
		try {
			data.setScale(size / getOriginalSize());
			GlyphLayout measured = new GlyphLayout();
			setLayout(measured, fallback.measurementFont, remapped, region.width);
			return measured.width;
		} finally {
			BitmapTextRuns.copyMetrics(fontData, data);
		}
	}

	// 代替の層への窓口(同じパッケージの試験・部品が使う)。 / Entry points into the fallback layer (used by tests and package peers).
	void setFallbackFontSource(SkinFontSource fallbackSource) { fallback.systemFallback.setSource(fallbackSource, false); }
	void setFallbackFontSource(SkinFontSource fallbackSource, boolean owns) { fallback.systemFallback.setSource(fallbackSource, owns); }
	void updateFallbackFontParameters() { fallback.systemFallback.refresh(); }
	FreeTypeFontGenerator.FreeTypeFontParameter getFallbackParameter() { return fallback.systemFallback.style(); }
	GlyphProvider getGlyphProvider() { return fallback.systemFallback.provider(); }
	int getFallbackAtlasPageCount() { return fallback.systemFallback.atlasPageCount(); }
	PuaSlotAllocator getPuaSlotAllocator() { return fallback.missingGlyphs.slots(); }
	long getGlyphRevision() { return fallback.missingGlyphs.revision(); }
	boolean isFallbackPage(int page) { return fallback.isFallbackPage(page); }
	boolean isFallbackGlyph(Glyph glyph) { return fallback.isFallbackGlyph(glyph); }
	void registerFallbackGlyph(int codePoint, Glyph glyph) { fallback.register(codePoint, glyph); }
	String remapMissingGlyphs(String text) { return fallback.remap(text); }
	void flushFallbackTextures() { fallback.flush(); }
	/** 供給元や方針の変更後に、成功・失敗両方の代替結果を無効にする。 / Invalidates fallback results after a source or policy change. */
	void invalidateFallbackGlyphs() { fallback.reset(); }

	@Override
	public void load() {
	}

	@Override
	public void prepareFont(String text) {
		prepareText(normalizeDisplay(text));
	}

	@Override
	protected void prepareText(String text) {
		invalidateLayout();
		updateFallbackFontParameters();
		if (text != null && font != null) {
			remapMissingGlyphs(text);
		}
	}

	public void draw(SkinObjectRenderer sprite, float offsetX, float offsetY) {
		if (font == null) return;
		BitmapTextLayout layout = updateLayout(region);
		flushFallbackTextures();
		drawRunPlan(sprite, layout.runs, layout.scaleX, layout.scaleY, layout.hasShadow, offsetX, offsetY);
	}

	@Override
	public CachedTextLayout createCachedTextLayout(String text) {
		if (font == null || text == null) return null;
		return buildLayout(normalizeDisplay(text));
	}

	private BitmapTextLayout buildLayout(String text) {
		String remapped = remapMissingGlyphs(text);
		float scale = size / getOriginalSize();
		BitmapFont.BitmapFontData data = fallback.measurementFont.getData();
		BitmapTextRuns.copyMetrics(fontData, data);
		GlyphLayout measured = new GlyphLayout();
		float scaleX = scale;
		try {
			if (!isWrapping() && getOverflow() == OVERFLOW_SHRINK && region.width <= 0) {
				return new BitmapTextLayout(this, text, new BitmapTextRuns(measured, fallback.fallbackRunSelector),
						region.width, scale, scale, getGlyphRevision(), getAlign(), getOverflow(), false,
						!getShadowOffset().isZero());
			}
			data.setScale(scaleX, scale);
			setLayout(measured, fallback.measurementFont, remapped, region.width);
			if (!isWrapping() && getOverflow() == OVERFLOW_SHRINK && measured.width > region.width) {
				scaleX *= region.width / measured.width;
				data.setScale(scaleX, scale);
				setLayout(measured, fallback.measurementFont, remapped, region.width);
			}
			return new BitmapTextLayout(this, text, new BitmapTextRuns(measured, fallback.fallbackRunSelector),
					region.width, scaleX, scale, getGlyphRevision(), getAlign(), getOverflow(), isWrapping(),
					!getShadowOffset().isZero());
		} finally {
			BitmapTextRuns.copyMetrics(fontData, data);
		}
	}

	@Override
	public boolean isCachedTextLayoutValid(CachedTextLayout layout) {
		updateFallbackFontParameters();
		if (!(layout instanceof BitmapTextLayout cached) || font == null || isDisposed() || cached.owner != this) return false;
		float scale = size / getOriginalSize();
		return cached.width == region.width && cached.scaleY == scale && cached.dependenciesValid()
				&& cached.align == getAlign() && cached.overflow == getOverflow() && cached.wrapping == isWrapping()
				&& cached.hasShadow == !getShadowOffset().isZero();
	}

	@Override
	public boolean drawCachedTextLayout(SkinObjectRenderer sprite, CachedTextLayout layout, float offsetX, float offsetY) {
		if (!(layout instanceof BitmapTextLayout cached) || !isCachedTextLayoutValid(cached)) return false;
		flushFallbackTextures();
		drawRunPlan(sprite, cached.runs, cached.scaleX, cached.scaleY, cached.hasShadow, offsetX, offsetY);
		return true;
	}

	private void drawRunPlan(SkinObjectRenderer sprite, BitmapTextRuns plan, float scaleX, float scaleY,
			boolean hasShadow, float offsetX, float offsetY) {
		BitmapTextRuns.copyMetrics(fontData, originalMetrics);
		BitmapTextRuns.copyMetrics(fallback.fallbackData, fallback.fallbackMetrics);
		int previousType = sprite.getType();
		previousSpriteColor.set(sprite.getColor());
		float x = (getAlign() == 2 ? region.x - region.width : getAlign() == 1 ? region.x - region.width / 2 : region.x) + offsetX;
		float y = region.y + offsetY + region.height;
		boolean sdf = isDistanceField();
		try {
			fontData.setScale(scaleX, scaleY);
			fallback.fallbackData.setScale(scaleX, scaleY);
			if (hasShadow && !sdf) {
				shadowDrawColor.set(color.r / 2, color.g / 2, color.b / 2, color.a);
				drawRuns(sprite, plan, x + getShadowOffset().x, y - getShadowOffset().y, shadowDrawColor, false);
			} else if (hasShadow) {
				drawGeneratedFallbackShadows(sprite, plan, x, y, scaleX, scaleY);
			}
			drawRuns(sprite, plan, x, y, color, sdf);
		} finally {
			BitmapTextRuns.copyMetrics(originalMetrics, fontData);
			BitmapTextRuns.copyMetrics(fallback.fallbackMetrics, fallback.fallbackData);
			sprite.setType(previousType);
			sprite.setColor(previousSpriteColor);
		}
	}

	/**
	 * SDF スキンで、生成した RGBA の代替字形にだけ影を描く。SDF の字形は影をシェーダーで描くが、生成字形はそれを通らない。
	 * 影のずれはシェーダーと同じくテクセル単位なので、表示の拡大率を掛ける。
	 * In SDF skins, draws shadows for generated RGBA fallback glyphs only: SDF glyphs get their shadow from the shader,
	 * which generated glyphs bypass. The offset is in texels like the shader's, so it is scaled to the display.
	 */
	private void drawGeneratedFallbackShadows(SkinObjectRenderer sprite, BitmapTextRuns plan, float x, float y,
			float scaleX, float scaleY) {
		Color shadow = getShadowColor();
		shadowDrawColor.set(shadow.r, shadow.g, shadow.b, shadow.a * color.a);
		float sx = x + getShadowOffset().x * scaleX;
		float sy = y - getShadowOffset().y * scaleY;
		for (BitmapTextRuns.Run run : plan.runs) {
			if (!isGeneratedFallbackRun(run)) {
				continue;
			}
			sprite.setType(SkinObjectRenderer.TYPE_BILINEAR);
			applyColor(run.layout, shadowDrawColor);
			sprite.draw(fallback.fallbackFont, run.layout, sx, sy, null);
		}
	}

	private boolean isGeneratedFallbackRun(BitmapTextRuns.Run run) {
		return run.fallback && run.layout.runs.size > 0 && run.layout.runs.first().glyphs.size > 0
				&& isFallbackGlyph(run.layout.runs.first().glyphs.first());
	}

	private void drawRuns(SkinObjectRenderer sprite, BitmapTextRuns plan, float x, float y, Color tint, boolean sdf) {
		for (BitmapTextRuns.Run run : plan.runs) {
			boolean distanceField = sdf && (!run.fallback || !isFallbackGlyph(run.layout.runs.first().glyphs.first()));
			sprite.setType(distanceField ? SkinObjectRenderer.TYPE_DISTANCE_FIELD : SkinObjectRenderer.TYPE_BILINEAR);
			applyColor(run.layout, tint);
			sprite.draw(run.fallback ? fallback.fallbackFont : font, run.layout, x, y,
					distanceField ? distanceFieldUniformSetter : null);
		}
	}

	private void setDistanceFieldUniforms(ShaderProgram shader) {
		shader.setUniformf("u_outlineDistance", Math.max(0.1f, 0.5f - getOutlineWidth() / 2f));
		shader.setUniformf("u_outlineColor", getOutlineColor());
		shader.setUniformf("u_shadowColor", getShadowColor());
		shader.setUniformf("u_shadowSmoothing", getShadowSmoothness() / 2f);
		float pw = source != null ? source.getPageWidth() : (regions.size > 0 && regions.get(0) != null ? regions.get(0).getRegionWidth() : 512f);
		float ph = source != null ? source.getPageHeight() : (regions.size > 0 && regions.get(0) != null ? regions.get(0).getRegionHeight() : 512f);
		shadowOffsetUniform.set(getShadowOffset().x / Math.max(1f, pw), getShadowOffset().y / Math.max(1f, ph));
		shader.setUniformf("u_shadowOffset", shadowOffsetUniform);
	}

	/**
	 * 通常描画用レイアウトを返す。条件が前回と同じなら再利用し、違えば作り直す。
	 * Returns the normal-path layout, reusing the previous one when every layout input is unchanged.
	 */
	private BitmapTextLayout updateLayout(Rectangle r) {
		updateFallbackFontParameters();
		String text = getText() != null ? getText() : "";
		float scale = size / getOriginalSize();
		BitmapTextLayout current = drawLayout;
		if (current != null && current.text.equals(text) && current.width == r.width && current.scaleY == scale
				&& current.glyphRevision == getGlyphRevision() && current.align == getAlign() && current.overflow == getOverflow()
				&& current.wrapping == isWrapping() && current.hasShadow == !getShadowOffset().isZero()) {
			return current;
		}
		drawLayout = buildLayout(text);
		return drawLayout;
	}

	private static void applyColor(GlyphLayout target, Color color) {
		int packedColor = color.toIntBits();
		for (int i = 1; i < target.colors.size; i += 2) {
			target.colors.set(i, packedColor);
		}
	}

	private void invalidateLayout() {
		drawLayout = null;
	}

	/**
	 * 元フォント自身がそのコードポイントのグリフを持つか。 / Whether the original font itself has a glyph for the code point.
	 */
	private static boolean hasNativeGlyph(BitmapFont.BitmapFontData data, int codePoint) {
		if (data == null || data.glyphs == null || codePoint < 0 || codePoint > Character.MAX_VALUE) {
			return false;
		}
		int pageIndex = codePoint / 512;
		if (pageIndex >= data.glyphs.length) {
			return false;
		}
		Glyph[] page = data.glyphs[pageIndex];
		return page != null && page[codePoint & 511] != null;
	}

	/**
	 * 計測済みレイアウトとその作成条件をまとめた値。通常描画とキャッシュ描画の両方が使う。
	 * dependencies/versions は使った文字ごとのグリフ版で、代替グリフが変わったときの無効化に使う。
	 * Measured layout plus the inputs it was built from, shared by the normal and cached draw paths.
	 * dependencies/versions record the glyph version of each used character to detect fallback changes.
	 */
	private static final class BitmapTextLayout implements CachedTextLayout {
		private final SkinTextBitmap owner;
		private final String text;
		private final BitmapTextRuns runs;
		private final float width, scaleX, scaleY;
		private final long glyphRevision;
		private final int align, overflow;
		private final boolean wrapping, hasShadow;
		private final int[] dependencies;
		private final long[] versions;
		private final long generation;
		private final boolean usesFallback;

		private BitmapTextLayout(SkinTextBitmap owner, String text, BitmapTextRuns runs,
				float width, float scaleX, float scaleY, long glyphRevision, int align, int overflow,
				boolean wrapping, boolean hasShadow) {
			this.owner = owner;
			this.text = text;
			this.runs = runs;
			this.width = width;
			this.scaleX = scaleX;
			this.scaleY = scaleY;
			this.glyphRevision = glyphRevision;
			this.align = align;
			this.overflow = overflow;
			this.wrapping = wrapping;
			this.hasShadow = hasShadow;
			boolean hasNegative = text.codePoints().anyMatch(owner.fallback.missingGlyphs::isFailed);
			// 失敗した文字は欠落記号の複製で描くので、記号の版にも依存させる。
			// Failed characters are drawn with a marker copy, so they also depend on the marker versions.
			dependencies = (hasNegative ? java.util.stream.IntStream.concat(text.codePoints(),
					java.util.Arrays.stream(MISSING_GLYPH_CANDIDATES)) : text.codePoints())
					.distinct().toArray();
			versions = new long[dependencies.length];
			boolean missing = false;
			for (int i = 0; i < dependencies.length; i++) {
				versions[i] = owner.fallback.glyphVersions.get(dependencies[i], 0L);
				missing |= dependencies[i] != '\n' && dependencies[i] != '\r' && !hasNativeGlyph(owner.fontData, dependencies[i]);
			}
			usesFallback = missing;
			generation = owner.fallback.fallbackGeneration;
		}

		private boolean dependenciesValid() {
			if (usesFallback && generation != owner.fallback.fallbackGeneration) return false;
			for (int i = 0; i < dependencies.length; i++) {
				if (versions[i] != owner.fallback.glyphVersions.get(dependencies[i], 0L)) return false;
			}
			return true;
		}

		@Override
		public float getWidth() { return width; }
	}

	/**
	 * 自分のフォント包装とアトラス貸与を解放する。共有ページは最後の借り手が返したときだけ破棄される。
	 * Releases local font wrappers and the atlas lease; shared pages retire only after their last borrower.
	 */
	public void dispose() {
		if (isDisposed()) {
			return;
		}
		setDisposed();
		// 解放に失敗・再入しても二重解放しないよう、先に参照をすべて切り離す。
		// Detach every reference first so a failing or re-entrant cleanup never releases twice.
		BitmapFont ownedFont = font;
		font = null;
		invalidateLayout();
		List<Disposable> resources = new ArrayList<>();
		if (ownedFont != null) {
			ownedFont.setOwnsTexture(false); // スキン共有ページは解放しない / never dispose shared skin pages
			resources.add(ownedFont);
		}
		fallback.detachForDispose(resources);
		FontResourceDisposer.disposeAll(resources);
	}

	/**
	 * 元フォントにない文字を描く代替の層。元フォントと分けたグリフ表({@code fallbackData})・ページ表・計測用フォントを持ち、
	 * 足りないグリフを {@link MissingGlyphMapper} で決め、スキン指定の .fnt と {@link SystemFallbackGlyphs}(FreeType)から用意する。
	 * 元フォントのグリフ・UV・ページ表は変えない。外側のスキン文字は、配置・描画でこの層のフォントとグリフ表を読むだけ。
	 * The fallback layer for characters missing from the original font. It owns a glyph table separate from the original
	 * ({@code fallbackData}), a page table and a measurement font; {@link MissingGlyphMapper} decides what is missing and
	 * glyphs come from the skin's .fnt fallbacks and {@link SystemFallbackGlyphs} (FreeType). Original glyphs, UVs and pages
	 * are never changed. The outer skin text only reads this layer's fonts and glyph table while laying out and drawing.
	 */
	private final class FallbackLayer {
		private static final int[] BASELINE_REFERENCES = { 'H', 'E', 'T', 'M', 'N', 'P', 'R', 'A' };

		private final BitmapFont.BitmapFontData fallbackData = new BitmapFont.BitmapFontData();
		private final Array<TextureRegion> drawRegions = new Array<>();
		private BitmapFont fallbackFont;
		private BitmapFont measurementFont;
		private final BitmapFont.BitmapFontData fallbackMetrics = new BitmapFont.BitmapFontData();
		private final com.badlogic.gdx.utils.IntMap<Long> glyphVersions = new com.badlogic.gdx.utils.IntMap<>();
		private long fallbackGeneration;
		private final java.util.function.Predicate<Glyph> fallbackRunSelector = this::isFallbackRunGlyph;
		/** プログラム全体の代替フォントで足りないグリフを作る側。 / Builds missing glyphs from the program-wide fallback fonts. */
		private SystemFallbackGlyphs systemFallback;
		/** アトラスに詰めたが、まだ UV を結び付けていないグリフ。 / Packed glyphs whose UV is bound after the next texture upload. */
		private final List<Glyph> pendingFallbackGlyphs = new ArrayList<>();
		private final Set<Integer> activeFallbackPages = new HashSet<>();
		/** 元フォントにない文字の代替処理(PUA 割り当て・成功/失敗の記録・グリフ版)。 / Missing-glyph handling (PUA mapping, bookkeeping, glyph revision). */
		private final MissingGlyphMapper missingGlyphs = new MissingGlyphMapper(new FallbackGlyphHost(), "Bitmap fallback glyph generation failed");

		/** 代替・計測用フォントを作り、元グリフの PUA を予約する。 / Builds the fallback and measurement fonts and reserves native PUA glyphs. */
		private void initialize(SkinFontSource fallbackSource) {
			systemFallback = new SystemFallbackGlyphs(new SystemFallbackOwner(), basePageCount, size, missingGlyphs::reportFailure);
			if (fontData != null) {
				BitmapTextRuns.copyMetrics(fontData, fallbackData);
				drawRegions.addAll(regions);
				fallbackFont = new BitmapFont(fallbackData, drawRegions, false);
				measurementFont = BitmapTextRuns.measurementFont(fontData, fallbackData, fallbackRunSelector);
				if (source != null) activeFallbackPages.addAll(source.activeFallbackPages);
				missingGlyphs.reserveNativeGlyphs(fontData);
			}
			if (fallbackSource != null) {
				systemFallback.setSource(fallbackSource, false);
			}
		}

		private boolean isFallbackPage(int page) {
			return page >= basePageCount || activeFallbackPages.contains(page);
		}

		private boolean isFallbackGlyph(Glyph glyph) {
			return glyph != null && isFallbackPage(glyph.page);
		}

		private boolean isFallbackRunGlyph(Glyph glyph) {
			return fontData.getGlyph((char) glyph.id) != glyph || isFallbackGlyph(glyph);
		}

		/**
		 * 元フォントにない文字の代替グリフを用意し、補助平面の文字を PUA スロットへ置き換えた文字列を返す。
		 * Prepares fallback glyphs for characters missing from the original font and returns the text
		 * with supplementary-plane characters replaced by their PUA slots.
		 */
		private String remap(String text) {
			if (text == null || fontData == null || isDisposed()) {
				return text;
			}
			systemFallback.refresh();
			// PUA スロットの確保やグリフ収集の前に、遅延生成の代替フォント供給元を決めておく。
			// Resolve the lazy provider before allocating PUA slots or collecting this text's glyphs.
			if (systemFallback.provider() == null && missingGlyphs.needsFallback(text)) {
				systemFallback.sourceOrDefault();
			}
			String remapped = missingGlyphs.remap(text);
			flush();
			return remapped;
		}

		/**
		 * 所有する代替グリフを登録する。元グリフとページ表は変更しない。
		 * Registers an owned fallback glyph without replacing original glyphs or changing their page table.
		 */
		private void register(int codePoint, Glyph glyph) {
			if (fontData != null && glyph != null && !hasNativeGlyph(fontData, codePoint)) {
				fallbackData.setGlyph(codePoint, glyph);
				missingGlyphs.markActive(codePoint);
				glyphVersions.put(codePoint, glyphVersions.get(codePoint, 0L) + 1);
				if (isMissingGlyphCandidate(codePoint)) {
					// 失敗済みの文字は欠落記号の複製を持つだけなので、記号が変わったら捨てて再解決させる。
					// Failed entries only hold copies of the marker, so drop them when the marker changes.
					for (int missing : missingGlyphs.failedCodePoints()) {
						if (missing <= Character.MAX_VALUE) fallbackData.setGlyph(missing, null);
					}
					missingGlyphs.failedCodePoints().clear();
				}
				if (isFallbackPage(glyph.page)) {
					activeFallbackPages.add(glyph.page);
				}
				missingGlyphs.bumpRevision();
				invalidateLayout();
			}
		}

		private static boolean isMissingGlyphCandidate(int codePoint) {
			for (int candidate : MISSING_GLYPH_CANDIDATES) {
				if (candidate == codePoint) return true;
			}
			return false;
		}

		/**
		 * 全体の代替フォントでグリフを作る。アトラスに詰めたグリフは、次のテクスチャ反映で UV を結び付ける。
		 * Builds a glyph with the program-wide fallback fonts; packed glyphs get their UV bound at the next texture upload.
		 */
		private Glyph createFallbackGlyph(int codePoint, int mappedCodePoint, boolean fallbackOnly) {
			Glyph glyph = systemFallback.create(codePoint, mappedCodePoint, fallbackOnly);
			if (glyph != null && glyph.width > 0 && glyph.page >= basePageCount) {
				activeFallbackPages.add(glyph.page);
				pendingFallbackGlyphs.add(glyph);
			}
			return glyph;
		}

		/**
		 * 詰めたグリフをテクスチャへ反映し、新しいページの領域と UV を描画用データに結び付ける。GL がなければ何もしない。
		 * Uploads packed glyphs and binds new page regions and UVs to the draw data; no-op without a GL context.
		 */
		private void flush() {
			Array<TextureRegion> atlasPages = systemFallback.flush();
			if (atlasPages == null) {
				return;
			}
			boolean pageAdded = false;
			for (int i = 0; i < atlasPages.size; i++) {
				int targetPage = basePageCount + i;
				while (drawRegions.size <= targetPage) {
					drawRegions.add(null);
					pageAdded = true;
				}
				drawRegions.set(targetPage, atlasPages.get(i));
			}
			if (pageAdded && fontData != null) {
				fallbackFont = new BitmapFont(fallbackData, drawRegions, false);
			}
			if (!pendingFallbackGlyphs.isEmpty()) {
				for (Glyph g : pendingFallbackGlyphs) {
					if (g.page >= 0 && g.page < drawRegions.size && drawRegions.get(g.page) != null) {
						fallbackData.setGlyphRegion(g, drawRegions.get(g.page));
					}
				}
				pendingFallbackGlyphs.clear();
			}
		}

		/** 成功・失敗両方の代替結果を無効にする。 / Invalidates both successful and failed fallback results. */
		private void reset() {
			fallbackGeneration++;
			java.util.Arrays.fill(fallbackData.glyphs, null);
			missingGlyphs.reset();
			if (fontData != null) missingGlyphs.reserveNativeGlyphs(fontData);
			try {
				FontResourceDisposer.disposeAll(detachAtlas());
			} finally {
				if (source != null) activeFallbackPages.addAll(source.activeFallbackPages);
				missingGlyphs.bumpRevision();
				invalidateLayout();
			}
		}

		/**
		 * 代替アトラスへの参照と描画ページを切り離し、後で返却すべき貸与を返す。
		 * Detaches the fallback atlas and its draw pages, returning the lease to release afterwards.
		 */
		private List<Disposable> detachAtlas() {
			List<Disposable> resources = systemFallback.detach();
			pendingFallbackGlyphs.clear();
			activeFallbackPages.clear();
			drawRegions.truncate(basePageCount);
			return resources;
		}

		/** 解放対象(代替・計測用フォント、アトラス貸与、所有する供給元)を順に渡す。 / Hands over everything to release, in order. */
		private void detachForDispose(List<Disposable> resources) {
			if (fallbackFont != null) {
				fallbackFont.setOwnsTexture(false);
				resources.add(fallbackFont);
				fallbackFont = null;
			}
			if (measurementFont != null) {
				measurementFont.setOwnsTexture(false);
				resources.add(measurementFont);
				measurementFont = null;
			}
			resources.addAll(detachAtlas());
			resources.addAll(systemFallback.close());
			missingGlyphs.reset();
		}

		private static Glyph retarget(Glyph glyph, int codePoint) {
			Glyph copy = BitmapFontDataCopy.copyGlyph(glyph);
			copy.id = codePoint;
			return copy;
		}

		/**
		 * ビットマップ版の代替グリフの探し方と登録先。代替グリフは元フォントと分けた fallbackData に置く。
		 * Bitmap-side lookup and install target for fallback glyphs; they live in fallbackData, apart from the original font.
		 */
		private final class FallbackGlyphHost implements MissingGlyphMapper.Host {
			@Override
			public boolean hasNativeGlyph(int codePoint) {
				return SkinTextBitmap.hasNativeGlyph(fontData, codePoint);
			}

			/**
			 * 探索順: スキン指定 .fnt → (BMP のみ)同等文字 → FreeType(スキン指定の TTF 等、続いて共通代替)。
			 * Order: skin .fnt fallbacks → equivalent character (BMP only) → FreeType (skin-specified vector fonts, then the global fallbacks).
			 */
			@Override
			public Glyph resolveFallbackGlyph(int codePoint, int mappedCodePoint, boolean supplementary) {
				Glyph glyph = null;
				if (source != null) {
					glyph = source.findFallbackGlyph(codePoint, mappedCodePoint, supplementary);
					if (glyph == null && !supplementary) {
						glyph = source.findEquivalentBmpGlyph(codePoint, mappedCodePoint);
					}
				}
				return glyph != null ? glyph : createFallbackGlyph(codePoint, mappedCodePoint, false);
			}

			/** スキンの .fnt にある欠落記号。 / The missing marker found in the skin's .fnt files. */
			@Override
			public Glyph resolveMarkerGlyph(int mappedCodePoint) {
				return source != null ? source.findMissingGlyph(mappedCodePoint) : null;
			}

			/**
			 * 解決できない BMP 文字は欠落記号の複製で表示する。複製は記号の UV をそのまま持つので結び直さない。
			 * Unresolved BMP characters are shown with a copy of the marker; the copy keeps the marker's UV, so it is not rebound.
			 */
			@Override
			public boolean installUnresolved(int codePoint) {
				Glyph glyph = copyMissingMarker(codePoint);
				if (glyph == null) {
					return false;
				}
				fallbackData.setGlyph(codePoint, glyph);
				if (isFallbackPage(glyph.page)) {
					activeFallbackPages.add(glyph.page);
				}
				return true;
			}

			@Override
			public boolean hasDrawableGlyph(int codePoint) {
				return measurementFont.getData().getGlyph((char) codePoint) != null;
			}

			/** ページの領域があれば UV もここで結び付ける。 / Binds the UV here when the page region already exists. */
			@Override
			public void installGlyph(int key, Glyph glyph) {
				if (glyph.page >= 0 && glyph.page < drawRegions.size && drawRegions.get(glyph.page) != null) {
					fallbackData.setGlyphRegion(glyph, drawRegions.get(glyph.page));
				}
				fallbackData.setGlyph(key, glyph);
				if (isFallbackPage(glyph.page)) {
					activeFallbackPages.add(glyph.page);
				}
			}

			@Override
			public void removeGlyph(int slot, int evictedCodePoint) {
				fallbackData.setGlyph(slot, null);
				glyphVersions.put(evictedCodePoint, glyphVersions.get(evictedCodePoint, 0L) + 1);
			}

			/**
			 * 欠落記号 U+25A1、次に ? の順で用意し、その複製を codePoint 用に返す。
			 * Prepares the U+25A1 marker, then ?, and returns a copy re-targeted to codePoint.
			 */
			private Glyph copyMissingMarker(int codePoint) {
				for (char marker : new char[] { '\u25A1', '?' }) {
					if (codePoint == marker) continue;
					missingGlyphs.ensureBmp(marker);
					Glyph g = measurementFont.getData().getGlyph(marker);
					if (g != null) {
						return retarget(g, codePoint);
					}
				}
				return fallbackData.missingGlyph != null ? retarget(fallbackData.missingGlyph, codePoint) : null;
			}
		}

		/**
		 * 全体の代替フォントで作るグリフを、このスキン文字に合わせる部分(スタイル・基準線・破棄)。
		 * The part that fits glyphs built from the program-wide fallback fonts to this skin text (style, baseline, dropping).
		 */
		private final class SystemFallbackOwner implements SystemFallbackGlyphs.Owner {
			@Override
			public void fillStyle(FreeTypeFontGenerator.FreeTypeFontParameter param) {
				param.borderWidth = 0;
				param.borderColor = Color.BLACK;
				param.shadowOffsetX = param.shadowOffsetY = 0;
				param.shadowColor = Color.BLACK;
				param.size = (int) Math.max(1, getOriginalSize());

				// 1. 縁取りはスキン文字の outline 設定に従う / Outline follows the skin text's outline settings
				Color outlineC = getOutlineColor() != null ? getOutlineColor() : Color.BLACK;
				if (getOutlineWidth() > 0) {
					param.borderWidth = Math.max(1f, getOutlineWidth());
					param.borderColor = outlineC;
				} else if (isDistanceField()) {
					// SDF スキンでは代替グリフだけが RGBA で描かれるため、縁取りを焼き込んで暗い背景でも見えるようにする。
					// On SDF skins only fallback glyphs are drawn as RGBA, so bake an outline to keep them visible on dark backgrounds.
					param.borderWidth = Math.max(1f, Math.round(param.size * 0.05f));
					param.borderColor = outlineC;
				}

				// 2. 影はスキン文字の shadow 設定に従う / Shadow follows the skin text's shadow settings
				Vector2 shadowOff = getShadowOffset();
				if (shadowOff != null && !shadowOff.isZero()) {
					param.shadowOffsetX = (int) shadowOff.x;
					param.shadowOffsetY = (int) shadowOff.y;
					param.shadowColor = getShadowColor() != null ? getShadowColor() : Color.DARK_GRAY;
				}

				// 3. フィルタ / Filtering
				param.minFilter = Texture.TextureFilter.Linear;
				param.magFilter = Texture.TextureFilter.Linear;
			}

			/**
			 * 元フォントと代替フォントで基準文字(H, E, T…)を比べ、代替グリフに加える縦ずれを求める。
			 * Compares reference letters (H, E, T…) in both fonts to find the vertical offset applied to fallback glyphs.
			 */
			@Override
			public int baselineOffset(FreeTypeFontGenerator generator, FreeTypeFontGenerator.FreeTypeFontParameter style) {
				if (fontData == null) {
					return 0;
				}
				float scaleY = fontData.scaleY != 0f ? fontData.scaleY : 1f;
				int padBottom = Math.round(fontData.padBottom / scaleY);
				for (int codePoint : BASELINE_REFERENCES) {
					Glyph nativeGlyph = fontData.getGlyph((char) codePoint);
					if (nativeGlyph == null || nativeGlyph.height <= 0 || missingGlyphs.isActive(codePoint)
							|| missingGlyphs.isFailed(codePoint) || isFallbackGlyph(nativeGlyph)) {
						continue;
					}
					try {
						FreeTypeFontGenerator.GlyphAndBitmap reference = generator.generateGlyphAndBitmap(
								codePoint, style.size, style.flip);
						if (reference != null && reference.glyph != null && reference.glyph.height > 0) {
							return nativeGlyph.yoffset + padBottom - reference.glyph.yoffset;
						}
					} catch (RuntimeException ignored) {
						// 次の基準文字を試す。 / Try the next reference letter.
					}
				}
				return 0;
			}

			@Override
			public void invalidate() {
				reset();
			}
		}
	}

	/**
	 * スキンの代替指定が .fnt(ビットマップ)か。代替は形式ごとに分けて使う: .fnt の文字は元フォント → .fnt の代替(宣言順)→
	 * TTF/OTF の代替(宣言順)→ 共通代替 → 欠落記号。TTF の文字は TTF/OTF の代替だけを使い、.fnt の代替は使わない。
	 * Whether a skin fallback entry is a .fnt (bitmap) font. Fallbacks are used per format: .fnt text tries the original font,
	 * then the .fnt fallbacks (declaration order), then the TTF/OTF fallbacks (declaration order), then the global fallbacks,
	 * then the missing marker. TTF text uses only TTF/OTF fallbacks and never .fnt ones.
	 */
	public static boolean isBitmapFontPath(String path) {
		return path != null && path.toLowerCase(java.util.Locale.ROOT).endsWith(".fnt");
	}

	/**
	 * .fnt ファイル由来の元フォント。BitmapFontCache から借りたページに、スキン指定の .fnt 代替ページを後ろに連結する。
	 * Original font loaded from a .fnt file. Pages borrowed from BitmapFontCache are followed by the skin's .fnt fallback pages.
	 */
	public static final class SkinTextBitmapSource implements Disposable {

		public static final int TYPE_STANDARD = 0;
		public static final int TYPE_DISTANCE_FIELD = 1;
		public static final int TYPE_COLORED_DISTANCE_FIELD = 2;

		private boolean usecim;
		private boolean useMipMaps;
		private Path fontPath;
		private FallbackFont[] fallbackFonts = new FallbackFont[0];
		private BitmapFont.BitmapFontData fontData;
		private Array<TextureRegion> regions;
		private BitmapFont font;
		private float originalSize;
		private int type;
		private float pageWidth;
		private float pageHeight;
		private int glyphYOffset;
		private Map<Integer, BitmapFont.Glyph> supplementaryGlyphs = Collections.emptyMap();
		/** 代替 .fnt ごとの、連結後ページ番号の開始位置。 / First concatenated page index of each fallback .fnt. */
		private final Map<FallbackFont, Integer> fallbackPageOffsets = new HashMap<>();
		/** RGBA で描くべき(SDF でない)代替ページ。 / Fallback pages that must be drawn as RGBA (not SDF). */
		private final Set<Integer> activeFallbackPages = new HashSet<>();
		private final Set<FallbackFont> acquiredFallbackFonts = new HashSet<>();
		private boolean primaryFontAcquired;
		private boolean disposed;

		private static final int WAVE_DASH = 0x301c;
		private static final int FULLWIDTH_TILDE = 0xff5e;

		public SkinTextBitmapSource(Path fontPath, boolean usecim) {
			this(fontPath, usecim, true);
		}

		public SkinTextBitmapSource(Path fontPath, boolean usecim, boolean useMipMaps) {
			this(fontPath, new FallbackFont[0], usecim, useMipMaps);
		}

		public SkinTextBitmapSource(Path fontPath, Path[] fallbackFontPaths, boolean usecim) {
			this(fontPath, fallbackFontPaths, usecim, true);
		}

		public SkinTextBitmapSource(Path fontPath, Path[] fallbackFontPaths, boolean usecim, boolean useMipMaps) {
			this(fontPath, toFallbackFonts(fallbackFontPaths), usecim, useMipMaps);
		}

		public SkinTextBitmapSource(Path fontPath, FallbackFont[] fallbackFonts, boolean usecim) {
			this(fontPath, fallbackFonts, usecim, true);
		}

		public SkinTextBitmapSource(Path fontPath, FallbackFont[] fallbackFonts, boolean usecim, boolean useMipMaps) {
			this.usecim = usecim;
			this.useMipMaps = useMipMaps;
			this.fontPath = fontPath;
			this.fallbackFonts = fallbackFonts != null ? fallbackFonts : new FallbackFont[0];
		}

		public SkinTextBitmapSource(BitmapFont.BitmapFontData fontData, Array<TextureRegion> regions, float originalSize, int type) {
			this.fontData = BitmapFontDataCopy.copyFontData(fontData);
			this.regions = regions;
			this.originalSize = originalSize;
			this.type = type;
			this.font = this.fontData != null ? new BitmapFont(this.fontData, regions, false) : null;
			if (this.fontData != null) {
				this.glyphYOffset = getRepresentativeGlyphYOffset(this.fontData);
			}
		}

		public CacheableBitmapFont createCacheableFont(Path fontPath, int type) {
			return BitmapFontLoader.load(fontPath, type, usecim, useMipMaps);
		}

		/**
		 * 初回呼び出しでキャッシュから元フォントを借り、作業用に複製し、代替 .fnt のページを連結する。
		 * On first call borrows the original font from the cache, copies it for local use and appends fallback .fnt pages.
		 */
		public BitmapFont getFont() {
			if (disposed) {
				return null;
			}
			if (font != null) {
				return font;
			}
			if (fontPath == null) {
				return null;
			}
			CacheableBitmapFont cached = getPrimaryCacheableFont();

			fontData = cached != null ? BitmapFontDataCopy.copyFontData(cached.fontData) : null;
			regions = cached != null && cached.regions != null ? new Array<>(cached.regions) : new Array<>();
			originalSize = cached != null ? cached.originalSize : 0;
			type = cached != null ? cached.type : 0;
			pageWidth = cached != null ? cached.pageWidth : 0;
			pageHeight = cached != null ? cached.pageHeight : 0;
			glyphYOffset = cached != null ? cached.glyphYOffset : 0;
			supplementaryGlyphs = cached != null && cached.supplementaryGlyphs != null ? cached.supplementaryGlyphs : Collections.emptyMap();
			fallbackPageOffsets.clear();
			activeFallbackPages.clear();
			attachFallbackFonts();
			font = fontData != null ? new BitmapFont(fontData, regions, false) : null;

			return font;
		}

		BitmapFont.BitmapFontData getFontData() {
			if (fontData == null && !disposed) {
				getFont();
			}
			return fontData;
		}

		Array<TextureRegion> getRegions() {
			if (regions == null && !disposed) {
				getFont();
			}
			return regions;
		}

		private CacheableBitmapFont getPrimaryCacheableFont() {
			if (!primaryFontAcquired) {
				primaryFontAcquired = true;
				return BitmapFontCache.acquire(fontPath, type, () -> createCacheableFont(fontPath, type));
			}
			return BitmapFontCache.Get(fontPath, type);
		}

		private void attachFallbackFonts() {
			for (FallbackFont fallback : fallbackFonts) {
				if (fallback == null || fallback.path == null || fallbackPageOffsets.containsKey(fallback)) {
					continue;
				}
				CacheableBitmapFont fallbackFont = getFallbackCacheableFont(fallback);
				if (fallbackFont == null || fallbackFont.fontData == null || fallbackFont.regions == null) {
					continue;
				}
				int pageOffset = regions.size;
				int pageCount = 0;
				for (TextureRegion region : fallbackFont.regions) {
					regions.add(region);
					if (fallback.type == TYPE_STANDARD) {
						activeFallbackPages.add(pageOffset + pageCount);
					}
					pageCount++;
				}
				if (pageCount == 0) {
					continue;
				}
				fallbackPageOffsets.put(fallback, pageOffset);
			}
		}

		private CacheableBitmapFont getFallbackCacheableFont(FallbackFont fallback) {
			if (acquiredFallbackFonts.add(fallback)) {
				return BitmapFontCache.acquire(fallback.path, fallback.type,
						() -> createCacheableFont(fallback.path, fallback.type));
			}
			return BitmapFontCache.Get(fallback.path, fallback.type);
		}

		/**
		 * 欠落記号(□ など)を元フォント、次に代替 .fnt から探し、mappedCodePoint 用の複製を返す。
		 * Finds a missing-glyph marker (□ etc.) in the original font, then in fallback .fnt files, copied for mappedCodePoint.
		 */
		BitmapFont.Glyph findMissingGlyph(int mappedCodePoint) {
			for (int candidate : MISSING_GLYPH_CANDIDATES) {
				BitmapFont.Glyph glyph = fontData.getGlyph((char) candidate);
				if (glyph != null && hasRegion(glyph.page)) {
					return copyGlyph(glyph, mappedCodePoint, 0, 0);
				}
			}
			return findInFallbacks(MISSING_GLYPH_CANDIDATES, false, mappedCodePoint);
		}

		/**
		 * 波ダッシュ(U+301C)と全角チルダ(U+FF5E)のような同等文字を探す。
		 * Finds an equivalent character such as WAVE DASH (U+301C) for FULLWIDTH TILDE (U+FF5E) and vice versa.
		 */
		BitmapFont.Glyph findEquivalentBmpGlyph(int codePoint, int mappedCodePoint) {
			int equivalentCodePoint = getEquivalentBmpCodePoint(codePoint);
			if (equivalentCodePoint == -1) {
				return null;
			}
			BitmapFont.Glyph glyph = fontData.getGlyph((char) equivalentCodePoint);
			if (glyph != null && hasRegion(glyph.page)) {
				return copyGlyph(glyph, mappedCodePoint, 0, 0);
			}
			return findInFallbacks(new int[] { equivalentCodePoint }, false, mappedCodePoint);
		}

		private int getEquivalentBmpCodePoint(int codePoint) {
			if (codePoint == WAVE_DASH) {
				return FULLWIDTH_TILDE;
			}
			if (codePoint == FULLWIDTH_TILDE) {
				return WAVE_DASH;
			}
			return -1;
		}

		/**
		 * スキン指定の代替 .fnt から文字を探す。supplementary が真なら補助平面の表を使う。
		 * Finds a character in the skin's fallback .fnt files; uses the supplementary-plane table when supplementary is true.
		 */
		BitmapFont.Glyph findFallbackGlyph(int codePoint, int mappedCodePoint, boolean supplementary) {
			if (supplementary) {
				BitmapFont.Glyph glyph = supplementaryGlyphs.get(codePoint);
				if (glyph != null && hasRegion(glyph.page)) {
					return copyGlyph(glyph, mappedCodePoint, 0, 0);
				}
			}
			return findInFallbacks(new int[] { codePoint }, supplementary, mappedCodePoint);
		}

		/**
		 * 代替 .fnt を宣言順に調べ、候補のうち最初に描けるグリフを、連結後のページ番号と基準線補正付きで複製する。
		 * Scans fallback .fnt files in declaration order and copies the first drawable candidate,
		 * shifted to its concatenated page and adjusted to the original baseline.
		 */
		private BitmapFont.Glyph findInFallbacks(int[] candidates, boolean supplementary, int mappedCodePoint) {
			for (FallbackFont fallback : fallbackFonts) {
				if (fallback == null || fallback.path == null) {
					continue;
				}
				CacheableBitmapFont fallbackFont = BitmapFontCache.Get(fallback.path, fallback.type);
				Integer pageOffset = fallbackPageOffsets.get(fallback);
				if (fallbackFont == null || fallbackFont.fontData == null || fallbackFont.regions == null || pageOffset == null) {
					continue;
				}
				for (int candidate : candidates) {
					BitmapFont.Glyph glyph = supplementary
							? (fallbackFont.supplementaryGlyphs != null ? fallbackFont.supplementaryGlyphs.get(candidate) : null)
							: fallbackFont.fontData.getGlyph((char) candidate);
					if (glyph != null && glyph.page >= 0 && glyph.page < fallbackFont.regions.size
							&& hasRegion(glyph.page + pageOffset.intValue())) {
						return copyGlyph(glyph, mappedCodePoint, pageOffset.intValue(), fallbackFont.glyphYOffset - glyphYOffset);
					}
				}
			}
			return null;
		}

		private boolean hasRegion(int page) {
			return page >= 0 && page < regions.size && regions.get(page) != null;
		}

		/**
		 * 位置と送り幅だけを複製し、ページ番号と縦位置を補正する。UV は登録時に付け直す。
		 * Copies position and advance only, shifting the page and y offset; UVs are rebound on registration.
		 */
		private BitmapFont.Glyph copyGlyph(BitmapFont.Glyph source, int mappedCodePoint, int pageOffset, int yoffsetAdjust) {
			BitmapFont.Glyph glyph = new BitmapFont.Glyph();
			glyph.id = mappedCodePoint;
			glyph.srcX = source.srcX;
			glyph.srcY = source.srcY;
			glyph.width = source.width;
			glyph.height = source.height;
			glyph.xoffset = source.xoffset;
			glyph.yoffset = source.yoffset + yoffsetAdjust;
			glyph.xadvance = source.xadvance;
			glyph.page = source.page + pageOffset;
			return glyph;
		}

		static int getRepresentativeGlyphYOffset(BitmapFont.BitmapFontData fontData) {
			return BitmapFontLoader.getRepresentativeGlyphYOffset(fontData);
		}

		private static FallbackFont[] toFallbackFonts(Path[] fallbackFontPaths) {
			if (fallbackFontPaths == null) {
				return new FallbackFont[0];
			}
			FallbackFont[] fallbackFonts = new FallbackFont[fallbackFontPaths.length];
			for (int i = 0; i < fallbackFontPaths.length; i++) {
				fallbackFonts[i] = new FallbackFont(fallbackFontPaths[i], TYPE_STANDARD);
			}
			return fallbackFonts;
		}

		public float getOriginalSize() {
			return originalSize;
		}

		int getGlyphYOffset() {
			if (fontData == null && !disposed) {
				getFont();
			}
			return glyphYOffset;
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		public float getPageWidth() {
			return pageWidth;
		}

		public float getPageHeight() {
			return pageHeight;
		}

		@Override
		public void dispose() {
			if (disposed) {
				return;
			}
			disposed = true;
			font = null;
			if (primaryFontAcquired) {
				BitmapFontCache.release(fontPath, type);
			}
			for (FallbackFont fallback : acquiredFallbackFonts) {
				BitmapFontCache.release(fallback.path, fallback.type);
			}
			acquiredFallbackFonts.clear();
		}

		public static final class FallbackFont {
			private final Path path;
			private final int type;

			public FallbackFont(Path path, int type) {
				this.path = path;
				this.type = type;
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj) {
					return true;
				}
				if (!(obj instanceof FallbackFont)) {
					return false;
				}
				FallbackFont other = (FallbackFont) obj;
				return type == other.type && path.equals(other.path);
			}

			@Override
			public int hashCode() {
				return 31 * path.hashCode() + type;
			}
		}
	}

	/** 元と代替の両方をまとめて計測し、その共通位置を別々の描画 run に保つ。 / Measures both sources together, then retains their common positions in separate draw runs. */
	static final class BitmapTextRuns {

		static final class Run {
			final boolean fallback;
			final GlyphLayout layout = new GlyphLayout();

			Run(boolean fallback) {
				this.fallback = fallback;
			}
		}

		final List<Run> runs = new ArrayList<>();

		BitmapTextRuns(GlyphLayout measured, BitmapFontData original) {
			this(measured, glyph -> original.getGlyph((char) glyph.id) != glyph);
		}

		BitmapTextRuns(GlyphLayout measured, Predicate<Glyph> isFallback) {
			int glyphIndex = 0;
			int colorIndex = 0;
			int packedColor = measured.colors.size > 1 ? measured.colors.get(1)
					: com.badlogic.gdx.graphics.Color.WHITE.toIntBits();
			for (GlyphRun line : measured.runs) {
				float pen = line.x;
				Run part = null;
				GlyphRun target = null;
				for (int i = 0; i < line.glyphs.size; i++) {
					while (colorIndex + 3 < measured.colors.size
							&& measured.colors.get(colorIndex + 2) <= glyphIndex) {
						colorIndex += 2;
						packedColor = measured.colors.get(colorIndex + 1);
					}
					Glyph glyph = line.glyphs.get(i);
					pen += line.xAdvances.get(i);
					boolean fallback = isFallback.test(glyph);
					if (part == null || part.fallback != fallback
							|| (fallback && target.glyphs.peek().page != glyph.page)) {
						if (target != null) target.xAdvances.add(0);
						part = new Run(fallback);
						runs.add(part);
						target = new GlyphRun();
						target.x = pen;
						target.y = line.y;
						part.layout.runs.add(target);
						// libGDX 1.14 では色の情報は GlyphRun ではなく GlyphLayout が持つ。 / libGDX 1.14 stores packed colours on the layout, not GlyphRun.
						part.layout.colors.add(0, packedColor);
						part.layout.width = measured.width;
						part.layout.height = measured.height;
						target.xAdvances.add(0);
					} else {
						target.xAdvances.add(line.xAdvances.get(i));
						if (part.layout.colors.peek() != packedColor)
							part.layout.colors.add(part.layout.glyphCount, packedColor);
					}
					target.glyphs.add(glyph);
					part.layout.glyphCount++;
					glyphIndex++;
				}
				if (target != null) target.xAdvances.add(line.xAdvances.peek());
			}
		}

		/** 配置計算専用。元フォントにグリフやページを追加しない。このフォントで描画しないこと。 / Layout-only lookup: no glyphs or pages are inserted into the original font. Never draw this font. */
		static BitmapFont measurementFont(BitmapFontData original, BitmapFontData fallback) {
			return measurementFont(original, fallback, glyph -> original.getGlyph((char) glyph.id) != glyph);
		}

		static BitmapFont measurementFont(BitmapFontData original, BitmapFontData fallback, Predicate<Glyph> isFallback) {
			BitmapFontData data = new BitmapFontData() {
				@Override
				public Glyph getGlyph(char ch) {
					Glyph glyph = original.getGlyph(ch);
					return glyph != null ? glyph : fallback.getGlyph(ch);
				}

				@Override
				public void getGlyphs(GlyphRun run, CharSequence text, int start, int end, Glyph lastGlyph) {
					super.getGlyphs(run, text, start, end, lastGlyph);
					// カーニング表は書体ごとのもの。異なる供給元の境界をまたいで適用しない。 / Kerning tables belong to a face; never apply one across source boundaries.
					Glyph previous = lastGlyph;
					for (int i = 0; i < run.glyphs.size; i++) {
						Glyph glyph = run.glyphs.get(i);
						if (previous != null && isOriginal(previous) != isOriginal(glyph)) {
							run.xAdvances.set(i, previous.xadvance * scaleX);
						}
						previous = glyph;
					}
				}

				private boolean isOriginal(Glyph glyph) {
					return !isFallback.test(glyph);
				}
			};
			copyMetrics(original, data);
			Array<TextureRegion> unused = new Array<>();
			unused.add(new TextureRegion());
			return new BitmapFont(data, unused, false);
		}

		static void copyMetrics(BitmapFontData from, BitmapFontData to) {
			to.flipped = from.flipped;
			to.padTop = from.padTop;
			to.padRight = from.padRight;
			to.padBottom = from.padBottom;
			to.padLeft = from.padLeft;
			to.lineHeight = from.lineHeight;
			to.capHeight = from.capHeight;
			to.ascent = from.ascent;
			to.descent = from.descent;
			to.down = from.down;
			to.blankLineScale = from.blankLineScale;
			to.scaleX = from.scaleX;
			to.scaleY = from.scaleY;
			to.markupEnabled = from.markupEnabled;
			to.cursorX = from.cursorX;
			to.spaceXadvance = from.spaceXadvance;
			to.xHeight = from.xHeight;
			to.breakChars = from.breakChars;
		}
	}
}
