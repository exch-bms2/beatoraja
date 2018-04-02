package bms.player.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainController.SystemSoundManager;
import bms.player.beatoraja.MainLoader;
import bms.player.beatoraja.PlayDataAccessor;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlayerResource.PlayMode;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;

public class MainControllerTest {
	public static final int STATE_SELECTMUSIC = 0;
	public static final int STATE_DECIDE = 1;
	public static final int STATE_PLAYBMS = 2;
	public static final int STATE_SKIN_SELECT = 6;
	static Stage stage;
	static MainLoader mainLoader;
	static PlayerConfig playerconfig;
	static MainController main;
	static Config config;
	static Thread thread;

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
		Thread.sleep(10000); // Time to use the app, with out this, the thread
		// will be killed before you can tell.

	}
//
//	@Test
//	public void MainLoaderePlayTest() {
//		String a[] = { "" };
//		//mainLoader.main(a);
//	}
	
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
