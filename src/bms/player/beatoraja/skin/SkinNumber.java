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
	private TextureRegion[] image;
	private TextureRegion[] mimage;

	private int id;

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

	public SkinNumber(TextureRegion[] image, TextureRegion[] mimage, int cycle, int keta, int zeropadding, NumberResourceAccessor resource) {
		this.image = image;
		this.mimage = mimage;
		this.cycle = cycle;
		this.setKeta(keta);
		this.zeropadding = zeropadding;
		this.resource = resource;
	}

	public void setNumberResourceAccessor(NumberResourceAccessor resource) {
		this.resource = resource;
	}
	
	public TextureRegion[] getImage() {
		return image;
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
	
	public TextureRegion[] getValue(int value, int zeropadding) {
		final TextureRegion[] image = (value >= 0 || mimage == null) ? this.image : mimage;
		if(image == null) {
			return new TextureRegion[0];
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
		if(resource != null) {
			final int value = resource.getValue(state);
			if(value != Integer.MIN_VALUE && value != Integer.MAX_VALUE) {
				draw(sprite, time, resource.getValue(state));				
			}
		}
	}
	
	public void draw(SpriteBatch sprite, long time, int value) {
		Rectangle r = this.getDestination(time);
		TextureRegion[] values = getValue(value, zeropadding);
		for (int j = 0; j < values.length; j++) {
			if(values[j] != null) {
				draw(sprite, values[j], r.x + r.width * j, r.y, r.width, r.height, getColor(time));				
			}
		}
	}

	public void dispose() {
		if(image != null) {
			for(TextureRegion tr : image) {
				tr.getTexture().dispose();
			}
			image = null;
		}
		if(mimage != null) {
			for(TextureRegion tr : mimage) {
				tr.getTexture().dispose();
			}
			mimage = null;
		}
	}
}
