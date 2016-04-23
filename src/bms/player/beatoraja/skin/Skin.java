package bms.player.beatoraja.skin;

import bms.model.BMSModel;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Skin {

	private SkinImage[] skinparts = new SkinImage[0];

	private SkinNumber[] numbers = new SkinNumber[0];

	private SkinText genre;

	private SkinText title;
	private SkinText artist;

	public SkinImage[] getSkinPart() {
		return skinparts;
	}

	public void setSkinPart(SkinImage[] parts) {
		skinparts = parts;
	}

	public SkinNumber[] getSkinNumbers() {
		return numbers;
	}

	public void setSkinNumbers(SkinNumber[] numbers) {
		this.numbers = numbers;
	}

	public SkinText getGenre() {
		return genre;
	}

	public void setGenre(SkinText genre) {
		this.genre = genre;
	}

	public SkinText getTitle() {
		return title;
	}

	public void setTitle(SkinText title) {
		this.title = title;
	}

	public SkinText getArtist() {
		return artist;
	}

	public void setArtist(SkinText artist) {
		this.artist = artist;
	}

	public void setText(BMSModel model) {
		if(genre != null) {
			genre.setText(model.getGenre());
		}
		if(title != null) {
			title.setText(model.getFullTitle());
		}
		if(artist != null) {
			artist.setText(model.getFullArtist());
		}
	}

	public void drawAllObjects(SpriteBatch sprite, long time) {
		for(SkinImage obj : skinparts) {
			obj.draw(sprite, time);
		}
		for(SkinNumber num : numbers) {
			num.draw(sprite, time, 0);
		}
		if(genre != null) {
			genre.draw(sprite, time);
		}
		if(title != null) {
			title.draw(sprite, time);
		}
		if(artist != null) {
			artist.draw(sprite, time);
		}
	}
}
