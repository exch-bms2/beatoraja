package bms.player.beatoraja.play.bga;

import java.nio.file.*;
import java.util.Arrays;
import java.util.logging.Logger;

import bms.model.Layer.Sequence;
import bms.model.*;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.ResourcePool;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.play.SkinBGA;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * BGAのリソース管理、描画用クラス
 *
 * @author exch
 */
public class BGAProcessor {
	
	// TODO イベントレイヤー対応(現状はミスレイヤーのみ)

	private PlayerConfig player;
	private float progress = 0;

	private MovieProcessor[] movies = new MovieProcessor[0]; 
	
	private final ResourcePool<String, MovieProcessor> mpgresource;

	public static final String[] mov_extension = { "mp4", "wmv", "m4v", "webm", "mpg", "mpeg", "m1v", "m2v", "avi"};

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
	private long misslayertime;

	private long getMisslayerduration;
	/**
	 * 現在のミスレイヤーシーケンス
	 */
	private Layer misslayer = null;

	private long time;

	private BGImageProcessor cache;

	private Texture blanktex;

	private TimeLine[] timelines;
	private int pos;
	private TextureRegion image;
	private Rectangle tmpRect = new Rectangle();
	
	private boolean rbga;
	private boolean rlayer;

	public BGAProcessor(Config config, PlayerConfig player) {
		this.player = player;

		Pixmap blank = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		blank.setColor(Color.BLACK);
		blank.fill();
		blanktex = new Texture(blank);
		blank.dispose();

		mpgresource = new ResourcePool<String, MovieProcessor>(Math.max(config.getSongResourceGen(), 1)) {
			@Override
			protected MovieProcessor load(String key) {
				FFmpegProcessor mm = new FFmpegProcessor(config.getFrameskip());
				mm.create(key);
				return mm;
			}

			@Override
			protected void dispose(MovieProcessor resource) {
				resource.dispose();
			}
		};
		cache = new BGImageProcessor(256, Math.max(config.getSongResourceGen(), 1));
		image = new TextureRegion();
	}

	public synchronized void setModel(BMSModel model) {
		progress = 0;

		cache.clear();
		resetCurrentlyPlayingBGA();

		int id = 0;

		Array<TimeLine> tls = new Array<TimeLine>();

		if(model != null) {
			for(TimeLine tl : model.getAllTimeLines()) {
				if(tl.getBGA() != -1 || tl.getLayer() != -1 || tl.getEventlayer().length > 0) {
					tls.add(tl);
				}
			}

			// BMS格納ディレクトリ
			Path dpath = Paths.get(model.getPath()).getParent();

			movies = new MovieProcessor[model.getBgaList().length];
			for (String name : model.getBgaList()) {
				if (progress == 1) {
					break;
				}
				Path f = null;
				try {
					if (Files.exists(dpath.resolve(name))) {
						final int index = name.lastIndexOf('.');
						String fex = null;
						if (index != -1) {
							fex = name.substring(index + 1).toLowerCase();
						}
						if (fex != null) {
							if (Arrays.asList(mov_extension).contains(fex)){
								name = name.substring(0, index);
								for (String mov : mov_extension) {
									final Path mpgfile = dpath.resolve(name + "." + mov);
									if (Files.exists(mpgfile)) {
										f = mpgfile;
										break;
									}
								}
							}else if (Arrays.asList(BGImageProcessor.pic_extension).contains(fex)){
								name = name.substring(0, index);
								for (String pic : BGImageProcessor.pic_extension) {
									final Path picfile = dpath.resolve(name + "." + pic);
									if (Files.exists(picfile)) {
										f = picfile;
										break;
									}
								}
							}else{
								f = dpath.resolve(name);
							}
						}
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
				} catch (InvalidPathException e) {
					Logger.getGlobal().warning(e.getMessage());
				}

				if (f != null) {
					boolean isMovie = false;
					for (String mov : mov_extension) {
						if (f.getFileName().toString().toLowerCase().endsWith(mov)) {
							try {
								MovieProcessor mm = mpgresource.get(f.toString());
								movies[id] = mm;
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
		}
		timelines = tls.toArray(TimeLine.class);

		disposeOld();

		Logger.getGlobal().info("BGAファイル読み込み完了。BGA数:" + id);
		progress = 1;
	}

	public void abort() {
		progress = 1;
	}

	public void disposeOld() {
		cache.disposeOld();
		Gdx.app.postRunnable(() -> mpgresource.disposeOld());
	}
	/**
	 * BGAの初期データをあらかじめキャッシュする
	 */
	public void prepare(BMSPlayer player) {
		pos = 0;
		if(cache != null) {
			cache.prepare(timelines);			
		}
		for (MovieProcessor mp : movies) {
			if(mp != null) {
				mp.stop();				
			}
		}
		resetCurrentlyPlayingBGA();
		time = 0;		
	}

	private void resetCurrentlyPlayingBGA() {
		playingbgaid = -1;
		playinglayerid = -1;
		misslayertime = 0;
		misslayer = null;
	}

	private Texture getBGAData(long time, int id, boolean cont) {
		if (progress != 1 || id == -1) {
			return null;
		}

		if(movies[id] != null) {
			if (!cont) {
				movies[id].play(time, false);
			}
			return movies[id].getFrame(time);
		}
		return cache != null ? cache.getTexture(id) : null;
	}
	
	public void prepareBGA(long time) {
		if (time < 0 || timelines == null) {
			this.time = -1;
			return;
		}
		for (int i = pos; i < timelines.length; i++) {
			final TimeLine tl = timelines[i];
			if (tl.getTime() > time) {
				break;
			}

			if (tl.getTime() > this.time) {
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

				final Layer[] eventlayer = tl.getEventlayer();
				
				for(Layer poor : eventlayer) {
					if (poor.event.type == Layer.EventType.MISS) {
						misslayer = poor;
					}					
				}
			} else {
				pos++;
			}
		}
		
		this.time = time;
	}


	public void drawBGA(SkinBGA dst, SkinObjectRenderer sprite, Rectangle r) {
		sprite.setColor(dst.getColor());
		sprite.setBlend(dst.getBlend());
		if (time < 0 || timelines == null) {
			sprite.draw(blanktex, r.x, r.y, r.width, r.height);
			return;
		}

		if (misslayer != null && misslayertime != 0 && time >= misslayertime && time < misslayertime + getMisslayerduration) {
			// draw miss layer
			final Sequence[] seq = misslayer.sequence[0];
			final int index = seq[(int) ((seq.length - 1) * (time - misslayertime) / getMisslayerduration)].id;
			if(index != Integer.MIN_VALUE) {
				Texture miss = getBGAData(time, index, true);
				if (miss != null) {
					sprite.setType(SkinObjectRenderer.TYPE_LINEAR);
					drawBGAFixRatio(dst, sprite, r, miss);
				}				
			}
		} else {
			// draw BGA
			final Texture playingbgatex = getBGAData(time, playingbgaid, rbga);
			rbga = true;
			if (playingbgatex != null) {
				if (movies[playingbgaid] != null) {
					sprite.setType(SkinObjectRenderer.TYPE_FFMPEG);
					drawBGAFixRatio(dst, sprite, r, playingbgatex);
				} else {
					sprite.setType(SkinObjectRenderer.TYPE_LINEAR);
					drawBGAFixRatio(dst, sprite, r, playingbgatex);
				}
			} else {
				sprite.draw(blanktex, r.x, r.y, r.width, r.height);
			}
			// draw layer
			final Texture playinglayertex = getBGAData(time, playinglayerid, rlayer);
			rlayer = true;
			if (playinglayertex != null) {
				if (movies[playinglayerid] != null) {
					sprite.setType(SkinObjectRenderer.TYPE_FFMPEG);
					drawBGAFixRatio(dst, sprite, r, playinglayertex);
				} else {
					sprite.setType(SkinObjectRenderer.TYPE_LAYER);
					drawBGAFixRatio(dst, sprite, r, playinglayertex);
				}
			}
		}
	}
	
	/**
	 * Modify the aspect ratio and draw BGA
	 */
	private void drawBGAFixRatio(SkinBGA dst, SkinObjectRenderer sprite, Rectangle r, Texture bga){
		tmpRect.set(r);
		image.setTexture(bga);
		image.setRegion(0, 0, bga.getWidth(), bga.getHeight());
		dst.getStretch().stretchRect(tmpRect, image, image);
		sprite.draw(image, tmpRect.x, tmpRect.y, tmpRect.width, tmpRect.height);
	}

	/**
	 * ミスレイヤー開始時間を設定する
	 *
	 * @param time
	 *            ミスレイヤー開始時間(ms)
	 */
	public void setMisslayerTme(long time) {
		misslayertime = time;
		getMisslayerduration = player.getMisslayerDuration();
	}

	public void stop() {
		for (MovieProcessor mpg : movies) {
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
