package bms.player.beatoraja.ir;

public interface IRResponse<T> {

	public abstract boolean isSuccessed();
	
	public abstract String getMessage();
	
	public abstract T getData();
}
