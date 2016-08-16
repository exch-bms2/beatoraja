package bms.player.beatoraja.skin;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

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

	public PlaySkin loadPlaySkin(File f, int[] option) throws IOException {

		skin = new PlaySkin(7);

		this.loadSkin(skin, f, option);

		skin.setNote(note);
		skin.setMinenote(mine);
		skin.setLongnote(new Sprite[][] { lnend, lnstart, lnbodya, lnbody });
		skin.setLaneregion(laner);
		return skin;
	}
}
