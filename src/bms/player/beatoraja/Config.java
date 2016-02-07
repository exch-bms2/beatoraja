package bms.player.beatoraja;

public class Config {
	
	private boolean fullscreen;
	
	private boolean vsync;
	
	private int audioDeviceBufferSize = 384;
	
	private int audioDeviceSimultaneousSources = 64;
	
	private int maxFramePerSecond = 240;
	
	private int gauge = 0;
	
	private int random;
	
	private float hispeed = 1.0f;
	
	private boolean fixhispeed = true;
	
	private int greenvalue = 300;
	
	private float lanecover = 0.2f;
	
	private boolean enablelanecover = true;

	private float lift = 0.1f;
	
	private boolean enablelift = true;
	
	private int judgetiming = 0;
	
	private int judgedetail = 0;
	
	private boolean constant = false;
	
	private boolean bpmguide = false;
	
	private int lnassist = 0;
	
	private String lr2playskin;
	
	private String[] bmsroot = new String[0];
	
	private String[] tableURL = new String[0];
	
	private int bga = BGA_OFF;
	public static final int BGA_ON = 0;
	public static final int BGA_AUTO = 1;
	public static final int BGA_OFF = 2;	
	
	private String vlcpath = "";
	
	public Config() {
//		lr2playskin = "skin/spdframe/csv/left_ACwide.csv";
		tableURL = new String[]{"http://bmsnormal2.syuriken.jp/table.html"};
		judgedetail = 2;
	}
	
	public boolean isFullscreen() {
		return fullscreen;
	}

	public void setFullscreen(boolean fullscreen) {
		this.fullscreen = fullscreen;
	}

	public boolean isVsync() {
		return vsync;
	}

	public void setVsync(boolean vsync) {
		this.vsync = vsync;
	}

	public int getGauge() {
		return gauge;
	}

	public void setGauge(int gauge) {
		this.gauge = gauge;
	}

	public int getRandom() {
		return random;
	}

	public void setRandom(int random) {
		this.random = random;
	}

	public float getHispeed() {
		return hispeed;
	}

	public void setHispeed(float hispeed) {
		this.hispeed = hispeed;
	}

	public boolean isFixhispeed() {
		return fixhispeed;
	}

	public void setFixhispeed(boolean fixhispeed) {
		this.fixhispeed = fixhispeed;
	}

	public int getGreenvalue() {
		return greenvalue;
	}

	public void setGreenvalue(int greenvalue) {
		this.greenvalue = greenvalue;
	}

	public float getLanecover() {
		return lanecover;
	}

	public void setLanecover(float lanecover) {
		this.lanecover = lanecover;
	}

	public boolean isEnablelanecover() {
		return enablelanecover;
	}

	public void setEnablelanecover(boolean enablelanecover) {
		this.enablelanecover = enablelanecover;
	}

	public float getLift() {
		return lift;
	}

	public void setLift(float lift) {
		this.lift = lift;
	}

	public boolean isEnablelift() {
		return enablelift;
	}

	public void setEnablelift(boolean enablelift) {
		this.enablelift = enablelift;
	}

	public int getBga() {
		return bga;
	}

	public void setBga(int bga) {
		this.bga = bga;
	}

	public int getJudgetiming() {
		return judgetiming;
	}

	public void setJudgetiming(int judgetiming) {
		this.judgetiming = judgetiming;
	}
	
	public boolean isConstant() {
		return constant;
	}

	public void setConstant(boolean constant) {
		this.constant = constant;
	}

	public boolean isBpmguide() {
		return bpmguide;
	}

	public void setBpmguide(boolean bpmguide) {
		this.bpmguide = bpmguide;
	}

	public int getLnassist() {
		return lnassist;
	}

	public void setLnassist(int lnassist) {
		this.lnassist = lnassist;
	}

	public String getVlcpath() {
		return vlcpath;
	}

	public void setVlcpath(String vlcpath) {
		this.vlcpath = vlcpath;
	}
	
	public String getLR2PlaySkinPath() {
		return lr2playskin;
	}
	
	public int getAudioDeviceBufferSize() {
		return audioDeviceBufferSize;
	}

	public void setAudioDeviceBufferSize(int audioDeviceBufferSize) {
		this.audioDeviceBufferSize = audioDeviceBufferSize;
	}

	public int getAudioDeviceSimultaneousSources() {
		return audioDeviceSimultaneousSources;
	}

	public void setAudioDeviceSimultaneousSources(int audioDeviceSimultaneousSources) {
		this.audioDeviceSimultaneousSources = audioDeviceSimultaneousSources;
	}

	public int getMaxFramePerSecond() {
		return maxFramePerSecond;
	}

	public void setMaxFramePerSecond(int maxFramePerSecond) {
		this.maxFramePerSecond = maxFramePerSecond;
	}

	public String[] getBmsroot() {
		return bmsroot;
	}

	public void setBmsroot(String[] bmsroot) {
		this.bmsroot = bmsroot;
	}

	public String[] getTableURL() {
		return tableURL;
	}

	public void setTableURL(String[] tableURL) {
		this.tableURL = tableURL;
	}

	public int getJudgedetail() {
		return judgedetail;
	}

	public void setJudgedetail(int judgedetail) {
		this.judgedetail = judgedetail;
	}
}
