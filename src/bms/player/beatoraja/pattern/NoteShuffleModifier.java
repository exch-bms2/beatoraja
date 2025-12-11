package bms.player.beatoraja.pattern;

import java.util.ArrayList;
import java.util.List;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.model.TimeLine;
import bms.player.beatoraja.PlayerConfig;

/**
 * タイムライン単位でノーツを入れ替えるためのクラス．
 *
 * @author KEH
 */
public class NoteShuffleModifier extends PatternModifier {
	
	private Randomizer randomizer;
	private boolean isScratchLaneModify;

	public NoteShuffleModifier(Random r, Mode mode, PlayerConfig config) {
		this(r, 0, mode, config);
	}

	public NoteShuffleModifier(Random r, int player, Mode mode, PlayerConfig config) {
		super(player);
		this.isScratchLaneModify = r.isScratchLaneModify;
		this.randomizer = Randomizer.create(r, player, mode, config);
	}

	@Override
	public void modify(BMSModel model) {
		randomizer.setRandomSeed(getSeed());
		randomizer.setModifyLanes(getKeys(model.getMode(), player, isScratchLaneModify));
		for(TimeLine tl : model.getAllTimeLines()) {
			if (tl.existNote() || tl.existHiddenNote()) {
				randomizer.permutate(tl);
			}
		}
		setAssistLevel(randomizer.getAssistLevel());
	}
}
