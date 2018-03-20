package bms.player.beatoraja.config;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.SkinType;

public class SkinConfiguration extends MainState {

	public SkinConfiguration(MainController main) {
		super(main);
	}

	public void create() {
		loadSkin(SkinType.SKIN_SELECT);
	}

	public void render() {

		if (main.getInputProcessor().isExitPressed()) {
			main.getInputProcessor().setExitPressed(false);
			main.saveConfig();
			main.changeState(MainController.STATE_SELECTMUSIC);
		}
	}

	@Override
	public void dispose() {
		super.dispose();
	}
}
