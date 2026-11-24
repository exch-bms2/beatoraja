package bms.player.beatoraja.skin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bms.player.beatoraja.skin.BitmapFontCache.CacheableBitmapFont;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

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
	private static final Pattern PAGE_ID_PATTERN = Pattern.compile(".*id=(\\d+)");
	private static final Pattern PAGE_FILE_PATTERN = Pattern.compile(".*file=\"?([^\"]+)\"?");
	private static final int FNT_READ_BUFFER_SIZE = 65536;
	private static final int PAGE_LOAD_PARALLELISM = Math.min(8, Math.max(1, Runtime.getRuntime().availableProcessors()));

	private static final class PageLoadExecutorHolder {
		static final ThreadPoolExecutor INSTANCE = createExecutor();

		private static ThreadPoolExecutor createExecutor() {
			AtomicInteger nextThreadId = new AtomicInteger(1);
			ThreadFactory factory = runnable -> {
				Thread thread = new Thread(runnable, "bitmap-font-page-loader-" + nextThreadId.getAndIncrement());
				thread.setDaemon(true);
				return thread;
			};
			ThreadPoolExecutor executor = new ThreadPoolExecutor(
					PAGE_LOAD_PARALLELISM, PAGE_LOAD_PARALLELISM,
					30L, TimeUnit.SECONDS,
					new LinkedBlockingQueue<>(),
					factory);
			executor.allowCoreThreadTimeOut(true);
			return executor;
		}
	}

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
			ParsedFontFile parsed = parseFontFile(fontFile);
			_fontData = parsed.fontData;
			_supplementaryGlyphs = parsed.supplementaryGlyphs;

			_regions = loadPageRegions(_fontData.imagePaths, usecim, useMipMaps);
			_font = new BitmapFont(_fontData, _regions, true);

			if (parsed.headerMetadataValid) {
				_originalSize = parsed.originalSize;
				_base = parsed.base;
				_pageWidth = parsed.pageWidth;
				_pageHeight = parsed.pageHeight;
				_glyphYOffset = parsed.glyphYOffset;
			} else {
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
			disposeRegions(_regions);
			_fontData = null;
			_regions = null;
			_font = null;
			_supplementaryGlyphs = Collections.emptyMap();
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

	/** .fnt を 1 パスでパースし、BitmapFontData・補助平面グリフ・ヘッダ寸法を返す。 */
	static ParsedFontFile parseFontFile(FileHandle fontFile) {
		BitmapFont.BitmapFontData data = new BitmapFont.BitmapFontData();
		data.fontFile = fontFile;
		data.flipped = false;
		data.name = fontFile.nameWithoutExtension();

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(fontFile.read(), StandardCharsets.UTF_8), FNT_READ_BUFFER_SIZE)) {
			FontHeader header = parseHeader(reader, data);
			parsePages(reader, fontFile, data, header.pageCount());

			int[] cursor = new int[1];
			GlyphSection glyphs = parseGlyphs(reader, data, header.baseLine(), cursor);
			String trailingLine = parseKernings(reader, data, glyphs.nextLine(), cursor);
			finalizeMetrics(data, header.baseLine(), trailingLine);

			return new ParsedFontFile(
					data,
					glyphs.supplementaryGlyphs(),
					header.metadataValid(),
					header.originalSize(),
					header.metadataValid() ? header.baseLine() : 0,
					header.pageWidth(),
					header.pageHeight(),
					glyphs.glyphYOffset());
		} catch (Exception ex) {
			throw new GdxRuntimeException("Error loading font file: " + fontFile, ex);
		}
	}

	private static FontHeader parseHeader(BufferedReader reader, BitmapFont.BitmapFontData data) throws IOException {
		String line = reader.readLine();
		if (line == null) {
			throw new GdxRuntimeException("File is empty.");
		}
		int padIndex = line.indexOf("padding=");
		if (padIndex < 0) {
			throw new GdxRuntimeException("Invalid padding.");
		}
		int padStart = padIndex + 8;
		int padEnd = line.indexOf(' ', padStart);
		String padSpec = padEnd >= 0 ? line.substring(padStart, padEnd) : line.substring(padStart);
		String[] padding = padSpec.split(",", 4);
		if (padding.length != 4) {
			throw new GdxRuntimeException("Invalid padding.");
		}
		data.padTop = Integer.parseInt(padding[0]);
		data.padRight = Integer.parseInt(padding[1]);
		data.padBottom = Integer.parseInt(padding[2]);
		data.padLeft = Integer.parseInt(padding[3]);

		boolean metadataValid = true;
		float originalSize = 0;
		float pageWidth = 0;
		float pageHeight = 0;
		try {
			originalSize = (float) parseRequiredIntAttribute(line, "size=");
		} catch (RuntimeException e) {
			metadataValid = false;
		}

		line = reader.readLine();
		if (line == null) {
			throw new GdxRuntimeException("Missing common header.");
		}
		String[] common = line.split(" ", 9);
		if (common.length < 3) {
			throw new GdxRuntimeException("Invalid common header.");
		}
		if (!common[1].startsWith("lineHeight=")) {
			throw new GdxRuntimeException("Missing: lineHeight");
		}
		data.lineHeight = Integer.parseInt(common[1].substring(11));
		if (!common[2].startsWith("base=")) {
			throw new GdxRuntimeException("Missing: base");
		}
		int baseLine = Integer.parseInt(common[2].substring(5));

		int pageCount = 1;
		if (common.length >= 6 && common[5] != null && common[5].startsWith("pages=")) {
			try {
				pageCount = Math.max(1, Integer.parseInt(common[5].substring(6)));
			} catch (NumberFormatException ignored) {
			}
		}

		if (metadataValid) {
			try {
				pageWidth = (float) parseRequiredIntAttribute(line, "scaleW=");
				pageHeight = (float) parseRequiredIntAttribute(line, "scaleH=");
			} catch (RuntimeException e) {
				metadataValid = false;
			}
		}
		return new FontHeader(baseLine, pageCount, metadataValid, originalSize, pageWidth, pageHeight);
	}

	private static void parsePages(BufferedReader reader, FileHandle fontFile, BitmapFont.BitmapFontData data,
			int pageCount) throws IOException {
		data.imagePaths = new String[pageCount];
		for (int p = 0; p < pageCount; p++) {
			String line = reader.readLine();
			if (line == null) {
				throw new GdxRuntimeException("Missing additional page definitions.");
			}
			Matcher idMatcher = PAGE_ID_PATTERN.matcher(line);
			if (idMatcher.find()) {
				String id = idMatcher.group(1);
				try {
					if (Integer.parseInt(id) != p) {
						throw new GdxRuntimeException("Page IDs must be indices starting at 0: " + id);
					}
				} catch (NumberFormatException ex) {
					throw new GdxRuntimeException("Invalid page id: " + id, ex);
				}
			}
			Matcher fileMatcher = PAGE_FILE_PATTERN.matcher(line);
			if (!fileMatcher.find()) {
				throw new GdxRuntimeException("Missing: file");
			}
			String fileName = fileMatcher.group(1);
			data.imagePaths[p] = fontFile.parent().child(fileName).path().replace('\\', '/');
		}
	}

	private static GlyphSection parseGlyphs(BufferedReader reader, BitmapFont.BitmapFontData data, float baseLine,
			int[] cursor) throws IOException {
		data.descent = 0;
		Map<Integer, BitmapFont.Glyph> supplementaryGlyphs = null;
		boolean hasFirstVisible = false;
		int firstVisibleOffset = 0;
		int bestAnchorPriority = VERTICAL_ANCHORS.length;
		int bestAnchorOffset = 0;

		String line;
		while (true) {
			line = reader.readLine();
			if (line == null || line.startsWith("kernings ") || line.startsWith("metrics ")) {
				break;
			}
			if (!line.startsWith("char ")) {
				continue;
			}
			int len = line.length();
			cursor[0] = skipToken(line, skipToken(line, 0, len), len);
			int ch = nextIntToken(line, cursor, len);
			if (ch > Character.MAX_VALUE) {
				if (ch <= Character.MAX_CODE_POINT) {
					BitmapFont.Glyph supGlyph = createGlyph(line, ch);
					if (supGlyph != null) {
						if (supplementaryGlyphs == null) {
							supplementaryGlyphs = new HashMap<>();
						}
						supplementaryGlyphs.put(ch, supGlyph);
						if (supGlyph.height > 0 && !hasFirstVisible) {
							hasFirstVisible = true;
							firstVisibleOffset = -supGlyph.yoffset - supGlyph.height;
						}
					}
				}
				continue;
			}

			Glyph glyph = new Glyph();
			if (ch <= 0) {
				data.missingGlyph = glyph;
			} else {
				data.setGlyph(ch, glyph);
			}
			glyph.id = ch;
			cursor[0] = skipToken(line, cursor[0], len);
			glyph.srcX = nextIntToken(line, cursor, len);
			cursor[0] = skipToken(line, cursor[0], len);
			glyph.srcY = nextIntToken(line, cursor, len);
			cursor[0] = skipToken(line, cursor[0], len);
			glyph.width = nextIntToken(line, cursor, len);
			cursor[0] = skipToken(line, cursor[0], len);
			glyph.height = nextIntToken(line, cursor, len);
			cursor[0] = skipToken(line, cursor[0], len);
			glyph.xoffset = nextIntToken(line, cursor, len);
			cursor[0] = skipToken(line, cursor[0], len);
			int rawYoffset = nextIntToken(line, cursor, len);
			glyph.yoffset = -(glyph.height + rawYoffset);
			cursor[0] = skipToken(line, cursor[0], len);
			glyph.xadvance = nextIntToken(line, cursor, len);

			int nextPos = skipDelimiters(line, cursor[0], len);
			if (nextPos < len) {
				cursor[0] = skipToken(line, nextPos, len);
				nextPos = skipDelimiters(line, cursor[0], len);
				if (nextPos < len) {
					try {
						glyph.page = nextIntToken(line, cursor, len);
					} catch (NumberFormatException ignored) {
					}
				}
			}

			if (glyph.width > 0 && glyph.height > 0) {
				data.descent = Math.min(baseLine + glyph.yoffset, data.descent);
			}
			if (glyph.height > 0) {
				if (!hasFirstVisible) {
					hasFirstVisible = true;
					firstVisibleOffset = rawYoffset;
				}
				int priority = verticalAnchorPriority(ch);
				if (priority >= 0 && priority <= bestAnchorPriority) {
					bestAnchorPriority = priority;
					bestAnchorOffset = rawYoffset;
				}
			}
		}
		data.descent += data.padBottom;
		int glyphYOffset = bestAnchorPriority < VERTICAL_ANCHORS.length
				? bestAnchorOffset
				: (hasFirstVisible ? firstVisibleOffset : 0);
		return new GlyphSection(
				supplementaryGlyphs != null ? supplementaryGlyphs : Collections.emptyMap(),
				glyphYOffset,
				line);
	}

	private static String parseKernings(BufferedReader reader, BitmapFont.BitmapFontData data, String line,
			int[] cursor) throws IOException {
		if (line == null || !line.startsWith("kernings ")) {
			return line;
		}
		while (true) {
			line = reader.readLine();
			if (line == null || !line.startsWith("kerning ")) {
				return line;
			}
			int len = line.length();
			cursor[0] = skipToken(line, skipToken(line, 0, len), len);
			int first = nextIntToken(line, cursor, len);
			cursor[0] = skipToken(line, cursor[0], len);
			int second = nextIntToken(line, cursor, len);
			if (first < 0 || first > Character.MAX_VALUE || second < 0 || second > Character.MAX_VALUE) {
				continue;
			}
			Glyph glyph = data.getGlyph((char) first);
			cursor[0] = skipToken(line, cursor[0], len);
			int amount = nextIntToken(line, cursor, len);
			if (glyph != null) {
				glyph.setKerning(second, amount);
			}
		}
	}

	private static void finalizeMetrics(BitmapFont.BitmapFontData data, float baseLine, String metricsLine) {
		float padY = data.padTop + data.padBottom;

		Glyph spaceGlyph = data.getGlyph(' ');
		if (spaceGlyph == null) {
			spaceGlyph = new Glyph();
			spaceGlyph.id = ' ';
			Glyph xadvanceGlyph = data.getGlyph('l');
			if (xadvanceGlyph == null) {
				xadvanceGlyph = data.getFirstGlyph();
			}
			spaceGlyph.xadvance = xadvanceGlyph.xadvance;
			data.setGlyph(' ', spaceGlyph);
		}
		if (spaceGlyph.width == 0) {
			spaceGlyph.width = (int) (data.padLeft + spaceGlyph.xadvance + data.padRight);
			spaceGlyph.xoffset = (int) (-data.padLeft);
		}
		data.spaceXadvance = spaceGlyph.xadvance;

		Glyph xGlyph = null;
		for (char xChar : data.xChars) {
			xGlyph = data.getGlyph(xChar);
			if (xGlyph != null) {
				break;
			}
		}
		if (xGlyph == null) {
			xGlyph = data.getFirstGlyph();
		}
		data.xHeight = xGlyph.height - padY;

		Glyph capGlyph = null;
		for (char capChar : data.capChars) {
			capGlyph = data.getGlyph(capChar);
			if (capGlyph != null) {
				break;
			}
		}
		if (capGlyph == null) {
			for (Glyph[] page : data.glyphs) {
				if (page == null) {
					continue;
				}
				for (Glyph glyph : page) {
					if (glyph == null || glyph.height == 0 || glyph.width == 0) {
						continue;
					}
					data.capHeight = Math.max(data.capHeight, glyph.height);
				}
			}
		} else {
			data.capHeight = capGlyph.height;
		}
		data.capHeight -= padY;

		data.ascent = baseLine - data.capHeight;
		data.down = -data.lineHeight;

		if (metricsLine != null && metricsLine.startsWith("metrics ")) {
			applyMetricsOverride(data, metricsLine);
		}
	}

	private static void applyMetricsOverride(BitmapFont.BitmapFontData data, String line) {
		StringTokenizer metricsTokens = new StringTokenizer(line, " =");
		metricsTokens.nextToken();
		metricsTokens.nextToken();
		data.ascent = Float.parseFloat(metricsTokens.nextToken());
		metricsTokens.nextToken();
		data.descent = Float.parseFloat(metricsTokens.nextToken());
		metricsTokens.nextToken();
		data.down = Float.parseFloat(metricsTokens.nextToken());
		metricsTokens.nextToken();
		data.capHeight = Float.parseFloat(metricsTokens.nextToken());
		metricsTokens.nextToken();
		data.lineHeight = Float.parseFloat(metricsTokens.nextToken());
		metricsTokens.nextToken();
		data.spaceXadvance = Float.parseFloat(metricsTokens.nextToken());
		metricsTokens.nextToken();
		data.xHeight = Float.parseFloat(metricsTokens.nextToken());
	}

	private static Array<TextureRegion> loadPageRegions(String[] imagePaths, boolean usecim, boolean useMipMaps)
			throws IOException {
		int pageCount = imagePaths != null ? imagePaths.length : 0;
		if (pageCount == 0) {
			return new Array<>(0);
		}
		SkinLoader.PixmapLoadContext context = SkinLoader.capturePixmapLoadContext(usecim);
		if (pageCount == 1 || PAGE_LOAD_PARALLELISM <= 1) {
			return loadPageRegionsSerial(context, imagePaths, useMipMaps);
		}

		List<Integer> mainThreadIndices = new ArrayList<>();
		Map<String, List<Integer>> asyncPathToIndices = new LinkedHashMap<>();
		for (int i = 0; i < pageCount; i++) {
			String path = imagePaths[i];
			// WebP デコードは GL_MAX_TEXTURE_SIZE を参照するためメインスレッドで行う
			if (context.resource.exists(path) || path.toLowerCase(Locale.ROOT).endsWith(".webp")) {
				mainThreadIndices.add(i);
			} else {
				asyncPathToIndices.computeIfAbsent(path, ignored -> new ArrayList<>(1)).add(i);
			}
		}

		if (asyncPathToIndices.size() <= 1) {
			return loadPageRegionsSerial(context, imagePaths, useMipMaps);
		}

		TextureRegion[] regionArray = new TextureRegion[pageCount];
		ExecutorCompletionService<DecodedPage> completionService =
				new ExecutorCompletionService<>(PageLoadExecutorHolder.INSTANCE);
		List<Future<DecodedPage>> submitted = new ArrayList<>(asyncPathToIndices.size());
		AtomicBoolean aborted = new AtomicBoolean(false);
		boolean completedAll = false;
		try {
			for (String path : asyncPathToIndices.keySet()) {
				submitted.add(completionService.submit(() -> {
					if (aborted.get()) {
						return null;
					}
					return new DecodedPage(path, SkinLoader.loadPixmap(context, path));
				}));
			}
			for (int i = 0; i < mainThreadIndices.size(); i++) {
				int idx = mainThreadIndices.get(i);
				String path = imagePaths[idx];
				Pixmap pixmap = SkinLoader.loadPixmap(context, path);
				if (pixmap == null) {
					throw new IOException("Could not load bitmap font page: " + path);
				}
				regionArray[idx] = new TextureRegion(new Texture(pixmap, useMipMaps));
			}
			for (int c = 0; c < submitted.size(); c++) {
				DecodedPage decoded = completionService.take().get();
				if (decoded == null || decoded.pixmap == null) {
					throw new IOException("Could not load bitmap font page: " + (decoded != null ? decoded.path : ""));
				}
				List<Integer> indices = asyncPathToIndices.get(decoded.path);
				for (int i = 0; i < indices.size(); i++) {
					regionArray[indices.get(i)] = new TextureRegion(new Texture(decoded.pixmap, useMipMaps));
				}
			}
			Array<TextureRegion> regions = new Array<>(pageCount);
			for (int i = 0; i < pageCount; i++) {
				regions.add(regionArray[i]);
			}
			completedAll = true;
			return regions;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while loading bitmap font pages", e);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof IOException io) {
				throw io;
			}
			if (cause instanceof RuntimeException re) {
				throw re;
			}
			if (cause instanceof Error err) {
				throw err;
			}
			throw new IOException("Failed to load bitmap font page", cause);
		} finally {
			if (!completedAll) {
				aborted.set(true);
				awaitPendingTasks(submitted);
				disposeRegions(regionArray);
			}
		}
	}

	private static Array<TextureRegion> loadPageRegionsSerial(
			SkinLoader.PixmapLoadContext context, String[] imagePaths, boolean useMipMaps) throws IOException {
		Array<TextureRegion> regions = new Array<>(imagePaths.length);
		try {
			for (int i = 0; i < imagePaths.length; i++) {
				String path = imagePaths[i];
				Pixmap pixmap = SkinLoader.loadPixmap(context, path);
				if (pixmap == null) {
					throw new IOException("Could not load bitmap font page: " + path);
				}
				regions.add(new TextureRegion(new Texture(pixmap, useMipMaps)));
			}
			return regions;
		} catch (Throwable t) {
			disposeRegions(regions);
			throw t;
		}
	}

	private static void awaitPendingTasks(List<Future<DecodedPage>> submitted) {
		boolean interrupted = false;
		try {
			for (int i = 0; i < submitted.size(); i++) {
				Future<DecodedPage> future = submitted.get(i);
				while (true) {
					try {
						future.get();
						break;
					} catch (InterruptedException e) {
						interrupted = true;
					} catch (ExecutionException ignored) {
						break;
					}
				}
			}
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private static void disposeRegions(Array<TextureRegion> regions) {
		if (regions == null) {
			return;
		}
		for (int i = 0; i < regions.size; i++) {
			disposeRegion(regions.get(i));
		}
	}

	private static void disposeRegions(TextureRegion[] regionArray) {
		if (regionArray == null) {
			return;
		}
		for (int i = 0; i < regionArray.length; i++) {
			disposeRegion(regionArray[i]);
		}
	}

	private static void disposeRegion(TextureRegion region) {
		if (region != null && region.getTexture() != null) {
			try {
				region.getTexture().dispose();
			} catch (Throwable ignored) {
			}
		}
	}

	private static BitmapFont.Glyph createGlyph(String line, int codePoint) {
		try {
			BitmapFont.Glyph glyph = new BitmapFont.Glyph();
			glyph.id = codePoint;
			glyph.srcX = getIntAttribute(line, "x=");
			glyph.srcY = getIntAttribute(line, "y=");
			glyph.width = getIntAttribute(line, "width=");
			glyph.height = getIntAttribute(line, "height=");
			glyph.xoffset = getIntAttribute(line, "xoffset=");
			glyph.yoffset = -glyph.height - getIntAttribute(line, "yoffset=");
			glyph.xadvance = getIntAttribute(line, "xadvance=");
			glyph.page = getIntAttribute(line, "page=");
			return glyph;
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static int getIntAttribute(String line, String prefix) {
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
		return Integer.parseInt(line, valueStart, valueEnd, 10);
	}

	private static int parseRequiredIntAttribute(String line, String prefix) {
		int idx = line.indexOf(prefix);
		if (idx < 0) {
			throw new NumberFormatException("Missing attribute: " + prefix);
		}
		int valueStart = idx + prefix.length();
		int valueEnd = valueStart;
		if (valueEnd < line.length() && line.charAt(valueEnd) == '-') {
			valueEnd++;
		}
		int digitsStart = valueEnd;
		while (valueEnd < line.length() && Character.isDigit(line.charAt(valueEnd))) {
			valueEnd++;
		}
		if (valueEnd == digitsStart || (valueEnd < line.length() && line.charAt(valueEnd) != ' ')) {
			throw new NumberFormatException("Invalid integer attribute: " + prefix);
		}
		return Integer.parseInt(line, valueStart, valueEnd, 10);
	}

	private static int skipDelimiters(String line, int pos, int len) {
		while (pos < len) {
			char c = line.charAt(pos);
			if (c != ' ' && c != '=') {
				break;
			}
			pos++;
		}
		return pos;
	}

	private static int skipToken(String line, int pos, int len) {
		pos = skipDelimiters(line, pos, len);
		if (pos >= len) {
			throw new NoSuchElementException();
		}
		while (pos < len) {
			char c = line.charAt(pos);
			if (c == ' ' || c == '=') {
				break;
			}
			pos++;
		}
		return pos;
	}

	private static int nextIntToken(String line, int[] cursor, int len) {
		int start = skipDelimiters(line, cursor[0], len);
		if (start >= len) {
			throw new NoSuchElementException();
		}
		int end = start;
		while (end < len) {
			char c = line.charAt(end);
			if (c == ' ' || c == '=') {
				break;
			}
			end++;
		}
		cursor[0] = end;
		return Integer.parseInt(line, start, end, 10);
	}

	private static int verticalAnchorPriority(int codePoint) {
		for (int i = 0; i < VERTICAL_ANCHORS.length; i++) {
			if (VERTICAL_ANCHORS[i] == codePoint) {
				return i;
			}
		}
		return -1;
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

	private record FontHeader(int baseLine, int pageCount, boolean metadataValid, float originalSize, float pageWidth,
			float pageHeight) {
	}

	private record GlyphSection(Map<Integer, BitmapFont.Glyph> supplementaryGlyphs, int glyphYOffset, String nextLine) {
	}

	static final class ParsedFontFile {
		final BitmapFont.BitmapFontData fontData;
		final Map<Integer, BitmapFont.Glyph> supplementaryGlyphs;
		final boolean headerMetadataValid;
		final float originalSize;
		final int base;
		final float pageWidth;
		final float pageHeight;
		final int glyphYOffset;

		private ParsedFontFile(BitmapFont.BitmapFontData fontData, Map<Integer, BitmapFont.Glyph> supplementaryGlyphs,
				boolean headerMetadataValid, float originalSize, int base, float pageWidth, float pageHeight,
				int glyphYOffset) {
			this.fontData = fontData;
			this.supplementaryGlyphs = supplementaryGlyphs;
			this.headerMetadataValid = headerMetadataValid;
			this.originalSize = originalSize;
			this.base = base;
			this.pageWidth = pageWidth;
			this.pageHeight = pageHeight;
			this.glyphYOffset = glyphYOffset;
		}
	}

	private static final class DecodedPage {
		final String path;
		final Pixmap pixmap;

		private DecodedPage(String path, Pixmap pixmap) {
			this.path = path;
			this.pixmap = pixmap;
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

		private static void copyHeaderFields(BitmapFont.BitmapFontData src, BitmapFont.BitmapFontData dst) {
			dst.name = src.name;
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
		}

		/** グリフ表を読み取り専用で共有するビューを作る。setGlyph 時のみページ単位で複製する。 */
		static BitmapFont.BitmapFontData shareGlyphs(BitmapFont.BitmapFontData src) {
			if (src == null) {
				return null;
			}
			BitmapFont.BitmapFontData dst = new BitmapFont.BitmapFontData() {
				private final boolean[] ownedPages = new boolean[glyphs.length];

				@Override
				public void setGlyphRegion(Glyph glyph, TextureRegion region) {
					if (glyph == null || region == null || region.getTexture() == null) {
						return;
					}
					super.setGlyphRegion(glyph, region);
				}

				@Override
				public void setGlyph(int ch, Glyph glyph) {
					int pageIdx = ch / 512;
					if (pageIdx >= 0 && pageIdx < glyphs.length && !ownedPages[pageIdx]) {
						if (glyphs[pageIdx] != null) {
							glyphs[pageIdx] = glyphs[pageIdx].clone();
						}
						ownedPages[pageIdx] = true;
					}
					super.setGlyph(ch, glyph);
				}
			};
			copyHeaderFields(src, dst);
			if (src.glyphs != null) {
				System.arraycopy(src.glyphs, 0, dst.glyphs, 0, Math.min(dst.glyphs.length, src.glyphs.length));
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
			copyHeaderFields(src, dst);

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
