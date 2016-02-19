package bms.player.beatoraja;

import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.swing.JFileChooser;

import bms.model.*;
import bms.player.beatoraja.decide.MusicDecide;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.result.MusicResult;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.lunaticrave2.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

public class MainController extends ApplicationAdapter {

	private BMSPlayer player;
	private MusicDecide decide;
	private MusicSelector selector;
	private MusicResult result;

	private ApplicationAdapter current;

	private Config config;
	private int auto;

	private LunaticRave2ScoreDatabaseManager scoredb;
	private LunaticRave2SongDatabaseManager songdb;

	private SpriteBatch sprite;
	private ShapeRenderer shape;

	private File f;

	private BMSPlayerInputProcessor input;

	public MainController(File f, Config config, int auto) {
		this.auto = auto;
		this.config = config;
		this.f = f;

		try {
			Class.forName("org.sqlite.JDBC");
			scoredb = new LunaticRave2ScoreDatabaseManager(new File(".")
					.getAbsoluteFile().getParent(), "/", "/");
			scoredb.createTable("Player");
			songdb = new LunaticRave2SongDatabaseManager(
					new File("song.db").getPath(), true,
					BMSModel.LNTYPE_CHARGENOTE);
			songdb.createTable();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public LunaticRave2ScoreDatabaseManager getScoreDatabase() {
		return scoredb;
	}

	public LunaticRave2SongDatabaseManager getSongDatabase() {
		return songdb;
	}

	public SpriteBatch getSpriteBatch() {
		return sprite;
	}

	public ShapeRenderer getShapeRenderer() {
		return shape;
	}

	public static final int STATE_SELECTMUSIC = 0;
	public static final int STATE_DECIDE = 1;
	public static final int STATE_PLAYBMS = 2;
	public static final int STATE_RESULT = 3;

	public void changeState(int state, PlayerResource resource) {
		switch (state) {
		case STATE_SELECTMUSIC:
			if (this.f != null) {
				exit();
			}
			selector.create(resource);
			current = selector;
			break;
		case STATE_DECIDE:
			decide.create(resource);
			current = decide;
			break;
		case STATE_PLAYBMS:
			player = new BMSPlayer(this, resource);
			player.create();
			current = player;
			break;
		case STATE_RESULT:
			result.create(resource);
			current = result;
			break;
		}
	}

	public void setAuto(int auto) {
		this.auto = auto;

	}

	@Override
	public void create() {
		sprite = new SpriteBatch();
		shape = new ShapeRenderer();

		input = new BMSPlayerInputProcessor();

		selector = new MusicSelector(this, config);
		decide = new MusicDecide(this);
		result = new MusicResult(this);

		if (f != null) {
			PlayerResource resource = new PlayerResource();
			resource.setBMSFile(f, config, auto);
			changeState(STATE_PLAYBMS, resource);
		} else {
			changeState(STATE_SELECTMUSIC, null);
		}
	}

	@Override
	public void render() {
		current.render();

//		if (false) {
//			byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0,
//					Gdx.graphics.getBackBufferWidth(),
//					Gdx.graphics.getBackBufferHeight(), true);
//
//			Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(),
//					Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
//			BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
//			PixmapIO.writePNG(Gdx.files.external("mypixmap.png"), pixmap);
//			pixmap.dispose();
//		}
	}

	@Override
	public void dispose() {
		shape.dispose();
		sprite.dispose();
		if (player != null) {
			player.dispose();
		}
		if (selector != null) {
			selector.dispose();
		}
		if (decide != null) {
			decide.dispose();
		}
		if (result != null) {
			result.dispose();
		}
	}

	@Override
	public void pause() {
		current.pause();
	}

	@Override
	public void resize(int width, int height) {
		current.resize(width, height);
	}

	@Override
	public void resume() {
		current.resume();
	}

	public static void main(String[] args) {
		Logger logger = Logger.getGlobal();
		try {
			logger.addHandler(new FileHandler("beatoraja_log.xml"));
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		File f = null;
		int auto = 0;
		boolean config = false;
		for (String s : args) {
			if (s.startsWith("-")) {
				if (s.equals("-a")) {
					auto = 1;
				}
				if (s.equals("-r")) {
					auto = 2;
				}
				if (s.equals("-c")) {
					config = true;
				}
			} else {
				f = new File(s);
			}
		}
		if (config) {
			BMSInformationLoader.main(args);
		} else {
			MainController.play(f, auto, true);
		}
	}

	public static void play(File f, int auto, boolean forceExit) {
		Config config = new Config();
		if (new File("config.json").exists()) {
			Json json = new Json();
			try {
				config = json.fromJson(Config.class, new FileReader(
						"config.json"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			Json json = new Json();
			json.setOutputType(OutputType.json);
			try {
				FileWriter fw = new FileWriter("config.json");
				fw.write(json.prettyPrint(config));
				fw.flush();
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (config.getBmsroot().length == 0) {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setDialogTitle("Choose BMS root directory");
			if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				File[] files = new File[] { chooser.getSelectedFile() };
				String[] rootdir = new String[] { files[0].getAbsolutePath() };
				config.setBmsroot(rootdir);
				Json json = new Json();
				json.setOutputType(OutputType.json);
				try {
					FileWriter fw = new FileWriter("config.json");
					fw.write(json.prettyPrint(config));
					fw.flush();
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					LunaticRave2SongDatabaseManager songdb = new LunaticRave2SongDatabaseManager(
							new File("song.db").getPath(), true,
							BMSModel.LNTYPE_CHARGENOTE);
					songdb.createTable();
					Logger.getGlobal().info("song.db更新開始");
					songdb.updateSongDatas(files, rootdir,
							new File(".").getAbsolutePath());
					Logger.getGlobal().info("song.db更新完了");
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}

			} else {
				System.exit(1);
			}
		}

		try {
			MainController player = new MainController(f, config, auto);
			LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
			cfg.width = 1280;
			cfg.height = 720;

			// fullscreen
			cfg.fullscreen = config.isFullscreen();
			// vSync
			cfg.vSyncEnabled = config.isVsync();
			if (!config.isVsync()) {
				cfg.backgroundFPS = config.getMaxFramePerSecond();
				cfg.foregroundFPS = config.getMaxFramePerSecond();
			}
			cfg.title = "Beatoraja";

			cfg.audioDeviceBufferSize = config.getAudioDeviceBufferSize();
			cfg.audioDeviceSimultaneousSources = config.getAudioDeviceSimultaneousSources();
			cfg.forceExit = forceExit;

			new LwjglApplication(player, cfg);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.getGlobal().severe(
					e.getClass().getName() + " : " + e.getMessage());
		} catch (Error e) {
			e.printStackTrace();
			Logger.getGlobal().severe(
					e.getClass().getName() + " : " + e.getMessage());
		}
	}

	public void exit() {
		Gdx.app.exit();
	}

	public BMSPlayerInputProcessor getInputProcessor() {
		return input;
	}

	public static class BMSInformationLoader extends Application {

		private PlayConfigurationView bmsinfo;

		public static void main(String[] args) {
			launch(args);
		}

		@Override
		public void start(Stage primaryStage) throws Exception {
			Config config = new Config();
			if (new File("config.json").exists()) {
				Json json = new Json();
				try {
					config = json.fromJson(Config.class, new FileReader(
							"config.json"));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			} else {
				Json json = new Json();
				json.setOutputType(OutputType.json);
				try {
					FileWriter fw = new FileWriter("config.json");
					fw.write(json.prettyPrint(config));
					fw.flush();
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try {
				FXMLLoader loader = new FXMLLoader(
						BMSInformationLoader.class
								.getResource("/bms/player/beatoraja/PlayConfigurationView.fxml"));
				VBox stackPane = (VBox) loader.load();
				bmsinfo = (PlayConfigurationView) loader.getController();
				bmsinfo.update(config);
				// scene.getStylesheets().addAll("/bms/res/win7glass.css",
				// "/bms/res/style.css");
				// primaryStage.getIcons().addAll(this.primaryStage.getIcons());
				Scene scene = new Scene(stackPane, stackPane.getPrefWidth(),
						stackPane.getPrefHeight());

				primaryStage.setScene(scene);
				primaryStage.setTitle("beatoraja configuration");
				primaryStage.show();

			} catch (IOException e) {
				Logger.getGlobal().severe(e.getMessage());
				e.printStackTrace();
			}
		}

	}
}
