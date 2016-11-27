package bms.player.beatoraja.skin;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import bms.player.beatoraja.MainState;
import com.badlogic.gdx.math.Rectangle;

import bms.player.beatoraja.result.MusicResultSkin;
import bms.player.beatoraja.result.SkinGaugeGraphObject;

/**
 * LR2リザルトスキン読み込み用クラス
 * 
 * @author exch
 */
public class LR2ResultSkinLoader extends LR2SkinCSVLoader {

	private MusicResultSkin skin;

	private Rectangle gauge = new Rectangle();
	
	public LR2ResultSkinLoader(final float srcw, final float srch, final float dstw, final float dsth) {
		super(srcw, srch, dstw, dsth);

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
					try {
						int fieldw = Integer.parseInt(str[11]);
						int fieldh = Integer.parseInt(str[12]);
						gauge = new Rectangle(0, 0, fieldw, fieldh);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
			}
		});
		addCommandWord(new CommandWord("DST_GAUGECHART_1P") {
			@Override
			public void execute(String[] str) {
					try {
						gauge.x = Integer.parseInt(str[3]);
						gauge.y = srch - Integer.parseInt(str[4]);
						SkinGaugeGraphObject obj = new SkinGaugeGraphObject();
						skin.setDestination(obj,0,gauge.x, gauge.y, gauge.width,gauge.height,0,255,255,255,255,0,0,0,0,0,0,0,0,0);
						skin.add(obj);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
			}
		});
	}
	
	public MusicResultSkin loadResultSkin(File f, MainState state, LR2SkinHeader header, int[] option, Map property) throws IOException {

		skin = new MusicResultSkin(srcw, srch, dstw, dsth);

		this.loadSkin(skin, f, state, header, option, property);
		
		return skin;
	}

}
