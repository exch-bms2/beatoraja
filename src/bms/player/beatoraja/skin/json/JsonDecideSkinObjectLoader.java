package bms.player.beatoraja.skin.json;

import bms.player.beatoraja.decide.MusicDecideSkin;
import bms.player.beatoraja.skin.SkinHeader;

public class JsonDecideSkinObjectLoader extends JsonSkinObjectLoader<MusicDecideSkin> {

	public JsonDecideSkinObjectLoader(JSONSkinLoader loader) {
		super(loader);
	}

	@Override
	public MusicDecideSkin getSkin(SkinHeader header) {
		return new MusicDecideSkin(header);
	}

}
