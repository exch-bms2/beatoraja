package bms.player.beatoraja.external;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import bms.player.beatoraja.song.SongData;
import com.badlogic.gdx.utils.Json;

import bms.player.beatoraja.*;
import bms.player.beatoraja.TableData.TableFolder;
import lombok.Data;

public class BMSSearchAccessor extends TableDataAccessor.TableAccessor {

	public BMSSearchAccessor() {
		super("BMS Search");
	}

	public TableData read() {
		TableData td = null;
		try (InputStream input = new URL("http://qstol.info/bmssearch/api/services/?method=bms.new").openStream()) {
			Json json = new Json();
			BMSSearchElement[] elements = json.fromJson(BMSSearchElement[].class, input);
			
			td = new TableData();
			TableFolder tdenew = new TableFolder();
			tdenew.setName("New");
			List<SongData> songs = new ArrayList();
			for(BMSSearchElement element : elements) {
				if(element.getFumen() != null && element.getFumen().length > 0) {
					for(Fumen fumen : element.getFumen()) {
						SongData song = new SongData();
						song.setTitle(fumen.getTitle());
						song.setArtist(element.getArtist());
						song.setGenre(element.getGenre());
						song.setMd5(fumen.getMd5hash());
						if(element.getDladdress() != null && element.getDladdress().length > 0) {
							song.setUrl(element.getDladdress()[0]);							
						}
						songs.add(song);
					}
				} else {
					// 譜面が存在しない場合の処理はここに書く
				}
			}
			tdenew.setSongs(songs.toArray(new SongData[songs.size()]));
			td.setFolder(new TableFolder[]{tdenew});
			td.setName("BMS Search");
            Logger.getGlobal().info("BMS Search取得完了");
		} catch (Throwable e) {
			Logger.getGlobal().severe("BMS Search更新中の例外:" + e.getMessage());
		}
		return td;
	}

	@Override
	public void write(TableData td) {
		new TableDataAccessor().write(td);
	}

	@Data
	public static class BMSSearchElement {
		
		private String id;
		private String title;
		private String genre;
		private String artist;
		private String[] dladdress;
		private Fumen[] fumen;
	}
	
	@Data
	public static class Fumen {
		private String title;
		private String filename;
		private String md5hash;
	}
}
