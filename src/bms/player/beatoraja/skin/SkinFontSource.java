package bms.player.beatoraja.skin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

import bms.player.beatoraja.skin.FontGeneratorPool.FontFileSnapshot;
import bms.player.beatoraja.skin.FontRenderRetirement.FontResourceDisposer;

/**
 * Shared FreeType generators used by text objects in one skin.
 *
 * <p>A generator keeps a native FreeType face and a copy of the font data. Keeping one source per
 * font/fallback combination prevents every {@link SkinTextFont} from loading that data separately.</p>
 */
public final class SkinFontSource implements Disposable {
	private static final FreeTypeFontGenerator[] NO_FALLBACKS = new FreeTypeFontGenerator[0];
	private static final FontGeneratorPool.Lease[] NO_LEASES = new FontGeneratorPool.Lease[0];

	private final String fontPath;
	private final FileHandle fontFile;
	private final String[] fallbackFontPaths;
	/** 結合した一覧の先頭にあるスキン自身の代替フォントの数。 / Number of the skin's own fallbacks at the front of the merged list. */
	private final int skinFallbackCount;

	private FreeTypeFontGenerator generator;
	private FreeTypeFontGenerator[] fallbackGenerators = NO_FALLBACKS;
	private FontGeneratorPool.Lease[] fallbackLeases = NO_LEASES;
	/** 読み込めたスキン自身の代替フォント数(fallbackGenerators の先頭範囲)。 / Count of loaded skin-declared fallbacks at the front of fallbackGenerators. */
	private int skinLoadedCount;
	/**
	 * 共通代替フォントの並びと文字ごとの選択。共通供給元が起動時に一度だけ作り、ほかの供給元は同じものを参照する。
	 * The global fallbacks' order and per-character choice, built once at startup by the shared global source and
	 * referenced by every other source.
	 */
	private FallbackFontPolicy.GlobalChoice<FreeTypeFontGenerator> globalChoice;
	private boolean loaded;
	private boolean disposed;
	private final Map<FreeTypeFontGenerator, FontCoverage> coverage = new IdentityHashMap<>();

	public SkinFontSource(String fontPath, String[] fallbackFontPaths) {
		this(fontPath, null, fallbackFontPaths);
	}

	public SkinFontSource(FileHandle fontFile, String[] fallbackFontPaths) {
		this(fontFile != null ? fontFile.path() : null, fontFile, fallbackFontPaths);
	}

	private SkinFontSource(String fontPath, FileHandle fontFile, String[] fallbackFontPaths) {
		this.fontPath = fontPath;
		this.fontFile = fontFile;
		this.fallbackFontPaths = FallbackFontPolicy.resolve(fontPath, fallbackFontPaths).clone();
		this.skinFallbackCount = fallbackFontPaths != null ? fallbackFontPaths.length : 0;
	}

	public boolean isAvailable() {
		if (disposed) {
			return false;
		}
		if (!loaded) {
			load();
		}
		return generator != null || fallbackGenerators.length > 0;
	}

	FreeTypeFontGenerator getGenerator() {
		if (!isAvailable()) {
			return null;
		}
		if (generator != null) {
			return generator;
		}
		return fallbackGenerators.length > 0 ? fallbackGenerators[0] : null;
	}

	/**
	 * スキン固有の主フォント生成器だけを返す(代替からの昇格を行わない)。
	 * Returns only the skin's own primary generator without promoting {@code fallbackGenerators[0]}.
	 */
	FreeTypeFontGenerator getPrimaryGenerator() {
		if (!isAvailable()) {
			return null;
		}
		return generator;
	}

	FreeTypeFontGenerator[] getFallbackGenerators() {
		if (!isAvailable() || fallbackGenerators == null) {
			return NO_FALLBACKS;
		}
		return fallbackGenerators;
	}

	/**
	 * 指定したコードポイントに対して試すべき代替フォント候補を優先順で返す。
	 * スキンが明示した代替フォントを宣言順のまま先に並べ(収録範囲で絞る)、続いて共通代替フォントの選択
	 * ({@link FallbackFontPolicy.GlobalChoice#candidates(int)})が返す受け持ちフォントと次の候補を並べる。
	 *
	 * Returns the ordered fallback generators to try for {@code codePoint}:
	 * skin-declared fallbacks first in their explicit order (filtered by coverage), followed by global fallbacks
	 * as chosen by {@link FallbackFontPolicy.GlobalChoice#candidates(int)} (the owner, then the next choices).
	 */
	List<FreeTypeFontGenerator> getFallbackCandidates(int codePoint) {
		if (!isAvailable() || fallbackGenerators == null || fallbackGenerators.length == 0) {
			return List.of();
		}
		var candidates = new ArrayList<FreeTypeFontGenerator>(fallbackGenerators.length);
		for (int i = 0; i < skinLoadedCount; i++) {
			FreeTypeFontGenerator skinFallback = fallbackGenerators[i];
			if (skinFallback != null && mayHaveGlyph(skinFallback, codePoint)) {
				candidates.add(skinFallback);
			}
		}
		if (globalChoice != null) {
			for (FreeTypeFontGenerator globalCandidate : globalChoice.candidates(codePoint)) {
				if (!candidates.contains(globalCandidate)) {
					candidates.add(globalCandidate);
				}
			}
		}
		return Collections.unmodifiableList(candidates);
	}

	/** 一定時間の参照だけ。ファイル解析はこの供給元を読み込むときにすべて済ませる。 / Constant-size lookup only: all file parsing happens when this source loads. */
	boolean mayHaveGlyph(FreeTypeFontGenerator generator, int codePoint) {
		return coverage.getOrDefault(generator, FontCoverage.UNKNOWN).mayContain(codePoint);
	}

	String[] getFallbackFontPaths() {
		return fallbackFontPaths != null ? fallbackFontPaths.clone() : new String[0];
	}

	/** 見つからないファイルの拡張子だけを置き換える。フォルダー・基本名・ハンドル種別は保つ。 / Only substitute a missing file's extension; preserve directory, basename and handle type. */
	static FileHandle resolveFontFile(FileHandle file) {
		if (file == null || file.exists()) return file;
		String alt = getAlternativeExtension(file.name());
		if (alt == null) return file;
		FileHandle sibling = file.sibling(alt);
		return sibling.exists() ? sibling : file;
	}

	boolean isScalablePrimary() {
		String path = fontFile != null ? fontFile.path() : fontPath;
		return isScalablePath(path);
	}

	static boolean isScalablePath(String path) {
		if (path == null) return false;
		String lower = path.toLowerCase(Locale.ROOT);
		return lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".ttc");
	}

	private static String getAlternativeExtension(String path) {
		String lower = path.toLowerCase(Locale.ROOT);
		if (lower.endsWith(".otf")) {
			return path.substring(0, path.length() - 4) + ".ttf";
		} else if (lower.endsWith(".ttf")) {
			return path.substring(0, path.length() - 4) + ".otf";
		}
		return null;
	}

	private void load() {
		loaded = true;
		// 共通代替フォントの供給元はプログラム全体で一つだけ持ち続ける。先に読み込んでおくと、その生成器が共有の場に残り、
		// スキンを替えても読み直さない。 / The global fallback source lives for the whole program; loading it first keeps its
		// generators pooled, so a skin change never re-reads them.
		SkinFontSource global = SystemFallbackGlyphs.globalSource();
		if (global != null && global != this) {
			global.isAvailable();
		}
		try {
			if (isScalablePrimary()) {
				try {
					FileHandle rawFile = fontFile != null ? fontFile
							: (Gdx.files != null ? Gdx.files.internal(fontPath) : new FileHandle(fontPath));
					FileHandle file = resolveFontFile(rawFile);
					FontFileSnapshot snapshot = FontFileSnapshot.capture(file);
					generator = new FreeTypeFontGenerator(snapshot.file());
					coverage.put(generator, snapshot.coverage());
				} catch (GdxRuntimeException e) {
					Logger.getGlobal().warning("Skin Font load failed: " + fontPath + " - " + e.getMessage());
					generator = null;
				}
			}
			fallbackGenerators = loadFallbackGenerators();
		} catch (RuntimeException e) {
			try {
				disposeGenerators();
			} catch (RuntimeException cleanupFailure) {
				e.addSuppressed(cleanupFailure);
				throw e;
			}
			if (e instanceof GdxRuntimeException) {
				Logger.getGlobal().warning("Skin Font load failed: " + fontPath + " - " + e.getMessage());
			} else {
				throw e;
			}
		}
	}

	private FreeTypeFontGenerator[] loadFallbackGenerators() {
		var leases = new FontGeneratorPool.Lease[fallbackFontPaths.length];
		// 読み込み前に所有を公開し、予期しない失敗でも先に取った項目を取り残さない。
		// Publish ownership before loading so an unexpected failure cannot orphan earlier entries.
		fallbackLeases = leases;
		var size = 0;
		var skinLoaded = 0;
		for (var index = 0; index < fallbackFontPaths.length; index++) {
			var fallbackPath = fallbackFontPaths[index];
			if (fallbackPath == null || fallbackPath.isEmpty()) {
				continue;
			}
			try {
				// スキンが指定したフォントはそのまま、共通代替フォントは可変なら決めた太さで開く。
				// Skin-declared fonts open as they are; global fallbacks open at the chosen weight when variable.
				leases[size] = index < skinFallbackCount ? FontGeneratorPool.acquire(fallbackPath)
						: FontGeneratorPool.acquire(fallbackPath, 0, FallbackFontPolicy.GLOBAL_WEIGHT);
				coverage.put(leases[size].generator(), leases[size].coverage());
				size++;
				if (index < skinFallbackCount) {
					skinLoaded = size;
				}
			} catch (GdxRuntimeException e) {
				Logger.getGlobal().warning("Fallback skin font load failed: " + fallbackPath + " - " + e.getMessage());
			}
		}
		if (size == 0) {
			skinLoadedCount = 0;
			globalChoice = null;
			return NO_FALLBACKS;
		}
		var generators = new FreeTypeFontGenerator[size];
		for (var i = 0; i < size; i++) {
			generators[i] = leases[i].generator();
		}
		this.skinLoadedCount = skinLoaded;
		// 共通代替フォントは起動時に一度だけ読み、変えない。並びと文字ごとの選択も共通供給元が一度だけ作り、ほかの供給元は参照するだけ。
		// スキンの順序は変えない。 / Global fallbacks are read once at startup and never change, so the shared source builds their
		// order and per-character choice once and every other source only refers to it. The skin's own order is kept.
		SkinFontSource shared = SystemFallbackGlyphs.globalSource();
		if (shared != null && shared != this && shared.isAvailable() && shared.globalChoice != null) {
			globalChoice = shared.globalChoice;
		} else if (size > skinLoaded) {
			var globals = Arrays.asList(generators).subList(skinLoaded, size);
			globalChoice = FallbackFontPolicy.GlobalChoice.build(
					FallbackFontPolicy.preferLocale(globals, coverage::get, Locale.getDefault()), coverage::get);
		}
		if (globalChoice != null) {
			var globals = globalChoice.fonts();
			generators = Arrays.copyOf(generators, skinLoaded + globals.size());
			for (var i = 0; i < globals.size(); i++) {
				generators[skinLoaded + i] = globals.get(i);
			}
		}
		return generators;
	}

	@Override
	public void dispose() {
		if (disposed) {
			return;
		}
		disposed = true;
		disposeGenerators();
	}

	private void disposeGenerators() {
		// 解放前に切り離し、失敗や再入でネイティブ資源を再解放しないようにする。
		// Detach before cleanup so failed/reentrant disposal cannot retry native resources.
		var ownedLeases = fallbackLeases;
		var ownedGenerator = generator;
		fallbackLeases = NO_LEASES;
		fallbackGenerators = NO_FALLBACKS;
		skinLoadedCount = 0;
		globalChoice = null;
		generator = null;
		coverage.clear();
		FontResourceDisposer.disposeAll(ownedLeases, ownedGenerator);
	}
}