package bms.player.beatoraja.skin.json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Regression tests for invalid JSON skin headers. */
public final class JSONSkinLoaderTest {

	private JSONSkinLoaderTest() {
	}

	public static void main(String[] args) throws IOException {
		assertInvalidHeader("{");
		assertInvalidHeader("null");
	}

	private static void assertInvalidHeader(String contents) throws IOException {
		Path path = Files.createTempFile("json-skin-loader-test", ".json");
		try {
			Files.writeString(path, contents);
			if (new JSONSkinLoader().loadHeader(path) != null) {
				throw new AssertionError("Invalid JSON skin header must be ignored");
			}
		} finally {
			Files.deleteIfExists(path);
		}
	}
}
