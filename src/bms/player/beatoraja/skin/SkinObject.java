package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

/**
 * スキンオブジェクト
 * 
 * @author exch
 */
public abstract class SkinObject implements Disposable {

	/**
	 * X座標オフセットの参照ID
	 */
	private int offsetx = -1;
	/**
	 * Y座標オフセットの参照ID
	 */
	private int offsety = -1;

	private int imageid = -1;

	private int dsttimer = 0;
	private int dstloop = 0;
	/**
	 * ブレンド(2:加算, 9:反転)
	 */
	private int dstblend = 0;
	/**
	 * フィルター
	 */
	private int dstfilter;
	/**
	 * 画像回転の中心
	 */
	private int dstcenter;

	private int clickevent = -1;

	private int[] dstop = new int[0];

	private List<SkinObjectDestination> dst = new ArrayList<SkinObjectDestination>();

	private Rectangle r = new Rectangle();
	private Color c = new Color();

	private Rectangle fixr = null;
	private Color fixc = null;
	private int fixa = Integer.MIN_VALUE;

	public SkinObjectDestination[] getAllDestination() {
		return dst.toArray(new SkinObjectDestination[dst.size()]);
	}

	public void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
			int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3) {
		setDestination(time, x, y, w, h, acc, a, r, g, b, blend, filter, angle, center, loop, timer, new int[]{op1,op2,op3});
	}

	public void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
			int blend, int filter, int angle, int center, int loop, int timer, int[] op) {
		SkinObjectDestination obj = new SkinObjectDestination(time, new Rectangle(x, y, w, h), new Color(r / 255.0f,
				g / 255.0f, b / 255.0f, a / 255.0f), angle, acc);
		if (dst.size() == 0) {
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
			List<Integer> l = new ArrayList<Integer>();
			for(int i : op) {
				if(i != 0) {
					l.add(i);
				}
			}
			op = new int[l.size()];
			for (int i = 0; i < l.size(); i++) {
				op[i] = l.get(i);
			}
			dstop = op;
		}
		for (int i = 0; i < dst.size(); i++) {
			if (dst.get(i).time > time) {
				dst.add(i, obj);
				return;
			}
		}
		dst.add(obj);
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

	/**
	 * 指定して時間に応じた描画領域を返す
	 * 
	 * @param time
	 *            時間(ms)
	 * @return 描画領域
	 */
	public Rectangle getDestination(long time, MainState state) {
		if (dst.size() == 0) {
			// System.out.println("void image");
			return new Rectangle(0, 0, 0, 0);
		}
		final int timer = dsttimer;

		if (timer != 0 && timer < 256) {
			final long stime = state.getTimer()[timer];
			if (stime == Long.MIN_VALUE) {
				return null;
			}
			time -= stime;
		}
		if (time < 0) {
			return null;
		}

		final long lasttime = dst.get(dst.size() - 1).time;
		if (dstloop == -1) {
			if (lasttime < time) {
				return null;
			}
		} else if (lasttime > 0 && time > dstloop) {
			if (lasttime == dstloop) {
				time = dstloop;
			} else {
				time = (time - dstloop) % (lasttime - dstloop) + dstloop;
			}
		}
		if (dst.get(0).time > time) {
			return null;
		}
		if (fixr == null) {
			for (int i = 0; i < dst.size() - 1; i++) {
				final SkinObjectDestination obj1 = dst.get(i);
				final SkinObjectDestination obj2 = dst.get(i + 1);
				if (obj1.time <= time && obj2.time >= time) {
					final Rectangle r1 = obj1.region;
					final Rectangle r2 = obj2.region;
					final float rate = (float) (time - obj1.time) / (obj2.time - obj1.time);
					r.x = r1.x + (r2.x - r1.x) * rate;
					r.y = r1.y + (r2.y - r1.y) * rate;
					r.width = r1.width + (r2.width - r1.width) * rate;
					r.height = r1.height + (r2.height - r1.height) * rate;
					if (state != null && offsetx != -1) {
						r.x += state.getSliderValue(offsetx);
					}
					if (state != null && offsety != -1) {
						r.y += state.getSliderValue(offsety);
					}
					return r;
				}
			}
		} else {
			if (offsetx == -1 && offsety == -1) {
				return fixr;
			}
			r.set(fixr);
			if (state != null && offsetx != -1) {
				r.x += state.getSliderValue(offsetx);
			}
			if (state != null && offsety != -1) {
				r.y += state.getSliderValue(offsety);
			}
			return r;
		}

		r.set(dst.get(0).region);
		if (state != null && offsetx != -1) {
			r.x += state.getSliderValue(offsetx);
		}
		if (state != null && offsety != -1) {
			r.y += state.getSliderValue(offsety);
		}
		return r;
	}

	public Color getColor(long time, MainState state) {
		if (fixc != null) {
			return fixc;
		}
		final int timer = dsttimer;

		if (timer != 0 && timer < 256) {
			if (state.getTimer()[timer] == Long.MIN_VALUE) {
				return null;
			}
			time -= state.getTimer()[timer];
		}
		if (time < 0) {
			return null;
		}

		if (dst.size() == 0) {
			// System.out.println("void color");
			return new Color(0, 0, 0, 0);
		}
		long lasttime = dst.get(dst.size() - 1).time;
		if (lasttime > 0 && time > dstloop) {
			if (lasttime == dstloop) {
				time = dstloop;
			} else {
				time = (time - dstloop) % (lasttime - dstloop) + dstloop;
			}
		}
		for (int i = 0; i < dst.size() - 1; i++) {
			final SkinObjectDestination obj1 = dst.get(i);
			final SkinObjectDestination obj2 = dst.get(i + 1);
			if (obj1.time <= time && obj2.time >= time) {
				final Color r1 = obj1.color;
				final Color r2 = obj2.color;
				final float rate = (float) (time - obj1.time) / (obj2.time - obj1.time);
				c.r = r1.r + (r2.r - r1.r) * rate;
				c.g = r1.g + (r2.g - r1.g) * rate;
				c.b = r1.b + (r2.b - r1.b) * rate;
				c.a = r1.a + (r2.a - r1.a) * rate;
				return c;
			}
		}
		return dst.get(0).color;
	}

	public int getAngle(long time, MainState state) {
		if (fixa != Integer.MIN_VALUE) {
			return fixa;
		}
		final int timer = dsttimer;

		if (timer != 0 && timer < 256) {
			if (state.getTimer()[timer] == Long.MIN_VALUE) {
				return 0;
			}
			time -= state.getTimer()[timer];
		}
		if (time < 0) {
			return 0;
		}

		if (dst.size() == 0) {
			return 0;
		}
		long lasttime = dst.get(dst.size() - 1).time;
		if (lasttime > 0 && time > dstloop) {
			if (lasttime == dstloop) {
				time = dstloop;
			} else {
				time = (time - dstloop) % (lasttime - dstloop) + dstloop;
			}
		}
		for (int i = 0; i < dst.size() - 1; i++) {
			final SkinObjectDestination obj1 = dst.get(i);
			final SkinObjectDestination obj2 = dst.get(i + 1);
			if (obj1.time <= time && obj2.time >= time) {
				final int r1 = obj1.angle;
				final int r2 = obj2.angle;
				return (int) (r1 + (r2 - r1) * (time - obj1.time) / (obj2.time - obj1.time));
			}
		}
		return dst.get(0).angle;
	}

	public abstract void draw(SpriteBatch sprite, long time, MainState state);

	private final float[] CENTERX = { 0.5f, 0, 0.5f, 1, 0, 0.5f, 1, 0, 0.5f, 1, };
	private final float[] CENTERY = { 0.5f, 0, 0, 0, 0.5f, 0.5f, 0.5f, 1, 1, 1 };

	private float centerx;
	private float centery;

	protected void draw(SpriteBatch sprite, TextureRegion image, float x, float y, float width, float height,
			Color color, int angle) {
		if (color == null || image == null) {
			return;
		}
		final Color c = sprite.getColor();
		switch (dstblend) {
		case 2:
			sprite.setBlendFunction(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			break;
			case 3:
			sprite.setBlendFunction(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
				break;
			case 9:
			sprite.setBlendFunction(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ZERO);
			break;
		}
		sprite.setColor(color);
		
//		if(dstfilter == 1) {
//			image.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
//		} else {
//			image.getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);			
//		}
		if (angle != 0) {
			sprite.draw(image, x, y, centerx * width, centery * height, width, height, 1, 1, angle);
		} else {
			sprite.draw(image, x, y, width, height);
		}
		sprite.setColor(c);
		if (dstblend >= 2) {
			sprite.setBlendFunction(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		}
	}
	
	protected boolean mousePressed(MainState state, int button, int x, int y) {
		if (clickevent != -1) {
			Rectangle r = getDestination(state.getNowTime(), state);
			// System.out.println(obj.getClickevent() + " : " + r.x +
			// "," + r.y + "," + r.width + "," + r.height + " - " + x +
			// "," + y);
			if (r != null && r.x <= x && r.x + r.width >= x && r.y <= y && r.y + r.height >= y) {
				state.executeClickEvent(clickevent);
				return true;
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

	public class SkinObjectDestination {

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

	public abstract void dispose();

	public int getOffsetx() {
		return offsetx;
	}

	public void setOffsetx(int offsetX) {
		this.offsetx = offsetX;
	}

	public int getOffsety() {
		return offsety;
	}

	public void setOffsety(int offsetY) {
		this.offsety = offsetY;
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

}
