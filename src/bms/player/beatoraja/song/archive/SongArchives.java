package bms.player.beatoraja.song.archive;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry and path-based facade for song archives. Add a {@link SongArchive}
 * implementation here to support another archive format.
 */
public final class SongArchives {

	private static final String ARCHIVE_STORAGE_DIRECTORY = ".beatoraja-archives";
	private static final List<SongArchive> ARCHIVES = List.of(new ZipSongArchive(), new RarSongArchive());
	private static final Map<Path, ExtractedArchive> EXTRACTED_ARCHIVES = new ConcurrentHashMap<>();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> EXTRACTED_ARCHIVES.values().forEach(archive ->
				deleteRecursively(archive.root())), "song archive cleanup"));
	}

	private SongArchives() {
	}

	public static boolean isSupportedArchive(Path path) {
		return archiveFor(path) != null;
	}

	public static Path virtualPath(Path archive, String entryName) {
		return Path.of(archive + "!/" + normalizeEntryName(entryName));
	}

	/**
	 * Returns an archive path whose visible hierarchy omits one shared top-level
	 * directory while preserving the actual archive entry name internally.
	 */
	public static Path virtualPath(Path archive, String entryName, String rootDirectory) {
		String normalizedEntryName = normalizeEntryName(entryName);
		if (rootDirectory == null) {
			return virtualPath(archive, normalizedEntryName);
		}
		String normalizedRootDirectory = normalizeRootDirectory(rootDirectory);
		if (!normalizedEntryName.startsWith(normalizedRootDirectory + "/")) {
			throw new IllegalArgumentException("Archive entry is not inside the root directory: " + entryName);
		}
		return Path.of(archive + "!-" + normalizedRootDirectory + "/"
				+ normalizedEntryName.substring(normalizedRootDirectory.length() + 1));
	}

	public static Path virtualRoot(Path archive) {
		return Path.of(archive + "!");
	}

	public static Path virtualRoot(Path archive, String rootDirectory) {
		return rootDirectory == null ? virtualRoot(archive)
				: Path.of(archive + "!-" + normalizeRootDirectory(rootDirectory));
	}

	public static boolean isVirtualPath(Path path) {
		return parse(path) != null;
	}

	public static String entryName(Path path) {
		ArchivePath archivePath = parse(path);
		return archivePath != null ? archivePath.entryName() : null;
	}

	/** Returns an ordinary path for a regular path or an archive entry path. */
	public static Path resolve(Path path) throws IOException {
		ArchivePath archivePath = parse(path);
		if (archivePath == null) {
			return path;
		}
		Path root = extractToTemporaryDirectory(archivePath.archive());
		Path resolved = root.resolve(archivePath.entryName()).normalize();
		if (!resolved.startsWith(root) || !Files.isRegularFile(resolved)) {
			throw new IOException("Archive entry does not exist: " + archivePath.entryName());
		}
		return resolved;
	}

	/**
	 * Expands an archive beside itself and moves the source archive into the
	 * reserved, non-scanned archive storage directory.
	 */
	public static Path expandToSongDirectory(Path archive) throws IOException {
		Path normalizedArchive = archive.toAbsolutePath().normalize();
		ArchiveContents contents = readContents(normalizedArchive);
		if (contents.entries().stream().noneMatch(SongArchives::isChartEntry)) {
			throw new IOException("Song archive does not contain a chart: " + archive);
		}

		Path parent = normalizedArchive.getParent();
		if (parent == null) {
			throw new IOException("Song archive does not have a parent directory: " + archive);
		}
		String archiveBaseName = archiveBaseName(normalizedArchive);
		String targetName = contents.rootDirectory() != null ? contents.rootDirectory() : archiveBaseName;
		Path target = parent.resolve(targetName);
		if (Files.exists(target)) {
			throw new IOException("Song archive destination already exists: " + target);
		}

		Path staging = Files.createTempDirectory(parent, "." + archiveBaseName + "-extract-");
		try {
			archiveForRequired(normalizedArchive).extract(normalizedArchive, staging);
			Path extractedRoot = contents.rootDirectory() != null ? staging.resolve(contents.rootDirectory()) : staging;
			if (!Files.isDirectory(extractedRoot)) {
				throw new IOException("Song archive root was not extracted: " + normalizedArchive);
			}

			Path archiveStorage = parent.resolve(ARCHIVE_STORAGE_DIRECTORY);
			Files.createDirectories(archiveStorage);
			Path storedArchive = uniqueArchivePath(archiveStorage, normalizedArchive.getFileName().toString());
			move(normalizedArchive, storedArchive);
			try {
				move(extractedRoot, target);
			} catch (IOException e) {
				try {
					move(storedArchive, normalizedArchive);
				} catch (IOException restoreFailure) {
					e.addSuppressed(restoreFailure);
				}
				throw e;
			}
			return target;
		} finally {
			deleteRecursively(staging);
		}
	}

	public static boolean isArchiveStorageDirectory(Path directory) {
		Path name = directory.getFileName();
		return name != null && ARCHIVE_STORAGE_DIRECTORY.equals(name.toString());
	}

	public static List<String> listEntries(Path archive) throws IOException {
		return archiveForRequired(archive).listEntries(archive);
	}

	/** Inspects entries and identifies a single top-level content directory. */
	public static ArchiveContents readContents(Path archive) throws IOException {
		List<String> entries = listEntries(archive);
		String rootDirectory = null;
		for (String entry : entries) {
			if (isArchiveMetadata(entry)) {
				continue;
			}
			int separator = entry.indexOf('/');
			if (separator <= 0) {
				return new ArchiveContents(entries, null);
			}
			String directory = entry.substring(0, separator);
			if (rootDirectory == null) {
				rootDirectory = directory;
			} else if (!rootDirectory.equals(directory)) {
				return new ArchiveContents(entries, null);
			}
		}
		return new ArchiveContents(entries, rootDirectory);
	}

	/** Reads a chart entry without materializing the entire archive. */
	public static byte[] readEntry(Path path) throws IOException {
		ArchivePath archivePath = parse(path);
		if (archivePath == null) {
			return Files.readAllBytes(path);
		}
		return archiveForRequired(archivePath.archive()).readEntry(archivePath.archive(), archivePath.entryName());
	}

	private static Path extractToTemporaryDirectory(Path archive) throws IOException {
		Path normalizedArchive = archive.toAbsolutePath().normalize();
		FileTime modifiedTime = Files.getLastModifiedTime(normalizedArchive);
		long size = Files.size(normalizedArchive);
		ExtractedArchive existing = EXTRACTED_ARCHIVES.get(normalizedArchive);
		if (existing != null && existing.matches(modifiedTime, size)) {
			return existing.root();
		}

		synchronized (EXTRACTED_ARCHIVES) {
			existing = EXTRACTED_ARCHIVES.get(normalizedArchive);
			if (existing != null && existing.matches(modifiedTime, size)) {
				return existing.root();
			}

			Path root = Files.createTempDirectory("beatoraja-archive-");
			try {
				archiveForRequired(normalizedArchive).extract(normalizedArchive, root);
				EXTRACTED_ARCHIVES.put(normalizedArchive, new ExtractedArchive(root, modifiedTime, size));
				return root;
			} catch (IOException | RuntimeException e) {
				deleteRecursively(root);
				throw e;
			}
		}
	}

	private static SongArchive archiveFor(Path path) {
		return ARCHIVES.stream().filter(archive -> archive.supports(path)).findFirst().orElse(null);
	}

	private static SongArchive archiveForRequired(Path path) throws IOException {
		SongArchive archive = archiveFor(path);
		if (archive == null) {
			throw new IOException("Unsupported song archive: " + path);
		}
		return archive;
	}

	private static ArchivePath parse(Path path) {
		String value = path.toString();
		String lowerCase = value.toLowerCase(Locale.ROOT);
		for (SongArchive archive : ARCHIVES) {
			for (String extension : archive.extensions()) {
				int marker = lowerCase.indexOf(extension + "!");
				if (marker < 0) {
					continue;
				}
				int archiveEnd = marker + extension.length();
				if (value.length() <= archiveEnd + 1 || value.charAt(archiveEnd) != '!') {
					continue;
				}
				char separator = value.charAt(archiveEnd + 1);
				if (separator != '/' && separator != '\\' && separator != '-') {
					continue;
				}
				String entryName = normalizeEntryNameOrNull(value.substring(archiveEnd + 2));
				if (entryName != null) {
					return new ArchivePath(Path.of(value.substring(0, archiveEnd)), entryName);
				}
			}
		}
		return null;
	}

	private static String normalizeEntryName(String entryName) {
		String normalized = normalizeEntryNameOrNull(entryName);
		if (normalized == null) {
			throw new IllegalArgumentException("Unsafe archive entry: " + entryName);
		}
		return normalized;
	}

	private static String normalizeEntryNameOrNull(String entryName) {
		String normalized = entryName.replace('\\', '/');
		while (normalized.startsWith("./")) {
			normalized = normalized.substring(2);
		}
		if (normalized.isEmpty() || normalized.startsWith("/") || normalized.contains("//")) {
			return null;
		}
		for (String component : normalized.split("/")) {
			if (component.equals(".") || component.equals("..") || component.isEmpty()) {
				return null;
			}
		}
		return normalized;
	}

	private static String normalizeRootDirectory(String rootDirectory) {
		String normalized = normalizeEntryName(rootDirectory);
		if (normalized.indexOf('/') >= 0) {
			throw new IllegalArgumentException("Archive root directory must be a single path component: " + rootDirectory);
		}
		return normalized;
	}

	private static boolean isArchiveMetadata(String entry) {
		return entry.startsWith("__MACOSX/") || entry.startsWith("._");
	}

	private static boolean isChartEntry(String entry) {
		String lowerCase = entry.toLowerCase(Locale.ROOT);
		return lowerCase.endsWith(".bms") || lowerCase.endsWith(".bme") || lowerCase.endsWith(".bml")
				|| lowerCase.endsWith(".pms") || lowerCase.endsWith(".bmson");
	}

	private static String archiveBaseName(Path archive) {
		String filename = archive.getFileName().toString();
		int extension = filename.lastIndexOf('.');
		return extension > 0 ? filename.substring(0, extension) : filename;
	}

	private static Path uniqueArchivePath(Path directory, String filename) {
		Path candidate = directory.resolve(filename);
		if (!Files.exists(candidate)) {
			return candidate;
		}
		int extension = filename.lastIndexOf('.');
		String baseName = extension > 0 ? filename.substring(0, extension) : filename;
		String suffix = extension > 0 ? filename.substring(extension) : "";
		for (int index = 1; ; index++) {
			candidate = directory.resolve(baseName + " (" + index + ")" + suffix);
			if (!Files.exists(candidate)) {
				return candidate;
			}
		}
	}

	private static void move(Path source, Path target) throws IOException {
		try {
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(source, target);
		}
	}

	private static void deleteRecursively(Path path) {
		try (var paths = Files.walk(path)) {
			paths.sorted(Comparator.reverseOrder()).forEach(file -> {
				try {
					Files.deleteIfExists(file);
				} catch (IOException ignored) {
				}
			});
		} catch (IOException ignored) {
		}
	}

	private record ArchivePath(Path archive, String entryName) {
	}

	public record ArchiveContents(List<String> entries, String rootDirectory) {
	}

	private record ExtractedArchive(Path root, FileTime modifiedTime, long size) {
		private boolean matches(FileTime currentModifiedTime, long currentSize) {
			return modifiedTime.equals(currentModifiedTime) && size == currentSize && Files.isDirectory(root);
		}
	}
}
