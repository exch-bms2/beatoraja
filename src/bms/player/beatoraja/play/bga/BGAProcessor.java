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
 * BGAのリソース管理用クラス
 * 
 * @author exch
 */
public class BGAProcessor {

	// TODO VLC依存の切り離し

	private BMSModel model;
	private Config config;
	private float progress = 0;

	private Pixmap[] bgamap = new Pixmap[0];
	private Map<Integer, MovieProcessor> mpgmap = new HashMap<Integer, MovieProcessor>();

	private Pixmap backbmpp;
	private Texture backbmp;
	private Pixmap stagefilep;
	private Texture stagefile;

	private final String[] mov_extension = { "mpg", "mpeg", "avi", "wmv" };
	private final String[] pic_extension = { "jpg", "jpeg", "gif", "bmp", "png" };

	private Texture[] bgacache = new Texture[256];
	private int[] bgacacheid = new int[256];
	
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

	private ShaderProgram layershader;

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

		System.out.println(layershader.getLog());
	}

	public void setModel(BMSModel model, String filepath) {
		this.model = model;
		Arrays.fill(bgacacheid, -1);
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

		bgamap = new Pixmap[model.getBgaList().length];
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
		Logger.getGlobal().info("BGAファイル読み込み完了。BGA数:" + model.getBgaList().length);
		progress = 1;
	}

	private MovieProcessor loadMovie(int id, File f) throws Exception {
		if (config.getVlcpath().length() > 0) {
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
		long l = System.currentTimeMillis();
		int count = 0;
		for (TimeLine tl : model.getAllTimeLines()) {
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

	public Texture getBGAData(int id) {
		if (progress != 1 || id == -1) {
			return null;
		}
		if (mpgmap.get(id) != null) {
			return mpgmap.get(id).getBGAData();
		}
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
	
	public void drawBGA(SpriteBatch sprite, Rectangle r, int time) {
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
			sprite.draw(getBackbmpData(), r.x, r.y, r.width, r.height);
			sprite.end();
		} else if (misslayer != null && misslayertime != 0 && time >= misslayertime
				&& time < misslayertime + 500) {
			// draw miss layer
			Texture miss = getBGAData(misslayer[misslayer.length * (time - misslayertime) / 500]);
			if (miss != null) {
				sprite.begin();
				sprite.draw(miss, r.x, r.y, r.width, r.height);
				sprite.end();
			}
		} else {
			// draw BGA
			Texture playingbgatex = getBGAData(playingbgaid);
			if (playingbgatex != null) {
				sprite.begin();
				sprite.draw(playingbgatex, r.x, r.y, r.width, r.height);
				sprite.end();
			}
			// draw layer
			Texture playinglayertex = getBGAData(playinglayerid);
			if (playinglayertex != null) {
				sprite.begin();
				if (layershader.isCompiled()) {
					sprite.setShader(layershader);
					sprite.draw(playinglayertex, r.x, r.y, r.width, r.height);
					sprite.setShader(null);
				} else {
					sprite.draw(playinglayertex, r.x, r.y, r.width, r.height);
				}
				sprite.end();
			}
		}
		
		prevrendertime = time;
	}
	
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
		for (Texture bga : bgacache) {
			if (bga != null) {
				bga.dispose();
			}
		}
		for (Pixmap id : bgamap) {
			if (id != null) {
				id.dispose();
			}
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
