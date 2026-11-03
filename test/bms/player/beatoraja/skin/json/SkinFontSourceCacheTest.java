package bms.player.beatoraja.skin.json;

import bms.player.beatoraja.skin.SkinFontSource;

import java.util.HashMap;

/** Regression tests for per-skin FreeType generator sharing. */
public final class SkinFontSourceCacheTest {

	private SkinFontSourceCacheTest() {
	}

	public static void main(String[] args) {
		var loader = new JSONSkinLoader();
		loader.fontSourceMap = new HashMap<>();

		var first = loader.getFontSource("skin/font.ttf", new String[] { "skin/fallback.ttf" });
		var same = loader.getFontSource("skin/font.ttf", new String[] { "skin/fallback.ttf" });
		var differentFallback = loader.getFontSource("skin/font.ttf", new String[] { "skin/other.ttf" });
		var noFallback = loader.getFontSource("skin/font.ttf", new String[0]);

		assertSame(first, same);
		assertNotSame(first, differentFallback);
		assertNotSame(first, noFallback);
	}

	private static void assertSame(SkinFontSource expected, SkinFontSource actual) {
		if (expected != actual) {
			throw new AssertionError("Expected the same font source");
		}
	}

	private static void assertNotSame(SkinFontSource expected, SkinFontSource actual) {
		if (expected == actual) {
			throw new AssertionError("Expected separate font sources");
		}
	}
}
