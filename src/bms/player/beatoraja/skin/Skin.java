package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import java.util.*;
import java.util.logging.Logger;

public class Skin {

	private float width;
	private float height;

	private float dw;
	private float dh;

	private List<SkinObject> objects = new ArrayList<SkinObject>();
	private List<SkinObject> removes = new ArrayList<SkinObject>();
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

	private Map<Integer, Boolean> option = new HashMap<Integer, Boolean>();
	private int[] fixopt;

	public Skin(float orgw, float orgh, float dstw, float dsth) {
		this(orgw, orgh, dstw, dsth, new int[0]);
	}

	public Skin(float orgw, float orgh, float dstw, float dsth, int[] fixopt) {
		width = dstw;
		height = dsth;
		dw = dstw / orgw;
		dh = dsth / orgh;
		this.fixopt = fixopt;
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
	
	public void prepare(MainState state) {
		
		for(SkinObject obj : objects) {
			List<Integer> l = new ArrayList();
			for(int op : obj.getOption()) {
				if(op > 0) {
					if(option.containsKey(op)) {
						if(!option.get(op)) {
							removes.add(obj);						
						}
					} else {
						boolean fix = false;
						for(int fop : fixopt) {
							if(op == fop) {
								fix = true;
								if(!state.getBooleanValue(op)) {
									removes.add(obj);						
								}							
								break;
							}
						}
						if(!fix) {
							l.add(op);
						}
					}					
				} else {
					if(option.containsKey(-op)) {
						if(option.get(-op)) {
							removes.add(obj);						
						}
					} else {
						boolean fix = false;
						for(int fop : fixopt) {
							if(-op == fop) {
								fix = true;
								if(state.getBooleanValue(-op)) {
									removes.add(obj);						
								}							
								break;
							}
						}
						if(!fix) {
							l.add(op);
						}
					}
				}				
			}
			
			int[] newop = new int[l.size()];
			for(int i = 0;i < newop.length;i++) {
				newop[i] = l.get(i);
			}
			obj.setOption(newop);
 		}
		Logger.getGlobal().info("描画されないことが確定しているSkinObject削除 : " + removes.size() + " / " + objects.size());
		objects.removeAll(removes);
		option.clear();
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
				if (!state.getBooleanValue(op)) {
					return false;
				}
			} else {
				if (state.getBooleanValue(-op)) {
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
		for (SkinObject obj : removes) {
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

	public Map<Integer,Boolean> getOption() {
		return option;
	}

	public void setOption(Map<Integer, Boolean> option) {
		this.option = option;
	}

	public float getWidth() {
		return width;
	}

	public float getHeight() {
		return height;
	}

	public double getScaleX() {
		return dw;
	}

	public double getScaleY() {
		return dh;
	}
}
