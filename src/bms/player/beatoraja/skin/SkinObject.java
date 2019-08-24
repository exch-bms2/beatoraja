package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.property.*;

import com.badlogic.gdx.graphics.Color;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.*;

/**
 * スキンオブジェクト
 * 
 * @author exch
 */
public abstract class SkinObject implements Disposable {

	/**
	 * オフセットの参照ID
	 */
	private int[] offset = new int[0];

	private boolean relative;

	/**
	 * 参照するタイマー定義
	 */
	private TimerProperty dsttimer;
	/**
	 * ループ開始タイマー
	 */
	private int dstloop = 0;
	/**
	 * ブレンド(2:加算, 9:反転)
	 */
	private int dstblend = 0;
    /**
     * 0 : Nearest neighbor
     * 1 : Linear filtering
     */
	private int dstfilter;
	
	private int imageType;
	
	/**
	 * 画像回転の中心
	 */
	private int dstcenter;

	private int acc;
	/**
	 * オブジェクトクリック時に実行するイベント
	 */
	private Event clickevent = null;
	/**
	 * オブジェクトクリック判定・イベント引数の種類
	 * 0: 通常(plus only)
	 * 1: 通常(minus only)
	 * 2: 左右分割(左=minus,右=plus)
	 * 3: 上下分割(下=minus,上=plus)
	 */
	private int clickeventType = 0;
	/**
	 * 描画条件となるオプション定義
	 */
	private int[] dstop = new int[0];
	private BooleanProperty[] dstdraw = new BooleanProperty[0];
	/**
	 * 描画条件のマウス範囲
	 */
	private Rectangle mouseRect = null;
	/**
	 * 画像の伸縮方法の指定
	 */
	private StretchType stretch = StretchType.STRETCH;

	private static final float[] CENTERX = { 0.5f, 0, 0.5f, 1, 0, 0.5f, 1, 0, 0.5f, 1, };
	private static final float[] CENTERY = { 0.5f, 0, 0, 0, 0.5f, 0.5f, 0.5f, 1, 1, 1 };

	/**
	 * 回転中心のX座標(左端:0.0 - 右端:1.0)
	 */
	private float centerx;
	/**
	 * 回転中心のY座標(下端:0.0 - 上端:1.0)
	 */
	private float centery;
	/**
	 * 描画先
	 */
	private SkinObjectDestination[] dst = new SkinObjectDestination[0];
	
	// 以下、高速化用
	private long starttime;
	private long endtime;

	public boolean draw;
	public Rectangle region = new Rectangle();
	public Color color = new Color();
	public int angle;
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

	public void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
	                           int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3, int offset) {
		setDestination(time, x, y, w, h, acc, a, r, g, b, blend, filter, angle, center, loop,
				timer > 0 ? TimerPropertyFactory.getTimerProperty(timer) : null, new int[]{op1,op2,op3});
		setOffsetID(offset);
	}

	public void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
	                           int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3, int[] offset) {
		setDestination(time, x, y, w, h, acc, a, r, g, b, blend, filter, angle, center, loop,
				timer > 0 ? TimerPropertyFactory.getTimerProperty(timer) : null, new int[]{op1,op2,op3});
		setOffsetID(offset);
	}

	public void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
	                           int blend, int filter, int angle, int center, int loop, int timer, int[] op) {
		setDestination(time, x, y, w, h, acc, a, r, g, b, blend, filter, angle, center, loop,
				timer > 0 ? TimerPropertyFactory.getTimerProperty(timer) : null);
		if (dstop.length == 0 && dstdraw.length == 0) {
			setDrawCondition(op);
		}
	}

	public void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
	                           int blend, int filter, int angle, int center, int loop, int timer, BooleanProperty draw) {
		setDestination(time, x, y, w, h, acc, a, r, g, b, blend, filter, angle, center, loop,
				timer > 0 ? TimerPropertyFactory.getTimerProperty(timer) : null);
		dstdraw = new BooleanProperty[] {draw};
	}

	public void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
			int blend, int filter, int angle, int center, int loop, TimerProperty timer, int op1, int op2, int op3, int offset) {
		setDestination(time, x, y, w, h, acc, a, r, g, b, blend, filter, angle, center, loop, timer, new int[]{op1,op2,op3});
		setOffsetID(offset);
	}

	public void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
							   int blend, int filter, int angle, int center, int loop, TimerProperty timer, int op1, int op2, int op3, int[] offset) {
		setDestination(time, x, y, w, h, acc, a, r, g, b, blend, filter, angle, center, loop, timer, new int[]{op1,op2,op3});
		setOffsetID(offset);
	}

	public void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
			int blend, int filter, int angle, int center, int loop, TimerProperty timer, int[] op) {
		setDestination(time, x, y, w, h, acc, a, r, g, b, blend, filter, angle, center, loop, timer);
		if (dstop.length == 0 && dstdraw.length == 0) {
			setDrawCondition(op);
		}
	}
	
	public void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
			int blend, int filter, int angle, int center, int loop, TimerProperty timer, BooleanProperty draw) {
		setDestination(time, x, y, w, h, acc, a, r, g, b, blend, filter, angle, center, loop, timer);
		dstdraw = new BooleanProperty[] {draw};
	}
	
	private void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
			int blend, int filter, int angle, int center, int loop, TimerProperty timer) {
		SkinObjectDestination obj = new SkinObjectDestination(time, new Rectangle(x, y, w, h), new Color(r / 255.0f,
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
		
		if (dstcenter == 0 && center >= 0 && center < 10) {
			dstcenter = center;
			centerx = CENTERX[center];
			centery = CENTERY[center];
		}
		if (dsttimer == null) {
			dsttimer = timer;
		}
		if (dstloop == 0) {
			dstloop = loop;
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

	public BooleanProperty[] getDrawCondition() {
		return dstdraw;
	}

	public int[] getOption() {
		return dstop;
	}

	public void setOption(int[] dstop) {
		this.dstop = dstop;
	}

	public void setDrawCondition(int[] dstop) {
		IntSet l = new IntSet(dstop.length);
		IntArray op = new IntArray(dstop.length);
		Array<BooleanProperty> draw = new Array(dstop.length);
		for(int i : dstop) {
			if(i != 0 && !l.contains(i)) {
				BooleanProperty dc = BooleanPropertyFactory.getBooleanProperty(i);
				if(dc != null) {
					draw.add(dc);
				} else {
					op.add(i);
				}
				l.add(i);
			}
		}
		this.dstop = op.toArray();
		this.dstdraw = draw.toArray(BooleanProperty.class);
	}
	
	public void setDrawCondition(BooleanProperty[] dstdraw) {
		this.dstdraw = dstdraw;
	}

	public void setStretch(int stretch) {
		if (stretch < 0)
			return;
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

	public StretchType getStretch() {
		return stretch;
	}

	public int getBlend() {
		return this.dstblend;
	}

	/**
	 * 指定して時間に応じた描画領域を返す
	 * 
	 * @param time
	 *            時間(ms)
	 * @return 描画領域
	 */
	public void prepareRegion(long time, MainState state) {
		final TimerProperty timer = dsttimer;

		if (timer != null) {
			if (timer.isOff(state)) {
				draw = false;
				return;
			}
			time -= timer.get(state);
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
			draw = false;
			return;
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
				region.set(dst[index].region);
			} else {
				if(acc == 3) {
					final Rectangle r1 = dst[index].region;
					region.x = r1.x;
					region.y = r1.y;
					region.width = r1.width;
					region.height = r1.height;
				} else {
					final Rectangle r1 = dst[index].region;
					final Rectangle r2 = dst[index + 1].region;
					region.x = r1.x + (r2.x - r1.x) * rate;
					region.y = r1.y + (r2.y - r1.y) * rate;
					region.width = r1.width + (r2.width - r1.width) * rate;
					region.height = r1.height + (r2.height - r1.height) * rate;
				}
			}

			for(SkinOffset off : this.off) {
				if (off != null) {
					if(!relative) {
						region.x += off.x - off.w / 2;
						region.y += off.y - off.h / 2;
					}
					region.width += off.w;
					region.height += off.h;
				}
			}
			return;
		} else {
			if (offset.length == 0) {
				region.set(fixr);
				return;
			}
			region.set(fixr);
			for(SkinOffset off : this.off) {
				if (off != null) {
					if(!relative) {
						region.x += off.x - off.w / 2;
						region.y += off.y - off.h / 2;
					}
					region.width += off.w;
					region.height += off.h;
				}
			}
			return;
		}
	}
	
	public Rectangle getDestination(long time, MainState state) {
		return draw ? region : null;
	}


	private void prepareColor() {
		if (fixc != null) {
			color.set(fixc);
			for(SkinOffset off :this.off) {
				if(off != null) {
					float a = color.a + (off.a / 255.0f);
					a = a > 1 ? 1 : (a < 0 ? 0 : a);
					color.a = a;
				}
			}
			return;
		}
		getRate();
		if(rate == 0) {
			color.set(dst[index].color);			
		} else {
			if(acc == 3) {
				final Color r1 = dst[index].color;
				color.r = r1.r;
				color.g = r1.g;
				color.b = r1.b;
				color.a = r1.a;
				return;
			} else {
				final Color r1 = dst[index].color;
				final Color r2 = dst[index + 1].color;
				color.r = r1.r + (r2.r - r1.r) * rate;
				color.g = r1.g + (r2.g - r1.g) * rate;
				color.b = r1.b + (r2.b - r1.b) * rate;
				color.a = r1.a + (r2.a - r1.a) * rate;
				return;
			}
		}
		for(SkinOffset off :this.off) {
			if(off != null) {
				float a = color.a + (off.a / 255.0f);
				a = a > 1 ? 1 : (a < 0 ? 0 : a);
				color.a = a;
			}
		}
	}
	
	public Color getColor() {
		return color;
	}
	
	private void prepareAngle() {
		if (fixa != Integer.MIN_VALUE) {
			angle = fixa;
			for(SkinOffset off :this.off) {
				if(off != null) {
					angle += off.r;
				}
			}
			return;
		}
		getRate();
		angle = (rate == 0 || acc == 3 ? dst[index].angle :  (int) (dst[index].angle + (dst[index + 1].angle - dst[index].angle) * rate));
		for(SkinOffset off :this.off) {
			if(off != null) {
				angle += off.r;
			}
		}
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
	
	public boolean validate() {
		return getAllDestination().length > 0;
	}

	/**
	 * リソースをあらかじめロードしておく
	 */
	public void load() {
	}
	
	public void prepare(long time, MainState state) {
		prepare(time, state, 0, 0);
	}

	public void prepare(long time, MainState state, float offsetX, float offsetY) {
		for (BooleanProperty draw : dstdraw) {
			if(!draw.get(state)) {
				this.draw = false;
				return;
			}
		}
		draw = true;
		prepareRegion(time, state);
		region.x += offsetX;
		region.y += offsetY;
		if (mouseRect != null && !mouseRect.contains(state.main.getInputProcessor().getMouseX() -region.x,
				state.main.getInputProcessor().getMouseY() - region.y)) {
			draw = false;
			return;
		}

		prepareColor();
		prepareAngle();
	}

	public abstract void draw(SkinObjectRenderer sprite);

	protected void draw(SkinObjectRenderer sprite, TextureRegion image) {
		if (color.a == 0f || image == null) {
			return;
		}
		
		tmpRect.set(region);
		if(stretch != null) {
			stretch.stretchRect(tmpRect, tmpImage, image);
		}
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

	protected void draw(SkinObjectRenderer sprite, TextureRegion image, float x, float y, float width, float height) {
		draw(sprite, image, x, y, width, height, color, angle);
	}

	protected void draw(SkinObjectRenderer sprite, TextureRegion image, float x, float y, float width, float height,
			Color color, int angle) {
		if (color == null || color.a == 0f || image == null) {
			return;
		}
		tmpRect.set(x, y, width, height);
		if(stretch != null) {
			stretch.stretchRect(tmpRect, tmpImage, image);
		}
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

	protected boolean mousePressed(MainState state, int button, int x, int y) {
		if (clickevent != null) {
			final Rectangle r = region;
			// System.out.println(obj.getClickeventId() + " : " + r.x +
			// "," + r.y + "," + r.width + "," + r.height + " - " + x +
			// "," + y);
			switch (clickeventType) {
			case 0:
				if (r != null && r.x <= x && r.x + r.width >= x && r.y <= y && r.y + r.height >= y) {
					clickevent.exec(state, 1);
					return true;
				}
				break;
			case 1:
				if (r != null && r.x <= x && r.x + r.width >= x && r.y <= y && r.y + r.height >= y) {
					clickevent.exec(state, -1);
					return true;
				}
				break;
			case 2:
				if (r != null && r.x <= x && r.x + r.width >= x && r.y <= y && r.y + r.height >= y) {
					clickevent.exec(state, x >= r.x + r.width/2 ? 1 : -1);
					return true;
				}
				break;
			case 3:
				if (r != null && r.x <= x && r.x + r.width >= x && r.y <= y && r.y + r.height >= y) {
					clickevent.exec(state, y >= r.y + r.height/2 ? 1 : -1);
					return true;
				}
				break;
			}
		}
		return false;
	}

	public int getClickeventId() {
		return clickevent.getEventId();
	}

	public Event getClickevent() {
		return clickevent;
	}

	public void setClickevent(int clickevent) {
		this.clickevent = EventFactory.getEvent(clickevent);
	}

	public void setClickevent(Event clickevent) {
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
	 * スキンオブジェクトの描画先を表現するクラス
	 * 
	 * @author exch
	 */
	public static class SkinObjectDestination {

		public final long time;
		/**
		 * 描画領域
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
	 * オフセット
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

	/**
	 * IntegerPropertyからmin - max間の比率を表現するためのProperty
	 *
	 * @author exch
	 */
	public static class RateProperty implements FloatProperty {
		
		private final IntegerProperty ref;
		private final int min;
		private final int max;
		
		public RateProperty(int type, int min, int max) {
			this.ref = IntegerPropertyFactory.getIntegerProperty(type);
			this.min = min;
			this.max = max;
		}
		
		public float get(MainState state) {
			final int value = ref != null ? ref.get(state) : 0;
			if(min < max) {
				if(value > max) {
					return 1;
				} else if(value < min) {
					return 0;
				} else {
					return Math.abs( ((float) value - min) / (max - min) );
				}
			} else {
				if(value < max) {
					return 1;
				} else if(value > min) {
					return 0;
				} else {
					return Math.abs( ((float) value - min) / (max - min) );
				}
			}
		}
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
	
	public SkinOffset[] getOffsets() {
		return off;
	}

	public TimerProperty getDestinationTimer() {
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
