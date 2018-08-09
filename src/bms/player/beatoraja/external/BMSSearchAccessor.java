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

public class BMSSearchAccessor extends TableDataAccessor.TableAccessor {

	private String tabledir;
	
	public BMSSearchAccessor(String tabledir) {
		super("BMS Search");
		this.tabledir = tabledir;
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
			tdenew.setSong(songs.toArray(new SongData[songs.size()]));
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
		new TableDataAccessor(tabledir).write(td);
	}
	
	public static class BMSSearchElement {
		
		private String id;
		private String title;
		private String genre;
		private String artist;
		private String[] dladdress;
		private Fumen[] fumen;
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getTitle() {
			return title;
		}
		public void setTitle(String title) {
			this.title = title;
		}
		public String getGenre() {
			return genre;
		}
		public void setGenre(String genre) {
			this.genre = genre;
		}
		public Fumen[] getFumen() {
			return fumen;
		}
		public void setFumen(Fumen[] fumen) {
			this.fumen = fumen;
		}
		public String[] getDladdress() {
			return dladdress;
		}
		public void setDladdress(String[] dladdress) {
			this.dladdress = dladdress;
		}
		public String getArtist() {
			return artist;
		}
		public void setArtist(String artist) {
			this.artist = artist;
		}
	}
	
	public static class Fumen {
		private String title;
		private String filename;
		private String md5hash;
		public String getTitle() {
			return title;
		}
		public void setTitle(String title) {
			this.title = title;
		}
		public String getFilename() {
			return filename;
		}
		public void setFilename(String filename) {
			this.filename = filename;
		}
		public String getMd5hash() {
			return md5hash;
		}
		public void setMd5hash(String md5hash) {
			this.md5hash = md5hash;
		}		
	}
}
