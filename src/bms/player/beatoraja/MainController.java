package bms.player.beatoraja;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.lwjgl.input.Mouse;

import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.select.bar.TableBar;
import bms.player.beatoraja.skin.SkinLoader;
import bms.player.beatoraja.skin.SkinProperty;
import com.badlogic.gdx.Graphics;

import bms.player.beatoraja.skin.SkinObject.SkinOffset;
import bms.player.beatoraja.audio.*;
import bms.player.beatoraja.config.KeyConfiguration;
import bms.player.beatoraja.decide.MusicDecide;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.ir.IRResponse;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.result.CourseResult;
import bms.player.beatoraja.result.MusicResult;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.SQLiteSongDatabaseAccessor;
import bms.player.beatoraja.song.SongDatabaseAccessor;
import bms.player.beatoraja.song.SongInformationAccessor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.badlogic.gdx.utils.StringBuilder;

/**
 * アプリケーションのルートクラス
 * 
 * @author exch
 */
public class MainController extends ApplicationAdapter {

	public static final String VERSION = "beatoraja 0.5.2";
	
	private static final boolean debug = false;

	/**
	 *
	 */
	private final long boottime = System.currentTimeMillis();
	private final Calendar cl = Calendar.getInstance();

	private BMSPlayer bmsplayer;
	private MusicDecide decide;
	private MusicSelector selector;
	private MusicResult result;
	private CourseResult gresult;
	private KeyConfiguration keyconfig;

	private AudioDriver audio;
	
	private PlayerResource resource;

	private BitmapFont systemfont;

	private MainState current;

	private Config config;
	private PlayerConfig player;
	private int auto;
	private boolean songUpdated;

	private SongDatabaseAccessor songdb;
	private SongInformationAccessor infodb;

	private IRConnection ir;
	
	private SpriteBatch sprite;
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

	static final Path configpath = Paths.get("config.json");
	private static final Path songdbpath = Paths.get("songdata.db");
	private static final Path infodbpath = Paths.get("songinfo.db");

	private SystemSoundManager sound;

	private ScreenShotThread screenshot;

	public static final int timerCount = SkinProperty.TIMER_MAX + 1;
	private final long[] timer = new long[timerCount];
	public static final int offsetCount = SkinProperty.OFFSET_MAX + 1;
	private final SkinOffset[] offset = new SkinOffset[offsetCount];

	public MainController(Path f, Config config, int auto, boolean songUpdated) {
		this.auto = auto;
		this.config = config;
		this.songUpdated = songUpdated;
		
		for(int i = 0;i < offset.length;i++) {
			offset[i] = new SkinOffset();
		}
		
		player = PlayerConfig.readPlayerConfig(config.getPlayername());
		this.bmsfile = f;

		try {
			Class.forName("org.sqlite.JDBC");
			songdb = new SQLiteSongDatabaseAccessor(songdbpath.toString(), config.getBmsroot());
			if(config.isUseSongInfo()) {
				infodb = new SongInformationAccessor(infodbpath.toString());				
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		playdata = new PlayDataAccessor(config.getPlayername());
		
		ir = IRConnection.getIRConnection(player.getIrname());
		if(player.getUserid().length() > 0 && ir != null) {
			IRResponse response = ir.login(player.getUserid(), player.getPassword());
			if(!response.isSuccessed()) {
				Logger.getGlobal().warning("IRへのログイン失敗 : " + response.getMessage());
				ir = null;
			}
		}
		
		switch(config.getAudioDriver()) {
		case Config.AUDIODRIVER_ASIO:
			try {
				audio = new ASIODriver(config);
			} catch(Throwable e) {
				e.printStackTrace();
				config.setAudioDriver(Config.AUDIODRIVER_SOUND);
			}
			break;
		case Config.AUDIODRIVER_PORTAUDIO:
			try {
				audio = new PortAudioDriver(config);
			} catch(Throwable e) {
				e.printStackTrace();
				config.setAudioDriver(Config.AUDIODRIVER_SOUND);
			}
			break;
		}

		sound = new SystemSoundManager(config);
	}

	public long[] getTimer() {
		return timer;
	}

	public SkinOffset getOffset(int index) {
		return offset[index];
	}

	public SongDatabaseAccessor getSongDatabase() {
		return songdb;
	}

	public SongInformationAccessor getInfoDatabase() {
		return infodb;
	}

	public PlayDataAccessor getPlayDataAccessor() {
		return playdata;
	}

	public SpriteBatch getSpriteBatch() {
		return sprite;
	}

	public PlayerResource getPlayerResource() {
		return resource;
	}

	public Config getConfig() {
		return config;
	}

	public PlayerConfig getPlayerConfig() {
		return player;
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
			if (bmsplayer != null) {
				bmsplayer.dispose();
			}
			bmsplayer = new BMSPlayer(this, resource);
			newState = bmsplayer;
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
		
		if(state == STATE_PLAYBMS) {
		    Mouse.setGrabbed(true);
		} else {
		    Mouse.setGrabbed(false);
		}

		if (newState != null && current != newState) {
			Arrays.fill(timer, Long.MIN_VALUE);
			if(current != null) {
				current.setSkin(null);
			}
			newState.create();
			newState.getSkin().prepare(newState);
			current = newState;
			current.setStartTime();
		}
		if (current.getStage() != null) {
			Gdx.input.setInputProcessor(new InputMultiplexer(current.getStage(), input.getKeyBoardInputProcesseor()));
		} else {
			Gdx.input.setInputProcessor(input.getKeyBoardInputProcesseor());
		}
	}

	public void setAuto(int auto) {
		this.auto = auto;

	}

	@Override
	public void create() {
		final long t = System.currentTimeMillis();
		sprite = new SpriteBatch();

		input = new BMSPlayerInputProcessor(config, player);
		switch(config.getAudioDriver()) {
		case Config.AUDIODRIVER_SOUND:
			audio = new GdxSoundDriver();
			break;
		case Config.AUDIODRIVER_AUDIODEVICE:
			audio = new GdxAudioDeviceDriver();
			break;
		}

		resource = new PlayerResource(audio, config, player);
		selector = new MusicSelector(this, songUpdated);
		decide = new MusicDecide(this);
		result = new MusicResult(this);
		gresult = new CourseResult(this);
		keyconfig = new KeyConfiguration(this);
		if (bmsfile != null) {
			if(resource.setBMSFile(bmsfile, auto)) {
				changeState(STATE_PLAYBMS);				
			} else {
				// ダミーステートに移行してすぐexitする
				changeState(STATE_CONFIG);
				exit();
			}
		} else {
			changeState(STATE_SELECTMUSIC);
		}

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
		systemfont = generator.generateFont(parameter);
		generator.dispose();
		Logger.getGlobal().info("初期化時間(ms) : " + (System.currentTimeMillis() - t));
		
		Thread polling = new Thread() {
			public void run() {
				long time = 0;
				for (;;) {
					final long now = System.nanoTime() / 1000000;
					if (time != now) {
						time = now;
						input.poll();
					} else {
						try {
							sleep(0, 500000);
						} catch (InterruptedException e) {
						}
					}
				}
			}			
		};
		polling.start();

		if(player.getTarget() >= TargetProperty.getAllTargetProperties().length) {
			player.setTarget(0);
		}
		Gdx.gl.glClearColor(0, 0, 0, 1);
	}
	
	private long prevtime;

	private final StringBuilder message = new StringBuilder();
	
	@Override
	public void render() {
//		input.poll();
		current.updateNowTime();

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		current.render();
		sprite.begin();
		current.getSkin().drawAllObjects(sprite, current);
		sprite.end();

		final Stage stage = current.getStage();
		if (stage != null) {
			stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
			stage.draw();
		}

		// show fps
		if (showfps) {
			sprite.begin();
			systemfont.setColor(Color.PURPLE);
			message.setLength(0);
			systemfont.draw(sprite, message.append("FPS ").append(Gdx.graphics.getFramesPerSecond()), 10,
					config.getResolution().height - 2);
			if(debug) {
				message.setLength(0);
				systemfont.draw(sprite, message.append("Skin Pixmap Images ").append(SkinLoader.getResource().size()), 10,
						config.getResolution().height - 26);
				message.setLength(0);
				systemfont.draw(sprite, message.append("Total Memory Used(MB) ").append(Runtime.getRuntime().totalMemory() / (1024 * 1024)), 10,
						config.getResolution().height - 50);
				message.setLength(0);
				systemfont.draw(sprite, message.append("Total Free Memory(MB) ").append(Runtime.getRuntime().freeMemory() / (1024 * 1024)), 10,
						config.getResolution().height - 74);
				message.setLength(0);
				systemfont.draw(sprite, message.append("Max Sprite In Batch ").append(sprite.maxSpritesInBatch), 10,
						config.getResolution().height - 98);
			}
			
			sprite.end();
		}
		// show screenshot status
		if (screenshot != null && screenshot.savetime + 2000 > System.currentTimeMillis()) {
			sprite.begin();
			systemfont.setColor(Color.GOLD);
			systemfont.draw(sprite, "Screen shot saved : " + screenshot.path, 100,
					config.getResolution().height - 2);
			sprite.end();
		} else if(updateSong != null && updateSong.isAlive()) {
			sprite.begin();
			systemfont.setColor(0,1,1,0.5f + (System.currentTimeMillis() % 500) / 1000.0f);
			systemfont.draw(sprite, updateSong.message, 100, config.getResolution().height - 2);
			sprite.end();
		}

		final long time = System.currentTimeMillis();
		if(time > prevtime) {
		    prevtime = time;
            current.input();
            // move song bar position by mouse
            if (input.isMousePressed()) {
                input.setMousePressed();
                current.getSkin().mousePressed(current, input.getMouseButton(), input.getMouseX(), input.getMouseY());
            }
            if (input.isMouseDragged()) {
                input.setMouseDragged();
                current.getSkin().mouseDragged(current, input.getMouseButton(), input.getMouseX(), input.getMouseY());
            }

            // FPS表示切替
            if (input.getFunctionstate()[0] && input.getFunctiontime()[0] != 0) {
                showfps = !showfps;
                input.getFunctiontime()[0] = 0;
            }
            // fullscrees - windowed
            if (input.getFunctionstate()[3] && input.getFunctiontime()[3] != 0) {
                boolean fullscreen = Gdx.graphics.isFullscreen();
                Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
                if (fullscreen) {
                    Gdx.graphics.setWindowedMode(currentMode.width, currentMode.height);
                } else {
                    Gdx.graphics.setFullscreenMode(currentMode);
                }
                config.setFullscreen(!fullscreen);
                input.getFunctiontime()[3] = 0;
            }

            // if (input.getFunctionstate()[4] && input.getFunctiontime()[4] != 0) {
            // int resolution = config.getResolution();
            // resolution = (resolution + 1) % RESOLUTION.length;
            // if (config.isFullscreen()) {
            // Gdx.graphics.setWindowedMode((int) RESOLUTION[resolution].width,
            // (int) RESOLUTION[resolution].height);
            // Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
            // Gdx.graphics.setFullscreenMode(currentMode);
            // }
            // else {
            // Gdx.graphics.setWindowedMode((int) RESOLUTION[resolution].width,
            // (int) RESOLUTION[resolution].height);
            // }
            // config.setResolution(resolution);
            // input.getFunctiontime()[4] = 0;
            // }

            // screen shot
            if (input.getFunctionstate()[5] && input.getFunctiontime()[5] != 0) {
                if (screenshot == null || screenshot.savetime != 0) {
                    screenshot = new ScreenShotThread(ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(),
                            Gdx.graphics.getBackBufferHeight(), true));
                    screenshot.start();
                }
                input.getFunctiontime()[5] = 0;
            }
        }
	}

	@Override
	public void dispose() {
		if (bmsplayer != null) {
			bmsplayer.dispose();
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
		if (keyconfig != null) {
			keyconfig.dispose();
		}
		resource.dispose();
//		input.dispose();
		SkinLoader.getResource().dispose();
		ShaderManager.dispose();
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

	public void saveConfig(){
		Json json = new Json();
		json.setOutputType(OutputType.json);
		try (FileWriter fw = new FileWriter(configpath.toFile())) {
			fw.write(json.prettyPrint(config));
			fw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Logger.getGlobal().info("設定情報をconfig.jsonに保存");

		Path p = Paths.get("player/" + config.getPlayername() + "/config.json");
		try (FileWriter fw = new FileWriter(p.toFile())) {
			fw.write(json.prettyPrint(player));
			fw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Logger.getGlobal().info("設定情報を" + p.toString() + "に保存");
	}

	public void exit() {
		saveConfig();

		dispose();
		Gdx.app.exit();
	}

	public BMSPlayerInputProcessor getInputProcessor() {
		return input;
	}

	public AudioDriver getAudioProcessor() {
		return audio;
	}

	public IRConnection getIRConnection() {
		return ir;
	}

	public SystemSoundManager getSoundManager() {
		return sound;
	}

	public long getPlayTime() {
		return System.currentTimeMillis() - boottime;
	}

	public Calendar getCurrnetTime() {
		cl.setTimeInMillis(System.currentTimeMillis());
		return cl;
	}
	/**
	 * スクリーンショット処理用スレッド
	 * 
	 * @author exch
	 */
	static class ScreenShotThread extends Thread {

		/**
		 * 処理が完了した時間
		 */
		private long savetime;
		/**
		 * スクリーンショット保存先
		 */
		private final String path;
		/**
		 * スクリーンショットのpixelデータ
		 */
		private final byte[] pixels;
		
		public ScreenShotThread(byte[] pixels) {
			this.pixels = pixels;
			final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			path = "screenshot/" + sdf.format(Calendar.getInstance().getTime()) + ".png";
		}

		@Override
		public void run() {
			// 全ピクセルのアルファ値を255にする(=透明色を無くす)
			for(int i = 3;i < pixels.length;i+=4) {
				pixels[i] = (byte) 0xff;
			}
			Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(),
					Pixmap.Format.RGBA8888);
			BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
			PixmapIO.writePNG(new FileHandle(path), pixmap);
			pixmap.dispose();
			Logger.getGlobal().info("スクリーンショット保存:" + path);
			savetime = System.currentTimeMillis();
		}
	}

	private UpdateThread updateSong;

	public void updateSong(String path) {
		if (updateSong == null || !updateSong.isAlive()) {
			updateSong = new SongUpdateThread(path);
			updateSong.start();
		} else {
			Logger.getGlobal().warning("楽曲更新中のため、更新要求は取り消されました");
		}
	}

	public void updateTable(TableBar reader) {
		if (updateSong == null || !updateSong.isAlive()) {
			updateSong = new TableUpdateThread(reader);
			updateSong.start();
		} else {
			Logger.getGlobal().warning("楽曲更新中のため、更新要求は取り消されました");
		}
	}

	abstract class UpdateThread extends Thread {

		private String message;

		public UpdateThread(String message) {
			this.message = message;
		}
	}

	/**
	 * 楽曲データベース更新用スレッド
	 *
	 * @author exch
	 */
	class SongUpdateThread extends UpdateThread {

		private final String path;

		public SongUpdateThread(String path) {
			super("updating folder : " + (path == null ? "ALL" : path));
			this.path = path;
		}

		public void run() {
			getSongDatabase().updateSongDatas(path, false, getInfoDatabase());
		}
	}

	/**
	 * 難易度表更新用スレッド
	 *
	 * @author exch
	 */
	class TableUpdateThread extends UpdateThread {

		private final TableBar accessor;

		public TableUpdateThread(TableBar bar) {
			super("updating table : " + bar.getReader().name);
			accessor = bar;
		}

		public void run() {
			TableData td = accessor.getReader().read();
			if (td != null) {
				new TableDataAccessor().write(td);
				accessor.setTableData(td);
			}
		}
	}

	public static class SystemSoundManager {
		private Array<Path> bgms = new Array();
		private Path currentBGMPath;
		private Array<Path> sounds = new Array();
		private Path currentSoundPath;

		public SystemSoundManager(Config config) {
			scan(Paths.get(config.getBgmpath()), bgms, "select.");
			scan(Paths.get(config.getSoundpath()), sounds, "clear.");
			Logger.getGlobal().info("検出されたBGM Set : " + bgms.size + " Sound Set : " + sounds.size);
		}

		public void shuffle() {
			if(bgms.size > 0) {
				currentBGMPath = bgms.get((int) (Math.random() * bgms.size));
			}
			if(sounds.size > 0) {
				currentSoundPath = sounds.get((int) (Math.random() * sounds.size));
			}
			Logger.getGlobal().info("BGM Set : " + currentBGMPath + " Sound Set : " + currentSoundPath);
		}

		public Path getBGMPath() {
			return currentBGMPath;
		}

		public Path getSoundPath() {
			return currentSoundPath;
		}

		private void scan(Path p, Array<Path> paths, String name) {
			if (Files.isDirectory(p)) {
				try (Stream<Path> sub = Files.list(p)) {
					sub.forEach(new Consumer<Path>() {
						@Override
						public void accept(Path t) {
							scan(t, paths, name);
						}
					});
				} catch (IOException e) {
				}
			} else if (p.getFileName().toString().toLowerCase().equals(name + "wav") ||
					p.getFileName().toString().toLowerCase().equals(name + "ogg")) {
				paths.add(p.getParent());
			}

		}
	}
}
