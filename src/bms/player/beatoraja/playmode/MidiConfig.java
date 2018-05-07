package bms.player.beatoraja.playmode;

import java.util.Arrays;

import bms.model.Mode;


public class MidiConfig extends Configuration<Input> {
	public MidiConfig() {
        this(Mode.BEAT_7K, true);
    }

    public MidiConfig(Mode mode, boolean enable) {
        this.setKeyAssign(mode, enable);
    }
    
    public Input[] getKeys() {
        return keys;
    }

    public void setKeys(Input[] keys) {
        this.keys = keys;
    }

    public Input getKey(int index) {
        return keys[index];
    }
    
    public void setKey(int index, Input input) {
        keys[index] = input;
    }

    public void setKeyAssign(Mode mode, boolean enable) {
        switch (mode) {
            case BEAT_5K:
            case BEAT_7K:
            default:
                // 7keys
                keys = new Input[9];
                for (int i = 0; i < 7; i++) {
                    keys[i] = new Input(Input.Type.NOTE, 53 + i);
                }
                keys[7] = new Input(Input.Type.NOTE, 49);
                keys[8] = new Input(Input.Type.NOTE, 51);
                start = new Input(Input.Type.NOTE, 47);
                select = new Input(Input.Type.NOTE, 48);
                break;
            case BEAT_10K:
            case BEAT_14K:
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
                break;
            case POPN_5K:
            case POPN_9K:
                keys = new Input[9];
                for (int i = 0; i < 9; i++) {
                    keys[i] = new Input(Input.Type.NOTE, 52 + i);
                }
                start = new Input(Input.Type.NOTE, 47);
                select = new Input(Input.Type.NOTE, 48);
                break;
            case KEYBOARD_24K:
                keys = new Input[26];
                for (int i = 0; i < 24; i++) {
                    keys[i] = new Input(Input.Type.NOTE, 48 + i);
                }
                keys[24] = new Input(Input.Type.PITCH_BEND, 1);
                keys[25] = new Input(Input.Type.PITCH_BEND, -1);
                start = new Input(Input.Type.NOTE, 44);
                select = new Input(Input.Type.NOTE, 46);
                break;
            case KEYBOARD_24K_DOUBLE:
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
                break;
        }
        if(!enable) {
            Arrays.fill(keys, null);
        }
    }
}
