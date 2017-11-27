package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.TableData;
import bms.player.beatoraja.TableDataAccessor;
import bms.player.beatoraja.select.*;
import bms.player.beatoraja.song.SongData;

import java.util.*;

/**
 * 難易度表バー
 * 
 * @author exch
 */
public class TableBar extends DirectoryBar {
	/**
	 * 難易度表データ
	 */
	private TableData td;
	/**
	 * 難易度表レベルバー
	 */
    private HashBar[] levels;
    /**
     * 難易度表コースバー
     */
    private GradeBar[] grades;
    /**
     * レベルバー+コースバー
     */
    private Bar[] children;
    private MusicSelector selector;
    private TableDataAccessor.TableReader tr;

    public TableBar(MusicSelector selector, TableData td, TableDataAccessor.TableReader tr) {
        this.selector = selector;
        this.tr = tr;
        setTableData(td);
    }

    @Override
    public String getTitle() {
        return td.getName();
    }

    public TableDataAccessor.TableReader getReader() {
    	return tr;
    }

    public void setTableData(TableData td) {
    	this.td = td;

    	final TableData.TableFolder[] folder = td.getFolder();
    	levels = new HashBar[folder.length];
    	for(int i = 0;i < folder.length;i++) {
    		levels[i] = new HashBar(selector, folder[i].getName(), folder[i].getSong());
    	}
    	
		final CourseData[] courses = td.getCourse();
		Set<String> hashset = new HashSet<String>(courses.length * 4);
		for (CourseData course : courses) {
			for (SongData hash : course.getSong()) {
				hashset.add(hash.getSha256().length() > 0 ? hash.getSha256() : hash.getMd5());
			}
		}
		SongData[] songs = selector.getSongDatabase().getSongDatas(hashset.toArray(new String[hashset.size()]));

		grades = new GradeBar[courses.length];
		List<SongData> songlist = new ArrayList<SongData>();
		for (int i = 0;i < courses.length;i++) {
			for (SongData hash : courses[i].getSong()) {
				SongData song = null;
				for(SongData sd :songs) {
					if((hash.getMd5().length() > 0 && hash.getMd5().equals(sd.getMd5())) || (hash.getSha256().length() > 0 && hash.getSha256().equals(sd.getSha256()))) {
						song = sd;
						break;
					}
				}
				songlist.add(song);
			}
			grades[i] = new GradeBar(courses[i].getName(), songlist.toArray(new SongData[songlist.size()]), courses[i]);
			songlist.clear();
		}
		
		children = new Bar[levels.length + grades.length];
		int index = 0;
		for (int i = 0;i < levels.length;i++, index++) {
			children[index] = levels[i];
		}
		for (int i = 0;i < grades.length;i++, index++) {
			children[index] = grades[i];
		}
    }

    public HashBar[] getLevels() {
        return levels;
    }

    public GradeBar[] getGrades() {
        return grades;
    }

    @Override
    public Bar[] getChildren() {
    	return children;
    }
}
