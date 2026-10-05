package bms.player.beatoraja;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.LinkedHashMap;
import java.util.Map;
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
	private record Category(int count, long bytes, String details) {
	}

	private final Map<String, Category> previous = new LinkedHashMap<>();

	public void capture(MainController main, String context) {
		if (!main.getConfig().isMemoryDiagnostics()) {
			return;
		}

		Map<String, Category> current = new LinkedHashMap<>();
		collectJvm(current);
		collectPools(main, current);
		log(context, current);
		previous.clear();
		previous.putAll(current);
	}

	private static void collectJvm(Map<String, Category> categories) {
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

		for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
			if ("direct".equals(pool.getName())) {
				categories.put("jvm-direct-buffers", new Category((int) pool.getCount(), pool.getMemoryUsed(),
						"capacity=" + formatBytes(pool.getTotalCapacity())));
			}
		}
	}

	private static void collectPools(MainController main, Map<String, Category> categories) {
		addPixmapPool(categories, "skin-pixmaps", SkinLoader.getResource().getMemoryStatistics());

		MusicSelector selector = main.getMusicSelector();
		if (selector != null) {
			addPixmapPool(categories, "select-banners", selector.getBannerResource().getMemoryStatistics());
			addPixmapPool(categories, "select-stagefiles", selector.getStagefileResource().getMemoryStatistics());
		}

		BMSResource bmsResource = main.getPlayerResource().getBMSResource();
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

	private void log(String context, Map<String, Category> current) {
		Logger logger = Logger.getGlobal();
		logger.info("Memory diagnostics [" + context + "]");
		for (Map.Entry<String, Category> entry : current.entrySet()) {
			Category value = entry.getValue();
			Category old = previous.get(entry.getKey());
			String delta = old == null ? "" : " (delta=" + formatSignedBytes(value.bytes - old.bytes)
					+ ", count=" + formatSigned(value.count - old.count) + ")";
			String details = value.details.isEmpty() ? "" : ", " + value.details;
			logger.info("  " + entry.getKey() + ": count=" + value.count + ", estimated="
					+ formatBytes(value.bytes) + delta + details);
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
