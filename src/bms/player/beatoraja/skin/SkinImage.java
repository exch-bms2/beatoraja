package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * スキンイメージ
 * 
 * @author exch
 */
public class SkinImage extends SkinObject {
	
	/**
	 * イメージ
	 */
	private TextureRegion[][] image;
	private int cycle;
	
	private int timing;
	private int[] option = new int[3];

	private NumberResourceAccessor resource;

	public SkinImage() {
		
	}
	
	public SkinImage(TextureRegion[] image, int cycle) {
		setImage(image, cycle);
	}
		
	public TextureRegion[] getImage() {
		return image[0];
	}

	public TextureRegion getImage(long time) {
		return getImage(0 ,time);
	}

	public TextureRegion getImage(int value, long time) {
		if(cycle == 0) {
			return image[value][0];
		}
		final int index = (int) ((time / (cycle / image[value].length))) % image[value].length;
//		System.out.println(index + " / " + image.length);
		return image[value][index];
	}
	
	public void setImage(TextureRegion[] image, int cycle) {
		this.image = new TextureRegion[1][];
		this.image[0] = image;
		this.cycle = cycle;
	}

	public void setImage(TextureRegion[][] image, int cycle) {
		this.image = image;
		this.cycle = cycle;
	}

	public int getTiming() {
		return timing;
	}

	public void setTiming(int timing) {
		this.timing = timing;
	}

	public int[] getOption() {
		return option;
	}

	public void setOption(int[] option) {
		this.option = option;
	}

	public void setNumberResourceAccessor(NumberResourceAccessor resource) {
		this.resource = resource;
	}

	public void draw(SpriteBatch sprite, long time, MainState state) {
		if(image == null) {
			return;
		}
		if(timing != 0) {
			if(state.getTimer()[timing] == -1) {
				return;
			}
			time -= state.getTimer()[timing];
		}
		Rectangle r = this.getDestination(time);
        if (r != null) {
			final int value = resource != null ? resource.getValue(state) : 0;
			if(value >= 0 && value < image.length) {
				Color c = sprite.getColor();
				sprite.setColor(getColor(time));
				sprite.draw(getImage(value, time), r.x, r.y, r.width, r.height);
				sprite.setColor(c);
			}
        }
	}

	public void dispose() {
		if(image != null) {
			for(TextureRegion[] tr : image) {
				for(TextureRegion ctr : tr) {
					ctr.getTexture().dispose();
				}
			}
			image = null;
		}
	}

}