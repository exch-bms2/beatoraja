package bms.player.beatoraja;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * コースデータへのアクセス
 *
 * @author exch
 */
public class CourseDataAccessor {

    private final String coursedir;

    public CourseDataAccessor(String path) {
        coursedir = path;
		try {
			Files.createDirectories(Paths.get(coursedir));
		} catch (IOException e) {
		}
    }
    /**
     * 全てのキャッシュされた難易度表データを読み込む
     *
     * @return 全てのキャッシュされた難易度表データ
     */
    public CourseData[] readAll() {
        List<CourseData> result = new ArrayList<>();
        for(String name : readAllNames()) {
        	result.addAll(Arrays.asList(read(name)));
        }
        return result.toArray(new CourseData[result.size()]) ;
    }
    
    public String[] readAllNames() {
        List<String> result = new ArrayList<>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(coursedir))) {
            for (Path p : paths) {
                if (p.toString().endsWith(".json")) {
                	String filename = p.getFileName().toString();
                	result.add(filename.substring(0, filename.lastIndexOf('.')));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toArray(new String[result.size()]) ;    	
    }

    public CourseData[] read(String name) {
        Path p = Paths.get(coursedir + "/" + name + ".json");
        boolean isList = false;
        try {
            Json json = new Json();
			json.setIgnoreUnknownFields(true);
            CourseData[] courses =  json.fromJson(CourseData[].class,
                    new BufferedInputStream(Files.newInputStream(p)));
            List<CourseData> result = new ArrayList<CourseData>();            
            for(CourseData course : courses) {
            	if(course.validate()) {
                	result.add(course);
            	}
            }
            return result.toArray(new CourseData[result.size()]);
        } catch(Throwable e) {

        }
        if(!isList) {
            try {
                Json json = new Json();
				json.setIgnoreUnknownFields(true);
                CourseData course = json.fromJson(CourseData.class,
                        new BufferedInputStream(Files.newInputStream(p)));
            	if(course.validate()) {
            		return new CourseData[]{course};
            	}
            } catch(Throwable e) {
            }
        }
        return new CourseData[0] ;
    }
    /**
     * コースデータを保存する
     *
     * @param cd コースデータ
     */
    public void write(String name, CourseData[] cd) {
        try {
        	for(CourseData c : cd) {
        		c.shrink();
        	}
            Json json = new Json();
            json.setOutputType(JsonWriter.OutputType.json);
            OutputStreamWriter fw = new OutputStreamWriter(new BufferedOutputStream(
                    new FileOutputStream(coursedir + "/" + name + ".json")), "UTF-8");
            fw.write(json.prettyPrint(cd));
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
