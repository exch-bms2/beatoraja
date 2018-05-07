package bms.player.beatoraja;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import com.badlogic.gdx.Graphics;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.launcher.PlayConfigurationView;

/**
 * 壅룟땿�뵪�궚�꺀�궧
 *
 * @author exch
 */
public class MainLoader extends Application {
	
	private static final boolean ALLOWS_32BIT_JAVA = false;
	public static MainController main; 
	public static void main(String[] args) {
		if(!ALLOWS_32BIT_JAVA && !System.getProperty( "os.arch" ).contains( "64")) {
			JOptionPane.showMessageDialog(null, "This Application needs 64bit-Jaja.", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		
		Logger logger = Logger.getGlobal();
		try {
			logger.addHandler(new FileHandler("beatoraja_log.xml"));
		} catch (Throwable e) {
			e.printStackTrace();
		}

		Path f = null;
		PlayMode auto = null;
		for (String s : args) {
			if (s.startsWith("-")) {
				if (s.equals("-a")) {
					auto = PlayMode.AUTOPLAY;
				}
				if (s.equals("-p")) {
					auto = PlayMode.PRACTICE;
				}
				if (s.equals("-r") || s.equals("-r1")) {
					auto = PlayMode.REPLAY_1;
				}
				if (s.equals("-r2")) {
					auto = PlayMode.REPLAY_2;
				}
				if (s.equals("-r3")) {
					auto = PlayMode.REPLAY_3;
				}
				if (s.equals("-r4")) {
					auto = PlayMode.REPLAY_4;
				}
				if (s.equals("-s")) {
					auto = PlayMode.PLAY;
				}
			} else {
				f = Paths.get(s);
				if(auto == null) {
					auto = PlayMode.PLAY;
				}
			}
		}
		
		if(Files.exists(MainController.configpath) && (f != null || auto != null)) {
			play(f, auto, true, null, null, f != null);			
		} else {
			launch(args);			
		}
	}

	public static void play(Path f, PlayMode auto, boolean forceExit, Config config, PlayerConfig player, boolean songUpdated) {		
		if(config == null) {
			config = Config.read();
		}

		try {
	
			main = new MainController(f, config, player, auto, songUpdated);
			
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
			// System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL",
			// "true");
			new LwjglApplication(main, cfg);
//			Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
//
//			final int w = (int) RESOLUTION[config.getResolution()].width;
//			final int h = (int) RESOLUTION[config.getResolution()].height;
//			if (config.isFullscreen()) {
//				DisplayMode d = null;
//				for (DisplayMode display : cfg.getDisplayModes()) {
//					System.out.println("available DisplayMode : w - " + display.width + " h - " + display.height
//							+ " refresh - " + display.refreshRate + " color bit - " + display.bitsPerPixel);
//					if (display.width == w
//							&& display.height == h
//							&& (d == null || (d.refreshRate <= display.refreshRate && d.bitsPerPixel <= display.bitsPerPixel))) {
//						d = display;
//					}
//				}
//				if (d != null) {
//					cfg.setFullscreenMode(d);
//				} else {
//					cfg.setWindowedMode(w, h);
//				}
//			} else {
//				cfg.setWindowedMode(w, h);
//			}
//			// vSync
//			cfg.useVsync(config.isVsync());
//			cfg.setIdleFPS(config.getMaxFramePerSecond());
//			cfg.setTitle(VERSION);
//
//			cfg.setAudioConfig(config.getAudioDeviceSimultaneousSources(), config.getAudioDeviceBufferSize(), 1);
//
//			new Lwjgl3Application(main, cfg);
		} catch (Throwable e) {
			e.printStackTrace();
			Logger.getGlobal().severe(e.getClass().getName() + " : " + e.getMessage());
		}
	}

	public static Graphics.DisplayMode[] getAvailableDisplayMode() {
		return LwjglApplicationConfiguration.getDisplayModes();
	}

	public static Graphics.DisplayMode getDesktopDisplayMode() {
		return LwjglApplicationConfiguration.getDesktopDisplayMode();
	}

	@Override
	public void start(javafx.stage.Stage primaryStage) throws Exception {
		Config config = Config.read();
		
		try {
			ResourceBundle bundle = ResourceBundle.getBundle("resources.UIResources");
			FXMLLoader loader = new FXMLLoader(
					MainLoader.class.getResource("/bms/player/beatoraja/launcher/PlayConfigurationView.fxml"), bundle);
			VBox stackPane = (VBox) loader.load();
			PlayConfigurationView bmsinfo = (PlayConfigurationView) loader.getController();
			bmsinfo.setBMSInformationLoader(this);
			bmsinfo.update(config);
			Scene scene = new Scene(stackPane, stackPane.getPrefWidth(), stackPane.getPrefHeight());
			primaryStage.setScene(scene);
			primaryStage.setTitle(MainController.VERSION + " configuration");
			primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {				
				@Override
				public void handle(WindowEvent event) {
					bmsinfo.exit();
				}
			});
			primaryStage.show();

		} catch (IOException e) {
			Logger.getGlobal().severe(e.getMessage());
			e.printStackTrace();
		}
	}
}