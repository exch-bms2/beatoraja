package bms.player.beatoraja;

public abstract class MainState {

	private long starttime;
	
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
}
