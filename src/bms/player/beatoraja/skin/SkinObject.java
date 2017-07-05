package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainState;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.Arrays;
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
	/**
	 * 参照するタイマーID
	 */
	private int dsttimer = 0;
	/**
	 * ループ開始タイマー
	 */
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

	private int acc;
	/**
	 * オブジェクトクリック時に実行するイベントの参照ID
	 */
	private int clickevent = -1;
	/**
	 * 描画条件となるオプション定義
	 */
	private int[] dstop = new int[0];

	private final float[] CENTERX = { 0.5f, 0, 0.5f, 1, 0, 0.5f, 1, 0, 0.5f, 1, };
	private final float[] CENTERY = { 0.5f, 0, 0, 0, 0.5f, 0.5f, 0.5f, 1, 1, 1 };

	private float centerx;
	private float centery;

	private SkinObjectDestination[] dst = new SkinObjectDestination[0];
	
	// 以下、高速化用
	private long starttime;
	private long endtime;

	private Rectangle r = new Rectangle();
	private Color c = new Color();

	private Rectangle fixr = null;
	private Color fixc = null;
	private int fixa = Integer.MIN_VALUE;

	private long nowtime = 0;
	private float rate = 0;
	private int index = 0;
	
	public SkinObjectDestination[] getAllDestination() {
		return dst;
	}

	public void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
			int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3) {
		setDestination(time, x, y, w, h, acc, a, r, g, b, blend, filter, angle, center, loop, timer, new int[]{op1,op2,op3});
	}

	public void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
			int blend, int filter, int angle, int center, int loop, int timer, int[] op) {
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
		for (int i = 0; i < dst.length; i++) {
			if (dst[i].time > time) {
				List<SkinObjectDestination> l = new ArrayList<SkinObjectDestination>(Arrays.asList(dst));
				l.add(i, obj);
				dst = l.toArray(new SkinObjectDestination[l.size()]);
				starttime = dst[0].time;
				endtime = dst[dst.length - 1].time;
				return;
			}
		}
		List<SkinObjectDestination> l = new ArrayList<SkinObjectDestination>(Arrays.asList(dst));
		l.add(obj);
		dst = l.toArray(new SkinObjectDestination[l.size()]);
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

	/**
	 * 指定して時間に応じた描画領域を返す
	 * 
	 * @param time
	 *            時間(ms)
	 * @return 描画領域
	 */
	public Rectangle getDestination(long time, MainState state) {
		final int timer = dsttimer;

		if (timer != 0 && timer < MainController.timerCount) {
			final long stime = state.getTimer()[timer];
			if (stime == Long.MIN_VALUE) {
				return null;
			}
			time -= stime;
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
		
		if (fixr == null) {
			getRate();
			if(rate == 0) {
				r.set(dst[index].region);
			} else {
				final Rectangle r1 = dst[index].region;
				final Rectangle r2 = dst[index + 1].region;
				r.x = r1.x + (r2.x - r1.x) * rate;
				r.y = r1.y + (r2.y - r1.y) * rate;
				r.width = r1.width + (r2.width - r1.width) * rate;
				r.height = r1.height + (r2.height - r1.height) * rate;
			}
			if (state != null && offsetx != -1) {
				r.x += state.getSliderValue(offsetx);
			}
			if (state != null && offsety != -1) {
				r.y += state.getSliderValue(offsety);
			}
			return r;
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
	}

	public Color getColor() {
		if (fixc != null) {
			return fixc;
		}
		getRate();
		if(rate == 0) {
			return dst[index].color;			
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

	public int getAngle() {
		if (fixa != Integer.MIN_VALUE) {
			return fixa;
		}
		getRate();
		return rate == 0 ? dst[index].angle :  (int) (dst[index].angle + (dst[index + 1].angle - dst[index].angle) * rate);
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

	public abstract void draw(SpriteBatch sprite, long time, MainState state);

	protected void draw(SpriteBatch sprite, TextureRegion image, float x, float y, float width, float height) {
		draw(sprite, image, x, y, width, height, getColor(), getAngle());
	}

	protected void draw(SpriteBatch sprite, TextureRegion image, float x, float y, float width, float height,
			Color color, int angle) {
		if (color == null || color.a == 0f || image == null) {
			return;
		}
		final Color c = sprite.getColor();
		switch (dstblend) {
		case 2:
			sprite.setBlendFunction(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			break;
			case 3:
				// TODO 減算描画は難しいか？
				Gdx.gl.glBlendEquation(GL20.GL_FUNC_SUBTRACT);
				sprite.setBlendFunction(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
				Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD);
				break;
			case 4:
				sprite.setBlendFunction(GL11.GL_ZERO, GL11.GL_SRC_COLOR);
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
