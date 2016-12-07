package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.List;

public class Skin {

	private float dw;
	private float dh;

	private List<SkinObject> objects = new ArrayList<SkinObject>();
	/**
	 * 入力受付開始時間(ms)
	 */
	private int input;
	/**
	 * シーンの時間(ms)
	 */
	private int scene = 3600000 * 24;
	/**
	 * シーン以降準備開始からシーン移行までの時間(ms)
	 */
	private int fadeout;

	private int[] option = new int[0];

	public Skin(float orgw, float orgh, float dstw, float dsth) {
		dw = dstw / orgw;
		dh = dsth / orgh;
	}

	protected void add(SkinObject object) {
		objects.add(object);
	}

	protected void setDestination(SkinObject object, long time, float x, float y, float w, float h, int acc, int a,
			int r, int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2,
			int op3) {
		object.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op1, op2, op3);
	}

	protected void setDestination(SkinObject object, long time, float x, float y, float w, float h, int acc, int a,
			int r, int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int[] op) {
		object.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op);
	}

	protected void addNumber(SkinNumber number, long time, float x, float y, float w, float h, int acc, int a, int r,
			int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3) {
		number.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op1, op2, op3);
		objects.add(number);
	}

	protected SkinImage addImage(TextureRegion tr, long time, float x, float y, float w, float h, int acc, int a,
			int r, int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2,
			int op3) {
		SkinImage si = new SkinImage(tr);
		si.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center, loop,
				timer, op1, op2, op3);
		objects.add(si);
		return si;
	}

	public SkinObject[] getAllSkinObjects() {
		return objects.toArray(new SkinObject[objects.size()]);
	}

	public void removeSkinObject(SkinObject obj) {
		objects.remove(obj);
	}

	public void drawAllObjects(SpriteBatch sprite, MainState state) {
		final long time = state.getNowTime();
		for (SkinObject obj : objects) {
			if (isDraw(obj.getOption(), state)) {
				obj.draw(sprite, time, state);
			}
		}
	}

	private final boolean isDraw(int[] opt, MainState state) {
		for (int op : opt) {
			if (op > 0) {
				if (state.getBooleanValue(op)) {
					continue;
				}
				boolean soption = false;
				for (int sop : option) {
					if (op == sop) {
						soption = true;
						break;
					}
				}
				if (!soption) {
					return false;
				}
			} else {
				if (!state.getBooleanValue(-op)) {
					continue;
				}
				boolean soption = true;
				for (int sop : option) {
					if (-op == sop) {
						soption = false;
						break;
					}
				}
				if (!soption) {
					return false;
				}
			}
		}
		return true;
	}

	public void mousePressed(MainState state, int button, int x, int y) {
		for (int i = objects.size() - 1; i >= 0; i--) {
			final SkinObject obj = objects.get(i);
			if (isDraw(obj.getOption(), state) && obj.mousePressed(state, button, x, y)) {
				break;
			}
		}
	}

	public void mouseDragged(MainState state, int button, int x, int y) {
		for (int i = objects.size() - 1; i >= 0; i--) {
			final SkinObject obj = objects.get(i);
			if (obj instanceof SkinSlider && isDraw(obj.getOption(), state) && obj.mousePressed(state, button, x, y)) {
				break;
			}
		}
	}

	public void dispose() {
		for (SkinObject obj : objects) {
			obj.dispose();
		}

	}

	public int getFadeout() {
		return fadeout;
	}

	public void setFadeout(int fadeout) {
		this.fadeout = fadeout;
	}

	public int getInput() {
		return input;
	}

	public void setInput(int input) {
		this.input = input;
	}

	public int getScene() {
		return scene;
	}

	public void setScene(int scene) {
		this.scene = scene;
	}

	public int[] getOption() {
		return option;
	}

	public void setOption(int[] option) {
		this.option = option;
	}
}
