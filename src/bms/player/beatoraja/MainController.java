package bms.player.beatoraja;

import java.awt.Rectangle;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import bms.model.*;
import bms.player.beatoraja.config.KeyConfiguration;
import bms.player.beatoraja.decide.MusicDecide;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.result.GradeResult;
import bms.player.beatoraja.result.MusicResult;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.skin.SkinNumber;
import bms.player.lunaticrave2.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

public class MainController extends ApplicationAdapter {

	private BMSPlayer player;
	private MusicDecide decide;
	private MusicSelector selector;
	private MusicResult result;
	private GradeResult gresult;
	private KeyConfiguration keyconfig;

	private PlayerResource resource;

	private BitmapFont systemfont;

	private MainState current;

	private Config config;
	private int auto;

	private LunaticRave2SongDatabaseManager songdb;

	private SpriteBatch sprite;
	private ShapeRenderer shape;
	/**
	 * 1曲プレイで指定したBMSファイル
	 */
	private File bmsfile;

	private BMSPlayerInputProcessor input;
	/**
	 * FPSを描画するかどうか
	 */
	private boolean showfps;
	/**
	 * プレイデータアクセサ
	 */
	private PlayDataAccessor playdata;

	public static final Rectangle[] RESOLUTION = {new Rectangle(640, 480),new Rectangle(1280, 720),new Rectangle(1920, 1280),new Rectangle(3840, 2560)};

	public MainController(File f, Config config, int auto) {
		this.auto = auto;
		this.config = config;
		this.bmsfile = f;

		try {
			Class.forName("org.sqlite.JDBC");
			songdb = new LunaticRave2SongDatabaseManager(new File("song.db").getPath(), true,
					BMSModel.LNTYPE_CHARGENOTE);
			songdb.createTable();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		playdata = new PlayDataAccessor("Player");
	}

	public LunaticRave2SongDatabaseManager getSongDatabase() {
		return songdb;
	}

	public PlayDataAccessor getPlayDataAccessor() {
		return playdata;
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
	public static final int STATE_GRADE_RESULT = 4;
	public static final int STATE_CONFIG = 5;

	public void changeState(int state) {
		switch (state) {
		case STATE_SELECTMUSIC:
			if (this.bmsfile != null) {
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
		case STATE_GRADE_RESULT:
			gresult.create(resource);
			current = gresult;
			break;
		case STATE_CONFIG:
			keyconfig.create(resource);
			current = keyconfig;
			break;
		}
		current.setStartTime(System.currentTimeMillis());
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
		gresult = new GradeResult(this);
		keyconfig = new KeyConfiguration(this);

		resource = new PlayerResource(config);

		if (bmsfile != null) {
			resource.setBMSFile(bmsfile, config, auto);
			changeState(STATE_PLAYBMS);
		} else {
			changeState(STATE_SELECTMUSIC);
		}

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
		systemfont = generator.generateFont(parameter);
		generator.dispose();

	}

	@Override
	public void render() {
		final int time = current.getNowTime();
		current.render();
		
		SkinNumber[] numbers = current.getSkin().getSkinNumbers();
		sprite.begin();
		for(SkinNumber number : numbers) {
			number.draw(sprite, time ,current);
		}
		sprite.end();

		// FPS表示切替
		if (input.getFunctionstate()[0] && input.getFunctiontime()[0] != 0) {
			showfps = !showfps;
			input.getFunctiontime()[0] = 0;
		}
		if (showfps) {
			sprite.begin();
			systemfont.setColor(Color.PURPLE);
			systemfont.draw(sprite, String.format("FPS %d", Gdx.graphics.getFramesPerSecond()), 10, 718);
			sprite.end();
		}

		// スクリーンショット
		if (input.getFunctionstate()[5] && input.getFunctiontime()[5] != 0) {
			byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(),
					Gdx.graphics.getBackBufferHeight(), true);

			Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(),
					Pixmap.Format.RGBA8888);
			BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			String path = "screenshot/" + sdf.format(Calendar.getInstance().getTime()) + ".png";
			PixmapIO.writePNG(new FileHandle(path), pixmap);
			pixmap.dispose();
			input.getFunctiontime()[5] = 0;
			Logger.getGlobal().info("スクリーンショット保存:" + path);
		}
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
		boolean config = (!new File("song.db").exists());
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
				config = json.fromJson(Config.class, new FileReader("config.json"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (SerializationException e) {
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
			MainController player = new MainController(f, config, auto);
			LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
			cfg.width = RESOLUTION[config.getResolution()].width;
			cfg.height = RESOLUTION[config.getResolution()].height;

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
			Logger.getGlobal().severe(e.getClass().getName() + " : " + e.getMessage());
		} catch (Error e) {
			e.printStackTrace();
			Logger.getGlobal().severe(e.getClass().getName() + " : " + e.getMessage());
		}
	}

	public void exit() {
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
					config = json.fromJson(Config.class, new FileReader("config.json"));
				} catch (Exception e) {
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
						BMSInformationLoader.class.getResource("/bms/player/beatoraja/PlayConfigurationView.fxml"));
				VBox stackPane = (VBox) loader.load();
				bmsinfo = (PlayConfigurationView) loader.getController();
				bmsinfo.update(config);
				// scene.getStylesheets().addAll("/bms/res/win7glass.css",
				// "/bms/res/style.css");
				// primaryStage.getIcons().addAll(this.primaryStage.getIcons());
				Scene scene = new Scene(stackPane, stackPane.getPrefWidth(), stackPane.getPrefHeight());

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
