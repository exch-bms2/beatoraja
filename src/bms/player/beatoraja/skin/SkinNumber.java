package bms.player.beatoraja.skin;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class SkinNumber {

	/**
	 * イメージ
	 */
	private TextureRegion[] image;
	
	private List<SkinObjectDestination> dst = new ArrayList<SkinObjectDestination>();


	private int id;
	
	private int cycle;
	
	private int keta;
	
	public void setImage(TextureRegion[] image, int cycle) {
		this.image = image;
		this.cycle = cycle;
	}
	
	public TextureRegion[] getImage() {
		return image;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setDestination(long time, float x, float y, float w, float h, int acc, int a, int r, int g, int b,
			int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3) {
		SkinObjectDestination obj = new SkinObjectDestination();
		obj.time = time;
		obj.region = new Rectangle(x,y,w,h);
		obj.acc = acc;
		obj.color = new Color(r / 255.0f, g/ 255.0f, b / 255.0f, a / 255.0f);
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
		if(dst.size() == 0) {
			System.out.println("void image");
			return new Rectangle(0,0,0,0);
		}
		long lasttime = dst.get(dst.size() - 1).time;
		int loop = dst.get(0).loop;
		if(lasttime > 0 && time > loop) {
			if(lasttime - loop == 0) {
				time = loop;
			} else {
				time = (time - loop) % (lasttime - loop) + loop;				
			}
		}
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

	public Color getColor(long time) {
		if(dst.size() == 0) {
			System.out.println("void color");
			return new Color(0,0,0,0);
		}
		long lasttime = dst.get(dst.size() - 1).time;
		int loop = dst.get(0).loop;
		if(lasttime > 0 && time > loop) {
			if(lasttime - loop == 0) {
				time = loop;
			} else {
				time = (time - loop) % (lasttime - loop) + loop;				
			}
		}
		for(int i = 0; i < dst.size();i++) {
			final SkinObjectDestination obj1 = dst.get(i);
			if(obj1.time <= time) {
				if(i + 1 == dst.size()) {
					return obj1.color;
				} else {
					final Color r1 = obj1.color;
					final long time2 = dst.get(i + 1).time;					
					final Color r2 = dst.get(i + 1).color;
					float r = r1.r + (r2.r - r1.r) * (time - obj1.time) / (time2 - obj1.time);
					float g = r1.g + (r2.g - r1.g) * (time - obj1.time) / (time2 - obj1.time);
					float b = r1.b + (r2.b - r1.b) * (time - obj1.time) / (time2 - obj1.time);
					float a = r1.a + (r2.a - r1.a) * (time - obj1.time) / (time2 - obj1.time);
					return new Color(r,g,b,a);
				}
			}
		}
		return null;
	}
	
	public int getKeta() {
		return keta;
	}

	public void setKeta(int keta) {
		this.keta = keta;
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
