package bms.player.beatoraja.skin;

public class SkinOption {
	private int dstOpt1;
	private int dstOpt2;
	private int dstOpt3;
	private int dstOffset;
	
	public SkinOption(int dstOpt1, int dstOpt2, int dstOpt3, int dstOffset){
		this.dstOpt1 = dstOpt1;
		this.dstOpt2 = dstOpt2;
		this.dstOpt3 = dstOpt3;
		this.dstOffset = dstOffset;
	}

	public int getDstOpt1() {
		return dstOpt1;
	}

	public void setDstOpt1(int dstOpt1) {
		this.dstOpt1 = dstOpt1;
	}

	public int getDstOpt2() {
		return dstOpt2;
	}

	public void setDstOpt2(int dstOpt2) {
		this.dstOpt2 = dstOpt2;
	}

	public int getDstOpt3() {
		return dstOpt3;
	}

	public void setDstOpt3(int dstOpt3) {
		this.dstOpt3 = dstOpt3;
	}

	public int getDstOffset() {
		return dstOffset;
	}

	public void setDstOffset(int dstOffset) {
		this.dstOffset = dstOffset;
	}
	
	
}
