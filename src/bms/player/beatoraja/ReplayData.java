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
	public int random;
}
