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
	private Map<Integer, VLCMovieProcessor> mpgmap = new HashMap<Integer, VLCMovieProcessor>();
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
			stage = stage.substring(0, stage.lastIndexOf('.'));
			stagefile = this.loadPicture(directorypath, stage);
		}
		String back = model.getBackbmp();
		if(back != null && back.length() > 0) {
			back = back.substring(0, back.lastIndexOf('.'));
			backbmp = this.loadPicture(directorypath, back);
		}

		bgamap = new Pixmap[model.getBgaList().length];
		int id = 0;
		for (String name : model.getBgaList()) {
			name = name.substring(0, name.lastIndexOf('.'));

			for (String mov : mov_extension) {
				File mpgfile = new File(directorypath + name + "." + mov);
				if (mpgfile.exists()) {
					try {
						VLCMovieProcessor mm = this.loadMovie(id, mpgfile);
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
				bgamap[id] = this.loadPicture(directorypath, name);
			progress += 1f / model.getBgaList().length;
			id++;
		}
		Logger.getGlobal().info("BGAファイル読み込み完了。BGA数:" + model.getBgaList().length);
		progress = 1;
	}

	private VLCMovieProcessor loadMovie(int id, File f) throws Exception {
		if (config.getVlcpath().length() > 0) {
			VLCMovieProcessor mm = new VLCMovieProcessor(config.getVlcpath());
			mm.create(f.getPath());
			return mm;
		}
		return null;
	}

	private Pixmap loadPicture(String dir, String name) {
		Pixmap tex = null;
		for (String mov : pic_extension) {
			File f = new File(dir + name + "." + mov);
			if (f.exists()) {
				try {
					tex = new Pixmap(Gdx.files.internal(f.getPath()));
					System.out.println("BGA Picture loaded  : " + name);
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
