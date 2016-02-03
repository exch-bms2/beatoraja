package bms.player.beatoraja.pattern;

/**
 * 譜面変更ログ
 * 
 * @author exch
 */
public class PatternModifyLog {
	public int time;
	public int[]  modify;
	
	public PatternModifyLog() {
		
	}
	
	public PatternModifyLog(int time, int[] modify) {
		this.time = time;
		this.modify = modify;
	}
}