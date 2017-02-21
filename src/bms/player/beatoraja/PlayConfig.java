package bms.player.beatoraja;

import bms.player.beatoraja.input.BMControllerInputProcessor.BMKeys;

import com.badlogic.gdx.Input.Keys;

/**
 * プレイコンフィグ。モード毎に保持するべき値についてはこちらに格納する
 * 
 * @author exch
 */
public class PlayConfig {

	/**
	 * ハイスピード。1.0で等速
	 */
	private float hispeed = 1.0f;
	/**
	 * デュレーション(ノーツ表示時間)
	 */
	private int duration = 500;

	/**
	 * レーンカバー表示量(0-1)
	 */
	private float lanecover = 0.2f;
	/**
	 * レーンカバー使用
	 */
	private boolean enablelanecover = true;
	/**
	 * リフト表示量(0-1)
	 */
	private float lift = 0.1f;
	/**
	 * リフト使用
	 */
	private boolean enablelift = false;

	private int[] keyassign = { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
			Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
			Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT, Keys.Q, Keys.W };
	
	private ControllerConfig[] controller = new ControllerConfig[1];
	
	public PlayConfig() {
		controller[0] = new ControllerConfig();
	}
	
	public PlayConfig(int[] keyassign, int[][] controller) {
		this.keyassign = keyassign;
		this.controller = new ControllerConfig[controller.length];
		for(int i = 0;i < controller.length;i++) {
			this.controller[i] = new ControllerConfig(controller[i]);
		}
	}
		
	public int[] getKeyassign() {
		return keyassign;
	}

	public void setKeyassign(int[] keyassign) {
		this.keyassign = keyassign;
	}

	public ControllerConfig[] getController() {
		return controller;
	}

	public void setController(ControllerConfig[] controllerassign) {
		this.controller = controllerassign;
	}

	public float getHispeed() {
		return hispeed;
	}

	public void setHispeed(float hispeed) {
		this.hispeed = hispeed;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public float getLanecover() {
		return lanecover;
	}

	public void setLanecover(float lanecover) {
		this.lanecover = lanecover;
	}

	public boolean isEnablelanecover() {
		return enablelanecover;
	}

	public void setEnablelanecover(boolean enablelanecover) {
		this.enablelanecover = enablelanecover;
	}

	public float getLift() {
		return lift;
	}

	public void setLift(float lift) {
		this.lift = lift;
	}

	public boolean isEnablelift() {
		return enablelift;
	}

	public void setEnablelift(boolean enablelift) {
		this.enablelift = enablelift;
	}

	public static class ControllerConfig {

		private String name = "";

		private int[] assign = { BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8,
				BMKeys.BUTTON_2, BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, BMKeys.BUTTON_9,
				BMKeys.BUTTON_10 };

		public ControllerConfig() {
			
		}
		
		public ControllerConfig(int[] assign) {
			this.assign = assign;
		}
		
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int[] getAssign() {
			return assign;
		}

		public void setAssign(int[] assign) {
			this.assign = assign;
		}
		
		
	}

}
