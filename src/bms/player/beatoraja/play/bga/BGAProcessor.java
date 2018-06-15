package bms.player.beatoraja.play.bga;

import static bms.player.beatoraja.skin.SkinProperty.TIMER_PLAY;

import java.nio.file.*;
import java.util.Arrays;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.model.TimeLine;
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
	private int[] misslayer = null;

	private long prevrendertime;

	private BGImageProcessor cache;

	private Texture blanktex;

	private TimeLine[] timelines;
	private int pos;
	private TextureRegion image;
	private Rectangle tmpRect = new Rectangle();

	public BGAProcessor(Config config, PlayerConfig player) {
		this.config = config;
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
		this.model = model;
		progress = 0;

		mpgmap.clear();
		cache.clear();

		int id = 0;

		Array<TimeLine> tls = new Array<TimeLine>();

		if(model != null) {
			for(TimeLine tl : model.getAllTimeLines()) {
				if(tl.getBGA() != -1 || tl.getLayer() != -1 || (tl.getPoor() != null && tl.getPoor().length > 0)) {
					tls.add(tl);
				}
			}

			// BMS格納ディレクトリ
			Path dpath = Paths.get(model.getPath()).getParent();

			for (String name : model.getBgaList()) {
				if (progress == 1) {
					break;
				}
				Path f = null;
				if (Files.exists(dpath.resolve(name))) {
					final int index = name.lastIndexOf('.');
					String fex = null;
					if (index != -1) {
						fex = name.substring(index + 1).toLowerCase();
					}
					if(fex != null && !(Arrays.asList(mov_extension).contains(fex))){
						f = dpath.resolve(name);
					}else if(fex != null){
						name = name.substring(0, index);
						for (String mov : mov_extension) {
							final Path mpgfile = dpath.resolve(name + "." + mov);
							if (Files.exists(mpgfile)) {
								f = mpgfile;
								break;
							}
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
		for (MovieProcessor mp : mpgmap.values()) {
			mp.stop();				
		}
		playingbgaid = -1;
		playinglayerid = -1;
		misslayertime = 0;
		misslayer = null;
		prevrendertime = 0;		
	}

	private Texture getBGAData(long time, int id, boolean cont) {
		if (progress != 1 || id == -1) {
			return null;
		}

		MovieProcessor mp = getMovieProcessor(id);
		if(mp != null) {
			if (!cont) {
				mp.play(time, false);
			}
			return mp.getFrame(time);
		}
		return cache != null ? cache.getTexture(id) : null;
	}

	public void drawBGA(SkinBGA dst, SkinObjectRenderer sprite, Rectangle r, long time) {
		sprite.setColor(dst.getColor());
		sprite.setBlend(dst.getBlend());
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
			Texture miss = getBGAData(time, misslayer[(int) (misslayer.length * (time - misslayertime) / getMisslayerduration)], true);
			if (miss != null) {
				sprite.setType(SkinObjectRenderer.TYPE_LINEAR);
				drawBGAFixRatio(dst, sprite, r, miss);
			}
		} else {
			// draw BGA
			final Texture playingbgatex = getBGAData(time, playingbgaid, rbga);
			if (playingbgatex != null) {
				final MovieProcessor mp = getMovieProcessor(playingbgaid);
				if (mp != null) {
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
			if (playinglayertex != null) {
				final MovieProcessor mp = getMovieProcessor(playinglayerid);
				if (mp != null) {
					sprite.setType(SkinObjectRenderer.TYPE_FFMPEG);
					drawBGAFixRatio(dst, sprite, r, playinglayertex);
				} else {
					sprite.setType(SkinObjectRenderer.TYPE_LAYER);
					drawBGAFixRatio(dst, sprite, r, playinglayertex);
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
	private void drawBGAFixRatio(SkinBGA dst, SkinObjectRenderer sprite, Rectangle r, Texture bga){
		tmpRect.set(r);
		image.setTexture(bga);
		image.setRegion(0, 0, bga.getWidth(), bga.getHeight());
		dst.getStretchedRect(tmpRect, image, image);
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
