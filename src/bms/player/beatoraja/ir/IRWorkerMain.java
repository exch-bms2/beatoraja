package bms.player.beatoraja.ir;

import static bms.player.beatoraja.ir.IRWorkerProtocol.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

import com.badlogic.gdx.utils.Json;

/**
 * 分離IRプロセスのエントリポイント。
 * <p>
 * 第1引数で指定されたIR名の実装をin-processで生成し、標準入力から
 * {@link IRWorkerProtocol.Request}を1行ずつ読み込んで、標準出力へ
 * {@link IRWorkerProtocol.Response}を1行JSONとして返す。
 * </p>
 */
public final class IRWorkerMain {

	private IRWorkerMain() {
	}

	public static void main(String[] args) {
		if(args.length == 0) {
			System.err.println("IR worker requires IR name");
			return;
		}

		Json json = IsolatedIRConnection.createJson();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FileDescriptor.out),
					StandardCharsets.UTF_8));
			// IR実装がSystem.outへログを出してもJSON応答ストリームを壊さないようにする。
			System.setOut(System.err);

			IRConnection connection = IRConnectionManager.getIRConnectionInProcess(args[0]);
			if(connection == null) {
				write(writer, json, Response.error("IR connection not found : " + args[0]));
				return;
			}

			String line;
			while((line = reader.readLine()) != null) {
				write(writer, json, handle(connection, json.fromJson(Request.class, line)));
			}
		} catch(Throwable e) {
			e.printStackTrace(System.err);
		}
	}

	/**
	 * JSON-RPC相当のmethod名を既存{@link IRConnection}メソッドへ割り当てる。
	 */
	private static Response handle(IRConnection connection, Request request) {
		try {
			return switch(request.method) {
				case METHOD_REGISTER -> playerResponse(connection.register(request.account.toIRAccount()));
				case METHOD_LOGIN -> playerResponse(connection.login(request.account.toIRAccount()));
				case METHOD_GET_RIVALS -> playersResponse(connection.getRivals());
				case METHOD_GET_TABLE_DATAS -> tablesResponse(connection.getTableDatas());
				case METHOD_GET_PLAY_DATA -> scoresResponse(connection.getPlayData(
						request.player != null ? request.player.toIRPlayerData() : null,
						request.chart != null ? request.chart.toIRChartData() : null));
				case METHOD_GET_COURSE_PLAY_DATA -> scoresResponse(connection.getCoursePlayData(
						request.player != null ? request.player.toIRPlayerData() : null,
						request.course != null ? request.course.toIRCourseData() : null));
				case METHOD_SEND_PLAY_DATA -> objectResponse(connection.sendPlayData(
						request.chart != null ? request.chart.toIRChartData() : null,
						request.score != null ? request.score.toIRScoreData() : null));
				case METHOD_SEND_COURSE_PLAY_DATA -> objectResponse(connection.sendCoursePlayData(
						request.course != null ? request.course.toIRCourseData() : null,
						request.score != null ? request.score.toIRScoreData() : null));
				case METHOD_GET_SONG_URL -> textResponse(connection.getSongURL(
						request.chart != null ? request.chart.toIRChartData() : null));
				case METHOD_GET_COURSE_URL -> textResponse(connection.getCourseURL(
						request.course != null ? request.course.toIRCourseData() : null));
				case METHOD_GET_PLAYER_URL -> textResponse(connection.getPlayerURL(
						request.player != null ? request.player.toIRPlayerData() : null));
				case METHOD_GET_VERSION_INFO -> versionResponse(connection.getVersionInfo(request.currentVersion));
				case METHOD_GET_ILLEGAL_SONGS -> illegalSongsResponse(connection.getIllegalSongs());
				default -> Response.error("Unknown IR worker method : " + request.method);
			};
		} catch(Throwable e) {
			return Response.error("IR worker method error : " + e.getMessage());
		}
	}

	private static Response playerResponse(IRResponse<IRPlayerData> irResponse) {
		Response response = base(irResponse);
		response.player = Player.from(irResponse.getData());
		return response;
	}

	private static Response playersResponse(IRResponse<IRPlayerData[]> irResponse) {
		Response response = base(irResponse);
		response.players = fromPlayers(irResponse.getData());
		return response;
	}

	private static Response tablesResponse(IRResponse<IRTableData[]> irResponse) {
		Response response = base(irResponse);
		response.tables = fromTables(irResponse.getData());
		return response;
	}

	private static Response scoresResponse(IRResponse<IRScoreData[]> irResponse) {
		Response response = base(irResponse);
		response.scores = fromScores(irResponse.getData());
		return response;
	}

	private static Response objectResponse(IRResponse<Object> irResponse) {
		return base(irResponse);
	}

	private static Response textResponse(String text) {
		Response response = new Response();
		response.succeeded = text != null;
		response.text = text;
		response.message = "";
		return response;
	}

	private static Response versionResponse(IRResponse<IRVersionInfo> irResponse) {
		Response response = base(irResponse);
		response.version = Version.from(irResponse.getData());
		return response;
	}

	private static Response illegalSongsResponse(IRResponse<String[]> irResponse) {
		Response response = base(irResponse);
		response.illegalSongs = irResponse.getData();
		return response;
	}

	private static Response base(IRResponse<?> irResponse) {
		if(irResponse == null) {
			return Response.error("IR returned null response");
		}
		Response response = new Response();
		response.succeeded = irResponse.isSucceeded();
		response.message = irResponse.getMessage();
		return response;
	}

	private static void write(BufferedWriter writer, Json json, Response response) throws IOException {
		writer.write(json.toJson(response));
		writer.newLine();
		writer.flush();
	}
}
