package bms.player.beatoraja.skin.lr2;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import static bms.player.beatoraja.skin.SkinProperty.*;

import bms.player.beatoraja.*;
import bms.player.beatoraja.SkinConfig.Offset;
import bms.player.beatoraja.play.bga.BGAProcessor;
import bms.player.beatoraja.select.MusicSelectSkin;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.SkinHeader.*;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * LR2のスキン定義用csvファイルのローダー
 * 
 * @author exch
 */
public abstract class LR2SkinCSVLoader<S extends Skin> extends LR2SkinLoader {

	List<Object> imagelist = new ArrayList<Object>();
	List<SkinTextImage.SkinTextImageSource> fontlist = new ArrayList<>();

	/**
	 * スキンの元サイズ
	 */
	public final Resolution src;
	/**
	 * 描画サイズ
	 */
	public final Resolution dst;
	private boolean usecim;

	protected S skin;

	private MainState state;

	public LR2SkinCSVLoader(Resolution src, Config c) {
		this.src = src;
		this.dst = c.getResolution();
		usecim = c.isCacheSkinImage();

		final float srcw = src.width;
		final float srch = src.height;
		final float dstw = dst.width;
		final float dsth = dst.height;

		addCommandWord(new CommandWord("STARTINPUT") {
			@Override
			public void execute(String[] str) {
				skin.setInput(Integer.parseInt(str[1]));
			}
		});
		addCommandWord(new CommandWord("SCENETIME") {
			@Override
			public void execute(String[] str) {
				skin.setScene(Integer.parseInt(str[1]));
			}
		});
		addCommandWord(new CommandWord("FADEOUT") {
			@Override
			public void execute(String[] str) {
				skin.setFadeout(Integer.parseInt(str[1]));
			}
		});

		addCommandWord(new CommandWord("INCLUDE") {
			@Override
			public void execute(String[] str) {
				final File imagefile = SkinLoader.getPath(str[1].replace("LR2files\\Theme", "skin").replace("\\", "/"), filemap);
				if (imagefile.exists()) {
					try (BufferedReader br = new BufferedReader(
							new InputStreamReader(new FileInputStream(imagefile), "MS932"));) {
						while ((line = br.readLine()) != null) {
							processLine(line, state);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});

		addCommandWord(new CommandWord("IMAGE") {
			@Override
			public void execute(String[] str) {
				final File imagefile = SkinLoader.getPath(str[1].replace("LR2files\\Theme", "skin").replace("\\", "/"), filemap);
				if (imagefile.exists()) {
					boolean isMovie = false;
					for (String mov : BGAProcessor.mov_extension) {
						if (imagefile.getName().toLowerCase().endsWith(mov)) {
							try {
								SkinSourceMovie mm = new SkinSourceMovie(imagefile.getPath());
								imagelist.add(mm);
								isMovie = true;
								break;
							} catch (Throwable e) {
								Logger.getGlobal().warning("BGAファイル読み込み失敗。" + e.getMessage());
								e.printStackTrace();
							}
						}
					}

					if (!isMovie) {
						imagelist.add(getTexture(imagefile.getPath(), usecim));
					}
				} else {
					Logger.getGlobal()
							.warning("IMAGE " + imagelist.size() + " : ファイルが見つかりません : " + imagefile.getPath());
					imagelist.add(null);
				}
				// System.out
				// .println("Image Loaded - " + (imagelist.size() -
				// 1) + " : " + imagefile.getPath());
			}
		});

		addCommandWord(new CommandWord("LR2FONT") {
			@Override
			public void execute(String[] str) {
				final File imagefile = SkinLoader.getPath(str[1].replace("LR2files\\Theme", "skin").replace("\\", "/"), filemap);
				if (imagefile.exists()) {
					LR2FontLoader font = new LR2FontLoader(usecim);
					try {
						SkinTextImage.SkinTextImageSource source = font.loadFont(imagefile.toPath());
						fontlist.add(source);
					} catch (IOException e) {
						e.printStackTrace();
						fontlist.add(null);
					}

				} else {
					Logger.getGlobal()
							.warning("IMAGE " + imagelist.size() + " : ファイルが見つかりません : " + imagefile.getPath());
					fontlist.add(null);
				}
				// System.out
				// .println("Image Loaded - " + (imagelist.size() -
				// 1) + " : " + imagefile.getPath());
			}
		});

		addCommandWord(new CommandWord("SRC_IMAGE") {
			@Override
			public void execute(String[] str) {
				part = null;
				int gr = Integer.parseInt(str[2]);
				if (gr >= 100) {
					part = new SkinImage(gr);
					// System.out.println("add reference image : "
					// + gr);
				} else {
					int[] values = parseInt(str);
					if (values[2] < imagelist.size() && imagelist.get(values[2]) != null
							&& imagelist.get(values[2]) instanceof SkinSourceMovie) {
						part = new SkinImage((SkinSourceMovie) imagelist.get(values[2]));
					} else {
						TextureRegion[] images = getSourceImage(values);
						if (images != null) {
							part = new SkinImage(images, values[10], values[9]);
							// System.out.println("Object Added - " +
							// (part.getTiming()));
						}
					}

				}
				if (part != null) {
					skin.add(part);
				}
			}
		});

		addCommandWord(new CommandWord("IMAGESET") {
			@Override
			public void execute(String[] str) {
				int gr = Integer.parseInt(str[2]);
				if (gr < imagelist.size() && imagelist.get(gr) != null) {
					int[] values = parseInt(str);
					imagesetarray.add(getSourceImage(values));
				}
			}
		});

		addCommandWord(new CommandWord("SRC_IMAGESET") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				TextureRegion[][] tr = new TextureRegion[values[4]][];
				for (int i = 0; i < values[4]; i++) {
					tr[i] = (TextureRegion[]) imagesetarray.get(values[5+i]);
				}
				part = new SkinImage(tr, values[2], values[1]);
				part.setReferenceID(values[3]);
				if (part != null) {
					skin.add(part);
				}
			}
		});

		addCommandWord(new CommandWord("DST_IMAGE") {
			@Override
			public void execute(String[] str) {
				if (part != null) {
					int[] values = parseInt(str);
					if (values[5] < 0) {
						values[3] += values[5];
						values[5] = -values[5];
					}
					if (values[6] < 0) {
						values[4] += values[6];
						values[6] = -values[6];
					}
					part.setDestination(values[2], values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
							values[12], values[13], values[14], values[15], values[16], values[17], values[18],
							values[19], values[20], values[21]);
				}
			}
		});

		addCommandWord(new CommandWord("SRC_NUMBER") {
			@Override
			public void execute(String[] str) {
				num = null;
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

							num = new SkinNumber(pn, mn, values[10], values[9], values[13] + 1, 0, values[11]);
							num.setAlign(values[12]);
						} else {
							int d = images.length % 10 == 0 ? 10 : 11;

							TextureRegion[][] nimages = new TextureRegion[divx * divy / d][d];
							for (int i = 0; i < d; i++) {
								for (int j = 0; j < divx * divy / d; j++) {
									nimages[j][i] = images[j * d + i];
								}
							}

							num = new SkinNumber(nimages, values[10], values[9], values[13], d > 10 ? 2 : 0,
									values[11]);
							num.setAlign(values[12]);
						}
						skin.add(num);
						// System.out.println("Number Added - " +
						// (num.getId()));
					}
				}
			}
		});
		addCommandWord(new CommandWord("DST_NUMBER") {
			@Override
			public void execute(String[] str) {
				if (num != null) {
					int[] values = parseInt(str);
					num.setDestination(values[2], values[3] * dstw / srcw, dsth - (values[4] + values[6]) * dsth / srch,
							values[5] * dstw / srcw, values[6] * dsth / srch, values[7], values[8], values[9],
							values[10], values[11], values[12], values[13], values[14], values[15], values[16],
							values[17], values[18], values[19], values[20], values[21]);
				}
			}
		});

		addCommandWord(new CommandWord("SRC_TEXT") {
			@Override
			public void execute(String[] str) {
				text = null;
				int[] values = parseInt(str);
				if (values[2] < fontlist.size() && fontlist.get(values[2]) != null) {
					text = new SkinTextImage(fontlist.get(values[2]));
				} else {
					text = new SkinTextFont("skin/default/VL-Gothic-Regular.ttf", 0, 48, 2);
				}
				text.setReferenceID(values[3]);
				text.setAlign(values[4]);
				text.setEditable(values[5] != 0);
				int panel = values[6];
				skin.add(text);
				// System.out.println("Text Added - " +
				// (values[3]));
			}
		});
		addCommandWord(new CommandWord("DST_TEXT") {
			@Override
			public void execute(String[] str) {
				if (text != null) {
					int[] values = parseInt(str);
					text.setDestination(values[2], values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
							values[12], values[13], values[14], values[15], values[16], values[17], values[18],
							values[19], values[20], values[21]);
					if(text.isEditable() && text.getReferenceID() == SkinProperty.STRING_SEARCHWORD && skin instanceof MusicSelectSkin) {
						Rectangle r = new Rectangle(values[3] * dstw / srcw,
								dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
								values[6] * dsth / srch);
						((MusicSelectSkin) skin).setSearchTextRegion(r);
					}
				}
			}
		});

		addCommandWord(new CommandWord("SRC_SLIDER") {
			@Override
			public void execute(String[] str) {
				slider = null;
				int[] values = parseInt(str);
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					slider = new SkinSlider(images, values[10], values[9], values[11],
							(int) (values[12] * (values[11] == 1 || values[11] == 3 ? (dstw / srcw) : (dsth / srch))),
							values[13]);
					slider.setChangable(values[14] == 0);
					skin.add(slider);
					// System.out.println("Object Added - " +
					// (part.getTiming()));
				}
			}
		});
		addCommandWord(new CommandWord("DST_SLIDER") {
			@Override
			public void execute(String[] str) {
				if (slider != null) {
					int[] values = parseInt(str);
					slider.setDestination(values[2], values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
							values[12], values[13], values[14], values[15], values[16], values[17], values[18],
							values[19], values[20], values[21]);
				}
			}
		});

		addCommandWord(new CommandWord("SRC_BARGRAPH") {
			@Override
			public void execute(String[] str) {
				bar = null;
				int[] values = parseInt(str);
				int gr = values[2];
				if (gr >= 100) {
					bar = new SkinGraph(gr);
					bar.setReferenceID(values[11] + 100);
					bar.setDirection(values[12]);
				} else {
					TextureRegion[] images = getSourceImage(values);
					if (images != null) {
						bar = new SkinGraph(images, values[10], values[9]);
						bar.setReferenceID(values[11] + 100);
						bar.setDirection(values[12]);
						// System.out.println("Object Added - " +
						// (part.getTiming()));
					}
				}
				if (bar != null) {
					skin.add(bar);
				}
			}
		});
		addCommandWord(new CommandWord("DST_BARGRAPH") {
			@Override
			public void execute(String[] str) {
				if (bar != null) {
					int[] values = parseInt(str);
					if (bar.getDirection() == 1) {
						values[4] += values[6];
						values[6] = -values[6];
					}
					bar.setDestination(values[2], values[3] * dstw / srcw, dsth - (values[4] + values[6]) * dsth / srch,
							values[5] * dstw / srcw, values[6] * dsth / srch, values[7], values[8], values[9],
							values[10], values[11], values[12], values[13], values[14], values[15], values[16],
							values[17], values[18], values[19], values[20], values[21]);
				}
			}
		});
		addCommandWord(new CommandWord("SRC_BUTTON") {
			@Override
			public void execute(String[] str) {
				button = null;
				int gr = Integer.parseInt(str[2]);
				if (gr < imagelist.size() && imagelist.get(gr) != null) {
					int[] values = parseInt(str);
					int x = values[3];
					int y = values[4];
					int w = values[5];
					if (w == -1) {
						w = ((Texture) imagelist.get(gr)).getWidth();
					}
					int h = values[6];
					if (h == -1) {
						h = ((Texture) imagelist.get(gr)).getHeight();
					}
					int divx = values[7];
					if (divx <= 0) {
						divx = 1;
					}
					int divy = values[8];
					if (divy <= 0) {
						divy = 1;
					}
					TextureRegion[][] tr;
					int length = values[15];
					if (length <= 0) {
						tr = new TextureRegion[divx * divy][];
						for (int i = 0; i < divx; i++) {
							for (int j = 0; j < divy; j++) {
								tr[divx * j + i] = new TextureRegion[] { new TextureRegion((Texture) imagelist.get(gr),
										x + w / divx * i, y + h / divy * j, w / divx, h / divy) };
							}
						}
					}else {
						tr = new TextureRegion[length][];
						TextureRegion[] srcimg = getSourceImage(values);
						for (int i = 0; i < tr.length; i++) {
							tr[i] = new TextureRegion[srcimg.length / length];
							for (int j = 0; j < tr[i].length; j++) {
								tr[i][j] = srcimg[i * tr[i].length + j];
							}
						}
					}
					button = new SkinImage(tr, values[10], values[9]);
					button.setReferenceID(values[11]);
					if (values[12] == 1) {
						button.setClickevent(values[11]);
					}
					skin.add(button);
					// System.out.println("Object Added - " +
					// (part.getTiming()));
				}
			}
		});
		addCommandWord(new CommandWord("DST_BUTTON") {
			@Override
			public void execute(String[] str) {
				if (button != null) {
					int[] values = parseInt(str);
					button.setDestination(values[2], values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
							values[12], values[13], values[14], values[15], values[16], values[17], values[18],
							values[19], values[20], values[21]);
				}
			}
		});
		addCommandWord(new CommandWord("DST_PM_CHARA_1P") {
			@Override
			public void execute(String[] str) {
				//プレイ用 判定連動
				//x,y,w,h,color,offset,folderpath
				int[] values = parseInt(str);
				if (values[3] < 0) {
					values[1] += values[3];
					values[3] = -values[3];
				}
				if (values[4] < 0) {
					values[2] += values[4];
					values[4] = -values[4];
				}
				final File imagefile = SkinLoader.getPath(str[7].replace("LR2files\\Theme", "skin").replace("\\", "/"), filemap);
				if (imagefile.exists()) {
					PMcharaLoader(imagefile, 0, (values[5] == 1 || values[5] == 2) ? values[5] : 1,
							values[1] * dstw / srcw, dsth - (values[2] + values[4]) * dsth / srch, values[3] * dstw / srcw, values[4] * dsth / srch,
							1, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, values[6]);
				}
			}
		});
		addCommandWord(new CommandWord("DST_PM_CHARA_2P") {
			@Override
			public void execute(String[] str) {
				//プレイ用 判定連動
				//x,y,w,h,color,offset,folderpath
				int[] values = parseInt(str);
				if (values[3] < 0) {
					values[1] += values[3];
					values[3] = -values[3];
				}
				if (values[4] < 0) {
					values[2] += values[4];
					values[4] = -values[4];
				}
				final File imagefile = SkinLoader.getPath(str[7].replace("LR2files\\Theme", "skin").replace("\\", "/"), filemap);
				if (imagefile.exists()) {
					PMcharaLoader(imagefile, 0, (values[5] == 1 || values[5] == 2) ? values[5] : 1,
							values[1] * dstw / srcw, dsth - (values[2] + values[4]) * dsth / srch, values[3] * dstw / srcw, values[4] * dsth / srch,
							2, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, values[6]);
				}
			}
		});
		addCommandWord(new CommandWord("DST_PM_CHARA_ANIMATION") {
			@Override
			public void execute(String[] str) {
				//プレイ以外用 判定非連動
				//x,y,w,h,color,animationtype,timer,op1,op2,op3,offset,folderpath
				//type 0:NEUTRAL 1:FEVER 2:GREAT 3:GOOD 4:BAD 5:FEVERWIN 6:WIN 7:LOSE 8:OJAMA 9:DANCE
				int[] values = parseInt(str);
				if(values[6] >= 0 && values[6] <= 9) {
					if (values[3] < 0) {
						values[1] += values[3];
						values[3] = -values[3];
					}
					if (values[4] < 0) {
						values[2] += values[4];
						values[4] = -values[4];
					}
					final File imagefile = SkinLoader.getPath(str[12].replace("LR2files\\Theme", "skin").replace("\\", "/"), filemap);
					if (imagefile.exists()) {
						PMcharaLoader(imagefile, values[6] + 6, (values[5] == 1 || values[5] == 2) ? values[5] : 1,
								values[1] * dstw / srcw, dsth - (values[2] + values[4]) * dsth / srch, values[3] * dstw / srcw, values[4] * dsth / srch,
								Integer.MIN_VALUE, values[7], values[8], values[9], values[10], values[11]);
					}
				}
			}
		});
		addCommandWord(new CommandWord("SRC_PM_CHARA_IMAGE") {
			@Override
			public void execute(String[] str) {
				//color,type,folderpath
				//type 0:キャラ背景 1:名前画像 2:ハリアイ画像(上半身のみ) 3:ハリアイ画像(全体) 4:キャラアイコン
				PMcharaPart = null;
				int[] values = parseInt(str);
				if(values[2] >= 0 && values[2] <= 4) {
					final File imagefile = SkinLoader.getPath(str[3].replace("LR2files\\Theme", "skin").replace("\\", "/"), filemap);
					if (imagefile.exists()) {
						PMcharaLoader(imagefile, values[2] + 1, (values[1] == 1 || values[1] == 2) ? values[1] : 1,
								Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE,
								Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
					}
				}
			}
		});
		addCommandWord(new CommandWord("DST_PM_CHARA_IMAGE") {
			@Override
			public void execute(String[] str) {
				//DST_IMAGEと同様
				if (PMcharaPart != null) {
					int[] values = parseInt(str);
					if (values[5] < 0) {
						values[3] += values[5];
						values[5] = -values[5];
					}
					if (values[6] < 0) {
						values[4] += values[6];
						values[6] = -values[6];
					}
					PMcharaPart.setDestination(values[2], values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
							values[12], values[13], values[14], values[15], values[16], values[17], values[18],
							values[19], values[20], values[21]);
				}
			}
		});

	}

	protected void loadSkin(Skin skin, File f, MainState state) throws IOException {
		this.loadSkin(skin, f, state, new HashMap<Integer, Boolean>());
	}

	protected void loadSkin(Skin skin, File f, MainState state, Map<Integer, Boolean> option) throws IOException {
		this.loadSkin0(skin, f, state, option);
	}

	private Map<String, String> filemap = new HashMap();

	protected S loadSkin(S skin, File f, MainState state, SkinHeader header, Map<Integer, Boolean> option,
			Map<String, Object> property) throws IOException {
		this.skin = skin;
		this.state = state;
		for (String key : property.keySet()) {
			if (property.get(key) != null) {
				if (property.get(key) instanceof Integer) {
					op.put((Integer) property.get(key), true);
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

		Map<Integer, SkinConfig.Offset> offset = new HashMap<>();
		for (CustomOffset of : Arrays.asList(header.getCustomOffsets())) {
			offset.put(of.id, (Offset) property.get(of.name));
		}
		skin.setOffset(offset);

		op.putAll(option);
		this.loadSkin0(skin, f, state, op);
		
		return skin;
	}

	SkinImage part = null;
	SkinImage button = null;
	SkinGraph bar = null;
	SkinSlider slider = null;
	SkinNumber num = null;
	SkinText text = null;
	String line = null;
	SkinImage PMcharaPart = null;

	List <Object> imagesetarray = new ArrayList<Object>();

	protected void loadSkin0(Skin skin, File f, MainState state, Map<Integer, Boolean> option) throws IOException {

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "MS932"));

		while ((line = br.readLine()) != null) {
			try {
				processLine(line, state);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		br.close();

		skin.setOption(option);

		for (SkinObject obj : skin.getAllSkinObjects()) {
			if (obj instanceof SkinImage && obj.getAllDestination().length == 0) {
				skin.removeSkinObject(obj);
				Logger.getGlobal().warning("NO_DESTINATION : " + obj);
			}
		}
	}

	protected int[] parseInt(String[] s) {
		int[] result = new int[22];
		for (int i = 1; i < s.length; i++) {
			try {
				result[i] = Integer.parseInt(s[i].replace('!', '-').replaceAll(" ", ""));
			} catch (Exception e) {

			}
		}
		return result;
	}

	protected TextureRegion[] getSourceImage(int[] values) {
		if (values[2] < imagelist.size() && imagelist.get(values[2]) != null
				&& imagelist.get(values[2]) instanceof Texture) {
			return getSourceImage((Texture) imagelist.get(values[2]), values[3], values[4], values[5], values[6],
					values[7], values[8]);
		}
		Logger.getGlobal().warning("IMAGEが定義されてないか、読み込みに失敗しています : " + line);
		return null;
	}

	protected TextureRegion[] getSourceImage(Texture image, int x, int y, int w, int h, int divx, int divy) {
		if (w == -1) {
			w = image.getWidth();
		}
		if (h == -1) {
			h = image.getHeight();
		}
		if (divx <= 0) {
			divx = 1;
		}
		if (divy <= 0) {
			divy = 1;
		}
		TextureRegion[] images = new TextureRegion[divx * divy];
		for (int i = 0; i < divx; i++) {
			for (int j = 0; j < divy; j++) {
				images[divx * j + i] = new TextureRegion(image, x + w / divx * i, y + h / divy * j, w / divx, h / divy);
			}
		}
		return images;
	}

	public S loadSkin(File f, MainState decide, SkinHeader header, Map<Integer, Boolean> option, SkinConfig.Property property) throws IOException {
		Map m = new HashMap();
		for(SkinConfig.Option op : property.getOption()) {
			m.put(op.name, op.value);
		}
		for(SkinConfig.FilePath file : property.getFile()) {
			m.put(file.name, file.path);
		}
		for(SkinConfig.Offset offset : property.getOffset()) {
			m.put(offset.name, offset);
		}
		return loadSkin(f, decide, header, option, m);
	}

	public abstract S loadSkin(File f, MainState decide, SkinHeader header, Map<Integer, Boolean> option, Map property) throws IOException;
	
	public static LR2SkinCSVLoader getSkinLoader(SkinType type, Resolution src, Config c) {
		switch(type) {
		case MUSIC_SELECT:
			return new LR2SelectSkinLoader(src, c);
		case DECIDE:
			return new LR2DecideSkinLoader(src, c);
		case PLAY_5KEYS:
		case PLAY_7KEYS:
		case PLAY_9KEYS:
		case PLAY_10KEYS:
		case PLAY_14KEYS:
			return new LR2PlaySkinLoader(type, src, c);
		case RESULT:
			return new LR2ResultSkinLoader(src, c);
		case COURSE_RESULT:
			return new LR2CourseResultSkinLoader(src, c);
		case KEY_CONFIG:
		}
		return null;
	}

	protected void PMcharaLoader(File imagefile, int type, int color, float dstx, float dsty, float dstw, float dsth, int side, int dsttimer, int dstOp1, int dstOp2, int dstOp3, int dstOffset) {
		//type 0:プレイ 1:キャラ背景 2:名前画像 3:ハリアイ画像(上半身のみ) 4:ハリアイ画像(全体) 5:キャラアイコン 6:NEUTRAL 7:FEVER 8:GREAT 9:GOOD 10:BAD 11:FEVERWIN 12:WIN 13:LOSE 14:OJAMA 15:DANCE
		final int PLAY = 0;
		final int BACKGROUND = 1;
		final int NAME = 2;
		final int FACE_UPPER = 3;
		final int FACE_ALL = 4;
		final int SELECT_CG = 5;
		final int NEUTRAL = 6;
		final int FEVER = 7;
		final int GREAT = 8;
		final int GOOD = 9;
		final int BAD = 10;
		final int FEVERWIN = 11;
		final int WIN = 12;
		final int LOSE = 13;
		final int OJAMA = 14;
		final int DANCE = 15;

		if(type < 0 || type > 15) return;

		File chp = null;
		File chpdir = null;

		if(imagefile.exists() && imagefile.getPath().substring(imagefile.getPath().length()-4,imagefile.getPath().length()).equalsIgnoreCase(".chp")) {
			chp = new File(imagefile.getPath());
		} else if (!imagefile.exists() && imagefile.getPath().substring(imagefile.getPath().length()-4,imagefile.getPath().length()).equalsIgnoreCase(".chp")) {
			chpdir = new File(imagefile.getPath().substring(0, Math.max(imagefile.getPath().lastIndexOf('\\'), imagefile.getPath().lastIndexOf('/')) + 1));
		} else {
			if(imagefile.getPath().charAt(imagefile.getPath().length()-1) != '/' && imagefile.getPath().charAt(imagefile.getPath().length()-1) != '\\') chpdir = new File(imagefile.getPath()+"/");
			else chpdir = new File(imagefile.getPath());
		}
		if(chp == null && chpdir != null) {
			//chpファイルを探す
			File[] filename = chpdir.listFiles();
			for(int i = 0; i < filename.length; i++) {
				if (filename[i].getPath().substring(filename[i].getPath().length()-4,filename[i].getPath().length()).equalsIgnoreCase(".chp")) {
					chp = new File(filename[i].getPath());
					break;
				}
			}
		}
		if(chp == null) return;

		//画像データ 0:#CharBMP 1:#CharBMP2P 2:#CharTex 3:#CharTex2P 4:#CharFace 5:#CharFace2P 6:#SelectCG 7:#SelectCG2P
		Texture[] CharBMP = new Texture[8];
		Arrays.fill(CharBMP, null);
		final int CharBMPIndex = 0;
		final int CharTexIndex = 2;
		final int CharFaceIndex = 4;
		final int SelectCGIndex = 6;
		//各パラメータ
		int[][] xywh = new int[256][4];
		for(int[] i: xywh){
			Arrays.fill(i, 0);
		}
		int anime = 100;
		int size[] = {0, 0};
		int frame[] = new int[20];
		Arrays.fill(frame, Integer.MIN_VALUE);
		int loop[] = new int[20];
		Arrays.fill(loop, -1);
		//最終的な色
		int setColor = 1;
		//フレーム補間の基準の時間 60FPSの17ms
		int increaseRateThreshold = 17;
		//#Pattern,#Texture,#Layerのデータ
		List<List<String>> patternData = new ArrayList<List<String>>();
		for(int i = 0; i < 3; i++) patternData.add(new ArrayList<String>());

		try (BufferedReader br = new BufferedReader(
			new InputStreamReader(new FileInputStream(chp), "MS932"));) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#") ) {
					String[] str = line.split("\t", -1);
					if (str.length > 1) {
						List<String> data = PMparseStr(str);
						if (str[0].equalsIgnoreCase("#CharBMP")) {
							//#Pattern, #Layer用画像
							if(data.size() > 1) CharBMP[CharBMPIndex] = getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1), usecim);
						} else if(str[0].equalsIgnoreCase("#CharBMP2P")) {
							//#Pattern, #Layer用画像2P
							if(data.size() > 1) CharBMP[CharBMPIndex+1] = getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1), usecim);
						} else if(str[0].equalsIgnoreCase("#CharTex")) {
							//#Texture用画像
							if(data.size() > 1) CharBMP[CharTexIndex] = getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1), usecim);
						} else if(str[0].equalsIgnoreCase("#CharTex2P")) {
							//#Texture用画像2P
							if(data.size() > 1) CharBMP[CharTexIndex+1] = getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1), usecim);
						} else if(str[0].equalsIgnoreCase("#CharFace")) {
							//ハリアイ
							if(data.size() > 1) CharBMP[CharFaceIndex] = getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1), usecim);
						} else if(str[0].equalsIgnoreCase("#CharFace2P")) {
							//ハリアイ2P
							if(data.size() > 1) CharBMP[CharFaceIndex+1] = getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1), usecim);
						} else if(str[0].equalsIgnoreCase("#SelectCG")) {
							//選択画面アイコン
							if(data.size() > 1) CharBMP[SelectCGIndex] = getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1), usecim);
						} else if(str[0].equalsIgnoreCase("#SelectCG2P")) {
							//選択画面アイコン2P
							if(data.size() > 1) CharBMP[SelectCGIndex+1] = getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1), usecim);
						} else if(str[0].equalsIgnoreCase("#Patern") || str[0].equalsIgnoreCase("#Pattern")) {
							//アニメーションデータ  表示優先度低  「ふぃーりんぐぽみゅ せかんど」ではスペルミスのtが一つ足りない#Paternが正式?
							patternData.get(0).add(line);
						} else if(str[0].equalsIgnoreCase("#Texture")) {
							//アニメーションデータ  表示優先度中
							patternData.get(1).add(line);
						} else if(str[0].equalsIgnoreCase("#Layer")) {
							//アニメーションデータ  表示優先度高
							patternData.get(2).add(line);
						} else if(str[0].equalsIgnoreCase("#Flame") || str[0].equalsIgnoreCase("#Frame")) {
							//アニメ速度 動き毎の1枚あたりの時間(ms) 「ふぃーりんぐぽみゅ せかんど」ではスペルミスの#Flameが正式?
							if(data.size() > 2) {
								if(PMparseInt(data.get(1)) >= 0 && PMparseInt(data.get(1)) < frame.length) frame[PMparseInt(data.get(1))] = PMparseInt(data.get(2));
							}
						} else if(str[0].equalsIgnoreCase("#Anime")) {
							//#Frame定義の指定がない時のアニメ速度 1枚あたりの時間(ms)
							if(data.size() > 1) anime = PMparseInt(data.get(1));
						} else if(str[0].equalsIgnoreCase("#Size")) {
							//#Patternや背景に用いる大きさ
							if(data.size() > 2) {
								size[0] = PMparseInt(data.get(1));
								size[1] = PMparseInt(data.get(2));
							}
						} else if(str[0].length() == 3 && PMparseInt(str[0].substring(1,3), 16) >= 0 && PMparseInt(str[0].substring(1,3), 16) < xywh.length) {
							//座標定義
							if(data.size() > xywh[0].length) {
								for(int i = 0; i < xywh[0].length; i++) {
									xywh[PMparseInt(str[0].substring(1,3), 16)][i] = PMparseInt(data.get(i+1));
								}
							}
						} else if(str[0].equalsIgnoreCase("#Loop")) {
							//ループ位置
							if(data.size() > 2) {
								if(PMparseInt(data.get(1)) >= 0 && PMparseInt(data.get(1)) < loop.length) loop[PMparseInt(data.get(1))] = PMparseInt(data.get(2));
							}
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		//#CharBMPが無い時はreturn
		if(CharBMP[CharBMPIndex] == null) return;
		//#CharBMP2Pが存在し、かつ#Texture定義があるときは#CharTex2Pが存在するなら2Pカラーとする
		if(color == 2 && CharBMP[CharBMPIndex+1] != null
				&& (patternData.get(1).size() == 0 || (patternData.get(1).size() > 0 && CharBMP[CharTexIndex+1] != null))
				) setColor = 2;
		//#Texture定義があるのに#CharTexが無い時はreturn
		if(setColor == 1 && patternData.get(1).size() > 0 && CharBMP[CharTexIndex] == null) return;


		//透過処理 右下の1pixelが透過色 選択画面アイコンは透過しない
		for(int i = 0; i < SelectCGIndex; i++) {
			if(CharBMP[i] != null) {
				Pixmap pixmap = new Pixmap( CharBMP[i].getWidth(), CharBMP[i].getHeight(), Format.RGBA8888 );
				int transparentColor = CharBMP[i].getTextureData().consumePixmap().getPixel(CharBMP[i].getWidth() - 1, CharBMP[i].getHeight() - 1);
				for(int x = 0; x < CharBMP[i].getWidth(); x++) {
					for(int y = 0; y < CharBMP[i].getHeight(); y++) {
						if(transparentColor != CharBMP[i].getTextureData().consumePixmap().getPixel(x, y)) {
							pixmap.drawPixel(x, y, CharBMP[i].getTextureData().consumePixmap().getPixel(x, y));
						}
					}
				}
				CharBMP[i].dispose();
				CharBMP[i] = new Texture( pixmap );
				pixmap.dispose();
			}
		}

		TextureRegion[] image = new TextureRegion[1];
		Texture setBMP;
		int setMotion = Integer.MIN_VALUE;
		PMcharaPart = null;
		switch(type) {
			case BACKGROUND:
				setBMP = CharBMP[CharBMPIndex + setColor-1];
				image = new TextureRegion[1];
				image[0] = new TextureRegion(setBMP, xywh[1][0], xywh[1][1], xywh[1][2], xywh[1][3]);
				PMcharaPart = new SkinImage(image, 0, 0);
				skin.add(PMcharaPart);
				break;
			case NAME:
				setBMP = CharBMP[CharBMPIndex + setColor-1];
				image = new TextureRegion[1];
				image[0] = new TextureRegion(setBMP, xywh[0][0], xywh[0][1], xywh[0][2], xywh[0][3]);
				PMcharaPart = new SkinImage(image, 0, 0);
				skin.add(PMcharaPart);
				break;
			case FACE_UPPER:
				setBMP = setColor == 2 && CharBMP[CharFaceIndex + 1] != null ? CharBMP[CharFaceIndex + 1] : CharBMP[CharFaceIndex];
				if(setBMP == null) break;
				image = new TextureRegion[1];
				image[0] = new TextureRegion(setBMP, 0, 0, 256, 256);
				PMcharaPart = new SkinImage(image, 0, 0);
				skin.add(PMcharaPart);
				break;
			case FACE_ALL:
				setBMP = setColor == 2 && CharBMP[CharFaceIndex + 1] != null ? CharBMP[CharFaceIndex + 1] : CharBMP[CharFaceIndex];
				if(setBMP == null) break;
				image = new TextureRegion[1];
				image[0] = new TextureRegion(setBMP, 320, 0, 320, 480);
				PMcharaPart = new SkinImage(image, 0, 0);
				skin.add(PMcharaPart);
				break;
			case SELECT_CG:
				setBMP = setColor == 2 && CharBMP[SelectCGIndex + 1] != null ? CharBMP[SelectCGIndex + 1] : CharBMP[SelectCGIndex];
				if(setBMP == null) break;
				image = new TextureRegion[1];
				image[0] = new TextureRegion(setBMP, 0, 0, setBMP.getWidth(), setBMP.getHeight());
				PMcharaPart = new SkinImage(image, 0, 0);
				skin.add(PMcharaPart);
				break;
			case NEUTRAL:
				if(setMotion == Integer.MIN_VALUE) setMotion = 1;
			case FEVER:
				if(setMotion == Integer.MIN_VALUE) setMotion = 6;
			case GREAT:
				if(setMotion == Integer.MIN_VALUE) setMotion = 7;
			case GOOD:
				if(setMotion == Integer.MIN_VALUE) setMotion = 8;
			case BAD:
				if(setMotion == Integer.MIN_VALUE) setMotion = 10;
			case FEVERWIN:
				if(setMotion == Integer.MIN_VALUE) setMotion = 17;
			case WIN:
				if(setMotion == Integer.MIN_VALUE) setMotion = 15;
			case LOSE:
				if(setMotion == Integer.MIN_VALUE) setMotion = 16;
			case OJAMA:
				if(setMotion == Integer.MIN_VALUE) setMotion = 3;
			case DANCE:
				if(setMotion == Integer.MIN_VALUE) setMotion = 14;
			case PLAY:
				for(int i = 0; i < frame.length; i++) {
					if(frame[i] == Integer.MIN_VALUE) frame[i] = anime;
					if(frame[i] < 1) frame[i] = 100;
				}
				//ダミー用
				Pixmap pixmap = new Pixmap( 1, 1, Format.RGBA8888 );
				Texture transparent = new Texture( pixmap );
				SkinImage part = null;
				//#Pattern,#Texture,#Layerの順に描画設定を行う
				int[] setBMPIndex = {CharBMPIndex,CharTexIndex,CharBMPIndex};
				for(int patternIndex = 0; patternIndex < 3; patternIndex++) {
					setBMP = CharBMP[setBMPIndex[patternIndex] + setColor-1];
					for(int patternDataIndex = 0; patternDataIndex < patternData.get(patternIndex).size(); patternDataIndex++) {
						String[] str = patternData.get(patternIndex).get(patternDataIndex).split("\t", -1);
						if (str.length > 1) {
							int motion = Integer.MIN_VALUE;
							String dst[] = new String[4];
							Arrays.fill(dst, "");
							List<String> data = PMparseStr(str);
							if(data.size() > 1) motion = PMparseInt(data.get(1));
							for (int i = 0; i < dst.length; i++) {
								if(data.size() > i + 2) dst[i] = data.get(i + 2).replaceAll("[^0-9a-fA-F-]", "");
							}
							int timer = Integer.MIN_VALUE;
							int op[] = {0,0,0};
							if(setMotion != Integer.MIN_VALUE && setMotion == motion) {
								timer = dsttimer;
								op[0] = dstOp1;
								op[1] = dstOp2;
								op[2] = dstOp3;
							} else if(setMotion == Integer.MIN_VALUE) {
								if(side != 2) {
									if(motion == 1) timer = TIMER_PM_CHARA_1P_NEUTRAL;
									else if(motion == 6) timer = TIMER_PM_CHARA_1P_FEVER;
									else if(motion == 7) timer = TIMER_PM_CHARA_1P_GREAT;
									else if(motion == 8) timer = TIMER_PM_CHARA_1P_GOOD;
									else if(motion == 10) timer = TIMER_PM_CHARA_1P_BAD;
									else if(motion >= 15 && motion <= 17) {
										timer = TIMER_MUSIC_END;
										if(motion == 15) {
											op[0] = OPTION_1P_BORDER_OR_MORE;	//WIN
											op[1] = -OPTION_1P_100;
										}
										else if(motion == 16) op[0] = -OPTION_1P_BORDER_OR_MORE;	//LOSE
										else if(motion == 17) op[0] = OPTION_1P_100;	//FEVERWIN
									}
								} else {
									if(motion == 1) timer = TIMER_PM_CHARA_2P_NEUTRAL;
									else if(motion == 7) timer = TIMER_PM_CHARA_2P_GREAT;
									else if(motion == 10) timer = TIMER_PM_CHARA_2P_BAD;
									else if(motion == 15 || motion == 16) {
										timer = TIMER_MUSIC_END;
										if(motion == 15) op[0] = -OPTION_1P_BORDER_OR_MORE;	//WIN
										else if(motion == 16) op[0] = OPTION_1P_BORDER_OR_MORE;	//LOSE
									}
								}
							}
							if(timer != Integer.MIN_VALUE
									&& (dst[0].length() > 0 && dst[0].length() % 2 == 0)
									&& (dst[1].length() == 0 || (dst[1].length() > 0 && dst[1].length() == dst[0].length()))
									&& (dst[2].length() == 0 || (dst[2].length() > 0 && dst[2].length() == dst[0].length()))
									&& (dst[3].length() == 0 || (dst[3].length() > 0 && dst[3].length() == dst[0].length()))
									) {
								if(loop[motion] >= dst[0].length() / 2 - 1) loop[motion] = dst[0].length() / 2 - 2;
								else if(loop[motion] < -1) loop[motion] = -1;
								int cycle = frame[motion] * dst[0].length() / 2;
								int loopTime = frame[motion] * (loop[motion]+1);
								if(setMotion == Integer.MIN_VALUE && timer >= TIMER_PM_CHARA_1P_NEUTRAL && timer < TIMER_MUSIC_END) {
									skin.setPMcharaTime(timer - TIMER_PM_CHARA_1P_NEUTRAL, cycle);
								}
								boolean hyphenFlag = false;
								for(int i = 1; i < dst.length; i++) {
									if(dst[i].indexOf("-") != -1) {
										hyphenFlag = true;
										break;
									}
								}
								//ハイフンがある時はフレーム補間を行う 60FPSの17msが基準
								int increaseRate = 1;
								if(hyphenFlag && frame[motion] >= increaseRateThreshold) {
									for(int i = 1; i <= frame[motion]; i++) {
										if(frame[motion] / i < increaseRateThreshold && frame[motion] % i == 0) {
											increaseRate = i;
											break;
										}
									}
									for(int i = 1; i < dst.length; i++) {
										int charsIndex = 0;
										char[] chars = new char[dst[i].length() * increaseRate];
										for(int j = 0; j < dst[i].length(); j+=2) {
											for(int k = 0; k < increaseRate; k++) {
												chars[charsIndex] = dst[i].charAt(j);
												charsIndex++;
												chars[charsIndex] = dst[i].charAt(j+1);
												charsIndex++;
											}
										}
										dst[i] = String.valueOf(chars);
									}
								}
								//DST読み込み
								double frameTime = frame[motion]/increaseRate;
								int loopFrame = loop[motion]*increaseRate;
								int dstxywh[][] = new int[dst[1].length() > 0 ? dst[1].length()/2 : dst[0].length()/2][4];
								for(int i = 0; i < dstxywh.length;i++){
									dstxywh[i][0] = 0;
									dstxywh[i][1] = 0;
									dstxywh[i][2] = size[0];
									dstxywh[i][3] = size[1];
								}
								int startxywh[] = {0,0,size[0],size[1]};
								int endxywh[] = {0,0,size[0],size[1]};
								int count;
								for(int i = 0; i < dst[1].length(); i+=2) {
									if(dst[1].length() >= i+2) {
										if(dst[1].substring(i, i+2).equals("--")) {
											count = 0;
											for(int j = i; j < dst[1].length() && dst[1].substring(j, j+2).equals("--"); j+=2) count++;
											if(PMparseInt(dst[1].substring(i+count*2, i+count*2+2), 16) >= 0 && PMparseInt(dst[1].substring(i+count*2, i+count*2+2), 16) <= 255) endxywh = xywh[PMparseInt(dst[1].substring(i+count*2, i+count*2+2), 16)];
											for(int j = i; j < dst[1].length() && dst[1].substring(j, j+2).equals("--"); j+=2) {
												int[] value = new int[dstxywh[0].length];
												for(int k = 0; k < dstxywh[0].length; k++) {
													value[k] = startxywh[k] + (endxywh[k] - startxywh[k]) * ((j - i) / 2 + 1) / (count + 1);
												}
												System.arraycopy(value,0,dstxywh[j/2],0,value.length);
											}
											i += (count - 1) * 2;
										} else if(PMparseInt(dst[1].substring(i, i+2), 16) >= 0 && PMparseInt(dst[1].substring(i, i+2), 16) <= 255) {
											startxywh = xywh[PMparseInt(dst[1].substring(i, i+2), 16)];
											System.arraycopy(startxywh,0,dstxywh[i/2],0,startxywh.length);
										}
									}
								}
								//alphaとangleの読み込み
								int alphaAngle[][] = new int[dstxywh.length][2];
								for(int i = 0; i < alphaAngle.length; i++){
									alphaAngle[i][0] = 255;
									alphaAngle[i][1] = 0;
								}
								for(int index = 2 ; index < dst.length; index++) {
									int startValue = 0;
									int endValue = 0;
									for(int i = 0; i < dst[index].length(); i+=2) {
										if(dst[index].length() >= i+2) {
											if(dst[index].substring(i, i+2).equals("--")) {
												count = 0;
												for(int j = i; j < dst[index].length() && dst[index].substring(j, j+2).equals("--"); j+=2) count++;
												if(PMparseInt(dst[index].substring(i+count*2, i+count*2+2), 16) >= 0 && PMparseInt(dst[index].substring(i+count*2, i+count*2+2), 16) <= 255) {
													endValue = PMparseInt(dst[index].substring(i+count*2, i+count*2+2), 16);
													if(index == 3) endValue = Math.round(endValue * 360f / 256f);
												}
												for(int j = i; j < dst[index].length() && dst[index].substring(j, j+2).equals("--"); j+=2) {
													alphaAngle[j/2][index - 2] = startValue + (endValue - startValue) * ((j - i) / 2 + 1) / (count + 1);
												}
												i += (count - 1) * 2;
											} else if(PMparseInt(dst[index].substring(i, i+2), 16) >= 0 && PMparseInt(dst[index].substring(i, i+2), 16) <= 255) {
												startValue = PMparseInt(dst[index].substring(i, i+2), 16);
												if(index == 3) startValue = Math.round(startValue * 360f / 256f);;
												alphaAngle[i/2][index - 2] = startValue;
											}
										}
									}
								}
								//ループ開始フレームまで
								if((loopFrame+increaseRate) != 0) {
									TextureRegion[] images = new TextureRegion[(loop[motion]+1)];
									for(int i = 0; i < (loop[motion]+1) * 2; i+=2) {
										int index = PMparseInt(dst[0].substring(i, i+2), 16);
										if(index >= 0 && index < xywh.length && xywh[index][2] > 0 && xywh[index][3] > 0) images[i/2] = new TextureRegion(setBMP, xywh[index][0], xywh[index][1], xywh[index][2], xywh[index][3]);
										else images[i/2] = new TextureRegion(transparent, 0, 0, 1, 1);
									}
									part = new SkinImage(images, timer, loopTime);
									skin.add(part);
									for(int i = 0; i < (loopFrame+increaseRate); i++) {
										part.setDestination((int)(frameTime*i),dstx+dstxywh[i][0]*dstw/size[0], dsty+dsth-(dstxywh[i][1]+dstxywh[i][3])*dsth/size[1], dstxywh[i][2]*dstw/size[0], dstxywh[i][3]*dsth/size[1],3,alphaAngle[i][0],255,255,255,1,0,alphaAngle[i][1],0,-1,timer,op[0],op[1],op[2],0);
									}
									part.setDestination(loopTime-1,dstx+dstxywh[(loopFrame+increaseRate)-1][0]*dstw/size[0], dsty+dsth-(dstxywh[(loopFrame+increaseRate)-1][1]+dstxywh[(loopFrame+increaseRate)-1][3])*dsth/size[1], dstxywh[(loopFrame+increaseRate)-1][2]*dstw/size[0], dstxywh[(loopFrame+increaseRate)-1][3]*dsth/size[1],3,alphaAngle[(loopFrame+increaseRate)-1][0],255,255,255,1,0,alphaAngle[(loopFrame+increaseRate)-1][1],0,-1,timer,op[0],op[1],op[2],dstOffset);
								}
								//ループ開始フレームから
								TextureRegion[] images = new TextureRegion[dst[0].length() / 2 - (loop[motion]+1)];
								for(int i = (loop[motion]+1)  * 2; i < dst[0].length(); i+=2) {
									int index = PMparseInt(dst[0].substring(i, i+2), 16);
									if(index >= 0 && index < xywh.length && xywh[index][2] > 0 && xywh[index][3] > 0) images[i/2-(loop[motion]+1)] = new TextureRegion(setBMP, xywh[index][0], xywh[index][1], xywh[index][2], xywh[index][3]);
									else images[i/2-(loop[motion]+1)] = new TextureRegion(transparent, 0, 0, 1, 1);
								}
								part = new SkinImage(images, timer, cycle - loopTime);
								skin.add(part);
								for(int i = (loopFrame+increaseRate); i < dstxywh.length; i++) {
									part.setDestination((int)(frameTime*i),dstx+dstxywh[i][0]*dstw/size[0], dsty+dsth-(dstxywh[i][1]+dstxywh[i][3])*dsth/size[1], dstxywh[i][2] * dstw / size[0], dstxywh[i][3] * dsth / size[1],3,alphaAngle[i][0],255,255,255,1,0,alphaAngle[i][1],0,loopTime,timer,op[0],op[1],op[2],0);
								}
								part.setDestination(cycle,dstx+dstxywh[dstxywh.length-1][0]*dstw/size[0], dsty+dsth-(dstxywh[dstxywh.length-1][1]+dstxywh[dstxywh.length-1][3])*dsth/size[1], dstxywh[dstxywh.length-1][2] * dstw / size[0], dstxywh[dstxywh.length-1][3] * dsth / size[1],3,alphaAngle[dstxywh.length-1][0],255,255,255,1,0,alphaAngle[dstxywh.length-1][1],0,loopTime,timer,op[0],op[1],op[2],dstOffset);
							}
						}
					}
				}
				break;
		}
	}
	private int PMparseInt(String s) {
		return Integer.parseInt(s.replaceAll("[^0-9-]", ""));
	}
	private int PMparseInt(String s, int radix) {
		return Integer.parseInt(s.replaceAll("[^0-9a-fA-F-]", ""), radix);
	}
	private List<String> PMparseStr(String[] s) {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < s.length; i++) {
			if(s[i].length() > 0) {
				if(s[i].startsWith("/")) {
					break;
				} else if(s[i].indexOf("//") != -1) {
					list.add(s[i].substring(0, s[i].indexOf("//")));
					break;
				} else {
					list.add(s[i]);
				}
			}
		}
		return list;
	}


}
