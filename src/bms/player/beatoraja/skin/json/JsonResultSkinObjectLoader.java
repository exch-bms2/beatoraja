package bms.player.beatoraja.skin.json;

import bms.player.beatoraja.result.MusicResultSkin;
import bms.player.beatoraja.skin.SkinHeader;

public class JsonResultSkinObjectLoader extends JsonSkinObjectLoader<MusicResultSkin> {

	public JsonResultSkinObjectLoader(JSONSkinLoader loader) {
		super(loader);
	}

	@Override
	public MusicResultSkin getSkin(SkinHeader header) {
		return new MusicResultSkin(header);
	}
}
