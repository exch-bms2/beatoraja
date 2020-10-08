package bms.player.beatoraja.skin.json;

import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.result.CourseResultSkin;

public class JsonCourseResultSkinObjectLoader extends JsonSkinObjectLoader<CourseResultSkin> {

	public JsonCourseResultSkinObjectLoader(JSONSkinLoader loader) {
		super(loader);
	}

	@Override
	public CourseResultSkin getSkin(Resolution src, Resolution dst) {
		return new CourseResultSkin(src, dst);
	}

}
