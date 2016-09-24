package bms.player.beatoraja;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import com.badlogic.gdx.Graphics;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import bms.player.beatoraja.config.KeyConfiguration;
import bms.player.beatoraja.decide.MusicDecide;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.result.GradeResult;
import bms.player.beatoraja.result.MusicResult;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.SQLiteSongDatabaseAccessor;
import bms.player.beatoraja.song.SongDatabaseAccessor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.badlogic.gdx.utils.viewport.FitViewport;

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

	private SongDatabaseAccessor songdb;

	private SpriteBatch sprite;
	private ShapeRenderer shape;
	/**
	 * 1曲プレイで指定したBMSファイル
	 */
	private Path bmsfile;

	private BMSPlayerInputProcessor input;
	/**
	 * FPSを描画するかどうか
	 */
	private boolean showfps;
	/**
	 * プレイデータアクセサ
	 */
	private PlayDataAccessor playdata;
	
	private static final Path configpath = Paths.get("config.json");
	private static final Path songdbpath = Paths.get("songdata.db");
	
	private TextField search;
	
	private ScreenShotThread screenshot;

	public static final Rectangle[] RESOLUTION = { new Rectangle(0, 0, 640, 480), new Rectangle(0, 0, 1280, 720),
			new Rectangle(0, 0, 1920, 1080), new Rectangle(0, 0, 3840, 2160) };

	public MainController(Path f, Config config, int auto) {
		this.auto = auto;
		this.config = config;
		this.bmsfile = f;

		try {
			Class.forName("org.sqlite.JDBC");
			songdb = new SQLiteSongDatabaseAccessor(songdbpath.toString());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		playdata = new PlayDataAccessor("playerscore");
	}

	public SongDatabaseAccessor getSongDatabase() {
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
	
	public PlayerResource getPlayerResource() {
		return resource;
	}

	public static final int STATE_SELECTMUSIC = 0;
	public static final int STATE_DECIDE = 1;
	public static final int STATE_PLAYBMS = 2;
	public static final int STATE_RESULT = 3;
	public static final int STATE_GRADE_RESULT = 4;
	public static final int STATE_CONFIG = 5;

	public void changeState(int state) {
		MainState newState = null;
		switch (state) {
		case STATE_SELECTMUSIC:
			if (this.bmsfile != null) {
				exit();
			}
			newState = selector;
			break;
		case STATE_DECIDE:
			newState = decide;
			break;
		case STATE_PLAYBMS:
			if(player != null) {
				player.dispose();
			}
			player = new BMSPlayer(this, resource);
			newState = player;
			break;
		case STATE_RESULT:
			newState = result;
			break;
		case STATE_GRADE_RESULT:
			newState = gresult;
			break;
		case STATE_CONFIG:
			newState = keyconfig;
			break;
		}
		
		if(newState != null && current != newState) {
			Arrays.fill(newState.getTimer(), Long.MIN_VALUE);
			newState.create();
			current = newState;
			current.setStartTime(System.currentTimeMillis());			
		}
	}

	public void setAuto(int auto) {
		this.auto = auto;

	}

	@Override
	public void create() {
		sprite = new SpriteBatch();
		shape = new ShapeRenderer();

		input = new BMSPlayerInputProcessor(RESOLUTION[config.getResolution()]);

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

//		Stage stage = new Stage(new FitViewport(RESOLUTION[config.getResolution()].width, RESOLUTION[config.getResolution()].height));
//		/* TextfieldStyleの定義 */
//		TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle(
//				systemfont,                           // BitmapFont
//		    Color.BLACK,                    // font color
//		    new TextureRegionDrawable(new TextureRegion(new Texture("skin/system.png"),0,0,8,8)),      // cusor image 
//		    new TextureRegionDrawable(new TextureRegion(new Texture("skin/system.png"),0,0,2,8)),   // selectoin image 
//		    new TextureRegionDrawable(new TextureRegion(new Texture("skin/system.png"),0,8,8,8))); // background image
//
//		/* textFieldの生成 */
//		search = new TextField("sample", textFieldStyle);
//
//		/* 文字数を10に制限する */
//		search.setMaxLength(10);
//
//		/* tabキーでTextField間をできなくする(デフォルトはtrue)   */
//		search.setFocusTraversal(false);
//
//		/* TextFieldをStageに追加 */
//		stage.addActor(search);
//
//		/* TextFieldをフォーカス状態にする(複数ある場合） */
//		stage.setKeyboardFocus(search);
//		
//		search.setVisible(true);
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		current.render();
		sprite.begin();
		current.getSkin().drawAllObjects(sprite, current);
		sprite.end();
		// move song bar position by mouse
		if (input.isMouseConsumed()) {
			input.setMouseConsumed();
			current.getSkin().mousePressed(current, input.getMouseX(), input.getMouseY());
		}

		// FPS表示切替
		if (input.getFunctionstate()[0] && input.getFunctiontime()[0] != 0) {
			showfps = !showfps;
			input.getFunctiontime()[0] = 0;
		}
		if (showfps) {
			sprite.begin();
			systemfont.setColor(Color.PURPLE);
			systemfont.draw(sprite, String.format("FPS %d", Gdx.graphics.getFramesPerSecond()), 10,
					RESOLUTION[config.getResolution()].height - 2);
			sprite.end();
		}

		// fullscrees - windowed
		if (input.getFunctionstate()[3] && input.getFunctiontime()[3] != 0) {
			boolean fullscreen = Gdx.graphics.isFullscreen();
			Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
			if (fullscreen) {
				Gdx.graphics.setWindowedMode(currentMode.width, currentMode.height);
			}
			else {
				Gdx.graphics.setFullscreenMode(currentMode);
			}
			config.setFullscreen(!fullscreen);
			input.getFunctiontime()[3] = 0;
		}
		
//		if (input.getFunctionstate()[4] && input.getFunctiontime()[4] != 0) {
//			int resolution = config.getResolution();
//			resolution = (resolution + 1) % RESOLUTION.length;
//			if (config.isFullscreen()) {
//				Gdx.graphics.setWindowedMode((int) RESOLUTION[resolution].width, (int) RESOLUTION[resolution].height);
//				Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
//				Gdx.graphics.setFullscreenMode(currentMode);
//			}
//			else {
//				Gdx.graphics.setWindowedMode((int) RESOLUTION[resolution].width, (int) RESOLUTION[resolution].height);
//			}
//			config.setResolution(resolution);
//			input.getFunctiontime()[4] = 0;
//		}

		// screen shot
		if (input.getFunctionstate()[5] && input.getFunctiontime()[5] != 0) {
			if(screenshot == null || screenshot.isCompleted) {
				screenshot = new ScreenShotThread();
				screenshot.pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(),
						Gdx.graphics.getBackBufferHeight(), true);
				screenshot.start();
				screenshot.savetime = System.currentTimeMillis();
			}
		}
		if(screenshot != null && screenshot.savetime + 2000 > System.currentTimeMillis()) {
			sprite.begin();
			systemfont.setColor(Color.GOLD);
			systemfont.draw(sprite, "Screen shot saved : " + screenshot.path, 100,
					RESOLUTION[config.getResolution()].height - 2);
			sprite.end();			
		}
	}

	@Override
	public void dispose() {
//		shape.dispose();
//		sprite.dispose();
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
		if (gresult != null) {
			gresult.dispose();
		}
		if(keyconfig != null) {
			keyconfig.dispose();
		}
		resource.dispose();
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

		Path f = null;
		int auto = 0;
		boolean config = (!Files.exists(Paths.get("songdata.db")));
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
				f = Paths.get(s);
			}
		}
		if (config) {
			BMSInformationLoader.main(args);
		} else {
			MainController.play(f, auto, true);
		}
	}

	public static void play(Path f, int auto, boolean forceExit) {
		Config config = new Config();
		if (Files.exists(configpath)) {
			Json json = new Json();
			try {
				config = json.fromJson(Config.class, Files.newBufferedReader(configpath));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			Json json = new Json();
			json.setOutputType(OutputType.json);
			try {
				BufferedWriter fw = Files.newBufferedWriter(configpath);
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
			cfg.width = (int) RESOLUTION[config.getResolution()].width;
			cfg.height = (int) RESOLUTION[config.getResolution()].height;

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
//			System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "true");
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
			BufferedWriter fw = Files.newBufferedWriter(configpath);
			fw.write(json.prettyPrint(config));
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		dispose();
		Gdx.app.exit();
	}

	public BMSPlayerInputProcessor getInputProcessor() {
		return input;
	}

	public static class BMSInformationLoader extends Application {

		private PlayConfigurationView bmsinfo;

		private VBox stackPane;

		public static void main(String[] args) {
			launch(args);
		}

		@Override
		public void start(javafx.stage.Stage primaryStage) throws Exception {
			Config config = new Config();
			if (Files.exists(configpath)) {
				Json json = new Json();
				try {
					config = json.fromJson(Config.class, Files.newBufferedReader(configpath));
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				Json json = new Json();
				json.setOutputType(OutputType.json);
				try {
					BufferedWriter fw = Files.newBufferedWriter(configpath);
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
				stackPane = (VBox) loader.load();
				bmsinfo = (PlayConfigurationView) loader.getController();
				bmsinfo.setBMSInformationLoader(this);
				bmsinfo.update(config);
				Scene scene = new Scene(stackPane, stackPane.getPrefWidth(), stackPane.getPrefHeight());
				primaryStage.setScene(scene);
				primaryStage.setTitle("beatoraja configuration");
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
	
	class ScreenShotThread extends Thread {

		private boolean isCompleted;
		private String path = "";
		private long savetime;
		private byte[] pixels;
		@Override
		public void run() {
			Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(),
					Pixmap.Format.RGBA8888);
			BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			path = "screenshot/" + sdf.format(Calendar.getInstance().getTime()) + ".png";
			PixmapIO.writePNG(new FileHandle(path), pixmap);
			pixmap.dispose();
			input.getFunctiontime()[5] = 0;
			Logger.getGlobal().info("スクリーンショット保存:" + path);
			isCompleted = true;
		}
		
	}
}
