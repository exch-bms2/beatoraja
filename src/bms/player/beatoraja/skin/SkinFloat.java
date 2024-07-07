package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.property.FloatProperty;
import bms.player.beatoraja.skin.property.FloatPropertyFactory;

import bms.player.beatoraja.skin.property.TimerProperty;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * 小数点数字オブジェクト
 *
 * @author keh
 */
final public class SkinFloat extends SkinObject {


	/**
	 * double値から各桁の配列を返すフォーマッター
	 */
	private FloatFormatter ff;
	/**
	 * プラス値用イメージ
	 */
	private SkinSourceSet image;
	/**
	 * マイナス値用イメージ
	 */
	private SkinSourceSet mimage;
	/**
	 * 数値参照先
	 */
	private FloatProperty ref;
	/**
	 * 整数表示桁数
	 */
	public final int iketa;
	/**
	 * 小数表示桁数
	 */
	public final int fketa;
	/**
	 * 符号を表示するか
	 */
	public final boolean isSignvisible;
	/**
	 * 値の倍数
	 */
	public final float gain;

	private int keta;
	/**
	 * 0:なし 1:表ゼロ 2:裏ゼロ
	 */
	public final int zeropadding;

	private int space;
	/**
	 * 0:左 1:右 2:中央
	 */
	private int align;

	private float value = Float.MIN_VALUE;
	private int shiftbase;

	private SkinOffset[] offsets;
	/**
	 * 現在の描画幅
	 */
	private float length;

	private TextureRegion[] currentImages;
	private TextureRegion[] imageSet;

	private float shift;

	/**
	 * コンストラクタ
	 * マイナス側イメージ有無
	 * TimerとValueがidとProperty
	 * 8パターン
	 *  */
	public SkinFloat(TextureRegion[][] image, int timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, int id, float gain) {
		this(image, null, timer, cycle, iketa, fketa, isSignvisible, align, zeropadding, space, id, gain);
	}
	public SkinFloat(TextureRegion[][] image, TimerProperty timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, int id, float gain) {
		this(image, null, timer, cycle, iketa, fketa, isSignvisible, align, zeropadding, space, id, gain);
	}
	public SkinFloat(TextureRegion[][] image, int timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, FloatProperty ref, float gain) {
		this(image, null, timer, cycle, iketa, fketa, isSignvisible, align, zeropadding, space, ref, gain);
	}
	public SkinFloat(TextureRegion[][] image, TimerProperty timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, FloatProperty ref, float gain) {
		this(image, null, timer, cycle, iketa, fketa, isSignvisible, align, zeropadding, space, ref, gain);
	}
	public SkinFloat(TextureRegion[][] image, TextureRegion[][] mimage, int timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, int id, float gain) {
		this(image, mimage, timer, cycle, iketa, fketa, isSignvisible, align, zeropadding, space, gain);
		this.ref = FloatPropertyFactory.getFloatProperty(id);
	}
	public SkinFloat(TextureRegion[][] image, TextureRegion[][] mimage, TimerProperty timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, int id, float gain) {
		this(image, mimage, timer, cycle, iketa, fketa, isSignvisible, align, zeropadding, space, gain);
		this.ref = FloatPropertyFactory.getFloatProperty(id);
	}
	public SkinFloat(TextureRegion[][] image, TextureRegion[][] mimage, int timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, FloatProperty ref, float gain) {
		this(image, mimage, timer, cycle, iketa, fketa, isSignvisible, align, zeropadding, space, gain);
		this.ref = ref;
	}
	public SkinFloat(TextureRegion[][] image, TextureRegion[][] mimage, TimerProperty timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, FloatProperty ref, float gain) {
		this(image, mimage, timer, cycle, iketa, fketa, isSignvisible, align, zeropadding, space, gain);
		this.ref = ref;
	}
	private SkinFloat(TextureRegion[][] image, TextureRegion[][] mimage, TimerProperty timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, float gain) {
		this(iketa, fketa, isSignvisible, align, zeropadding, space, gain);
		this.image = new SkinSourceImageSet(image, timer, cycle);
		this.mimage = mimage != null ? new SkinSourceImageSet(mimage, timer, cycle) : null;
	}
	private SkinFloat(TextureRegion[][] image, TextureRegion[][] mimage, int timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, float gain) {
		this(iketa, fketa, isSignvisible, align, zeropadding, space, gain);
		this.image = new SkinSourceImageSet(image, timer, cycle);
		this.mimage = mimage != null ? new SkinSourceImageSet(mimage, timer, cycle) : null;
	}	
	private SkinFloat(int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, float gain) {
		this.ff = new FloatFormatter(iketa, fketa, isSignvisible, zeropadding);
		this.align = align;
		this.zeropadding = zeropadding;
		this.space = space;
		this.iketa = this.ff.getIketa();
		this.fketa = this.ff.getFketa();
		this.gain = gain;
		this.keta = this.ff.getketaLength();
		this.isSignvisible = isSignvisible;
		this.currentImages = new TextureRegion[this.keta];
	}

	public void setOffsets(SkinOffset[] offsets) {
		this.offsets = offsets;
	}

	public void prepare(long time, MainState state) {
		prepare(time, state, 0, 0);
	}

	public void prepare(long time, MainState state, float offsetX, float offsetY) {
		prepare(time, state, ref != null ? ref.get(state) : Float.MIN_VALUE, offsetX, offsetY);
	}

	public void prepare(long time, MainState state, float value, float offsetX, float offsetY) {
		var v = value * gain;
		if (value == Float.MIN_VALUE || value == Float.MAX_VALUE || Float.isInfinite(v) || Float.isNaN(v) || v == Float.MIN_VALUE || v == Float.MAX_VALUE || this.keta == 0) {
			length = 0;
			draw = false;
			return;
		}
		final SkinSourceSet images = (mimage == null || v >= 0.0f) ? this.image : mimage;
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

		if(this.value != v || this.imageSet != image) {
			this.value = v;
			this.imageSet = image;
			shiftbase = 0;
			int[] digits = ff.calcuateAndGetDigits(Math.abs(v));
			for (int nowketa = 1; nowketa < digits.length; nowketa++) {
				currentImages[nowketa - 1] = (digits[nowketa] != -1) ? image[digits[nowketa]] : null;
				if (digits[nowketa] == -1) {
					shiftbase++;
				}
			}

		}
		length = (region.width + space) * (currentImages.length - shiftbase);
		shift = align == 0 ? 0 : (align == 1 ? (region.width + space) * shiftbase : (region.width + space) * 0.5f * shiftbase);
	}

	public void draw(SkinObjectRenderer sprite) {
		for (int j = 0; j < currentImages.length; j++) {
			if (currentImages[j] != null) {
				if(offsets != null && j < offsets.length) {
					draw(sprite, currentImages[j], region.x + (region.width + space) * j + shift + offsets[j].x, region.y + offsets[j].y, region.width + offsets[j].w, region.height + offsets[j].h);
				} else {
					draw(sprite, currentImages[j], region.x + (region.width + space) * j + shift, region.y, region.width, region.height);
				}
			}
		}
	}

	public void draw(SkinObjectRenderer sprite, long time, float value, MainState state, float offsetX, float offsetY) {
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
		if (image != null) {
			image.dispose();
			image = null;
		}
		if (mimage != null) {
			mimage.dispose();
			mimage = null;
		}
	}
}
