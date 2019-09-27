package bms.player.beatoraja.skin.lr2;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static bms.player.beatoraja.skin.SkinProperty.*;

import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.SkinConfig.Offset;
import bms.player.beatoraja.play.PlaySkin;
import bms.player.beatoraja.play.SkinGauge;
import bms.player.beatoraja.play.bga.BGAProcessor;
import bms.player.beatoraja.select.MusicSelectSkin;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.SkinHeader.*;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * LR2のスキン定義用csvファイルのローダー
 *
 * @author exch
 */
public abstract class LR2SkinCSVLoader<S extends Skin> extends LR2SkinLoader {

	Array<Object> imagelist = new Array<Object>();
	Array<SkinTextImage.SkinTextImageSource> fontlist = new Array<SkinTextImage.SkinTextImageSource>();

	/**
	 * スキンの元サイズ
	 */
	public final Resolution src;
	/**
	 * 描画サイズ
	 */
	public final Resolution dst;
	private boolean usecim;
	private String skinpath;

	protected S skin;

	private MainState state;

	public LR2SkinCSVLoader(Resolution src, Config c) {
		this.src = src;
		this.dst = c.getResolution();
		usecim = c.isCacheSkinImage();
		skinpath = c.getSkinpath();

		final float srcw = src.width;
		final float srch = src.height;
		final float dstw = dst.width;
		final float dsth = dst.height;
		
		addCommandWord(CSVCommand.values());

		addCommandWord(new CommandWord("INCLUDE") {
			@Override
			public void execute(String[] str) {
				final File imagefile = LR2SkinLoader.getPath(skinpath, str[1], filemap);
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
				final File imagefile = LR2SkinLoader.getPath(skinpath, str[1], filemap);
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
							.warning("IMAGE " + imagelist.size + " : ファイルが見つかりません : " + imagefile.getPath());
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
				final File imagefile = LR2SkinLoader.getPath(skinpath, str[1], filemap);
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
							.warning("IMAGE " + imagelist.size + " : ファイルが見つかりません : " + imagefile.getPath());
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
					if (values[2] < imagelist.size && imagelist.get(values[2]) != null
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
				if (gr < imagelist.size && imagelist.get(gr) != null) {
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
							values[19], values[20], readOffset(str, 21));
					part.setStretch(stretch);
				}
			}
		});

		addCommandWord(new CommandWord("SRC_NUMBER") {
			//#SRC_NUMBER,(NULL),gr,x,y,w,h,div_x,div_y,cycle,timer,num,align,keta,zeropadding
			@Override
			public void execute(String[] str) {
				num = null;
				int[] values = parseInt(str);
				final int divx = values[7] > 0 ? values[7] : 1;
				final int divy = values[8] > 0 ? values[8] : 1;

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

							num = new SkinNumber(pn, mn, values[10], values[9], values[13] + 1, str[14].length() > 0 ? values[14] : 2, values[15], values[11]);
							num.setAlign(values[12]);
						} else {
							int d = images.length % 10 == 0 ? 10 : 11;

							TextureRegion[][] nimages = new TextureRegion[divx * divy / d][d];
							for (int i = 0; i < d; i++) {
								for (int j = 0; j < divx * divy / d; j++) {
									nimages[j][i] = images[j * d + i];
								}
							}

							num = new SkinNumber(nimages, values[10], values[9], values[13], d > 10 ? 2 : 0, values[15],
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
							values[17], values[18], values[19], values[20], readOffset(str, 21));
				}
			}
		});

		addCommandWord(new CommandWord("SRC_TEXT") {
			@Override
			public void execute(String[] str) {
				text = null;
				int[] values = parseInt(str);
				if (values[2] < fontlist.size && fontlist.get(values[2]) != null) {
					text = new SkinTextImage(fontlist.get(values[2]), values[3]);
				} else {
					text = new SkinTextFont("skin/default/VL-Gothic-Regular.ttf", 0, 48, 2);
				}
				text.setAlign(values[4]);
				text.setEditable(values[5] != 0);
				int panel = values[6];
				skin.add(text);
				if(text.isEditable() && values[3] == SkinProperty.STRING_SEARCHWORD && skin instanceof MusicSelectSkin) {
					((MusicSelectSkin) skin).searchText = text;
				}

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
							values[19], values[20], readOffset(str, 21));
					if(skin instanceof MusicSelectSkin && ((MusicSelectSkin) skin).searchText == text) {
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
					slider.setChangeable(values[14] == 0);
					skin.add(slider);

					// TODO 固有実装の汎用化
					if((skin instanceof PlaySkin) && values[13] == SLIDER_LANECOVER) {
						((PlaySkin)skin).laneCover = slider;
					}

					// System.out.println("Object Added - " +
					// (part.getTiming()));
				}
			}
		});
		addCommandWord(new CommandWord("SRC_SLIDER_REFNUMBER") {
			//NUMBER値参照版
			//#SRC_SLIDER_REFNUMBER,(NULL),gr,x,y,w,h,div_x,div_y,cycle,timer,muki,range,type,disable,min_value,max_value
			@Override
			public void execute(String[] str) {
				slider = null;
				int[] values = parseInt(str);
				TextureRegion[] images = getSourceImage(values);
				if (images != null) {
					slider = new SkinSlider(images, values[10], values[9], values[11],
							(int) (values[12] * (values[11] == 1 || values[11] == 3 ? (dstw / srcw) : (dsth / srch))),
							values[13], values[15], values[16]);
					slider.setChangeable(values[14] == 0);
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
					bar = new SkinGraph(gr, values[11] + 100);
					bar.setDirection(values[12]);
				} else {
					TextureRegion[] images = getSourceImage(values);
					if (images != null) {
						bar = new SkinGraph(images, values[10], values[9],values[11] + 100);
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
			//NUMBER値参照版
			//#SRC_BARGRAPH_REFNUMBER,(NULL),gr,x,y,w,h,div_x,div_y,cycle,timer,type,muki,min_value,max_value
			@Override
			public void execute(String[] str) {
				bar = null;
				int[] values = parseInt(str);
				int gr = values[2];
				if (gr >= 100) {
					bar = new SkinGraph(gr,values[11],values[13],values[14]);
					bar.setDirection(values[12]);
				} else {
					TextureRegion[] images = getSourceImage(values);
					if (images != null) {
						bar = new SkinGraph(images,values[10], values[9], values[11],values[13],values[14]);
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
							values[17], values[18], values[19], values[20], readOffset(str, 21));
				}
			}
		});
		addCommandWord(new CommandWord("SRC_BUTTON") {
			@Override
			public void execute(String[] str) {
				button = null;
				int gr = Integer.parseInt(str[2]);
				if (gr < imagelist.size && imagelist.get(gr) != null) {
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
					final int divx = values[7] > 0 ? values[7] : 1;
					final int divy = values[8] > 0 ? values[8] : 1;
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
					button.setDestination(values[2], values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
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
				if (gr < imagelist.size && imagelist.get(gr) != null) {
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
					onmouse.setDestination(values[2], values[3] * dstw / srcw,
							dsth - (values[4] + values[6]) * dsth / srch, values[5] * dstw / srcw,
							values[6] * dsth / srch, values[7], values[8], values[9], values[10], values[11],
							values[12], values[13], values[14], values[15], values[16], values[17], values[18],
							values[19], values[20], readOffset(str, 21));
				}
			}
		});
		addCommandWord(new CommandWord("SRC_GROOVEGAUGE") {
			//SRC定義,index,gr,x,y,w,h,div_x,div_y,cycle,timer,add_x,add_y,parts,animation_type,animation_range,animation_cycle,starttime,endtime
			//parts:粒数 animation_type:アニメーションの種類(0:RANDOM 1:INCLEASE 2:DECLEASE 3:PMS用明滅アニメーション)
			//animation_range:アニメーションする範囲 animation_cycle:アニメーション間隔(ms)
			//starttime, endtime:リザルト用 ゲージが0から曲終了時の値まで増える演出の開始時間・終了時間(ms)
			@Override
			public void execute(String[] str) {
				gauger = null;
				int[] values = parseInt(str);
				if (values[2] < imagelist.size && imagelist.get(values[2]) != null) {
					int playside = values[1];
					final int divx = values[7] > 0 ? values[7] : 1;
					final int divy = values[8] > 0 ? values[8] : 1;
					TextureRegion[][] gauge;
					if(values[14] == 3 && divx * divy % 6 == 0) {
						//アニメーションタイプがPMS用明滅アニメーションの場合 表赤、表緑、裏赤、裏緑、発光表赤、発光表緑の順にsrc分割
						gauge = new TextureRegion[(divx * divy) / 6][36];
						final int w = values[5];
						final int h = values[6];
						for (int x = 0; x < divx; x++) {
							for (int y = 0; y < divy; y++) {
								if ((y * divx + x) / 6 < gauge.length) {
									TextureRegion tr = new TextureRegion(
											(Texture) imagelist.get(values[2]), values[3] + w * x / divx,
											values[4] + h * y / divy, w / divx, h / divy);
									final int dx = (y * divx + x) / 6;
									final int dy = (y * divx + x) % 6;
									gauge[dx][dy] = gauge[dx][dy + 6] = gauge[dx][dy + 12] = gauge[dx][dy + 18]
											= gauge[dx][dy + 24] = gauge[dx][dy + 30] = tr;

								}
							}
						}
					} else {
						gauge = new TextureRegion[(divx * divy) / 4][36];
						final int w = values[5];
						final int h = values[6];
						for (int x = 0; x < divx; x++) {
							for (int y = 0; y < divy; y++) {
								if ((y * divx + x) / 4 < gauge.length) {
									TextureRegion tr = new TextureRegion(
											(Texture) imagelist.get(values[2]), values[3] + w * x / divx,
											values[4] + h * y / divy, w / divx, h / divy);
									final int dx = (y * divx + x) / 4;
									final int dy = (y * divx + x) % 4;
									gauge[dx][dy] = gauge[dx][dy + 6] = gauge[dx][dy + 12] = gauge[dx][dy + 18]
											= gauge[dx][dy + 24] = gauge[dx][dy + 30] = tr;
									if(dy < 2) {
										gauge[dx][dy + 4] = gauge[dx][dy + 10] = gauge[dx][dy + 16] = gauge[dx][dy + 22]
												= gauge[dx][dy + 28] = gauge[dx][dy + 34] = tr;
									}
								}
							}
						}
					}
					groovex = values[11];
					groovey = values[12];
					if (gauger == null) {
						if(values[13] == 0) {
							gauger = new SkinGauge(gauge, values[10], values[9], mode == Mode.POPN_9K ? 24 : 50, 0, mode == Mode.POPN_9K ? 0 : 3, 33);
						} else {
							gauger = new SkinGauge(gauge, values[10], values[9], values[13], values[14], values[15], values[16]);
						}

						gauger.setStarttime(values[17]);
						gauger.setEndtime(values[18]);

						skin.add(gauger);
					}
				}
			}
		});
		addCommandWord(new CommandWord("SRC_GROOVEGAUGE_EX") {
			//SRC定義,index,gr,x,y,w,h,div_x,div_y,cycle,timer,add_x,add_y,parts,animation_type,animation_range,animation_cycle,starttime,endtime
			//JSONスキンと同形式版 表赤、表緑、裏赤、裏緑、EX表赤、EX表緑、EX裏赤、EX裏緑の順にsrc分割
			//parts:粒数 animation_type:アニメーションの種類(0:RANDOM 1:INCLEASE 2:DECLEASE 3:PMS用明滅アニメーション)
			//animation_range:アニメーションする範囲 animation_cycle:アニメーション間隔(ms)
			//starttime, endtime:リザルト用 ゲージが0から曲終了時の値まで増える演出の開始時間・終了時間(ms
			@Override
			public void execute(String[] str) {
				gauger = null;
				int[] values = parseInt(str);
				if (values[2] < imagelist.size && imagelist.get(values[2]) != null) {
					int playside = values[1];
					final int divx = values[7] > 0 ? values[7] : 1;
					final int divy = values[8] > 0 ? values[8] : 1;
					TextureRegion[][] gauge;
					if(values[14] == 3 && divx * divy % 12 == 0) {
						//アニメーションタイプがPMS用明滅アニメーションの場合 表赤、表緑、裏赤、裏緑、EX表赤、EX表緑、EX裏赤、EX裏緑、発光表赤、発光表緑、発光EX表赤、発光EX表緑の順にsrc分割
						gauge = new TextureRegion[(divx * divy) / 12][36];
						final int w = values[5];
						final int h = values[6];
						for (int x = 0; x < divx; x++) {
							for (int y = 0; y < divy; y++) {
								if ((y * divx + x) / 12 < gauge.length) {
										TextureRegion tr = new TextureRegion(
												(Texture) imagelist.get(values[2]), values[3] + w * x / divx,
												values[4] + h * y / divy, w / divx, h / divy);
										
										final int dx = (y * divx + x) / 12;
										final int dy = (y * divx + x) % 12;
										if(dy < 4) {
											gauge[dx][dy] = gauge[dx][dy + 6] = gauge[dx][dy + 12] = gauge[dx][dy + 18] = tr;											
										} else if(dy >= 4 && dy < 8){
											gauge[dx][dy + 20] = gauge[dx][dy + 26] = tr;
										} else if(dy == 8 || dy == 9) {
											gauge[dx][dy - 4] = gauge[dx][dy + 2] = gauge[dx][dy + 8] = gauge[dx][dy + 14] = tr;
										} else {
											gauge[dx][dy + 18] = gauge[dx][dy + 24] = tr;											
										}
								}
							}
						}
					} else {
						gauge = new TextureRegion[(divx * divy) / 8][36];
						final int w = values[5];
						final int h = values[6];
						for (int x = 0; x < divx; x++) {
							for (int y = 0; y < divy; y++) {
								if ((y * divx + x) / 8 < gauge.length) {
									TextureRegion tr = new TextureRegion(
											(Texture) imagelist.get(values[2]), values[3] + w * x / divx,
											values[4] + h * y / divy, w / divx, h / divy);
									
									final int dx = (y * divx + x) / 8;
									final int dy = (y * divx + x) % 8;
									if(dy < 4) {
										gauge[dx][dy] = gauge[dx][dy + 6] = gauge[dx][dy + 12] = gauge[dx][dy + 18] = tr;											
										if(dy < 2) {
											gauge[dx][dy + 4] = gauge[dx][dy + 10] = gauge[dx][dy + 16] = gauge[dx][dy + 22] = tr;
										}
									} else {
										gauge[dx][dy + 20] = gauge[dx][dy + 26] = tr;
										if(dy < 6) {
											gauge[dx][dy + 24] = gauge[dx][dy + 30] = tr;											
										}
									}
								}
							}
						}
					}
					groovex = values[11];
					groovey = values[12];
					if (gauger == null) {
						if(values[13] == 0) {
							gauger = new SkinGauge(gauge, values[10], values[9], mode == Mode.POPN_9K ? 24 : 50, 0, mode == Mode.POPN_9K ? 0 : 3, 33);
						} else {
							gauger = new SkinGauge(gauge, values[10], values[9], values[13], values[14], values[15], values[16]);
						}

						gauger.setStarttime(values[17]);
						gauger.setEndtime(values[18]);

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
					int[] values = parseInt(str);
					gauger.setDestination(values[2], x, y, width, height, values[7],
							values[8], values[9], values[10], values[11], values[12], values[13], values[14],
							values[15], values[16], values[17], values[18], values[19], values[20], readOffset(str, 21));
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
				final File imagefile = LR2SkinLoader.getPath(skinpath, str[7], filemap);
				new PomyuCharaLoader(skin).load(usecim, imagefile,
						0, (values[5] == 1 || values[5] == 2) ? values[5] : 1,
						values[1] * dstw / srcw, dsth - (values[2] + values[4]) * dsth / srch, values[3] * dstw / srcw, values[4] * dsth / srch,
						1, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, values[6]);
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
				final File imagefile = LR2SkinLoader.getPath(skinpath, str[7], filemap);
				new PomyuCharaLoader(skin).load(usecim, imagefile,
						0, (values[5] == 1 || values[5] == 2) ? values[5] : 1,
						values[1] * dstw / srcw, dsth - (values[2] + values[4]) * dsth / srch, values[3] * dstw / srcw, values[4] * dsth / srch,
						2, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, values[6]);
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
					final File imagefile = LR2SkinLoader.getPath(skinpath, str[12], filemap);
					new PomyuCharaLoader(skin).load(usecim, imagefile,
							values[6] + 6, (values[5] == 1 || values[5] == 2) ? values[5] : 1,
							values[1] * dstw / srcw, dsth - (values[2] + values[4]) * dsth / srch, values[3] * dstw / srcw, values[4] * dsth / srch,
							Integer.MIN_VALUE, values[7], values[8], values[9], values[10], values[11]);
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
					final File imagefile = LR2SkinLoader.getPath(skinpath, str[3], filemap);
					PMcharaPart = new PomyuCharaLoader(skin).load(usecim, imagefile,
							values[2] + 1, (values[1] == 1 || values[1] == 2) ? values[1] : 1,
							Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE,
							Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
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
							values[19], values[20], readOffset(str, 21));
				}
			}
		});
		addCommandWord(new CommandWord("STRETCH") {
			@Override
			public void execute(String[] str) {
				int[] values = parseInt(str);
				stretch = values[1];
			}
		});
	}

	protected void loadSkin(Skin skin, Path f, MainState state) throws IOException {
		this.loadSkin(skin, f, state, new IntIntMap());
	}

	protected void loadSkin(Skin skin, Path f, MainState state, IntIntMap option) throws IOException {
		this.loadSkin0(skin, f, state, option);
	}

	private ObjectMap<String, String> filemap = new ObjectMap<String, String>();

	protected S loadSkin(S skin, Path f, MainState state, SkinHeader header, IntIntMap option,
			ObjectMap<String, Object> property) throws IOException {
		this.skin = skin;
		this.state = state;
		for (String key : property.keys()) {
			if (property.get(key) != null) {
				if (property.get(key) instanceof Integer) {
					op.put((Integer) property.get(key), 1);
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

		IntMap<SkinConfig.Offset> offset = new IntMap<>();
		for (CustomOffset of : header.getCustomOffsets()) {
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

	SkinGauge gauger = null;
	int groovex = 0;
	int groovey = 0;
	Mode mode;

	Array<Object> imagesetarray = new Array<Object>();

	int stretch = -1;

	protected void loadSkin0(Skin skin, Path f, MainState state, IntIntMap option) throws IOException {

		try (Stream<String> lines = Files.lines(f, Charset.forName("MS932"))) {
			lines.forEach(line -> {
				try {
					processLine(line, state);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			});
		};

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
		if (values[2] < imagelist.size && imagelist.get(values[2]) != null
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

	public S loadSkin(Path f, MainState state, SkinHeader header, IntIntMap option, SkinConfig.Property property) throws IOException {
		ObjectMap m = new ObjectMap();
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
						final int slashindex = cf.path.lastIndexOf('/');
						File dir = slashindex != -1 ? new File(cf.path.substring(0, slashindex)) : new File(cf.path);
						if (dir.exists() && dir.isDirectory()) {
							Array<File> l = new Array<File>();
							for (File subfile : dir.listFiles()) {
								if (subfile.getPath().toLowerCase().endsWith(ext)) {
									l.add(subfile);
								}
							}
							if (l.size > 0) {
								String filename = l.get((int) (Math.random() * l.size)).getName();
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

		mode = header.getSkinType().getMode();

		return loadSkin(f, state, header, option, m);
	}

	public abstract S loadSkin(Path f, MainState state, SkinHeader header, IntIntMap option, ObjectMap property) throws IOException;

	/**
	 * SkinTypeに対応したLR2SkinCSVLoaderを返す
	 * @param type SkinType
	 * @param src Skinの元解像度
	 * @param c コンフィグ
	 * @return 対応するLR2SkinCSVLoader。存在しない場合はnull
	 */
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

enum CSVCommand implements LR2SkinLoader.Command<LR2SkinCSVLoader> {
	STARTINPUT {
		@Override
		public void execute(LR2SkinCSVLoader loader, String[] str) {
			loader.skin.setInput(Integer.parseInt(str[1]));
		}
	},
	SCENETIME {
		@Override
		public void execute(LR2SkinCSVLoader loader, String[] str) {
			loader.skin.setScene(Integer.parseInt(str[1]));
		}
	},
	FADEOUT {
		@Override
		public void execute(LR2SkinCSVLoader loader, String[] str) {
			loader.skin.setFadeout(Integer.parseInt(str[1]));
		}
	}
	;
}
