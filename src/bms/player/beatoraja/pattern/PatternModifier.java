package bms.player.beatoraja.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
	
	private int type;
	
	public static final int PLAYER1_5KEYS = 0;
	public static final int PLAYER1_7KEYS = 1;
	public static final int PLAYER2_5KEYS = 2;
	public static final int PLAYER2_7KEYS = 3;
	public static final int NINEKEYS = 4;
	
	public PatternModifier(int assist) {
		this.assist = assist;
	}
	
	public abstract List<PatternModifyLog> modify(BMSModel model);
	
	public static void modify(BMSModel model, List<PatternModifyLog> log) {
		for (TimeLine tl : model.getAllTimeLines()) {
			PatternModifyLog pm = null;
			for(PatternModifyLog pms : log) {
				if(pms.time == tl.getTime()) {
					pm = pms;
					break;
				}
			}
			if(pm != null) {
				int lanes = pm.modify.length;				
				Note[] notes = new Note[lanes];
				Note[] hnotes = new Note[lanes];
				for (int i = 0; i < lanes; i++) {
					notes[i] = tl.getNote(pm.modify[i]);
					hnotes[i] = tl.getHiddenNote(pm.modify[i]);
				}
				for (int i = 0; i < lanes; i++) {
					tl.setNote(i, notes[i]);
					tl.setHiddenNote(i, hnotes[i]);
				}
				
			}
		}

	}
	
	public static List<PatternModifyLog> merge(List<PatternModifyLog> log, List<PatternModifyLog> log2) {
		List<PatternModifyLog> result = new ArrayList(Math.max(log.size(), log2.size()));
		for(PatternModifyLog pml : log) {
			boolean b = true;
			for(PatternModifyLog pml2 : log2) {
				if(pml.time == pml2.time) {
					int[] newmod = new int[pml.modify.length];
					for(int i = 0;i < pml.modify.length;i++) {
						newmod[i] = pml.modify[pml2.modify[i]];
					}
					result.add(new PatternModifyLog(pml.time, newmod));
					b = true;
					break;
				}
			}
			if(b) {
				result.add(pml);
			}
		}
		
		for(PatternModifyLog pml2 : log2) {
			boolean b = true;
			for(PatternModifyLog pml : log) {
				if(pml2.time == pml.time) {
					b = false;
					break;
				}
			}
			if(b) {
				for(int index = 0;index < result.size();index++) {
					if(pml2.time < result.get(index).time) {
						result.add(index, pml2);
						b = false;
						break;
					}
				}
				if(b) {
					result.add(pml2);
				}
			}
		}
		return result;
	}
	
	public int getAssistLevel() {
		return assist;
	}

	public int getModifyTarget() {
		return type;
	}

	public void setModifyTarget(int type) {
		this.type = type;
	}
	
	public static PatternModifier create(int id, int type) {
		PatternModifier pm = null;
		switch(id) {
		case 0:
			pm = new DummyModifier();
			break;
		case 1:
			pm = new LaneShuffleModifier(LaneShuffleModifier.MIRROR);
			break;
		case 2:
			pm = new LaneShuffleModifier(LaneShuffleModifier.RANDOM);
			break;
		case 3:
			pm = new LaneShuffleModifier(LaneShuffleModifier.R_RANDOM);
			break;
		case 4:
			pm = new NoteShuffleModifier(NoteShuffleModifier.S_RANDOM);
			break;
		case 5:
			pm = new NoteShuffleModifier(NoteShuffleModifier.SPIRAL);
			break;
		case 6:
			pm = new NoteShuffleModifier(NoteShuffleModifier.H_RANDOM);
			break;
		case 7:
			pm = new NoteShuffleModifier(NoteShuffleModifier.ALL_SCR);
			break;
		case 8:
			pm = new LaneShuffleModifier(LaneShuffleModifier.RANDOM_EX);
			break;
		case 9:
			pm = new NoteShuffleModifier(NoteShuffleModifier.S_RANDOM_EX);	
			break;
		}
		
		if(pm != null) {
			pm.setModifyTarget(type);
		}
 		return pm;
	}
	
	static class DummyModifier extends PatternModifier {

		public DummyModifier() {
			super(0);
		}

		@Override
		public List<PatternModifyLog> modify(BMSModel model) {
			return Collections.emptyList();
		}
		
	}
}
