package bms.player.beatoraja.pattern;

import java.util.List;

import bms.model.BMSModel;
import bms.model.TimeLine;

/**
 * 指定のレーンを自動演奏にするオプション
 * 
 * @author exch
 */
public class AutoplayModifier extends PatternModifier {

	private int[] lanes;
	
	public AutoplayModifier(int[] lanes) {
		super(2);
		this.lanes = lanes;
	}
	@Override
	public List<PatternModifyLog> modify(BMSModel model) {
		for (TimeLine tl : model.getAllTimeLines()) {
			for(int lane : lanes) {
				if(tl.getNote(lane) != null) {
					tl.addBackGroundNote(tl.getNote(lane));
					tl.addNote(lane, null);
				}
			}
		}
		return null;
	}

}
