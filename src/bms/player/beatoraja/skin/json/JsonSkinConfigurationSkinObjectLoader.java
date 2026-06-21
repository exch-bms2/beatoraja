package bms.player.beatoraja.skin.json;

import bms.player.beatoraja.config.SkinConfigurationSkin;
import bms.player.beatoraja.config.SkinPreview;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinObject;

import java.nio.file.Path;

public class JsonSkinConfigurationSkinObjectLoader extends JsonSkinObjectLoader<SkinConfigurationSkin> {

	public JsonSkinConfigurationSkinObjectLoader(JSONSkinLoader loader) {
		super(loader);
	}

	@Override
	public SkinConfigurationSkin getSkin(SkinHeader header) {
		return new SkinConfigurationSkin(header);
	}

	@Override
	public SkinObject loadSkinObject(SkinConfigurationSkin skin, JsonSkin.Skin sk, JsonSkin.Destination dst, Path p) {
		SkinObject obj = super.loadSkinObject(skin, sk, dst, p);
		if (obj != null) {
			return obj;
		}
		return dst.id.equals("skin-preview") ? new SkinPreview() : null;
	}

}
