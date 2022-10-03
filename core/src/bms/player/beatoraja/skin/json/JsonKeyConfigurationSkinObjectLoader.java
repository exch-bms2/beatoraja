package bms.player.beatoraja.skin.json;

import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.config.KeyConfigurationSkin;

public class JsonKeyConfigurationSkinObjectLoader extends JsonSkinObjectLoader<KeyConfigurationSkin> {

	public JsonKeyConfigurationSkinObjectLoader(JSONSkinLoader loader) {
		super(loader);
	}

	@Override
	public KeyConfigurationSkin getSkin(Resolution src, Resolution dst) {
		return new KeyConfigurationSkin(src, dst);
	}

}
