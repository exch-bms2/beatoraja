package bms.player.beatoraja.select;

import java.util.ArrayList;
import java.util.List;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.*;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.math.Rectangle;

public class MusicSelectSkin extends Skin {

	/**
	 * 楽曲バー画像
	 */
	private Sprite[] bar = new Sprite[10];
	/**
	 * ランプ画像
	 */
	private Animation[] lamp = new Animation[11];

	private Rectangle seekRegion;

	private List<SkinNumber> numbers = new ArrayList<SkinNumber>();

	private Sprite[] trophy = new Sprite[3];

	public MusicSelectSkin() {
		super(640, 480, 1280, 720);
	}
	
	public MusicSelectSkin(Rectangle r) {
		super(1280, 720, r.width, r.height);
		float dw = r.width / 1280.0f;
		float dh = r.height / 720.0f;

		Texture bart = new Texture("skin/songbar.png");
		for (int i = 0; i < bar.length; i++) {
			bar[i] = new Sprite(bart, 0, i * 30, 500, 30);
		}
		TextureRegion[][] lampt = TextureRegion.split(new Texture("skin/lamp.png"), 15, 30);
		for (int i = 0; i < lamp.length; i++) {
			lamp[i] = new Animation(2 / 60f, lampt[i]);
			lamp[i].setPlayMode(PlayMode.LOOP);
		}

		TextureRegion[][] ttrophy = TextureRegion.split(new Texture("skin/trophy.png"), 32, 32);
		trophy[0] = new Sprite(ttrophy[0][10]);
		trophy[1] = new Sprite(ttrophy[0][11]);
		trophy[2] = new Sprite(ttrophy[0][12]);

		SkinText dir = new SkinText("skin/VL-Gothic-Regular.ttf",0,24, 2);
		dir.setTextResourceAccessor(TextResourceAccessor.DIRECTORY);
		setDestination(dir, 0, 40, 670, 18, 18, 0, 255,255,128,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(dir);
		SkinText genre = new SkinText("skin/VL-Gothic-Regular.ttf",0,20, 2);
		genre.setTextResourceAccessor(TextResourceAccessor.GENRE);
		setDestination(genre, 0, 100, 630, 18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(genre);
		SkinText title = new SkinText("skin/VL-Gothic-Regular.ttf",0,24, 2);
		title.setTextResourceAccessor(TextResourceAccessor.FULLTITLE);
		setDestination(title, 0, 100, 600, 18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(title);
		SkinText artist = new SkinText("skin/VL-Gothic-Regular.ttf",0,20, 2);
		artist.setTextResourceAccessor(TextResourceAccessor.FULLARTIST);
		setDestination(artist, 0, 100, 570, 18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(artist);

		Texture cl = new Texture("skin/clear.png");
		SkinImage clear = new SkinImage();
		clear.setImage(TextureRegion.split(cl, 200, 20),0);
		clear.setNumberResourceAccessor(NumberResourceAccessor.CLEAR);
		setDestination(clear, 0, 100, 405, 200, 20, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(clear);
		// 数字
		Texture nt = new Texture("skin/number.png");
		TextureRegion[][] ntr = TextureRegion.split(nt, 24, 24);

		addNumber(new SkinNumber(ntr[0], 0, 4, 0, NumberResourceAccessor.MIN_BPM), 0, 300, 512, 18, 18, 0, 255,255,255,255,
				0, 0, 0, 0, 0, 0, 0, 0, 0);

		addNumber(new SkinNumber(ntr[0], 0, 4, 0, NumberResourceAccessor.MAX_BPM), 0, 372, 512, 18, 18, 0, 255,255,255,255,
				0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, NumberResourceAccessor.SCORE), 0, 200, 372, 18, 18, 0, 255,255,255,255, 0,
				0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, NumberResourceAccessor.MISSCOUNT), 0, 200, 342, 18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, NumberResourceAccessor.MAXCOMBO), 0, 450, 342, 18, 18, 0, 255,255,255,255,
				0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, NumberResourceAccessor.CLEARCOUNT), 0, 250, 312, 18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		addNumber(new SkinNumber(ntr[0], 0, 4, 0, NumberResourceAccessor.PLAYCOUNT), 0, 400, 312, 18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 6, 0, NumberResourceAccessor.PLAYER_PLAYCOUNT), 0, 160, 102, 18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		addNumber(new SkinNumber(ntr[0], 0, 10, 0, NumberResourceAccessor.PLAYER_TOTALNOTES), 0, 430, 102, 18, 18, 0,
				255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		addNumber(new SkinNumber(ntr[0], 0, 4, 1, NumberResourceAccessor.TIME_YEAR), 0, 1028, 2, 12, 12, 0, 255,255,255,255,
				0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, NumberResourceAccessor.TIME_MONTH), 0, 1088, 2, 12, 12, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, NumberResourceAccessor.TIME_DAY), 0, 1130, 2, 12, 12, 0, 255,255,255,255,
				0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, NumberResourceAccessor.TIME_HOUR), 0, 1178, 2, 12, 12, 0, 255,255,255,255,
				0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, NumberResourceAccessor.TIME_MINUTE), 0, 1214, 2, 12, 12, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, NumberResourceAccessor.TIME_SECOND), 0, 1250, 2, 12, 12, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		
		seekRegion = new Rectangle(1240 * dw, 90 * dh, 10 * dw, 540 * dh);

		Texture st = new Texture("skin/system.png");
		SkinImage si = new SkinImage(new TextureRegion[]{new TextureRegion(st, 0,10,10,251)}, 0);
		setDestination(si, 0, 1240, 75, 10, 570, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(si);
		SkinSlider seek = new SkinSlider(new TextureRegion[]{new TextureRegion(st, 0,265,17,24)}, 0, 2, (int) (516 * dh), MainState.SLIDER_MUSICSELECT_POSITION);
		setDestination(seek, 0, 1237, 606, 17, 24, 0, 255,255,255,255, 2, 0, 0, 0, 0, 0, 0, 0, 0);
		add(seek);
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

	public Rectangle getSeekRegion() {
		return seekRegion;
	}

	public void setSeekRegion(Rectangle positionRegion) {
		this.seekRegion = positionRegion;
	}

	public Sprite[] getTrophy() {
		return trophy;
	}

}
