package bms.player.beatoraja.playmode;

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
}
