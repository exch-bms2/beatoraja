package bms.player.beatoraja.skin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.play.*;
import bms.player.beatoraja.play.bga.BGAProcessor;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.ResolutionFileResolver;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.BooleanArray;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;

import bms.player.beatoraja.decide.MusicDecideSkin;
import bms.player.beatoraja.result.*;
import bms.player.beatoraja.select.MusicSelectSkin;
import bms.player.beatoraja.select.SkinBar;
import bms.player.beatoraja.select.SkinDistributionGraph;

import static bms.player.beatoraja.Resolution.*;

public class SkinLoader {

	private Resolution dstr;
	private boolean usecim;

	private JsonSkin sk;

	Map<String, Texture> texmap;

	Map<String, String> filemap = new HashMap();

	public SkinLoader() {
		dstr = HD;
		usecim = false;
	}

	public SkinLoader(Config c) {
		dstr = c.getResolution();
		usecim = false;
	}

	public MusicResultSkin loadResultSkin(Path p, Map property) {
		return (MusicResultSkin) load(p, SkinType.RESULT, property);
	}

	public MusicDecideSkin loadDecideSkin(Path p, Map property) {
		return (MusicDecideSkin) load(p, SkinType.DECIDE, property);
	}

	public MusicSelectSkin loadSelectSkin(Path p, Map property) {
		return (MusicSelectSkin) load(p, SkinType.MUSIC_SELECT, property);
	}

	public PlaySkin loadPlaySkin(Path p, SkinType type, Map property) {
		return (PlaySkin) load(p, type, property);
	}

	public SkinHeader loadHeader(Path p) {
		SkinHeader header = null;
		try {
			Json json = new Json();
			json.setIgnoreUnknownFields(true);
			sk = json.fromJson(JsonSkin.class, new FileReader(p.toFile()));

			if (sk.type != -1) {
				header = new SkinHeader();
				header.setMode(sk.type);
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
					options[i] = new SkinHeader.CustomOption(pr.name, op, name);
				}
				header.setCustomOptions(options);

				SkinHeader.CustomFile[] files = new SkinHeader.CustomFile[sk.filepath.length];
				for (int i = 0; i < sk.filepath.length; i++) {
					Filepath pr = sk.filepath[i];
					files[i] = new SkinHeader.CustomFile(pr.name, p.getParent().toString() + "/" + pr.path);
				}
				header.setCustomFiles(files);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return header;
	}

	public Skin load(Path p, SkinType type, Map property) {
		Skin skin = null;
		try {
			Json json = new Json();
			json.setIgnoreUnknownFields(true);

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

			Map<Integer, Boolean> op = new HashMap<>();
			for (Property pr : sk.property) {
				int pop = 0;
				if (property.get(pr.name) instanceof Integer) {
					pop = (int) property.get(pr.name);
				}

				for (int i = 0; i < pr.item.length; i++) {
					op.put(pr.item[i].op, pr.item[i].op == pop);
				}
			}
			skin.setOption(op);

			filemap = new HashMap<>();
			for (Filepath pr : sk.filepath) {
				if (property.get(pr.name) != null) {
					filemap.put(p.getParent().toString() + "/" + pr.path, property.get(pr.name).toString());
				}
			}

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
									if (text.ref < 0) {
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
								switch (img.type) {
								case -1:
									obj = new SkinDistributionGraph(0);
									break;
								case -2:
									obj = new SkinDistributionGraph(1);
									break;
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

						SkinNote sn = new SkinNote(notes, lnss, mines);
						sn.setLaneRegion(region, scale);
						((PlaySkin) skin).setLaneGroupRegion(gregion);
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

						obj = new SkinGauge(gaugetex, 0, 0);
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
										numbers[i].setAlign(value.align);
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

		obj.setOffsetx(dst.offsetx);
		obj.setOffsety(dst.offsety);
	}

	private Texture getTexture(String srcid, Path p) {
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
	
	public static File getPath(String imagepath, Map<String, String> filemap) {
		File imagefile = new File(imagepath);
		for (String key : filemap.keySet()) {
			if (imagepath.startsWith(key)) {
				String foot = imagepath.substring(key.length());
				imagefile = new File(
						imagepath.substring(0, imagepath.lastIndexOf('*')) + filemap.get(key) + foot);
				// System.out.println(imagefile.getPath());
				imagepath = "";
				break;
			}
		}
		if (imagepath.contains("*")) {
			String ext = imagepath.substring(imagepath.lastIndexOf("*") + 1);
			File imagedir = new File(imagepath.substring(0, imagepath.lastIndexOf('/')));
			if (imagedir.exists() && imagedir.isDirectory()) {
				List<File> l = new ArrayList<File>();
				for (File subfile : imagedir.listFiles()) {
					if (subfile.getPath().toLowerCase().endsWith(ext)) {
						l.add(subfile);
					}
				}
				if (l.size() > 0) {
					imagefile = l.get((int) (Math.random() * l.size()));
				}
			}
		}
		return imagefile;
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

	public static Texture getTexture(String path, boolean usecim) {
		try {
			long modifiedtime = Files.getLastModifiedTime(Paths.get(path)).toMillis() / 1000;
			
			String cim = path.substring(0, path.lastIndexOf('.')) + "__" + modifiedtime + ".cim";
			if (Files.exists(Paths.get(cim))) {
				return new Texture(Gdx.files.internal(cim));
			} else if(usecim){
				Pixmap pixmap = new Pixmap(Gdx.files.internal(path));
				
				try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(path).getParent())) {
					for (Path p : paths) {
						final String filename = p.toString();
						if(filename.startsWith(path.substring(0, path.lastIndexOf('.')) + "__") && filename.endsWith(".cim")) {
							Files.deleteIfExists(p);
							break;
						}
					}
				} catch(Throwable e) {
					e.printStackTrace();
				}
				PixmapIO.writeCIM(Gdx.files.local(cim), pixmap);
				
				Texture tex = new Texture(pixmap);
				pixmap.dispose();
				return tex;
			} else {
				return new Texture(Gdx.files.internal(path));
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
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

		public Property[] property = new Property[0];
		public Filepath[] filepath = new Filepath[0];
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
	}

	public static class PropertyItem {
		public String name;
		public int op;
	}

	public static class Filepath {
		public String name;
		public String path;
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
		public float[] size = new float[0];
		public Destination[] group = new Destination[0];
	}

	public static class Gauge {
		public String id;
		public String[] nodes;
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
		public Destination[] trophy = new Destination[0];
		public Destination[] label = new Destination[0];
	}

	public static class Destination {
		public String id;
		public int blend;
		public int filter;
		public int timer;
		public int loop;
		public int center;
		public int offsetx = -1;
		public int offsety = -1;
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
}
