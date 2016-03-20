package bms.player.beatoraja.skin;

import java.util.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * スキンパーツ
 * 
 * @author exch
 */
public class SkinObject {
	
	/**
	 * イメージ
	 */
	private TextureRegion image;
	
	private List<SkinObjectDestination> dst = new ArrayList<SkinObjectDestination>();

	private int timing;
	private int[] option = new int[3];

	public TextureRegion getImage() {
		return image;
	}
	
	public void setImage(TextureRegion image) {
		this.image = image;
	}
	
	public void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
			int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3) {
		SkinObjectDestination obj = new SkinObjectDestination();
		obj.time = time;
		obj.region = new Rectangle(x,y,w,h);
		obj.acc = acc;
		obj.color = new Color(r / 256.0f, g/ 256.0f, b / 256.0f, a / 256.0f);
		obj.blend = blend;
		obj.filter = filter;
		obj.angle = angle;
		obj.center = center;
		obj.loop = loop;
		obj.timer = timer;
		obj.op = new int[]{op1, op2,op3};
		for(int i = 0;i < dst.size();i++) {
			if(dst.get(i).time > time) {
				dst.add(i, obj);
				return;
			}
		}
		dst.add(obj);
	}

	public Rectangle getDestination(long time) {
		for(int i = 0; i < dst.size();i++) {
			final SkinObjectDestination obj1 = dst.get(i);
			if(obj1.time <= time) {
				if(i + 1 == dst.size()) {
					return obj1.region;
				} else {
					final Rectangle r1 = obj1.region;
					final long time2 = dst.get(i + 1).time;					
					final Rectangle r2 = dst.get(i + 1).region;
					float x = r1.x + (r2.x - r1.x) * (time - obj1.time) / (time2 - obj1.time);
					float y = r1.y + (r2.y - r1.y) * (time - obj1.time) / (time2 - obj1.time);
					float w = r1.width + (r2.width - r1.width) * (time - obj1.time) / (time2 - obj1.time);
					float h = r1.height + (r2.height - r1.height) * (time - obj1.time) / (time2 - obj1.time);
					return new Rectangle(x,y,w,h);
				}
			}
		}
		return null;
	}

	public int getTiming() {
		return timing;
	}

	public void setTiming(int timing) {
		this.timing = timing;
	}

	public int[] getOption() {
		return option;
	}

	public void setOption(int[] option) {
		this.option = option;
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
		public int timer;
		public int[] op = new int[3];
		
	}
}