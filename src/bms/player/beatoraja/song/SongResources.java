package bms.player.beatoraja.song;

import java.io.IOException;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import bms.player.beatoraja.song.archive.SongArchives;

/** Factory and single-file materialization cache for {@link SongResource}. */
public final class SongResources {

	private static final Map<String, Path> MATERIALIZED = new ConcurrentHashMap<>();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> MATERIALIZED.values().forEach(path -> {
			try {
				Files.deleteIfExists(path);
			} catch (IOException ignored) {
			}
		}), "song resource cleanup"));
	}

	private SongResources() {
	}

	public static SongResource fromPath(Path path) {
		return SongArchives.resource(path);
	}

	public static SongResource local(Path path) {
		return new LocalSongResource(path.normalize());
	}

	/**
	 * Creates a resource for a directly accessible HTTP(S) file. Directory
	 * listing intentionally requires a future manifest API and is unsupported.
	 */
	public static SongResource remote(URI uri) {
		return new RemoteSongResource(uri.normalize());
	}

	public static Path materialize(SongResource resource) throws IOException {
		Optional<Path> localPath = resource.localPath();
		if (localPath.isPresent()) {
			return localPath.get();
		}
		Path cached = MATERIALIZED.get(resource.cacheKey());
		if (cached != null && Files.isRegularFile(cached)) {
			return cached;
		}
		synchronized (MATERIALIZED) {
			cached = MATERIALIZED.get(resource.cacheKey());
			if (cached != null && Files.isRegularFile(cached)) {
				return cached;
			}
			String suffix = extensionSuffix(resource.name());
			Path target = Files.createTempFile("beatoraja-song-resource-", suffix);
			try (InputStream input = resource.openStream()) {
				Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException | RuntimeException e) {
				Files.deleteIfExists(target);
				throw e;
			}
			MATERIALIZED.put(resource.cacheKey(), target);
			return target;
		}
	}

	private static String extensionSuffix(String name) {
		int index = name.lastIndexOf('.');
		return index >= 0 && index < name.length() - 1 ? name.substring(index) : ".tmp";
	}

	private record LocalSongResource(Path path) implements SongResource {
		@Override
		public SongResource parent() {
			Path parent = path.getParent();
			return parent != null ? local(parent) : this;
		}

		@Override
		public SongResource resolve(String relativePath) {
			return local(path.resolve(relativePath));
		}

		@Override
		public String name() {
			Path filename = path.getFileName();
			return filename != null ? filename.toString() : path.toString();
		}

		@Override
		public String displayPath() {
			return path.toString();
		}

		@Override
		public String cacheKey() {
			try {
				return path.toAbsolutePath() + ":" + Files.size(path) + ":" + Files.getLastModifiedTime(path);
			} catch (IOException e) {
				return path.toAbsolutePath().toString();
			}
		}

		@Override
		public boolean exists() {
			return Files.exists(path);
		}

		@Override
		public boolean isDirectory() {
			return Files.isDirectory(path);
		}

		@Override
		public long size() throws IOException {
			return Files.size(path);
		}

		@Override
		public InputStream openStream() throws IOException {
			return Files.newInputStream(path);
		}

		@Override
		public List<SongResource> list() throws IOException {
			try (var paths = Files.list(path)) {
				return paths.map(SongResources::local).toList();
			}
		}

		@Override
		public Optional<Path> localPath() {
			return Optional.of(path);
		}
	}

	private record RemoteSongResource(URI uri) implements SongResource {
		@Override
		public SongResource parent() {
			String value = uri.toString();
			int slash = value.lastIndexOf('/');
			return slash >= 0 ? remote(URI.create(value.substring(0, slash + 1))) : this;
		}

		@Override
		public SongResource resolve(String relativePath) {
			return remote(uri.resolve(relativePath));
		}

		@Override
		public String name() {
			String path = uri.getPath();
			int slash = path.lastIndexOf('/');
			return slash >= 0 ? path.substring(slash + 1) : path;
		}

		@Override
		public String displayPath() {
			return uri.toString();
		}

		@Override
		public String cacheKey() {
			return uri.toString();
		}

		@Override
		public boolean exists() throws IOException {
			HttpURLConnection connection = connection("HEAD");
			try {
				return connection.getResponseCode() / 100 == 2;
			} finally {
				connection.disconnect();
			}
		}

		@Override
		public boolean isDirectory() {
			return false;
		}

		@Override
		public long size() throws IOException {
			HttpURLConnection connection = connection("HEAD");
			try {
				long length = connection.getContentLengthLong();
				if (length < 0) {
					throw new IOException("Remote resource does not report a size: " + uri);
				}
				return length;
			} finally {
				connection.disconnect();
			}
		}

		@Override
		public InputStream openStream() throws IOException {
			HttpURLConnection connection = connection("GET");
			return new FilterInputStream(connection.getInputStream()) {
				@Override
				public void close() throws IOException {
					try {
						super.close();
					} finally {
						connection.disconnect();
					}
				}
			};
		}

		@Override
		public List<SongResource> list() throws IOException {
			throw new IOException("Remote song resources require a manifest to list entries: " + uri);
		}

		private HttpURLConnection connection(String method) throws IOException {
			HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
			connection.setRequestMethod(method);
			connection.setConnectTimeout(10_000);
			connection.setReadTimeout(30_000);
			return connection;
		}
	}
}
