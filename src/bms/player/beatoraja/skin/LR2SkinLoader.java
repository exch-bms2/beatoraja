package bms.player.beatoraja.skin;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import bms.player.beatoraja.MainState;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.GdxRuntimeException;

public abstract class LR2SkinLoader {

	private List<CommandWord> commands = new ArrayList();

	List<Texture> imagelist = new ArrayList();

	protected void addCommandWord(CommandWord cm) {
		commands.add(cm);
	}

	protected void loadSkin(Skin skin, File f) throws IOException {
		this.loadSkin(skin, f, new int[0]);
	}

	protected void loadSkin(Skin skin, File f, int[] option) throws IOException {
		float srcw = 640;
		float srch = 480;
		float dstw = 1280;
		float dsth = 720;

		SkinImage part = null;
		SkinImage button = null;
		SkinSlider slider = null;
		SkinNumber num = null;
		SkinText text = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "MS932"));
		String line = null;

		boolean skip = false;
		boolean ifs = false;

		while ((line = br.readLine()) != null) {
			if (!line.startsWith("//")) {
				String[] str = line.split(",", -1);
				if (str.length > 0) {
					if (str[0].equals("#IF")) {
						for (int i = 1; i < str.length; i++) {
							try {
								int opt = Integer.parseInt(str[i]);
								for (int j = 0; j < option.length; j++) {
									if (option[j] == opt) {
										ifs = true;
									}
								}
							} catch (NumberFormatException e) {
								break;
							}
						}

						skip = !ifs;
					}
					if (str[0].equals("#ELSEIF")) {
						if (ifs) {
							skip = true;
						} else {
							for (int i = 1; i < str.length; i++) {
								try {
									int opt = Integer.parseInt(str[i]);
									for (int j = 0; j < option.length; j++) {
										if (option[j] == opt) {
											ifs = true;
										}
									}
								} catch (NumberFormatException e) {
									break;
								}
							}

							skip = !ifs;
						}
					}
					if (str[0].equals("#ELSE")) {
						skip = ifs;
					}
					if (str[0].equals("#ENDIF")) {
						skip = false;
						ifs = false;
					}
					if (!skip) {

						if (str[0].equals("#STARTINPUT")) {
							skin.setInputTime(Integer.parseInt(str[1]));
						}
						if (str[0].equals("#SCENETIME")) {
							skin.setSceneTime(Integer.parseInt(str[1]));
						}
						if (str[0].equals("#FADEOUT")) {
							skin.setFadeoutTime(Integer.parseInt(str[1]));
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
							System.out
									.println("Image Loaded - " + (imagelist.size() - 1) + " : " + imagefile.getPath());
						}

						if (str[0].equals("#SRC_SLIDER")) {
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
									slider = new SkinSlider(images, values[9], values[11], values[12], values[13]);
									slider.setTiming(values[10]);
									skin.add(slider);
									// System.out.println("Object Added - " +
									// (part.getTiming()));
								} catch (NumberFormatException e) {
									e.printStackTrace();
								}
							}
						}
						if (str[0].equals("#DST_SLIDER")) {
							if (slider != null) {
								try {
									int[] values = parseInt(str);
									slider.setDestination(values[2], values[3] * dstw / srcw, dsth
											- (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6]
											* dsth / srch, values[7], values[8], values[9], values[10], values[11],
											values[12], values[13], values[14], values[15], values[16], values[17],
											values[18], values[19], values[20]);
								} catch (NumberFormatException e) {
									e.printStackTrace();
								}
							}
						}

						if (str[0].equals("#SRC_IMAGE")) {
							int gr = Integer.parseInt(str[2]);
							if(gr == 100) {
								part = new SkinImage(MainState.IMAGE_STAGEFILE);
							} else if(gr == 101) {
								part = new SkinImage(MainState.IMAGE_BACKBMP);
							} else if(gr == 102) {
								part = new SkinImage(MainState.IMAGE_BANNER);								
							} else 
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
									part = new SkinImage(images, values[9]);
									part.setTiming(values[10]);
									// System.out.println("Object Added - " +
									// (part.getTiming()));
								} catch (NumberFormatException e) {
									e.printStackTrace();
								}
							}
							if(part != null) {
								skin.add(part);
							}
						}
						if (str[0].equals("#DST_IMAGE")) {
							if (part != null) {
								try {
									int[] values = parseInt(str);
									part.setDestination(values[2], values[3] * dstw / srcw, dsth
											- (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6]
											* dsth / srch, values[7], values[8], values[9], values[10], values[11],
											values[12], values[13], values[14], values[15], values[16], values[17],
											values[18], values[19], values[20]);
								} catch (NumberFormatException e) {
									e.printStackTrace();
								}
							}
						}

						if (str[0].equals("#SRC_NUMBER")) {
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
											images[j][i] = new TextureRegion(imagelist.get(gr), x + w / divx
													* i, y + h / divy * j, w / divx, h / divy);
										}
									}
									if(images.length > 1) {
										num = new SkinNumber(images[0], images[1], values[9], values[13], 0, values[11]);
									} else {
										num = new SkinNumber(images[0], values[9], values[13], images[0].length > 10 ? 2 : 0, values[11]);
									}

									skin.add(num);
									// System.out.println("Number Added - " +
									// (num.getId()));
								} catch (NumberFormatException e) {
									e.printStackTrace();
								}
							}
						}
						if (str[0].equals("#DST_NUMBER")) {
							if (num != null) {
								try {
									int[] values = parseInt(str);
									num.setDestination(values[2], values[3] * dstw / srcw, dsth
											- (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6]
											* dsth / srch, values[7], values[8], values[9], values[10], values[11],
											values[12], values[13], values[14], values[15], values[16], values[17],
											values[18], values[19], values[20]);
								} catch (NumberFormatException e) {
									e.printStackTrace();
								}
							}
						}

						if (str[0].equals("#SRC_TEXT")) {
							int gr = Integer.parseInt(str[2]);
							try {
								text = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 40, 2);
								int[] values = parseInt(str);
								text.setReferenceID(values[3]);
								text.setAlign(values[4]);
								int edit = values[5];
								int panel = values[6];
								skin.add(text);
								System.out.println("Text Added - " + (values[3]));
							} catch (NumberFormatException e) {
								e.printStackTrace();
							}
						}
						if (str[0].equals("#DST_TEXT")) {
							if (text != null) {
								try {
									int[] values = parseInt(str);
									text.setDestination(values[2], values[3] * dstw / srcw, dsth
											- values[4] * dsth / srch, values[5] * dstw / srcw, values[6]
											* dsth / srch, values[7], values[8], values[9], values[10], values[11],
											values[12], values[13], values[14], values[15], values[16], values[17],
											values[18], values[19], values[20]);
								} catch (NumberFormatException e) {
									e.printStackTrace();
								}
							}
						}
						if (str[0].equals("#SRC_BARGRAPH")) {

						}
						if (str[0].equals("#DST_BARGRAPH")) {

						}
						if (str[0].equals("#SRC_BUTTON")) {
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
									TextureRegion[][] images = new TextureRegion[divx * divy][];
									for (int i = 0; i < divx; i++) {
										for (int j = 0; j < divy; j++) {
											images[divx * j + i] = new TextureRegion[]{new TextureRegion(imagelist.get(gr), x + w / divx
													* i, y + h / divy * j, w / divx, h / divy)};
										}
									}
									button = new SkinImage(images, values[9]);
									button.setTiming(values[10]);
									button.setReferenceID(values[11] + 1000);
									skin.add(button);
									// System.out.println("Object Added - " +
									// (part.getTiming()));
								} catch (NumberFormatException e) {
									e.printStackTrace();
								}
							}

						}
						if (str[0].equals("#DST_BUTTON")) {
							if (button != null) {
								try {
									int[] values = parseInt(str);
									button.setDestination(values[2], values[3] * dstw / srcw, dsth
											- (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6]
											* dsth / srch, values[7], values[8], values[9], values[10], values[11],
											values[12], values[13], values[14], values[15], values[16], values[17],
											values[18], values[19], values[20]);
								} catch (NumberFormatException e) {
									e.printStackTrace();
								}
							}
						}
						if (str[0].equals("#SRC_ONMOUSE")) {

						}
						if (str[0].equals("#DST_ONMOUSE")) {

						}

						for (CommandWord cm : commands) {
							if (str[0].equals("#" + cm.str)) {
								cm.execute(str);
							}
						}
					} else {
						// System.out.println("line skip : " + line);
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
		br.close();
	}

	private int[] parseInt(String[] s) {
		int[] result = new int[21];
		for (int i = 2; i < s.length; i++) {
			try {
				result[i] = Integer.parseInt(s[i]);
			} catch (Exception e) {

			}
		}
		return result;
	}

	public abstract class CommandWord {

		public final String str;

		public CommandWord(String str) {
			this.str = str;
		}

		public abstract void execute(String[] values);
	}
}
