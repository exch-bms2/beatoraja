package bms.player.beatoraja.skin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntMap;

import bms.player.beatoraja.skin.BitmapFontLoader.BitmapFontDataCopy;
import bms.player.beatoraja.skin.FontRenderRetirement.FontResourceDisposer;
import bms.player.beatoraja.skin.FontSession.FallbackAtlas;

/**
 * プログラム全体の代替フォント(スキンが指定していない FreeType の代替)で、ビットマップ文字に足りないグリフを作る。
 * 供給元の選択とスタイルをここで持ち、ラスタ化・縁取り・ページはスタイル・供給元ごとに共有する {@link SystemFallbackAtlas} から借りる。
 * スキン文字に合わせる部分(スタイルの元になる設定・基準線の補正・作ったグリフの破棄)は {@link Owner} が受け持つ。
 * Builds glyphs missing from bitmap skin text with the program-wide fallback fonts (FreeType fallbacks the skin did not specify).
 * Source selection and style live here; rasterization, outlines and pages are borrowed from {@link SystemFallbackAtlas},
 * shared per style and source. The {@link Owner} supplies what depends on the skin text (style inputs, baseline alignment,
 * dropping built glyphs).
 */
final class SystemFallbackGlyphs {

    /**
     * スキン文字側の処理。 / The skin-text side.
     */
    interface Owner {
        /** スキン文字の設定から代替のスタイル(大きさ・縁取り・影)を埋める。 / Fills the fallback style (size, outline, shadow) from the skin text settings. */
        void fillStyle(FreeTypeFontParameter style);

        /** この代替フォントのグリフをスキン文字の基準線にそろえる縦ずれ。 / Vertical offset that aligns this fallback font with the skin text baseline. */
        int baselineOffset(FreeTypeFontGenerator generator, FreeTypeFontParameter style);

        /** ここで作ったグリフをすべて捨てる(供給元・スタイル・アトラスが変わったとき)。 / Drops every glyph built here (source, style or atlas changed). */
        void invalidate();
    }

    private final Owner owner;
    /** アトラスのページを置く、スキン文字のページ表上の開始番号。 / First page number of the atlas pages in the skin text's page table. */
    private final int pageOffset;
    /** グリフ情報がないときの送り幅の基準。 / Basis for the advance when the raster has no glyph metrics. */
    private final float textSize;
    private final GlyphProvider.FailureSink failures;

    private SkinFontSource source;
    private boolean ownsSource;
    private boolean usesGlobalSource;
    private FreeTypeFontParameter style;
    private final FreeTypeFontParameter styleProbe = new FreeTypeFontParameter();
    private GlyphProvider provider;
    private final Map<FreeTypeFontGenerator, Integer> baselineOffsets = new IdentityHashMap<>();

    private SystemFallbackAtlas atlas;
    /** スキン文字のページ表へ最後に写したアトラスの版。 / Atlas version last copied into the skin text's page table. */
    private long attachedVersion = -1;

    /**
     * 共通代替フォントの供給元。起動時に一度だけ読み、プログラム全体で一つだけ持ち、作り直しも削除もしない。並びと文字ごとの選択も
     * 読み込み時に一度だけ作る。 / The global fallback source: read once at startup, one for the whole program, never
     * rebuilt or removed. Its order and per-character choice are built once when it loads.
     */
    private static SkinFontSource globalSource;
    private static boolean globalSourceResolved;

    /**
     * 共通代替フォント({@link FallbackFontPolicy} の一覧)の供給元を返す。最初の呼び出しで作る。共通代替フォントがなければ null。
     * Returns the source for the global fallback fonts ({@link FallbackFontPolicy}), created on first use. Null when there are none.
     */
    static synchronized SkinFontSource globalSource() {
        if (!globalSourceResolved) {
            globalSourceResolved = true;
            if (FallbackFontPolicy.resolve(new String[0]).length > 0) {
                globalSource = new SkinFontSource((String) null, new String[0]);
            }
        }
        return globalSource;
    }

    /** 共通代替フォントの供給元を手放し、次に使うとき作り直す。試験専用。 / Releases the global source so it is rebuilt on next use. Test use only. */
    static synchronized void resetGlobalSource() {
        SkinFontSource released = globalSource;
        globalSource = null;
        globalSourceResolved = false;
        if (released != null) {
            released.dispose();
        }
    }

    /**
     * 終了時に一度だけ呼ぶ。共有アトラスと共通代替フォントを解放し、以後は作り直さない。解放は次の描画の区切りか終了時の drain で行う。
     * Call once at shutdown: releases the shared atlases and the global source, which is not rebuilt afterwards.
     * Native cleanup runs at the next render boundary or the shutdown drain.
     */
    static synchronized void shutdown() {
        FontSession.invalidateSharedAtlases();
        SkinFontSource released = globalSource;
        globalSource = null;
        globalSourceResolved = true;
        if (released != null) {
            released.dispose();
        }
    }

    SystemFallbackGlyphs(Owner owner, int pageOffset, float textSize, GlyphProvider.FailureSink failures) {
        this.owner = owner;
        this.pageOffset = pageOffset;
        this.textSize = textSize;
        this.failures = failures;
    }

    SkinFontSource source() {
        return source;
    }

    GlyphProvider provider() {
        return provider;
    }

    FreeTypeFontParameter style() {
        return style;
    }

    /**
     * 供給元がなければ共通代替フォントの供給元を借りる(所有しない)。共通供給元が作り直されていれば(試験のみ)それに合わせる。
     * Borrows the global fallback source when none is set (not owned), following it if it was recreated (tests only).
     */
    SkinFontSource sourceOrDefault() {
        if (provider == null || usesGlobalSource) {
            SkinFontSource global = globalSource();
            if (global != null && source != global) {
                setSourceInternal(global, false, true);
            }
        }
        return source;
    }

    /**
     * 代替の供給元を差し替える。違う供給元なら作ったグリフを捨て、所有していた供給元を解放する。
     * Replaces the fallback source. A different source drops the built glyphs and releases an owned previous source.
     */
    void setSource(SkinFontSource newSource, boolean owns) {
        boolean isGlobal = !owns && newSource != null && newSource == globalSource;
        setSourceInternal(newSource, owns, isGlobal);
    }

    private void setSourceInternal(SkinFontSource newSource, boolean owns, boolean isGlobal) {
        if (source != newSource) {
            releaseAtlasIfPresent();
            owner.invalidate();
            if (source != null && ownsSource) {
                source.dispose();
            }
        } else if (newSource != null) {
            ownsSource = owns;
            usesGlobalSource = isGlobal;
            refresh();
            return;
        }
        source = newSource;
        ownsSource = owns;
        usesGlobalSource = isGlobal;
        if (newSource != null) {
            style = createStyle();
            provider = new GlyphProvider(newSource, style, failures);
        } else {
            style = null;
            provider = null;
        }
    }

    /**
     * アトラスが生きているか、スタイルが変わっていないかを確かめ、変わっていれば作り直す。
     * Checks that the atlas is live and the style is unchanged, rebuilding when either changed.
     */
    void refresh() {
        ensureLive();
        if (usesGlobalSource) {
            SkinFontSource global = globalSource();
            if (global != source) {
                setSourceInternal(global, false, true);
                return;
            }
        }
        if (source != null) {
            owner.fillStyle(styleProbe);
            if (isStyleChanged(style, styleProbe)) {
                releaseAtlasIfPresent();
                owner.invalidate();
                style = createStyle();
                provider = new GlyphProvider(source, style, failures);
            }
        }
    }

    /**
     * アトラスが捨てられていたら(GL 文脈の喪失など)参照を解放し、作ったグリフを捨てる。
     * Releases the retired atlas reference and drops built glyphs when the atlas was invalidated (e.g. GL context loss).
     */
    void ensureLive() {
        if (atlas != null && !atlas.isLive()) {
            releaseAtlasIfPresent();
            owner.invalidate();
        }
    }

    private void releaseAtlasIfPresent() {
        if (atlas != null) {
            SystemFallbackAtlas previous = atlas;
            atlas = null;
            attachedVersion = -1;
            baselineOffsets.clear();
            previous.release();
        }
    }

    /**
     * 代替フォントでグリフを作る。アトラスに詰めたグリフのページはスキン文字のページ表上の番号になる。見つからなければ null。
     * Builds a glyph with the fallback fonts; packed glyphs are addressed to the skin text's page table. Null when none is found.
     */
    Glyph create(int codePoint, int mappedCodePoint, boolean fallbackOnly) {
        ensureLive();
        sourceOrDefault();
        if (provider == null) {
            return null;
        }
        if (atlas == null) {
            // 共通代替フォントの組だけはスキンを替えても残す。 / Only the global fallback pair outlives skin changes.
            atlas = SystemFallbackAtlas.acquire(style, source, usesGlobalSource || source == globalSource);
        }
        SystemFallbackAtlas.Cached cached = atlas.glyph(codePoint, provider, fallbackOnly, style);
        if (cached == null) {
            return null;
        }
        int offset = baselineOffsets.computeIfAbsent(cached.generator, g -> owner.baselineOffset(g, style));
        return cached.toGlyph(mappedCodePoint, offset, pageOffset, textSize);
    }

    /**
     * 詰めたグリフをテクスチャへ反映し、前回写してから変わっていればアトラスのページ領域を返す。変わっていない、または GL がなければ null。
     * Uploads packed glyphs and returns the atlas page regions when they changed since the last copy; null otherwise or without GL.
     */
    Array<TextureRegion> flush() {
        ensureLive();
        if (atlas == null || Gdx.gl == null) {
            return null;
        }
        atlas.upload();
        if (atlas.version() == attachedVersion) {
            return null;
        }
        attachedVersion = atlas.version();
        return atlas.regions();
    }

    /**
     * アトラスへの参照を切り離し、後で返却すべき貸与を返す。共通代替フォントの組は返却後も共有され、次に借りたときに使い回す。
     * Detaches the atlas and returns the lease to release afterwards. A global fallback pair stays shared and is reused next time.
     */
    List<Disposable> detach() {
        SystemFallbackAtlas ownedAtlas = atlas;
        atlas = null;
        attachedVersion = -1;
        List<Disposable> resources = new ArrayList<>();
        if (ownedAtlas != null) {
            resources.add(ownedAtlas::release);
        }
        baselineOffsets.clear();
        return resources;
    }

    /**
     * 供給元を手放し、所有していれば解放対象として返す。アトラスは先に {@link #detach()} で返しておく。
     * Lets go of the source and returns it for release when owned; detach the atlas with {@link #detach()} first.
     */
    List<Disposable> close() {
        releaseAtlasIfPresent();
        SkinFontSource ownedSource = ownsSource ? source : null;
        source = null;
        ownsSource = false;
        usesGlobalSource = false;
        provider = null;
        List<Disposable> resources = new ArrayList<>();
        if (ownedSource != null) {
            resources.add(ownedSource);
        }
        return resources;
    }

    /** スタイルの複製。色は共有しない。 / Copies the style without sharing color instances. */
    private FreeTypeFontParameter createStyle() {
        FreeTypeFontParameter param = new FreeTypeFontParameter();
        owner.fillStyle(param);
        param.borderColor = new Color(param.borderColor);
        param.shadowColor = new Color(param.shadowColor);
        return param;
    }

    private static boolean isStyleChanged(FreeTypeFontParameter p1, FreeTypeFontParameter p2) {
        if (p1 == null || p2 == null) {
            return p1 != p2;
        }
        if (p1.size != p2.size || p1.flip != p2.flip) return true;
        if (Float.compare(p1.borderWidth, p2.borderWidth) != 0) return true;
        if (p1.shadowOffsetX != p2.shadowOffsetX || p1.shadowOffsetY != p2.shadowOffsetY) return true;
        if (!Objects.equals(p1.borderColor, p2.borderColor)) return true;
        if (!Objects.equals(p1.shadowColor, p2.shadowColor)) return true;
        return false;
    }

    /**
     * 代替フォントのグリフを、スタイル(大きさ・縁取り・影など)と供給元の組ごとに一度だけラスタ化して共有するページ置き場。描画スレッド専用。
     * <ul>
     * <li>共通代替フォントの組はスキンを替えても消さない。使われていない組は、数かページ数が上限を超えたときに古い順に捨てる。使用中の組は捨てない。</li>
     * <li>スキンが指定した供給元(TTF など)の組は、借り手がいなくなった時点で捨てる。</li>
     * <li>GL 文脈を失ったときはすべて捨てる。借り手は {@link #isLive()} で気づいて作り直す。</li>
     * </ul>
     * グリフは基準線補正前・アトラス内ページ番号のまま持つ。借り手ごとの補正とページ番号の付け替えは {@link Cached#toGlyph} で行う。
     * {@link FallbackAtlas} は FreeType 文字のセッションごとのページで、こちらとは別物。
     *
     * Shared pages that rasterize each fallback glyph only once per (style, source) pair. Render thread only.
     */
    static final class SystemFallbackAtlas {
        /** 使われていない組を残す数の上限(調整用)。 / Cap on unused pairs kept for reuse (tuning knob). */
        static final int MAX_IDLE = 8;
        /** 使われていない組のページ合計の上限(調整用)。 / Cap on the total pages of unused pairs (tuning knob). */
        static final int MAX_IDLE_PAGES = 16;

        private static final Map<Key, SystemFallbackAtlas> LIVE = new HashMap<>();
        /** 使われていない組。古い順。 / Unused pairs, oldest first. */
        private static final LinkedHashMap<Key, SystemFallbackAtlas> IDLE = new LinkedHashMap<>();

        /** 供給元は同一性で比べる。 / The source is compared by identity. */
        private record Key(FallbackAtlas.Key style, SkinFontSource source) {
        }

        /**
         * ラスタ化したグリフ(基準線補正前、アトラス内ページ番号)と、それを作った生成器。
         * A rasterized glyph (before baseline correction, atlas-local page) and the generator that produced it.
         */
        static final class Cached {
            final Glyph glyph;
            final FreeTypeFontGenerator generator;
            /** ページに詰めたか(空白などは詰めない)。 / Whether it was packed into a page (blank glyphs are not). */
            final boolean packed;
            /** FreeType がグリフ情報を返したか。 / Whether FreeType returned glyph metrics. */
            final boolean hasMetrics;

            private Cached(Glyph glyph, FreeTypeFontGenerator generator, boolean packed, boolean hasMetrics) {
                this.glyph = glyph;
                this.generator = generator;
                this.packed = packed;
                this.hasMetrics = hasMetrics;
            }

            /**
             * 借り手用の複製。基準線補正を足し、ページ番号を借り手のページ表上の番号にする。
             * A copy for one borrower, with its baseline correction added and the page moved into its page table.
             */
            Glyph toGlyph(int mappedCodePoint, int baselineOffset, int pageOffset, float textSize) {
                Glyph copy = BitmapFontDataCopy.copyGlyph(glyph);
                copy.id = mappedCodePoint;
                copy.yoffset += baselineOffset;
                copy.page = packed ? pageOffset + glyph.page : 0;
                if (!hasMetrics) {
                    copy.xadvance = (int) Math.round(textSize * 0.35f);
                }
                return copy;
            }
        }

        private final Key key;
        private final boolean keepWhenIdle;
        final PixmapPacker packer;
        private final Array<TextureRegion> regions = new Array<>();
        private final IntMap<Cached> glyphs = new IntMap<>();
        private final IntMap<Cached> fallbackOnlyGlyphs = new IntMap<>();
        private int borrowers;
        private int sequence;
        private boolean dirty;
        private long version;
        private boolean retired;
        private boolean disposed;

        private SystemFallbackAtlas(Key key, FreeTypeFontParameter style, boolean keepWhenIdle) {
            this.key = key;
            this.keepWhenIdle = keepWhenIdle;
            this.packer = FontSession.createPacker(style);
        }

        /** スタイルと供給元の組を借りる。なければ作る。 / Borrows the pair for a style and source, creating it when missing. */
        static SystemFallbackAtlas acquire(FreeTypeFontParameter style, SkinFontSource source, boolean keepWhenIdle) {
            Key key = new Key(FallbackAtlas.Key.of(style), source);
            SystemFallbackAtlas atlas = LIVE.get(key);
            if (atlas == null || atlas.retired) {
                atlas = new SystemFallbackAtlas(key, style, keepWhenIdle);
                LIVE.put(key, atlas);
            }
            IDLE.remove(key);
            atlas.borrowers++;
            return atlas;
        }

        void release() {
            if (borrowers <= 0) throw new IllegalStateException("Unbalanced atlas release");
            if (--borrowers > 0) {
                return;
            }
            if (keepWhenIdle && !retired) {
                IDLE.put(key, this);
                trimIdle();
            } else {
                LIVE.remove(key, this);
                dispose();
            }
        }

        boolean isLive() { return !retired; }
        int borrowerCount() { return borrowers; }
        int glyphCount() { return glyphs.size + fallbackOnlyGlyphs.size; }
        static int idleCount() { return IDLE.size(); }

        /**
         * 文字のグリフを返す。まだなければ provider でラスタ化して詰める。どの代替フォントにもなければ null。
         * {@code fallbackOnly == true} の場合は主フォント由来のキャッシュと混同しないよう分離する。
         * Returns the glyph for a code point, rasterizing and packing it with the provider when missing. Null when no fallback has it.
         */
        Cached glyph(int codePoint, GlyphProvider provider, boolean fallbackOnly, FreeTypeFontParameter style) {
            if (retired) {
                return null;
            }
            IntMap<Cached> targetMap = fallbackOnly ? fallbackOnlyGlyphs : glyphs;
            Cached cached = targetMap.get(codePoint);
            if (cached != null) {
                return cached;
            }
            if (!fallbackOnly) {
                Cached fromFallback = fallbackOnlyGlyphs.get(codePoint);
                if (fromFallback != null && (key.source == null || key.source.getPrimaryGenerator() == null)) {
                    return fromFallback;
                }
            }
            FreeTypeFontGenerator[] used = new FreeTypeFontGenerator[1];
            boolean[] info = new boolean[2]; // [0] packed, [1] hasMetrics
            GlyphProvider.Result res = provider.resolvePrepared(codePoint, fallbackOnly, generator -> {
                used[0] = generator;
                return (raster, renderedCodePoint) -> pack(raster, style, info);
            });
            if (res == null || res.glyph == null) {
                return null;
            }
            cached = new Cached(res.glyph, used[0], info[0], info[1]);
            targetMap.put(codePoint, cached);
            if (!fallbackOnly && (key.source == null || used[0] != key.source.getPrimaryGenerator())) {
                fallbackOnlyGlyphs.put(codePoint, cached);
            }
            return cached;
        }

        /**
         * 詰めたグリフをテクスチャへ反映する。反映したら版が上がる。GL がなければ何もしない。
         * Uploads packed glyphs; the version goes up when something was uploaded. No-op without a GL context.
         */
        void upload() {
            if (!dirty || retired || Gdx.gl == null) {
                return;
            }
            packer.updateTextureRegions(regions, Texture.TextureFilter.Linear, Texture.TextureFilter.Linear, false);
            dirty = false;
            version++;
        }

        long version() { return version; }

        Array<TextureRegion> regions() { return regions; }

        /** GL 文脈を失ったときに呼ぶ。すべての組を捨てる。 / Called when the GL context is lost; drops every pair. */
        static void invalidateAll() {
            for (SystemFallbackAtlas atlas : new ArrayList<>(LIVE.values())) {
                atlas.retired = true;
                if (atlas.borrowers == 0) {
                    atlas.dispose();
                }
            }
            LIVE.clear();
            IDLE.clear();
        }

        private static void trimIdle() {
            int pages = 0;
            for (SystemFallbackAtlas atlas : IDLE.values()) {
                pages += atlas.packer.getPages().size;
            }
            Iterator<SystemFallbackAtlas> oldestFirst = IDLE.values().iterator();
            while (oldestFirst.hasNext() && (IDLE.size() > MAX_IDLE || pages > MAX_IDLE_PAGES)) {
                SystemFallbackAtlas oldest = oldestFirst.next();
                oldestFirst.remove();
                pages -= oldest.packer.getPages().size;
                LIVE.remove(oldest.key, oldest);
                oldest.retired = true;
                oldest.dispose();
            }
        }

        private void dispose() {
            if (disposed) {
                return;
            }
            disposed = true;
            retired = true;
            glyphs.clear();
            fallbackOnlyGlyphs.clear();
            FontRenderRetirement.retire(() -> FontResourceDisposer.disposeAll(FontSession.resources(null, packer), null));
        }

        /**
         * FreeType のラスタを詰め、基準線補正前・アトラス内ページ番号のグリフを返す。空白などはページに詰めない。
         * Packs a FreeType raster and returns a glyph before baseline correction with an atlas-local page. Blank glyphs are not packed.
         */
        private Glyph pack(FreeTypeFontGenerator.GlyphAndBitmap raster, FreeTypeFontParameter style, boolean[] info) {
            if (raster == null) {
                return null;
            }
            info[0] = false;
            info[1] = raster.glyph != null;
            if (raster.bitmap == null || raster.bitmap.getWidth() == 0 || raster.bitmap.getRows() == 0) {
                // 空白文字または不可視グリフ（全角スペース等）の処理 / Handle whitespace or zero-bitmap invisible glyphs
                info[0] = false;
                Glyph glyph = new Glyph();
                glyph.width = 0;
                glyph.height = 0;
                glyph.xoffset = raster.glyph != null ? raster.glyph.xoffset : 0;
                glyph.yoffset = raster.glyph != null ? raster.glyph.yoffset : 0;
                glyph.xadvance = raster.glyph != null ? raster.glyph.xadvance : 0;
                return glyph;
            }

            int b = (style != null && style.borderWidth > 0) ? Math.max(1, Math.round(style.borderWidth)) : 0;
            Pixmap srcPixmap = raster.bitmap.getPixmap(Pixmap.Format.RGBA8888, Color.WHITE, 1.0f);
            Pixmap pixmap;
            if (b > 0) {
                Color borderC = (style != null && style.borderColor != null) ? style.borderColor : Color.BLACK;
                pixmap = createOutlineBakedPixmap(srcPixmap, b, borderC);
                srcPixmap.dispose();
            } else {
                pixmap = srcPixmap;
            }

            try {
                String glyphName = "system-fallback-" + sequence++;
                var packed = packer.pack(glyphName, pixmap);
                int packerPage = packer.getPageIndex(glyphName);
                if (packerPage < 0) {
                    return null;
                }
                dirty = true;
                info[0] = true;

                Glyph glyph = new Glyph();
                glyph.srcX = packed.getX();
                glyph.srcY = packed.getY();
                glyph.width = packed.getWidth();
                glyph.height = packed.getHeight();
                glyph.xoffset = (raster.glyph != null ? raster.glyph.xoffset : 0) - b;
                glyph.yoffset = (raster.glyph != null ? raster.glyph.yoffset : 0) - b;
                glyph.xadvance = raster.glyph != null ? raster.glyph.xadvance : 0;
                glyph.page = packerPage;
                return glyph;
            } finally {
                pixmap.dispose();
            }
        }

        /**
         * 半径 b の円形で縁取りを焼き込んだ新しい Pixmap を返す。元の Pixmap は変更しない。
         * Returns a new pixmap with a circular outline of radius b baked in; the source pixmap is unchanged.
         */
        private static Pixmap createOutlineBakedPixmap(Pixmap srcPixmap, int b, Color borderC) {
            int srcWidth = srcPixmap.getWidth();
            int srcHeight = srcPixmap.getHeight();
            int dstWidth = srcWidth + 2 * b;
            int dstHeight = srcHeight + 2 * b;
            Pixmap baked = new Pixmap(dstWidth, dstHeight, Pixmap.Format.RGBA8888);

            int borderR = (int) (borderC.r * 255) & 0xff;
            int borderG = (int) (borderC.g * 255) & 0xff;
            int borderB = (int) (borderC.b * 255) & 0xff;
            float baseBorderA = borderC.a;

            int bSq = b * b;
            for (int y = 0; y < srcHeight; y++) {
                for (int x = 0; x < srcWidth; x++) {
                    int pixel = srcPixmap.getPixel(x, y);
                    int a = pixel & 0xff;
                    if (a <= 0) {
                        continue;
                    }
                    int targetA = Math.round(a * baseBorderA);
                    int borderPixel = (borderR << 24) | (borderG << 16) | (borderB << 8) | targetA;

                    for (int dy = -b; dy <= b; dy++) {
                        for (int dx = -b; dx <= b; dx++) {
                            if (dx * dx + dy * dy <= bSq) {
                                int tx = x + b + dx;
                                int ty = y + b + dy;
                                int existing = baked.getPixel(tx, ty);
                                if ((existing & 0xff) < targetA) {
                                    baked.drawPixel(tx, ty, borderPixel);
                                }
                            }
                        }
                    }
                }
            }

            baked.drawPixmap(srcPixmap, b, b);
            return baked;
        }
    }
}