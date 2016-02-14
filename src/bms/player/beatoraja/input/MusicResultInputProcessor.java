package bms.player.beatoraja.input;

import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;

import bms.player.beatoraja.result.MusicResult;

public class MusicResultInputProcessor extends AbstractInputProcessor {

	private MusicResult selector;
	
	public MusicResultInputProcessor(MusicResult selector) {
		this.selector = selector;
		Gdx.input.setInputProcessor(new KeyBoardInputProcesseor());
		for (Controller controller : Controllers.getControllers()) {
			Logger.getGlobal().info("コントローラーを検出 : " + controller.getName());
			controller.addListener(new BMSControllerListener());
		}
	}

}
