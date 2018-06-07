package bms.player.beatoraja.skin;

import java.util.ArrayList;
import java.util.List;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

public abstract class Subject {
	private List<SkinObserver> skinObservers = new ArrayList<SkinObserver>();

	public void attach(SkinObserver skinObserver) {
		skinObservers.add(skinObserver);
	}

	public void detach(SkinObserver skinObserver) {
		skinObservers.remove(skinObserver);
	}

	public void notify_(SkinObjectRenderer sprite, long time, MainState state) {
		for (SkinObserver observer : skinObservers) {
				observer.draw(sprite, time, state);
		}
	}
}
