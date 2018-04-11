package bms.player.beatoraja.pattern;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import bms.model.BMSDecoder;
import bms.model.BMSModel;

class RandomizerTest {

	BMSModel testModel;
	BMSDecoder d;

	@Test
	public void SRandomizerTest() {
		// File f = new File("S:\\__BMSDATA__\\BMSALL\\M\\minefield surfing\\37_surfing_spx.bme");
		// File f = new File("S:\\__BMSDATA__\\BMSALL\\A\\A BEAUTIFUL WINGS\\ABW7extra.bme");
		File f = new File("S:\\__BMSDATA__\\BMSALL\\N\\Nan\\_934_Nan_UndefinedLN.bml");
		d = new BMSDecoder();
		testModel = d.decode(f);
		PatternModifier pm = new NewNoteShuffleModifier(Random.SPIRAL, testModel.getMode());
		pm.setModifyTarget(0);
		List<PatternModifyLog> log = pm.modify(testModel);
	}

}
