package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * �빊耶쀣궕�깣�궦�궒�궚�깉
 * 
 * @author exch
 */
public class SkinNumber extends SkinObject implements SkinObserver  {

	/**
	 * �깤�꺀�궧�ㅷ뵪�궎�깳�꺖�궦
	 */
	private SkinSource image;
	/**
	 * �깯�궎�깏�궧�ㅷ뵪�궎�깳�꺖�궦
	 */
	private SkinSource mimage;

	private int id = -1;
	/**
	 * 烏①ㅊ旅곫빊
	 */
	private int keta;

	private int zeropadding;

	private int align;

	private int value = Integer.MIN_VALUE;
	private int[] values;
	private int shiftbase;

	private SkinOffset[] offsets;
	/**
	 * �뤎�쑉�겗�룒�뵽亮�
	 */
	private float length;

	public SkinNumber(TextureRegion[] image, int keta, int zeropadding) {
		this(image, keta, zeropadding, -1);
	}

	public SkinNumber(TextureRegion[][] image, int timer, int cycle, int keta, int zeropadding, int rid) {
		this(image, null, timer, cycle, keta, zeropadding, rid);
	}

	public SkinNumber(TextureRegion[] image, int keta, int zeropadding, int rid) {
		this(image, null, keta, zeropadding, rid);
	}

	public SkinNumber(TextureRegion[] image, TextureRegion[] mimage, int keta, int zeropadding, int id) {
		this.image = new SkinSourceImage(new TextureRegion[][]{ image }, 0, 0) ;
		this.mimage = mimage != null ? new SkinSourceImage(new TextureRegion[][]{ mimage }, 0, 0) : null;
		this.setKeta(keta);
		this.zeropadding = zeropadding;
		this.id = id;
	}

	public SkinNumber(TextureRegion[][] image, TextureRegion[][] mimage, int timer, int cycle, int keta, int zeropadding, int id) {
		this.image = new SkinSourceImage(image, timer, cycle) ;
		this.mimage = mimage != null ? new SkinSourceImage(mimage, timer, cycle) : null;
		this.setKeta(keta);
		this.zeropadding = zeropadding;
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getKeta() {
		return keta;
	}

	public void setKeta(int keta) {
		this.keta = keta;
		this.values = new int[keta];
	}
	
	public void setOffsets(SkinOffset[] offsets) {
		this.offsets = offsets;
	}

	public void draw(SkinObjectRenderer sprite, long time, MainState state) {
		int value = Integer.MIN_VALUE;
		if (id != -1) {
			value = state.getNumberValue(id);
		}
		if (value != Integer.MIN_VALUE && value != Integer.MAX_VALUE) {
			draw(sprite, time, value, state);
		}
	}

	public void draw(SkinObjectRenderer sprite, long time, int value, MainState state) {
		draw(sprite, time, value, state, 0,0);
	}

	public void draw(SkinObjectRenderer sprite, long time, int value, MainState state, float offsetX, float offsetY) {
		Rectangle r = this.getDestination(time, state);
		if (r == null) {
			length = 0;
			return;
		}
		final SkinSource images = (value >= 0 || mimage == null) ? this.image : mimage;
		if (images == null) {
			length = 0;
			return;
		}
		TextureRegion[] image = images.getImages(time, state);
		if(image == null) {
			length = 0;
			return;
		}

		if(this.value != value) {
			this.value = value;
			shiftbase = 0;
			value = Math.abs(value);
			for (int j = values.length - 1; j >= 0; j--) {
				if(mimage != null && zeropadding > 0) {
					if(j == 0) {
						values[j] = 11;
					} else if(value > 0 || j == values.length - 1) {
						values[j] = value % 10;
					} else {
						values[j] = zeropadding == 2 ? 10 : 0;
					}
				} else {
					if (value > 0 || j == values.length - 1) {
						values[j] = value % 10;
					} else {
						values[j] = (zeropadding == 2 ? 10 : (zeropadding == 1 ? 0 : (mimage != null
								&& (values[j + 1] != 11 && values[j + 1] != -1) ? 11 : -1)));
					}
				}
				if(values[j] == -1) {
					shiftbase++;
				} else {
				}
				value /= 10;
			}
		}
		length = r.width * (values.length - shiftbase);
		float shift = align == 0 ? 0 : (align == 1 ? r.width * shiftbase : r.width * 0.5f * shiftbase);
		for (int j = 0; j < values.length; j++) {
			if (values[j] != -1) {
				if(offsets != null && j < offsets.length) {
					draw(sprite, image[values[j]], r.x + r.width * j + offsetX - shift + offsets[j].x, r.y + offsetY + offsets[j].y, r.width + offsets[j].w, r.height + offsets[j].h, state);
				} else {
					draw(sprite, image[values[j]], r.x + r.width * j + offsetX - shift, r.y + offsetY, r.width, r.height, state);
				}
			}
		}
	}
	
	public float getLength() {
		return length;
	}

	public void dispose() {
		if (image != null) {
			image.dispose();
			image = null;
		}
		if (mimage != null) {
			mimage.dispose();
			mimage = null;
		}
	}

	public int getAlign() {
		return align;
	}

	public void setAlign(int align) {
		this.align = align;
	}
}
