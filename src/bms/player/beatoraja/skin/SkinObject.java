package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

/**
 * スキンオブジェクト
 * 
 * @author exch
 */
public abstract class SkinObject {

	/**
	 * X座標オフセットの参照ID
	 */
	private int offsetx = -1;
	/**
	 * Y座標オフセットの参照ID
	 */
	private int offsety = -1;

	private int timer;
	private int cycle;

	private int dsttimer = 0;
	private int dstloop = 0;
	private int dstblend = 0;
	private int dstfilter;
	private int dstcenter;

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
		}
		if (dsttimer == 0) {
			dsttimer = timer;
		}
		if (dstloop == 0) {
			dstloop = loop;
		}
		if (dstop.length == 0) {
			dstop = new int[] { op1, op2, op3 };
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
//			System.out.println("void image");
			return new Rectangle(0, 0, 0, 0);
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
		long lasttime = dst.get(dst.size() - 1).time;
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
					final long time2 = obj2.time;
					final Rectangle r2 = dst.get(i + 1).region;
					r.x = r1.x + (r2.x - r1.x) * (time - obj1.time) / (time2 - obj1.time);
					r.y = r1.y + (r2.y - r1.y) * (time - obj1.time) / (time2 - obj1.time);
					r.width = r1.width + (r2.width - r1.width) * (time - obj1.time) / (time2 - obj1.time);
					r.height = r1.height + (r2.height - r1.height) * (time - obj1.time) / (time2 - obj1.time);
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
//			System.out.println("void color");
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
				final Color r2 = dst.get(i + 1).color;
				c.r = r1.r + (r2.r - r1.r) * (time - obj1.time) / (obj2.time - obj1.time);
				c.g = r1.g + (r2.g - r1.g) * (time - obj1.time) / (obj2.time - obj1.time);
				c.b = r1.b + (r2.b - r1.b) * (time - obj1.time) / (obj2.time - obj1.time);
				c.a = r1.a + (r2.a - r1.a) * (time - obj1.time) / (obj2.time - obj1.time);
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
				final int r2 = dst.get(i + 1).angle;
				return (int) (r1 + (r2 - r1) * (time - obj1.time) / (obj2.time - obj1.time));
			}
		}
		return dst.get(0).angle;
	}

	public abstract void draw(SpriteBatch sprite, long time, MainState state);

	private final float[] centerx = { 0.5f, 0, 0.5f, 1,0, 0.5f, 1,0, 0.5f, 1,};
	private final float[] centery = { 0.5f, 0, 0, 0, 0.5f, 0.5f, 0.5f, 1, 1, 1 };

	protected void draw(SpriteBatch sprite, TextureRegion image, float x, float y, float width, float height,
			Color color, int angle) {
		if (color == null || image == null) {
			return;
		}
		Color c = sprite.getColor();
		switch(dstblend) {
			case 2:
				sprite.setBlendFunction(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
				break;
			case 9:
				sprite.setBlendFunction(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ZERO);
				break;
		}
		sprite.setColor(color);
		sprite.draw(image, x, y, centerx[dstcenter] * width, centery[dstcenter] * height, width, height, 1, 1, angle);
		sprite.setColor(c);
		if (dstblend >= 2) {
			sprite.setBlendFunction(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		}
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

	public int getTimer() {
		return timer;
	}

	public void setTimer(int timing) {
		this.timer = timing;
	}

	public int getCycle() {
		return cycle;
	}

	public void setCycle(int cycle) {
		this.cycle = cycle;
	}

	public int getDestinationTimer() {
		return dsttimer;
	}

}
