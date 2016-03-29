package bms.player.beatoraja.select;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;

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
