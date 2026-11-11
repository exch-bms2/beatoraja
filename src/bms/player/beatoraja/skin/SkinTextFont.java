package bms.player.beatoraja.skin;

import bms.player.beatoraja.skin.property.StringProperty;
import bms.player.beatoraja.skin.property.StringPropertyFactory;

import java.util.logging.Logger;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;

import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import bms.player.beatoraja.skin.MissingGlyphMapper.PuaSlotAllocator;
import bms.player.beatoraja.skin.FontRenderRetirement.FontResourceDisposer;

/**
 * フォントデータをソースとして持つスキン用テキスト
 * 
 * @author exch
 */
public final class SkinTextFont extends SkinText {

    /**
     * ビットマップフォント
     */
    private BitmapFont font;
    /** 生成したフォントと専用packerの所有者。本クラスは参照だけ持つ */
    private FontSession session;

    private GlyphLayout layout;
    private final GlyphLayout shadowLayout = new GlyphLayout();

    private final SkinFontSource source;
    private final boolean ownsSource;
    private FreeTypeFontGenerator.FreeTypeFontParameter parameter;
    private GlyphProvider glyphProvider;
    private String preparedFonts;
    /** 元フォントにない文字の代替処理(PUA 割り当て・成功/失敗の記録・グリフ版)。 / Missing-glyph handling (PUA mapping, bookkeeping, glyph revision). */
    private final MissingGlyphMapper missingGlyphs = new MissingGlyphMapper(new FallbackGlyphHost(), "Glyph generation failed");
    
    private final Color shadowcolor = new Color();
    private String layoutText;
    private float layoutWidth = Float.NaN;
    private float layoutScaleY = Float.NaN;
    private float layoutScaleX;
    private int layoutAlign;
    private int layoutOverflow;
    private boolean layoutWrapping;
    private boolean layoutHasShadow;
    private boolean layoutValid;
    private long layoutGlyphRevision = -1;

    public SkinTextFont(String fontpath, int cycle, int size, int shadow) {
        this(fontpath, new String[0], cycle, size, shadow, StringPropertyFactory.getStringProperty(-1));
    }

    public SkinTextFont(String fontpath, int cycle, int size, int shadow, StringProperty property) {
        this(fontpath, new String[0], cycle, size, shadow, property);
    }

    public SkinTextFont(String fontpath, String[] fallbackFontPaths, int cycle, int size, int shadow, StringProperty property) {
        this(new SkinFontSource(fontpath, fallbackFontPaths), cycle, size, shadow, property, true);
    }

    public SkinTextFont(SkinFontSource source, int cycle, int size, int shadow, StringProperty property) {
        this(source, cycle, size, shadow, property, false);
    }

    private SkinTextFont(SkinFontSource source, int cycle, int size, int shadow, StringProperty property,
            boolean ownsSource) {
        super(property);
        this.source = source;
        this.ownsSource = ownsSource;
        parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.characters = "";
        glyphProvider = new GlyphProvider(source, parameter, missingGlyphs::reportFailure, true);
        parameter.incremental = false;
//      this.setCycle(cycle);
        parameter.size = size;
        setShadowOffset(new Vector2(shadow, shadow));
    }
    
    public boolean validate() {
        if (!source.isAvailable()) {
            return false;
        }
        return super.validate();
    }

    @Override
    public boolean requiresFontPreparation() {
        return true;
    }

    public void prepareFont(String text) {
        if (isDisposed() || text == null) {
            return;
        }
        text = normalizeDisplay(text);
        replaceSession(text, true);
    }    

	@Override
	protected void prepareText(String text) {
        if (isDisposed()) {
            return;
        }
        invalidateLayout();
        if(preparedFonts != null && session != null && session.isLive()) {
        	return;
        }
        replaceSession(preparedFonts != null ? preparedFonts : text, preparedFonts != null);
    }

    /**
     * 公開前に完全に作り終える。準備に失敗したら直前の READY セッションと対応表を保つ。
     * Build completely before publishing. Preparation failures preserve the previous READY session and mappings.
     */
    private void replaceSession(String text, boolean prepared) {
        FreeTypeFontGenerator generator = source.getGenerator();
        if (generator == null) {
            return;
        }
        String previousCharacters = parameter.characters;
        com.badlogic.gdx.graphics.g2d.PixmapPacker previousPacker = parameter.packer;
        FontSession candidate = null;
        GlyphLayout candidateLayout;
        try {
            parameter.characters = toBmpCharacters(text);
            candidate = FontSession.open(generator, parameter, session);
            disableMissingGlyph(candidate.font());
            candidateLayout = new GlyphLayout(candidate.font(), "");
        } catch (RuntimeException e) {
            parameter.characters = previousCharacters;
            parameter.packer = previousPacker;
            if (candidate != null) {
                try { candidate.dispose(); } catch (RuntimeException cleanup) { e.addSuppressed(cleanup); }
            }
            if (!(e instanceof GdxRuntimeException)) throw e;
            Logger.getGlobal().warning("Font準備失敗 : " + text + " - " + e.getMessage());
            return;
        }
        FontSession previous = session;
        session = candidate;
        font = candidate.font();
        layout = candidateLayout;
        if (prepared) preparedFonts = text;
        clearSupplementaryGlyphs();
        missingGlyphs.reserveNativeGlyphs(font.getData());
        missingGlyphs.bumpRevision();
        invalidateLayout();
        if (previous != null) previous.dispose();
    }
	
	@Override
    public void draw(SkinObjectRenderer sprite, float offsetX, float offsetY) {
        recoverSession();
        if (font == null || session == null || !session.isLive()) return;
        try (FontScaleSnapshot ignored = new FontScaleSnapshot(font.getData())) {
            drawScaled(sprite, offsetX, offsetY);
        }
    }

    private void drawScaled(SkinObjectRenderer sprite, float offsetX, float offsetY) {
        if (font == null || layout == null) {
            return;
        }

        updateLayout(region);
        font.getData().setScale(layoutScaleX, layoutScaleY);
        sprite.setType(getFilter() != 0 ? SkinObjectRenderer.TYPE_LINEAR : SkinObjectRenderer.TYPE_NORMAL);

        final float x = getAlign() == 2 ? region.x - region.width : getAlign() == 1 ? region.x - region.width / 2 : region.x;
        if (layoutHasShadow) {
            shadowcolor.set(color.r / 2, color.g / 2, color.b / 2, color.a);
            applyColor(shadowLayout, shadowcolor);
            sprite.draw(font, shadowLayout, x + getShadowOffset().x + offsetX,
                    region.y - getShadowOffset().y + offsetY + region.getHeight());
        }
        applyColor(layout, color);
        sprite.draw(font, layout, x + offsetX, region.y + offsetY + region.getHeight());
    }

    @Override
    public CachedTextLayout createCachedTextLayout(String text) {
        recoverSession();
        if (font == null || session == null || !session.isLive()) return null;
        try (FontScaleSnapshot ignored = new FontScaleSnapshot(font.getData())) {
            return createScaledTextLayout(text);
        }
    }

    private CachedTextLayout createScaledTextLayout(String text) {
        if (font == null || text == null) {
            return null;
        }
        text = normalizeDisplay(text);

        float scaleY = (region.height > 0 && parameter.size > 0) ? region.height / parameter.size : 1.0f;
        boolean hasShadow = !getShadowOffset().isZero();
        String remappedText = remapMissingGlyphs(text);
        float scaleX = scaleY;
        GlyphLayout cachedLayout = new GlyphLayout();
        font.getData().setScale(scaleX, scaleY);
        setLayout(cachedLayout, font, remappedText, region.width);
        if (!isWrapping() && getOverflow() == OVERFLOW_SHRINK && cachedLayout.width > region.width) {
            scaleX *= region.width / cachedLayout.width;
            font.getData().setScale(scaleX, scaleY);
            setLayout(cachedLayout, font, remappedText, region.width);
        }
        GlyphLayout cachedShadowLayout = null;
        if (hasShadow) {
            cachedShadowLayout = new GlyphLayout();
            setLayout(cachedShadowLayout, font, remappedText, region.width);
        }
        return new FontTextLayout(this, session.generation(), cachedLayout, cachedShadowLayout, region.width, scaleX, scaleY, missingGlyphs.revision(),
                getAlign(), getOverflow(), isWrapping());
    }

    @Override
    public boolean isCachedTextLayoutValid(CachedTextLayout layout) {
        if (!(layout instanceof FontTextLayout cached) || font == null || session == null || !session.isLive()
                || cached.owner != this || cached.generation != session.generation()) {
            return false;
        }
        float scaleY = (region.height > 0 && parameter.size > 0) ? region.height / parameter.size : 1.0f;
        return cached.width == region.width && cached.scaleY == scaleY
                && cached.glyphRevision == missingGlyphs.revision() && cached.align == getAlign()
                && cached.overflow == getOverflow() && cached.wrapping == isWrapping()
                && (cached.shadowLayout != null) == !getShadowOffset().isZero();
    }

    @Override
    public boolean drawCachedTextLayout(SkinObjectRenderer sprite, CachedTextLayout layout, float offsetX, float offsetY) {
        recoverSession();
        if (font == null || session == null || !session.isLive()) return false;
        try (FontScaleSnapshot ignored = new FontScaleSnapshot(font.getData())) {
            return drawScaledCachedTextLayout(sprite, layout, offsetX, offsetY);
        }
    }

    private boolean drawScaledCachedTextLayout(SkinObjectRenderer sprite, CachedTextLayout layout, float offsetX, float offsetY) {
        if (!(layout instanceof FontTextLayout cached) || !isCachedTextLayoutValid(cached)) {
            return false;
        }

        font.getData().setScale(cached.scaleX, cached.scaleY);
        sprite.setType(getFilter() != 0 ? SkinObjectRenderer.TYPE_LINEAR : SkinObjectRenderer.TYPE_NORMAL);
        final float x = getAlign() == 2 ? region.x - region.width : getAlign() == 1 ? region.x - region.width / 2 : region.x;
        if (cached.shadowLayout != null) {
            shadowcolor.set(color.r / 2, color.g / 2, color.b / 2, color.a);
            applyColor(cached.shadowLayout, shadowcolor);
            sprite.draw(font, cached.shadowLayout, x + getShadowOffset().x + offsetX,
                    region.y - getShadowOffset().y + offsetY + region.getHeight());
        }
        applyColor(cached.layout, color);
        sprite.draw(font, cached.layout, x + offsetX, region.y + offsetY + region.getHeight());
        return true;
    }

    private void updateLayout(Rectangle r) {
        String text = getText();
        float scaleY = (r.height > 0 && parameter.size > 0) ? r.height / parameter.size : 1.0f;
        boolean hasShadow = !getShadowOffset().isZero();
        if (layoutValid && text.equals(layoutText) && layoutWidth == r.width && layoutScaleY == scaleY
                && layoutAlign == getAlign() && layoutOverflow == getOverflow() && layoutWrapping == isWrapping()
                && layoutHasShadow == hasShadow && layoutGlyphRevision == missingGlyphs.revision()) {
            return;
        }

        String remappedText = remapMissingGlyphs(text);
        float scaleX = scaleY;
        font.getData().setScale(scaleX, scaleY);
        setLayout(layout, font, remappedText, r.width);
        if (!isWrapping() && getOverflow() == OVERFLOW_SHRINK && layout.width > r.width) {
            scaleX *= r.width / layout.width;
            font.getData().setScale(scaleX, scaleY);
            setLayout(layout, font, remappedText, r.width);
        }
        if (hasShadow) {
            setLayout(shadowLayout, font, remappedText, r.width);
        } else {
            shadowLayout.reset();
        }

        layoutText = text;
        layoutWidth = r.width;
        layoutScaleX = scaleX;
        layoutScaleY = scaleY;
        layoutAlign = getAlign();
        layoutOverflow = getOverflow();
        layoutWrapping = isWrapping();
        layoutHasShadow = hasShadow;
        layoutGlyphRevision = missingGlyphs.revision();
        layoutValid = true;
    }

    private static void applyColor(GlyphLayout target, Color color) {
        int packedColor = color.toIntBits();
        for (int i = 1; i < target.colors.size; i += 2) {
            target.colors.set(i, packedColor);
        }
    }

    private void invalidateLayout() {
        layoutValid = false;
        layoutGlyphRevision = -1;
    }

    /** libGDX の暗黙の豆腐を無効にし、欠けたグリフを独自の代替チェーンで解決する。 / Disable libgdx's implicit tofu so that missing glyphs are resolved by our own fallback chain. */
    private static void disableMissingGlyph(BitmapFont target) {
        if (target != null && target.getData() != null) {
            target.getData().missingGlyph = null;
        }
    }

    /**
     * フォント自身がその文字の実グリフを持つときだけ true(暗黙の欠落グリフや .notdef は含めない)。
     * True only if the font itself holds a real glyph for the char (never the implicit missing glyph / .notdef).
     */
    static boolean hasNativeGlyph(BitmapFont.BitmapFontData data, char codePoint) {
        if (data == null) {
            return false;
        }
        BitmapFont.Glyph glyph = data.getGlyph(codePoint);
        return glyph != null && glyph != data.missingGlyph;
    }

    static boolean hasNativeGlyph(BitmapFont.BitmapFontData data, int codePoint) {
        if (data == null || codePoint < 0 || codePoint > Character.MAX_VALUE) {
            return false;
        }
        return hasNativeGlyph(data, (char) codePoint);
    }

    boolean hasNativeGlyph(int codePoint) {
        return font != null && hasNativeGlyph(font.getData(), codePoint);
    }

    private String toBmpCharacters(String text) {
        StringBuilder result = null;
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            if (codePoint > Character.MAX_VALUE) {
                if (result == null) {
                    result = new StringBuilder(text.length());
                    result.append(text, 0, index);
                }
            } else if (result != null) {
                result.append((char) codePoint);
            }
            index += Character.charCount(codePoint);
        }
        return result != null ? result.toString() : text;
    }

    /** 元の LibGdxCompatibilityTest がこの名前で呼ぶので残す。 / Kept under this name because the existing LibGdxCompatibilityTest calls it. */
    private String remapMissingGlyphs(String text) {
        if (text == null) {
            return "";
        }
        return missingGlyphs.remap(text);
    }

    /**
     * FreeType 版の代替グリフの探し方と登録先。グリフは生成中のフォントへ直接登録する。
     * FreeType-side lookup and install target for fallback glyphs; glyphs go straight into the generated font.
     */
    private final class FallbackGlyphHost implements MissingGlyphMapper.Host {
        @Override
        public boolean hasNativeGlyph(int codePoint) {
            return font != null && SkinTextFont.hasNativeGlyph(font.getData(), codePoint);
        }

        /** 供給元の代替列(スキン指定の後に共通代替が付いている)。 / The source's fallback chain (skin-specified, then the global fallbacks). */
        @Override
        public BitmapFont.Glyph resolveFallbackGlyph(int codePoint, int mappedCodePoint, boolean supplementary) {
            // 元の文字集合で既に試した BMP 文字は代替フォントだけを探す。 / BMP characters already tried in the primary set only search the fallbacks.
            boolean alreadyTriedPrimary = !supplementary && parameter.characters != null && parameter.characters.indexOf(codePoint) >= 0;
            return glyphProvider.resolve(codePoint, alreadyTriedPrimary,
                    (raster, renderedCodePoint) -> session.pack(raster, renderedCodePoint, mappedCodePoint)).glyph;
        }

        @Override
        public BitmapFont.Glyph resolveMarkerGlyph(int mappedCodePoint) {
            return createMissingGlyph(mappedCodePoint);
        }

        @Override
        public boolean installUnresolved(int codePoint) {
            return false;
        }

        @Override
        public boolean hasDrawableGlyph(int codePoint) {
            return font != null && font.getData().getGlyph((char) codePoint) != null;
        }

        @Override
        public void installGlyph(int key, BitmapFont.Glyph glyph) {
            font.getData().setGlyph(key, glyph);
        }

        @Override
        public void removeGlyph(int slot, int evictedCodePoint) {
            if (font != null && font.getData() != null) {
                font.getData().setGlyph(slot, null);
            }
        }

        private BitmapFont.Glyph createMissingGlyph(int mappedCodePoint) {
            if (session == null) {
                return null;
            }
            BitmapFont.Glyph glyph = glyphProvider.resolveMissingMarker(
                    (raster, renderedCodePoint) -> session.pack(raster, renderedCodePoint, mappedCodePoint)).glyph;
            return glyph != null ? glyph : session.packMissingBox(mappedCodePoint);
        }
    }

    private void clearSupplementaryGlyphs() {
        missingGlyphs.clearMappings();
        if (session != null) {
            session.resetSequence();
        }
        missingGlyphs.bumpRevision();
    }

    PuaSlotAllocator getPuaSlotAllocator() {
        return missingGlyphs.slots();
    }

    long getGlyphRevision() {
        return missingGlyphs.revision();
    }

    @Override
    SkinFontSource getFontSource() {
        return source;
    }

    BitmapFont getFont() {
        return font;
    }

    /**
     * 指定されたテキストの描画幅を現在のフォントスケールで計算して返します。
     * Computes and returns the layout advance width for the given text under the current scale.
     */
    float getTextWidth(String text) {
        recoverSession();
        if (font == null || session == null || !session.isLive()) return 0;
        try (FontScaleSnapshot ignored = new FontScaleSnapshot(font.getData())) {
            return getScaledTextWidth(text);
        }
    }

    private float getScaledTextWidth(String text) {
        if (text == null || text.isEmpty() || font == null) {
            return 0;
        }
        text = normalizeDisplay(text);
        String remapped = remapMissingGlyphs(text);
        GlyphLayout gl = new GlyphLayout();
        float scaleY = (region.height > 0 && parameter.size > 0) ? region.height / parameter.size : 1.0f;
        font.getData().setScale(scaleY, scaleY);
        gl.setText(font, remapped);
        return gl.width;
    }

    private void recoverSession() {
        if (!isDisposed() && session != null && !session.isLive()) {
            String text = preparedFonts != null ? preparedFonts : getText();
            replaceSession(text != null ? text : "", preparedFonts != null);
        }
    }

    private static final class FontTextLayout implements CachedTextLayout {
        private final SkinTextFont owner;
        private final long generation;
        private final GlyphLayout layout;
        private final GlyphLayout shadowLayout;
        private final float width;
        private final float scaleX;
        private final float scaleY;
        private final long glyphRevision;
        private final int align;
        private final int overflow;
        private final boolean wrapping;

        private FontTextLayout(SkinTextFont owner, long generation, GlyphLayout layout, GlyphLayout shadowLayout, float width, float scaleX, float scaleY,
                long glyphRevision, int align, int overflow, boolean wrapping) {
            this.owner = owner;
            this.generation = generation;
            this.layout = layout;
            this.shadowLayout = shadowLayout;
            this.width = width;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.glyphRevision = glyphRevision;
            this.align = align;
            this.overflow = overflow;
            this.wrapping = wrapping;
        }

        @Override
        public float getWidth() {
            return width;
        }
    }

    public void dispose() {
        if (isDisposed()) {
            return;
        }
        // 失敗や再入の経路も含め、解放前に無効化する。 / Invalidate before cleanup, including failure and reentrant paths.
        setDisposed();
        missingGlyphs.reset();
        FontSession ownedSession = session;
        session = null;
        font = null;
        layout = null;
        FontResourceDisposer.disposeAll(ownedSession != null ? ownedSession.resources() : new Disposable[0],
                ownsSource ? source : null);
    }

    /**
     * 同梱の BitmapFontData.setScale が変える公開フィールドだけを正確に戻す(逆算による丸めをしない)。
     * Restores exactly the public fields mutated by the bundled BitmapFontData.setScale (no inverse rounding).
     */
    static final class FontScaleSnapshot implements AutoCloseable {
        private final BitmapFontData data;
        private final float scaleX, scaleY, lineHeight, spaceXadvance, xHeight, capHeight, ascent, descent, down;
        private final float padTop, padLeft, padBottom, padRight;

        FontScaleSnapshot(BitmapFontData d) {
            data = d;
            scaleX = d.scaleX; scaleY = d.scaleY; lineHeight = d.lineHeight; spaceXadvance = d.spaceXadvance;
            xHeight = d.xHeight; capHeight = d.capHeight; ascent = d.ascent; descent = d.descent; down = d.down;
            padTop = d.padTop; padLeft = d.padLeft; padBottom = d.padBottom; padRight = d.padRight;
        }

        @Override public void close() {
            data.scaleX = scaleX; data.scaleY = scaleY; data.lineHeight = lineHeight; data.spaceXadvance = spaceXadvance;
            data.xHeight = xHeight; data.capHeight = capHeight; data.ascent = ascent; data.descent = descent; data.down = down;
            data.padTop = padTop; data.padLeft = padLeft; data.padBottom = padBottom; data.padRight = padRight;
        }
    }
}
