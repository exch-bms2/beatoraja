package bms.player.beatoraja.pattern;

import java.util.List;

import bms.model.BMSModel;
import bms.model.TimeLine;

public class PracticeModifier extends PatternModifier {

	private int start;
	private int end;
	
	public PracticeModifier(int start, int end) {
		super(2);
		this.start = start;
		this.end = end;		
	}
	
	@Override
	public List<PatternModifyLog> modify(BMSModel model) {
		int totalnotes = model.getTotalNotes();
		for (TimeLine tl : model.getAllTimeLines()) {
			if(tl.getSection() < start || tl.getSection() >= end) {
				for(int i =0;i < 18;i++) {
					if(tl.getNote(i) != null) {
						tl.addBackGroundNote(tl.getNote(i));
						tl.setNote(i, null);
					}
				}				
			}
		}
		model.setTotal(model.getTotal() * model.getTotalNotes() / totalnotes);
		
		return null;
	}

}
