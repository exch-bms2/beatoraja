package bms.player.beatoraja.config;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainState;
import static bms.player.beatoraja.skin.SkinProperty.*;
import bms.player.beatoraja.skin.SkinPropertyMapper;
import bms.player.beatoraja.skin.SkinType;

public class SkinConfiguration extends MainState {

	private SkinConfigurationSkin skin;
	private SkinType type;

	public SkinConfiguration(MainController main) {
		super(main);
	}

	public void create() {
		loadSkin(SkinType.SKIN_SELECT);
		skin = (SkinConfigurationSkin) getSkin();
		type = SkinType.getSkinTypeById(skin.getDefaultSkinType());
		if (type == null) {
			type = SkinType.PLAY_7KEYS;
		}
	}

	public void render() {

		if (main.getInputProcessor().isExitPressed()) {
			main.getInputProcessor().setExitPressed(false);
			main.saveConfig();
			main.changeState(MainController.STATE_SELECTMUSIC);
		}
	}

	public int getImageIndex(int id) {
		if (SkinPropertyMapper.isSkinSelectTypeId(id)) {
			SkinType t = SkinPropertyMapper.getSkinSelectType(id);
			return type == t ? 1 : 0;
		}
		return super.getImageIndex(id);
	}

	public void executeClickEvent(int id) {
		switch (id) {
		case BUTTON_CHANGE_SKIN:
			break;
		default:
			if (SkinPropertyMapper.isSkinSelectTypeId(id)) {
				SkinType t = SkinPropertyMapper.getSkinSelectType(id);
				changeSkinType(t);
			}
		}
	}

	private void changeSkinType(SkinType type) {
		this.type = type;
	}

	@Override
	public void dispose() {
		super.dispose();
	}
}
