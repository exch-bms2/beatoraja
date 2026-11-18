package bms.player.beatoraja.skin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.lang.Character.UnicodeScript;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * スキン由来ではない代替フォント(共通代替フォント)を供給し、文字ごとにどれを使うかを決める。
 *
 * <p>最終的な一覧はスキン自身の代替フォント、続いて共通代替フォントの順。共通代替フォントは {@value #DIRECTORY} フォルダー内の
 * フォントファイル、次にシステムプロパティ {@value #PROPERTY}({@code ;} 区切りのパス)から取る。フォルダーは作業ディレクトリ基準。
 * ここではファイルを読まず、見つけた順(フォルダー内はパス順、続いてプロパティ順)のパスだけを返す。ファイル名で順位は付けない。</p>
 *
 * <p>共通代替フォントは起動時に一度だけ読み、その後は読み直しも削除も変更もしない。そこで並び({@link #preferLocale})と文字ごとの
 * 選択({@link GlobalChoice})も起動時に一度だけ作り、変えずに使う。ロケールとの照合は正規 BCP 47 で行い({@link #canonicalBcp47(String)}、
 * {@link #localeMatchScore})、根拠は meta.dlng・meta.slng、なければ cmap の文字体系で、name 表の言語やロケールの表示名は使わない。</p>
 *
 * Supplies fallback fonts that do not come from a skin (global fallbacks) and decides which one draws each character.
 *
 * <p>The final list is the skin's own fallbacks followed by the global fallbacks. Global fallbacks come from
 * the font files in the {@value #DIRECTORY} folder and then from the system property {@value #PROPERTY}
 * (paths separated by {@code ;}). The folder is relative to the working directory. This class reads no font file
 * and returns the paths in the order they were found; file names never rank fonts.</p>
 *
 * <p>Global fallbacks are read once at startup and are never re-read, removed or changed, so their order
 * ({@link #preferLocale}) and the per-character choice ({@link GlobalChoice}) are also built once at startup and used
 * as is. Locale matching is done in canonical BCP 47 ({@link #canonicalBcp47(String)}, {@link #localeMatchScore}) with
 * meta.dlng and meta.slng as evidence, or the cmap's scripts when the font has no meta languages; name table
 * languages and locale display names are not used.</p>
 */
public final class FallbackFontPolicy {
	public static final String PROPERTY = "beatoraja.fallbackFonts";
	public static final String DIRECTORY = "font";
	/**
	 * 可変の共通代替フォントを開く太さ(OpenType の wght、500 = Medium、調整用)。最も近い名前付きインスタンスを使う。
	 * 既定インスタンスが極細(100)の可変フォントでも、本文として読める太さで描く。
	 * Weight (OpenType wght, 500 = Medium; tuning knob) at which variable global fallbacks are opened, using the
	 * nearest named instance, so a variable font whose default instance is Thin (100) still draws readable text.
	 */
	static final int GLOBAL_WEIGHT = 500;

	private static final Logger LOGGER = Logger.getGlobal();

	private static List<String> globalFallbacks;

	private FallbackFontPolicy() {
	}

	/**
	 * 共通代替フォントの文字ごとの選択。起動時に一度だけ作り、その後は変えない。
	 *
	 * <p>強い順の一覧を受け取り、弱いフォントから順に cmap の各区間を {@link #overwrite} で書き込むので、最後に書いた(強い)フォントが
	 * その文字を受け持つ。基本ラテン文字のない絵文字・記号フォントは一覧の最後(最も弱い)なので、ほかのどのフォントにもない文字だけが残る。
	 * 結果は開始・終了・所有者の配列に固め、二分探索で引く。{@link #candidates} は所有者の後に、強い順でその文字を持ちうる残りの
	 * フォント(収録範囲が読めないフォントを含む)を返し、FreeType での生成に失敗したときの次の候補にする。</p>
	 *
	 * The per-character choice among global fallbacks, built once at startup and never changed.
	 *
	 * <p>It takes the fonts strongest first and writes each cmap range with {@link #overwrite} from the weakest font
	 * up, so the last (strongest) writer owns a character. Emoji and symbol fonts without basic Latin letters are last
	 * (weakest), so they keep only characters no other font has. The result is frozen into start, end and owner arrays
	 * and looked up by binary search. {@link #candidates} returns the owner, then the other fonts that may hold the
	 * character strongest first (fonts with unreadable coverage included), as next choices when FreeType fails.</p>
	 */
	static final class GlobalChoice<T> {
		/** 書き込み中の区間。所有者は強い順の一覧での番号。 / A range while building; the owner is its index in the strongest-first list. */
		record Segment(int start, int end, int owner) {
		}

		private final List<T> fonts;
		private final List<FontCoverage> coverages;
		private final int[] starts;
		private final int[] ends;
		private final int[] owners;

		private GlobalChoice(List<T> fonts, List<FontCoverage> coverages, List<Segment> segments) {
			this.fonts = List.copyOf(fonts);
			this.coverages = List.copyOf(coverages);
			this.starts = new int[segments.size()];
			this.ends = new int[segments.size()];
			this.owners = new int[segments.size()];
			for (int i = 0; i < segments.size(); i++) {
				starts[i] = segments.get(i).start();
				ends[i] = segments.get(i).end();
				owners[i] = segments.get(i).owner();
			}
		}

		/** 強い順の一覧から作る。 / Builds from the fonts, strongest first. */
		static <T> GlobalChoice<T> build(List<T> strongestFirst, Function<T, FontCoverage> coverage) {
			var coverages = new ArrayList<FontCoverage>(strongestFirst.size());
			for (T font : strongestFirst) {
				FontCoverage c = coverage.apply(font);
				coverages.add(c != null ? c : FontCoverage.UNKNOWN);
			}
			var segments = new ArrayList<Segment>();
			for (int owner = strongestFirst.size() - 1; owner >= 0; owner--) {
				FontCoverage c = coverages.get(owner);
				if (c.state() == FontCoverage.State.READY) {
					int id = owner;
					c.forEachRange((start, end) -> overwrite(segments, start, end, id));
				}
			}
			return new GlobalChoice<>(strongestFirst, coverages, segments);
		}

		/**
		 * [start, end] を owner で上書きする。重なった区間は前後の残りだけ残し、隣り合う同じ所有者の区間はまとめる。一覧は開始順のまま。
		 * Overwrites [start, end] with owner: overlapped segments keep only their parts outside the range, neighbours with
		 * the same owner are joined, and the list stays sorted by start.
		 */
		static void overwrite(List<Segment> segments, int start, int end, int owner) {
			int low = 0, high = segments.size();
			while (low < high) { // end >= start となる最初の区間 / first segment with end >= start
				int middle = (low + high) >>> 1;
				if (segments.get(middle).end() < start) low = middle + 1;
				else high = middle;
			}
			int first = low, last = low;
			var replacement = new ArrayList<Segment>(3);
			Segment right = null;
			while (last < segments.size() && segments.get(last).start() <= end) {
				Segment overlapped = segments.get(last++);
				if (overlapped.start() < start) replacement.add(new Segment(overlapped.start(), start - 1, overlapped.owner()));
				if (overlapped.end() > end) right = new Segment(end + 1, overlapped.end(), overlapped.owner());
			}
			replacement.add(new Segment(start, end, owner));
			if (right != null) replacement.add(right);
			segments.subList(first, last).clear();
			segments.addAll(first, replacement);
			// 新しい区間の前後だけを見て、隣り合う同じ所有者をまとめる / join same-owner neighbours around the new range
			int at = first + replacement.size() - (right != null ? 2 : 1);
			if (at + 1 < segments.size() && joins(segments.get(at), segments.get(at + 1))) {
				segments.set(at, new Segment(segments.get(at).start(), segments.get(at + 1).end(), owner));
				segments.remove(at + 1);
			}
			if (at > 0 && joins(segments.get(at - 1), segments.get(at))) {
				segments.set(at - 1, new Segment(segments.get(at - 1).start(), segments.get(at).end(), owner));
				segments.remove(at);
			}
		}

		private static boolean joins(Segment left, Segment right) {
			return left.owner() == right.owner() && left.end() + 1 == right.start();
		}

		/** 強い順の一覧。 / The fonts, strongest first. */
		List<T> fonts() {
			return fonts;
		}

		/** その文字を受け持つフォント。どのフォントの収録範囲にもなければ null。 / The font owning the character, or null when no coverage holds it. */
		T owner(int codePoint) {
			int low = 0, high = starts.length - 1;
			while (low <= high) {
				int middle = (low + high) >>> 1;
				if (codePoint < starts[middle]) high = middle - 1;
				else if (codePoint > ends[middle]) low = middle + 1;
				else return fonts.get(owners[middle]);
			}
			return null;
		}

		/** 所有者、続いて強い順にその文字を持ちうる残りのフォント。 / The owner, then the other fonts that may hold the character, strongest first. */
		List<T> candidates(int codePoint) {
			T owner = owner(codePoint);
			var result = new ArrayList<T>(4);
			if (owner != null) result.add(owner);
			for (int i = 0; i < fonts.size(); i++) {
				T font = fonts.get(i);
				if (font != owner && coverages.get(i).mayContain(codePoint)) result.add(font);
			}
			return result;
		}

		/** 区間の数(試験用)。 / Number of segments (tests). */
		int segmentCount() {
			return starts.length;
		}
	}

	/**
	 * スキンの代替パスに共通代替パスを続けて返す。null は返さず、引数も変更しない。
	 *
	 * <p>スキン側の項目は null・重複・不正な値も含めて与えられた順のまま残し、扱いは従来どおりフォント読み込み側に任せる。
	 * 重複を除くのは共通側の項目だけで、共通項目同士と、有効なパスであるスキン項目との重複を除く。</p>
	 *
	 * Returns the skin's fallback paths followed by the global fallback paths. Never returns null. The argument is
	 * not modified.
	 *
	 * <p>Skin entries are kept exactly as given, in order, including null, repeated and malformed entries, so the
	 * font loading stage decides what to do with them as it did before global fallbacks existed. Only the global
	 * entries are de-duplicated: against each other and against every skin entry that is a valid path.</p>
	 */
	public static String[] resolve(String[] skinFallbacks) {
		return merge(skinFallbacks, getGlobalFallbacks(), null);
	}

	/**
	 * {@link #resolve(String[])} と同じだが、スキンの主フォント {@code fontPath} と同じファイルの共通項目も除き、同じファイルを二度開かない。
	 * Same as {@link #resolve(String[])}, and additionally leaves out a global entry that is the same file as
	 * {@code fontPath}, the skin's main font, so that file is not opened twice.
	 */
	public static String[] resolve(String fontPath, String[] skinFallbacks) {
		return merge(skinFallbacks, getGlobalFallbacks(), fontPath);
	}

	static String[] merge(String[] skinFallbacks, List<String> globals) {
		return merge(skinFallbacks, globals, null);
	}

	static String[] merge(String[] skinFallbacks, List<String> globals, String mainFontPath) {
		if (globals.isEmpty()) {
			return skinFallbacks == null ? new String[0] : skinFallbacks;
		}
		var merged = new ArrayList<String>();
		var seen = new LinkedHashSet<Path>();
		var mainNormalized = tryNormalize(mainFontPath);
		if (mainNormalized != null) {
			seen.add(mainNormalized);
		}
		if (skinFallbacks != null) {
			for (var path : skinFallbacks) {
				var normalized = tryNormalize(path);
				if (normalized != null) {
					seen.add(normalized);
				}
				merged.add(path);
			}
		}
		for (var path : globals) {
			var normalized = tryNormalize(path);
			if (normalized == null || seen.add(normalized)) {
				merged.add(path);
			}
		}
		return merged.toArray(new String[0]);
	}

	private static synchronized List<String> getGlobalFallbacks() {
		if (globalFallbacks == null) {
			globalFallbacks = collect(System.getProperty(PROPERTY), Path.of(DIRECTORY));
		}
		return globalFallbacks;
	}

	/** キャッシュした共通一覧を捨てる。試験専用。 / Forgets the cached global list. Test use only. */
	static synchronized void reset() {
		globalFallbacks = null;
	}

	/**
	 * フォルダー内のフォント(パス順)、続いてプロパティのフォントを集める。ファイルの中身は読まない。
	 * Collects folder fonts (in path order) then property fonts. File contents are not read.
	 */
	static List<String> collect(String propertyValue, Path... directories) {
		Set<Path> seen = new LinkedHashSet<>();
		var found = new ArrayList<String>();
		if (directories != null) {
			for (Path directory : directories) {
				if (directory != null && Files.isDirectory(directory)) {
					try (Stream<Path> files = Files.list(directory)) {
						var fonts = files.filter(Files::isRegularFile)
								.filter(p -> isFontFile(p.getFileName().toString()))
								.map(Path::toString)
								.sorted()
								.toList();
						if (fonts.isEmpty()) {
							LOGGER.info("Global fallback font folder has no .ttf/.otf/.ttc files: " + directory);
						}
						fonts.forEach(p -> addIfUsable(p, seen, found));
					} catch (IOException e) {
						LOGGER.warning("Global fallback font folder read failed: " + directory + " - " + e.getMessage());
					}
				}
			}
		}
		if (propertyValue != null) {
			for (var entry : propertyValue.split(";")) {
				addIfUsable(entry.trim(), seen, found);
			}
		}
		return List.copyOf(found);
	}

	/** 言語(と地域)に既定の文字体系。CLDR likelySubtags の一部で、一行足せば広げられる。 / Default script per language (and region); a CLDR likelySubtags subset, extended one line at a time. */
	private static final Map<String, String> LIKELY_SCRIPTS = Map.ofEntries(
			Map.entry("ja", "Jpan"), Map.entry("ko", "Kore"), Map.entry("zh", "Hans"), Map.entry("zh-TW", "Hant"),
			Map.entry("zh-HK", "Hant"), Map.entry("zh-MO", "Hant"), Map.entry("th", "Thai"), Map.entry("en", "Latn"),
			Map.entry("de", "Latn"), Map.entry("es", "Latn"), Map.entry("fr", "Latn"), Map.entry("it", "Latn"),
			Map.entry("pt", "Latn"), Map.entry("vi", "Latn"), Map.entry("ru", "Cyrl"), Map.entry("uk", "Cyrl"));
	/**
	 * 一つの言語だけが使う文字体系とその言語(und-Jpan → ja-Jpan)。Latn・Hani など複数の言語が使う文字体系は載せず、言語を作り出さない
	 * (Hani だけは {@link #UNDETERMINED_HAN_LANGUAGE} の慣習で補う)。
	 * Scripts used by a single language, with that language (und-Jpan becomes ja-Jpan). Scripts shared by several
	 * languages (Latn, Hani, ...) are not listed, so no language is invented for them (Hani alone gets
	 * {@link #UNDETERMINED_HAN_LANGUAGE} by convention).
	 */
	private static final Map<String, String> SINGLE_LANGUAGE_SCRIPTS = Map.of(
			"Jpan", "ja", "Hira", "ja", "Kana", "ja", "Kore", "ko", "Hang", "ko", "Hans", "zh", "Hant", "zh", "Thai", "th");
	/** 合成文字体系(ISO 15924)の構成。 / Parts of the ISO 15924 composite scripts. */
	private static final Map<String, Set<String>> SCRIPT_PARTS = Map.of(
			"Jpan", Set.of("Hani", "Hira", "Kana"), "Kore", Set.of("Hang", "Hani"), "Hans", Set.of("Hani"), "Hant", Set.of("Hani"));
	/**
	 * cmap だけで判断するとき、その文字体系を他から分ける Unicode 文字体系。漢字の収録数は字形セットごとに大きく違う(同じ地域向けでも
	 * BMP の 3 割から 10 割)ので、合成文字体系は漢字以外の構成で見る。載っていなければ同名の Unicode 文字体系を使う。
	 * The Unicode scripts that set a script apart when only the cmap is available. Han coverage varies widely between
	 * glyph sets (from 30% to 100% of BMP Han), so composite scripts are judged by their non-Han parts. Scripts not listed
	 * use the Unicode script of the same name.
	 */
	private static final Map<String, Set<UnicodeScript>> CMAP_EVIDENCE = Map.of(
			"Jpan", Set.of(UnicodeScript.HIRAGANA, UnicodeScript.KATAKANA), "Kore", Set.of(UnicodeScript.HANGUL),
			"Hans", Set.of(UnicodeScript.HAN), "Hant", Set.of(UnicodeScript.HAN));
	/** 根拠ごとの重み: meta.dlng > meta.slng > cmap。 / Weight per evidence: meta.dlng over meta.slng over cmap. */
	private static final int SCORE_DLNG = 100, SCORE_SLNG = 10, SCORE_CMAP = 1;
	private static final String LATIN = "Latn", UNDETERMINED = "und";
	/**
	 * 言語のない漢字(und-Hani)に補う言語。BMS の文字列は慣習的に MS932(日本語)なので、言語が分からない漢字は日本語とみなす。
	 * The language given to Han without a language (und-Hani): BMS text is conventionally MS932 (Japanese), so Han of unknown
	 * language is taken as Japanese.
	 */
	private static final String HAN = "Hani", UNDETERMINED_HAN_LANGUAGE = "ja";

	/**
	 * 言語タグ(BCP 47、または言語を省いた OpenType ScriptLangTag)を language-Script-REGION の正規 BCP 47 にする。
	 * 文字体系だけのタグには und- を付け、一つの言語だけが使う文字体系なら言語を補い、文字体系のない言語には既定の文字体系を補う
	 * (ko-KR → ko-Kore-KR、Kore → ko-Kore、Latn → und-Latn、zh-TW → zh-Hant-TW)。表にないものはそのまま。不正なら null。
	 * Normalizes a language tag (BCP 47, or an OpenType ScriptLangTag without a language) to canonical
	 * language-Script-REGION BCP 47: a script-only tag gains und-, a single-language script gains its language, and a
	 * language without a script gains its default script (ko-KR to ko-Kore-KR, Kore to ko-Kore, Latn to und-Latn,
	 * zh-TW to zh-Hant-TW). Tags not in the tables keep their parts. Null when malformed.
	 */
	static String canonicalBcp47(String rawTag) {
		if (rawTag == null) {
			return null;
		}
		String tag = rawTag.trim().replace('_', '-');
		if (tag.isEmpty() || !tag.chars().allMatch(c -> c > ' ' && c < 0x7f)) {
			return null;
		}
		int dash = tag.indexOf('-');
		String first = dash < 0 ? tag : tag.substring(0, dash);
		if (first.length() == 4 && first.chars().allMatch(Character::isLetter)) {
			tag = UNDETERMINED + "-" + tag;
		}
		Locale parsed = Locale.forLanguageTag(tag);
		// Java は und を空の言語として返す。 / Java reports und as an empty language.
		String language = parsed.getLanguage(), script = parsed.getScript(), region = parsed.getCountry();
		if (language.isEmpty() && script.isEmpty()) {
			return null;
		}
		if (language.isEmpty()) {
			language = HAN.equals(script) ? UNDETERMINED_HAN_LANGUAGE : SINGLE_LANGUAGE_SCRIPTS.getOrDefault(script, "");
		}
		if (script.isEmpty()) {
			script = LIKELY_SCRIPTS.getOrDefault(language + "-" + region, LIKELY_SCRIPTS.getOrDefault(language, ""));
		}
		return new Locale.Builder().setLanguage(language).setScript(script).setRegion(region).build().toLanguageTag();
	}

	/** ロケールの正規 BCP 47。 / Canonical BCP 47 of a locale. */
	static String canonicalBcp47(Locale locale) {
		return locale != null ? canonicalBcp47(locale.toLanguageTag()) : null;
	}

	/**
	 * 正規化した二つのタグを (language, script, region) で比べ、一致度を 0(不一致)〜4 で返す。und(言語なし)はどの言語とも合い、
	 * 文字体系は同じか合成文字体系の構成なら合い、地域は両方にあるときだけ比べる(ko-KR と ko-Kore、ja-JP と und-Hira、zh-TW と zh-Hant は合い、
	 * zh-TW と zh-Hans、ko-KR と und-Latn は合わない)。
	 * Compares two canonical tags by (language, script, region); 0 means no match, up to 4. und (no language) matches any
	 * language, scripts match when equal or one is a part of the other composite, and regions are compared only when
	 * both have one (ko-KR matches ko-Kore, ja-JP matches und-Hira and zh-TW matches zh-Hant, while zh-TW does not match
	 * zh-Hans and ko-KR does not match und-Latn).
	 */
	static int tagMatchScore(String fontTag, String localeTag) {
		if (fontTag == null || localeTag == null) {
			return 0;
		}
		Locale font = Locale.forLanguageTag(fontTag), wanted = Locale.forLanguageTag(localeTag);
		if (wanted.getLanguage().isEmpty()) {
			return 0;
		}
		boolean anyLanguage = font.getLanguage().isEmpty();
		if (!anyLanguage && !font.getLanguage().equals(wanted.getLanguage())) {
			return 0;
		}
		if (!scriptsAgree(font.getScript(), wanted.getScript())) {
			return 0;
		}
		if (!font.getCountry().isEmpty() && !wanted.getCountry().isEmpty() && !font.getCountry().equals(wanted.getCountry())) {
			return 0;
		}
		int score = 1;
		if (!anyLanguage) {
			score++;
		}
		if (!font.getScript().isEmpty() && font.getScript().equals(wanted.getScript())) {
			score++;
		}
		if (!font.getCountry().isEmpty() && font.getCountry().equals(wanted.getCountry())) {
			score++;
		}
		return score;
	}

	private static boolean scriptsAgree(String a, String b) {
		return a.isEmpty() || b.isEmpty() || a.equals(b) || SCRIPT_PARTS.getOrDefault(a, Set.of()).contains(b)
				|| SCRIPT_PARTS.getOrDefault(b, Set.of()).contains(a);
	}

	private static int bestTagMatchScore(Set<String> fontTags, String localeTag) {
		int best = 0;
		for (String tag : fontTags) {
			best = Math.max(best, tagMatchScore(tag, localeTag));
		}
		return best;
	}

	/**
	 * フォントとロケールの適合度(0 = 不適合)。根拠は meta.dlng(100〜400)> meta.slng(10〜40)> cmap の文字体系(1)の順で、
	 * すべて正規 BCP 47 で比べる。meta に言語があって合わなければ cmap は見ない。cmap はラテン文字体系のロケールでは根拠にしない
	 * (どのフォントもラテン文字を持つため)。name 表の言語やロケールの表示名は根拠にしない。
	 * The locale match score of a font (0 = no match). Evidence, compared as canonical BCP 47: meta.dlng (100..400) over
	 * meta.slng (10..40) over the cmap's scripts (1). When meta lists languages and none matches, the cmap is not
	 * consulted. The cmap is not evidence for a Latin-script locale, since every font holds Latin letters. Name table
	 * languages and locale display names are never evidence.
	 */
	static int localeMatchScore(FontCoverage font, Locale locale) {
		String wanted = canonicalBcp47(locale);
		if (font == null || wanted == null) {
			return 0;
		}
		int dlng = bestTagMatchScore(font.designLanguages(), wanted);
		if (dlng > 0) {
			return SCORE_DLNG * dlng;
		}
		int slng = bestTagMatchScore(font.supportedLanguages(), wanted);
		if (slng > 0) {
			return SCORE_SLNG * slng;
		}
		if (!font.designLanguages().isEmpty() || !font.supportedLanguages().isEmpty()) {
			return 0;
		}
		return providesLocaleScript(font, wanted) ? SCORE_CMAP : 0;
	}

	private static boolean providesLocaleScript(FontCoverage font, String localeTag) {
		String script = Locale.forLanguageTag(localeTag).getScript();
		if (script.isEmpty() || LATIN.equals(script) || font.state() != FontCoverage.State.READY) {
			return false;
		}
		Set<UnicodeScript> evidence = CMAP_EVIDENCE.get(script);
		if (evidence == null) {
			try {
				evidence = Set.of(UnicodeScript.forName(script));
			} catch (IllegalArgumentException unknownScript) {
				return false;
			}
		}
		return font.scripts().containsAll(evidence);
	}

	private static boolean isLatinLocale(Locale locale) {
		String canonical = canonicalBcp47(locale);
		return canonical != null && LATIN.equals(Locale.forLanguageTag(canonical).getScript());
	}

	/** 基本ラテン文字(A-Z・a-z)を一つも収録しないと分かっているフォント。 / A font known to hold no basic Latin letter (A-Z, a-z). */
	static boolean lacksLatinLetters(FontCoverage font) {
		if (font == null || font.state() == FontCoverage.State.UNKNOWN) {
			return false;
		}
		for (int letter = 'A'; letter <= 'z'; letter++) {
			if (Character.isLetter(letter) && font.mayContain(letter)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isSymbolOnlyFont(FontCoverage font, Locale locale, int localeScore) {
		if (!lacksLatinLetters(font)) {
			return false;
		}
		// 非ラテン系ロケール(ja, ko, zh, th 等)に明示一致する CJK/非ラテン専用フォントは、基本ラテン文字を持たなくても記号フォント扱いしない。
		// A font that positively matches a non-Latin locale (e.g. CJK subset font) is not demoted to a symbol font merely for lacking A-Z.
		return localeScore <= 0 || isLatinLocale(locale);
	}

	/**
	 * 共通代替フォントを並べ直す(安定): ロケールに合うもの(適合スコア順)、次に見つけた順のもの、最後に基本ラテン文字を持たない記号・絵文字フォント。
	 * 非ラテン系ロケールに明示一致する CJK フォントは基本ラテン文字を持たなくても先頭群に置く。
	 * Reorders the global fallbacks (stable): locale matches first (ordered by match strength), then the rest in found
	 * order, and last symbol/emoji fonts that hold no basic Latin letter and do not match a non-Latin target locale.
	 */
	static <T> List<T> preferLocale(List<T> items, Function<T, FontCoverage> coverage, Locale locale) {
		record Scored<T>(T item, int score, int index) {
		}
		var matching = new ArrayList<Scored<T>>();
		var others = new ArrayList<T>();
		var symbols = new ArrayList<T>();
		int idx = 0;
		for (T item : items) {
			FontCoverage font = coverage.apply(item);
			int score = localeMatchScore(font, locale);
			if (isSymbolOnlyFont(font, locale, score)) {
				symbols.add(item);
			} else if (score > 0) {
				matching.add(new Scored<>(item, score, idx));
			} else {
				others.add(item);
			}
			idx++;
		}
		matching.sort(Comparator.<Scored<T>>comparingInt(s -> s.score).reversed()
				.thenComparingInt(s -> s.index));
		var result = new ArrayList<T>(items.size());
		for (Scored<T> s : matching) {
			result.add(s.item);
		}
		result.addAll(others);
		result.addAll(symbols);
		return result;
	}

	private static void addIfUsable(String path, Set<Path> seen, List<String> result) {
		if (path.isEmpty()) {
			return;
		}
		try {
			if (!Files.isRegularFile(Path.of(path))) {
				LOGGER.warning("Global fallback font not found: " + path);
				return;
			}
			if (seen.add(normalize(path))) {
				result.add(path.replace('\\', '/'));
			}
		} catch (InvalidPathException e) {
			LOGGER.warning("Global fallback font path invalid: " + path + " - " + e.getMessage());
		}
	}

	private static boolean isFontFile(String name) {
		var lower = name.toLowerCase(Locale.ROOT);
		return lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".ttc");
	}

	private static Path normalize(String path) {
		return Path.of(path).toAbsolutePath().normalize();
	}

	/** 正規化したパスを返す。null またはこの環境で不正なパスなら null。 / Returns the normalized path, or null when the string is null or not a valid path on this platform. */
	private static Path tryNormalize(String path) {
		if (path == null) {
			return null;
		}
		try {
			return normalize(path);
		} catch (InvalidPathException e) {
			return null;
		}
	}
}