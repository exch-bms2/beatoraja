package bms.player.beatoraja.skin.lr2;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Resolution;
import com.badlogic.gdx.math.Rectangle;

import bms.player.beatoraja.result.MusicResultSkin;
import bms.player.beatoraja.result.SkinGaugeGraphObject;
import bms.player.beatoraja.skin.SkinHeader;

/**
 * LR2リザルトスキン読み込み用クラス
 * 
 * @author exch
 */
public class LR2ResultSkinLoader extends LR2SkinCSVLoader {

	private MusicResultSkin skin;

	private Rectangle gauge = new Rectangle();

	public LR2ResultSkinLoader(final Resolution src, final Config c) {
		super(src, c);

		addCommandWord(new CommandWord("STARTINPUT") {
			@Override
			public void execute(String[] str) {
				skin.setInput(Integer.parseInt(str[1]));
				skin.setRankTime(Integer.parseInt(str[2]));
			}
		});
		addCommandWord(new CommandWord("SRC_GAUGECHART_1P") {
			@Override
			public void execute(String[] str) {
				int fieldw = Integer.parseInt(str[11]);
				int fieldh = Integer.parseInt(str[12]);
				gauge = new Rectangle(0, 0, fieldw, fieldh);
			}
		});
		addCommandWord(new CommandWord("DST_GAUGECHART_1P") {
			@Override
			public void execute(String[] str) {
				gauge.x = Integer.parseInt(str[3]);
				gauge.y = src.height - Integer.parseInt(str[4]);
				SkinGaugeGraphObject obj = new SkinGaugeGraphObject();
				skin.setDestination(obj, 0, gauge.x, gauge.y, gauge.width, gauge.height, 0, 255, 255, 255, 255, 0, 0, 0,
						0, 0, 0, 0, 0, 0);
				skin.add(obj);
			}
		});
	}

	public MusicResultSkin loadResultSkin(File f, MainState state, SkinHeader header, Map<Integer, Boolean> option,
			Map property) throws IOException {

		skin = new MusicResultSkin(src, dst);

		this.loadSkin(skin, f, state, header, option, property);

		return skin;
	}

}
