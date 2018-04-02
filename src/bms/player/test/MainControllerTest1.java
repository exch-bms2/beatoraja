package bms.player.test;

import static org.junit.Assert.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bms.model.BMSModel;
import bms.player.beatoraja.BMSResource;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainController.SystemSoundManager;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.decide.MusicDecide;
import bms.player.beatoraja.external.BMSSearchAccessor;
import bms.player.beatoraja.pattern.AutoplayModifier;
import bms.player.beatoraja.select.MusicSelector;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import bms.player.beatoraja.MainLoader;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlayerResource;

public class MainControllerTest1 {
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
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		thread = new Thread(new Runnable() {
	          @Override
	          public void run() {
	             new JFXPanel(); // Initializes the JavaFx Platform
	             Platform.runLater(new Runnable() {
	                @Override
	                public void run() {
	                   try {
	                      stage = new Stage();
	                      mainLoader = new MainLoader();
	                      mainLoader.start(stage);
	                      config = new Config();
	                      config.setPlayername("player1");
	                      playerconfig = new PlayerConfig();
	                      playerconfig.setId("player1");
	                      mainLoader.play(null, PlayMode.PLAY, true, config, playerconfig, false);
	                      mainController = mainLoader.getController();
	                   } catch (Exception e) {
	                      e.printStackTrace();
	                   } 
	                }
	             });
	          }
	       });
	       thread.start();// Initialize the thread
	       Thread.sleep(10000);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	/*
	 * When test process is over, test the 'exit' method.
	 * */
	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void pauseResumeTest() {
		mainController.pause();
		mainController.resume();
	}
	
	@Test
	public void disposeTest() {
		//
	}
	
	@Test
	public void saveConfigTest() {
		mainController.saveConfig();
		Config playerConfig = mainController.getConfig();
		assertEquals(config, null);
	}
	
	/*
	 * It will be check each path which indicates sound files
	 * and Bgm files is null or not.
	 * */
	@Test
	public void configureInitTest() {
		assertNotNull(config.getSoundpath());
		assertNotNull(config.getBgmpath());
		
		assertNotEquals(config.getSoundpath(), "");
		assertNotEquals(config.getBgmpath(), "");
	}
	
	@Test
	public void systemSoundManagerConstructorTest() {
		SystemSoundManager systemSoundManager = new SystemSoundManager(config);
		assertNotNull(systemSoundManager);
		assertNotNull(systemSoundManager.getBGMPath());
		assertNotNull(systemSoundManager.getSoundPath());
	}
	
	/*
	 * It will be check shuffle method actually can change
	 * current paths. Unfortunately, there is a probability
	 * that even if current paths was changed but same.
	 * */
	@Test
	public void systemSoundManagerShuffleTest() {
		SystemSoundManager systemSoundManager = new SystemSoundManager(config);
		Path currentBGMPath = systemSoundManager.getBGMPath();
		Path currentSoundPath = systemSoundManager.getSoundPath();
		systemSoundManager.shuffle();
		assertNotEquals(currentBGMPath, systemSoundManager.getBGMPath());
		assertNotEquals(currentSoundPath, systemSoundManager.getSoundPath());
	}
	
	@Test
	public void songDBValidTest() {
		assertNotNull(mainController.getSongDatabase());
	}
	
	@Test
	public void songInfoValidtest() {
		assertNotNull(mainController.getInfoDatabase());
	}
	
	@Test
	public void configValidTest() {
		assertNotNull(mainController.getConfig());
	}
	
	@Test
	public void autoPlayModifierZeroMarginTest() {
		final int[] lanes = {3, 4, 55, 1, 2, -3};
		AutoplayModifier autoplayModifier = new AutoplayModifier(lanes);
		autoplayModifier.modify(mainController.getPlayerResource().getBMSModel());
	}
	
	@Test
	public void BMSResourceTest() {
		BMSResource bmsResource = new BMSResource(mainController.getAudioProcessor(),
										mainController.getConfig(),
										mainController.getPlayerConfig());
		assertNotNull(bmsResource);
	}
	
	@Test
	public void mediaLoadFinishedTest() {
		BMSResource bmsResource = new BMSResource(mainController.getAudioProcessor(),
				mainController.getConfig(),
				mainController.getPlayerConfig());
		assertEquals(bmsResource.mediaLoadFinished(),false);
	}
	
	@Test
	public void disposeBMSResourceTest() {
		BMSResource bmsResource = new BMSResource(mainController.getAudioProcessor(),
				mainController.getConfig(),
				mainController.getPlayerConfig());
		bmsResource.dispose();
		assertNull(bmsResource.getBackbmp());
		assertNull(bmsResource.getBGAProcessor());
		assertNull(bmsResource.getStagefile());
	}
	
	@Test
	public void musicDecideTest() {
		MusicDecide musicDecide = new MusicDecide(mainController);
		assertNotNull(musicDecide);
	}
	
	@Test
	public void BMSSearchAccesor() {
		BMSSearchAccessor bmsSearchAccessor = new BMSSearchAccessor();
		assertNotNull(bmsSearchAccessor);
		bmsSearchAccessor.read();
	}

}
