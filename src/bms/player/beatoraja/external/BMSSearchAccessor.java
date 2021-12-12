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

/**
 * BMS Searchアクセス用クラス
 * @see <a href="https://bmssearch.stoplight.io/docs/bmssearch-api/YXBpOjg0MzMw-bms-search-api">bmssearch-api</a>
 * @author exch
 */
public class BMSSearchAccessor extends TableDataAccessor.TableAccessor {
	private static String API_STRING = "https://api.bmssearch.net/v1/bmses/search?orderBy=PUBLISHED&orderDirection=DESC&limit=20";

	private String tabledir;

	public BMSSearchAccessor(String tabledir) {
		super("BMS Search");
		this.tabledir = tabledir;
	}

	public TableData read() {
		TableData td = null;
		try (InputStream input = new URL(BMSSearchAccessor.API_STRING).openStream()) {
			Json json = new Json();
			json.setIgnoreUnknownFields(true);
			BMSSearchElement[] elements = json.fromJson(BMSSearchElement[].class, input);

			td = new TableData();
			TableFolder tdenew = new TableFolder();
			tdenew.setName("New");
			List<SongData> songs = new ArrayList<SongData>();
			for (BMSSearchElement element : elements) {
				SongData song = new SongData();
				song.setTitle(element.getTitle());
				song.setArtist(element.getArtist());
				song.setGenre(element.getGenre());
				if (element.getDownloads() != null && element.getDownloads().length > 0) {
					song.setUrl(element.getDownloads()[0].getUrl());
				} else {
					// URLが存在しない場合の処理はここに書く
				}

				// MD5取得処理
				try (InputStream bmsInput = new URL(
						"https://api.bmssearch.net/v1/bmses/" + element.getId() + "/patterns?limit=1").openStream()) {
					Json _json = new Json();
					_json.setIgnoreUnknownFields(true);
					BMSPatterns[] patterns = _json.fromJson(BMSPatterns[].class, bmsInput);
					if (patterns != null && patterns.length > 0 && patterns[0].getFile() != null) {
						song.setMd5(patterns[0].getFile().getHashMd5());
					} else {
						// BMSPatterns.file が存在しない場合の処理はここに書く
					}
				} catch (Throwable e) {
					throw e;
				}

				songs.add(song);
			}
			tdenew.setSong(songs.toArray(new SongData[songs.size()]));
			td.setFolder(new TableFolder[] { tdenew });
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
		private Downloads[] downloads;

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

		public Downloads[] getDownloads() {
			return downloads;
		}

		public void setDownloads(Downloads[] downloads) {
			this.downloads = downloads;
		}

		public String getArtist() {
			return artist;
		}

		public void setArtist(String artist) {
			this.artist = artist;
		}
	}

	public static class Downloads {
		private String url;
		private String description;

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

	public static class BMSPatterns {
		private BMSPatternsFile file;

		public BMSPatternsFile getFile() {
			return file;
		}

		public void setFile(BMSPatternsFile file) {
			this.file = file;
		}
	}

	public static class BMSPatternsFile {
		private String hashMd5;

		public String getHashMd5() {
			return hashMd5;
		}

		public void setHashMd5(String hashMd5) {
			this.hashMd5 = hashMd5;
		}
	}
}
