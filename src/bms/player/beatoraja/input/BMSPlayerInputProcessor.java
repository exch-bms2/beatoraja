package bms.player.beatoraja.input;

import java.awt.image.ImageProducer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import bms.player.beatoraja.BMSPlayer;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;

/**
 * キー入力処理用クラス
 * 
 * @author exch
 */
public class BMSPlayerInputProcessor extends AbstractInputProcessor {

	private BMSPlayer player;

	private long start_time;

	private boolean autoplay;

	private List<KeyInputLog> keylog = new ArrayList<KeyInputLog>();

	public BMSPlayerInputProcessor(BMSPlayer player, boolean autoplay) {
		this.player = player;
		this.autoplay = autoplay;
		Gdx.input.setInputProcessor(new KeyBoardInputProcesseor());
		for (Controller controller : Controllers.getControllers()) {
			Logger.getGlobal().info("コントローラーを検出 : " + controller.getName());
			controller.addListener(new BMSControllerListener());
		}
	}
	
	public void keyChanged(int presstime, int i,boolean pressed) {
		if(!autoplay) {
			super.keyChanged(presstime, i, pressed);			
		}
		if(startPressed() && pressed) {
			switch(i) {
			case 0:
			case 2:
			case 4:
			case 6:
				hispeedChanged(false);
				break;
			case 1:
			case 3:
			case 5:
				hispeedChanged(true);
				break;
			}
		}
		if(this.getStartTime() != 0) {
			keylog.add(new KeyInputLog(presstime, i, pressed));						
		}		
	}

	public void startChanged(boolean pressed) {
		super.startChanged(pressed);
		if(pressed) {
			if (start_time > System.currentTimeMillis() - 500) {
				player.getLaneRenderer().setEnableLanecover(!player.getLaneRenderer().isEnableLanecover());
				start_time = 0;
			} else {
				start_time = System.currentTimeMillis();
			}			
		}
	}
	
	public void lanecoverChanged(float f) {
		float lanecover = player.getLaneRenderer().getLanecover();
		lanecover = lanecover + f;
		if (lanecover > 1) {
			lanecover = 1;
		}
		if (lanecover < 0) {
			lanecover = 0;
		}
		player.getLaneRenderer().setLanecover(lanecover);
	}
	
	public void hispeedChanged(boolean b) {
		player.getLaneRenderer().changeHispeed(b);
	}

	public List<KeyInputLog> getKeyInputLog() {
		return keylog;
	}	
	
	public void stopPlay() {
		player.stopPlay();
	}
}
