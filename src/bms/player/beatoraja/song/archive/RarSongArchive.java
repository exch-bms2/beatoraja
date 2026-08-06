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

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

/** RAR implementation backed by Junrar. */
public final class RarSongArchive extends SongArchive {

	public RarSongArchive() {
		super(".rar");
	}

	@Override
	public List<String> listEntries(Path archive) throws IOException {
		try (Archive rar = open(archive)) {
			List<FileHeader> headers = rar.getFileHeaders();
			if (headers.size() > MAX_ENTRY_COUNT) {
				throw new IOException("RAR contains too many entries: " + archive);
			}
			List<String> entries = new ArrayList<>();
			Set<String> names = new HashSet<>();
			for (FileHeader header : headers) {
				validate(header, archive);
				if (header.isDirectory()) {
					continue;
				}
				String name = checkedEntryName(names, header.getFileName());
				entries.add(name);
			}
			return entries;
		} catch (RarException e) {
			throw new IOException("Unable to read RAR archive: " + archive, e);
		}
	}

	@Override
	public long entrySize(Path archive, String entryName) throws IOException {
		try (Archive rar = open(archive)) {
			for (FileHeader header : rar.getFileHeaders()) {
				validate(header, archive);
				if (!header.isDirectory() && entryName.equals(normalizeEntryNameOrNull(header.getFileName()))) {
					return header.getFullUnpackSize();
				}
			}
			throw new IOException("RAR entry does not exist: " + entryName);
		} catch (RarException e) {
			throw new IOException("Unable to read RAR entry: " + entryName, e);
		}
	}

	@Override
	public InputStream openEntry(Path archive, String entryName) throws IOException {
		try {
			Archive rar = open(archive);
			try {
				for (FileHeader header : rar.getFileHeaders()) {
					validate(header, archive);
					if (!header.isDirectory() && entryName.equals(normalizeEntryNameOrNull(header.getFileName()))) {
						return new FilterInputStream(rar.getInputStream(header)) {
							@Override
							public void close() throws IOException {
								try {
									super.close();
								} finally {
									rar.close();
								}
							}
						};
					}
				}
				rar.close();
				throw new IOException("RAR entry does not exist: " + entryName);
			} catch (IOException | RuntimeException e) {
				rar.close();
				throw e;
			}
		} catch (RarException e) {
			throw new IOException("Unable to read RAR entry: " + entryName, e);
		}
	}

	@Override
	public byte[] readEntry(Path archive, String entryName) throws IOException {
		try (Archive rar = open(archive)) {
			for (FileHeader header : rar.getFileHeaders()) {
				validate(header, archive);
				if (header.isDirectory() || !entryName.equals(normalizeEntryNameOrNull(header.getFileName()))) {
					continue;
				}
				long size = header.getFullUnpackSize();
				if (size > MAX_CHART_SIZE) {
					throw new IOException("RAR chart is too large: " + entryName);
				}
				try (InputStream input = rar.getInputStream(header);
						ByteArrayOutputStream output = new ByteArrayOutputStream((int) Math.max(size, 0))) {
					return readLimited(input, output, MAX_CHART_SIZE, "RAR chart is too large: " + entryName);
				}
			}
			throw new IOException("RAR entry does not exist: " + entryName);
		} catch (RarException e) {
			throw new IOException("Unable to read RAR entry: " + entryName, e);
		}
	}

	@Override
	public void extract(Path archive, Path root) throws IOException {
		try (Archive rar = open(archive)) {
			List<FileHeader> headers = rar.getFileHeaders();
			if (headers.size() > MAX_ENTRY_COUNT) {
				throw new IOException("RAR contains too many entries: " + archive);
			}
			Set<String> names = new HashSet<>();
			long[] extractedSize = { 0 };
			for (FileHeader header : headers) {
				validate(header, archive);
				if (header.isDirectory()) {
					continue;
				}
				String name = checkedEntryName(names, header.getFileName());
				Path target = extractionTarget(root, name, "RAR entry");
				Files.createDirectories(target.getParent());
				try (OutputStream output = new LimitedOutputStream(Files.newOutputStream(target), extractedSize,
						MAX_EXTRACTED_SIZE, "RAR is too large after extraction: " + archive)) {
					rar.extractFile(header, output);
				}
			}
		} catch (RarException e) {
			throw new IOException("Unable to extract RAR archive: " + archive, e);
		}
	}

	private Archive open(Path archive) throws IOException, RarException {
		Archive rar = new Archive(archive.toFile());
		try {
			if (rar.isPasswordProtected()) {
				throw new IOException("Password protected RAR archives are not supported: " + archive);
			}
			return rar;
		} catch (IOException | RarException e) {
			try {
				rar.close();
			} catch (IOException ignored) {
			}
			throw e;
		}
	}

	private void validate(FileHeader header, Path archive) throws IOException {
		if (header.isEncrypted()) {
			throw new IOException("Password protected RAR entries are not supported: " + archive);
		}
		if (header.isSplitBefore() || header.isSplitAfter()) {
			throw new IOException("Split RAR archives are not supported: " + archive);
		}
	}

	private String checkedEntryName(Set<String> names, String entryName) throws IOException {
		String name = normalizeEntryNameOrNull(entryName);
		if (name == null || !names.add(name)) {
			throw new IOException("Unsafe RAR entry: " + entryName);
		}
		return name;
	}
}
