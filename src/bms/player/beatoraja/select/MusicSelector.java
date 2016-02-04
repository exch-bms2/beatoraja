package bms.player.beatoraja.select;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import bms.model.TimeLine;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainController.PlayerResource;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.MusicSelectorInputProcessor;
import bms.player.lunaticrave2.FolderData;
import bms.player.lunaticrave2.LunaticRave2SongDatabaseManager;
import bms.player.lunaticrave2.SongData;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;

/**
 * 選曲部分。 楽曲一覧とカーソルが指す楽曲のステータスを表示し、選択した楽曲を 曲決定部分に渡す。
 * 
 * @author exch
 */
public class MusicSelector extends ApplicationAdapter {

	// TODO オプション選択
	// TODO スコア取得、閲覧
	// TODO フォルダの実装(LR2DBアクセサ側も要変更)

	private MainController main;

	private ShapeRenderer shape;
	private SpriteBatch sprite;
	private BitmapFont titlefont;

	private MusicSelectorInputProcessor input;

	private Bar[] currentsongs;
	private int selectedindex;
	private List<String> dir = new ArrayList();
	/**
	 * 楽曲DBアクセサ
	 */
	private LunaticRave2SongDatabaseManager songdb;

	private Config config;

	public MusicSelector(MainController main, Config config) {
		this.main = main;
		this.config = config;
		try {
			songdb = new LunaticRave2SongDatabaseManager(
					new File("song.db").getPath(), true);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		songdb.createTable();

	}

	public void create() {
		shape = new ShapeRenderer();
		sprite = new SpriteBatch();
		updateBar("e2977170");
		input = new MusicSelectorInputProcessor(this);

	}

	public void render() {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		final float w = 1280;
		final float h = 720;

		// 背景描画
		// sprite.begin();
		// sprite.draw(skin.getBackground(), 0, 0, w, h);
		// sprite.end();

		// draw song bar
		final float barh = 30;
		for (int i = 0; i < h / barh; i++) {
			Bar sd = currentsongs[(int) (selectedindex + currentsongs.length
					* 100 + i - h / barh / 2)
					% currentsongs.length];
			int x = 720;
			if (i == h / barh / 2) {
				x = 700;
			}
			shape.begin(ShapeType.Filled);
			if (sd instanceof FolderBar) {
				shape.setColor(Color.valueOf("606000"));
			}
			if (sd instanceof SongBar) {
				shape.setColor(Color.valueOf("006000"));
			}
			shape.rect(x, i * barh, 560, barh - 1);
			shape.end();
			shape.begin(ShapeType.Line);
			shape.setColor(Color.valueOf("888888"));
			shape.rect(x, i * barh, 560, barh - 1);
			shape.end();
			sprite.begin();
			titlefont.setColor(Color.WHITE);
			titlefont.draw(sprite, sd.getTitle(), x + 20, (i + 1) * barh);
			sprite.end();

		}

		boolean[] keystate = input.getKeystate();
		long[] keytime = input.getTime();
		if (keystate[7]) {
			selectedindex++;
		}
		if (keystate[8]) {
			selectedindex += currentsongs.length - 1;
		}
		selectedindex = selectedindex % currentsongs.length;

		if (keystate[0] && keytime[0] != 0) {
			keytime[0] = 0;
			if (currentsongs[selectedindex] instanceof FolderBar) {
				FolderData fd = ((FolderBar) currentsongs[selectedindex])
						.getFolderData();
				String path = fd.getPath();
				if (path.endsWith(String.valueOf(File.separatorChar))) {
					path = path.substring(0, path.length() - 1);
				}
				if (updateBar(songdb.crc32(path, new String[0],
						new File(".").getAbsolutePath()))) {
					dir.add(fd.getParent());
				}
			} else if (currentsongs[selectedindex] instanceof SongBar) {
				main.setAuto(0);
				PlayerResource resource = new PlayerResource();
				resource.setBMSFile(new File(
						((SongBar) currentsongs[selectedindex]).getSongData()
								.getPath()), config, 0);
				main.changeState(MainController.STATE_DECIDE, resource);
			}
		}

		if (keystate[1] && keytime[1] != 0) {
			keytime[1] = 0;
			if (dir.size() > 0) {
				String crc = dir.get(dir.size() - 1);
				updateBar(crc);
				dir.remove(dir.size() - 1);
			}
		}

		if (keystate[4]) {
			if (currentsongs[selectedindex] instanceof SongBar) {
				PlayerResource resource = new PlayerResource();
				resource.setBMSFile(new File(
						((SongBar) currentsongs[selectedindex]).getSongData()
								.getPath()), config, 1);
				main.changeState(MainController.STATE_DECIDE, resource);
			}
		}
		if (keystate[6]) {
			if (currentsongs[selectedindex] instanceof SongBar) {
				PlayerResource resource = new PlayerResource();
				resource.setBMSFile(new File(
						((SongBar) currentsongs[selectedindex]).getSongData()
								.getPath()), config, 2);
				main.changeState(MainController.STATE_DECIDE, resource);
			}
		}

	}

	public boolean updateBar(String crc) {
		Logger.getGlobal().info("crc :" + crc);
		FolderData[] folders = songdb.getFolderDatas("parent", crc, new File(
				".").getAbsolutePath());
		SongData[] songs = songdb.getSongDatas("parent", crc,
				new File(".").getAbsolutePath());
		List<Bar> l = new ArrayList();
		if (songs.length == 0) {
			for (FolderData folder : folders) {
				l.add(new FolderBar(folder));
			}
		} else {
			for (SongData song : songs) {
				l.add(new SongBar(song));
			}
		}
		if (l.size() > 0) {
			currentsongs = l.toArray(new Bar[0]);
			selectedindex = 0;
			
			FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
					Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
			FreeTypeFontParameter parameter = new FreeTypeFontParameter();
			parameter.size = 24;
			String str = "";
			for (Bar song : currentsongs) {
				str += song.getTitle();
			}
			parameter.characters = str;
			titlefont = generator.generateFont(parameter);
			return true;
		}
		Logger.getGlobal().warning("楽曲がありません");
		return false;
	}

	public void exit() {
		main.exit();
	}

	public void dispose() {
		shape.dispose();
		sprite.dispose();
		titlefont.dispose();
	}
}

abstract class Bar {

	public abstract String getTitle();
}

class SongBar extends Bar {

	private SongData song;

	public SongBar(SongData song) {
		this.song = song;
	}

	public SongData getSongData() {
		return song;
	}

	@Override
	public String getTitle() {
		return song.getTitle();
	}
}

class FolderBar extends Bar {

	private FolderData folder;

	public FolderBar(FolderData folder) {
		this.folder = folder;
	}

	public FolderData getFolderData() {
		return folder;
	}

	@Override
	public String getTitle() {
		return folder.getTitle();
	}
}
