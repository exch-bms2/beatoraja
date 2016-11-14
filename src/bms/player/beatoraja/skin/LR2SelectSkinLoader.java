package bms.player.beatoraja.skin;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import bms.player.beatoraja.select.MusicSelectSkin;
import bms.player.beatoraja.select.SkinBar;
import bms.player.beatoraja.select.MusicSelector;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class LR2SelectSkinLoader extends LR2SkinCSVLoader {

	private MusicSelector selector;

	private MusicSelectSkin skin;

	private SkinBar skinbar = new SkinBar();

	private TextureRegion[][] barimage = new TextureRegion[10][];
	private int barcycle;

	private final int[][] lampg = { { 0 }, { 1 }, { 2, 3, 4 }, { 5 }, { 6, 7 }, {}, { 8, 9, 10 }, {} };

	public LR2SelectSkinLoader(final float srcw, final float srch, final float dstw, final float dsth) {
		super(srcw, srch, dstw, dsth);
		addCommandWord(new CommandWord("SRC_BAR_BODY") {
			@Override
			public void execute(String[] str) {
				try {
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
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}

			}
		});
		addCommandWord(new CommandWord("DST_BAR_BODY_OFF") {

		    private boolean added = false;
			@Override
			public void execute(String[] str) {
                if(!added) {
                    skinbar.setBarImages(barimage, barcycle);
                    skin.add(skinbar);
                    added = true;
                }
				try {
					int[] values = parseInt(str);
					if (values[5] < 0) {
						values[3] += values[5];
						values[5] = -values[5];
					}
					if (values[6] < 0) {
						values[4] += values[6];
						values[6] = -values[6];
					}
					skinbar.getBarImages(false,values[1]).setDestination(values[2], values[3] * dstw / srcw, dsth
							- (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6] * dsth / srch,
							values[7], values[8], values[9], values[10], values[11], values[12], values[13],
							values[14], values[15], values[16], values[17], values[18], values[19], values[20]);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		});
		addCommandWord(new CommandWord("DST_BAR_BODY_ON") {
			@Override
			public void execute(String[] str) {
				try {
					int[] values = parseInt(str);
					if (values[5] < 0) {
						values[3] += values[5];
						values[5] = -values[5];
					}
					if (values[6] < 0) {
						values[4] += values[6];
						values[6] = -values[6];
					}
					skinbar.getBarImages(true, values[1]).setDestination(values[2], values[3] * dstw / srcw, dsth
							- (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6] * dsth / srch,
							values[7], values[8], values[9], values[10], values[11], values[12], values[13],
							values[14], values[15], values[16], values[17], values[18], values[19], values[20]);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
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
				try {
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
								TextureRegion[] pn = new TextureRegion[12];
								TextureRegion[] mn = new TextureRegion[12];

								for (int i = 0; i < 12; i++) {
									pn[i] = images[i];
									mn[i] = images[i + 12];
								}
								skinbar.getBarlevel()[values[1]] = new SkinNumber(pn, mn, values[9], values[13] + 1, 0, values[11]);
								skinbar.getBarlevel()[values[1]].setAlign(values[12]);
							} else {
								int d = images.length % 10 == 0 ? 10 : 11;

								TextureRegion[][] nimages = new TextureRegion[divx * divy / d][d];
								for (int i = 0; i < d; i++) {
									for (int j = 0; j < divx * divy / d; j++) {
										nimages[j][i] = images[j * d + i];
									}
								}

								skinbar.getBarlevel()[values[1]] = new SkinNumber(nimages, values[9], values[13], d > 10 ? 2 : 0,
										values[11]);
								skinbar.getBarlevel()[values[1]].setAlign(values[12]);
							}
							skinbar.getBarlevel()[values[1]].setTimer(values[10]);
							// System.out.println("Number Added - " +
							// (num.getId()));
						}
					}
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		});
		addCommandWord(new CommandWord("DST_BAR_LEVEL") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				if (skinbar.getBarlevel()[values[1]] != null) {
					try {
						skinbar.getBarlevel()[values[1]].setDestination(values[2], values[3] * dstw / srcw, -values[4] * dsth / srch,
								values[5] * dstw / srcw, values[6] * dsth / srch, values[7], values[8], values[9],
								values[10], values[11], values[12], values[13], values[14], values[15], values[16],
								values[17], values[18], values[19], values[20]);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});
		addCommandWord(new CommandWord("SRC_BAR_LAMP") {

			@Override
			public void execute(String[] str) {
				try {
					int[] values = parseInt(str);
					TextureRegion[] images = getSourceImage(values);
					if (images != null) {
						int[] lamps = lampg[values[1]];
						for (int i = 0; i < lamps.length; i++) {
							skinbar.getLamp()[lamps[i]] = new SkinImage(images, values[9]);
							skinbar.getLamp()[lamps[i]].setTimer(values[10]);
						}
						// System.out.println("Nowjudge Added - " + (5 -
						// values[1]));
					}
				} catch (NumberFormatException e) {
					e.printStackTrace();
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
						try {
							if (values[5] < 0) {
								values[3] += values[5];
								values[5] = -values[5];
							}
							if (values[6] < 0) {
								values[4] += values[6];
								values[6] = -values[6];
							}
							skinbar.getLamp()[lamps[i]].setDestination(values[2], values[3] * dstw / srcw, dsth
									- (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6] * dsth
									/ srch, values[7], values[8], values[9], values[10], values[11], values[12],
									values[13], values[14], values[15], values[16], values[17], values[18], values[19],
									values[20]);
						} catch (NumberFormatException e) {
							e.printStackTrace();
						}
					}
				}
			}
		});
		addCommandWord(new CommandWord("SRC_BAR_TITLE") {
			@Override
			public void execute(String[] str) {
			}
		});
		addCommandWord(new CommandWord("DST_BAR_TITLE") {
			@Override
			public void execute(String[] str) {
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

	public MusicSelectSkin loadSelectSkin(File f, MusicSelector selector, LR2SkinHeader header, int[] option,
			Map property) throws IOException {
		skin = new MusicSelectSkin(srcw, srch, dstw, dsth);

		this.loadSkin(skin, f, selector, header, option, property);

		return skin;
	}
}
