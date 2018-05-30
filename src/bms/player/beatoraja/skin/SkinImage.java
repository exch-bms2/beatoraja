package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * �궧�궘�꺍�궎�깳�꺖�궦
 * 
 * @author exch
 */
public class SkinImage extends SkinObject implements SkinObserver {
	
	/**
	 * �궎�깳�꺖�궦
	 */
	private SkinSource[] image;

	private int id = -1;

	public SkinImage() {
		
	}

	public SkinImage(int imageid) {
		setImageID(imageid);
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
		
	public TextureRegion getImage(long time, MainState state) {
		return getImage(0 ,time, state);
	}

	public SkinImage(SkinSourceMovie image) {
		this.image = new SkinSource[1];
		this.image[0] = image;
		this.setImageType(SkinObjectRenderer.TYPE_FFMPEG);
	}

	public TextureRegion getImage(int value, long time, MainState state) {
		if(getImageID() != -1) {
			return state.getImage(getImageID());
		}
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

	public void draw(SkinObjectRenderer sprite, long time, MainState state) {
		draw(sprite, time, state, 0,0);
	}

	public void draw(SkinObjectRenderer sprite, long time, MainState state, float offsetX, float offsetY) {
	    if(getImageID() != -1) {
            final Rectangle r = this.getDestination(time, state);
            final TextureRegion tr = state.getImage(getImageID());
            if (r != null && tr != null) {
                draw(sprite, tr, r.x + offsetX, r.y + offsetY, r.width, r.height, state);
            }
        } else {
            if(image == null) {
                return;
            }
            int value = 0;
            if(id != -1) {
                value = state.getImageIndex(id);
            }
            if(value >= image.length) {
                value = 0;
            }

            final Rectangle r = this.getDestination(time, state);
            if (r != null) {
                if(value >= 0 && value < image.length) {
                	if(image[0] instanceof SkinSourceMovie) {
                		setImageType(3);
                        draw(sprite, getImage(value, time, state), r.x + offsetX, r.y + offsetY, r.width, r.height, state);
                		setImageType(0);
                	} else {
						draw(sprite, getImage(value, time, state), r.x + offsetX, r.y + offsetY, r.width, r.height, state);
                	}
                }
            }
        }
	}

    public void draw(SkinObjectRenderer sprite, long time, MainState state, int value, float offsetX, float offsetY) {
        if(getImageID() != -1) {
            final Rectangle r = this.getDestination(time, state);
            final TextureRegion tr = state.getImage(getImageID());
            if (r != null && tr != null) {
                draw(sprite, tr, r.x + offsetX, r.y + offsetY, r.width, r.height, state);
            }
        } else {
            if(image == null) {
                return;
            }
            final Rectangle r = this.getDestination(time, state);
            if (r != null) {
                if(value >= 0 && value < image.length) {
                    draw(sprite, getImage(value, time, state), r.x + offsetX, r.y + offsetY, r.width, r.height, state);
                }
            }
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

	public void setReferenceID(int id) {
		this.id = id;
	}
}