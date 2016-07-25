package bms.player.beatoraja.play.bga;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.model.TimeLine;
import bms.player.beatoraja.Config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Rectangle;

/**
 * BGAのリソース管理、描画用クラス
 * 
 * @author exch
 */
public class BGAProcessor {

	// TODO VLC依存の切り離し

	private BMSModel model;
	private Config config;
	private float progress = 0;

	private Map<Integer, MovieProcessor> mpgmap = new HashMap<Integer, MovieProcessor>();

	/**
	 * backbmp
	 */
	private Pixmap backbmpp;
	private Texture backbmp;
	/**
	 * stagefile
	 */
	private Pixmap stagefilep;
	private Texture stagefile;

	private final String[] mov_extension = { "mpg", "mpeg", "avi", "wmv", "mp4" };
	private final String[] pic_extension = { "jpg", "jpeg", "gif", "bmp", "png" };

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
	private ShaderProgram bgrshader;

	private BGImageManager cache;

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
				+ "{ gl_FragColor = v_color * vec4(c4.r, c4.g, c4.b, 0.0);}"
				+ " else {gl_FragColor = v_color * c4;}\n"
				+ "}";
		layershader = new ShaderProgram(vertex, fragment);

		vertex = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
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

		fragment = "#ifdef GL_ES\n" //
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
				+ "gl_FragColor = v_color * vec4(c4.b, c4.g, c4.r, c4.a);\n"
				+ "}";
		bgrshader = new ShaderProgram(vertex, fragment);
		
		System.out.println(layershader.getLog());
	}

	public void setModel(BMSModel model, String filepath) {
		this.model = model;
		// BMS格納ディレクトリ
		String directorypath = filepath.substring(0, filepath.lastIndexOf(File.separatorChar) + 1);

		dispose();
		progress = 0;

		String stage = model.getStagefile();
		if (stage != null && stage.length() > 0) {
			stagefilep = this.loadPicture(new File(directorypath + stage));
		}
		String back = model.getBackbmp();
		if (back != null && back.length() > 0) {
			backbmpp = this.loadPicture(new File(directorypath + back));
		}

		Pixmap[] bgamap = new Pixmap[model.getBgaList().length];
		int id = 0;
		for (String name : model.getBgaList()) {
			if (progress == 1) {
				break;
			}
			File f = null;
			if (new File(directorypath + name).exists()) {
				f = new File(directorypath + name);
			}
			if (f == null) {
				name = name.substring(0, name.lastIndexOf('.'));
				for (String mov : mov_extension) {
					File mpgfile = new File(directorypath + name + "." + mov);
					if (mpgfile.exists()) {
						f = mpgfile;
						break;
					}
				}
				for (String mov : pic_extension) {
					File picfile = new File(directorypath + name + "." + mov);
					if (picfile.exists()) {
						f = picfile;
						break;
					}
				}
			}

			if (f != null) {
				for (String mov : mov_extension) {
					if (f.getName().endsWith(mov)) {
						try {
							MovieProcessor mm = this.loadMovie(id, f);
							mpgmap.put(id, mm);
							break;
						} catch (Exception e) {
							Logger.getGlobal().warning("BGAファイル読み込み失敗。" + e.getMessage());
							e.printStackTrace();
						} catch (Error e) {
							Logger.getGlobal().severe("BGAファイル読み込み失敗。" + e.getMessage());
							e.printStackTrace();
						}
					}
				}
				bgamap[id] = this.loadPicture(f);
			}

			progress += 1f / model.getBgaList().length;
			id++;
		}
		cache = new BGImageManager(bgamap, BGACACHE_SIZE);
		Logger.getGlobal().info("BGAファイル読み込み完了。BGA数:" + model.getBgaList().length);
		progress = 1;
	}

	private MovieProcessor loadMovie(int id, File f) throws Exception {
		if (config.getMovieplayer() == Config.MOVIEPLAYER_FFMPEG) {
			MovieProcessor mm = new FFmpegProcessor(config.getFrameskip());
			mm.create(f.getPath());
			return mm;
		}
		if (config.getMovieplayer() == Config.MOVIEPLAYER_VLC && config.getVlcpath().length() > 0) {
			MovieProcessor mm = new VLCMovieProcessor(config.getVlcpath());
			mm.create(f.getPath());
			return mm;
		}
		return null;
	}

	private Pixmap loadPicture(File dir) {
		Pixmap tex = null;
		for (String mov : pic_extension) {
			if (dir.getName().endsWith(mov)) {
				try {
					tex = new Pixmap(Gdx.files.internal(dir.getPath()));
//					System.out.println("BGA Picture loaded  : " + dir.getName());
					break;
				} catch (Exception e) {
					Logger.getGlobal().warning("BGAファイル読み込み失敗。" + e.getMessage());
					e.printStackTrace();
				} catch (Error e) {
					Logger.getGlobal().severe("BGAファイル読み込み失敗。" + e.getMessage());
					e.printStackTrace();
				}
			}
		}

		return tex;
	}

	/**
	 * BGAの初期データをあらかじめキャッシュする
	 */
	public void prepare() {
		if (model == null) {
			return;
		}
		cache.prepare(model.getAllTimeLines());
		for(MovieProcessor mp : mpgmap.values()) {
			mp.stop();
		}
		playingbgaid = -1;
		playinglayerid = -1;
	}

	public Texture getBackbmpData() {
		if (backbmpp == null) {
			return null;
		}
		if (backbmp == null) {
			backbmp = new Texture(backbmpp);
		}
		return backbmp;
	}

	public Texture getStagefileData() {
		if (stagefilep == null) {
			return null;
		}
		if (stagefile == null) {
			stagefile = new Texture(stagefilep);
		}
		return stagefile;
	}

	private Texture getBGAData(int id, boolean cont) {
		if (progress != 1 || id == -1) {
			return null;
		}
		if (mpgmap.get(id) != null) {
			return mpgmap.get(id).getBGAData(cont);
		}
		return cache.getTexture(id);
	}
	
	public void drawBGA(SpriteBatch sprite, Rectangle[] region, int time) {
		if(model == null) {
			return;
		}
		final int bgaid = playingbgaid;
		final int layerid = playinglayerid;
		for (TimeLine tl : model.getAllTimeLines()) {
			if (tl.getTime() > time) {
				break;
			}

			if (tl.getTime() > prevrendertime) {
				if (tl.getBGA() != -1) {
					playingbgaid = tl.getBGA();
				}
				if (tl.getLayer() != -1) {
					playinglayerid = tl.getLayer();
				}
				if (tl.getPoor() != null && tl.getPoor().length > 0) {
					misslayer = tl.getPoor();
				}
			}
		}

		if (time < 0 && getBackbmpData() != null) {
			sprite.begin();
			for(Rectangle r : region) {
				sprite.draw(getBackbmpData(), r.x, r.y, r.width, r.height);				
			}
			sprite.end();
		} else if (misslayer != null && misslayertime != 0 && time >= misslayertime
				&& time < misslayertime + 500) {
			// draw miss layer
			Texture miss = getBGAData(misslayer[misslayer.length * (time - misslayertime) / 500], true);
			if (miss != null) {
				sprite.begin();
				for(Rectangle r : region) {
					sprite.draw(miss, r.x, r.y, r.width, r.height);
				}
				sprite.end();
			}
		} else {
			// draw BGA
			Texture playingbgatex = getBGAData(playingbgaid, bgaid == playingbgaid);
			if (playingbgatex != null) {
				sprite.begin();
				if(mpgmap.containsKey(playingbgaid) && bgrshader.isCompiled()) {
					sprite.setShader(bgrshader);
					for(Rectangle r : region) {
						sprite.draw(playingbgatex, r.x, r.y, r.width, r.height);											
					}
					sprite.setShader(null);					
				} else {
					for(Rectangle r : region) {
						sprite.draw(playingbgatex, r.x, r.y, r.width, r.height);						
					}
				}
				sprite.end();
			}
			// draw layer
			Texture playinglayertex = getBGAData(playinglayerid, layerid == playinglayerid);
			if (playinglayertex != null) {
				sprite.begin();
				if (layershader.isCompiled()) {
					sprite.setShader(layershader);					
					for(Rectangle r : region) {
						sprite.draw(playinglayertex, r.x, r.y, r.width, r.height);						
					}
					sprite.setShader(null);
				} else {
					for(Rectangle r : region) {
						sprite.draw(playinglayertex, r.x, r.y, r.width, r.height);						
					}
				}
				sprite.end();
			}
		}
		
		prevrendertime = time;
	}
	
	/**
	 * ミスレイヤー開始時間を設定する
	 * @param time ミスレイヤー開始時間(ms)
	 */
	public void setMisslayerTme(int time) {
		misslayertime = time;
	}

	/**
	 * リソースを開放する
	 */
	public void dispose() {
		if (stagefile != null) {
			stagefile.dispose();
			stagefilep.dispose();
		}
		if (backbmp != null) {
			backbmp.dispose();
			backbmpp.dispose();
		}
		if(cache != null) {
			cache.dispose();			
		}
		for (int id : mpgmap.keySet()) {
			if (mpgmap.get(id) != null) {
				mpgmap.get(id).dispose();
			}
		}
		mpgmap.clear();
	}

	public float getProgress() {
		return progress;
	}

	public void forceFinish() {
		progress = 1;
	}
}

/**
 * BGIリソース管理用クラス
 * 
 * @author exch
 */
class BGImageManager {
	
	private Pixmap[] bgamap;
	/**
	 * BGイメージのキャッシュ
	 */
	private Texture[] bgacache;
	private int[] bgacacheid;
	
	public BGImageManager(Pixmap[] pixmap, int size) {
		this.bgamap = pixmap;
		bgacache = new Texture[size];
		bgacacheid = new int[size];
		Arrays.fill(bgacacheid, -1);
	}
	
	/**
	 * BGAの初期データをあらかじめキャッシュする
	 */
	public void prepare(TimeLine[] timelines) {
		long l = System.currentTimeMillis();
		int count = 0;
		for (TimeLine tl : timelines) {
			int bga = tl.getBGA();
			if (bga != -1 && bgacache[bga % bgacache.length] == null) {
				Pixmap pix = bgamap[bga];
				if (pix != null) {
					bgacache[bga % bgacache.length] = new Texture(pix);
					bgacacheid[bga % bgacache.length] = bga;
					count++;
				}
			}
			bga = tl.getLayer();
			if (bga != -1 && bgacache[bga % bgacache.length] == null) {
				Pixmap pix = bgamap[bga];
				if (pix != null) {
					bgacache[bga % bgacache.length] = new Texture(pix);
					bgacacheid[bga % bgacache.length] = bga;
					count++;
				}
			}
		}
		Logger.getGlobal().info(
				"BGAデータの事前Texture化 - BGAデータ数:" + count + " time(ms):" + (System.currentTimeMillis() - l));
	}

	public Texture getTexture(int id) {
		if (bgacacheid[id % bgacache.length] == id) {
			return bgacache[id % bgacache.length];
		}
		if (bgacache[id % bgacache.length] != null) {
			bgacache[id % bgacache.length].dispose();
		}
		Pixmap pix = bgamap[id];
		if (pix != null) {
			bgacache[id % bgacache.length] = new Texture(pix);
			bgacacheid[id % bgacache.length] = id;
			return bgacache[id % bgacache.length];
		}
		return null;

	}
	/**
	 * リソースを開放する
	 */
	public void dispose() {
		for (Texture bga : bgacache) {
			if (bga != null) {
				bga.dispose();
			}
		}
		bgacache = new Texture[0];
		for (Pixmap id : bgamap) {
			if (id != null) {
				id.dispose();
			}
		}
		bgamap = new Pixmap[0];
	}
}
