package bms.player.beatoraja.skin;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

import com.badlogic.gdx.files.FileHandle;

/**
 * OpenType 書体用の読み取り専用・CPU だけで動く「含まれない」判定。書体を開くときに作り、描画中には作らない。
 * 未知のエンコーディングは通常の FreeType 検索に任せる。
 *
 * <p>ネイティブライブラリがどの cmap を選んだか推測せず、対応する Unicode cmap をすべて合わせる。含まれることは候補の目安にすぎず、
 * ラスタ化や shaping ができる証明ではない。異体字シーケンスは通常の検索・shaping に任せる。</p>
 *
 * A read-only, CPU-only absence filter for an OpenType face. Built when the face is opened,
 * never during drawing. Unknown encodings retain the normal FreeType lookup path.
 *
 * <p>We union supported Unicode cmaps rather than guessing which one the native library
 * selected. Membership is only a candidate hint, not proof of rasterization or shaping.
 * Variation sequences are deliberately left to the normal lookup/shaping path.</p>
 */
final class FontCoverage {
    enum State { READY, EMPTY, UNKNOWN }

    static final FontCoverage UNKNOWN = new FontCoverage(State.UNKNOWN, new int[0], "unknown");
    private static final int MAX_FONT_BYTES = 128 * 1024 * 1024;
    private static final int MAX_CODE_POINT = 0x10ffff;
    private static final int CMAP = 0x636d6170;
    private final State state;
    private final int[] ranges;
    private final String fingerprint;

    private FontCoverage(State state, int[] ranges, String fingerprint) {
        this.state = state;
        this.ranges = ranges;
        this.fingerprint = fingerprint;
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
        try {
            if (bytes.length > MAX_FONT_BYTES || faceIndex < 0) return UNKNOWN;
            identity = fingerprint(bytes, faceIndex);
            ByteBuffer font = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            int directory = 0;
            if (font.getInt(0) == 0x74746366) { // TTC/OTC のディレクトリ位置は絶対値 / TTC/OTC directory offsets are absolute.
                long faces = u32(font, 8);
                if (faceIndex >= faces) return unknown(identity);
                directory = offset(font, 12L + 4L * faceIndex, 4);
                directory = offset(font, u32(font, directory), 12);
            } else if (faceIndex != 0) {
                return unknown(identity);
            }
            int signature = font.getInt(directory);
            if (signature != 0x00010000 && signature != 0x4f54544f && signature != 0x74727565)
                return unknown(identity);
            int tables = u16(font, directory + 4);
            offset(font, directory + 12L, 16L * tables);
            for (int i = 0; i < tables; i++) {
                int record = directory + 12 + 16 * i;
                if (font.getInt(record) != CMAP) continue;
                int start = offset(font, u32(font, record + 8), u32(font, record + 12));
                int length = (int) u32(font, record + 12);
                ByteBuffer cmap = font.duplicate().position(start).limit(start + length).slice()
                        .order(ByteOrder.BIG_ENDIAN);
                return parse(cmap, identity);
            }
        } catch (Exception e) {
            // 壊れた・未対応の cmap を「含まれない」と誤判定してはならない。 / A corrupt/unsupported cmap must not turn into a false negative.
        }
        return unknown(identity);
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
        covered.clear(0xd800, 0xe000); // Unicode スカラー値のみ、サロゲート片は含めない / Unicode scalars only; no surrogate halves.
        int[] ranges = new int[32];
        int used = 0;
        for (int from = covered.nextSetBit(0); from >= 0; ) {
            int end = covered.nextClearBit(from);
            if (used + 2 > ranges.length) ranges = Arrays.copyOf(ranges, ranges.length * 2);
            ranges[used++] = from;
            ranges[used++] = end - 1;
            from = covered.nextSetBit(end);
        }
        return new FontCoverage(used == 0 ? State.EMPTY : State.READY,
                Arrays.copyOf(ranges, used), identity);
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
        if (!Character.isValidCodePoint(codePoint) || codePoint >= 0xd800 && codePoint <= 0xdfff) return false;
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

    State state() { return state; }
    String fingerprint() { return fingerprint; }
}
