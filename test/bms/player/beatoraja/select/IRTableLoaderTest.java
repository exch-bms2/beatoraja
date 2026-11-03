package bms.player.beatoraja.select;

import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import bms.player.beatoraja.*;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import bms.player.beatoraja.ir.*;
import bms.player.beatoraja.select.bar.*;
import bms.player.beatoraja.song.*;

/** No window/native/network: delayed metadata, late disposal and real TableBar publication contracts. */
public final class IRTableLoaderTest {
    public static void main(String[] args) throws Exception {
        var started = new CountDownLatch(1); var release = new CountDownLatch(1);
        var calls = new AtomicInteger(); Thread owner = Thread.currentThread();
        IRTableData table = table();
        var connection = new TestIRConnectionBase() {
            @Override public IRResponse<IRTableData[]> getTableDatas() {
                calls.incrementAndGet(); check(Thread.currentThread() != owner, "No UI-thread fetch"); started.countDown();
                await(release); return response(true, new IRTableData[]{table});
            }
        };
        try (var loader = new IRTableLoader()) {
            loader.start(connection); loader.start(connection);
            check(started.await(3, TimeUnit.SECONDS) && loader.take() == null, "Startup returns while optional IR is blocked");
            release.countDown(); waitStopped();
            check(loader.take()[0] == table && loader.take() == null && calls.get() == 1, "Publish once, no duplicate requests");
        } finally { release.countDown(); }
        var lateStarted = new CountDownLatch(1); var lateRelease = new CountDownLatch(1);
        var late = new IRTableLoader();
        try {
            late.start(new TestIRConnectionBase() {
                @Override public IRResponse<IRTableData[]> getTableDatas() {
                    lateStarted.countDown(); await(lateRelease); return response(true, new IRTableData[]{table});
                }
            });
            check(lateStarted.await(3, TimeUnit.SECONDS), "Late request started");
            late.close(); lateRelease.countDown(); waitStopped();
            check(late.take() == null, "No publication after disposal even if plugin ignores interruption");
        } finally { lateRelease.countDown(); late.close(); }
        for (int fault = 0; fault < 3; fault++) {
            final int mode = fault;
            try (var loader = new IRTableLoader()) {
                loader.start(new TestIRConnectionBase() {
                    @Override public IRResponse<IRTableData[]> getTableDatas() {
                        if (mode == 0) return response(false, null);
                        if (mode == 1) throw new IllegalStateException("fixture");
                        return null;
                    }
                });
                waitStopped(); check(loader.take() == null, "Failure leaves local tables alone");
            }
        }
        publication(table);
        check(BarManager.toTableData(new IRTableData("empty", null, null)).getFolder().length == 0, "Optional arrays handled");
        System.out.println("IRTableLoaderTest passed (nonblocking fetch, duplicate/failure/disposal, render-thread TableBars, local tables/folder/cursor retained)");
    }
    private static void publication(IRTableData table) throws Exception {
        var select = KeyboardQueueTestSupport.allocate(MusicSelector.class);
        var main = KeyboardQueueTestSupport.allocate(MainController.class);
        var resource = KeyboardQueueTestSupport.allocate(PlayerResource.class);
        KeyboardQueueTestSupport.set(resource, PlayerResource.class, "config", new Config());
        KeyboardQueueTestSupport.set(select, MainState.class, "main", main);
        KeyboardQueueTestSupport.set(select, MainState.class, "resource", resource);
        Thread renderer = Thread.currentThread();
        var database = (SongDatabaseAccessor)Proxy.newProxyInstance(IRTableLoaderTest.class.getClassLoader(),
                new Class<?>[]{SongDatabaseAccessor.class}, (proxy, method, arguments) -> {
                    check(Thread.currentThread() == renderer, "No UI/DB publication on loader thread");
                    return new SongData[0];
                });
        KeyboardQueueTestSupport.set(select, MusicSelector.class, "songdb", database);
        var manager = new BarManager(select);
        var localData = new TableData(); localData.setName("local");
        var local = new TableBar(select, localData, null);
        KeyboardQueueTestSupport.set(manager, BarManager.class, "tables", new TableBar[]{local});
        var folder = new HashBar(select, "stay here", new SongData[0]); manager.getDirectory().addLast(folder);
        manager.currentsongs = new Bar[]{folder}; manager.selectedindex = 0;
        var field = BarManager.class.getDeclaredField("irTables"); field.setAccessible(true);
        var loader = (IRTableLoader)field.get(manager);
        try {
            loader.start(new TestIRConnectionBase() {
                @Override public IRResponse<IRTableData[]> getTableDatas() { return response(true, new IRTableData[]{null, table}); }
            });
            waitStopped(); manager.updateIRTables();
            var tablesField = BarManager.class.getDeclaredField("tables"); tablesField.setAccessible(true);
            var tables = (TableBar[])tablesField.get(manager);
            check(tables.length == 2 && tables[0] == local && tables[1].getTitle().equals("remote"), "Append IR tables without replacing local ones");
            check(tables[1].getLevels()[0].getElements()[0].getSha256().equals("a".repeat(64)), "Preserve chart metadata");
            check(manager.getDirectory().last() == folder && manager.getSelected() == folder && manager.selectedindex == 0, "No folder/cursor jump");
            manager.updateIRTables(); check(((TableBar[])tablesField.get(manager)).length == 2, "Apply once");
            manager.dispose(); manager.updateIRTables();
        } finally { manager.dispose(); }
    }
    private static IRTableData table() {
        var song = new SongData(); song.setSha256("a".repeat(64)); song.setTitle("fixture");
        return new IRTableData("remote", new IRTableData.IRTableFolder[]{new IRTableData.IRTableFolder("level", new IRChartData[]{new IRChartData(song)})}, new IRCourseData[0]);
    }
    private static IRResponse<IRTableData[]> response(boolean success, IRTableData[] data) {
        return new IRResponse<>() {
            public boolean isSucceeded() { return success; }
            public String getMessage() { return "fixture"; }
            public IRTableData[] getData() { return data; }
        };
    }
    private static void await(CountDownLatch latch) {
        boolean interrupted = false;
        try {
            while (true) try { if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("Latch timeout"); return; }
            catch (InterruptedException e) { interrupted = true; }
        } finally { if (interrupted) Thread.currentThread().interrupt(); }
    }
    private static void waitStopped() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (Thread.getAllStackTraces().keySet().stream().anyMatch(t -> t.isAlive() && t.getName().equals("ir-table-loader"))) {
            check(System.nanoTime() < deadline, "Loader thread did not stop"); Thread.sleep(10);
        }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
