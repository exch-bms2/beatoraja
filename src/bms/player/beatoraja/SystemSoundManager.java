package bms.player.beatoraja;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.badlogic.gdx.utils.Array;

import bms.player.beatoraja.audio.AudioDriver;

/**
 * BGM、効果音セット管理用クラス
 *
 * @author exch
 */
public class SystemSoundManager {
	/**
	 * 検出されたBGMセットのディレクトリパス
	 */
	private Array<Path> bgms = new Array<Path>();
	/**
	 * 現在のBGMセットのディレクトリパス
	 */
	private Path currentBGMPath;
	/**
	 * 検出された効果音セットのディレクトリパス
	 */
	private Array<Path> sounds = new Array<Path>();
	/**
	 * 現在の効果音セットのディレクトリパス
	 */
	private Path currentSoundPath;

	public SystemSoundManager(Config config) {
		if(config.getBgmpath() != null && config.getBgmpath().length() > 0) {
			scan(Paths.get(config.getBgmpath()).toAbsolutePath(), bgms, "select.wav");
		}
		if(config.getSoundpath() != null && config.getSoundpath().length() > 0) {
			scan(Paths.get(config.getSoundpath()).toAbsolutePath(), sounds, "clear.wav");
		}
		Logger.getGlobal().info("検出されたBGM Set : " + bgms.size + " Sound Set : " + sounds.size);
	}

	public void shuffle() {
		if(bgms.size > 0) {
			currentBGMPath = bgms.get((int) (Math.random() * bgms.size));
		}
		if(sounds.size > 0) {
			currentSoundPath = sounds.get((int) (Math.random() * sounds.size));
		}
		Logger.getGlobal().info("BGM Set : " + currentBGMPath + " Sound Set : " + currentSoundPath);
	}

	public Path getBGMPath() {
		return currentBGMPath;
	}

	public Path getSoundPath() {
		return currentSoundPath;
	}

	private void scan(Path p, Array<Path> paths, String name) {
		if (Files.isDirectory(p)) {
			try (Stream<Path> sub = Files.list(p)) {
				sub.forEach((t) -> {
					scan(t, paths, name);
				});
				if (AudioDriver.getPaths(p.resolve(name).toString()).length > 0) {
					paths.add(p);
				}
			} catch (IOException e) {
			}
		}
	}
	
	public enum SoundType {
		SCRATCH("scratch.wav"), 
		FOLDER_OPEN("f-open.wav"), 
		FOLDER_CLOSE("f-close.wav"), 
		OPTION_CHANGE("o-change.wav"), 
		OPTION_OPEN("o-open.wav"), 
		OPTION_CLOSE("o-close.wav"), 
		PLAY_READY("playready.wav"), 
		PLAY_STOP("playstop.wav"), 
		RESULT_CLEAR("clear.wav"), 
		RESULT_FAIL("fail.wav"), 
		RESULT_CLOSE("resultclose.wav");
		
		public final String path; 
		
		private SoundType(String path) {
			this.path = path;
		}
	}
	
	public enum BGMType {
		SELECT("select.wav"), 
		DECIDE("decide.wav");
		
		public final String path; 
		
		private BGMType(String path) {
			this.path = path;
		}

	}
}