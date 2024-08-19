package bms.player.beatoraja.pattern;

import bms.model.*;

public class MineNoteModifier extends PatternModifier {

	private boolean exists = false;

	private Mode mode = Mode.REMOVE;

	private int damage = 10;

	public MineNoteModifier() {
	}

	public MineNoteModifier(int mode) {
		this.mode = Mode.values()[mode];
	}

	@Override
	public void modify(BMSModel model) {
		if(mode == Mode.REMOVE) {
			AssistLevel assist = AssistLevel.NONE;
			for (TimeLine tl : model.getAllTimeLines()) {
				for(int lane = 0;lane < model.getMode().key;lane++) {
					if(tl.getNote(lane) instanceof MineNote) {
						assist = AssistLevel.LIGHT_ASSIST;
						exists = true;
						tl.setNote(lane, null);
					}
				}
			}
			setAssistLevel(assist);
		} else {
			TimeLine[] tls = model.getAllTimeLines();
			boolean[] ln = new boolean[model.getMode().key];
			boolean[] blank = new boolean[model.getMode().key];

			for (int i = 0;i < tls.length;i++) {
				final TimeLine tl = tls[i];

				for(int key = 0;key < model.getMode().key;key++) {
					final Note note = tl.getNote(key);
					if(note instanceof LongNote) {
						ln[key] = !((LongNote) note).isEnd();
					}
					blank[key] = !ln[key] && note == null;
				}

				for(int key = 0;key < model.getMode().key;key++) {
					if(blank[key]) {
						switch (mode) {
							case ADD_RANDOM -> {
								if(Math.random() > 0.9) {
									tl.setNote(key, new MineNote(-1, damage));
								}
							}
							case ADD_NEAR -> {
								if((key > 0 && !blank[key - 1]) || (key < model.getMode().key - 1 && !blank[key + 1])) {
									tl.setNote(key, new MineNote(-1, damage));
								}							
							}
							case ADD_BLANK -> {
								tl.setNote(key, new MineNote(-1, damage));
							}
						}
					}
				}
			}			
		}
	}
	
	public boolean mineNoteExists() {
		return exists;
	}

	public enum Mode {
		REMOVE, ADD_RANDOM, ADD_NEAR, ADD_BLANK;
	}
}
