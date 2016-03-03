package bms.player.beatoraja.pattern;

import java.util.List;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.MineNote;
import bms.model.NormalNote;
import bms.model.TimeLine;

public class MineNoteModifier extends PatternModifier {

	private boolean exists = false;
	
	public MineNoteModifier() {
		super(2);
	}
	
	@Override
	public List<PatternModifyLog> modify(BMSModel model) {
		for (TimeLine tl : model.getAllTimeLines()) {
			for(int lane = 0;lane < 18;lane++) {
				if(tl.getNote(lane) instanceof MineNote) {
					exists = true;
					tl.addNote(lane, null);
				}
			}
		}
		return null;
	}
	
	public boolean mineNoteExists() {
		return exists;
	}

}
