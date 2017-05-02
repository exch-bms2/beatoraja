package bms.player.beatoraja.play.bga;

import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.model.TimeLine;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.play.BMSPlayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Rectangle;

/**
 * BGAのリソース管理、描画用クラス
 *
 * @author exch
 */
public class BGAProcessor {

	private BMSModel model;
	private Config config;
	private float progress = 0;

	private int[] mpgid = new int[0];
	private MovieProcessor[] mpgmap = new MovieProcessor[0];

	/**
	 * backbmp
	 */
	private TextureRegion backbmp;
	/**
	 * stagefile
	 */
	private TextureRegion stagefile;

	public static final String[] mov_extension = { "mpg", "mpeg", "m1v", "m2v", "avi", "wmv", "mp4" };

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
	/**
	 * 現在のミスレイヤーシーケンス
	 */
	private int[] misslayer = null;

	private int prevrendertime;
	/**
	 * レイヤー描画用シェーダ
	 */
	private ShaderProgram layershader;

	private BGImageProcessor cache;

	private Texture blanktex;

	private TimeLine[] timelines;
	private int pos;

	public BGAProcessor(Config config) {
		this.config = config;

		String vertex = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
				+ "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
				+ "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
				+ "uniform mat4 u_projTrans;\n" //
				+ "varying vec4 v_color;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "\n" //
				+ "void main()\n" //
				+ "{\n" //
				+ "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
				+ "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
				+ "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
				+ "}\n";

		String fragment = "#ifdef GL_ES\n" //
				+ "#define LOWP lowp\n" //
				+ "precision mediump float;\n" //
				+ "#else\n" //
				+ "#define LOWP \n" //
				+ "#endif\n" //
				+ "varying LOWP vec4 v_color;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "uniform sampler2D u_texture;\n" //
				+ "void main()\n"//
				+ "{\n" //
				+ "    vec4 c4 = texture2D(u_texture, v_texCoords);\n"
				+ "    if(c4.r == 0.0 && c4.g == 0.0 && c4.b == 0.0) "
				+ "{ gl_FragColor = v_color * vec4(c4.r, c4.g, c4.b, 0.0);}" + " else {gl_FragColor = v_color * c4;}\n"
				+ "}";
		layershader = new ShaderProgram(vertex, fragment);

		System.out.println(layershader.getLog());

		Pixmap blank = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		blank.setColor(Color.BLACK);
		blank.fill();
		blanktex = new Texture(blank);
		
		cache = new BGImageProcessor(BGACACHE_SIZE);
	}

	public synchronized void setModel(BMSModel model) {
		this.model = model;
		timelines = model.getAllTimeLines();
		// BMS格納ディレクトリ
		Path dpath = Paths.get(model.getPath()).getParent();

		progress = 0;

		final MovieProcessor[] oldmpgmap = mpgmap;
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				for (MovieProcessor mpg : oldmpgmap) {
					if (mpg != null) {
						mpg.dispose();
					}
				}
				if(stagefile != null) {
					stagefile.getTexture().dispose();
					stagefile = null;
				}
				String stage = model.getStagefile();
				if (stage != null && stage.length() > 0) {
					Path p = dpath.resolve(stage);
					if(Files.exists(p)) {
						Pixmap pix = BGImageProcessor.loadPicture(p);
						if(pix != null) {
							stagefile = new TextureRegion(new Texture(pix));
							pix.dispose();
						}
					}
				}
				
				if(backbmp != null) {
					backbmp.getTexture().dispose();
					backbmp = null;
				}
				String back = model.getBackbmp();
				if (back != null && back.length() > 0) {
					Path p = dpath.resolve(back);
					if(Files.exists(p)) {
						Pixmap pix = BGImageProcessor.loadPicture(p);
						if(pix != null) {
							backbmp = new TextureRegion(new Texture(pix));
							pix.dispose();
						}
					}
				}
			}			
		});

		mpgid = new int[0];
		
		Map<Integer, MovieProcessor> mpgmap = new HashMap<Integer, MovieProcessor>();
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
							MovieProcessor mm = this.loadMovie(id, f);
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
		mpgid = new int[mpgmap.size()];
		this.mpgmap = new MovieProcessor[mpgmap.size()];
		int i = 0;
		for(Map.Entry<Integer, MovieProcessor> e : mpgmap.entrySet()) {
			mpgid[i] = e.getKey();
			this.mpgmap[i] = e.getValue();
			i++;
		}
		Logger.getGlobal().info("BGAファイル読み込み完了。BGA数:" + model.getBgaList().length);
		progress = 1;
	}

	private MovieProcessor loadMovie(int id, Path p) throws Exception {
		if (config.getMovieplayer() == Config.MOVIEPLAYER_FFMPEG) {
			MovieProcessor mm = new FFmpegProcessor(config.getFrameskip());
			mm.create(p.toString());
			return mm;
		}
		if (config.getMovieplayer() == Config.MOVIEPLAYER_VLC && config.getVlcpath().length() > 0) {
			MovieProcessor mm = new VLCMovieProcessor(config.getVlcpath());
			mm.create(p.toString());
			return mm;
		}
		return null;
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
		for (MovieProcessor mp : mpgmap) {
			mp.stop();				
		}
		playingbgaid = -1;
		playinglayerid = -1;

		for(int i = 0;i < mpgid.length;i++) {
			if (mpgmap[i] instanceof FFmpegProcessor) {
				((FFmpegProcessor) mpgmap[i]).setBMSPlayer(player);
			}
		}
	}

	public TextureRegion getBackbmpData() {
		return backbmp;
	}

	public TextureRegion getStagefileData() {
		return stagefile;
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

	public void drawBGA(SpriteBatch sprite, Rectangle r, int time) {
		if (timelines == null) {
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
				if (tl.getBGA() == -2) {
					playingbgaid = -1;
					rbga = false;
				} else if (tl.getBGA() >= 0) {
					playingbgaid = tl.getBGA();
					rbga = false;
				}
				if (tl.getLayer() == -2) {
					playinglayerid = -1;
					rlayer = false;
				} else if (tl.getLayer() >= 0) {
					playinglayerid = tl.getLayer();
					rlayer = false;
				}

				if (tl.getPoor() != null && tl.getPoor().length > 0) {
					misslayer = tl.getPoor();
				}
			} else {
				pos++;
			}
		}

		if (time < 0) {
			// draw backbmp
			if (getBackbmpData() != null) {
				drawBGAFixRatio(sprite, r, getBackbmpData().getTexture());
			}
		} else if (misslayer != null && misslayertime != 0 && time >= misslayertime && time < misslayertime + 500) {
			// draw miss layer
			Texture miss = getBGAData(misslayer[misslayer.length * (time - misslayertime) / 500], true);
			if (miss != null) {
				miss.setFilter(TextureFilter.Linear, TextureFilter.Linear);
				drawBGAFixRatio(sprite, r, miss);
			}
		} else {
			// draw BGA
			final Texture playingbgatex = getBGAData(playingbgaid, rbga);
			if (playingbgatex != null) {
				final MovieProcessor mp = getMovieProcessor(playingbgaid);
				playingbgatex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
				if (mp != null) {
					final ShaderProgram shader = mp.getShader();
					sprite.setShader(shader);
					drawBGAFixRatio(sprite, r, playingbgatex);
					sprite.setShader(null);
				} else {
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
					playinglayertex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
					final ShaderProgram shader = mp.getShader();
					sprite.setShader(shader);
					drawBGAFixRatio(sprite, r, playinglayertex);
					sprite.setShader(null);
				} else if (layershader.isCompiled()) {
					sprite.setShader(layershader);
					drawBGAFixRatio(sprite, r, playinglayertex);
					sprite.setShader(null);
				} else {
					drawBGAFixRatio(sprite, r, playinglayertex);
				}
			}
		}

		prevrendertime = time;
	}
	
	private MovieProcessor getMovieProcessor(int id) {
		for(int i = 0;i < mpgid.length;i++) {
			if(mpgid[i] == id) {
				return mpgmap[i];
			}
		}
		return null;
	}

	/**
	 * Modify the aspect ratio and draw BGA
	 */
	private void drawBGAFixRatio(SpriteBatch sprite, Rectangle r, Texture bga){
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
	}

	public void stop() {
		for (MovieProcessor mpg : mpgmap) {
			if (mpg != null) {
				mpg.stop();
			}
		}
	}

	/**
	 * リソースを開放する
	 */
	public void dispose() {
		if (stagefile != null) {
			stagefile.getTexture().dispose();
		}
		if (backbmp != null) {
			backbmp.getTexture().dispose();
		}
		if (cache != null) {
			cache.dispose();
		}
		for (MovieProcessor mpg : mpgmap) {
			if (mpg != null) {
				mpg.dispose();
			}
		}
		mpgid = new int[0];

		try {
			layershader.dispose();
		} catch(Throwable e) {

		}
	}

	public float getProgress() {
		return progress;
	}
}
