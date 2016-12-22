package bms.player.beatoraja.audio;

import java.nio.file.Path;

import com.badlogic.gdx.utils.Disposable;

public interface AudioDriver extends Disposable {

	public void play(String path, boolean loop);
	
	public void stop(String path);	
}
