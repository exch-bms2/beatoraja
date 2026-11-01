package bms.player.beatoraja.select;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.ir.IRTableData;

/** Fetches optional IR metadata without touching UI objects or waiting on the render thread. */
final class IRTableLoader implements AutoCloseable {
    private final AtomicReference<IRTableData[]> pending = new AtomicReference<>();
    private ExecutorService executor;
    private Future<?> task;
    private boolean closed;

    synchronized void start(IRConnection connection) {
        if (closed || executor != null) return;
        executor = Executors.newSingleThreadExecutor(work -> {
            Thread thread = new Thread(work, "ir-table-loader"); thread.setDaemon(true); return thread;
        });
        task = executor.submit(() -> {
            try {
                var response = connection.getTableDatas();
                if (response == null || !response.isSucceeded()) {
                    Logger.getGlobal().warning("IRからのテーブル取得失敗 : " + (response == null ? "null response" : response.getMessage()));
                    return;
                }
                IRTableData[] tables = response.getData();
                synchronized (IRTableLoader.this) {
                    if (!closed) pending.set(tables != null ? tables.clone() : new IRTableData[0]);
                }
            } catch (Exception | LinkageError failure) {
                Logger.getGlobal().warning("IRからのテーブル取得失敗 : " + failure.getClass().getSimpleName());
            } finally {
                synchronized (IRTableLoader.this) { executor.shutdown(); }
            }
        });
    }

    IRTableData[] take() { return pending.get() == null ? null : pending.getAndSet(null); }

    @Override public synchronized void close() {
        closed = true; pending.set(null);
        if (task != null) task.cancel(true);
        if (executor != null) executor.shutdownNow();
    }
}
