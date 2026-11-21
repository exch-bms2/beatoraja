package bms.player.beatoraja;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.logging.Logger;

import bms.player.beatoraja.MainController.IRStatus;
import bms.player.beatoraja.ir.*;

/** Coordinates parallel IR checks and login with per-IR connection reuse. */
final class IRStartup implements AutoCloseable {
	private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger();

	private final Function<String, IRConnection> connectionFactory;
	private final List<Entry> entries;
	private volatile boolean closed;

	IRStartup(IRConfig[] irConfigs) {
		this(irConfigs, IRConnectionManager::getIRConnection);
	}

	IRStartup(IRConfig[] irConfigs, Function<String, IRConnection> connectionFactory) {
		this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
		List<Entry> list = new ArrayList<>();
		if (irConfigs != null) {
			for (IRConfig config : irConfigs) {
				if (config == null) continue;
				String irName = config.getIrname();
				if (irName == null || irName.isEmpty()) continue;
				list.add(new Entry(config, irName, createAccount(config)));
			}
		}
		this.entries = List.copyOf(list);
	}

	boolean collectIllegalSongs(Consumer<String[]> candidateConsumer) {
		Objects.requireNonNull(candidateConsumer, "candidateConsumer");
		if (closed) return true;
		if (entries.isEmpty()) return false;
		List<IllegalSongs> results = runAll(entries, this::fetchIllegalSongs,
				(entry, error) -> Logger.getGlobal().warning("IR illegal song list取得失敗 - " + entry.irName + " : " + error.getMessage()));
		if (Thread.currentThread().isInterrupted()) {
			close();
			return true;
		}
		boolean failed = false;
		for (IllegalSongs result : results) {
			if (result == null) { failed = true; continue; }
			if (result.hashes() != null) candidateConsumer.accept(result.hashes());
			failed |= result.failed();
		}
		return failed;
	}

	IRStatus[] login() {
		if (closed) return new IRStatus[0];
		List<Entry> targets = entries.stream().filter(e -> e.account != null).toList();
		if (targets.isEmpty()) return new IRStatus[0];
		List<IRStatus> results = runAll(targets, this::performLogin,
				(entry, error) -> Logger.getGlobal().warning("IRへのログイン失敗 : " + error.getMessage()));
		if (Thread.currentThread().isInterrupted()) {
			close();
			return new IRStatus[0];
		}
		List<IRStatus> statuses = new ArrayList<>();
		for (int i = 0; i < targets.size(); i++) {
			IRStatus status = results.get(i);
			Entry entry = targets.get(i);
			if (status != null && entry.connection.compareAndSet(status.connection, null)) {
				statuses.add(status);
			} else {
				entry.account = null;
				entry.closeConnection();
			}
		}
		return statuses.toArray(new IRStatus[0]);
	}

	@Override
	public void close() {
		if (closed) return;
		closed = true;
		for (Entry entry : entries) { entry.account = null; entry.closeConnection(); }
	}

	private IllegalSongs fetchIllegalSongs(Entry entry) {
		IRConnection conn = openConnection(entry);
		if (conn == null) return new IllegalSongs(null, false);
		boolean keep = false;
		try {
			IRResponse<String[]> response = conn.getIllegalSongs();
			keep = entry.account != null;
			if (response == null) return new IllegalSongs(null, true);
			if (response.isSucceeded()) return new IllegalSongs(response.getData(), false);
			String message = response.getMessage();
			if (message != null && !message.isEmpty() && !"Not supported".equals(message)) {
				Logger.getGlobal().warning("IR illegal song list取得失敗 - " + entry.irName + " : " + message);
				return new IllegalSongs(null, true);
			}
			return new IllegalSongs(null, false);
		} finally {
			if (!keep) entry.closeConnection();
		}
	}

	private IRStatus performLogin(Entry entry) {
		IRAccount account = entry.account;
		entry.account = null;
		if (account == null || Thread.currentThread().isInterrupted()) {
			entry.closeConnection();
			return null;
		}
		IRConnection conn = openConnection(entry);
		if (conn == null) return null;
		boolean ok = false;
		try {
			IRResponse<IRPlayerData> response;
			try (var timing = StartupDiagnostics.measure(StartupDiagnostics.Step.IR_LOGIN)) {
				response = conn.login(account);
				ok = response != null && response.isSucceeded();
				timing.outcome(ok ? StartupDiagnostics.Outcome.OK : StartupDiagnostics.Outcome.FAILED);
			}
			if (ok) return new IRStatus(entry.config, conn, response.getData());
			if (response != null) Logger.getGlobal().warning("IRへのログイン失敗 : " + response.getMessage());
			return null;
		} finally {
			if (!ok) entry.closeConnection();
		}
	}

	private <T> List<T> runAll(List<Entry> targets, Function<Entry, T> task, BiConsumer<Entry, Throwable> onError) {
		List<T> results = new ArrayList<>(Collections.nCopies(targets.size(), null));
		if (targets.size() == 1) {
			try { results.set(0, task.apply(targets.get(0))); }
			catch (RuntimeException | LinkageError error) { onError.accept(targets.get(0), error); }
			return results;
		}
		ExecutorService pool = Executors.newFixedThreadPool(targets.size(), runnable -> {
			Thread thread = new Thread(runnable, "ir-startup-" + THREAD_SEQUENCE.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		});
		List<Future<T>> futures = new ArrayList<>(targets.size());
		try {
			for (Entry entry : targets) futures.add(pool.submit(() -> task.apply(entry)));
			for (int i = 0; i < targets.size(); i++) {
				try {
					results.set(i, futures.get(i).get());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				} catch (ExecutionException e) {
					Throwable cause = e.getCause() != null ? e.getCause() : e;
					if (cause instanceof Error err && !(err instanceof LinkageError)) throw err;
					onError.accept(targets.get(i), cause);
				}
			}
		} finally {
			pool.shutdownNow();
		}
		return results;
	}

	private IRConnection openConnection(Entry entry) {
		if (closed) return null;
		IRConnection conn = entry.connection.get();
		if (conn != null) return conn;
		IRConnection fresh = connectionFactory.apply(entry.irName);
		if (fresh == null) return null;
		if (closed || Thread.currentThread().isInterrupted() || !entry.connection.compareAndSet(null, fresh)) {
			IRConnectionManager.close(fresh);
			return null;
		}
		if (closed) { entry.closeConnection(); return null; }
		return fresh;
	}

	private static IRAccount createAccount(IRConfig config) {
		String userid = config.getUserid();
		if (userid == null || userid.isEmpty()) return null;
		String password = config.getPassword();
		return (password == null || password.isEmpty()) ? null : new IRAccount(userid, password, "");
	}

	private static final class Entry {
		final IRConfig config;
		final String irName;
		volatile IRAccount account;
		final AtomicReference<IRConnection> connection = new AtomicReference<>();

		Entry(IRConfig config, String irName, IRAccount account) {
			this.config = config;
			this.irName = irName;
			this.account = account;
		}

		void closeConnection() {
			IRConnectionManager.close(connection.getAndSet(null));
		}
	}

	private record IllegalSongs(String[] hashes, boolean failed) { }
}
