package bms.player.beatoraja;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.audio.AudioDriver;
import bms.player.beatoraja.play.bga.BGAProcessor;
import bms.player.beatoraja.select.bar.SongBar;
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
	 * BGAオプション
	 */
	private int bgashow;
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
		String bmspath = this.model != null ? this.model.getPath() : null;

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
		if (bmspath == null || !f.toAbsolutePath().toString().equals(bmspath) || bgashow != config.getBga()
				|| (model.getRandom() != null && model.getRandom().length > 0)) {
			// 前回と違うbmsファイルを読み込んだ場合、BGAオプション変更時はリソースのロード
			// 同フォルダの違うbmsファイルでも、WAV/,BMP定義が違う可能性があるのでロード
			// RANDOM定義がある場合はリロード
			this.bgashow = config.getBga();			
			
			if (config.getBga() == Config.BGA_ON || (config.getBga() == Config.BGA_AUTO && (mode == PlayMode.AUTOPLAY || mode.isReplayMode()))) {
				BGALoaderThread bgaloader = new BGALoaderThread(model, bga);
				bgaloaders.addLast(bgaloader);
				bgaloader.start();					
			}
			AudioLoaderThread audioloader = new AudioLoaderThread(model, audio);
			audioloaders.addLast(audioloader);
			audioloader.start();
		} else {
			// windowsだけ動画を含むBGAがあれば読み直す(ffmpegがエラー終了する。今後のupdateで直れば外す)
//			if ("\\".equals(System.getProperty("file.separator"))) {
				Logger.getGlobal().info("WindowsのためBGA再読み込み");
				
				if (config.getBga() == Config.BGA_ON || (config.getBga() == Config.BGA_AUTO && (mode == PlayMode.AUTOPLAY || mode.isReplayMode()))) {
					BGALoaderThread bgaloader = new BGALoaderThread(model, bga);
					bgaloaders.addLast(bgaloader);
					bgaloader.start();					
				}
//			}
		}
		return true;
	}
	
	public BGAProcessor getBGAProcessor() {
		return bga;
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

	static class BGALoaderThread extends Thread {

		private final BMSModel model;
		private final BGAProcessor bga;
		
		public BGALoaderThread(BMSModel model, BGAProcessor bga) {
			this.model = model;
			this.bga = bga;
		}
		
		@Override
		public void run() {
			try {
				bga.abort();
				bga.setModel(model);
			} catch (Throwable e) {
				Logger.getGlobal().severe(e.getClass().getName() + " : " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	static class AudioLoaderThread extends Thread {

		private final BMSModel model;
		private final AudioDriver audio;
		
		public AudioLoaderThread(BMSModel model, AudioDriver audio) {
			this.model = model;
			this.audio = audio;
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
