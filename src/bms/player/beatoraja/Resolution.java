package bms.player.beatoraja;

import com.badlogic.gdx.math.Rectangle;

public class Resolution {

	public static final int RESOLUTION_SD = 0;
	public static final int RESOLUTION_HD = 1;
	public static final int RESOLUTION_FULLHD = 2;
	public static final int RESOLUTION_ULTRAHD = 3;
	
	public static final Rectangle[] RESOLUTION = { new Rectangle(0, 0, 640, 480), new Rectangle(0, 0, 1280, 720),
		new Rectangle(0, 0, 1920, 1080), new Rectangle(0, 0, 3840, 2160) };

}
