package bms.player.beatoraja;

import bms.player.beatoraja.input.KeyInputLog;
import bms.player.beatoraja.pattern.PatternModifyLog;

/**
 * リプレイデータ。キー入力ログ、譜面変更情報、ゲージ種類を含む
 * 
 * @author exch
 */
public class ReplayData {

	/**
	 * プレイヤー名
	 */
	public String player;
	/**
	 * 楽曲のSHA-256
 	 */
	public String sha256;
	/**
	 * モード
	 */
	public int mode;
	/**
	 * キー入力ログ
	 */
	public KeyInputLog[] keylog;
	/**
	 * ゲージの種類
	 */
	public int gauge;
	/**
	 * 譜面オプションによる変更ログ
	 */
	public PatternModifyLog[] pattern;
	/**
	 * ランダムシーケンスを含むbmsの場合、選択されたRANDOM番号
	 */
	public int[] rand = new int[0];
	/**
	 * プレイ日時(unixtime)
	 */
	public long date = 0;
	/**
	 * 7to9配置
	 */
	public int sevenToNinePattern = 0;
	/**
	 * 譜面オプション
	 */
	public int randomoption = 0;
	/**
	 * 譜面オプション(2P)
	 */
	public int randomoption2 = 0;
	/**
	 * DP用オプション
	 */
	public int doubleoption = 0;
	/**
	 * プレイコンフィグ
	 */
	public PlayConfig config;
}
