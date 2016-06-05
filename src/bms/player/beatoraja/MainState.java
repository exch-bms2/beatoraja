package bms.player.beatoraja;

import bms.player.beatoraja.skin.Skin;

public abstract class MainState {

	private long starttime;
	
	private Skin skin;
	
	public abstract void create(PlayerResource resource);
	
	public abstract void render();
	
	public void pause() {
		
	}
	
	public void resume() {
		
	}
	
	public void resize(int width, int height) {
		
	}
	
	public abstract void dispose();

	public long getStartTime() {
		return starttime;
	}

	public void setStartTime(long starttime) {
		this.starttime = starttime;
	}

	public int getNowTime() {
		return (int) (System.currentTimeMillis() - starttime);
	}

	public Skin getSkin() {
		return skin;
	}

	public void setSkin(Skin skin) {
		this.skin = skin;
	}
	
	public int getScore() {
		return 0;
	}
	
	public int getTargetScore() {
		return 0;
	}
	
	public int getMaxcombo() {
		return 0;
	}
	
	public int getTargetMaxcombo() {
		return 0;
	}

	public int getJudgeCount(int judge, boolean fast) {
		return 0;
	}

	public int getMinBPM() {
		return 0;
	}
	
	public int getBPM() {
		return 0;
	}
	
	public int getMaxBPM() {
		return 0;
	}
	
	public float getHispeed() {
		return 0;
	}

	public int getDuration() {
		return 0;
	}

	public float getGrooveGauge() {
		return 0;
	}

	public int getTimeleftMinute() {
		return 0;
	}

	public int getTimeleftSecond() {
		return 0;
	}
	
	public int getMisscount() {
		return 0;
	}

	public int getTargetMisscount() {
		return 0;
	}

}
