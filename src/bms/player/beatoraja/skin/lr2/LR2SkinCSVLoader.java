package bms.player.beatoraja.skin.lr2;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.*;
import bms.player.beatoraja.SkinConfig.Offset;
import bms.player.beatoraja.play.bga.BGAProcessor;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.SkinHeader.*;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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

				addCommandWord(new CommandWord("SET_IMAGESET") {
			@Override
			public void execute(String[] str) {
				imagesetarray.clear();
				part = null;
				cycle = Integer.parseInt(str[1]);
				timer = Integer.parseInt(str[2]);
				ref = Integer.parseInt(str[3]);
			}
		});

		addCommandWord(new CommandWord("SRC_IMAGESET") {
			@Override
			public void execute(String[] str) {
				int gr = Integer.parseInt(str[2]);
				if (gr < imagelist.size() && imagelist.get(gr) != null) {
					int[] values = parseInt(str);
					imagesetarray.add(getSourceImage(values));
				}
			}
		});

		addCommandWord(new CommandWord("END_IMAGESET") {
			@Override
			public void execute(String[] str) {
				TextureRegion[][] tr = imagesetarray.toArray(new TextureRegion[imagesetarray.size()][]);
				part = new SkinImage(tr, timer, cycle);
				part.setReferenceID(ref);
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
				int edit = values[5];
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

	int timer;
	int cycle;
	int ref;
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
}
