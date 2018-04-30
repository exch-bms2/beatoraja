package bms.player.beatoraja.pattern;

import bms.player.beatoraja.Validatable;

/**
 * 譜面変更ログ
 * 
 * @author exch
 */
public class PatternModifyLog implements Validatable {

	public static final PatternModifyLog[] EMPTYARRAY = new PatternModifyLog[0];
	/**
	 * 対象のTimeLineの小節
	 */
	public double section = -1;
	/**
	 * 対象のTimeLineの各レーンのノーツ移動先
	 */
	public int[] modify;
	
	public PatternModifyLog() {
		
	}
	
	public PatternModifyLog(double section, int[] modify) {
		this.section = section;
		this.modify = modify;
	}

	@Override
	public boolean validate() {
		return section >= 0 && modify != null;
	}

}