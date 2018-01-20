package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

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

	private int id = -1;
	/**
	 * 表示桁数
	 */
	private int keta;

	private int zeropadding;

	private int align;

	private TextureRegion[] values;

	private SkinOffset[] offsets;

	private int length;

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
		this.values = new TextureRegion[keta];
	}
	
	public void setOffsets(SkinOffset[] offsets) {
		this.offsets = offsets;
	}

	public TextureRegion[] getValue(long time, int value, int zeropadding, MainState state) {
		final SkinSource images = (value >= 0 || mimage == null) ? this.image : mimage;
		if (images == null) {
			return new TextureRegion[0];
		}
		TextureRegion[] image = images.getImages(time, state);
		if(image == null) {
			return new TextureRegion[0];
		}

		value = Math.abs(value);
		for (int j = values.length - 1; j >= 0; j--) {
			if (value > 0 || j == values.length - 1) {
				values[j] = image[value % 10];
			} else {
				values[j] = (zeropadding == 2 ? image[10] : (zeropadding == 1 ? image[0] : (mimage != null
						&& (values[j + 1] != image[11] && values[j + 1] != null) ? image[11] : null)));
			}
			value /= 10;
		}
		return values;
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
		length = 0;
		if (r != null) {
			TextureRegion[] values = getValue(time, value, zeropadding, state);
			int shift = 0;
			if (align == 1) {
				for (int j = 0; j < values.length && values[j] == null; j++) {
					shift += r.width;
				}
			} else if (align == 2) {
				for (int j = 0; j < values.length && values[j] == null; j++) {
					shift += r.width * 0.5f;
				}
			}
			for (int j = 0; j < values.length; j++) {
				if (values[j] != null) {
					if(offsets != null && j < offsets.length) {
						draw(sprite, values[j], r.x + r.width * j + offsetX - shift + offsets[j].x, r.y + offsetY + offsets[j].y, r.width + offsets[j].w, r.height + offsets[j].h);
					} else {
						draw(sprite, values[j], r.x + r.width * j + offsetX - shift, r.y + offsetY, r.width, r.height);						
					}
					length += r.width;
				}
			}
		}
	}
	
	public int getLength() {
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
