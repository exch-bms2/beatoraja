package bms.player.beatoraja.skin;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.decide.MusicDecide;
import bms.player.beatoraja.play.BMSPlayer;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.GdxRuntimeException;

import bms.player.beatoraja.play.PlaySkin;

public class LR2PlaySkinLoader extends LR2SkinLoader {

	private PlaySkin.SkinBGAObject bga;

	private PlaySkin.SkinLaneObject lanerender;

	private Rectangle playerr = new Rectangle(0, 0, 0, 0);

	private PlaySkin skin;
	private Sprite[] note = new Sprite[8];
	private Sprite[] lnstart = new Sprite[8];
	private Sprite[] lnend = new Sprite[8];
	private Sprite[] lnbody = new Sprite[8];
	private Sprite[] lnbodya = new Sprite[8];
	private Sprite[] mine = new Sprite[8];
	private Rectangle[] laner = new Rectangle[8];
	private Sprite[] gauge = new Sprite[4];
	private Rectangle gauger = new Rectangle();
	private SkinImage line;
	private List<SkinImage> lines = new ArrayList<SkinImage>();
	private SkinImage li;

	private SkinImage[] nowjudge = new SkinImage[6];
	private SkinNumber[] nowcombo = new SkinNumber[6];
	private SkinImage[] nowjudge2 = new SkinImage[6];
	private SkinNumber[] nowcombo2 = new SkinNumber[6];
	private boolean shift;
	private boolean shift2;

	private int groovex = 0;
	private int groovey = 0;

	public LR2PlaySkinLoader(final float srcw, final float srch, final float dstw, final float dsth) {
		super(srcw, srch, dstw, dsth);

		addCommandWord(new CommandWord("CLOSE") {
			@Override
			public void execute(String[] str) {
				skin.setClose(Integer.parseInt(str[1]));
			}
		});
		addCommandWord(new CommandWord("PLAYSTART") {
			@Override
			public void execute(String[] str) {
				skin.setPlaystart(Integer.parseInt(str[1]));
			}
		});
		addCommandWord(new CommandWord("SRC_BGA") {
			@Override
			public void execute(String[] str) {
				bga = new PlaySkin.SkinBGAObject(skin);
				skin.add(bga);
			}
		});
		addCommandWord(new CommandWord("DST_BGA") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				if (bga != null) {
					skin.setDestination(bga, 0, values[3], srch - values[4] - values[6], values[5], values[6],
							values[7], values[8], values[9], values[10], values[11], values[12], values[13],
							values[14], values[15], values[16], values[17], values[18], values[19], values[20]);
				}
			}
		});
		addCommandWord(new CommandWord("SRC_LINE") {
			@Override
			public void execute(String[] str) {
				int gr = Integer.parseInt(str[2]);
				if (gr < imagelist.size() && imagelist.get(gr) != null) {
					try {
						int[] values = parseInt(str);
						int x = values[3];
						int y = values[4];
						int w = values[5];
						if (w == -1) {
							w = imagelist.get(gr).getWidth();
						}
						int h = values[6];
						if (h == -1) {
							h = imagelist.get(gr).getHeight();
						}
						int divx = values[7];
						if (divx <= 0) {
							divx = 1;
						}
						int divy = values[8];
						if (divy <= 0) {
							divy = 1;
						}
						TextureRegion[] images = new TextureRegion[divx * divy];
						for (int i = 0; i < divx; i++) {
							for (int j = 0; j < divy; j++) {
								images[divx * j + i] = new TextureRegion(imagelist.get(gr), x + w / divx * i, y + h
										/ divy * j, w / divx, h / divy);
							}
						}
						li = new SkinImage(images, values[9]);
						li.setTimer(values[10]);
						li.setOffsety(BMSPlayer.OFFSET_LIFT);
						lines.add(li);
						// System.out.println("Object Added - " +
						// (part.getTiming()));
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});
		addCommandWord(new CommandWord("DST_LINE") {
			@Override
			public void execute(String[] str) {
				if (li != null) {
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
						li.setDestination(values[2], values[3] * dstw / srcw, dsth - (values[4] + values[6]) * dsth
								/ srch, values[5] * dstw / srcw, values[6] * dsth / srch, values[7], values[8],
								values[9], values[10], values[11], values[12], values[13], values[14], values[15],
								values[16], values[17], values[18], values[19], values[20]);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});
		addCommandWord(new CommandWord("SRC_NOTE") {
			@Override
			public void execute(String[] str) {
				int lane = Integer.parseInt(str[1]);
				if (lane == 10) {
					lane = 15;
				} else if (lane >= 10) {
					lane -= 3;
				} else if (lane == 0) {
					lane = 7;
				} else {
					lane -= 1;
				}
				if (note[lane] == null) {
					note[lane] = new Sprite(imagelist.get(Integer.parseInt(str[2])), Integer.parseInt(str[3]),
							Integer.parseInt(str[4]), Integer.parseInt(str[5]), Integer.parseInt(str[6]));
				}
				if (lanerender == null) {
					lanerender = new PlaySkin.SkinLaneObject(skin);
					skin.add(lanerender);
				}
			}
		});
		addCommandWord(new CommandWord("SRC_LN_END") {
			@Override
			public void execute(String[] str) {
				int lane = Integer.parseInt(str[1]);
				if (lane == 10) {
					lane = 15;
				} else if (lane >= 10) {
					lane -= 3;
				} else if (lane == 0) {
					lane = 7;
				} else {
					lane -= 1;
				}
				if (lnend[lane] == null) {
					lnend[lane] = new Sprite(imagelist.get(Integer.parseInt(str[2])), Integer.parseInt(str[3]),
							Integer.parseInt(str[4]), Integer.parseInt(str[5]), Integer.parseInt(str[6]));
				}
			}
		});
		addCommandWord(new CommandWord("SRC_LN_START") {
			@Override
			public void execute(String[] str) {
				int lane = Integer.parseInt(str[1]);
				if (lane == 10) {
					lane = 15;
				} else if (lane >= 10) {
					lane -= 3;
				} else if (lane == 0) {
					lane = 7;
				} else {
					lane -= 1;
				}
				if (lnstart[lane] == null) {
					lnstart[lane] = new Sprite(imagelist.get(Integer.parseInt(str[2])), Integer.parseInt(str[3]),
							Integer.parseInt(str[4]), Integer.parseInt(str[5]), Integer.parseInt(str[6]));
				}
			}
		});

		addCommandWord(new CommandWord("SRC_LN_BODY") {
			@Override
			public void execute(String[] str) {
				int lane = Integer.parseInt(str[1]);
				if (lane == 10) {
					lane = 15;
				} else if (lane >= 10) {
					lane -= 3;
				} else if (lane == 0) {
					lane = 7;
				} else {
					lane -= 1;
				}
				if (lnbody[lane] == null) {
					lnbody[lane] = new Sprite(imagelist.get(Integer.parseInt(str[2])), Integer.parseInt(str[3]),
							Integer.parseInt(str[4]), Integer.parseInt(str[5]), 1);
					lnbodya[lane] = new Sprite(imagelist.get(Integer.parseInt(str[2])), Integer.parseInt(str[3]),
							Integer.parseInt(str[4]) + Integer.parseInt(str[6]) - 1, Integer.parseInt(str[5]), 1);
				}
			}
		});

		addCommandWord(new CommandWord("SRC_MINE") {
			@Override
			public void execute(String[] str) {
				int lane = Integer.parseInt(str[1]);
				if (lane == 10) {
					lane = 15;
				} else if (lane >= 10) {
					lane -= 3;
				} else if (lane == 0) {
					lane = 7;
				} else {
					lane -= 1;
				}
				if (mine[lane] == null) {
					mine[lane] = new Sprite(imagelist.get(Integer.parseInt(str[2])), Integer.parseInt(str[3]),
							Integer.parseInt(str[4]), Integer.parseInt(str[5]), Integer.parseInt(str[6]));
				}
			}
		});

		addCommandWord(new CommandWord("DST_NOTE") {
			@Override
			public void execute(String[] str) {
				int lane = Integer.parseInt(str[1]);
				if (lane == 10) {
					lane = 15;
				} else if (lane >= 10) {
					lane -= 3;
				} else if (lane == 0) {
					lane = 7;
				} else {
					lane -= 1;
				}
				if (laner[lane] == null) {
					laner[lane] = new Rectangle(Integer.parseInt(str[3]) * dstw / srcw, dsth
							- (Integer.parseInt(str[4]) + Integer.parseInt(str[6])) * dsth / srch,
							Integer.parseInt(str[5]) * dstw / srcw,
							(Integer.parseInt(str[4]) + Integer.parseInt(str[6])) * dsth / srch);
				}
				if (laner[lane].x < playerr.x) {
					playerr.width += playerr.x - laner[lane].x;
					playerr.x = laner[lane].x;
				}
				if (laner[lane].x + laner[lane].width > playerr.x + playerr.width) {
					playerr.width += laner[lane].x + laner[lane].width - (playerr.x + playerr.width);
				}
				if (laner[lane].y > playerr.y) {
					playerr.y = laner[lane].y;
					playerr.height = laner[lane].height;
				}
			}
		});

		addCommandWord(new CommandWord("SRC_NOWJUDGE_1P") {
			@Override
			public void execute(String[] str) {
				int gr = Integer.parseInt(str[2]);
				if (gr < imagelist.size() && imagelist.get(gr) != null) {
					try {
						int[] values = parseInt(str);
						int x = values[3];
						int y = values[4];
						int w = values[5];
						if (w == -1) {
							w = imagelist.get(gr).getWidth();
						}
						int h = values[6];
						if (h == -1) {
							h = imagelist.get(gr).getHeight();
						}
						int divx = values[7];
						if (divx <= 0) {
							divx = 1;
						}
						int divy = values[8];
						if (divy <= 0) {
							divy = 1;
						}
						TextureRegion[] images = new TextureRegion[divx * divy];
						for (int i = 0; i < divx; i++) {
							for (int j = 0; j < divy; j++) {
								images[divx * j + i] = new TextureRegion(imagelist.get(gr), x + w / divx * i, y + h
										/ divy * j, w / divx, h / divy);
							}
						}
						nowjudge[5 - values[1]] = new SkinImage(images, values[9]);
						nowjudge[5 - values[1]].setTimer(values[10]);
						shift = (values[11] != 1);
						// System.out.println("Nowjudge Added - " + (5 -
						// values[1]));
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});

		addCommandWord(new CommandWord("DST_NOWJUDGE_1P") {
			@Override
			public void execute(String[] str) {
				if (nowjudge[5 - Integer.parseInt(str[1])] != null) {
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
						nowjudge[5 - values[1]].setDestination(values[2], values[3] * dstw / srcw, dsth
								- (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6] * dsth
								/ srch, values[7], values[8], values[9], values[10], values[11], values[12],
								values[13], values[14], values[15], values[16], values[17], values[18], values[19],
								values[20]);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});

		addCommandWord(new CommandWord("SRC_NOWJUDGE_2P") {
			@Override
			public void execute(String[] str) {
				int gr = Integer.parseInt(str[2]);
				if (gr < imagelist.size() && imagelist.get(gr) != null) {
					try {
						int[] values = parseInt(str);
						int x = values[3];
						int y = values[4];
						int w = values[5];
						if (w == -1) {
							w = imagelist.get(gr).getWidth();
						}
						int h = values[6];
						if (h == -1) {
							h = imagelist.get(gr).getHeight();
						}
						int divx = values[7];
						if (divx <= 0) {
							divx = 1;
						}
						int divy = values[8];
						if (divy <= 0) {
							divy = 1;
						}
						TextureRegion[] images = new TextureRegion[divx * divy];
						for (int i = 0; i < divx; i++) {
							for (int j = 0; j < divy; j++) {
								images[divx * j + i] = new TextureRegion(imagelist.get(gr), x + w / divx * i, y + h
										/ divy * j, w / divx, h / divy);
							}
						}
						nowjudge2[5 - values[1]] = new SkinImage(images, values[9]);
						nowjudge2[5 - values[1]].setTimer(values[10]);
						shift2 = (values[11] != 1);
						// System.out.println("Nowjudge Added - " + (5 -
						// values[1]));
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});

		addCommandWord(new CommandWord("DST_NOWJUDGE_2P") {
			@Override
			public void execute(String[] str) {
				if (nowjudge2[5 - Integer.parseInt(str[1])] != null) {
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
						nowjudge2[5 - values[1]].setDestination(values[2], values[3] * dstw / srcw, dsth
								- (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6] * dsth
								/ srch, values[7], values[8], values[9], values[10], values[11], values[12],
								values[13], values[14], values[15], values[16], values[17], values[18], values[19],
								values[20]);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});

		addCommandWord(new CommandWord("SRC_NOWCOMBO_1P") {
			@Override
			public void execute(String[] str) {
				int gr = Integer.parseInt(str[2]);
				if (gr < imagelist.size() && imagelist.get(gr) != null) {
					try {
						int[] values = parseInt(str);
						int x = values[3];
						int y = values[4];
						int w = values[5];
						if (w == -1) {
							w = imagelist.get(gr).getWidth();
						}
						int h = values[6];
						if (h == -1) {
							h = imagelist.get(gr).getHeight();
						}
						int divx = values[7];
						if (divx <= 0) {
							divx = 1;
						}
						int divy = values[8];
						if (divy <= 0) {
							divy = 1;
						}
						TextureRegion[][] images = new TextureRegion[divy][divx];
						for (int i = 0; i < divx; i++) {
							for (int j = 0; j < divy; j++) {
								images[j][i] = new TextureRegion(imagelist.get(gr), x + w / divx * i, y + h / divy * j,
										w / divx, h / divy);
							}
						}

						nowcombo[5 - values[1]] = new SkinNumber(images, values[9], values[13], images.length > 10 ? 2
								: 0, values[11]);
						nowcombo[5 - values[1]].setTimer(values[10]);
						nowcombo[5 - values[1]].setAlign(values[12]);
						// System.out.println("Number Added - " +
						// (num.getId()));
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});

		addCommandWord(new CommandWord("DST_NOWCOMBO_1P") {
			@Override
			public void execute(String[] str) {
				if (nowcombo[5 - Integer.parseInt(str[1])] != null) {
					try {
						int[] values = parseInt(str);
						nowcombo[5 - values[1]].setDestination(values[2], values[3] * dstw / srcw, -values[4] * dsth
								/ srch, values[5] * dstw / srcw, values[6] * dsth / srch, values[7], values[8],
								values[9], values[10], values[11], values[12], values[13], values[14], values[15],
								values[16], values[17], values[18], values[19], values[20]);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});

		addCommandWord(new CommandWord("SRC_NOWCOMBO_2P") {
			@Override
			public void execute(String[] str) {
				int gr = Integer.parseInt(str[2]);
				if (gr < imagelist.size() && imagelist.get(gr) != null) {
					try {
						int[] values = parseInt(str);
						int x = values[3];
						int y = values[4];
						int w = values[5];
						if (w == -1) {
							w = imagelist.get(gr).getWidth();
						}
						int h = values[6];
						if (h == -1) {
							h = imagelist.get(gr).getHeight();
						}
						int divx = values[7];
						if (divx <= 0) {
							divx = 1;
						}
						int divy = values[8];
						if (divy <= 0) {
							divy = 1;
						}
						TextureRegion[][] images = new TextureRegion[divy][divx];
						for (int i = 0; i < divx; i++) {
							for (int j = 0; j < divy; j++) {
								images[j][i] = new TextureRegion(imagelist.get(gr), x + w / divx * i, y + h / divy * j,
										w / divx, h / divy);
							}
						}

						nowcombo2[5 - values[1]] = new SkinNumber(images, values[9], values[13], images.length > 10 ? 2
								: 0, values[11]);
						nowcombo2[5 - values[1]].setTimer(values[10]);
						nowcombo2[5 - values[1]].setAlign(values[12]);
						// System.out.println("Number Added - " +
						// (num.getId()));
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});

		addCommandWord(new CommandWord("DST_NOWCOMBO_2P") {
			@Override
			public void execute(String[] str) {
				if (nowcombo2[5 - Integer.parseInt(str[1])] != null) {
					try {
						int[] values = parseInt(str);
						nowcombo2[5 - values[1]].setDestination(values[2], values[3] * dstw / srcw, -values[4] * dsth
								/ srch, values[5] * dstw / srcw, values[6] * dsth / srch, values[7], values[8],
								values[9], values[10], values[11], values[12], values[13], values[14], values[15],
								values[16], values[17], values[18], values[19], values[20]);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});

		addCommandWord(new CommandWord("SRC_JUDGELINE") {
			@Override
			public void execute(String[] str) {
				int gr = Integer.parseInt(str[2]);
				if (gr < imagelist.size() && imagelist.get(gr) != null) {
					try {
						int[] values = parseInt(str);
						int x = values[3];
						int y = values[4];
						int w = values[5];
						if (w == -1) {
							w = imagelist.get(gr).getWidth();
						}
						int h = values[6];
						if (h == -1) {
							h = imagelist.get(gr).getHeight();
						}
						int divx = values[7];
						if (divx <= 0) {
							divx = 1;
						}
						int divy = values[8];
						if (divy <= 0) {
							divy = 1;
						}
						TextureRegion[] images = new TextureRegion[divx * divy];
						for (int i = 0; i < divx; i++) {
							for (int j = 0; j < divy; j++) {
								images[divx * j + i] = new TextureRegion(imagelist.get(gr), x + w / divx * i, y + h
										/ divy * j, w / divx, h / divy);
							}
						}
						line = new SkinImage(images, values[9]);
						line.setTimer(values[10]);
						line.setOffsety(BMSPlayer.OFFSET_LIFT);
						// System.out.println("Object Added - " +
						// (part.getTiming()));
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
				if (line != null) {
					skin.add(line);
				}
			}
		});

		addCommandWord(new CommandWord("DST_JUDGELINE") {
			@Override
			public void execute(String[] str) {
				if (line != null) {
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
						line.setDestination(values[2], values[3] * dstw / srcw, dsth - (values[4] + values[6]) * dsth
								/ srch, values[5] * dstw / srcw, values[6] * dsth / srch, values[7], values[8],
								values[9], values[10], values[11], values[12], values[13], values[14], values[15],
								values[16], values[17], values[18], values[19], values[20]);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});

		addCommandWord(new CommandWord("SRC_GROOVEGAUGE") {
			@Override
			public void execute(String[] str) {
				int playside = Integer.parseInt(str[1]);
				final int divx = Integer.parseInt(str[7]);
				final int divy = Integer.parseInt(str[8]);
				gauge = new Sprite[divx * divy * 2];
				final int w = Integer.parseInt(str[5]);
				final int h = Integer.parseInt(str[6]);
				for (int x = 0; x < divx; x++) {
					for (int y = 0; y < divy; y++) {
						gauge[y * divx + x] = new Sprite(imagelist.get(Integer.parseInt(str[2])),
								Integer.parseInt(str[3]) + w * x / divx, Integer.parseInt(str[4]) + h * y / divy, w
										/ divx, h / divy);
						gauge[y * divx + x + divx * divy] = new Sprite(imagelist.get(Integer.parseInt(str[2])),
								Integer.parseInt(str[3]) + w * x / divx, Integer.parseInt(str[4]) + h * y / divy, w
										/ divx, h / divy);
					}
				}
				groovex = Integer.parseInt(str[11]);
				groovey = Integer.parseInt(str[12]);
			}
		});
		addCommandWord(new CommandWord("DST_GROOVEGAUGE") {
			@Override
			public void execute(String[] str) {
				if (gauger.x == 0) {
					skin.add(new PlaySkin.SkinGaugeObject(skin));
				}
				gauger.width = (groovex >= 1) ? (groovex * 50 * dstw / srcw) : (Integer.parseInt(str[5]) * dstw / srcw);
				gauger.height = (groovey >= 1) ? (groovey * 50 * dsth / srch)
						: (Integer.parseInt(str[6]) * dsth / srch);
				gauger.x = Integer.parseInt(str[3]) * dstw / srcw;
				gauger.y = dsth - Integer.parseInt(str[4]) * dsth / srch - gauger.height;
			}
		});

	}

	public PlaySkin loadPlaySkin(File f, BMSPlayer player, LR2SkinHeader header, int[] option, Map property)
			throws IOException {

		skin = new PlaySkin(srcw, srch, dstw, dsth);
		if (header.getMode() == 2 || header.getMode() == 3) {
			note = new Sprite[16];
			lnstart = new Sprite[16];
			lnend = new Sprite[16];
			lnbody = new Sprite[16];
			lnbodya = new Sprite[16];
			mine = new Sprite[16];
			laner = new Rectangle[16];
		}
		if (header.getMode() == 4) {
			note = new Sprite[9];
			lnstart = new Sprite[9];
			lnend = new Sprite[9];
			lnbody = new Sprite[9];
			lnbodya = new Sprite[9];
			mine = new Sprite[9];
			laner = new Rectangle[9];
		}
		this.loadSkin(skin, f, player, header, option, property);

		skin.setNote(note);
		skin.setMinenote(mine);
		skin.setLongnote(new Sprite[][] { lnend, lnstart, lnbodya, lnbody, lnend, lnstart, lnbodya, lnbody, lnbodya,
				lnbody });
		skin.setLaneregion(laner);
		skin.setGauge(gauge);
		skin.setLine(lines.toArray(new SkinImage[lines.size()]));
		skin.setGaugeRegion(gauger);

		if (nowjudge2[0] != null) {
			skin.setJudgeregion(new PlaySkin.JudgeRegion[] { new PlaySkin.JudgeRegion(nowjudge, nowcombo, shift),
					new PlaySkin.JudgeRegion(nowjudge2, nowcombo2, shift2) });
			skin.setLaneGroupRegion(new Rectangle[] { playerr, playerr });
		} else {
			skin.setJudgeregion(new PlaySkin.JudgeRegion[] { new PlaySkin.JudgeRegion(nowjudge, nowcombo, shift) });
			skin.setLaneGroupRegion(new Rectangle[] { playerr });
		}

		return skin;
	}
}
