package bms.player.beatoraja.play.audio;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;

import bms.model.*;

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

	private List<SliceWav> slicesound = new ArrayList<SliceWav>();

	private long[] playmap = new long[0];

	private float progress = 0;

	/**
	 * BMSの音源データを読み込む
	 * 
	 * @param model
	 * @param filepath
	 */
	public void setModel(BMSModel model) {
		dispose();
		progress = 0;
		// BMS格納ディレクトリ
		Path dpath = Paths.get(model.getPath()).getParent();

		Map<Integer, byte[]> orgwavmap = new HashMap<Integer, byte[]>();
		Map<Integer, Sound> soundmap = new HashMap<Integer, Sound>();

		TimeLine[] timelines = model.getAllTimeLines();
		int wavcount = model.getWavList().length;

		for (TimeLine tl : timelines) {
			if (progress == 1) {
				break;
			}
			List<Note> notes = new ArrayList<Note>();
			for (int i = 0; i < 18; i++) {
				if (tl.getNote(i) != null) {
					notes.add(tl.getNote(i));
				}
				if (tl.getHiddenNote(i) != null) {
					notes.add(tl.getHiddenNote(i));
				}
			}
			notes.addAll(Arrays.asList(tl.getBackGroundNotes()));

			for (Note note : notes) {
				if (note.getWav() >= 0) {
					String name = model.getWavList()[note.getWav()];
					if (note.getStarttime() == 0 && note.getDuration() == 0) {
						// BMSのケース(音切りなし)
						if (soundmap.get(note.getWav()) == null) {
							Sound sound = getSound(dpath.resolve(name).toString());
							soundmap.put(note.getWav(), sound);
						}

					} else {
						// BMSONのケース(音切りあり)
						boolean b = true;
						for (SliceWav slice : slicesound) {
							if (slice.id == note.getWav() && slice.starttime == note.getStarttime()
									&& slice.duration == note.getDuration()) {
								b = false;
								break;
							}
						}
						if (b) {
							name = name.substring(0, name.lastIndexOf('.'));
							final Path wavfile = dpath.resolve(name + ".wav");
							final Path oggfile = dpath.resolve(name + ".ogg");
							final Path mp3file = dpath.resolve(name + ".mp3");

							byte[] wav = null;
							if (orgwavmap.get(note.getWav()) != null) {
								wav = orgwavmap.get(note.getWav());
							}
							if (wav == null && Files.exists(wavfile)) {
								try {
									wav = convertWav(wavfile);
									orgwavmap.put(note.getWav(), wav);
								} catch (UnsupportedAudioFileException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							if (wav == null && Files.exists(oggfile)) {
								try {
									wav = convertWav(oggfile);
									orgwavmap.put(note.getWav(), wav);
								} catch (UnsupportedAudioFileException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							if (wav == null && Files.exists(mp3file)) {
								try {
									wav = convertWav(mp3file);
									orgwavmap.put(note.getWav(), wav);
								} catch (UnsupportedAudioFileException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}

							if (wav != null) {
								// スライシング、wavid振り直し
								ByteArrayInputStream bais = new ByteArrayInputStream(wav);
								try {
									final byte[] slicewav = sliceWav(bais, note.getStarttime(), note.getDuration());
									Sound sound = Gdx.audio.newSound(new FileHandleStream("tempwav.wav") {
										@Override
										public InputStream read() {
											return new ByteArrayInputStream(slicewav);
										}

										@Override
										public OutputStream write(boolean overwrite) {
											return null;
										}
									});
									slicesound.add(new SliceWav(note, sound));
									// System.out.println("WAV slicing - Name:"
									// + name + " ID:" + note.getWav() +
									// " start:" + note.getStarttime() +
									// " duration:" + note.getDuration());
								} catch (UnsupportedAudioFileException e1) {
									e1.printStackTrace();
								} catch (IOException e1) {
									e1.printStackTrace();
								} catch (GdxRuntimeException e) {
									Logger.getGlobal().warning("音源(wav)ファイルスライシング失敗。" + e.getMessage());
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
			progress += 1f / timelines.length;
		}

		Logger.getGlobal().info("音源ファイル読み込み完了。音源数:" + soundmap.keySet().size());
		wavmap = new Sound[wavcount];
		for (int i = 0; i < wavmap.length; i++) {
			wavmap[i] = soundmap.get(i);
		}
		playmap = new long[wavmap.length];
		Arrays.fill(playmap, -1);
		progress = 1;
	}

	synchronized public void play(int id, int starttime, int duration) {
		try {
			// if(starttime == 0) {
			if (starttime == 0 && duration == 0) {
				if (id >= 0 && wavmap[id] != null) {
					if (playmap[id] != -1) {
						wavmap[id].stop(playmap[id]);
					}
					playmap[id] = wavmap[id].play();
				}
			} else {
				for (SliceWav slice : slicesound) {
					if (slice.id == id && slice.starttime == starttime && slice.duration == duration) {
						if (slice.playid != -1) {
							slice.wav.stop(slice.playid);
						}
						slice.playid = slice.wav.play();
						// System.out.println("slice WAV play - ID:" + id +
						// " start:" + starttime + " duration:" + duration);
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop(int id, int starttime, int duration) {
		if (id < 0) {
			for (Sound s : wavmap) {
				if (s != null) {
					s.stop();
				}
			}
			for (SliceWav slice : slicesound) {
				slice.wav.stop();
			}

		} else {
			if (starttime == 0 && duration == 0) {
				wavmap[id].stop();
			} else {
				for (SliceWav slice : slicesound) {
					if (slice.id == id && slice.starttime == starttime && slice.duration == duration) {
						slice.wav.stop(slice.playid);
						break;
					}
				}
			}

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
		for (SliceWav slice : slicesound) {
			slice.wav.dispose();
		}
		slicesound.clear();
	}

	public float getProgress() {
		return progress;
	}

	public void forceFinish() {
		progress = 1;
	}

	/**
	 * 音源ファイルをlibGDXの読み込める形に変換する
	 * 
	 * @param sourceFile
	 * @return
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	private static byte[] convertWav(Path sourceFile) throws UnsupportedAudioFileException, IOException {

		byte[] result = null;
		AudioInputStream sourceStream = null;
		sourceStream = AudioSystem.getAudioInputStream(sourceFile.toFile());
		AudioFormat sourceFormat = sourceStream.getFormat();
//		System.out.println(sourceFormat + " length : " + sourceStream.getFrameLength());
		if (sourceFormat.getEncoding().toString().equals("VORBIS")) {
			AudioFormat targetFormat = new AudioFormat(Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
			// System.out.println(sourceFormat + " : "
			// +sourceFormat.getFrameSize());
			// System.out.println(targetFormat + " : "
			// +targetFormat.getFrameSize());
			AudioInputStream targetStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
			Path tmp = Files.createTempFile("wav", "tmp");
			AudioSystem.write(targetStream, Type.WAVE, tmp.toFile());
			result = Files.readAllBytes(tmp);
			// System.out.println(result.length);
			Files.delete(tmp);
		} else if((sourceFormat.getEncoding() == Encoding.PCM_SIGNED && sourceFormat.getSampleSizeInBits() == 16) || 
				(sourceFormat.getEncoding() == Encoding.PCM_FLOAT && sourceFormat.getSampleSizeInBits() == 32)) {
			return Files.readAllBytes(sourceFile);
		} else {
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
			result = bos.toByteArray();
		}

		return result;
	}

	/**
	 * WAVデータを指定の開始時間、間隔で切り出す
	 * 
	 * @param is
	 * @param starttime
	 * @param duration
	 * @return
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	private byte[] sliceWav(InputStream is, long starttime, long duration) throws UnsupportedAudioFileException,
			IOException {
		AudioInputStream sourceStream = AudioSystem.getAudioInputStream(is);
		AudioFormat format = sourceStream.getFormat();

		int bytesPerSecond = format.getFrameSize() * (int) format.getFrameRate();
		sourceStream.skip(starttime * bytesPerSecond / 1000);
		long framesOfAudioToCopy = duration * (int) format.getFrameRate() / 1000;
		if (duration == 0) {
			framesOfAudioToCopy = sourceStream.getFrameLength() * format.getFrameSize() - starttime * bytesPerSecond
					/ 1000;
		}
		AudioInputStream shortenedStream = new AudioInputStream(is, format, framesOfAudioToCopy);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		AudioSystem.write(shortenedStream, Type.WAVE, bos);
		// System.out.println("sliced WAV status - offset : " + (starttime *
		// bytesPerSecond / 1000) + " len : " + framesOfAudioToCopy);
		return bos.toByteArray();
	}

	class SliceWav {
		public final int id;
		public final int starttime;
		public final int duration;
		public final Sound wav;

		public long playid = -1;

		public SliceWav(Note note, Sound wav) {
			this.id = note.getWav();
			this.starttime = note.getStarttime();
			this.duration = note.getDuration();
			this.wav = wav;
		}
	}

	public static Sound getSound(String name) {
		name = name.substring(0, name.lastIndexOf('.'));
		final Path wavfile = Paths.get(name + ".wav");
		final Path oggfile = Paths.get(name + ".ogg");
		final Path mp3file = Paths.get(name + ".mp3");

		Sound sound = null;
		try {
			if (Files.exists(wavfile)) {
				RandomAccessFile f;
				try {
					f = new RandomAccessFile(wavfile.toFile(), "r");
					byte[] header = new byte[44];
					f.read(header, 0, 44);
					f.close();
					if (header[20] == 85) {
						// WAVの中身がmp3の場合
						sound = Gdx.audio.newSound(new FileHandleStream("tempwav.mp3") {
							@Override
							public InputStream read() {
								try {
									BufferedInputStream input = new BufferedInputStream(Files.newInputStream(wavfile));
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
								} catch (Exception e) {
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
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		} catch (GdxRuntimeException e) {
			Logger.getGlobal().warning("音源(wav)ファイル読み込み失敗。" + e.getMessage());
			e.printStackTrace();
		}
		if (sound == null && Files.exists(oggfile)) {
			try {
				sound = Gdx.audio.newSound(Gdx.files.internal(oggfile.toString()));
			} catch (GdxRuntimeException e) {
				Logger.getGlobal().warning("音源(ogg)ファイル読み込み失敗。" + e.getMessage());
				// e.printStackTrace();
			}
		}
		if (sound == null && Files.exists(mp3file)) {
			try {
				sound = Gdx.audio.newSound(Gdx.files.internal(mp3file.toString()));
			} catch (GdxRuntimeException e) {
				Logger.getGlobal().warning("音源(mp3)ファイル読み込み失敗。" + e.getMessage());
				// e.printStackTrace();
			}
		}

		return sound;
	}
}
