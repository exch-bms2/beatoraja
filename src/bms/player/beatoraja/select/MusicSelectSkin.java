package bms.player.beatoraja.select;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.*;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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

	private Sprite[] trophy = new Sprite[3];

	public MusicSelectSkin(float srcw, float srch, float dstw, float dsth) {
		super(srcw, srch, dstw, dsth);
	}

	public MusicSelectSkin(Rectangle r) {
		super(1280, 720, r.width, r.height);
		float dw = r.width / 1280.0f;
		float dh = r.height / 720.0f;

		SkinImage back = new SkinImage(new TextureRegion[] { new TextureRegion(new Texture("skin/select.png")) }, 0);
		setDestination(back, 0, 0, 0, 1280, 720, 0, 48, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(back);

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

		SkinText dir = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24, 2);
		dir.setReferenceID(MainState.STRING_DIRECTORY);
		setDestination(dir, 0, 40, 670, 18, 24, 0, 255, 255, 128, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(dir);
		SkinText genre = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 20, 2);
		genre.setReferenceID(MainState.STRING_GENRE);
		setDestination(genre, 0, 100, 630, 18, 20, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(genre);
		SkinText title = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24, 2);
		title.setReferenceID(MainState.STRING_FULLTITLE);
		setDestination(title, 0, 100, 600, 18, 24, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(title);
		SkinText artist = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 20, 2);
		artist.setReferenceID(MainState.STRING_ARTIST);
		setDestination(artist, 0, 100, 570, 18, 20, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(artist);

		SkinText grade1 = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24, 2);
		grade1.setReferenceID(MainState.STRING_COURSE1_TITLE);
		setDestination(grade1, 0, 80, 540, 18, 24, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(grade1);
		SkinText grade2 = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24, 2);
		grade2.setReferenceID(MainState.STRING_COURSE2_TITLE);
		setDestination(grade2, 0, 80, 512, 18, 24, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(grade2);
		SkinText grade3 = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24, 2);
		grade3.setReferenceID(MainState.STRING_COURSE3_TITLE);
		setDestination(grade3, 0, 80, 484, 18, 24, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(grade3);
		SkinText grade4 = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24, 2);
		grade4.setReferenceID(MainState.STRING_COURSE4_TITLE);
		setDestination(grade4, 0, 80, 456, 18, 24, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(grade4);

		Texture cl = new Texture("skin/clear.png");
		SkinImage clear = new SkinImage();
		clear.setImage(TextureRegion.split(cl, 200, 20), 0);
		clear.setReferenceID(MainState.NUMBER_CLEAR);
		setDestination(clear, 0, 100, 405, 200, 20, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(clear);
		// 数字
		Texture st = new Texture("skin/system.png");

		Texture nt = new Texture("skin/number.png");
		TextureRegion[][] ntr = TextureRegion.split(nt, 24, 24);

		addNumber(new SkinNumber(ntr[0], 0, 5, 0, MainState.NUMBER_FOLDER_TOTALSONGS), 0, 260, 482, 18, 18, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_FOLDERBAR, 0, 0);
		addImage(new TextureRegion(st, 400, 648, 96, 24),0, 36, 362, 48, 24, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_FOLDERBAR, 0, 0);
		addImage(new TextureRegion(st, 0, 672, 96, 24),0, 36, 322, 48, 24, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_FOLDERBAR, 0, 0);

		// key
		SkinImage bg = new SkinImage(MainState.IMAGE_BANNER);
		setDestination(bg, 0, 400, 400, 300, 90, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(bg);

		addImage(new TextureRegion(st, 240, 600, 144, 24),0, 100, 512, 108, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_7KEYSONG, 0, 0);
		addImage(new TextureRegion(st, 240, 624, 144, 24),0, 100, 512, 108, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_14KEYSONG, 0, 0);
		addImage(new TextureRegion(st, 240, 648, 144, 24),0, 100, 512, 108, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_9KEYSONG, 0, 0);
		addImage(new TextureRegion(st, 240, 672, 144, 24),0, 100, 512, 108, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_5KEYSONG, 0, 0);
		addImage(new TextureRegion(st, 240, 696, 144, 24),0, 100, 512, 108, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_10KEYSONG, 0, 0);

		addImage(new TextureRegion(st, 0, 768, 72, 24),0, 300, 512, 60, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MusicSelector.OPTION_SONGBAR, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, MainState.NUMBER_MINBPM), 0, 370, 512, 18, 18, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, MainState.NUMBER_MAXBPM), 0, 442, 512, 18, 18, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addImage(new TextureRegion(st, 0, 600, 120, 24),0, 80, 372, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MusicSelector.OPTION_SONGBAR, 0, 0);
		addImage(new TextureRegion(st, 0, 600, 120, 24),0, 80, 372, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MusicSelector.OPTION_GRADEBAR, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, MainState.NUMBER_SCORE), 0, 200, 372, 18, 18, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addImage(new TextureRegion(st, 0, 672, 96, 24),0, 330, 372, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MusicSelector.OPTION_SONGBAR, 0, 0);
		addImage(new TextureRegion(st, 0, 672, 96, 24),0, 330, 372, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MusicSelector.OPTION_GRADEBAR, 0, 0);
		addImage(new TextureRegion(st, 0, 624, 220, 24),0, 80, 342, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MusicSelector.OPTION_SONGBAR, 0, 0);
		addImage(new TextureRegion(st, 0, 624, 220, 24),0, 80, 342, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MusicSelector.OPTION_GRADEBAR, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, MainState.NUMBER_MISSCOUNT), 0, 200, 342, 18, 18, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addImage(new TextureRegion(st, 0, 648, 192, 24),0, 330, 342, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MusicSelector.OPTION_SONGBAR, 0, 0);
		addImage(new TextureRegion(st, 0, 648, 192, 24),0, 330, 342, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MusicSelector.OPTION_GRADEBAR, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, MainState.NUMBER_MAXCOMBO), 0, 450, 342, 18, 18, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addImage(new TextureRegion(st, 0, 696, 120, 24),0, 80, 312, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MusicSelector.OPTION_SONGBAR, 0, 0);
		addImage(new TextureRegion(st, 0, 696, 120, 24),0, 80, 312, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MusicSelector.OPTION_GRADEBAR, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, MainState.NUMBER_CLEARCOUNT), 0, 320, 312, 18, 18, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addImage(new TextureRegion(st, 0, 720, 96, 24),0, 200, 312, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MusicSelector.OPTION_SONGBAR, 0, 0);
		addImage(new TextureRegion(st, 0, 720, 96, 24),0, 200, 312, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MusicSelector.OPTION_GRADEBAR, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, MainState.NUMBER_PLAYCOUNT), 0, 400, 312, 18, 18, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		addImage(new TextureRegion(st, 0, 720, 96, 24),0, 60, 102, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 6, 0, MainState.NUMBER_TOTALPLAYCOUNT), 0, 160, 102, 18, 18, 0,
				255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addImage(new TextureRegion(st, 0, 744, 120, 24),0, 330, 102, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 10, 0, MainState.NUMBER_TOTALPLAYNOTES), 0, 430, 102, 18, 18, 0,
				255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		add(new SkinBarObject());

		addImage(new TextureRegion(st, 0, 10, 10, 251), 0, 1240, 75, 10, 570, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		SkinSlider seek = new SkinSlider(new TextureRegion[] { new TextureRegion(st, 0, 265, 17, 24) }, 0, 2,
				(int) (516 * dh), MainState.SLIDER_MUSICSELECT_POSITION);
		setDestination(seek, 0, 1237, 606, 17, 24, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0, 0, 0, 0);
		seek.setChangable(true);
		add(seek);

		// option panel1
		addImage(new TextureRegion(st, 8,0,8,8), 500, 0, 0,1280, 720, 0, 128,255,255,255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL1, 0, 0);
		Texture pt = new Texture("skin/panel.png");
		addImage(new TextureRegion(pt, 0, 0, 290, 294), 0, 50, 100, 580, 588, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL1, 0, 0);
		SkinImage rc = new SkinImage();
		TextureRegion[][] rct = new TextureRegion[10][];
		for(int i = 0;i < rct.length;i++) {
			rct[i] = new TextureRegion[]{new TextureRegion(pt, 3,429 - i * 15 ,67,150)};
		}
		rc.setImage(rct, 0);
		rc.setReferenceID(MainState.BUTTON_RANDOM_1P);
		setDestination(rc, 0, 56, 100, 134, 300, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL1, 0, 0);
		add(rc);

		SkinImage gc = new SkinImage();
		TextureRegion[][] gct = new TextureRegion[6][];
		for(int i = 0;i < gct.length;i++) {
			gct[i] = new TextureRegion[]{new TextureRegion(pt, 75,369 - i * 15 ,67,90)};
		}
		gc.setImage(gct, 0);
		gc.setReferenceID(MainState.BUTTON_GAUGE_1P);
		setDestination(gc, 0, 200, 220, 134, 180, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL1, 0, 0);
		add(gc);

		SkinImage hc = new SkinImage();
		TextureRegion[][] hct = new TextureRegion[5][];
		for(int i = 0;i < hct.length;i++) {
			hct[i] = new TextureRegion[]{new TextureRegion(pt, 146,354 - i * 15 ,67,75)};
		}
		hc.setImage(hct, 0);
		hc.setReferenceID(MainState.BUTTON_HSFIX);
		setDestination(hc, 0, 342, 250, 134, 150, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL1, 0, 0);
		add(hc);

		SkinImage rc2 = new SkinImage();
		rc2.setImage(rct, 0);
		rc2.setReferenceID(MainState.BUTTON_RANDOM_2P);
		setDestination(rc2, 0, 486, 100, 134, 300, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL1, 0, 0);
		add(rc2);

		SkinImage fc = new SkinImage();
		TextureRegion[][] fct = new TextureRegion[3][];
		for(int i = 0;i < fct.length;i++) {
			fct[i] = new TextureRegion[]{new TextureRegion(pt, 218,324 - i * 15 ,67,45)};
		}
		fc.setImage(fct, 0);
		fc.setReferenceID(MainState.BUTTON_DPOPTION);
		setDestination(fc, 0, 270, 594, 134, 90, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL1, 0, 0);
		add(fc);

		// option panel2
		addImage(new TextureRegion(st, 8,0,8,8), 500, 0, 0,1280, 720, 0, 128,255,255,255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL2, 0, 0);
		addImage(new TextureRegion(pt, 298, 0, 300, 159), 0, 50, 370, 600, 318, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL2, 0, 0);

		TextureRegion[][] aot = new TextureRegion[2][];
		for(int i = 0;i < aot.length;i++) {
			aot[i] = new TextureRegion[]{new TextureRegion(pt, 299,175 - i * 15 ,67,15)};
		}
		SkinImage ac = new SkinImage();
		ac.setImage(aot, 0);
		ac.setReferenceID(MainState.BUTTON_ASSIST_EXJUDGE);
		setDestination(ac, 0, 52, 368, 134, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL2, 0, 0);
		add(ac);
		ac = new SkinImage();
		ac.setImage(aot, 0);
		ac.setReferenceID(MainState.BUTTON_ASSIST_CONSTANT);
		setDestination(ac, 0, 124, 596, 134, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL2, 0, 0);
		add(ac);
		ac = new SkinImage();
		ac.setImage(aot, 0);
		ac.setReferenceID(MainState.BUTTON_ASSIST_JUDGEAREA);
		setDestination(ac, 0, 192, 368, 134, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL2, 0, 0);
		add(ac);
		ac = new SkinImage();
		ac.setImage(aot, 0);
		ac.setReferenceID(MainState.BUTTON_ASSIST_LEGACY);
		setDestination(ac, 0, 264, 596, 134, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL2, 0, 0);
		add(ac);
		ac = new SkinImage();
		ac.setImage(aot, 0);
		ac.setReferenceID(MainState.BUTTON_ASSIST_MARKNOTE);
		setDestination(ac, 0, 332, 368, 134, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL2, 0, 0);
		add(ac);
		ac = new SkinImage();
		ac.setImage(aot, 0);
		ac.setReferenceID(MainState.BUTTON_ASSIST_BPMGUIDE);
		setDestination(ac, 0, 404, 596, 134, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL2, 0, 0);
		add(ac);
		ac = new SkinImage();
		ac.setImage(aot, 0);
		ac.setReferenceID(MainState.BUTTON_ASSIST_NOMINE);
		setDestination(ac, 0, 472, 368, 134, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL2, 0, 0);
		add(ac);
		// option panel3
		addImage(new TextureRegion(st, 8,0,8,8), 500, 0, 0,1280, 720, 0, 128,255,255,255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL3, 0, 0);
		addImage(new TextureRegion(pt, 598, 0, 300, 188), 0, 50, 314, 600, 374, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL3, 0, 0);

		ac = new SkinImage();
		ac.setImage(fct, 0);
		ac.setReferenceID(MainState.BUTTON_BGA);
		setDestination(ac, 0, 52, 312, 134, 90, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL3, 0, 0);
		add(ac);
		ac = new SkinImage();
		ac.setImage(fct, 0);
		ac.setReferenceID(MainState.BUTTON_JUDGEDETAIL);
		setDestination(ac, 0, 120, 594, 134, 90, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL3, 0, 0);
		add(ac);

		addNumber(new SkinNumber(ntr[0], 0, 4, 0, MainState.NUMBER_DURATION), 0, 440, 598, 12, 24, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL3, 0, 0);
		addNumber(new SkinNumber(ntr[1], ntr[2], 0, 3, 0, MainState.NUMBER_JUDGETIMING), 0, 512, 376, 12, 24, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, MainState.OPTION_PANEL3, 0, 0);

		// mode,sort,lnmode
		TextureRegion[][] modet = new TextureRegion[6][];
		final int[] mode_lr2 = {5,3,0,4,1,2};
		for(int i = 0;i < modet.length;i++) {
			modet[i] = new TextureRegion[]{new TextureRegion(st, 240 ,600 + mode_lr2[i] * 24 ,144,24)};
		}
		SkinImage mode = new SkinImage();
		mode.setImage(modet, 0);
		mode.setReferenceID(MusicSelector.BUTTON_MODE);
		setDestination(mode, 0, 120, 10, 144, 24, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(mode);
		addImage(new TextureRegion(st, 240,744 ,144,24), 0, 10, 10,144, 24, 0, 128,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		TextureRegion[][] sortt = new TextureRegion[5][];
		for(int i = 0;i < sortt.length;i++) {
			sortt[i] = new TextureRegion[]{new TextureRegion(st, 400 ,600 + i * 24 ,120,24)};
		}
		SkinImage sort = new SkinImage();
		sort.setImage(sortt, 0);
		sort.setReferenceID(MusicSelector.BUTTON_SORT);
		setDestination(sort, 0, 390, 10, 120, 24, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(sort);
		addImage(new TextureRegion(st, 400,720 ,96,24), 0, 280, 10,96, 24, 0, 128,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		TextureRegion[][] lnt = new TextureRegion[3][];
		for(int i = 0;i < lnt.length;i++) {
			lnt[i] = new TextureRegion[]{new TextureRegion(st, 550 ,600 + i * 24 ,72,24)};
		}
		SkinImage lnmode = new SkinImage();
		lnmode.setImage(lnt, 0);
		lnmode.setReferenceID(MusicSelector.BUTTON_LNMODE);
		setDestination(lnmode, 0, 700, 10, 72, 24, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(lnmode);
		addImage(new TextureRegion(st, 550,672 ,144,24), 0, 540, 10,144, 24, 0, 128,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);


		// timer
		addNumber(new SkinNumber(ntr[0], 0, 4, 1, MainState.NUMBER_TIME_YEAR), 0, 1028, 2, 12, 12, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, MainState.NUMBER_TIME_MONTH), 0, 1088, 2, 12, 12, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, MainState.NUMBER_TIME_DAY), 0, 1130, 2, 12, 12, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, MainState.NUMBER_TIME_HOUR), 0, 1178, 2, 12, 12, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, MainState.NUMBER_TIME_MINUTE), 0, 1214, 2, 12, 12, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, MainState.NUMBER_TIME_SECOND), 0, 1250, 2, 12, 12, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
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

	public Sprite[] getTrophy() {
		return trophy;
	}

	public static class SkinBarObject extends SkinObject {

		@Override
		public void draw(SpriteBatch sprite, long time, MainState state) {
			((MusicSelector)state).renderBar((int)time);
		}

		@Override
		public void dispose() {

		}
	}
}
