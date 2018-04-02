package bms.player.test.GrooveGauge;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bms.model.BMSDecoder;
import bms.model.BMSModel;
import bms.player.beatoraja.BMSResource;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainLoader;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.input.KeyInputLog;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.play.GaugeProperty;
import bms.player.beatoraja.play.GrooveGauge;
import bms.player.beatoraja.play.JudgeManager;
import bms.player.beatoraja.play.JudgeProperty;
import bms.player.beatoraja.select.MusicSelector;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;

public class GrooveGaugeTest extends MainLoader{
	static GrooveGauge gauge;
	static Thread thread;
	static MainLoader mainLoader;
	static Config config;
	static PlayerConfig playerconfig;
	static MainController mainController;
	static BMSModel bmsModel;
	static PlayerResource playerResource;
	static MusicSelector musicSelector;
	public static final int STATE_PLAYBMS = 2;
	static Stage stage;
	static BMSDecoder bmsDecoder;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File bmsFile = new File("C:\\Users\\rnwhd\\Desktop\\programing\\git\\beatoraja\\yassu_ff6_nakama\\yassu_ff6_nakama\\_yassu_ff6_nakama_24b.bmson");
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
	public void GrooveGaueType0MaxCheckTest() {
		
		gauge = new GrooveGauge(bmsModel,0,GaugeProperty.FIVEKEYS);		
		assertTrue(100.0==gauge.getMaxValue());		
		
		gauge = new GrooveGauge(bmsModel,0,GaugeProperty.SEVENKEYS);		
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,0,GaugeProperty.PMS);
		assertTrue(120.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,0,GaugeProperty.KEYBOARD);	
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,0,GaugeProperty.LR2);
		assertTrue(100.0==gauge.getMaxValue());
	}

	@Test
	public void GrooveGaueType1MaxCheckTest() {
		gauge = new GrooveGauge(bmsModel,1,GaugeProperty.FIVEKEYS);		
		assertTrue(100.0==gauge.getMaxValue());		
		
		gauge = new GrooveGauge(bmsModel,1,GaugeProperty.SEVENKEYS);		
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,1,GaugeProperty.PMS);
		assertTrue(120.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,1,GaugeProperty.KEYBOARD);	
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,1,GaugeProperty.LR2);
		assertTrue(100.0==gauge.getMaxValue());
	}

	@Test
	public void GrooveGaueType2MaxCheckTest() {
		gauge = new GrooveGauge(bmsModel,2,GaugeProperty.FIVEKEYS);		
		assertTrue(100.0==gauge.getMaxValue());		
		
		gauge = new GrooveGauge(bmsModel,2,GaugeProperty.SEVENKEYS);		
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,2,GaugeProperty.PMS);
		assertTrue(120.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,2,GaugeProperty.KEYBOARD);	
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,2,GaugeProperty.LR2);
		assertTrue(100.0==gauge.getMaxValue());
	}

	@Test
	public void GrooveGaueType3MaxCheckTest() {
		gauge = new GrooveGauge(bmsModel,3,GaugeProperty.FIVEKEYS);		
		assertTrue(100.0==gauge.getMaxValue());		
		
		gauge = new GrooveGauge(bmsModel,3,GaugeProperty.SEVENKEYS);		
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,3,GaugeProperty.PMS);
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,3,GaugeProperty.KEYBOARD);	
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,3,GaugeProperty.LR2);
		assertTrue(100.0==gauge.getMaxValue());
	}

	@Test
	public void GrooveGaueType4MaxCheckTest() {
		gauge = new GrooveGauge(bmsModel,4,GaugeProperty.FIVEKEYS);		
		assertTrue(100.0==gauge.getMaxValue());		
		
		gauge = new GrooveGauge(bmsModel,4,GaugeProperty.SEVENKEYS);		
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,4,GaugeProperty.PMS);
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,4,GaugeProperty.KEYBOARD);	
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,4,GaugeProperty.LR2);
		assertTrue(100.0==gauge.getMaxValue());
	}

	@Test
	public void GrooveGaueType5MaxCheckTest() {
		gauge = new GrooveGauge(bmsModel,5,GaugeProperty.FIVEKEYS);		
		assertTrue(100.0==gauge.getMaxValue());		
		
		gauge = new GrooveGauge(bmsModel,5,GaugeProperty.SEVENKEYS);		
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,5,GaugeProperty.PMS);
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,5,GaugeProperty.KEYBOARD);	
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,5,GaugeProperty.LR2);
		assertTrue(100.0==gauge.getMaxValue());
	}

	@Test
	public void GrooveGaueType6MaxCheckTest() {
		gauge = new GrooveGauge(bmsModel,6,GaugeProperty.FIVEKEYS);		
		assertTrue(100.0==gauge.getMaxValue());		
		
		gauge = new GrooveGauge(bmsModel,6,GaugeProperty.SEVENKEYS);		
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,6,GaugeProperty.PMS);
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,6,GaugeProperty.KEYBOARD);	
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,6,GaugeProperty.LR2);
		assertTrue(100.0==gauge.getMaxValue());
	}

	@Test
	public void GrooveGaueType7MaxCheckTest() {
		gauge = new GrooveGauge(bmsModel,7,GaugeProperty.FIVEKEYS);		
		assertTrue(100.0==gauge.getMaxValue());		
		
		gauge = new GrooveGauge(bmsModel,7,GaugeProperty.SEVENKEYS);		
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,7,GaugeProperty.PMS);
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,7,GaugeProperty.KEYBOARD);	
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,7,GaugeProperty.LR2);
		assertTrue(100.0==gauge.getMaxValue());
	}

	@Test
	public void GrooveGaueType8MaxCheckTest() {
		gauge = new GrooveGauge(bmsModel,8,GaugeProperty.FIVEKEYS);		
		assertTrue(100.0==gauge.getMaxValue());		
		
		gauge = new GrooveGauge(bmsModel,8,GaugeProperty.SEVENKEYS);		
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,8,GaugeProperty.PMS);
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,8,GaugeProperty.KEYBOARD);	
		assertTrue(100.0==gauge.getMaxValue());
		
		gauge = new GrooveGauge(bmsModel,8,GaugeProperty.LR2);
		assertTrue(100.0==gauge.getMaxValue());
		
	}
	@Test
	public void updateTest(){
		gauge = new GrooveGauge(bmsModel,8,GaugeProperty.LR2);
		gauge.update(3);
		System.out.println(gauge.getValue(3));
		assertTrue(33.28f==gauge.getValue(3));
		gauge.update(4);
		assertTrue(0==gauge.getValue(4));
		gauge.update(5);
		assertTrue(0==gauge.getValue(5));
	}
	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void updateIndexOutOfBoundsException1Test(){
		gauge = new GrooveGauge(bmsModel,8,GaugeProperty.LR2);
		gauge.update(6);
	}
	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void updateIndexOutOfBoundsException2Test(){
		gauge = GrooveGauge.create(bmsModel, 8,3, GaugeProperty.LR2);		
		gauge.update(-1);
	}
	@Test
	public void createGaugeTest(){
		gauge = GrooveGauge.create(bmsModel, 1, 1,GaugeProperty.LR2);
		assertNotNull(gauge);
		
		gauge = GrooveGauge.create(bmsModel, 1, 1,null);
		assertNotNull(gauge);
		gauge = GrooveGauge.create(bmsModel, 3, 1,null);
		assertNotNull(gauge);		
		gauge = GrooveGauge.create(bmsModel, 1, -1,null);
		assertNotNull(gauge);

		gauge = GrooveGauge.create(bmsModel, -1, 5,null);
		assertNotNull(gauge);
		gauge = GrooveGauge.create(bmsModel, -1, -1,null);
		assertNull(gauge);
	}
	@Test
	public void setValueTest(){
		gauge = GrooveGauge.create(bmsModel, 1, GaugeProperty.LR2);
		gauge.setValue(5.0f);		
		assertTrue(gauge.getValue(gauge.getType())==5.0f);
		gauge.setValue(120.0f);
		assertTrue(gauge.getValue(gauge.getType())==gauge.getMaxValue());
		gauge.setValue(-1.0f);
		assertTrue(gauge.getValue(gauge.getType())==gauge.getMinValue());
	}
	@Test
	public void isQualifiedTest(){
		gauge = GrooveGauge.create(bmsModel, 1, GaugeProperty.LR2);
		gauge.setValue(gauge.getBorder());
		assertTrue(gauge.isQualified());
		gauge.setValue(gauge.getBorder()-1);
		assertFalse(gauge.isQualified());
	}
	@Test
	public void changeTypeOfClearTest(){
		gauge = GrooveGauge.create(bmsModel, GrooveGauge.ASSISTEASY, 1,null);
		assertTrue(gauge.changeTypeOfClear(1)==gauge.getType());
		gauge = GrooveGauge.create(bmsModel, GrooveGauge.ASSISTEASY, 1,null);
		gauge.setValue(0.0f);
		assertTrue(gauge.changeTypeOfClear(1)==gauge.getType());
		gauge = GrooveGauge.create(bmsModel, GrooveGauge.EXHARDCLASS, 1,null);
		gauge.setValue(10.0f);
		gauge.changeTypeOfClear(GrooveGauge.EXHARDCLASS);
		
	}
}
