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
	private int iketa;
	/**
	 * 小数表示桁数
	 */
	private int fketa;
	/**
	 * 符号を表示するか
	 */
	private boolean isSignvisible;

	private int keta;
	/**
	 * 0:なし 1:表ゼロ 2:裏ゼロ
	 */
	private int zeropadding;

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
	public SkinFloat(TextureRegion[][] image, int timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, int id) {
		this(image, null, timer, cycle, iketa, fketa, isSignvisible, align, zeropadding, space, id);
	}
	public SkinFloat(TextureRegion[][] image, TimerProperty timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, int id) {
		this(image, null, timer, cycle, iketa, fketa, isSignvisible, align, zeropadding, space, id);
	}
	public SkinFloat(TextureRegion[][] image, int timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, FloatProperty ref) {
		this(image, null, timer, cycle, iketa, fketa, isSignvisible, align, zeropadding, space, ref);
	}
	public SkinFloat(TextureRegion[][] image, TimerProperty timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, FloatProperty ref) {
		this(image, null, timer, cycle, iketa, fketa, isSignvisible, align, zeropadding, space, ref);
	}
	public SkinFloat(TextureRegion[][] image, TextureRegion[][] mimage, int timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, int id) {
		this(image, mimage, timer, cycle, iketa, fketa, isSignvisible, align, zeropadding, space);
		this.ref = FloatPropertyFactory.getFloatProperty(id);
	}
	public SkinFloat(TextureRegion[][] image, TextureRegion[][] mimage, TimerProperty timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, int id) {
		this(image, mimage, timer, cycle, iketa, fketa, isSignvisible, align, zeropadding, space);
		this.ref = FloatPropertyFactory.getFloatProperty(id);
	}
	public SkinFloat(TextureRegion[][] image, TextureRegion[][] mimage, int timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, FloatProperty ref) {
		this(image, mimage, timer, cycle, iketa, fketa, isSignvisible, align, zeropadding, space);
		this.ref = ref;
	}
	public SkinFloat(TextureRegion[][] image, TextureRegion[][] mimage, TimerProperty timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space, FloatProperty ref) {
		this(image, mimage, timer, cycle, iketa, fketa, isSignvisible, align, zeropadding, space);
		this.ref = ref;
	}
	private SkinFloat(TextureRegion[][] image, TextureRegion[][] mimage, TimerProperty timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space) {
		this(iketa, fketa, isSignvisible, align, zeropadding, space);
		this.image = new SkinSourceImageSet(image, timer, cycle);
		this.mimage = mimage != null ? new SkinSourceImageSet(mimage, timer, cycle) : null;
	}
	private SkinFloat(TextureRegion[][] image, TextureRegion[][] mimage, int timer, int cycle, int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space) {
		this(iketa, fketa, isSignvisible, align, zeropadding, space);
		this.image = new SkinSourceImageSet(image, timer, cycle);
		this.mimage = mimage != null ? new SkinSourceImageSet(mimage, timer, cycle) : null;
	}	
	private SkinFloat(int iketa, int fketa, boolean isSignvisible, int align, int zeropadding, int space) {
		this.align = align;
		this.zeropadding = zeropadding;
		this.space = space;
		setKeta(iketa, fketa, isSignvisible);	
	}

	/**
	 * 以下の3パターン * 符号ありなし + 符号のみ
	 * 整数部
	 * 整数部 & 小数点 & 小数部
	 * 小数点 & 小数部
	 *  */
	private void setKeta(int k, int fk, boolean sign) {
		int keta = sign ? 1 : 0;
		if(fk == 0) {
			keta += k;
		}
		else {
			keta += k + fk + 1;
		}
		this.keta = keta;
		this.iketa = k;
		this.fketa = fk;
		this.currentImages = new TextureRegion[keta];
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
		if (value == Float.MIN_VALUE || value == Float.MAX_VALUE || this.keta == 0) {
			length = 0;
			draw = false;
			return;
		}
		final SkinSourceSet images = (mimage == null || value >= 0.0f) ? this.image : mimage;
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
			int nowketa = 0;
			// currentImages[]に値設定 数値:0~9 裏ゼロ:10 符号:11 小数点:12
			// 左詰めで入れていく。ゼロパディングなしなら数値なしはnull
			// 小数は必ずfketa分表示
			// iketa==0なら整数部表示なし
			// 
			value = Math.abs(value);
			// 符号の表示
			if (isSignvisible) {
				currentImages[nowketa] = image[11];
				nowketa++;
			}
			// 整数部の表示
			if (iketa != 0) {
				// 表示桁数baseの算出
				int ivalue = (int) value;
				int base;
				if (zeropadding == 0) {
					if (ivalue == 0) {
						base = 1;
					} else {
						base = Math.min(iketa, (int)Math.log10(ivalue) + 1);
					}
				} else {
					base = iketa;
				}

				// 整数部を一の位から割当
				int j = 1;
				do {
					currentImages[nowketa + base - j] = image[ivalue % 10];
					ivalue /= 10;
					j++;
				} while (ivalue > 0 && base >= j);
				
				// ゼロ埋め
				if (zeropadding != 0) {
					while(base >= j) {
						currentImages[nowketa + base - j] = image[(zeropadding == 1 ? 0 : 10)];
						j++;
					}
				}
				
				nowketa += base;
			}
			// 小数点の表示
			if (fketa != 0) {
				currentImages[nowketa] = image[11];
				nowketa++;
			}
			// 小数部の表示
			double fvalue = value - ((long) value);
			for (int j = 0; j < fketa; j++) {
				fvalue *= 10;
				int num = (int) fvalue;
				currentImages[nowketa] = image[num];
				fvalue -= num;
				nowketa++;
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
