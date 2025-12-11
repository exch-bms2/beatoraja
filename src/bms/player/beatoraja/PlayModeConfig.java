package bms.player.beatoraja;

import java.util.Arrays;

import com.badlogic.gdx.Input.Keys;

import bms.model.Mode;
import bms.player.beatoraja.input.BMControllerInputProcessor.BMKeys;
import com.badlogic.gdx.math.MathUtils;

/**
 * プレイコンフィグ。モード毎に保持するべき値についてはこちらに格納する
 *
 * @author exch
 */
public final class PlayModeConfig {

    private PlayConfig playconfig = new PlayConfig();
    /**
     * キーボード設定
     */
    private KeyboardConfig keyboard = new KeyboardConfig();
    /**
     * コントローラー設定
     */
    private ControllerConfig[] controller = new ControllerConfig[] { new ControllerConfig() };
    /**
     * MIDI設定
     */
    private MidiConfig midi = new MidiConfig();

    private int version;

    public PlayModeConfig() {
        this(Mode.BEAT_7K);
    }

    public PlayModeConfig(Mode mode) {
        boolean midi = (mode == Mode.KEYBOARD_24K || mode == Mode.KEYBOARD_24K_DOUBLE);
        this.keyboard = new KeyboardConfig(mode, !midi);
        controller = new ControllerConfig[mode.player];
        for(int i = 0;i < controller.length;i++) {
            controller[i] = new ControllerConfig(mode, i, false);
        }
        this.midi = new MidiConfig(mode, midi);
    }

    public PlayModeConfig(KeyboardConfig keyboard, ControllerConfig[] controllers, MidiConfig midi) {
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

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}
	
    public void validate(int keys) {
        if(playconfig == null) {
            playconfig = new PlayConfig();
        }

        playconfig.validate();

        if (keyboard.keys == null) {
            keyboard.keys = new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
                    Keys.CONTROL_LEFT };
        }
        if (keyboard.keys.length != keys) {
            keyboard.keys = Arrays.copyOf(keyboard.keys, keys);
        }
        keyboard.duration = MathUtils.clamp(keyboard.duration, 0, 100);

        MouseScratchConfig mousescratch = keyboard.mouseScratchConfig;
        if (mousescratch.keys == null || mousescratch.keys.length != keys) {
            mousescratch.keys = new int[keys];
            Arrays.fill(mousescratch.keys, -1);
        }
        mousescratch.mouseScratchDistance = MathUtils.clamp(mousescratch.mouseScratchDistance, 1, 10000);
        mousescratch.mouseScratchTimeThreshold = MathUtils.clamp(mousescratch.mouseScratchTimeThreshold, 1, 10000);

        int index = 0;
        for (ControllerConfig c : controller) {
            if (c.keys == null) {
                c.keys = new int[] { BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8,
                        BMKeys.BUTTON_2, BMKeys.BUTTON_5, BMKeys.AXIS2_PLUS, BMKeys.AXIS1_PLUS, BMKeys.AXIS1_MINUS };
            }
            if (c.keys.length != keys) {
                int[] newkeys = new int[keys];
                Arrays.fill(newkeys, -1);
                for (int i = 0; i < c.keys.length && index < newkeys.length; i++, index++) {
                    newkeys[index] = c.keys[i];
                }
                c.keys = newkeys;
            }
            c.duration = MathUtils.clamp(c.duration, 0, 100);            
        }
        
		// ボタsン数拡張(16->32)に伴う変換(0.8.1 -> 0.8.2)。あとで消す
		if(version == 0) {
	        for (ControllerConfig c : controller) {
				for(int i = 0;i < c.keys.length;i++) {
					if(c.keys[i] >= BMKeys.BUTTON_17 && c.keys[i] <= BMKeys.BUTTON_20) {
						c.keys[i] += BMKeys.AXIS1_PLUS - BMKeys.BUTTON_17;
					}
				}	        	
	        }
			version = 1;
		}

        if (midi.keys == null) {
            midi.keys = new MidiConfig().keys;
        }
        if (midi.keys.length != keys) {
            midi.keys = Arrays.copyOf(midi.keys, keys);
        }

        // KB, コントローラー, Midiの各ボタンについて排他的処理を実施
        boolean[] exclusive = new boolean[keyboard.keys.length];
        validate0(keyboard.keys,  exclusive);
        for(int i = 0;i < controller.length;i++) {
            validate0(controller[i].keys,  exclusive);
        }

        for(int i = 0;i < midi.getKeys().length;i++) {
            if(exclusive[i]) {
                midi.getKeys()[i] = null;
            }
        }
    }

    private void validate0(int[] keys, boolean[] exclusive) {
        for(int i = 0;i < exclusive.length;i++) {
            if(exclusive[i]) {
                keys[i] = -1;
            } else if(keys[i] != -1){
                exclusive[i] = true;
            }
        }
    }

    public PlayConfig getPlayconfig() {
        return playconfig;
    }

    public void setPlayconfig(PlayConfig playconfig) {
        this.playconfig = playconfig;
    }

    /**
     * キーボード設定定義用クラス
     *
     * @author exch
     */
    public static final class KeyboardConfig {
        /**
         * マウス皿設定
         */
        private final MouseScratchConfig mouseScratchConfig;

        private int[] keys;

        private int start;

        private int select;

        private int duration = 16;

        public KeyboardConfig() {
            this(Mode.BEAT_14K, true);
        }

        public KeyboardConfig(Mode mode, boolean enable) {
            this.setKeyAssign(mode, enable);
            mouseScratchConfig = new MouseScratchConfig(mode);
        }

        public MouseScratchConfig getMouseScratchConfig() {
            return mouseScratchConfig;
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

        public void setKeyAssign(Mode mode, boolean enable) {
            keys = switch (mode) {
                case BEAT_5K -> new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.SHIFT_LEFT, Keys.CONTROL_LEFT };
                case BEAT_7K -> new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT, Keys.CONTROL_LEFT };
                case BEAT_10K -> new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.SHIFT_LEFT,
                            Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT };
                case BEAT_14K -> new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
                            Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
                            Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT };
                case POPN_5K, POPN_9K -> new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.G, Keys.B };
                case KEYBOARD_24K -> Arrays.copyOf(new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
                            Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
                            Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT }, 26);
                case KEYBOARD_24K_DOUBLE -> Arrays.copyOf(new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
                            Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
                            Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT }, 52);
                default -> new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
                            Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
                            Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT };
            };
            if(!enable) {
                Arrays.fill(keys, -1);
            }
            start = Keys.Q;
            select = Keys.W;
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

        public int getDuration() {
            return duration;
        }

        public void setDuration(int inputduration) {
            this.duration = inputduration;
        }
    }

    /**
     * マウス皿設定定義用クラス
     */
    public static final class MouseScratchConfig {
        public static final int MOUSE_SCRATCH_VER_2 = 0;
        public static final int MOUSE_SCRATCH_VER_1 = 1;

        private static final String[] MOUSESCRATCH_STRING = new String[]{
            "MOUSE RIGHT",
            "MOUSE LEFT",
            "MOUSE DOWN",
            "MOUSE UP",
        };

        private int[] keys;

        private int start = -1;

        private int select = -1;

        /**
         * マウス皿を利用すてる
         */
        private boolean mouseScratchEnabled = false;
        /**
         * スクラッチ停止閾値 (ms)
         */
        private int mouseScratchTimeThreshold = 150;
        /**
         * スクラッチ距離
         */
        private int mouseScratchDistance = 12;
        /**
         * マウス皿モード
         */
        private int mouseScratchMode = 0;
        
        public MouseScratchConfig() {
            this(Mode.BEAT_7K);
        }

        public MouseScratchConfig(Mode mode) {
            this.setKeyAssign(mode);
        }


        public void setKeyAssign(Mode mode) {
            keys = new int[switch (mode) {
            	case BEAT_5K -> 7;
            	case BEAT_7K -> 9;
            	case BEAT_10K -> 14;
            	case BEAT_14K -> 18;
            	case POPN_5K,POPN_9K -> 9;
            	case KEYBOARD_24K -> 26;
            	case KEYBOARD_24K_DOUBLE -> 52;
            	default -> 18;
            }];
            Arrays.fill(keys, -1);
            start = -1;
            select = -1;
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

        public boolean isMouseScratchEnabled() {
            return mouseScratchEnabled;
        }

        public void setMouseScratchEnabled(boolean mouseScratchEnabled) {
            this.mouseScratchEnabled = mouseScratchEnabled;
        }

        public int getMouseScratchTimeThreshold() {
            return mouseScratchTimeThreshold;
        }

        public void setMouseScratchTimeThreshold(int mouseScratchTimeThreshold) {
            this.mouseScratchTimeThreshold = mouseScratchTimeThreshold > 0 ? mouseScratchTimeThreshold : 1;
        }

        public int getMouseScratchDistance() {
            return mouseScratchDistance;
        }

        public void setMouseScratchDistance(int mouseScratchDistance) {
            this.mouseScratchDistance = mouseScratchDistance > 0 ? mouseScratchDistance : 1;
        }

        public int getMouseScratchMode() {
            return mouseScratchMode;
        }

        public void setMouseScratchMode(int mouseScratchMode) {
            this.mouseScratchMode = mouseScratchMode;
        }

        public String getKeyString(int index) {
            if (keys[index] == -1) return null;
            return MOUSESCRATCH_STRING[keys[index]];
        }

        public String getStartString() {
            if (start == -1) return null;
            return MOUSESCRATCH_STRING[start];
        }

        public String getSelectString() {
            if (select == -1) return null;
            return MOUSESCRATCH_STRING[select];
        }
    }

    /**
     * コントローラー設定定義用クラス
     *
     * @author exch
     */
    public static final class ControllerConfig {

        public static final int ANALOG_SCRATCH_VER_2 = 0;
        
        public static final int ANALOG_SCRATCH_VER_1 = 1;

        private String name = "";

        private int[] keys;

        private int start;

        private int select;

        private int duration = 16;
        /**
         * JKOC Hack (boolean) private variable
         */
        private boolean jkoc_hack = false;
        /**
         * アナログスクラッチを利用するか(INFINITASコントローラの場合true)
         */
        private boolean analogScratch = false;
        /**
         * アナログスクラッチモード
         */
        private int analogScratchMode = 0;
        /**
         * アナログスクラッチ停止閾値
         */
        private int analogScratchThreshold = 100;

        private static final ControllerConfig IIDX_PS2 = new ControllerConfig(new int[] { BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8,
				BMKeys.BUTTON_2, BMKeys.BUTTON_5, BMKeys.AXIS4_MINUS, BMKeys.AXIS3_MINUS, BMKeys.AXIS3_PLUS }, 
					BMKeys.BUTTON_9, BMKeys.BUTTON_10);
        private static final ControllerConfig DAO = new ControllerConfig(new int[] { BMKeys.BUTTON_1, BMKeys.BUTTON_2, BMKeys.BUTTON_3, BMKeys.BUTTON_4,
					BMKeys.BUTTON_5, BMKeys.BUTTON_6, BMKeys.BUTTON_7, BMKeys.AXIS1_PLUS, BMKeys.AXIS1_MINUS }, 
						BMKeys.BUTTON_9, BMKeys.BUTTON_10);
        private static final ControllerConfig IIDX_PREMIUM = new ControllerConfig(new int[] { BMKeys.BUTTON_1, BMKeys.BUTTON_2, BMKeys.BUTTON_3, BMKeys.BUTTON_4,
					BMKeys.BUTTON_5, BMKeys.BUTTON_6, BMKeys.BUTTON_7, BMKeys.AXIS1_MINUS, BMKeys.AXIS1_PLUS }, 
						BMKeys.BUTTON_9, BMKeys.BUTTON_10);

        public ControllerConfig() {
            this(Mode.BEAT_7K, 0, true);
        }

        public ControllerConfig(Mode mode, int player, boolean enable) {
            this.setKeyAssign(mode, player, enable);
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

        public int getStart() {
            return start;
        }

        public int getSelect() {
            return select;
        }

        public void setKeyAssign(Mode mode, int player, boolean enable) {
        	final ControllerConfig con = IIDX_PS2;
            if(player == 0) {
                keys = switch (mode) {
                	case BEAT_5K -> new int[]{ con.keys[0], con.keys[1], con.keys[2], con.keys[3], con.keys[4],con.keys[7], con.keys[8]};
                	case BEAT_7K, POPN_5K, POPN_9K -> new int[]{ con.keys[0], con.keys[1], con.keys[2], con.keys[3], con.keys[4],con.keys[5], con.keys[6],con.keys[7], con.keys[8]};
                	case BEAT_10K -> new int[]{ con.keys[0], con.keys[1], con.keys[2], con.keys[3], con.keys[4],con.keys[7], con.keys[8], -1,-1,-1,-1,-1,-1,-1 };
                	case BEAT_14K -> new int[]{ con.keys[0], con.keys[1], con.keys[2], con.keys[3], con.keys[4],con.keys[5], con.keys[6],con.keys[7], con.keys[8]
                    		, -1,-1,-1,-1,-1,-1,-1,-1,-1 };
                	case KEYBOARD_24K -> Arrays.copyOf(con.keys, 26);
                	case KEYBOARD_24K_DOUBLE -> Arrays.copyOf(con.keys, 52);
                	default -> new int[]{ con.keys[0], con.keys[1], con.keys[2], con.keys[3], con.keys[4],con.keys[5], con.keys[6],con.keys[7], con.keys[8]};
                };
            } else {
            	keys = switch (mode) {
                	case BEAT_5K,BEAT_7K,POPN_5K,POPN_9K -> new int[] {-1,-1,-1,-1,-1,-1,-1,-1,-1};
                	case BEAT_10K -> new int[]{-1,-1,-1,-1,-1,-1,-1,con.keys[0], con.keys[1], con.keys[2], con.keys[3], con.keys[4],con.keys[7], con.keys[8]};
                	case BEAT_14K -> new int[]{-1,-1,-1,-1,-1,-1,-1,-1,-1,con.keys[0], con.keys[1], con.keys[2], con.keys[3], con.keys[4],con.keys[5], con.keys[6],con.keys[7], con.keys[8]};
                	case KEYBOARD_24K -> Arrays.copyOf(new int[]{-1,-1,-1,-1,-1,-1,-1,-1,-1,con.keys[0], con.keys[1], con.keys[2], con.keys[3], con.keys[4],con.keys[5], con.keys[6],con.keys[7], con.keys[8]}, 26);
                	case KEYBOARD_24K_DOUBLE -> keys = Arrays.copyOf(new int[]{-1,-1,-1,-1,-1,-1,-1,-1,-1,con.keys[0], con.keys[1], con.keys[2], con.keys[3], con.keys[4],con.keys[5], con.keys[6],con.keys[7], con.keys[8]}, 52);
                	default -> new int[] {-1,-1,-1,-1,-1,-1,-1,-1,-1};
                };
            }
            if(!enable) {
                Arrays.fill(keys, -1);
            }
            start = con.start;
            select = con.select;
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

        public int getDuration() {
            return duration;
        }

        public void setDuration(int inputduration) {
            this.duration = inputduration;
        }

        public boolean getJKOC()  {
            return jkoc_hack;
        }

        public void setJKOC(boolean jkoc)  {
            this.jkoc_hack = jkoc;
        }

        public boolean isAnalogScratch() {
            return analogScratch;
        }

        public void setAnalogScratch(boolean analogScratch) {
            this.analogScratch = analogScratch;
        }
        
        public int getAnalogScratchMode() {
            return analogScratchMode;
        }

        public void setAnalogScratchMode(int analogScratchMode) {
            this.analogScratchMode = analogScratchMode;
        }
        
        public int getAnalogScratchThreshold() {
            return this.analogScratchThreshold;
        }

        public void setAnalogScratchThreshold(int analogScratchThreshold) {
            this.analogScratchThreshold = 
            	analogScratchThreshold > 0 ? 
            		analogScratchThreshold <= 1000 ? analogScratchThreshold : 1000 
    			:1;
        }
    }

    public static final class MidiConfig {
    	
        public static final class Input {
            public enum Type {
                NOTE, PITCH_BEND, CONTROL_CHANGE,
            }

            public Type type;
            public int value;

            public Input() {
            	this(Type.NOTE, 0);
            }

            public Input(Input input) {
            	this(input.type, input.value);
            }

            public Input(Type type, int value) {
                this.type = type;
                this.value = value;
            }

            public String toString() {
                return switch (type) {
                	case NOTE -> "NOTE " + value;
                	case PITCH_BEND -> "PITCH " + (value > 0 ? "+" : "-");
                	case CONTROL_CHANGE -> "CC " + value;
                	default -> null;
                };
            }
        }

        private Input[] keys;
        private Input start;
        private Input select;

        public Input[] getKeys() {
            return keys;
        }

        public void setKeys(Input[] keys) {
            this.keys = keys;
        }

        public Input getStart() {
            return start;
        }

        public Input getSelect() {
            return select;
        }

        public void setStart(Input input) {
            start = input;
        }

        public void setSelect(Input input) {
            select = input;
        }

        public MidiConfig() {
            this(Mode.BEAT_7K, true);
        }

        public MidiConfig(Mode mode, boolean enable) {
            this.setKeyAssign(mode, enable);
        }

        public Input getKeyAssign(int index) {
            return keys[index];
        }

        public void setKeyAssign(Mode mode, boolean enable) {
            switch (mode) {
                case BEAT_5K -> {
                    // 5keys
                    keys = new Input[7];
                    for (int i = 0; i < 5; i++) {
                        keys[i] = new Input(Input.Type.NOTE, 53 + i);
                    }
                    keys[5] = new Input(Input.Type.NOTE, 49);
                    keys[6] = new Input(Input.Type.NOTE, 51);
                    start = new Input(Input.Type.NOTE, 47);
                    select = new Input(Input.Type.NOTE, 48);
                }
                case BEAT_7K -> {
                    // 7keys
                    keys = new Input[9];
                    for (int i = 0; i < 7; i++) {
                        keys[i] = new Input(Input.Type.NOTE, 53 + i);
                    }
                    keys[7] = new Input(Input.Type.NOTE, 49);
                    keys[8] = new Input(Input.Type.NOTE, 51);
                    start = new Input(Input.Type.NOTE, 47);
                    select = new Input(Input.Type.NOTE, 48);
                }
                case BEAT_10K -> {
                    keys = new Input[14];
                    for (int i = 0; i < 5; i++) {
                        // 1P keys
                        keys[i] = new Input(Input.Type.NOTE, 53 + i);
                        // 2P keys
                        keys[7 + i] = new Input(Input.Type.NOTE, 65 + i);
                    }
                    // 1P turntables
                    keys[5] = new Input(Input.Type.NOTE, 49);
                    keys[6] = new Input(Input.Type.NOTE, 51);
                    // 2P turntables
                    keys[12] = new Input(Input.Type.NOTE, 73);
                    keys[13] = new Input(Input.Type.NOTE, 75);
                    start = new Input(Input.Type.NOTE, 47);
                    select = new Input(Input.Type.NOTE, 48);
                }
                case BEAT_14K -> {
                    keys = new Input[18];
                    for (int i = 0; i < 7; i++) {
                        // 1P keys
                        keys[i] = new Input(Input.Type.NOTE, 53 + i);
                        // 2P keys
                        keys[9 + i] = new Input(Input.Type.NOTE, 65 + i);
                    }
                    // 1P turntables
                    keys[7] = new Input(Input.Type.NOTE, 49);
                    keys[8] = new Input(Input.Type.NOTE, 51);
                    // 2P turntables
                    keys[16] = new Input(Input.Type.NOTE, 73);
                    keys[17] = new Input(Input.Type.NOTE, 75);
                    start = new Input(Input.Type.NOTE, 47);
                    select = new Input(Input.Type.NOTE, 48);
                }
                case POPN_5K, POPN_9K -> {
                    keys = new Input[9];
                    for (int i = 0; i < 9; i++) {
                        keys[i] = new Input(Input.Type.NOTE, 52 + i);
                    }
                    start = new Input(Input.Type.NOTE, 47);
                    select = new Input(Input.Type.NOTE, 48);
                }
                case KEYBOARD_24K -> {
                    keys = new Input[26];
                    for (int i = 0; i < 24; i++) {
                        keys[i] = new Input(Input.Type.NOTE, 48 + i);
                    }
                    keys[24] = new Input(Input.Type.PITCH_BEND, 1);
                    keys[25] = new Input(Input.Type.PITCH_BEND, -1);
                    start = new Input(Input.Type.NOTE, 44);
                    select = new Input(Input.Type.NOTE, 46);
                }
                case KEYBOARD_24K_DOUBLE -> {
                    keys = new Input[52];
                    for (int i = 0; i < 24; i++) {
                        keys[i] = new Input(Input.Type.NOTE, 48 + i);
                        keys[i + 26] = new Input(Input.Type.NOTE, 72 + i);
                    }
                    keys[24] = new Input(Input.Type.PITCH_BEND, 1);
                    keys[25] = new Input(Input.Type.PITCH_BEND, -1);
                    keys[50] = new Input(Input.Type.NOTE, 99);
                    keys[51] = new Input(Input.Type.NOTE, 97);
                    start = new Input(Input.Type.NOTE, 44);
                    select = new Input(Input.Type.NOTE, 46);
                }
            }
            if(!enable) {
                Arrays.fill(keys, null);
            }
        }

        public void setKeyAssign(int index, Input input) {
            keys[index] = input;
        }
    }
}
