package bms.player.beatoraja.skin;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.*;

import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.play.PlaySkin;
import bms.player.beatoraja.play.SkinBGA;
import bms.player.beatoraja.play.SkinGauge;
import bms.player.beatoraja.play.SkinNote;
import com.badlogic.gdx.assets.loaders.resolvers.ResolutionFileResolver;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Json;

import bms.player.beatoraja.decide.MusicDecideSkin;
import bms.player.beatoraja.result.*;

public class SkinLoader {
	
	private Rectangle dstr;

	private JsonSkin sk;

	Map<Integer, Texture> texmap;

	public SkinLoader() {
		this(Resolution.RESOLUTION[1]);
	}
	
	public SkinLoader(Rectangle r) {
		dstr = r;
	}

	public MusicResultSkin loadResultSkin(Path p) {
		return (MusicResultSkin) load(p, 7);		
	}

	public MusicDecideSkin loadDecideSkin(Path p) {
		return (MusicDecideSkin) load(p, 6);
	}

	public PlaySkin loadPlaySkin(Path p, int skinmode) {
		return (PlaySkin) load(p, skinmode);
	}

	public LR2SkinHeader loadHeader(Path p) {
		LR2SkinHeader header = null;
		try {
			Json json = new Json();
			json.setIgnoreUnknownFields(true);
			sk = json.fromJson(JsonSkin.class, new FileReader(p.toFile()));

			if(sk.type != -1) {
				header = new LR2SkinHeader();
				header.setMode(sk.type);
				header.setName(sk.name);
				header.setPath(p);
				header.setType(LR2SkinHeader.TYPE_BEATORJASKIN);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return header;
	}

	public Skin load(Path p, int type) {
		Skin skin = null;
		try {
			Json json = new Json();
			json.setIgnoreUnknownFields(true);
			
			sk = json.fromJson(JsonSkin.class, new FileReader(p.toFile()));

			texmap = new HashMap();

			if(type >= 0 && type < 5) {
				skin = new PlaySkin(sk.w, sk.h, dstr.width, dstr.height);
				((PlaySkin)skin).setClose(sk.close);
				((PlaySkin)skin).setPlaystart(sk.playstart);
			}
			if(type == 6) {
				skin = new MusicDecideSkin(sk.w, sk.h, dstr.width, dstr.height);				
			}
			if(type == 7) {
				skin = new MusicResultSkin(sk.w, sk.h, dstr.width, dstr.height);				
			}
			skin.setFadeout(sk.fadeout);
			skin.setInput(sk.input);
			skin.setScene(sk.scene);
			
			for (Destination dst : sk.destination) {
				SkinObject obj = null;
				if (dst.id < 0) {
					obj = new SkinImage(-dst.id);
				} else {
					for (Image img : sk.image) {
						if (dst.id == img.id) {
							Texture tex = getTexture(img.src, p);

							if(img.len > 1) {
								TextureRegion[] srcimg = getSourceImage(tex,  img.x, img.y, img.w,
										img.h, img.divx, img.divy);
								TextureRegion[][] tr = new TextureRegion[img.len][];
								for(int i = 0;i < tr.length;i++) {
									tr[i] = new TextureRegion[srcimg.length / img.len];
									for(int j = 0;j < tr[i].length;j++) {
										tr[i][j] = srcimg[i * tr[i].length + j];
									}
								}
								SkinImage si = new SkinImage(tr, img.timer, img.cycle);
								si.setReferenceID(img.ref);
								obj = si;
							} else {
								obj = new SkinImage(getSourceImage(tex,  img.x, img.y, img.w,
										img.h, img.divx, img.divy), img.timer, img.cycle);
							}
                            if(img.act > 0) {
                                obj.setClickevent(img.act);
                            }

                            break;
						}
					}
					for (ImageSet imgs : sk.imageset) {
						if (dst.id == imgs.id) {
							TextureRegion[][] tr = new TextureRegion[imgs.images.length][];
							int timer = -1;
							int cycle = -1;
							for(int i = 0;i < imgs.images.length;i++) {
								for(Image img : sk.image) {
									if(img.id == imgs.images[i]) {
										Texture tex = getTexture(img.src, p);
										tr[i] = getSourceImage(tex,  img.x, img.y, img.w,
												img.h, img.divx, img.divy);
										if(timer == -1) {
											timer = img.timer;
										}
										if(cycle == -1) {
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
						if (dst.id == value.id) {
							Texture tex = getTexture(value.src, p);
							TextureRegion[] images = getSourceImage(tex,  value.x, value.y, value.w,
									value.h, value.divx, value.divy);
							if (images.length % 24 == 0) {
								TextureRegion[][] pn = new TextureRegion[images.length / 24][];
								TextureRegion[][] mn = new TextureRegion[images.length / 24][];

								for(int j = 0;j < pn.length;j++) {
									pn[j] = new TextureRegion[12];
									mn[j] = new TextureRegion[12];

									for (int i = 0; i < 12; i++) {
										pn[j][i] = images[j * 24 + i];
										mn[j][i] = images[j * 24 + i + 12];
									}
								}
								SkinNumber num = new SkinNumber(pn, mn, value.timer, value.cycle, value.digit, 0, value.ref);
								num.setAlign(value.align);
								obj = num;
							} else {
								int d = images.length % 10 == 0 ? 10 :11;

								TextureRegion[][] nimages = new TextureRegion[value.divx * value.divy / d][d];
								for (int i = 0; i < d; i++) {
									for (int j = 0; j < value.divx * value.divy / d; j++) {
										nimages[j][i] = images[j * d + i];
									}
								}

								SkinNumber num = new SkinNumber(nimages, value.timer, value.cycle, value.digit, d > 10 ? 2 : 0, value.ref);
								num.setAlign(value.align);
								obj = num;
							}
							break;
						}
					}
					// text
					for (Text text : sk.text) {
						if (dst.id == text.id) {
							for (Font font : sk.font) {
								if (text.font == font.id) {
									SkinText st = new SkinText(p.getParent().resolve(font.path).toString(), 0,
											text.size);
									st.setAlign(text.align);
									st.setReferenceID(text.ref);
									obj = st;
									break;
								}
							}
							break;
						}
					}
					// slider
					for (Slider img : sk.slider) {
						if (dst.id == img.id) {
							Texture tex = getTexture(img.src, p);

							obj = new SkinSlider(getSourceImage(tex,  img.x, img.y, img.w,
									img.h, img.divx, img.divy), img.timer, img.cycle, img.angle,
									(int) ((img.angle == 1 || img.angle == 3 ? (dstr.width / sk.w) : (dstr.height / sk.h)) * img.range), img.type);
							break;
						}
					}
					// graph
					for (Graph img : sk.graph) {
						if (dst.id == img.id) {
							Texture tex = getTexture(img.src, p);
							obj = new SkinGraph(getSourceImage(tex,  img.x, img.y, img.w,
									img.h, img.divx, img.divy), img.timer, img.cycle);
							((SkinGraph)obj).setDirection(img.angle);
							((SkinGraph)obj).setReferenceID(img.type);
							break;
						}
					}

					for (GaugeGraph ggraph : sk.gaugegraph) {
						if (dst.id == ggraph.id) {
							SkinGaugeGraphObject st = new SkinGaugeGraphObject();
							obj = st;
							break;
						}
					}
					for (JudgeGraph ggraph : sk.judgegraph) {
						if (dst.id == ggraph.id) {
							SkinNoteDistributionGraph st = new SkinNoteDistributionGraph(ggraph.type);
							obj = st;
							break;
						}
					}
					// note
					if(sk.note != null && dst.id == sk.note.id) {
						TextureRegion[][] notes = getNoteTexture(sk.note.note, p);
						TextureRegion[][][] lns = new TextureRegion[10][][];
						lns[0] = getNoteTexture(sk.note.lnstart, p);
						lns[1] = getNoteTexture(sk.note.lnend, p);
						lns[2] = getNoteTexture(sk.note.lnbody, p);
						lns[3] = getNoteTexture(sk.note.lnactive, p);
						lns[4] = getNoteTexture(sk.note.hcnstart, p);
						lns[5] = getNoteTexture(sk.note.hcnend, p);
						lns[6] = getNoteTexture(sk.note.hcnbody, p);
						lns[7] = getNoteTexture(sk.note.hcnactive, p);
						lns[8] = getNoteTexture(sk.note.hcndamage, p);
						lns[9] = getNoteTexture(sk.note.hcnreactive, p);
						TextureRegion[][] mines = getNoteTexture(sk.note.mine, p);

						Rectangle[] region = new Rectangle[sk.note.dst.length];
						for(int i = 0;i < region.length;i++) {
							Animation dest = sk.note.dst[i];
							region[i] = new Rectangle(dest.x, dest.y, dest.w, dest.h);
						}
						Rectangle[] gregion = new Rectangle[sk.note.group.length];
						SkinImage[] lines = new SkinImage[gregion.length];
						for(int i = 0;i < gregion.length;i++) {
							Destination dest = sk.note.group[i];
							gregion[i] = new Rectangle(dest.dst[0].x, dest.dst[0].y, dest.dst[0].w, dest.dst[0].h);

							Image img = sk.note.line[i];
							Texture tex = getTexture(img.src, p);
							lines[i] = new SkinImage(getSourceImage(tex,  img.x, img.y, img.w,
									img.h, img.divx, img.divy), img.timer, img.cycle);
							setDestination(skin, lines[i], dest);
						}
						((PlaySkin)skin).setLine(lines);

						SkinNote sn = new SkinNote(notes, lns, mines, 0, notes[0][0].getRegionHeight());
						sn.setLaneRegion(region);
						((PlaySkin)skin).setLaneGroupRegion(gregion);
						obj = sn;
					}
					// gauge
					if(sk.gauge != null && dst.id == sk.gauge.id) {
						TextureRegion[][] pgaugetex = new TextureRegion[8][];
						for(int i = 0;i < 8;i++) {
							Image img = sk.gauge.nodes[i];
							Texture tex = getTexture(img.src, p);
							pgaugetex[i] = getSourceImage(tex,  img.x, img.y, img.w,
									img.h, img.divx, img.divy);
						}

						TextureRegion[][] gaugetex = new TextureRegion[pgaugetex[0].length][8];
						for(int i = 0;i < 8;i++) {
							for(int j = 0;j < gaugetex.length;j++) {
								gaugetex[j][i] = pgaugetex[i][j];
							}
						}

						obj = new SkinGauge(gaugetex, 0, 0);
					}
					// bga
					if(sk.bga != null && dst.id == sk.bga.id) {
						obj = new SkinBGA();
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
			if(prev == null) {
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
			skin.setDestination(obj, a.time, a.x, a.y, a.w, a.h, a.acc, a.a, a.r, a.g, a.b, dst.blend,
					dst.filter, a.angle, dst.center, dst.loop, dst.timer, dst.op);
			prev = a;
		}
		obj.setOffsetx(dst.offsetx);
		obj.setOffsety(dst.offsety);
	}

	private Texture getTexture(int srcid, Path p) {
		for (Source src : sk.source) {
			if (srcid == src.id) {
				if (texmap.get(src.id) == null) {
					texmap.put(src.id, new Texture(p.getParent().resolve(src.path).toString()));
				}
				return texmap.get(src.id);
			}
		}
		return null;
	}

	private TextureRegion[][] getNoteTexture(Image[] images, Path p) {
		TextureRegion[][] noteimages = new TextureRegion[images.length][];
		for(int i = 0;i < images.length;i++) {
			Image note = images[i];
			Texture tex = getTexture(note.src, p);
			noteimages[i] = getSourceImage(tex,  note.x, note.y, note.w,
					note.h, note.divx, note.divy);
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

		public Destination[] destination;
	}

	public static class Source {
		public int id;
		public String path;
	}

	public static class Font {
		public int id;
		public String path;
	}

	public static class Image {
		public int id;
		public int src;
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
		public int id;
		public int ref;
		public int[] images = new int[0];
	}

	public static class Value {
		public int id;
		public int src;
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
		public int ref;
	}

	public static class Text {
		public int id;
		public int font;
		public int size;
		public int align;
		public int ref;
	}

	public static class Slider {
		public int id;
		public int src;
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
		public int id;
		public int src;
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
		public int id;
	}

	public static class JudgeGraph {
		public int id;
		public int type;
	}

	public static class NoteSet {
		public int id;
		public Image[] note = new Image[0];
		public Image[] lnstart = new Image[0];
		public Image[] lnend = new Image[0];
		public Image[] lnbody = new Image[0];
		public Image[] lnactive = new Image[0];
		public Image[] hcnstart = new Image[0];
		public Image[] hcnend = new Image[0];
		public Image[] hcnbody = new Image[0];
		public Image[] hcnactive = new Image[0];
		public Image[] hcndamage = new Image[0];
		public Image[] hcnreactive = new Image[0];
		public Image[] mine = new Image[0];
		public Image[] hidden = new Image[0];
		public Image[] processed = new Image[0];
		public Animation[] dst = new Animation[0];
		public Image[] line = new Image[0];
		public Destination[] group = new Destination[0];
	}

	public static class Gauge {
		public int id;
		public Image[] nodes;
	}

	public static class BGA {
		public int id;
	}

	public static class Destination {
		public int id;
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
		public int y  = Integer.MIN_VALUE;
		public int w  = Integer.MIN_VALUE;
		public int h = Integer.MIN_VALUE;

		public int acc = Integer.MIN_VALUE;

		public int a  = Integer.MIN_VALUE;
		public int r  = Integer.MIN_VALUE;
		public int g  = Integer.MIN_VALUE;
		public int b  = Integer.MIN_VALUE;

		public int angle = Integer.MIN_VALUE;

	}
}
