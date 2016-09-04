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

	private int input;

	private int scene = 3600000 * 24;

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

	protected void addNumber(SkinNumber number, long time, float x, float y, float w, float h, int acc, int a, int r,
			int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3) {
		number.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op1, op2, op3);
		objects.add(number);
	}

	protected void addImage(TextureRegion tr, long time, float x, float y, float w, float h, int acc, int a, int r,
			int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3) {
		SkinImage si = new SkinImage(new TextureRegion[] { tr }, 0);
		si.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center, loop,
				timer, op1, op2, op3);
		objects.add(si);
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
			boolean draw = true;
			for (int op : obj.getOption()) {
				boolean soption = false;
				if(op > 0) {
					for(int sop : option) {
						if(op == sop) {
							soption = true;
							break;
						}
					}					
				} else {
					soption = true;
					for(int sop : option) {
						if(-op == sop) {
							soption = false;
							break;
						}
					}					
				}
				if(soption) {
					continue;
				}
				final boolean b = state.getBooleanValue(op);
				if ((op > 0 && !b) || (op < 0 && b)) {
					draw = false;
					break;
				}
			}
			if (draw) {
				obj.draw(sprite, time, state);
			}
		}
	}

	public void mousePressed(MainState state, int x, int y) {
		final long time = state.getNowTime();
		for (SkinObject obj : objects) {
			if (obj instanceof SkinSlider && ((SkinSlider) obj).isChangable()) {
				final SkinSlider slider = (SkinSlider) obj;
				boolean draw = true;
				for (int op : obj.getOption()) {
					boolean soption = false;
					if(op > 0) {
						for(int sop : option) {
							if(op == sop) {
								soption = true;
								break;
							}
						}					
					} else {
						soption = true;
						for(int sop : option) {
							if(-op == sop) {
								soption = false;
								break;
							}
						}					
					}
					if(soption) {
						continue;
					}
					final boolean b = state.getBooleanValue(op);
					if ((op > 0 && !b) || (op < 0 && b)) {
						draw = false;
						break;
					}
				}
				if (draw) {
					Rectangle r = obj.getDestination(time, state);
					if (r != null) {
						int sa = slider.getSliderAngle();
						switch (sa) {
						case 0:
							if (r.x <= x && r.x + r.width >= x && r.y <= y && r.y + slider.getRange() >= y) {
								state.setSliderValue(slider.getType(), (y - r.y) / slider.getRange());
							}
							break;
						case 1:
							if (r.x <= x && r.x + slider.getRange() >= x && r.y <= y && r.y + r.height >= y) {
								state.setSliderValue(slider.getType(), (x - r.x) / slider.getRange());
							}
							break;
						case 2:
							if (r.x <= x && r.x + r.width >= x && r.y - slider.getRange() <= y && r.y >= y) {
								state.setSliderValue(slider.getType(), (r.y - y) / slider.getRange());
							}
							break;
						case 3:
							if (r.x <= x && r.x + slider.getRange() >= x && r.y <= y && r.y + r.height >= y) {
								state.setSliderValue(slider.getType(),
										(r.x + slider.getRange() - x) / slider.getRange());
							}
							break;
						}
					}
				}
			}
		}
	}

	public void dispose() {
		for (SkinObject obj : objects) {
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

	public int[] getOption() {
		return option;
	}

	public void setOption(int[] option) {
		this.option = option;
	}
}
