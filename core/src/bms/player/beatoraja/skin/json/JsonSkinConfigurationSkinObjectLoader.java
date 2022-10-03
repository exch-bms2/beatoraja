package bms.player.beatoraja.skin.json;

import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.config.SkinConfigurationSkin;

public class JsonSkinConfigurationSkinObjectLoader extends JsonSkinObjectLoader<SkinConfigurationSkin> {

	public JsonSkinConfigurationSkinObjectLoader(JSONSkinLoader loader) {
		super(loader);
	}

	@Override
	public SkinConfigurationSkin getSkin(Resolution src, Resolution dst) {
		return new SkinConfigurationSkin(src, dst);
	}

}
