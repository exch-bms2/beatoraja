package bms.player.beatoraja.skin.lr2;

import java.io.IOException;
import java.nio.file.Path;

import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.ObjectMap;

import bms.player.beatoraja.*;
import bms.player.beatoraja.decide.MusicDecideSkin;
import bms.player.beatoraja.skin.SkinHeader;

public class LR2DecideSkinLoader extends LR2SkinCSVLoader<MusicDecideSkin> {

	public LR2DecideSkinLoader(Resolution src, final Config c) {
		super(src, c);
	}

	public MusicDecideSkin loadSkin(Path f, MainState decide, SkinHeader header, IntIntMap option, ObjectMap property) throws IOException {
		return this.loadSkin(new MusicDecideSkin(src, dst), f, decide, header, option, property);
	}
}
