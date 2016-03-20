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

public abstract class LR2SkinLoader {

	private List<CommandWord> commands = new ArrayList();

	List<Texture> imagelist = new ArrayList();

	protected void addCommandWord(CommandWord cm) {
		commands.add(cm);
	}

	protected void loadSkin(Skin skin, File f) throws IOException {
		float srcw = 640;
		float srch = 480;
		float dstw = 1280;
		float dsth = 720;

		List<SkinObject> partlist = new ArrayList();
		SkinObject part = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "MS932"));
		String line = null;
		int[] option = new int[0];

		while ((line = br.readLine()) != null) {
			if (!line.startsWith("//")) {
				String[] str = line.split(",", -1);
				if (str.length > 0) {
					if (str[0].equals("#IF")) {
						List<Integer> l = new ArrayList();
						for (int i = 1; i < str.length; i++) {
							try {
								l.add(Integer.parseInt(str[1]));
							} catch (NumberFormatException e) {
								break;
							}
						}

						option = new int[l.size()];
						for (int i = 0; i < l.size(); i++) {
							option[i] = l.get(i);
						}
					}
					if (str[0].equals("#ELSEIF")) {

					}
					if (str[0].equals("#ELSE")) {

					}
					if (str[0].equals("#ENDIF")) {
						option = new int[0];
					}
					if (str[0].equals("#IMAGE")) {
						String imagepath = str[1].replace("LR2files\\Theme", "skin").replace("\\", "/");
						File imagefile = new File(imagepath);
						if (imagepath.contains("*")) {
							String ext = imagepath.substring(imagepath.lastIndexOf("*") + 1);
							File imagedir = new File(imagepath.substring(0, imagepath.lastIndexOf('/')));
							if (imagedir.exists() && imagedir.isDirectory()) {
								for (File subfile : imagedir.listFiles()) {
									if (subfile.getPath().toLowerCase().endsWith(ext)) {
										imagefile = subfile;
										break;
									}
								}
							}
						}
						if (imagefile.exists()) {
							try {
								imagelist.add(new Texture(Gdx.files.internal(imagefile.getPath())));
							} catch (GdxRuntimeException e) {
								imagelist.add(null);
								e.printStackTrace();
							}
						} else {
							imagelist.add(null);
						}
						System.out.println("Image Loaded - " + (imagelist.size() - 1) + " : " + imagefile.getPath());
					}

					if (str[0].equals("#SRC_IMAGE")) {
						int gr = Integer.parseInt(str[2]);
						if (gr < imagelist.size() && imagelist.get(gr) != null) {
							try {
								part = new SkinObject();
								part.setImage(new TextureRegion(imagelist.get(gr), Integer.parseInt(str[3]), Integer
										.parseInt(str[4]), Integer.parseInt(str[5]), Integer.parseInt(str[6])));
								part.setTiming(Integer.parseInt(str[10]));
								part.setOption(option);
								partlist.add(part);								
							} catch(NumberFormatException e) {
								e.printStackTrace();
							}
						}
					}
					if (str[0].equals("#DST_IMAGE")) {
						if (part != null) {
							try {
								part.setDestination(Integer.parseInt(str[2]), Integer.parseInt(str[3]) * dstw / srcw,
										Integer.parseInt(str[4]) * dstw / srcw, Integer.parseInt(str[5]) * dstw / srcw,
										Integer.parseInt(str[6]) * dstw / srcw, Integer.parseInt(str[7]),
										Integer.parseInt(str[8]), Integer.parseInt(str[9]), Integer.parseInt(str[10]),
										Integer.parseInt(str[11]), Integer.parseInt(str[12]),
										Integer.parseInt(str[13]), Integer.parseInt(str[14]),
										Integer.parseInt(str[15]), Integer.parseInt(str[16]),
										Integer.parseInt(str[17]), Integer.parseInt(str[18]),
										Integer.parseInt(str[19]), Integer.parseInt(str[20]));
							} catch (NumberFormatException e) {
								e.printStackTrace();
							}
							part = null;
						}
					}

					for (CommandWord cm : commands) {
						if (str[0].equals("#" + cm.str)) {
							cm.execute(str);
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

		skin.setSkinPart(partlist.toArray(new SkinObject[0]));

	}

	public abstract class CommandWord {

		public final String str;

		public CommandWord(String str) {
			this.str = str;
		}

		public abstract void execute(String[] values);
	}
}
