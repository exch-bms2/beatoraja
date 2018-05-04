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
import bms.player.beatoraja.pattern.PracticeModifier;

public class PracticeModifierTest {
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
	 * Extract Notes from the specific time interval for
	 * generating practice mode. Amount of node will be changed.
	 * */
	@Test
	public void modifierTotalTest() {
		PracticeModifier practiceModifier = new PracticeModifier(10,100);
		double totalNum = bmsModel.getTotal();
		practiceModifier.modify(bmsModel);
		assertNotEquals(bmsModel.getTotal(), totalNum);
	}
	
	@Test
	public void modifierNoteTest() {
		PracticeModifier practiceModifier = new PracticeModifier(10,100);
		double totalNum = bmsModel.getTotalNotes();
		practiceModifier.modify(bmsModel);
		assertNotEquals(bmsModel.getTotalNotes(), totalNum);
	}

	@Test
	public void modifierTest() {
		PracticeModifier practiceModifier = new PracticeModifier(10,100);
		double totalNum = bmsModel.getTotalNotes();
		practiceModifier.modify(bmsModel);
		assertNotEquals(bmsModel.getTotalNotes(), totalNum);
	}
}
