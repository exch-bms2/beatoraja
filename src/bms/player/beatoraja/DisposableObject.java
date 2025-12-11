package bms.player.beatoraja;

import com.badlogic.gdx.utils.*;

public abstract class DisposableObject implements Disposable {

	private boolean disposed = false;
	
	public final boolean isDisposed() {
		return disposed;
	}

	public final boolean isNotDisposed() {
		return !disposed;
	}

	public final void setDisposed() {
		this.disposed = true;
	}

	public static void disposeAll(DisposableObject... objects) {
		if(objects == null) {
			return;
		}
		for(DisposableObject obj : objects) {
			if(obj != null && obj.isNotDisposed()) {
				obj.dispose();
				obj.setDisposed();
			}
		}
	}
}
