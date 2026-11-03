package bms.player.beatoraja;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/** Regression tests for concurrent resource loading and eviction. */
public final class ResourcePoolTest {
    public static void main(String[] args) throws Exception {
        assertSingleLoad();
        assertParallelLoads();
        assertEvictionWaitsForLoad(false);
        assertEvictionWaitsForLoad(true);
        assertGenerationsAndRetry();
        assertResourceFactory();
        System.out.println("ResourcePoolTest passed");
    }

    private static void assertResourceFactory() {
        var pool = new TestPool(0);
        Resource resource = pool.get("archive-key", Resource::new);
        check(pool.get("archive-key", () -> {
            throw new AssertionError("Cache hit must not invoke the resource factory");
        }) == resource, "Resource-backed loads must use the same cache");
        check(pool.loads.get() == 0, "Resource-backed loads must bypass path loading");
        pool.disposeOld();
        check(resource.disposals.get() == 1, "Resource-backed loads must be evicted normally");
    }

    private static void assertSingleLoad() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        var pool = new TestPool(1) {
            @Override
            protected Resource load(String key) {
                Resource resource = super.load(key);
                entered.countDown();
                await(finish);
                return resource;
            }
        };
        var executor = Executors.newFixedThreadPool(8);
        try {
            var results = new ArrayList<Future<Resource>>();
            for (int i = 0; i < 8; i++) {
                results.add(executor.submit(() -> pool.get("same")));
            }
            await(entered);
            finish.countDown();
            Resource first = results.get(0).get(5, TimeUnit.SECONDS);
            for (Future<Resource> result : results) {
                check(result.get(5, TimeUnit.SECONDS) == first, "Same key must share one resource");
            }
            check(pool.loads.get() == 1, "Same key must be loaded only once");
            pool.dispose();
            pool.dispose();
            check(first.disposals.get() == 1, "Resource must be disposed exactly once");
        } finally {
            finish.countDown();
            executor.shutdownNow();
        }
    }

    private static void assertParallelLoads() throws Exception {
        CountDownLatch entered = new CountDownLatch(2);
        var pool = new TestPool(1) {
            @Override
            protected Resource load(String key) {
                entered.countDown();
                await(entered);
                return super.load(key);
            }
        };
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<Resource> first = executor.submit(() -> pool.get("first"));
            Future<Resource> second = executor.submit(() -> pool.get("second"));
            check(first.get(5, TimeUnit.SECONDS) != second.get(5, TimeUnit.SECONDS),
                    "Different keys must load concurrently");
            pool.dispose();
        } finally {
            executor.shutdownNow();
        }
    }

    private static void assertEvictionWaitsForLoad(boolean oldOnly) throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        CountDownLatch evictionStarted = new CountDownLatch(1);
        var pool = new TestPool(0) {
            @Override
            protected Resource load(String key) {
                Resource resource = super.load(key);
                entered.countDown();
                await(finish);
                return resource;
            }
        };
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<Resource> loading = executor.submit(() -> pool.get("loading"));
            await(entered);
            Future<?> eviction = executor.submit(() -> {
                evictionStarted.countDown();
                if (oldOnly) {
                    pool.disposeOld();
                } else {
                    pool.dispose();
                }
            });
            await(evictionStarted);
            try {
                eviction.get(100, TimeUnit.MILLISECONDS);
                throw new AssertionError("Eviction must wait for resource registration");
            } catch (TimeoutException expected) {
            }
            finish.countDown();
            Resource resource = loading.get(5, TimeUnit.SECONDS);
            eviction.get(5, TimeUnit.SECONDS);
            check(pool.size() == 0, "In-flight resource must not survive eviction");
            check(resource.disposals.get() == 1, "In-flight resource must be disposed once");
            pool.dispose();
            check(resource.disposals.get() == 1, "Repeated eviction must not double-dispose");
        } finally {
            finish.countDown();
            executor.shutdownNow();
        }
    }

    private static void assertGenerationsAndRetry() {
        var pool = new TestPool(1);
        Resource resource = pool.get("generation");
        pool.disposeOld();
        check(pool.get("generation") == resource, "Cache hit must reset the generation");
        pool.disposeOld();
        check(resource.disposals.get() == 0, "Recently used resource must survive");
        pool.disposeOld();
        check(resource.disposals.get() == 1 && !pool.exists("generation"),
                "Expired resource must be removed and disposed");
        Resource replacement = pool.get("generation");
        check(replacement != resource, "Expired resource must be reloaded");
        check(pool.getStatistics(value -> 16).estimatedBytes() == 16, "Statistics must reflect live entries");
        pool.dispose();
        check(replacement.disposals.get() == 1, "Reloaded resource must be disposed");

        var retryPool = new TestPool(1) {
            @Override
            protected Resource load(String key) {
                int attempt = loads.incrementAndGet();
                if (attempt == 1) {
                    return null;
                }
                if (attempt == 2) {
                    throw new IllegalStateException("Expected test failure");
                }
                return new Resource();
            }
        };
        check(retryPool.get("retry") == null, "Null loads must not be cached");
        try {
            retryPool.get("retry");
            throw new AssertionError("Load exception must propagate");
        } catch (IllegalStateException expected) {
        }
        check(retryPool.get("retry") != null, "Failed loads must be retryable");
        retryPool.dispose();
    }

    private static void await(CountDownLatch latch) {
        try {
            check(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for concurrent operation");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static class TestPool extends ResourcePool<String, Resource> {
        final AtomicInteger loads = new AtomicInteger();

        TestPool(int generations) {
            super(generations);
        }

        @Override
        protected Resource load(String key) {
            loads.incrementAndGet();
            return new Resource();
        }

        @Override
        protected void dispose(Resource resource) {
            resource.disposals.incrementAndGet();
        }
    }

    private static final class Resource {
        final AtomicInteger disposals = new AtomicInteger();
    }
}
