package bms.player.beatoraja.playmode;

import java.util.Arrays;

import com.badlogic.gdx.Input.Keys;

import bms.model.Mode;


/**
 * �궘�꺖�깭�꺖�깋鼇�若싧츣獰?��뵪�궚��?�궧
 *
 * @author exch
 */
public class KeyboardConfig {

    private int[] keys;

    private int start;

    private int select;

    public KeyboardConfig() {
        this(Mode.BEAT_14K, true);
    }

    public KeyboardConfig(Mode mode, boolean enable) {
        this.setKeyAssign(mode, enable);
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
    
    public boolean isKeyAssigned() {
    	return keys != null;
    }
    
    public void setKeys(int[] newKeys) {
    	keys = newKeys;
    }
    
    public int[] getKeys() {
    	return keys;
    }
    
    public int getKeyLength() {
    	return keys.length;
    }

    public void setKeyAssign(Mode mode, boolean enable) {
        switch (mode) {
            case BEAT_5K:
            case BEAT_7K:
                keys = new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
                        Keys.CONTROL_LEFT };
                break;
            case BEAT_10K:
            case BEAT_14K:
            default:
                keys = new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
                        Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
                        Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT };
                break;
            case POPN_5K:
            case POPN_9K:
                keys = new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.G, Keys.B };
                break;
            case KEYBOARD_24K:
                keys = new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
                        Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
                        Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT };
                keys = Arrays.copyOf(keys, 26);
                break;
            case KEYBOARD_24K_DOUBLE:
                keys = new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
                        Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
                        Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT };
                keys = Arrays.copyOf(keys, 52);
                break;
        }
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
}
