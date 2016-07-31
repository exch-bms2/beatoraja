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

    private List<SkinObjectDestination> dst = new ArrayList<SkinObjectDestination>();

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

    /**
     * 指定して時間に応じた描画領域を返す
     * @param time 時間(ms)
     * @return 描画領域
     */
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

        for(int i = 0; i < dst.size() - 1;i++) {
            final SkinObjectDestination obj1 = dst.get(i);
            final SkinObjectDestination obj2 = dst.get(i + 1);
            if(obj1.time <= time && obj2.time >= time) {
                    final Rectangle r1 = obj1.region;
                    final long time2 = obj2.time;
                    final Rectangle r2 = dst.get(i + 1).region;
                    float x = r1.x + (r2.x - r1.x) * (time - obj1.time) / (time2 - obj1.time);
                    float y = r1.y + (r2.y - r1.y) * (time - obj1.time) / (time2 - obj1.time);
                    float w = r1.width + (r2.width - r1.width) * (time - obj1.time) / (time2 - obj1.time);
                    float h = r1.height + (r2.height - r1.height) * (time - obj1.time) / (time2 - obj1.time);
                    return new Rectangle(x,y,w,h);
            }
        }
        return dst.get(0).region;
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
        for(int i = 0; i < dst.size() - 1;i++) {
            final SkinObjectDestination obj1 = dst.get(i);
            final SkinObjectDestination obj2 = dst.get(i + 1);
            if(obj1.time <= time && obj2.time >= time) {
                    final Color r1 = obj1.color;
                    final Color r2 = dst.get(i + 1).color;
                    float r = r1.r + (r2.r - r1.r) * (time - obj1.time) / (obj2.time - obj1.time);
                    float g = r1.g + (r2.g - r1.g) * (time - obj1.time) / (obj2.time - obj1.time);
                    float b = r1.b + (r2.b - r1.b) * (time - obj1.time) / (obj2.time - obj1.time);
                    float a = r1.a + (r2.a - r1.a) * (time - obj1.time) / (obj2.time - obj1.time);
                    return new Color(r,g,b,a);
            }
        }
        return dst.get(0).color;
    }

    public abstract void draw(SpriteBatch sprite, long time, MainState state);

	protected void draw(SpriteBatch sprite, TextureRegion image, float x, float y, float width, float height, Color color) {
		Color c = sprite.getColor();
		final int blend = dst.get(0).blend;
		final int angle = dst.get(0).angle;
		final int center = dst.get(0).center;
		final int filter = dst.get(0).filter;
		if(blend == 2) {
			sprite.setBlendFunction(GL11.GL_ONE, GL11.GL_ONE);			
		}
		sprite.setColor(color);
		sprite.draw(image, x, y, width, height);
		sprite.setColor(c);
		if(blend >= 2) {
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
        public int timer;
        public int[] op = new int[3];

    }

    public abstract void dispose();

}
