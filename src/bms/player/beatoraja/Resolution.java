package bms.player.beatoraja;

/**
 * 解像度
 * 
 * @author exch
 */
public enum Resolution {
	SD(640, 480),
	SVGA(800, 600),
	XGA(1024, 768),
	HD(1280, 720),
	FWXGA(1366, 768),
	HDPLUS(1600, 900),
	WSXGAPLUS(1680,1050),
	FULLHD(1920, 1080),
	WUXGA(1920, 1200),
	WQHD(2560, 1440),
	ULTRAHD(3840, 2160);

	/**
	 * 幅
	 */
	public final int width;
	/**
	 * 高さ
	 */
	public final int height;

	private Resolution(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public String toString() {
		return name() + " (" + width + " x " + height + ")";
	}
}
