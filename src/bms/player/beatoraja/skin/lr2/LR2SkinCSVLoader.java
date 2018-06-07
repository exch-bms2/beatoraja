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
import com.badlogic.gdx.utils.IntArray;

/**
<<<<<<< HEAD
 * LR2�겗�궧�궘�꺍若싩쑴�뵪csv�깢�궊�궎�꺂�겗�꺆�꺖���꺖
=======
 * LR2占쎄쿁占쎄때占쎄텣占쎄틡畑댁떓�뫒占쎈뎁csv占쎄묄占쎄텏占쎄텕占쎄틓占쎄쿁占쎄틙占쎄틬占쏙옙占쎄틬
>>>>>>> upstream/master
 * 
 * @author exch
 */
public abstract class LR2SkinCSVLoader<S extends Skin> extends LR2SkinLoader {

	List<Object> imagelist = new ArrayList<Object>();
	List<SkinTextImage.SkinTextImageSource> fontlist = new ArrayList<>();

	/**
	 * 占쎄때占쎄텣占쎄틡占쎄쿁占쎈�뉛옙沅�占쎄텕占쎄땍
	 */
	public final Resolution src;
	/**
	 * 占쎈짂占쎈돕占쎄땁占쎄텕占쎄땍
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
								Logger.getGlobal().warning("BGA占쎄묄占쎄텏占쎄텕占쎄틓亦껓옙占쎄껙歷��뇠寃녜툣湲몃리占쏙옙" + e.getMessage());
								e.printStackTrace();
							}
						}
					}

					if (!isMovie) {
						imagelist.add(getTexture(imagefile.getPath(), usecim));
					}
				} else {
					Logger.getGlobal()
							.warning("IMAGE " + imagelist.size() + " : 占쎄묄占쎄텏占쎄텕占쎄틓占쎄괏沃ㅻ뿣寃뉛옙嫄�占쎄덮占쎄께占쎄굘占쎄뎐 : " + imagefile.getPath());
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
							.warning("IMAGE " + imagelist.size() + " : 占쎄묄占쎄텏占쎄텕占쎄틓占쎄괏沃ㅻ뿣寃뉛옙嫄�占쎄덮占쎄께占쎄굘占쎄뎐 : " + imagefile.getPath());
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
					SkinDestinationSize dstSize = new SkinDestinationSize( values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch);
					part.setDestination(values[2], dstSize, values[7], values[8], values[9], values[10], values[11],
							values[12], values[13], values[14], values[15], values[16], values[17], values[18],
							values[19], values[20], readOffset(str, 21));
				}
			}
		});

		addCommandWord(new CommandWord("SRC_NUMBER") {
			//#SRC_NUMBER,(NULL),gr,x,y,w,h,div_x,div_y,cycle,timer,num,align,keta,zeropadding
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

							num = new SkinNumber(pn, mn, values[10], values[9], values[13] + 1, str[14].length() > 0 ? values[14] : 2, values[11]);
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
					SkinDestinationSize dstSize = new SkinDestinationSize( values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch);
					num.setDestination(values[2], dstSize, values[7], values[8], values[9],
							values[10], values[11], values[12], values[13], values[14], values[15], values[16],
							values[17], values[18], values[19], values[20], readOffset(str, 21));
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
					SkinDestinationSize dstSize = new SkinDestinationSize( values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch);
					text.setDestination(values[2], dstSize, values[7], values[8], values[9], values[10], values[11],
							values[12], values[13], values[14], values[15], values[16], values[17], values[18],
							values[19], values[20], readOffset(str, 21));
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
		addCommandWord(new CommandWord("SRC_SLIDER_REFNUMBER") {
			//NUMBER占썬뀿猶울옙�깍옙�렦
			//#SRC_SLIDER_REFNUMBER,(NULL),gr,x,y,w,h,div_x,div_y,cycle,timer,muki,range,type,disable,min_value,max_value
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
					slider.setRefNum(true);
					slider.setMin(values[15]);
					slider.setMax(values[16]);
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
					SkinDestinationSize dstSize = new SkinDestinationSize( values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch);
					slider.setDestination(values[2], dstSize, values[7], values[8], values[9], values[10], values[11],
							values[12], values[13], values[14], values[15], values[16], values[17], values[18],
							values[19], values[20], readOffset(str, 21));
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
		addCommandWord(new CommandWord("SRC_BARGRAPH_REFNUMBER") {
			//NUMBER占썬뀿猶울옙�깍옙�렦
			//#SRC_BARGRAPH_REFNUMBER,(NULL),gr,x,y,w,h,div_x,div_y,cycle,timer,type,muki,min_value,max_value
			@Override
			public void execute(String[] str) {
				bar = null;
				int[] values = parseInt(str);
				int gr = values[2];
				if (gr >= 100) {
					bar = new SkinGraph(gr);
					bar.setReferenceID(values[11]);
					bar.setDirection(values[12]);
					bar.setRefNum(true);
					bar.setMin(values[13]);
					bar.setMax(values[14]);
				} else {
					TextureRegion[] images = getSourceImage(values);
					if (images != null) {
						bar = new SkinGraph(images, values[10], values[9]);
						bar.setReferenceID(values[11]);
						bar.setDirection(values[12]);
						bar.setRefNum(true);
						bar.setMin(values[13]);
						bar.setMax(values[14]);
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
					SkinDestinationSize dstSize = new SkinDestinationSize( values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch);
					bar.setDestination(values[2], dstSize, values[7], values[8], values[9],
							values[10], values[11], values[12], values[13], values[14], values[15], values[16],
							values[17], values[18], values[19], values[20], readOffset(str, 21));
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
						button.setClickeventType(values[14] > 0 ? 0 : values[14] < 0 ? 1 : 2);
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
					SkinDestinationSize dstSize = new SkinDestinationSize( values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch);
					button.setDestination(values[2], dstSize, values[7], values[8], values[9], values[10], values[11],
							values[12], values[13], values[14], values[15], values[16], values[17], values[18],
							values[19], values[20], readOffset(str, 21));
				}
			}
		});
		addCommandWord(new CommandWord("SRC_ONMOUSE") {
			@Override
			public void execute(String[] str) {
				onmouse = null;
				int gr = Integer.parseInt(str[2]);
				if (gr < imagelist.size() && imagelist.get(gr) != null) {
					int[] values = parseInt(str);
					TextureRegion[] images = getSourceImage(values);
					if (images != null) {
						onmouse = new SkinImage(images, values[10], values[9]);
						skin.setMouseRect(onmouse, values[12], values[6] - values[13] - values[15], values[14], values[15]);
						skin.add(onmouse);
					}
				}
			}
		});
		addCommandWord(new CommandWord("DST_ONMOUSE") {
			@Override
			public void execute(String[] str) {
				if (onmouse != null) {
					int[] values = parseInt(str);
					SkinDestinationSize dstSize = new SkinDestinationSize( values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch);
					onmouse.setDestination(values[2], dstSize, values[7], values[8], values[9], values[10], values[11],
							values[12], values[13], values[14], values[15], values[16], values[17], values[18],
							values[19], values[20], readOffset(str, 21));
				}
			}
		});
		addCommandWord(new CommandWord("DST_PM_CHARA_1P") {
			@Override
			public void execute(String[] str) {
				//占쎄묏占쎄틕占쎄텕占쎈뎁 占쎈떓畑댁떘占쏙퐛�빣
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
				System.out.println(str[7]);
				final File imagefile = SkinLoader.getPath(str[7].replace("LR2files\\Theme", "skin").replace("\\", "/"), filemap);
				SkinOption skinOption = new SkinOption(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, values[6]);
				SkinDestinationSize skinSize = new SkinDestinationSize((int)(values[1] * dstw / srcw), (int)(dsth -(values[2] + values[4]) * dsth / srch), (int)(values[3] * dstw / srcw), (int)(values[4] * dsth / srch));
				PMcharaLoader pmCharaLoader = new PMcharaLoader(skin);
				pmCharaLoader.Load(usecim, imagefile,
						0, (values[5] == 1 || values[5] == 2) ? values[5] : 1,
						skinSize, 1, Integer.MIN_VALUE, skinOption);
			}
		});
		addCommandWord(new CommandWord("DST_PM_CHARA_2P") {
			@Override
			public void execute(String[] str) {
				//占쎄묏占쎄틕占쎄텕占쎈뎁 占쎈떓畑댁떘占쏙퐛�빣

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
				SkinOption skinOption = new SkinOption(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE,  values[6]);
				SkinDestinationSize skinSize = new SkinDestinationSize((int)(values[1] * dstw / srcw), (int)(dsth -(values[2] + values[4]) * dsth / srch), (int)(values[3] * dstw / srcw), (int)(values[4] * dsth / srch));
				PMcharaLoader pmCharaLoader = new PMcharaLoader(skin);
				pmCharaLoader.Load(usecim, imagefile,
						0, (values[5] == 1 || values[5] == 2) ? values[5] : 1,
						skinSize, 2, Integer.MIN_VALUE, skinOption);
			}
		});
		addCommandWord(new CommandWord("DST_PM_CHARA_ANIMATION") {
			@Override
			public void execute(String[] str) {
				//占쎄묏占쎄틕占쎄텕繞벿듭㉫占쎈뎁 占쎈떓畑댁떘�봿占쏙퐛�빣
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
					SkinOption skinOption = new SkinOption(values[8], values[9], values[10], values[11]);
					SkinDestinationSize skinSize = new SkinDestinationSize((int)(values[1] * dstw / srcw), (int)(dsth -(values[2] + values[4]) * dsth / srch), (int)(values[3] * dstw / srcw), (int)(values[4] * dsth / srch));
					PMcharaLoader pmCharaLoader = new PMcharaLoader(skin);
					pmCharaLoader.Load(usecim, imagefile,
							values[6] + 6, (values[5] == 1 || values[5] == 2) ? values[5] : 1,
							skinSize, Integer.MIN_VALUE, values[7], skinOption);
				}
			}
		});
		addCommandWord(new CommandWord("SRC_PM_CHARA_IMAGE") {
			@Override
			public void execute(String[] str) {
				//color,type,folderpath
				//type 0:占쎄텣占쎄뭇占쎄�占쎄퉺占쎌녂 1:占쎈┼占쎈렭占쎈돕占쎄퉿 2:占쎄퉿占쎄틒占쎄텑占쎄텕占쎈돕占쎄퉿(鼇앸벩�쐡玲곥깵寃쀯옙寃�) 3:占쎄퉿占쎄틒占쎄텑占쎄텕占쎈돕占쎄퉿(占쎈�꿴턁占�) 4:占쎄텣占쎄뭇占쎄�占쎄텑占쎄텕占쎄텭占쎄틡
				PMcharaPart = null;
				int[] values = parseInt(str);
				if(values[2] >= 0 && values[2] <= 4) {
					System.out.println(str[3]);
					final File imagefile = SkinLoader.getPath(str[3].replace("LR2files\\Theme", "skin").replace("\\", "/"), filemap);
					SkinOption skinOption = new SkinOption(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
					SkinDestinationSize skinSize = new SkinDestinationSize(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
					PMcharaLoader pmCharaLoader = new PMcharaLoader(skin);
					PMcharaPart = pmCharaLoader.Load(usecim, imagefile,
							values[2] + 1, (values[1] == 1 || values[1] == 2) ? values[1] : 1,
									skinSize, Integer.MIN_VALUE, Integer.MIN_VALUE, skinOption);
				}
			}
		});
		addCommandWord(new CommandWord("DST_PM_CHARA_IMAGE") {
			@Override
			public void execute(String[] str) {
				//DST_IMAGE占쎄쾹占쎈┴癲낉옙
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
					SkinDestinationSize dstSize = new SkinDestinationSize( values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch);
					PMcharaPart.setDestination(values[2], dstSize, values[7], values[8], values[9], values[10], values[11],
							values[12], values[13], values[14], values[15], values[16], values[17], values[18],
							values[19], values[20], readOffset(str, 21));
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
	SkinImage onmouse = null;
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
		for (int i = 1; i < result.length && i < s.length; i++) {
			try {
				result[i] = Integer.parseInt(s[i].replace('!', '-').replaceAll(" ", ""));
			} catch (Exception e) {

			}
		}
		return result;
	}

	protected int[] readOffset(String[] str, int startIndex) {
		return readOffset(str, startIndex, new int[0]);
	}

	protected int[] readOffset(String[] str, int startIndex, int[] offset) {
		IntArray result = new IntArray();
		for(int i : offset) {
			result.add(i);
		}
		for (int i = startIndex; i < str.length; i++) {
			String s = str[i].replaceAll("[^0-9-]", "");
			if(s.length() > 0) {
				result.add(Integer.parseInt(s));
			}
		}
		return result.toArray();
	}

	protected TextureRegion[] getSourceImage(int[] values) {
		if (values[2] < imagelist.size() && imagelist.get(values[2]) != null
				&& imagelist.get(values[2]) instanceof Texture) {
			return getSourceImage((Texture) imagelist.get(values[2]), values[3], values[4], values[5], values[6],
					values[7], values[8]);
		}
		Logger.getGlobal().warning("IMAGE占쎄괏畑댁떓�뫒占쎄괵占쎄덱占쎄쾷占쎄쾼占쎄콢占쎄괍占쎄낟�걤占쎄껙歷��뇠寃놂옙寃볣툣湲몃리占쎄괼占쎄쾷占쎄콢占쎄께占쎄굉 : " + line);
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
			if(op.value != OPTION_RANDOM_VALUE) {
				m.put(op.name, op.value);
			} else {
				for (CustomOption opt : header.getCustomOptions()) {
					if(op.name.equals(opt.name)) {
						if(header.getRandomSelectedOptions(op.name) >= 0) m.put(op.name, header.getRandomSelectedOptions(op.name));
					}
				}
			}
		}
		for(SkinConfig.FilePath file : property.getFile()) {
			if(!file.path.equals("Random")) {
				m.put(file.name, file.path);
			} else {
				for (CustomFile cf : header.getCustomFiles()) {
					if(file.name.equals(cf.name)) {
						String ext = cf.path.substring(cf.path.lastIndexOf("*") + 1);
						if(cf.path.contains("|")) {
							if(cf.path.length() > cf.path.lastIndexOf('|') + 1) {
								ext = cf.path.substring(cf.path.lastIndexOf("*") + 1, cf.path.indexOf('|')) + cf.path.substring(cf.path.lastIndexOf('|') + 1);
							} else {
								ext = cf.path.substring(cf.path.lastIndexOf("*") + 1, cf.path.indexOf('|'));
							}
						}
						File dir = new File(cf.path.substring(0, cf.path.lastIndexOf('/')));
						if (dir.exists() && dir.isDirectory()) {
							List<File> l = new ArrayList<File>();
							for (File subfile : dir.listFiles()) {
								if (subfile.getPath().toLowerCase().endsWith(ext)) {
									l.add(subfile);
								}
							}
							if (l.size() > 0) {
								String filename = l.get((int) (Math.random() * l.size())).getName();
								m.put(file.name, filename);
							}
						}
					}
				}
			}
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
			return null;
		case SKIN_SELECT:
			return new LR2SkinSelectSkinLoader(src, c);
		}
		return null;
	}




}
