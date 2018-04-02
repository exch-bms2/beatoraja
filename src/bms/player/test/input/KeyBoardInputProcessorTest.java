package bms.player.test.input;

import static org.junit.Assert.*;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainLoader;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.MainController.SystemSoundManager;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyBoardInputProcesseor;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import bms.player.beatoraja.PlayerResource.PlayMode;

public class KeyBoardInputProcessorTest {

	static Stage stage;
	static MainLoader mainLoader;
	static PlayerConfig playerconfig;
	static MainController main;
	static Config config;
	static Thread thread;
	static BMSPlayerInputProcessor bmsPlayerinputProcessor;
	static KeyBoardInputProcesseor keyboardInputProcesseor;

	@BeforeClass
	public static void setUp() throws InterruptedException {
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
							//mainLoader.play(null, PlayMode.PLAY, true, config, playerconfig, false);
							if(config == null) {
								config = Config.read();			
							}

							try {
								main = new MainController(null, config, playerconfig, PlayMode.PLAY, true);
								LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
								cfg.width = config.getResolution().width;
								cfg.height = config.getResolution().height;

								// fullscreen
								switch (config.getDisplaymode()) {
									case FULLSCREEN:
										cfg.fullscreen = true;
										break;
									case BORDERLESS:
										System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
										cfg.fullscreen = false;
										break;
									case WINDOW:
										cfg.fullscreen = false;
										break;
								}
								// vSync
								cfg.vSyncEnabled = config.isVsync();
								cfg.backgroundFPS = config.getMaxFramePerSecond();
								cfg.foregroundFPS = config.getMaxFramePerSecond();
								cfg.title = MainController.VERSION;

								
								cfg.audioDeviceBufferSize = config.getAudioDeviceBufferSize();
								cfg.audioDeviceSimultaneousSources = config.getAudioDeviceSimultaneousSources();
								cfg.forceExit = true;
								if(config.getAudioDriver() != Config.AUDIODRIVER_SOUND && config.getAudioDriver() != Config.AUDIODRIVER_AUDIODEVICE) {
									LwjglApplicationConfiguration.disableAudio = true;				
								}

								new LwjglApplication(main, cfg);

							} catch (Throwable e) {
								e.printStackTrace();
								Logger.getGlobal().severe(e.getClass().getName() + " : " + e.getMessage());
							}
							

						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} // Create and
							// initialize
							// your app.

					}
				});
			}
		});
		thread.start();// Initialize the thread
		Thread.sleep(15000); // Time to use the app, with out this, the thread
		// will be killed before you can tell.

		// Config config = new Config();
		// PlayerConfig playerconfig = new PlayerConfig();
		KeyboardConfig keyboardconfig = new KeyboardConfig();
		bmsPlayerinputProcessor = new BMSPlayerInputProcessor(config, playerconfig);
		keyboardInputProcesseor = new KeyBoardInputProcesseor(bmsPlayerinputProcessor, keyboardconfig, Resolution.HD);
		try {
			Robot robot = new Robot();
			keyboardInputProcesseor.setEnable(true);
			robot.keyPress(KeyEvent.VK_UP);
			robot.keyPress(KeyEvent.VK_DOWN);
			robot.keyPress(KeyEvent.VK_LEFT);
			robot.keyPress(KeyEvent.VK_RIGHT);
			robot.keyPress(KeyEvent.VK_S);
			robot.keyPress(KeyEvent.VK_X);
			robot.keyPress(KeyEvent.VK_1);
			robot.keyPress(KeyEvent.VK_2);
			robot.keyPress(KeyEvent.VK_3);
			robot.keyPress(KeyEvent.VK_ENTER);
			robot.keyPress(KeyEvent.VK_DELETE);
			robot.keyPress(KeyEvent.VK_F2);
			robot.keyPress(KeyEvent.VK_Q);
			robot.keyPress(KeyEvent.VK_W);
		} catch (AWTException e) {
			e.printStackTrace();
		}

	}

	/*
	 * setLastPressedKeyTest initialize key value is -1 input : int 0 ~ 255 output
	 * test use getLastPressedKey() function
	 */
	@Test
	public void setLastPressedValidKeytest() {
		keyboardInputProcesseor.setLastPressedKey(-1);
		assertEquals(-1, keyboardInputProcesseor.getLastPressedKey());
		keyboardInputProcesseor.setLastPressedKey(0);
		assertEquals(0, keyboardInputProcesseor.getLastPressedKey());
		keyboardInputProcesseor.setLastPressedKey(255);
		assertEquals(255, keyboardInputProcesseor.getLastPressedKey());
	}

	/*
	 * setLastPressedInvalidKeyTest is inserting invalid key value valid input : int
	 * 0~255 but minus value and extra 255 value is inserting
	 */
	@Test
	public void setLastPressedInValidKeytest() {
		keyboardInputProcesseor.clear();
		assertEquals(-1, keyboardInputProcesseor.getLastPressedKey());
		keyboardInputProcesseor.setLastPressedKey(256);
		assertEquals(256, keyboardInputProcesseor.getLastPressedKey());
		keyboardInputProcesseor.setLastPressedKey(-1);
		assertEquals(-1, keyboardInputProcesseor.getLastPressedKey());
	}

	/*
	 * isReservedKeyTest is reserved key test. valid input : keys class key value
	 * output : if Reserved Key is in reserved key return true else false
	 */
	@Test
	public void isReservedKeyTest() {
		assertEquals(true, keyboardInputProcesseor.isReservedKey(19));
		assertEquals(true, keyboardInputProcesseor.isReservedKey(20));
		assertEquals(true, keyboardInputProcesseor.isReservedKey(244));
		assertEquals(true, keyboardInputProcesseor.isReservedKey(245));
	}

	/*
	 * isReservedKeyInvalidTest is out of reserved key test. valid input : keys
	 * class key value output : if Reserved Key is in reserved key return false else
	 * true
	 */
	@Test
	public void isReservedKeyInvalidTest() {
		assertEquals(false, keyboardInputProcesseor.isReservedKey(256));
		assertEquals(false, keyboardInputProcesseor.isReservedKey(-2));
		assertEquals(false, keyboardInputProcesseor.isReservedKey(-1));
		assertEquals(false, keyboardInputProcesseor.isReservedKey(257));
	}

	/*
	 * mouseMovedtest is mouseMove(x,y) in graphics width and height default
	 * Gdx.graphics.getWidth = 1280 default Gdx.graphics.getHeight = 720 inputX :
	 * 0~1279 , inputY : 0~719 TEST : bmsPlayerInputProcessor.mousex return value
	 */
	@Test
	public void mouseMovedOneTest() {
		keyboardInputProcesseor.mouseMoved(0, 0);
		assertEquals(0 * Resolution.HD.width / Gdx.graphics.getWidth(), bmsPlayerinputProcessor.getMouseX());
		assertEquals(Resolution.HD.height - 0 * Resolution.HD.height / Gdx.graphics.getHeight(),
				bmsPlayerinputProcessor.getMouseY());
	}

	@Test
	public void mouseMovedTwoTest() {
		keyboardInputProcesseor.mouseMoved(1280, 0);
		assertEquals(1280 * Resolution.HD.width / Gdx.graphics.getWidth(), bmsPlayerinputProcessor.getMouseX());
		assertEquals(Resolution.HD.height - 0 * Resolution.HD.height / Gdx.graphics.getHeight(),
				bmsPlayerinputProcessor.getMouseY());
	}

	@Test
	public void mouseMovedThreeTest() {
		keyboardInputProcesseor.mouseMoved(0, 720);
		assertEquals(0 * Resolution.HD.width / Gdx.graphics.getWidth(), bmsPlayerinputProcessor.getMouseX());
		assertEquals(Resolution.HD.height - 720 * Resolution.HD.height / Gdx.graphics.getHeight(),
				bmsPlayerinputProcessor.getMouseY());
	}

	@Test
	public void mouseMovedFourTest() {
		keyboardInputProcesseor.mouseMoved(1280, 720);
		assertEquals(1280 * Resolution.HD.width / Gdx.graphics.getWidth(), bmsPlayerinputProcessor.getMouseX());
		assertEquals(Resolution.HD.height - 720 * Resolution.HD.height / Gdx.graphics.getHeight(),
				bmsPlayerinputProcessor.getMouseY());
	}

	/*
	 * mouseMovedtest is mouseMove(x,y) in graphics width and height default
	 * Gdx.graphics.getWidth = 1280 default Gdx.graphics.getHeight = 720 inputX :out
	 * of 0~1279 , inputY : out of 0~719 TEST : bmsPlayerInputProcessor.mousex
	 * return value this expected result is failure but result is success
	 */
	@Test
	public void mouseMovedInvalidOneTest() {
		keyboardInputProcesseor.mouseMoved(1281, 721);
		assertEquals(1281 * Resolution.HD.width / Gdx.graphics.getWidth(), bmsPlayerinputProcessor.getMouseX());
		assertEquals(Resolution.HD.height - 721 * Resolution.HD.height / Gdx.graphics.getHeight(),
				bmsPlayerinputProcessor.getMouseY());
	}

	@Test
	public void mouseMovedInvalidTwoTest() {
		keyboardInputProcesseor.mouseMoved(-1, -1);
		assertEquals((-1) * Resolution.HD.width / Gdx.graphics.getWidth(), bmsPlayerinputProcessor.getMouseX());
		assertEquals(Resolution.HD.height - (-1) * Resolution.HD.height / Gdx.graphics.getHeight(),
				bmsPlayerinputProcessor.getMouseY());
	}

	/*
	 * touchDown function test default Gdx.graphics.getWidth = 1280 default
	 * Gdx.graphics.getHeight = 720 inputX :out of 0~1279 , inputY : out of 0~719
	 * TEST : bmsPlayerInputProcessor.mousex return value
	 */
	@Test
	public void TouchDownOneTest() {
		keyboardInputProcesseor.touchDown(0, 0, 100, 244);
		assertEquals(244, bmsPlayerinputProcessor.getMouseButton());
		assertEquals(0 * Resolution.HD.width / Gdx.graphics.getWidth(), bmsPlayerinputProcessor.getMouseX());
		assertEquals(Resolution.HD.height - 0 * Resolution.HD.height / Gdx.graphics.getHeight(),
				bmsPlayerinputProcessor.getMouseY());
	}

	@Test
	public void TouchDownTwoTest() {
		keyboardInputProcesseor.touchDown(1280, 0, 100, 245);
		assertEquals(245, bmsPlayerinputProcessor.getMouseButton());
		assertEquals(1280 * Resolution.HD.width / Gdx.graphics.getWidth(), bmsPlayerinputProcessor.getMouseX());
		assertEquals(Resolution.HD.height - 0 * Resolution.HD.height / Gdx.graphics.getHeight(),
				bmsPlayerinputProcessor.getMouseY());
	}

	@Test
	public void TouchDownThreeTest() {
		keyboardInputProcesseor.touchDown(0, 720, 100, 19);
		assertEquals(19, bmsPlayerinputProcessor.getMouseButton());
		assertEquals(0 * Resolution.HD.width / Gdx.graphics.getWidth(), bmsPlayerinputProcessor.getMouseX());
		assertEquals(Resolution.HD.height - 720 * Resolution.HD.height / Gdx.graphics.getHeight(),
				bmsPlayerinputProcessor.getMouseY());
	}

	@Test
	public void TouchDownFourTest() {
		keyboardInputProcesseor.touchDown(1280, 720, 100, 20);
		assertEquals(20, bmsPlayerinputProcessor.getMouseButton());
		assertEquals(1280 * Resolution.HD.width / Gdx.graphics.getWidth(), bmsPlayerinputProcessor.getMouseX());
		assertEquals(Resolution.HD.height - 720 * Resolution.HD.height / Gdx.graphics.getHeight(),
				bmsPlayerinputProcessor.getMouseY());
	}

	/*
	 * touchDown function test is mouseMove(x,y) in graphics width and height
	 * default Gdx.graphics.getWidth = 1280 default Gdx.graphics.getHeight = 720
	 * inputX :out of 0~1279 , inputY : out of 0~719 TEST :
	 * bmsPlayerInputProcessor.mousex return value this expected result is failure
	 * but result is success
	 */
	@Test
	public void TouchDownInvalidOneTest() {
		keyboardInputProcesseor.touchDown(1281, 721, 100, 256);
		assertEquals(256, bmsPlayerinputProcessor.getMouseButton());
		assertEquals(1281 * Resolution.HD.width / Gdx.graphics.getWidth(), bmsPlayerinputProcessor.getMouseX());
		assertEquals(Resolution.HD.height - 721 * Resolution.HD.height / Gdx.graphics.getHeight(),
				bmsPlayerinputProcessor.getMouseY());
	}

	@Test
	public void TouchDownInvalidTwoTest() {
		keyboardInputProcesseor.touchDown(-1, -1, 100, -1);
		assertEquals(-1, bmsPlayerinputProcessor.getMouseButton());
		assertEquals((-1) * Resolution.HD.width / Gdx.graphics.getWidth(), bmsPlayerinputProcessor.getMouseX());
		assertEquals(Resolution.HD.height - (-1) * Resolution.HD.height / Gdx.graphics.getHeight(),
				bmsPlayerinputProcessor.getMouseY());
	}

	/*
	 * touch Dragged function Test x,y input data is tested above Tests so one test
	 * will run.
	 */
	@Test
	public void touchDraggedTest() {
		keyboardInputProcesseor.touchDragged(1280, 720, 100);
		assertEquals(true, bmsPlayerinputProcessor.isMouseDragged());
		assertEquals(1280 * Resolution.HD.width / Gdx.graphics.getWidth(), bmsPlayerinputProcessor.getMouseX());
		assertEquals(Resolution.HD.height - 720 * Resolution.HD.height / Gdx.graphics.getHeight(),
				bmsPlayerinputProcessor.getMouseY());
	}

	/*
	 * scrolled function test intput : scroll value -100~100
	 * 
	 * output : getScrolled function equals
	 */
	@Test
	public void scrolledOneTest() {
		int before_scrolled = bmsPlayerinputProcessor.getScroll();
		keyboardInputProcesseor.scrolled(-100);
		int after_scrolled = bmsPlayerinputProcessor.getScroll();
		assertEquals(before_scrolled + 100, after_scrolled);
	}

	@Test
	public void scrolledTwoTest() {
		int before_scrolled = bmsPlayerinputProcessor.getScroll();
		keyboardInputProcesseor.scrolled(100);
		int after_scrolled = bmsPlayerinputProcessor.getScroll();
		assertEquals(before_scrolled - 100, after_scrolled);
	}

	/*
	 * Poll function test intput : default Gdx.graphics.getWidth = 1280 output :
	 * getScrolled function equals
	 */

	@Test
	public void HeadPressPollTest() {
		keyboardInputProcesseor.setEnable(true);
		keyboardInputProcesseor.poll(0);
		assertEquals(true, bmsPlayerinputProcessor.isEnterPressed());
	}

	@Test
	public void ControlPressPollTest() {
		keyboardInputProcesseor.setEnable(true);
		keyboardInputProcesseor.poll(0);
		assertEquals(true, bmsPlayerinputProcessor.startPressed());
		assertEquals(true, bmsPlayerinputProcessor.isSelectPressed());

	}

	@Test
	public void numbersPressPollTest() {
		keyboardInputProcesseor.setEnable(true);
		keyboardInputProcesseor.poll(100);
		assertEquals(true, bmsPlayerinputProcessor.getNumberState()[1]);
		assertEquals(true, bmsPlayerinputProcessor.getNumberState()[2]);
		assertEquals(true, bmsPlayerinputProcessor.getNumberState()[3]);

	}

	@Test
	public void coverPressPollTest() {
		keyboardInputProcesseor.setEnable(true);
		keyboardInputProcesseor.poll(100);
		assertEquals(true, bmsPlayerinputProcessor.getCursorState()[0]);
		assertEquals(true, bmsPlayerinputProcessor.getNumberState()[1]);
		assertEquals(true, bmsPlayerinputProcessor.getNumberState()[2]);
		assertEquals(true, bmsPlayerinputProcessor.getNumberState()[3]);

	}

	@Test
	public void functionPressPollTest() {
		keyboardInputProcesseor.setEnable(true);

		keyboardInputProcesseor.poll(100);
		assertEquals(true, bmsPlayerinputProcessor.getFunctionstate()[1]);
	}

	@Test
	public void KeyPressPollTest() {
		keyboardInputProcesseor.setEnable(true);

		keyboardInputProcesseor.poll(100);
		assertEquals(true, bmsPlayerinputProcessor.getKeystate()[1]);
		assertEquals(true, bmsPlayerinputProcessor.getKeystate()[2]);

	}
	
	@Test
	public void MainLoaderSettingtest() {
		assertEquals("beatoraja 0.5.5 configuration", stage.getTitle());
		assertEquals("647.0", String.valueOf(stage.getHeight()));
		assertEquals("818.0", String.valueOf(stage.getWidth()));
	}
	@Test
	public void MainControllerConfigtest() {
		//assertEquals("beatoraja 0.5.5 configuration", main.getConfig().getAudioDriverName());
		assertEquals("647.0", String.valueOf(stage.getHeight()));
		assertEquals("818.0", String.valueOf(stage.getWidth()));
	}
	
	/*
	 * maincontroller inner class soundManager Suffle function test
	 * 
	 */
	@Test
	public void MainControllerSuffletest() {
		SystemSoundManager system = new SystemSoundManager(config); 
		main.getSoundManager().shuffle();
	}

	

}
