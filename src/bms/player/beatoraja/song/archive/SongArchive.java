package bms.player.beatoraja.song.archive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Base class for a song archive format. Implementations provide archive-format
 * specific I/O while this class centralizes limits and path validation.
 */
public abstract class SongArchive {

	protected static final long MAX_EXTRACTED_SIZE = 4L * 1024 * 1024 * 1024;
	protected static final long MAX_CHART_SIZE = 64L * 1024 * 1024;
	protected static final int MAX_ENTRY_COUNT = 100_000;

	private final List<String> extensions;

	protected SongArchive(String... extensions) {
		this.extensions = Arrays.stream(extensions)
				.map(extension -> extension.toLowerCase(Locale.ROOT))
				.toList();
	}

	public final boolean supports(Path path) {
		Path filename = path.getFileName();
		if (filename == null || !Files.isRegularFile(path)) {
			return false;
		}
		String name = filename.toString().toLowerCase(Locale.ROOT);
		return extensions.stream().anyMatch(name::endsWith);
	}

	public final List<String> extensions() {
		return extensions;
	}

	public abstract List<String> listEntries(Path archive) throws IOException;

	/** Opens one entry. Closing the returned stream must also close the archive. */
	public abstract InputStream openEntry(Path archive, String entryName) throws IOException;

	public abstract long entrySize(Path archive, String entryName) throws IOException;

	public abstract byte[] readEntry(Path archive, String entryName) throws IOException;

	public abstract void extract(Path archive, Path root) throws IOException;

	protected static String normalizeEntryName(String entryName) {
		String normalized = normalizeEntryNameOrNull(entryName);
		if (normalized == null) {
			throw new IllegalArgumentException("Unsafe archive entry: " + entryName);
		}
		return normalized;
	}

	protected static String normalizeEntryNameOrNull(String entryName) {
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

	protected static Path extractionTarget(Path root, String entryName, String errorPrefix) throws IOException {
		Path target = root.resolve(entryName).normalize();
		if (!target.startsWith(root)) {
			throw new IOException("Unsafe " + errorPrefix + ": " + entryName);
		}
		return target;
	}

	protected static byte[] readLimited(InputStream input, ByteArrayOutputStream output, long maxSize,
			String errorMessage) throws IOException {
		byte[] buffer = new byte[8192];
		long size = 0;
		int read;
		while ((read = input.read(buffer)) >= 0) {
			size += read;
			if (size > maxSize) {
				throw new IOException(errorMessage);
			}
			output.write(buffer, 0, read);
		}
		return output.toByteArray();
	}

	protected static final class LimitedOutputStream extends OutputStream {
		private final OutputStream delegate;
		private final long[] extractedSize;
		private final long maxSize;
		private final String errorMessage;

		public LimitedOutputStream(OutputStream delegate, long[] extractedSize, long maxSize, String errorMessage) {
			this.delegate = delegate;
			this.extractedSize = extractedSize;
			this.maxSize = maxSize;
			this.errorMessage = errorMessage;
		}

		@Override
		public void write(int value) throws IOException {
			increment(1);
			delegate.write(value);
		}

		@Override
		public void write(byte[] buffer, int offset, int length) throws IOException {
			increment(length);
			delegate.write(buffer, offset, length);
		}

		@Override
		public void close() throws IOException {
			delegate.close();
		}

		private void increment(int length) throws IOException {
			extractedSize[0] += length;
			if (extractedSize[0] > maxSize) {
				throw new IOException(errorMessage);
			}
		}
	}
}
