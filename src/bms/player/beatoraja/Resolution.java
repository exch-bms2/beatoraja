package bms.player.beatoraja;

public enum Resolution {
	SD(640, 480),
	HD(1280, 720),
	FWXGA(1366, 768),
	HDPLUS(1600, 900),
	FULLHD(1920, 1080),
	WQHD(2560, 1440),
	ULTRAHD(3840, 2160);

	public final int width;
	public final int height;

	private Resolution(int width, int height) {
		this.width = width;
		this.height = height;
	}
}
