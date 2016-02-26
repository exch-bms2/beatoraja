package bms.player.beatoraja.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bms.model.BMSModel;
import bms.model.Note;
import bms.model.TimeLine;

/**
 * 譜面オプションの抽象クラス
 * 
 * @author exch
 */
public abstract class PatternModifier {

	private int assist;
	
	public PatternModifier(int assist) {
		this.assist = assist;
	}
	
	public abstract List<PatternModifyLog> modify(BMSModel model);
	
	public static void modify(BMSModel model, List<PatternModifyLog> log) {
		int lanes = 8;
		for (TimeLine tl : model.getAllTimeLines()) {
			Note[] notes = new Note[lanes];
			PatternModifyLog pm = null;
			for(PatternModifyLog pms : log) {
				if(pms.time == tl.getTime()) {
					pm = pms;
					break;
				}
			}
			if(pm != null) {
				for (int i = 0; i < lanes; i++) {
					notes[i] = tl.getNote(pm.modify[i]);
				}
				for (int i = 0; i < lanes; i++) {
					tl.addNote(i, notes[i]);
				}
				
			}
		}

	}
	
	public int getAssistLevel() {
		return assist;
	}
}
