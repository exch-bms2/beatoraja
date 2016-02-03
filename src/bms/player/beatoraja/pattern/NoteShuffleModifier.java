package bms.player.beatoraja.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bms.model.BMSModel;
import bms.model.Note;
import bms.model.TimeLine;

public class NoteShuffleModifier extends PatternModifier {

	// TODO CNが衝突するケースがあるので、これを回避する手段の実装
	
	public static final int S_RANDOM = 0;
	public static final int ALL_SCR = 1;
	public static final int H_RANDOM = 2;
	public static final int S_RANDOM_EX = 3;
	
	private int type;
	
	public NoteShuffleModifier(int type) {
		super(type != S_RANDOM ? 1 : 0);
		this.type = type;
	}

	@Override
	public List<PatternModifyLog> modify(BMSModel model) {
		List<PatternModifyLog> log = new ArrayList();
		int lanes = 8;
		int[] random = new int[0];
		for (int time : model.getAllTimes()) {
			TimeLine tl = model.getTimeLine(time);
			Note[] notes = new Note[lanes];
			for (int i = 0; i < lanes; i++) {
				notes[i] = tl.getNote(i);
			}
			switch (type) {
			case S_RANDOM:
			case ALL_SCR:
				List<Integer> l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6));
				random = new int[8];
				for (int lane = 0; lane < 7; lane++) {
					int r = (int) (Math.random() * l.size());
					random[lane] = l.get(r);
					l.remove(r);
				}
				random[7] = 7;
				break;
			case H_RANDOM:
			case S_RANDOM_EX:
				List<Integer> le = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
				random = new int[8];
				for (int lane = 0; lane < 8; lane++) {
					int re = (int) (Math.random() * le.size());
					random[lane] = le.get(re);
					le.remove(re);
				}
				break;

			}

			for (int i = 0; i < lanes; i++) {
				tl.addNote(i, notes[random[i]]);
			}
			log.add(new PatternModifyLog(time, random));
		}
		return log;
	}
}
