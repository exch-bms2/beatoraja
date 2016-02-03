package bms.player.beatoraja.pattern;

import java.util.List;

import bms.model.BMSModel;
import bms.model.TimeLine;

public class ConstantBPMModifier extends PatternModifier {

	public ConstantBPMModifier() {
		super(1);
	}
	
	@Override
	public List<PatternModifyLog> modify(BMSModel model) {
		for (int time : model.getAllTimes()) {
			TimeLine tl = model.getTimeLine(time);
			tl.setBPM(model.getBpm());
		}
		return null;
	}

}
