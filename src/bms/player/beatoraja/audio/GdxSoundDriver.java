package bms.player.beatoraja.audio;

import java.util.*;
import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class GdxSoundDriver implements AudioDriver {

	private Map<String, Sound> soundmap = new HashMap<String, Sound>();

	public void play(String p, boolean loop) {
		Sound sound = soundmap.get(p);
		if (!soundmap.containsKey(p)) {
			try {
				sound = Gdx.audio.newSound(Gdx.files.internal(p));
				soundmap.put(p, sound);
			} catch (GdxRuntimeException e) {
				Logger.getGlobal().warning("音源読み込み失敗。" + e.getMessage());
			}
		}

		if (sound != null) {
			if (loop) {
				sound.loop();
			} else {
				sound.play();
			}
		}
	}

	public void stop(String p) {
		Sound sound = soundmap.get(p);
		if (sound != null) {
			sound.stop();
		}
	}

	@Override
	public void dispose() {
	}
}
