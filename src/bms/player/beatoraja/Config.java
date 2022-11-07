package bms.player.beatoraja;

import static bms.player.beatoraja.Resolution.*;

import java.io.*;
import java.nio.file.Files;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

import bms.player.beatoraja.AudioConfig.DriverType;
import bms.player.beatoraja.AudioConfig.FrequencyType;

/**
 * 各種設定項目。config.jsonで保持される
 *
 * @author exch
 */
public class Config implements Validatable {

	/**
	 * 選択中のプレイヤー名
	 */
	private String playername;
	/**
	 * ディスプレイモード
	 */
	private DisplayMode displaymode = DisplayMode.WINDOW;
	/**
	 * 垂直同期
	 */
	private boolean vsync;
	/**
	 * 解像度
	 */
	private Resolution resolution = HD;

	private boolean useResolution = true;
	private int windowWidth = 1280;
	private int windowHeight = 720;

	/**
	 * フォルダランプの有効/無効
	 */
	private boolean folderlamp = true;

	/**
	 * オーディオコンフィグ
	 */
	private AudioConfig audio;

	/**
	 * 最大FPS。垂直同期OFFの時のみ有効
	 */
	private int maxFramePerSecond = 240;

	private int prepareFramePerSecond = 0;
	/**
	 * 検索バー同時表示上限数
	 */
	private int maxSearchBarCount = 10;
	/**
	 * 所持していない楽曲バーを表示するかどうか
	 */
	private boolean showNoSongExistingBar = true;
	/**
	 * 選曲バー移動速度の最初
	 */
	private int scrolldurationlow = 300;
	/**
	 * 選曲バー移動速度の2つ目以降
	 */
	private int scrolldurationhigh = 50;
	/**
	 * 選曲バーとレーンカバーのアナログスクロール
	 */
	private boolean analogScroll = true;
	/**
	 * 選曲バー移動速度に関連（アナログスクロール）
	 */
	private int analogTicksPerScroll = 3;

	/**
	 * プレビュー再生
	 */
	private SongPreview songPreview = SongPreview.LOOP;
	/**
	 * スキン画像のキャッシュイメージを作成するかどうか
	 */
    private boolean cacheSkinImage = false;
    /**
     * songinfoデータベースを使用するかどうか
     */
    private boolean useSongInfo = true;

	private String songpath = SONGPATH_DEFAULT;
	public static final String SONGPATH_DEFAULT = "songdata.db";

	private String songinfopath = SONGINFOPATH_DEFAULT;
	public static final String SONGINFOPATH_DEFAULT = "songinfo.db";

	private String tablepath = TABLEPATH_DEFAULT;
	public static final String TABLEPATH_DEFAULT = "table";

	private String playerpath = PLAYERPATH_DEFAULT;
	public static final String PLAYERPATH_DEFAULT = "player";

	private String skinpath = SKINPATH_DEFAULT;
	public static final String SKINPATH_DEFAULT = "skin";

	private String bgmpath = "bgm";

	private String soundpath = "sound";

	/**
	 * BMSルートディレクトリパス
	 */
	private String[] bmsroot = new String[0];
	/**
	 * 難易度表URL
	 */
	private String[] tableURL = DEFAULT_TABLEURL;
	/**
	 * BGA表示
	 */
	private int bga = BGA_ON;
	public static final int BGA_ON = 0;
	public static final int BGA_AUTO = 1;
	public static final int BGA_OFF = 2;
	/**
	 * BGA拡大
	 */
	private int bgaExpand = BGAEXPAND_KEEP_ASPECT_RATIO;
	public static final int BGAEXPAND_FULL = 0;
	public static final int BGAEXPAND_KEEP_ASPECT_RATIO = 1;
	public static final int BGAEXPAND_OFF = 2;

	private int frameskip = 1;

	private boolean updatesong = false;

	private int skinPixmapGen = 4;
	private int stagefilePixmapGen = 2;
	private int bannerPixmapGen = 2;
	private int songResourceGen = 1;

	private boolean enableIpfs = true;
	private String ipfsurl = "https://gateway.ipfs.io/";

	private int irSendCount = 5;

	private boolean useDiscordRPC = true;

	private static final String[] DEFAULT_TABLEURL = { "http://bmsnormal2.syuriken.jp/table.html",
			"http://bmsnormal2.syuriken.jp/table_insane.html",
			"http://www.ribbit.xyz/bms/tables/normal.html",
			"http://www.ribbit.xyz/bms/tables/insane.html",
			"http://walkure.net/hakkyou/for_glassist/bms/?lamp=easy",
			"http://walkure.net/hakkyou/for_glassist/bms/?lamp=normal",
			"http://walkure.net/hakkyou/for_glassist/bms/?lamp=hard",
			"http://walkure.net/hakkyou/for_glassist/bms/?lamp=fc",
			"https://mocha-repository.info/table/dpn_header.json",
			"https://mocha-repository.info/table/dpi_header.json",
			"https://mocha-repository.info/table/ln_header.json",
			"http://stellawingroad.web.fc2.com/new/pms.html",
			"https://excln.github.io/table24k/table.html",
	};

	// TODO 以下の変数は別クラスに移行済(0.8.2)。バージョンが進んだら消す
	int autosavereplay[] = {0,0,0,0};
	private int audioDriver = 0;
	private String audioDriverName = null;
	private int audioDeviceBufferSize = 384;
	private int audioDeviceSimultaneousSources = 128;
	private int audioFreqOption = 1;
	private int audioFastForward = 1;
	private float systemvolume = 0.5f;
	private float keyvolume = 0.5f;
	private float bgvolume = 0.5f;

	public Config() {
	}

	public String getPlayername() {
		return playername;
	}

	public void setPlayername(String playername) {
		this.playername = playername;
	}

	public boolean isVsync() {
		return vsync;
	}

	public void setVsync(boolean vsync) {
		this.vsync = vsync;
	}

	public int getBga() {
		return bga;
	}

	public void setBga(int bga) {
		this.bga = bga;
	}

	public AudioConfig getAudioConfig() {
		return audio;
	}

	public void setAudioConfig(AudioConfig audio) {
		this.audio = audio;
	}

	public int getMaxFramePerSecond() {
		return maxFramePerSecond;
	}

	public void setMaxFramePerSecond(int maxFramePerSecond) {
		this.maxFramePerSecond = maxFramePerSecond;
	}

	public int getPrepareFramePerSecond() {
		return prepareFramePerSecond;
	}

	public void setPrepareFramePerSecond(int prepareFramePerSecond) {
		this.prepareFramePerSecond = prepareFramePerSecond;
	}

	public String[] getBmsroot() {
		return bmsroot;
	}

	public void setBmsroot(String[] bmsroot) {
		this.bmsroot = bmsroot;
	}

	public String[] getTableURL() {
		return tableURL;
	}

	public void setTableURL(String[] tableURL) {
		this.tableURL = tableURL;
	}

	public boolean isFolderlamp() {
		return folderlamp;
	}

	public void setFolderlamp(boolean folderlamp) {
		this.folderlamp = folderlamp;
	}

	public Resolution getResolution() {
		return resolution;
	}

	public void setResolution(Resolution resolution) {
		this.resolution = resolution;
	}

	public int getWindowWidth() {
		return windowWidth;
	}

	public void setWindowWidth(int width) {
		this.windowWidth = width;
	}

	public int getWindowHeight() {
		return windowHeight;
	}

	public void setWindowHeight(int height) {
		this.windowHeight = height;
	}

	public int getFrameskip() {
		return frameskip;
	}

	public void setFrameskip(int frameskip) {
		this.frameskip = frameskip;
	}

	public String getBgmpath() {
		return bgmpath;
	}

	public void setBgmpath(String bgmpath) {
		this.bgmpath = bgmpath;
	}

	public String getSoundpath() {
		return soundpath;
	}

	public void setSoundpath(String soundpath) {
		this.soundpath = soundpath;
	}

	public int getMaxSearchBarCount() {
	    return maxSearchBarCount;
    }

    public void setMaxSearchBarCount(int maxSearchBarCount) {
	    this.maxSearchBarCount = maxSearchBarCount;
    }

	public boolean isShowNoSongExistingBar() {
		return showNoSongExistingBar;
	}

	public void setShowNoSongExistingBar(boolean showNoExistingSongBar) {
		this.showNoSongExistingBar = showNoExistingSongBar;
	}

	public int getScrollDurationLow(){
		return scrolldurationlow;
	}
	public void setScrollDutationLow(int scrolldurationlow){
		this.scrolldurationlow = scrolldurationlow;
	}
	public int getScrollDurationHigh(){
		return scrolldurationhigh;
	}
	public void setScrollDutationHigh(int scrolldurationhigh){
		this.scrolldurationhigh = scrolldurationhigh;
	}

    public boolean isAnalogScroll() {
        return analogScroll;
    }
    public void setAnalogScroll(boolean analogScroll) {
        this.analogScroll = analogScroll;
    }

    public int getAnalogTicksPerScroll() {
        return analogTicksPerScroll;
    }
    public void setAnalogTicksPerScroll(int analogTicksPerScroll) {
        this.analogTicksPerScroll = Math.max(analogTicksPerScroll, 1);
    }

	public SongPreview getSongPreview() {
		return songPreview;
	}

	public void setSongPreview(SongPreview songPreview) {
		this.songPreview = songPreview;
	}

	public boolean isUseSongInfo() {
		return useSongInfo;
	}

	public void setUseSongInfo(boolean useSongInfo) {
		this.useSongInfo = useSongInfo;
	}

	public int getBgaExpand() {
		return bgaExpand;
	}

	public void setBgaExpand(int bgaExpand) {
		this.bgaExpand = bgaExpand;
	}

	public boolean isCacheSkinImage() {
		return cacheSkinImage;
	}

	public void setCacheSkinImage(boolean cacheSkinImage) {
		this.cacheSkinImage = cacheSkinImage;
	}

	public boolean isUseDiscordRPC() {
		return useDiscordRPC;
	}

	public void setUseDiscordRPC(boolean useDiscordRPC) {
		this.useDiscordRPC = useDiscordRPC;
	}

	public boolean isUpdatesong() {
		return updatesong;
	}

	public void setUpdatesong(boolean updatesong) {
		this.updatesong = updatesong;
	}

	public DisplayMode getDisplaymode() {
		return displaymode;
	}

	public void setDisplaymode(DisplayMode displaymode) {
		this.displaymode = displaymode;
	}

	public int getSkinPixmapGen() {
		return skinPixmapGen;
	}

	public void setSkinPixmapGen(int skinPixmapGen) {
		this.skinPixmapGen = skinPixmapGen;
	}

	public int getStagefilePixmapGen() {
		return stagefilePixmapGen;
	}

	public void setStagefilePixmapGen(int stagefilePixmapGen) {
		this.stagefilePixmapGen = stagefilePixmapGen;
	}

	public int getBannerPixmapGen() {
		return bannerPixmapGen;
	}

	public void setBannerPixmapGen(int bannerPixmapGen) {
		this.bannerPixmapGen = bannerPixmapGen;
	}

	public int getSongResourceGen() {
		return songResourceGen;
	}

	public void setSongResourceGen(int songResourceGen) {
		this.songResourceGen = songResourceGen;
	}

	public boolean isEnableIpfs() {
		return enableIpfs;
	}

	public void setEnableIpfs(boolean enableIpfs) {
		this.enableIpfs = enableIpfs;
	}

	public String getIpfsUrl() {
		return ipfsurl;
	}

	public void setIpfsUrl(String ipfsUrl) {
		this.ipfsurl = ipfsUrl;
	}

	public String getSongpath() {
		return songpath;
	}

	public void setSongpath(String songpath) {
		this.songpath = songpath;
	}

	public String getSonginfopath() {
		return songinfopath;
	}

	public void setSonginfopath(String songinfopath) {
		this.songinfopath = songinfopath;
	}

	public String getTablepath() {
		return tablepath;
	}

	public void setTablepath(String tablepath) {
		this.tablepath = tablepath;
	}

	public String getPlayerpath() {
		return playerpath;
	}

	public void setPlayerpath(String playerpath) {
		this.playerpath = playerpath;
	}

	public String getSkinpath() {
		return skinpath;
	}

	public void setSkinpath(String skinpath) {
		this.skinpath = skinpath;
	}

	public boolean validate() {
		displaymode = (displaymode != null) ? displaymode : DisplayMode.WINDOW;
		resolution = (resolution != null) ? resolution : Resolution.HD;

		windowWidth = MathUtils.clamp(windowWidth, Resolution.SD.width, Resolution.ULTRAHD.width);
		windowHeight = MathUtils.clamp(windowHeight, Resolution.SD.height, Resolution.ULTRAHD.height);

		if(audio == null) {
			audio = new AudioConfig();
			audio.setDriver(audioDriver == 2 ? DriverType.PortAudio : DriverType.OpenAL);
			audio.setDriverName(audioDriverName);
			audio.setDeviceBufferSize(audioDeviceBufferSize);
			audio.setDeviceSimultaneousSources(audioDeviceSimultaneousSources);
			audio.setFreqOption(audioFreqOption == 1 ? FrequencyType.FREQUENCY : FrequencyType.UNPROCESSED);
			audio.setFastForward(audioFastForward == 1 ? FrequencyType.FREQUENCY : FrequencyType.UNPROCESSED);
			audio.setSystemvolume(systemvolume);
			audio.setKeyvolume(keyvolume);
			audio.setBgvolume(bgvolume);
		}
		audio.validate();
		maxFramePerSecond = MathUtils.clamp(maxFramePerSecond, 0, 50000);
		prepareFramePerSecond = MathUtils.clamp(prepareFramePerSecond, 0, 100000);
        maxSearchBarCount = MathUtils.clamp(maxSearchBarCount, 1, 100);
        songPreview = (songPreview != null) ? songPreview : SongPreview.LOOP;

		scrolldurationlow = MathUtils.clamp(scrolldurationlow, 2, 1000);
		scrolldurationhigh = MathUtils.clamp(scrolldurationhigh, 1, 1000);
		irSendCount = MathUtils.clamp(irSendCount, 1, 100);

		skinPixmapGen = MathUtils.clamp(skinPixmapGen, 0, 100);
		stagefilePixmapGen = MathUtils.clamp(stagefilePixmapGen, 0, 100);
		bannerPixmapGen = MathUtils.clamp(bannerPixmapGen, 0, 100);
		songResourceGen = MathUtils.clamp(songResourceGen, 0, 100);

		bmsroot = Validatable.removeInvalidElements(bmsroot);

		if(tableURL == null) {
			tableURL = DEFAULT_TABLEURL;
		}
		tableURL = Validatable.removeInvalidElements(tableURL);

		bga = MathUtils.clamp(bga, 0, 2);
		bgaExpand = MathUtils.clamp(bgaExpand, 0, 2);
		if (ipfsurl == null) {
			ipfsurl = "https://gateway.ipfs.io/";
		}

		songpath = songpath != null ? songpath : SONGPATH_DEFAULT;
		songinfopath = songinfopath != null ? songinfopath : SONGINFOPATH_DEFAULT;
		tablepath = tablepath != null ? tablepath : TABLEPATH_DEFAULT;
		playerpath = playerpath != null ? playerpath : PLAYERPATH_DEFAULT;
		skinpath = skinpath != null ? skinpath : SKINPATH_DEFAULT;
		return true;
	}

	public static Config read() {
		Config config = null;
		if (Files.exists(MainController.configpath)) {
			Json json = new Json();
			json.setIgnoreUnknownFields(true);
			try (FileReader reader = new FileReader(MainController.configpath.toFile())) {
				config = json.fromJson(Config.class, reader);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(config == null) {
			config = new Config();
		}
		config.validate();

		PlayerConfig.init(config);

		return config;
	}

	public static void write(Config config) {
		Json json = new Json();
		json.setUsePrototypes(false);
		json.setOutputType(OutputType.json);
		try (FileWriter writer = new FileWriter(MainController.configpath.toFile())) {
			writer.write(json.prettyPrint(config));
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getIrSendCount() {
		return irSendCount;
	}

	public void setIrSendCount(int irSendCount) {
		this.irSendCount = irSendCount;
	}

	public boolean isUseResolution() {
		return useResolution;
	}

	public void setUseResolution(boolean useResolution) {
		this.useResolution = useResolution;
	}

	public enum DisplayMode {
		FULLSCREEN,BORDERLESS,WINDOW;
	}

	public enum SongPreview {
		NONE,ONCE,LOOP;
	}
}
