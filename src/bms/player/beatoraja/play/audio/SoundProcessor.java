package bms.player.beatoraja.play.audio;

import java.io.*;
import java.util.*;
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
		// BMS格納ディレクトリ
		String directorypath = filepath.substring(0, filepath.lastIndexOf(File.separatorChar) + 1);

		List<Sound> sounds = new ArrayList<Sound>();
		// TODO WAVデータを一旦全部読み込み、スライシングにかけてIDを振り直す
		for (String name : model.getWavList()) {
			name = name.substring(0, name.lastIndexOf('.'));
			Sound sound = null;
			final File wavfile = new File(directorypath + name + ".wav");
			File oggfile = new File(directorypath + name + ".ogg");
			File mp3file = new File(directorypath + name + ".mp3");
			try {
				if (wavfile.exists()) {
					RandomAccessFile f;
					try {
						f = new RandomAccessFile(wavfile, "r");
						byte[] header = new byte[44];
						f.read(header, 0, 44);
						f.close();
						if(header[20] == 85) {
							// WAVの中身がmp3の場合
							sound = Gdx.audio.newSound(new FileHandleStream("tempwav.mp3") {
								@Override
								public InputStream read() {
									try {
										BufferedInputStream input =new BufferedInputStream(new FileInputStream(wavfile));
										input.skip(44);
										return input;
									} catch (IOException e) {
										e.printStackTrace();
									}
									return null;
								}

								@Override
								public OutputStream write(boolean overwrite) {
									return null;
								}
							});							
						} else {
							sound = Gdx.audio.newSound(new FileHandleStream("tempwav.wav") {
								@Override
								public InputStream read() {
									try {
										return new ByteArrayInputStream(convertWav(wavfile));
									} catch (UnsupportedAudioFileException e) {
										e.printStackTrace();
									} catch (IOException e) {
										e.printStackTrace();
									}
									try {
										return new FileInputStream(wavfile);
									} catch (FileNotFoundException e) {
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
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				}
			} catch (GdxRuntimeException e) {
				Logger.getGlobal().warning("音源(wav)ファイル読み込み失敗。" + e.getMessage());
				e.printStackTrace();
			}
			if (sound == null) {
				try {
					if (oggfile.exists()) {
						sound = Gdx.audio.newSound(Gdx.files.internal(oggfile.getPath()));
					}
				} catch (GdxRuntimeException e) {
					Logger.getGlobal().warning("音源(ogg)ファイル読み込み失敗。" + e.getMessage());
					// e.printStackTrace();
				}
			}
			if (sound == null) {
				try {
					if (mp3file.exists()) {
						sound = Gdx.audio.newSound(Gdx.files.internal(mp3file.getPath()));
					}
				} catch (GdxRuntimeException e) {
					Logger.getGlobal().warning("音源(mp3)ファイル読み込み失敗。" + e.getMessage());
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

	synchronized public void play(int id, int starttime) {
		if (starttime != 0) {
			// TODO 音切りのロジック実装までは途中からの再生は行わない
			return;
		}
		try {
			if (id != -1 && wavmap[id] != null) {
				if (playmap[id] != -1) {
					wavmap[id].stop(playmap[id]);
				}
				playmap[id] = wavmap[id].play();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop(int id) {
		if (id == -1) {
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

	/**
	 * WAVデータをlibGDXの読み込める形に変換する
	 * @param sourceFile
	 * @return
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	private byte[] convertWav(File sourceFile) throws UnsupportedAudioFileException, IOException {

		AudioInputStream sourceStream = null;
		sourceStream = AudioSystem.getAudioInputStream(sourceFile);
		AudioFormat sourceFormat = sourceStream.getFormat();

		AudioFormat targetFormat = new AudioFormat(Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16,
				sourceFormat.getChannels(), sourceFormat.getFrameSize() * 16 / sourceFormat.getSampleSizeInBits(),
				sourceFormat.getSampleRate(), sourceFormat.isBigEndian());
		// System.out.println(sourceFormat + " : "
		// +sourceFormat.getFrameSize());
		// System.out.println(targetFormat + " : "
		// +targetFormat.getFrameSize());

		AudioInputStream targetStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		AudioSystem.write(targetStream, Type.WAVE, bos);

		return bos.toByteArray();
	}

	/**
	 * WAVデータを指定の開始時間、間隔で切り出す
	 * @param is
	 * @param starttime
	 * @param duration
	 * @return
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	private byte[] sliceWav(InputStream is, int starttime, int duration) throws UnsupportedAudioFileException, IOException {
		AudioInputStream sourceStream = AudioSystem.getAudioInputStream(is);
		AudioFormat format = sourceStream.getFormat();

		int bytesPerSecond = format.getFrameSize() * (int) format.getFrameRate();
		sourceStream.skip(starttime * bytesPerSecond / 1000);
		long framesOfAudioToCopy = duration * (int) format.getFrameRate() / 1000;
		AudioInputStream shortenedStream = new AudioInputStream(is, format, framesOfAudioToCopy);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		AudioSystem.write(shortenedStream, Type.WAVE, bos);

		return bos.toByteArray();
	}
}
