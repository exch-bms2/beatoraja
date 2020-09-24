package bms.player.beatoraja.skin.json;

import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.decide.MusicDecideSkin;

public class JsonDecideSkinObjectLoader extends JsonSkinObjectLoader<MusicDecideSkin> {

	public JsonDecideSkinObjectLoader(JSONSkinLoader loader) {
		super(loader);
	}

	@Override
	public MusicDecideSkin getSkin(Resolution src, Resolution dst) {
		return new MusicDecideSkin(src, dst);
	}

}
