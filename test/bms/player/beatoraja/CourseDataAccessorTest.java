package bms.player.beatoraja;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

/** Regression tests for course file discovery. */
public final class CourseDataAccessorTest {

	private CourseDataAccessorTest() {
	}

	public static void main(String[] args) throws IOException {
		Path directory = Files.createTempDirectory("course-data-accessor-test");
		try {
			Files.writeString(directory.resolve("valid.json"), "[]");
			Files.writeString(directory.resolve("upper.JSON"), "[]");
			Files.writeString(directory.resolve("invalid.json"), "{");
			Files.createFile(directory.resolve("no-extension"));
			Files.createDirectory(directory.resolve("directory.json"));
			Files.createFile(directory.resolve("ignored.txt"));

			CourseDataAccessor accessor = new CourseDataAccessor(directory.toString());
			String[] names = accessor.readAllNames();
			Arrays.sort(names);
			assertEquals(new String[] { "invalid", "upper", "valid" }, names);
			assertEquals(0, accessor.readAll().length);
		} finally {
			try (var paths = Files.walk(directory)) {
				for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
					Files.deleteIfExists(path);
				}
			}
		}
	}

	private static void assertEquals(String[] expected, String[] actual) {
		if (!Arrays.equals(expected, actual)) {
			throw new AssertionError("Expected " + Arrays.toString(expected) + " but was " + Arrays.toString(actual));
		}
	}

	private static void assertEquals(int expected, int actual) {
		if (expected != actual) {
			throw new AssertionError("Expected " + expected + " but was " + actual);
		}
	}
}
