package bms.player.beatoraja.skin;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import bms.player.beatoraja.decide.MusicDecide;
import bms.player.beatoraja.decide.MusicDecideSkin;

public class LR2DecideSkinLoader extends LR2PlaySkinLoader {

	private MusicDecideSkin skin;
	
	public MusicDecideSkin loadMusicDecideSkin(File f, MusicDecide decide, LR2SkinHeader header, int[] option, Map property) throws IOException {

		skin = new MusicDecideSkin();

		this.loadSkin(skin, f, decide, header, option, property);

		return skin;
	}
}
