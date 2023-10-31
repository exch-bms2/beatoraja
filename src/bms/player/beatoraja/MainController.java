package bms.player.beatoraja;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.lwjgl.input.Mouse;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.StringBuilder;

import bms.player.beatoraja.AudioConfig.DriverType;
import bms.player.beatoraja.MainState.MainStateType;
import bms.player.beatoraja.MessageRenderer.Message;
import bms.player.beatoraja.audio.*;
import bms.player.beatoraja.config.KeyConfiguration;
import bms.player.beatoraja.config.SkinConfiguration;
import bms.player.beatoraja.decide.MusicDecide;
import bms.player.beatoraja.external.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyCommand;
import bms.player.beatoraja.ir.*;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.result.CourseResult;
import bms.player.beatoraja.result.MusicResult;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.select.bar.TableBar;
import bms.player.beatoraja.skin.SkinLoader;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;
import bms.player.beatoraja.skin.SkinProperty;
import bms.player.beatoraja.skin.property.StringPropertyFactory;
import bms.player.beatoraja.song.*;
import bms.player.beatoraja.stream.StreamController;
import bms.tool.mdprocessor.MusicDownloadProcessor;

/**
 * アプリケーションのルートクラス
 *
 * @author exch
 */
public class MainController extends ApplicationAdapter {

	private static final String VERSION = "beatoraja 0.8.6";

	public static final boolean debug = false;

	/**
	 * 起動時間
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
	private MessageRenderer messageRenderer;

	private MainState current;
	
	private TimerManager timer;

	private Config config;
	private PlayerConfig player;
	private BMSPlayerMode auto;
	private boolean songUpdated;

	private SongInformationAccessor infodb;

	private IRStatus[] ir;

	private RivalDataAccessor rivals = new RivalDataAccessor();

	private RankingDataCache ircache = new RankingDataCache();

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

	private SystemSoundManager sound;

	private Thread screenshot;

	private MusicDownloadProcessor download;
	
	private StreamController streamController;

	public static final int offsetCount = SkinProperty.OFFSET_MAX + 1;
	private final SkinOffset[] offset = new SkinOffset[offsetCount];

	protected TextureRegion black;
	protected TextureRegion white;

	private final Array<MainStateListener> stateListener = new Array<MainStateListener>();

	public MainController(Path f, Config config, PlayerConfig player, BMSPlayerMode auto, boolean songUpdated) {
		this.auto = auto;
		this.config = config;
		this.songUpdated = songUpdated;

		for(int i = 0;i < offset.length;i++) {
			offset[i] = new SkinOffset();
		}

		if(player == null) {
			player = PlayerConfig.readPlayerConfig(config.getPlayerpath(), config.getPlayername());
		}
		this.player = player;

		this.bmsfile = f;

		if (config.isEnableIpfs()) {
			Path ipfspath = Paths.get("ipfs").toAbsolutePath();
			if (!ipfspath.toFile().exists())
				ipfspath.toFile().mkdirs();
			List<String> roots = new ArrayList<>(Arrays.asList(getConfig().getBmsroot()));
			if (ipfspath.toFile().exists() && !roots.contains(ipfspath.toString())) {
				roots.add(ipfspath.toString());
				getConfig().setBmsroot(roots.toArray(new String[roots.size()]));
			}
		}
		try {
			Class.forName("org.sqlite.JDBC");
			if(config.isUseSongInfo()) {
				infodb = new SongInformationAccessor(config.getSonginfopath());
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		playdata = new PlayDataAccessor(config);

		Array<IRStatus> irarray = new Array<IRStatus>();
		for(IRConfig irconfig : player.getIrconfig()) {
			final IRConnection ir = IRConnectionManager.getIRConnection(irconfig.getIrname());
			if(ir != null) {
				if(irconfig.getUserid().length() == 0 || irconfig.getPassword().length() == 0) {
				} else {
					IRResponse<IRPlayerData> response = ir.login(irconfig.getUserid(), irconfig.getPassword());
					if(response.isSucceeded()) {
						irarray.add(new IRStatus(irconfig, ir, response.getData()));
					} else {
						Logger.getGlobal().warning("IRへのログイン失敗 : " + response.getMessage());
					}
				}
			}

		}
		ir = irarray.toArray(IRStatus.class);
		
		rivals.update(this);

		switch(config.getAudioConfig().getDriver()) {
		case PortAudio:
			try {
				audio = new PortAudioDriver(config);
			} catch(Throwable e) {
				e.printStackTrace();
				config.getAudioConfig().setDriver(DriverType.OpenAL);
			}
			break;
		}

		timer = new TimerManager();
		sound = new SystemSoundManager(config);
		
		if(config.isUseDiscordRPC()) {
			stateListener.add(new DiscordListener());
		}
	}

	public SkinOffset getOffset(int index) {
		return offset[index];
	}

	public SongDatabaseAccessor getSongDatabase() {
		return MainLoader.getScoreDatabaseAccessor();
	}

	public SongInformationAccessor getInfoDatabase() {
		return infodb;
	}

	public PlayDataAccessor getPlayDataAccessor() {
		return playdata;
	}
	
	public RivalDataAccessor getRivalDataAccessor() {
		return rivals;
	}
	
	public RankingDataCache getRankingDataCache() {
		return ircache;
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

	public void changeState(MainStateType state) {
		MainState newState = null;
		switch (state) {
		case MUSICSELECT:
			if (this.bmsfile != null) {
				exit();
			} else {
				newState = selector;
			}
			break;
		case DECIDE:
			newState = decide;
			break;
		case PLAY:
			if (bmsplayer != null) {
				bmsplayer.dispose();
			}
			bmsplayer = new BMSPlayer(this, resource);
			newState = bmsplayer;
			break;
		case RESULT:
			newState = result;
			break;
		case COURSERESULT:
			newState = gresult;
			break;
		case CONFIG:
			newState = keyconfig;
			break;
		case SKINCONFIG:
			newState = skinconfig;
			break;
		}

		if (newState != null && current != newState) {
			if(current != null) {
				current.setSkin(null);
			}
			newState.create();
			if(newState.getSkin() != null) {
				newState.getSkin().prepare(newState);
			}
			if (current != null) {
				current.shutdown();
			}
			current = newState;
			timer.setMainState(newState);
			current.prepare();
			updateMainStateListener(0);
		}
		if (current.getStage() != null) {
			Gdx.input.setInputProcessor(new InputMultiplexer(current.getStage(), input.getKeyBoardInputProcesseor()));
		} else {
			Gdx.input.setInputProcessor(input.getKeyBoardInputProcesseor());
		}
	}

	public MainState getCurrentState() {
		return current;
	}

	public void setPlayMode(BMSPlayerMode auto) {
		this.auto = auto;

	}

	@Override
	public void create() {
		final long t = System.currentTimeMillis();
		sprite = new SpriteBatch();
		SkinLoader.initPixmapResourcePool(config.getSkinPixmapGen());

		try {
			generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
			FreeTypeFontParameter parameter = new FreeTypeFontParameter();
			parameter.size = 24;
			systemfont = generator.generateFont(parameter);
		} catch (GdxRuntimeException e) {
			Logger.getGlobal().severe("System Font１読み込み失敗");
		}
		messageRenderer = new MessageRenderer();

		input = new BMSPlayerInputProcessor(config, player);
		switch(config.getAudioConfig().getDriver()) {
		case OpenAL:
			audio = new GdxSoundDriver(config);
			break;
//		case AudioDevice:
//			audio = new GdxAudioDeviceDriver(config);
//			break;
		}

		resource = new PlayerResource(audio, config, player);
		selector = new MusicSelector(this, songUpdated);
		if(player.getRequestEnable()) {
		    streamController = new StreamController(selector, (player.getRequestNotify() ? messageRenderer : null));
	        streamController.run();
		}
		decide = new MusicDecide(this);
		result = new MusicResult(this);
		gresult = new CourseResult(this);
		keyconfig = new KeyConfiguration(this);
		skinconfig = new SkinConfiguration(this, player);
		if (bmsfile != null) {
			if(resource.setBMSFile(bmsfile, auto)) {
				changeState(MainStateType.PLAY);
			} else {
				// ダミーステートに移行してすぐexitする
				changeState(MainStateType.CONFIG);
				exit();
			}
		} else {
			changeState(MainStateType.MUSICSELECT);
		}

		Logger.getGlobal().info("初期化時間(ms) : " + (System.currentTimeMillis() - t));

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

		Array<String> targetlist = new Array(player.getTargetlist());
		for(int i = 0;i < rivals.getRivals().length;i++) {
			targetlist.add("RIVAL_" + (i + 1));
		}
		TargetProperty.setTargets(targetlist.toArray(String.class), this);

		Pixmap plainPixmap = new Pixmap(2,1, Pixmap.Format.RGBA8888);
		plainPixmap.drawPixel(0,0, Color.toIntBits(255,0,0,0));
		plainPixmap.drawPixel(1,0, Color.toIntBits(255,255,255,255));
		Texture plainTexture = new Texture(plainPixmap);
		black = new TextureRegion(plainTexture,0,0,1,1);
		white = new TextureRegion(plainTexture,1,0,1,1);
		plainPixmap.dispose();

		Gdx.gl.glClearColor(0, 0, 0, 1);

		if (config.isEnableIpfs()) {
			download = new MusicDownloadProcessor(config.getIpfsUrl(), (md5) -> {
				SongData[] s = getSongDatabase().getSongDatas(md5);
				String[] result = new String[s.length];
				for(int i = 0;i < result.length;i++) {
					result[i] = s[i].getPath();
				}
				return result;
			});
			download.start(null);
		}

		if(ir.length > 0) {
			messageRenderer.addMessage(ir.length + " IR Connection Succeed" ,5000, Color.GREEN, 1);
		}
	}

	private long prevtime;

	private final StringBuilder message = new StringBuilder();

	@Override
	public void render() {
//		input.poll();
		timer.update();

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		current.render();
		sprite.begin();
		if (current.getSkin() != null) {
			current.getSkin().updateCustomObjects(current);
			current.getSkin().drawAllObjects(sprite, current);
		}
		sprite.end();

		final Stage stage = current.getStage();
		if (stage != null) {
			stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
			stage.draw();
		}

		// show fps
		if (showfps && systemfont != null) {
			sprite.begin();
			systemfont.setColor(Color.CYAN);
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
				message.setLength(0);
				systemfont.draw(sprite, message.append("Skin Pixmap Resource Size ").append(SkinLoader.getResource().size()), 10,
						config.getResolution().height - 122);
				message.setLength(0);
				systemfont.draw(sprite, message.append("Stagefile Pixmap Resource Size ").append(selector.getStagefileResource().size()), 10,
						config.getResolution().height - 146);
				message.setLength(0);
				systemfont.draw(sprite, message.append("Banner Pixmap Resource Size ").append(selector.getBannerResource().size()), 10,
						config.getResolution().height - 170);
			}

			sprite.end();
		}

		// show message
		sprite.begin();
		messageRenderer.render(current, sprite, 100, config.getResolution().height - 2);
		sprite.end();

		// TODO renderループに入れるのではなく、MusicDownloadProcessorのListenerとして実装したほうがいいのでは
		if(download != null && download.isDownload()){
			downloadIpfsMessageRenderer(download.getMessage());
		}

		final long time = System.currentTimeMillis();
		if(time > prevtime) {
		    prevtime = time;
            current.input();
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

            // マウスカーソル表示判定
            if(input.isMouseMoved()) {
            	input.setMouseMoved(false);
            	mouseMovedTime = time;
			}
			Mouse.setGrabbed(current == bmsplayer && time > mouseMovedTime + 5000 && Mouse.isInsideWindow());

			// FPS表示切替
            if (input.isActivated(KeyCommand.SHOW_FPS)) {
                showfps = !showfps;
            }
            // fullscrees - windowed
            if (input.isActivated(KeyCommand.SWITCH_SCREEN_MODE)) {
                boolean fullscreen = Gdx.graphics.isFullscreen();
                Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
                if (fullscreen) {
                    Gdx.graphics.setWindowedMode(currentMode.width, currentMode.height);
                } else {
                    Gdx.graphics.setFullscreenMode(currentMode);
                }
                config.setDisplaymode(fullscreen ? Config.DisplayMode.WINDOW : Config.DisplayMode.FULLSCREEN);
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
            if (input.isActivated(KeyCommand.SAVE_SCREENSHOT)) {
                if (screenshot == null || !screenshot.isAlive()) {
            		final byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight(), true);
                    screenshot = new Thread(() -> {
                		// 全ピクセルのアルファ値を255にする(=透明色を無くす)
                		for(int i = 3;i < pixels.length;i+=4) {
                			pixels[i] = (byte) 0xff;
                		}
                    	new ScreenShotFileExporter().send(current, pixels);
                    });
                    screenshot.start();
                }
            }

            if (input.isActivated(KeyCommand.POST_TWITTER)) {
                if (screenshot == null || !screenshot.isAlive()) {
            		final byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight(), false);
                    screenshot = new Thread(() -> {
                		// 全ピクセルのアルファ値を255にする(=透明色を無くす)
                		for(int i = 3;i < pixels.length;i+=4) {
                			pixels[i] = (byte) 0xff;
                		}
                    	new ScreenShotTwitterExporter(player).send(current, pixels);
                    });
                    screenshot.start();
                }
            }

			if (download != null && download.getDownloadpath() != null) {
            	this.updateSong(download.getDownloadpath());
            	download.setDownloadpath(null);
            }
			if (updateSong != null && !updateSong.isAlive()) {
				selector.getBarRender().updateBar();
				updateSong = null;
			}
        }
	}

	@Override
	public void dispose() {
		saveConfig();

		if (bmsplayer != null) {
			bmsplayer.dispose();
		}
		if (selector != null) {
			selector.dispose();
		}
		if (streamController != null) {
		    streamController.dispose();
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
		if (download != null) {
			download.dispose();
		}

		Logger.getGlobal().info("全リソース破棄完了");
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
		PlayerConfig.write(config.getPlayerpath(), player);
		Logger.getGlobal().info("設定情報を保存");
	}

	public void exit() {
		Gdx.app.exit();
	}

	public BMSPlayerInputProcessor getInputProcessor() {
		return input;
	}

	public AudioDriver getAudioProcessor() {
		return audio;
	}

	public IRStatus[] getIRStatus() {
		return ir;
	}

	public SystemSoundManager getSoundManager() {
		return sound;
	}

	public MusicDownloadProcessor getMusicDownloadProcessor(){
		return download;
	}

	public MessageRenderer getMessageRenderer() {
		return messageRenderer;
	}
	
	public void updateMainStateListener(int status) {
		for(MainStateListener listener : stateListener) {
			listener.update(current, status);
		}
	}

	public long getPlayTime() {
		return System.currentTimeMillis() - boottime;
	}

	public Calendar getCurrnetTime() {
		cl.setTimeInMillis(System.currentTimeMillis());
		return cl;
	}

	public TimerManager getTimer() {
		return timer;
	}

	public long getStartTime() {
		return timer.getStartTime();
	}

	public long getStartMicroTime() {
		return timer.getStartMicroTime();
	}

	public long getNowTime() {
		return timer.getNowTime();
	}

	public long getNowTime(int id) {
		return timer.getNowTime(id);
	}

	public long getNowMicroTime() {
		return timer.getNowMicroTime();
	}

	public long getNowMicroTime(int id) {
		return timer.getNowMicroTime(id);
	}

	public long getTimer(int id) {
		return getMicroTimer(id) / 1000;
	}

	public long getMicroTimer(int id) {
		return timer.getMicroTimer(id);
	}

	public boolean isTimerOn(int id) {
		return getMicroTimer(id) != Long.MIN_VALUE;
	}

	public void setTimerOn(int id) {
		timer.setTimerOn(id);
	}

	public void setTimerOff(int id) {
		setMicroTimer(id, Long.MIN_VALUE);
	}

	public void setMicroTimer(int id, long microtime) {
		timer.setMicroTimer(id, microtime);
	}

	public void switchTimer(int id, boolean on) {
		timer.switchTimer(id, on);
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

	private UpdateThread downloadIpfs;

	public void downloadIpfsMessageRenderer(String message) {
		if (downloadIpfs == null || !downloadIpfs.isAlive()) {
			downloadIpfs = new DownloadMessageThread(message);
			downloadIpfs.start();
		}
	}

	public static String getVersion() {
		return VERSION;
	}

	abstract class UpdateThread extends Thread {

		protected String message;

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
			Message message = messageRenderer.addMessage(this.message, Color.CYAN, 1);
			getSongDatabase().updateSongDatas(path, config.getBmsroot(), false, getInfoDatabase());
			message.stop();
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
			super("updating table : " + bar.getAccessor().name);
			accessor = bar;
		}

		public void run() {
			Message message = messageRenderer.addMessage(this.message, Color.CYAN, 1);
			TableData td = accessor.getAccessor().read();
			if (td != null) {
				accessor.getAccessor().write(td);
				accessor.setTableData(td);
			}
			message.stop();
		}
	}

	class DownloadMessageThread extends UpdateThread {
		public DownloadMessageThread(String message) {
			super(message);
		}

		public void run() {
			Message message = messageRenderer.addMessage(this.message, Color.LIME, 1);
			while (download != null && download.isDownload() && download.getMessage() != null) {
				message.setText(download.getMessage());
				try {
					sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			message.stop();
		}
	}

	/**
	 * BGM、効果音セット管理用クラス
	 *
	 * @author exch
	 */
	public static class SystemSoundManager {
		/**
		 * 検出されたBGMセットのディレクトリパス
		 */
		private Array<Path> bgms = new Array<Path>();
		/**
		 * 現在のBGMセットのディレクトリパス
		 */
		private Path currentBGMPath;
		/**
		 * 検出された効果音セットのディレクトリパス
		 */
		private Array<Path> sounds = new Array<Path>();
		/**
		 * 現在の効果音セットのディレクトリパス
		 */
		private Path currentSoundPath;

		public SystemSoundManager(Config config) {
			if(config.getBgmpath() != null && config.getBgmpath().length() > 0) {
				scan(Paths.get(config.getBgmpath()).toAbsolutePath(), bgms, "select.wav");
			}
			if(config.getSoundpath() != null && config.getSoundpath().length() > 0) {
				scan(Paths.get(config.getSoundpath()).toAbsolutePath(), sounds, "clear.wav");
			}
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
					sub.forEach((t) -> {
						scan(t, paths, name);
					});
					if (AudioDriver.getPaths(p.resolve(name).toString()).length > 0) {
						paths.add(p);
					}
				} catch (IOException e) {
				}
			}
		}
	}

	public static class IRStatus {

		public final IRConfig config;
		public final IRConnection connection;
		public final IRPlayerData player;

		public IRStatus(IRConfig config, IRConnection connection, IRPlayerData player) {
			this.config = config;
			this.connection = connection;
			this.player = player;
		}
	}
}
