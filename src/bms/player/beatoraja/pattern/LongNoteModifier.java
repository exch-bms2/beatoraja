package bms.player.beatoraja.pattern;

import java.util.List;

import bms.model.*;

public class LongNoteModifier extends PatternModifier {

	public LongNoteModifier() {
		super(2);
	}
	
	@Override
	public List<PatternModifyLog> modify(BMSModel model) {
		for (TimeLine tl : model.getAllTimeLines()) {
			for(int lane = 0;lane < model.getMode().key;lane++) {
				if(tl.getNote(lane) instanceof LongNote) {
					LongNote ln = (LongNote) tl.getNote(lane);
					if(ln.getSection() == tl.getSection()) {
						tl.setNote(lane, new NormalNote(ln.getWav()));						
					} else {
						tl.setNote(lane, null);
					}
				}
			}
		}
		return null;
	}

}
