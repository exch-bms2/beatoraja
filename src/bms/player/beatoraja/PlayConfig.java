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

	private KeyboardConfig keyboard = new KeyboardConfig();
	
	private ControllerConfig[] controller = new ControllerConfig[] { ControllerConfig.default7() };

	private MidiConfig midi = new MidiConfig();
	
	public PlayConfig() {
	}
	
	public PlayConfig(KeyboardConfig keyboard, ControllerConfig[] controllers, MidiConfig midi) {
		this.keyboard = keyboard;
		this.controller = controllers.clone();
		this.midi = midi;
	}
		
	public KeyboardConfig getKeyboardConfig() {
		return keyboard;
	}

	public void setKeyboardConfig(KeyboardConfig keyboard) {
		this.keyboard = keyboard;
	}

	public ControllerConfig[] getController() {
		return controller;
	}

	public MidiConfig getMidiConfig() {
		return midi;
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

	public static class KeyboardConfig {

		private int[] keys = { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
				Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
				Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT };

		private int start = Keys.Q;

		private int select = Keys.W;

		public KeyboardConfig() {

		}

		public KeyboardConfig(int[] keys, int start, int select) {
			this.keys = keys;
			this.start = start;
			this.select = select;
		}

		public int[] getKeyAssign() {
			return keys;
		}

		public int getStart() {
			return start;
		}

		public int getSelect() {
			return select;
		}

		public void setKeyAssign(int[] keys) {
			this.keys = keys;
		}

		public void setStart(int start) {
			this.start = start;
		}

		public void setSelect(int select) {
			this.select = select;
		}

		public static KeyboardConfig default14() {
			return new KeyboardConfig();
		}

		public static KeyboardConfig default9() {
			KeyboardConfig config = new KeyboardConfig();
			config.keys = new int [] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.G, Keys.B, Keys.COMMA, Keys.L,
					Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE, Keys.UNKNOWN, Keys.SHIFT_RIGHT,
					Keys.CONTROL_RIGHT };
			config.start = Keys.Q;
			config.select = Keys.W;
			return config;
		}
	}

	public static class ControllerConfig {

		private String name = "";

		private int[] keys = { BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8,
				BMKeys.BUTTON_2, BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN };

		private int start = BMKeys.BUTTON_9;

		private int select = BMKeys.BUTTON_10;

		public ControllerConfig() {
			
		}
		
		public ControllerConfig(int[] keys, int start, int select) {
			this.keys = keys;
			this.start = start;
			this.select = select;
		}
		
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int[] getKeyAssign() {
			return keys;
		}

		public int getStart() { return start; }

		public int getSelect() { return select; }

		public void setKeyAssign(int[] keys) {
			this.keys = keys;
		}

		public void setStart(int start) {
			this.start = start;
		}

		public void setSelect(int select) {
			this.select = select;
		}
		
		public static ControllerConfig default7() {
			return new ControllerConfig();
		}

		public static ControllerConfig default9() {
			return new ControllerConfig();
		}
	}

	public static class MidiConfig {

		public static class Input {
			public enum Type {
				NOTE,
				PITCH_BEND,
				CONTROL_CHANGE,
			}
			public Type type;
			public int value;
			public Input() {
				this.type = Type.NOTE;
				this.value = 0;
			}
			public Input(Input input) {
				this.type = input.type;
				this.value = input.value;
			}
			public Input(Type type, int value) {
				this.type = type;
				this.value = value;
			}
			public String toString() {
				switch (type) {
				case NOTE:
					return "NOTE " + value;
				case PITCH_BEND:
					return "PITCH " + (value > 0 ? "+" : "-");
				case CONTROL_CHANGE:
					return "CC " + value;
				default:
					return null;
				}
			}
		}

		private Input[] keys;
		private Input start;
		private Input select;

		public Input[] getKeys() { return keys; }
		public void setKeys(Input[] keys) { this.keys = keys; }
		public Input getStart() { return start; }
		public Input getSelect() { return select; }
		public void setStart(Input input) { start = input; }
		public void setSelect(Input input) { select = input; }

		public MidiConfig() {
			// 7keys
			keys = new Input[9];
			for (int i=0; i<7; i++) {
				keys[i] = new Input(Input.Type.NOTE, 53 + i);
			}
			keys[7] = new Input(Input.Type.NOTE, 49);
			keys[8] = new Input(Input.Type.NOTE, 51);
			start = new Input(Input.Type.NOTE, 47);
			select = new Input(Input.Type.NOTE, 48);
		}

		public Input getKeyAssign(int index) {
			return keys[index];
		}

		public void setKeyAssign(int index, Input input) {
			keys[index] = input;
		}

		public static MidiConfig default7() {
			return new MidiConfig();
		}

		public static MidiConfig default14() {
			MidiConfig config = new MidiConfig();
			config.keys = new Input[18];
			for (int i=0; i<7; i++) {
				// 1P keys
				config.keys[i] = new Input(Input.Type.NOTE, 53 + i);
				// 2P keys
				config.keys[9 + i] = new Input(Input.Type.NOTE, 65 + i);
			}
			// 1P turntables
			config.keys[7] = new Input(Input.Type.NOTE, 49);
			config.keys[8] = new Input(Input.Type.NOTE, 51);
			// 2P turntables
			config.keys[16] = new Input(Input.Type.NOTE, 73);
			config.keys[17] = new Input(Input.Type.NOTE, 75);
			// start/select
			config.start = new Input(Input.Type.NOTE, 47);
			config.select = new Input(Input.Type.NOTE, 48);
			return config;
		}

		public static MidiConfig default9() {
			MidiConfig config = new MidiConfig();
			config.keys = new Input[9];
			for (int i=0; i<9; i++) {
				config.keys[i] = new Input(Input.Type.NOTE, 52 + i);
			}
			config.start = new Input(Input.Type.NOTE, 47);
			config.select = new Input(Input.Type.NOTE, 48);
			return config;
		}
	}

}
