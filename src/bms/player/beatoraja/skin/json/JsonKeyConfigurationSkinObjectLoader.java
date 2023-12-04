package bms.player.beatoraja.skin.json;

import bms.player.beatoraja.config.KeyConfigurationSkin;
import bms.player.beatoraja.skin.SkinHeader;

public class JsonKeyConfigurationSkinObjectLoader extends JsonSkinObjectLoader<KeyConfigurationSkin> {

	public JsonKeyConfigurationSkinObjectLoader(JSONSkinLoader loader) {
		super(loader);
	}

	@Override
	public KeyConfigurationSkin getSkin(SkinHeader header) {
		return new KeyConfigurationSkin(header);
	}

}
