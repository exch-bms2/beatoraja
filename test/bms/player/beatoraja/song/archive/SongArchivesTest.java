package bms.player.beatoraja.song.archive;

import java.nio.file.Path;

/** Regression tests for archive virtual path handling. */
public final class SongArchivesTest {

	private SongArchivesTest() {
	}

	public static void main(String[] args) {
		Path archive = Path.of("/songs/example.ZIP");
		assertEquals(archive, SongArchives.archivePath(SongArchives.virtualPath(archive, "set/chart.bms")));
		assertEquals(archive, SongArchives.archivePath(SongArchives.virtualPath(archive, "set/chart.bms", "set")));
		assertNull(SongArchives.archivePath(Path.of("/songs/set/chart.bms")));
	}

	private static void assertEquals(Object expected, Object actual) {
		if (!expected.equals(actual)) {
			throw new AssertionError("Expected " + expected + " but was " + actual);
		}
	}

	private static void assertNull(Object actual) {
		if (actual != null) {
			throw new AssertionError("Expected null but was " + actual);
		}
	}
}
