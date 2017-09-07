package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.TableData;
import bms.player.beatoraja.TableDataAccessor;
import bms.player.beatoraja.select.*;
import bms.player.beatoraja.song.SongData;

import java.util.*;

/**
 * Created by exch on 2017/09/02.
 */
public class TableBar extends DirectoryBar {

	private TableData td;
    private HashBar[] levels;
    private GradeBar[] grades;
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

    	final long t = System.currentTimeMillis();
		List<HashBar> levels = new ArrayList<HashBar>();
		for (TableData.TableFolder lv : td.getFolder()) {
			levels.add(new HashBar(selector, lv.getName(), lv.getSong()));
		}

		this.levels = levels.toArray(new HashBar[levels.size()]);
		List<GradeBar> l = new ArrayList<GradeBar>();

		Set<String> hashset = new HashSet<String>();
		for (CourseData course : td.getCourse()) {
			for (SongData hash : course.getSong()) {
				hashset.add(hash.getSha256().length() > 0 ? hash.getSha256() : hash.getMd5());
			}
		}
		SongData[] songs = selector.getSongDatabase().getSongDatas(hashset.toArray(new String[hashset.size()]));

		for (CourseData course : td.getCourse()) {
			List<SongData> songlist = new ArrayList<SongData>();
			for (SongData hash : course.getSong()) {
				SongData song = null;
				for(SongData sd :songs) {
					if((hash.getMd5().length() > 0 && hash.getMd5().equals(sd.getMd5())) || (hash.getSha256().length() > 0 && hash.getSha256().equals(sd.getSha256()))) {
						song = sd;
						break;
					}
				}
				songlist.add(song);
			}

			l.add(new GradeBar(course.getName(), songlist.toArray(new SongData[0]), course));
		}
		grades = l.toArray(new GradeBar[l.size()]);
    }

    public HashBar[] getLevels() {
        return levels;
    }

    public GradeBar[] getGrades() {
        return grades;
    }

    @Override
    public Bar[] getChildren() {
        List<Bar> l = new ArrayList<Bar>();
        l.addAll(Arrays.asList(getLevels()));
        l.addAll(Arrays.asList(getGrades()));
        return l.toArray(new Bar[0]);
    }
}
