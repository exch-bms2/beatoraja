package bms.player.beatoraja;

import java.lang.reflect.Array;

public interface Validatable {
	
	public boolean validate();

    public static <T> T[] removeInvalidElements(T[] hash) {
    	int hashcount = hash.length;;
    	for(int i = 0;i < hash.length;i++) {
    		if (hash[i] == null || (hash[i] instanceof Validatable && !((Validatable)hash[i]).validate())) {
    			hash[i] = null;
    			hashcount--;
    		}
    	}
    	if(hashcount < hash.length) {
    		T[] newhash = (T[]) Array.newInstance(hash.getClass().getComponentType(), hashcount);
    		int i = 0;
        	for(T sd : hash) {
        		if (sd != null) {
        			newhash[i++] = sd;
        		}
        	}
        	return newhash;
    	}
    	return hash;
    }
}
