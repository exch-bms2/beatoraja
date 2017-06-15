package bms.player.beatoraja.pattern;

/**
 * 譜面変更ログ
 * 
 * @author exch
 */
public class PatternModifyLog {

	// TODO time廃止(0.5以降)

	/**
	 * 対象のTimeLineの時間(ms)。下位互換のために残しており、廃止予定
	 */
	public int time = -1;
	/**
	 * 対象のTimeLineの小節
	 */
	public double section = -1;
	/**
	 * 対象のTimeLineの各レーンのノーツ異動先
	 */
	public int[] modify;
	
	public PatternModifyLog() {
		
	}
	
	public PatternModifyLog(double section, int[] modify) {
		this.section = section;
		this.modify = modify;
	}

}