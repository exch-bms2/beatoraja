package bms.player.beatoraja;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

import bms.player.beatoraja.config.SkinConfiguration;
import org.lwjgl.input.Mouse;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.StringBuilder;

import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.audio.AudioDriver;
import bms.player.beatoraja.audio.GdxAudioDeviceDriver;
import bms.player.beatoraja.audio.GdxSoundDriver;
import bms.player.beatoraja.audio.PortAudioDriver;
import bms.player.beatoraja.config.KeyConfiguration;
import bms.player.beatoraja.decide.MusicDecide;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.ir.IRResponse;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.result.CourseResult;
import bms.player.beatoraja.result.MusicResult;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.select.bar.TableBar;
import bms.player.beatoraja.skin.SkinLoader;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;
import bms.player.beatoraja.skin.SkinProperty;
import bms.player.beatoraja.song.SQLiteSongDatabaseAccessor;
import bms.player.beatoraja.song.SongDatabaseAccessor;
import bms.player.beatoraja.song.SongInformationAccessor;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.UploadedMedia;
import twitter4j.conf.ConfigurationBuilder;

/**
 * 占쎄텑占쎄묏占쎄틒占쎄텪占쎄틬占쎄땅占쎄뭬占쎄틡占쎄쿁占쎄틓占쎄틬占쎄퉱占쎄텥占쎄�占쎄때
 *
 * @author exch
 */
public class MainController extends ApplicationAdapter {

	public static final String VERSION = "beatoraja 0.5.5";
	
	private static final boolean debug = true;

	/**
	 *
	 */
	private final long boottime = System.currentTimeMillis();
	private final Calendar cl = Calendar.getInstance();
	private long mouseMovedTime;

	private BMSPlayer bmsplayer;
	private MusicDecide decide;
	private MusicSelector selector;
	private MusicResult result;
	private CourseResult gresult;
	private KeyConfiguration keyconfig;
	private SkinConfiguration skinconfig;

	private AudioDriver audio;

	private PlayerResource resource;

	private FreeTypeFontGenerator generator;
	private BitmapFont systemfont;
	private BitmapFont updatefont;

	private MainState current;
	private static MainState currentState;
	/**
	 * 占쎈뱚占쎈�묕옙寃쀯옙堉��넼�뿦�끋占쎈펻
	 */
	private long starttime;
	private long nowmicrotime;

	private Config config;
	private PlayerConfig player;
	private PlayMode auto;
	private boolean songUpdated;

	private SongDatabaseAccessor songdb;
	private SongInformationAccessor infodb;

	private IRConnection ir;

	private SpriteBatch sprite;
	/**
	 * 1占쎌럧占쎄묏占쎄틕占쎄텕占쎄쾸占쎈셼畑댁떏嫄�占쎄굴BMS占쎄묄占쎄텏占쎄텕占쎄틓
	 */
	private Path bmsfile;

	private BMSPlayerInputProcessor input;
	/**
	 * FPS占쎄뎌占쎈짂占쎈돕占쎄굉占쎄데占쎄괍占쎄쾻占쎄콨占쎄괍
	 */
	private boolean showfps;
	/**
	 * 占쎄묏占쎄틕占쎄텕占쎄퉰占쎄틬占쎄땟占쎄텑占쎄텥占쎄땐占쎄땁
	 */
	private PlayDataAccessor playdata;

	static final Path configpath = Paths.get("config.json");
	private static final Path songdbpath = Paths.get("songdata.db");
	private static final Path infodbpath = Paths.get("songinfo.db");

	private SystemSoundManager sound;

	private ScreenShotThread screenshot;

	private TwitterUploadThread twitterUpload;

	public static final int timerCount = SkinProperty.TIMER_MAX + 1;
	private final long[] timer = new long[timerCount];
	public static final int offsetCount = SkinProperty.OFFSET_MAX + 1;
	private final SkinOffset[] offset = new SkinOffset[offsetCount];

	protected TextureRegion black;
	protected TextureRegion white;

	public MainController(Path f, Config config, PlayerConfig player, PlayMode auto, boolean songUpdated) {
		this.auto = auto;
		this.config = config;
		this.songUpdated = songUpdated;

		for(int i = 0;i < offset.length;i++) {
			offset[i] = new SkinOffset();
		}

		if(player == null) {
			player = PlayerConfig.readPlayerConfig(config.getPlayername());
		}
		this.player = player;

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
		if(ir != null) {
			if(player.getUserid().length() == 0 || player.getPassword().length() == 0) {
				ir = null;
			} else {
				IRResponse response = ir.login(player.getUserid(), player.getPassword());
				if(!response.isSuccessed()) {
					Logger.getGlobal().warning("IR占쎄꺅占쎄쿁占쎄틙占쎄텦占쎄텕占쎄틡勇싰만釉� : " + response.getMessage());
					ir = null;
				}
			}
		}

		switch(config.getAudioDriver()) {
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
	public static final int STATE_SKIN_SELECT = 6;

	public void changeState(int state) {
		MainState newState = null;
		switch (state) {
		case STATE_SELECTMUSIC:
			if (this.bmsfile != null) {
				exit();
			} else {
				newState = selector;
			}
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
		case STATE_SKIN_SELECT:
			newState = skinconfig;
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
			currentState = newState;
			starttime = System.nanoTime();
		}
		if (current.getStage() != null) {
			Gdx.input.setInputProcessor(new InputMultiplexer(current.getStage(), input.getKeyBoardInputProcesseor()));
		} else {
			Gdx.input.setInputProcessor(input.getKeyBoardInputProcesseor());
		}
	}

	public void setPlayMode(PlayMode auto) {
		this.auto = auto;

	}

	@Override
	public void create() {
		final long t = System.currentTimeMillis();
		sprite = new SpriteBatch();
		SkinLoader.initPixmapResourcePool(config.getSkinPixmapGen());

		generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
		systemfont = generator.generateFont(parameter);

		input = new BMSPlayerInputProcessor(config, player);
		switch(config.getAudioDriver()) {
		case Config.AUDIODRIVER_SOUND:
			audio = new GdxSoundDriver(config);
			break;
		case Config.AUDIODRIVER_AUDIODEVICE:
			audio = new GdxAudioDeviceDriver(config);
			break;
		}

		resource = new PlayerResource(audio, config, player);
		selector = new MusicSelector(this, songUpdated);
		decide = new MusicDecide(this);
		result = new MusicResult(this);
		gresult = new CourseResult(this);
		keyconfig = new KeyConfiguration(this);
		skinconfig = new SkinConfiguration(this);
		if (bmsfile != null) {
			if(resource.setBMSFile(bmsfile, auto)) {
				changeState(STATE_PLAYBMS);
			} else {
				// 占쏙옙占쎄묽占쎄틬占쎄때占쎄퉯占쎄틬占쎄퉱占쎄쾽�뇖�궘二깍옙嫄�占쎄쾷占쎄굉占쎄괠exit占쎄굉占쎄데
				changeState(STATE_CONFIG);
				exit();
			}
		} else {
			changeState(STATE_SELECTMUSIC);
		}

		Logger.getGlobal().info("占쎈떆占쎌맾占쎈솑占쎌끋占쎈펻(ms) : " + (System.currentTimeMillis() - t));

		Thread polling = new Thread(() -> {
			long time = 0;
			for (;;) {
				final long now = System.nanoTime() / 1000000;
				if (time != now) {
					time = now;
					input.poll();
				} else {
					try {
						Thread.sleep(0, 500000);
					} catch (InterruptedException e) {
					}
				}
			}
		});
		polling.start();

		if(player.getTarget() >= TargetProperty.getAllTargetProperties().length) {
			player.setTarget(0);
		}

		Pixmap plainPixmap = new Pixmap(2,1, Pixmap.Format.RGBA8888);
		plainPixmap.drawPixel(0,0, Color.toIntBits(255,0,0,0));
		plainPixmap.drawPixel(1,0, Color.toIntBits(255,255,255,255));
		Texture plainTexture = new Texture(plainPixmap);
		black = new TextureRegion(plainTexture,0,0,1,1);
		white = new TextureRegion(plainTexture,1,0,1,1);
		plainPixmap.dispose();

		Gdx.gl.glClearColor(0, 0, 0, 1);
	}

	private long prevtime;

	private final StringBuilder message = new StringBuilder();

	@Override
	public void render() {
//		input.poll();
		nowmicrotime = ((System.nanoTime() - starttime) / 1000);

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		current.render();
		sprite.begin();
		if (current.getSkin() != null) {
			current.getSkin().drawAllObjects(sprite, current);
		}
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
		} else if (twitterUpload != null && twitterUpload.savetime + 2000 > System.currentTimeMillis()) {
			sprite.begin();
			systemfont.setColor(Color.GOLD);
			systemfont.draw(sprite, "Twitter Upload : " + twitterUpload.text, 100,
					config.getResolution().height - 2);
			sprite.end();
		} else if(updateSong != null && updateSong.isAlive()) {
			if(currentState instanceof MusicSelector) {
				sprite.begin();
				updatefont.setColor(0,1,1,0.5f + (System.currentTimeMillis() % 750) / 1000.0f);
				updatefont.draw(sprite, updateSong.message, 100, config.getResolution().height - 2);
				sprite.end();
			}
		}

		final long time = System.currentTimeMillis();
		if(time > prevtime) {
		    prevtime = time;
            //current.input();
            // event - move pressed
            if (input.isMousePressed()) {
                input.setMousePressed();
                current.getSkin().mousePressed(current, input.getMouseButton(), input.getMouseX(), input.getMouseY());
            }
            // event - move dragged
            if (input.isMouseDragged()) {
                input.setMouseDragged();
                current.getSkin().mouseDragged(current, input.getMouseButton(), input.getMouseX(), input.getMouseY());
            }

            // 占쎄묻占쎄텘占쎄때占쎄텠占쎄틬占쎄땜占쎄틓�깗�몺�뀏占쎈떓畑댐옙
            if(input.isMouseMoved()) {
            	input.setMouseMoved(false);
            	mouseMovedTime = time;
			}
			Mouse.setGrabbed(current == bmsplayer && time > mouseMovedTime + 5000 && Mouse.isInsideWindow());

//			// FPS�깗�몺�뀏占쎈듋占쎌럹
//            if (input.checkIfFunctionPressed(0)) {
//                showfps = !showfps;
//                input.resetFunctionTime(0);
//            }
            // fullscrees - windowed
//            if (input.checkIfFunctionPressed(3)) {
//                boolean fullscreen = Gdx.graphics.isFullscreen();
//                Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
//                if (fullscreen) {
//                    Gdx.graphics.setWindowedMode(currentMode.width, currentMode.height);
//                } else {
//                    Gdx.graphics.setFullscreenMode(currentMode);
//                }
//                config.setDisplaymode(fullscreen ? Config.DisplayMode.WINDOW : Config.DisplayMode.FULLSCREEN);
//                input.resetFunctionTime(3);
//            }

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
//            if (input.checkIfFunctionPressed(5)) {
//                if (screenshot == null || screenshot.savetime != 0) {
//                    screenshot = new ScreenShotThread(ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(),
//                            Gdx.graphics.getBackBufferHeight(), true));
//                    screenshot.start();
//                }
//                input.resetFunctionTime(5);
//            }
//
//            if (input.checkIfFunctionPressed(6)) {
//                if (twitterUpload == null || twitterUpload.savetime != 0) {
//                	twitterUpload = new TwitterUploadThread(ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(),
//                            Gdx.graphics.getBackBufferHeight(), false), player);
//                	twitterUpload.start();
//                }
//                input.resetFunctionTime(6);
//            }
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
		if (skinconfig != null) {
			skinconfig.dispose();
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
		Config.write(config);
		PlayerConfig.write(player);
		Logger.getGlobal().info("庸뉛옙畑댁떒源꾬옙�졒占쎄뎌略녹빆異�");
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

	public long getStartTime() {
		return starttime / 1000000;
	}

	public long getStartMicroTime() {
		return starttime / 1000;
	}

	public long getNowTime() {
		return nowmicrotime / 1000;
	}

	public long getNowTime(int id) {
		if(isTimerOn(id)) {
			return (nowmicrotime - timer[id]) / 1000;
		}
		return 0;
	}

	public long getNowMicroTime() {
		return nowmicrotime;
	}

	public long getNowMicroTime(int id) {
		if(isTimerOn(id)) {
			return nowmicrotime - timer[id];
		}
		return 0;
	}

	public long getTimer(int id) {
		return timer[id] / 1000;
	}

	public long getMicroTimer(int id) {
		return timer[id];
	}

	public boolean isTimerOn(int id) {
		return timer[id] != Long.MIN_VALUE;
	}

	public void setTimerOn(int id) {
		timer[id] = nowmicrotime;
	}

	public void setTimerOff(int id) {
		timer[id] = Long.MIN_VALUE;
	}

	public void setMicroTimer(int id, long microtime) {
		timer[id] = microtime;
	}

	public void switchTimer(int id, boolean on) {
		if(on) {
			if(timer[id] == Long.MIN_VALUE) {
				timer[id] = nowmicrotime;
			}
		} else {
			timer[id] = Long.MIN_VALUE;
		}
	}

	public static String getClearTypeName() {
		String[] clearTypeName = { "NO PLAY", "FAILED", "ASSIST EASY CLEAR", "LIGHT ASSIST EASY CLEAR", "EASY CLEAR",
				"CLEAR", "HARD CLEAR", "EXHARD CLEAR", "FULL COMBO", "PERFECT", "MAX" };

		if(currentState.getNumberValue(NUMBER_CLEAR) >= 0 && currentState.getNumberValue(NUMBER_CLEAR) < clearTypeName.length) {
			return clearTypeName[currentState.getNumberValue(NUMBER_CLEAR)];
		}

		return "";
	}

	public static String getRankTypeName() {
		String rankTypeName = "";
		if(currentState.getBooleanValue(OPTION_RESULT_AAA_1P)) rankTypeName += "AAA";
		else if(currentState.getBooleanValue(OPTION_RESULT_AA_1P)) rankTypeName += "AA";
		else if(currentState.getBooleanValue(OPTION_RESULT_A_1P)) rankTypeName += "A";
		else if(currentState.getBooleanValue(OPTION_RESULT_B_1P)) rankTypeName += "B";
		else if(currentState.getBooleanValue(OPTION_RESULT_C_1P)) rankTypeName += "C";
		else if(currentState.getBooleanValue(OPTION_RESULT_D_1P)) rankTypeName += "D";
		else if(currentState.getBooleanValue(OPTION_RESULT_E_1P)) rankTypeName += "E";
		else if(currentState.getBooleanValue(OPTION_RESULT_F_1P)) rankTypeName += "F";
		return rankTypeName;
	}


	/**
	 * 占쎄때占쎄텥占쎄틒占쎄틬占쎄틡占쎄땅占쎄뭬占쎄맙占쎄퉱占쎈늾占쎈┐占쎈뎁占쎄때占쎄틕占쎄맙占쎄퉳
	 *
	 * @author exch
	 */
	static class ScreenShotThread extends Thread {

		/**
		 * 占쎈늾占쎈┐占쎄괏畑대슖�떃占쎄괼占쎄굴占쎌끋占쎈펻
		 */
		private long savetime;
		/**
		 * 占쎄때占쎄텥占쎄틒占쎄틬占쎄틡占쎄땅占쎄뭬占쎄맙占쎄퉱略녹빆異쇽옙��
		 */
		private final String path;
		/**
		 * 占쎄때占쎄텥占쎄틒占쎄틬占쎄틡占쎄땅占쎄뭬占쎄맙占쎄퉱占쎄쿁pixel占쎄퉰占쎄틬占쎄땟
		 */
		private final byte[] pixels;

		public ScreenShotThread(byte[] pixels) {
			this.pixels = pixels;
			final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			String stateName = "";
			if(currentState instanceof MusicSelector) {
				stateName = "_Music_Select";
			} else if(currentState instanceof MusicDecide) {
				stateName = "_Decide";
			} if(currentState instanceof BMSPlayer) {
				if(currentState.getTextValue(STRING_TABLE_LEVEL).length() > 0){
					stateName = "_Play_" + currentState.getTextValue(STRING_TABLE_LEVEL);
				}else{
					stateName = "_Play_LEVEL" + currentState.getNumberValue(NUMBER_PLAYLEVEL);
				}
				if(currentState.getTextValue(STRING_FULLTITLE).length() > 0) stateName += " " + currentState.getTextValue(STRING_FULLTITLE);
			} else if(currentState instanceof MusicResult || currentState instanceof CourseResult) {
				if(currentState instanceof MusicResult){
					if(currentState.getTextValue(STRING_TABLE_LEVEL).length() > 0){
						stateName += "_" + currentState.getTextValue(STRING_TABLE_LEVEL);
					}else{
						stateName += "_LEVEL" + currentState.getNumberValue(NUMBER_PLAYLEVEL);
					}
				}else{
					stateName += "_";
				}
				if(currentState.getTextValue(STRING_FULLTITLE).length() > 0) stateName += currentState.getTextValue(STRING_FULLTITLE);
				stateName += " " + getClearTypeName();
				stateName += " " + getRankTypeName();
			} else if(currentState instanceof KeyConfiguration) {
				stateName = "_Config";
			} else if(currentState instanceof SkinConfiguration) {
				stateName = "_Skin_Select";
			}
			stateName = stateName.replace("\\", "�뜝占�").replace("/", "塋딉옙").replace(":", "塋딉옙").replace("*", "塋딉옙").replace("?", "塋딉옙").replace("\"", "占쏙옙").replace("<", "塋딉옙").replace(">", "塋딉옙").replace("|", "影�占�").replace("\t", " ");

			path = "screenshot/" + sdf.format(Calendar.getInstance().getTime()) + stateName +".png";
		}

		@Override
		public void run() {
			// 占쎈�뀐옙源욑옙沅싷옙沅⑼옙爰귨옙寃쀯옙沅뗰옙爰귨옙源�占쎄텏占썬뀽援�255占쎄쾽占쎄굉占쎄데(=占쎈ㅈ�굟占쎈룾占쎄뎌占쎄퐩占쎄괜占쎄굉)
			for(int i = 3;i < pixels.length;i+=4) {
				pixels[i] = (byte) 0xff;
			}
			Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(),
					Pixmap.Format.RGBA8888);
			BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
			PixmapIO.writePNG(new FileHandle(path), pixmap);
			pixmap.dispose();
			Logger.getGlobal().info("占쎄때占쎄텥占쎄틒占쎄틬占쎄틡占쎄땅占쎄뭬占쎄맙占쎄퉱略녹빆異�:" + path);
			savetime = System.currentTimeMillis();
		}
	}

	/**
	 * Twitter占쎈뮊�몛�슌逾わ옙沅㏆옙爰껓옙湲울옙源�
	 */
	static class TwitterUploadThread extends Thread {

		/**
		 * 占쎈늾占쎈┐占쎄괏畑대슖�떃占쎄괼占쎄굴占쎌끋占쎈펻
		 */
		private long savetime;

		/**
		 * 占쎈늾占쎈┐占쎄괏畑대슖�떃占쎄괼占쎄굴占쎌끋占쎈펻
		 */
		private String text = "";

		private final PlayerConfig player;

		/**
		 * 占쎄때占쎄텥占쎄틒占쎄틬占쎄틡占쎄땅占쎄뭬占쎄맙占쎄퉱占쎄쿁pixel占쎄퉰占쎄틬占쎄땟
		 */
		private final byte[] pixels;

		public TwitterUploadThread(byte[] pixels, PlayerConfig player) {
			this.pixels = pixels;
			this.player = player;
			java.lang.StringBuilder builder = new java.lang.StringBuilder();
			if(currentState instanceof MusicSelector) {
				// empty
			} else if(currentState instanceof MusicDecide) {
				// empty
			} if(currentState instanceof BMSPlayer) {
				if(currentState.getTextValue(STRING_TABLE_NAME).length() > 0){
					builder.append(currentState.getTextValue(STRING_TABLE_LEVEL));
				}else{
					builder.append("LEVEL");
					builder.append(currentState.getNumberValue(NUMBER_PLAYLEVEL));
				}
				if(currentState.getTextValue(STRING_FULLTITLE).length() > 0) {
					builder.append(" ");
					builder.append(currentState.getTextValue(STRING_FULLTITLE));
				}
			} else if(currentState instanceof MusicResult || currentState instanceof CourseResult) {
				if(currentState instanceof MusicResult) {
					if(currentState.getTextValue(STRING_TABLE_NAME).length() > 0){
						builder.append(currentState.getTextValue(STRING_TABLE_LEVEL));
					}else{
						builder.append("LEVEL");
						builder.append(currentState.getNumberValue(NUMBER_PLAYLEVEL));
					}
				}
				if(currentState.getTextValue(STRING_FULLTITLE).length() > 0) {
					builder.append(" ");
					builder.append(currentState.getTextValue(STRING_FULLTITLE));
				}
				builder.append(" ");
				builder.append(getClearTypeName());
				builder.append(" ");
				builder.append(getRankTypeName());
			} else if(currentState instanceof KeyConfiguration) {
				// empty
			} else if(currentState instanceof SkinConfiguration) {
				// empty
			}
			text = builder.toString();
			text = text.replace("\\", "�뜝占�").replace("/", "塋딉옙").replace(":", "塋딉옙").replace("*", "塋딉옙").replace("?", "塋딉옙").replace("\"", "占쏙옙").replace("<", "塋딉옙").replace(">", "塋딉옙").replace("|", "影�占�").replace("\t", " ");
		}

		@Override
		public void run() {
			ConfigurationBuilder cb = new ConfigurationBuilder();
			cb.setOAuthConsumerKey(player.getTwitterConsumerKey())
			  .setOAuthConsumerSecret(player.getTwitterConsumerSecret())
			  .setOAuthAccessToken(player.getTwitterAccessToken())
			  .setOAuthAccessTokenSecret(player.getTwitterAccessTokenSecret());
			TwitterFactory twitterFactory = new TwitterFactory(cb.build());
			Twitter twitter = twitterFactory.getInstance();

			Pixmap pixmap = null;
	        try {
				// 占쎈�뀐옙源욑옙沅싷옙沅⑼옙爰귨옙寃쀯옙沅뗰옙爰귨옙源�占쎄텏占썬뀽援�255占쎄쾽占쎄굉占쎄데(=占쎈ㅈ�굟占쎈룾占쎄뎌占쎄퐩占쎄괜占쎄굉)
				for(int i = 3;i < pixels.length;i+=4) {
					pixels[i] = (byte) 0xff;
				}

				// create png byte stream
				pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(),
						Pixmap.Format.RGBA8888);
				BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
				ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
				PixmapIO.PNG png = new PixmapIO.PNG((int)(pixmap.getWidth() * pixmap.getHeight() * 1.5f));
				png.write(byteArrayOutputStream, pixmap);
				byte[] imageBytes=byteArrayOutputStream.toByteArray();
				ByteArrayInputStream byteArrayInputStream=new ByteArrayInputStream(imageBytes);

				// Upload Media and Post
				UploadedMedia mediastatus = twitter.uploadMedia("from beatoraja", byteArrayInputStream);
				Logger.getGlobal().info("Twitter Media Upload:" + mediastatus.toString());
				StatusUpdate update = new StatusUpdate(text);
				update.setMediaIds(new long[]{mediastatus.getMediaId()});
				Status status = twitter.updateStatus(update);
				Logger.getGlobal().info("Twitter Post:" + status.toString());
				savetime = System.currentTimeMillis();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if(pixmap != null) pixmap.dispose();
			}
		}
	}

	private UpdateThread updateSong;

	public void updateSong(String path) {
		if (updateSong == null || !updateSong.isAlive()) {
			updateSong = new SongUpdateThread(path);
			updateSong.start();
		} else {
			Logger.getGlobal().warning("�뮫�뜽�럧占쎌럪占쎈서鼇앾옙占쎄쿁占쎄굴占쎄탽占쎄낙�럪占쎈서沃ㅺ낙肄놅옙寃섓옙猷싷옙援ㅶ삌��嫄э옙援�占쎄께占쎄괼占쎄굴");
		}
	}

	public void updateTable(TableBar reader) {
		if (updateSong == null || !updateSong.isAlive()) {
			updateSong = new TableUpdateThread(reader);
			updateSong.start();
		} else {
			Logger.getGlobal().warning("�뮫�뜽�럧占쎌럪占쎈서鼇앾옙占쎄쿁占쎄굴占쎄탽占쎄낙�럪占쎈서沃ㅺ낙肄놅옙寃섓옙猷싷옙援ㅶ삌��嫄э옙援�占쎄께占쎄괼占쎄굴");
		}
	}

	abstract class UpdateThread extends Thread {

		private String message;

		public UpdateThread(String message) {
			this.message = message;
			FreeTypeFontParameter parameter = new FreeTypeFontParameter();
			parameter.size = 24;
			parameter.characters += message;
			if(updatefont != null) {
				updatefont.dispose();
			}
			updatefont = generator.generateFont(parameter);

		}
	}

	/**
	 * �뮫�뜽�럧占쎄퉰占쎄틬占쎄땟占쎄묜占쎄틬占쎄때占쎌럪占쎈서占쎈뎁占쎄때占쎄틕占쎄맙占쎄퉳
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
	 * 占쎌뜷占쎌굦鵝�占썹깗�벃�럪占쎈서占쎈뎁占쎄때占쎄틕占쎄맙占쎄퉳
	 *
	 * @author exch
	 */
	class TableUpdateThread extends UpdateThread {

		private final TableBar accessor;

		public TableUpdateThread(TableBar bar) {
			super("updating table : " + bar.getAccessor().name);
			accessor = bar;
		}

		public void run() {
			TableData td = accessor.getAccessor().read();
			if (td != null) {
				accessor.getAccessor().write(td);
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
			Logger.getGlobal().info("癲꾩뭿�닧占쎄괵占쎄덱占쎄굴BGM Set : " + bgms.size + " Sound Set : " + sounds.size);
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
