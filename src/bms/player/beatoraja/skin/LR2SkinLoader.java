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

import bms.player.beatoraja.PlaySkin;
import bms.player.beatoraja.PlaySkin.SkinPart;

public class LR2SkinLoader {

	public PlaySkin loadPlaySkin(File f) throws IOException {
		float srcw = 640;
		float srch = 480;
		float dstw = 1280;
		float dsth = 720;

		PlaySkin skin = new PlaySkin();
		Rectangle[] laner = new Rectangle[8];
		Sprite[] note = new Sprite[8];
		Sprite[] lnstart = new Sprite[8];
		Sprite[] lnend = new Sprite[8];
		Sprite[] lnbody = new Sprite[8];
		Sprite[] lnbodya = new Sprite[8];
		Sprite[] mine = new Sprite[8];
		List<Texture> imagelist = new ArrayList();
		List<SkinPart> partlist = new ArrayList();
		SkinPart part = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(f), "MS932"));
		String line = null;

		while ((line = br.readLine()) != null) {
			if (!line.startsWith("//")) {
				String[] str = line.split(",");
				if (str.length > 0) {
					if (str[0].equals("#IMAGE")) {
						String imagepath = str[1].replace("LR2files\\Theme",
								"skin").replace("\\", "/");
						File imagefile = new File(imagepath);
						if (imagepath.contains("*")) {
							String ext = imagepath.substring(imagepath
									.lastIndexOf("*") + 1);
							File imagedir = new File(imagepath.substring(0,
									imagepath.lastIndexOf('/')));
							if (imagedir.exists() && imagedir.isDirectory()) {
								for (File subfile : imagedir.listFiles()) {
									if (subfile.getPath().toLowerCase()
											.endsWith(ext)) {
										imagefile = subfile;
										break;
									}
								}
							}
						}
						if (imagefile.exists()) {
							try {
								imagelist.add(new Texture(Gdx.files
										.internal(imagefile.getPath())));
							} catch (GdxRuntimeException e) {
								imagelist.add(null);
								e.printStackTrace();
							}
						} else {
							imagelist.add(null);
						}
						System.out.println("Image Loaded - "
								+ (imagelist.size() - 1) + " : "
								+ imagefile.getPath());
					}

					if (str[0].equals("#SRC_IMAGE")) {
						int gr = Integer.parseInt(str[2]);
						if (gr < imagelist.size() && imagelist.get(gr) != null) {
							part = new SkinPart();
							part.image = new TextureRegion(imagelist.get(gr),
									Integer.parseInt(str[3]),
									Integer.parseInt(str[4]),
									Integer.parseInt(str[5]),
									Integer.parseInt(str[6]));
							if (str.length > 13) {
								part.timing = Integer.parseInt(str[10]);
								part.op[0] = Integer.parseInt(str[11]);
								part.op[1] = Integer.parseInt(str[12]);
								part.op[2] = Integer.parseInt(str[13]);
							}
						}
					}
					if (str[0].equals("#DST_IMAGE")) {
						if (part != null) {
							part.dst = new Rectangle(Integer.parseInt(str[3])
									* dstw / srcw, dsth
									- Integer.parseInt(str[4]) * dsth / srch,
									Integer.parseInt(str[5]) * dstw / srcw,
									Integer.parseInt(str[6]) * dsth / srch);
							if(str.length > 20) {
								part.timing = Integer.parseInt(str[17]);
								part.op[0] = Integer.parseInt(str[18]);
								part.op[1] = Integer.parseInt(str[19]);
								part.op[2] = Integer.parseInt(str[20]);
							}
							partlist.add(part);
							part = null;
						}
					}

					if (str[0].equals("#DST_BGA")) {
						skin.setBGAregion(new Rectangle(Integer.parseInt(str[3])
								* dstw / srcw, dsth - Integer.parseInt(str[4])
								* dsth / srch - Integer.parseInt(str[6]) * dsth / srch, Integer.parseInt(str[5]) * dstw
								/ srcw, Integer.parseInt(str[6]) * dsth / srch));
					}

					if (str[0].equals("#DST_LINE")) {

					}

					if (str[0].equals("#SRC_NOTE")) {
						int lane = Integer.parseInt(str[1]);
						if (lane <= 7) {
							lane = (lane == 0 ? 7 : lane - 1);
							if (note[lane] == null) {
								note[lane] = new Sprite(imagelist.get(Integer
										.parseInt(str[2])),
										Integer.parseInt(str[3]),
										Integer.parseInt(str[4]),
										Integer.parseInt(str[5]),
										Integer.parseInt(str[6]));
							}
						}
					}
					if (str[0].equals("#SRC_LN_END")) {
						int lane = Integer.parseInt(str[1]);
						if (lane <= 7) {
							lane = (lane == 0 ? 7 : lane - 1);
							if (lnend[lane] == null) {
								lnend[lane] = new Sprite(imagelist.get(Integer
										.parseInt(str[2])),
										Integer.parseInt(str[3]),
										Integer.parseInt(str[4]),
										Integer.parseInt(str[5]),
										Integer.parseInt(str[6]));
							}
						}
					}
					if (str[0].equals("#SRC_LN_START")) {
						int lane = Integer.parseInt(str[1]);
						if (lane <= 7) {
							lane = (lane == 0 ? 7 : lane - 1);
							if (lnstart[lane] == null) {
								lnstart[lane] = new Sprite(
										imagelist.get(Integer.parseInt(str[2])),
										Integer.parseInt(str[3]), Integer
												.parseInt(str[4]), Integer
												.parseInt(str[5]), Integer
												.parseInt(str[6]));
							}
						}
					}
					if (str[0].equals("#SRC_LN_BODY")) {
						int lane = Integer.parseInt(str[1]);
						if (lane <= 7) {
							lane = (lane == 0 ? 7 : lane - 1);
							if (lnbody[lane] == null) {
								lnbody[lane] = new Sprite(imagelist.get(Integer
										.parseInt(str[2])),
										Integer.parseInt(str[3]),
										Integer.parseInt(str[4]),
										Integer.parseInt(str[5]), 1);
								lnbodya[lane] = new Sprite(imagelist
										.get(Integer.parseInt(str[2])), Integer
										.parseInt(str[3]), Integer
										.parseInt(str[4])
										+ Integer.parseInt(str[6]) - 1,
										Integer.parseInt(str[5]), 1);
							}
						}
					}
					if (str[0].equals("#SRC_MINE")) {
						int lane = Integer.parseInt(str[1]);
						if (lane <= 7) {
							lane = (lane == 0 ? 7 : lane - 1);
							if (mine[lane] == null) {
								mine[lane] = new Sprite(imagelist.get(Integer
										.parseInt(str[2])),
										Integer.parseInt(str[3]),
										Integer.parseInt(str[4]),
										Integer.parseInt(str[5]),
										Integer.parseInt(str[6]));
							}
						}
					}

					if (str[0].equals("#DST_NOTE")) {
						int lane = Integer.parseInt(str[1]);
						if (lane <= 7) {
							lane = (lane == 0 ? 7 : lane - 1);
							if (laner[lane] == null) {
								laner[lane] = new Rectangle(
										Integer.parseInt(str[3]) * dstw / srcw,
										dsth - Integer.parseInt(str[4]) * dsth
												/ srch,
										Integer.parseInt(str[5]) * dstw / srcw,
										Integer.parseInt(str[4]) * dsth / srch);
							}
						}
					}

					// if (str[0].equals("#DST_SLIDER")) {
					// slider = new Sprite(imagelist.get(Integer
					// .parseInt(str[2])),
					// Integer.parseInt(str[3]),
					// Integer.parseInt(str[4]),
					// Integer.parseInt(str[5]),
					// Integer.parseInt(str[6]));
					// }
				}
			}
		}

		skin.setNote(note);
		skin.setMinenote(mine);
		skin.setLongnote(new Sprite[][] { lnend, lnstart, lnbodya, lnbody });
		skin.setLaneregion(laner);
		skin.setSkinPart(partlist.toArray(new SkinPart[0]));
		return skin;

	}
}
