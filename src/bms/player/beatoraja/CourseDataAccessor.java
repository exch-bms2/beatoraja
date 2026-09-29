package bms.player.beatoraja;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.io.*;
import java.nio.file.*;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.logging.Logger;

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
        try (Stream<Path> paths = Files.list(Paths.get(coursedir))) {
            return paths
                    .filter(this::isCourseFile)
                    .flatMap(path -> Stream.of(read(path)))
                    .toArray(CourseData[]::new);
        } catch (IOException e) {
            e.printStackTrace();
            return new CourseData[0];
        }
    }
    
    public String[] readAllNames() {
        try (Stream<Path> paths = Files.list(Paths.get(coursedir))) {
            return paths
                    .filter(this::isCourseFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(name -> name.substring(0, name.length() - 5))
                    .toArray(String[]::new);
        } catch (IOException e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    public CourseData[] read(String name) {
        Path p = Paths.get(coursedir, name + ".json");
        if (!Files.isRegularFile(p)) {
            try (Stream<Path> paths = Files.list(Paths.get(coursedir))) {
                p = paths.filter(this::isCourseFile)
                        .filter(path -> path.getFileName().toString().equalsIgnoreCase(name + ".json"))
                        .findFirst()
                        .orElse(p);
            } catch (IOException e) {
                return new CourseData[0];
            }
        }
        return read(p);
    }

    private boolean isCourseFile(Path path) {
        String name = path.getFileName().toString();
        return Files.isRegularFile(path) && name.length() > 5
                && name.toLowerCase(Locale.ROOT).endsWith(".json");
    }

    private CourseData[] read(Path p) {
        try (InputStream input = new BufferedInputStream(Files.newInputStream(p))) {
            Json json = new Json();
            json.setIgnoreUnknownFields(true);
            CourseData[] courses = json.fromJson(CourseData[].class, input);
            if (courses != null) {
                return Stream.of(courses)
                        .filter(Objects::nonNull)
                        .filter(CourseData::validate)
                        .toArray(CourseData[]::new);
            }
        } catch (IOException | RuntimeException e) {
            // A single course object is supported as a fallback below.
        }
        try (InputStream input = new BufferedInputStream(Files.newInputStream(p))) {
            Json json = new Json();
            json.setIgnoreUnknownFields(true);
            CourseData course = json.fromJson(CourseData.class, input);
            if (course != null && course.validate()) {
                return new CourseData[] { course };
            }
            Logger.getGlobal().warning("コースデータが不正です : " + p);
        } catch (IOException | RuntimeException e) {
            Logger.getGlobal().warning("コースデータの読み込み失敗 : " + p + " : " + e.getMessage());
        }
        return new CourseData[0];
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
