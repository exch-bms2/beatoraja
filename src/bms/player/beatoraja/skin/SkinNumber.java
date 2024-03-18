package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.property.IntegerProperty;
import bms.player.beatoraja.skin.property.IntegerPropertyFactory;

import bms.player.beatoraja.skin.property.TimerProperty;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * 数字オブジェクト
 *
 * @author exch
 */
public final class SkinNumber extends SkinObject {

	/**
	 * プラス値用イメージ
	 */
	private final SkinSourceSet image;
	/**
	 * マイナス値用イメージ
	 */
	private final SkinSourceSet mimage;
	/**
	 * 数値参照先
	 */
	private final IntegerProperty ref;
	/**
	 * 表示桁数
	 */
	public final int keta;

	public final int zeropadding;

	public final int space;

	public final int align;

	private int value = Integer.MIN_VALUE;
	private int shiftbase;

	private SkinOffset[] offsets;
	/**
	 * 現在の描画幅
	 */
	private float length;

	private TextureRegion[] currentImages;
	private TextureRegion[] imageSet;

	private float shift;

	public SkinNumber(TextureRegion[][] image, int timer, int cycle, int keta, int zeropadding, int space, int rid, int align) {
		this(image, null, timer, cycle, keta, zeropadding, space, rid, align);
	}

	public SkinNumber(TextureRegion[][] image, int timer, int cycle, int keta, int zeropadding, int space, IntegerProperty ref, int align) {
		this(image, null, timer, cycle, keta, zeropadding, space, ref, align);
	}

	public SkinNumber(TextureRegion[][] image, TimerProperty timer, int cycle, int keta, int zeropadding, int space, int rid, int align) {
		this(image, null, timer, cycle, keta, zeropadding, space, rid, align);
	}

	public SkinNumber(TextureRegion[][] image, TimerProperty timer, int cycle, int keta, int zeropadding, int space, IntegerProperty ref, int align) {
		this(image, null, timer, cycle, keta, zeropadding, space, ref, align);
	}

	public SkinNumber(TextureRegion[][] image, TextureRegion[][] mimage, int timer, int cycle, int keta, int zeropadding, int space, int id, int align) {
		this.image = new SkinSourceImageSet(image, timer, cycle) ;
		this.mimage = mimage != null ? new SkinSourceImageSet(mimage, timer, cycle) : null;
		this.keta = keta;
		this.currentImages = new TextureRegion[keta];
		this.zeropadding = zeropadding;
		this.space = space;
		ref = IntegerPropertyFactory.getIntegerProperty(id);
		this.align = align;
	}

	public SkinNumber(TextureRegion[][] image, TextureRegion[][] mimage, int timer, int cycle, int keta, int zeropadding, int space, IntegerProperty ref, int align) {
		this.image = new SkinSourceImageSet(image, timer, cycle) ;
		this.mimage = mimage != null ? new SkinSourceImageSet(mimage, timer, cycle) : null;
		this.keta = keta;
		this.currentImages = new TextureRegion[keta];
		this.zeropadding = zeropadding;
		this.space = space;
		this.ref = ref;
		this.align = align;
	}

	public SkinNumber(TextureRegion[][] image, TextureRegion[][] mimage, TimerProperty timer, int cycle, int keta, int zeropadding, int space, int id, int align) {
		this.image = new SkinSourceImageSet(image, timer, cycle) ;
		this.mimage = mimage != null ? new SkinSourceImageSet(mimage, timer, cycle) : null;
		this.keta = keta;
		this.currentImages = new TextureRegion[keta];
		this.zeropadding = zeropadding;
		this.space = space;
		ref = IntegerPropertyFactory.getIntegerProperty(id);
		this.align = align;
	}

	public SkinNumber(TextureRegion[][] image, TextureRegion[][] mimage, TimerProperty timer, int cycle, int keta, int zeropadding, int space, IntegerProperty ref, int align) {
		this.image = new SkinSourceImageSet(image, timer, cycle) ;
		this.mimage = mimage != null ? new SkinSourceImageSet(mimage, timer, cycle) : null;
		this.keta = keta;
		this.currentImages = new TextureRegion[keta];
		this.zeropadding = zeropadding;
		this.space = space;
		this.ref = ref;
		this.align = align;
	}

	public int getKeta() {
		return keta;
	}

	public void setOffsets(SkinOffset[] offsets) {
		this.offsets = offsets;
	}

	public void prepare(long time, MainState state) {
		prepare(time, state, 0, 0);
	}

	public void prepare(long time, MainState state, float offsetX, float offsetY) {
		prepare(time, state, ref != null ? ref.get(state) : Integer.MIN_VALUE, offsetX, offsetY);
	}

	public void prepare(long time, MainState state, int value, float offsetX, float offsetY) {
		if (value == Integer.MIN_VALUE || value == Integer.MAX_VALUE) {
			length = 0;
			draw = false;
			return;
		}
		final SkinSourceSet images = (value >= 0 || mimage == null) ? this.image : mimage;
		if (images == null) {
			length = 0;
			draw = false;
			return;
		}
		super.prepare(time, state, offsetX, offsetY);
		if(!draw) {
			length = 0;
			return;
		}
		TextureRegion[] image = images.getImages(time, state);
		if(image == null) {
			length = 0;
			draw = false;
			return;
		}

		if(this.value != value || this.imageSet != image) {
			this.value = value;
			this.imageSet = image;
			shiftbase = 0;
			value = Math.abs(value);
			for (int j = currentImages.length - 1; j >= 0; j--) {
				if(mimage != null && zeropadding > 0) {
					if(j == 0) {
						currentImages[j] = image[11];
					} else if(value > 0 || j == currentImages.length - 1) {
						currentImages[j] = image[value % 10];
					} else {
						currentImages[j] = image[zeropadding == 2 ? 10 : 0];
					}
				} else {
					if (value > 0 || j == currentImages.length - 1) {
						currentImages[j] = image[value % 10];
					} else {
						currentImages[j] = (zeropadding == 2 ? image[10] : (zeropadding == 1 ? image[0] : (mimage != null
								&& (currentImages[j + 1] != image[11] && currentImages[j + 1] != null) ? image[11] : null)));
					}
				}
				if(currentImages[j] == null) {
					shiftbase++;
				} else {
				}
				value /= 10;
			}
		}
		length = (region.width + space) * (currentImages.length - shiftbase);
		shift = align == 0 ? 0 : (align == 1 ? (region.width + space) * shiftbase : (region.width + space) * 0.5f * shiftbase);
	}

	public void draw(SkinObjectRenderer sprite) {
		for (int j = 0; j < currentImages.length; j++) {
			if (currentImages[j] != null) {
				if(offsets != null && j < offsets.length) {
					draw(sprite, currentImages[j], region.x + (region.width + space) * j - shift + offsets[j].x, region.y + offsets[j].y, region.width + offsets[j].w, region.height + offsets[j].h);
				} else {
					draw(sprite, currentImages[j], region.x + (region.width + space) * j - shift, region.y, region.width, region.height);
				}
			}
		}
	}

	public void draw(SkinObjectRenderer sprite, long time, int value, MainState state, float offsetX, float offsetY) {
		prepare(time, state, value, offsetX,offsetY);
		if(draw) {
			draw(sprite);
		}
	}

	/**
	 * 現在表示中の数値の幅を返す。
	 *
	 * @return 現在表示中の数値の幅
	 */
	public float getLength() {
		return length;
	}

	public void dispose() {
		disposeAll(image, mimage);
		setDisposed();
	}
}
