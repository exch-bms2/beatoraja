package bms.player.beatoraja.pattern;

import java.util.List;

import bms.model.*;

public class LongNoteModifier extends PatternModifier {

	private boolean exists = false;

	private Mode mode = Mode.REMOVE;

	private int rate = 50;

	public LongNoteModifier() {
	}

	public LongNoteModifier(int mode) {
		this.mode = Mode.values()[mode];
	}

	@Override
	public List<PatternModifyLog> modify(BMSModel model) {

		if(mode == Mode.REMOVE) {
			AssistLevel assist = AssistLevel.NONE;
			for (TimeLine tl : model.getAllTimeLines()) {
				for(int lane = 0;lane < model.getMode().key;lane++) {
					if(tl.getNote(lane) instanceof LongNote) {
						LongNote ln = (LongNote) tl.getNote(lane);
						if(ln.isEnd()) {
							tl.setNote(lane, null);
						} else {
							tl.setNote(lane, new NormalNote(ln.getWav()));
						}
						assist = AssistLevel.ASSIST;
						exists = true;
					}
				}
			}
			setAssistLevel(assist);
			return null;
		}

		int r = 0;

		AssistLevel assist = AssistLevel.NONE;

		TimeLine[] tls = model.getAllTimeLines();
		for (int i = 0;i < tls.length - 1;i++) {
			for(int lane = 0;lane < model.getMode().key;lane++) {
				if(tls[i].getNote(lane) instanceof NormalNote && !tls[i + 1].existNote(lane)) {
					int lntype = LongNote.TYPE_UNDEFINED;
					switch(mode) {
						case ADD_LN:
							lntype = LongNote.TYPE_LONGNOTE;
							break;
						case ADD_CN:
							lntype = LongNote.TYPE_CHARGENOTE;
							break;
						case ADD_HCN:
							lntype = LongNote.TYPE_HELLCHARGENOTE;
							break;
						case ADD_ALL:
							lntype = (int) (Math.random() * 3 + 1);
							break;
					}

					if(lntype != LongNote.TYPE_LONGNOTE) {
						assist = AssistLevel.ASSIST;
					}

					LongNote lnstart = new LongNote(tls[i].getNote(lane).getWav());
					lnstart.setType(lntype);
					LongNote lnend = new LongNote(-2);

					tls[i].setNote(lane, lnstart);
					tls[i + 1].setNote(lane, lnend);
					lnstart.setPair(lnend);
				}
			}
		}
		setAssistLevel(assist);
		return null;

	}

	public boolean longNoteExists() {
		return exists;
	}

	public enum Mode {
		REMOVE, ADD_LN, ADD_CN, ADD_HCN, ADD_ALL;
	}

}
