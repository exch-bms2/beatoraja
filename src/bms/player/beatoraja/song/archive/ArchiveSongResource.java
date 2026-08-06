package bms.player.beatoraja.song.archive;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bms.player.beatoraja.song.SongResource;

/** A read-only view of a file or directory inside a song archive. */
final class ArchiveSongResource implements SongResource {

	private final Path archive;
	private final String entryName;
	private final String visibleRoot;
	private final boolean directory;

	ArchiveSongResource(Path archive, String entryName, String visibleRoot, boolean directory) {
		this.archive = archive.toAbsolutePath().normalize();
		this.entryName = entryName;
		this.visibleRoot = visibleRoot;
		this.directory = directory;
	}

	@Override
	public SongResource parent() {
		if (entryName.isEmpty() || entryName.equals(visibleRoot)) {
			return this;
		}
		int separator = entryName.lastIndexOf('/');
		String parent = separator >= 0 ? entryName.substring(0, separator) : "";
		return new ArchiveSongResource(archive, parent, visibleRoot, true);
	}

	@Override
	public SongResource resolve(String relativePath) {
		if (relativePath.isEmpty()) {
			return this;
		}
		String base = directory ? entryName : parentEntryName();
		String resolved = SongArchives.resolveEntryName(base, relativePath);
		if (visibleRoot != null && !resolved.equals(visibleRoot) && !resolved.startsWith(visibleRoot + "/")) {
			throw new IllegalArgumentException("Archive resource escapes visible root: " + relativePath);
		}
		return new ArchiveSongResource(archive, resolved, visibleRoot, false);
	}

	@Override
	public String name() {
		if (entryName.isEmpty()) {
			return archive.getFileName().toString();
		}
		int separator = entryName.lastIndexOf('/');
		return entryName.substring(separator + 1);
	}

	@Override
	public String displayPath() {
		if (visibleRoot != null) {
			if (entryName.equals(visibleRoot)) {
				return archive + "!-" + visibleRoot;
			}
			return archive + "!-" + visibleRoot + "/" + entryName.substring(visibleRoot.length() + 1);
		}
		return entryName.isEmpty() ? archive + "!" : archive + "!/" + entryName;
	}

	@Override
	public String cacheKey() {
		return SongArchives.archiveCacheKey(archive) + "!" + entryName;
	}

	@Override
	public boolean exists() throws IOException {
		if (directory) {
			return SongArchives.hasDirectory(archive, entryName);
		}
		return SongArchives.hasEntry(archive, entryName);
	}

	@Override
	public boolean isDirectory() {
		return directory;
	}

	@Override
	public long size() throws IOException {
		if (directory) {
			throw new IOException("Archive directories do not have a size: " + displayPath());
		}
		return SongArchives.entrySize(archive, entryName);
	}

	@Override
	public InputStream openStream() throws IOException {
		if (directory) {
			throw new IOException("Cannot open an archive directory: " + displayPath());
		}
		return SongArchives.openEntry(archive, entryName);
	}

	@Override
	public List<SongResource> list() throws IOException {
		if (!directory) {
			throw new IOException("Cannot list an archive file: " + displayPath());
		}
		String prefix = entryName.isEmpty() ? "" : entryName + "/";
		List<SongResource> children = new ArrayList<>();
		Set<String> childNames = new HashSet<>();
		for (String candidate : SongArchives.listEntries(archive)) {
			if (!candidate.startsWith(prefix)) {
				continue;
			}
			String remainder = candidate.substring(prefix.length());
			if (remainder.isEmpty()) {
				continue;
			}
			int separator = remainder.indexOf('/');
			String childName = separator >= 0 ? remainder.substring(0, separator) : remainder;
			if (!childNames.add(childName)) {
				continue;
			}
			String childEntry = prefix + childName;
			children.add(new ArchiveSongResource(archive, childEntry, visibleRoot, separator >= 0));
		}
		return children;
	}

	private String parentEntryName() {
		int separator = entryName.lastIndexOf('/');
		return separator >= 0 ? entryName.substring(0, separator) : "";
	}
}
