package bms.model;

/**
 * ノート
 * 
 * @author exch
 */
public abstract class Note {

	/**
	 * アサインされている 音源ID
	 */
	private int wav;
	
	private int state;

	public int getWav() {
		return wav;
	}

	public void setWav(int wav) {
		this.wav = wav;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}
}
