package bms.player.beatoraja.ir;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.badlogic.gdx.utils.ObjectMap;

import bms.model.BMSDecoder;
import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.CourseData.CourseDataConstraint;
import bms.player.beatoraja.song.SongData;

/**
 * IRアクセスデータのキャッシュ
 *
 * @author exch
 */
public class RankingDataCache {

    /**
     * IRアクセスデータのキャッシュ
     */
    private ObjectMap<String, RankingData>[] scorecache;
    private ObjectMap<String, RankingData>[] cscorecache;

    public RankingDataCache() {
        scorecache = new ObjectMap[4];
        cscorecache = new ObjectMap[4];
        for (int i = 0; i < scorecache.length; i++) {
            scorecache[i] = new ObjectMap(2000);
            cscorecache[i] = new ObjectMap(100);
        }
    }

    /**
     * 指定した楽曲データ、LN MODEに対するIRアクセスデータを返す
     * @param song 楽曲データ
     * @param lnmode LN MODE
     * @return IRアクセスデータ。存在しない場合はnull
     */
    public RankingData get(SongData song, int lnmode) {
        final int cacheindex = song.hasUndefinedLongNote() ? lnmode : 3;
        if (scorecache[cacheindex].containsKey(song.getSha256())) {
            return scorecache[cacheindex].get(song.getSha256());
        }
        return null;
    }

    /**
     * 指定したコースデータ、LN MODEに対するIRアクセスデータを返す
     * @param course コースデータ
     * @param lnmode LN MODE
     * @return IRアクセスデータ。存在しない場合はnull
     */
    public RankingData get(CourseData course, int lnmode) {
        int cacheindex = 3;
        for(SongData song : course.getSong()) {
        	if(song.hasUndefinedLongNote()) {
        		cacheindex = lnmode;
        	}
        }
        String hash = createCourseHash(course);
        if (cscorecache[cacheindex].containsKey(hash)) {
            return cscorecache[cacheindex].get(hash);
        }
        return null;
    }

    public void put(SongData song, int lnmode, RankingData iras) {
        final int cacheindex = song.hasUndefinedLongNote() ? lnmode : 3;
        scorecache[cacheindex].put(song.getSha256(), iras);
    }
    
    public void put(CourseData course, int lnmode, RankingData iras) {
        int cacheindex = 3;
        for(SongData song : course.getSong()) {
        	if(song.hasUndefinedLongNote()) {
        		cacheindex = lnmode;
        	}
        }
        cscorecache[cacheindex].put(createCourseHash(course), iras);
    }
    
	private String createCourseHash(CourseData course) {
		StringBuilder sb = new StringBuilder();
		for(SongData song : course.getSong()) {
			if(song.getSha256() != null && song.getSha256().length() == 64) {
				sb.append(song.getSha256());
			} else {
				return null;
			}
		}
		for(CourseDataConstraint constraint : course.getConstraint()) {
			sb.append(constraint.name);
		}
		try {
			MessageDigest md = MessageDigest.getInstance("sha-256");
			md.update(sb.toString().getBytes());
			return BMSDecoder.convertHexString(md.digest());
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}