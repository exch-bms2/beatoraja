package bms.player.beatoraja.audio;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * PCM音源処理用クラス
 * 
 * @author exch
 */
public abstract class PCM<T> {

	// TODO PCM実データのダイレクトバッファ化

	/**
	 * チャンネル数
	 */
	public final int channels;
	/**
	 * 音源のサンプリングレート(Hz)
	 */
	public final int sampleRate;
	/**
	 * PCMデータ
	 */
	public final T sample;
	/**
	 * PCMデータ開始位置
	 */
	public final int start;
	/**
	 * PCMデータ長
	 */	
	public final int len;

	PCM(int channels, int sampleRate, int start, int len, T sample) {
		this.channels = channels;
		this.sampleRate = sampleRate;
		this.start = start;
		this.len = len;
		this.sample = sample;
	}

	public static PCM load(Path p) {
		try {
			return ShortPCM.loadPCM(p);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static PCM load(String name) {
		int index = name.lastIndexOf('.');
		if(index >= 0) {
			name = name.substring(0, index);			
		}
		final Path wavfile = Paths.get(name + ".wav");
		if (Files.exists(wavfile)) {
			PCM pcm = PCM.load(wavfile);
			if(pcm != null) {
				return pcm;
			}
		}
		final Path flacfile = Paths.get(name + ".flac");
		if (Files.exists(flacfile)) {
			PCM pcm = PCM.load(flacfile);
			if(pcm != null) {
				return pcm;
			}
		}
		final Path oggfile = Paths.get(name + ".ogg");
		if (Files.exists(oggfile)) {
			PCM pcm = PCM.load(oggfile);
			if(pcm != null) {
				return pcm;
			}
		}
		final Path mp3file = Paths.get(name + ".mp3");
		if (Files.exists(mp3file)) {
			PCM pcm = PCM.load(mp3file);
			if(pcm != null) {
				return pcm;
			}
		}
		return null;
	}
	
	/**
	 * サンプリングレートを変更したPCMを返す
	 * 
	 * @param sample
	 *            サンプリングレート
	 * @return サンプリングレート変更後のPCM
	 */
	public abstract PCM<T> changeSampleRate(int sample);

	/**
	 * 再生速度を変更したPCMを返す
	 * 
	 * @param rate
	 *            再生速度。基準は1.0
	 * @return 再生速度を変更したPCM
	 */
	public abstract PCM<T> changeFrequency(float rate);
	
	/**
	 * チャンネル数を変更したPCMを返す
	 * 
	 * @param channels
	 *            チャンネル数
	 * @return チャンネル数を変更したPCM
	 */
	public abstract PCM<T> changeChannels(int channels);
	/**
	 * トリミングしたPCMを返す
	 * 
	 * @param starttime
	 *            開始時間(us)
	 * @param duration
	 *            再生時間(us)
	 * @return トリミングしたPCM
	 */
	public abstract PCM<T> slice(long starttime, long duration);

	public abstract InputStream getInputStream();

}
