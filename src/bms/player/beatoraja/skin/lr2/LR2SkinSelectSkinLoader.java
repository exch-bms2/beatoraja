package bms.player.beatoraja.skin.lr2;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.config.SkinConfigurationSkin;
import bms.player.beatoraja.skin.SkinHeader;
import bms.player.beatoraja.skin.SkinObject;
import bms.player.beatoraja.skin.SkinPropertyMapper;

import java.io.IOException;
import java.nio.file.Path;

import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.ObjectMap;

public class LR2SkinSelectSkinLoader extends LR2SkinCSVLoader<SkinConfigurationSkin> {

	public LR2SkinSelectSkinLoader(final Resolution src, final Config c) {
		super(src, c);

		addCommandWord(new CommandWord("SAMPLEBMS") {
			@Override
			public void execute(String[] str) {
				skin.setSampleBMS(new String[] {str[1]});
			}
		});
	}

	public SkinConfigurationSkin loadSkin(Path f, MainState selector, SkinHeader header,
	                                      IntIntMap option, ObjectMap property) throws IOException {
		SkinConfigurationSkin skin = this.loadSkin(new SkinConfigurationSkin(src, dst), f, selector, header, option, property);
		int count = 0;
		for (SkinObject obj : skin.getAllSkinObjects()) {
			if (SkinPropertyMapper.isSkinCustomizeButton(obj.getClickevent())) {
				int index = SkinPropertyMapper.getSkinCustomizeIndex(obj.getClickevent());
				if (count <= index)
					count = index + 1;
			}
		}
		skin.setCustomPropertyCount(count);
		return skin;
	}
}
