package bms.player.beatoraja.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bms.model.BMSModel;
import bms.model.Note;
import bms.model.TimeLine;

/**
 * レーン単位でノーツを入れ替えるオプション MIRROR、RANDOM、R-RANDOMが該当する
 * 
 * @author exch
 */
public class LaneShuffleModifier extends PatternModifier {

	private int[] random;

	public static final int MIRROR = 0;
	public static final int R_RANDOM = 1;
	public static final int RANDOM = 2;
	public static final int RANDOM_EX = 3;

	public LaneShuffleModifier(int type) {
		super(type == RANDOM_EX ? 1 : 0);
		switch (type) {
		case MIRROR:
			random = new int[] { 6, 5, 4, 3, 2, 1, 0, 7 };
			break;
		case R_RANDOM:
			int i = (int) (Math.random() * 6);
			int j = (int) (Math.random() * 2);
			random = new int[8];
			for (int lane = 0; lane < 7; lane++) {
				i = (i + 1) % 7;
				random[lane] = (j == 0 ? i : 6 - i);
			}
			random[7] = 7;
			break;
		case RANDOM:
			List<Integer> l = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6));
			random = new int[8];
			for (int lane = 0; lane < 7; lane++) {
				int r = (int) (Math.random() * l.size());
				random[lane] = l.get(r);
				l.remove(r);
			}
			random[7] = 7;
			break;
		case RANDOM_EX:
			List<Integer> le = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
			random = new int[8];
			for (int lane = 0; lane < 8; lane++) {
				int re = (int) (Math.random() * le.size());
				random[lane] = le.get(re);
				le.remove(re);
			}
			break;

		}
	}

	@Override
	public List<PatternModifyLog> modify(BMSModel model) {
		List<PatternModifyLog> log = new ArrayList();
		int lanes = 8;
		for (TimeLine tl : model.getAllTimeLines()) {
			Note[] notes = new Note[lanes];
			for (int i = 0; i < lanes; i++) {
				notes[i] = tl.getNote(i);
			}
			for (int i = 0; i < lanes; i++) {
				tl.addNote(i, notes[random[i]]);
			}
			log.add(new PatternModifyLog(tl.getTime(), random));
		}
		return log;
	}

}
