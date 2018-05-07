package bms.player.beatoraja.playmode;

import java.util.Arrays;

public abstract class IntConfiguration extends Configuration<Integer> {
	public int[] getKeys() {
		int[] toArray = new int[keys.length];
    	for (int i = 0; i < keys.length; i++)
    		toArray[i] = keys[i].intValue();
        return toArray;
	}
	
	public void setKeys(int[] keys) {
		this.keys = Arrays.stream(keys).boxed().toArray(Integer[]::new);
	}
	
	public int getKey(int i) {
    	return keys[i];
    }
	
	public void setKey(int i, int newKey) {
    	keys[i] = newKey;
    }
}
