package bms.player.beatoraja;

/**
 * BMSPlayerのモード
 * 
 * @author exch
 */
public class BMSPlayerMode {
	
	// TODO ReplayDataを含めたい
	
	public static final BMSPlayerMode PLAY = new BMSPlayerMode(Mode.PLAY);
	public static final BMSPlayerMode PRACTICE = new BMSPlayerMode(Mode.PRACTICE);
	public static final BMSPlayerMode AUTOPLAY = new BMSPlayerMode(Mode.AUTOPLAY);
	public static final BMSPlayerMode REPLAY_1 = new BMSPlayerMode(Mode.REPLAY, 0);
	public static final BMSPlayerMode REPLAY_2 = new BMSPlayerMode(Mode.REPLAY, 1);
	public static final BMSPlayerMode REPLAY_3 = new BMSPlayerMode(Mode.REPLAY, 2);
	public static final BMSPlayerMode REPLAY_4 = new BMSPlayerMode(Mode.REPLAY, 3);

	/**
	 * モード
	 */
	public final Mode mode;
	public final int id;
	
	public BMSPlayerMode(Mode mode) {
		this(mode, 0);
	}
	
	public BMSPlayerMode(Mode mode, int id) {
		this.mode = mode;
		this.id = id;
	}
	
	public static BMSPlayerMode getReplayMode(int index) {
		switch(index) {
		case 0:
			return REPLAY_1;
		case 1:
			return REPLAY_2;
		case 2:
			return REPLAY_3;
		case 3:
			return REPLAY_4;
		default:
			return null;
		}			
	}
	
	public enum Mode {
		PLAY, PRACTICE, AUTOPLAY, REPLAY;
	}
}