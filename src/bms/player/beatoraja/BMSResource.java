package bms.player.beatoraja;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.audio.AudioDriver;
import bms.player.beatoraja.play.bga.BGAProcessor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * BMSの音源、BGAリソースを管理するクラス
 *
 * @author exch
 */
public class BMSResource {

 	/**
	 * 選曲中のBMS
	 */
	private BMSModel model;
	/**
	 * BMSの音源リソース
	 */
	private AudioDriver audio;
	/**
	 * 音源読み込みタスク
	 */
	private ArrayDeque<AudioLoaderThread> audioloaders = new ArrayDeque<AudioLoaderThread>();
	/**
	 * BMSのBGAリソース
	 */
	private BGAProcessor bga;

	private boolean bgaon;
	/**
	 * BGA読み込みタスク
	 */
	private ArrayDeque<BGALoaderThread> bgaloaders = new ArrayDeque<BGALoaderThread>();
	/**
	 * backbmp
	 */
	private TextureRegion backbmp;
	/**
	 * stagefile
	 */
	private TextureRegion stagefile;

	private Pixmap stagefilePix;

	/**
	 * stagefile
	 */
	private TextureRegion banner;

	private Pixmap bannerPix;

	public BMSResource(AudioDriver audio, Config config, PlayerConfig player) {
		this.audio = audio;
		bga = new BGAProcessor(config, player);
	}

	public boolean setBMSFile(BMSModel model, final Path f, final Config config, PlayMode mode) {
		if(stagefile != null) {
			stagefile.getTexture().dispose();
			stagefile = null;
		}
		try {
			Pixmap pix = PixmapResourcePool.loadPicture(f.getParent().resolve(model.getStagefile()).toString());
			if(pix != null) {
				stagefile = new TextureRegion(new Texture(pix));
				pix.dispose();
			}
		} catch(Throwable e) {
			Logger.getGlobal().warning(e.getMessage());
		}

		if(backbmp != null) {
			backbmp.getTexture().dispose();
			backbmp = null;
		}
		try {
			Pixmap pix = PixmapResourcePool.loadPicture(f.getParent().resolve(model.getBackbmp()).toString());
			if(pix != null) {
				backbmp = new TextureRegion(new Texture(pix));
				pix.dispose();
			}
		} catch(Throwable e) {
			Logger.getGlobal().warning(e.getMessage());
		}

		this.model = model;
		while(!audioloaders.isEmpty() && !audioloaders.getFirst().isAlive()) {
			audioloaders.removeFirst();
		}
		while(!bgaloaders.isEmpty() && !bgaloaders.getFirst().isAlive()) {
			bgaloaders.removeFirst();
		}
		
		if(MainLoader.getIllegalSongCount() == 0) {
			// Audio, BGAともキャッシュがあるため、何があっても全リロードする
			BGALoaderThread bgaloader = new BGALoaderThread(
					config.getBga() == Config.BGA_ON || (config.getBga() == Config.BGA_AUTO && (mode == PlayMode.AUTOPLAY || mode.isReplayMode())) ? model : null);
			bgaloaders.addLast(bgaloader);
			bgaloader.start();
			AudioLoaderThread audioloader = new AudioLoaderThread(model);
			audioloaders.addLast(audioloader);
			audioloader.start();			
		}
		return true;
	}

	public AudioDriver getAudioDriver() {
		return audio;
	}

	public BGAProcessor getBGAProcessor() {
		return bga;
	}

	public boolean isBGAOn() {
		return bgaon;
	}

	public boolean mediaLoadFinished() {
		if(!audioloaders.isEmpty() && audioloaders.getLast().isAlive()) {
			return false;
		}
		if(!bgaloaders.isEmpty() && bgaloaders.getLast().isAlive()) {
			return false;
		}
		return true;
	}

	public TextureRegion getBackbmp() {
		return backbmp;
	}

	public TextureRegion getStagefile() {
		return stagefile;
	}

	public TextureRegion getBanner() {
		return banner;
	}

	public void setStagefile(Pixmap pixmap) {
		final TextureRegion oldstagefile = stagefile;
		if (pixmap != null) {
			if(stagefilePix != pixmap) {
				stagefile = new TextureRegion(new Texture(pixmap));
				stagefilePix = pixmap;
			}
		} else {
			stagefile = null;
			stagefilePix = null;
		}
		if (oldstagefile != stagefile && oldstagefile != null) {
			oldstagefile.getTexture().dispose();
		}
	}

	public void setBanner(Pixmap pixmap) {
		final TextureRegion oldbanner = banner;
		if (pixmap != null) {
			if(bannerPix != pixmap) {
				banner = new TextureRegion(new Texture(pixmap));
				bannerPix = pixmap;
			}
		} else {
			banner = null;
			bannerPix = null;
		}
		if (oldbanner != banner && oldbanner != null) {
			oldbanner.getTexture().dispose();
		}
	}

	public void dispose() {
		if (audio != null) {
			audio.dispose();
			audio = null;
		}
		if (bga != null) {
			bga.dispose();
			bga = null;
		}
		if(stagefile != null) {
			stagefile.getTexture().dispose();
			stagefile = null;
		}
		if(backbmp != null) {
			backbmp.getTexture().dispose();
			backbmp = null;
		}
	}

	class BGALoaderThread extends Thread {

		private final BMSModel model;

		public BGALoaderThread(BMSModel model) {
			this.model = model;
		}

		@Override
		public void run() {
			try {
				bga.abort();
				bga.setModel(model);
				bgaon = model != null;
			} catch (Throwable e) {
				Logger.getGlobal().severe(e.getClass().getName() + " : " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	class AudioLoaderThread extends Thread {

		private final BMSModel model;

		public AudioLoaderThread(BMSModel model) {
			this.model = model;
		}

		@Override
		public void run() {
			try {
				audio.abort();
				audio.setModel(model);
			} catch (Throwable e) {
				Logger.getGlobal().severe(e.getClass().getName() + " : " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
 }
