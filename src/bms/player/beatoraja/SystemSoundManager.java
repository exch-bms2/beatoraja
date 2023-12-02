package bms.player.beatoraja;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import bms.player.beatoraja.audio.AudioDriver;

/**
 * BGM、効果音セット管理用クラス
 *
 * @author exch
 */
public class SystemSoundManager {
	
	private final MainController main;
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
	
	private ObjectMap<SoundType, String> soundmap = new ObjectMap<>();

	public SystemSoundManager(MainController main) {
		this.main = main;
		Config config = main.getConfig();
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
		
		for(SoundType sound : SoundType.values()) {
			for(Path p :getSoundPaths(sound)) {
				String newpath = p.toString();
				String oldpath = soundmap.get(sound);
				if (newpath.equals(oldpath)) {
					break;
				}
				if (oldpath != null) {
					main.getAudioProcessor().dispose(oldpath);
				}
				soundmap.put(sound, newpath);
				break;
			}

		}
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
				sub.forEach((t) -> scan(t, paths, name));
				if (AudioDriver.getPaths(p.resolve(name).toString()).length > 0) {
					paths.add(p);
				}
			} catch (IOException e) {
			}
		}
	}
	
	public Path[] getSoundPaths(SoundType type) {
		Path p = type.isBGM ? currentBGMPath : currentSoundPath;
		
		Array<Path> paths = new Array<Path>();
		if(p != null) {
			paths.addAll(AudioDriver.getPaths(p.resolve(type.path).toString()));			
		}
		paths.addAll(AudioDriver.getPaths(Paths.get("defaultsound").resolve(type.path).toString()));
		return paths.toArray(Path.class);
	}
	
	public String getSound(SoundType sound) {
		return soundmap.get(sound);
	}

	public void play(SoundType sound, boolean loop) {
		final String path = soundmap.get(sound);
		if (path != null) {
			main.getAudioProcessor().play(path, main.getConfig().getAudioConfig().getSystemvolume(), loop);
		}
	}

	public void stop(SoundType sound) {
		final String path = soundmap.get(sound);
		if (path != null) {
			main.getAudioProcessor().stop(path);
		}
	}

	public enum SoundType {
		SCRATCH("scratch.wav",false), 
		FOLDER_OPEN("f-open.wav",false), 
		FOLDER_CLOSE("f-close.wav",false), 
		OPTION_CHANGE("o-change.wav",false), 
		OPTION_OPEN("o-open.wav",false), 
		OPTION_CLOSE("o-close.wav",false), 
		PLAY_READY("playready.wav",false), 
		PLAY_STOP("playstop.wav",false), 
		RESULT_CLEAR("clear.wav",false), 
		RESULT_FAIL("fail.wav",false), 
		RESULT_CLOSE("resultclose.wav",false),
		COURSE_CLEAR("course_clear.wav",false), 
		COURSE_FAIL("course_fail.wav",false), 
		COURSE_CLOSE("course_close.wav",false),
		GUIDESE_PG("guide-pg.wav",false),
		GUIDESE_GR("guide-gr.wav",false),
		GUIDESE_GD("guide-gd.wav",false),
		GUIDESE_BD("guide-bd.wav",false),
		GUIDESE_PR("guide-pr.wav",false),
		GUIDESE_MS("guide-ms.wav",false),
		SELECT("select.wav",true), 
		DECIDE("decide.wav",true);
		
		public final boolean isBGM;
		public final String path; 
		
		private SoundType(String path, boolean isBGM) {
			this.path = path;
			this.isBGM = isBGM;
		}
	}	
}