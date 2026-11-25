package bms.player.beatoraja.skin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * スキン画像を作業スレッドで先にデコードし、PixmapResourcePool に入れておく。
 * テクスチャ化(GL)は従来どおり描画スレッドで行い、その直前に await(path) で該当画像のデコード完了だけを待つ。
 *
 * Decodes skin images on worker threads ahead of time into the PixmapResourcePool. Texture upload
 * stays on the render thread, which calls await(path) just before it needs that image.
 * The pool owns every decoded Pixmap, so an image that is never used is aged out by disposeOld as before.
 * The owner must call awaitAll() before the load returns: a practice preview disposes its temporary pool
 * right after loading.
 */
public final class SkinImagePrefetcher {

	/** -Dbeatoraja.skinImageDecodeThreads=N overrides the worker count; 1 or less disables prefetching. */
	public static final String THREADS_PROPERTY = "beatoraja.skinImageDecodeThreads";

	static final int PARALLELISM = parallelism(Integer.getInteger(THREADS_PROPERTY, -1),
			Runtime.getRuntime().availableProcessors());

	static int parallelism(int requested, int processors) {
		if (requested >= 0) {
			return Math.min(16, requested);
		}
		// 3-4 本で頭打ち: 待ち時間は最大の 1 枚(ModernChic の mainframe.png など)のデコード時間で決まる。
		// Gains stop at 3-4 workers: the wait is bounded by the largest single image, so more threads only add contention.
		return Math.min(4, Math.max(1, processors - 1));
	}

	private static final class ExecutorHolder {
		static final ThreadPoolExecutor INSTANCE = createExecutor();

		private static ThreadPoolExecutor createExecutor() {
			AtomicInteger nextThreadId = new AtomicInteger(1);
			ThreadFactory factory = runnable -> {
				Thread thread = new Thread(runnable, "skin-image-decoder-" + nextThreadId.getAndIncrement());
				thread.setDaemon(true);
				return thread;
			};
			ThreadPoolExecutor executor = new ThreadPoolExecutor(PARALLELISM, PARALLELISM, 30L, TimeUnit.SECONDS,
					new LinkedBlockingQueue<>(), factory);
			executor.allowCoreThreadTimeOut(true);
			return executor;
		}
	}

	private static final SkinImagePrefetcher NONE = new SkinImagePrefetcher(Map.of());

	private final Map<String, Future<?>> pending;

	private SkinImagePrefetcher(Map<String, Future<?>> pending) {
		this.pending = pending;
	}

	/**
	 * 指定パスの画像のデコードを開始する。重複パス、プール済み、WebP(デコードに GL_MAX_TEXTURE_SIZE を参照するため
	 * 描画スレッドで行う)、存在しないファイルは対象外。
	 * Starts decoding the given image paths. Skips duplicates, images already pooled, WebP (its decoder reads
	 * GL_MAX_TEXTURE_SIZE, so it stays on the render thread) and missing files.
	 */
	public static SkinImagePrefetcher start(Collection<String> paths, boolean usecim) {
		if (paths.isEmpty() || PARALLELISM <= 1) {
			return NONE;
		}
		final SkinLoader.PixmapLoadContext context = SkinLoader.capturePixmapLoadContext(usecim);
		final Map<String, Future<?>> pending = new HashMap<>();
		for (String path : paths) {
			if (pending.containsKey(path) || !isPrefetchable(path) || context.resource.exists(path)
					|| !new java.io.File(path).isFile()) {
				continue;
			}
			// .cim の書き込みは非アトミックなので同一パスを二重に投げない(pending のキーで重複排除済み)
			pending.put(path, ExecutorHolder.INSTANCE.submit(() -> SkinLoader.loadPixmap(context, path)));
		}
		return pending.isEmpty() ? NONE : new SkinImagePrefetcher(pending);
	}

	static boolean isPrefetchable(String path) {
		final String lower = path.toLowerCase(Locale.ROOT);
		// WebP は対象外。画像以外(.chp など source に書かれうるもの)も投げない
		return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".bmp")
				|| lower.endsWith(".gif") || lower.endsWith(".tga") || lower.endsWith(".cim");
	}

	/** Number of images submitted for decoding. */
	public int size() {
		return pending.size();
	}

	/** Waits until the image at this path, if it was prefetched, is decoded and pooled. */
	public void await(String path) {
		if (pending.isEmpty()) {
			return;
		}
		final Future<?> future = pending.remove(path);
		if (future == null) {
			return;
		}
		var phase = SkinLoadEvents.begin("prefetch-wait");
		waitFor(future);
		SkinLoadEvents.end(phase, path);
	}

	/** Waits for every outstanding decode. Failures are left for the render thread to retry serially. */
	public void awaitAll() {
		if (pending.isEmpty()) {
			return;
		}
		var phase = SkinLoadEvents.begin("prefetch-wait-all");
		int n = pending.size();
		for (Future<?> future : pending.values()) {
			waitFor(future);
		}
		pending.clear();
		SkinLoadEvents.end(phase, null, n);
	}

	private static void waitFor(Future<?> future) {
		boolean interrupted = false;
		while (true) {
			try {
				future.get();
				break;
			} catch (InterruptedException e) {
				// 途中で戻ると作業スレッドが一時プールへ書き込み続けるため、完了までは待つ
				interrupted = true;
			} catch (ExecutionException e) {
				break;
			}
		}
		if (interrupted) {
			Thread.currentThread().interrupt();
		}
	}
}
