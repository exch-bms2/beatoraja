package bms.player.beatoraja.skin;

import java.io.File;
import java.io.IOException;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Rectangle;

import bms.player.beatoraja.result.MusicResultSkin;
import bms.player.beatoraja.skin.LR2SkinLoader.CommandWord;

public class LR2ResultSkinLoader extends LR2SkinLoader {

	private MusicResultSkin skin;
	
	public LR2ResultSkinLoader() {
		final float srcw = 640;
		final float srch = 480;
		final float dstw = 1280;
		final float dsth = 720;

		addCommandWord(new CommandWord("SRC_GAUGECHART_1P") {
			@Override
			public void execute(String[] str) {
					try {
						int fieldw = Integer.parseInt(str[11]);
						int fieldh = Integer.parseInt(str[12]);
						Rectangle gaugechart = new Rectangle(0, 0, fieldw * dstw / srcw, fieldh * dsth / srch);
						skin.setGaugeRegion(gaugechart);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
			}
		});
		addCommandWord(new CommandWord("DST_GAUGECHART_1P") {
			@Override
			public void execute(String[] str) {
				if (skin.getGaugeRegion() != null) {
					try {
						Rectangle gaugechart = skin.getGaugeRegion();
						gaugechart.x = Integer.parseInt(str[3]) * dstw / srcw;		
						gaugechart.y = dsth - Integer.parseInt(str[4]) * dsth / srch;
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}
	
	public MusicResultSkin loadResultSkin(File f, int[] option) throws IOException {

		skin = new MusicResultSkin();

		this.loadSkin(skin, f, option);
		
		return skin;
	}

}
