package bms.player.beatoraja;

import java.util.Arrays;

import bms.player.beatoraja.skin.Skin;

public abstract class MainState {

	private final MainController main;
	
	private long starttime;
	
	private long[] timer = new long[256];
	
	private Skin skin;
	
	public static final int TIMER_FADEOUT = 2;
	public static final int TIMER_FAILED = 3;
	public static final int TIMER_READY = 40;
	public static final int TIMER_PLAY = 41;
	public static final int TIMER_FULLCOMBO1 = 48;
	public static final int TIMER_FULLCOMBO2 = 49;
	public static final int TIMER_BOMB = 50;
	public static final int TIMER_HOLD = 70;
	public static final int TIMER_KEYON = 100;
	public static final int TIMER_KEYOFF = 120;
	
	// 選曲専用
	public static final int SLIDER_MUSICSELECT_POSITION = 1;
	// プレイ専用
	public static final int SLIDER_MUSIC_PROGRESS = 6;
	
	public static final int STRING_TITLE = 10;
	public static final int STRING_SUBTITLE = 11;
	public static final int STRING_FULLTITLE = 12;
	public static final int STRING_GENRE = 13;
	public static final int STRING_ARTIST = 14;
	public static final int STRING_SUBARTIST = 15;
	public static final int STRING_DIRECTORY = 1000;
	
	public MainState() {
		this(null);
	}
	
	public MainState(MainController main) {
		this.main = main;
		Arrays.fill(timer, -1);
	}
	
	public MainController getMainController() {
		return main;
	}
	
	public abstract void create();
	
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

	public long[] getTimer() {
		return timer;
	}
	
	public Skin getSkin() {
		return skin;
	}

	public void setSkin(Skin skin) {
		this.skin = skin;
	}

	public int getClear() {
		return 0;
	}

	public int getTargetClear() {
		return 0;
	}

	public int getScore() {
		return 0;
	}
	
	public int getBestScore() {
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

	public int getTotalJudgeCount(int judge) {
		return 0;
	}
	
	public int getTotalPlayCount(boolean clear) {
		return 0;
	}
	
	public int getPlayCount(boolean clear) {
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
	
	public int getTotalNotes() {
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

	public float getSliderValue(int id) {
		return 0;
	}
	
	public String getTextValue(int id) {
		return "";
	}
}
