package bms.player.beatoraja.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;

import bms.model.BMSModel;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandleStream;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * キー音リソースの管理、及びキー音を鳴らすためのクラス
 * 
 * @author exch
 */
public class SoundProcessor implements AudioProcessor {

	private BMSModel model;

	private Sound[] wavmap = new Sound[0];

	private long[] playmap = new long[0];
	
	private float progress = 0;

	/**
	 * BMSの音源データを読み込む
	 * 
	 * @param model
	 * @param filepath
	 */
	public void setModel(BMSModel model, String filepath) {
		dispose();
		progress = 0;

		this.model = model;
		// BMS格納ディレクトリ
		String directorypath = filepath.substring(0,
				filepath.lastIndexOf(File.separatorChar) + 1);

		List<Sound> sounds = new ArrayList<Sound>();
		for (String name : model.getWavList()) {
			name = name.substring(0, name.lastIndexOf('.'));
			Sound sound = null;
			final File wavfile = new File(directorypath + name + ".wav");
			File oggfile = new File(directorypath + name + ".ogg");
			try {
				if (wavfile.exists()) {
					sound = Gdx.audio.newSound(new FileHandleStream(
							"tempwav.wav") {
						@Override
						public InputStream read() {
							try {
								return new ByteArrayInputStream(
										convertWav(wavfile));
							} catch (UnsupportedAudioFileException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
							try {
								return new FileInputStream(wavfile);
							} catch (FileNotFoundException e) {
								// TODO 自動生成された catch ブロック
								e.printStackTrace();
							}
							return null;
						}

						@Override
						public OutputStream write(boolean overwrite) {
							return null;
						}
					});
				}
			} catch (GdxRuntimeException e) {
				Logger.getGlobal().warning(
						"音源(wav)ファイル読み込み失敗。" + e.getMessage());
				e.printStackTrace();
			}
			if (sound == null) {
				try {
					if (oggfile.exists()) {
						sound = Gdx.audio.newSound(Gdx.files.internal(oggfile
								.getPath()));
					}
				} catch (GdxRuntimeException e) {
					Logger.getGlobal().warning(
							"音源(ogg)ファイル読み込み失敗。" + e.getMessage());
					// e.printStackTrace();
				}
			}
			sounds.add(sound);
			progress += 1f / model.getWavList().length;
		}
		Logger.getGlobal().info("音源ファイル読み込み完了。音源数:" + model.getWavList().length);
		wavmap = sounds.toArray(new Sound[0]);
		playmap = new long[wavmap.length];
		Arrays.fill(playmap, -1);
		progress = 1;
	}

	public void play(int id, int starttime) {
		if(starttime != 0) {
			// TODO 音切りのロジック実装までは途中からの再生は行わない
			return;
		}
		try {
			if (id != -1 && wavmap[id] != null) {
				if(playmap[id] != -1) {
					wavmap[id].stop(playmap[id]);					
				}
				playmap[id] = wavmap[id].play();
			}			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void stop(int id) {
		if(id == -1) {
			for (Sound s : wavmap) {
				if (s != null) {
					s.stop();
				}
			}
		} else {
			wavmap[id].stop();			
		}
	}

	/**
	 * リソースを開放する
	 */
	public void dispose() {
		for (Sound id : wavmap) {
			if (id != null) {
				id.dispose();
			}
		}
	}

	public float getProgress() {
		return progress;
	}

	private byte[] convertWav(File sourceFile)
			throws UnsupportedAudioFileException, IOException {
		
		AudioInputStream sourceStream = null;
		sourceStream = AudioSystem.getAudioInputStream(sourceFile);
		AudioFormat sourceFormat = sourceStream.getFormat();

		AudioFormat targetFormat = new AudioFormat(Encoding.PCM_SIGNED,
				sourceFormat.getSampleRate(), 16,
				sourceFormat.getChannels(), sourceFormat.getFrameSize() * 16 / sourceFormat.getSampleSizeInBits(),
				sourceFormat.getSampleRate(), sourceFormat.isBigEndian());
//		System.out.println(sourceFormat + " : " +sourceFormat.getFrameSize());
//		System.out.println(targetFormat + " : " +targetFormat.getFrameSize());

		AudioInputStream targetStream = AudioSystem.getAudioInputStream(
				targetFormat, sourceStream);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		AudioSystem.write(targetStream, Type.WAVE, bos);

		return bos.toByteArray();
	}

}
