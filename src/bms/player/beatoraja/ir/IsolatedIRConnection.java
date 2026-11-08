package bms.player.beatoraja.ir;

import static bms.player.beatoraja.ir.IRWorkerProtocol.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

/**
 * IRConnectionを別Javaプロセスで実行するためのプロキシ実装。
 * <p>
 * beatoraja本体からは通常の{@link IRConnection}として扱えるが、実際のIR実装は
 * {@link IRWorkerMain}上でロードされ、stdin/stdoutの1行JSONで要求と応答をやり取りする。
 * これによりIR jarへ{@code MainController}や{@code MainState}の参照を渡さずに済ませる。
 * </p>
 */
public class IsolatedIRConnection implements IRConnection, AutoCloseable {

	private final String irName;
	private final Json json = createJson();
	private final long timeoutMillis;
	private final WorkerFactory workerFactory;
	private final ReentrantLock requests = new ReentrantLock();
	private final Object lifecycle = new Object();
	private Worker worker;
	private Account lastLogin;
	private boolean closed;
	private int generation;

	public IsolatedIRConnection(String irName) {
		this(irName, 30_000, () -> IRProcessSupport.startWorker(irName));
	}

	IsolatedIRConnection(String irName, long timeoutMillis, WorkerFactory workerFactory) {
		this.irName = irName;
		this.timeoutMillis = Math.max(1, timeoutMillis);
		this.workerFactory = workerFactory;
	}

	@FunctionalInterface
	interface WorkerFactory { Process start() throws IOException; }

	@Override
	public IRResponse<IRPlayerData> register(IRAccount account) {
		Request request = new Request();
		request.method = METHOD_REGISTER;
		request.account = Account.from(account);
		Response response = invoke(request);
		return new SimpleIRResponse<>(response.succeeded, response.message,
				response.player != null ? response.player.toIRPlayerData() : null);
	}

	@Override
	public IRResponse<IRPlayerData> login(IRAccount account) {
		Request request = new Request();
		request.method = METHOD_LOGIN;
		request.account = Account.from(account);
		Response response = invoke(request);
		return new SimpleIRResponse<>(response.succeeded, response.message,
				response.player != null ? response.player.toIRPlayerData() : null);
	}

	@Override
	public IRResponse<IRPlayerData[]> getRivals() {
		Request request = new Request();
		request.method = METHOD_GET_RIVALS;
		Response response = invoke(request);
		return new SimpleIRResponse<>(response.succeeded, response.message, toIRPlayers(response.players));
	}

	@Override
	public IRResponse<IRTableData[]> getTableDatas() {
		Request request = new Request();
		request.method = METHOD_GET_TABLE_DATAS;
		Response response = invoke(request);
		return new SimpleIRResponse<>(response.succeeded, response.message, toIRTables(response.tables));
	}

	@Override
	public IRResponse<IRScoreData[]> getPlayData(IRPlayerData player, IRChartData chart) {
		Request request = new Request();
		request.method = METHOD_GET_PLAY_DATA;
		request.player = Player.from(player);
		request.chart = Chart.from(chart);
		Response response = invoke(request);
		return new SimpleIRResponse<>(response.succeeded, response.message, toIRScores(response.scores));
	}

	@Override
	public IRScoreDataSyncResponse getPlayDataSince(IRPlayerData player, long scoreRevision) {
		Request request = new Request();
		request.method = METHOD_GET_PLAY_DATA;
		request.player = Player.from(player);
		request.scoreRevision = scoreRevision;
		Response response = invoke(request);
		return new IRScoreDataSyncResponse(response.succeeded, response.message, toIRScores(response.scores), response.scoreRevision, response.hasMore);
	}

	@Override
	public IRResponse<IRScoreData[]> getCoursePlayData(IRPlayerData player, IRCourseData course) {
		Request request = new Request();
		request.method = METHOD_GET_COURSE_PLAY_DATA;
		request.player = Player.from(player);
		request.course = Course.from(course);
		Response response = invoke(request);
		return new SimpleIRResponse<>(response.succeeded, response.message, toIRScores(response.scores));
	}

	@Override
	public IRResponse<Object> sendPlayData(IRChartData model, IRScoreData score) {
		Request request = new Request();
		request.method = METHOD_SEND_PLAY_DATA;
		request.chart = Chart.from(model);
		request.score = Score.from(score);
		return submit(request);
	}

	@Override
	public IRResponse<Object> setReview(IRReview review) {
		if (review == null) return new SimpleIRResponse<>(false, "Missing review update", null);
		Request request = new Request();
		request.method = METHOD_SET_REVIEW;
		request.review = Review.from(review);
		return submit(request);
	}

	@Override
	public IRResponse<Object> sendCoursePlayData(IRCourseData course, IRScoreData score) {
		Request request = new Request();
		request.method = METHOD_SEND_COURSE_PLAY_DATA;
		request.course = Course.from(course);
		request.score = Score.from(score);
		return submit(request);
	}

	@Override
	public String getSongURL(IRChartData chart) {
		Request request = new Request();
		request.method = METHOD_GET_SONG_URL;
		request.chart = Chart.from(chart);
		return invoke(request).text;
	}

	@Override
	public String getCourseURL(IRCourseData course) {
		Request request = new Request();
		request.method = METHOD_GET_COURSE_URL;
		request.course = Course.from(course);
		return invoke(request).text;
	}

	@Override
	public String getPlayerURL(IRPlayerData player) {
		Request request = new Request();
		request.method = METHOD_GET_PLAYER_URL;
		request.player = Player.from(player);
		return invoke(request).text;
	}

	@Override
	public IRResponse<IRVersionInfo> getVersionInfo(String currentVersion) {
		Request request = new Request();
		request.method = METHOD_GET_VERSION_INFO;
		request.currentVersion = currentVersion;
		Response response = invoke(request);
		return new SimpleIRResponse<>(response.succeeded, response.message,
				response.version != null ? response.version.toIRVersionInfo() : null);
	}

	@Override
	public IRResponse<String[]> getIllegalSongs() {
		Request request = new Request();
		request.method = METHOD_GET_ILLEGAL_SONGS;
		Response response = invoke(request);
		return new SimpleIRResponse<>(response.succeeded, response.message,
				response.illegalSongs != null ? response.illegalSongs : new String[0]);
	}

	@Override
	public void close() {
		Worker old;
		synchronized (lifecycle) {
			closed = true;
			lastLogin = null;
			old = worker;
			worker = null;
		}
		if (old != null) old.stop();
	}

	private IRResponse<Object> submit(Request request) {
		Invocation result = execute(request);
		Response response = result.response;
		if (METHOD_SET_REVIEW.equals(request.method) && response.reviewUnsupported)
			return IRReview.UNSUPPORTED;
		if (!response.succeeded && result.attempted)
			return IRSubmissionResponse.unknown(response.message);
		return new SimpleIRResponse<>(response.succeeded, response.message, null);
	}

	private Response invoke(Request request) { return execute(request).response; }

	private Invocation execute(Request request) {
		Worker session = null;
		boolean locked = false, attempted = false;
		try {
			requests.lockInterruptibly();
			locked = true;
			Account account;
			synchronized (lifecycle) {
				if (closed) return new Invocation(Response.error("IR connection closed"), false);
				if (METHOD_LOGIN.equals(request.method)) lastLogin = null;
				account = lastLogin;
			}
			if (METHOD_SET_REVIEW.equals(request.method) && account == null)
				return new Invocation(Response.error("IR review requires login"), false);
			session = ensureProcess();
			if (!session.authenticated && account != null && !METHOD_LOGIN.equals(request.method)
					&& !METHOD_REGISTER.equals(request.method)) {
				Request login = new Request(); login.method = METHOD_LOGIN; login.account = account;
				Response authentication = exchange(session, login);
				if (!authentication.succeeded) {
					invalidate(session);
					return new Invocation(Response.error("IR worker reauthentication failed : " + authentication.message), false);
				}
				session.authenticated = true;
				Logger.getGlobal().info("IR worker reauthenticated : " + irName);
			}
			// No request is replayed after this point, including read-only calls.
			attempted = true;
			Response response = exchange(session, request);
			synchronized (lifecycle) {
				if (closed || worker != session) return new Invocation(Response.error("IR connection closed"), attempted);
				if (METHOD_LOGIN.equals(request.method) && response.succeeded) {
					Account saved = new Account();
					saved.id = request.account.id; saved.password = request.account.password; saved.name = request.account.name;
					lastLogin = saved;
					session.authenticated = true;
				}
			}
			if (METHOD_LOGIN.equals(request.method) && !response.succeeded) invalidate(session);
			return new Invocation(response, attempted);
		} catch (InterruptedException e) {
			if (session != null) invalidate(session);
			Thread.currentThread().interrupt();
			return new Invocation(Response.error("IR worker interrupted"), attempted);
		} catch (Exception | LinkageError e) {
			if (session != null) invalidate(session);
			String message = e instanceof TimeoutException ? "IR worker timeout"
					: e instanceof EOFException ? "IR worker exited" : "IR worker error : " + e.getClass().getSimpleName();
			return new Invocation(Response.error(message), attempted);
		} finally {
			if (locked) requests.unlock();
		}
	}

	private Response exchange(Worker session, Request request) throws Exception {
		if (!current(session)) throw new IOException("Connection closed");
		String payload = json.toJson(request);
		Future<String> future = session.io.submit(() -> {
			// Captures this generation's pipes, never mutable connection fields.
			session.writer.write(payload); session.writer.newLine(); session.writer.flush();
			return session.reader.readLine();
		});
		try {
			String line = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
			if (line == null) throw new EOFException();
			Response response = json.fromJson(Response.class, line);
			if (response == null) throw new IOException("Empty worker response");
			return response;
		} finally { if (!future.isDone()) future.cancel(true); }
	}

	/** Starts a fresh generation; authentication is restored by execute before any API call. */
	private Worker ensureProcess() throws IOException {
		Worker old;
		synchronized (lifecycle) {
			if (closed) throw new IOException("Connection closed");
			if (worker != null && worker.process.isAlive()) return worker;
			old = worker; worker = null;
		}
		if (old != null) old.stop();
		Worker fresh = new Worker(workerFactory.start(), ++generation);
		synchronized (lifecycle) {
			if (!closed) worker = fresh;
		}
		if (current(fresh)) {
			Logger.getGlobal().info("IR worker started : " + irName);
			return fresh;
		}
		fresh.stop();
		throw new IOException("Connection closed");
	}

	private boolean current(Worker session) { synchronized (lifecycle) { return !closed && worker == session; } }

	private void invalidate(Worker session) {
		synchronized (lifecycle) { if (worker == session) worker = null; }
		session.stop();
	}

	private record Invocation(Response response, boolean attempted) { }

	/** Owns one worker's I/O and bounded termination; close() never waits for the request lock. */
	private final class Worker {
		final Process process;
		final BufferedWriter writer;
		final BufferedReader reader;
		final ExecutorService io;
		final AtomicBoolean stopped = new AtomicBoolean();
		boolean authenticated;
		Worker(Process process, int generation) {
			this.process = process;
			writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
			reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
			io = Executors.newSingleThreadExecutor(task -> {
				Thread thread = new Thread(task, "isolated-ir-io-" + irName + "-" + generation);
				thread.setDaemon(true); return thread;
			});
		}
		void stop() {
			if (!stopped.compareAndSet(false, true)) return;
			// Terminate before touching BufferedReader/Writer locks held by blocked I/O.
			try (var descendants = process.descendants()) { descendants.forEach(ProcessHandle::destroyForcibly); }
			catch (RuntimeException e) { Logger.getGlobal().warning("IR descendant cleanup failed : " + irName); }
			try { process.destroyForcibly(); }
			catch (RuntimeException e) { Logger.getGlobal().warning("IR worker termination failed : " + irName); }
			io.shutdownNow();
			Thread cleanup = new Thread(() -> {
				closeQuietly(writer); closeQuietly(reader); closeQuietly(process.getErrorStream());
			}, "isolated-ir-cleanup-" + irName);
			cleanup.setDaemon(true); cleanup.start();
			try {
				process.waitFor(1, TimeUnit.SECONDS);
				cleanup.join(1000);
				io.awaitTermination(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			if (process.isAlive() || cleanup.isAlive() || !io.isTerminated())
				Logger.getGlobal().warning("IR worker cleanup still pending : " + irName);
		}
	}

	private static void closeQuietly(Closeable closeable) {
		if(closeable != null) {
			try {
				closeable.close();
			} catch(IOException e) {
			}
		}
	}

	/**
	 * workerと本体で共有するJSON設定を生成する。
	 * <p>
	 * protocol DTOは後方互換性を保ちやすいよう、未知フィールドを無視して読み込む。
	 * </p>
	 */
	static Json createJson() {
		Json json = new Json();
		// Worker output is untrusted: only the explicitly requested protocol DTO may be constructed.
		json.setTypeName(null);
		json.setIgnoreUnknownFields(true);
		json.setOutputType(OutputType.json);
		json.setUsePrototypes(false);
		return json;
	}
}
