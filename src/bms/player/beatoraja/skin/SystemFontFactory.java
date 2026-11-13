package bms.player.beatoraja.skin;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntMap;
import bms.player.beatoraja.skin.FontRenderRetirement.FontResourceDisposer;

/**
 * 共有のオンデマンド代替 {@link BitmapFont} を作るファクトリー。
 *
 * <p>{@link SkinFontSource}・{@link FontSession}・{@link GlyphProvider} を {@link OnDemandFallbackFontData} 付きの
 * {@link FallbackBitmapFont} にまとめる。最初にラスタ化しなかったグリフを呼び出し側(libGDX Scene2D の {@code TextField} を含む)が
 * 求めると、代替フォントチェーンから必要になった時点で解決する。</p>
 *
 * <p>返したフォントの {@link BitmapFont#dispose()} を呼ぶと、{@link FontSession}・ページ {@code Texture}・{@code PixmapPacker}・
 * {@link SkinFontSource} の貸与を {@link FontResourceDisposer} で自動的に解放する。</p>
 *
 * Factory for creating shared on-demand fallback {@link BitmapFont} instances.
 *
 * <p>Wraps a {@link SkinFontSource}, a {@link FontSession}, and a {@link GlyphProvider}
 * into a {@link FallbackBitmapFont} with {@link OnDemandFallbackFontData}. Glyphs requested
 * by callers (including libGDX Scene2D {@code TextField}) that were not rasterized initially
 * are resolved lazily on demand from the fallback font chain.</p>
 *
 * <p>Calling {@link BitmapFont#dispose()} on the returned font automatically releases the
 * {@link FontSession}, page {@code Texture}s, {@code PixmapPacker}, and {@link SkinFontSource}
 * leases via {@link FontResourceDisposer}.</p>
 */
public final class SystemFontFactory {

    private SystemFontFactory() {
    }

    public static BitmapFont createFont(FileHandle primaryFontFile,
                                        FreeTypeFontGenerator.FreeTypeFontParameter parameter) {
        // 共通代替フォントは SkinFontSource が後ろに付ける。 / SkinFontSource appends the global fallback fonts.
        return createFont(primaryFontFile, parameter, new String[0]);
    }

    public static BitmapFont createFont(String primaryFontPath,
                                        FreeTypeFontGenerator.FreeTypeFontParameter parameter) {
        return createFont(primaryFontPath, parameter, new String[0]);
    }

    public static BitmapFont createFont(String primaryFontPath,
                                        FreeTypeFontGenerator.FreeTypeFontParameter parameter,
                                        String[] fallbackFontPaths) {
        if (primaryFontPath == null || primaryFontPath.isEmpty()) {
            return null;
        }
        FileHandle handle = Gdx.files != null ? Gdx.files.internal(primaryFontPath) : null;
        return createFont(primaryFontPath, handle, parameter, fallbackFontPaths);
    }

    public static BitmapFont createFont(FileHandle primaryFontFile,
                                        FreeTypeFontGenerator.FreeTypeFontParameter parameter,
                                        String[] fallbackFontPaths) {
        if (primaryFontFile == null) {
            return null;
        }
        return createFont(primaryFontFile.path(), primaryFontFile, parameter, fallbackFontPaths);
    }

    private static BitmapFont createFont(String primaryFontPath,
                                         FileHandle primaryFontFile,
                                         FreeTypeFontGenerator.FreeTypeFontParameter parameter,
                                         String[] fallbackFontPaths) {
        SkinFontSource source = primaryFontFile != null
                ? new SkinFontSource(primaryFontFile, fallbackFontPaths)
                : new SkinFontSource(primaryFontPath, fallbackFontPaths);
        return create(source, parameter, true);
    }

    /**
     * 呼び出し側が持つ供給元を借りて作る。返したフォントを解放しても、作れなかったときも、供給元は解放しない。
     * Builds on a source the caller owns. Neither disposing the returned font nor a failure releases the source.
     */
    public static BitmapFont createFont(SkinFontSource source, FreeTypeFontGenerator.FreeTypeFontParameter parameter) {
        return source != null ? create(source, parameter, false) : null;
    }

    private static BitmapFont create(SkinFontSource source, FreeTypeFontGenerator.FreeTypeFontParameter parameter,
                                     boolean ownsSource) {
        if (!source.isAvailable()) {
            if (ownsSource) source.dispose();
            return null;
        }

        FreeTypeFontGenerator generator = source.getGenerator();
        if (generator == null) {
            if (ownsSource) source.dispose();
            return null;
        }

        FreeTypeFontGenerator.FreeTypeFontParameter paramCopy = new FreeTypeFontGenerator.FreeTypeFontParameter();
        if (parameter != null) {
            paramCopy.size = parameter.size;
            paramCopy.mono = parameter.mono;
            paramCopy.hinting = parameter.hinting;
            paramCopy.color = parameter.color;
            paramCopy.gamma = parameter.gamma;
            paramCopy.renderCount = parameter.renderCount;
            paramCopy.borderWidth = parameter.borderWidth;
            paramCopy.borderColor = parameter.borderColor;
            paramCopy.borderStraight = parameter.borderStraight;
            paramCopy.borderGamma = parameter.borderGamma;
            paramCopy.shadowOffsetX = parameter.shadowOffsetX;
            paramCopy.shadowOffsetY = parameter.shadowOffsetY;
            paramCopy.shadowColor = parameter.shadowColor;
            paramCopy.spaceX = parameter.spaceX;
            paramCopy.spaceY = parameter.spaceY;
            paramCopy.characters = parameter.characters != null ? parameter.characters : "";
            paramCopy.kerning = parameter.kerning;
            paramCopy.flip = parameter.flip;
            paramCopy.genMipMaps = parameter.genMipMaps;
            paramCopy.minFilter = parameter.minFilter;
            paramCopy.magFilter = parameter.magFilter;
        }
        paramCopy.incremental = false;

        FontSession session = null;
        try {
            session = FontSession.open(generator, paramCopy);
            BitmapFont baseFont = session.font();

            FailureLogger failureLogger = new FailureLogger();
            GlyphProvider glyphProvider = new GlyphProvider(source, paramCopy, failureLogger, true);

            OnDemandFallbackFontData data = new OnDemandFallbackFontData(baseFont.getData(), glyphProvider, session);
            FallbackBitmapFont fallbackFont = new FallbackBitmapFont(data, baseFont.getRegions(),
                    baseFont.usesIntegerPositions(), session, ownsSource ? source : null);
            session.setFont(fallbackFont);
            return fallbackFont;
        } catch (RuntimeException e) {
            if (session != null) {
                session.dispose();
            }
            if (ownsSource) source.dispose();
            throw e;
        }
    }

    private static final class FailureLogger implements GlyphProvider.FailureSink {
        private final Set<Integer> reported = new HashSet<>();

        @Override
        public void report(int codePoint, Exception cause) {
            if (reported.add(codePoint)) {
                Logger.getGlobal().warning(String.format("Glyph generation failed: U+%04X (%s)", codePoint,
                        cause != null ? cause.getClass().getSimpleName() : "null"));
            }
        }
    }

    /**
     * BitmapFontData that dynamically resolves missing glyphs on-demand across the fallback font chain.
     * <p>
     * [日本語] フォールバックフォント一覧から不足グリフをオンデマンドで動的解決する BitmapFontData です。
     * 検索入力欄（{@code SkinTextInput}）における補助平面文字（SMP: U+10000+ 絵文字）のサロゲートペアにも対応します。
     */
    public static class OnDemandFallbackFontData extends BitmapFont.BitmapFontData {
        private final GlyphProvider glyphProvider;
        private final FontSession session;
        private final Set<Integer> failedCodePoints = new HashSet<>();
        private final IntMap<BitmapFont.Glyph> supplementaryGlyphs = new IntMap<>();
        private boolean renewalScheduled;
        /**
         * Dummy glyph for low surrogate halves, clipped pairs and invisible controls with zero advance.
         * <p>
         * [日本語] 下位サロゲート用の advance=0 ダミーグリフ。
         * LibGDX TextField の glyphPositions 配列長（text.length + 1）との 1:1 整合性を保ち、
         * カーソル移動や末尾到達時の IndexOutOfBoundsException を防止します。
         */
        private static final BitmapFont.Glyph ZERO_ADVANCE = new BitmapFont.Glyph();
        static {
            ZERO_ADVANCE.width = 0;
            ZERO_ADVANCE.height = 0;
            ZERO_ADVANCE.xadvance = 0;
            ZERO_ADVANCE.page = 0;
            ZERO_ADVANCE.fixedWidth = true;
            ZERO_ADVANCE.id = ' ';
        }

        public OnDemandFallbackFontData(BitmapFont.BitmapFontData sourceData,
                                        GlyphProvider glyphProvider,
                                        FontSession session) {
            this.glyphProvider = glyphProvider;
            this.session = session;
            if (sourceData != null) {
                this.imagePaths = sourceData.imagePaths;
                this.fontFile = sourceData.fontFile;
                this.flipped = sourceData.flipped;
                this.padTop = sourceData.padTop;
                this.padRight = sourceData.padRight;
                this.padBottom = sourceData.padBottom;
                this.padLeft = sourceData.padLeft;
                this.lineHeight = sourceData.lineHeight;
                this.capHeight = sourceData.capHeight;
                this.ascent = sourceData.ascent;
                this.descent = sourceData.descent;
                this.down = sourceData.down;
                this.blankLineScale = sourceData.blankLineScale;
                this.scaleX = sourceData.scaleX;
                this.scaleY = sourceData.scaleY;
                this.markupEnabled = sourceData.markupEnabled;
                this.cursorX = sourceData.cursorX;
                this.missingGlyph = sourceData.missingGlyph;
                this.spaceXadvance = sourceData.spaceXadvance;
                this.xHeight = sourceData.xHeight;
                this.breakChars = sourceData.breakChars;
                this.xChars = sourceData.xChars;
                this.capChars = sourceData.capChars;

                for (int pageIdx = 0; pageIdx < sourceData.glyphs.length; pageIdx++) {
                    BitmapFont.Glyph[] page = sourceData.glyphs[pageIdx];
                    if (page != null) {
                        this.glyphs[pageIdx] = new BitmapFont.Glyph[page.length];
                        System.arraycopy(page, 0, this.glyphs[pageIdx], 0, page.length);
                    }
                }
            }
        }

        @Override
        public BitmapFont.Glyph getGlyph(char ch) {
            if (isInvisibleControl(ch)) {
                return ZERO_ADVANCE;
            }
            BitmapFont.Glyph glyph = super.getGlyph(ch);
            if (glyph == null && ch != 0) {
                glyph = resolveOnDemand(ch);
            }
            return glyph;
        }

        /** ZWNJ・ZWJ と異体字セレクター VS1–VS16(U+FE00–FE0F)。 / ZWNJ, ZWJ and the variation selectors VS1–VS16 (U+FE00–FE0F). */
        private static boolean isInvisibleControl(char ch) {
            return ch == '\u200C' || ch == '\u200D' || (ch >= '\uFE00' && ch <= '\uFE0F');
        }

        /** 異体字セレクター補助 VS17–VS256(U+E0100–E01EF)。 / Variation Selectors Supplement VS17–VS256 (U+E0100–E01EF). */
        private static boolean isSupplementaryVariationSelector(int codePoint) {
            return codePoint >= 0xE0100 && codePoint <= 0xE01EF;
        }

        private synchronized BitmapFont.Glyph resolveOnDemand(char ch) {
            BitmapFont.Glyph existing = super.getGlyph(ch);
            if (existing != null) {
                return existing;
            }
            if (glyphProvider == null || session == null) {
                return null;
            }
            if (failedCodePoints.contains((int) ch)) {
                return null;
            }
            try {
                GlyphProvider.Result result = glyphProvider.resolve(ch, false,
                        (raster, renderedCodePoint) -> session.pack(raster, renderedCodePoint, ch));
                BitmapFont.Glyph resolved = result.glyph;
                if (resolved == null) {
                    GlyphProvider.Result markerResult = glyphProvider.resolveMissingMarker(
                            (raster, renderedCodePoint) -> session.pack(raster, renderedCodePoint, ch));
                    resolved = markerResult.glyph;
                }
                if (resolved == null) {
                    resolved = session.packMissingBox(ch);
                }
                if (resolved != null) {
                    setGlyph(ch, resolved);
                    scheduleRenewal();
                    return resolved;
                } else {
                    failedCodePoints.add((int) ch);
                }
            } catch (Exception e) {
                failedCodePoints.add((int) ch);
            }
            return null;
        }

        /**
         * 代替ページが上限に達したら、フレームの区切りで新しい代替ページへ移る。レイアウトの途中ではページを捨てない。
         * When the fallback pages reach their cap, move to fresh ones at the frame boundary; pages are never dropped mid-layout.
         */
        private void scheduleRenewal() {
            if (renewalScheduled || session == null || !session.isLive() || !session.atlas().isFull() || Gdx.app == null) {
                return;
            }
            renewalScheduled = true;
            Gdx.app.postRunnable(this::renewFallbackPages);
        }

        /** 代替ページ上のグリフを捨てて新しい代替ページへ移る。捨てた文字は次に必要になったとき作り直す。 / Drops glyphs on fallback pages and moves to fresh ones; dropped characters are rebuilt when next needed. */
        synchronized void renewFallbackPages() {
            renewalScheduled = false;
            if (session == null) {
                return;
            }
            session.renewFallbackAtlas();
            com.badlogic.gdx.utils.IntArray dropped = new com.badlogic.gdx.utils.IntArray();
            for (IntMap.Entry<BitmapFont.Glyph> entry : supplementaryGlyphs.entries()) {
                if (session.isFallbackPage(entry.value.page)) dropped.add(entry.key);
            }
            for (int i = 0; i < dropped.size; i++) {
                supplementaryGlyphs.remove(dropped.get(i));
            }
        }

        /**
         * Resolves a 32-bit supplementary code point (e.g. SMP emojis) using fallback font providers.
         * <p>
         * [日本語] フォールバックフォント（NotoEmoji 等）から 32 ビット補助平面コードポイント（U+10000+）を
         * 検索・ラスタライズしてキャッシュします。
         *
         * @param codePoint 32-bit Unicode code point
         * @return resolved Glyph, or null if unresolvable
         */
        synchronized BitmapFont.Glyph resolveSupplementary(int codePoint) {
            BitmapFont.Glyph existing = supplementaryGlyphs.get(codePoint);
            if (existing != null) {
                return existing;
            }
            if (glyphProvider == null || session == null || failedCodePoints.contains(codePoint)) {
                return null;
            }
            try {
                GlyphProvider.Result result = glyphProvider.resolve(codePoint, false,
                        (raster, renderedCp) -> session.pack(raster, renderedCp, codePoint));
                BitmapFont.Glyph resolved = result != null ? result.glyph : null;
                if (resolved == null) {
                    GlyphProvider.Result markerResult = glyphProvider.resolveMissingMarker(
                            (raster, renderedCp) -> session.pack(raster, renderedCp, codePoint));
                    resolved = markerResult != null ? markerResult.glyph : null;
                }
                if (resolved == null) {
                    resolved = session.packMissingBox(codePoint);
                }
                if (resolved != null) {
                    supplementaryGlyphs.put(codePoint, resolved);
                    scheduleRenewal();
                    return resolved;
                } else {
                    failedCodePoints.add(codePoint);
                }
            } catch (Exception e) {
                failedCodePoints.add(codePoint);
            }
            return null;
        }

        /**
         * Checks whether a glyph exists or can be resolved.
         * Returns true for surrogate halves to prevent {@code TextField.updateDisplayText()} from replacing them with spaces.
         * <p>
         * [日本語] 指定文字のグリフが存在するか判定します。サロゲート文字（0xD800..0xDFFF）に対しても true を返し、
         * LibGDX TextField がサロゲート文字を空白スペースに置換するのを防止します。
         */
        @Override
        public boolean hasGlyph(char ch) {
            if (Character.isSurrogate(ch)) {
                return true;
            }
            if (super.hasGlyph(ch)) {
                return true;
            }
            return getGlyph(ch) != null;
        }

        /**
         * Populates glyph runs and advance values for layout.
         * High/low surrogate pairs are combined into 32-bit code points:
         * the high surrogate receives the actual emoji glyph, while the low surrogate receives ZERO_ADVANCE,
         * preserving exact 1:1 cursor-advance array alignment for Scene2D TextField.
         * <p>
         * [日本語] レイアウト用のグリフおよび advance 配列を生成します。
         * 上位/下位サロゲートペアを検出し、上位サロゲート位置に実際の 32 ビット絵文字グリフを、
         * 下位サロゲート位置に ZERO_ADVANCE ダミーグリフを出力することで、1 文字分の描画と
         * TextField のカーソルステップ整合性を両立します。
         */
        @Override
        public void getGlyphs(GlyphLayout.GlyphRun run, CharSequence str, int start, int end, BitmapFont.Glyph lastGlyph) {
            if (start == end) return;
            float scaleX = this.scaleX;
            BitmapFont.Glyph missing = this.missingGlyph;
            Array<BitmapFont.Glyph> glyphs = run.glyphs;
            FloatArray xAdvances = run.xAdvances;

            glyphs.ensureCapacity(end - start);
            xAdvances.ensureCapacity(end - start + 1);

            int i = start;
            while (i < end) {
                char ch = str.charAt(i);
                // 代替グリフを解決するときも BitmapFontData の CR・マークアップのエスケープ規則を保つ。下の UTF-16 ペア処理はその追加。
                // Keep BitmapFontData's CR/markup escape contract when resolving
                // fallback glyphs; the UTF-16 pair handling below is additional.
                if (ch == '\r') { i++; continue; }
                if (Character.isHighSurrogate(ch) && i + 1 < end && Character.isLowSurrogate(str.charAt(i + 1))) {
                    char low = str.charAt(i + 1);
                    int codePoint = Character.toCodePoint(ch, low);
                    // 異体字セレクターは前の文字の形を選ぶだけで、それ自体の字形・幅を持たない。
                    // A variation selector only selects the previous character's form and has no glyph or advance of its own.
                    BitmapFont.Glyph emojiGlyph = isSupplementaryVariationSelector(codePoint) ? ZERO_ADVANCE
                            : resolveSupplementary(codePoint);
                    if (emojiGlyph == null) {
                        emojiGlyph = missing;
                    }
                    if (emojiGlyph == null) {
                        emojiGlyph = ZERO_ADVANCE;
                    }

                    // 1. 上位サロゲートの位置: 実際の絵文字グリフを出す / High surrogate position: emits real emoji glyph
                    glyphs.add(emojiGlyph);
                    if (lastGlyph == null) {
                        xAdvances.add(emojiGlyph.fixedWidth ? 0 : -emojiGlyph.xoffset * scaleX - padLeft);
                    } else {
                        xAdvances.add((lastGlyph.xadvance + lastGlyph.getKerning(ch)) * scaleX);
                    }
                    lastGlyph = emojiGlyph;

                    // 2. 下位サロゲートの位置: 送り幅 0 のダミーグリフを出す / Low surrogate position: emits ZERO_ADVANCE dummy glyph
                    glyphs.add(ZERO_ADVANCE);
                    xAdvances.add((lastGlyph.xadvance + lastGlyph.getKerning(low)) * scaleX);
                    lastGlyph = ZERO_ADVANCE;

                    i += 2;
                    continue;
                }

                // 入力全体を見る。start/end で切れた本物のペアだけを見えなくする。単独のサロゲートは通常の欠落記号・四角にする。
                // Consult the complete input: only a real pair cut by start/end is invisible.
                // A genuine orphan must still resolve to the normal missing marker/box.
                boolean clippedPair = (i == start && Character.isLowSurrogate(ch) && i > 0
                        && Character.isHighSurrogate(str.charAt(i - 1)))
                        || (i == end - 1 && Character.isHighSurrogate(ch) && i + 1 < str.length()
                        && Character.isLowSurrogate(str.charAt(i + 1)));
                BitmapFont.Glyph glyph = clippedPair ? ZERO_ADVANCE : getGlyph(ch);
                if (glyph == null) {
                    if (missing == null) {
                        i++;
                        continue;
                    }
                    glyph = missing;
                }

                glyphs.add(glyph);
                if (lastGlyph == null) {
                    xAdvances.add(glyph.fixedWidth ? 0 : -glyph.xoffset * scaleX - padLeft);
                } else {
                    xAdvances.add((lastGlyph.xadvance + lastGlyph.getKerning(ch)) * scaleX);
                }
                lastGlyph = glyph;
                i++;
                if (markupEnabled && ch == '[' && i < end && str.charAt(i) == '[') i++;
            }

            if (lastGlyph != null) {
                float amount = lastGlyph.fixedWidth ? lastGlyph.xadvance * scaleX
                        : (lastGlyph.xoffset + lastGlyph.width) * scaleX - padRight;
                xAdvances.add(amount);
            }
        }

        GlyphProvider glyphProvider() {
            return glyphProvider;
        }

        public FontSession session() {
            return session;
        }
    }

    /**
     * 解放すると FontSession と SkinFontSource の後始末まで安全に行う BitmapFont の派生クラス。
     * Subclass of BitmapFont whose disposal safely triggers full cleanup of FontSession and SkinFontSource.
     */
    public static class FallbackBitmapFont extends BitmapFont {
        private final FontSession session;
        private final Disposable source;
        private boolean disposed;

        public FallbackBitmapFont(BitmapFontData data,
                                  Array<TextureRegion> pageRegions,
                                  boolean integer,
                                  FontSession session,
                                  Disposable source) {
            super(data, pageRegions, integer);
            this.session = session;
            this.source = source;
        }

        public FontSession session() {
            return session;
        }

        public Disposable source() {
            return source;
        }

        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public void dispose() {
            if (disposed) {
                return;
            }
            disposed = true;
            try {
                super.dispose();
            } catch (Throwable ignored) {
            }
            FontResourceDisposer.disposeAll(session != null ? session.resourcesWithoutFont() : new Disposable[0], source);
        }
    }
}
