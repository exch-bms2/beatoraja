package bms.player.beatoraja;

public class DataUtils {

    public static <T> T[] removeNullElements(T[] hash) {
    	int hashcount = hash.length;;
    	for(T sd : hash) {
    		if (sd == null) {
    			hashcount--;
    		}
    	}
    	if(hashcount < hash.length) {
    		T[] newhash = (T[]) new Object[hashcount];
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
