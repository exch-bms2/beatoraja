package bms.player.test.pattern;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bms.model.BMSDecoder;
import bms.model.BMSModel;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.pattern.LongNoteModifier;

public class LongNoteModifierTest {
	static BMSModel bmsModel1;
	static BMSModel bmsModel2;
	static BMSModel bmsModel3;
	static BMSModel bmsModel4;
	static BMSDecoder bmsDecoder;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File bmsFile = new File("src\\bms\\player\\test\\end_time_dpnep.bms");
		bmsDecoder = new BMSDecoder();
		bmsModel1 = bmsDecoder.decode(bmsFile);
	
	
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
	public void LongNoteModifierConstructTest() {
		LongNoteModifier longNoteModifier = new LongNoteModifier();
		assertNotNull(longNoteModifier);
	}
	
	/**
	 * After call modify, longNoteExists should return true
	 * if the bms model has a long note.
	 * All long notes in BMS models will be eliminated.
	 */
	@Test
	public void modifyTest() {
		LongNoteModifier longNoteModifier = new LongNoteModifier();
		longNoteModifier.modify(bmsModel1);
		assertEquals(longNoteModifier.longNoteExists(),bmsModel1.containsLongNote());
	}
	
	@Test
	public void checkLongNoteEliminatedTest() {
		LongNoteModifier longNoteModifier = new LongNoteModifier();
		longNoteModifier.modify(bmsModel1);
		assertEquals(longNoteModifier.longNoteExists(),false);
	}

}
