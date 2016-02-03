package bms.player.beatoraja.input;

import java.util.logging.Logger;

import bms.player.beatoraja.select.MusicSelector;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;

public class MusicSelectorInputProcessor extends AbstractInputProcessor{

	private MusicSelector selector;
	
	public MusicSelectorInputProcessor(MusicSelector selector) {
		this.selector = selector;
		Gdx.input.setInputProcessor(new KeyBoardInputProcesseor());
		for (Controller controller : Controllers.getControllers()) {
			Logger.getGlobal().info("コントローラーを検出 : " + controller.getName());
			controller.addListener(new BMSControllerListener());
		}
	}
	
	public void stopPlay() {
		selector.exit();
	}

}
