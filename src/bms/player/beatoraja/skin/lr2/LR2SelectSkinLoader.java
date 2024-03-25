package bms.player.beatoraja.skin.lr2;

import java.io.IOException;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.select.*;
import bms.player.beatoraja.skin.*;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.IntIntMap;

/**
 * LR2セレクトスキンローダー
 *
 * @author exch
 */
public class LR2SelectSkinLoader extends LR2SkinCSVLoader<MusicSelectSkin> {

	// TODO コピペのリファクタリング

	private SkinBar skinbar = new SkinBar(1);

	private TextureRegion[][] barimage = new TextureRegion[10][];
	private SkinImage[] barimageon = new SkinImage[SkinBar.BAR_COUNT];
	private SkinImage[] barimageoff = new SkinImage[SkinBar.BAR_COUNT];
	private int barcycle;

	private Rectangle gauge = new Rectangle();
	private SkinNoteDistributionGraph noteobj;
	private SkinBPMGraph bpmgraphobj;

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
				
				if(barimageoff[values[1]] == null) {
	                barimageoff[values[1]] = new SkinImage(barimage, 0, barcycle, null);
				}
                barimageoff[values[1]].setDestination(values[2], values[3] * dstw / srcw,
						dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6] * dsth / srch,
						values[7], values[8], values[9], values[10], values[11], values[12], values[13], values[14],
						values[15], values[16], values[17], values[18], values[19], values[20], readOffset(str, 21));
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
				if(barimageon[values[1]] == null) {
					barimageon[values[1]] = new SkinImage(barimage, 0, barcycle, null);
				}
				barimageon[values[1]].setDestination(values[2], values[3] * dstw / srcw,
						dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6] * dsth / srch,
						values[7], values[8], values[9], values[10], values[11], values[12], values[13], values[14],
						values[15], values[16], values[17], values[18], values[19], values[20], readOffset(str, 21));
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
				if(values[1] < 0 && values[1] >= SkinBar.BARLEVEL_COUNT) {
					return;
				}
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

							skinbar.setBarlevel(values[1], new SkinNumber(pn, mn, values[10], values[9],
									values[13] + 1, 0, values[15], values[11], values[12]));
						} else {
							int d = images.length % 10 == 0 ? 10 : 11;

							TextureRegion[][] nimages = new TextureRegion[divx * divy / d][d];
							for (int i = 0; i < d; i++) {
								for (int j = 0; j < divx * divy / d; j++) {
									nimages[j][i] = images[j * d + i];
								}
							}

							skinbar.setBarlevel(values[1], new SkinNumber(nimages, values[10], values[9],
									values[13], d > 10 ? 2 : 0, values[15], values[11], values[12]));
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
				if (skinbar.getBarlevel(values[1]) != null) {
					skinbar.getBarlevel(values[1]).setDestination(values[2], values[3] * dstw / srcw,
							-(values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6] * dsth / srch,
							values[7], values[8], values[9], values[10], values[11], values[12], values[13], values[14],
							values[15], values[16], values[17], values[18], values[19], values[20], readOffset(str, 21));
				}
			}
		});
		addCommandWord(new CommandWord("SRC_BAR_LAMP") {

			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				if(values[1] < 0 && values[1] >= SkinBar.BARLAMP_COUNT) {
					return;
				}
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					int[] lamps = lampg[values[1]];
					skinbar.setLamp(lamps[0], new SkinImage(images, values[10], values[9]));
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
					if (skinbar.getLamp(lamps[i]) != null) {
						if (values[5] < 0) {
							values[3] += values[5];
							values[5] = -values[5];
						}
						if (values[6] < 0) {
							values[4] += values[6];
							values[6] = -values[6];
						}
						skinbar.getLamp(lamps[i]).setDestination(values[2], values[3] * dstw / srcw,
								-(values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
								values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
								values[12], values[13], values[14], values[15], values[16], values[17], values[18],
								values[19], values[20], readOffset(str, 21));
					} else {
						skinbar.setLamp(lamps[i], skinbar.getLamp(lamps[0]));
					}
				}
			}
		});
		addCommandWord(new CommandWord("SRC_BAR_MY_LAMP") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				if(values[1] < 0 && values[1] >= SkinBar.BARLAMP_COUNT) {
					return;
				}
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					int[] lamps = lampg[values[1]];
					skinbar.setPlayerLamp(lamps[0], new SkinImage(images, values[10], values[9]));
					// System.out.println("Nowjudge Added - " + (5 -
					// values[1]));
				}
			}
		});
		addCommandWord(new CommandWord("DST_BAR_MY_LAMP") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				int[] lamps = lampg[values[1]];
				for (int i = 0; i < lamps.length; i++) {
					if (skinbar.getPlayerLamp(lamps[i]) != null) {
						if (values[5] < 0) {
							values[3] += values[5];
							values[5] = -values[5];
						}
						if (values[6] < 0) {
							values[4] += values[6];
							values[6] = -values[6];
						}
						skinbar.getPlayerLamp(lamps[i]).setDestination(values[2], values[3] * dstw / srcw,
								-(values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
								values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
								values[12], values[13], values[14], values[15], values[16], values[17], values[18],
								values[19], values[20], readOffset(str, 21));
					} else {
						skinbar.setPlayerLamp(lamps[i], skinbar.getPlayerLamp(lamps[0]));
					}
				}
			}
		});
		addCommandWord(new CommandWord("SRC_BAR_RIVAL_LAMP") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				if(values[1] < 0 && values[1] >= SkinBar.BARLAMP_COUNT) {
					return;
				}
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					int[] lamps = lampg[values[1]];
					skinbar.setRivalLamp(lamps[0], new SkinImage(images, values[10], values[9]));
					// System.out.println("Nowjudge Added - " + (5 -
					// values[1]));
				}
			}
		});
		addCommandWord(new CommandWord("DST_BAR_RIVAL_LAMP") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				int[] lamps = lampg[values[1]];
				for (int i = 0; i < lamps.length; i++) {
					if (skinbar.getRivalLamp(lamps[i]) != null) {
						if (values[5] < 0) {
							values[3] += values[5];
							values[5] = -values[5];
						}
						if (values[6] < 0) {
							values[4] += values[6];
							values[6] = -values[6];
						}
						skinbar.getRivalLamp(lamps[i]).setDestination(values[2], values[3] * dstw / srcw,
								-(values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
								values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
								values[12], values[13], values[14], values[15], values[16], values[17], values[18],
								values[19], values[20], readOffset(str, 21));
					} else {
						skinbar.setRivalLamp(lamps[i], skinbar.getRivalLamp(lamps[0]));
					}
				}
			}
		});
		// 拡張定義。段位のトロフィー画像を定義する。0:銅、1:銀、2:金
		addCommandWord(new CommandWord("SRC_BAR_TROPHY") {

			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				if(values[1] < 0 && values[1] >= SkinBar.BARTROPHY_COUNT) {
					return;
				}
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					skinbar.setTrophy(values[1], new SkinImage(images, values[10], values[9]));
					// System.out.println("Nowjudge Added - " + (5 -
					// values[1]));
				}
			}
		});
		addCommandWord(new CommandWord("DST_BAR_TROPHY") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				if (skinbar.getTrophy(values[1]) != null) {
					if (values[5] < 0) {
						values[3] += values[5];
						values[5] = -values[5];
					}
					if (values[6] < 0) {
						values[4] += values[6];
						values[6] = -values[6];
					}
					skinbar.getTrophy(values[1]).setDestination(values[2], values[3] * dstw / srcw,
							-(values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
							values[12], values[13], values[14], values[15], values[16], values[17], values[18],
							values[19], values[20], readOffset(str, 21));
				}
			}
		});

		// 拡張定義。楽曲バーのラベルを定義する。0:LNあり, 1:ランダム分岐あり、2:地雷あり
		addCommandWord(new CommandWord("SRC_BAR_LABEL") {

			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				if(values[1] < 0 && values[1] >= SkinBar.BARLABEL_COUNT) {
					return;
				}
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					skinbar.setLabel(values[1], new SkinImage(images, values[10], values[9]));
					// System.out.println("Nowjudge Added - " + (5 -
					// values[1]));
				}
			}
		});
		addCommandWord(new CommandWord("DST_BAR_LABEL") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				if (skinbar.getLabel(values[1]) != null) {
					if (values[5] < 0) {
						values[3] += values[5];
						values[5] = -values[5];
					}
					if (values[6] < 0) {
						values[4] += values[6];
						values[6] = -values[6];
					}
					skinbar.getLabel(values[1]).setDestination(values[2], values[3] * dstw / srcw,
							-(values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
							values[12], values[13], values[14], values[15], values[16], values[17], values[18],
							values[19], values[20], readOffset(str, 21));
				}
			}
		});
		// 拡張定義。段位のトロフィー画像を定義する。0:銅、1:銀、2:金
		addCommandWord(new CommandWord("SRC_BAR_GRAPH") {

			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					final int len = values[1] == 0 ? 11 : 28;
					TextureRegion[][] imgs = new TextureRegion[images.length / len][len];
					imgs = new TextureRegion[len][images.length / len];
					for(int j = 0 ;j < len;j++) {
						for(int i = 0 ;i < imgs[j].length;i++) {
							imgs[j][i] = images[i * len + j];
						}
					}
					skinbar.setGraph(new SkinDistributionGraph(values[1], imgs, values[10], values[9]));
					// System.out.println("Nowjudge Added - " + (5 -
					// values[1]));
				}
			}
		});
		addCommandWord(new CommandWord("DST_BAR_GRAPH") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				if (skinbar.getGraph() != null) {
					if (values[5] < 0) {
						values[3] += values[5];
						values[5] = -values[5];
					}
					if (values[6] < 0) {
						values[4] += values[6];
						values[6] = -values[6];
					}
					skinbar.getGraph().setDestination(values[2], values[3] * dstw / srcw,
							-(values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
							values[12], values[13], values[14], values[15], values[16], values[17], values[18],
							values[19], values[20], readOffset(str, 21));
				}
			}
		});

		addCommandWord(new CommandWord("SRC_NOTECHART") {
			//#SRC_NOTECHART_1P,(index),(gr),(x),(y),(w),(h),(div_x),(div_y),(cycle),(timer),field_w,field_h,(start),(end),delay,backTexOff,orderReverse,noGap
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				noteobj = new SkinNoteDistributionGraph(values[1], values[15], values[16], values[17], values[18], values[19]);
				gauge = new Rectangle(0, 0, values[11], values[12]);
				skin.add(noteobj);
			}
		});
		addCommandWord(new CommandWord("DST_NOTECHART") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				gauge.x = values[3];
				gauge.y = src.height - values[4];
				skin.setDestination(noteobj, values[2], gauge.x, gauge.y, gauge.width, gauge.height, values[7], values[8],
						values[9], values[10], values[11], values[12], values[13], values[14], values[15],
						values[16], values[17], values[18], values[19], values[20], readOffset(str, 21));
			}
		});

		addCommandWord(new CommandWord("SRC_BPMCHART") {
			//#SRC_BPMCHART, field_w, field_h, delay, lineWidth, mainBPMColor, minBPMColor, maxBPMColor, otherBPMColor, stopLineColor, transitionLineColor
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				bpmgraphobj = new SkinBPMGraph(values[3], values[4], str[5], str[6], str[7], str[8], str[9], str[10]);
				gauge = new Rectangle(0, 0, values[1], values[2]);
				skin.add(bpmgraphobj);
			}
		});
		addCommandWord(new CommandWord("DST_BPMCHART") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				gauge.x = values[3];
				gauge.y = src.height - values[4];
				skin.setDestination(bpmgraphobj, values[2], gauge.x, gauge.y, gauge.width, gauge.height, values[7], values[8],
						values[9], values[10], values[11], values[12], values[13], values[14], values[15],
						values[16], values[17], values[18], values[19], values[20], readOffset(str, 21));
			}
		});

		addCommandWord(new CommandWord("SRC_BAR_TITLE") {
			@Override
			public void execute(String[] str) {
				// 拡張Index 0:通常 1:新規 2:SongBar(通常) 3:SongBar(新規) 4:FolderBar(通常) 5:FolderBar(新規) 6:TableBar or HashBar
				// 7:GradeBar(曲所持) 8:(SongBar or GradeBar)(曲未所持) 9:CommandBar or ContainerBar 10:SearchWordBar
				// 3以降で定義されてなければ0か1が用いられる
				int[] values = parseInt(str);
				if(values[1] < 0 && values[1] >= SkinBar.BARTEXT_COUNT) {
					return;
				}
				SkinText bartext = null;
				if (values[2] < fontlist.size && fontlist.get(values[2]) != null) {
					bartext = new SkinTextImage(fontlist.get(values[2]));
				} else {
					bartext = new SkinTextFont("skin/default/VL-Gothic-Regular.ttf", 0, 48, 2);
				}
				bartext.setAlign(values[4]);
				skinbar.setText(values[1], bartext);
				// System.out.println("Text Added - " +
				// (values[3]));
			}
		});
		addCommandWord(new CommandWord("DST_BAR_TITLE") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				skinbar.getText(values[1]).setDestination(values[2], values[3] * dstw / srcw,
						- (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6] * dsth / srch, values[7],
						values[8], values[9], values[10], values[11], values[12], values[13], values[14], values[15],
						values[16], values[17], values[18], values[19], values[20], readOffset(str, 21));
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

	public MusicSelectSkin loadSkin(MainState selector, SkinHeader header, IntIntMap option) throws IOException {
		MusicSelectSkin skin = this.loadSkin(new MusicSelectSkin(header), selector, option);
		skinbar.setBarImage(barimageon, barimageoff);
		return skin;
	}
}
