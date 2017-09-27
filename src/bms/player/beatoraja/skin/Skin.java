package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Resolution;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.*;
import java.util.logging.Logger;

/**
 * スキン
 * 
 * @author exch
 */
public class Skin {

	/**
	 * 幅
	 */
	private final float width;
	/**
	 * 高さ
	 */
	private final float height;
	/**
	 * 元データからの幅比率
	 */
	private final float dw;
	/**
	 * 元データからの高さ比率
	 */
	private final float dh;

	/**
	 * 登録されているスキンオブジェクト
	 */
	private List<SkinObject> objects = new ArrayList<SkinObject>();
	private SkinObject[] objectarray = new SkinObject[0];
	/**
	 * 除外されているスキンオブジェクト
	 */
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
	
	private Map<Integer, int[]> offset = new HashMap<Integer, int[]>();

	/**
	 * 読み込み時から不変であることが確定しているop
	 */
	private int[] fixopt;

	public Skin(Resolution org, Resolution dst) {
		this(org, dst, new int[0]);
	}

	public Skin(Resolution org, Resolution dst, int[] fixopt) {
		width = dst.width;
		height = dst.height;
		dw = ((float)dst.width) / org.width;
		dh = ((float)dst.height) / org.height;
		this.fixopt = fixopt;
	}

	public void add(SkinObject object) {
		objects.add(object);
	}

	public void setDestination(SkinObject object, long time, float x, float y, float w, float h, int acc, int a,
			int r, int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2,
			int op3, int offset) {
		object.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op1, op2, op3, offset);
	}

	public void setDestination(SkinObject object, long time, float x, float y, float w, float h, int acc, int a,
			int r, int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int[] op) {
		object.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op);
	}

	public void addNumber(SkinNumber number, long time, float x, float y, float w, float h, int acc, int a, int r,
			int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3, int offset) {
		number.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center,
				loop, timer, op1, op2, op3, offset);
		objects.add(number);
	}

	public SkinImage addImage(TextureRegion tr, long time, float x, float y, float w, float h, int acc, int a,
			int r, int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2,
			int op3, int offset) {
		SkinImage si = new SkinImage(tr);
		si.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center, loop,
				timer, op1, op2, op3, offset);
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
			if(obj.getAllDestination().length == 0) {
				removes.add(obj);
			} else {
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
			
 		}
		Logger.getGlobal().info("描画されないことが確定しているSkinObject削除 : " + removes.size() + " / " + objects.size());
		objects.removeAll(removes);
		objectarray = objects.toArray(new SkinObject[objects.size()]);
		option.clear();
	}

	public void drawAllObjects(SpriteBatch sprite, MainState state) {
		final long time = state.getNowTime();
		for (SkinObject obj : objectarray) {
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
		for (int i = objectarray.length - 1; i >= 0; i--) {
			final SkinObject obj = objectarray[i];
			if (isDraw(obj.getOption(), state) && obj.mousePressed(state, button, x, y)) {
				break;
			}
		}
	}

	public void mouseDragged(MainState state, int button, int x, int y) {
		for (int i = objectarray.length - 1; i >= 0; i--) {
			final SkinObject obj = objectarray[i];
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

	public Map<Integer, int[]> getOffset() {
		return offset;
	}

	public void setOffset(Map<Integer, int[]> offset) {
		this.offset = offset;
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
