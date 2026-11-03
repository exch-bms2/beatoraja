package bms.player.beatoraja;

import bms.player.beatoraja.song.SongInformation;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

/** Regression tests for malformed local data. */
public final class InputDataValidationTest {

	private InputDataValidationTest() {
	}

	public static void main(String[] args) throws Exception {
		testInvalidSortIdFallsBackToDefault();
		testNullDistributionIsAccepted();
		testMalformedReplayIsIgnored();
	}

	private static void testInvalidSortIdFallsBackToDefault() {
		PlayerConfig config = new PlayerConfig();
		config.setSortid("NOT_A_SORTER");
		config.validate();
		assertEquals("TITLE", config.getSortid());
	}

	private static void testNullDistributionIsAccepted() {
		SongInformation information = new SongInformation();
		information.setDistribution(null);
		assertEquals("", information.getDistribution());
		assertEquals(0, information.getDistributionValues().length);
	}

	private static void testMalformedReplayIsIgnored() throws Exception {
		Path replay = Files.createTempFile("malformed-replay", ".brd");
		try (OutputStream output = new GZIPOutputStream(Files.newOutputStream(replay))) {
			output.write("{invalid".getBytes());
		}
		try {
			if (PlayDataAccessor.readReplayFile(replay, ReplayData.class) != null) {
				throw new AssertionError("Malformed replay must be ignored");
			}
		} finally {
			Files.deleteIfExists(replay);
		}
	}

	private static void assertEquals(Object expected, Object actual) {
		if (!expected.equals(actual)) {
			throw new AssertionError("Expected " + expected + " but was " + actual);
		}
	}
}
