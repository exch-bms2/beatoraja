package bms.player.beatoraja.song.archive;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/** ZIP implementation, including CP932 fallback for legacy Japanese archives. */
public final class ZipSongArchive extends SongArchive {

	private static final String LEGACY_ZIP_ENCODING = "MS932";

	public ZipSongArchive() {
		super(".zip");
	}

	@Override
	public List<String> listEntries(Path archive) throws IOException {
		try {
			return listEntriesWithDefaultEncoding(archive);
		} catch (ZipException e) {
			return listEntriesWithLegacyEncoding(archive);
		}
	}

	@Override
	public long entrySize(Path archive, String entryName) throws IOException {
		try {
			return entrySizeWithDefaultEncoding(archive, entryName);
		} catch (ZipException e) {
			return entrySizeWithLegacyEncoding(archive, entryName);
		}
	}

	@Override
	public InputStream openEntry(Path archive, String entryName) throws IOException {
		try {
			return openEntryWithDefaultEncoding(archive, entryName);
		} catch (ZipException e) {
			return openEntryWithLegacyEncoding(archive, entryName);
		}
	}

	private long entrySizeWithDefaultEncoding(Path archive, String entryName) throws IOException {
		try (ZipFile zip = new ZipFile(archive.toFile())) {
			ZipEntry entry = zip.getEntry(entryName);
			if (entry == null || entry.isDirectory()) {
				throw new IOException("ZIP entry does not exist: " + entryName);
			}
			return entry.getSize();
		}
	}

	private long entrySizeWithLegacyEncoding(Path archive, String entryName) throws IOException {
		try (var zip = new org.apache.commons.compress.archivers.zip.ZipFile(archive.toFile(), LEGACY_ZIP_ENCODING)) {
			var entry = zip.getEntry(entryName);
			if (entry == null || entry.isDirectory()) {
				throw new IOException("ZIP entry does not exist: " + entryName);
			}
			return entry.getSize();
		}
	}

	@Override
	public byte[] readEntry(Path archive, String entryName) throws IOException {
		try {
			return readEntryWithDefaultEncoding(archive, entryName);
		} catch (ZipException e) {
			return readEntryWithLegacyEncoding(archive, entryName);
		}
	}

	private InputStream openEntryWithDefaultEncoding(Path archive, String entryName) throws IOException {
		ZipFile zip = new ZipFile(archive.toFile());
		try {
			ZipEntry entry = zip.getEntry(entryName);
			if (entry == null || entry.isDirectory()) {
				throw new IOException("ZIP entry does not exist: " + entryName);
			}
			return closeArchiveWithStream(zip.getInputStream(entry), zip);
		} catch (IOException | RuntimeException e) {
			zip.close();
			throw e;
		}
	}

	private InputStream openEntryWithLegacyEncoding(Path archive, String entryName) throws IOException {
		var zip = new org.apache.commons.compress.archivers.zip.ZipFile(archive.toFile(), LEGACY_ZIP_ENCODING);
		try {
			var entry = zip.getEntry(entryName);
			if (entry == null || entry.isDirectory()) {
				throw new IOException("ZIP entry does not exist: " + entryName);
			}
			return closeArchiveWithStream(zip.getInputStream(entry), zip);
		} catch (IOException | RuntimeException e) {
			zip.close();
			throw e;
		}
	}

	private InputStream closeArchiveWithStream(InputStream stream, AutoCloseable archive) {
		return new FilterInputStream(stream) {
			@Override
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					try {
						archive.close();
					} catch (Exception e) {
						if (e instanceof IOException ioException) {
							throw ioException;
						}
						throw new IOException(e);
					}
				}
			}
		};
	}

	@Override
	public void extract(Path archive, Path root) throws IOException {
		try {
			extractWithDefaultEncoding(archive, root);
		} catch (ZipException e) {
			extractWithLegacyEncoding(archive, root);
		}
	}

	private List<String> listEntriesWithDefaultEncoding(Path archive) throws IOException {
		List<String> entries = new ArrayList<>();
		try (ZipFile zip = new ZipFile(archive.toFile())) {
			if (zip.size() > MAX_ENTRY_COUNT) {
				throw new IOException("ZIP contains too many entries: " + archive);
			}
			Set<String> names = new HashSet<>();
			var zipEntries = zip.entries();
			while (zipEntries.hasMoreElements()) {
				ZipEntry entry = zipEntries.nextElement();
				if (entry.isDirectory()) {
					continue;
				}
				addEntry(entries, names, entry.getName());
			}
		}
		return entries;
	}

	private List<String> listEntriesWithLegacyEncoding(Path archive) throws IOException {
		List<String> entries = new ArrayList<>();
		try (var zip = new org.apache.commons.compress.archivers.zip.ZipFile(archive.toFile(), LEGACY_ZIP_ENCODING)) {
			Set<String> names = new HashSet<>();
			var zipEntries = zip.getEntries();
			int entryCount = 0;
			while (zipEntries.hasMoreElements()) {
				if (++entryCount > MAX_ENTRY_COUNT) {
					throw new IOException("ZIP contains too many entries: " + archive);
				}
				var entry = zipEntries.nextElement();
				if (entry.isDirectory()) {
					continue;
				}
				addEntry(entries, names, entry.getName());
			}
		}
		return entries;
	}

	private byte[] readEntryWithDefaultEncoding(Path archive, String entryName) throws IOException {
		try (ZipFile zip = new ZipFile(archive.toFile())) {
			ZipEntry entry = zip.getEntry(entryName);
			if (entry == null || entry.isDirectory()) {
				throw new IOException("ZIP entry does not exist: " + entryName);
			}
			return readEntry(zip.getInputStream(entry), entry.getSize(), entryName);
		}
	}

	private byte[] readEntryWithLegacyEncoding(Path archive, String entryName) throws IOException {
		try (var zip = new org.apache.commons.compress.archivers.zip.ZipFile(archive.toFile(), LEGACY_ZIP_ENCODING)) {
			var entry = zip.getEntry(entryName);
			if (entry == null || entry.isDirectory()) {
				throw new IOException("ZIP entry does not exist: " + entryName);
			}
			return readEntry(zip.getInputStream(entry), entry.getSize(), entryName);
		}
	}

	private byte[] readEntry(InputStream input, long entrySize, String entryName) throws IOException {
		if (entrySize > MAX_CHART_SIZE) {
			throw new IOException("ZIP chart is too large: " + entryName);
		}
		try (input; ByteArrayOutputStream output = new ByteArrayOutputStream((int) Math.max(entrySize, 0))) {
			return readLimited(input, output, MAX_CHART_SIZE, "ZIP chart is too large: " + entryName);
		}
	}

	private void extractWithDefaultEncoding(Path archive, Path root) throws IOException {
		long extractedSize = 0;
		try (ZipFile zip = new ZipFile(archive.toFile())) {
			if (zip.size() > MAX_ENTRY_COUNT) {
				throw new IOException("ZIP contains too many entries: " + archive);
			}
			var entries = zip.entries();
			Set<String> names = new HashSet<>();
			byte[] buffer = new byte[8192];
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory()) {
					continue;
				}
				String name = checkedEntryName(names, entry.getName());
				extractedSize = extractEntry(zip.getInputStream(entry), root, name, buffer, extractedSize, archive);
			}
		}
	}

	private void extractWithLegacyEncoding(Path archive, Path root) throws IOException {
		long extractedSize = 0;
		try (var zip = new org.apache.commons.compress.archivers.zip.ZipFile(archive.toFile(), LEGACY_ZIP_ENCODING)) {
			var entries = zip.getEntries();
			Set<String> names = new HashSet<>();
			byte[] buffer = new byte[8192];
			int entryCount = 0;
			while (entries.hasMoreElements()) {
				if (++entryCount > MAX_ENTRY_COUNT) {
					throw new IOException("ZIP contains too many entries: " + archive);
				}
				var entry = entries.nextElement();
				if (entry.isDirectory()) {
					continue;
				}
				String name = checkedEntryName(names, entry.getName());
				extractedSize = extractEntry(zip.getInputStream(entry), root, name, buffer, extractedSize, archive);
			}
		}
	}

	private void addEntry(List<String> entries, Set<String> names, String entryName) throws IOException {
		entries.add(checkedEntryName(names, entryName));
	}

	private String checkedEntryName(Set<String> names, String entryName) throws IOException {
		String name = normalizeEntryNameOrNull(entryName);
		if (name == null || !names.add(name)) {
			throw new IOException("Unsafe ZIP entry: " + entryName);
		}
		return name;
	}

	private long extractEntry(InputStream input, Path root, String entryName, byte[] buffer, long extractedSize,
			Path archive) throws IOException {
		Path target = extractionTarget(root, entryName, "ZIP entry");
		Files.createDirectories(target.getParent());
		try (input; OutputStream output = Files.newOutputStream(target)) {
			int read;
			while ((read = input.read(buffer)) >= 0) {
				extractedSize += read;
				if (extractedSize > MAX_EXTRACTED_SIZE) {
					throw new IOException("ZIP is too large after extraction: " + archive);
				}
				output.write(buffer, 0, read);
			}
		}
		return extractedSize;
	}
}
