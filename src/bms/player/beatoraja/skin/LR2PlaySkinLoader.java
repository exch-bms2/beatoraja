package bms.player.beatoraja.skin;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import bms.player.beatoraja.MainState;
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
	
	private Rectangle playerr = new Rectangle(0,0,0,0);
	
	public LR2PlaySkinLoader() {
		addCommandWord(new CommandWord("CLOSE") {
			@Override
			public void execute(String[] str) {
				skin.setCloseTime(Integer.parseInt(str[1]));
			}
		});
		addCommandWord(new CommandWord("PLAYSTART") {
			@Override
			public void execute(String[] str) {
				skin.setPlaystartTime(Integer.parseInt(str[1]));
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
				if (bga != null) {
					skin.setDestination(bga, 0, Integer.parseInt(str[3]),
							480 - Integer.parseInt(str[4]) - Integer.parseInt(str[6]), Integer.parseInt(str[5]),
							Integer.parseInt(str[6]), 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
				}
			}
		});
		addCommandWord(new CommandWord("DST_LINE") {
			@Override
			public void execute(String[] str) {
			}
		});
		addCommandWord(new CommandWord("SRC_NOTE") {
			@Override
			public void execute(String[] str) {
				int lane = Integer.parseInt(str[1]);
				if (lane <= 7) {
					lane = (lane == 0 ? 7 : lane - 1);
					if (note[lane] == null) {
						note[lane] = new Sprite(imagelist.get(Integer.parseInt(str[2])), Integer.parseInt(str[3]),
								Integer.parseInt(str[4]), Integer.parseInt(str[5]), Integer.parseInt(str[6]));
					}
				}
				if(lanerender == null) {
					lanerender = new PlaySkin.SkinLaneObject(skin);
					skin.add(lanerender);
				}
			}
		});
		addCommandWord(new CommandWord("SRC_LN_END") {
			@Override
			public void execute(String[] str) {
				int lane = Integer.parseInt(str[1]);
				if (lane <= 7) {
					lane = (lane == 0 ? 7 : lane - 1);
					if (lnend[lane] == null) {
						lnend[lane] = new Sprite(imagelist.get(Integer.parseInt(str[2])), Integer.parseInt(str[3]),
								Integer.parseInt(str[4]), Integer.parseInt(str[5]), Integer.parseInt(str[6]));
					}
				}
			}
		});
		addCommandWord(new CommandWord("SRC_LN_START") {
			@Override
			public void execute(String[] str) {
				int lane = Integer.parseInt(str[1]);
				if (lane <= 7) {
					lane = (lane == 0 ? 7 : lane - 1);
					if (lnstart[lane] == null) {
						lnstart[lane] = new Sprite(imagelist.get(Integer.parseInt(str[2])), Integer.parseInt(str[3]),
								Integer.parseInt(str[4]), Integer.parseInt(str[5]), Integer.parseInt(str[6]));
					}
				}
			}
		});

		addCommandWord(new CommandWord("SRC_LN_BODY") {
			@Override
			public void execute(String[] str) {
				int lane = Integer.parseInt(str[1]);
				if (lane <= 7) {
					lane = (lane == 0 ? 7 : lane - 1);
					if (lnbody[lane] == null) {
						lnbody[lane] = new Sprite(imagelist.get(Integer.parseInt(str[2])), Integer.parseInt(str[3]),
								Integer.parseInt(str[4]), Integer.parseInt(str[5]), 1);
						lnbodya[lane] = new Sprite(imagelist.get(Integer.parseInt(str[2])), Integer.parseInt(str[3]),
								Integer.parseInt(str[4]) + Integer.parseInt(str[6]) - 1, Integer.parseInt(str[5]), 1);
					}
				}
			}
		});

		addCommandWord(new CommandWord("SRC_MINE") {
			@Override
			public void execute(String[] str) {
				int lane = Integer.parseInt(str[1]);
				if (lane <= 7) {
					lane = (lane == 0 ? 7 : lane - 1);
					if (mine[lane] == null) {
						mine[lane] = new Sprite(imagelist.get(Integer.parseInt(str[2])), Integer.parseInt(str[3]),
								Integer.parseInt(str[4]), Integer.parseInt(str[5]), Integer.parseInt(str[6]));
					}
				}
			}
		});

		addCommandWord(new CommandWord("DST_NOTE") {
			@Override
			public void execute(String[] str) {
				int lane = Integer.parseInt(str[1]);
				if (lane <= 7) {
					lane = (lane == 0 ? 7 : lane - 1);
					if (laner[lane] == null) {
						laner[lane] = new Rectangle(Integer.parseInt(str[3]) * dstw / srcw, dsth
								- Integer.parseInt(str[4]) * dsth / srch, Integer.parseInt(str[5]) * dstw / srcw,
								Integer.parseInt(str[4]) * dsth / srch);
					}
					if(laner[lane].x < playerr.x) {
						playerr.width += playerr.x - laner[lane].x;
						playerr.x = laner[lane].x;
					}
					if(laner[lane].x + laner[lane].width > playerr.x + playerr.width) {
						playerr.width += laner[lane].x + laner[lane].width - (playerr.x + playerr.width);
					}
					if(laner[lane].y > playerr.y) {
						playerr.y = laner[lane].y;
						playerr.height = laner[lane].height;
					}
				}
			}
		});

		addCommandWord(new CommandWord("SRC_NOWJUDGE_1P") {
			@Override
			public void execute(String[] str) {
				// TODO 未実装
			}
		});

		addCommandWord(new CommandWord("DST_NOWJUDGE_1P") {
			@Override
			public void execute(String[] str) {
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
								images[divx * j + i] = new TextureRegion(imagelist.get(gr), x + w / divx
										* i, y + h / divy * j, w / divx, h / divy);
							}
						}
						line = new SkinImage(images, values[9]);
						line.setTiming(values[10]);
						line.setOffsetYReferenceID(BMSPlayer.OFFSET_LIFT);
						// System.out.println("Object Added - " +
						// (part.getTiming()));
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
				if(line != null) {
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
						if(values[5] < 0) {
							values[3] += values[5];
							values[5] = -values[5];
						}
						if(values[6] < 0) {
							values[4] += values[6];
							values[6] = -values[6];										
						}
						line.setDestination(values[2], values[3] * dstw / srcw, dsth
								- (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6]
								* dsth / srch, values[7], values[8], values[9], values[10], values[11],
								values[12], values[13], values[14], values[15], values[16], values[17],
								values[18], values[19], values[20]);
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
				gauge = new Sprite[divx * divy];
				final int w = Integer.parseInt(str[5]);
				final int h = Integer.parseInt(str[6]);
				for(int x = 0;x < divx;x++) {
					for(int y = 0;y < divy;y++) {
						gauge[y * divx + x] = new Sprite(imagelist.get(Integer.parseInt(str[2])), Integer.parseInt(str[3]) + w * x / divx,
								Integer.parseInt(str[4]) + h * y / divy, w / divx, h / divy);
					}
				}
				final int addx = Integer.parseInt(str[11]);
				final int addy = Integer.parseInt(str[12]);
				gauger.width = (addx > 0 ? addx * 50 : w) * dstw / srcw;
				gauger.height = (addy > 0 ? addy * 50 : h) * dsth / srch;
			}
		});
		addCommandWord(new CommandWord("DST_GROOVEGAUGE") {
			@Override
			public void execute(String[] str) {
				if(gauger.x == 0) {
					skin.add(new PlaySkin.SkinGaugeObject(skin));

				}
				gauger.x = Integer.parseInt(str[3]) * dstw / srcw;
				gauger.y = dsth - Integer.parseInt(str[4]) * dsth / srch - gauger.height;
			}
		});

	}

	private PlaySkin skin;
	float srcw = 640;
	float srch = 480;
	float dstw = 1280;
	float dsth = 720;
	Sprite[] note = new Sprite[8];
	Sprite[] lnstart = new Sprite[8];
	Sprite[] lnend = new Sprite[8];
	Sprite[] lnbody = new Sprite[8];
	Sprite[] lnbodya = new Sprite[8];
	Sprite[] mine = new Sprite[8];
	Rectangle[] laner = new Rectangle[8];
	Sprite[] gauge = new Sprite[4];
	Rectangle gauger = new Rectangle();
	private SkinImage line;

	public PlaySkin loadPlaySkin(File f, BMSPlayer player, int[] option) throws IOException {

		skin = new PlaySkin(7);
		this.loadSkin(skin, f, player, option);

		Texture lct = new Texture("skin/lanecover.png");
		skin.lanecover = new Sprite(lct, 0, 0, 390, 580);

		skin.setLaneGroupRegion(new Rectangle[]{playerr});
		skin.setNote(note);
		skin.setMinenote(mine);
		skin.setLongnote(new Sprite[][] { lnend, lnstart, lnbodya, lnbody,lnend, lnstart, lnbodya, lnbody, lnbodya, lnbody });
		skin.setLaneregion(laner);
		skin.setGauge(gauge);
		skin.setGaugeRegion(gauger);
		return skin;
	}
}
