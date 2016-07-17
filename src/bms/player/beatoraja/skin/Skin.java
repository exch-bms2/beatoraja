package bms.player.beatoraja.skin;

import bms.model.BMSModel;
import bms.player.beatoraja.MainState;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.List;

public class Skin {

	private float dw;
	private float dh;

	private List<SkinObject> objects = new ArrayList();

	public Skin(float orgw, float orgh, float dstw, float dsth) {
		dw = dstw / orgw;
		dh = dsth / orgh;
	}

	protected void add(SkinObject object) {
		objects.add(object);
	}

	protected void setDestination(SkinObject object, long time, float x, float y, float w, float h, int acc, int a, int r,
								int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3) {
		object.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center, loop, timer, op1, op2, op3);
	}

	protected void addNumber(SkinNumber number, long time, float x, float y, float w, float h, int acc, int a, int r,
						   int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3) {
		number.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op1, op2, op3);
		objects.add(number);
	}

	public void drawAllObjects(SpriteBatch sprite, long time, MainState state) {
		for(SkinObject obj : objects) {
			obj.draw(sprite, time, state);
		}
	}
}
