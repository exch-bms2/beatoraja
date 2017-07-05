package bms.player.beatoraja.pattern;

import java.util.List;

import bms.model.BMSModel;
import bms.model.TimeLine;

/**
 * BPMを一定化し、ストップシーケンスを無効化するオプション
 *
 * @author exch
 */
public class ConstantBPMModifier extends PatternModifier {
	
	public ConstantBPMModifier() {
		super(1);
	}
	
	@Override
	public List<PatternModifyLog> modify(BMSModel model) {
		for (TimeLine tl : model.getAllTimeLines()) {
			tl.setSection(model.getBpm() * tl.getMicroTime() / 240000000);
			tl.setStop(0);
			tl.setBPM(model.getBpm());
		}
		return null;
	}

}
