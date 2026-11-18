package bms.player.beatoraja.skin;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import bms.player.beatoraja.skin.BitmapFontCache.CacheableBitmapFont;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;

/**
 * キャッシュにないときだけ .fnt のメタデータとページを読む。返したテクスチャの所有者は BitmapFontCache。
 * Reads .fnt metadata and pages on a cache miss. BitmapFontCache owns the returned textures.
 */
final class BitmapFontLoader {

	/**
	 * 縦位置の代表値を決める基準文字。先頭ほど優先。
	 * Reference characters for the representative vertical offset, highest priority first.
	 */
	private static final int[] VERTICAL_ANCHORS = { 'S', 'P', 'A', 'N', 'O', 'T', 'H', 'E', 'R', '[', ']', 'M', '0' };

	private BitmapFontLoader() {
	}

	/**
	 * .fnt と各ページ画像を読み込む。失敗時は fontData などが null の結果を返す(読み込み済みテクスチャは解放する)。
	 * Loads a .fnt file and its page images. On failure returns a result with null font data
	 * after disposing any textures already loaded.
	 */
	static CacheableBitmapFont load(Path _fontPath, int _type, boolean usecim, boolean useMipMaps) {
		BitmapFont.BitmapFontData _fontData = null;
		Array<TextureRegion> _regions = null;
		BitmapFont _font = null;
		float _originalSize = 0;
		float _pageWidth = 0;
		float _pageHeight = 0;
		int _base = 0;
		int _glyphYOffset = 0;
		Map<Integer, BitmapFont.Glyph> _supplementaryGlyphs = Collections.emptyMap();

		try {
			FileHandle fontFile = new FileHandle(_fontPath.toFile());
			RemappedFontFile remappedFontFile = createRemappedFontFile(fontFile);
			_fontData = new BitmapFont.BitmapFontData(remappedFontFile.fileHandle, false);
			_supplementaryGlyphs = remappedFontFile.supplementaryGlyphs;

			_regions = new Array<>(_fontData.imagePaths.length);
			for (int i = 0; i < _fontData.imagePaths.length; ++i) {
				Texture texture = SkinLoader.getTexture(_fontData.imagePaths[i], usecim, useMipMaps);
				if (texture == null) {
					throw new IOException("Could not load bitmap font page: " + _fontData.imagePaths[i]);
				}
				_regions.add(new TextureRegion(texture));
			}

			_font = new BitmapFont(_fontData, _regions, true);

			// 先頭 2 行(info/common)から元サイズとページ寸法を読む。読めなければ libGDX の計測値で代用する。
			// Read the original size and page size from the info/common lines; fall back to libGDX metrics if unreadable.
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileHandle(_fontPath.toFile()).read(),
					StandardCharsets.UTF_8), 512)) {
				String line = reader.readLine();
				_originalSize = (float) Integer.parseInt(line.substring(line.indexOf("size=") + 5).split(" ")[0]);
				line = reader.readLine();
				_base = getIntAttribute(line, "base");
				_pageWidth = (float) Integer.parseInt(line.substring(line.indexOf("scaleW=") + 7).split(" ")[0]);
				_pageHeight = (float) Integer.parseInt(line.substring(line.indexOf("scaleH=") + 7).split(" ")[0]);
				_glyphYOffset = readRepresentativeGlyphYOffset(reader);
			} catch (Exception e) {
				_originalSize = _fontData.lineHeight;
				_base = (int) _fontData.ascent;
				_glyphYOffset = getRepresentativeGlyphYOffset(_fontData);
				if (_regions.size > 0) {
					_pageWidth = (float) _regions.get(0).getRegionWidth();
					_pageHeight = (float) _regions.get(0).getRegionHeight();
				}
			}
		} catch (Exception e) {
			// 読み込み失敗は空の結果で返すが、原因が分かるよう一度記録する。
			// A load failure still returns an empty result, but log the cause once so it can be traced.
			Logger.getGlobal().warning("Bitmap font load failed: " + _fontPath + " - " + e);
			if (_regions != null) {
				for (TextureRegion region : _regions) {
					region.getTexture().dispose();
				}
			}
			_fontData = null;
			_regions = null;
			_font = null;
		}

		CacheableBitmapFont result = new CacheableBitmapFont();
		result.fontData = _fontData;
		result.regions = _regions;
		result.font = _font;
		result.originalSize = _originalSize;
		result.type = _type;
		result.pageWidth = _pageWidth;
		result.pageHeight = _pageHeight;
		result.base = _base;
		result.glyphYOffset = _glyphYOffset;
		result.supplementaryGlyphs = _supplementaryGlyphs;

		return result;
	}

	/**
	 * libGDX が読めない補助平面(U+10000 以上)の char/kerning 行を取り除き、件数を数え直した .fnt を返す。
	 * 取り除いた char は supplementaryGlyphs に別途保持する。補助平面がなければ元のハンドルをそのまま返す。
	 * Strips supplementary-plane (U+10000+) char/kerning lines that libGDX cannot read and fixes the counts.
	 * Removed chars are kept in supplementaryGlyphs. Returns the original handle when nothing was removed.
	 */
	static RemappedFontFile createRemappedFontFile(FileHandle fontFile) {
		String fontText = new String(fontFile.readBytes(), StandardCharsets.UTF_8);
		Map<Integer, BitmapFont.Glyph> glyphMap = new HashMap<>();
		String[] lines = fontText.split("\\r?\\n", -1);
		StringBuilder remappedText = new StringBuilder(fontText.length());
		int charCount = 0;
		int kerningCount = 0;

		for (String line : lines) {
			if (line.startsWith("char ")) {
				int codePoint = getIntAttribute(line, "id");
				if (codePoint > Character.MAX_VALUE && codePoint <= Character.MAX_CODE_POINT) {
					BitmapFont.Glyph glyph = createGlyph(line, codePoint);
					if (glyph != null) {
						glyphMap.put(codePoint, glyph);
					}
					continue;
				}
				charCount++;
			} else if (line.startsWith("kerning ")) {
				int first = getIntAttribute(line, "first");
				int second = getIntAttribute(line, "second");
				if (first > Character.MAX_VALUE || second > Character.MAX_VALUE) {
					continue;
				}
				kerningCount++;
			}
		}

		for (String line : lines) {
			if (line.startsWith("char ")) {
				int codePoint = getIntAttribute(line, "id");
				if (codePoint > Character.MAX_VALUE && codePoint <= Character.MAX_CODE_POINT) {
					continue;
				}
			} else if (line.startsWith("kerning ")) {
				int first = getIntAttribute(line, "first");
				int second = getIntAttribute(line, "second");
				if (first > Character.MAX_VALUE || second > Character.MAX_VALUE) {
					continue;
				}
			}
			String remappedLine = line;
			if (line.startsWith("chars count=")) {
				remappedLine = "chars count=" + charCount;
			} else if (line.startsWith("kernings count=")) {
				remappedLine = "kernings count=" + kerningCount;
			}
			if (remappedText.length() > 0) {
				remappedText.append('\n');
			}
			remappedText.append(remappedLine);
		}

		if (glyphMap.isEmpty()) {
			return new RemappedFontFile(fontFile, Collections.emptyMap());
		}
		return new RemappedFontFile(new InMemoryFontFileHandle(fontFile, remappedText.toString().getBytes(StandardCharsets.UTF_8)),
				glyphMap);
	}

	private static BitmapFont.Glyph createGlyph(String line, int codePoint) {
		try {
			BitmapFont.Glyph glyph = new BitmapFont.Glyph();
			glyph.id = codePoint;
			glyph.srcX = getIntAttribute(line, "x");
			glyph.srcY = getIntAttribute(line, "y");
			glyph.width = getIntAttribute(line, "width");
			glyph.height = getIntAttribute(line, "height");
			glyph.xoffset = getIntAttribute(line, "xoffset");
			glyph.yoffset = -glyph.height - getIntAttribute(line, "yoffset");
			glyph.xadvance = getIntAttribute(line, "xadvance");
			glyph.page = getIntAttribute(line, "page");
			return glyph;
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static int getIntAttribute(String line, String name) {
		String prefix = name + "=";
		int valueStart = line.indexOf(prefix);
		if (valueStart < 0) {
			return 0;
		}
		valueStart += prefix.length();
		int valueEnd = valueStart;
		if (valueEnd < line.length() && line.charAt(valueEnd) == '-') {
			valueEnd++;
		}
		while (valueEnd < line.length() && Character.isDigit(line.charAt(valueEnd))) {
			valueEnd++;
		}
		return Integer.parseInt(line.substring(valueStart, valueEnd));
	}

	/**
	 * .fnt の char 行から代表的な yoffset を求める。基準文字がなければ最初の可視文字の値を使う。
	 * Finds the representative yoffset from .fnt char lines, falling back to the first visible character.
	 */
	private static int readRepresentativeGlyphYOffset(BufferedReader reader) throws java.io.IOException {
		Map<Integer, Integer> anchorOffsets = new HashMap<>();
		Integer firstVisibleOffset = null;
		String line;
		while ((line = reader.readLine()) != null) {
			if (!line.startsWith("char ")) {
				continue;
			}
			int height = getIntAttribute(line, "height");
			if (height <= 0) {
				continue;
			}
			int codePoint = getIntAttribute(line, "id");
			int yoffset = getIntAttribute(line, "yoffset");
			if (firstVisibleOffset == null) {
				firstVisibleOffset = yoffset;
			}
			if (isVerticalAnchor(codePoint)) {
				anchorOffsets.put(codePoint, yoffset);
			}
		}
		for (int codePoint : VERTICAL_ANCHORS) {
			Integer yoffset = anchorOffsets.get(codePoint);
			if (yoffset != null) {
				return yoffset.intValue();
			}
		}
		return firstVisibleOffset != null ? firstVisibleOffset.intValue() : 0;
	}

	/**
	 * 読み込み済みデータから同じ代表値を求める(libGDX の座標系から .fnt の yoffset に戻す)。
	 * Same representative value computed from loaded data, converted back from libGDX coordinates to .fnt yoffset.
	 */
	static int getRepresentativeGlyphYOffset(BitmapFont.BitmapFontData fontData) {
		for (int codePoint : VERTICAL_ANCHORS) {
			BitmapFont.Glyph glyph = fontData.getGlyph((char) codePoint);
			if (glyph != null && glyph.height > 0) {
				return -glyph.yoffset - glyph.height;
			}
		}
		return 0;
	}

	private static boolean isVerticalAnchor(int codePoint) {
		for (int anchor : VERTICAL_ANCHORS) {
			if (anchor == codePoint) return true;
		}
		return false;
	}

	/**
	 * 読み込み用ハンドルと、取り除いた補助平面グリフの組。 / Read handle paired with the removed supplementary-plane glyphs.
	 */
	static final class RemappedFontFile {
		final FileHandle fileHandle;
		final Map<Integer, BitmapFont.Glyph> supplementaryGlyphs;

		private RemappedFontFile(FileHandle fileHandle, Map<Integer, BitmapFont.Glyph> supplementaryGlyphs) {
			this.fileHandle = fileHandle;
			this.supplementaryGlyphs = supplementaryGlyphs;
		}
	}

	/**
	 * 書き換えた .fnt をメモリから返す。親フォルダは元ファイルのものを返し、ページ画像の相対パスを保つ。
	 * Serves the rewritten .fnt from memory while keeping the original parent so relative page paths still resolve.
	 */
	private static final class InMemoryFontFileHandle extends FileHandle {
		private final FileHandle original;
		private final byte[] bytes;

		private InMemoryFontFileHandle(FileHandle original, byte[] bytes) {
			super(original.file());
			this.original = original;
			this.bytes = bytes;
		}

		@Override
		public InputStream read() {
			return new ByteArrayInputStream(bytes);
		}

		@Override
		public FileHandle parent() {
			return original.parent();
		}
	}

	/**
	 * 変更可能な計測値とグリフを複製する。テクスチャページは所有者から借りたまま。
	 * Copies mutable metrics and glyphs; texture pages remain borrowed from their owner.
	 */
	static final class BitmapFontDataCopy {

		private BitmapFontDataCopy() {
		}

		/**
		 * UV・カーニングを含むグリフの深いコピー。null なら null。 / Deep copy of a glyph including UVs and kerning; null for null.
		 */
		static Glyph copyGlyph(Glyph src) {
			if (src == null) {
				return null;
			}
			Glyph dst = new Glyph();
			dst.id = src.id;
			dst.srcX = src.srcX;
			dst.srcY = src.srcY;
			dst.width = src.width;
			dst.height = src.height;
			dst.u = src.u;
			dst.v = src.v;
			dst.u2 = src.u2;
			dst.v2 = src.v2;
			dst.xoffset = src.xoffset;
			dst.yoffset = src.yoffset;
			dst.xadvance = src.xadvance;
			dst.page = src.page;
			dst.fixedWidth = src.fixedWidth;
			if (src.kerning != null) {
				dst.kerning = new byte[src.kerning.length][];
				for (int k = 0; k < src.kerning.length; k++) {
					if (src.kerning[k] != null) {
						dst.kerning[k] = src.kerning[k].clone();
					}
				}
			}
			return dst;
		}

		/**
		 * 作業用の BitmapFontData を作る。テクスチャのない領域では UV 設定を無視し、元データは変更しない。
		 * Creates a working BitmapFontData copy that ignores UV binding to texture-less regions; the source is unchanged.
		 */
		static BitmapFont.BitmapFontData copyFontData(BitmapFont.BitmapFontData src) {
			if (src == null) {
				return null;
			}
			BitmapFont.BitmapFontData dst = new BitmapFont.BitmapFontData() {
				@Override
				public void setGlyphRegion(Glyph glyph, TextureRegion region) {
					if (glyph == null || region == null || region.getTexture() == null) {
						return;
					}
					super.setGlyphRegion(glyph, region);
				}
			};
			dst.imagePaths = src.imagePaths != null ? src.imagePaths.clone() : null;
			dst.fontFile = src.fontFile;
			dst.flipped = src.flipped;
			dst.padTop = src.padTop;
			dst.padRight = src.padRight;
			dst.padBottom = src.padBottom;
			dst.padLeft = src.padLeft;
			dst.lineHeight = src.lineHeight;
			dst.capHeight = src.capHeight;
			dst.ascent = src.ascent;
			dst.descent = src.descent;
			dst.down = src.down;
			dst.blankLineScale = src.blankLineScale;
			dst.scaleX = src.scaleX;
			dst.scaleY = src.scaleY;
			dst.markupEnabled = src.markupEnabled;
			dst.cursorX = src.cursorX;
			dst.spaceXadvance = src.spaceXadvance;
			dst.xHeight = src.xHeight;
			dst.breakChars = src.breakChars != null ? src.breakChars.clone() : null;
			dst.xChars = src.xChars != null ? src.xChars.clone() : null;
			dst.capChars = src.capChars != null ? src.capChars.clone() : null;

			// libGDX の missingGlyph があると欠落文字がすべて豆腐として扱われ、代替フォント探索に進まない。
			// 作業用コピーでだけ null にする(元データの方針は変えない)。
			// libGDX's missingGlyph turns every missing character into tofu and hides it from fallback lookup.
			// Clear it on the working copy only; the source keeps its own policy.
			dst.missingGlyph = null;

			if (src.glyphs != null) {
				for (int pageIdx = 0; pageIdx < src.glyphs.length; pageIdx++) {
					Glyph[] srcPage = src.glyphs[pageIdx];
					if (srcPage != null) {
						Glyph[] dstPage = new Glyph[srcPage.length];
						for (int glyphIdx = 0; glyphIdx < srcPage.length; glyphIdx++) {
							if (srcPage[glyphIdx] != null) {
								dstPage[glyphIdx] = copyGlyph(srcPage[glyphIdx]);
							}
						}
						dst.glyphs[pageIdx] = dstPage;
					} else {
						dst.glyphs[pageIdx] = null;
					}
				}
			}
			return dst;
		}
	}
}
