package bms.player.beatoraja.skin.json;

import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.result.MusicResultSkin;

public class JsonResultSkinObjectLoader extends JsonSkinObjectLoader<MusicResultSkin> {

	public JsonResultSkinObjectLoader(JSONSkinLoader loader) {
		super(loader);
	}

	@Override
	public MusicResultSkin getSkin(Resolution src, Resolution dst) {
		return new MusicResultSkin(src, dst);
	}
}
