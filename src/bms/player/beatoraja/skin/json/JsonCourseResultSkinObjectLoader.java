package bms.player.beatoraja.skin.json;

import bms.player.beatoraja.result.CourseResultSkin;
import bms.player.beatoraja.skin.SkinHeader;

public class JsonCourseResultSkinObjectLoader extends JsonSkinObjectLoader<CourseResultSkin> {

	public JsonCourseResultSkinObjectLoader(JSONSkinLoader loader) {
		super(loader);
	}

	@Override
	public CourseResultSkin getSkin(SkinHeader header) {
		return new CourseResultSkin(header);
	}

}
