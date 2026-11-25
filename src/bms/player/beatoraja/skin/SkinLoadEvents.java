package bms.player.beatoraja.skin;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * JFR events for measuring skin loading. They cost almost nothing while no recording is running.
 * Summarize with scripts/skin-load-jfr.py.
 */
public final class SkinLoadEvents {

	private SkinLoadEvents() {
	}

	@Name("beatoraja.SkinLoad")
	@Label("Skin Load")
	@Category({ "beatoraja", "Skin" })
	@Description("Whole SkinLoader.load call")
	@StackTrace(false)
	public static final class Load extends Event {
		@Label("Skin Type")
		public String skinType;
		@Label("Path")
		public String path;
		@Label("Format")
		public String format;
		@Label("Success")
		public boolean success;
	}

	@Name("beatoraja.SkinLoadPhase")
	@Label("Skin Load Phase")
	@Category({ "beatoraja", "Skin" })
	@Description("Timed part of a skin load (header, lua-exec, json-parse, pixmap-decode, texture-upload, ...)")
	@StackTrace(false)
	public static final class Phase extends Event {
		@Label("Phase")
		public String phase;
		@Label("Detail")
		public String detail;
		@Label("Bytes")
		public long bytes;
	}

	/** Starts a phase event; returns null when JFR is not recording it, so callers pay only one check. */
	public static Phase begin(String phase) {
		Phase event = new Phase();
		if (!event.isEnabled()) {
			return null;
		}
		event.phase = phase;
		event.begin();
		return event;
	}

	public static void end(Phase event, String detail) {
		end(event, detail, 0);
	}

	public static void end(Phase event, String detail, long bytes) {
		if (event == null) {
			return;
		}
		event.end();
		if (event.shouldCommit()) {
			event.detail = detail;
			event.bytes = bytes;
			event.commit();
		}
	}
}
