package bms.player.beatoraja.skin;

import bms.model.BMSModel;
import bms.player.beatoraja.MainState;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.List;

public class Skin {

	private float dw;
	private float dh;

	private List<SkinObject> objects = new ArrayList();
	
	private int input;
	
	private int scene = 3600000 * 24;

	private int fadeout;

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

	protected void addImage(TextureRegion tr, long time, float x, float y, float w, float h, int acc, int a, int r,
							int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3) {
		SkinImage si = new SkinImage(new TextureRegion[] {tr}, 0);
		si.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op1, op2, op3);
		objects.add(si);
	}

	public void drawAllObjects(SpriteBatch sprite, MainState state) {
		final long time = state.getNowTime();
		for(SkinObject obj : objects) {
			boolean draw = true;
			for(int op :obj.getOption()) {
				final boolean b = state.getBooleanValue(op);
				if((op > 0 && !b) || (op < 0 && b)) {
					draw = false;
					break;
				}
			}
			if(draw) {
				obj.draw(sprite, time, state);
			}
		}
	}

	public void dispose() {
		for(SkinObject obj : objects) {
			obj.dispose();
		}

	}

	public int getFadeoutTime() {
		return fadeout;
	}

	public void setFadeoutTime(int fadeout) {
		this.fadeout = fadeout;
	}

	public int getInputTime() {
		return input;
	}

	public void setInputTime(int input) {
		this.input = input;
	}

	public int getSceneTime() {
		return scene;
	}

	public void setSceneTime(int scene) {
		this.scene = scene;
	}
}
