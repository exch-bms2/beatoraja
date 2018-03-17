package bms.player.beatoraja;

import java.util.Map;
import bms.player.beatoraja.play.JudgeAlgorithm;
import bms.player.beatoraja.skin.SkinType;
import lombok.Data;

import static bms.player.beatoraja.Resolution.*;

/**
 * 各種設定項目。config.jsonで保持される
 * 
 * @author exch
 */
@Data
public class Config {

	// TODO プレイヤー毎に異なる見込みの大きい要素をPlayerConfigに移動

	private String playername;

	private DisplayMode displaymode = DisplayMode.WINDOW;
	/**
	 * 垂直同期
	 */
	private boolean vsync;
	/**
	 * 解像度
	 */
	private Resolution resolution = HD;

	/**
	 * フォルダランプの有効/無効
	 */
	private boolean folderLamp = true;
	/**
	 * オーディオドライバー
	 */
	private int audioDriver = 0;
	/**
	 * オーディオ:OpenAL (libGDX Sound)
	 */
	public static final int AUDIODRIVER_SOUND = 0;
	public static final int AUDIODRIVER_AUDIODEVICE = 1;
	
	/**
	 * オーディオ:PortAudio
	 */
	public static final int AUDIODRIVER_PORTAUDIO = 2;

	private String audioDriverName = null;
	/**
	 * オーディオバッファサイズ。大きすぎると音声遅延が発生し、少なすぎるとノイズが発生する
	 */
	private int audioDeviceBufferSize = 384;
	/**
	 * オーディオ同時発音数
	 */
	private int audioDeviceSimultaneousSources = 128;

	/**
	 * オーディオ再生速度変化の処理:なし
	 */
	public static final int AUDIO_PLAY_UNPROCESSED = 0;
	/**
	 * オーディオ再生速度変化の処理:周波数を合わせる(速度に応じてピッチも変化)
	 */
	public static final int AUDIO_PLAY_FREQ = 1;
	/**
	 * オーディオ再生速度変化の処理:ピッチ変化なしに速度を変更(未実装)
	 */
	public static final int AUDIO_PLAY_SPEED = 1;
	/**
	 * PracticeモードのFREQUENCYオプションに対する音声処理方法
	 */
	private int audioFreqOption = AUDIO_PLAY_FREQ;
	/**
	 * 早送り再生に対する音声処理方法
	 */
	private int audioFastForward = AUDIO_PLAY_FREQ;

	/**
	 * システム音ボリューム
	 */
	private float systemVolume = 1.0f;
	/**
	 * キー音のボリューム
	 */
	private float keyVolume = 1.0f;
	/**
	 * BGノート音のボリューム
	 */
	private float bgVolume = 1.0f;
	/**
	 * 最大FPS。垂直同期OFFの時のみ有効
	 */
	private int maxFramePerSecond = 240;
	/**
	 * 最小入力感覚
	 */
	private int inputduration = 10;
	/**
	 * 選曲バー移動速度の最初
	 */
	private int scrollDurationLow = 300;
	/**
	 * 選曲バー移動速度の2つ目以降
	 */
	private int scrollDurationHigh = 50;
	/**
	 * 判定アルゴリズム
	 */
	private String judgeType = JudgeAlgorithm.Combo.name();
	
    private boolean cacheSkinImage = false;
    
    private boolean useSongInfo = true;

	private boolean showHiddenNote = false;

	private boolean showPastNote = false;

	private String bgmPath = "";

	private String soundPath = "";

	private SkinConfig[] skin;
	/**
	 * BMSルートディレクトリパス
	 */
	private String[] bmsRoot = new String[0];
	/**
	 * 難易度表URL
	 */
	private String[] tableURL = new String[0];
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
	private int bgaExpand = BGAEXPAND_FULL;
	public static final int BGAEXPAND_FULL = 0;
	public static final int BGAEXPAND_KEEP_ASPECT_RATIO = 1;
	public static final int BGAEXPAND_OFF = 2;

	private int frameSkip = 1;

	private boolean updateSong = false;

	private int autoSaveReplay[] = {0,0,0,0};
	
	private int skinPixmapGen = 4;
	private int bannerPixmapGen = 2;

	public Config() {
		tableURL = new String[] { "http://bmsnormal2.syuriken.jp/table.html",
				"http://bmsnormal2.syuriken.jp/table_insane.html",
				"http://walkure.net/hakkyou/for_glassist/bms/?lamp=easy",
				"http://walkure.net/hakkyou/for_glassist/bms/?lamp=normal",
				"http://walkure.net/hakkyou/for_glassist/bms/?lamp=hard",
				"http://walkure.net/hakkyou/for_glassist/bms/?lamp=fc",
				"http://dpbmsdelta.web.fc2.com/table/dpdelta.html",
				"http://dpbmsdelta.web.fc2.com/table/insane.html",
				"http://flowermaster.web.fc2.com/lrnanido/gla/LN.html",
				"http://stellawingroad.web.fc2.com/new/pms.html",
				"http://sky.geocities.jp/exclusion_bms/table24k/table.html",
		};
		int maxSkinType = 0;
		for (SkinType type : SkinType.values()) {
			if (type.getId() > maxSkinType) {
				maxSkinType = type.getId();
			}
		}
		skin = new SkinConfig[maxSkinType + 1];
		for (Map.Entry<SkinType, String> entry : SkinConfig.defaultSkinPathMap.entrySet()) {
			skin[entry.getKey().getId()] = new SkinConfig(entry.getValue());
		}
	}

	public String getJudgeType() {
		for(JudgeAlgorithm type : JudgeAlgorithm.values()) {
			if(type.name().equals(judgeType)) {
				return judgeType;
			}
		}
		judgeType = JudgeAlgorithm.Combo.name();
		return judgeType;
	}

	public float getKeyVolume() {
		if(keyVolume < 0 || keyVolume > 1) {
			keyVolume = 1;
		}
		return keyVolume;
	}

	public float getBgVolume() {
		if(bgVolume < 0 || bgVolume > 1) {
			bgVolume = 1;
		}
		return bgVolume;
	}

	public int getAudioDriver() {
		if(audioDriver != Config.AUDIODRIVER_SOUND && audioDriver != Config.AUDIODRIVER_PORTAUDIO) {
			audioDriver = Config.AUDIODRIVER_SOUND;
		}
		return audioDriver;
	}

	public float getSystemVolume() {
		if(systemVolume < 0 || systemVolume > 1) {
			systemVolume = 1;
		}
		return systemVolume;
	}

	public enum DisplayMode {
		FULLSCREEN,BORDERLESS,WINDOW;
	}
}
