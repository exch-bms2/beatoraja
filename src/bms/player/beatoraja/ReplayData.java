package bms.player.beatoraja;

import bms.player.beatoraja.input.KeyInputLog;
import bms.player.beatoraja.pattern.PatternModifyLog;
import static bms.player.beatoraja.PlayerConfig.*;

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
	 * ハイスピード固定。固定する場合はデュレーションが有効となり、固定しない場合はハイスピードが有効になる
	 */
	public int fixhispeed = FIX_HISPEED_MAINBPM;
	/**
	 * ハイスピード。1.0で等速
	 */
	public float hispeed = 1.0f;
	/**
	 * デュレーション(ノーツ表示時間)
	 */
	public int duration = 500;
	/**
	 * レーンカバー表示量(0-1)
	 */
	public float lanecover = 0.2f;
	/**
	 * レーンカバー使用
	 */
	public boolean enablelanecover = true;
	/**
	 * リフト表示量(0-1)
	 */
	public float lift = 0.1f;
	/**
	 * リフト使用
	 */
	public boolean enablelift = false;
}
