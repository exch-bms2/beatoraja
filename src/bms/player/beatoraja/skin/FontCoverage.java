package bms.player.beatoraja.skin;

import java.io.InputStream;
import java.lang.Character.UnicodeScript;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.files.FileHandle;

/**
 * OpenType 書体用の読み取り専用・CPU だけで動く「含まれない」判定。書体を開くときに作り、描画中には作らない。
 * 未知のエンコーディングは通常の FreeType 検索に任せる。
 *
 * <p>ネイティブライブラリがどの cmap を選んだか推測せず、対応する Unicode cmap をすべて合わせる。含まれることは候補の目安にすぎず、
 * ラスタ化や shaping ができる証明ではない。異体字シーケンスは通常の検索・shaping に任せる。</p>
 *
 * <p>ロケールとの照合に使う情報も同じバイトから一度だけ読む: OpenType meta 表の dlng・slng(BCP 47 に正規化)と、
 * cmap が各文字体系(Unicode Script)をどれだけ収録するか。name 表の言語 ID は照合の根拠にしないので読まない。</p>
 *
 * A read-only, CPU-only absence filter for an OpenType face. Built when the face is opened,
 * never during drawing. Unknown encodings retain the normal FreeType lookup path.
 *
 * <p>We union supported Unicode cmaps rather than guessing which one the native library
 * selected. Membership is only a candidate hint, not proof of rasterization or shaping.
 * Variation sequences are deliberately left to the normal lookup/shaping path.</p>
 *
 * <p>The locale evidence is read once from the same bytes: the meta table's dlng and slng (normalized to BCP 47) and
 * how much of each Unicode script the cmap holds. Name table language IDs are not locale evidence and are not read.</p>
 */
final class FontCoverage {
    enum State { READY, EMPTY, UNKNOWN }

    /** 圧縮された [startInclusive, endInclusive] 区間を受け取る読み取り専用コールバック。 / Read-only callback for compressed [startInclusive, endInclusive] ranges. */
    @FunctionalInterface
    interface RangeConsumer {
        void accept(int startInclusive, int endInclusive);
    }

    static final FontCoverage UNKNOWN = new FontCoverage(State.UNKNOWN, new int[0], "unknown");
    /**
     * 文字体系を「収録している」とみなす割合(BMP 内の割り当て済みコードポイントに対する収録数、調整用)。
     * Share of a script's assigned BMP code points a font must hold to count as providing that script (tuning knob).
     */
    static final double SCRIPT_COVERAGE_RATIO = 0.5;
    private static final int MAX_FONT_BYTES = 128 * 1024 * 1024;
    private static final int MAX_CODE_POINT = 0x10ffff;
    private static final int BMP_END = 0xffff;
    private static final int SURROGATE_START = 0xd800, SURROGATE_END = 0xdfff;
    private static final int CMAP = 0x636d6170; // 'cmap'
    private static final int META = 0x6d657461; // 'meta'
    private static final int META_DLNG = 0x646c6e67; // 'dlng'
    private static final int META_SLNG = 0x736c6e67; // 'slng'
    private static final int COLLECTION = 0x74746366; // 'ttcf'
    private static final int TRUETYPE_OUTLINES = 0x00010000, CFF_OUTLINES = 0x4f54544f /* 'OTTO' */,
            APPLE_TRUETYPE = 0x74727565 /* 'true' */;
    private static final int COLLECTION_FACE_COUNT = 8, COLLECTION_FACE_OFFSETS = 12;
    private static final int TABLE_DIRECTORY_HEADER = 12, TABLE_RECORD = 16;
    private static final int META_VERSION = 1, META_HEADER = 16, META_RECORD = 12, META_COUNT = 12;
    private static final int FVAR = 0x66766172; // 'fvar'
    private static final int WGHT = 0x77676874; // 'wght'
    private static final int FVAR_HEADER = 16, FVAR_AXES_OFFSET = 4, FVAR_AXIS_COUNT = 8, FVAR_AXIS_SIZE = 10,
            FVAR_INSTANCE_COUNT = 12, FVAR_INSTANCE_SIZE = 14, AXIS_RECORD = 20, INSTANCE_COORDINATES = 4;
    private static final double FIXED_ONE = 65536.0;
    private static final int[] NO_INSTANCES = new int[0];
    /** BMP 内の文字体系ごとの割り当て済みコードポイント数(JDK の Unicode 版による)。 / Assigned BMP code points per script (per the JDK's Unicode version). */
    private static Map<UnicodeScript, Integer> assignedBmp;

    private final State state;
    private final int[] ranges;
    private final String fingerprint;
    private final Set<String> designLanguages;
    private final Set<String> supportedLanguages;
    private final Set<UnicodeScript> scripts;
    /** 可変フォントの名前付きインスタンスごとの wght。可変でなければ空。 / wght of each named instance of a variable font; empty otherwise. */
    private final int[] instanceWeights;

    private FontCoverage(State state, int[] ranges, String fingerprint) {
        this(state, ranges, fingerprint, Set.of(), Set.of());
    }

    private FontCoverage(State state, int[] ranges, String fingerprint, Set<String> designLanguages,
            Set<String> supportedLanguages) {
        this(state, ranges, fingerprint, designLanguages, supportedLanguages, NO_INSTANCES);
    }

    private FontCoverage(State state, int[] ranges, String fingerprint, Set<String> designLanguages,
            Set<String> supportedLanguages, int[] instanceWeights) {
        this.state = state;
        this.ranges = ranges;
        this.fingerprint = fingerprint;
        this.designLanguages = Set.copyOf(designLanguages);
        this.supportedLanguages = Set.copyOf(supportedLanguages);
        this.scripts = state == State.READY ? providedScripts(ranges) : Set.of();
        this.instanceWeights = instanceWeights;
    }

    /** 区間と meta 言語から直接作る(試験・合成用)。 / Builds a coverage directly from ranges and meta languages (tests and synthesis). */
    static FontCoverage ofRanges(State state, int[] ranges, String fingerprint, Set<String> designLanguages,
            Set<String> supportedLanguages) {
        int[] normalized = normalizeRanges(ranges);
        State effective = state == State.UNKNOWN ? State.UNKNOWN : normalized.length == 0 ? State.EMPTY : State.READY;
        return new FontCoverage(effective, effective == State.UNKNOWN ? new int[0] : normalized,
                fingerprint != null ? fingerprint : "unknown",
                designLanguages != null ? designLanguages : Set.of(), supportedLanguages != null ? supportedLanguages : Set.of());
    }

    static FontCoverage read(FileHandle file, int faceIndex) {
        if (file == null) return UNKNOWN;
        try (InputStream input = file.read()) {
            byte[] bytes = input.readNBytes(MAX_FONT_BYTES + 1);
            return bytes.length > MAX_FONT_BYTES ? UNKNOWN : read(bytes, faceIndex);
        } catch (Exception e) {
            return UNKNOWN;
        }
    }

    /** パッケージ内のバイト入口。壊れたフォントの試験をネイティブ・GL から切り離す。 / Package-private byte entry point keeps malformed-font tests independent of native/GL. */
    static FontCoverage read(byte[] bytes, int faceIndex) {
        String identity = "unknown";
        MetaTags meta = MetaTags.EMPTY;
        int[] weights = NO_INSTANCES;
        try {
            if (bytes == null || bytes.length > MAX_FONT_BYTES || faceIndex < 0) return UNKNOWN;
            identity = fingerprint(bytes, faceIndex);
            ByteBuffer font = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            meta = metaLanguageTags(font, faceIndex);
            weights = namedInstanceWeights(font, faceIndex);
            ByteBuffer cmap = table(font, faceIndex, CMAP);
            if (cmap != null) return parse(cmap, identity).withTables(meta, weights);
        } catch (Exception e) {
            // 壊れた・未対応の cmap を「含まれない」と誤判定してはならない。 / A corrupt/unsupported cmap must not turn into a false negative.
        }
        return unknown(identity).withTables(meta, weights);
    }

    /** 書体の表。書体番号が範囲外・未対応の形式・表がなければ null。 / The face's table, or null for an out-of-range face, an unsupported format or a missing table. */
    private static ByteBuffer table(ByteBuffer font, int faceIndex, int tag) {
        int directory = 0;
        if (font.getInt(0) == COLLECTION) { // TTC/OTC のディレクトリ位置は絶対値 / TTC/OTC directory offsets are absolute.
            offset(font, 0, COLLECTION_FACE_OFFSETS);
            long faces = u32(font, COLLECTION_FACE_COUNT);
            if (faceIndex >= faces) return null;
            directory = offset(font, COLLECTION_FACE_OFFSETS + (long) Integer.BYTES * faceIndex, Integer.BYTES);
            directory = offset(font, u32(font, directory), TABLE_DIRECTORY_HEADER);
        } else if (faceIndex != 0) {
            return null;
        } else {
            offset(font, 0, TABLE_DIRECTORY_HEADER);
        }
        int signature = font.getInt(directory);
        if (signature != TRUETYPE_OUTLINES && signature != CFF_OUTLINES && signature != APPLE_TRUETYPE) return null;
        int tables = u16(font, directory + 4);
        offset(font, directory + (long) TABLE_DIRECTORY_HEADER, (long) TABLE_RECORD * tables);
        for (int i = 0; i < tables; i++) {
            int record = directory + TABLE_DIRECTORY_HEADER + TABLE_RECORD * i;
            if (font.getInt(record) != tag) continue;
            int start = offset(font, u32(font, record + 8), u32(font, record + 12));
            int length = (int) u32(font, record + 12);
            return font.duplicate().position(start).limit(start + length).slice().order(ByteOrder.BIG_ENDIAN);
        }
        return null;
    }

    private record MetaTags(Set<String> designLanguages, Set<String> supportedLanguages) {
        static final MetaTags EMPTY = new MetaTags(Set.of(), Set.of());
    }

    /**
     * meta 表の dlng(設計対象言語)と slng(対応言語)を {@link FallbackFontPolicy#canonicalBcp47(String)} で正規化した BCP 47 で返す。
     * 読めなければ空。 / Reads meta dlng (design languages) and slng (supported languages) as BCP 47 tags normalized by
     * {@link FallbackFontPolicy#canonicalBcp47(String)}. Empty when unreadable.
     */
    private static MetaTags metaLanguageTags(ByteBuffer font, int faceIndex) {
        try {
            ByteBuffer meta = table(font, faceIndex, META);
            if (meta == null || meta.limit() < META_HEADER || u32(meta, 0) != META_VERSION) return MetaTags.EMPTY;
            long count = u32(meta, META_COUNT);
            offset(meta, META_HEADER, META_RECORD * count);
            Set<String> dlng = new LinkedHashSet<>();
            Set<String> slng = new LinkedHashSet<>();
            for (int i = 0; i < count; i++) {
                int record = META_HEADER + META_RECORD * i;
                int tag = meta.getInt(record);
                if (tag != META_DLNG && tag != META_SLNG) continue;
                long length = u32(meta, record + 8);
                int start = offset(meta, u32(meta, record + 4), length);
                byte[] raw = new byte[(int) length];
                meta.get(start, raw);
                for (String part : new String(raw, StandardCharsets.UTF_8).split(",")) {
                    String canonical = FallbackFontPolicy.canonicalBcp47(part);
                    if (canonical != null) (tag == META_DLNG ? dlng : slng).add(canonical);
                }
            }
            return new MetaTags(Set.copyOf(dlng), Set.copyOf(slng));
        } catch (RuntimeException e) {
            return MetaTags.EMPTY;
        }
    }

    private FontCoverage withTables(MetaTags meta, int[] weights) {
        if (meta.designLanguages.isEmpty() && meta.supportedLanguages.isEmpty() && weights.length == 0) return this;
        return new FontCoverage(state, ranges, fingerprint, meta.designLanguages, meta.supportedLanguages, weights);
    }

    /**
     * fvar 表の名前付きインスタンスごとの wght 座標(整数に丸める)。fvar・wght 軸がない、または読めなければ空。
     * The wght coordinate (rounded) of each named instance in the fvar table; empty when there is no fvar or wght
     * axis, or the table cannot be read.
     */
    private static int[] namedInstanceWeights(ByteBuffer font, int faceIndex) {
        try {
            ByteBuffer fvar = table(font, faceIndex, FVAR);
            if (fvar == null || fvar.limit() < FVAR_HEADER) return NO_INSTANCES;
            int axesOffset = u16(fvar, FVAR_AXES_OFFSET), axisCount = u16(fvar, FVAR_AXIS_COUNT), axisSize = u16(fvar, FVAR_AXIS_SIZE);
            int instanceCount = u16(fvar, FVAR_INSTANCE_COUNT), instanceSize = u16(fvar, FVAR_INSTANCE_SIZE);
            if (axisSize < AXIS_RECORD || instanceSize < INSTANCE_COORDINATES + Integer.BYTES * axisCount) return NO_INSTANCES;
            offset(fvar, axesOffset, (long) axisSize * axisCount);
            int weightAxis = -1;
            for (int a = 0; a < axisCount && weightAxis < 0; a++) {
                if (fvar.getInt(axesOffset + axisSize * a) == WGHT) weightAxis = a;
            }
            if (weightAxis < 0) return NO_INSTANCES;
            int instances = axesOffset + axisSize * axisCount;
            offset(fvar, instances, (long) instanceSize * instanceCount);
            int[] weights = new int[instanceCount];
            for (int i = 0; i < instanceCount; i++) {
                int coordinate = fvar.getInt(instances + instanceSize * i + INSTANCE_COORDINATES + Integer.BYTES * weightAxis);
                weights[i] = (int) Math.round(coordinate / FIXED_ONE);
            }
            return weights;
        } catch (RuntimeException e) {
            return NO_INSTANCES;
        }
    }

    /**
     * BMP 内の割り当て済みコードポイントの {@link #SCRIPT_COVERAGE_RATIO} 以上を収録する文字体系(共通・継承を除く)。
     * Scripts (other than Common and Inherited) whose assigned BMP code points the ranges hold at {@link #SCRIPT_COVERAGE_RATIO} or more.
     */
    private static Set<UnicodeScript> providedScripts(int[] ranges) {
        Map<UnicodeScript, Integer> held = new EnumMap<>(UnicodeScript.class);
        for (int i = 0; i < ranges.length && ranges[i] <= BMP_END; i += 2) {
            for (int cp = ranges[i], end = Math.min(ranges[i + 1], BMP_END); cp <= end; cp++) {
                if (Character.getType(cp) != Character.UNASSIGNED) held.merge(UnicodeScript.of(cp), 1, Integer::sum);
            }
        }
        Map<UnicodeScript, Integer> assigned = assignedBmp();
        Set<UnicodeScript> provided = EnumSet.noneOf(UnicodeScript.class);
        held.forEach((script, count) -> {
            if (script != UnicodeScript.COMMON && script != UnicodeScript.INHERITED && script != UnicodeScript.UNKNOWN
                    && count >= SCRIPT_COVERAGE_RATIO * assigned.get(script)) {
                provided.add(script);
            }
        });
        return Set.copyOf(provided);
    }

    private static synchronized Map<UnicodeScript, Integer> assignedBmp() {
        if (assignedBmp == null) {
            Map<UnicodeScript, Integer> counts = new EnumMap<>(UnicodeScript.class);
            for (int cp = 0; cp <= BMP_END; cp++) {
                if ((cp < SURROGATE_START || cp > SURROGATE_END) && Character.getType(cp) != Character.UNASSIGNED) {
                    counts.merge(UnicodeScript.of(cp), 1, Integer::sum);
                }
            }
            assignedBmp = counts;
        }
        return assignedBmp;
    }

    private static FontCoverage parse(ByteBuffer cmap, String identity) {
        if (u16(cmap, 0) != 0) return unknown(identity);
        int count = u16(cmap, 2);
        offset(cmap, 4, 8L * count);
        BitSet covered = new BitSet();
        Set<Integer> seen = new HashSet<>();
        boolean mapped = false;
        for (int i = 0; i < count; i++) {
            int record = 4 + i * 8;
            int platform = u16(cmap, record), encoding = u16(cmap, record + 2);
            if (platform != 0 && !(platform == 3 && (encoding == 1 || encoding == 10))) continue;
            int sub = offset(cmap, u32(cmap, record + 4), 2);
            int format = u16(cmap, sub);
            if (platform == 0 && encoding == 5 && format == 14) continue;
            // 解析しなかった Unicode サブテーブルには他にない文字があり得る。 / An unparsed Unicode subtable could contain characters absent from the others.
            if (format != 4 && format != 12) return unknown(identity);
            if (!seen.add(sub)) continue;
            long length = format == 4 ? u16(cmap, sub + 2) : u32(cmap, sub + 4);
            offset(cmap, sub, length);
            ByteBuffer table = cmap.duplicate().position(sub).limit(sub + (int) length).slice()
                    .order(ByteOrder.BIG_ENDIAN);
            if (format == 4) readFormat4(table, covered);
            else readFormat12(table, covered);
            mapped = true;
        }
        if (!mapped) return unknown(identity);
        int[] ranges = toRanges(covered);
        return new FontCoverage(ranges.length == 0 ? State.EMPTY : State.READY, ranges, identity);
    }

    private static int[] normalizeRanges(int[] rawRanges) {
        BitSet covered = new BitSet();
        if (rawRanges != null) {
            for (int i = 0; i + 1 < rawRanges.length; i += 2) {
                int start = Math.max(0, rawRanges[i]), end = Math.min(MAX_CODE_POINT, rawRanges[i + 1]);
                if (start <= end) covered.set(start, end + 1);
            }
        }
        return toRanges(covered);
    }

    /** Unicode スカラー値だけの圧縮区間(サロゲート片は含めない)。 / Compressed ranges of Unicode scalars only (no surrogate halves). */
    private static int[] toRanges(BitSet covered) {
        covered.clear(SURROGATE_START, SURROGATE_END + 1);
        int[] ranges = new int[32];
        int used = 0;
        for (int from = covered.nextSetBit(0); from >= 0; ) {
            int end = covered.nextClearBit(from);
            if (used + 2 > ranges.length) ranges = Arrays.copyOf(ranges, ranges.length * 2);
            ranges[used++] = from;
            ranges[used++] = end - 1;
            from = covered.nextSetBit(end);
        }
        return Arrays.copyOf(ranges, used);
    }

    private static void readFormat4(ByteBuffer table, BitSet covered) {
        int segments = u16(table, 6) / 2;
        if (segments == 0 || u16(table, 6) % 2 != 0) throw new IllegalArgumentException("segments");
        offset(table, 14, 8L * segments + 2);
        int starts = 16 + 2 * segments, deltas = starts + 2 * segments, offsets = deltas + 2 * segments;
        int previousEnd = -1;
        for (int i = 0; i < segments; i++) {
            int end = u16(table, 14 + 2 * i), start = u16(table, starts + 2 * i);
            if (start > end || start <= previousEnd) throw new IllegalArgumentException("segment order");
            previousEnd = end;
            int delta = table.getShort(deltas + 2 * i), displacement = u16(table, offsets + 2 * i);
            for (int cp = start; cp <= end; cp++) {
                int glyph = displacement == 0 ? (cp + delta) & 0xffff
                        : u16(table, offset(table, offsets + 2L * i + displacement + 2L * (cp - start), 2));
                if (displacement != 0 && glyph != 0) glyph = (glyph + delta) & 0xffff;
                if (glyph != 0) covered.set(cp);
            }
        }
    }

    private static void readFormat12(ByteBuffer table, BitSet covered) {
        if (u16(table, 2) != 0) throw new IllegalArgumentException("reserved");
        long groups = u32(table, 12);
        offset(table, 16, groups * 12);
        long previousEnd = -1;
        for (int i = 0; i < groups; i++) {
            int p = 16 + 12 * i;
            long start = u32(table, p), end = u32(table, p + 4), glyph = u32(table, p + 8);
            if (start > end || start <= previousEnd || end > MAX_CODE_POINT
                    || glyph + end - start > 0xffffffffL) throw new IllegalArgumentException("group");
            previousEnd = end;
            // .notdef から始まるグループでも、続く文字は対応付けられることがある。 / A group starting at .notdef can still map its subsequent characters.
            if (glyph == 0) start++;
            if (start <= end) covered.set((int) start, (int) end + 1);
        }
    }

    private static int offset(ByteBuffer data, long start, long length) {
        if (start < 0 || length < 0 || start > data.limit() || length > data.limit() - start)
            throw new IllegalArgumentException("font table bounds");
        return (int) start;
    }

    private static int u16(ByteBuffer data, int at) { return Short.toUnsignedInt(data.getShort(at)); }
    private static long u32(ByteBuffer data, int at) { return Integer.toUnsignedLong(data.getInt(at)); }
    private static FontCoverage unknown(String identity) { return new FontCoverage(State.UNKNOWN, new int[0], identity); }

    static String fingerprint(byte[] bytes, int faceIndex) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
                    + ":" + faceIndex + ":unicode-union-v1";
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 is required by the Java runtime", e);
        }
    }

    boolean mayContain(int codePoint) {
        if (!Character.isValidCodePoint(codePoint) || codePoint >= SURROGATE_START && codePoint <= SURROGATE_END) return false;
        if (state == State.UNKNOWN) return true;
        int low = 0, high = ranges.length / 2 - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (codePoint < ranges[2 * middle]) high = middle - 1;
            else if (codePoint > ranges[2 * middle + 1]) low = middle + 1;
            else return true;
        }
        return false;
    }

    /** 各圧縮区間 [startInclusive, endInclusive] を昇順に渡す。 / Visits each compressed [startInclusive, endInclusive] interval in ascending order. */
    void forEachRange(RangeConsumer consumer) {
        for (int i = 0; i < ranges.length; i += 2) {
            consumer.accept(ranges[i], ranges[i + 1]);
        }
    }

    State state() { return state; }
    String fingerprint() { return fingerprint; }
    /** meta 表の dlng(設計対象言語)、正規化済み BCP 47。 / Design languages from meta dlng, as canonical BCP 47. */
    Set<String> designLanguages() { return designLanguages; }
    /** meta 表の slng(対応言語)、正規化済み BCP 47。 / Supported languages from meta slng, as canonical BCP 47. */
    Set<String> supportedLanguages() { return supportedLanguages; }
    /** cmap が収録する文字体系({@link #SCRIPT_COVERAGE_RATIO} 以上)。 / Scripts the cmap provides (at {@link #SCRIPT_COVERAGE_RATIO} or more). */
    Set<UnicodeScript> scripts() { return scripts; }
    /**
     * 目標の太さに最も近い名前付きインスタンスの番号(同じ距離なら先のもの)。可変フォントでなければ -1。
     * Index of the named instance whose weight is nearest to the target (the earlier one on a tie); -1 when the
     * font is not variable in weight.
     */
    int namedInstanceNearest(int weight) {
        int best = -1;
        for (int i = 0; i < instanceWeights.length; i++) {
            if (best < 0 || Math.abs(instanceWeights[i] - weight) < Math.abs(instanceWeights[best] - weight)) best = i;
        }
        return best;
    }
}
