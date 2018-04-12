package bms.player.beatoraja.pattern;

import java.util.ArrayList;
import java.util.List;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.model.TimeLine;

/**
 * タイムラインごとにノーツを入れ替えるクラス
 * @author KEH
 *
 */
public class NewNoteShuffleModifier extends PatternModifier {

	private Randomizer r;

	public NewNoteShuffleModifier(Random r, Mode mode) {
		super(r.assist);
		this.r = Randomizer.create(r, mode, 0);
	}

	public NewNoteShuffleModifier(Random r, int type, Mode mode) {
		super(r.assist);
		this.r = Randomizer.create(r, mode);
	}

	@Override
	public List<PatternModifyLog> modify(BMSModel model) {
		List<PatternModifyLog> log = new ArrayList<PatternModifyLog>();
		r.setModifyLanes(getKeys(model.getMode(), r.random.isScratchLaneModify));
		r.setMode(model.getMode());
		for(TimeLine tl : model.getAllTimeLines()) {
			if (tl.existNote() || tl.existHiddenNote()) {
				log.add(new PatternModifyLog(tl.getSection(), r.permutate(tl)));
			}
		}
		return log;
	}

}
