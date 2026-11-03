package bms.player.beatoraja.launcher;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Regression tests for skin scanning through symbolic links. */
public final class SkinConfigurationViewTest {

	private SkinConfigurationViewTest() {
	}

	public static void main(String[] args) throws Exception {
		Path root = Files.createTempDirectory("skin-scan-test");
		Path outside = Files.createTempDirectory("skin-scan-outside-test");
		try {
			Path actualSkin = Files.createFile(root.resolve("actual.lr2skin"));
			Files.createFile(outside.resolve("outside.lr2skin"));
			try {
				Files.createSymbolicLink(root.resolve("outside"), outside);
				Files.createSymbolicLink(root.resolve("loop"), root);
			} catch (UnsupportedOperationException | IOException e) {
				return;
			}

			List<Path> paths = scan(root);
			assertEquals(List.of(actualSkin), paths);
		} finally {
			deleteTree(root);
			deleteTree(outside);
		}
	}

	private static List<Path> scan(Path root) throws Exception {
		SkinConfigurationView view = new SkinConfigurationView();
		Method scan = SkinConfigurationView.class.getDeclaredMethod("scan", Path.class, List.class);
		scan.setAccessible(true);
		List<Path> paths = new ArrayList<>();
		try {
			scan.invoke(view, root, paths);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Exception exception) {
				throw exception;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw new RuntimeException(cause);
		}
		return paths;
	}

	private static void deleteTree(Path root) throws IOException {
		try (var paths = Files.walk(root)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(path);
			}
		}
	}

	private static void assertEquals(Object expected, Object actual) {
		if (!expected.equals(actual)) {
			throw new AssertionError("Expected " + expected + " but was " + actual);
		}
	}
}
