package bms.player.beatoraja.skin;

public class SkinDestinationSize {
	private int dstx;
	private int dsty;
	private int dstw;
	private int dsth;
	
	public SkinDestinationSize(int dstx, int dsty, int dstw, int dsth) {
		this.dstx= dstx;
		this.dsty= dsty;
		this.dstw= dstw;
		this.dsth= dsth;
	}
	public SkinDestinationSize(float dstx, float dsty, float dstw, float dsth) {
		this.dstx= (int)dstx;
		this.dsty= (int)dsty;
		this.dstw= (int)dstw;
		this.dsth= (int)dsth;
	}
	public int getDstx() {
		return dstx;
	}
	public void setDstx(int dstx) {
		this.dstx = dstx;
	}
	public int getDsty() {
		return dsty;
	}
	public void setDsty(int dsty) {
		this.dsty = dsty;
	}
	public int getDstw() {
		return dstw;
	}
	public void setDstw(int dstw) {
		this.dstw = dstw;
	}
	public int getDsth() {
		return dsth;
	}
	public void setDsth(int dsth) {
		this.dsth = dsth;
	}
	
	
}
