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
import bms.player.beatoraja.pattern.NoteShuffleModifier;

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

	@Test
	public void NoteShuffleModifierTest() {
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
	}
	
	@Test
	public void modifyTest() {
		NoteShuffleModifier noteShuffleModifier = new NoteShuffleModifier(0);
		noteShuffleModifier.modify(bmsModel);
	}

}
