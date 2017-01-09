package bms.player.beatoraja.skin;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.*;

import bms.player.beatoraja.Resolution;
import com.badlogic.gdx.assets.loaders.resolvers.ResolutionFileResolver;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Json;

import bms.player.beatoraja.decide.MusicDecideSkin;
import bms.player.beatoraja.result.*;

public class SkinLoader {
	
	private Rectangle dstr;

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

	public LR2SkinHeader loadHeader(Path p) {
		LR2SkinHeader header = null;
		try {
			Json json = new Json();
			json.setIgnoreUnknownFields(true);
			JsonSkin sk = json.fromJson(JsonSkin.class, new FileReader(p.toFile()));

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
			
			JsonSkin sk = json.fromJson(JsonSkin.class, new FileReader(p.toFile()));

			Map<Integer, Texture> texmap = new HashMap();
			
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
							for (Source src : sk.source) {
								if (img.src == src.id) {
									if (texmap.get(src.id) == null) {
										texmap.put(src.id, new Texture(p.getParent().resolve(src.path).toString()));
									}
									
									if(img.len > 1) {
										TextureRegion[] srcimg = getSourceImage(texmap.get(src.id),  img.x, img.y, img.w,
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
										obj = new SkinImage(getSourceImage(texmap.get(src.id),  img.x, img.y, img.w,
												img.h, img.divx, img.divy), img.timer, img.cycle);										
									}
									
									break;
								}
							}
							break;
						}
					}
					for (Value value : sk.value) {
						if (dst.id == value.id) {
							for (Source src : sk.source) {
								if (value.src == src.id) {
									if (texmap.get(src.id) == null) {
										texmap.put(src.id, new Texture(p.getParent().resolve(src.path).toString()));
									}
									TextureRegion[] images = getSourceImage(texmap.get(src.id),  value.x, value.y, value.w,
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
							break;
						}
					}
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
				}

				if (obj != null) {
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
					skin.add(obj);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return skin;
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
		
		public Source[] source = new Source[0];
		public Font[] font = new Font[0];
		public Image[] image = new Image[0];
		public Value[] value = new Value[0];
		public Text[] text = new Text[0];
		public GaugeGraph[] gaugegraph = new GaugeGraph[0];
		public JudgeGraph[] judgegraph = new JudgeGraph[0];

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

	public static class GaugeGraph {
		public int id;
	}

	public static class JudgeGraph {
		public int id;
		public int type;
	}

	public static class Destination {
		public int id;
		public int blend;
		public int filter;
		public int timer;
		public int loop;
		public int center;
		public int[] op = new int[0];
		public Animation[] dst;
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
