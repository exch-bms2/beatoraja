package bms.player.beatoraja.skin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.utils.GdxNativesLoader;

import bms.player.beatoraja.PixmapResourcePool;

/**
 * Skin image decode throughput by worker count, without GL.
 *
 * Usage: SkinImageDecodeBenchmark threads=1,2,4,8 rounds=5 &lt;image-list-file&gt;...
 * Each list file holds one image path per line (one skin load). For every thread count it reports the median
 * wall time to decode each list, and how long a fixed CPU task on the calling thread takes while the
 * decodes run (the render thread keeps building skin objects during prefetch), relative to running alone.
 */
public final class SkinImageDecodeBenchmark {

	private SkinImageDecodeBenchmark() {
	}

	public static void main(String[] args) throws Exception {
		GdxNativesLoader.load();
		Gdx.files = new Lwjgl3Files();
		int[] threads = { 1, 2, 4, 6, 8 };
		int rounds = 5;
		List<Path> lists = new ArrayList<>();
		for (String arg : args) {
			if (arg.startsWith("threads=")) {
				threads = Arrays.stream(arg.substring(8).split(",")).mapToInt(Integer::parseInt).toArray();
			} else if (arg.startsWith("rounds=")) {
				rounds = Integer.parseInt(arg.substring(7));
			} else {
				lists.add(Path.of(arg));
			}
		}
		List<List<String>> sets = new ArrayList<>();
		for (Path list : lists) {
			sets.add(Files.readAllLines(list).stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
		}

		// warm up decoder and JIT
		for (int i = 0; i < 3; i++) {
			for (List<String> set : sets) {
				decodeAll(set, 2, true);
			}
		}
		System.out.printf("processors=%d  main-thread task alone: %.1f ms%n",
				Runtime.getRuntime().availableProcessors(), medianNanos(rounds, () -> cpuTask()) / 1e6);
		System.out.printf("%-8s", "threads");
		for (Path list : lists) {
			System.out.printf("  %22s", list.getFileName());
		}
		System.out.printf("  %18s%n", "main task slowdown");
		for (int n : threads) {
			System.out.printf("%-8d", n);
			double slowdownSum = 0;
			for (List<String> set : sets) {
				long[] wall = new long[rounds];
				double[] slow = new double[rounds];
				for (int r = 0; r < rounds; r++) {
					long alone = medianNanos(1, () -> cpuTask());
					long[] result = decodeAll(set, n, n > 1);
					wall[r] = result[0];
					slow[r] = n > 1 ? (double) result[1] / alone : 1.0;
				}
				Arrays.sort(wall);
				Arrays.sort(slow);
				slowdownSum += slow[rounds / 2];
				System.out.printf("  %15.1f ms (%2d)", wall[rounds / 2] / 1e6, set.size());
			}
			System.out.printf("  %17.2fx%n", slowdownSum / sets.size());
		}
	}

	/** Returns {wall nanos for all decodes, nanos of the concurrent main-thread task (0 if none)}. */
	private static long[] decodeAll(List<String> paths, int threads, boolean withMainTask) throws Exception {
		final long start = System.nanoTime();
		long mainTask = 0;
		if (threads <= 1) {
			for (String path : paths) {
				dispose(PixmapResourcePool.loadPictureResource(path));
			}
			long wall = System.nanoTime() - start;
			return new long[] { wall, mainTask };
		}
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			List<Future<?>> futures = new ArrayList<>();
			for (String path : paths) {
				futures.add(pool.submit(() -> dispose(PixmapResourcePool.loadPictureResource(path))));
			}
			if (withMainTask) {
				long t = System.nanoTime();
				cpuTask();
				mainTask = System.nanoTime() - t;
			}
			for (Future<?> f : futures) {
				f.get();
			}
			return new long[] { System.nanoTime() - start, mainTask };
		} finally {
			pool.shutdown();
		}
	}

	private static void dispose(PixmapResourcePool.PixmapResource resource) {
		if (resource != null) {
			resource.dispose();
		}
	}

	private static volatile long sink;

	/** About 100 ms of allocation-heavy work, roughly like building skin objects. */
	private static void cpuTask() {
		long acc = 0;
		for (int i = 0; i < 1_500_000; i++) {
			StringBuilder sb = new StringBuilder();
			sb.append("dst").append(i).append('_').append(i * 31);
			acc += sb.toString().hashCode();
			int[] a = new int[16];
			a[i & 15] = i;
			acc += a[(i * 7) & 15];
		}
		sink = acc;
	}

	private static long medianNanos(int rounds, Runnable task) {
		long[] t = new long[rounds];
		for (int i = 0; i < rounds; i++) {
			long s = System.nanoTime();
			task.run();
			t[i] = System.nanoTime() - s;
		}
		Arrays.sort(t);
		return t[rounds / 2];
	}
}
