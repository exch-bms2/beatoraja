package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * 数字オブジェクト
 * 
 * @author exch
 */
public class SkinNumber extends SkinObject {

	/**
	 * イメージ
	 */
	private TextureRegion[][] image;
	private TextureRegion[][] mimage;

	private int id = -1;

	private int cycle;
	
	private int keta;
	
	private int zeropadding;
	
	private TextureRegion[] values;
	
	private NumberResourceAccessor resource;
	
	public SkinNumber(TextureRegion[] image, int cycle, int keta, int zeropadding) {
		this(image, cycle, keta, zeropadding, null);
	}
	
	public SkinNumber(TextureRegion[] image, int cycle, int keta, int zeropadding, NumberResourceAccessor resource) {
		this(image, null, cycle, keta, zeropadding, resource);
	}

	public SkinNumber(TextureRegion[][] image, int cycle, int keta, int zeropadding, NumberResourceAccessor resource) {
		this.image = image;
		this.mimage = null;
		this.cycle = cycle;
		this.setKeta(keta);
		this.zeropadding = zeropadding;
		this.resource = resource;
	}

	public SkinNumber(TextureRegion[] image, int cycle, int keta, int zeropadding, int rid) {
		this(image, null, cycle, keta, zeropadding, rid);
	}

	public SkinNumber(TextureRegion[] image, TextureRegion[] mimage, int cycle, int keta, int zeropadding, int id) {
		this.image = new TextureRegion[][]{image};
		this.mimage = mimage != null ? new TextureRegion[][]{mimage} : null;
		this.cycle = cycle;
		this.setKeta(keta);
		this.zeropadding = zeropadding;
		this.id = id;
	}
	
	public SkinNumber(TextureRegion[] image, TextureRegion[] mimage, int cycle, int keta, int zeropadding, NumberResourceAccessor resource) {
		this.image = new TextureRegion[][]{image};
		this.mimage = mimage != null ? new TextureRegion[][]{mimage} : null;
		this.cycle = cycle;
		this.setKeta(keta);
		this.zeropadding = zeropadding;
		this.resource = resource;
	}

	public void setNumberResourceAccessor(NumberResourceAccessor resource) {
		this.resource = resource;
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
	
	
	public TextureRegion[] getValue(long time, int value, int zeropadding) {
		final TextureRegion[][] images = (value >= 0 || mimage == null) ? this.image : mimage;
		if(images == null) {
			return new TextureRegion[0];
		}
		TextureRegion[] image = images[0];
		if(cycle != 0) {
			final int index = (int) ((time / (cycle / images.length))) % images.length;
//			System.out.println(index + " / " + image.length);
			image = images[index];
		}

		value = Math.abs(value);
		for (int j = values.length - 1; j >= 0; j--) {
			if(value > 0 || j == values.length - 1) {
				values[j] = image[value % 10];
			} else {
				values[j] = (zeropadding == 2 ? image[10] : (zeropadding == 1 ? image[0] : (mimage != null && (values[j + 1] != image[11] && values[j + 1] != null) ? image[11] : null)));
			}
			value /= 10;
		}
		return values;
	}
	
	public  void draw(SpriteBatch sprite, long time, MainState state) {
		int value = Integer.MIN_VALUE;
		if(id != -1) {
			value = state.getNumberValue(id);
		}
		else if(resource != null) {
			value = resource.getValue(state);
		}
		if(value != Integer.MIN_VALUE && value != Integer.MAX_VALUE) {
			draw(sprite, time, value, state);				
		}
	}
	
	public void draw(SpriteBatch sprite, long time, int value, MainState state) {
		Rectangle r = this.getDestination(time,state);
		if(r != null) {
			TextureRegion[] values = getValue(time, value, zeropadding);
			for (int j = 0; j < values.length; j++) {
				if(values[j] != null) {
					draw(sprite, values[j], r.x + r.width * j, r.y, r.width, r.height, getColor(time,state));				
				}
			}			
		}
	}

	public void dispose() {
		if(image != null) {
			for(TextureRegion[] ptr : image) {
				if(ptr != null) {
					for(TextureRegion tr : ptr) {
						tr.getTexture().dispose();					
					}					
				}
			}
			image = null;
		}
		if(mimage != null) {
			for(TextureRegion[] ptr : mimage) {
				if(ptr != null) {
					for(TextureRegion tr : ptr) {
						tr.getTexture().dispose();					
					}					
				}
			}
			mimage = null;
		}
	}
}
