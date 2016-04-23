package bms.player.beatoraja.skin;

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
	private TextureRegion[] image;
	private int cycle;
	
	private int timing;
	private int[] option = new int[3];

	public TextureRegion[] getImage() {
		return image;
	}
	
	public TextureRegion getImage(long time) {
		if(cycle == 0) {
			return image[0];
		}
		final int index = (int) ((time / (cycle / image.length))) % image.length;
//		System.out.println(index + " / " + image.length);
		return image[index];
	}
	
	public void setImage(TextureRegion[] image, int cycle) {
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

	public void draw(SpriteBatch sprite, long time) {
		Rectangle r = this.getDestination(time);
		sprite.draw(getImage(time), r.x, r.y, r.width, r.height);
	}

}