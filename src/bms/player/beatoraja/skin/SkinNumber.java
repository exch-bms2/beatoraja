package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.property.IntegerProperty;
import bms.player.beatoraja.skin.property.IntegerPropertyFactory;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * 数字オブジェクト
 * 
 * @author exch
 */
public class SkinNumber extends SkinObject {

	/**
	 * プラス値用イメージ
	 */
	private SkinSource image;
	/**
	 * マイナス値用イメージ
	 */
	private SkinSource mimage;

	private IntegerProperty ref;
	/**
	 * 表示桁数
	 */
	private int keta;

	private int zeropadding;

	private int align;

	private int value = Integer.MIN_VALUE;
	private int[] values;
	private int shiftbase;

	private SkinOffset[] offsets;
	/**
	 * 現在の描画幅
	 */
	private float length;

	public SkinNumber(TextureRegion[][] image, int timer, int cycle, int keta, int zeropadding, int rid) {
		this(image, null, timer, cycle, keta, zeropadding, rid);
	}

	public SkinNumber(TextureRegion[][] image, int timer, int cycle, int keta, int zeropadding, IntegerProperty ref) {
		this(image, null, timer, cycle, keta, zeropadding, ref);
	}

	public SkinNumber(TextureRegion[][] image, TextureRegion[][] mimage, int timer, int cycle, int keta, int zeropadding, int id) {
		this.image = new SkinSourceImage(image, timer, cycle) ;
		this.mimage = mimage != null ? new SkinSourceImage(mimage, timer, cycle) : null;
		this.setKeta(keta);
		this.zeropadding = zeropadding;
		ref = IntegerPropertyFactory.getIntegerProperty(id);
	}

	public SkinNumber(TextureRegion[][] image, TextureRegion[][] mimage, int timer, int cycle, int keta, int zeropadding, IntegerProperty ref) {
		this.image = new SkinSourceImage(image, timer, cycle) ;
		this.mimage = mimage != null ? new SkinSourceImage(mimage, timer, cycle) : null;
		this.setKeta(keta);
		this.zeropadding = zeropadding;
		this.ref = ref;
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
		if (ref != null) {
			value = ref.get(state);
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
