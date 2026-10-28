package bms.player.beatoraja;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.logging.Logger;

import bms.player.beatoraja.play.bga.BGAProcessor;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.skin.BitmapFontCache;
import bms.player.beatoraja.skin.SkinLoader;
import bms.player.beatoraja.song.SongResources;

/**
 * Low-frequency memory summaries for diagnosing resource growth across main-state transitions.
 * Values outside the Java heap are estimates based on resource dimensions and retained byte arrays.
 */
public final class MemoryDiagnostics {
	public record Category(int count, long bytes, String details) {
		public Category { details = details == null ? "" : details; }
	}
	public record Snapshot(Map<String, Category> categories, long gcCount, long gcTimeMs) {
		public Snapshot { categories = Collections.unmodifiableMap(new LinkedHashMap<>(categories)); }
	}
	private static final String HEADER = "run,sample,context,event,iteration,elapsed_ns,transition_ns,first_frame_ns,gc_count,gc_time_ms,category,count,bytes,same_state_count_delta,same_state_bytes_delta,details\n";

	private final Map<String, Category> previous = new LinkedHashMap<>();
	private final Map<String, Map<String, Category>> lastByState = new LinkedHashMap<>();
	private final Map<String, Long> visits = new LinkedHashMap<>();
	private final Path output;
	private final long started = System.nanoTime();
	private final String run = Long.toHexString(System.currentTimeMillis()) + "-" + Long.toHexString(started);
	private long sequence, pendingStart, pendingTransition;
	private String pendingState;
	private String settlingState;
	private long settleAt, firstFrameNanos;
	private final long settleNanos;
	private boolean outputFailed;
	private boolean outputChecked;

	public MemoryDiagnostics() { this(outputPath()); }
	public MemoryDiagnostics(Path output) { this(output, Long.getLong("beatoraja.memoryDiagnostics.settleMillis", 5000)); }
	public MemoryDiagnostics(Path output, long settleMillis) {
		this.output = output; settleNanos = Math.max(0, Math.min(600000, settleMillis)) * 1_000_000;
	}
	private static Path outputPath() {
		String value = System.getProperty("beatoraja.memoryDiagnostics.output", "");
		try { return value.isBlank() ? null : Path.of(value); }
		catch (RuntimeException e) { Logger.getGlobal().warning("Invalid memory diagnostics output path"); return null; }
	}

	public void capture(MainController main, String context) {
		if (!main.getConfig().isMemoryDiagnostics()) {
			return;
		}

		record(context, "sample", nextVisit(context), -1, -1, sample(main));
	}

	/** Measurement starts before state construction/disposal, not only before create(). */
	public void transitionComplete(MainController main, String context, long startNanos) {
		if (!main.getConfig().isMemoryDiagnostics()) return;
		pendingState = context; pendingStart = startNanos;
		settlingState = context; settleAt = System.nanoTime() + settleNanos; firstFrameNanos = -1;
		pendingTransition = Math.max(0, System.nanoTime() - startNanos);
		record(context, "transition", nextVisit(context), pendingTransition, -1, sample(main));
	}

	/** First CPU draw completion, not a GPU present or proof that asynchronous assets are ready. */
	public void frameRendered(MainController main, String context) {
		if (!context.equals(pendingState) && !context.equals(settlingState)) return;
		if (!main.getConfig().isMemoryDiagnostics()) { pendingState = settlingState = null; return; }
		long now = System.nanoTime();
		if (context.equals(pendingState)) {
			pendingState = null; firstFrameNanos = Math.max(0, now - pendingStart);
			record(context, "first-frame", visits.getOrDefault(context, 0L), pendingTransition, firstFrameNanos, sample(main));
		}
		if (context.equals(settlingState) && now >= settleAt) {
			settlingState = null;
			record(context, "settled", visits.getOrDefault(context, 0L), pendingTransition, firstFrameNanos, sample(main));
		}
	}

	private long nextVisit(String context) {
		if (!visits.containsKey(context) && visits.size() >= 64) visits.remove(visits.keySet().iterator().next());
		return visits.merge(context, 1L, Long::sum);
	}

	/** Low-frequency snapshot for the standalone endurance harness; no MainController or GL context needed. */
	public static Snapshot sample() { return sample(null); }
	private static Snapshot sample(MainController main) {
		Map<String, Category> current = new LinkedHashMap<>();
		long[] gc = collectJvm(current);
		collectPools(main, current);
		return new Snapshot(current, gc[0], gc[1]);
	}

	/** Export one opt-in diagnostic sample. Callers must use fixed state/event names, not song identifiers. */
	public void record(String context, String event, long iteration, long transitionNanos, long firstFrameNanos, Snapshot snapshot) {
		String key = context + '\0' + event;
		Map<String, Category> sameState = lastByState.get(key);
		log(context, snapshot.categories(), sameState);
		Logger.getGlobal().info("  event=" + event + ", iteration=" + iteration + ", transitionNs=" + transitionNanos
				+ ", firstFrameNs=" + firstFrameNanos);
		writeCsv(context, event, iteration, transitionNanos, firstFrameNanos, snapshot, sameState);
		previous.clear(); previous.putAll(snapshot.categories());
		if (!lastByState.containsKey(key) && lastByState.size() >= 64) lastByState.remove(lastByState.keySet().iterator().next());
		lastByState.put(key, snapshot.categories());
	}

	private void writeCsv(String context, String event, long iteration, long transition, long firstFrame,
			Snapshot snapshot, Map<String, Category> sameState) {
		long sample = ++sequence;
		if (output == null || outputFailed) return;
		try {
			Path parent = output.toAbsolutePath().getParent();
			if (parent != null) Files.createDirectories(parent);
			boolean header = !Files.exists(output) || Files.size(output) == 0;
			if (!header && !outputChecked) {
				try (var reader = Files.newBufferedReader(output, StandardCharsets.UTF_8)) {
					if (!HEADER.strip().equals(reader.readLine())) throw new IOException("Incompatible diagnostics CSV header");
				}
			}
			outputChecked = true;
			long elapsed = System.nanoTime() - started;
			try (var writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
				if (header) writer.write(HEADER);
				for (var entry : snapshot.categories().entrySet()) {
					Category c = entry.getValue(), old = sameState == null ? null : sameState.get(entry.getKey());
					writer.write(csv(run) + "," + sample + "," + csv(context) + "," + csv(event) + "," + iteration + ","
							+ elapsed + "," + transition + "," + firstFrame + "," + snapshot.gcCount() + "," + snapshot.gcTimeMs()
							+ "," + csv(entry.getKey()) + "," + c.count() + "," + c.bytes() + ","
							+ (old == null ? "" : Integer.toString(c.count() - old.count())) + ","
							+ (old == null ? "" : Long.toString(c.bytes() - old.bytes())) + "," + csv(c.details()) + "\n");
				}
			}
		} catch (IOException | RuntimeException e) {
			outputFailed = true;
			Logger.getGlobal().warning("Memory diagnostics CSV disabled after output failure: " + e.getClass().getSimpleName());
		}
	}

	private static String csv(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }

	private static long[] collectJvm(Map<String, Category> categories) {
		MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
		long heapUsed = memory.getHeapMemoryUsage().getUsed();
		long heapCommitted = memory.getHeapMemoryUsage().getCommitted();
		long gcCount = 0;
		long gcTime = 0;
		for (GarbageCollectorMXBean collector : ManagementFactory.getGarbageCollectorMXBeans()) {
			gcCount += Math.max(0, collector.getCollectionCount());
			gcTime += Math.max(0, collector.getCollectionTime());
		}
		categories.put("jvm-heap", new Category(1, heapUsed,
				"committed=" + formatBytes(heapCommitted) + ", gc=" + gcCount + ", gcTimeMs=" + gcTime));
		categories.put("jvm-heap-committed", new Category(1, heapCommitted, "not resident memory"));
		categories.put("jvm-nonheap", new Category(1, memory.getNonHeapMemoryUsage().getUsed(), ""));

		for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
			if ("direct".equals(pool.getName()) || "mapped".equals(pool.getName())) {
				categories.put("direct".equals(pool.getName()) ? "jvm-direct-buffers" : "jvm-mapped-buffers", new Category((int) pool.getCount(), pool.getMemoryUsed(),
						"capacity=" + formatBytes(pool.getTotalCapacity())));
			}
		}
		return new long[] {gcCount, gcTime};
	}

	private static void collectPools(MainController main, Map<String, Category> categories) {
		if (SkinLoader.getResource() != null) addPixmapPool(categories, "skin-pixmaps", SkinLoader.getResource().getMemoryStatistics());

		MusicSelector selector = main == null ? null : main.getMusicSelector();
		if (selector != null) {
			addPixmapPool(categories, "select-banners", selector.getBannerResource().getMemoryStatistics());
			addPixmapPool(categories, "select-stagefiles", selector.getStagefileResource().getMemoryStatistics());
		}

		BMSResource bmsResource = main == null || main.getPlayerResource() == null ? null : main.getPlayerResource().getBMSResource();
		if (bmsResource != null && bmsResource.getBGAProcessor() != null) {
			BGAProcessor.MemoryStatistics bga = bmsResource.getBGAProcessor().getMemoryStatistics();
			var images = bga.images();
			categories.put("bga-images", new Category(images.pixmapCount() + images.textureCount(),
					images.estimatedPixmapBytes() + images.estimatedTextureBytes(),
					"pixmaps=" + images.pixmapCount() + ", textures=" + images.textureCount()
							+ ", songResources=" + images.songResourceCount()));
			categories.put("bga-movies", new Category(bga.movieCount(), bga.retainedMovieBytes(),
					"songResources=" + bga.movieResourceCount()));
		}

		SongResources.MaterializedStatistics materialized = SongResources.getMaterializedStatistics();
		categories.put("song-resource-materialized", new Category(materialized.fileCount(), materialized.totalBytes(), ""));

		BitmapFontCache.Statistics fonts = BitmapFontCache.getStatistics();
		categories.put("bitmap-fonts", new Category(fonts.entryCount(), fonts.estimatedTextureBytes(),
				"references=" + fonts.referenceCount()));
	}

	private static void addPixmapPool(Map<String, Category> categories, String name,
			PixmapResourcePool.MemoryStatistics pool) {
		categories.put(name, new Category(pool.pixmapCount(), pool.estimatedNativeBytes(),
				"songResources=" + pool.songResourceCount()));
	}

	private void log(String context, Map<String, Category> current, Map<String, Category> sameState) {
		Logger logger = Logger.getGlobal();
		logger.info("Memory diagnostics [" + context + "]");
		for (Map.Entry<String, Category> entry : current.entrySet()) {
			Category value = entry.getValue();
			Category old = previous.get(entry.getKey());
			String delta = old == null ? "" : " (delta=" + formatSignedBytes(value.bytes - old.bytes)
					+ ", count=" + formatSigned(value.count - old.count) + ")";
			String details = value.details.isEmpty() ? "" : ", " + value.details;
			Category priorState = sameState == null ? null : sameState.get(entry.getKey());
			String same = priorState == null ? "" : ", sameStateDelta=" + formatSignedBytes(value.bytes - priorState.bytes)
					+ ", sameStateCount=" + formatSigned(value.count - priorState.count);
			logger.info("  " + entry.getKey() + ": count=" + value.count + ", estimated="
					+ formatBytes(value.bytes) + delta + same + details);
		}
	}

	private static String formatBytes(long bytes) {
		return String.format(java.util.Locale.ROOT, "%.1f MiB", bytes / 1024.0 / 1024.0);
	}

	private static String formatSignedBytes(long bytes) {
		return (bytes >= 0 ? "+" : "-") + formatBytes(Math.abs(bytes));
	}

	private static String formatSigned(int value) {
		return value >= 0 ? "+" + value : Integer.toString(value);
	}
}
