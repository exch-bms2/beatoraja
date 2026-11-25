package bms.player.beatoraja.skin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.logging.Logger;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;

/**
 * Starts a JFR recording in the game process when -Dbeatoraja.skinLoadJfr=&lt;directory&gt; is set,
 * so skin loads from startup on are captured. The file is written when the game exits.
 * JDK 17 cannot put the pid in -XX:StartFlightRecording file names, and the launcher passes its
 * JVM options to the game process, so the recording is started here instead.
 */
public final class SkinLoadRecording {

	public static final String PROPERTY = "beatoraja.skinLoadJfr";

	private SkinLoadRecording() {
	}

	public static void startIfRequested() {
		final String dir = System.getProperty(PROPERTY);
		if (dir == null || dir.isBlank()) {
			return;
		}
		try {
			Map<String, String> settings = new java.util.HashMap<>(Configuration.getConfiguration("profile").getSettings());
			// Loads take a few hundred ms, so sample often. Image decoding runs in native code, so sample native threads too.
			settings.put("jdk.ExecutionSample#period", "1 ms");
			settings.put("jdk.NativeMethodSample#period", "1 ms");
			settings.put("beatoraja.SkinLoad#enabled", "true");
			settings.put("beatoraja.SkinLoad#threshold", "0 ms");
			settings.put("beatoraja.SkinLoadPhase#enabled", "true");
			settings.put("beatoraja.SkinLoadPhase#threshold", "0 ms");
			Path out = Path.of(dir).toAbsolutePath();
			Files.createDirectories(out);
			out = out.resolve("skinload-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
					+ "-" + ProcessHandle.current().pid() + ".jfr");
			Recording recording = new Recording(settings);
			recording.setName("skinload");
			recording.setToDisk(true);
			recording.setDestination(out);
			recording.setDumpOnExit(true);
			recording.start();
			Logger.getGlobal().info("Skin load JFR recording started; written on exit to " + out);
		} catch (Throwable e) {
			Logger.getGlobal().warning("Could not start skin load JFR recording: " + e);
		}
	}
}
