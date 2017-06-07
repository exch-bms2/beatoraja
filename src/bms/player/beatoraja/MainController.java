package bms.player.beatoraja;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.logging.Logger;

import bms.player.beatoraja.play.TargetProperty;
import com.badlogic.gdx.Graphics;

import bms.player.beatoraja.audio.*;
import bms.player.beatoraja.config.KeyConfiguration;
import bms.player.beatoraja.decide.MusicDecide;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.result.GradeResult;
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

/**
 * アプリケーションのルートクラス
 * 
 * @author exch
 */
public class MainController extends ApplicationAdapter {

	public static final String VERSION = "beatoraja 0.4.1";

	private BMSPlayer bmsplayer;
	private MusicDecide decide;
	private MusicSelector selector;
	private MusicResult result;
	private GradeResult gresult;
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

	private ScreenShotThread screenshot;

	private long[] timer = new long[256];

	public MainController(Path f, Config config, int auto, boolean songUpdated) {
		this.auto = auto;
		this.config = config;
		this.songUpdated = songUpdated;
		this.player = config.getPlayers()[config.getPlayer()];
		this.bmsfile = f;

		try {
			Class.forName("org.sqlite.JDBC");
			songdb = new SQLiteSongDatabaseAccessor(songdbpath.toString(), config.getBmsroot());
			infodb = new SongInformationAccessor(infodbpath.toString());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		playdata = new PlayDataAccessor(player.getName());
		
		ir = IRConnection.getIRConnection(config.getIrname());
		if(config.getUserid().length() > 0 && ir != null) {
			ir.login(config.getUserid(), config.getPassword());
		}
	}

	public long[] getTimer() {
		return timer;
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

		if (newState != null && current != newState) {
			Arrays.fill(timer, Long.MIN_VALUE);
			if(current != null) {
				current.setSkin(null);
			}
			newState.create();
			newState.getSkin().prepare(newState);
			current = newState;
			current.setStartTime(System.nanoTime() / 1000000);
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

		input = new BMSPlayerInputProcessor(config.getResolution());
		switch(config.getAudioDriver()) {
		case Config.AUDIODRIVER_SOUND:
			audio = new GdxSoundDriver();
			break;
		case Config.AUDIODRIVER_AUDIODEVICE:
			audio = new GdxAudioDeviceDriver();
			break;
		case Config.AUDIODRIVER_ASIO:
			try {
				audio = new ASIODriver(config);
			} catch(Throwable e) {
				e.printStackTrace();
				config.setAudioDriver(Config.AUDIODRIVER_SOUND);
				audio = new GdxSoundDriver();
			}
			break;
		}

		selector = new MusicSelector(this, config, songUpdated);
		decide = new MusicDecide(this);
		result = new MusicResult(this);
		gresult = new GradeResult(this);
		keyconfig = new KeyConfiguration(this);
		resource = new PlayerResource(audio, config);
		if (bmsfile != null) {
			if(resource.setBMSFile(bmsfile, config, auto)) {
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

		if(config.getTarget() >= TargetProperty.getAllTargetProperties(this).length) {
			config.setTarget(0);
		}
		Gdx.gl.glClearColor(0, 0, 0, 1);
	}
	
	private long prevtime;

	@Override
	public void render() {
//		input.poll();

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
			systemfont.draw(sprite, String.format("FPS %d", Gdx.graphics.getFramesPerSecond()), 10,
					config.getResolution().height - 2);
			sprite.end();
		}
		// show screenshot status
		if (screenshot != null && screenshot.savetime + 2000 > System.currentTimeMillis()) {
			sprite.begin();
			systemfont.setColor(Color.GOLD);
			systemfont.draw(sprite, "Screen shot saved : " + screenshot.path, 100,
					config.getResolution().height - 2);
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

	public void exit() {
		Json json = new Json();
		json.setOutputType(OutputType.json);
		try (FileWriter fw = new FileWriter(configpath.toFile())) {
			fw.write(json.prettyPrint(config));
			fw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
}
