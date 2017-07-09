package bms.player.beatoraja;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * コースデータへのアクセス
 *
 * @author exch
 */
public class CourseDataAccessor {

    private final String coursedir;

    public CourseDataAccessor(String path) {
        coursedir = path;
    }
    /**
     * 全てのキャッシュされた難易度表データを読み込む
     *
     * @return 全てのキャッシュされた難易度表データ
     */
    public CourseData[] readAll() {
        List<CourseData> result = new ArrayList<>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(coursedir))) {
            for (Path p : paths) {
                if (p.toString().endsWith(".json")) {
                    boolean isList = false;
                    try {
                        Json json = new Json();
                        CourseData[] courses = json.fromJson(CourseData[].class,
                                new BufferedInputStream(Files.newInputStream(p)));
                        result.addAll(Arrays.asList(courses));
                        isList = true;
                    } catch(Throwable e) {

                    }
                    if(!isList) {
                        try {
                            Json json = new Json();
                            CourseData course = json.fromJson(CourseData.class,
                                    new BufferedInputStream(Files.newInputStream(p)));
                            result.add(course);
                        } catch(Throwable e) {

                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toArray(new CourseData[result.size()]) ;
    }

    /**
     * コースデータを保存する
     *
     * @param cd コースデータ
     */
    public void write(String name, CourseData cd) {
        try {
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
