package bms.player.beatoraja.ir;

import static bms.player.beatoraja.ir.IRWorkerProtocol.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.*;
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

	private static final long REQUEST_TIMEOUT_SECONDS = 30;

	private final String irName;
	private final Json json = createJson();
	private final ExecutorService readerExecutor;

	private Process process;
	private BufferedWriter writer;
	private BufferedReader reader;

	public IsolatedIRConnection(String irName) {
		this.irName = irName;
		readerExecutor = Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "isolated-ir-reader-" + irName);
			thread.setDaemon(true);
			return thread;
		});
	}

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
		Response response = invoke(request);
		return new SimpleIRResponse<>(response.succeeded, response.message, null);
	}

	@Override
	public IRResponse<Object> sendCoursePlayData(IRCourseData course, IRScoreData score) {
		Request request = new Request();
		request.method = METHOD_SEND_COURSE_PLAY_DATA;
		request.course = Course.from(course);
		request.score = Score.from(score);
		Response response = invoke(request);
		return new SimpleIRResponse<>(response.succeeded, response.message, null);
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
		destroyProcess();
		readerExecutor.shutdownNow();
	}

	private synchronized Response invoke(Request request) {
		Future<String> future = null;
		try {
			ensureProcess();
			writer.write(json.toJson(request));
			writer.newLine();
			writer.flush();
			future = readerExecutor.submit(() -> reader.readLine());
			String line = future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			if(line == null) {
				destroyProcess();
				return Response.error("IR worker exited");
			}
			return json.fromJson(Response.class, line);
		} catch(TimeoutException e) {
			if(future != null) {
				future.cancel(true);
			}
			destroyProcess();
			return Response.error("IR worker timeout");
		} catch(Throwable e) {
			destroyProcess();
			return Response.error("IR worker error : " + e.getMessage());
		}
	}

	/**
	 * workerが未起動、または前回の異常で終了している場合に起動する。
	 * <p>
	 * classpathは現在のJVMと同じものを引き継ぎ、worker側で従来の
	 * {@link IRConnectionManager}探索を使ってIR実装を生成する。
	 * </p>
	 */
	private void ensureProcess() throws IOException {
		if(process != null && process.isAlive()) {
			return;
		}
		ProcessBuilder builder = new ProcessBuilder(
				Path.of(System.getProperty("java.home"), "bin", javaCommand()).toString(),
				"-cp",
				System.getProperty("java.class.path"),
				IRWorkerMain.class.getName(),
				irName);
		builder.redirectError(ProcessBuilder.Redirect.INHERIT);
		process = builder.start();
		writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
		reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
		Logger.getGlobal().info("IR worker started : " + irName);
	}

	/**
	 * 通信異常やタイムアウト時にworkerを破棄する。
	 * <p>
	 * 次回のIR API呼び出しでは新しいworkerが起動される。
	 * </p>
	 */
	private void destroyProcess() {
		closeQuietly(writer);
		closeQuietly(reader);
		writer = null;
		reader = null;
		if(process != null) {
			process.destroyForcibly();
			process = null;
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

	private static String javaCommand() {
		return System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java";
	}

	/**
	 * workerと本体で共有するJSON設定を生成する。
	 * <p>
	 * protocol DTOは後方互換性を保ちやすいよう、未知フィールドを無視して読み込む。
	 * </p>
	 */
	static Json createJson() {
		Json json = new Json();
		json.setIgnoreUnknownFields(true);
		json.setOutputType(OutputType.json);
		json.setUsePrototypes(false);
		return json;
	}
}
