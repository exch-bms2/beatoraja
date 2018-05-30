package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import com.badlogic.gdx.graphics.Color;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.*;

/**
 * 占쎄때占쎄텣占쎄틡占쎄텞占쎄묍占쎄땋占쎄텙占쎄텥占쎄퉱
 * 
 * @author exch
 */
public abstract class SkinObject implements Disposable {

	/**
	 * 占쎄텞占쎄묄占쎄땐占쎄맙占쎄퉱占쎄쿁占쎈♧占쎈�켌D
	 */
	private int[] offset = new int[0];

	private boolean relative;

	private int imageid = -1;
	/**
	 * 占쎈♧占쎈�깍옙嫄뀐옙援ο옙沅�占쎄텕占쎄묻占쎄틬ID
	 */
	private int dsttimer = 0;
	/**
	 * 占쎄틓占쎄틬占쎄묏占쎈��넼�뿣沅�占쎄텕占쎄묻占쎄틬
	 */
	private int dstloop = 0;
	/**
	 * 占쎄묍占쎄틕占쎄틡占쎄퉳(2:占쎈뮙蒻몌옙, 9:占쎈즽�굲占�)
	 */
	private int dstblend = 0;
    /**
     * 0 : Nearest neighbor
     * 1 : Linear filtering
     */
	private int dstfilter;
	
	private int imageType;
	
	/**
	 * 占쎈돕占쎄퉿占쎌뜮�굲�뢿寃쀨쯁占썸걲占�
	 */
	private int dstcenter;

	private int acc;
	/**
	 * 占쎄텞占쎄묍占쎄땋占쎄텙占쎄텥占쎄퉱占쎄텥占쎄틒占쎄맙占쎄텥占쎌끋占쎄쾽畑댁옊二깍옙嫄뀐옙援ο옙沅롳옙源㏆옙爰랃옙源됵옙寃쀯옙猶울옙�켌D
	 */
	private int clickevent = -1;
	/**
	 * 占쎄텞占쎄묍占쎄땋占쎄텙占쎄텥占쎄퉱占쎄텥占쎄틒占쎄맙占쎄텥占쎈떓畑댁떏爰뺧옙沅롳옙源㏆옙爰랃옙源됵Ⅹ類욌퉲占쎄쿁�몛占썹챻占�
	 * 0: 占쎌떑留�(plus only)
	 * 1: 占쎌떑留�(minus only)
	 * 2: �뿥占쏙옙琉귨옙�듊占쎈룾(�뿥占�=minus,占쎈쨧=plus)
	 * 3: 鼇앸벦�닎占쎈듊占쎈룾(鼇앾옙=minus,鼇앾옙=plus)
	 */
	private int clickeventType = 0;
	/**
	 * 占쎈짂占쎈돕占쎌뵃繞볥쑑寃륅옙寃믭옙援ο옙沅뺧옙源ㅿ옙沅ο옙源억옙爰랃Ⅴ�떓�뫒
	 */
	private int[] dstop = new int[0];
	/**
	 * 占쎈짂占쎈돕占쎌뵃繞볥쑑寃쀯옙源�占쎄텘占쎄때壤깃쐼�럧
	 */
	private Rectangle mouseRect = null;
	/**
	 * 占쎈돕占쎄퉿占쎄쿁辱됰ŉ由э옙堉띰쫲類쏄쿁占쎈셼畑댐옙
	 */
	private StretchType stretch = StretchType.STRETCH;

	public enum StretchType {
		// 占쎈짂占쎈돕占쎈�랃옙寃쀥쉽袁��럧占쎄쾽占쎈└占쎄뎀占쎄굘占쎄쾷辱됰ŉ由э옙嫄뀐옙援�
		STRETCH(0),
		// 占쎄텑占쎄때占쎄묫占쎄텥占쎄퉱癲뉖떫援�略녹빂嫄억옙寃뉛옙寃뉛옙猷믭옙逾쏙옙�랃옙寃쀥쉽袁��럧占쎄쾽占쎈즿占쎄께占쎄데占쎄턄占쎄콨占쎄쾽辱됰ŉ由э옙嫄뀐옙援�
		KEEP_ASPECT_RATIO_FIT_INNER(1),
		// 占쎄텑占쎄때占쎄묫占쎄텥占쎄퉱癲뉖떫援�略녹빂嫄억옙寃뉛옙寃뉛옙猷믭옙逾쏙옙�랃옙寃쀥쉽袁��럧占쎈�꿴턁蹂앷뎌沃ㅻ뀾嫄앾옙援좑옙嫄앾옙寃볣큺硫곕━占쎄굉占쎄데
		KEEP_ASPECT_RATIO_FIT_OUTER(2),
		KEEP_ASPECT_RATIO_FIT_OUTER_TRIMMED(3),
		// 占쎄텑占쎄때占쎄묫占쎄텥占쎄퉱癲뉖떫援�略녹빂嫄억옙寃뉛옙寃뉛옙猷믭옙逾쏙옙�랃옙寃쀨늾�굝�돪占쎄쾽占쎈└占쎄뎀占쎄굘占쎄쾷辱됰ŉ由э옙嫄뀐옙援�
		KEEP_ASPECT_RATIO_FIT_WIDTH(4),
		KEEP_ASPECT_RATIO_FIT_WIDTH_TRIMMED(5),
		// 占쎄텑占쎄때占쎄묫占쎄텥占쎄퉱癲뉖떫援�略녹빂嫄억옙寃뉛옙寃뉛옙猷믭옙逾쏙옙�랃옙寃쀦퐗占쏙Ⅷ�겧寃볩옙由�占쎄뎀占쎄굘占쎄쾷辱됰ŉ由э옙嫄뀐옙援�
		KEEP_ASPECT_RATIO_FIT_HEIGHT(6),
		KEEP_ASPECT_RATIO_FIT_HEIGHT_TRIMMED(7),
		// 占쎈짂占쎈돕占쎈�랃옙寃볩옙猷묕옙寃뀐옙援�占쎄쾼占쎄콢占쎌죨占쎈└占쎄쾽占쎄쿂占쎄텑占쎄때占쎄묫占쎄텥占쎄퉱癲뉖떫援�略녹빂嫄억옙寃뉛옙寃뉑퐗占썲선琉듦굉占쎄데
		KEEP_ASPECT_RATIO_NO_EXPANDING(8),
		// 辱됰ŉ由э옙嫄�占쎄쾼占쎄콢塋딅뛻由롩툣占쏙옙寃볩옙由�占쎄뎀占쎄굘占쎄데塋딉옙
		NO_RESIZE(9),
		NO_RESIZE_TRIMMED(10),
		;
		StretchType(int id) {
			this.id = id;
		}
		public final int id;
	}

	private static final float[] CENTERX = { 0.5f, 0, 0.5f, 1, 0, 0.5f, 1, 0, 0.5f, 1, };
	private static final float[] CENTERY = { 0.5f, 0, 0, 0, 0.5f, 0.5f, 0.5f, 1, 1, 1 };

	/**
	 * 占쎌뜮�굲�룜由롦걲猿곴쿁X鵝��렕怡�(�뿥占썲죰占�:0.0 - 占쎈쨧櫻뗰옙:1.0)
	 */
	private float centerx;
	/**
	 * 占쎌뜮�굲�룜由롦걲猿곴쿁Y鵝��렕怡�(鼇앸뿧�궚:0.0 - 鼇앸봺�궚:1.0)
	 */
	private float centery;
	/**
	 * 占쎈짂占쎈돕占쎈��
	 */
	private SkinObjectDestination[] dst = new SkinObjectDestination[0];
	
	// 繞벿댄닎占쎄남爾듸옙�엺�솑占쎈뎁
	private long starttime;
	private long endtime;

	private Rectangle r = new Rectangle();
	private Color c = new Color();
	private SkinOffset[] off = new SkinOffset[0];

	private Rectangle fixr = null;
	private Color fixc = null;
	private int fixa = Integer.MIN_VALUE;

	private long nowtime = 0;
	private float rate = 0;
	private int index = 0;

	private Rectangle tmpRect = new Rectangle();
	private TextureRegion tmpImage = new TextureRegion();
	
	public SkinObjectDestination[] getAllDestination() {
		return dst;
	}

	public void setDestination(long time, SkinDestinationSize destSize, int acc, int a, int r, int g, int b,
			int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3, int offset) {
		setDestination(time, destSize, acc, a, r, g, b, blend, filter, angle, center, loop, timer, new int[]{op1,op2,op3});
		setOffsetID(offset);
	}

	public void setDestination(long time, SkinDestinationSize destSize, int acc, int a, int r, int g, int b,
							   int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3, int[] offset) {
		setDestination(time, destSize, acc, a, r, g, b, blend, filter, angle, center, loop, timer, new int[]{op1,op2,op3});
		setOffsetID(offset);
	}

	public void setDestination(long time, SkinDestinationSize destSize, int acc, int a, int r, int g, int b,
			int blend, int filter, int angle, int center, int loop, int timer, int[] op) {
		SkinObjectDestination obj = new SkinObjectDestination(time, new Rectangle(destSize.getDstx(), destSize.getDsty(),destSize.getDstw(),destSize.getDsth()), new Color(r / 255.0f,
				g / 255.0f, b / 255.0f, a / 255.0f), angle, acc);
		if (dst.length == 0) {
			fixr = obj.region;
			fixc = obj.color;
			fixa = obj.angle;
		} else {
			if (!obj.region.equals(fixr)) {
				fixr = null;
			}
			if (!obj.color.equals(fixc)) {
				fixc = null;
			}
			if (!(fixa == obj.angle)) {
				fixa = Integer.MIN_VALUE;
			}
		}
		if (this.acc == 0) {
			this.acc = acc;
		}
		if (dstblend == 0) {
			dstblend = blend;
		}
		
		if (dstfilter == 0) {
			dstfilter = filter;
		}
		
		if (dstcenter == 0 && center < 10) {
			dstcenter = center;
			centerx = CENTERX[center];
			centery = CENTERY[center];
		}
		if (dsttimer == 0) {
			dsttimer = timer;
		}
		if (dstloop == 0) {
			dstloop = loop;
		}
		if (dstop.length == 0) {
			IntSet l = new IntSet();
			for(int i : op) {
				if(i != 0) {
					l.add(i);
				}
			}
			dstop = l.iterator().toArray().toArray();
		}
		for (int i = 0; i < dst.length; i++) {
			if (dst[i].time > time) {
				Array<SkinObjectDestination> l = new Array<SkinObjectDestination>(dst);
				l.insert(i, obj);
				dst = l.toArray(SkinObjectDestination.class);
				starttime = dst[0].time;
				endtime = dst[dst.length - 1].time;
				return;
			}
		}
		Array<SkinObjectDestination> l = new Array<SkinObjectDestination>(dst);
		l.add(obj);
		dst = l.toArray(SkinObjectDestination.class);
		starttime = dst[0].time;
		endtime = dst[dst.length - 1].time;
	}

	public int[] getOption() {
		return dstop;
	}

	public void setOption(int[] dstop) {
		this.dstop = dstop;
	}

	public Rectangle getDestination(long time) {
		return this.getDestination(time, null);
	}

	public void setStretch(int stretch) {
		for (StretchType type : StretchType.values()) {
			if (type.id == stretch) {
				this.stretch = type;
				return;
			}
		}
	}

	public void setStretch(StretchType stretch) {
		this.stretch = stretch;
	}

	public int getBlend() {
		return this.dstblend;
	}

	/**
	 * 占쎈셼畑댁떏嫄�占쎄쾷占쎌끋占쎈펻占쎄쾽�걲�뭽嫄�占쎄굴占쎈짂占쎈돕占쎌젦占쎌쐺占쎄뎌�벀遺듦굉
	 * 
	 * @param time
	 *            占쎌끋占쎈펻(ms)
	 * @return 占쎈짂占쎈돕占쎌젦占쎌쐺
	 */
	public Rectangle getDestination(long time, MainState state) {
		final int timer = dsttimer;

		if (timer != 0 && timer < MainController.timerCount) {
			if (!state.main.isTimerOn(timer)) {
				return null;
			}
			time -= state.main.getTimer(timer);
		}

		final long lasttime = endtime;
		if( dstloop == -1) {
			if(time > endtime) {
				time = -1;
			}
		} else if (lasttime > 0 && time > dstloop) {
			if (lasttime == dstloop) {
				time = dstloop;
			} else {
				time = (time - dstloop) % (lasttime - dstloop) + dstloop;
			}
		}
		if (starttime > time) {
			return null;
		}
		nowtime = time;
		rate = -1;
		index = -1;
		for(int i = 0;i < off.length;i++) {
			off[i] = state != null ? state.getOffsetValue(offset[i]) : null;
		}

		if (fixr == null) {
			getRate();
			if(rate == 0) {
				r.set(dst[index].region);
			} else {
				if(acc == 3) {
					final Rectangle r1 = dst[index].region;
					r.x = r1.x;
					r.y = r1.y;
					r.width = r1.width;
					r.height = r1.height;
				} else {
					final Rectangle r1 = dst[index].region;
					final Rectangle r2 = dst[index + 1].region;
					r.x = r1.x + (r2.x - r1.x) * rate;
					r.y = r1.y + (r2.y - r1.y) * rate;
					r.width = r1.width + (r2.width - r1.width) * rate;
					r.height = r1.height + (r2.height - r1.height) * rate;
				}
			}

			for(SkinOffset off : this.off) {
				if (off != null) {
					if(!relative) {
						r.x += off.x - off.w / 2;
						r.y += off.y - off.h / 2;
					}
					r.width += off.w;
					r.height += off.h;
				}
			}
			return r;
		} else {
			if (offset.length == 0) {
				return fixr;
			}
			r.set(fixr);
			for(SkinOffset off : this.off) {
				if (off != null) {
					if(!relative) {
						r.x += off.x - off.w / 2;
						r.y += off.y - off.h / 2;
					}
					r.width += off.w;
					r.height += off.h;
				}
			}
			return r;
		}
	}

	public Color getColor() {
		if (fixc != null) {
			c.set(fixc);
			for(SkinOffset off :this.off) {
				if(off != null) {
					float a = c.a + (off.a / 255.0f);
					a = a > 1 ? 1 : (a < 0 ? 0 : a);
					c.a = a;
				}
			}
			return c;
		}
		getRate();
		if(rate == 0) {
			c.set(dst[index].color);			
		} else {
			if(acc == 3) {
				final Color r1 = dst[index].color;
				c.r = r1.r;
				c.g = r1.g;
				c.b = r1.b;
				c.a = r1.a;
				return c;
			} else {
				final Color r1 = dst[index].color;
				final Color r2 = dst[index + 1].color;
				c.r = r1.r + (r2.r - r1.r) * rate;
				c.g = r1.g + (r2.g - r1.g) * rate;
				c.b = r1.b + (r2.b - r1.b) * rate;
				c.a = r1.a + (r2.a - r1.a) * rate;
				return c;
			}
		}
		for(SkinOffset off :this.off) {
			if(off != null) {
				float a = c.a + (off.a / 255.0f);
				a = a > 1 ? 1 : (a < 0 ? 0 : a);
				c.a = a;
			}
		}
		return c;
	}

	public int getAngle() {
		if (fixa != Integer.MIN_VALUE) {
			int a = fixa;
			for(SkinOffset off :this.off) {
				if(off != null) {
					a += off.r;
				}
			}
			return a;
		}
		getRate();
		int a = (rate == 0 || acc == 3 ? dst[index].angle :  (int) (dst[index].angle + (dst[index + 1].angle - dst[index].angle) * rate));
		for(SkinOffset off :this.off) {
			if(off != null) {
				a += off.r;
			}
		}
		return a;
	}
	
	private void getRate() {
		if(rate != -1) {
			return;
		}
		long time2 = dst[dst.length - 1].time;
		if(nowtime == time2) {
			this.rate = 0;
			this.index = dst.length - 1;
			return;
		}
		for (int i = dst.length - 2; i >= 0; i--) {
			final long time1 = dst[i].time;
			if (time1 <= nowtime && time2 > nowtime) {
				float rate = (float) (nowtime - time1) / (time2 - time1);
				switch(acc) {
				case 1:
					rate = rate * rate;
					break;
				case 2:
					rate = 1 - (rate - 1) * (rate - 1);
					break;
				}
				this.rate = rate;
				this.index = i;
				return;
			}
			time2 = time1;
		}
		this.rate = 0;
		this.index = 0;
	}

	public abstract void draw(SkinObjectRenderer sprite, long time, MainState state);

	protected void draw(SkinObjectRenderer sprite, TextureRegion image, float x, float y, float width, float height, MainState state) {
		draw(sprite, image, x, y, width, height, getColor(), getAngle(), state);
	}

	protected void draw(SkinObjectRenderer sprite, TextureRegion image, float x, float y, float width, float height,
			Color color, int angle, MainState state) {
		if (color == null || color.a == 0f || image == null) {
			return;
		}
		if (mouseRect != null && !mouseRect.contains(state.main.getInputProcessor().getMouseX() - x,
				state.main.getInputProcessor().getMouseY() - y)) {
			return;
		}
		tmpRect.set(x, y, width, height);
		getStretchedRect(tmpRect, tmpImage, image);
		sprite.setColor(color);
		sprite.setBlend(dstblend);
		sprite.setType(dstfilter != 0 && imageType == SkinObjectRenderer.TYPE_NORMAL ? 
				(tmpRect.width == tmpImage.getRegionWidth() && tmpRect.height == tmpImage.getRegionHeight() ?
				SkinObjectRenderer.TYPE_NORMAL : SkinObjectRenderer.TYPE_BILINEAR) : imageType);
		
		if (angle != 0) {
			sprite.draw(tmpImage, tmpRect.x, tmpRect.y, tmpRect.width, tmpRect.height, centerx , centery, angle);
		} else {
			sprite.draw(tmpImage, tmpRect.x, tmpRect.y, tmpRect.width, tmpRect.height);
		}
	}

	public void getStretchedRect(Rectangle rectangle, TextureRegion trimmedImage, TextureRegion image) {
		trimmedImage.setRegion(image);
		if (this.stretch == StretchType.STRETCH) {
			return;
		}
		float scaleX = rectangle.width / image.getRegionWidth();
		float scaleY = rectangle.height / image.getRegionHeight();
		switch (this.stretch) {
		case KEEP_ASPECT_RATIO_FIT_INNER:
			if (scaleX <= scaleY) {
				fitHeight(rectangle, image.getRegionHeight() * scaleX);
			} else {
				fitWidth(rectangle, image.getRegionWidth() * scaleY);
			}
			break;
		case KEEP_ASPECT_RATIO_FIT_OUTER:
			if (scaleX >= scaleY) {
				fitHeight(rectangle, image.getRegionHeight() * scaleX);
			} else {
				fitWidth(rectangle, image.getRegionWidth() * scaleY);
			}
			break;
		case KEEP_ASPECT_RATIO_FIT_OUTER_TRIMMED:
			if (scaleX >= scaleY) {
				fitHeightTrimmed(rectangle, scaleX, trimmedImage);
			} else {
				fitWidthTrimmed(rectangle, scaleY, trimmedImage);
			}
			break;
		case KEEP_ASPECT_RATIO_FIT_WIDTH:
			fitHeight(rectangle, image.getRegionHeight() * scaleX);
			break;
		case KEEP_ASPECT_RATIO_FIT_WIDTH_TRIMMED:
			fitHeightTrimmed(rectangle, scaleX, trimmedImage);
			break;
		case KEEP_ASPECT_RATIO_FIT_HEIGHT:
			fitWidth(rectangle, image.getRegionWidth() * scaleY);
			break;
		case KEEP_ASPECT_RATIO_FIT_HEIGHT_TRIMMED:
			fitWidthTrimmed(rectangle, scaleY, trimmedImage);
			break;
		case KEEP_ASPECT_RATIO_NO_EXPANDING: {
			float scale = Math.min(1f, Math.min(scaleX, scaleY));
			fitWidth(rectangle, image.getRegionWidth() * scale);
			fitHeight(rectangle, image.getRegionHeight() * scale);
			break;
		}
		case NO_RESIZE:
			fitWidth(rectangle, image.getRegionWidth());
			fitHeight(rectangle, image.getRegionHeight());
			break;
		case NO_RESIZE_TRIMMED:
			fitWidthTrimmed(rectangle, 1.0f, trimmedImage);
			fitHeightTrimmed(rectangle, 1.0f, trimmedImage);
			break;
		}
	}

	private void fitWidth(Rectangle rectangle, float width) {
		float cx = rectangle.x + rectangle.width * 0.5f;
		rectangle.width = width;
		rectangle.x = cx - rectangle.width * 0.5f;
	}

	private void fitHeight(Rectangle rectangle, float height) {
		float cy = rectangle.y + rectangle.height * 0.5f;
		rectangle.height = height;
		rectangle.y = cy - rectangle.height * 0.5f;
	}

	private void fitWidthTrimmed(Rectangle rectangle, float scale, TextureRegion image) {
		float width = scale * image.getRegionWidth();
		if (rectangle.width < width) {
			float cx = image.getRegionX() + image.getRegionWidth() * 0.5f;
			float w = rectangle.width / scale;
			image.setRegionX((int)(cx - w * 0.5f));
			image.setRegionWidth((int)w);
		} else {
			fitWidth(rectangle, width);
		}
	}

	private void fitHeightTrimmed(Rectangle rectangle, float scale, TextureRegion image) {
		float height = scale * image.getRegionHeight();
		if (rectangle.height < height) {
			float cy = image.getRegionY() + image.getRegionHeight() * 0.5f;
			float h = rectangle.height / scale;
			image.setRegionY((int)(cy - h * 0.5f));
			image.setRegionHeight((int)h);
		} else {
			fitHeight(rectangle, height);
		}
	}
	
	protected boolean mousePressed(MainState state, int button, int x, int y) {
		if (clickevent != -1) {
			Rectangle r = getDestination(state.main.getNowTime(), state);
			// System.out.println(obj.getClickevent() + " : " + r.x +
			// "," + r.y + "," + r.width + "," + r.height + " - " + x +
			// "," + y);
			switch (clickeventType) {
			case 0:
				if (r != null && r.x <= x && r.x + r.width >= x && r.y <= y && r.y + r.height >= y) {
					state.executeClickEvent(clickevent, 1);
					return true;
				}
				break;
			case 1:
				if (r != null && r.x <= x && r.x + r.width >= x && r.y <= y && r.y + r.height >= y) {
					state.executeClickEvent(clickevent, -1);
					return true;
				}
				break;
			case 2:
				if (r != null && r.x <= x && r.x + r.width >= x && r.y <= y && r.y + r.height >= y) {
					state.executeClickEvent(clickevent, x >= r.x + r.width/2 ? 1 : -1);
					return true;
				}
				break;
			case 3:
				if (r != null && r.x <= x && r.x + r.width >= x && r.y <= y && r.y + r.height >= y) {
					state.executeClickEvent(clickevent, y >= r.y + r.height/2 ? 1 : -1);
					return true;
				}
				break;
			}
		}
		return false;
	}

	public int getClickevent() {
		return clickevent;
	}

	public void setClickevent(int clickevent) {
		this.clickevent = clickevent;
	}

	public int getClickeventType() {
		return clickeventType;
	}

	public void setClickeventType(int clickeventType) {
		this.clickeventType = clickeventType;
	}

	public boolean isRelative() {
		return relative;
	}

	public void setRelative(boolean relative) {
		this.relative = relative;
	}

	/**
	 * 占쎄때占쎄텣占쎄틡占쎄텞占쎄묍占쎄땋占쎄텙占쎄텥占쎄퉱占쎄쿁占쎈짂占쎈돕占쎈�랃옙援��깗�몺琉롳옙嫄뀐옙援ο옙沅싷옙爰�占쎄때
	 * 
	 * @author exch
	 */
	public static class SkinObjectDestination {

		public final long time;
		/**
		 * 占쎈짂占쎈돕占쎌젦占쎌쐺
		 */
		public final Rectangle region;
		public final int acc;
		public final Color color;
		public final int angle;

		public SkinObjectDestination(long time, Rectangle region, Color color, int angle, int acc) {
			this.time = time;
			this.region = region;
			this.color = color;
			this.angle = angle;
			this.acc = acc;
		}
	}
	
	/**
	 * 占쎄텞占쎄묄占쎄땐占쎄맙占쎄퉱
	 * 
	 * @author exch
	 */
	public static class SkinOffset {
		public float x;
		public float y;
		public float w;
		public float h;
		public float r;
		public float a;
	}

	public abstract void dispose();

	public int[] getOffsetID() {
		return offset;
	}

	public void setOffsetID(int offset) {
		setOffsetID(new int[]{offset});
	}

	public void setOffsetID(int[] offset) {
		if(this.offset.length > 0) {
			return;
		}
		IntSet a = new IntSet(offset.length);
		for(int o : offset) {
			if(o > 0 && o < SkinProperty.OFFSET_MAX + 1) {
				a.add(o);
			}
		}
		if(a.size > 0) {
			this.offset = a.iterator().toArray().toArray();
			this.off = new SkinOffset[this.offset.length];
		}
	}

	public int getImageID() {
		return imageid;
	}

	public void setImageID(int imageid) {
		this.imageid = imageid;
	}

	public int getDestinationTimer() {
		return dsttimer;
	}
	
	public static void disposeAll(Disposable[] obj) {
		for(int i = 0;i < obj.length;i++) {
			if(obj[i] != null) {
				obj[i].dispose();
				obj[i] = null;
			}
		}
	}

	public int getImageType() {
		return imageType;
	}

	public void setImageType(int imageType) {
		this.imageType = imageType;
	}
	
	public int getFilter() {
		return dstfilter;
	}

	public void setFilter(int filter) {
		dstfilter = filter;
	}

	public void setMouseRect(float x2, float y2, float w2, float h2) {
		this.mouseRect = new Rectangle(x2, y2, w2, h2);
	}
}
