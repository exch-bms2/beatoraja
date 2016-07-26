package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class SkinGraph extends SkinObject {

	/**
	 * イメージ
	 */
	private TextureRegion[] image;
	private int cycle;

	private int timing;
	private int[] option = new int[3];

	private NumberResourceAccessor resource;
	private NumberResourceAccessor maxresource;
	private int max = 100;

	public TextureRegion[] getImage() {
		return image;
	}

	public TextureRegion getImage(long time) {
		if (cycle == 0) {
			return image[0];
		}
		final int index = (int) ((time / (cycle / image.length))) % image.length;
		// System.out.println(index + " / " + image.length);
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

	public void setNumberResourceAccessor(NumberResourceAccessor resource, NumberResourceAccessor maxresource) {
		this.resource = resource;
		this.maxresource = maxresource;
	}

	public void setMaxValue(int max) {
		this.max = max;
	}

	public void draw(SpriteBatch sprite, long time, MainState state) {
		if (image == null) {
			return;
		}
		Rectangle r = this.getDestination(time);
		if (r != null) {
			final int value = resource != null ? resource.getValue(state) : 0;
			final int maxvalue = maxresource != null ? maxresource.getValue(state) : max;
			if (maxvalue > 0) {
				Color c = sprite.getColor();
				sprite.setColor(getColor(time));
				TextureRegion image = getImage(time);
//				sprite.draw(image, r.x, r.y, r.width, r.height);
				sprite.draw(new TextureRegion(image, 0, image.getRegionY() + image.getRegionHeight()
						- (image.getRegionHeight() * value / maxvalue), image.getRegionWidth(), image.getRegionHeight()
						* value / maxvalue), r.x, r.y, r.width, r.height * value / maxvalue);
				sprite.setColor(c);
			}
		}
	}

	public void dispose() {
		if (image != null) {
			for (TextureRegion tr : image) {
				tr.getTexture().dispose();
			}
			image = null;
		}
	}
}
