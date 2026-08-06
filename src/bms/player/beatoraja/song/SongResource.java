package bms.player.beatoraja.song;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Read-only resource belonging to a song. A resource may be a local file, an
 * archive entry, or a remotely hosted file.
 */
public interface SongResource {

	SongResource parent();

	SongResource resolve(String relativePath);

	String name();

	String displayPath();

	/** Stable identity used by audio and image caches. */
	String cacheKey();

	boolean exists() throws IOException;

	boolean isDirectory() throws IOException;

	long size() throws IOException;

	InputStream openStream() throws IOException;

	List<SongResource> list() throws IOException;

	/** Returns a directly usable local path when this resource has one. */
	default Optional<Path> localPath() {
		return Optional.empty();
	}

	/**
	 * Creates a single-file cache entry when a third-party API only accepts a
	 * {@link Path}. Implementations must not expand an entire archive.
	 */
	default Path materialize() throws IOException {
		return SongResources.materialize(this);
	}
}
