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

	private int offsetX = -1;
	private int offsetY = -1;

	private int dsttimer = 0;
	private int[] dstop = new int[0];
	private List<SkinObjectDestination> dst = new ArrayList<SkinObjectDestination>();

	private Rectangle r = new Rectangle();
	private Color c = new Color();
	
	public void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
			int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3) {
		SkinObjectDestination obj = new SkinObjectDestination();
		obj.time = time;
		obj.region = new Rectangle(x, y, w, h);
		obj.acc = acc;
		obj.color = new Color(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
		obj.blend = blend;
		obj.filter = filter;
		obj.angle = angle;
		obj.center = center;
		obj.loop = loop;
		if(dsttimer == 0) {
			dsttimer = timer;			
		}
		if(dstop.length == 0) {
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
			System.out.println("void image");
			return new Rectangle(0, 0, 0, 0);
		}
		final int timer = dsttimer;

		if (timer != 0 && timer < 256) {
			if (state.getTimer()[timer] == -1) {
				return null;
			}
			time -= state.getTimer()[timer];
		}
		if(time < 0) {
			return null;
		}
		long lasttime = dst.get(dst.size() - 1).time;
		int loop = dst.get(0).loop;
		if (lasttime > 0 && time > loop) {
			if (lasttime - loop == 0) {
				time = loop;
			} else {
				time = (time - loop) % (lasttime - loop) + loop;
			}
		}

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
				if (state != null && offsetX != -1) {
					r.x += state.getSliderValue(offsetX);
				}
				if (state != null && offsetY != -1) {
					r.y += state.getSliderValue(offsetY);
				}
				return r;
			}
		}
		r.x = dst.get(0).region.x;
		r.y = dst.get(0).region.y;
		r.width = dst.get(0).region.width;
		r.height = dst.get(0).region.height;
		if (state != null && offsetX != -1) {
			r.x += state.getSliderValue(offsetX);
		}
		if (state != null && offsetY != -1) {
			r.y += state.getSliderValue(offsetY);
		}
		return r;
	}

	public Color getColor(long time, MainState state) {
		final int timer = dsttimer;

		if (timer != 0 && timer < 256) {
			if (state.getTimer()[timer] == -1) {
				return null;
			}
			time -= state.getTimer()[timer];
		}
		if(time < 0) {
			return null;
		}

		if (dst.size() == 0) {
			System.out.println("void color");
			return new Color(0, 0, 0, 0);
		}
		long lasttime = dst.get(dst.size() - 1).time;
		int loop = dst.get(0).loop;
		if (lasttime > 0 && time > loop) {
			if (lasttime - loop == 0) {
				time = loop;
			} else {
				time = (time - loop) % (lasttime - loop) + loop;
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

	public abstract void draw(SpriteBatch sprite, long time, MainState state);

	protected void draw(SpriteBatch sprite, TextureRegion image, float x, float y, float width, float height,
			Color color) {
		if(color == null || image == null) {
			return;
		}
		Color c = sprite.getColor();
		final int blend = dst.get(0).blend;
		final int angle = dst.get(0).angle;
		final int center = dst.get(0).center;
		final int filter = dst.get(0).filter;
		if (blend == 2) {
			sprite.setBlendFunction(GL11.GL_ONE, GL11.GL_ONE);
		}
		sprite.setColor(color);
		sprite.draw(image, x, y, width, height);
		sprite.setColor(c);
		if (blend >= 2) {
			sprite.setBlendFunction(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		}
	}

	private class SkinObjectDestination {

		public long time;
		/**
		 * 描画領域
		 */
		public Rectangle region;
		public int acc;
		public Color color;
		public int blend;
		public int filter;
		public int angle;
		public int center;
		public int loop;
	}

	public abstract void dispose();

	public void setOffsetXReferenceID(int offsetX) {
		this.offsetX = offsetX;
	}

	public void setOffsetYReferenceID(int offsetY) {
		this.offsetY = offsetY;
	}

}
