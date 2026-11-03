package bms.player.beatoraja.play;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Regression tests for corrupt practice configuration files. */
public final class PracticeConfigurationTest {

	private PracticeConfigurationTest() {
	}

	public static void main(String[] args) throws IOException {
		Path directory = Files.createTempDirectory("practice-configuration-test");
		try {
			Path nullProperty = directory.resolve("null.json");
			Files.writeString(nullProperty, "null");
			assertNull(PracticeConfiguration.loadPracticeProperty(nullProperty));

			Path invalidProperty = directory.resolve("invalid.json");
			Files.writeString(invalidProperty, "{");
			assertNull(PracticeConfiguration.loadPracticeProperty(invalidProperty));

			assertNull(PracticeConfiguration.loadPracticeProperty(directory.resolve("directory.json")));
		} finally {
			try (var paths = Files.walk(directory)) {
				for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
					Files.deleteIfExists(path);
				}
			}
		}
	}

	private static void assertNull(Object value) {
		if (value != null) {
			throw new AssertionError("Expected null but was " + value);
		}
	}
}
