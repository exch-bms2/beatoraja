package bms.player.beatoraja.skin;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import bms.player.beatoraja.skin.FontRenderRetirement.FontResourceDisposer;
import bms.player.beatoraja.skin.FontGeneratorPool.FontFileSnapshot;

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

	private FreeTypeFontGenerator generator;
	private FreeTypeFontGenerator[] fallbackGenerators = NO_FALLBACKS;
	private FontGeneratorPool.Lease[] fallbackLeases = NO_LEASES;
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
		this.fallbackFontPaths = fallbackFontPaths == null ? new String[0] : fallbackFontPaths.clone();
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

	FreeTypeFontGenerator[] getFallbackGenerators() {
		return isAvailable() && fallbackGenerators != null ? fallbackGenerators : NO_FALLBACKS;
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
		try {
			if (isScalablePrimary()) {
				try {
					FileHandle file = resolveFontFile(fontFile != null ? fontFile : Gdx.files.internal(fontPath));
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
		for (var fallbackPath : fallbackFontPaths) {
			if (fallbackPath == null || fallbackPath.isEmpty()) {
				continue;
			}
			try {
				leases[size] = FontGeneratorPool.acquire(fallbackPath);
				coverage.put(leases[size].generator(), leases[size].coverage());
				size++;
			} catch (GdxRuntimeException e) {
				Logger.getGlobal().warning("Fallback skin font load failed: " + fallbackPath + " - " + e.getMessage());
			}
		}
		if (size == 0) {
			return NO_FALLBACKS;
		}
		var generators = new FreeTypeFontGenerator[size];
		for (var i = 0; i < size; i++) {
			generators[i] = leases[i].generator();
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
		generator = null;
		coverage.clear();
		FontResourceDisposer.disposeAll(ownedLeases, ownedGenerator);
	}
}
