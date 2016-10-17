package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.play.BMSPlayer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import static bms.player.beatoraja.skin.SkinProperty.*;

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

	private int id = -1;

	private int scratch = 0;

	public SkinImage() {
		
	}

	public SkinImage(int imageid) {
		setImageID(imageid);
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

	public TextureRegion getImage(long time, MainState state) {
		return getImage(0 ,time, state);
	}

	public TextureRegion getImage(int value, long time, MainState state) {
		if(getImageID() != -1) {
			return state.getImage(getImageID());
		}
		return image[value][getImageIndex(image[value].length, time, state)];
	}
	
	public void setImage(TextureRegion[] image, int cycle) {
		this.image = new TextureRegion[1][];
		this.image[0] = image;
		setCycle(cycle);
	}

	public void setImage(TextureRegion[][] image, int cycle) {
		this.image = image;
		setCycle(cycle);
	}

	public void draw(SpriteBatch sprite, long time, MainState state) {
		draw(sprite, time, state, 0,0);
	}

	public void draw(SpriteBatch sprite, long time, MainState state, int offsetX, int offsetY) {
	    if(getImageID() != -1) {
            Rectangle r = this.getDestination(time, state);
            TextureRegion tr = state.getImage(getImageID());
            if (r != null && tr != null) {
                draw(sprite, tr, r.x + offsetX, r.y + offsetY, r.width, r.height, getColor(time,state),getAngle(time,state));
            }
        } else {
            if(image == null) {
                return;
            }
            int value = 0;
            if(id != -1) {
                value = state.getNumberValue(id);
            }
            if(value >= image.length) {
                value = 0;
            }
            if(value < 0 || image[value].length == 0) {
                return;
            }

            Rectangle r = this.getDestination(time, state);
            if (r != null) {
                if(value >= 0 && value < image.length) {
                	if(scratch == 1) {
						draw(sprite, getImage(value, time, state), r.x + offsetX, r.y + offsetY, r.width, r.height, getColor(time,state),state.getNumberValue(NUMBER_SCRATCHANGLE_1P));
					} else if(scratch == 2) {
						draw(sprite, getImage(value, time, state), r.x + offsetX, r.y + offsetY, r.width, r.height, getColor(time,state),state.getNumberValue(NUMBER_SCRATCHANGLE_2P));
					} else {
						draw(sprite, getImage(value, time, state), r.x + offsetX, r.y + offsetY, r.width, r.height, getColor(time,state),getAngle(time,state));
					}
                }
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

	public int getScratch() {
		return scratch;
	}

	public void setScratch(int scratch) {
		this.scratch = scratch;
	}
}