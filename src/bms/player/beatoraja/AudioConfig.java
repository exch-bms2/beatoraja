package bms.player.beatoraja;

import com.badlogic.gdx.math.MathUtils;

/**
 * オーディオコンフィグ
 * 
 * @author exch
 */
public class AudioConfig implements Validatable {

	/**
	 * オーディオドライバー
	 */
	private DriverType driver = DriverType.OpenAL;

	/**
	 * オーディオドライバー名
	 */
	private String driverName = null;
	/**
	 * オーディオバッファサイズ。大きすぎると音声遅延が発生し、少なすぎるとノイズが発生する
	 */
	private int deviceBufferSize = 384;
	/**
	 * オーディオ同時発音数
	 */
	private int deviceSimultaneousSources = 256;
	/**
	 * オーディオサンプリングレート(0:指定なし)
	 */
	private int sampleRate = 0;

	/**
	 * PracticeモードのFREQUENCYオプションに対する音声処理方法
	 */
	private FrequencyType freqOption = FrequencyType.FREQUENCY;
	/**
	 * 早送り再生に対する音声処理方法
	 */
	private FrequencyType fastForward = FrequencyType.FREQUENCY;

	/**
	 * システム音ボリューム
	 */
	private float systemvolume = 0.5f;
	/**
	 * キー音のボリューム
	 */
	private float keyvolume = 0.5f;
	/**
	 * BGノート音のボリューム
	 */
	private float bgvolume = 0.5f;

	/**
	 * リザルト画面のサウンドをループ再生するか
	 */
	private boolean isLoopResultSound = false;

	/**
	 * コースリザルト画面のサウンドをループ再生するか
	 */
	private boolean isLoopCourseResultSound = false;

	public DriverType getDriver() {
		return driver;
	}

	public void setDriver(DriverType driver) {
		this.driver = driver;
	}

	public String getDriverName() {
		return driverName;
	}

	public void setDriverName(String driverName) {
		this.driverName = driverName;
	}

	public int getDeviceBufferSize() {
		return deviceBufferSize;
	}

	public void setDeviceBufferSize(int deviceBufferSize) {
		this.deviceBufferSize = deviceBufferSize;
	}

	public int getDeviceSimultaneousSources() {
		return deviceSimultaneousSources;
	}

	public void setDeviceSimultaneousSources(int deviceSimultaneousSources) {
		this.deviceSimultaneousSources = deviceSimultaneousSources;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}

	public FrequencyType getFreqOption() {
		return freqOption;
	}

	public void setFreqOption(FrequencyType freqOption) {
		this.freqOption = freqOption;
	}

	public FrequencyType getFastForward() {
		return fastForward;
	}

	public void setFastForward(FrequencyType fastForward) {
		this.fastForward = fastForward;
	}
	
	public float getSystemvolume() {
		return systemvolume;
	}

	public void setSystemvolume(float systemvolume) {
		this.systemvolume = systemvolume;
	}

	public float getKeyvolume() {
		return keyvolume;
	}

	public void setKeyvolume(float keyvolume) {
		this.keyvolume = keyvolume;
	}

	public float getBgvolume() {
		return bgvolume;
	}

	public void setBgvolume(float bgvolume) {
		this.bgvolume = bgvolume;
	}

	public boolean isLoopResultSound() {
		return isLoopResultSound;
	}

	public void setLoopResultSound(boolean loopResultSound) {
		isLoopResultSound = loopResultSound;
	}

	public boolean isLoopCourseResultSound() {
		return isLoopCourseResultSound;
	}

	public void setLoopCourseResultSound(boolean loopCourseResultSound) {
		isLoopCourseResultSound = loopCourseResultSound;
	}
	
	public boolean validate() {
		if(driver == null) {
			driver = DriverType.OpenAL;
		}
		deviceBufferSize = MathUtils.clamp(deviceBufferSize, 4, 4096);
		deviceSimultaneousSources = MathUtils.clamp(deviceSimultaneousSources, 16, 1024);
		if(freqOption == null) {
			freqOption = FrequencyType.FREQUENCY;
		}
		if(fastForward == null) {
			fastForward = FrequencyType.FREQUENCY;
		}
		systemvolume = MathUtils.clamp(systemvolume, 0f, 1f);
		keyvolume = MathUtils.clamp(keyvolume, 0f, 1f);
		bgvolume = MathUtils.clamp(bgvolume, 0f, 1f);
		return true;
	}
	
	public enum DriverType {

		/**
		 * OpenAL (libGDX Sound)
		 */
		OpenAL,
		/**
		 * PortAudio
		 */
		PortAudio,
		/**
		 * AudioDevice (libGDX AudioDevice, 未実装)
		 */
//		AudioDevice,
	}
	
	public enum FrequencyType {
		
		/**
		 * オーディオ再生速度変化の処理:なし
		 */
		UNPROCESSED,
		/**
		 * オーディオ再生速度変化の処理:周波数を合わせる(速度に応じてピッチも変化)
		 */
		FREQUENCY,
		/**
		 * オーディオ再生速度変化の処理:ピッチ変化なしに速度を変更(未実装)
		 */
//		SPEED
	}
}
