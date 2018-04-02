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
import bms.player.beatoraja.pattern.ConstantBPMModifier;

public class ConstantBPMModifierTest {
	static BMSModel bmsModel;
	static BMSDecoder bmsDecoder;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File bmsFile = new File("C:\\Users\\dhehd\\Desktop\\_laika_24b.bmson");
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
	public void constantBPMModifierConstructTest() {
		ConstantBPMModifier constantBPMModifier = new ConstantBPMModifier();
		assertNotNull(constantBPMModifier);
	}
	
	/*
	 * ConstantBPMModifier should make stopping option disabled.
	 * */
	@Test
	public void modifyTest() {
		ConstantBPMModifier constantBPMModifier = new ConstantBPMModifier();
		constantBPMModifier.modify(bmsModel);
		assertEquals(bmsModel.getAllTimeLines()[0].getStop(), 0);
	}

}
