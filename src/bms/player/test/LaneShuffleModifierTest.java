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
import bms.model.Mode;
import bms.model.Note;
import bms.model.TimeLine;
import bms.player.beatoraja.pattern.LaneShuffleModifier;
import bms.player.beatoraja.pattern.PatternModifyLog;

public class LaneShuffleModifierTest {
	static BMSModel bmsModel;
	static BMSDecoder bmsDecoder;
	
	public static final int MIRROR = 0;
	public static final int R_RANDOM = 1;
	public static final int RANDOM = 2;
	public static final int CROSS = 3;
	public static final int RANDOM_EX = 4;
	public static final int FLIP = 5;
	public static final int BATTLE = 6;
	
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

	@Test
	public void LaneShuffleModifierConstructorTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(MIRROR);
		assertNotNull(laneShuffleModifier);
	}
	
	@Test
	public void modifyRandomTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(RANDOM);
		Note[] before = getNote(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}

	@Test
	public void modifyMirrorTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(RANDOM);
		Note[] before = getNote(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test
	public void modifyCrossTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(RANDOM);
		Note[] before = getNote(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test
	public void modifyFlipTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(RANDOM);
		Note[] before = getNote(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test
	public void modifyRRandomTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(RANDOM);
		Note[] before = getNote(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test
	public void modifyRandomEXTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(RANDOM);
		Note[] before = getNote(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test
	public void modifyBattleTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(RANDOM);
		Note[] before = getNote(bmsModel);
		laneShuffleModifier.modify(bmsModel);
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
