package bms.player.beatoraja.pattern;

import java.util.List;

import bms.model.*;

public class LongNoteModifier extends PatternModifier {

	private boolean exists = false;
	
	public LongNoteModifier() {
		super(2);
	}
	
	@Override
	public List<PatternModifyLog> modify(BMSModel model) {
		for (TimeLine tl : model.getAllTimeLines()) {
			for(int lane = 0;lane < model.getMode().key;lane++) {
				if(tl.getNote(lane) instanceof LongNote) {
					LongNote ln = (LongNote) tl.getNote(lane);
					if(ln.isEnd()) {
						tl.setNote(lane, null);
					} else {
						tl.setNote(lane, new NormalNote(ln.getWav()));						
					}
					exists = true;
				}
			}
		}
		return null;
	}

	public boolean longNoteExists() {
		return exists;
	}
}
