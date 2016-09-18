package bms.player.beatoraja.skin;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.result.MusicResultSkin;
import bms.player.beatoraja.skin.LR2SkinHeader.CustomFile;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.GdxRuntimeException;

public abstract class LR2SkinLoader {

	private List<CommandWord> commands = new ArrayList<CommandWord>();

	List<Texture> imagelist = new ArrayList<Texture>();

	public final float srcw;
	public final float srch;
	public final float dstw;
	public final float dsth;

	public LR2SkinLoader(float srcw, float srch, float dstw, float dsth) {
		this.srcw = srcw;
		this.srch = srch;
		this.dstw = dstw;
		this.dsth = dsth;
	}

	protected void addCommandWord(CommandWord cm) {
		commands.add(cm);
	}

	protected void loadSkin(Skin skin, File f, MainState state) throws IOException {
		this.loadSkin(skin, f, state, new int[0]);
	}

	protected void loadSkin(Skin skin, File f, MainState state, int[] option) throws IOException {
		this.loadSkin0(skin, f, state, option, new HashMap());
	}

	protected void loadSkin(Skin skin, File f, MainState state, LR2SkinHeader header, int[] option,
			Map<String, Object> property) throws IOException {
		List<Integer> op = new ArrayList();
		Map<String, String> filemap = new HashMap();
		for (String key : property.keySet()) {
			if (property.get(key) != null) {
				if (property.get(key) instanceof Integer) {
					op.add((Integer) property.get(key));
				}
				if (property.get(key) instanceof String) {
					for (CustomFile file : header.getCustomFiles()) {
						if (file.name.equals(key)) {
							filemap.put(file.path, (String) property.get(key));
							break;
						}
					}
				}
			}
		}
		for (int i : option) {
			op.add(i);
		}
		option = new int[op.size()];
		for (int i = 0; i < op.size(); i++) {
			option[i] = op.get(i);
		}
		this.loadSkin0(skin, f, state, option, filemap);
	}

	protected void loadSkin0(Skin skin, File f, MainState state, int[] option, Map<String, String> filemap)
			throws IOException {
		List<Integer> op = new ArrayList<Integer>();
		for (int i : option) {
			op.add(i);
		}

		SkinImage part = null;
		SkinImage button = null;
		SkinGraph bar = null;
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
						ifs =true;
						for (int i = 1; i < str.length; i++) {
							boolean b = false;
							try {
								int opt = Integer.parseInt(str[i]);
								for (Integer o : op) {
									if (o == opt) {
										b = true;
										break;
									}
								}
								if (!b) {
									b = state.getBooleanValue(opt);
								}
								if(!b) {
									ifs = false;
									break;
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
								boolean b = false;
								try {
									int opt = Integer.parseInt(str[i]);
									for (Integer o : op) {
										if (o == opt) {
											b = true;
											break;
										}
									}
									if (!b) {
										b = state.getBooleanValue(opt);
									}
									if(!b) {
										ifs = false;
										break;
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

						if (str[0].equals("#SETOPTION")) {
							if (Integer.parseInt(str[2]) >= 1) {
								op.add(Integer.parseInt(str[1]));
							}
						}
						if (str[0].equals("#STARTINPUT")) {
							skin.setInputTime(Integer.parseInt(str[1]));
							if (skin instanceof MusicResultSkin) {
								MusicResultSkin resultskin = (MusicResultSkin) skin;
								try {
									resultskin.setRankTime(Integer.parseInt(str[2]));
								} catch (NumberFormatException e) {

								}
							}
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
							if (filemap.get(imagepath) != null) {
								imagefile = new File(imagepath.substring(0, imagepath.lastIndexOf('/') + 1)
										+ filemap.get(imagepath));
							} else if (imagepath.contains("*")) {
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
							// System.out
							// .println("Image Loaded - " + (imagelist.size() -
							// 1) + " : " + imagefile.getPath());
						}

						if (str[0].equals("#SRC_SLIDER")) {
							slider = null;
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
									slider = new SkinSlider(images, values[9], values[11],
											(int) (values[12] * (values[11] == 1 || values[11] == 3 ? (dstw / srcw)
													: (dsth / srch))), values[13]);
									slider.setTiming(values[10]);
									slider.setChangable(values[14] == 0);
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
							try {
								part = null;
								int gr = Integer.parseInt(str[2]);
								if (gr >= 100) {
									part = new SkinImage(gr);
									// System.out.println("add reference image : "
									// + gr);
								} else if (gr < imagelist.size() && imagelist.get(gr) != null) {
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
								}
								if (part != null) {
									skin.add(part);
								} else {
									System.out.println("NO_DESTINATION : " + line);
								}
							} catch (NumberFormatException e) {
								e.printStackTrace();
							}
						}
						if (str[0].equals("#DST_IMAGE")) {
							if (part != null) {
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
							num = null;
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
									TextureRegion[] images = new TextureRegion[divy * divx];
									for (int i = 0; i < divx; i++) {
										for (int j = 0; j < divy; j++) {
											images[j * divx + i] = new TextureRegion(imagelist.get(gr), x + w / divx
													* i, y + h / divy * j, w / divx, h / divy);
										}
									}
									if (images.length >= 24) {
										TextureRegion[] pn = new TextureRegion[12];
										TextureRegion[] mn = new TextureRegion[12];
										for (int i = 0; i < 12; i++) {
											pn[i] = images[i];
											mn[i] = images[i + 12];
										}
										num = new SkinNumber(pn, mn, values[9], values[13] + 1, 0, values[11]);
										num.setAlign(values[12]);
									} else {
										num = new SkinNumber(images, values[9], values[13], images.length > 10 ? 2 : 0,
												values[11]);
										num.setAlign(values[12]);
									}
									num.setTiming(values[10]);
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
							text = null;
							int gr = Integer.parseInt(str[2]);
							try {
								text = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 40, 2);
								int[] values = parseInt(str);
								text.setReferenceID(values[3]);
								text.setAlign(values[4]);
								int edit = values[5];
								int panel = values[6];
								skin.add(text);
								// System.out.println("Text Added - " +
								// (values[3]));
							} catch (NumberFormatException e) {
								e.printStackTrace();
							}
						}
						if (str[0].equals("#DST_TEXT")) {
							if (text != null) {
								try {
									int[] values = parseInt(str);
									text.setDestination(values[2], values[3] * dstw / srcw, dsth - values[4] * dsth
											/ srch, values[5] * dstw / srcw, values[6] * dsth / srch, values[7],
											values[8], values[9], values[10], values[11], values[12], values[13],
											values[14], values[15], values[16], values[17], values[18], values[19],
											values[20]);
								} catch (NumberFormatException e) {
									e.printStackTrace();
								}
							}
						}
						if (str[0].equals("#SRC_BARGRAPH")) {
							bar = null;
							try {
								int[] values = parseInt(str);
								int gr = values[2];
								if (gr >= 100) {
									bar = new SkinGraph(gr);
									bar.setTiming(values[10]);
									bar.setReferenceID(values[11] + 1000);
									bar.setDirection(values[12]);
								} else if (gr < imagelist.size() && imagelist.get(gr) != null) {
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
									bar = new SkinGraph(images, values[9]);
									bar.setTiming(values[10]);
									bar.setReferenceID(values[11] + 1000);
									bar.setDirection(values[12]);
									// System.out.println("Object Added - " +
									// (part.getTiming()));
								}
							} catch (NumberFormatException e) {
								e.printStackTrace();
							}
							if (bar != null) {
								skin.add(bar);
							}

						}
						if (str[0].equals("#DST_BARGRAPH")) {
							if (bar != null) {
								try {
									int[] values = parseInt(str);
									if (bar.getDirection() == 1) {
										values[4] += values[6];
										values[6] = -values[6];
									}
									bar.setDestination(values[2], values[3] * dstw / srcw, dsth
											- (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw, values[6]
											* dsth / srch, values[7], values[8], values[9], values[10], values[11],
											values[12], values[13], values[14], values[15], values[16], values[17],
											values[18], values[19], values[20]);
								} catch (NumberFormatException e) {
									e.printStackTrace();
								}
							}

						}
						if (str[0].equals("#SRC_BUTTON")) {
							button = null;
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
											images[divx * j + i] = new TextureRegion[] { new TextureRegion(
													imagelist.get(gr), x + w / divx * i, y + h / divy * j, w / divx, h
															/ divy) };
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
				}
			}
		}
		br.close();

		int[] soption = new int[op.size()];
		for (int i = 0; i < op.size(); i++) {
			soption[i] = op.get(i);
		}
		skin.setOption(soption);

		for (SkinObject obj : skin.getAllSkinObjects()) {
			if (obj instanceof SkinImage && obj.getAllDestination().length == 0) {
				skin.removeSkinObject(obj);
				System.out.println("NO_DESTINATION : " + obj);
			}
		}
	}

	protected int[] parseInt(String[] s) {
		int[] result = new int[21];
		for (int i = 1; i < s.length; i++) {
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
