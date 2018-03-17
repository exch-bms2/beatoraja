package bms.player.beatoraja;

import lombok.Data;

@Data
public class PlayerData {

	/**
	 * プレイヤーデータ取得日時(unixtime, 1日刻み)
	 */
	private long date = 0;	
	/**
	 * 総プレイ回数
	 */
	private long playcount = 0;
	/**
	 * 総クリア回数
	 */
	private long clear = 0;
	/**
	 * 総PGREATノート数
	 */
	private long epg = 0;
	private long lpg = 0;
	/**
	 * 総GREATノート数
	 */
	private long egr = 0;
	private long lgr = 0;
	/**
	 * 総GOODノート数
	 */
	private long egd = 0;
	private long lgd = 0;
	/**
	 * 総BADノート数
	 */
	private long ebd = 0;
	private long lbd = 0;
	/**
	 * 総POORノート数
	 */
	private long epr = 0;
	private long lpr = 0;
	/**
	 * 総MISSノート数
	 */
	private long ems = 0;
	private long lms = 0;
	/**
	 * 総プレイ時間
	 */
	private long playtime = 0;
	private long maxcombo = 0;
}
