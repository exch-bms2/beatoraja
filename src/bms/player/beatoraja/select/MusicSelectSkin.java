package bms.player.beatoraja.select;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;

import bms.player.beatoraja.skin.Skin;

public class MusicSelectSkin extends Skin {

	/**
	 * 楽曲バー画像
	 */
	private Sprite[] bar = new Sprite[10];
	/**
	 * ランプ画像
	 */
	private Animation[] lamp = new Animation[11];

	public MusicSelectSkin() {
		Texture bart = new Texture("skin/songbar.png");
		for(int i = 0;i < bar.length;i++) {
			bar[i] = new Sprite(bart, 0, i * 30, 500, 30);
		}
		TextureRegion[][] lampt = TextureRegion.split(new Texture("skin/lamp.png"),15,30);
		for(int i = 0;i < lamp.length;i++) {
			lamp[i] = new Animation(2 / 60f, lampt[i]);
			lamp[i].setPlayMode(PlayMode.LOOP);
		}
	}
	
	public Sprite[] getBar() {
		return bar;
	}

	public void setBar(Sprite[] bar) {
		this.bar = bar;
	}

	public Animation[] getLamp() {
		return lamp;
	}

	public void setLamp(Animation[] lamp) {
		this.lamp = lamp;
	}
}
