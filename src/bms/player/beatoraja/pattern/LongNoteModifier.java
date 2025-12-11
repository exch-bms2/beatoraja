package bms.player.beatoraja.pattern;

import bms.model.*;

/**
 * ロングノーツを除去/追加する譜面オプション
 *
 * @author exch
 */
public class LongNoteModifier extends PatternModifier {

	private Mode mode = Mode.REMOVE;

	private double rate = 1.0;

	public LongNoteModifier() {
	}

	public LongNoteModifier(int mode, double rate) {
		this.mode = Mode.values()[mode];
		this.rate = rate;
	}

	@Override
	public void modify(BMSModel model) {

		if(mode == Mode.REMOVE) {
			AssistLevel assist = AssistLevel.NONE;
			for (TimeLine tl : model.getAllTimeLines()) {
				for(int lane = 0;lane < model.getMode().key;lane++) {
					if(tl.getNote(lane) instanceof LongNote ln && Math.random() < rate) {
						tl.setNote(lane, ln.isEnd() ? null : new NormalNote(ln.getWav()));
						assist = AssistLevel.ASSIST;
					}
				}
			}
			setAssistLevel(assist);
		} else {
			int r = 0;

			AssistLevel assist = AssistLevel.NONE;

			TimeLine[] tls = model.getAllTimeLines();
			for (int i = 0;i < tls.length - 1;i++) {
				for(int lane = 0;lane < model.getMode().key;lane++) {
					if(tls[i].getNote(lane) instanceof NormalNote && !tls[i + 1].existNote(lane) && Math.random() < rate) {
						int lntype = switch(mode) {
							case ADD_LN -> LongNote.TYPE_LONGNOTE;
							case ADD_CN -> LongNote.TYPE_CHARGENOTE;
							case ADD_HCN -> LongNote.TYPE_HELLCHARGENOTE;
							case ADD_ALL -> (int) (Math.random() * 3 + 1);
							default -> LongNote.TYPE_UNDEFINED;
						};

						if(lntype != LongNote.TYPE_LONGNOTE) {
							assist = AssistLevel.ASSIST;
						}

						LongNote lnstart = new LongNote(tls[i].getNote(lane).getWav(),tls[i].getNote(lane).getMicroStarttime(),tls[i].getNote(lane).getMicroDuration());
						lnstart.setType(lntype);
						LongNote lnend = new LongNote(-2);

						tls[i].setNote(lane, lnstart);
						tls[i + 1].setNote(lane, lnend);
						lnstart.setPair(lnend);
					}
				}
			}
			setAssistLevel(assist);
		}
	}

	public enum Mode {
		REMOVE, ADD_LN, ADD_CN, ADD_HCN, ADD_ALL;
	}

}
