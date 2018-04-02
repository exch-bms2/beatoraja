package bms.player.test.pattern;

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
		File bmsFile = new File("src\\bms\\player\\test\\end_time_dpnep.bms");
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
	public void AutoPlayModifierConstructWithoutLanesTest() {
		int lanes[] = null;
		AutoplayModifier autoplayModifier = new AutoplayModifier(lanes);
		assertNotNull(autoplayModifier);
	}
	
	@Test
	public void AutoPlayModifierWithMassLanesTest() {
		int lanes[] = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17
				,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32};
		AutoplayModifier autoplayModifier = new AutoplayModifier(lanes);
		assertNotNull(autoplayModifier);
	}
	
	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void modifyWithMassLanesTest() {
		int lanes[] = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17
				,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32};
		AutoplayModifier autoplayModifier = new AutoplayModifier(lanes);
		Note[] before = getNote(bmsModel);
		autoplayModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void modifyWithMinusLaneTest() {
		int lanes[] = {-1,0,1,2,3};
		AutoplayModifier autoplayModifier = new AutoplayModifier(lanes);
		Note[] before = getNote(bmsModel);
		autoplayModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
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
	
	@Test
	public void modifyWithMinusMargin() {
		int lanes[] = {0, 1};
		AutoplayModifier autoplayModifier = new AutoplayModifier(lanes,-1);
		Note[] before = getNote(bmsModel);
		autoplayModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void modifyWithMassLanesWithMarginTest() {
		int lanes[] = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17
				,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32};
		AutoplayModifier autoplayModifier = new AutoplayModifier(lanes,1);
		Note[] before = getNote(bmsModel);
		autoplayModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void modifyWithMinusLaneWithMarginTest() {
		int lanes[] = {-1,0,1,2,3};
		AutoplayModifier autoplayModifier = new AutoplayModifier(lanes,1);
		Note[] before = getNote(bmsModel);
		autoplayModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void modifyWithMassLanesMinusMarginTest() {
		int lanes[] = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17
				,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32};
		AutoplayModifier autoplayModifier = new AutoplayModifier(lanes,-2);
		Note[] before = getNote(bmsModel);
		autoplayModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void modifyWithMinusLaneMinusMarginTest() {
		int lanes[] = {-1,0,1,2,3};
		AutoplayModifier autoplayModifier = new AutoplayModifier(lanes,-3);
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
