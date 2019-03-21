package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.property.IntegerProperty;
import bms.player.beatoraja.skin.property.IntegerPropertyFactory;

import bms.player.beatoraja.skin.property.TimerProperty;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * スキンイメージ
 * 
 * @author exch
 */
public class SkinImage extends SkinObject {
	
	/**
	 * イメージ
	 */
	private SkinSource[] image;

	private IntegerProperty ref;

	private TextureRegion currentImage;
	
	public SkinImage() {
		
	}

	public SkinImage(int imageid) {
		this.image = new SkinSource[1];
		this.image[0] = new SkinSourceReference(imageid);
	}

	public SkinImage(TextureRegion image) {
		setImage(new TextureRegion[]{image}, 0, 0);
	}

	public SkinImage(TextureRegion[] image, int timer, int cycle) {
		setImage(image, timer, cycle);
	}

	public SkinImage(TextureRegion[][] image, int timer, int cycle) {
		setImage(image, timer, cycle);
	}

	public SkinImage(TextureRegion[] image, TimerProperty timer, int cycle) {
		setImage(image, timer, cycle);
	}

	public SkinImage(TextureRegion[][] image, TimerProperty timer, int cycle) {
		setImage(image, timer, cycle);
	}

	public TextureRegion getImage(long time, MainState state) {
		return getImage(0 ,time, state);
	}

	public SkinImage(SkinSourceMovie image) {
		this.image = new SkinSource[1];
		this.image[0] = image;
		this.setImageType(SkinObjectRenderer.TYPE_FFMPEG);
	}

	public TextureRegion getImage(int value, long time, MainState state) {
		return image[value].getImage(time, state);
	}

	public void setImage(TextureRegion[] image, int timer, int cycle) {
		this.image = new SkinSource[1];
		this.image[0] = new SkinSourceImage(image, timer, cycle);
	}

	public void setImage(TextureRegion[][] image, int timer, int cycle) {
		this.image = new SkinSource[image.length];
		for(int i = 0;i < image.length;i++) {
			this.image[i] = new SkinSourceImage(image[i], timer, cycle);
		}		
	}

	public void setImage(TextureRegion[] image, TimerProperty timer, int cycle) {
		this.image = new SkinSource[1];
		this.image[0] = new SkinSourceImage(image, timer, cycle);
	}

	public void setImage(TextureRegion[][] image, TimerProperty timer, int cycle) {
		this.image = new SkinSource[image.length];
		for(int i = 0;i < image.length;i++) {
			this.image[i] = new SkinSourceImage(image[i], timer, cycle);
		}
	}

	public void prepare(long time, MainState state) {
        prepare(time, state, 0, 0);
	}
	
	public void prepare(long time, MainState state, float offsetX, float offsetY) {		
        prepare(time, state, ref != null ? ref.get(state) : 0, offsetX, offsetY);
	}
	
	public void prepare(long time, MainState state, int value, float offsetX, float offsetY) {		
        if(image == null || value < 0) {
            draw = false;
            return;
        }
		super.prepare(time, state, offsetX, offsetY);
        if(value >= image.length) {
            value = 0;
        }
        currentImage = getImage(value, time, state);
        if(currentImage == null) {
            draw = false;
            return;        	
        }
	}

	public void draw(SkinObjectRenderer sprite) {
    	if(image[0] instanceof SkinSourceMovie) {
    		setImageType(3);
            draw(sprite, currentImage, region.x, region.y, region.width, region.height);
    		setImageType(0);
    	} else {
            draw(sprite, currentImage, region.x, region.y, region.width, region.height);
    	}                    				
	}

	public void draw(SkinObjectRenderer sprite, long time, MainState state, float offsetX, float offsetY) {
		prepare(time, state, offsetX, offsetY);
		if(draw) {
			draw(sprite);
		}
	}

    public void draw(SkinObjectRenderer sprite, long time, MainState state, int value, float offsetX, float offsetY) {
		prepare(time, state, value, offsetX, offsetY);
		if(draw) {
			draw(sprite);
		}
    }

    public void dispose() {
		if(image != null) {
			for(SkinSource tr : image) {
				tr.dispose();
			}
			image = null;
		}
	}

	public void setReference(IntegerProperty property) {
		ref = property;
	}

	public void setReferenceID(int id) {
		ref = IntegerPropertyFactory.getImageIndexProperty(id);
	}
}