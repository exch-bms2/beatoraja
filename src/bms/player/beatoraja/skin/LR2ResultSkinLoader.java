package bms.player.beatoraja.skin;

import java.io.File;
import java.io.IOException;

import bms.player.beatoraja.result.MusicResultSkin;

public class LR2ResultSkinLoader extends LR2SkinLoader {

	private MusicResultSkin skin;
	
	public MusicResultSkin loadResultSkin(File f, int[] option) throws IOException {

		skin = new MusicResultSkin();

		this.loadSkin(skin, f, option);
		
		return skin;
	}

}
