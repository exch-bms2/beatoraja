package bms.player.beatoraja.skin;

import static bms.player.beatoraja.Resolution.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.*;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.reflect.*;

import bms.player.beatoraja.*;
import bms.player.beatoraja.config.SkinConfigurationSkin;
import bms.player.beatoraja.decide.MusicDecideSkin;
import bms.player.beatoraja.play.*;
import bms.player.beatoraja.result.*;
import bms.player.beatoraja.select.*;
import bms.player.beatoraja.skin.SkinHeader.CustomOffset;
import bms.player.beatoraja.skin.SkinObject.*;
import bms.player.beatoraja.skin.lua.SkinLuaAccessor;
import bms.player.beatoraja.skin.property.*;

public class JSONSkinLoader extends SkinLoader{

	private Resolution dstr;
	private boolean usecim;
	private int bgaExpand = -1;

	protected JsonSkin sk;

	Map<String, Texture> texmap;

	protected final SkinLuaAccessor lua;

	protected ObjectMap<String, String> filemap = new ObjectMap();

	public JSONSkinLoader() {
		lua = null;
		dstr = HD;
		usecim = false;
	}

	public JSONSkinLoader(SkinLuaAccessor lua) {
		this.lua = lua;
		dstr = HD;
		usecim = false;
	}

	public JSONSkinLoader(MainState state, Config c) {
		lua = new SkinLuaAccessor(state);
		dstr = c.getResolution();
		usecim = false;
		bgaExpand = c.getBgaExpand();
	}

	public Skin loadSkin(Path p, SkinType type, SkinConfig.Property property) {
		return load(p, type, property);
	}

	public SkinHeader loadHeader(Path p) {
		SkinHeader header = null;
		try {
			Json json = new Json();
			json.setIgnoreUnknownFields(true);
			setSerializers(json, null, p);
			sk = json.fromJson(JsonSkin.class, new FileReader(p.toFile()));
			header = loadJsonSkinHeader(sk, p);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return header;
	}

	protected SkinHeader loadJsonSkinHeader(JsonSkin sk, Path p) {
		SkinHeader header = null;
		try {
			if (sk.type != -1) {
				header = new SkinHeader();
				header.setSkinType(SkinType.getSkinTypeById(sk.type));
				header.setName(sk.name);
				header.setPath(p);
				header.setType(SkinHeader.TYPE_BEATORJASKIN);

				SkinHeader.CustomOption[] options = new SkinHeader.CustomOption[sk.property.length];
				for (int i = 0; i < sk.property.length; i++) {
					Property pr = sk.property[i];

					int[] op = new int[pr.item.length];
					String[] name = new String[pr.item.length];
					for (int j = 0; j < pr.item.length; j++) {
						op[j] = pr.item[j].op;
						name[j] = pr.item[j].name;
					}
					options[i] = new SkinHeader.CustomOption(pr.name, op, name, pr.def);
				}
				header.setCustomOptions(options);

				SkinHeader.CustomFile[] files = new SkinHeader.CustomFile[sk.filepath.length];
				for (int i = 0; i < sk.filepath.length; i++) {
					Filepath pr = sk.filepath[i];
					files[i] = new SkinHeader.CustomFile(pr.name, p.getParent().toString() + "/" + pr.path, pr.def);
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
						offsetLengthAddition = 4;
				}
				SkinHeader.CustomOffset[] offsets = new SkinHeader.CustomOffset[sk.offset.length + offsetLengthAddition];
				for (int i = 0; i < sk.offset.length; i++) {
					Offset pr = sk.offset[i];
					offsets[i] = new SkinHeader.CustomOffset(pr.name, pr.id, pr.x, pr.y, pr.w, pr.h, pr.r, pr.a);
				}
				switch (header.getSkinType()) {
					case PLAY_5KEYS:
					case PLAY_7KEYS:
					case PLAY_9KEYS:
					case PLAY_10KEYS:
					case PLAY_14KEYS:
					case PLAY_24KEYS:
					case PLAY_24KEYS_DOUBLE:
						offsets[sk.offset.length + 0] = new SkinHeader.CustomOffset("All offset(%)", SkinProperty.OFFSET_ALL, true, true, true, true, false, false);

						offsets[sk.offset.length + 1] = new SkinHeader.CustomOffset("Notes offset", SkinProperty.OFFSET_NOTES_1P, false, false, false, true, false, false);
						offsets[sk.offset.length + 2] = new SkinHeader.CustomOffset("Judge offset", SkinProperty.OFFSET_JUDGE_1P, true, true, true, true, false, true);
						offsets[sk.offset.length + 3] = new SkinHeader.CustomOffset("Judge Detail offset", SkinProperty.OFFSET_JUDGEDETAIL_1P, true, true, true, true, false, true);
				}
				header.setCustomOffsets(offsets);

			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return header;
	}

	public Skin load(Path p, SkinType type, SkinConfig.Property property) {
		Skin skin = null;
		SkinHeader header = loadHeader(p);
		try {
			Json json = new Json();
			json.setIgnoreUnknownFields(true);

			setSerializers(json, getEnabledOptions(header, property), p);
			initFileMap(header, property);

			sk = json.fromJson(JsonSkin.class, new FileReader(p.toFile()));
			skin = loadJsonSkin(header, sk, type, property, p);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return skin;
	}

	protected HashSet<Integer> getEnabledOptions(SkinHeader header, SkinConfig.Property property) {
		HashSet<Integer> enabledOptions = new HashSet<>();
		for (SkinHeader.CustomOption customOption : header.getCustomOptions()) {
			int op = customOption.getDefaultOption();
			for (SkinConfig.Option option : property.getOption()) {
				if (option.name.equals(customOption.name)) {
					if (option.value != OPTION_RANDOM_VALUE) {
						op = option.value;
					} else {
						if (customOption.option.length > 0) {
							op = customOption.option[(int) (Math.random() * customOption.option.length)];
							header.setRandomSelectedOptions(option.name, op);
						}
					}
					break;
				}
			}
			enabledOptions.add(op);
		}
		return enabledOptions;
	}

	protected void initFileMap(SkinHeader header, SkinConfig.Property property) {
		filemap = new ObjectMap<>();
		for (SkinHeader.CustomFile customFile : header.getCustomFiles()) {
			for (SkinConfig.FilePath file : property.getFile()) {
				if (customFile.name.equals(file.name)) {
					if (!file.path.equals("Random")) {
						filemap.put(customFile.path, file.path);
					} else {
						String ext = customFile.path.substring(customFile.path.lastIndexOf("*") + 1);
						if (customFile.path.contains("|")) {
							if (customFile.path.length() > customFile.path.lastIndexOf('|') + 1) {
								ext = customFile.path.substring(customFile.path.lastIndexOf("*") + 1, customFile.path.indexOf('|')) + customFile.path.substring(customFile.path.lastIndexOf('|') + 1);
							} else {
								ext = customFile.path.substring(customFile.path.lastIndexOf("*") + 1, customFile.path.indexOf('|'));
							}
						}
						File dir = new File(customFile.path.substring(0, customFile.path.lastIndexOf('/')));
						if (dir.exists() && dir.isDirectory()) {
							List<File> l = new ArrayList<File>();
							for (File subfile : dir.listFiles()) {
								if (subfile.getPath().toLowerCase().endsWith(ext)) {
									l.add(subfile);
								}
							}
							if (l.size() > 0) {
								String filename = l.get((int) (Math.random() * l.size())).getName();
								filemap.put(customFile.path, filename);
							}
						}
					}
				}
			}
		}
	}

	protected Skin loadJsonSkin(SkinHeader header, JsonSkin sk, SkinType type, SkinConfig.Property property, Path p){
		Skin skin = null;
		try {
			Resolution src = HD;
			for(Resolution r : Resolution.values()) {
				if(sk.w == r.width && sk.h == r.height) {
					src = r;
					break;
				}
			}

			texmap = new HashMap();

			if (type.isPlay()) {
				skin = new PlaySkin(src, dstr);
				((PlaySkin) skin).setClose(sk.close);
				((PlaySkin) skin).setPlaystart(sk.playstart);
				((PlaySkin) skin).setJudgetimer(sk.judgetimer);
				((PlaySkin) skin).setFinishMargin(sk.finishmargin);
			}
			if (type == SkinType.MUSIC_SELECT) {
				skin = new MusicSelectSkin(src, dstr);
			}
			if (type == SkinType.DECIDE) {
				skin = new MusicDecideSkin(src, dstr);
			}
			if (type == SkinType.RESULT) {
				skin = new MusicResultSkin(src, dstr);
			}
			if (type == SkinType.COURSE_RESULT) {
				skin = new CourseResultSkin(src, dstr);
			}
			if (type == SkinType.SKIN_SELECT) {
				skin = new SkinConfigurationSkin(src, dstr);
			}

			IntIntMap op = new IntIntMap();
			for (Property pr : sk.property) {
				int pop = 0;
				for(SkinConfig.Option opt : property.getOption()) {
					if(opt.name.equals(pr.name)) {
						if(opt.value != OPTION_RANDOM_VALUE) {
							pop = opt.value;
						} else {
							if(header.getRandomSelectedOptions(opt.name) >= 0) pop = header.getRandomSelectedOptions(opt.name);
						}
						break;
					}
				}
				for (int i = 0; i < pr.item.length; i++) {
					op.put(pr.item[i].op, pr.item[i].op == pop ? 1 : 0);
				}
			}
			skin.setOption(op);

			IntMap<SkinConfig.Offset> offset = new IntMap<>();
			for (CustomOffset of : header.getCustomOffsets()) {
				for(SkinConfig.Offset off : property.getOffset()) {
					if (off.name.equals(of.name)) {
						offset.put(of.id, off);
						break;
					}
				}
			}
			skin.setOffset(offset);

			skin.setFadeout(sk.fadeout);
			skin.setInput(sk.input);
			skin.setScene(sk.scene);

			for (Destination dst : sk.destination) {
				SkinObject obj = null;
				try {
					int id = Integer.parseInt(dst.id);
					if (id < 0) {
						obj = new SkinImage(-id);
					}
				} catch (Exception e) {

				}
				if (obj == null) {
					for (Image img : sk.image) {
						if (dst.id.equals(img.id)) {
							Texture tex = getTexture(img.src, p);

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
							if (img.act > 0) {
								obj.setClickevent(img.act);
								obj.setClickeventType(img.click);
							}

							break;
						}
					}
					for (ImageSet imgs : sk.imageset) {
						if (dst.id.equals(imgs.id)) {
							TextureRegion[][] tr = new TextureRegion[imgs.images.length][];
							int timer = -1;
							int cycle = -1;
							for (int i = 0; i < imgs.images.length; i++) {
								for (Image img : sk.image) {
									if (img.id.equals(imgs.images[i])) {
										Texture tex = getTexture(img.src, p);
										tr[i] = getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy);
										if (timer == -1) {
											timer = img.timer;
										}
										if (cycle == -1) {
											cycle = img.cycle;
										}
										break;
									}
								}
							}

							SkinImage si = new SkinImage(tr, timer, cycle);
							si.setReferenceID(imgs.ref);
							obj = si;
							if (imgs.act > 0) {
								obj.setClickevent(imgs.act);
								obj.setClickeventType(imgs.click);
							}
							break;
						}
					}
					for (Value value : sk.value) {
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

								IntegerProperty val = null;
								if(value.value != null) {
									val = lua.loadIntegerProperty(value.value);
								}

								SkinNumber num = null;
								if(val != null) {
									num = new SkinNumber(pn, mn, value.timer, value.cycle, value.digit, 0,
											val);
								} else {
									num = new SkinNumber(pn, mn, value.timer, value.cycle, value.digit, 0,
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

								IntegerProperty val = null;
								if(value.value != null) {
									val = lua.loadIntegerProperty(value.value);
								}
								SkinNumber num = null;
								if(val != null) {
									num = new SkinNumber(nimages, value.timer, value.cycle, value.digit,
											d > 10 ? 2 : value.padding, val);
								} else {
									num = new SkinNumber(nimages, value.timer, value.cycle, value.digit,
											d > 10 ? 2 : value.padding, value.ref);
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
					for (Text text : sk.text) {
						if (dst.id.equals(text.id)) {
							for (Font font : sk.font) {
								if (text.font.equals(font.id)) {
									if (text.ref == SkinProperty.STRING_SEARCHWORD) {
										Animation a = dst.dst[0];
										Rectangle r = new Rectangle(a.x * ((float)dstr.width / sk.w),
												a.y * ((float)dstr.height / sk.h), a.w * ((float)dstr.width / sk.w),
												a.h * ((float)dstr.height / sk.h));
										((MusicSelectSkin) skin).setSearchTextRegion(r);
									} else {
										SkinText st = new SkinTextFont(p.getParent().resolve(font.path).toString(), 0,
												text.size, 0, text.ref);
										st.setAlign(text.align);
										obj = st;
										break;
									}
								}
							}
							break;
						}
					}
					// slider
					for (Slider img : sk.slider) {
						if (dst.id.equals(img.id)) {
							Texture tex = getTexture(img.src, p);

							FloatProperty value = null;
							if(img.value != null) {
								value = lua.loadFloatProperty(img.value);
							}
							FloatWriter event = null;
							if(img.event != null) {
								event = lua.loadFloatWriter(img.event);
							}

							if(value != null) {
								obj = new SkinSlider(getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
										img.timer, img.cycle, img.angle, (int) ((img.angle == 1 || img.angle == 3
												? ((float)dstr.width / sk.w) : ((float)dstr.height / sk.h)) * img.range),
										value, event);
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
							break;
						}
					}
					// graph
					for (Graph img : sk.graph) {
						if (dst.id.equals(img.id)) {
							if (img.type < 0) {
								Texture tex = getTexture(img.src, p);
								TextureRegion[][] imgs = null;
								if(tex != null) {
									TextureRegion[] images = getSourceImage(tex, img.x, img.y, img.w, img.h,
											img.divx, img.divy);
									final int len = img.type == -1 ? 11 : 28;
									imgs = new TextureRegion[len][images.length / len];
									for(int j = 0 ;j < len;j++) {
										for(int i = 0 ;i < imgs[j].length;i++) {
											imgs[j][i] = images[i * len + j];
										}
									}
								}

								final int graphtype = img.type == -1 ? 0 : 1;

								if(imgs != null) {
									obj = new SkinDistributionGraph(graphtype,  imgs, img.timer, img.cycle);
								} else {
									obj = new SkinDistributionGraph(graphtype);
								}
							} else {
								Texture tex = getTexture(img.src, p);

								FloatProperty value = null;
								if(img.value != null) {
									value = lua.loadFloatProperty(img.value);
								}

								if(value != null) {
									obj = new SkinGraph(getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
											img.timer, img.cycle, value);
								} else if(img.isRefNum) {
									obj = new SkinGraph(getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
											img.timer, img.cycle, img.type, img.min, img.max);
								} else {
									obj = new SkinGraph(getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
											img.timer, img.cycle, img.type);
								}
								((SkinGraph) obj).setDirection(img.angle);
								break;
							}
						}
					}

					for (GaugeGraph ggraph : sk.gaugegraph) {
						if (dst.id.equals(ggraph.id)) {
							SkinGaugeGraphObject st = new SkinGaugeGraphObject();
							obj = st;
							break;
						}
					}
					for (JudgeGraph ggraph : sk.judgegraph) {
						if (dst.id.equals(ggraph.id)) {
							SkinNoteDistributionGraph st = new SkinNoteDistributionGraph(ggraph.type, ggraph.delay, ggraph.backTexOff, ggraph.orderReverse, ggraph.noGap);
							obj = st;
							break;
						}
					}
					for (BPMGraph ggraph : sk.bpmgraph) {
						if (dst.id.equals(ggraph.id)) {
							SkinBPMGraph st = new SkinBPMGraph(ggraph.delay, ggraph.lineWidth, ggraph.mainBPMColor, ggraph.minBPMColor, ggraph.maxBPMColor, ggraph.otherBPMColor, ggraph.stopLineColor, ggraph.transitionLineColor);
							obj = st;
							break;
						}
					}
					for (TimingVisualizer tv : sk.timingvisualizer) {
						if (dst.id.equals(tv.id)) {
							SkinTimingVisualizer st = new SkinTimingVisualizer(tv.width, tv.judgeWidthMillis, tv.lineWidth, tv.lineColor, tv.centerColor, tv.PGColor, tv.GRColor, tv.GDColor, tv.BDColor, tv.PRColor, tv.transparent, tv.drawDecay);
							obj = st;
						}
					}

					for (TimingDistributionGraph td : sk.timingdistributiongraph) {
						if (dst.id.equals(td.id)) {
							SkinTimingDistributionGraph st = new SkinTimingDistributionGraph(td.width, td.lineWidth, td.graphColor, td.averageColor, td.devColor, td.PGColor, td.GRColor, td.GDColor, td.BDColor, td.PRColor, td.drawAverage, td.drawDev);
							obj = st;
						}
					}
					// note (playskin only)
					if(sk.note != null && dst.id.equals(sk.note.id)) {
						SkinSource[] notes = getNoteTexture(sk.note.note, p);
						SkinSource[][] lns = new SkinSource[10][];
						lns[0] = getNoteTexture(sk.note.lnend, p);
						lns[1] = getNoteTexture(sk.note.lnstart, p);
						lns[2] = getNoteTexture(sk.note.lnbody, p);
						lns[3] = getNoteTexture(sk.note.lnactive, p);
						lns[4] = getNoteTexture(sk.note.hcnend, p);
						lns[5] = getNoteTexture(sk.note.hcnstart, p);
						lns[6] = getNoteTexture(sk.note.hcnbody, p);
						lns[7] = getNoteTexture(sk.note.hcnactive, p);
						lns[8] = getNoteTexture(sk.note.hcndamage, p);
						lns[9] = getNoteTexture(sk.note.hcnreactive, p);
						final SkinSource[][] lnss = new SkinSource[lns[0].length][10];
						for(int i = 0;i < 10;i++) {
							for(int j = 0;j < lns[0].length;j++) {
								lnss[j][i] = lns[i][j];
							}
						}

						SkinSource[] mines = getNoteTexture(sk.note.mine, p);

						Rectangle[] region = new Rectangle[sk.note.dst.length];
						float[] scale = new float[region.length];
						float dx = (float)dstr.width / sk.w;
						float dy = (float)dstr.height / sk.h;
						for (int i = 0; i < region.length; i++) {
							Animation dest = sk.note.dst[i];
							region[i] = new Rectangle(dest.x * dx, dest.y * dy, dest.w * dx, dest.h * dy);
							if(i < sk.note.size.length) {
								scale[i] = sk.note.size[i] * dy;
							} else {
								scale[i] = ((SkinSourceImage)notes[i]).getImages()[0][0].getRegionHeight() * dy;
							}
						}
						Rectangle[] gregion = new Rectangle[sk.note.group.length];
						SkinImage[] lines = new SkinImage[gregion.length];
						for (int i = 0; i < gregion.length; i++) {
							Destination dest = sk.note.group[i];
							gregion[i] = new Rectangle(dest.dst[0].x * dx, dest.dst[0].y * dy, dest.dst[0].w * dx,
									dest.dst[0].h * dy);

							for (Image img : sk.image) {
								if (dest.id.equals(img.id)) {
									Texture tex = getTexture(img.src, p);
									lines[i] = new SkinImage(
											getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
											img.timer, img.cycle);
									setDestination(skin, lines[i], dest);
									break;
								}
							}

						}
						((PlaySkin) skin).setLine(lines);

						if(sk.note.bpm != null) {
							SkinImage[] bpm = new SkinImage[gregion.length];
							for (int i = 0; i < gregion.length && i < sk.note.bpm.length; i++) {
								Destination dest = sk.note.bpm[i];

								for (Image img : sk.image) {
									if (dest.id.equals(img.id)) {
										Texture tex = getTexture(img.src, p);
										bpm[i] = new SkinImage(
												getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
												img.timer, img.cycle);
										setDestination(skin, bpm[i], dest);
										break;
									}
								}
							}
							((PlaySkin) skin).setBPMLine(bpm);
						}

						if(sk.note.stop != null) {
							SkinImage[] stop = new SkinImage[gregion.length];
							for (int i = 0; i < gregion.length && i < sk.note.stop.length; i++) {
								Destination dest = sk.note.stop[i];

								for (Image img : sk.image) {
									if (dest.id.equals(img.id)) {
										Texture tex = getTexture(img.src, p);
										stop[i] = new SkinImage(
												getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
												img.timer, img.cycle);
										setDestination(skin, stop[i], dest);
										break;
									}
								}
							}
							((PlaySkin) skin).setStopLine(stop);
						}

						if(sk.note.time != null) {
							SkinImage[] time = new SkinImage[gregion.length];
							for (int i = 0; i < gregion.length && i < sk.note.time.length; i++) {
								Destination dest = sk.note.time[i];

								for (Image img : sk.image) {
									if (dest.id.equals(img.id)) {
										Texture tex = getTexture(img.src, p);
										time[i] = new SkinImage(
												getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
												img.timer, img.cycle);
										setDestination(skin, time[i], dest);
										break;
									}
								}
							}
							((PlaySkin) skin).setTimeLine(time);
						}

						SkinNote sn = new SkinNote(notes, lnss, mines);
						sn.setLaneRegion(region, scale, skin);
						if(sk.note.dst2 != Integer.MIN_VALUE) {
							sn.setDstNote2((int) Math.round(sk.note.dst2 * dy));							
						}
						((PlaySkin) skin).setLaneRegion(region);
						((PlaySkin) skin).setLaneGroupRegion(gregion);
						((PlaySkin) skin).setNoteExpansionRate(sk.note.expansionrate);
						obj = sn;
					}
					// gauge (playskin or resultskin only)
					if (sk.gauge != null && dst.id.equals(sk.gauge.id)) {
						TextureRegion[][] pgaugetex = new TextureRegion[sk.gauge.nodes.length][];
						for (int i = 0; i < sk.gauge.nodes.length; i++) {
							for (Image img : sk.image) {
								if (sk.gauge.nodes[i].equals(img.id)) {
									Texture tex = getTexture(img.src, p);
									pgaugetex[i] = getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy);
									break;
								}
							}

						}

						TextureRegion[][] gaugetex = new TextureRegion[pgaugetex[0].length][sk.gauge.nodes.length];
						for (int i = 0; i < sk.gauge.nodes.length; i++) {
							for (int j = 0; j < gaugetex.length; j++) {
								gaugetex[j][i] = pgaugetex[i][j];
							}
						}

						obj = new SkinGauge(gaugetex, 0, 0, sk.gauge.parts, sk.gauge.type, sk.gauge.range, sk.gauge.cycle);

						((SkinGauge)obj).setStarttime(sk.gauge.starttime);
						((SkinGauge)obj).setEndtime(sk.gauge.endtime);
					}
					// hidden cover (playskin only)
					for (HiddenCover img : sk.hiddenCover) {
						if (dst.id.equals(img.id)) {
							Texture tex = getTexture(img.src, p);
							obj = new SkinHidden(getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy), img.timer, img.cycle);
							((SkinHidden) obj).setDisapearLine((float) (img.disapearLine * skin.getScaleY()));
							((SkinHidden) obj).setDisapearLineLinkLift(img.isDisapearLineLinkLift);
							int[] offsets = new int[dst.offsets.length + 2];
							for(int i = 0; i < dst.offsets.length; i++) {
								offsets[i] = dst.offsets[i];
							}
							offsets[dst.offsets.length] = OFFSET_LIFT;
							offsets[dst.offsets.length + 1] = OFFSET_HIDDEN_COVER;
							dst.offsets = offsets;
							break;
						}
					}
					// bga (playskin only)
					if (sk.bga != null && dst.id.equals(sk.bga.id)) {
						obj = new SkinBGA(this.bgaExpand);
					}
					// judge (playskin only)
					for (Judge judge : sk.judge) {
						if (dst.id.equals(judge.id)) {
							SkinImage[] images = new SkinImage[judge.images.length];
							SkinNumber[] numbers = new SkinNumber[judge.images.length];
							for (int i = 0; i < judge.images.length; i++) {
								for (Image img : sk.image) {
									if (judge.images[i].id.equals(img.id)) {
										Texture tex = getTexture(img.src, p);
										images[i] = new SkinImage(
												getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
												img.timer, img.cycle);
										setDestination(skin, images[i], judge.images[i]);
										break;
									}
								}

								for (Value value : sk.value) {
									if (judge.numbers[i].id.equals(value.id)) {
										Texture tex = getTexture(value.src, p);
										TextureRegion[] numimages = getSourceImage(tex, value.x, value.y, value.w,
												value.h, value.divx, value.divy);
										int d = numimages.length % 10 == 0 ? 10 : 11;

										TextureRegion[][] nimages = new TextureRegion[value.divx * value.divy / d][d];
										for (int j = 0; j < d; j++) {
											for (int k = 0; k < value.divx * value.divy / d; k++) {
												nimages[k][j] = numimages[k * d + j];
											}
										}
										numbers[i] = new SkinNumber(nimages, value.timer, value.cycle, value.digit,
												d > 10 ? 2 : 0, value.ref);
										numbers[i].setAlign(2);
										if(value.offset != null) {
											SkinOffset[] offsets = new SkinOffset[value.offset.length];
											for(int j = 0;j < offsets.length;j++) {
												offsets[j] = new SkinOffset();
												offsets[j].x = value.offset[j].x;
												offsets[j].y = value.offset[j].y;
												offsets[j].w = value.offset[j].w;
												offsets[j].h = value.offset[j].h;
											}
											numbers[i].setOffsets(offsets);
										}

										for(Animation ani : judge.numbers[i].dst) {
											ani.x -= ani.w * value.digit / 2;
										}
										setDestination(skin, numbers[i], judge.numbers[i]);
										break;
									}
								}

							}
							obj = new SkinJudge(images, numbers, judge.index, judge.shift);

							int region = ((PlaySkin) skin).getJudgeregion();
							if (judge.index >= region) {
								((PlaySkin) skin).setJudgeregion(judge.index + 1);
							}
							break;
						}
					}

					if (sk.songlist != null && dst.id.equals(sk.songlist.id)) {
						SkinBar barobj = new SkinBar(0);

						SkinImage[] onimage = new SkinImage[sk.songlist.liston.length];
						SkinImage[] offimage = new SkinImage[sk.songlist.listoff.length];

						for (int i = 0; i < sk.songlist.liston.length; i++) {
							for (ImageSet imgs : sk.imageset) {
								if (sk.songlist.liston[i].id.equals(imgs.id)) {
									TextureRegion[][] tr = new TextureRegion[imgs.images.length][];
									int timer = -1;
									int cycle = -1;
									for (int j = 0; j < imgs.images.length; j++) {
										for (Image img : sk.image) {
											if (img.id.equals(imgs.images[j])) {
												Texture tex = getTexture(img.src, p);
												tr[j] = getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx,
														img.divy);
												if (timer == -1) {
													timer = img.timer;
												}
												if (cycle == -1) {
													cycle = img.cycle;
												}
												break;
											}
										}
									}

									onimage[i] = new SkinImage(tr, timer, cycle);
									offimage[i] = new SkinImage(tr, timer, cycle);
									setDestination(skin, onimage[i], sk.songlist.liston[i]);
									setDestination(skin, offimage[i], sk.songlist.listoff[i]);
									break;
								}
							}
						}
						barobj.setBarImage(onimage, offimage);

						((MusicSelectSkin) skin).setCenterBar(sk.songlist.center);
						((MusicSelectSkin) skin).setClickableBar(sk.songlist.clickable);

						SkinImage[] lamp = new SkinImage[sk.songlist.lamp.length];
						for (int i = 0; i < sk.songlist.lamp.length; i++) {
							for (Image img : sk.image) {
								if (sk.songlist.lamp[i].id.equals(img.id)) {
									Texture tex = getTexture(img.src, p);
									lamp[i] = new SkinImage(
											getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
											img.timer, img.cycle);
									setDestination(skin, lamp[i], sk.songlist.lamp[i]);
									break;
								}
							}
						}
						barobj.setLamp(lamp);
						SkinImage[] playerlamp = new SkinImage[sk.songlist.playerlamp.length];
						for (int i = 0; i < sk.songlist.playerlamp.length; i++) {
							for (Image img : sk.image) {
								if (sk.songlist.playerlamp[i].id.equals(img.id)) {
									Texture tex = getTexture(img.src, p);
									playerlamp[i] = new SkinImage(
											getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
											img.timer, img.cycle);
									setDestination(skin, playerlamp[i], sk.songlist.playerlamp[i]);
									break;
								}
							}
						}
						barobj.setPlayerLamp(playerlamp);
						SkinImage[] rivallamp = new SkinImage[sk.songlist.rivallamp.length];
						for (int i = 0; i < sk.songlist.rivallamp.length; i++) {
							for (Image img : sk.image) {
								if (sk.songlist.rivallamp[i].id.equals(img.id)) {
									Texture tex = getTexture(img.src, p);
									rivallamp[i] = new SkinImage(
											getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
											img.timer, img.cycle);
									setDestination(skin, rivallamp[i], sk.songlist.rivallamp[i]);
									break;
								}
							}
						}
						barobj.setRivalLamp(rivallamp);

						SkinImage[] trophy = new SkinImage[sk.songlist.trophy.length];
						for (int i = 0; i < sk.songlist.trophy.length; i++) {
							for (Image img : sk.image) {
								if (sk.songlist.trophy[i].id.equals(img.id)) {
									Texture tex = getTexture(img.src, p);
									trophy[i] = new SkinImage(
											getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
											img.timer, img.cycle);
									setDestination(skin, trophy[i], sk.songlist.trophy[i]);
									break;
								}
							}
						}
						barobj.setTrophy(trophy);

						SkinImage[] label = new SkinImage[sk.songlist.label.length];
						for (int i = 0; i < sk.songlist.label.length; i++) {
							for (Image img : sk.image) {
								if (sk.songlist.label[i].id.equals(img.id)) {
									Texture tex = getTexture(img.src, p);
									label[i] = new SkinImage(
											getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
											img.timer, img.cycle);
									setDestination(skin, label[i], sk.songlist.label[i]);
									break;
								}
							}
						}
						barobj.setLabel(label);

						SkinText[] text = new SkinText[sk.songlist.text.length];
						for (int i = 0; i < sk.songlist.text.length; i++) {
							for (Text img : sk.text) {
								if (sk.songlist.text[i].id.equals(img.id)) {
									for (Font font : sk.font) {
										if (img.font.equals(font.id)) {
											text[i] = new SkinTextFont(p.getParent().resolve(font.path).toString(), 0,
													img.size, 0);
											text[i].setAlign(img.align);
											setDestination(skin, text[i], sk.songlist.text[i]);
											break;
										}
									}
									break;
								}
							}
						}
						for(int i = 0; i < barobj.getText().length && i < text.length; i++) {
							barobj.getText()[i] = text[i];
						}

						SkinNumber[] numbers = new SkinNumber[sk.songlist.level.length];
						for (int i = 0; i < sk.songlist.level.length; i++) {
							for (Value value : sk.value) {
								if (sk.songlist.level[i].id.equals(value.id)) {
									Texture tex = getTexture(value.src, p);
									TextureRegion[] numimages = getSourceImage(tex, value.x, value.y, value.w, value.h,
											value.divx, value.divy);
									int d = numimages.length % 10 == 0 ? 10 : 11;

									TextureRegion[][] nimages = new TextureRegion[value.divx * value.divy / d][d];
									for (int j = 0; j < d; j++) {
										for (int k = 0; k < value.divx * value.divy / d; k++) {
											nimages[k][j] = numimages[k * d + j];
										}
									}
									numbers[i] = new SkinNumber(nimages, value.timer, value.cycle, value.digit,
											d > 10 ? 2 : 0, value.ref);
									numbers[i].setAlign(value.align);
									setDestination(skin, numbers[i], sk.songlist.level[i]);
									break;
								}
							}
						}
						barobj.setBarlevel(numbers);

						// graph
						for (Graph img : sk.graph) {
							if (sk.songlist.graph != null && sk.songlist.graph.id.equals(img.id)) {
								if (img.type < 0) {
									Texture tex = getTexture(img.src, p);
									TextureRegion[][] imgs = null;
									if(tex != null) {
										TextureRegion[] images = getSourceImage(tex, img.x, img.y, img.w, img.h,
												img.divx, img.divy);
										final int len = img.type == -1 ? 11 : 28;
										imgs = new TextureRegion[len][images.length / len];
										for(int j = 0 ;j < len;j++) {
											for(int i = 0 ;i < imgs[j].length;i++) {
												imgs[j][i] = images[i * len + j];
											}
										}
									}

									final int graphtype = img.type == -1 ? 0 : 1;

									SkinDistributionGraph bargraph = null;
									if(imgs != null) {
										bargraph = new SkinDistributionGraph(graphtype,  imgs, img.timer, img.cycle);
									} else {
										bargraph = new SkinDistributionGraph(graphtype);
									}

									setDestination(skin, bargraph, sk.songlist.graph);
									barobj.setGraph(bargraph);
								}
							}
						}

						obj = barobj;

					}

					//POMYU chara
					for (PMchara chara : sk.pmchara) {
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
					for (Image image : sk.image) {
						if (SkinPropertyMapper.isSkinCustomizeButton(image.act)) {
							int index = SkinPropertyMapper.getSkinCustomizeIndex(image.act);
							if (count <= index)
								count = index + 1;
						}
					}
					for (ImageSet imageSet : sk.imageset) {
						if (SkinPropertyMapper.isSkinCustomizeButton(imageSet.act)) {
							int index = SkinPropertyMapper.getSkinCustomizeIndex(imageSet.act);
							if (count <= index)
								count = index + 1;
						}
					}
					skinSelect.setCustomPropertyCount(count);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
		return skin;
	}

	private void setDestination(Skin skin, SkinObject obj, Destination dst) {
		BooleanProperty draw = null;
		if(dst.draw != null) {
			draw = lua.loadBooleanProperty(dst.draw);
		}

		Animation prev = null;
		for (Animation a : dst.dst) {
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
			if(draw != null) {
				skin.setDestination(obj, a.time, a.x, a.y, a.w, a.h, a.acc, a.a, a.r, a.g, a.b, dst.blend, dst.filter,
						a.angle, dst.center, dst.loop, dst.timer, draw);
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

	private Texture getTexture(String srcid, Path p) {
		if(srcid == null) {
			return null;
		}
		for (Source src : sk.source) {
			if (srcid.equals(src.id)) {
				if (!texmap.containsKey(src.id)) {
					final File imagefile = getPath(p.getParent().toString() + "/" + src.path, filemap);
					if (imagefile.exists()) {
						boolean isMovie = false;
						// for (String mov : BGAProcessor.mov_extension) {
						// if (imagefile.getName().toLowerCase().endsWith(mov))
						// {
						// try {
						// SkinSourceMovie mm = new
						// SkinSourceMovie(imagefile.getPath());
						// imagelist.add(mm);
						// isMovie = true;
						// break;
						// } catch (Throwable e) {
						// Logger.getGlobal().warning("BGAファイル読み込み失敗。" +
						// e.getMessage());
						// e.printStackTrace();
						// }
						// }
						// }

						if (!isMovie) {
							texmap.put(src.id, getTexture(imagefile.getPath()));
						}
					} else {
						texmap.put(src.id, null);
					}
				}
				return texmap.get(src.id);
			}
		}
		return null;
	}

	private SkinSource[] getNoteTexture(String[] images, Path p) {
		SkinSource[] noteimages = new SkinSource[images.length];
		for(int i = 0;i < images.length;i++) {
			for (Image img : sk.image) {
				if (images[i].equals(img.id)) {
					Image note = img;
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

	public static class JsonSkin {

		public int type = -1;
		public String name;
		public int w = 1280;
		public int h = 720;
		public int fadeout;
		public int input;
		public int scene;
		public int close;
		public int playstart;
		public int judgetimer = 1;
		public int finishmargin = 0;

		public Property[] property = new Property[0];
		public Filepath[] filepath = new Filepath[0];
		public Offset[] offset = new Offset[0];
		public Source[] source = new Source[0];
		public Font[] font = new Font[0];
		public Image[] image = new Image[0];
		public ImageSet[] imageset = new ImageSet[0];
		public Value[] value = new Value[0];
		public Text[] text = new Text[0];
		public Slider[] slider = new Slider[0];
		public Graph[] graph = new Graph[0];
		public GaugeGraph[] gaugegraph = new GaugeGraph[0];
		public JudgeGraph[] judgegraph = new JudgeGraph[0];
		public BPMGraph[] bpmgraph = new BPMGraph[0];
		public TimingVisualizer[] timingvisualizer = new TimingVisualizer[0];
		public TimingDistributionGraph[] timingdistributiongraph = new TimingDistributionGraph[0];
		public NoteSet note;
		public Gauge gauge;
		public HiddenCover[] hiddenCover = new HiddenCover[0];
		public BGA bga;
		public Judge[] judge = new Judge[0];
		public SongList songlist;
		public PMchara[] pmchara = new PMchara[0];
		public SkinConfigurationProperty skinSelect;

		public Destination[] destination;
	}

	public static class Property {
		public String name;
		public PropertyItem[] item = new PropertyItem[0];
		public String def;
	}

	public static class PropertyItem {
		public String name;
		public int op;
	}

	public static class Filepath {
		public String name;
		public String path;
		public String def;
	}

	public static class Offset {
		public String name;
		public int id;
		public boolean x;
		public boolean y;
		public boolean w;
		public boolean h;
		public boolean r;
		public boolean a;
	}

	public static class Source {
		public String id;
		public String path;
	}

	public static class Font {
		public String id;
		public String path;
	}

	public static class Image {
		public String id;
		public String src;
		public int x;
		public int y;
		public int w;
		public int h;
		public int divx = 1;
		public int divy = 1;
		public int timer;
		public int cycle;
		public int len;
		public int ref;
		public int act;
		public int click = 0;
	}

	public static class ImageSet {
		public String id;
		public int ref;
		public String[] images = new String[0];
		public int act;
		public int click = 0;
	}

	public static class Value {
		public String id;
		public String src;
		public int x;
		public int y;
		public int w;
		public int h;
		public int divx = 1;
		public int divy = 1;
		public int timer;
		public int cycle;
		public int align;
		public int digit;
		public int padding;
		public int ref;
		public String value;
		public Value[] offset;
	}

	public static class Text {
		public String id;
		public String font;
		public int size;
		public int align;
		public int ref;
	}

	public static class Slider {
		public String id;
		public String src;
		public int x;
		public int y;
		public int w;
		public int h;
		public int divx = 1;
		public int divy = 1;
		public int timer;
		public int cycle;
		public int angle;
		public int range;
		public int type;
		public String value;
		public String event;
		public boolean isRefNum = false;
		public int min = 0;
		public int max = 0;
	}

	public static class Graph {
		public String id;
		public String src;
		public int x;
		public int y;
		public int w;
		public int h;
		public int divx = 1;
		public int divy = 1;
		public int timer;
		public int cycle;
		public int angle = 1;
		public int type;
		public String value;
		public boolean isRefNum = false;
		public int min = 0;
		public int max = 0;
	}

	public static class GaugeGraph {
		public String id;
	}

	public static class JudgeGraph {
		public String id;
		public int type;
		public int backTexOff = 0;
		public int delay = 500;
		public int orderReverse = 0;
		public int noGap = 0;
	}

	public static class BPMGraph {
		public String id;
		public int delay = 0;
		public int lineWidth = 2;
		public String mainBPMColor = "00ff00";
		public String minBPMColor = "0000ff";
		public String maxBPMColor = "ff0000";
		public String otherBPMColor = "ffff00";
		public String stopLineColor = "ff00ff";
		public String transitionLineColor = "7f7f7f";
	}

	public static class TimingVisualizer {
		public String id;
		public int width = 301;
		public int judgeWidthMillis = 150;
		public int lineWidth = 1;
		public String lineColor = "00FF00FF";
		public String centerColor = "FFFFFFFF";
		public String PGColor = "000088FF";
		public String GRColor = "008800FF";
		public String GDColor = "888800FF";
		public String BDColor = "880000FF";
		public String PRColor = "000000FF";
		public int transparent = 0;
		public int drawDecay = 1;
	}

	public static class TimingDistributionGraph {
		public String id;
		public int width = 301;
		public int lineWidth = 1;
		public String graphColor = "00FF00FF";
		public String averageColor = "FFFFFFFF";
		public String devColor = "FFFFFFFF";
		public String PGColor = "000088FF";
		public String GRColor = "008800FF";
		public String GDColor = "888800FF";
		public String BDColor = "880000FF";
		public String PRColor = "000000FF";
		public int drawAverage = 1;
		public int drawDev = 1;
	}

	public static class NoteSet {
		public String id;
		public String[] note = new String[0];
		public String[] lnstart = new String[0];
		public String[] lnend = new String[0];
		public String[] lnbody = new String[0];
		public String[] lnactive = new String[0];
		public String[] hcnstart = new String[0];
		public String[] hcnend = new String[0];
		public String[] hcnbody = new String[0];
		public String[] hcnactive = new String[0];
		public String[] hcndamage = new String[0];
		public String[] hcnreactive = new String[0];
		public String[] mine = new String[0];
		public String[] hidden = new String[0];
		public String[] processed = new String[0];
		public Animation[] dst = new Animation[0];
		public int dst2 = Integer.MIN_VALUE;
		public int[] expansionrate = {100,100};
		public float[] size = new float[0];
		public Destination[] group = new Destination[0];
		public Destination[] bpm = new Destination[0];
		public Destination[] stop = new Destination[0];
		public Destination[] time = new Destination[0];
	}

	public static class Gauge {
		public String id;
		public String[] nodes;
		public int parts = 50;
		public int type;
		public int range = 3;
		public int cycle = 33;
		public int starttime = 0;
		public int endtime = 500;
	}

	public static class HiddenCover {
		public String id;
		public String src;
		public int x;
		public int y;
		public int w;
		public int h;
		public int divx = 1;
		public int divy = 1;
		public int timer;
		public int cycle;
		public int disapearLine = -1;
		public boolean isDisapearLineLinkLift = true;
	}

	public static class BGA {
		public String id;
	}

	public static class Judge {
		public String id;
		public int index;
		public Destination[] images = new Destination[0];
		public Destination[] numbers = new Destination[0];
		public boolean shift;
	}

	public static class SongList {
		public String id;
		public int center;
		public int[] clickable = new int[0];
		public Destination[] listoff = new Destination[0];
		public Destination[] liston = new Destination[0];
		public Destination[] text = new Destination[0];
		public Destination[] level = new Destination[0];
		public Destination[] lamp = new Destination[0];
		public Destination[] playerlamp = new Destination[0];
		public Destination[] rivallamp = new Destination[0];
		public Destination[] trophy = new Destination[0];
		public Destination[] label = new Destination[0];
		public Destination graph;
	}

	public static class Destination {
		public String id;
		public int blend;
		public int filter;
		public int timer;
		public int loop;
		public int center;
		public int offset;
		public int[] offsets = new int[0];
		public int stretch = -1;
		public int[] op = new int[0];
		public String draw;
		public Animation[] dst = new Animation[0];
		public Rect mouseRect;
	}

	public static class Rect {
		public int x;
		public int y;
		public int w;
		public int h;
	}

	public static class Animation {
		public int time = Integer.MIN_VALUE;

		public int x = Integer.MIN_VALUE;
		public int y = Integer.MIN_VALUE;
		public int w = Integer.MIN_VALUE;
		public int h = Integer.MIN_VALUE;

		public int acc = Integer.MIN_VALUE;

		public int a = Integer.MIN_VALUE;
		public int r = Integer.MIN_VALUE;
		public int g = Integer.MIN_VALUE;
		public int b = Integer.MIN_VALUE;

		public int angle = Integer.MIN_VALUE;

	}

	public static class PMchara {
		public String id;
		public String src;
		public int color = 1;
		public int type = Integer.MIN_VALUE;
		public int side = 1;
	}

	public static class SkinConfigurationProperty {
		public String[] customBMS;
		public int defaultCategory = 0;
		public int customPropertyCount = -1;
		public int customOffsetStyle = 0;
	}

	private File getSrcIdPath(String srcid, Path p) {
		if(srcid == null) {
			return null;
		}
		for (Source src : sk.source) {
			if (srcid.equals(src.id)) {
				if (!texmap.containsKey(src.id)) {
					return getPath(p.getParent().toString() + "/" + src.path, filemap);
				}
			}
		}
		return null;
	}

	private void setSerializers(Json json, HashSet<Integer> enabledOptions, Path path) {
		Class[] classes = {
				Property.class,
				Filepath.class,
				Offset.class,
				Source.class,
				Font.class,
				Image.class,
				ImageSet.class,
				Value.class,
				Text.class,
				Slider.class,
				Graph.class,
				GaugeGraph.class,
				JudgeGraph.class,
				TimingVisualizer.class,
				TimingDistributionGraph.class,
				NoteSet.class,
				Gauge.class,
				BGA.class,
				Judge.class,
				SongList.class,
				Destination.class,
				Animation.class,
				SkinConfigurationProperty.class,
		};
		for (Class c : classes) {
			json.setSerializer(c, new ObjectSerializer<>(enabledOptions, path));
		}

		Class[] array_classes = {
				Property[].class,
				Filepath[].class,
				Offset[].class,
				Source[].class,
				Font[].class,
				Image[].class,
				ImageSet[].class,
				Value[].class,
				Text[].class,
				Slider[].class,
				Graph[].class,
				GaugeGraph[].class,
				JudgeGraph[].class,
				TimingVisualizer[].class,
				TimingDistributionGraph[].class,
				Judge[].class,
				Destination[].class,
				Animation[].class,
		};
		for (Class c : array_classes) {
			json.setSerializer(c, new ArraySerializer<>(enabledOptions, path));
		}
	}

	private abstract class Serializer<T> extends Json.ReadOnlySerializer<T> {

		HashSet<Integer> options;
		Path path;

		public Serializer(HashSet<Integer> op, Path path) {
			this.options = op != null ? op : new HashSet<>();
			this.path = path;
		}

		// test "if" as follows:
		// 901 -> 901 enabled
		// [901, 911] -> 901 enabled && 911 enabled
		// [[901, 902], 911] -> (901 || 902) && 911
		// -901 -> 901 disabled
		protected boolean testOption(JsonValue ops) {
			if (ops == null) {
				return true;
			} else if (ops.isNumber()) {
				return testNumber(ops.asInt());
			} else if (ops.isArray()) {
				boolean enabled = true;
				for (int j = 0; j < ops.size; j++) {
					JsonValue ops2 = ops.get(j);
					if (ops2.isNumber()) {
						enabled = testNumber(ops2.asInt());
					} else if (ops2.isArray()) {
						boolean enabled_sub = false;
						for (int k = 0; k < ops2.size; k++) {
							JsonValue ops3 = ops2.get(k);
							if (ops3.isNumber() && testNumber(ops3.asInt())) {
								enabled_sub = true;
								break;
							}
						}
						enabled = enabled_sub;
					} else {
						enabled = false;
					}
					if (!enabled)
						break;
				}
				return enabled;
			} else {
				return false;
			}
		}

		private boolean testNumber(int op) {
			return op >= 0 ? options.contains(op) : !options.contains(-op);
		}
	}

	private class ObjectSerializer<T> extends Serializer<T> {

		public ObjectSerializer(HashSet<Integer> op, Path path) {
			super(op, path);
		}

		public T read(Json json, JsonValue jsonValue, Class cls) {
			if (jsonValue.isArray()) {
				// conditional branch
				// take first clause satisfying its conditions
				JsonValue val = null;
				for (int i = 0; i < jsonValue.size; i++) {
					JsonValue branch = jsonValue.get(i);
					if (testOption(branch.get("if"))) {
						val = branch.get("value");
						break;
					}
				}
				return (T)json.readValue(cls, val);
			} else if (jsonValue.isObject() && jsonValue.has("include")) {
				Json subJson = new Json();
				subJson.setIgnoreUnknownFields(true);
				File file = getPath(path.getParent().toString() + "/" + jsonValue.get("include").asString(), filemap);
				if (file.exists()) {
					setSerializers(subJson, this.options, file.toPath());
					try {
						return (T)subJson.fromJson(cls, new FileReader(file));
					} catch (FileNotFoundException e) {
					}
				}
				return null;
			} else {
				// literal
				T instance = null;
				try {
					instance = (T)ClassReflection.newInstance(cls);
				} catch (ReflectionException e) {
					e.printStackTrace();
					return null;
				}
				try {
					Field[] fields = ClassReflection.getFields(cls);
					for (JsonValue child = jsonValue.child; child != null; child = child.next) {
						for (Field field : fields) {
							if (field.getName().equals(child.name)) {
								field.set(instance, json.readValue(field.getType(), child));
								break;
							}
						}
					}
				} catch (ReflectionException e) {
				} catch (NullPointerException e) {
				}
				return instance;
			}
		}
	}

	private class ArraySerializer<T> extends Serializer<T[]> {

		public ArraySerializer(HashSet<Integer> op, Path path) {
			super(op, path);
		}

		public T[] read(Json json, JsonValue jsonValue, Class cls) {
			Class componentClass = cls.getComponentType();
			ArrayList<T> items = new ArrayList<T>();
			try {
				if (jsonValue.isArray()) {
					for (int i = 0; i < jsonValue.size; i++) {
						JsonValue item = jsonValue.get(i);
						if (item.isObject() && item.has("if") && (item.has("value") || item.has("values"))) {
							// conditional item(s)
							// add item(s) to array if conditions are satisfied
							JsonValue value = item.get("value");
							JsonValue values = item.get("values");
							if (testOption(item.get("if"))) {
								if (value != null) {
									T obj = (T) json.readValue(componentClass, value);
									items.add(obj);
								}
								if (values != null) {
									T[] objs = (T[]) json.readValue(cls, values);
									Collections.addAll(items, objs);
								}
							}
						} else if (item.isObject() && item.has("include")) {
							// array include (inside)
							includeArray(json, item, cls, items);
						} else {
							// single item
							T obj = (T)json.readValue(componentClass, item);
							items.add(obj);
						}
					}
				} else if (jsonValue.isObject() && jsonValue.has("include")) {
					// array include (outside)
					includeArray(json, jsonValue, cls, items);
				} else if (jsonValue.isObject()) {
					// regard as a single item
					T obj = (T)json.readValue(componentClass, jsonValue);
					items.add(obj);
				}
			} catch (NullPointerException e) {
			}
			Object array = Array.newInstance(componentClass, items.size());
			for (int i=0; i<items.size(); i++) {
				Array.set(array, i, items.get(i));
			}
			return (T[])array;
		}

		private void includeArray(Json json, JsonValue jsonValue, Class cls, ArrayList<T> items) {
			Json subJson = new Json();
			subJson.setIgnoreUnknownFields(true);
			File file = getPath(path.getParent().toString() + "/" + jsonValue.get("include").asString(), filemap);
			if (file.exists()) {
				setSerializers(subJson, this.options, file.toPath());
				try {
					T[] array = (T[])subJson.fromJson(cls, new FileReader(file));
					Collections.addAll(items, array);
				} catch (FileNotFoundException e) {
				}
			}
		}
	}
}
