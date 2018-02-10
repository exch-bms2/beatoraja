package bms.player.beatoraja.skin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.play.*;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Json;

import bms.player.beatoraja.decide.MusicDecideSkin;
import bms.player.beatoraja.result.*;
import bms.player.beatoraja.select.MusicSelectSkin;
import bms.player.beatoraja.select.SkinBar;
import bms.player.beatoraja.select.SkinDistributionGraph;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import static bms.player.beatoraja.Resolution.*;

public class JSONSkinLoader extends SkinLoader{

	private Resolution dstr;
	private boolean usecim;

	private JsonSkin sk;

	Map<String, Texture> texmap;

	Map<String, String> filemap = new HashMap();

	public JSONSkinLoader() {
		dstr = HD;
		usecim = false;
	}

	public JSONSkinLoader(Config c) {
		dstr = c.getResolution();
		usecim = false;
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
				
				SkinHeader.CustomOffset[] offsets = new SkinHeader.CustomOffset[sk.offset.length];
				for (int i = 0; i < sk.offset.length; i++) {
					Offset pr = sk.offset[i];
					offsets[i] = new SkinHeader.CustomOffset(pr.name, pr.id, pr.x, pr.y, pr.w, pr.h, pr.r, pr.a);
				}
				header.setCustomOffsets(offsets);

			}
		} catch (FileNotFoundException e) {
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

			HashSet<Integer> enabledOptions = new HashSet<>();
			if (property != null) {
				for (SkinConfig.Option op : property.getOption()) {
					enabledOptions.add(op.value);
				}
			}
			setSerializers(json, enabledOptions, p);

			filemap = new HashMap<>();
			for (SkinHeader.CustomFile customFile : header.getCustomFiles()) {
				for(SkinConfig.FilePath file : property.getFile()) {
					if (customFile.name.equals(file.name)) {
						filemap.put(customFile.path, file.path);
					}
				}
			}

			sk = json.fromJson(JsonSkin.class, new FileReader(p.toFile()));
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

			Map<Integer, Boolean> op = new HashMap<>();
			for (Property pr : sk.property) {
				int pop = 0;
				for(SkinConfig.Option opt : property.getOption()) {
					if(opt.name.equals(pr.name)) {
						pop = opt.value;
						break;
					}
				}
				for (int i = 0; i < pr.item.length; i++) {
					op.put(pr.item[i].op, pr.item[i].op == pop);
				}
			}
			skin.setOption(op);

			Map<Integer, SkinConfig.Offset> offset = new HashMap<>();
			for (Offset of : sk.offset) {
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
								SkinNumber num = new SkinNumber(pn, mn, value.timer, value.cycle, value.digit, 0,
										value.ref);
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

								SkinNumber num = new SkinNumber(nimages, value.timer, value.cycle, value.digit,
										d > 10 ? 2 : value.padding, value.ref);
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
												text.size);
										st.setAlign(text.align);
										st.setReferenceID(text.ref);
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

							obj = new SkinSlider(getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
									img.timer, img.cycle, img.angle, (int) ((img.angle == 1 || img.angle == 3
											? ((float)dstr.width / sk.w) : ((float)dstr.height / sk.h)) * img.range),
									img.type);
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
								obj = new SkinGraph(getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
										img.timer, img.cycle);
								((SkinGraph) obj).setDirection(img.angle);
								((SkinGraph) obj).setReferenceID(img.type);
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
							SkinNoteDistributionGraph st = new SkinNoteDistributionGraph(ggraph.type);
							obj = st;
							break;
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
						sn.setDstNote2(sk.note.dst2);
						((PlaySkin) skin).setLaneGroupRegion(gregion);
						((PlaySkin) skin).setNoteExpansionRate(sk.note.expansionrate);
						obj = sn;
					}
					// gauge (playskin only)
					if (sk.gauge != null && dst.id.equals(sk.gauge.id)) {
						TextureRegion[][] pgaugetex = new TextureRegion[8][];
						for (int i = 0; i < 8; i++) {
							for (Image img : sk.image) {
								if (sk.gauge.nodes[i].equals(img.id)) {
									Texture tex = getTexture(img.src, p);
									pgaugetex[i] = getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy);
									break;
								}
							}

						}

						TextureRegion[][] gaugetex = new TextureRegion[pgaugetex[0].length][8];
						for (int i = 0; i < 8; i++) {
							for (int j = 0; j < gaugetex.length; j++) {
								gaugetex[j][i] = pgaugetex[i][j];
							}
						}

						obj = new SkinGauge(gaugetex, 0, 0, sk.gauge.parts, sk.gauge.type, sk.gauge.range, sk.gauge.cycle);
					}
					// bga (playskin only)
					if (sk.bga != null && dst.id.equals(sk.bga.id)) {
						obj = new SkinBGA();
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
													img.size);
											text[i].setAlign(img.align);
											setDestination(skin, text[i], sk.songlist.text[i]);
											break;
										}
									}
									break;
								}
							}
						}
						barobj.getText()[0] = text[0];
						barobj.getText()[1] = text[1];

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
				}

				if (obj != null) {
					setDestination(skin, obj, dst);
					skin.add(obj);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return skin;
	}

	private void setDestination(Skin skin, SkinObject obj, Destination dst) {
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
			skin.setDestination(obj, a.time, a.x, a.y, a.w, a.h, a.acc, a.a, a.r, a.g, a.b, dst.blend, dst.filter,
					a.angle, dst.center, dst.loop, dst.timer, dst.op);
			prev = a;
		}

		obj.setOffsetID(dst.offset);
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
		public NoteSet note;
		public Gauge gauge;
		public BGA bga;
		public Judge[] judge = new Judge[0];
		public SongList songlist;

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
	}

	public static class ImageSet {
		public String id;
		public int ref;
		public String[] images = new String[0];
		public int act;
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
	}

	public static class GaugeGraph {
		public String id;
	}

	public static class JudgeGraph {
		public String id;
		public int type;
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
		public int cycle = 33;;
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
		public int[] op = new int[0];
		public Animation[] dst = new Animation[0];
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
				NoteSet.class,
				Gauge.class,
				BGA.class,
				Judge.class,
				SongList.class,
				Destination.class,
				Animation.class,
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
