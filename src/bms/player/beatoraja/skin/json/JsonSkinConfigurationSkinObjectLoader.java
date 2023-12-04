package bms.player.beatoraja.skin.json;

import bms.player.beatoraja.config.SkinConfigurationSkin;
import bms.player.beatoraja.skin.SkinHeader;

public class JsonSkinConfigurationSkinObjectLoader extends JsonSkinObjectLoader<SkinConfigurationSkin> {

	public JsonSkinConfigurationSkinObjectLoader(JSONSkinLoader loader) {
		super(loader);
	}

	@Override
	public SkinConfigurationSkin getSkin(SkinHeader header) {
		return new SkinConfigurationSkin(header);
	}

}
