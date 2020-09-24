package bms.player.beatoraja.skin.json;

import static bms.player.beatoraja.Resolution.*;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.*;

import bms.player.beatoraja.*;
import bms.player.beatoraja.config.SkinConfigurationSkin;
import bms.player.beatoraja.play.*;
import bms.player.beatoraja.play.bga.BGAProcessor;
import bms.player.beatoraja.result.*;
import bms.player.beatoraja.select.*;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.SkinHeader.CustomItem;
import bms.player.beatoraja.skin.SkinObject.*;
import bms.player.beatoraja.skin.lua.SkinLuaAccessor;
import bms.player.beatoraja.skin.property.*;

/**
 * JSONスキンローダー
 * 
 * @author exch
 */
public class JSONSkinLoader extends SkinLoader {

	protected Resolution dstr;
	protected boolean usecim;
	protected int bgaExpand = -1;

	protected JsonSkin.Skin sk;

	Map<String, SourceData> sourceMap;
	Map<String, SkinTextBitmap.SkinTextBitmapSource> bitmapSourceMap;

	protected final SkinLuaAccessor lua;

	protected ObjectMap<String, String> filemap = new ObjectMap<String, String>();

	protected JsonSkinSerializer serializer;
	
	protected static class SourceData {
		public final String path;
		public boolean loaded = false;
		public Object data;
		
		public SourceData(String path) {
			this.path = path;
		}
	}

	/**
	 * ヘッダの読み込みに使われるコンストラクタ
	 */
	public JSONSkinLoader() {
		this(null);
	}

	public JSONSkinLoader(SkinLuaAccessor lua) {
		this.lua = lua;
		dstr = HD;
		usecim = false;
	}

	/**
	 * スキン本体の読み込みに使われるコンストラクタ
	 * @param state
	 * @param c
	 */
	public JSONSkinLoader(MainState state, Config c) {
		this(state, c, new SkinLuaAccessor(true));
	}

	public JSONSkinLoader(MainState state, Config c, SkinLuaAccessor lua) {
		this.lua = lua;
		dstr = c.getResolution();
		usecim = false;
		bgaExpand = c.getBgaExpand();
		lua.exportMainStateAccessor(state);
		lua.exportUtilities(state);
	}

	public Skin loadSkin(Path p, SkinType type, SkinConfig.Property property) {
		return load(p, type, property);
	}

	public SkinHeader loadHeader(Path p) {
		serializer = new JsonSkinSerializer(lua, path -> getPath(path, filemap));
		SkinHeader header = null;
		try {
			Json json = new Json();
			json.setIgnoreUnknownFields(true);
			serializer.setSerializers(json, null, p);
			sk = json.fromJson(JsonSkin.Skin.class, new FileReader(p.toFile()));
			header = loadJsonSkinHeader(sk, p);
		} catch (FileNotFoundException e) {
			Logger.getGlobal().severe("JSONスキンファイルが見つかりません : " + p.toString());
		}
		return header;
	}

	protected SkinHeader loadJsonSkinHeader(JsonSkin.Skin sk, Path p) {
		SkinHeader header = null;
		try {
			if (sk.type != -1) {
				header = new SkinHeader();
				header.setSkinType(SkinType.getSkinTypeById(sk.type));
				header.setName(sk.name);
				header.setPath(p);
				header.setType(SkinHeader.TYPE_BEATORJASKIN);

				ObjectMap<JsonSkin.Category, SkinHeader.CustomItem[]> categories = new ObjectMap<JsonSkin.Category, SkinHeader.CustomItem[]>();
				for(JsonSkin.Category category : sk.category) {
					categories.put(category, new SkinHeader.CustomItem[category.item.length]);
				}
				
				SkinHeader.CustomOption[] options = new SkinHeader.CustomOption[sk.property.length];
				for (int i = 0; i < sk.property.length; i++) {
					JsonSkin.Property pr = sk.property[i];

					int[] op = new int[pr.item.length];
					String[] name = new String[pr.item.length];
					for (int j = 0; j < pr.item.length; j++) {
						op[j] = pr.item[j].op;
						name[j] = pr.item[j].name;
					}
					options[i] = new SkinHeader.CustomOption(pr.name, op, name, pr.def);
					
					for(JsonSkin.Category category : sk.category) {
						for(int index = 0;index < category.item.length;index++) {
							if(category.item[index].equals(pr.category)) {
								categories.get(category)[index] = options[i];
							}
						}
					}
				}
				header.setCustomOptions(options);

				SkinHeader.CustomFile[] files = new SkinHeader.CustomFile[sk.filepath.length];
				for (int i = 0; i < sk.filepath.length; i++) {
					JsonSkin.Filepath pr = sk.filepath[i];
					files[i] = new SkinHeader.CustomFile(pr.name, p.getParent().toString() + "/" + pr.path, pr.def);
					for(JsonSkin.Category category : sk.category) {
						for(int index = 0;index < category.item.length;index++) {
							if(category.item[index].equals(pr.category)) {
								categories.get(category)[index] = files[i];
							}
						}
					}
				}
				header.setCustomFiles(files);

				int offsetLengthAddition = 0;
				switch (header.getSkinType()) {
					case PLAY_5KEYS:
					case PLAY_7KEYS:
					case PLAY_9KEYS:
					case PLAY_10KEYS:
					case PLAY_14KEYS:
					case PLAY_24KEYS:
					case PLAY_24KEYS_DOUBLE:
					default:
						offsetLengthAddition = 4;
				}
				SkinHeader.CustomOffset[] offsets = new SkinHeader.CustomOffset[sk.offset.length + offsetLengthAddition];
				for (int i = 0; i < sk.offset.length; i++) {
					JsonSkin.Offset pr = sk.offset[i];
					offsets[i] = new SkinHeader.CustomOffset(pr.name, pr.id, pr.x, pr.y, pr.w, pr.h, pr.r, pr.a);
					for(JsonSkin.Category category : sk.category) {
						for(int index = 0;index < category.item.length;index++) {
							if(category.item[index].equals(pr.category)) {
								categories.get(category)[index] = offsets[i];
							}
						}
					}
				}
				switch (header.getSkinType()) {
					case PLAY_5KEYS:
					case PLAY_7KEYS:
					case PLAY_9KEYS:
					case PLAY_10KEYS:
					case PLAY_14KEYS:
					case PLAY_24KEYS:
					case PLAY_24KEYS_DOUBLE:
					default:
						offsets[sk.offset.length + 0] = new SkinHeader.CustomOffset("All offset(%)", SkinProperty.OFFSET_ALL, true, true, true, true, false, false);

						offsets[sk.offset.length + 1] = new SkinHeader.CustomOffset("Notes offset", SkinProperty.OFFSET_NOTES_1P, false, false, false, true, false, false);
						offsets[sk.offset.length + 2] = new SkinHeader.CustomOffset("Judge offset", SkinProperty.OFFSET_JUDGE_1P, true, true, true, true, false, true);
						offsets[sk.offset.length + 3] = new SkinHeader.CustomOffset("Judge Detail offset", SkinProperty.OFFSET_JUDGEDETAIL_1P, true, true, true, true, false, true);
				}
				header.setCustomOffsets(offsets);
				
				SkinHeader.CustomCategory[] category = new SkinHeader.CustomCategory[sk.category.length];
				for(int i = 0;i < sk.category.length;i++) {
					JsonSkin.Category pr = sk.category[i];
					Array<SkinHeader.CustomItem> array = new Array<CustomItem>();
					for(SkinHeader.CustomItem item : categories.get(pr)) {
						if(item != null) {
							array.add(item);
						}
					}
					category[i] = new SkinHeader.CustomCategory(pr.name, array.toArray(SkinHeader.CustomItem.class)); 
				}
				header.setCustomCategories(category);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return header;
	}

	public Skin load(Path p, SkinType type, SkinConfig.Property property) {
		serializer = new JsonSkinSerializer(lua, path -> getPath(path, filemap));
		Skin skin = null;
		SkinHeader header = loadHeader(p);
		if(header == null) {
			return null;
		}
		header.setSkinConfigProperty(property);

		try {
			Json json = new Json();
			json.setIgnoreUnknownFields(true);

			serializer.setSerializers(json, getEnabledOptions(header), p);
			
			filemap = new ObjectMap<>();
			for(SkinHeader.CustomFile customFile : header.getCustomFiles()) {
				if(customFile.getSelectedFilename() != null) {
					filemap.put(customFile.path, customFile.getSelectedFilename());
				}
			}

			lua.exportSkinProperty(header, property, (String path) -> {
				return getPath(p.getParent().toString() + "/" + path, filemap).getPath();
			});

			sk = json.fromJson(JsonSkin.Skin.class, new FileReader(p.toFile()));
			skin = loadJsonSkin(header, sk, type, property, p);
		} catch (FileNotFoundException e) {
			Logger.getGlobal().severe("JSONスキンファイルが見つかりません : " + p.toString());
		} catch (Throwable e) {
			Logger.getGlobal().severe("何らかの原因でJSONスキンファイルの読み込みに失敗しました");
			e.printStackTrace();
		}
		return skin;
	}

	protected HashSet<Integer> getEnabledOptions(SkinHeader header) {
		HashSet<Integer> enabledOptions = new HashSet<>();
		for (SkinHeader.CustomOption customOption : header.getCustomOptions()) {
			enabledOptions.add(customOption.getSelectedOption());	
		}		
		return enabledOptions;
	}

	protected Skin loadJsonSkin(SkinHeader header, JsonSkin.Skin sk, SkinType type, SkinConfig.Property property, Path p){
		Skin skin = null;
		try {
			Resolution src = HD;
			for(Resolution r : Resolution.values()) {
				if(sk.w == r.width && sk.h == r.height) {
					src = r;
					break;
				}
			}

			sourceMap = new HashMap<>();
			bitmapSourceMap = new HashMap<>();

			JsonSkinObjectLoader objectLoader = null;
			switch(type) {
			case MUSIC_SELECT:
				objectLoader = new JsonSelectSkinObjectLoader(this);
				break;
			case PLAY_5KEYS:
			case PLAY_7KEYS:
			case PLAY_9KEYS:
			case PLAY_10KEYS:
			case PLAY_14KEYS:
			case PLAY_24KEYS:
			case PLAY_24KEYS_DOUBLE:
				objectLoader = new JsonPlaySkinObjectLoader(this);
				break;
			case DECIDE:
				objectLoader = new JsonDecideSkinObjectLoader(this);
				break;
			case RESULT:
				break;
			case COURSE_RESULT:
				break;
			case SKIN_SELECT:
				objectLoader = new JsonSkinConfigurationSkinObjectLoader(this);
				break;
			case KEY_CONFIG:
			default:
				objectLoader = new JsonKeyConfigurationSkinObjectLoader(this);
				break;				
			}
			
			if(objectLoader != null) {
				skin = objectLoader.getSkin(src, dstr);
			} else switch(type) {
			case RESULT:
				skin = new MusicResultSkin(src, dstr);
				break;
			case COURSE_RESULT:
				skin = new CourseResultSkin(src, dstr);
				break;
			}
			
			IntIntMap op = new IntIntMap();
			for (SkinHeader.CustomOption option : header.getCustomOptions()) {
				for (int i = 0; i < option.option.length; i++) {
					op.put(option.option[i], option.option[i] == option.getSelectedOption() ? 1 : 0);
				}
			}
			skin.setOption(op);

			IntMap<SkinConfig.Offset> offset = new IntMap<>();
			for (SkinHeader.CustomOffset of : header.getCustomOffsets()) {
				offset.put(of.id, of.getOffset());
			}
			skin.setOffset(offset);

			skin.setFadeout(sk.fadeout);
			skin.setInput(sk.input);
			skin.setScene(sk.scene);
			
			for(JsonSkin.Source source : sk.source) {
				sourceMap.put(source.id, new SourceData(source.path));
			}

			for (JsonSkin.Destination dst : sk.destination) {
				SkinObject obj = null;
				try {
					int id = Integer.parseInt(dst.id);
					if (id < 0) {
						obj = new SkinImage(-id);
					}
				} catch (Exception e) {

				}
				if (obj == null) {
					for (JsonSkin.Image img : sk.image) {
						if (dst.id.equals(img.id)) {
							Object data = getSource(img.src, p);
							
							if(data instanceof SkinSourceMovie) {
								obj = new SkinImage((SkinSourceMovie)data);
							} else if(data instanceof Texture) {
								Texture tex = (Texture) data;
								if (img.len > 1) {
									TextureRegion[] srcimg = getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx,
											img.divy);
									TextureRegion[][] tr = new TextureRegion[img.len][];
									for (int i = 0; i < tr.length; i++) {
										tr[i] = new TextureRegion[srcimg.length / img.len];
										for (int j = 0; j < tr[i].length; j++) {
											tr[i][j] = srcimg[i * tr[i].length + j];
										}
									}
									SkinImage si = new SkinImage(tr, img.timer, img.cycle);
									si.setReferenceID(img.ref);
									obj = si;
								} else {
									obj = new SkinImage(getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
											img.timer, img.cycle);
								}
							} 
							
							if (obj != null && img.act != null) {
								obj.setClickevent(img.act);
								obj.setClickeventType(img.click);
							}								
							break;
						}
					}
					for (JsonSkin.ImageSet imgs : sk.imageset) {
						if (dst.id.equals(imgs.id)) {
							SkinSourceImage[] sources = new SkinSourceImage[imgs.images.length];
							for (int index = 0; index < imgs.images.length; index++) {
								for (JsonSkin.Image img : sk.image) {
									if (img.id.equals(imgs.images[index])) {
										Object data = getSource(img.src, p);
										
										if(data instanceof Texture) {
											Texture tex = (Texture) data;
											sources[index] = new SkinSourceImage(getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
													img.timer, img.cycle);
										} 
										
										break;
									}
								}
							}

							SkinImage si = new SkinImage(sources);
							if (imgs.value != null) {
								si.setReference(imgs.value);
							} else {
								si.setReferenceID(imgs.ref);
							}
							obj = si;
							if (imgs.act != null) {
								obj.setClickevent(imgs.act);
								obj.setClickeventType(imgs.click);
							}
							break;
						}
					}
					for (JsonSkin.Value value : sk.value) {
						if (dst.id.equals(value.id)) {
							Texture tex = getTexture(value.src, p);
							TextureRegion[] images = getSourceImage(tex, value.x, value.y, value.w, value.h, value.divx,
									value.divy);
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

								SkinNumber num = null;
								if(value.value != null) {
									num = new SkinNumber(pn, mn, value.timer, value.cycle, value.digit, value.zeropadding, value.space,
											value.value);
								} else {
									num = new SkinNumber(pn, mn, value.timer, value.cycle, value.digit, value.zeropadding, value.space,
											value.ref);
								}

								num.setAlign(value.align);
								if(value.offset != null) {
									SkinOffset[] offsets = new SkinOffset[value.offset.length];
									for(int i = 0;i < offsets.length;i++) {
										offsets[i] = new SkinOffset();
										offsets[i].x = value.offset[i].x;
										offsets[i].y = value.offset[i].y;
										offsets[i].w = value.offset[i].w;
										offsets[i].h = value.offset[i].h;
									}
									num.setOffsets(offsets);
								}
								obj = num;
							} else {
								int d = images.length % 10 == 0 ? 10 : 11;

								TextureRegion[][] nimages = new TextureRegion[value.divx * value.divy / d][d];
								for (int i = 0; i < d; i++) {
									for (int j = 0; j < value.divx * value.divy / d; j++) {
										nimages[j][i] = images[j * d + i];
									}
								}

								SkinNumber num = null;
								if(value.value != null) {
									num = new SkinNumber(nimages, value.timer, value.cycle, value.digit,
											d > 10 ? 2 : value.padding, value.space, value.value);
								} else {
									num = new SkinNumber(nimages, value.timer, value.cycle, value.digit,
											d > 10 ? 2 : value.padding, value.space, value.ref);
								}
								num.setAlign(value.align);
								if(value.offset != null) {
									SkinOffset[] offsets = new SkinOffset[value.offset.length];
									for(int i = 0;i < offsets.length;i++) {
										offsets[i] = new SkinOffset();
										offsets[i].x = value.offset[i].x;
										offsets[i].y = value.offset[i].y;
										offsets[i].w = value.offset[i].w;
										offsets[i].h = value.offset[i].h;
									}
									num.setOffsets(offsets);
								}
								obj = num;
							}
							break;
						}
					}
					// text
					for (JsonSkin.Text text : sk.text) {
						if (dst.id.equals(text.id)) {
							if (text.ref == SkinProperty.STRING_SEARCHWORD) {
								JsonSkin.Animation a = dst.dst[0];
								Rectangle r = new Rectangle(a.x * ((float)dstr.width / sk.w),
										a.y * ((float)dstr.height / sk.h), a.w * ((float)dstr.width / sk.w),
										a.h * ((float)dstr.height / sk.h));
								((MusicSelectSkin) skin).setSearchTextRegion(r);
							} else {
								obj = createText(text, p);
							}
							break;
						}
					}
					// slider
					for (JsonSkin.Slider img : sk.slider) {
						if (dst.id.equals(img.id)) {
							Texture tex = getTexture(img.src, p);

							if(tex != null) {
								if(img.value != null) {
									obj = new SkinSlider(getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
											img.timer, img.cycle, img.angle, (int) ((img.angle == 1 || img.angle == 3
													? ((float)dstr.width / sk.w) : ((float)dstr.height / sk.h)) * img.range),
											img.value, img.event);
								} else if(img.isRefNum) {
									obj = new SkinSlider(getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
											img.timer, img.cycle, img.angle, (int) ((img.angle == 1 || img.angle == 3
													? ((float)dstr.width / sk.w) : ((float)dstr.height / sk.h)) * img.range),
											img.type, img.min, img.max);
								} else {
									obj = new SkinSlider(getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
											img.timer, img.cycle, img.angle, (int) ((img.angle == 1 || img.angle == 3
													? ((float)dstr.width / sk.w) : ((float)dstr.height / sk.h)) * img.range),
											img.type);
								}								
								((SkinSlider)obj).setChangeable(img.changeable);
							}
							break;
						}
					}
					// graph
					for (JsonSkin.Graph img : sk.graph) {
						if (dst.id.equals(img.id)) {
							if (img.type < 0) {
								Texture tex = getTexture(img.src, p);
								if(tex != null) {
									TextureRegion[] images = getSourceImage(tex, img.x, img.y, img.w, img.h,
											img.divx, img.divy);
									final int len = img.type == -1 ? 11 : 28;
									TextureRegion[][] imgs = new TextureRegion[len][images.length / len];
									for(int j = 0 ;j < len;j++) {
										for(int i = 0 ;i < imgs[j].length;i++) {
											imgs[j][i] = images[i * len + j];
										}
									}

									final int graphtype = img.type == -1 ? 0 : 1;
									obj = new SkinDistributionGraph(graphtype,  imgs, img.timer, img.cycle);									
								}
								
							} else {
								Texture tex = getTexture(img.src, p);
								if(tex != null) {
									if(img.value != null) {
										obj = new SkinGraph(getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
												img.timer, img.cycle, img.value);
									} else if(img.isRefNum) {
										obj = new SkinGraph(getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
												img.timer, img.cycle, img.type, img.min, img.max);
									} else {
										obj = new SkinGraph(getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
												img.timer, img.cycle, img.type);
									}
									((SkinGraph) obj).setDirection(img.angle);									
								}

								break;
							}
						}
					}

					for (JsonSkin.GaugeGraph ggraph : sk.gaugegraph) {
						if (dst.id.equals(ggraph.id)) {
							SkinGaugeGraphObject st = null;
							if(ggraph.color != null) {
								Color[][] colors = new Color[6][4];
								for(int i = 0;i < 24 && i < ggraph.color.length;i++) {
									colors[i / 4][i % 4] = Color.valueOf(ggraph.color[i]);
								}
								st = new SkinGaugeGraphObject(colors);
							} else {
								st = new SkinGaugeGraphObject(ggraph.assistClearBGColor, ggraph.assistAndEasyFailBGColor, ggraph.grooveFailBGColor, ggraph.grooveClearAndHardBGColor, ggraph.exHardBGColor, ggraph.hazardBGColor,
										ggraph.assistClearLineColor, ggraph.assistAndEasyFailLineColor, ggraph.grooveFailLineColor, ggraph.grooveClearAndHardLineColor, ggraph.exHardLineColor, ggraph.hazardLineColor,
										ggraph.borderlineColor, ggraph.borderColor);								
							}
							obj = st;
							break;
						}
					}
					for (JsonSkin.JudgeGraph ggraph : sk.judgegraph) {
						if (dst.id.equals(ggraph.id)) {
							SkinNoteDistributionGraph st = new SkinNoteDistributionGraph(ggraph.type, ggraph.delay, ggraph.backTexOff, ggraph.orderReverse, ggraph.noGap);
							obj = st;
							break;
						}
					}
					for (JsonSkin.BPMGraph ggraph : sk.bpmgraph) {
						if (dst.id.equals(ggraph.id)) {
							SkinBPMGraph st = new SkinBPMGraph(ggraph.delay, ggraph.lineWidth, ggraph.mainBPMColor, ggraph.minBPMColor, ggraph.maxBPMColor, ggraph.otherBPMColor, ggraph.stopLineColor, ggraph.transitionLineColor);
							obj = st;
							break;
						}
					}
					for (JsonSkin.TimingVisualizer tv : sk.timingvisualizer) {
						if (dst.id.equals(tv.id)) {
							SkinTimingVisualizer st = new SkinTimingVisualizer(tv.width, tv.judgeWidthMillis, tv.lineWidth, tv.lineColor, tv.centerColor, tv.PGColor, tv.GRColor, tv.GDColor, tv.BDColor, tv.PRColor, tv.transparent, tv.drawDecay);
							obj = st;
						}
					}

					for (JsonSkin.TimingDistributionGraph td : sk.timingdistributiongraph) {
						if (dst.id.equals(td.id)) {
							SkinTimingDistributionGraph st = new SkinTimingDistributionGraph(td.width, td.lineWidth, td.graphColor, td.averageColor, td.devColor, td.PGColor, td.GRColor, td.GDColor, td.BDColor, td.PRColor, td.drawAverage, td.drawDev);
							obj = st;
						}
					}
					
					// gauge (playskin or resultskin only)
					if (sk.gauge != null && dst.id.equals(sk.gauge.id)) {
						int[][] indexmap = null;
						switch(sk.gauge.nodes.length) {
							case 4:
								indexmap = new int[][]{{0,4,6,10,12,16,18,22,24,28,30,34},{1,5,7,11,13,17,19,23,25,29,31,35},{2,8,14,20,26,32},{3,9,15,21,27,33}};
								break;
							case 8:
								indexmap = new int[][]{{12,16,18,22},{13,17,19,23},{14,20},{15,21},
										{0,4,6,10,24,28,30,34},{1,5,7,11,25,29,31,35},{2,8,26,32},{3,9,27,33}};
								break;
							case 12:
								indexmap = new int[][]{{12,18},{13,19},{14,20},{15,21},
										{0,6,24,30},{1,7,25,31},{2,8,26,32},{3,9,27,33},
										{16,22}, {17,23}, {4, 10, 28, 34}, {5,11,29,35}};
								break;
							case 36:
								break;
						}
						TextureRegion[][] pgaugetex = new TextureRegion[36][];

						int gaugelength = 0;
						for (int i = 0; i < sk.gauge.nodes.length; i++) {
							for (JsonSkin.Image img : sk.image) {
								if (sk.gauge.nodes[i].equals(img.id)) {
									Texture tex = getTexture(img.src, p);
									if(tex != null) {
										if(indexmap != null) {
											for(int index : indexmap[i]) {
												pgaugetex[index] = getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy);
												gaugelength = pgaugetex[index].length;
											}
										} else {
											pgaugetex[i] = getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy);
											gaugelength = pgaugetex[i].length;
										}
									}
									break;
								}
							}

						}

						TextureRegion[][] gaugetex = new TextureRegion[gaugelength][36];
						for (int i = 0; i < 36; i++) {
							for (int j = 0; j < gaugetex.length; j++) {
								gaugetex[j][i] = pgaugetex[i][j];
							}
						}

						obj = new SkinGauge(gaugetex, 0, 0, sk.gauge.parts, sk.gauge.type, sk.gauge.range, sk.gauge.cycle);

						((SkinGauge)obj).setStarttime(sk.gauge.starttime);
						((SkinGauge)obj).setEndtime(sk.gauge.endtime);
					}
										
					if(objectLoader != null) {
						SkinObject sobj = objectLoader.loadSkinObject(skin, sk, dst, p);
						if(sobj != null) {
							obj = sobj;
						}
					}

					//POMYU chara
					for (JsonSkin.PMchara chara : sk.pmchara) {
						if (dst.id.equals(chara.id)) {
							//type 0:プレイ 1:キャラ背景 2:名前画像 3:ハリアイ画像(上半身のみ) 4:ハリアイ画像(全体) 5:キャラアイコン 6:NEUTRAL 7:FEVER 8:GREAT 9:GOOD 10:BAD 11:FEVERWIN 12:WIN 13:LOSE 14:OJAMA 15:DANCE
							File imagefile = getSrcIdPath(chara.src, p);
							if(dst.dst.length > 0 && imagefile != null) {
								int color = chara.color == 2 ? 2 : 1;
								int side = chara.side == 2 ? 2 : 1;
								int[] option = new int[3];
								for(int i = 0; i < option.length; i++) {
									if(i < dst.op.length) option[i] = dst.op[i];
									else option[i] = 0;
								}
								if(chara.type == 0) {
									new PomyuCharaLoader(skin).load(usecim, imagefile, chara.type, color,
											dst.dst[0].x, dst.dst[0].y, dst.dst[0].w, dst.dst[0].h,
											side, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, dst.offset);
								} else if(chara.type >= 1 && chara.type <= 5) {
									SkinImage si = new PomyuCharaLoader(skin).load(usecim, imagefile, chara.type, color,
											Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE,
											Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
									obj = si;
								} else if(chara.type >= 6 && chara.type <= 15) {
									new PomyuCharaLoader(skin).load(usecim, imagefile, chara.type, color,
											dst.dst[0].x, dst.dst[0].y, dst.dst[0].w, dst.dst[0].h,
											Integer.MIN_VALUE, dst.timer, option[0], option[1], option[2], dst.offset);
								}
							}
							break;
						}
					}


				}

				if (obj != null) {
					setDestination(skin, obj, dst);
					skin.add(obj);
				}
			}

			if (sk.skinSelect != null && skin instanceof  SkinConfigurationSkin) {
				SkinConfigurationSkin skinSelect = (SkinConfigurationSkin) skin;
				skinSelect.setCustomOffsetStyle(sk.skinSelect.customOffsetStyle);
				skinSelect.setDefaultSkinType(sk.skinSelect.defaultCategory);
				skinSelect.setSampleBMS(sk.skinSelect.customBMS);
				if (sk.skinSelect.customPropertyCount > 0) {
					skinSelect.setCustomPropertyCount(sk.skinSelect.customPropertyCount);
				} else {
					int count = 0;
					for (JsonSkin.Image image : sk.image) {
						if (SkinPropertyMapper.isSkinCustomizeButton(image.act.getEventId())) {
							int index = SkinPropertyMapper.getSkinCustomizeIndex(image.act.getEventId());
							if (count <= index)
								count = index + 1;
						}
					}
					for (JsonSkin.ImageSet imageSet : sk.imageset) {
						if (SkinPropertyMapper.isSkinCustomizeButton(imageSet.act.getEventId())) {
							int index = SkinPropertyMapper.getSkinCustomizeIndex(imageSet.act.getEventId());
							if (count <= index)
								count = index + 1;
						}
					}
					skinSelect.setCustomPropertyCount(count);
				}
			}

			if (sk.customEvents != null) {
				for (JsonSkin.CustomEvent event : sk.customEvents) {
					skin.addCustomEvent(new CustomEvent(event.id, event.action, event.condition, event.minInterval));
				}
			}

			if (sk.customTimers != null) {
				for (JsonSkin.CustomTimer timer : sk.customTimers) {
					skin.addCustomTimer(new CustomTimer(timer.id, timer.timer));
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
		return skin;
	}

	private void setDestination(Skin skin, SkinObject obj, JsonSkin.Destination dst) {
		JsonSkin.Animation prev = null;
		for (JsonSkin.Animation a : dst.dst) {
			if (prev == null) {
				a.time = (a.time == Integer.MIN_VALUE ? 0 : a.time);
				a.x = (a.x == Integer.MIN_VALUE ? 0 : a.x);
				a.y = (a.y == Integer.MIN_VALUE ? 0 : a.y);
				a.w = (a.w == Integer.MIN_VALUE ? 0 : a.w);
				a.h = (a.h == Integer.MIN_VALUE ? 0 : a.h);
				a.acc = (a.acc == Integer.MIN_VALUE ? 0 : a.acc);
				a.angle = (a.angle == Integer.MIN_VALUE ? 0 : a.angle);
				a.a = (a.a == Integer.MIN_VALUE ? 255 : a.a);
				a.r = (a.r == Integer.MIN_VALUE ? 255 : a.r);
				a.g = (a.g == Integer.MIN_VALUE ? 255 : a.g);
				a.b = (a.b == Integer.MIN_VALUE ? 255 : a.b);
			} else {
				a.time = (a.time == Integer.MIN_VALUE ? prev.time : a.time);
				a.x = (a.x == Integer.MIN_VALUE ? prev.x : a.x);
				a.y = (a.y == Integer.MIN_VALUE ? prev.y : a.y);
				a.w = (a.w == Integer.MIN_VALUE ? prev.w : a.w);
				a.h = (a.h == Integer.MIN_VALUE ? prev.h : a.h);
				a.acc = (a.acc == Integer.MIN_VALUE ? prev.acc : a.acc);
				a.angle = (a.angle == Integer.MIN_VALUE ? prev.angle : a.angle);
				a.a = (a.a == Integer.MIN_VALUE ? prev.a : a.a);
				a.r = (a.r == Integer.MIN_VALUE ? prev.r : a.r);
				a.g = (a.g == Integer.MIN_VALUE ? prev.g : a.g);
				a.b = (a.b == Integer.MIN_VALUE ? prev.b : a.b);
			}
			if(dst.draw != null) {
				skin.setDestination(obj, a.time, a.x, a.y, a.w, a.h, a.acc, a.a, a.r, a.g, a.b, dst.blend, dst.filter,
						a.angle, dst.center, dst.loop, dst.timer, dst.draw);
			} else {
				skin.setDestination(obj, a.time, a.x, a.y, a.w, a.h, a.acc, a.a, a.r, a.g, a.b, dst.blend, dst.filter,
						a.angle, dst.center, dst.loop, dst.timer, dst.op);
			}
			if (dst.mouseRect != null) {
				skin.setMouseRect(obj, dst.mouseRect.x, dst.mouseRect.y, dst.mouseRect.w, dst.mouseRect.h);
			}
			prev = a;
		}

		int[] offsets = new int[dst.offsets.length + 1];
		for(int i = 0; i < dst.offsets.length; i++) {
			offsets[i] = dst.offsets[i];
		}
		offsets[dst.offsets.length] = dst.offset;
		obj.setOffsetID(offsets);
		if (dst.stretch >= 0) {
			obj.setStretch(dst.stretch);
		}
	}
	
	protected Object getSource(String srcid, Path p) {
		if(srcid == null) {
			return null;
		}
		
		final SourceData data = sourceMap.get(srcid);
		if(data == null) {
			return null;
		}
		
		if(data.loaded) {
			return data.data;
		}
		final File imagefile = getPath(p.getParent().toString() + "/" + data.path, filemap);
		if (imagefile.exists()) {
			boolean isMovie = false;
			 for (String mov : BGAProcessor.mov_extension) {
				 if (imagefile.getName().toLowerCase().endsWith(mov)) {
					 try {
					 	SkinSourceMovie mm = new SkinSourceMovie(imagefile.getAbsolutePath());
					 	data.data = mm;
					 	isMovie = true;
					 	break;
					 } catch (Throwable e) {
						Logger.getGlobal().warning("BGAファイル読み込み失敗。" + e.getMessage());
					 	e.printStackTrace();
					 }
				 }
			 }

			if (!isMovie) {
				data.data = getTexture(imagefile.getPath());
			}
		}
		data.loaded = true;
		
		return data.data;
	}

	private Texture getTexture(String srcid, Path p) {
		if(srcid == null) {
			return null;
		}
		
		final SourceData data = sourceMap.get(srcid);
		if(data == null) {
			return null;
		}

		if(data.loaded) {
			return (data.data instanceof Texture) ? (Texture)data.data : null;
		}
		final File imagefile = getPath(p.getParent().toString() + "/" + data.path, filemap);
		if (imagefile.exists()) {
			data.data = getTexture(imagefile.getPath());
		}
		data.loaded = true;
		
		return (Texture) data.data;
	}

	private SkinSource[] getNoteTexture(String[] images, Path p) {
		SkinSource[] noteimages = new SkinSource[images.length];
		for(int i = 0;i < images.length;i++) {
			for (JsonSkin.Image img : sk.image) {
				if (images[i].equals(img.id)) {
					JsonSkin.Image note = img;
					Texture tex = getTexture(note.src, p);
					noteimages[i] = new SkinSourceImage(getSourceImage(tex,  note.x, note.y, note.w,
							note.h, note.divx, note.divy), note.timer, note.cycle);
					break;
				}
			}

		}
		return noteimages;
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

	private Texture getTexture(String path) {
		return getTexture(path, usecim);
	}

	private SkinText createText(JsonSkin.Text text, Path skinPath) {
		for (JsonSkin.Font font : sk.font) {
			if (font.id.equals(text.font)) {
				Path path = skinPath.getParent().resolve(font.path);
				SkinText skinText;
				StringProperty property = text.value;
				if (property == null) {
					property = StringPropertyFactory.getStringProperty(text.ref);
				}
				if (path.toString().toLowerCase().endsWith(".fnt")) {
					if (!bitmapSourceMap.containsKey(font.id)) {
						SkinTextBitmap.SkinTextBitmapSource source = new SkinTextBitmap.SkinTextBitmapSource(path, usecim);
						source.setType(font.type);
						bitmapSourceMap.put(font.id, source);
					}
					skinText = new SkinTextBitmap(bitmapSourceMap.get(font.id), text.size * ((float)dstr.width / sk.w), property);
				} else {
					skinText = new SkinTextFont(path.toString(), 0, text.size, 0, property);
				}
				skinText.setConstantText(text.constantText);
				skinText.setAlign(text.align);
				skinText.setWrapping(text.wrapping);
				skinText.setOverflow(text.overflow);
				skinText.setOutlineColor(parseHexColor(text.outlineColor, Color.WHITE));
				skinText.setOutlineWidth(text.outlineWidth);
				skinText.setShadowColor(parseHexColor(text.shadowColor, Color.WHITE));
				skinText.setShadowOffset(new Vector2(text.shadowOffsetX, text.shadowOffsetY));
				skinText.setShadowSmoothness(text.shadowSmoothness);
				return skinText;
			}
		}
		return null;
	}

	private Color parseHexColor(String hex, Color fallbackColor) {
		try {
			return Color.valueOf(hex);
		} catch (Exception e) {
			return fallbackColor;
		}
	}

	private File getSrcIdPath(String srcid, Path p) {
		if(srcid == null) {
			return null;
		}
		
		final SourceData data = sourceMap.get(srcid);
		if(data == null) {
			return null;
		}
		
		return getPath(p.getParent().toString() + "/" + data.path, filemap);
	}
}
