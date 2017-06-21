package bms.player.beatoraja.skin.lr2;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import bms.player.beatoraja.*;
import bms.player.beatoraja.decide.MusicDecideSkin;
import bms.player.beatoraja.skin.SkinHeader;

public class LR2DecideSkinLoader extends LR2SkinCSVLoader<MusicDecideSkin> {

	public LR2DecideSkinLoader(Resolution src, final Config c) {
		super(src, c);
	}

	public MusicDecideSkin loadSkin(File f, MainState decide, SkinHeader header, Map<Integer, Boolean> option, Map property) throws IOException {
		return this.loadSkin(new MusicDecideSkin(src, dst), f, decide, header, option, property);
	}
}
