package bms.player.beatoraja.play.audio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;

/**
 * audio関係のメソッド群
 * 
 * @author exch
 */
public class AudioUtils {

	/**
	 * 音源ファイルをlibGDXの読み込める形に変換する
	 * 
	 * @param sourceFile
	 * @return
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	public static byte[] convertWav(Path sourceFile) throws UnsupportedAudioFileException, IOException {

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
	public static byte[] sliceWav(InputStream is, long starttime, long duration) throws UnsupportedAudioFileException,
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


}
