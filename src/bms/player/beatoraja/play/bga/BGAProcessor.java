package bms.player.beatoraja.play.bga;

import static bms.player.beatoraja.skin.SkinProperty.TIMER_PLAY;

import java.nio.file.*;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.model.TimeLine;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.ResourcePool;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

/**
 * BGAのリソース管理、描画用クラス
 *
 * @author exch
 */
public class BGAProcessor {

	private BMSModel model;
	private Config config;
	private PlayerConfig player;
	private float progress = 0;

	private IntMap<MovieProcessor> mpgmap = new IntMap<MovieProcessor>();
	
	private ResourcePool<String, MovieProcessor> mpgresource = new ResourcePool<String, MovieProcessor>(1) {

		@Override
		protected MovieProcessor load(String key) {
			if (config.getMovieplayer() == Config.MOVIEPLAYER_FFMPEG) {
				MovieProcessor mm = new FFmpegProcessor(config.getFrameskip());
				mm.create(key);
				return mm;
			}
			if (config.getMovieplayer() == Config.MOVIEPLAYER_VLC && config.getVlcpath().length() > 0) {
				MovieProcessor mm = new VLCMovieProcessor(config.getVlcpath());
				mm.create(key);
				return mm;
			}
			return null;
		}

		@Override
		protected void dispose(MovieProcessor resource) {
			resource.dispose();
		}

	};

	public static final String[] mov_extension = { "mpg", "mpeg", "m1v", "m2v", "m4v", "avi", "wmv", "mp4" };

	/**
	 * BGAイメージのキャッシュ枚数
	 */
	private static final int BGACACHE_SIZE = 256;

	/**
	 * 再生中のBGAID
	 */
	private int playingbgaid = -1;
	/**
	 * 再生中のレイヤーID
	 */
	private int playinglayerid = -1;
	/**
	 * ミスレイヤー表示開始時間
	 */
	private int misslayertime;

	private int getMisslayerduration;
	/**
	 * 現在のミスレイヤーシーケンス
	 */
	private int[] misslayer = null;

	private int prevrendertime;

	private BGImageProcessor cache;

	private Texture blanktex;

	private TimeLine[] timelines;
	private int pos;

	public BGAProcessor(Config config, PlayerConfig player) {
		this.config = config;
		this.player = player;

		Pixmap blank = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		blank.setColor(Color.BLACK);
		blank.fill();
		blanktex = new Texture(blank);
		
		cache = new BGImageProcessor(BGACACHE_SIZE);
	}

	public synchronized void setModel(BMSModel model) {
		this.model = model;
		Array<TimeLine> tls = new Array<TimeLine>();
		for(TimeLine tl : model.getAllTimeLines()) {
			if(tl.getBGA() != -1 || tl.getLayer() != -1 || (tl.getPoor() != null && tl.getPoor().length > 0)) {
				tls.add(tl);
			}
		}
		timelines = tls.toArray(TimeLine.class);

		// BMS格納ディレクトリ
		Path dpath = Paths.get(model.getPath()).getParent();

		progress = 0;

		mpgmap.clear();
		int id = 0;
		cache.clear();

		for (String name : model.getBgaList()) {
			if (progress == 1) {
				break;
			}
			Path f = null;
			if (Files.exists(dpath.resolve(name))) {
				f = dpath.resolve(name);
			}
			if (f == null) {
				final int index = name.lastIndexOf('.');
				if (index != -1) {
					name = name.substring(0, index);
				}
				for (String mov : mov_extension) {
					final Path mpgfile = dpath.resolve(name + "." + mov);
					if (Files.exists(mpgfile)) {
						f = mpgfile;
						break;
					}
				}
				for (String mov : BGImageProcessor.pic_extension) {
					final Path picfile = dpath.resolve(name + "." + mov);
					if (Files.exists(picfile)) {
						f = picfile;
						break;
					}
				}
			}

			if (f != null) {
				boolean isMovie = false;
				for (String mov : mov_extension) {
					if (f.getFileName().toString().toLowerCase().endsWith(mov)) {
						try {
							MovieProcessor mm = mpgresource.get(f.toString());
							mpgmap.put(id, mm);
							isMovie = true;
							break;
						} catch (Throwable e) {
							Logger.getGlobal().warning("BGAファイル読み込み失敗。" + e.getMessage());
							e.printStackTrace();
						}					
					}
				}
				if(isMovie) {
				} else {
					cache.put(id, f);					
				}
			}

			progress += 1f / model.getBgaList().length;
			id++;
		}
		
		cache.disposeOld();
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				mpgresource.disposeOld();
			}			
		});

		Logger.getGlobal().info("BGAファイル読み込み完了。BGA数:" + model.getBgaList().length);
		progress = 1;
	}

	public void abort() {
		progress = 1;
	}

	/**
	 * BGAの初期データをあらかじめキャッシュする
	 */
	public void prepare(BMSPlayer player) {
		if (model == null) {
			return;
		}
		pos = 0;
		if(cache != null) {
			cache.prepare(timelines);			
		}
		for (MovieProcessor mp : mpgmap.values()) {
			mp.stop();				
			if (mp instanceof FFmpegProcessor) {
				((FFmpegProcessor) mp).setTimerObserver(() -> {
					return (player.getNowTime() - player.getTimer()[TIMER_PLAY]) * 1000;
				});
			}
		}
		playingbgaid = -1;
		playinglayerid = -1;
		misslayertime = 0;
		misslayer = null;
		prevrendertime = 0;		
	}

	private Texture getBGAData(int id, boolean cont) {
		if (progress != 1 || id == -1) {
			return null;
		}

		MovieProcessor mp = getMovieProcessor(id);
		if(mp != null) {
			if (!cont) {
				mp.play(false);
			}
			return mp.getFrame();			
		}
		return cache != null ? cache.getTexture(id) : null;
	}

	public void drawBGA(SkinObjectRenderer sprite, Rectangle r, int time) {
		if (time < 0 || timelines == null) {
			prevrendertime = -1;
			sprite.draw(blanktex, r.x, r.y, r.width, r.height);
			return;
		}
		boolean rbga = true;
		boolean rlayer = true;
		for (int i = pos; i < timelines.length; i++) {
			final TimeLine tl = timelines[i];
			if (tl.getTime() > time) {
				break;
			}

			if (tl.getTime() > prevrendertime) {
				final int bga = tl.getBGA();
				if (bga == -2) {
					playingbgaid = -1;
					rbga = false;
				} else if (bga >= 0) {
					playingbgaid = bga;
					rbga = false;
				}
				
				final int layer = tl.getLayer();
				if (layer == -2) {
					playinglayerid = -1;
					rlayer = false;
				} else if (layer >= 0) {
					playinglayerid = layer;
					rlayer = false;
				}

				final int[] poor = tl.getPoor();
				if (poor != null && poor.length > 0) {
					misslayer = poor;
				}
			} else {
				pos++;
			}
		}

		if (misslayer != null && misslayertime != 0 && time >= misslayertime && time < misslayertime + getMisslayerduration) {
			// draw miss layer
			Texture miss = getBGAData(misslayer[misslayer.length * (time - misslayertime) / getMisslayerduration], true);
			if (miss != null) {
				sprite.setType(SkinObjectRenderer.TYPE_LINEAR);
				drawBGAFixRatio(sprite, r, miss);
			}
		} else {
			// draw BGA
			final Texture playingbgatex = getBGAData(playingbgaid, rbga);
			if (playingbgatex != null) {
				final MovieProcessor mp = getMovieProcessor(playingbgaid);
				if (mp != null) {
					sprite.setType(SkinObjectRenderer.TYPE_FFMPEG);
					drawBGAFixRatio(sprite, r, playingbgatex);
				} else {
					sprite.setType(SkinObjectRenderer.TYPE_LINEAR);
					drawBGAFixRatio(sprite, r, playingbgatex);
				}
			} else {
				sprite.draw(blanktex, r.x, r.y, r.width, r.height);
			}
			// draw layer
			final Texture playinglayertex = getBGAData(playinglayerid, rlayer);
			if (playinglayertex != null) {
				final MovieProcessor mp = getMovieProcessor(playinglayerid);
				if (mp != null) {
					sprite.setType(SkinObjectRenderer.TYPE_FFMPEG);
					drawBGAFixRatio(sprite, r, playinglayertex);
				} else {
					sprite.setType(SkinObjectRenderer.TYPE_LAYER);
					drawBGAFixRatio(sprite, r, playinglayertex);
				}
			}
		}

		prevrendertime = time;
	}
	
	private MovieProcessor getMovieProcessor(int id) {
		return mpgmap.get(id);
	}

	/**
	 * Modify the aspect ratio and draw BGA
	 */
	private void drawBGAFixRatio(SkinObjectRenderer sprite, Rectangle r, Texture bga){
		switch(config.getBgaExpand()) {
		case Config.BGAEXPAND_FULL:
	        sprite.draw(bga, r.x, r.y, r.width, r.height);
			break;
		case Config.BGAEXPAND_KEEP_ASPECT_RATIO:
			float fixx,fixy,fixheight,fixwidth;
			float movieaspect = (float)bga.getWidth() / bga.getHeight();
			float windowaspect = (float)r.width / r.height;
			float scaleheight = (float)windowaspect / movieaspect;
			float scalewidth  = (float)1.0f / scaleheight;
	        if(1.0f > scaleheight){
	        	fixx = r.x;
	            fixy = r.y+ (r.height * (1.0f - scaleheight)) / 2.0f;
	            fixheight = r.height * scaleheight;
	            fixwidth = r.width;
	        } else {
	            fixx = r.x+(r.width * (1.0f - scalewidth)) / 2.0f;
	            fixy = r.y;
	            fixheight = r.height;
	            fixwidth = r.width * scalewidth;
	        }
	        sprite.draw(bga, fixx, fixy, fixwidth, fixheight);
			break;
		case Config.BGAEXPAND_OFF:
            float w = Math.min(r.width, bga.getWidth());
            float h = Math.min(r.height, bga.getHeight());
	       	float x = r.x + (r.width - w) / 2;
            float y = r.y + (r.height - h) / 2;;
	        sprite.draw(bga, x, y, w, h);
			break;
		}
	}

	/**
	 * ミスレイヤー開始時間を設定する
	 *
	 * @param time
	 *            ミスレイヤー開始時間(ms)
	 */
	public void setMisslayerTme(int time) {
		misslayertime = time;
		getMisslayerduration = player.getMisslayerDuration();
	}

	public void stop() {
		for (MovieProcessor mpg : mpgmap.values()) {
			if (mpg != null) {
				mpg.stop();
			}
		}
	}

	/**
	 * リソースを開放する
	 */
	public void dispose() {
		if (cache != null) {
			cache.dispose();
		}
		mpgresource.dispose();
	}

	public float getProgress() {
		return progress;
	}
}
