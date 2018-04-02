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
import bms.player.beatoraja.pattern.MineNoteModifier;

public class MineNoteModifierTest {
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
	 * MineNote Modifier removes all mine note in the BMSModel.
	 * */
	
	@Test
	public void mineNoteModifierConstructorTest() {
		MineNoteModifier mineNoteModifier = new MineNoteModifier();
		assertEquals(mineNoteModifier.mineNoteExists(),bmsModel.containsMineNote());
		assertNotNull(mineNoteModifier);
	}
	
	@Test
	public void modifyTest() {
		MineNoteModifier mineNoteModifier = new MineNoteModifier();
		mineNoteModifier.modify(bmsModel);
		assertEquals(mineNoteModifier.mineNoteExists(),bmsModel.containsMineNote());
	}
	
	@Test
	public void mineNoteEliminatedCheckTest() {
		MineNoteModifier mineNoteModifier = new MineNoteModifier();
		mineNoteModifier.modify(bmsModel);
		assertEquals(bmsModel.containsMineNote(), false);
	
	}
}
