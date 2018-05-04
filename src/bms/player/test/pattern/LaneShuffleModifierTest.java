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
import bms.model.Mode;
import bms.model.Note;
import bms.model.TimeLine;
import bms.player.beatoraja.pattern.LaneShuffleModifier;
import bms.player.beatoraja.pattern.PatternModifier;
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

	@Test
	public void LaneShuffleModifierConstructorWithMirrorTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(MIRROR);
		assertNotNull(laneShuffleModifier);
	}
	
	@Test
	public void LaneShuffleModifierConstructorWithRRandomTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(R_RANDOM);
		assertNotNull(laneShuffleModifier);
	}
	
	@Test
	public void LaneShuffleModifierConstructorWithRandomTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(RANDOM);
		assertNotNull(laneShuffleModifier);
	}
	
	@Test
	public void LaneShuffleModifierConstructorWithCrossTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(CROSS);
		assertNotNull(laneShuffleModifier);
	}
	
	@Test
	public void LaneShuffleModifierConstructorWithRandomEXTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(RANDOM_EX);
		assertNotNull(laneShuffleModifier);
	}
	
	@Test
	public void LaneShuffleModifierConstructorWithFlipTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(FLIP);
		assertNotNull(laneShuffleModifier);
	}
	
	@Test
	public void LaneShuffleModifierConstructorWithBattleTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(BATTLE);
		assertNotNull(laneShuffleModifier);
	}
	
	@Test
	public void LaneShuffleModifierConstructorWithInvalidTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(-1);
		assertNotNull(laneShuffleModifier);
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
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(MIRROR);
		Note[] before = getNote(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test
	public void modifyCrossTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(CROSS);
		Note[] before = getNote(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test
	public void modifyFlipTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(FLIP);
		Note[] before = getNote(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test
	public void modifyRRandomTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(R_RANDOM);
		Note[] before = getNote(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test
	public void modifyRandomEXTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(RANDOM_EX);
		Note[] before = getNote(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test
	public void modifyBattleTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(BATTLE);
		Note[] before = getNote(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test(expected=NullPointerException.class)
	public void modifyWithInvalidTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(-1);
		Note[] before = getNote(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	
	/* Subclass constructing with PatternModifer.create.
	 * 
	 * */
	@Test
	public void dummyModifyTest() {
		PatternModifier modifier = PatternModifier.create(0, 0);
		Note[] before = getNote(bmsModel);
		modifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test
	public void patternModifyCreateWithMIRRORTest() {
		PatternModifier modifier = PatternModifier.create(1, 0);
		Note[] before = getNote(bmsModel);
		modifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test
	public void patternModifyCreateWithRANDOMTest() {
		PatternModifier modifier = PatternModifier.create(2, 0);
		Note[] before = getNote(bmsModel);
		modifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test
	public void patternModifyCreateWithRRandomTest() {
		PatternModifier modifier = PatternModifier.create(3, 0);
		Note[] before = getNote(bmsModel);
		modifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test(expected=NullPointerException.class)
	public void patternModifyCreateWithSRandomTest() {
		PatternModifier modifier = PatternModifier.create(4, 0);
		Note[] before = getNote(bmsModel);
		modifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test(expected=NullPointerException.class)
	public void patternModifyCreateWithSPIRALTest() {
		PatternModifier modifier = PatternModifier.create(5, 0);
		Note[] before = getNote(bmsModel);
		modifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test(expected=NullPointerException.class)
	public void patternModifyCreateWithHRandomTest() {
		PatternModifier modifier = PatternModifier.create(6, 0);
		Note[] before = getNote(bmsModel);
		modifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test(expected=NullPointerException.class)
	public void patternModifyCreateWithAllScrTest() {
		PatternModifier modifier = PatternModifier.create(7, 0);
		Note[] before = getNote(bmsModel);
		modifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test
	public void patternModifyCreateWithRandomEXandomTest() {
		PatternModifier modifier = PatternModifier.create(8, 0);
		Note[] before = getNote(bmsModel);
		modifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test(expected=NullPointerException.class)
	public void patternModifyCreateWithSRandomEXTest() {
		PatternModifier modifier = PatternModifier.create(9, 0);
		Note[] before = getNote(bmsModel);
		modifier.modify(bmsModel);
		assertNotEquals(before, getNote(bmsModel));
	}
	
	@Test
	public void modifyMergeMirrorRandomTest() {
		PatternModifier modifierDummy = PatternModifier.create(0, 0);
		List<PatternModifyLog> dummyLog = modifierDummy.modify(bmsModel);
		
		PatternModifier modifierBef = PatternModifier.create(1, 0);
		List<PatternModifyLog> befLog = modifierBef.modify(bmsModel);
		PatternModifier modifierAft = PatternModifier.create(2, 0);
		List<PatternModifyLog> aftLog = modifierBef.modify(bmsModel);
		
		dummyLog = modifierDummy.merge(befLog, aftLog);
		assertNotNull(dummyLog.get(0).modify);
		assertEquals(dummyLog.size(), befLog.size() + aftLog.size());
	}
	
	
	@Test
	public void modifyMergeMirrorMirrorTest() {
		PatternModifier modifierDummy = PatternModifier.create(0, 0);
		List<PatternModifyLog> dummyLog = modifierDummy.modify(bmsModel);
		
		PatternModifier modifierBef = PatternModifier.create(1, 0);
		List<PatternModifyLog> befLog = modifierBef.modify(bmsModel);
		PatternModifier modifierAft = PatternModifier.create(1, 0);
		List<PatternModifyLog> aftLog = modifierBef.modify(bmsModel);
		
		dummyLog = modifierDummy.merge(befLog, aftLog);
		assertNotNull(dummyLog.get(0).modify);
		assertEquals(dummyLog.size(), befLog.size() + aftLog.size());
	}
	
	@Test
	public void modifyMergeMirrorRRamdomTest() {
		PatternModifier modifierDummy = PatternModifier.create(0, 0);
		List<PatternModifyLog> dummyLog = modifierDummy.modify(bmsModel);
		
		PatternModifier modifierBef = PatternModifier.create(1, 0);
		List<PatternModifyLog> befLog = modifierBef.modify(bmsModel);
		PatternModifier modifierAft = PatternModifier.create(3, 0);
		List<PatternModifyLog> aftLog = modifierBef.modify(bmsModel);
		
		dummyLog = modifierDummy.merge(befLog, aftLog);
		assertNotNull(dummyLog.get(0).modify);
		assertEquals(dummyLog.size(), befLog.size() + aftLog.size());
	}
	
	@Test
	public void modifyMergeMirrorRandomEXTest() {
		PatternModifier modifierDummy = PatternModifier.create(0, 0);
		List<PatternModifyLog> dummyLog = modifierDummy.modify(bmsModel);
		
		PatternModifier modifierBef = PatternModifier.create(1, 0);
		List<PatternModifyLog> befLog = modifierBef.modify(bmsModel);
		PatternModifier modifierAft = PatternModifier.create(8, 0);
		List<PatternModifyLog> aftLog = modifierBef.modify(bmsModel);
		
		dummyLog = modifierDummy.merge(befLog, aftLog);
		assertNotNull(dummyLog.get(0).modify);
		assertEquals(dummyLog.size(), befLog.size() + aftLog.size());
	}
	
	@Test
	public void modifyMergeMirrorSRandomEXTest() {
		PatternModifier modifierDummy = PatternModifier.create(0, 0);
		List<PatternModifyLog> dummyLog = modifierDummy.modify(bmsModel);
		
		PatternModifier modifierBef = PatternModifier.create(1, 0);
		List<PatternModifyLog> befLog = modifierBef.modify(bmsModel);
		PatternModifier modifierAft = PatternModifier.create(9, 0);
		List<PatternModifyLog> aftLog = modifierBef.modify(bmsModel);
		
		dummyLog = modifierDummy.merge(befLog, aftLog);
		assertNotNull(dummyLog.get(0).modify);
		assertEquals(dummyLog.size(), befLog.size() + aftLog.size());
	}
	
	@Test
	public void modifyMergeRandomRandomTest() {
		PatternModifier modifierDummy = PatternModifier.create(0, 0);
		List<PatternModifyLog> dummyLog = modifierDummy.modify(bmsModel);
		
		PatternModifier modifierBef = PatternModifier.create(2, 0);
		List<PatternModifyLog> befLog = modifierBef.modify(bmsModel);
		PatternModifier modifierAft = PatternModifier.create(2, 0);
		List<PatternModifyLog> aftLog = modifierBef.modify(bmsModel);
		
		dummyLog = modifierDummy.merge(befLog, aftLog);
		assertNotNull(dummyLog.get(0).modify);
		assertEquals(dummyLog.size(), befLog.size() + aftLog.size());
	}
	
	@Test
	public void modifyMergeRandomRRandomTest() {
		PatternModifier modifierDummy = PatternModifier.create(0, 0);
		List<PatternModifyLog> dummyLog = modifierDummy.modify(bmsModel);
		
		PatternModifier modifierBef = PatternModifier.create(2, 0);
		List<PatternModifyLog> befLog = modifierBef.modify(bmsModel);
		PatternModifier modifierAft = PatternModifier.create(3, 0);
		List<PatternModifyLog> aftLog = modifierBef.modify(bmsModel);
		
		dummyLog = modifierDummy.merge(befLog, aftLog);
		assertNotNull(dummyLog.get(0).modify);
		assertEquals(dummyLog.size(), befLog.size() + aftLog.size());
	}
	
	@Test
	public void modifyMergeRandomRandomEXTest() {
		PatternModifier modifierDummy = PatternModifier.create(0, 0);
		List<PatternModifyLog> dummyLog = modifierDummy.modify(bmsModel);
		
		PatternModifier modifierBef = PatternModifier.create(2, 0);
		List<PatternModifyLog> befLog = modifierBef.modify(bmsModel);
		PatternModifier modifierAft = PatternModifier.create(8, 0);
		List<PatternModifyLog> aftLog = modifierBef.modify(bmsModel);
		
		dummyLog = modifierDummy.merge(befLog, aftLog);
		assertNotNull(dummyLog.get(0).modify);
		assertEquals(dummyLog.size(), befLog.size() + aftLog.size());
	}
	
	@Test
	public void modifyMergeRandomSRandomEXTest() {
		PatternModifier modifierDummy = PatternModifier.create(0, 0);
		List<PatternModifyLog> dummyLog = modifierDummy.modify(bmsModel);
		
		PatternModifier modifierBef = PatternModifier.create(2, 0);
		List<PatternModifyLog> befLog = modifierBef.modify(bmsModel);
		PatternModifier modifierAft = PatternModifier.create(9, 0);
		List<PatternModifyLog> aftLog = modifierBef.modify(bmsModel);
		
		dummyLog = modifierDummy.merge(befLog, aftLog);
		assertNotNull(dummyLog.get(0).modify);
		assertEquals(dummyLog.size(), befLog.size() + aftLog.size());
	}
	
	@Test
	public void modifyMergeRRandomRRandomTest() {
		PatternModifier modifierDummy = PatternModifier.create(0, 0);
		List<PatternModifyLog> dummyLog = modifierDummy.modify(bmsModel);
		
		PatternModifier modifierBef = PatternModifier.create(3, 0);
		List<PatternModifyLog> befLog = modifierBef.modify(bmsModel);
		PatternModifier modifierAft = PatternModifier.create(3, 0);
		List<PatternModifyLog> aftLog = modifierBef.modify(bmsModel);
		
		dummyLog = modifierDummy.merge(befLog, aftLog);
		assertNotNull(dummyLog.get(0).modify);
		assertEquals(dummyLog.size(), befLog.size() + aftLog.size());
	}
	
	@Test
	public void modifyMergeRRandomRandomEXTest() {
		PatternModifier modifierDummy = PatternModifier.create(0, 0);
		List<PatternModifyLog> dummyLog = modifierDummy.modify(bmsModel);
		
		PatternModifier modifierBef = PatternModifier.create(3, 0);
		List<PatternModifyLog> befLog = modifierBef.modify(bmsModel);
		PatternModifier modifierAft = PatternModifier.create(8, 0);
		List<PatternModifyLog> aftLog = modifierBef.modify(bmsModel);
		
		dummyLog = modifierDummy.merge(befLog, aftLog);
		assertNotNull(dummyLog.get(0).modify);
		assertEquals(dummyLog.size(), befLog.size() + aftLog.size());
	}

	@Test
	public void modifyMergeRRandomSRandomEXTest() {
		PatternModifier modifierDummy = PatternModifier.create(0, 0);
		List<PatternModifyLog> dummyLog = modifierDummy.modify(bmsModel);
		
		PatternModifier modifierBef = PatternModifier.create(3, 0);
		List<PatternModifyLog> befLog = modifierBef.modify(bmsModel);
		PatternModifier modifierAft = PatternModifier.create(9, 0);
		List<PatternModifyLog> aftLog = modifierBef.modify(bmsModel);
		
		dummyLog = modifierDummy.merge(befLog, aftLog);
		assertNotNull(dummyLog.get(0).modify);
		assertEquals(dummyLog.size(), befLog.size() + aftLog.size());
	}
	
	@Test
	public void modifyMergeRandomEXRandomEXTest() {
		PatternModifier modifierDummy = PatternModifier.create(0, 0);
		List<PatternModifyLog> dummyLog = modifierDummy.modify(bmsModel);
		
		PatternModifier modifierBef = PatternModifier.create(8, 0);
		List<PatternModifyLog> befLog = modifierBef.modify(bmsModel);
		PatternModifier modifierAft = PatternModifier.create(8, 0);
		List<PatternModifyLog> aftLog = modifierBef.modify(bmsModel);
		
		dummyLog = modifierDummy.merge(befLog, aftLog);
		assertNotNull(dummyLog.get(0).modify);
		assertEquals(dummyLog.size(), befLog.size() + aftLog.size());
	}
	
	@Test
	public void modifyMergeRandomEXSRandomEXTest() {
		PatternModifier modifierDummy = PatternModifier.create(0, 0);
		List<PatternModifyLog> dummyLog = modifierDummy.modify(bmsModel);
		
		PatternModifier modifierBef = PatternModifier.create(8, 0);
		List<PatternModifyLog> befLog = modifierBef.modify(bmsModel);
		PatternModifier modifierAft = PatternModifier.create(9, 0);
		List<PatternModifyLog> aftLog = modifierBef.modify(bmsModel);
		
		dummyLog = modifierDummy.merge(befLog, aftLog);
		assertNotNull(dummyLog.get(0).modify);
		assertEquals(dummyLog.size(), befLog.size() + aftLog.size());
	}
	
	@Test
	public void mergeTripleTest() {
		PatternModifier modifierDummy = PatternModifier.create(0, 0);
		List<PatternModifyLog> dummyLog = modifierDummy.modify(bmsModel);
		
		PatternModifier modifierBef = PatternModifier.create(8, 0);
		List<PatternModifyLog> befLog = modifierBef.modify(bmsModel);
		PatternModifier modifierAft = PatternModifier.create(2, 0);
		List<PatternModifyLog> aftLog = modifierBef.modify(bmsModel);
	
		PatternModifier modifierLast = PatternModifier.create(3, 0);
		List<PatternModifyLog> lastLog = modifierBef.modify(bmsModel);
		
		dummyLog = modifierDummy.merge(lastLog, modifierDummy.merge(befLog, aftLog));
		
		assertNotNull(dummyLog.get(0).modify);
		assertTrue(dummyLog.size() < (befLog.size() + aftLog.size() + lastLog.size()));
	}
	
	@Test
	public void mergeQuadTest() {
		PatternModifier modifierDummy = PatternModifier.create(0, 0);
		List<PatternModifyLog> dummyLog = modifierDummy.modify(bmsModel);
		
		PatternModifier modifierBef = PatternModifier.create(8, 0);
		List<PatternModifyLog> befLog = modifierBef.modify(bmsModel);
		PatternModifier modifierAft = PatternModifier.create(2, 0);
		List<PatternModifyLog> aftLog = modifierBef.modify(bmsModel);
	
		PatternModifier modifierLast = PatternModifier.create(3, 0);
		List<PatternModifyLog> lastLog = modifierBef.modify(bmsModel);
		
		PatternModifier modifierLastLast = PatternModifier.create(1, 0);
		List<PatternModifyLog> lastLastLog = modifierBef.modify(bmsModel);
		
		dummyLog = modifierDummy.merge(lastLastLog, modifierDummy.merge(lastLog, modifierDummy.merge(befLog, aftLog)));
		
		assertNotNull(dummyLog.get(0).modify);
		assertTrue(dummyLog.size() < ( lastLastLog.size() + befLog.size() + aftLog.size() + lastLog.size()));
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
	/* purpose : integer random suffle test
	 * input type : saved integer in bmsModel we test integer is 0-6
	 * output type : rotated saved integer
	 */
	@Test
	public void makeRandomOneTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(RANDOM);
		Note[] before = getNote(bmsModel);
		Mode mode = bmsModel.getMode();
		laneShuffleModifier.modify(bmsModel);
		int[] first = laneShuffleModifier.getRandom();
		laneShuffleModifier.modify(bmsModel);
		int[] two = laneShuffleModifier.getRandom();
		assertNotEquals(first,two);
	}
	@Test
	public void makeRandomTwoTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(RANDOM);
		Note[] before = getNote(bmsModel);
		Mode mode = bmsModel.getMode();
		laneShuffleModifier.modify(bmsModel);
		int[] first = laneShuffleModifier.getRandom();
		laneShuffleModifier.modify(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		int[] two = laneShuffleModifier.getRandom();
		assertNotEquals(first,two);
	}
	
	@Test
	public void makeRandomThreeTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(RANDOM);
		Note[] before = getNote(bmsModel);
		Mode mode = bmsModel.getMode();
		laneShuffleModifier.modify(bmsModel);
		int[] first = laneShuffleModifier.getRandom();
		laneShuffleModifier.modify(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		int[] two = laneShuffleModifier.getRandom();
		assertNotEquals(first,two);
	}
	
	@Test
	public void makeMIRROROneTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(MIRROR);
		Note[] before = getNote(bmsModel);
		Mode mode = bmsModel.getMode();
		laneShuffleModifier.modify(bmsModel);
		int[] first = laneShuffleModifier.getRandom();
		laneShuffleModifier.modify(bmsModel);
		int[] two = laneShuffleModifier.getRandom();
		assertNotEquals(first,two);
	}
	@Test
	public void makeMIRRORTwoTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(MIRROR);
		Note[] before = getNote(bmsModel);
		Mode mode = bmsModel.getMode();
		laneShuffleModifier.modify(bmsModel);
		int[] first = laneShuffleModifier.getRandom();
		laneShuffleModifier.modify(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		int[] two = laneShuffleModifier.getRandom();
		assertNotEquals(first,two);
	}
	
	@Test
	public void makeMIRRORThreeTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(MIRROR);
		Note[] before = getNote(bmsModel);
		Mode mode = bmsModel.getMode();
		laneShuffleModifier.modify(bmsModel);
		int[] first = laneShuffleModifier.getRandom();
		laneShuffleModifier.modify(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		int[] two = laneShuffleModifier.getRandom();
		assertNotEquals(first,two);
	}
	
	@Test
	public void makeR_RANDOMOneTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(R_RANDOM);
		Note[] before = getNote(bmsModel);
		Mode mode = bmsModel.getMode();
		laneShuffleModifier.modify(bmsModel);
		int[] first = laneShuffleModifier.getRandom();
		laneShuffleModifier.modify(bmsModel);
		int[] two = laneShuffleModifier.getRandom();
		assertNotEquals(first,two);
	}
	@Test
	public void makeR_RANDOMTwoTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(R_RANDOM);
		Note[] before = getNote(bmsModel);
		Mode mode = bmsModel.getMode();
		laneShuffleModifier.modify(bmsModel);
		int[] first = laneShuffleModifier.getRandom();
		laneShuffleModifier.modify(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		int[] two = laneShuffleModifier.getRandom();
		assertNotEquals(first,two);
	}
	
	@Test
	public void makeR_RANDOMThreeTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(R_RANDOM);
		Note[] before = getNote(bmsModel);
		Mode mode = bmsModel.getMode();
		laneShuffleModifier.modify(bmsModel);
		int[] first = laneShuffleModifier.getRandom();
		laneShuffleModifier.modify(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		int[] two = laneShuffleModifier.getRandom();
		assertNotEquals(first,two);
	}
	
	@Test
	public void makeCROSSOneTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(CROSS);
		Note[] before = getNote(bmsModel);
		Mode mode = bmsModel.getMode();
		laneShuffleModifier.modify(bmsModel);
		int[] first = laneShuffleModifier.getRandom();
		laneShuffleModifier.modify(bmsModel);
		int[] two = laneShuffleModifier.getRandom();
		assertNotEquals(first,two);
	}
	@Test
	public void makeCROSSTwoTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(CROSS);
		Note[] before = getNote(bmsModel);
		Mode mode = bmsModel.getMode();
		laneShuffleModifier.modify(bmsModel);
		int[] first = laneShuffleModifier.getRandom();
		laneShuffleModifier.modify(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		int[] two = laneShuffleModifier.getRandom();
		assertNotEquals(first,two);
	}
	
	@Test
	public void makeCROSSThreeTest() {
		LaneShuffleModifier laneShuffleModifier = new LaneShuffleModifier(CROSS);
		Note[] before = getNote(bmsModel);
		Mode mode = bmsModel.getMode();
		laneShuffleModifier.modify(bmsModel);
		int[] first = laneShuffleModifier.getRandom();
		laneShuffleModifier.modify(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		laneShuffleModifier.modify(bmsModel);
		int[] two = laneShuffleModifier.getRandom();
		assertNotEquals(first,two);
	}
	
	
	
}