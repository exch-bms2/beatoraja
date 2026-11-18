package bms.player.beatoraja.skin;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * 正規化パスと内容の指紋が一致するものごとに {@link FreeTypeFontGenerator} を一つ共有する。
 *
 * <p>利用者はそれぞれ {@link Lease} を取る。最後の返却で、描画の区切りにジェネレーターを退役させる。ジェネレーターは
 * 可変状態(ピクセルサイズ)を持つので、描画スレッドからだけ使うこと。</p>
 *
 * Shares one {@link FreeTypeFontGenerator} per normalized path and exact snapshot fingerprint.
 *
 * <p>Each user takes a {@link Lease}. The last release retires the generator at a renderer boundary. A generator
 * keeps mutable state (the pixel size), so callers must only use it from the render thread.</p>
 */
public final class FontGeneratorPool {
	private static final Map<String, Entry> ENTRIES = new HashMap<>();
	/**
	 * 経路・大きさ・更新時刻から項目の鍵への対応。共有中のフォントを借りるたびにファイル全体を読み直さないための近道。
	 * Path/size/modified-time to entry key; a shortcut so borrowing a pooled font does not re-read the whole file.
	 */
	private static final Map<String, String> STAMPS = new HashMap<>();
	private static long unknownSequence;
	/** FreeType の書体番号で名前付きインスタンスを表す位置。 / Bit position of the named instance in a FreeType face index. */
	private static final int NAMED_INSTANCE_SHIFT = 16;

	private FontGeneratorPool() {
	}

	private static final class Entry {
		private final FreeTypeFontGenerator generator;
		private final FontCoverage coverage;
		private int leases;

		private Entry(FreeTypeFontGenerator generator, FontCoverage coverage) {
			this.generator = generator;
			this.coverage = coverage;
		}
	}

	/** 共有ジェネレーターの持ち分。二度返しても害はない。 / A share of a pooled generator. Releasing twice is harmless. */
	public static final class Lease implements Disposable {
		private final String path;
		private final FreeTypeFontGenerator generator;
		private final FontCoverage coverage;
		private boolean released;

		private Lease(String path, Entry entry) {
			this.path = path;
			this.generator = entry.generator;
			this.coverage = entry.coverage;
		}

		public FreeTypeFontGenerator generator() {
			return released ? null : generator;
		}

		FontCoverage coverage() {
			return released ? FontCoverage.UNKNOWN : coverage;
		}

		@Override
		public void dispose() {
			if (released) {
				return;
			}
			released = true;
			release(path);
		}
	}

	private static String normalizePath(String path) {
		return path != null ? path.replace('\\', '/') : "";
	}

	/** 最初の貸与でフォントを読み込む。読み込みに失敗しても項目は残らない。 / Loads the font on the first lease; a load failure leaves no entry behind. */
	public static synchronized Lease acquire(String path) {
		return acquire(path, 0);
	}

	/** 書体番号(TTC/OTC 用)を指定して共有ジェネレーターを借りる。 / Acquires a pooled generator for the given path and face index. */
	static synchronized Lease acquire(String path, int faceIndex) {
		return acquire(path, faceIndex, 0);
	}

	/**
	 * 太さを指定して借りる。可変フォントなら目標 {@code weight} に最も近い名前付きインスタンスで開き、可変でなければ通常どおり開く。
	 * 0 以下は既定インスタンス。実際に開いた書体番号ごとに共有するので、可変でないフォントは太さ指定なしの借り手と同じジェネレーターを使う。
	 * Acquires with a target weight: a variable font is opened at the named instance nearest to {@code weight},
	 * anything else as usual; 0 or less keeps the default instance. Generators are shared per face actually opened,
	 * so a non-variable font shares the generator of borrowers that asked for no weight.
	 */
	static synchronized Lease acquire(String path, int faceIndex, int weight) {
		if (!SkinFontSource.isScalablePath(path)) {
			throw new GdxRuntimeException("Not a scalable font path: " + path);
		}
		if (faceIndex < 0) {
			throw new GdxRuntimeException("Invalid font face index: " + faceIndex);
		}
		FileHandle rawHandle = Gdx.files != null ? Gdx.files.internal(path) : new FileHandle(path);
		FileHandle file = SkinFontSource.resolveFontFile(rawHandle);
		String stamp = file.type() + ":" + normalizePath(file.path()) + "#" + faceIndex + "@" + Math.max(0, weight)
				+ "\n" + file.length() + ":" + file.lastModified();
		String known = STAMPS.get(stamp);
		if (known != null && ENTRIES.containsKey(known)) {
			var entry = ENTRIES.get(known);
			entry.leases++;
			return new Lease(known, entry);
		}
		FontFileSnapshot snapshot = FontFileSnapshot.capture(file, faceIndex);
		String fingerprint = snapshot.fingerprint();
		// FreeType は書体番号の上位 16 ビットで名前付きインスタンス(番号 + 1)を選ぶ。
		// FreeType selects a named instance (index + 1) through the upper 16 bits of the face index.
		int instance = weight > 0 ? snapshot.coverage().namedInstanceNearest(weight) : -1;
		int openedFace = instance < 0 ? faceIndex : ((instance + 1) << NAMED_INSTANCE_SHIFT) | faceIndex;
		// 実ファイル読み込みに戻った場合は内容の同一性が保証されないので、古いネイティブ書体を再利用しない。
		// A fallback to the live file has no stable byte identity: never reuse an older native face.
		String key = file.type() + ":" + normalizePath(file.path()) + "#" + openedFace + "\n"
				+ ("unknown".equals(fingerprint) ? "unidentified-" + ++unknownSequence : fingerprint);
		var entry = ENTRIES.get(key);
		if (entry == null) {
			FreeTypeFontGenerator generator = openedFace == 0
					? new FreeTypeFontGenerator(snapshot.file())
					: new FreeTypeFontGenerator(snapshot.file(), openedFace);
			entry = new Entry(generator, snapshot.coverage());
			ENTRIES.put(key, entry);
		}
		if (!"unknown".equals(fingerprint)) {
			STAMPS.put(stamp, key);
		}
		entry.leases++;
		return new Lease(key, entry);
	}

	private static synchronized void release(String path) {
		String key = path;
		var entry = ENTRIES.get(key);
		if (entry == null) {
			return;
		}
		if (--entry.leases > 0) {
			return;
		}
		// ネイティブ解放の前に切り離し、失敗しても解放途中のジェネレーターがプールに残らないようにする。
		// Detach before the native cleanup so a failure cannot leave a half-disposed generator pooled.
		ENTRIES.remove(key);
		STAMPS.values().removeIf(k -> k.equals(key));
		FontRenderRetirement.retire(entry.generator);
	}

	/**
	 * 終了時にすべての貸与が返されたか確認する。生きている借り手を強制クリアで無効にしてはならない。
	 * 互換性: 以前は使用中の項目を消していた呼び出し側は、先に貸与を返すこと。最後の返却で予約されたネイティブ解放は次の描画の区切りで実行される。
	 * Verifies shutdown has released every lease. Live borrowers may not be invalidated by a force-clear.
	 * Compatibility: callers that previously cleared live entries must release their leases first.
	 * Native cleanup already queued by the last release runs at the next application render boundary.
	 */
	public static synchronized void clear() {
		if (!ENTRIES.isEmpty()) {
			throw new IllegalStateException("Cannot clear generators with active leases");
		}
		STAMPS.clear();
	}

	/** 再利用できる項目数。退役待ちのネイティブジェネレーターは含まない。 / Number of reusable pooled entries, excluding native generators already queued for retirement. */
	static synchronized int activeCount() {
		return ENTRIES.size();
	}

	/** ネイティブ書体と CPU 側の判定に、同じ不変のファイル内容を渡す。 / Feeds the native face and its CPU coverage filter the same immutable file contents. */
	static final class FontFileSnapshot {
		private static final int MAX_BYTES = 128 * 1024 * 1024;
		private final FileHandle file;
		private final byte[] bytes;
		private final int faceIndex;
		private final String fingerprint;
		private FontCoverage coverage;

		private FontFileSnapshot(FileHandle file, byte[] bytes, int faceIndex) {
			this.file = file;
			this.bytes = bytes;
			this.faceIndex = faceIndex;
			this.fingerprint = bytes == null ? "unknown" : FontCoverage.fingerprint(bytes, faceIndex);
		}

		static FontFileSnapshot capture(FileHandle original) {
			return capture(original, 0);
		}

		static FontFileSnapshot capture(FileHandle original, int faceIndex) {
			if (original == null || faceIndex < 0) {
				return new FontFileSnapshot(original, null, Math.max(0, faceIndex));
			}
			// 大きい・未対応のファイルは従来のローダーを使い、「含まれない」の目安は出さない。 / Large/unsupported files keep their existing loader and never supply negative hints.
			if (original.length() > MAX_BYTES) return new FontFileSnapshot(original, null, faceIndex);
			try (InputStream input = original.read()) {
				byte[] bytes = input.readNBytes(MAX_BYTES + 1);
				if (bytes.length > MAX_BYTES) return new FontFileSnapshot(original, null, faceIndex);
				FileHandle snapshot = new FileHandle(original.path()) {
					@Override public InputStream read() { return new ByteArrayInputStream(bytes); }
					@Override public byte[] readBytes() { return bytes.clone(); }
					@Override public long length() { return bytes.length; }
					@Override public boolean exists() { return true; }
					@Override public FileHandle parent() { return original.parent(); }
					// FreeType 1.14 は read() より先に map() を試す。実ファイルのパスを map するとこのスナップショットを迂回し、
					// Windows では差し替え可能なファイルをロックしてしまう。
					// FreeType 1.14 tries map() before read(). Mapping the live path would
					// bypass this snapshot and lock replaceable files on Windows.
					@Override public ByteBuffer map() { throw new GdxRuntimeException("In-memory font snapshot cannot be file-mapped"); }
					@Override public ByteBuffer map(MapMode mode) { return map(); }
				};
				return new FontFileSnapshot(snapshot, bytes, faceIndex);
			} catch (Exception e) {
				return new FontFileSnapshot(original, null, faceIndex);
			}
		}

		FileHandle file() { return file; }
		String fingerprint() { return fingerprint; }
		FontCoverage coverage() {
			if (coverage == null) coverage = bytes == null ? FontCoverage.UNKNOWN : FontCoverage.read(bytes, faceIndex);
			return coverage;
		}
	}
}