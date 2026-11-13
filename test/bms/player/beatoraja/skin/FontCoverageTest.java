package bms.player.beatoraja.skin;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import com.badlogic.gdx.files.FileHandle;
import bms.player.beatoraja.skin.FontGeneratorPool.FontFileSnapshot;

/** アサーション有効で実行する。ネイティブライブラリやグラフィックコンテキストは不要。 / Run with assertions enabled; no native library or graphics context is needed. */
public final class FontCoverageTest {
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static ByteBuffer buffer(int size) { return ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN); }
    private static byte[] format4(int cp, int glyph, boolean indirect) {
        ByteBuffer b = buffer(indirect ? 36 : 32);
        b.putShort(0, (short) 4); b.putShort(2, (short) b.capacity()); b.putShort(6, (short) 4);
        b.putShort(14, (short) cp); b.putShort(16, (short) 0xffff);
        b.putShort(20, (short) cp); b.putShort(22, (short) 0xffff);
        b.putShort(24, (short) (indirect ? 2 : glyph - cp)); b.putShort(26, (short) 1);
        if (indirect) { b.putShort(28, (short) 4); b.putShort(32, (short) glyph); }
        return b.array();
    }
    private static byte[] format12(int from, int to, int firstGlyph) {
        ByteBuffer b = buffer(28);
        b.putShort(0, (short) 12); b.putInt(4, 28); b.putInt(12, 1);
        b.putInt(16, from); b.putInt(20, to); b.putInt(24, firstGlyph);
        return b.array();
    }
    private static byte[] cmap(byte[]... subtables) {
        int length = 4 + 8 * subtables.length;
        for (byte[] sub : subtables) length += sub.length;
        ByteBuffer b = buffer(length); b.putShort(2, (short) subtables.length);
        int pos = 4 + 8 * subtables.length;
        for (int i = 0; i < subtables.length; i++) {
            b.putShort(4 + 8 * i, (short) 0); b.putShort(6 + 8 * i, (short) 4); b.putInt(8 + 8 * i, pos);
            b.position(pos); b.put(subtables[i]); pos += subtables[i].length;
        }
        return b.array();
    }
    private static byte[] font(byte[] cmap) {
        ByteBuffer b = buffer(28 + cmap.length);
        b.putInt(0, 0x00010000); b.putShort(4, (short) 1); b.putInt(12, 0x636d6170);
        b.putInt(20, 28); b.putInt(24, cmap.length); b.position(28); b.put(cmap);
        return b.array();
    }
    private static byte[] collection(byte[] a, byte[] c) {
        ByteBuffer b = buffer(76 + a.length + c.length);
        b.putInt(0, 0x74746366); b.putInt(4, 0x00010000); b.putInt(8, 2);
        b.putInt(12, 20); b.putInt(16, 48);
        for (int i = 0; i < 2; i++) {
            int at = 20 + 28 * i; byte[] table = i == 0 ? a : c;
            b.putInt(at, 0x00010000); b.putShort(at + 4, (short) 1); b.putInt(at + 12, 0x636d6170);
            b.putInt(at + 20, i == 0 ? 76 : 76 + a.length); b.putInt(at + 24, table.length);
        }
        b.position(76); b.put(a); b.put(c); return b.array();
    }
    public static void main(String[] args) {
        byte[] a = font(cmap(format4('A', 7, false)));
        FontCoverage bmp = FontCoverage.read(a, 0);
        check(bmp.state() == FontCoverage.State.READY, "BMP ready");
        check(bmp.mayContain('A') && !bmp.mayContain('B'), "delta mapping");
        check(!bmp.mayContain(0xffff), "sentinel maps to notdef");
        FontCoverage indirect = FontCoverage.read(font(cmap(format4('A', 7, true))), 0);
        check(indirect.mayContain('A'), "glyph array address relative to range offset");
        FontCoverage empty = FontCoverage.read(font(cmap(format4('A', 0, true))), 0);
        check(empty.state() == FontCoverage.State.EMPTY && !empty.mayContain('A'), "zero glyph ignores delta");
        FontCoverage smp = FontCoverage.read(font(cmap(format12(0x20000, 0x20002, 0))), 0);
        check(!smp.mayContain(0x20000) && smp.mayContain(0x20001) && smp.mayContain(0x20002), "SMP zero start");
        FontCoverage union = FontCoverage.read(font(cmap(format4('A', 4, false), format12(0x20000, 0x20000, 8))), 0);
        check(union.mayContain('A') && union.mayContain(0x20000), "Unicode cmap union");
        check(!union.mayContain(0xd800) && !union.mayContain(0xdfff) && !union.mayContain(0x110000), "scalar boundary");
        byte[] unsupported = buffer(10).putShort(0, (short) 6).array();
        FontCoverage unknown = FontCoverage.read(font(cmap(format4('A', 4, false), unsupported)), 0);
        check(unknown.state() == FontCoverage.State.UNKNOWN && unknown.mayContain('B'), "unsupported is not empty");
        byte[] ttc = collection(cmap(format4('A', 4, false)), cmap(format4('B', 4, false)));
        check(FontCoverage.read(ttc, 0).mayContain('A') && !FontCoverage.read(ttc, 0).mayContain('B'), "TTC face0");
        check(FontCoverage.read(ttc, 1).mayContain('B') && !FontCoverage.read(ttc, 1).mayContain('A'), "TTC face1");
        check(FontCoverage.read(ttc, 2).state() == FontCoverage.State.UNKNOWN, "TTC face bounds");
        check(!FontCoverage.read(ttc, 0).fingerprint().equals(FontCoverage.read(ttc, 1).fingerprint()), "face identity");
        check(!bmp.fingerprint().equals(FontCoverage.read(font(cmap(format4('B', 4, false))), 0).fingerprint()), "byte identity");
        check(bmp.fingerprint().equals(FontCoverage.read(a.clone(), 0).fingerprint()), "deterministic rebuild");
        for (int size = 0; size < a.length; size++)
            check(FontCoverage.read(Arrays.copyOf(a, size), 0).state() == FontCoverage.State.UNKNOWN, "truncated " + size);
        int[] reads = { 0 };
        FileHandle file = new FileHandle("fixture.ttf") {
            @Override public InputStream read() { reads[0]++; return new ByteArrayInputStream(a); }
            @Override public long length() { return a.length; }
        };
        FontFileSnapshot snapshot = FontFileSnapshot.capture(file);
        check(reads[0] == 1, "one source read");
        long start = System.nanoTime();
        for (int i = 0; i < 100000; i++) check(snapshot.coverage().mayContain('A'), "warm member");
        long nanos = System.nanoTime() - start;
        check(reads[0] == 1, "warm contains performs no file IO");
        byte[] before = snapshot.file().readBytes(); a[a.length - 1] ^= 1;
        check(Arrays.equals(before, snapshot.file().readBytes()), "native face input snapshot remains pinned");
        byte[] exposed = snapshot.file().readBytes(); exposed[1] = 0;
        check(snapshot.file().readBytes()[1] == 1, "snapshot cannot be mutated through readBytes");
        System.out.println("FontCoverageTest: " + checks + " checks; warm100000_ns=" + nanos);
    }
}
