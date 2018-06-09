package bms.player.beatoraja.playmode;

import java.util.Arrays;

import com.badlogic.gdx.Input.Keys;

import bms.model.Mode;

public abstract class Configuration <T> {
	protected T[] keys;
	protected T start;
	protected T select;
		
    public boolean isKeyAssigned() {
    	return keys != null;
    }
    
    public int getKeyLength() {
    	return keys.length;
    }
    
    // start
    public T getStart() {
    	return start;
    }
    
    public void setStart(T start) {
    	this.start = start;
    }
    
    // select
    public T getSelect() {
    	return select;
    }
    
    public void setSelect(T select) {
    	this.select = select;
    }
    
    // the value of player is only required for ControllerConfig
    public void setKeyAssign(Mode mode, int player, boolean enable) {
		switch (mode) {
            case BEAT_5K:
            case BEAT_7K:
            	setKey_beat_5K_7K(player);
                break;
            case BEAT_10K:
            case BEAT_14K:
            	setKey_beat_10K_14K(player);
                break;
            case POPN_5K:
            case POPN_9K:
                setKey_popn_5K_9K(player);
                break;
            case KEYBOARD_24K:
            	setKey_popn_24K(player);
                break;
            case KEYBOARD_24K_DOUBLE:
            	setKey_popn_24K_double(player);
                break;
            default:
            	setKey_default(player);
            	break;
		}
    	if (!enable) {
    		Arrays.fill(keys, -1);
    	}
    	setKey_additional();
	}
    
    protected abstract void setKey_beat_5K_7K(int player);
    protected abstract void setKey_beat_10K_14K(int player);
    protected abstract void setKey_popn_5K_9K(int player);
    protected abstract void setKey_popn_24K(int player);
    protected abstract void setKey_popn_24K_double(int player);
    protected abstract void setKey_default(int player);
    protected abstract void setKey_additional();
}
