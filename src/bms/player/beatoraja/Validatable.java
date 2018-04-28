package bms.player.beatoraja;

public interface Validatable {
	
	public static final Object[] EMPTY = new Object[0];

	public boolean validate();

    public static <T> T[] removeInvalidElements(T[] hash) {
    	if(hash == null) {
    		return (T[]) EMPTY;
    	}
    	int hashcount = hash.length;;
    	for(int i = 0;i < hash.length;i++) {
    		if (hash[i] == null || (hash[i] instanceof Validatable && !((Validatable)hash[i]).validate())) {
    			hash[i] = null;
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
