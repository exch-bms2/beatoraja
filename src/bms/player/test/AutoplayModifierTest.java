package bms.player.test;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bms.model.BMSDecoder;
import bms.model.BMSModel;
import bms.player.beatoraja.pattern.AutoplayModifier;

public class AutoplayModifierTest {
	static BMSModel bmsModel;
	static BMSDecoder bmsDecoder;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File bmsFile = new File("C:\\Users\\dhehd\\Desktop\\_laika_24.bmson");
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
		autoplayModifier.modify(bmsModel);
	}
	
	@Test
	public void modifyWithMargin() {
		int lanes[] = {0, 1};
		AutoplayModifier autoplayModifier = new AutoplayModifier(lanes,1);
		autoplayModifier.modify(bmsModel);
	}
}
