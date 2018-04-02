package bms.player.test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bms.model.BMSDecoder;
import bms.model.BMSModel;
import bms.model.Note;
import bms.model.TimeLine;
import bms.player.beatoraja.pattern.AutoplayModifier;
import bms.player.beatoraja.pattern.LaneShuffleModifier;
import bms.player.beatoraja.pattern.PatternModifyLog;

public class AutoplayModifierTest {
	static BMSModel bmsModel;
	static BMSDecoder bmsDecoder;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File bmsFile = new File("C:\\Users\\dhehd\\Desktop\\end_time_dpnep.bms");
		bmsDecoder = new BMSDecoder();
		bmsModel = bmsDecoder.decode(bmsFile);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	/*
	 * Given lane number of the play,
	 * Those lanes will be played automatically.
	 * ( Player doesn`t need to catch notes. )
	 * */
	@Test
	public void AutoplayModifierConstructTest() {
		int lanes[] = {0, 1};
		AutoplayModifier autoplayModifier = new AutoplayModifier(lanes);
		assertNotNull(autoplayModifier);
	}

	@Test
	public void modifyTest() {
		int lanes[] = {0, 1};
		AutoplayModifier autoplayModifier = new AutoplayModifier(lanes);
		Note[] before = getNote(bmsModel);
		autoplayModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
		
	}
	
	@Test
	public void modifyWithMargin() {
		int lanes[] = {0, 1};
		AutoplayModifier autoplayModifier = new AutoplayModifier(lanes,1);
		Note[] before = getNote(bmsModel);
		autoplayModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	
	public Note[] getNote(BMSModel model) {
		List<PatternModifyLog> log = new ArrayList();
		int lanes = model.getMode().key;
		TimeLine[] timelines = model.getAllTimeLines();
		for (int index = 0; index < timelines.length; index++) {
			final TimeLine tl = timelines[index];
			if (tl.existNote() || tl.existHiddenNote()) {
				Note[] notes = new Note[lanes];
				for (int i = 0; i < lanes; i++) {
					notes[i] = tl.getNote(i);
					return notes;
				}
			}
		}
		return null;
	}
}
