package bms.player.beatoraja;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;

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
     * 全てのコースデータを読み込む
     *
     * @return 全てのキャッシュされた難易度表データ
     */
    public CourseData[] readAll() {
        return Stream.of(readAllNames()).flatMap(name -> Stream.of(read(name))).toArray(CourseData[]::new);
    }
    
    public String[] readAllNames() {
        try (Stream<Path> paths = Files.list(Paths.get(coursedir))) {
        	return paths.map(p -> p.getFileName().toString().substring(0, p.getFileName().toString().lastIndexOf('.'))).toArray(String[]::new);
        } catch (IOException e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    public CourseData[] read(String name) {
        Path p = Paths.get(coursedir + "/" + name + ".json");
        boolean isList = false;
        try {
            Json json = new Json();
			json.setIgnoreUnknownFields(true);
            CourseData[] courses =  json.fromJson(CourseData[].class,
                    new BufferedInputStream(Files.newInputStream(p)));
            return Stream.of(courses).filter(CourseData::validate).toArray(CourseData[]::new);
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
        	Stream.of(cd).forEach(CourseData::shrink);
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
