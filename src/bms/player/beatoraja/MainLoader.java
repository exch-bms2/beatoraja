package bms.player.beatoraja;

import static bms.player.beatoraja.Resolution.*;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import com.badlogic.gdx.Graphics;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

/**
 * 起動用クラス
 *
 * @author exch
 */
public class MainLoader extends Application {

	private VBox stackPane;

	public static void main(String[] args) {
		Logger logger = Logger.getGlobal();
		try {
			logger.addHandler(new FileHandler("beatoraja_log.xml"));
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Path f = null;
		int auto = 0;
		boolean config = false;
		for (String s : args) {
			if (s.startsWith("-")) {
				if (s.equals("-a")) {
					auto = 1;
				}
				if (s.equals("-p")) {
					auto = 2;
				}
				if (s.equals("-r") || s.equals("-r1")) {
					auto = 3;
				}
				if (s.equals("-r2")) {
					auto = 4;
				}
				if (s.equals("-r3")) {
					auto = 5;
				}
				if (s.equals("-r4")) {
					auto = 6;
				}
				if (s.equals("-c")) {
					config = true;
				}
			} else {
				f = Paths.get(s);
			}
		}
		if (config) {
			launch(args);
		} else {
			play(f, auto, true);
		}
	}

	public static void play(Path f, int auto, boolean forceExit) {
		Config config = new Config();
		if (Files.exists(MainController.configpath)) {
			Json json = new Json();
			try {
				json.setIgnoreUnknownFields(true);
				config = json.fromJson(Config.class, new FileReader(MainController.configpath.toFile()));
				config.validate();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			Json json = new Json();
			json.setOutputType(OutputType.json);
			try {
				BufferedWriter fw = Files.newBufferedWriter(MainController.configpath);
				fw.write(json.prettyPrint(config));
				fw.flush();
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			MainController player = new MainController(f, config, auto);

			LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
			cfg.width = config.getResolution().width;
			cfg.height = config.getResolution().height;

			// fullscreen
			cfg.fullscreen = config.isFullscreen();
			// vSync
			cfg.vSyncEnabled = config.isVsync();
			if (!config.isVsync()) {
				cfg.backgroundFPS = config.getMaxFramePerSecond();
				cfg.foregroundFPS = config.getMaxFramePerSecond();
			} else {
				cfg.backgroundFPS = 0;
				cfg.foregroundFPS = 0;
			}
			cfg.title = MainController.VERSION;

			cfg.audioDeviceBufferSize = config.getAudioDeviceBufferSize();
			cfg.audioDeviceSimultaneousSources = config.getAudioDeviceSimultaneousSources();
			cfg.forceExit = forceExit;
			// System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL",
			// "true");
			new LwjglApplication(player, cfg);

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
//			new Lwjgl3Application(player, cfg);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.getGlobal().severe(e.getClass().getName() + " : " + e.getMessage());
		} catch (Error e) {
			e.printStackTrace();
			Logger.getGlobal().severe(e.getClass().getName() + " : " + e.getMessage());
		}
	}

	public static Graphics.DisplayMode[] getAvailableDisplayMode() {
		return LwjglApplicationConfiguration.getDisplayModes();
	}

	@Override
	public void start(javafx.stage.Stage primaryStage) throws Exception {
		Config config = new Config();
		if (Files.exists(MainController.configpath)) {
			Json json = new Json();
			try {
				json.setIgnoreUnknownFields(true);
				config = json.fromJson(Config.class, new FileReader(MainController.configpath.toFile()));
				config.validate();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			Json json = new Json();
			json.setOutputType(OutputType.json);
			try {
				FileWriter fw = new FileWriter(MainController.configpath.toFile());
				fw.write(json.prettyPrint(config));
				fw.flush();
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			ResourceBundle bundle = ResourceBundle.getBundle("resources.UIResources");
			FXMLLoader loader = new FXMLLoader(
					MainLoader.class.getResource("/bms/player/beatoraja/PlayConfigurationView.fxml"), bundle);
			stackPane = (VBox) loader.load();
			PlayConfigurationView bmsinfo = (PlayConfigurationView) loader.getController();
			bmsinfo.setBMSInformationLoader(this);
			bmsinfo.update(config);
			Scene scene = new Scene(stackPane, stackPane.getPrefWidth(), stackPane.getPrefHeight());
			primaryStage.setScene(scene);
			primaryStage.setTitle(MainController.VERSION + " configuration");
			primaryStage.show();

		} catch (IOException e) {
			Logger.getGlobal().severe(e.getMessage());
			e.printStackTrace();
		}
	}

	public void hide() {
		stackPane.setDisable(true);
	}
}