package bms.player.beatoraja.skin.lr2;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.select.*;
import bms.player.beatoraja.skin.*;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * LR2セレクトスキンローダー
 *
 * @author exch
 */
public class LR2SelectSkinLoader extends LR2SkinCSVLoader {

	private MusicSelectSkin skin;

	private SkinBar skinbar = new SkinBar(1);

	private TextureRegion[][] barimage = new TextureRegion[10][];
	private int barcycle;

	/**
	 * LR2のLAMP IDとの対応
	 * 0:NO PLAY, 1:FAILED, 2:EASY, 3:NORMAL, 4:HARD, 5:EXH, 6:FC, 7:PERFECT, 8:MAX, 9:ASSIST, 10:L-ASSIST
	 */
	private final int[][] lampg = { { 0 }, { 1 }, { 4, 2, 3}, { 5 }, { 6, 7 }, {7}, { 8, 9, 10 }, {9}, {10}, {2}, {3} };

	public LR2SelectSkinLoader(final Resolution src, final Config c) {
		super(src, c);

		final float srcw = src.width;
		final float srch = src.height;
		final float dstw = dst.width;
		final float dsth = dst.height;

		addCommandWord(new CommandWord("SRC_BAR_BODY") {
			@Override
			public void execute(String[] str) {
				int gr = Integer.parseInt(str[2]);
				if (gr >= 100) {
				} else {
					int[] values = parseInt(str);
					TextureRegion[] images = getSourceImage(values);
					if (images != null) {
						barimage[values[1]] = images;
						barcycle = values[9];

					}
				}
			}
		});
		addCommandWord(new CommandWord("DST_BAR_BODY_OFF") {

			private boolean added = false;

			@Override
			public void execute(String[] str) {
				if (!added) {
					skinbar.setBarImages(barimage, barcycle);
					skin.add(skinbar);
					added = true;
				}
				int[] values = parseInt(str);
				if (values[5] < 0) {
					values[3] += values[5];
					values[5] = -values[5];
				}
				if (values[6] < 0) {
					values[4] += values[6];
					values[6] = -values[6];
				}
				skinbar.makeBarImages(false, values[1]).setDestination(values[2], values[3] * dstw / srcw,
						dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6] * dsth / srch,
						values[7], values[8], values[9], values[10], values[11], values[12], values[13], values[14],
						values[15], values[16], values[17], values[18], values[19], values[20]);
			}
		});
		addCommandWord(new CommandWord("DST_BAR_BODY_ON") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				if (values[5] < 0) {
					values[3] += values[5];
					values[5] = -values[5];
				}
				if (values[6] < 0) {
					values[4] += values[6];
					values[6] = -values[6];
				}
				skinbar.makeBarImages(true, values[1]).setDestination(values[2], values[3] * dstw / srcw,
						dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6] * dsth / srch,
						values[7], values[8], values[9], values[10], values[11], values[12], values[13], values[14],
						values[15], values[16], values[17], values[18], values[19], values[20]);
			}
		});
		addCommandWord(new CommandWord("BAR_CENTER") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				skin.setCenterBar(values[1]);
			}
		});
		addCommandWord(new CommandWord("BAR_AVAILABLE") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				int[] clickable = new int[values[2] - values[1] + 1];
				for (int i = 0; i < clickable.length; i++) {
					clickable[i] = values[1] + i;
				}
				skin.setClickableBar(clickable);
			}
		});
		addCommandWord(new CommandWord("SRC_BAR_FLASH") {
			@Override
			public void execute(String[] str) {
			}
		});
		addCommandWord(new CommandWord("DST_BAR_FLASH") {
			@Override
			public void execute(String[] str) {
			}
		});
		addCommandWord(new CommandWord("SRC_BAR_LEVEL") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				int divx = values[7];
				if (divx <= 0) {
					divx = 1;
				}
				int divy = values[8];
				if (divy <= 0) {
					divy = 1;
				}

				if (divx * divy >= 10) {
					TextureRegion[] images = getSourceImage(values);
					if (images != null) {
						if (images.length % 24 == 0) {
							TextureRegion[][] pn = new TextureRegion[images.length / 24][];
							TextureRegion[][] mn = new TextureRegion[images.length / 24][];

							for (int j = 0; j < pn.length; j++) {
								pn[j] = new TextureRegion[12];
								mn[j] = new TextureRegion[12];

								for (int i = 0; i < 12; i++) {
									pn[j][i] = images[j * 24 + i];
									mn[j][i] = images[j * 24 + i + 12];
								}
							}

							skinbar.getBarlevel()[values[1]] = new SkinNumber(pn, mn, values[10], values[9],
									values[13] + 1, 0, values[11]);
							skinbar.getBarlevel()[values[1]].setAlign(values[12]);
						} else {
							int d = images.length % 10 == 0 ? 10 : 11;

							TextureRegion[][] nimages = new TextureRegion[divx * divy / d][d];
							for (int i = 0; i < d; i++) {
								for (int j = 0; j < divx * divy / d; j++) {
									nimages[j][i] = images[j * d + i];
								}
							}

							skinbar.getBarlevel()[values[1]] = new SkinNumber(nimages, values[10], values[9],
									values[13], d > 10 ? 2 : 0, values[11]);
							skinbar.getBarlevel()[values[1]].setAlign(values[12]);
						}
						// System.out.println("Number Added - " +
						// (num.getId()));
					}
				}
			}
		});
		addCommandWord(new CommandWord("DST_BAR_LEVEL") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				if (skinbar.getBarlevel()[values[1]] != null) {
					skinbar.getBarlevel()[values[1]].setDestination(values[2], values[3] * dstw / srcw,
							-(values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6] * dsth / srch,
							values[7], values[8], values[9], values[10], values[11], values[12], values[13], values[14],
							values[15], values[16], values[17], values[18], values[19], values[20]);
				}
			}
		});
		addCommandWord(new CommandWord("SRC_BAR_LAMP") {

			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					int[] lamps = lampg[values[1]];
					skinbar.getLamp()[lamps[0]] = new SkinImage(images, values[10], values[9]);
					// System.out.println("Nowjudge Added - " + (5 -
					// values[1]));
				}
			}
		});
		addCommandWord(new CommandWord("DST_BAR_LAMP") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				int[] lamps = lampg[values[1]];
				for (int i = 0; i < lamps.length; i++) {
					if (skinbar.getLamp()[lamps[i]] != null) {
						if (values[5] < 0) {
							values[3] += values[5];
							values[5] = -values[5];
						}
						if (values[6] < 0) {
							values[4] += values[6];
							values[6] = -values[6];
						}
						skinbar.getLamp()[lamps[i]].setDestination(values[2], values[3] * dstw / srcw,
								-(values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
								values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
								values[12], values[13], values[14], values[15], values[16], values[17], values[18],
								values[19], values[20]);
					} else {
						skinbar.getLamp()[lamps[i]] = skinbar.getLamp()[lamps[0]];
					}
				}
			}
		});
		addCommandWord(new CommandWord("SRC_BAR_TITLE") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				SkinText bartext = null;
				if (values[2] < fontlist.size() && fontlist.get(values[2]) != null) {
					bartext = new SkinTextImage(fontlist.get(values[2]));
				} else {
					bartext = new SkinTextFont("skin/default/VL-Gothic-Regular.ttf", 0, 48, 2);
				}
				bartext.setAlign(values[4]);
				skinbar.getText()[values[1]] = bartext;
				// System.out.println("Text Added - " +
				// (values[3]));
			}
		});
		addCommandWord(new CommandWord("DST_BAR_TITLE") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				skinbar.getText()[values[1]].setDestination(values[2], values[3] * dstw / srcw,
						- (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6] * dsth / srch, values[7],
						values[8], values[9], values[10], values[11], values[12], values[13], values[14], values[15],
						values[16], values[17], values[18], values[19], values[20]);
			}
		});
		addCommandWord(new CommandWord("SRC_BAR_RANK") {
			@Override
			public void execute(String[] str) {
			}
		});
		addCommandWord(new CommandWord("DST_BAR_RANK") {
			@Override
			public void execute(String[] str) {
			}
		});
		addCommandWord(new CommandWord("SRC_README") {
			@Override
			public void execute(String[] str) {
			}
		});
		addCommandWord(new CommandWord("DST_README") {
			@Override
			public void execute(String[] str) {
			}
		});

	}

	public Skin loadSkin(File f, MainState selector, SkinHeader header,
			Map<Integer, Boolean> option, Map property) throws IOException {
		skin = new MusicSelectSkin(src, dst);

		this.loadSkin(skin, f, selector, header, option, property);

		return skin;
	}
}
