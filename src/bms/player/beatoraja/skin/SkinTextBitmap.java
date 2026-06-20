package bms.player.beatoraja.skin;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import bms.player.beatoraja.skin.property.StringProperty;
import bms.player.beatoraja.skin.property.StringPropertyFactory;
import bms.player.beatoraja.skin.BitmapFontCache.CacheableBitmapFont;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.GlyphLayout.GlyphRun;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

/**
 * .fnt ファイルをソースとして持つスキン用テキスト
 */
public final class SkinTextBitmap extends SkinText {

	private final SkinTextBitmapSource source;
	private final BitmapFont font;
	private final GlyphLayout layout;
	private final float size;

	public SkinTextBitmap(SkinTextBitmapSource source, float size) {
		this(source, size, StringPropertyFactory.getStringProperty(-1));
	}

	public SkinTextBitmap(SkinTextBitmapSource source, float size, StringProperty property) {
		super(property);
		this.source = source;
		this.size = size;
		this.layout =new GlyphLayout();
		this.font = source.getFont();
	}

	@Override
	public void load() {
	}

	@Override
	public void prepareFont(String text) {
	}

	@Override
	protected void prepareText(String text) {
	}

	public void draw(SkinObjectRenderer sprite, float offsetX, float offsetY) {
		if (font == null)
			return;

		float scale = this.size / source.getOriginalSize();
		font.getData().setScale(scale);
		final float x = (getAlign() == 2 ? region.x - region.width : (getAlign() == 1 ? region.x - region.width / 2 : region.x));
		if (source.getType() == SkinTextBitmapSource.TYPE_DISTANCE_FIELD ||
				source.getType() == SkinTextBitmapSource.TYPE_COLORED_DISTANCE_FIELD) {
			sprite.setType(SkinObjectRenderer.TYPE_DISTANCE_FIELD);
			setLayout(color, region);
			sprite.draw(font, layout, x + offsetX, region.y + offsetY + region.getHeight(), shader -> {
				shader.setUniformf("u_outlineDistance", Math.max(0.1f, 0.5f - getOutlineWidth()/2f));
				shader.setUniformf("u_outlineColor", getOutlineColor());
				shader.setUniformf("u_shadowColor", getShadowColor());
				shader.setUniformf("u_shadowSmoothing", getShadowSmoothness() / 2f);
				shader.setUniformf("u_shadowOffset",
						new Vector2(getShadowOffset().x / source.getPageWidth(), getShadowOffset().y / source.getPageHeight()));
			});
			drawFallbackColorGlyphs(sprite, x + offsetX, region.y + offsetY + region.getHeight());
		} else {
			sprite.setType(SkinObjectRenderer.TYPE_BILINEAR);
			if (!getShadowOffset().isZero()) {
				setLayout(new Color(color.r / 2, color.g / 2, color.b / 2, color.a), region);
				sprite.draw(font, layout, x + getShadowOffset().x + offsetX, region.y - getShadowOffset().y + offsetY + region.getHeight());
			}
			setLayout(color, region);
			sprite.draw(font, layout, x + offsetX, region.y + offsetY + region.getHeight());
		}
		font.getData().setScale(1);
	}

	private void drawFallbackColorGlyphs(SkinObjectRenderer sprite, float x, float y) {
		if (!source.hasFallbackColorGlyphs(layout)) {
			return;
		}

		Color previous = new Color(sprite.getColor());
		sprite.setColor(1, 1, 1, color.a);
		sprite.setType(SkinObjectRenderer.TYPE_BILINEAR);

		float scaleX = font.getData().scaleX;
		float scaleY = font.getData().scaleY;
		for (GlyphRun run : layout.runs) {
			float glyphX = x + run.x;
			float glyphY = y + run.y;
			for (int i = 0; i < run.glyphs.size; i++) {
				Glyph glyph = run.glyphs.get(i);
				glyphX += run.xAdvances.get(i);
				if (!source.isFallbackColorGlyph(glyph)) {
					continue;
				}
				TextureRegion region = source.getRegion(glyph.page);
				if (region == null) {
					continue;
				}
				TextureRegion glyphRegion = new TextureRegion(region);
				glyphRegion.setRegion(glyph.u, glyph.v2, glyph.u2, glyph.v);
				sprite.draw(glyphRegion, glyphX + glyph.xoffset * scaleX, glyphY + glyph.yoffset * scaleY,
						glyph.width * scaleX, glyph.height * scaleY);
			}
		}

		sprite.setType(SkinObjectRenderer.TYPE_DISTANCE_FIELD);
		sprite.setColor(previous);
	}

	private void setLayout(Color c, Rectangle r) {
		String text = source.remapSupplementaryGlyphs(getText());
		if (isWrapping()) {
			layout.setText(font, text, c, r.getWidth(), ALIGN[getAlign()], true);
		} else {
			switch (getOverflow()) {
				case OVERFLOW_OVERFLOW -> {
					layout.setText(font, text, c, r.getWidth(), ALIGN[getAlign()], false);
				}
				case OVERFLOW_SHRINK -> {
					layout.setText(font, text, c, r.getWidth(), ALIGN[getAlign()], false);
					float actualWidth = layout.width;
					if (actualWidth > r.getWidth()) {
						font.getData().setScale(font.getData().scaleX * r.getWidth() / actualWidth, font.getData().scaleY);
						layout.setText(font, text, c, r.getWidth(), ALIGN[getAlign()], false);
					}
				}
				case OVERFLOW_TRUNCATE -> {
					layout.setText(font, text, 0, text.length(), c, r.getWidth(), ALIGN[getAlign()], false, "");
				}
			}
		}
	}

	public void dispose() {
		source.dispose();
	}

	public static final class SkinTextBitmapSource implements Disposable {

		public static final int TYPE_STANDARD = 0;
		public static final int TYPE_DISTANCE_FIELD = 1;
		public static final int TYPE_COLORED_DISTANCE_FIELD = 2;

		private boolean usecim;
		private boolean useMipMaps;
		private Path fontPath;
		private FallbackFont[] fallbackFonts = new FallbackFont[0];
		private BitmapFont.BitmapFontData fontData;
		private Array<TextureRegion> regions;
		private BitmapFont font;
		private float originalSize;
		private int type;
		private float pageWidth;
		private float pageHeight;
		private int base;
		private int glyphYOffset;
		private Map<Integer, BitmapFont.Glyph> supplementaryGlyphs = Collections.emptyMap();
		private final Map<Integer, Character> activeSupplementaryGlyphMap = new HashMap<>();
		private final Map<Character, Integer> activePrivateUseGlyphMap = new HashMap<>();
		private final Set<Integer> activeBmpFallbackGlyphs = new HashSet<>();
		private final Map<FallbackFont, Integer> fallbackPageOffsets = new HashMap<>();
		private final Set<Integer> activeFallbackPages = new HashSet<>();

		private static final int PRIVATE_USE_AREA_START = 0xe000;
		private static final int PRIVATE_USE_AREA_END = 0xf8ff;
		private static final int[] MISSING_GLYPH_CANDIDATES = { 0x25a1, 0x25a2, 0x2610, 0x25a0, '?' }; // □, ▢, ☐, ■, ?

		public SkinTextBitmapSource(Path fontPath, boolean usecim) {
			this(fontPath, usecim, true);
		}

		public SkinTextBitmapSource(Path fontPath, boolean usecim, boolean useMipMaps) {
			this(fontPath, new FallbackFont[0], usecim, useMipMaps);
		}

		public SkinTextBitmapSource(Path fontPath, Path[] fallbackFontPaths, boolean usecim) {
			this(fontPath, fallbackFontPaths, usecim, true);
		}

		public SkinTextBitmapSource(Path fontPath, Path[] fallbackFontPaths, boolean usecim, boolean useMipMaps) {
			this(fontPath, toFallbackFonts(fallbackFontPaths), usecim, useMipMaps);
		}

		public SkinTextBitmapSource(Path fontPath, FallbackFont[] fallbackFonts, boolean usecim) {
			this(fontPath, fallbackFonts, usecim, true);
		}

		public SkinTextBitmapSource(Path fontPath, FallbackFont[] fallbackFonts, boolean usecim, boolean useMipMaps) {
			this.usecim = usecim;
			this.useMipMaps = useMipMaps;
			this.fontPath = fontPath;
			this.fallbackFonts = fallbackFonts != null ? fallbackFonts : new FallbackFont[0];
		}

		public CacheableBitmapFont createCacheableFont(Path _fontPath, int _type) {
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
					_regions.add(new TextureRegion(SkinLoader.getTexture(_fontData.imagePaths[i], usecim, useMipMaps)));
				}

				_font = new BitmapFont(_fontData, _regions, true);

				// size が BitmapFont から取得できないので、独自に取得する
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileHandle(_fontPath.toFile()).read()), 512)) {
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

		public BitmapFont getFont() {
			if (!BitmapFontCache.Has(fontPath, type)) {
				CacheableBitmapFont _newFont = createCacheableFont(fontPath, type);
				BitmapFontCache.Set(fontPath, type, _newFont);
			}

			CacheableBitmapFont cached = BitmapFontCache.Get(fontPath, type);

			fontData = cached.fontData;
			regions = cached.regions != null ? new Array<>(cached.regions) : new Array<>();
			originalSize = cached.originalSize;
			type = cached.type;
			pageWidth = cached.pageWidth;
			pageHeight = cached.pageHeight;
			base = cached.base;
			glyphYOffset = cached.glyphYOffset;
			supplementaryGlyphs = cached.supplementaryGlyphs != null ? cached.supplementaryGlyphs : Collections.emptyMap();
			activeBmpFallbackGlyphs.clear();
			activeSupplementaryGlyphMap.clear();
			activePrivateUseGlyphMap.clear();
			fallbackPageOffsets.clear();
			activeFallbackPages.clear();
			attachFallbackFonts();
			font = fontData != null ? new BitmapFont(fontData, regions, false) : null;

			return font;
		}

		public String remapSupplementaryGlyphs(String text) {
			if (fontData == null) {
				return text;
			}

			StringBuilder result = null;
			Set<Integer> requiredCodePoints = new HashSet<>();
			for (int index = 0; index < text.length();) {
				int codePoint = text.codePointAt(index);
				if (codePoint > Character.MAX_VALUE) {
					if (result == null) {
						result = new StringBuilder(text.length());
						result.append(text, 0, index);
					}
					requiredCodePoints.add(codePoint);
					Character mapped = ensureSupplementaryGlyphMapped(codePoint, requiredCodePoints);
					if (mapped == null) {
						index += Character.charCount(codePoint);
						continue;
					}
					result.append(mapped.charValue());
				} else if (result != null) {
					ensureBmpFallbackGlyph(codePoint);
					result.append((char) codePoint);
				} else {
					ensureBmpFallbackGlyph(codePoint);
				}
				index += Character.charCount(codePoint);
			}
			return result != null ? result.toString() : text;
		}

		private void attachFallbackFonts() {
			for (FallbackFont fallback : fallbackFonts) {
				if (fallback == null || fallback.path == null || fallbackPageOffsets.containsKey(fallback)) {
					continue;
				}
				if (!BitmapFontCache.Has(fallback.path, fallback.type)) {
					CacheableBitmapFont fallbackFont = createCacheableFont(fallback.path, fallback.type);
					BitmapFontCache.Set(fallback.path, fallback.type, fallbackFont);
				}
				CacheableBitmapFont fallbackFont = BitmapFontCache.Get(fallback.path, fallback.type);
				if (fallbackFont == null || fallbackFont.fontData == null || fallbackFont.regions == null) {
					continue;
				}
				int pageOffset = regions.size;
				int pageCount = 0;
				for (TextureRegion region : fallbackFont.regions) {
					regions.add(region);
					if (fallback.type == TYPE_STANDARD) {
						activeFallbackPages.add(pageOffset + pageCount);
					}
					pageCount++;
				}
				if (pageCount == 0) {
					continue;
				}
				fallbackPageOffsets.put(fallback, pageOffset);
			}
		}

		private RemappedFontFile createRemappedFontFile(FileHandle fontFile) {
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

		private void ensureBmpFallbackGlyph(int codePoint) {
			if (activeBmpFallbackGlyphs.contains(codePoint) || fontData.getGlyph((char) codePoint) != null) {
				return;
			}

			BitmapFont.Glyph glyph = findFallbackGlyph(codePoint, codePoint, false);
			if (glyph == null) {
				glyph = findMissingGlyph(codePoint);
			}
			if (glyph != null && hasRegion(glyph.page)) {
				fontData.setGlyphRegion(glyph, regions.get(glyph.page));
				fontData.setGlyph(codePoint, glyph);
				activeBmpFallbackGlyphs.add(codePoint);
			}
		}

		private Character ensureSupplementaryGlyphMapped(int codePoint, Set<Integer> requiredCodePoints) {
			Character mapped = activeSupplementaryGlyphMap.get(codePoint);
			if (mapped == null) {
				mapped = findPrivateUseSlot(requiredCodePoints);
				if (mapped == null) {
					return null;
				}
				BitmapFont.Glyph glyph = findFallbackGlyph(codePoint, mapped.charValue(), true);
				if (glyph == null) {
					glyph = findMissingGlyph(mapped.charValue());
				}
				if (glyph == null || !hasRegion(glyph.page)) {
					return null;
				}
				fontData.setGlyphRegion(glyph, regions.get(glyph.page));
				fontData.setGlyph(mapped.charValue(), glyph);
				activeSupplementaryGlyphMap.put(codePoint, mapped);
				activePrivateUseGlyphMap.put(mapped, codePoint);
			}
			return mapped;
		}

		private Character findPrivateUseSlot(Set<Integer> requiredCodePoints) {
			for (int codePoint = PRIVATE_USE_AREA_START; codePoint <= PRIVATE_USE_AREA_END; codePoint++) {
				Character slot = Character.valueOf((char) codePoint);
				Integer activeCodePoint = activePrivateUseGlyphMap.get(slot);
				if (activeCodePoint == null) {
					return slot;
				}
				if (!requiredCodePoints.contains(activeCodePoint)) {
					fontData.setGlyph(slot.charValue(), null);
					activePrivateUseGlyphMap.remove(slot);
					activeSupplementaryGlyphMap.remove(activeCodePoint);
					return slot;
				}
			}
			return null;
		}

		private BitmapFont.Glyph findMissingGlyph(int mappedCodePoint) {
			for (int candidate : MISSING_GLYPH_CANDIDATES) {
				BitmapFont.Glyph glyph = fontData.getGlyph((char) candidate);
				if (glyph != null && hasRegion(glyph.page)) {
					return copyGlyph(glyph, mappedCodePoint, 0);
				}
			}

			for (FallbackFont fallback : fallbackFonts) {
				if (fallback == null || fallback.path == null) {
					continue;
				}
				CacheableBitmapFont fallbackFont = BitmapFontCache.Get(fallback.path, fallback.type);
				Integer pageOffset = fallbackPageOffsets.get(fallback);
				if (fallbackFont == null || fallbackFont.fontData == null || fallbackFont.regions == null || pageOffset == null) {
					continue;
				}
				for (int candidate : MISSING_GLYPH_CANDIDATES) {
					BitmapFont.Glyph glyph = fallbackFont.fontData.getGlyph((char) candidate);
					if (glyph != null && glyph.page >= 0 && glyph.page < fallbackFont.regions.size
							&& hasRegion(glyph.page + pageOffset.intValue())) {
						return copyGlyph(glyph, mappedCodePoint, pageOffset.intValue(), fallbackFont.glyphYOffset - glyphYOffset);
					}
				}
			}
			return null;
		}

		private BitmapFont.Glyph findFallbackGlyph(int codePoint, int mappedCodePoint, boolean supplementary) {
			if (supplementary) {
				BitmapFont.Glyph glyph = supplementaryGlyphs.get(codePoint);
				if (glyph != null && hasRegion(glyph.page)) {
					return copyGlyph(glyph, mappedCodePoint, 0);
				}
			}

			for (FallbackFont fallback : fallbackFonts) {
				if (fallback == null || fallback.path == null) {
					continue;
				}
				CacheableBitmapFont fallbackFont = BitmapFontCache.Get(fallback.path, fallback.type);
				Integer pageOffset = fallbackPageOffsets.get(fallback);
				if (fallbackFont == null || fallbackFont.fontData == null || pageOffset == null) {
					continue;
				}
				BitmapFont.Glyph glyph = supplementary
						? (fallbackFont.supplementaryGlyphs != null ? fallbackFont.supplementaryGlyphs.get(codePoint) : null)
						: fallbackFont.fontData.getGlyph((char) codePoint);
				if (glyph != null && glyph.page >= 0 && glyph.page < fallbackFont.regions.size
						&& hasRegion(glyph.page + pageOffset.intValue())) {
					return copyGlyph(glyph, mappedCodePoint, pageOffset.intValue(), fallbackFont.glyphYOffset - glyphYOffset);
				}
			}
			return null;
		}

		private boolean hasRegion(int page) {
			return page >= 0 && page < regions.size && regions.get(page) != null;
		}

		private boolean isFallbackColorGlyph(Glyph glyph) {
			return glyph != null && activeFallbackPages.contains(glyph.page);
		}

		private boolean hasFallbackColorGlyphs(GlyphLayout layout) {
			for (GlyphRun run : layout.runs) {
				for (int i = 0; i < run.glyphs.size; i++) {
					if (isFallbackColorGlyph(run.glyphs.get(i))) {
						return true;
					}
				}
			}
			return false;
		}

		private TextureRegion getRegion(int page) {
			return hasRegion(page) ? regions.get(page) : null;
		}

		private BitmapFont.Glyph createGlyph(String line, int codePoint) {
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

		private BitmapFont.Glyph copyGlyph(BitmapFont.Glyph source, int mappedCodePoint) {
			return copyGlyph(source, mappedCodePoint, 0);
		}

		private BitmapFont.Glyph copyGlyph(BitmapFont.Glyph source, int mappedCodePoint, int pageOffset) {
			return copyGlyph(source, mappedCodePoint, pageOffset, 0);
		}

		private BitmapFont.Glyph copyGlyph(BitmapFont.Glyph source, int mappedCodePoint, int pageOffset, int yoffsetAdjust) {
			BitmapFont.Glyph glyph = new BitmapFont.Glyph();
			glyph.id = mappedCodePoint;
			glyph.srcX = source.srcX;
			glyph.srcY = source.srcY;
			glyph.width = source.width;
			glyph.height = source.height;
			glyph.xoffset = source.xoffset;
			glyph.yoffset = source.yoffset + yoffsetAdjust;
			glyph.xadvance = source.xadvance;
			glyph.page = source.page + pageOffset;
			return glyph;
		}

		private int getIntAttribute(String line, String name) {
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

		private int readRepresentativeGlyphYOffset(BufferedReader reader) throws java.io.IOException {
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
				if (isVerticalAnchorCodePoint(codePoint)) {
					anchorOffsets.put(codePoint, yoffset);
				}
			}

			int[] preferredCodePoints = { 'S', 'P', 'A', 'N', 'O', 'T', 'H', 'E', 'R', '[', ']', 'M', '0' };
			for (int codePoint : preferredCodePoints) {
				Integer yoffset = anchorOffsets.get(codePoint);
				if (yoffset != null) {
					return yoffset.intValue();
				}
			}
			return firstVisibleOffset != null ? firstVisibleOffset.intValue() : 0;
		}

		private int getRepresentativeGlyphYOffset(BitmapFont.BitmapFontData fontData) {
			int[] preferredCodePoints = { 'S', 'P', 'A', 'N', 'O', 'T', 'H', 'E', 'R', '[', ']', 'M', '0' };
			for (int codePoint : preferredCodePoints) {
				BitmapFont.Glyph glyph = fontData.getGlyph((char) codePoint);
				if (glyph != null && glyph.height > 0) {
					return -glyph.yoffset - glyph.height;
				}
			}
			return 0;
		}

		private boolean isVerticalAnchorCodePoint(int codePoint) {
			return codePoint == 'S' || codePoint == 'P' || codePoint == 'A' || codePoint == 'N' || codePoint == 'O'
					|| codePoint == 'T' || codePoint == 'H' || codePoint == 'E' || codePoint == 'R'
					|| codePoint == '[' || codePoint == ']' || codePoint == 'M' || codePoint == '0';
		}

		private static FallbackFont[] toFallbackFonts(Path[] fallbackFontPaths) {
			if (fallbackFontPaths == null) {
				return new FallbackFont[0];
			}
			FallbackFont[] fallbackFonts = new FallbackFont[fallbackFontPaths.length];
			for (int i = 0; i < fallbackFontPaths.length; i++) {
				fallbackFonts[i] = new FallbackFont(fallbackFontPaths[i], TYPE_STANDARD);
			}
			return fallbackFonts;
		}

		public float getOriginalSize() {
			return originalSize;
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		public float getPageWidth() {
			return pageWidth;
		}

		public float getPageHeight() {
			return pageHeight;
		}

		@Override
		public void dispose() {
			if (font != null) {
				font.dispose();
				font = null;
			}
		}

		private static final class RemappedFontFile {
			private final FileHandle fileHandle;
			private final Map<Integer, BitmapFont.Glyph> supplementaryGlyphs;

			private RemappedFontFile(FileHandle fileHandle, Map<Integer, BitmapFont.Glyph> supplementaryGlyphs) {
				this.fileHandle = fileHandle;
				this.supplementaryGlyphs = supplementaryGlyphs;
			}
		}

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

		public static final class FallbackFont {
			private final Path path;
			private final int type;

			public FallbackFont(Path path, int type) {
				this.path = path;
				this.type = type;
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj) {
					return true;
				}
				if (!(obj instanceof FallbackFont)) {
					return false;
				}
				FallbackFont other = (FallbackFont) obj;
				return type == other.type && path.equals(other.path);
			}

			@Override
			public int hashCode() {
				return 31 * path.hashCode() + type;
			}
		}
	}
}
