package bms.player.beatoraja;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import bms.player.beatoraja.MainController.IRStatus;
import bms.player.beatoraja.ir.*;

/** Regression tests for synchronous parallel IR checks, login, and per-IR connection reuse. */
public final class IRStartupTest {

	public static void main(String[] args) throws Exception {
		testParallelStartupAndConnectionReuse();
		testPartialFailuresEmptyCredsAndMissingIr();
		testCloseBeforeLoginClosesConnectionsAndStopsFurtherUse();
		testExceptionInIllegalSongsRecoversOnLogin();
		testDirectLoginWithoutIllegalSongCheck();
		testInterruptDuringParallelCheckCancelsAndClosesConnections();
		testEmptyConfigsAndIrNameSnapshot();
		System.out.println("IRStartupTest passed");
	}

	private static void testParallelStartupAndConnectionReuse() throws Exception {
		IRConfig primary = irConfig("ir-primary", "user1", "pass1");
		IRConfig secondary = irConfig("ir-secondary", "user2", "pass2");
		CyclicBarrier illegalBarrier = new CyclicBarrier(2), loginBarrier = new CyclicBarrier(2);
		CountDownLatch secondaryDone = new CountDownLatch(1);
		Map<String, AtomicInteger> createdCounts = new ConcurrentHashMap<>();
		Map<String, MockIRConnection> created = new ConcurrentHashMap<>();
		String hashA = "a".repeat(64), hashB = "b".repeat(64);

		try (IRStartup startup = new IRStartup(new IRConfig[] { primary, secondary }, name -> {
			createdCounts.computeIfAbsent(name, ignored -> new AtomicInteger()).incrementAndGet();
			MockIRConnection conn = new MockIRConnection(name)
					.onIllegal(() -> {
						awaitBarrier(illegalBarrier);
						return ok(new String[] { "ir-primary".equals(name) ? hashA : hashB });
					})
					.onLogin(account -> {
						awaitBarrier(loginBarrier);
						if ("ir-primary".equals(name)) awaitLatch(secondaryDone);
						IRResponse<IRPlayerData> res = ok(new IRPlayerData(account.id, name, "10"));
						if ("ir-secondary".equals(name)) secondaryDone.countDown();
						return res;
					});
			created.put(name, conn);
			return conn;
		})) {
			Set<String> hashes = new LinkedHashSet<>();
			check(!startup.collectIllegalSongs(candidates -> Collections.addAll(hashes, candidates)),
					"Expected healthy illegal song checks");
			check(Set.of(hashA, hashB).equals(hashes), "Expected collected hashes: " + hashes);

			IRStatus[] statuses = startup.login();
			check(statuses.length == 2, "Expected both IRs logged in");
			check(statuses[0].config == primary && statuses[1].config == secondary, "Must preserve IRConfig order");
			check(statuses[0].connection == created.get("ir-primary") && statuses[1].connection == created.get("ir-secondary"),
					"Must reuse opened connections");
			check(createdCounts.get("ir-primary").get() == 1 && createdCounts.get("ir-secondary").get() == 1,
					"Each IR connection must be created only once");
			check(List.of("illegal", "login:user1").equals(created.get("ir-primary").calls), "Unexpected primary calls");
			check(List.of("illegal", "login:user2").equals(created.get("ir-secondary").calls), "Unexpected secondary calls");
			check(startup.login().length == 0, "Repeated login() must return empty array");
			check(!created.get("ir-primary").closed && !created.get("ir-secondary").closed,
					"Repeated login() must not close transferred connections");
		}
		check(!created.get("ir-primary").closed && !created.get("ir-secondary").closed,
				"Transferred connections must remain open after close()");
		created.get("ir-primary").close();
		created.get("ir-secondary").close();
	}

	private static void testPartialFailuresEmptyCredsAndMissingIr() {
		IRConfig noCreds = irConfig("ir-no-creds", "", "");
		IRConfig loginFail = irConfig("ir-login-fail", "u2", "p2");
		IRConfig healthy = irConfig("ir-healthy", "u3", "p3");
		String hashC = "c".repeat(64);
		Map<String, MockIRConnection> created = new ConcurrentHashMap<>();

		IRStatus[] statuses;
		try (IRStartup startup = new IRStartup(new IRConfig[] { noCreds, loginFail, healthy }, name -> {
			MockIRConnection conn = new MockIRConnection(name)
					.onIllegal(() -> switch (name) {
						case "ir-no-creds" -> fail("Not supported");
						case "ir-login-fail" -> fail("network error");
						default -> ok(new String[] { hashC });
					})
					.onLogin(account -> "ir-login-fail".equals(name)
							? fail("bad credentials")
							: ok(new IRPlayerData(account.id, name, "1")));
			created.put(name, conn);
			return conn;
		})) {
			Set<String> hashes = new LinkedHashSet<>();
			check(startup.collectIllegalSongs(candidates -> Collections.addAll(hashes, candidates)),
					"Non-'Not supported' failure should mark check failed");
			check(Set.of(hashC).equals(hashes), "Healthy IR hashes should still be collected");
			check(created.get("ir-no-creds").closed, "Credential-less connection must close after check");
			statuses = startup.login();
		}
		check(statuses.length == 1 && statuses[0].config == healthy, "Only healthy IR should remain");
		check(created.get("ir-no-creds").calls.equals(List.of("illegal")), "No login attempt without credentials");
		check(created.get("ir-login-fail").closed && !created.get("ir-healthy").closed,
				"Failed login must close while healthy connection stays open");
		created.get("ir-healthy").close();

		IRConfig missing = irConfig("ir-missing", "u4", "p4");
		IRConfig healthyAfterMissing = irConfig("ir-healthy-2", "u5", "p5");
		Map<String, MockIRConnection> missingCreated = new ConcurrentHashMap<>();
		IRStatus[] missingStatuses;
		try (IRStartup startup = new IRStartup(new IRConfig[] { missing, healthyAfterMissing }, name -> {
			if ("ir-missing".equals(name)) return null;
			MockIRConnection conn = new MockIRConnection(name);
			missingCreated.put(name, conn);
			return conn;
		})) {
			check(!startup.collectIllegalSongs(candidates -> { }), "Unresolvable IR must not mark check failed");
			missingStatuses = startup.login();
		}
		check(missingStatuses.length == 1 && missingStatuses[0].config == healthyAfterMissing,
				"Unresolvable IR must be skipped during login");
		missingCreated.get("ir-healthy-2").close();
	}

	private static void testCloseBeforeLoginClosesConnectionsAndStopsFurtherUse() {
		Map<String, MockIRConnection> created = new ConcurrentHashMap<>();
		AtomicInteger factoryCalls = new AtomicInteger();
		IRStartup startup = new IRStartup(new IRConfig[] { irConfig("ir-abort", "user", "pass") }, name -> {
			factoryCalls.incrementAndGet();
			MockIRConnection conn = new MockIRConnection(name);
			created.put(name, conn);
			return conn;
		});
		try {
			startup.collectIllegalSongs(candidates -> { });
		} finally {
			startup.close();
		}
		check(created.get("ir-abort").closed, "Closing startup before login must close all connections");
		check(startup.collectIllegalSongs(candidates -> { }), "Closed startup collectIllegalSongs() must report failure");
		check(startup.login().length == 0, "Closed startup login() must return empty array");
		check(factoryCalls.get() == 1, "Closed startup must not invoke connection factory again");
	}

	private static void testExceptionInIllegalSongsRecoversOnLogin() {
		IRConfig recover = irConfig("ir-recover", "u1", "p1");
		IRConfig loginThrows = irConfig("ir-login-throws", "u2", "p2");
		List<MockIRConnection> recoverConns = Collections.synchronizedList(new ArrayList<>());
		Map<String, MockIRConnection> throwsConn = new ConcurrentHashMap<>();

		IRStatus[] statuses;
		try (IRStartup startup = new IRStartup(new IRConfig[] { recover, loginThrows }, name -> {
			if ("ir-recover".equals(name)) {
				int attempt = recoverConns.size();
				MockIRConnection conn = new MockIRConnection(name).onIllegal(() -> {
					if (attempt == 0) throw new RuntimeException("illegal boom");
					return ok(new String[0]);
				});
				recoverConns.add(conn);
				return conn;
			}
			MockIRConnection conn = new MockIRConnection(name).onLogin(account -> {
				throw new RuntimeException("login boom");
			});
			throwsConn.put(name, conn);
			return conn;
		})) {
			check(startup.collectIllegalSongs(candidates -> { }), "Exception in getIllegalSongs must mark check failed");
			check(recoverConns.size() == 1 && recoverConns.get(0).closed, "Failed check connection must close immediately");
			statuses = startup.login();
		}
		check(statuses.length == 1 && statuses[0].config == recover, "Recovering slot must succeed on fresh connection");
		check(recoverConns.size() == 2 && !recoverConns.get(1).closed && statuses[0].connection == recoverConns.get(1),
				"Fresh recovered connection must be transferred and stay open");
		check(throwsConn.get("ir-login-throws").closed, "Connection that threw during login must close");
		recoverConns.get(1).close();
	}

	private static void testDirectLoginWithoutIllegalSongCheck() {
		Thread callerThread = Thread.currentThread();
		AtomicReference<Thread> singleLoginThread = new AtomicReference<>();
		IRStatus[] singleStatuses;
		try (IRStartup startup = new IRStartup(new IRConfig[] { irConfig("ir-single", "u1", "p1") },
				name -> new MockIRConnection(name).onLogin(account -> {
					singleLoginThread.set(Thread.currentThread());
					return ok(new IRPlayerData(account.id, name, "1"));
				}))) {
			singleStatuses = startup.login();
		}
		check(singleStatuses.length == 1 && singleLoginThread.get() == callerThread,
				"Single-IR direct login must run inline on caller thread");
		IRConnectionManager.close(singleStatuses[0].connection);

		CyclicBarrier loginBarrier = new CyclicBarrier(2);
		IRStatus[] multiStatuses;
		try (IRStartup startup = new IRStartup(
				new IRConfig[] { irConfig("ir-direct-1", "u1", "p1"), irConfig("ir-direct-2", "u2", "p2") },
				name -> new MockIRConnection(name).onLogin(account -> {
					awaitBarrier(loginBarrier);
					return ok(new IRPlayerData(account.id, name, "1"));
				}))) {
			multiStatuses = startup.login();
		}
		check(multiStatuses.length == 2, "Multi-IR direct login must run in parallel");
		for (IRStatus status : multiStatuses) IRConnectionManager.close(status.connection);
	}

	private static void testInterruptDuringParallelCheckCancelsAndClosesConnections() throws Exception {
		IRConfig[] configs = new IRConfig[6];
		for (int i = 0; i < configs.length; i++) configs[i] = irConfig("ir-queued-" + i, "u" + i, "p" + i);
		CountDownLatch entered = new CountDownLatch(configs.length);
		List<MockIRConnection> created = Collections.synchronizedList(new ArrayList<>());
		IRStartup startup = new IRStartup(configs, name -> {
			MockIRConnection conn = new MockIRConnection(name).onIllegal(() -> {
				entered.countDown();
				try {
					Thread.sleep(10_000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("interrupted", e);
				}
				return ok(new String[0]);
			});
			created.add(conn);
			return conn;
		});

		AtomicBoolean failedResult = new AtomicBoolean(), interruptedObserved = new AtomicBoolean();
		AtomicReference<Throwable> waiterFailure = new AtomicReference<>();
		Thread waiter = new Thread(() -> {
			try {
				failedResult.set(startup.collectIllegalSongs(candidates -> { }));
				interruptedObserved.set(Thread.currentThread().isInterrupted());
			} catch (Throwable t) {
				waiterFailure.set(t);
			}
		}, "ir-startup-interrupt-waiter");
		waiter.start();
		try {
			check(entered.await(3, TimeUnit.SECONDS), "All 6 workers should enter getIllegalSongs concurrently");
			waiter.interrupt();
			waiter.join(3_000);
			check(!waiter.isAlive() && waiterFailure.get() == null, "Waiter must unblock without uncaught error");
			check(failedResult.get() && interruptedObserved.get(),
					"Interrupted collectIllegalSongs must report failure and preserve interrupt flag");
		} finally {
			startup.close();
		}
		check(created.size() == configs.length, "All 6 connections should have been created concurrently");
		for (MockIRConnection conn : created) {
			check(conn.closed && conn.closeCount.get() == 1,
					"Connection must close once on interrupt: " + conn.name + " count=" + conn.closeCount.get());
		}

		CountDownLatch fastDone = new CountDownLatch(1), slowEntered = new CountDownLatch(1);
		MockIRConnection fast = new MockIRConnection("ir-fast-login").onLogin(account -> {
			IRResponse<IRPlayerData> res = ok(new IRPlayerData(account.id, "ir-fast-login", "1"));
			fastDone.countDown();
			return res;
		});
		MockIRConnection slow = new MockIRConnection("ir-slow-login").onLogin(account -> {
			awaitLatch(fastDone);
			slowEntered.countDown();
			try {
				Thread.sleep(10_000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("interrupted", e);
			}
			return ok(new IRPlayerData(account.id, "ir-slow-login", "1"));
		});
		IRStartup loginStartup = new IRStartup(
				new IRConfig[] { irConfig("ir-fast-login", "u1", "p1"), irConfig("ir-slow-login", "u2", "p2") },
				name -> "ir-fast-login".equals(name) ? fast : slow);
		AtomicReference<IRStatus[]> loginResult = new AtomicReference<>();
		AtomicBoolean loginInterrupted = new AtomicBoolean();
		AtomicReference<Throwable> loginFailure = new AtomicReference<>();
		Thread loginWaiter = new Thread(() -> {
			try {
				loginResult.set(loginStartup.login());
				loginInterrupted.set(Thread.currentThread().isInterrupted());
			} catch (Throwable t) {
				loginFailure.set(t);
			}
		}, "ir-startup-login-interrupt-waiter");
		loginWaiter.start();
		try {
			check(slowEntered.await(3, TimeUnit.SECONDS), "Slow worker should enter login after fast worker completes");
			loginWaiter.interrupt();
			loginWaiter.join(3_000);
			check(!loginWaiter.isAlive() && loginFailure.get() == null, "Login waiter must unblock without uncaught error");
			check(loginResult.get() != null && loginResult.get().length == 0 && loginInterrupted.get(),
					"Interrupted login() must return empty array and preserve interrupt flag");
		} finally {
			loginStartup.close();
		}
		check(fast.closed && fast.closeCount.get() == 1 && slow.closed && slow.closeCount.get() == 1,
				"Interrupted login() must close both fast and slow connections once");
	}

	private static void testEmptyConfigsAndIrNameSnapshot() {
		for (IRConfig[] configs : new IRConfig[][] { null, new IRConfig[0], new IRConfig[] { null, new IRConfig() } }) {
			try (IRStartup empty = new IRStartup(configs, name -> {
				throw new AssertionError("Should not create connection for unconfigured IR");
			})) {
				check(!empty.collectIllegalSongs(candidates -> { }) && empty.login().length == 0,
						"Empty configs should succeed with empty login");
			}
		}

		IRConfig mutated = irConfig("ir-original", "u", "p");
		List<String> requested = Collections.synchronizedList(new ArrayList<>());
		IRStatus[] statuses;
		try (IRStartup startup = new IRStartup(new IRConfig[] { mutated }, name -> {
			requested.add(name);
			return new MockIRConnection(name);
		})) {
			mutated.setIrname("mutated");
			check(!startup.collectIllegalSongs(candidates -> { }), "Snapshot check should succeed");
			statuses = startup.login();
		}
		check(List.of("ir-original").equals(requested) && statuses.length == 1,
				"Must snapshot IR name at construction: " + requested);
		IRConnectionManager.close(statuses[0].connection);
	}

	private static IRConfig irConfig(String name, String userid, String password) {
		IRConfig config = new IRConfig();
		config.setUserid(userid);
		config.setPassword(password);
		config.setIrname(name);
		return config;
	}

	private static void awaitBarrier(CyclicBarrier barrier) {
		try { barrier.await(3, TimeUnit.SECONDS); }
		catch (Exception e) { throw new AssertionError("Expected parallel execution", e); }
	}

	private static void awaitLatch(CountDownLatch latch) {
		try { check(latch.await(3, TimeUnit.SECONDS), "Timed out waiting for peer worker"); }
		catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	private static final class MockIRConnection extends TestIRConnectionBase implements AutoCloseable {
		final String name;
		final List<String> calls = Collections.synchronizedList(new ArrayList<>());
		final AtomicInteger closeCount = new AtomicInteger();
		volatile boolean closed;
		private Supplier<IRResponse<String[]>> illegalHandler = () -> ok(new String[0]);
		private Function<IRAccount, IRResponse<IRPlayerData>> loginHandler;

		MockIRConnection(String name) {
			this.name = name;
			this.loginHandler = account -> ok(new IRPlayerData(account.id, name, "1"));
		}

		MockIRConnection onIllegal(Supplier<IRResponse<String[]>> handler) { illegalHandler = handler; return this; }
		MockIRConnection onLogin(Function<IRAccount, IRResponse<IRPlayerData>> handler) { loginHandler = handler; return this; }

		@Override
		public IRResponse<String[]> getIllegalSongs() {
			calls.add("illegal");
			return illegalHandler.get();
		}

		@Override
		public IRResponse<IRPlayerData> login(IRAccount account) {
			calls.add("login:" + account.id);
			return loginHandler.apply(account);
		}

		@Override
		public void close() {
			closeCount.incrementAndGet();
			closed = true;
		}
	}

	private record SimpleResponse<T>(boolean isSucceeded, String getMessage, T getData) implements IRResponse<T> { }
	private static <T> IRResponse<T> ok(T data) { return new SimpleResponse<>(true, "", data); }
	private static <T> IRResponse<T> fail(String message) { return new SimpleResponse<>(false, message, null); }
}
