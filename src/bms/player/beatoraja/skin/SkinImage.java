package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
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
	
	private int id = -1;

	public SkinImage() {
		
	}
	
	public SkinImage(TextureRegion[] image, int cycle) {
		setImage(image, cycle);
	}
		
	public SkinImage(TextureRegion[][] image, int cycle) {
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
		int value = 0;
        if(id != -1) {
        	value = state.getNumberValue(id);
        } else if(resource != null){
        	value = resource.getValue(state);
        }
        if(value < 0 || value >= image.length) {
            value = 0;
        }
        if(image[value].length == 0) {
        	return;
        }

		if(timing != 0 && timing < 256) {
			if(state.getTimer()[timing] == -1) {
				return;
			}
			time -= state.getTimer()[timing];
		}
		if(time < 0) {
			return;
		}
		Rectangle r = this.getDestination(time, state);
        if (r != null) {
			if(value >= 0 && value < image.length) {
				draw(sprite, getImage(value, time), r.x, r.y, r.width, r.height, getColor(time));
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

	public void setReferenceID(int id) {
		this.id = id;
	}
}