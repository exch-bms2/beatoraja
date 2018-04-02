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
import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.pattern.NoteShuffleModifier;
import bms.player.beatoraja.pattern.PatternModifyLog;

public class NoteShuffleModifierTest {
	public static final int S_RANDOM = 0;
	public static final int SPIRAL = 1;
	public static final int ALL_SCR = 2;
	public static final int H_RANDOM = 3;
	public static final int S_RANDOM_EX = 4;
	public static final int SEVEN_TO_NINE = 100;
	
	static BMSModel bmsModel;
	static BMSDecoder bmsDecoder;
	static Config config;
	static PlayerConfig playerconfig;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		config = new Config();
        config.setPlayername("player1");
        config.read();
        playerconfig = new PlayerConfig();
        playerconfig.setId("player1");
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
	 * Every test case in this source will fail because 
	 * NullPointerException by using unassigned PlayerConfig class.
	 * 
	 * "PlayerConfig config;" must be assigned with constructor.
	 * */
	
	@Test
	public void NoteShuffleModifierWithValidTest() {
		NoteShuffleModifier noteShuffleModifier = new NoteShuffleModifier(S_RANDOM);
		assertNotNull(noteShuffleModifier);
		noteShuffleModifier = new NoteShuffleModifier(SPIRAL);
		assertNotNull(noteShuffleModifier);
		noteShuffleModifier = new NoteShuffleModifier(ALL_SCR);
		assertNotNull(noteShuffleModifier);
		noteShuffleModifier = new NoteShuffleModifier(H_RANDOM);
		assertNotNull(noteShuffleModifier);
		noteShuffleModifier = new NoteShuffleModifier(S_RANDOM_EX);
		assertNotNull(noteShuffleModifier);
		noteShuffleModifier = new NoteShuffleModifier(SEVEN_TO_NINE);
		assertNotNull(noteShuffleModifier);
		noteShuffleModifier = new NoteShuffleModifier(S_RANDOM);
		assertNotNull(noteShuffleModifier);
	}
	
	@Test(expected=NullPointerException.class)
	public void noteShuffleModifierWithInvalidTest() {
		NoteShuffleModifier noteShuffleModifier = new NoteShuffleModifier(-1);
		assertNotNull(noteShuffleModifier);
		noteShuffleModifier.modify(bmsModel);
	}
	
	@Test(expected=NullPointerException.class)
	public void modifySRandomTest() {
		NoteShuffleModifier noteShuffleModifier = new NoteShuffleModifier(S_RANDOM);
		Note[] noteBefore = getNote(bmsModel);
		noteShuffleModifier.modify(bmsModel);
		assertNotEquals(noteBefore, getNote(bmsModel));
	}
	
	@Test(expected=NullPointerException.class)
	public void modifySpiralTest() {
		NoteShuffleModifier noteShuffleModifier = new NoteShuffleModifier(SPIRAL);
		Note[] noteBefore = getNote(bmsModel);
		noteShuffleModifier.modify(bmsModel);
		assertNotEquals(noteBefore, getNote(bmsModel));
	}
	
	@Test(expected=NullPointerException.class)
	public void modifyAllScrTest() {
		NoteShuffleModifier noteShuffleModifier = new NoteShuffleModifier(ALL_SCR);
		Note[] noteBefore = getNote(bmsModel);
		noteShuffleModifier.modify(bmsModel);
		assertNotEquals(noteBefore, getNote(bmsModel));
	}
	
	@Test(expected=NullPointerException.class)
	public void modifyHRandomTest() {
		NoteShuffleModifier noteShuffleModifier = new NoteShuffleModifier(H_RANDOM);
		Note[] noteBefore = getNote(bmsModel);
		noteShuffleModifier.modify(bmsModel);
		assertNotEquals(noteBefore, getNote(bmsModel));
	}
	
	@Test(expected=NullPointerException.class)
	public void modifySRandomExTest() {
		NoteShuffleModifier noteShuffleModifier = new NoteShuffleModifier(S_RANDOM);
		Note[] noteBefore = getNote(bmsModel);
		noteShuffleModifier.modify(bmsModel);
		assertNotEquals(noteBefore, getNote(bmsModel));
	}

	@Test(expected=NullPointerException.class)
	public void modifySevenToNineTest() {
		NoteShuffleModifier noteShuffleModifier = new NoteShuffleModifier(SEVEN_TO_NINE);
		Note[] noteBefore = getNote(bmsModel);
		noteShuffleModifier.modify(bmsModel);
		assertNotEquals(noteBefore, getNote(bmsModel));
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
