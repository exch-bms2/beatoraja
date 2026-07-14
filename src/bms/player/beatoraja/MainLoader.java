package bms.player.beatoraja;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import bms.player.beatoraja.AudioConfig.DriverType;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.ir.IRConnectionManager;
import bms.player.beatoraja.ir.IRResponse;
import bms.player.beatoraja.ir.IRVersionInfo;
import bms.player.beatoraja.launcher.PlayConfigurationView;
import bms.player.beatoraja.song.SQLiteSongDatabaseAccessor;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongDatabaseAccessor;
import bms.player.beatoraja.song.SongUtils;
import jdk.jfr.StackTrace;

/**
 * 起動用クラス
 *
 * @author exch
 */
public class MainLoader extends Application {

	private static final boolean ALLOWS_32BIT_JAVA = false;

	private static final Set<String> illegalSongs = new HashSet<String>();

	private static Path bmsPath;

	private static VersionChecker version;

	private final SongDatabaseAccessorProvider songDatabaseAccessorProvider = new SongDatabaseAccessorProvider();

	public static void main(String[] args) {

		if(!ALLOWS_32BIT_JAVA && !System.getProperty( "os.arch" ).contains( "64")) {
			JOptionPane.showMessageDialog(null, "This Application needs 64bit-Java.", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

		Logger logger = Logger.getGlobal();
		try {
			logger.addHandler(new FileHandler("beatoraja_log.xml"));
		} catch (Throwable e) {
			e.printStackTrace();
		}

		BMSPlayerMode auto = null;
		for (String s : args) {
			if (s.startsWith("-")) {
				if (s.equals("-a")) {
					auto = BMSPlayerMode.AUTOPLAY;
				}
				if (s.equals("-p")) {
					auto = BMSPlayerMode.PRACTICE;
				}
				if (s.equals("-r") || s.equals("-r1")) {
					auto = BMSPlayerMode.REPLAY_1;
				}
				if (s.equals("-r2")) {
					auto = BMSPlayerMode.REPLAY_2;
				}
				if (s.equals("-r3")) {
					auto = BMSPlayerMode.REPLAY_3;
				}
				if (s.equals("-r4")) {
					auto = BMSPlayerMode.REPLAY_4;
				}
				if (s.equals("-s")) {
					auto = BMSPlayerMode.PLAY;
				}
			} else {
				bmsPath = Paths.get(s);
				if(auto == null) {
					auto = BMSPlayerMode.PLAY;
				}
			}
		}



		if (Files.exists(Config.configpath) && (bmsPath != null || auto != null)) {
			IRConnectionManager.getAllAvailableIRConnectionName();
			play(bmsPath, auto, true, null, null, bmsPath != null);
		} else {
			launch(args);
		}
	}

	public static void play(Path f, BMSPlayerMode auto, boolean forceExit, Config config, PlayerConfig player, boolean songUpdated) {
		if(config == null) {
			config = Config.read();
		}
		if(player == null) {
			player = PlayerConfig.readPlayerConfig(config.getPlayerpath(), config.getPlayername());
		}

		SongDatabaseAccessor songdb;
		try {
			songdb = new SongDatabaseAccessorProvider().get(config, player.getId());
			detectIllegalSongs(songdb, player);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			Logger.getGlobal().severe("楽曲データベース初期化中の例外:" + e.getMessage());
			return;
		}
		if(illegalSongs.size() > 0) {
			JOptionPane.showMessageDialog(null, "This Application detects " + illegalSongs.size() + " illegal BMS songs. \n Remove them, update song database and restart.", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

		try {
			final MainController main = new MainController(f, config, player, auto, songUpdated, songdb);

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
			cfg.title = MainController.getVersion();

			if(config.getAudioConfig().getDriver() == DriverType.AudioDevice) {
				cfg.audioDeviceBufferSize = Math.max(config.getAudioConfig().getDeviceBufferSize() * 2 * 2, 4096);
			} else {
				cfg.audioDeviceBufferSize = config.getAudioConfig().getDeviceBufferSize();
			}
			cfg.audioDeviceSimultaneousSources = config.getAudioConfig().getDeviceSimultaneousSources();
			cfg.forceExit = forceExit;
			if(config.getAudioConfig().getDriver() == DriverType.PortAudio) {
				LwjglApplicationConfiguration.disableAudio = true;
			}
			// System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL",
			// "true");

			new LwjglApplication(new ApplicationListener() {
				
				public void resume() {
					main.resume();
				}
				
				public void resize(int width, int height) {
					main.resize(width, height);
				}
				
				public void render() {
					main.render();
				}
				
				public void pause() {
					main.pause();
				}
				
				public void dispose() {
					main.dispose();
				}
				
				public void create() {
					main.create();
				}
			}, cfg) {
				// This is some kind of hack. Refer to com.badlogic.gdx.controllers.desktop.OisControllers code.
				// OisControllers creates a runnable that polls the controller input. It polls the input every frame via
				// `Gdx.app.postRunnable` function.
				//
				// We want it to poll the input on every poll thread's interval. Be it 1000Hz or 8000Hz.
				// To achieve that, we *intercept* the runnable, store it somewhere else, and call that runnable
				// directly on the polling thread. (BMSPlayerInputProcessor.poll). Further postRunnable for that
				// runnable will be prohibited.
				private Runnable oisRunnable = null;
				@Override
				public void postRunnable(Runnable runnable) {
					if (oisRunnable == null) {
						StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
						if (stackTrace.length >= 3) {
							if (stackTrace[2].getClassName().startsWith("com.badlogic.gdx.controllers.desktop.OisControllers")) {
								oisRunnable = runnable;
								BMSPlayerInputProcessor.controllerPollRunner = runnable;
							}
						}
					}
					else if (runnable == oisRunnable) return;

					super.postRunnable(runnable);
				}
			};

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

	public static VersionChecker getVersionChecker() {
		if(version == null) {
			version = new CompositeVersionChecker();
		}
		return version;
	}

	public static void setVersionChecker(VersionChecker version) {
		if(version != null) {
			MainLoader.version = version;
		}
	}

	public static Path getBMSPath() {
		return bmsPath;
	}

	public static void putIllegalSong(String hash) {
		illegalSongs.add(hash);
	}

	public static String[] getIllegalSongs() {
		return illegalSongs.toArray(new String[illegalSongs.size()]);
	}

	public static int getIllegalSongCount() {
		return illegalSongs.size();
	}

	private static void detectIllegalSongs(SongDatabaseAccessor songdb, PlayerConfig player) {
		illegalSongs.clear();

		Set<String> hashes = new LinkedHashSet<>();
		addIllegalSongHashes(hashes, SongUtils.illegalsongs);
		for(IRConfig irconfig : player.getIrconfig()) {
			if(irconfig == null || irconfig.getIrname() == null || irconfig.getIrname().length() == 0) {
				continue;
			}
			IRConnection ir = IRConnectionManager.getIRConnection(irconfig.getIrname());
			if(ir == null) {
				continue;
			}
			try {
				IRResponse<String[]> response = ir.getIllegalSongs();
				if(response.isSucceeded()) {
					addIllegalSongHashes(hashes, response.getData());
				} else if(response.getMessage() != null && response.getMessage().length() > 0
						&& !"Not supported".equals(response.getMessage())) {
					Logger.getGlobal().warning("IR illegal song list取得失敗 - " + irconfig.getIrname() + " : "
							+ response.getMessage());
				}
			} finally {
				closeIRConnection(ir);
			}
		}

		if(hashes.isEmpty()) {
			return;
		}
		for(SongData song : songdb.getSongDatas(hashes.toArray(String[]::new))) {
			if(song != null && song.getSha256() != null && song.getSha256().length() > 0) {
				MainLoader.putIllegalSong(song.getSha256());
			}
		}
	}

	private static void addIllegalSongHashes(Set<String> hashes, String[] candidates) {
		if(candidates == null) {
			return;
		}
		for(String candidate : candidates) {
			if(candidate != null && candidate.length() == 64) {
				hashes.add(candidate);
			}
		}
	}

	private static void closeIRConnection(IRConnection ir) {
		if(ir instanceof AutoCloseable closeable) {
			try {
				closeable.close();
			} catch(Exception e) {
			}
		}
	}

	@Override
	public void start(javafx.stage.Stage primaryStage) throws Exception {
		Config config = Config.read();

		try {
//			final long t = System.currentTimeMillis();
			ResourceBundle bundle = ResourceBundle.getBundle("resources.UIResources");
			FXMLLoader loader = new FXMLLoader(
					MainLoader.class.getResource("/bms/player/beatoraja/launcher/PlayConfigurationView.fxml"), bundle);
			VBox stackPane = (VBox) loader.load();
			PlayConfigurationView bmsinfo = (PlayConfigurationView) loader.getController();
			bmsinfo.setBMSInformationLoader(this);
			bmsinfo.setSongDatabaseAccessorResolver(songDatabaseAccessorProvider::get);
			bmsinfo.update(config);
			Scene scene = new Scene(stackPane, stackPane.getPrefWidth(), stackPane.getPrefHeight());
			primaryStage.setScene(scene);
			primaryStage.setTitle(MainController.getVersion() + " configuration");
			primaryStage.setOnCloseRequest((event) -> {
				bmsinfo.exit();
			});
			primaryStage.show();
//			Logger.getGlobal().info("初期化時間(ms) : " + (System.currentTimeMillis() - t));

		} catch (IOException e) {
			Logger.getGlobal().severe(e.getMessage());
			e.printStackTrace();
		}
	}

	private static class SongDatabaseAccessorProvider {

		private SongDatabaseAccessor songdb;

		private String songpath;

		private String reviewpath;

		public synchronized SongDatabaseAccessor get(Config config) throws ClassNotFoundException {
			return get(config, config.getPlayername());
		}

		public synchronized SongDatabaseAccessor get(Config config, String playername) throws ClassNotFoundException {
			String nextSongpath = config.getSongpath();
			String nextReviewpath = getSongReviewPath(config, playername);
			if (songdb == null || !nextSongpath.equals(songpath) || !nextReviewpath.equals(reviewpath)) {
				Class.forName("org.sqlite.JDBC");
				songdb = new SQLiteSongDatabaseAccessor(nextSongpath, config.getBmsroot(), nextReviewpath);
				songpath = nextSongpath;
				reviewpath = nextReviewpath;
			}
			return songdb;
		}

		private String getSongReviewPath(Config config, String playername) {
			return config.getPlayerpath() + File.separatorChar + playername + File.separatorChar + "songreview.db";
		}
	}

	public interface VersionChecker {
		public String getMessage();
		public String getDownloadURL();
	}

	private static class CompositeVersionChecker implements VersionChecker {

		private String dlurl;
		private String message;

		public String getMessage() {
			if(message == null) {
				getInformation();
			}
			return message;
		}

		public String getDownloadURL() {
			if(message == null) {
				getInformation();
			}
			return dlurl;
		}

		private void getInformation() {
			VersionChecker github = new GithubVersionChecker();
			message = github.getMessage();
			dlurl = github.getDownloadURL();

			IRVersionInfo irVersion = getIRVersionInfo();
			if(irVersion != null) {
				message = getIRVersionMessage(irVersion);
				dlurl = irVersion.downloadURL;
			}
		}

		private IRVersionInfo getIRVersionInfo() {
			Config config = Config.read();
			PlayerConfig player = PlayerConfig.readPlayerConfig(config.getPlayerpath(), config.getPlayername());
			for(IRConfig irconfig : player.getIrconfig()) {
				if(irconfig == null || irconfig.getIrname() == null || irconfig.getIrname().length() == 0) {
					continue;
				}
				IRConnection ir = IRConnectionManager.getIRConnection(irconfig.getIrname());
				if(ir == null) {
					continue;
				}
				try {
					IRResponse<IRVersionInfo> response = ir.getVersionInfo(MainController.getVersion());
					if(response.isSucceeded() && response.getData() != null) {
						return response.getData();
					}
					if(response.getMessage() != null && response.getMessage().length() > 0
							&& !"Not supported".equals(response.getMessage())) {
						Logger.getGlobal().warning("IR version取得失敗 - " + irconfig.getIrname() + " : "
								+ response.getMessage());
					}
				} finally {
					closeIRConnection(ir);
				}
			}
			return null;
		}

		private String getIRVersionMessage(IRVersionInfo version) {
			if(version.message != null && version.message.length() > 0) {
				return version.message;
			}
			if(version.version != null && version.version.length() > 0
					&& !MainController.getVersion().contains(version.version)) {
				return String.format("最新版[%s]を利用可能です。", version.version);
			}
			return "最新版を利用中です";
		}
	}

	private static class GithubVersionChecker implements VersionChecker {

		private String dlurl;
		private String message;

		public String getMessage() {
			if(message == null) {
				getInformation();
			}
			return message;
		}

		public String getDownloadURL() {
			if(message == null) {
				getInformation();
			}
			return dlurl;
		}

		private void getInformation() {
			try {
				URL url = new URL("https://api.github.com/repos/exch-bms2/beatoraja/releases/latest");
				ObjectMapper mapper = new ObjectMapper();
				GithubLastestRelease lastestData = mapper.readValue(url, GithubLastestRelease.class);
				final String name = lastestData.name;
				if (MainController.getVersion().contains(name)) {
					message = "最新版を利用中です";
				} else {
					message = String.format("最新版[%s]を利用可能です。", name);
					dlurl = "https://mocha-repository.info/download/beatoraja" + name + ".zip";
				}
			} catch (Exception e) {
				Logger.getGlobal().warning("最新版URL取得時例外:" + e.getMessage());
				message = "バージョン情報を取得できませんでした";
			}
		}
	}

	@JsonIgnoreProperties(ignoreUnknown=true)
	static class GithubLastestRelease{
		public String name;
	}

}
