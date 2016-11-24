package bms.player.beatoraja.pattern;

import java.util.List;

import bms.model.*;

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
			for (int i = 0; i < 18; i++) {
				if (tl.getNote(i) != null) {
					if (tl.getNote(i) instanceof LongNote) {
						LongNote ln = (LongNote) tl.getNote(i);
						if (ln.getSection() < start || ln.getEndnote().getSection() >= end) {
							tl.addBackGroundNote(tl.getNote(i));
							tl.setNote(i, null);							
						}
					} else {
						if (tl.getSection() < start || tl.getSection() >= end) {
							tl.addBackGroundNote(tl.getNote(i));
							tl.setNote(i, null);
						}
					}
				}
			}
		}
		model.setTotal(model.getTotal() * model.getTotalNotes() / totalnotes);

		return null;
	}

}
