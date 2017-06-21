package bms.player.beatoraja.skin.lr2;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import bms.player.beatoraja.*;
import bms.player.beatoraja.decide.MusicDecideSkin;
import bms.player.beatoraja.skin.Skin;
import bms.player.beatoraja.skin.SkinHeader;

public class LR2DecideSkinLoader extends LR2SkinCSVLoader {

	public LR2DecideSkinLoader(Resolution src, final Config c) {
		super(src, c);
	}

	private MusicDecideSkin skin;
	
	public Skin loadSkin(File f, MainState decide, SkinHeader header, Map<Integer, Boolean> option, Map property) throws IOException {

		skin = new MusicDecideSkin(src, dst);

		this.loadSkin(skin, f, decide, header, option, property);

		return skin;
	}
}
