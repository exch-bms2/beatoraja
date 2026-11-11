package bms.player.beatoraja.skin;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Array;
import bms.player.beatoraja.skin.FontRenderRetirement.FontResourceDisposer;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import bms.player.beatoraja.skin.SystemFallbackGlyphs.SystemFallbackAtlas;

/**
 * 借り手ごとの生成フォント・データ、専用の主ページ、共通代替ページの貸与。
 *
 * <p>packer はここで作り、外部 packer としてジェネレーターに渡すので、フォントはページテクスチャを所有しない。
 * 後から追加する代替グリフは別の共有アトラスと借り手ごとのグリフ・ページ対応を使う。
 * 返したセッションはすぐに無効になり、ネイティブ資源は現在の描画が戻った後に退役する。</p>
 *
 * Borrower-local generated font/data, private primary pages, and a lease on common fallback pages.
 *
 * <p>The packer is created here and passed to the generator as an external packer, so the font does not own its page
 * textures. Subsequent fallback glyphs use a separate shared atlas and borrower-local glyph/page mappings.
 * Released sessions become stale immediately; native resources retire after the current render returns.</p>
 */
final class FontSession {
    private final FreeTypeFontGenerator.FreeTypeFontParameter parameter;
    private BitmapFont font;
    private final PixmapPacker packer;
    private final FallbackAtlas fallbackAtlas;
    private final int fallbackPageOffset;
    private final Array<TextureRegion> fallbackRegions = new Array<>();
    private static long nextGeneration;
    private final long generation = ++nextGeneration;
    private boolean disposed;

    private FontSession(FreeTypeFontGenerator.FreeTypeFontParameter parameter, BitmapFont font, PixmapPacker packer,
            FallbackAtlas fallbackAtlas) {
        this.parameter = parameter;
        this.font = font;
        this.packer = packer;
        this.fallbackAtlas = fallbackAtlas;
        fallbackPageOffset = font.getRegions().size;
    }

    void setFont(BitmapFont font) {
        this.font = font;
    }

    PixmapPacker packer() {
        return packer;
    }

    /** {@code parameter} でフォントを生成する。生成に失敗したら自分で作った packer を解放する。 / Generates the font for {@code parameter}. A failed generation releases the packer it created. */
    static FontSession open(FreeTypeFontGenerator generator, FreeTypeFontGenerator.FreeTypeFontParameter parameter) {
        return open(generator, parameter, null);
    }

    /** 前のセッションが貯めた代替ページを再取得せずに、置き換え用の資源を準備する。 / Prepare replacement resources without reacquiring the previous session's accumulated fallback pages. */
    static FontSession open(FreeTypeFontGenerator generator, FreeTypeFontGenerator.FreeTypeFontParameter parameter,
            FontSession previous) {
        PixmapPacker packer = createPacker(parameter);
        PixmapPacker previousPacker = parameter.packer;
        parameter.packer = packer;
        BitmapFont generated = null;
        FallbackAtlas acquired = null;
        try {
            if (parameter.characters != null && parameter.characters.isEmpty()) {
                // BitmapFont 1.14 は、事前生成の文字がないオンデマンドフォントでも最低一つのテクスチャ領域を要求する。
                // 透明な 1 ピクセルだけを用意し、見える文字は引き続き GlyphProvider を通す。
                // BitmapFont 1.14 requires at least one texture region, even for
                // an on-demand font with no pre-generated characters. Seed only
                // a transparent pixel; visible glyphs still go through GlyphProvider.
                Pixmap transparent = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
                try { transparent.setColor(0); transparent.fill(); packer.pack("empty-font-page", transparent); }
                finally { transparent.dispose(); }
            }
            generated = generator.generateFont(parameter);
            acquired = FallbackAtlas.acquire(parameter, previous != null ? previous.fallbackAtlas : null);
            return new FontSession(parameter, generated, packer, acquired);
        } catch (RuntimeException e) {
            parameter.packer = previousPacker;
            try {
                FontResourceDisposer.disposeAll(resources(generated, packer), acquired != null ? acquired::release : null);
            } catch (RuntimeException cleanupFailure) {
                if (cleanupFailure != e) {
                    e.addSuppressed(cleanupFailure);
                }
            }
            throw e;
        }
    }

    /**
     * ライブラリ自身の incremental packer と同じ設定(ページサイズ・余白・配置方式・透明色)。
     * Same settings as the library's own incremental packer (page size, padding, strategy, transparent colour).
     */
    static PixmapPacker createPacker(FreeTypeFontGenerator.FreeTypeFontParameter parameter) {
        int pageSize = FreeTypeFontGenerator.getMaxTextureSize();
        PixmapPacker packer = new PixmapPacker(pageSize, pageSize, Pixmap.Format.RGBA8888, 1, false,
                new PixmapPacker.GuillotineStrategy());
        packer.setTransparentColor(parameter.color);
        packer.getTransparentColor().a = 0;
        if (parameter.borderWidth > 0) {
            packer.setTransparentColor(parameter.borderColor);
            packer.getTransparentColor().a = 0;
        }
        return packer;
    }

    BitmapFont font() {
        return font;
    }

    void resetSequence() {
        // 名前は共通の所有者のもの。借り手が PUA 対応を初期化しても採番をやり直してはならない。
        // Names belong to the common owner and must never restart when a borrower resets its PUA map.
    }

    long generation() { return generation; }
    FallbackAtlas atlas() { return fallbackAtlas; }
    boolean isLive() { return !disposed && fallbackAtlas.isLive(); }
    static void invalidateSharedAtlases() { FallbackAtlas.invalidateAll(); SystemFallbackAtlas.invalidateAll(); }

    private void updateFallbackRegions() {
        fallbackAtlas.packer.updateTextureRegions(fallbackRegions, parameter.minFilter, parameter.magFilter,
                parameter.genMipMaps);
        for (int i = font.getRegions().size - fallbackPageOffset; i < fallbackRegions.size; i++) {
            font.getRegions().add(fallbackRegions.get(i));
        }
    }

    /**
     * ラスタ化したグリフを詰め、{@code mappedCodePoint} 用のグリフを返す。入りきらなければ null。
     * Packs the rasterised glyph and returns the glyph for {@code mappedCodePoint}, or null if it does not fit.
     */
    BitmapFont.Glyph pack(FreeTypeFontGenerator.GlyphAndBitmap raster, int renderedCodePoint, int mappedCodePoint) {
        if (!isLive()) throw new IllegalStateException("Retired font session");
        if (raster.bitmap == null || raster.bitmap.getWidth() == 0 || raster.bitmap.getRows() == 0) {
            // 空白文字または不可視グリフ（全角スペース等）の処理 / Handle whitespace or zero-bitmap invisible glyphs
            BitmapFont.Glyph glyph = copyGlyph(raster.glyph, mappedCodePoint);
            glyph.page = 0;
            glyph.srcX = 0;
            glyph.srcY = 0;
            glyph.width = 0;
            glyph.height = 0;
            // 32ビット補助平面文字（> 0xFFFF）による 16ビット BMP 配列外アクセス（ArrayIndexOutOfBoundsException）を防止
            // Prevent ArrayIndexOutOfBoundsException on 16-bit BMP font array for supplementary code points (> 0xFFFF)
            if (font != null && font.getData() != null && mappedCodePoint >= 0 && mappedCodePoint <= 0xFFFF) {
                font.getData().setGlyph(mappedCodePoint, glyph);
            }
            return glyph;
        }
        Pixmap pixmap = raster.bitmap.getPixmap(Pixmap.Format.RGBA8888, parameter.color, parameter.gamma);
        try {
            String glyphName = fallbackAtlas.nextName();
            var packed = fallbackAtlas.packer.pack(glyphName, pixmap);
            int page = fallbackAtlas.packer.getPageIndex(glyphName);
            if (page < 0) {
                return null;
            }
            updateFallbackRegions();

            BitmapFont.Glyph glyph = copyGlyph(raster.glyph, mappedCodePoint);
            glyph.page = fallbackPageOffset + page;
            glyph.srcX = packed.getX();
            glyph.srcY = packed.getY();
            font.getData().setGlyphRegion(glyph, font.getRegion(glyph.page));
            return glyph;
        } finally {
            pixmap.dispose();
        }
    }

    /** どのフォントにもないコードポイント用に中抜きの四角を描く。 / Draws a hollow box for a code point that no font has. */
    BitmapFont.Glyph packMissingBox(int mappedCodePoint) {
        if (!isLive()) throw new IllegalStateException("Retired font session");
        int height = Math.max(8, parameter.size);
        int width = Math.max(6, Math.round(height * 0.75f));
        int stroke = Math.max(1, height / 12);
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        try {
            pixmap.setColor(0, 0, 0, 0);
            pixmap.fill();
            Color c = parameter.color != null ? parameter.color : Color.WHITE;
            pixmap.setColor(c);
            for (int i = 0; i < stroke; i++) {
                pixmap.drawRectangle(i, i, width - i * 2, height - i * 2);
            }

            String glyphName = fallbackAtlas.nextName();
            var packed = fallbackAtlas.packer.pack(glyphName, pixmap);
            int page = fallbackAtlas.packer.getPageIndex(glyphName);
            if (page < 0) {
                return null;
            }
            updateFallbackRegions();

            BitmapFont.Glyph glyph = new BitmapFont.Glyph();
            glyph.id = mappedCodePoint;
            glyph.srcX = packed.getX();
            glyph.srcY = packed.getY();
            glyph.width = width;
            glyph.height = height;
            glyph.xoffset = 0;
            glyph.yoffset = -height;
            glyph.xadvance = width + Math.max(1, stroke);
            glyph.page = fallbackPageOffset + page;
            font.getData().setGlyphRegion(glyph, font.getRegion(glyph.page));
            return glyph;
        } finally {
            pixmap.dispose();
        }
    }

    private static BitmapFont.Glyph copyGlyph(BitmapFont.Glyph source, int mappedCodePoint) {
        BitmapFont.Glyph glyph = new BitmapFont.Glyph();
        glyph.id = mappedCodePoint;
        glyph.srcX = source.srcX;
        glyph.srcY = source.srcY;
        glyph.width = source.width;
        glyph.height = source.height;
        glyph.xoffset = source.xoffset;
        glyph.yoffset = source.yoffset;
        glyph.xadvance = source.xadvance;
        glyph.page = source.page;
        return glyph;
    }

    /** 解放順: フォント、ページテクスチャ、packer。ページテクスチャはそのページの Pixmap も解放する。 / Release order: font, page textures, packer. A page texture also releases its page pixmap. */
    Disposable[] resources() {
        return new Disposable[] { this::dispose };
    }

    Disposable[] resourcesWithoutFont() {
        return new Disposable[] { () -> release(false) };
    }

    static Disposable[] resources(BitmapFont ownedFont, PixmapPacker ownedPacker) {
        ArrayList<Disposable> resources = new ArrayList<>();
        if (ownedFont != null) {
            resources.add(ownedFont);
        }
        if (ownedPacker != null) {
            for (PixmapPacker.Page page : ownedPacker.getPages()) {
                Texture texture = page.getTexture();
                if (texture != null) {
                    resources.add(texture);
                }
            }
            resources.add(ownedPacker);
        }
        return resources.toArray(new Disposable[0]);
    }

    /** このセッションが所有するものをすべて解放する。所有者は先に参照を外すこと。 / Releases everything this session owns; the owner must drop its references first. */
    void dispose() {
        release(true);
    }

    private void release(boolean includeFont) {
        if (disposed) return;
        disposed = true;
        if (parameter.packer == packer) parameter.packer = null;
        BitmapFont ownedFont = includeFont ? font : null;
        font = null;
        FontResourceDisposer.disposeAll(new Disposable[] {
                () -> FontRenderRetirement.retire(() -> FontResourceDisposer.disposeAll(resources(ownedFont, packer), null)),
                fallbackAtlas::release }, null);
    }

    /**
     * 共有する代替ページだけを持つ。フォントデータ・グリフ表・PUA 枠は借り手ごとに持つ。描画スレッド専用。
     * Shared owned fallback pages only. Font data, glyph tables and PUA slots remain borrower-local. Render thread only.
     */
    static final class FallbackAtlas {
        private static final Map<Key, FallbackAtlas> LIVE = new HashMap<>();
        // 他の借り手がまだ借りている置換済みの所有者も含む(コンテキスト無効化と寿命管理のため)。
        // Includes displaced owners still leased by other borrowers, for context invalidation and lifetime accounting.
        private static final Set<FallbackAtlas> OWNERS = new HashSet<>();
        private static long nextGeneration;
        private final Key key;
        final PixmapPacker packer;
        final long generation = ++nextGeneration;
        private int borrowers;
        private int sequence;
        private boolean retired;

        private FallbackAtlas(Key key, FreeTypeFontParameter p) {
            this.key = key;
            packer = FontSession.createPacker(p);
        }

        static FallbackAtlas acquire(FreeTypeFontParameter p) {
            return acquire(p, null);
        }

        /** 置き換え時に元の所有者を再取得して、前の借り手のラスタ履歴を引き継いではならない。 / Replacement must not retain the old borrower's raster history by reacquiring its own owner. */
        static FallbackAtlas acquire(FreeTypeFontParameter p, FallbackAtlas excluded) {
            Key key = Key.of(p);
            FallbackAtlas atlas = LIVE.get(key);
            if (atlas == null || atlas == excluded) {
                atlas = new FallbackAtlas(key, p);
                OWNERS.add(atlas);
                LIVE.put(key, atlas);
            }
            atlas.borrowers++;
            return atlas;
        }

        String nextName() {
            if (retired) throw new IllegalStateException("Retired fallback atlas");
            return "fallback-" + sequence++;
        }

        void release() {
            if (borrowers <= 0) throw new IllegalStateException("Unbalanced atlas release");
            // 一人でも抜けたら、残る貸与は使えるままにして、新しい借り手はそのラスタ履歴につながない。
            // 一時的な利用の繰り返しで履歴が際限なく増えないようにする。
            // Once a member leaves, keep surviving leases usable but stop attaching new
            // borrowers to their raster history. Repeated temporary users must not grow it forever.
            LIVE.remove(key, this);
            if (--borrowers == 0) {
                retired = true;
                OWNERS.remove(this);
                FontRenderRetirement.retire(() -> FontResourceDisposer.disposeAll(FontSession.resources(null, packer), null));
            }
        }

        boolean isLive() { return !retired; }
        int borrowerCount() { return borrowers; }
        static int liveOwnerCount() { return OWNERS.size(); }

        /** 描画・コンテキストの所有者がコンテキスト喪失時、置き換えセッションを準備する前に呼ぶ。 / The render/context owner calls this on context loss, before preparing replacement sessions. */
        static void invalidateAll() {
            for (FallbackAtlas atlas : OWNERS) atlas.retired = true;
            LIVE.clear();
        }

        // 文字集合・incremental・外部 packer を除く描画設定をすべて写し取る。
        // プールは借り手のグリフ・コードポイント対応を持たないので、異なる書体が同じページに詰められても安全。
        // Snapshot every public rendering option, excluding characters, incremental mode and external packer.
        // The pool stores no borrowed glyph/CP mappings, so different faces may safely pack into the same pages.
        record Key(int size, boolean mono, Object hinting, String color, float gamma, int renderCount,
                float borderWidth, String borderColor, boolean borderStraight, float borderGamma,
                int shadowX, int shadowY, String shadowColor, int spaceX, int spaceY, boolean kerning,
                boolean flip, boolean mipMaps, Object minFilter, Object magFilter, int pageSize) {
            static Key of(FreeTypeFontParameter p) {
                return new Key(p.size, p.mono, p.hinting, colorKey(p.color), p.gamma, p.renderCount,
                        p.borderWidth, colorKey(p.borderColor), p.borderStraight, p.borderGamma,
                        p.shadowOffsetX, p.shadowOffsetY, colorKey(p.shadowColor), p.spaceX, p.spaceY,
                        p.kerning, p.flip, p.genMipMaps, p.minFilter, p.magFilter,
                        com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.getMaxTextureSize());
            }

            private static String colorKey(com.badlogic.gdx.graphics.Color c) {
                return c == null ? "null" : Float.floatToRawIntBits(c.r) + ":" + Float.floatToRawIntBits(c.g)
                        + ":" + Float.floatToRawIntBits(c.b) + ":" + Float.floatToRawIntBits(c.a);
            }
        }
    }
}
