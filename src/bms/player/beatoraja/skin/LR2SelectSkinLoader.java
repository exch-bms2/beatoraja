package bms.player.beatoraja.skin;

import java.io.File;
import java.io.IOException;

import bms.player.beatoraja.PlaySkin;
import bms.player.beatoraja.select.MusicSelectSkin;
import bms.player.beatoraja.skin.LR2SkinLoader.CommandWord;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class LR2SelectSkinLoader extends LR2SkinLoader {

	private Sprite[] bar = new Sprite[10];

	private Animation[] lamp = new Animation[11];

	public LR2SelectSkinLoader() {
		addCommandWord(new CommandWord("SRC_BAR_BODY") {
			@Override
			public void execute(String[] str) {
				int gr = Integer.parseInt(str[2]);
				if (gr < imagelist.size() && imagelist.get(gr) != null) {
					try {
						bar[Integer.parseInt(str[1])] = new Sprite(imagelist.get(Integer.parseInt(str[2])),
								Integer.parseInt(str[3]), Integer.parseInt(str[4]), Integer.parseInt(str[5]),
								Integer.parseInt(str[6]));
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});
		addCommandWord(new CommandWord("SRC_BAR_LAMP") {

			private final int[][] lampg = { { 0 }, { 1 }, { 2, 3, 4 }, { 5 }, { 6, 7 }, { 8, 9, 10 } };

			@Override
			public void execute(String[] str) {
				int gr = Integer.parseInt(str[2]);
				if (gr < imagelist.size() && imagelist.get(gr) != null) {
					try {
						int x = Integer.parseInt(str[3]);
						int y = Integer.parseInt(str[4]);
						int w = Integer.parseInt(str[5]);
						int h = Integer.parseInt(str[6]);
						int dx = Integer.parseInt(str[7]);
						int dy = Integer.parseInt(str[8]);
						
						TextureRegion[][] lamps = new TextureRegion[dy][dx];
						for(int dh = 0; dh < dy; dh++) {
							for(int dw = 0; dw < dx; dw++) {
								lamps[dh][dw] = new Sprite(imagelist.get(Integer.parseInt(str[2])),  + w / dx * dw,
										y + h / dy * dh, w / dx, h / dy);
							}							
						}
						for (int i : lampg[Integer.parseInt(str[1])]) {
							lamp[i] = new Animation(Integer.parseInt(str[9]) / 1000f, lamps[0]);
						}
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	public MusicSelectSkin loadSelectSkin(File f) throws IOException {

		MusicSelectSkin skin = new MusicSelectSkin();

		this.loadSkin(skin, f);

		skin.setBar(bar);

		return skin;
	}
}
