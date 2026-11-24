package bms.player.beatoraja.skin.lua;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaFunction;

/**
 * Opt-in measurement of Java-to-Lua skin calls on the render thread.
 * <p>
 * Disabled by default: {@link #site} then returns null and every call site skips measurement.
 * Enable with {@code -Dbeatoraja.luaDiagnostics=count|time|alloc} ({@code true} = time).
 * Reports are written per main-state visit after a warmup period, so skin loading and JIT warmup
 * are excluded. Times are inclusive of Java functions that Lua calls back (main_state.*).
 */
public final class LuaCallDiagnostics {
	enum Mode { COUNT, TIME, ALLOC }

	private static final LuaCallDiagnostics INSTANCE = create();
	private static final int MAX_FRAME_SAMPLES = 300_000;
	private static final int TOP_SITES = 40;
	/** Which Lua VM jar is on the classpath, so A/B reports cannot be mixed up. */
	static final String VM = vmDescription();

	private static String vmDescription() {
		String location;
		try {
			var source = LuaClosure.class.getProtectionDomain().getCodeSource();
			location = source == null ? "?" : Path.of(source.getLocation().toURI()).getFileName().toString();
		} catch (Exception | Error e) {
			location = "?";
		}
		return org.luaj.vm2.Lua._VERSION + " (" + location + ")";
	}

	/** One instrumented Lua entry point; sites with the same kind and label share counters. */
	public static final class Site {
		final String kind, label;
		long calls, nanos, maxNanos, bytes;
		Site(String kind, String label) { this.kind = kind; this.label = label; }
	}

	private final Mode mode;
	private final long warmupNanos;
	private final Path output;
	private final String run = Long.toHexString(System.currentTimeMillis());
	private final Map<String, Site> sites = new LinkedHashMap<>();
	private final AtomicLong offThreadCalls = new AtomicLong();
	private final com.sun.management.ThreadMXBean threads;

	private volatile Thread owner;
	private boolean recording, outputFailed;
	private int depth;
	private long allocAtStart;
	private double overheadNanos, overheadBytes;

	// Current visit
	private String state;
	private long visit, visitStart, recordStart, lastFrameEnd, gcCountStart, gcTimeStart;
	private long frameCalls, frameNanos, frameBytes, frameThreadBytesStart;
	private long frames, totalCalls, totalNanos, totalBytes, totalThreadBytes, maxFrameCalls;
	private long[] luaNanosPerFrame = new long[4096], intervalPerFrame = new long[4096];
	private int samples;
	private final Map<String, Long> visits = new LinkedHashMap<>();

	private static LuaCallDiagnostics create() {
		String value = System.getProperty("beatoraja.luaDiagnostics", "");
		Mode mode = switch (value.toLowerCase(Locale.ROOT)) {
			case "count" -> Mode.COUNT;
			case "true", "time" -> Mode.TIME;
			case "alloc" -> Mode.ALLOC;
			default -> null;
		};
		if (mode == null) return null;
		long warmup = Math.max(0, Math.min(600_000, Long.getLong("beatoraja.luaDiagnostics.warmupMillis", 5000L)));
		String out = System.getProperty("beatoraja.luaDiagnostics.output", "");
		Path dir;
		try { dir = out.isBlank() ? Path.of("luadiag") : Path.of(out); }
		catch (RuntimeException e) { dir = Path.of("luadiag"); }
		return new LuaCallDiagnostics(mode, warmup * 1_000_000L, dir);
	}

	LuaCallDiagnostics(Mode mode, long warmupNanos, Path output) {
		var bean = ManagementFactory.getThreadMXBean();
		threads = bean instanceof com.sun.management.ThreadMXBean t && t.isThreadAllocatedMemorySupported() ? t : null;
		if (threads != null) threads.setThreadAllocatedMemoryEnabled(true);
		this.mode = mode == Mode.ALLOC && threads == null ? Mode.TIME : mode;
		this.warmupNanos = warmupNanos; this.output = output;
		Logger.getGlobal().info("Lua call diagnostics enabled: vm=" + VM + ", mode=" + this.mode + ", warmupMs=" + warmupNanos / 1_000_000
				+ ", output=" + output.toAbsolutePath());
	}

	public static boolean isEnabled() { return INSTANCE != null; }

	/** Returns null when diagnostics are disabled. Call at load time, never per frame. */
	public static Site site(String kind, String label) {
		return INSTANCE == null ? null : INSTANCE.register(kind, label);
	}

	public static Site site(String kind, LuaFunction function) {
		return INSTANCE == null ? null : INSTANCE.register(kind, describe(function));
	}

	static String describe(LuaFunction function) {
		if (function instanceof LuaClosure closure)
			return "function " + closure.p.source.tojstring() + ":" + closure.p.linedefined;
		return "java " + function.getClass().getName();
	}

	synchronized Site register(String kind, String label) {
		String compact = label == null ? "" : label.replace('\n', ' ').replace('\r', ' ').strip();
		return sites.computeIfAbsent(kind + '\0' + compact, k -> new Site(kind, compact));
	}

	/** Start token for {@link #end}; negative when this call is not recorded. */
	public static long begin(Site site) {
		return site == null ? -1 : INSTANCE.start();
	}

	public static void end(Site site, long token) {
		if (token >= 0) INSTANCE.stop(site, token);
	}

	long start() {
		if (!recording || Thread.currentThread() != owner) {
			if (owner != null && Thread.currentThread() != owner) offThreadCalls.incrementAndGet();
			return -1;
		}
		if (depth++ == 0 && mode == Mode.ALLOC) allocAtStart = threads.getCurrentThreadAllocatedBytes();
		return mode == Mode.COUNT ? 0 : System.nanoTime();
	}

	void stop(Site site, long started) {
		long elapsed = mode == Mode.COUNT ? 0 : System.nanoTime() - started;
		site.calls++;
		site.nanos += elapsed;
		if (elapsed > site.maxNanos) site.maxNanos = elapsed;
		if (--depth == 0) {
			frameCalls++;
			frameNanos += elapsed;
			if (mode == Mode.ALLOC) {
				long b = threads.getCurrentThreadAllocatedBytes() - allocAtStart;
				site.bytes += b;
				frameBytes += b;
			}
		}
	}

	/** Called from the state-change path before the old state shuts down. */
	public static void stateChanging() {
		if (INSTANCE != null && Thread.currentThread() == INSTANCE.owner) INSTANCE.finishVisit();
	}

	/** Called once per rendered frame on the render thread, after skin objects were drawn. */
	public static void frameRendered(String state) {
		if (INSTANCE != null) INSTANCE.frame(state);
	}

	public static void shutdown() {
		if (INSTANCE != null && Thread.currentThread() == INSTANCE.owner) INSTANCE.finishVisit();
	}

	void frame(String current) {
		long now = System.nanoTime();
		if (owner == null) { owner = Thread.currentThread(); calibrate(); }
		if (Thread.currentThread() != owner) return;
		if (!current.equals(state)) {
			finishVisit();
			state = current; visit = visits.merge(current, 1L, Long::sum);
			visitStart = now; recordStart = 0; lastFrameEnd = 0;
		}
		if (!recording) {
			if (now - visitStart >= warmupNanos) startRecording(now);
			return;
		}
		long threadBytes = threads != null ? threads.getCurrentThreadAllocatedBytes() : 0;
		if (lastFrameEnd != 0) {
			if (samples == luaNanosPerFrame.length && samples < MAX_FRAME_SAMPLES) {
				int n = Math.min(MAX_FRAME_SAMPLES, samples * 2);
				luaNanosPerFrame = Arrays.copyOf(luaNanosPerFrame, n);
				intervalPerFrame = Arrays.copyOf(intervalPerFrame, n);
			}
			if (samples < luaNanosPerFrame.length) {
				luaNanosPerFrame[samples] = mode == Mode.COUNT ? frameCalls : frameNanos;
				intervalPerFrame[samples] = now - lastFrameEnd;
				samples++;
			}
			frames++;
			totalCalls += frameCalls; totalNanos += frameNanos; totalBytes += frameBytes;
			totalThreadBytes += threadBytes - frameThreadBytesStart;
			if (frameCalls > maxFrameCalls) maxFrameCalls = frameCalls;
		}
		lastFrameEnd = now;
		frameThreadBytesStart = threadBytes;
		frameCalls = frameNanos = frameBytes = 0;
	}

	private void startRecording(long now) {
		synchronized (this) {
			for (Site s : sites.values()) s.calls = s.nanos = s.maxNanos = s.bytes = 0;
		}
		offThreadCalls.set(0);
		frames = totalCalls = totalNanos = totalBytes = totalThreadBytes = maxFrameCalls = 0;
		frameCalls = frameNanos = frameBytes = 0;
		samples = 0; depth = 0;
		long[] gc = gc(); gcCountStart = gc[0]; gcTimeStart = gc[1];
		// This frame boundary opens the first recorded frame.
		recordStart = now; lastFrameEnd = now;
		frameThreadBytesStart = threads != null ? threads.getCurrentThreadAllocatedBytes() : 0;
		recording = true;
	}

	/** Measures the cost of an empty begin/end pair so reports can show it next to ns/call. */
	private void calibrate() {
		Site dummy = new Site("calibration", "");
		boolean was = recording;
		recording = true;
		long bytes0 = threads != null ? threads.getCurrentThreadAllocatedBytes() : 0;
		long best = Long.MAX_VALUE;
		for (int round = 0; round < 5; round++) {
			long t0 = System.nanoTime();
			for (int i = 0; i < 100_000; i++) stop(dummy, start());
			best = Math.min(best, System.nanoTime() - t0);
		}
		long bytes1 = threads != null ? threads.getCurrentThreadAllocatedBytes() : 0;
		recording = was;
		overheadNanos = best / 100_000.0;
		overheadBytes = mode == Mode.ALLOC ? (double) dummy.bytes / dummy.calls : 0;
		frameCalls = frameNanos = frameBytes = 0;
		depth = 0;
		Logger.getGlobal().info(String.format(Locale.ROOT,
				"Lua call diagnostics overhead: %.1f ns/call (wall, measured pair), %.1f B/call, calibration alloc %d B",
				overheadNanos, overheadBytes, bytes1 - bytes0));
	}

	void finishVisit() {
		if (!recording) return;
		recording = false;
		long elapsed = System.nanoTime() - recordStart;
		long[] gc = gc();
		List<Site> top;
		synchronized (this) {
			top = new ArrayList<>();
			for (Site s : sites.values()) if (s.calls > 0) top.add(s);
		}
		top.sort((a, b) -> mode == Mode.COUNT ? Long.compare(b.calls, a.calls) : Long.compare(b.nanos, a.nanos));
		String report = report(elapsed, gc[0] - gcCountStart, gc[1] - gcTimeStart, top);
		Logger.getGlobal().info(report);
		write("lua-calls-" + run + ".txt", null, report + "\n");
		StringBuilder csv = new StringBuilder();
		for (Site s : top) {
			csv.append(csv(run)).append(',').append(csv(state)).append(',').append(visit).append(',').append(frames)
					.append(',').append(csv(s.kind)).append(',').append(s.calls).append(',').append(s.nanos)
					.append(',').append(s.maxNanos).append(',').append(s.bytes).append(',').append(csv(s.label)).append('\n');
		}
		write("lua-sites-" + run + ".csv", "run,state,visit,frames,kind,calls,total_ns,max_ns,bytes,label\n", csv.toString());
	}

	private String report(long elapsed, long gcCount, long gcMs, List<Site> top) {
		StringBuilder r = new StringBuilder();
		double f = Math.max(1, frames);
		long[] lua = Arrays.copyOf(luaNanosPerFrame, samples), interval = Arrays.copyOf(intervalPerFrame, samples);
		Arrays.sort(lua); Arrays.sort(interval);
		r.append(String.format(Locale.ROOT, "=== Lua call diagnostics: state=%s visit=%d mode=%s ===%nvm %s, java %s, gc %s%n",
				state, visit, mode, VM, System.getProperty("java.runtime.version"), gcNames()));
		r.append(String.format(Locale.ROOT, "recorded %.1f s, %d frames (%.1f fps), GC %d collections / %d ms%n",
				elapsed / 1e9, frames, frames / Math.max(1e-9, elapsed / 1e9), gcCount, gcMs));
		r.append(String.format(Locale.ROOT, "frame interval ms: p50 %.3f  p95 %.3f  p99 %.3f  max %.3f%n",
				pct(interval, .50) / 1e6, pct(interval, .95) / 1e6, pct(interval, .99) / 1e6, pct(interval, 1) / 1e6));
		r.append(String.format(Locale.ROOT, "lua calls/frame: mean %.1f  max %d  (off-render-thread calls: %d)%n",
				totalCalls / f, maxFrameCalls, offThreadCalls.get()));
		if (mode == Mode.COUNT) {
			r.append(String.format(Locale.ROOT, "lua calls/frame: p50 %d  p95 %d  p99 %d%n",
					pct(lua, .50), pct(lua, .95), pct(lua, .99)));
		} else {
			double luaMean = totalNanos / f;
			r.append(String.format(Locale.ROOT, "lua time/frame us: mean %.1f  p50 %.1f  p95 %.1f  p99 %.1f  max %.1f%n",
					luaMean / 1e3, pct(lua, .50) / 1e3, pct(lua, .95) / 1e3, pct(lua, .99) / 1e3, pct(lua, 1) / 1e3));
			r.append(String.format(Locale.ROOT, "lua share of frame interval (mean): %.2f %%; measurement overhead ~%.1f ns/call (~%.1f us/frame)%n",
					100.0 * totalNanos / Math.max(1, elapsed), overheadNanos, overheadNanos * totalCalls / f / 1e3));
		}
		if (threads != null) {
			r.append(String.format(Locale.ROOT, "render thread alloc/frame: %.0f B", totalThreadBytes / f));
			if (mode == Mode.ALLOC)
				r.append(String.format(Locale.ROOT, "; inside lua calls: %.0f B (%.1f %%), overhead ~%.0f B/call",
						totalBytes / f, 100.0 * totalBytes / Math.max(1, totalThreadBytes), overheadBytes));
			r.append(String.format(Locale.ROOT, " -> %.2f MB/s%n", totalThreadBytes / Math.max(1e-9, elapsed / 1e9) / 1e6));
		}
		r.append(String.format(Locale.ROOT, "top %d of %d active sites (inclusive):%n", Math.min(TOP_SITES, top.size()), top.size()));
		r.append(String.format(Locale.ROOT, "%10s %9s %9s %9s %8s  %-8s %s%n", "calls", "calls/fr", "total ms", "ns/call", "B/call", "kind", "label"));
		for (int i = 0; i < Math.min(TOP_SITES, top.size()); i++) {
			Site s = top.get(i);
			String label = s.label.length() > 140 ? s.label.substring(0, 137) + "..." : s.label;
			// Bytes are only measured in alloc mode; do not print a misleading 0.
			String bytes = mode == Mode.ALLOC ? String.format(Locale.ROOT, "%.1f", (double) s.bytes / s.calls) : "-";
			r.append(String.format(Locale.ROOT, "%10d %9.2f %9.2f %9.1f %8s  %-8s %s%n", s.calls, s.calls / f,
					s.nanos / 1e6, (double) s.nanos / s.calls, bytes, s.kind, label));
		}
		return r.toString();
	}

	private static long pct(long[] sorted, double p) {
		int n = sorted.length;
		if (n == 0) return 0;
		return sorted[p >= 1 ? n - 1 : Math.max(0, (int) Math.ceil(p * n) - 1)];
	}

	private static String gcNames() {
		StringBuilder b = new StringBuilder();
		for (GarbageCollectorMXBean c : ManagementFactory.getGarbageCollectorMXBeans())
			b.append(b.length() == 0 ? "" : "+").append(c.getName());
		return b.toString();
	}

	private static long[] gc() {
		long count = 0, time = 0;
		for (GarbageCollectorMXBean c : ManagementFactory.getGarbageCollectorMXBeans()) {
			count += Math.max(0, c.getCollectionCount());
			time += Math.max(0, c.getCollectionTime());
		}
		return new long[] {count, time};
	}

	private void write(String name, String header, String text) {
		if (outputFailed) return;
		try {
			Files.createDirectories(output);
			Path file = output.resolve(name);
			boolean fresh = !Files.exists(file);
			try (var w = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
				if (fresh && header != null) w.write(header);
				w.write(text);
			}
		} catch (IOException | RuntimeException e) {
			outputFailed = true;
			Logger.getGlobal().warning("Lua call diagnostics output disabled: " + e.getClass().getSimpleName());
		}
	}

	private static String csv(String v) { return "\"" + v.replace("\"", "\"\"") + "\""; }
}
