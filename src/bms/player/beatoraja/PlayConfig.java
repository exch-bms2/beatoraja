package bms.player.beatoraja;

/**
 * プレイコンフィグ。モード毎に保持するべき値についてはこちらに格納する
 * 
 * @author exch
 */
public class PlayConfig implements Cloneable {

	/**
	 * ハイスピード。1.0で等速
	 */
	private float hispeed = 1.0f;
	/**
	 * デュレーション(ノーツ表示時間)
	 */
	private int duration = 500;
	/**
	 * ハイスピード固定。固定する場合はデュレーションが有効となり、固定しない場合はハイスピードが有効になる
	 */
	private int fixhispeed = FIX_HISPEED_MAINBPM;
	
	public static final int FIX_HISPEED_OFF = 0;
	public static final int FIX_HISPEED_STARTBPM = 1;
	public static final int FIX_HISPEED_MAXBPM = 2;
	public static final int FIX_HISPEED_MAINBPM = 3;
	public static final int FIX_HISPEED_MINBPM = 4;

	/**
	 * ハイスピード変化間隔
	 */
	public float hispeedmargin = 0.25f;

	/**
	 * レーンカバー表示量(0-1)
	 */
	private float lanecover = 0.2f;
	/**
	 * レーンカバー使用
	 */
	private boolean enablelanecover = true;
	/**
	 * リフト表示量(0-1)
	 */
	private float lift = 0.1f;
	/**
	 * リフト使用
	 */
	private boolean enablelift = false;

	public PlayConfig() {
	}

	public float getHispeed() {
		if (hispeed < 0.01) {
			hispeed = 0.01f;
		}
		return hispeed;
	}

	public void setHispeed(float hispeed) {
		this.hispeed = hispeed;
	}

	public int getDuration() {
		if (duration < 1) {
			duration = 1;
		}
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public float getHispeedMargin() {
		if(hispeedmargin < 0.0f || hispeedmargin > 10.0f)
			hispeedmargin = 0.25f;
		return hispeedmargin;
	}

	public void setHispeedMargin(float hispeedmargin) {
		this.hispeedmargin = hispeedmargin;
	}

	public int getFixhispeed() {
		if(fixhispeed < 0 || fixhispeed > FIX_HISPEED_MINBPM) {
			fixhispeed = FIX_HISPEED_OFF;
		}
		return fixhispeed;
	}

	public void setFixhispeed(int fixhispeed) {
		this.fixhispeed = fixhispeed;
	}

	public float getLanecover() {
		if (lanecover < 0 || lanecover > 1) {
			lanecover = 0;
		}
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
		if (lift < 0 || lift > 1) {
			lift = 0;
		}
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
	
	public PlayConfig clone() {
		try {
			return (PlayConfig) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}
}
