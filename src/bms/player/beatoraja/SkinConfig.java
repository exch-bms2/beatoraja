package bms.player.beatoraja;

import java.util.HashMap;
import java.util.Map;

import bms.player.beatoraja.skin.SkinType;

/**
 * スキンコンフィグ
 * 
 * @author exch
 */
public class SkinConfig {

	public static final String DEFAULT_PLAY7 = "skin/default/play7.json";
	public static final String DEFAULT_PLAY5 = "skin/default/play5.json";
	public static final String DEFAULT_PLAY14 = "skin/default/play14.json";
	public static final String DEFAULT_PLAY10 = "skin/default/play10.json";
	public static final String DEFAULT_PLAY9 = "skin/default/play9.json";
	public static final String DEFAULT_SELECT = "skin/default/select.json";
	public static final String DEFAULT_DECIDE = "skin/default/decide.json";
	public static final String DEFAULT_RESULT = "skin/default/result.json";
	public static final String DEFAULT_GRADERESULT = "skin/default/graderesult.json";
	public static final String DEFAULT_PLAY24 = "skin/default/play24.json";
	public static final String DEFAULT_PLAY24DOUBLE = "skin/default/play24double.json";

	public static final Map<SkinType, String> defaultSkinPathMap = new HashMap<SkinType, String>() {
		{
			put(SkinType.PLAY_7KEYS, DEFAULT_PLAY7);
			put(SkinType.PLAY_5KEYS, DEFAULT_PLAY5);
			put(SkinType.PLAY_14KEYS, DEFAULT_PLAY14);
			put(SkinType.PLAY_10KEYS, DEFAULT_PLAY10);
			put(SkinType.PLAY_9KEYS, DEFAULT_PLAY9);
			put(SkinType.MUSIC_SELECT, DEFAULT_SELECT);
			put(SkinType.DECIDE, DEFAULT_DECIDE);
			put(SkinType.RESULT, DEFAULT_RESULT);
			put(SkinType.COURSE_RESULT, DEFAULT_GRADERESULT);
			put(SkinType.PLAY_24KEYS, DEFAULT_PLAY24);
			put(SkinType.PLAY_24KEYS_DOUBLE, DEFAULT_PLAY24DOUBLE);
		}
	};

	private String path;

	private Map property;

	public SkinConfig() {

	}

	public SkinConfig(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Map getProperty() {
		return property;
	}

	public void setProperty(Map property) {
		this.property = property;
	}
}