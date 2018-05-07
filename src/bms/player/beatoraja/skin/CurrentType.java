package bms.player.beatoraja.skin;

import java.util.HashMap;

public class CurrentType {
	public static final int NEUTRAL = 6;
	public static final int FEVER = 7;
	public static final int GREAT = 8;
	public static final int GOOD = 9;
	public static final int BAD = 10;
	public static final int FEVERWIN = 11;
	public static final int WIN = 12;
	public static final int LOSE = 13;
	public static final int OJAMA = 14;
	public static final int DANCE = 15;
	
	HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
	public CurrentType() {
		map.put(NEUTRAL, 1);
		map.put(FEVER, 6);
		map.put(GREAT, 7);
		map.put(GOOD, 8);
		map.put(BAD, 10);
		map.put(FEVERWIN, 17);
		map.put(WIN, 15);
		map.put(LOSE, 16);
		map.put(OJAMA, 3);
		map.put(DANCE, 14);
	}
	public int getType(int type) {
		return map.get(type);
	}
	
}
