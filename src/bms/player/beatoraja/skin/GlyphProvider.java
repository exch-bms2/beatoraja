package bms.player.beatoraja.skin;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

/**
 * 主フォント、続いて各代替フォントを順に試して、コードポイントのグリフを探す。
 *
 * <p>フォントは一つずつ試す。あるフォントでの失敗は {@link FailureSink} に一度だけ報告し、次のフォントを試す。
 * ラスタ化したグリフを詰める {@link GlyphPacker} は呼び出し側が渡すので、詰められないグリフでも次のフォントに進む。</p>
 *
 * Finds a glyph for a code point by trying the main font and then each fallback font in order.
 *
 * <p>Each font is tried on its own: a failure in one font is reported once through the {@link FailureSink} and the
 * next font is tried. The caller supplies the {@link GlyphPacker} that turns a rasterised glyph into a packed one, so
 * a glyph that cannot be packed also lets the next font be tried.</p>
 */
final class GlyphProvider {
    private static final int[] MISSING_GLYPH_CANDIDATES = { 0x25a1, 0x25a2, 0x2610, 0x25a0, '?' }; // □, ▢, ☐, ■, ?

    enum Kind {
        /** いずれかのフォントがラスタ化してグリフを作った / a font produced the glyph by rasterising it */
        FALLBACK_RASTER,
        /** どのフォントにもない / no font has it */
        MISSING,
        /** どのフォントも作れず、少なくとも一つがエラーで失敗した / no font produced it and at least one font failed with an error */
        CANDIDATE_FAILED
    }

    static final class Result {
        final Kind kind;
        final BitmapFont.Glyph glyph;

        private Result(Kind kind, BitmapFont.Glyph glyph) {
            this.kind = kind;
            this.glyph = glyph;
        }
    }

    /**
     * {@code renderedCodePoint} のラスタ化グリフを詰めたグリフに変える。詰められなければ null。
     * Turns a rasterised glyph of {@code renderedCodePoint} into a packed glyph, or null when it cannot be packed.
     */
    interface GlyphPacker {
        BitmapFont.Glyph pack(FreeTypeFontGenerator.GlyphAndBitmap raster, int renderedCodePoint) throws Exception;
    }

    interface FailureSink {
        void report(int codePoint, Exception cause);
    }

    /**
     * 基準線を比べる文字。基準線の上に載る大文字、英字のないフォント用に最後に数字の 0。
     * Reference characters for baseline comparison: capitals that sit on the baseline, then the digit 0 for fonts without letters.
     */
    private static final int[] BASELINE_REFERENCES = { 'H', 'E', 'T', 'M', 'N', 'P', 'R', 'A', '0' };

    private final SkinFontSource source;
    private final FreeTypeFontGenerator.FreeTypeFontParameter parameter;
    private final FailureSink sink;
    /** 代替グリフの基準線を供給元の基本ジェネレーターにそろえるか。 / Whether fallback glyph baselines are aligned to the source's base generator. */
    private final boolean alignToBase;
    private final Map<FreeTypeFontGenerator, Integer> baselineShifts = new IdentityHashMap<>();

    GlyphProvider(SkinFontSource source, FreeTypeFontGenerator.FreeTypeFontParameter parameter, FailureSink sink) {
        this(source, parameter, sink, false);
    }

    /**
     * {@code alignToBase} は、グリフを {@code source.getGenerator()} から作ったフォントに足す呼び出し側(FontSession)用。
     * {@code alignToBase} is for callers that add glyphs to a font generated from {@code source.getGenerator()} (FontSession).
     */
    GlyphProvider(SkinFontSource source, FreeTypeFontGenerator.FreeTypeFontParameter parameter, FailureSink sink,
            boolean alignToBase) {
        this.source = source;
        this.parameter = parameter;
        this.sink = sink;
        this.alignToBase = alignToBase;
    }

    /**
     * {@code generateGlyphAndBitmap} はそのフォント自身の ascender で基準線を置くが、生成済みフォントのグリフは基本フォントの
     * ascender を使う。基準文字の縦位置の差を返し、代替グリフをそれだけずらして基準線をそろえる。
     * {@code generateGlyphAndBitmap} places the baseline with each font's own ascender, while the generated font's glyphs use
     * the base font's ascender. Returns the difference of a reference letter's vertical offset, which aligns the baselines.
     */
    private int baselineShift(FreeTypeFontGenerator generator) {
        FreeTypeFontGenerator base = source.getGenerator();
        if (!alignToBase || base == null || generator == base) {
            return 0;
        }
        return baselineShifts.computeIfAbsent(generator, fallback -> {
            for (int codePoint : BASELINE_REFERENCES) {
                if (!source.mayHaveGlyph(base, codePoint) || !source.mayHaveGlyph(fallback, codePoint)) {
                    continue;
                }
                try {
                    int baseOffset = referenceOffset(base, codePoint);
                    int fallbackOffset = referenceOffset(fallback, codePoint);
                    if (baseOffset != Integer.MIN_VALUE && fallbackOffset != Integer.MIN_VALUE) {
                        return baseOffset - fallbackOffset;
                    }
                } catch (RuntimeException ignored) {
                    // 次の基準文字を試す。 / Try the next reference letter.
                }
            }
            return 0;
        });
    }

    private int referenceOffset(FreeTypeFontGenerator generator, int codePoint) {
        FreeTypeFontGenerator.GlyphAndBitmap reference = generator.generateGlyphAndBitmap(codePoint, parameter.size, parameter.flip);
        return reference != null && reference.glyph != null && reference.glyph.height > 0
                ? reference.glyph.yoffset : Integer.MIN_VALUE;
    }

    /** 主フォント({@code fallbackOnly} でなければ)、続いて代替フォントの順。 / Main font first (unless {@code fallbackOnly}), then the fallback fonts in order. */
    Result resolve(int codePoint, boolean fallbackOnly, GlyphPacker packer) {
        return resolvePrepared(codePoint, fallbackOnly, generator -> packer);
    }

    /**
     * ラスタ化が FreeType のグリフ枠を使う前に、フォントごとの詰め込みを準備する。
     * スキン固有の主フォントがある場合はそれを先に試し、続いて {@link SkinFontSource#getFallbackCandidates(int)} の
     * 区間インデックス優先順(スキン指定代替 → 共通代替の区間所有者 → 次点候補)で試す。
     *
     * Prepares per-font packing before rasterization can occupy the FreeType glyph slot.
     * Tries the skin's own primary font first (when {@code !fallbackOnly}), then iterates through
     * {@link SkinFontSource#getFallbackCandidates(int)} so interval-indexed locale winners and failover candidates are respected.
     */
    Result resolvePrepared(int codePoint, boolean fallbackOnly,
            Function<FreeTypeFontGenerator, GlyphPacker> preparePacker) {
        FreeTypeFontGenerator primary = source.getPrimaryGenerator();
        // スケーラブル主フォントを持つスキンで fallbackOnly が指定された場合は、FontSession 側で既に試した getGenerator() を除外する。
        // When fallbackOnly is requested on a scalable-primary source, skip the generator already used by FontSession.
        FreeTypeFontGenerator excluded = fallbackOnly
                ? (source.isScalablePrimary() ? source.getGenerator() : primary)
                : primary;
        boolean failed = false;
        if (!fallbackOnly && primary != null) {
            Attempt attempt = attemptPrepared(primary, codePoint, preparePacker);
            if (attempt.glyph != null) {
                return new Result(Kind.FALLBACK_RASTER, attempt.glyph);
            }
            failed |= attempt.failed;
        }
        for (FreeTypeFontGenerator fallbackGenerator : source.getFallbackCandidates(codePoint)) {
            if (fallbackGenerator == excluded) {
                continue;
            }
            Attempt attempt = attemptPrepared(fallbackGenerator, codePoint, preparePacker);
            if (attempt.glyph != null) {
                return new Result(Kind.FALLBACK_RASTER, attempt.glyph);
            }
            failed |= attempt.failed;
        }
        return new Result(failed ? Kind.CANDIDATE_FAILED : Kind.MISSING, null);
    }

    /**
     * 四角に似た欠落記号をフォントから探す。記号ごとに全フォントを試してから次の記号へ進む。
     * Looks for a box-like marker glyph in the fonts, trying each marker code point in every font before the next one.
     */
    Result resolveMissingMarker(GlyphPacker packer) {
        FreeTypeFontGenerator primary = source.getPrimaryGenerator();
        boolean failed = false;
        for (int candidate : MISSING_GLYPH_CANDIDATES) {
            if (primary != null) {
                Attempt attempt = attempt(primary, candidate, packer);
                if (attempt.glyph != null) {
                    return new Result(Kind.FALLBACK_RASTER, attempt.glyph);
                }
                failed |= attempt.failed;
            }
            for (FreeTypeFontGenerator fallbackGenerator : source.getFallbackCandidates(candidate)) {
                if (fallbackGenerator == primary) {
                    continue;
                }
                Attempt attempt = attempt(fallbackGenerator, candidate, packer);
                if (attempt.glyph != null) {
                    return new Result(Kind.FALLBACK_RASTER, attempt.glyph);
                }
                failed |= attempt.failed;
            }
        }
        return new Result(failed ? Kind.CANDIDATE_FAILED : Kind.MISSING, null);
    }

    private static final class Attempt {
        final BitmapFont.Glyph glyph;
        final boolean failed;

        private Attempt(BitmapFont.Glyph glyph, boolean failed) {
            this.glyph = glyph;
            this.failed = failed;
        }
    }

    // 送り幅だけでは見える形がある証拠にならない。見える文字は次のフォントに任せる。
    // Advance alone does not prove visible ink: let the next font supply visible characters.
    static boolean allowsEmptyRaster(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)
                || Character.getType(codePoint) == Character.FORMAT || Character.isISOControl(codePoint);
    }

    private Attempt attempt(FreeTypeFontGenerator generator, int codePoint, GlyphPacker packer) {
        return attemptPrepared(generator, codePoint, ignored -> packer);
    }

    private Attempt attemptPrepared(FreeTypeFontGenerator generator, int codePoint,
            Function<FreeTypeFontGenerator, GlyphPacker> preparePacker) {
        if (!source.mayHaveGlyph(generator, codePoint)) {
            return new Attempt(null, false);
        }
        try {
            GlyphPacker packer = preparePacker.apply(generator);
            // 基準文字のラスタ化はグリフ枠を上書きするので、対象のラスタ化より先に求める。
            // Reference rasterization overwrites the glyph slot, so compute the shift before rasterizing the target.
            int shift = baselineShift(generator);
            FreeTypeFontGenerator.GlyphAndBitmap raster = generator.generateGlyphAndBitmap(codePoint, parameter.size, parameter.flip);
            if (raster == null || raster.glyph == null) {
                return new Attempt(null, false);
            }
            raster.glyph.yoffset += shift;
            if (raster.bitmap == null || raster.bitmap.getWidth() == 0 || raster.bitmap.getRows() == 0) {
                // 空白文字または不可視グリフ（全角スペース、ノーブレークスペース等）の処理
                // Handle whitespace or invisible glyphs (e.g. ideographic full-width space, non-breaking space)
                if (allowsEmptyRaster(codePoint)) {
                    BitmapFont.Glyph glyph = packer.pack(raster, codePoint);
                    if (glyph != null) {
                        return new Attempt(glyph, false);
                    }
                }
                return new Attempt(null, false);
            }
            BitmapFont.Glyph glyph = packer.pack(raster, codePoint);
            if (glyph == null) {
                if (sink != null) {
                    sink.report(codePoint, new IllegalStateException("Glyph packing failed"));
                }
                return new Attempt(null, true);
            }
            return new Attempt(glyph, false);
        } catch (Exception e) {
            if (sink != null) {
                sink.report(codePoint, e);
            }
            return new Attempt(null, true);
        }
    }
}