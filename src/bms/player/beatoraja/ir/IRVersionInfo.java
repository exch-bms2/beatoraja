package bms.player.beatoraja.ir;

/**
 * IRが提供するbeatoraja更新情報。
 */
public class IRVersionInfo {

	/**
	 * 最新バージョン名。例: {@code beatoraja 0.8.9}
	 */
	public final String version;

	/**
	 * UIに表示する更新メッセージ。空の場合は本体側で生成する。
	 */
	public final String message;

	/**
	 * ダウンロードURL。
	 */
	public final String downloadURL;

	public IRVersionInfo(String version, String message, String downloadURL) {
		this.version = version;
		this.message = message;
		this.downloadURL = downloadURL;
	}
}
