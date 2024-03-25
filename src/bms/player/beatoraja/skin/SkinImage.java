package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.property.IntegerProperty;
import bms.player.beatoraja.skin.property.IntegerPropertyFactory;

import bms.player.beatoraja.skin.property.TimerProperty;

import java.util.stream.Stream;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

/**
 * スキンイメージ
 * 
 * @author exch
 */
public class SkinImage extends SkinObject {
	
	/**
	 * イメージ
	 */
	private final SkinSource[] image;

	private final IntegerProperty ref;

	private TextureRegion currentImage;
	
	private final Array<SkinSource> removedSources = new Array<SkinSource>();
	
	public SkinImage(int imageid) {
		this.image = new SkinSource[]{new SkinSourceReference(imageid)};
		ref = null;
	}

	public SkinImage(TextureRegion image) {
		this.image = new SkinSource[]{new SkinSourceImage(new TextureRegion[]{image}, 0, 0)};
		ref = null;
	}

	public SkinImage(TextureRegion[] image, int timer, int cycle) {
		this.image = new SkinSource[]{new SkinSourceImage(image, timer, cycle)};
		ref = null;
	}

	public SkinImage(TextureRegion[][] images, int timer, int cycle, int ref) {
		this(images, timer, cycle, IntegerPropertyFactory.getImageIndexProperty(ref));
	}

	public SkinImage(TextureRegion[][] images, int timer, int cycle, IntegerProperty ref) {
		this.image = Stream.of(images).map(image -> new SkinSourceImage(image, timer, cycle)).toArray(SkinSource[]::new);
		this.ref = ref;
	}

	public SkinImage(TextureRegion[] image, TimerProperty timer, int cycle) {
		this.image = new SkinSource[]{new SkinSourceImage(image, timer, cycle)};
		ref = null;
	}

	public SkinImage(TextureRegion[][] images, TimerProperty timer, int cycle, int ref) {
		this(images, timer, cycle, IntegerPropertyFactory.getImageIndexProperty(ref));
	}

	public SkinImage(TextureRegion[][] images, TimerProperty timer, int cycle, IntegerProperty ref) {
		this.image = Stream.of(images).map(image -> new SkinSourceImage(image, timer, cycle)).toArray(SkinSource[]::new);
		this.ref = ref;
	}

	public SkinImage(SkinSourceMovie movie) {
		this.image = new SkinSource[]{movie};
		this.setImageType(SkinObjectRenderer.TYPE_FFMPEG);
		ref = null;
	}

	public SkinImage(SkinSourceImage[] image, int ref) {
		this(image, IntegerPropertyFactory.getImageIndexProperty(ref));
	}

	public SkinImage(SkinSourceImage[] image, IntegerProperty ref) {
		this.image = image;
		this.ref = ref;
	}

	public TextureRegion getImage(long time, MainState state) {
		return getImage(0 ,time, state);
	}

	public TextureRegion getImage(int value, long time, MainState state) {
		final SkinSource source = image[value];
		return source != null ? source.getImage(time, state) : null;
	}

	public boolean validate() {
		if(image == null) {
			return false;
		}
		
		boolean exist = false;
    	for(int i = 0;i < image.length;i++) {
    		if(image[i] != null) {
    			if(image[i].validate()) {
    				exist = true;
    			} else {
        			removedSources.add(image[i]);
        			image[i] = null;
    			}
    		}
    	}
    	
    	if(!exist) {
    		return false;
    	}

		return super.validate();
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

	public void draw(SkinObjectRenderer sprite, float offsetX, float offsetY) {
		if(image[0] instanceof SkinSourceMovie) {
			setImageType(3);
			draw(sprite, currentImage, region.x + offsetX, region.y + offsetY, region.width, region.height);
			setImageType(0);
		} else {
			draw(sprite, currentImage, region.x + offsetX, region.y + offsetY, region.width, region.height);
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
    	disposeAll(removedSources.toArray(SkinSource.class));
    	disposeAll(image);
    	setDisposed();
	}
}