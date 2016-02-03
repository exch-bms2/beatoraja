package bms.player.beatoraja;

import bms.player.beatoraja.input.KeyInputLog;
import bms.player.beatoraja.pattern.PatternModifyLog;

/**
 * リプレイデータ。キー入力ログ、譜面変更情報、ゲージ種類を含む
 * 
 * @author exch
 */
public class ReplayData {
	
	public KeyInputLog[] keylog;
	
	public int gauge;
	
	public PatternModifyLog[] pattern;
}
