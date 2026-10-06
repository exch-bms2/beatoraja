package bms.player.beatoraja.skin;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Shared FreeType generators used by text objects in one skin.
 *
 * <p>A generator keeps a native FreeType face and a copy of the font data. Keeping one source per
 * font/fallback combination prevents every {@link SkinTextFont} from loading that data separately.</p>
 */
public final class SkinFontSource implements Disposable {
	private static final FreeTypeFontGenerator[] NO_FALLBACKS = new FreeTypeFontGenerator[0];

	private final String fontPath;
	private final String[] fallbackFontPaths;

	private FreeTypeFontGenerator generator;
	private FreeTypeFontGenerator[] fallbackGenerators = NO_FALLBACKS;
	private boolean loaded;
	private boolean disposed;

	public SkinFontSource(String fontPath, String[] fallbackFontPaths) {
		this.fontPath = fontPath;
		this.fallbackFontPaths = fallbackFontPaths == null ? new String[0] : fallbackFontPaths.clone();
	}

	public boolean isAvailable() {
		if (disposed) {
			return false;
		}
		if (!loaded) {
			load();
		}
		return generator != null;
	}

	FreeTypeFontGenerator getGenerator() {
		return isAvailable() ? generator : null;
	}

	FreeTypeFontGenerator[] getFallbackGenerators() {
		return isAvailable() ? fallbackGenerators : NO_FALLBACKS;
	}

	private void load() {
		loaded = true;
		try {
			generator = new FreeTypeFontGenerator(Gdx.files.internal(fontPath));
			fallbackGenerators = loadFallbackGenerators();
		} catch (GdxRuntimeException e) {
			Logger.getGlobal().warning("Skin Font load failed: " + fontPath + " - " + e.getMessage());
			disposeGenerators();
		}
	}

	private FreeTypeFontGenerator[] loadFallbackGenerators() {
		var generators = new FreeTypeFontGenerator[fallbackFontPaths.length];
		var size = 0;
		for (var fallbackPath : fallbackFontPaths) {
			if (fallbackPath == null || fallbackPath.isEmpty()) {
				continue;
			}
			try {
				generators[size] = new FreeTypeFontGenerator(Gdx.files.internal(fallbackPath));
				size++;
			} catch (GdxRuntimeException e) {
				Logger.getGlobal().warning("Fallback skin font load failed: " + fallbackPath + " - " + e.getMessage());
			}
		}
		return size == 0 ? NO_FALLBACKS : Arrays.copyOf(generators, size);
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
		for (var fallbackGenerator : fallbackGenerators) {
			fallbackGenerator.dispose();
		}
		fallbackGenerators = NO_FALLBACKS;
		if (generator != null) {
			generator.dispose();
			generator = null;
		}
	}
}
