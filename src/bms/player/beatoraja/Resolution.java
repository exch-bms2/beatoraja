package bms.player.beatoraja;

public enum Resolution {
	SD(640, 480),
	HD(1280, 720),
	FULLHD(1920, 1080),
	ULTRAHD(3840, 2160);

	public final int width;
	public final int height;

	private Resolution(int width, int height) {
		this.width = width;
		this.height = height;
	}
}
