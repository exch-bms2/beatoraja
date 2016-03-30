package bms.player.beatoraja.skin;

import java.io.File;
import java.io.IOException;

import bms.player.beatoraja.decide.MusicDecideSkin;

public class LR2DecideSkinLoader extends LR2PlaySkinLoader {

	private MusicDecideSkin skin;
	
	public MusicDecideSkin loadMusicDecideSkin(File f, int[] option) throws IOException {

		skin = new MusicDecideSkin();

		this.loadSkin(skin, f, option);

		return skin;
	}
}
