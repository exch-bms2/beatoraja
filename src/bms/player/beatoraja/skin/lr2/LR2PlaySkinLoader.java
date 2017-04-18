package bms.player.beatoraja.skin.lr2;

import java.io.*;
import java.util.*;

import bms.model.Mode;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.play.*;
import bms.player.beatoraja.skin.*;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import static bms.player.beatoraja.skin.SkinProperty.*;

public class LR2PlaySkinLoader extends LR2SkinCSVLoader {

	private SkinBGA bga;

	private SkinNote lanerender;

	private Rectangle[] playerr;

	private PlaySkin skin;
	private SkinSource[] note = new SkinSource[8];
	private SkinSource[] lnstart = new SkinSource[8];
	private SkinSource[] lnend = new SkinSource[8];
	private SkinSource[] lnbody = new SkinSource[8];
	private SkinSource[] lnbodya = new SkinSource[8];
	private SkinSource[] mine = new SkinSource[8];
	private Rectangle[] laner = new Rectangle[8];
	private SkinGauge gauger = null;
	private SkinImage line;
	private List<SkinImage> lines = new ArrayList<SkinImage>();
	private SkinImage li;

	private SkinJudge[] judge = new SkinJudge[2];

	private int groovex = 0;
	private int groovey = 0;

	public LR2PlaySkinLoader(final Resolution src, final Resolution dst) {
		super(src, dst);

		final float srcw = src.width;
		final float srch = src.height;
		final float dstw = dst.width;
		final float dsth = dst.height;

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
		addCommandWord(new CommandWord("LOADSTART") {
			@Override
			public void execute(String[] str) {
				skin.setLoadstart(Integer.parseInt(str[1]));
			}
		});
		addCommandWord(new CommandWord("LOADEND") {
			@Override
			public void execute(String[] str) {
				skin.setLoadend(Integer.parseInt(str[1]));
			}
		});
		addCommandWord(new CommandWord("SRC_BGA") {
			@Override
			public void execute(String[] str) {
				bga = new SkinBGA();
				skin.add(bga);
			}
		});
		addCommandWord(new CommandWord("DST_BGA") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				if (bga != null) {
					skin.setDestination(bga, 0, values[3], srch - values[4] - values[6], values[5], values[6],
							values[7], values[8], values[9], values[10], values[11], values[12], values[13], values[14],
							values[15], values[16], values[17], values[18], values[19], values[20]);
				}
			}
		});
		addCommandWord(new CommandWord("SRC_LINE") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					li = new SkinImage(images, values[10], values[9]);
					li.setOffsety(OFFSET_LIFT);
					lines.add(li);
					// System.out.println("Object Added - " +
					// (part.getTiming()));
				}
			}
		});
		addCommandWord(new CommandWord("DST_LINE") {
			@Override
			public void execute(String[] str) {
				if (li != null) {
					int[] values = parseInt(str);
					if (values[5] < 0) {
						values[3] += values[5];
						values[5] = -values[5];
					}
					if (values[6] < 0) {
						values[4] += values[6];
						values[6] = -values[6];
					}
					li.setDestination(values[2], values[3] * dstw / srcw, dsth - (values[4] + values[6]) * dsth / srch,
							values[5] * dstw / srcw, values[6] * dsth / srch, values[7], values[8], values[9],
							values[10], values[11], values[12], values[13], values[14], values[15], values[16],
							values[17], values[18], values[19], values[20]);
					playerr[lines.size() == 1 ? 0 : 1] = new Rectangle(values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							(values[4] + values[6]) * dsth / srch);
				}
			}
		});
		addCommandWord(new CommandWord("SRC_NOTE") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				int lane = Integer.parseInt(str[1]);
				if (lane == 10) {
					lane = laner.length / playerr.length * 2 - 1;
				} else if (lane >= 10) {
					lane = (lane - 11) + laner.length / playerr.length;
				} else if (lane == 0) {
					lane = laner.length / playerr.length - 1;
				} else {
					lane -= 1;
				}
				if(lane < 0 || lane >= laner.length) {
					return;
				}
				if (lane < note.length && note[lane] == null) {
					TextureRegion[] images = getSourceImage(values);
					if (images != null) {
						note[lane] = new SkinSourceImage(images, values[10], values[9]);
					}
				}
			}
		});
		addCommandWord(new CommandWord("SRC_LN_END") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				int lane = Integer.parseInt(str[1]);
				if (lane == 10) {
					lane = laner.length / playerr.length * 2 - 1;
				} else if (lane >= 10) {
					lane = (lane - 11) + laner.length / playerr.length;
				} else if (lane == 0) {
					lane = laner.length / playerr.length - 1;
				} else {
					lane -= 1;
				}
				if(lane < 0 || lane >= laner.length) {
					return;
				}
				if (lane < lnend.length && lnend[lane] == null) {
					TextureRegion[] images = getSourceImage(values);
					if (images != null) {
						lnend[lane] = new SkinSourceImage(images, values[10], values[9]);
					}
				}
			}
		});
		addCommandWord(new CommandWord("SRC_LN_START") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				int lane = values[1];
				if (lane == 10) {
					lane = laner.length / playerr.length * 2 - 1;
				} else if (lane >= 10) {
					lane = (lane - 11) + laner.length / playerr.length;
				} else if (lane == 0) {
					lane = laner.length / playerr.length - 1;
				} else {
					lane -= 1;
				}
				if(lane < 0 || lane >= laner.length) {
					return;
				}
				if (lane < lnstart.length && lnstart[lane] == null) {
					TextureRegion[] images = getSourceImage(values);
					if (images != null) {
						lnstart[lane] = new SkinSourceImage(images, values[10], values[9]);
					}
				}
			}
		});

		addCommandWord(new CommandWord("SRC_LN_BODY") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				int lane = Integer.parseInt(str[1]);
				if (lane == 10) {
					lane = laner.length / playerr.length * 2 - 1;
				} else if (lane >= 10) {
					lane = (lane - 11) + laner.length / playerr.length;
				} else if (lane == 0) {
					lane = laner.length / playerr.length - 1;
				} else {
					lane -= 1;
				}
				if(lane < 0 || lane >= laner.length) {
					return;
				}
				if (lane < lnbody.length && lnbody[lane] == null) {
					TextureRegion[] images = getSourceImage(values);
					if (images != null) {
						lnbody[lane] = new SkinSourceImage(images, 0, 0);
						lnbodya[lane] = new SkinSourceImage(images, values[10], values[9]);
					}
				}
			}
		});

		addCommandWord(new CommandWord("SRC_MINE") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				int lane = Integer.parseInt(str[1]);
				if (lane == 10) {
					lane = laner.length / playerr.length * 2 - 1;
				} else if (lane >= 10) {
					lane = (lane - 11) + laner.length / playerr.length;
				} else if (lane == 0) {
					lane = laner.length / playerr.length - 1;
				} else {
					lane -= 1;
				}
				if(lane < 0 || lane >= laner.length) {
					return;
				}
				if (lane < mine.length && mine[lane] == null) {
					TextureRegion[] images = getSourceImage(values);
					if (images != null) {
						mine[lane] = new SkinSourceImage(images, values[10], values[9]);
					}
				}
			}
		});

		addCommandWord(new CommandWord("DST_NOTE") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				int lane = Integer.parseInt(str[1]);
				if (lane == 10) {
					lane = laner.length / playerr.length * 2 - 1;
				} else if (lane >= 10) {
					lane = (lane - 11) + laner.length / playerr.length;
				} else if (lane == 0) {
					lane = laner.length / playerr.length - 1;
				} else {
					lane -= 1;
				}
				if(lane < 0 || lane >= laner.length) {
					return;
				}
				if (laner[lane] == null) {
					laner[lane] = new Rectangle(values[3] * dstw / srcw, dsth - (values[4] + values[6]) * dsth / srch,
							values[5] * dstw / srcw, (values[4] + values[6]) * dsth / srch);
				}
				if (lanerender == null) {
					final SkinSource[][] lns = new SkinSource[][]{ lnend, lnstart, lnbodya, lnbody, lnend,
							lnstart, lnbodya, lnbody, lnbodya, lnbody };
					final SkinSource[][] lnss = new SkinSource[lnstart.length][10];
					for(int i = 0;i < 10;i++) {
						for(int j = 0;j < lnstart.length;j++) {
							lnss[j][i] = lns[i][j];
						}
					}
					lanerender = new SkinNote(note, lnss, mine, values[6] * dsth / srch);
					skin.add(lanerender);
				}
			}
		});

		addCommandWord(new CommandWord("SRC_NOWJUDGE_1P") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					if (judge[0] == null) {
						judge[0] = new SkinJudge(0, (values[11] != 1));
						skin.add(judge[0]);
					}
					judge[0].getJudge()[5 - values[1]] = new SkinImage(images, values[10], values[9]);
					// System.out.println("Nowjudge Added - " + (5 -
					// values[1]));
				}
			}
		});

		addCommandWord(new CommandWord("DST_NOWJUDGE_1P") {

			private boolean detail = false;

			@Override
			public void execute(String[] str) {
				if (judge[0] != null && judge[0].getJudge()[5 - Integer.parseInt(str[1])] != null) {
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
						judge[0].getJudge()[5 - values[1]].setDestination(values[2], values[3] * dstw / srcw,
								dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
								values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
								values[12], values[13], values[14], values[15], values[16], values[17], values[18],
								values[19], values[20]);

						if (!detail) {
							detail = true;
							Texture tex = new Texture("skin/default/judgedetail.png");

							SkinImage early = new SkinImage(new TextureRegion(tex, 0, 0, 50,20));
							early.setDestination(0, (values[3] + values[5] / 2) * dstw / srcw,
									dsth - (values[4] - 5) * dsth / srch, 35 * dstw / srcw, 14 * dsth / srch, 0, 255,
									255, 255, 255, 0, 0, 0, 0, -1, 46, 1998, 0, 1242);
							early.setDestination(500, (values[3] + values[5] / 2) * dstw / srcw,
									dsth - (values[4] - 5) * dsth / srch, 35 * dstw / srcw, 14 * dsth / srch, 0, 255,
									255, 255, 255, 0, 0, 0, 0, -1, 46, 1998, 0, 1242);
							skin.add(early);
							SkinImage late = new SkinImage(new TextureRegion(tex, 50, 0, 50,20));
							late.setDestination(0, (values[3] + values[5] / 2) * dstw / srcw,
									dsth - (values[4] - 5) * dsth / srch, 35 * dstw / srcw, 14 * dsth / srch, 0, 255,
									255, 255, 255, 0, 0, 0, 0, -1, 46, 1998, 0, 1243);
							late.setDestination(500, (values[3] + values[5] / 2) * dstw / srcw,
									dsth - (values[4] - 5) * dsth / srch, 35 * dstw / srcw, 14 * dsth / srch, 0, 255,
									255, 255, 255, 0, 0, 0, 0, -1, 46, 1998, 0, 1243);
							skin.add(late);

							TextureRegion[][] images = TextureRegion.split(tex, 10, 20);
							SkinNumber num = new SkinNumber(new TextureRegion[][] { images[1] },
									new TextureRegion[][] { images[2] }, 0, 0, 4, 0, 525);
							num.setAlign(values[12]);
							num.setDestination(0, (values[3] + values[5] / 2) * dstw / srcw,
									dsth - (values[4] - 5) * dsth / srch, 7 * dstw / srcw, 14 * dsth / srch, 0, 255,
									255, 255, 255, 0, 0, 0, 0, -1, 46, 1999, 0, 241);
							num.setDestination(500, (values[3] + values[5] / 2) * dstw / srcw,
									dsth - (values[4] - 5) * dsth / srch, 7 * dstw / srcw, 14 * dsth / srch, 0, 255,
									255, 255, 255, 0, 0, 0, 0, -1, 46, 1999, 0, 241);
							skin.add(num);
							SkinNumber num2 = new SkinNumber(new TextureRegion[][] { images[3] },
									new TextureRegion[][] { images[4] }, 0, 0, 4, 0, 525);
							num2.setAlign(values[12]);
							num2.setDestination(0, (values[3] + values[5] / 2) * dstw / srcw,
									dsth - (values[4] - 5) * dsth / srch, 7 * dstw / srcw, 14 * dsth / srch, 0, 255,
									255, 255, 255, 0, 0, 0, 0, -1, 46, 1999, 0, -241);
							num2.setDestination(500, (values[3] + values[5] / 2) * dstw / srcw,
									dsth - (values[4] - 5) * dsth / srch, 7 * dstw / srcw, 14 * dsth / srch, 0, 255,
									255, 255, 255, 0, 0, 0, 0, -1, 46, 1999, 0, -241);
							skin.add(num2);
						}

					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		});

		addCommandWord(new CommandWord("SRC_NOWJUDGE_2P") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					if (judge[1] == null) {
						judge[1] = new SkinJudge(1, (values[11] != 1));
						skin.add(judge[1]);
					}
					judge[1].getJudge()[5 - values[1]] = new SkinImage(images, values[10], values[9]);
					// System.out.println("Nowjudge Added - " + (5 -
					// values[1]));
				}
			}
		});

		addCommandWord(new CommandWord("DST_NOWJUDGE_2P") {

			private boolean detail = false;

			@Override
			public void execute(String[] str) {
				if (judge[1] != null && judge[1].getJudge()[5 - Integer.parseInt(str[1])] != null) {
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
						judge[1].getJudge()[5 - values[1]].setDestination(values[2], values[3] * dstw / srcw,
								dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
								values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
								values[12], values[13], values[14], values[15], values[16], values[17], values[18],
								values[19], values[20]);
						
						if (!detail) {
							detail = true;
							Texture tex = new Texture("skin/default/judgedetail.png");

							SkinImage early = new SkinImage(new TextureRegion(tex, 0, 0, 50,20));
							early.setDestination(0, (values[3] + values[5] / 2) * dstw / srcw,
									dsth - (values[4] - 5) * dsth / srch, 35 * dstw / srcw, 14 * dsth / srch, 0, 255,
									255, 255, 255, 0, 0, 0, 0, -1, 47, 1998, 0, 1262);
							early.setDestination(500, (values[3] + values[5] / 2) * dstw / srcw,
									dsth - (values[4] - 5) * dsth / srch, 35 * dstw / srcw, 14 * dsth / srch, 0, 255,
									255, 255, 255, 0, 0, 0, 0, -1, 47, 1998, 0, 1262);
							skin.add(early);
							SkinImage late = new SkinImage(new TextureRegion(tex, 50, 0, 50,20));
							late.setDestination(0, (values[3] + values[5] / 2) * dstw / srcw,
									dsth - (values[4] - 5) * dsth / srch, 35 * dstw / srcw, 14 * dsth / srch, 0, 255,
									255, 255, 255, 0, 0, 0, 0, -1, 47, 1998, 0, 1263);
							late.setDestination(500, (values[3] + values[5] / 2) * dstw / srcw,
									dsth - (values[4] - 5) * dsth / srch, 35 * dstw / srcw, 14 * dsth / srch, 0, 255,
									255, 255, 255, 0, 0, 0, 0, -1, 47, 1998, 0, 1263);
							skin.add(late);

							TextureRegion[][] images = TextureRegion.split(tex, 10, 20);
							SkinNumber num = new SkinNumber(new TextureRegion[][] { images[1] },
									new TextureRegion[][] { images[2] }, 0, 0, 4, 0, 525);
							num.setAlign(values[12]);
							num.setDestination(0, (values[3] + values[5] / 2) * dstw / srcw,
									dsth - (values[4] - 5) * dsth / srch, 7 * dstw / srcw, 14 * dsth / srch, 0, 255,
									255, 255, 255, 0, 0, 0, 0, -1, 47, 1999, 0, 261);
							num.setDestination(500, (values[3] + values[5] / 2) * dstw / srcw,
									dsth - (values[4] - 5) * dsth / srch, 7 * dstw / srcw, 14 * dsth / srch, 0, 255,
									255, 255, 255, 0, 0, 0, 0, -1, 47, 1999, 0, 261);
							skin.add(num);
							SkinNumber num2 = new SkinNumber(new TextureRegion[][] { images[3] },
									new TextureRegion[][] { images[4] }, 0, 0, 4, 0, 525);
							num2.setAlign(values[12]);
							num2.setDestination(0, (values[3] + values[5] / 2) * dstw / srcw,
									dsth - (values[4] - 5) * dsth / srch, 7 * dstw / srcw, 14 * dsth / srch, 0, 255,
									255, 255, 255, 0, 0, 0, 0, -1, 47, 1999, 0, -261);
							num2.setDestination(500, (values[3] + values[5] / 2) * dstw / srcw,
									dsth - (values[4] - 5) * dsth / srch, 7 * dstw / srcw, 14 * dsth / srch, 0, 255,
									255, 255, 255, 0, 0, 0, 0, -1, 47, 1999, 0, -261);
							skin.add(num2);
						}
					} catch(Throwable e) {
						e.printStackTrace();
					}
				}
			}
		});

		addCommandWord(new CommandWord("SRC_NOWCOMBO_1P") {
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
				TextureRegion[] simages = getSourceImage(values);
				if (simages != null) {
					TextureRegion[][] images = new TextureRegion[divy][divx];
					for (int i = 0; i < divx; i++) {
						for (int j = 0; j < divy; j++) {
							images[j][i] = simages[j * divx + i];
						}
					}

					judge[0].getJudgeCount()[5 - values[1]] = new SkinNumber(images, values[10], values[9], values[13],
							images.length > 10 ? 2 : 0, values[11]);
					judge[0].getJudgeCount()[5 - values[1]].setAlign(values[12]);
					// System.out.println("Number Added - " +
					// (num.getId()));
				}
			}
		});

		addCommandWord(new CommandWord("DST_NOWCOMBO_1P") {
			@Override
			public void execute(String[] str) {
				if (judge[0] != null && judge[0].getJudgeCount()[5 - Integer.parseInt(str[1])] != null) {
					int[] values = parseInt(str);
					judge[0].getJudgeCount()[5 - values[1]].setDestination(values[2], values[3] * dstw / srcw,
							-values[4] * dsth / srch, values[5] * dstw / srcw, values[6] * dsth / srch, values[7],
							values[8], values[9], values[10], values[11], values[12], values[13], values[14],
							values[15], values[16], values[17], values[18], values[19], values[20]);
				}
			}
		});

		addCommandWord(new CommandWord("SRC_NOWCOMBO_2P") {
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
				TextureRegion[] simages = getSourceImage(values);
				if (simages != null) {
					TextureRegion[][] images = new TextureRegion[divy][divx];
					for (int i = 0; i < divx; i++) {
						for (int j = 0; j < divy; j++) {
							images[j][i] = simages[j * divx + i];
						}
					}

					judge[1].getJudgeCount()[5 - values[1]] = new SkinNumber(images, values[10], values[9], values[13],
							images.length > 10 ? 2 : 0, values[11]);
					judge[1].getJudgeCount()[5 - values[1]].setAlign(values[12]);
					// System.out.println("Number Added - " +
					// (num.getId()));
				}
			}
		});

		addCommandWord(new CommandWord("DST_NOWCOMBO_2P") {
			@Override
			public void execute(String[] str) {
				if (judge[1] != null && judge[1].getJudgeCount()[5 - Integer.parseInt(str[1])] != null) {
					int[] values = parseInt(str);
					judge[1].getJudgeCount()[5 - values[1]].setDestination(values[2], values[3] * dstw / srcw,
							-values[4] * dsth / srch, values[5] * dstw / srcw, values[6] * dsth / srch, values[7],
							values[8], values[9], values[10], values[11], values[12], values[13], values[14],
							values[15], values[16], values[17], values[18], values[19], values[20]);
				}
			}
		});

		addCommandWord(new CommandWord("SRC_JUDGELINE") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					line = new SkinImage(images, values[10], values[9]);
					line.setOffsety(OFFSET_LIFT);
					// System.out.println("Object Added - " +
					// (part.getTiming()));
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
					int[] values = parseInt(str);
					if (values[5] < 0) {
						values[3] += values[5];
						values[5] = -values[5];
					}
					if (values[6] < 0) {
						values[4] += values[6];
						values[6] = -values[6];
					}
					line.setDestination(values[2], values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
							values[12], values[13], values[14], values[15], values[16], values[17], values[18],
							values[19], values[20]);
				}
			}
		});

		addCommandWord(new CommandWord("SRC_GROOVEGAUGE") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				if (values[2] < imagelist.size() && imagelist.get(values[2]) != null) {
					int playside = values[1];
					int divx = values[7];
					if (divx <= 0) {
						divx = 1;
					}
					int divy = values[8];
					if (divy <= 0) {
						divy = 1;
					}
					TextureRegion[][] gauge = new TextureRegion[(divx * divy) / 4][8];
					final int w = values[5];
					final int h = values[6];
					for (int x = 0; x < divx; x++) {
						for (int y = 0; y < divy; y++) {
							if ((y * divx + x) / 4 < gauge.length) {
								gauge[(y * divx + x) / 4][(y * divx + x) % 4] = new TextureRegion(
										(Texture) imagelist.get(values[2]), values[3] + w * x / divx,
										values[4] + h * y / divy, w / divx, h / divy);
								gauge[(y * divx + x) / 4][(y * divx + x) % 4 + 4] = new TextureRegion(
										(Texture) imagelist.get(values[2]), values[3] + w * x / divx,
										values[4] + h * y / divy, w / divx, h / divy);
							}
						}
					}
					groovex = values[11];
					groovey = values[12];
					if (gauger == null) {
						gauger = new SkinGauge(gauge, values[10], values[9]);
						skin.add(gauger);
					}
				}
			}
		});
		addCommandWord(new CommandWord("DST_GROOVEGAUGE") {
			@Override
			public void execute(String[] str) {
				if (gauger != null) {
					float width = (Math.abs(groovex) >= 1) ? (groovex * 50 * dstw / srcw)
							: (Integer.parseInt(str[5]) * dstw / srcw);
					float height = (Math.abs(groovey) >= 1) ? (groovey * 50 * dsth / srch)
							: (Integer.parseInt(str[6]) * dsth / srch);
					float x = Integer.parseInt(str[3]) * dstw / srcw - (groovex < 0 ? groovex * dstw / srcw : 0);
					float y = dsth - Integer.parseInt(str[4]) * dsth / srch - height;
					gauger.setDestination(0, x, y, width, height, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
				}
			}
		});

	}

	public PlaySkin loadPlaySkin(File f, BMSPlayer player, SkinHeader header, Map<Integer, Boolean> option,
			Map property) throws IOException {

		skin = new PlaySkin(src, dst);
		final Mode mode = SkinType.getSkinTypeById(header.getMode()).getMode();
		note = new SkinSource[mode.key];
		lnstart = new SkinSource[mode.key];
		lnend = new SkinSource[mode.key];
		lnbody = new SkinSource[mode.key];
		lnbodya = new SkinSource[mode.key];
		mine = new SkinSource[mode.key];
		laner = new Rectangle[mode.key];
		
		playerr = new Rectangle[mode.player];
		for(int i = 0;i < playerr.length;i++) {
			playerr[i] = new Rectangle();
		}
		
		this.loadSkin(skin, f, player, header, option, property);

		lanerender.setLaneRegion(laner);
		skin.setLine(lines.toArray(new SkinImage[lines.size()]));

		skin.setJudgeregion(judge[1] != null ? 2 : 1);

		skin.setLaneGroupRegion(playerr);

		return skin;
	}
}
