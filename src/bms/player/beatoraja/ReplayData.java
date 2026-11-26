package bms.player.beatoraja;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StreamUtils;
import com.badlogic.gdx.utils.StreamUtils.OptimizedByteArrayOutputStream;

import bms.player.beatoraja.input.KeyInputLog;
import bms.player.beatoraja.pattern.PatternModifyLog;

/**
 * リプレイデータ。キー入力ログ、譜面変更情報、ゲージ種類を含む
 * 
 * @author exch
 */
public final class ReplayData implements Validatable {

	/**
	 * プレイヤー名
	 */
	public String player;
	/**
	 * 楽曲のSHA-256
 	 */
	public String sha256;
	/**
	 * モード
	 */
	public int mode;
	/**
	 * キー入力ログ
	 */
	public KeyInputLog[] keylog = KeyInputLog.EMPTYARRAY;
	
	public String keyinput;
	/** Null for legacy replays; logical keys recorded through the lossless keyboard judgment stream. */
	public int[] keyboardEventKeys;
    /** Null in legacy/keyboard-only replays; logical controller keys using ordered judgment events. */
    public int[] controllerEventKeys;
    /** Null in older replays; logical keys recorded through the mouse-scratch judgment FIFO. */
    public int[] mouseScratchEventKeys;
	/**
	 * ゲージの種類
	 */
	public int gauge;
	/** Missing in older replays, which always use beatoraja. */
	public String primaryScoringRuleId;
	public String requestedPrimaryScoringRuleId;
	public String primaryScoringFallbackReason;
	public int scoringRuleVersion = 1;
	public long scoringFrameUs;
	/** Null in legacy replays, which did not record these chart modifiers. */
	public ChartModifiers chartModifiers;
	/** Null in older replays, which use the current scoring settings for compatibility. */
	public ScoringSettings scoringSettings;

	public static final class ScoringSettings {
		public boolean customJudge;
		public int keyPerfectGreat = 100, keyGreat = 100, keyGood = 100;
		public int scratchPerfectGreat = 100, scratchGreat = 100, scratchGood = 100;
		public int longnoteMargin = 100;
		public int gaugeAutoShift, bottomShiftableGauge;

		public static ScoringSettings capture(PlayerConfig config) {
			var settings = new ScoringSettings();
			settings.customJudge = config.isCustomJudge();
			settings.keyPerfectGreat = config.getKeyJudgeWindowRatePerfectGreat();
			settings.keyGreat = config.getKeyJudgeWindowRateGreat(); settings.keyGood = config.getKeyJudgeWindowRateGood();
			settings.scratchPerfectGreat = config.getScratchJudgeWindowRatePerfectGreat();
			settings.scratchGreat = config.getScratchJudgeWindowRateGreat(); settings.scratchGood = config.getScratchJudgeWindowRateGood();
			settings.longnoteMargin = config.getLongnoteMarginRate();
			settings.gaugeAutoShift = config.getGaugeAutoShift(); settings.bottomShiftableGauge = config.getBottomShiftableGauge();
			return settings;
		}

		/** A separate config limits restoration to scoring options and leaves the user profile intact. */
		public PlayerConfig toPlayerConfig(int gauge) {
			var config = new PlayerConfig(); config.setGauge(gauge);
			config.setCustomJudge(customJudge);
			config.setKeyJudgeWindowRatePerfectGreat(keyPerfectGreat);
			config.setKeyJudgeWindowRateGreat(keyGreat); config.setKeyJudgeWindowRateGood(keyGood);
			config.setScratchJudgeWindowRatePerfectGreat(scratchPerfectGreat);
			config.setScratchJudgeWindowRateGreat(scratchGreat); config.setScratchJudgeWindowRateGood(scratchGood);
			config.setLongnoteMarginRate(longnoteMargin);
			config.setGaugeAutoShift(gaugeAutoShift); config.setBottomShiftableGauge(bottomShiftableGauge);
			return config;
		}
	}

	public static final class ChartModifiers {
		public long seed;
		public int scrollMode, scrollSection, longnoteMode, mineMode, extranoteType, extranoteDepth;
		public double scrollRate, longnoteRate;
		public boolean extranoteScratch;
	}
	/**
	 * 譜面オプションによる変更ログ。旧データとの互換性維持用
	 */
	public PatternModifyLog[] pattern;
	public int[][] laneShufflePattern;
	/**
	 * ランダムシーケンスを含むbmsの場合、選択されたRANDOM番号
	 */
	public int[] rand = new int[0];
	/**
	 * プレイ日時(unixtime)
	 */
	public long date = 0;
	/**
	 * 7to9配置
	 */
	public int sevenToNinePattern = 0;
	/** Null in legacy replays, which only recorded the conversion pattern. */
	public Integer sevenToNineType;
	public Integer sevenToNineHranThresholdBPM;
	/**
	 * 譜面オプション
	 */
	public int randomoption = 0;
	
	public long randomoptionseed = -1;
	/**
	 * 譜面オプション(2P)
	 */
	public int randomoption2 = 0;
	
	public long randomoption2seed = -1;
	/**
	 * DP用オプション
	 */
	public int doubleoption = 0;
	/**
	 * プレイコンフィグ
	 */
	public PlayConfig config;
	
	public void shrink() {
		if (keylog.length == 0) return;
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			OutputStream base64 = Base64.getUrlEncoder().wrap(output);
			OutputStream gzip = new GZIPOutputStream(base64);
			ByteBuffer keyinputdata = ByteBuffer.allocate(keylog.length * 9).order(ByteOrder.LITTLE_ENDIAN);
			for (KeyInputLog log : keylog) {
				keyinputdata.put((byte)((log.getKeycode() + 1) * (log.isPressed() ? 1 : -1)));
				keyinputdata.putLong(log.getTime());
			}
			StreamUtils.copyStream(new ByteArrayInputStream(keyinputdata.array()), gzip);
			gzip.close();
			keyinput = output.toString();
			keylog = KeyInputLog.EMPTYARRAY;
 		} catch (IOException e) {
		}
	}

	@Override
	public boolean validate() {
        if (mouseScratchEventKeys != null && mouseScratchEventKeys.length > 256) return false;
        if (mouseScratchEventKeys != null) mouseScratchEventKeys = java.util.Arrays.stream(mouseScratchEventKeys)
                .filter(key -> key >= 0 && key < 256).distinct().toArray();
        if (controllerEventKeys != null && controllerEventKeys.length > 256) return false;
        if (controllerEventKeys != null) controllerEventKeys = java.util.Arrays.stream(controllerEventKeys)
                .filter(key -> key >= 0 && key < 256).distinct().toArray();
		if (keyboardEventKeys != null && keyboardEventKeys.length > 256) return false;
		if (keyboardEventKeys != null) keyboardEventKeys = java.util.Arrays.stream(keyboardEventKeys)
				.filter(key -> key >= 0 && key < 256).distinct().toArray();
		if (keyinput != null) {
			try {
				InputStream input = new ByteArrayInputStream(keyinput.getBytes());
				InputStream base64 = Base64.getUrlDecoder().wrap(input);
				GZIPInputStream gzip = new GZIPInputStream(base64);
				OptimizedByteArrayOutputStream output = new OptimizedByteArrayOutputStream(keyinput.length());
				StreamUtils.copyStream(gzip, output);
				ByteBuffer keyinputdata = ByteBuffer.wrap(output.getBuffer()).order(ByteOrder.LITTLE_ENDIAN);
				keyinputdata.limit(output.size());
				Array<KeyInputLog> keylogarray = new Array<KeyInputLog>(output.size() / 9);
				while(keyinputdata.remaining() >= 9) {
					final byte keycode = keyinputdata.get();
					final long time = keyinputdata.getLong();
					keylogarray.add(new KeyInputLog(time, Math.abs((int)keycode)  - 1, keycode >= 0));
//					System.out.println(time + " - " + (Math.abs((int)keycode) - 1) + " : " + (keycode >= 0));
				}
				keylog = keylogarray.toArray(KeyInputLog.class);
				gzip.close();
			} catch (IOException e) {
			}
			keyinput = null;
		}

		keylog = keylog != null ? Validatable.removeInvalidElements(keylog) : KeyInputLog.EMPTYARRAY;
		pattern = pattern != null ? Validatable.removeInvalidElements(pattern) : null;
		return keylog.length > 0;
	}
}
