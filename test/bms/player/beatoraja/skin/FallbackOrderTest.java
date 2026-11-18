package bms.player.beatoraja.skin;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 共通代替フォントの順序と区間インデックス: ロケールとの照合は正規 BCP 47 で、根拠は meta.dlng > meta.slng > cmap の文字体系。
 * name 表の言語やファイル名は根拠にしない。文字のないフォントは最後。
 * Global fallback order and interval index: locale matching in canonical BCP 47 with meta.dlng over meta.slng over the
 * cmap's scripts as evidence; name table languages and file names are never evidence; fonts without letters go last.
 */
public final class FallbackOrderTest {
    private static final int SFNT = 0x00010000, CMAP = 0x636d6170, NAME = 0x6e616d65, META = 0x6d657461;
    private static final int FVAR = 0x66766172, WDTH = 0x77647468, WGHT = 0x77676874;
    private static final int DLNG = 0x646c6e67, SLNG = 0x736c6e67, PLATFORM_WINDOWS = 3, LANGUAGE_KOREAN = 0x0412;
    private static int checks;
    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }

    /** 表を並べた最小の書体。 / A minimal face holding the given tables. */
    private static byte[] sfnt(Map<Integer, byte[]> tables) {
        int directory = 12 + 16 * tables.size(), size = directory;
        for (byte[] t : tables.values()) size += t.length;
        ByteBuffer b = ByteBuffer.allocate(size);
        b.putInt(SFNT).putShort((short) tables.size()).putShort((short) 0).putShort((short) 0).putShort((short) 0);
        int at = directory;
        for (var e : tables.entrySet()) {
            b.putInt(e.getKey()).putInt(0).putInt(at).putInt(e.getValue().length);
            at += e.getValue().length;
        }
        for (byte[] t : tables.values()) b.put(t);
        return b.array();
    }

    /** format 12 の cmap。範囲は {開始, 終了}。 / A format 12 cmap; ranges are {start, end}. */
    private static byte[] cmap(int[]... ranges) {
        int subtable = 16 + 12 * ranges.length;
        ByteBuffer b = ByteBuffer.allocate(12 + subtable);
        b.putShort((short) 0).putShort((short) 1).putShort((short) PLATFORM_WINDOWS).putShort((short) 10).putInt(12);
        b.putShort((short) 12).putShort((short) 0).putInt(subtable).putInt(0).putInt(ranges.length);
        int glyph = 1;
        for (int[] r : ranges) {
            b.putInt(r[0]).putInt(r[1]).putInt(glyph);
            glyph += r[1] - r[0] + 1;
        }
        return b.array();
    }

    /** Windows 言語 ID だけの name 表。 / A name table with Windows language IDs only. */
    private static byte[] name(int... windowsLanguages) {
        ByteBuffer b = ByteBuffer.allocate(6 + 12 * windowsLanguages.length);
        b.putShort((short) 0).putShort((short) windowsLanguages.length).putShort((short) b.capacity());
        for (int language : windowsLanguages) {
            b.putShort((short) PLATFORM_WINDOWS).putShort((short) 1).putShort((short) language).putShort((short) 1).putShort((short) 0).putShort((short) 0);
        }
        return b.array();
    }

    /** dlng・slng を持つ meta 表(null は省く)。 / A meta table with dlng and slng (null leaves it out). */
    private static byte[] meta(String dlng, String slng) {
        List<int[]> maps = new ArrayList<>();
        List<byte[]> data = new ArrayList<>();
        if (dlng != null) { maps.add(new int[] { DLNG }); data.add(dlng.getBytes(StandardCharsets.UTF_8)); }
        if (slng != null) { maps.add(new int[] { SLNG }); data.add(slng.getBytes(StandardCharsets.UTF_8)); }
        int at = 16 + 12 * maps.size(), size = at;
        for (byte[] d : data) size += d.length;
        ByteBuffer b = ByteBuffer.allocate(size);
        b.putInt(1).putInt(0).putInt(0).putInt(maps.size());
        for (int i = 0; i < maps.size(); i++) {
            b.putInt(maps.get(i)[0]).putInt(at).putInt(data.get(i).length);
            at += data.get(i).length;
        }
        for (byte[] d : data) b.put(d);
        return b.array();
    }

    /** 軸と名前付きインスタンスの座標を持つ fvar 表。 / An fvar table with the given axes and instance coordinates. */
    private static byte[] fvar(int[] axes, int[][] instances) {
        int axesOffset = 16, instanceSize = 4 + 4 * axes.length;
        ByteBuffer b = ByteBuffer.allocate(axesOffset + 20 * axes.length + instanceSize * instances.length);
        b.putShort((short) 1).putShort((short) 0).putShort((short) axesOffset).putShort((short) 2)
                .putShort((short) axes.length).putShort((short) 20).putShort((short) instances.length).putShort((short) instanceSize);
        for (int axis : axes) b.putInt(axis).putInt(100 << 16).putInt(100 << 16).putInt(900 << 16).putShort((short) 0).putShort((short) 256);
        for (int[] coordinates : instances) {
            b.putShort((short) 257).putShort((short) 0);
            for (int c : coordinates) b.putInt(c << 16);
        }
        return b.array();
    }

    private static FontCoverage variable(byte[] fvarTable) {
        Map<Integer, byte[]> tables = new LinkedHashMap<>();
        tables.put(CMAP, cmap(new int[] { 0x20, 0x7e }));
        tables.put(FVAR, fvarTable);
        return FontCoverage.read(sfnt(tables), 0);
    }

    private static FontCoverage font(byte[] metaTable, byte[] nameTable, int[]... ranges) {
        Map<Integer, byte[]> tables = new LinkedHashMap<>();
        tables.put(CMAP, cmap(ranges));
        if (nameTable != null) tables.put(NAME, nameTable);
        if (metaTable != null) tables.put(META, metaTable);
        return FontCoverage.read(sfnt(tables), 0);
    }

    private static int score(FontCoverage font, String locale) {
        return FallbackFontPolicy.localeMatchScore(font, Locale.forLanguageTag(locale));
    }

    private static int match(String font, String locale) {
        return FallbackFontPolicy.tagMatchScore(FallbackFontPolicy.canonicalBcp47(font), FallbackFontPolicy.canonicalBcp47(locale));
    }

    private static List<String> names(List<String> paths) {
        List<String> result = new ArrayList<>();
        for (String p : paths) result.add(Path.of(p).getFileName().toString());
        return result;
    }

    public static void main(String[] args) throws Exception {
        // 可変フォントの太さ / variable font weight
        int[][] weights = { { 100, 100 }, { 100, 300 }, { 100, 400 }, { 100, 500 }, { 100, 700 } };
        byte[] widthAndWeight = fvar(new int[] { WDTH, WGHT }, weights);
        FontCoverage variableFont = variable(widthAndWeight);
        check(variableFont.namedInstanceNearest(FallbackFontPolicy.GLOBAL_WEIGHT) == 3 && variableFont.namedInstanceNearest(450) == 2
                && variableFont.namedInstanceNearest(900) == 4, "the named instance nearest to the weight is chosen (the earlier one on a tie)");
        check(variable(fvar(new int[] { WDTH }, new int[][] { { 100 } })).namedInstanceNearest(500) == -1,
                "no wght axis, no instance");
        check(variable(Arrays.copyOf(widthAndWeight, widthAndWeight.length - 3)).namedInstanceNearest(500) == -1,
                "a truncated fvar gives no instance");
        check(font(null, null, new int[] { 0x20, 0x7e }).namedInstanceNearest(500) == -1, "a static font has no instance");

        // 正規 BCP 47 / canonical BCP 47
        check("ko-Kore".equals(FallbackFontPolicy.canonicalBcp47("Kore")) && "ja-Jpan".equals(FallbackFontPolicy.canonicalBcp47("Jpan"))
                && "zh-Hant-HK".equals(FallbackFontPolicy.canonicalBcp47("Hant-HK")), "script-only ScriptLangTags gain their single language");
        check("und-Latn".equals(FallbackFontPolicy.canonicalBcp47("Latn")), "shared scripts never invent a language");
        check("ja-Hani".equals(FallbackFontPolicy.canonicalBcp47("Hani")) && match("Hani", "ja-JP") > 0
                && match("Hani", "ko-KR") == 0 && match("Hani", "zh-CN") == 0 && match("Hani", "zh-TW") == 0,
                "Han without a language is taken as Japanese (MS932 convention) and matches only ja");
        check("ko-Hani".equals(FallbackFontPolicy.canonicalBcp47("ko-Hani")) && "zh-Hani".equals(FallbackFontPolicy.canonicalBcp47("zh-Hani"))
                && match("ko-Hani", "ko-KR") > 0, "an explicit language on Han is kept");
        check("ko-Kore-KR".equals(FallbackFontPolicy.canonicalBcp47(Locale.KOREA)) && "ja-Jpan-JP".equals(FallbackFontPolicy.canonicalBcp47(Locale.JAPAN))
                && "zh-Hant-TW".equals(FallbackFontPolicy.canonicalBcp47("zh-TW")) && "zh-Hans-CN".equals(FallbackFontPolicy.canonicalBcp47("zh_CN"))
                && "en-Latn-US".equals(FallbackFontPolicy.canonicalBcp47(Locale.US)), "locales gain their default script");
        check("ar-EG".equals(FallbackFontPolicy.canonicalBcp47("ar-EG")) && FallbackFontPolicy.canonicalBcp47("x") == null
                && FallbackFontPolicy.canonicalBcp47(" ") == null && FallbackFontPolicy.canonicalBcp47("한국") == null,
                "tags outside the tables are kept as is; malformed tags are null");

        // (language, script, region) の照合 / matching by language, script and region
        check(match("ko-Kore", "ko-KR") > 0 && match("Kore", "ko") > 0 && match("ja-Jpan", "ja-JP") > 0 && match("Hira", "ja-JP") > 0,
                "language and composite script parts match");
        check(match("zh-Hant", "zh-TW") > 0 && match("zh-Hans", "zh-TW") == 0 && match("zh-CN", "zh-TW") == 0,
                "zh-TW matches zh-Hant but not zh-Hans or zh-CN");
        check(match("Latn", "en-US") > 0 && match("Latn", "ko-KR") == 0 && match("ja", "ko-KR") == 0, "und-Latn matches Latin locales only");
        check(match("ko-Kore-KR", "ko-KR") > match("ko-Kore", "ko-KR") && match("ko-Kore", "ko-KR") > match("Hang", "ko-KR")
                && match("Hang", "ko-KR") > 0, "more matching subtags score higher; a composite part still matches");

        // 表の読み取り / reading the tables
        int[] latin = { 0x20, 0x7e }, cjk = { 0x3000, 0x33ff }, halfwidth = { 0xff00, 0xffef }, han = { 0x4e00, 0x9fff },
                someHan = { 0x4e00, 0x5fff }, hangul = { 0xac00, 0xd7a3 }, thai = { 0x0e00, 0x0e7f };
        FontCoverage metaFont = font(meta("Kore", "Jpan, Hant-HK ,x"), null, latin);
        check(metaFont.designLanguages().equals(Set.of("ko-Kore")) && metaFont.supportedLanguages().equals(Set.of("ja-Jpan", "zh-Hant-HK")),
                "meta dlng and slng are read as canonical BCP 47 and kept apart; malformed entries are dropped");
        byte[] broken = meta("Kore", null);
        check(font(Arrays.copyOf(broken, broken.length - 2), null, latin).designLanguages().isEmpty(), "a truncated meta table gives no languages");
        FontCoverage koreanFont = font(null, null, latin, cjk, someHan, hangul, halfwidth);
        check(koreanFont.scripts().contains(Character.UnicodeScript.HANGUL) && !koreanFont.scripts().contains(Character.UnicodeScript.HAN)
                && koreanFont.scripts().contains(Character.UnicodeScript.HIRAGANA), "scripts held at the coverage ratio are provided, partial Han is not");
        Path vl = Path.of("font/VL-Gothic-Regular.ttf");
        if (Files.exists(vl)) {
            FontCoverage real = FontCoverage.read(Files.readAllBytes(vl), 0);
            check(real.scripts().contains(Character.UnicodeScript.HIRAGANA) && real.scripts().contains(Character.UnicodeScript.KATAKANA)
                    && !real.scripts().contains(Character.UnicodeScript.HANGUL), "real VL Gothic provides kana and no Hangul");
        }

        // 根拠の重み / evidence weights
        FontCoverage dlngKorean = font(meta("Kore", null), null, latin, cjk, han);
        FontCoverage slngKorean = font(meta(null, "ko"), null, latin, cjk, han);
        FontCoverage dlngJapaneseWithHangul = font(meta("Jpan", null), null, latin, cjk, han, hangul);
        FontCoverage nameKorean = font(null, name(LANGUAGE_KOREAN), latin, cjk, han);
        check(score(dlngKorean, "ko-KR") > score(slngKorean, "ko-KR") && score(slngKorean, "ko-KR") > score(koreanFont, "ko-KR")
                && score(koreanFont, "ko-KR") > 0, "meta.dlng over meta.slng over cmap");
        check(score(dlngJapaneseWithHangul, "ko-KR") == 0, "meta languages that do not match are not overridden by the cmap");
        check(score(nameKorean, "ko-KR") == 0, "name table languages are not evidence");
        check(score(koreanFont, "en-US") == 0 && score(font(meta("Latn", null), null, latin), "en-US") > 0,
                "the cmap is no evidence for a Latin locale; meta und-Latn is");

        // 並べ直し / reordering
        Map<String, FontCoverage> fonts = new LinkedHashMap<>();
        fonts.put("emoji", font(null, name(0x0409), new int[] { 0x20, 0x20 }, new int[] { 0x23, 0x23 }, new int[] { 0x2a, 0x2a },
                new int[] { 0x30, 0x39 }, new int[] { 0x1f600, 0x1f64f }));
        fonts.put("hk", font(null, null, latin, cjk, han, halfwidth));
        fonts.put("kr", koreanFont);
        fonts.put("thai", font(null, null, latin, thai));
        fonts.put("vl", font(null, null, latin, cjk, someHan, halfwidth));
        fonts.put("metaKR", dlngKorean);
        fonts.put("unknown", FontCoverage.UNKNOWN);
        check(FallbackFontPolicy.lacksLatinLetters(fonts.get("emoji")) && !FallbackFontPolicy.lacksLatinLetters(fonts.get("unknown")),
                "emoji font has digits and the space but no Latin letter; unknown coverage is not judged");
        List<String> found = List.copyOf(fonts.keySet());
        List<String> korean = FallbackFontPolicy.preferLocale(found, fonts::get, Locale.KOREA);
        check(korean.equals(List.of("metaKR", "kr", "hk", "thai", "vl", "unknown", "emoji")), "ko-KR: meta first, then Hangul by cmap, emoji last: " + korean);
        check(FallbackFontPolicy.preferLocale(korean, fonts::get, Locale.KOREA).equals(korean), "ordering is idempotent");
        check(FallbackFontPolicy.preferLocale(found, fonts::get, Locale.JAPAN).equals(List.of("hk", "kr", "vl", "thai", "metaKR", "unknown", "emoji")),
                "ja-JP: every kana font matches by cmap and keeps found order; a Korean meta font does not");
        check(FallbackFontPolicy.preferLocale(found, fonts::get, Locale.US).equals(List.of("hk", "kr", "thai", "vl", "metaKR", "unknown", "emoji")),
                "en-US: found order, emoji last");
        check(FallbackFontPolicy.preferLocale(found, fonts::get, Locale.forLanguageTag("th-TH")).get(0).equals("thai"), "th-TH: Thai first");

        // 文字ごとの選択(起動時に一度だけ作る) / per-character choice, built once at startup
        var segments = new ArrayList<FallbackFontPolicy.GlobalChoice.Segment>();
        FallbackFontPolicy.GlobalChoice.overwrite(segments, 0x30, 0x60, 1);
        FallbackFontPolicy.GlobalChoice.overwrite(segments, 0x41, 0x5a, 0);
        check(segments.size() == 3 && segments.get(1).owner() == 0 && segments.get(0).end() == 0x40 && segments.get(2).start() == 0x5b,
                "overwrite keeps the parts outside the range");
        FallbackFontPolicy.GlobalChoice.overwrite(segments, 0x45, 0x50, 2);
        check(segments.size() == 5, "overwrite splits the segment it lands in");
        FallbackFontPolicy.GlobalChoice.overwrite(segments, 0x41, 0x5a, 1);
        check(segments.size() == 1 && segments.get(0).start() == 0x30 && segments.get(0).end() == 0x60, "neighbours of one owner are joined");
        FallbackFontPolicy.GlobalChoice.overwrite(segments, 0x61, 0x70, 1);
        FallbackFontPolicy.GlobalChoice.overwrite(segments, 0x10, 0x20, 1);
        check(segments.size() == 2 && segments.get(0).start() == 0x10 && segments.get(1).end() == 0x70,
                "adjacent ranges join, separate ones stay apart, order stays sorted");

        var choice = FallbackFontPolicy.GlobalChoice.build(korean, fonts::get);
        check("metaKR".equals(choice.owner('1')) && "metaKR".equals(choice.owner('A')) && "kr".equals(choice.owner(0xac00))
                && "emoji".equals(choice.owner(0x1f600)) && "thai".equals(choice.owner(0x0e01)),
                "the strongest font owns a character; emoji keeps only what no one else has");
        List<String> candidates = choice.candidates('1');
        check(candidates.get(0).equals("metaKR") && candidates.get(candidates.size() - 1).equals("emoji") && candidates.contains("unknown"),
                "candidates: owner first, unknown coverage kept, emoji last: " + candidates);
        check(choice.candidates(0xd800).isEmpty() && choice.owner(0x10ffff) == null, "no candidate for a surrogate or an unheld code point");
        var japanese = FallbackFontPolicy.GlobalChoice.build(FallbackFontPolicy.preferLocale(found, fonts::get, Locale.JAPAN), fonts::get);
        check("hk".equals(japanese.owner('1')) && "hk".equals(japanese.owner(' ')), "ja-JP: the emoji font never owns digits or the space");
        var edge = FallbackFontPolicy.GlobalChoice.build(List.of("edge"),
                f -> FontCoverage.ofRanges(FontCoverage.State.READY, new int[] { 0x10fff0, 0x10ffff }, "edge", null, null));
        check("edge".equals(edge.owner(0x10ffff)) && edge.owner(0x10ffef) == null && edge.segmentCount() == 1, "U+10FFFF is the last scalar");
        var empty = FallbackFontPolicy.GlobalChoice.build(List.<String>of(), fonts::get);
        check(empty.owner('A') == null && empty.candidates('A').isEmpty(), "no global fonts, no choice");

        // 経路は見つけた順のまま、ファイルは読まない / paths stay in found order and no file is read
        Path dir = Files.createDirectories(Path.of("build/fallback-order"));
        try (var old = Files.list(dir)) { for (Path p : old.toList()) Files.delete(p); }
        for (String n : List.of("c_korean.ttc", "a_latin.ttf", "b_japanese.otf", "d_broken.ttf")) {
            Files.write(dir.resolve(n), new byte[] { 'a', 'b', 'c' });
        }
        Files.write(dir.resolve("notes.txt"), new byte[] { 'x' });
        List<String> all = List.of("a_latin.ttf", "b_japanese.otf", "c_korean.ttc", "d_broken.ttf");
        check(names(FallbackFontPolicy.collect(null, dir)).equals(all), "folder fonts in path order, non-fonts skipped");
        String extra = dir.resolve("a_latin.ttf") + ";" + dir.resolve("missing.ttf");
        check(names(FallbackFontPolicy.collect(extra, dir)).equals(all), "property entries are de-duplicated and missing files skipped");

        List<String> merged = Arrays.asList(FallbackFontPolicy.merge(new String[]{"skin/Z.ttf", "skin/A.ttf"},
                List.of("global/NotoSansKR.ttf")));
        check(merged.equals(List.of("skin/Z.ttf", "skin/A.ttf", "global/NotoSansKR.ttf")), "explicit skin chain preserved");
        String[] skinOrder = {"skin/Z.ttf", null, "skin/A.ttf"};
        SkinFontSource source = new SkinFontSource("skin/main.ttf", skinOrder);
        check(Arrays.equals(Arrays.copyOf(source.getFallbackFontPaths(), skinOrder.length), skinOrder),
                "source preserves explicit skin order and loader-skipped null entries");
        source.dispose();
        System.out.println("FallbackOrderTest: " + checks + " checks passed");
    }
}
