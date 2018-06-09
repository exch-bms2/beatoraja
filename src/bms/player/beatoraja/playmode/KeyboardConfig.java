package bms.player.beatoraja.playmode;

import java.util.Arrays;

import com.badlogic.gdx.Input.Keys;

import bms.model.Mode;


/**
 * �궘�꺖�깭�꺖�깋鼇�若싧츣獰?��뵪�궚��?�궧
 *
 * @author exch
 */
public class KeyboardConfig extends IntConfiguration {
    public KeyboardConfig() {
        this(Mode.BEAT_14K, true);
    }

    public KeyboardConfig(Mode mode, boolean enable) {
        super.setKeyAssign(mode, -1, enable);
    }

    public KeyboardConfig(int[] keys, int start, int select) {
    	setKeys(keys);
    	setStart(start);
    	setSelect(select);
    }

    // set key assign
	@Override
	protected void setKey_beat_5K_7K(int player) {
		keys = new Integer[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
                Keys.CONTROL_LEFT };
	}

	@Override
	protected void setKey_beat_10K_14K(int player) {
		keys = new Integer[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
                Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
                Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT };
	}

	@Override
	protected void setKey_popn_5K_9K(int player) {
		keys = new Integer[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.G, Keys.B };
	}

	@Override
	protected void setKey_popn_24K(int player) {
		keys = new Integer[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
                Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
                Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT };
        keys = Arrays.copyOf(keys, 26);
	}

	@Override
	protected void setKey_popn_24K_double(int player) {
		keys = new Integer[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
                Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
                Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT };
        keys = Arrays.copyOf(keys, 52);
	}

	@Override
	protected void setKey_default(int player) {
		setKey_beat_10K_14K(player);
	}

	@Override
	protected void setKey_additional() {
		start = Keys.Q;
        select = Keys.W;
	}
}
