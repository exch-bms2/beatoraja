package bms.player.test;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.junit.Test;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import bms.player.beatoraja.MainLoader;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.PlayerResource.PlayMode;
import javafx.application.Application;
import javafx.stage.Stage;

public class runTest {

	@Test
	public void test() {		
		MainLoader main = new MainLoader();
		
		Path f = null;
		PlayMode auto = null;
		boolean forceExit = true;
		
		Config config = null;// = new Config();
//		config.setPlayername("p1");
		PlayerConfig player = new PlayerConfig();
		player.setId("p1");
		
		//main.play(null, null, true, null, null, true);
		main.play(null, null, true, null, player, true);

		if(config == null) {
			config = Config.read();			
		}
		
		try {
			MainController mainController = new MainController(f, config, player, PlayMode.PLAY, true);
/*
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
			cfg.forceExit = forceExit;
			if(config.getAudioDriver() != Config.AUDIODRIVER_SOUND && config.getAudioDriver() != Config.AUDIODRIVER_AUDIODEVICE) {
				LwjglApplicationConfiguration.disableAudio = true;				
			}
			new LwjglApplication(mainController, cfg);
			
			launch();*/
		} catch (Throwable e) {
			e.printStackTrace();
			Logger.getGlobal().severe(e.getClass().getName() + " : " + e.getMessage());
		}
		
		//launch();
		
		
	}
}
