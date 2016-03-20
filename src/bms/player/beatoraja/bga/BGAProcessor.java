package bms.player.beatoraja.bga;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.player.beatoraja.Config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;

/**
 * 
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
	private Pixmap backbmp;
	private Pixmap stagefile;

	private final String[] mov_extension = { "mpg", "mpeg", "avi", "wmv" };
	private final String[] pic_extension = { "jpg", "jpeg", "gif", "bmp", "png" };

	public BGAProcessor(Config config) {
		this.config = config;
	}

	public void setModel(BMSModel model, String filepath) {
		this.model = model;
		// BMS格納ディレクトリ
		String directorypath = filepath.substring(0, filepath.lastIndexOf(File.separatorChar) + 1);

		dispose();
		progress = 0;

		String stage = model.getStagefile();
		if(stage != null && stage.length() > 0) {
			stagefile = this.loadPicture(new File(directorypath + stage));
		}
		String back = model.getBackbmp();
		if(back != null && back.length() > 0) {
			backbmp = this.loadPicture(new File(directorypath + back));
		}

		bgamap = new Pixmap[model.getBgaList().length];
		int id = 0;
		for (String name : model.getBgaList()) {
			File f = null;
			if(new File(directorypath + name).exists()) {
				f = new File(directorypath + name);
			}
			if(f == null) {
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
			
			if(f != null) {
				for (String mov : mov_extension) {
					if(f.getName().endsWith(mov)) {
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
					System.out.println("BGA Picture loaded  : " + dir.getName());
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
	
	public Pixmap getBackbmpData() {
		return backbmp;
	}

	public Pixmap getStagefileData() {
		return stagefile;
	}

	public Pixmap getBGAData(int id) {
		if (progress != 1 || id == -1) {
			return null;
		}

		Pixmap pix = bgamap[id];
		if(pix != null) {
			return pix;
		}
		if (mpgmap.get(id) != null) {
			return mpgmap.get(id).getBGAData();
		}
		return null;
	}

	/**
	 * リソースを開放する
	 */
	public void dispose() {
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
}
