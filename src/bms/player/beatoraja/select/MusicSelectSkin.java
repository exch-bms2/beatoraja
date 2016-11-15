package bms.player.beatoraja.select;

import bms.player.beatoraja.skin.*;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Rectangle;

import static bms.player.beatoraja.skin.SkinProperty.*;

public class MusicSelectSkin extends Skin {

	private int centerBar;
	private int[] clickableBar = new int[0];

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
		
		TextureRegion[][] bar = new TextureRegion[10][];
		for (int i = 0; i < bar.length; i++) {
			bar[i] = new TextureRegion[]{new TextureRegion(bart, 0, i * 30, 500, 30)};
		}
		TextureRegion[][] lampt = TextureRegion.split(new Texture("skin/lamp.png"), 15, 30);
        SkinImage[] lamp = new SkinImage[11];
		for (int i = 0; i < lamp.length; i++) {
			lamp[i] = new SkinImage(lampt[i], 100);
			setDestination(lamp[i], 0, 0, 2, 15, 34, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		}

		TextureRegion[][] ttrophy = TextureRegion.split(new Texture("skin/trophy.png"), 32, 32);
		SkinImage[] trophy = new SkinImage[3];
		trophy[0] = new SkinImage(ttrophy[0][10]);
		trophy[1] = new SkinImage(ttrophy[0][11]);
		trophy[2] = new SkinImage(ttrophy[0][12]);

		SkinText dir = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24, 2);
		dir.setReferenceID(STRING_DIRECTORY);
		setDestination(dir, 0, 40, 670, 18, 24, 0, 255, 255, 128, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(dir);
		SkinText genre = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 20, 2);
		genre.setReferenceID(STRING_GENRE);
		setDestination(genre, 0, 100, 630, 18, 20, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(genre);
		SkinText title = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24, 2);
		title.setReferenceID(STRING_FULLTITLE);
		setDestination(title, 0, 100, 600, 18, 24, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(title);
		SkinText artist = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 20, 2);
		artist.setReferenceID(STRING_ARTIST);
		setDestination(artist, 0, 100, 570, 18, 20, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(artist);

		SkinText grade1 = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24, 2);
		grade1.setReferenceID(STRING_COURSE1_TITLE);
		setDestination(grade1, 0, 80, 596, 18, 24, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(grade1);
		SkinText grade2 = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24, 2);
		grade2.setReferenceID(STRING_COURSE2_TITLE);
		setDestination(grade2, 0, 80, 568, 18, 24, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(grade2);
		SkinText grade3 = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24, 2);
		grade3.setReferenceID(STRING_COURSE3_TITLE);
		setDestination(grade3, 0, 80, 540, 18, 24, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(grade3);
		SkinText grade4 = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24, 2);
		grade4.setReferenceID(STRING_COURSE4_TITLE);
		setDestination(grade4, 0, 80, 512, 18, 24, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(grade4);

		Texture cl = new Texture("skin/clear.png");
		SkinImage clear = new SkinImage();
		clear.setImage(TextureRegion.split(cl, 200, 20), 0);
		clear.setReferenceID(NUMBER_CLEAR);
		setDestination(clear, 0, 100, 405, 200, 20, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(clear);
		// 数字
		Texture st = new Texture("skin/system.png");

		Texture nt = new Texture("skin/number.png");
		TextureRegion[][] ntr = TextureRegion.split(nt, 24, 24);

		addImage(new TextureRegion(st, 0, 816, 220, 24),0, 95, 482, 165, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_FOLDERBAR, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 5, 0, NUMBER_FOLDER_TOTALSONGS), 0, 260, 482, 18, 18, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, OPTION_FOLDERBAR, 0, 0);
		// lamp/rank distribution graph
		addImage(new TextureRegion(st, 400, 648, 96, 24),0, 36, 362, 48, 24, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_FOLDERBAR, 0, 0);
		SkinDistributionGraph lampgraph = new SkinDistributionGraph(0);
		setDestination(lampgraph,0, 90, 362, 300, 24, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_FOLDERBAR, 0, 0);
		add(lampgraph);
		addImage(new TextureRegion(st, 0, 672, 96, 24),0, 36, 322, 48, 24, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_FOLDERBAR, 0, 0);
		SkinDistributionGraph rankgraph = new SkinDistributionGraph(1);
		setDestination(rankgraph,0, 90, 322, 300, 24, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_FOLDERBAR, 0, 0);
		add(rankgraph);

		// key
		SkinImage bg = new SkinImage(IMAGE_BANNER);
		setDestination(bg, 0, 400, 400, 300, 90, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.add(bg);

		addImage(new TextureRegion(st, 240, 600, 144, 24),0, 100, 512, 108, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_7KEYSONG, 0, 0);
		addImage(new TextureRegion(st, 240, 624, 144, 24),0, 100, 512, 108, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_14KEYSONG, 0, 0);
		addImage(new TextureRegion(st, 240, 648, 144, 24),0, 100, 512, 108, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_9KEYSONG, 0, 0);
		addImage(new TextureRegion(st, 240, 672, 144, 24),0, 100, 512, 108, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_5KEYSONG, 0, 0);
		addImage(new TextureRegion(st, 240, 696, 144, 24),0, 100, 512, 108, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_10KEYSONG, 0, 0);

		addImage(new TextureRegion(st, 0, 768, 72, 24),0, 300, 512, 60, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_SONGBAR, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, NUMBER_MINBPM), 0, 370, 512, 18, 18, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, NUMBER_MAXBPM), 0, 442, 512, 18, 18, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addImage(new TextureRegion(st, 0, 790, 120, 24),0, 100, 482, 90, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_SONGBAR, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 3, 0, NUMBER_PLAYLEVEL), 0, 200, 482, 18, 18, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addImage(new TextureRegion(st, 0, 600, 120, 24),0, 80, 372, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_SONGBAR, 0, 0);
		addImage(new TextureRegion(st, 0, 600, 120, 24),0, 80, 372, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_GRADEBAR, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, NUMBER_SCORE), 0, 200, 372, 18, 18, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addImage(new TextureRegion(st, 0, 672, 96, 24),0, 330, 372, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_SONGBAR, 0, 0);
		addImage(new TextureRegion(st, 0, 672, 96, 24),0, 330, 372, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_GRADEBAR, 0, 0);
		addImage(new TextureRegion(st, 0, 624, 220, 24),0, 80, 342, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_SONGBAR, 0, 0);
		addImage(new TextureRegion(st, 0, 624, 220, 24),0, 80, 342, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_GRADEBAR, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, NUMBER_MISSCOUNT), 0, 200, 342, 18, 18, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addImage(new TextureRegion(st, 0, 648, 192, 24),0, 330, 342, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_SONGBAR, 0, 0);
		addImage(new TextureRegion(st, 0, 648, 192, 24),0, 330, 342, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_GRADEBAR, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, NUMBER_MAXCOMBO), 0, 450, 342, 18, 18, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addImage(new TextureRegion(st, 0, 696, 120, 24),0, 80, 312, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_SONGBAR, 0, 0);
		addImage(new TextureRegion(st, 0, 696, 120, 24),0, 80, 312, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_GRADEBAR, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, NUMBER_CLEARCOUNT), 0, 320, 312, 18, 18, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addImage(new TextureRegion(st, 0, 720, 96, 24),0, 200, 312, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_SONGBAR, 0, 0);
		addImage(new TextureRegion(st, 0, 720, 96, 24),0, 200, 312, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_GRADEBAR, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, NUMBER_PLAYCOUNT), 0, 400, 312, 18, 18, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		// play, autoplay, replay button
		addImage(new TextureRegion(st, 0, 310, 15, 15),0, 80, 450, 30, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_SONGBAR, 0, 0).setClickevent(BUTTON_PLAY);
		addImage(new TextureRegion(st, 0, 310, 15, 15),0, 80, 450, 30, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_GRADEBAR, 0, 0).setClickevent(BUTTON_PLAY);
		addImage(new TextureRegion(st, 0, 325, 15, 15),0, 130, 450, 30, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_SONGBAR, 0, 0).setClickevent(BUTTON_AUTOPLAY);
		addImage(new TextureRegion(st, 0, 325, 15, 15),0, 130, 450, 30, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_GRADEBAR, 0, 0).setClickevent(BUTTON_AUTOPLAY);
		addImage(new TextureRegion(st, 0, 340, 15, 15),0, 180, 450, 30, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_REPLAYDATA, 0, 0).setClickevent(BUTTON_REPLAY);
		addImage(new TextureRegion(st, 0, 355, 15, 15),0, 230, 450, 30, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_REPLAYDATA2, 0, 0).setClickevent(BUTTON_REPLAY2);
		addImage(new TextureRegion(st, 0, 370, 15, 15),0, 280, 450, 30, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_REPLAYDATA3, 0, 0).setClickevent(BUTTON_REPLAY3);
		addImage(new TextureRegion(st, 0, 385, 15, 15),0, 330, 450, 30, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_REPLAYDATA4, 0, 0).setClickevent(BUTTON_REPLAY4);

		addImage(new TextureRegion(st, 0, 720, 96, 24),0, 60, 102, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 6, 0, NUMBER_TOTALPLAYCOUNT), 0, 160, 102, 18, 18, 0,
				255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addImage(new TextureRegion(st, 0, 744, 120, 24),0, 330, 102, 100, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 10, 0, NUMBER_TOTALPLAYNOTES), 0, 430, 102, 18, 18, 0,
				255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        SkinBar barobj = new SkinBar(0, bar, 0);
		add(barobj);
        for(int i = 0;i < 22;i++) {
            setDestination(barobj.makeBarImages(false, i),0, 800, 720 - i * 36, 500, 36, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            setDestination(barobj.makeBarImages(true, i),0, 780, 720 - i * 36, 500, 36, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
        setCenterBar(10);
        barobj.setLamp(lamp);
		barobj.setTrophy(trophy);
		SkinBar.SkinBarText bartext = new SkinBar.SkinBarText("skin/VL-Gothic-Regular.ttf", 0, 24, 2);
		setDestination(bartext, 0, 80, 30, 18, 24, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		barobj.getBarText()[0] = bartext;
		bartext = new SkinBar.SkinBarText("skin/VL-Gothic-Regular.ttf", 0, 24, 2);
		setDestination(bartext, 0, 80, 30, 18, 24, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		barobj.getBarText()[1] = bartext;
		for(int i = 0;i < 7;i++) {
			barobj.getBarlevel()[i] = new SkinNumber(ntr[0], 0, 2, 0);
		}
		setDestination(barobj.getBarlevel()[0], 0, 20, 8, 24, 24, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(barobj.getBarlevel()[1], 0, 20, 8, 24, 24, 0, 255, 0, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(barobj.getBarlevel()[2], 0, 20, 8, 24, 24, 0, 255, 0, 0, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(barobj.getBarlevel()[3], 0, 20, 8, 24, 24, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(barobj.getBarlevel()[4], 0, 20, 8, 24, 24, 0, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(barobj.getBarlevel()[5], 0, 20, 8, 24, 24, 0, 255, 255, 0, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(barobj.getBarlevel()[6], 0, 20, 8, 24, 24, 0, 255, 128, 128, 128, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		addImage(new TextureRegion(st, 0, 10, 10, 251), 0, 1240, 75, 10, 570, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		SkinSlider seek = new SkinSlider(new TextureRegion[] { new TextureRegion(st, 0, 265, 17, 24) }, 0, 2,
				(int) (516 * dh), SLIDER_MUSICSELECT_POSITION);
		setDestination(seek, 0, 1237, 606, 17, 24, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0, 0, 0, 0);
		seek.setChangable(true);
		add(seek);

		// option panel1
		addImage(new TextureRegion(st, 8,0,8,8), 500, 0, 0,1280, 720, 0, 128,255,255,255, 0, 0, 0, 0, 0, 0, OPTION_PANEL1, 0, 0);
		Texture pt = new Texture("skin/panel.png");
		addImage(new TextureRegion(pt, 0, 0, 290, 294), 0, 50, 100, 580, 588, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL1, 0, 0);
		SkinImage rc = new SkinImage();
		TextureRegion[][] rct = new TextureRegion[10][];
		for(int i = 0;i < rct.length;i++) {
			rct[i] = new TextureRegion[]{new TextureRegion(pt, 3,429 - i * 15 ,67,150)};
		}
		rc.setImage(rct, 0);
		rc.setReferenceID(BUTTON_RANDOM_1P);
		setDestination(rc, 0, 56, 100, 134, 300, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL1, 0, 0);
		add(rc);

		SkinImage gc = new SkinImage();
		TextureRegion[][] gct = new TextureRegion[6][];
		for(int i = 0;i < gct.length;i++) {
			gct[i] = new TextureRegion[]{new TextureRegion(pt, 75,369 - i * 15 ,67,90)};
		}
		gc.setImage(gct, 0);
		gc.setReferenceID(BUTTON_GAUGE_1P);
		setDestination(gc, 0, 200, 220, 134, 180, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL1, 0, 0);
		add(gc);

		SkinImage hc = new SkinImage();
		TextureRegion[][] hct = new TextureRegion[5][];
		for(int i = 0;i < hct.length;i++) {
			hct[i] = new TextureRegion[]{new TextureRegion(pt, 146,354 - i * 15 ,67,75)};
		}
		hc.setImage(hct, 0);
		hc.setReferenceID(BUTTON_HSFIX);
		setDestination(hc, 0, 342, 250, 134, 150, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL1, 0, 0);
		add(hc);

		SkinImage rc2 = new SkinImage();
		rc2.setImage(rct, 0);
		rc2.setReferenceID(BUTTON_RANDOM_2P);
		setDestination(rc2, 0, 486, 100, 134, 300, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL1, 0, 0);
		add(rc2);

		SkinImage fc = new SkinImage();
		TextureRegion[][] fct = new TextureRegion[3][];
		for(int i = 0;i < fct.length;i++) {
			fct[i] = new TextureRegion[]{new TextureRegion(pt, 218,324 - i * 15 ,67,45)};
		}
		fc.setImage(fct, 0);
		fc.setReferenceID(BUTTON_DPOPTION);
		setDestination(fc, 0, 270, 594, 134, 90, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL1, 0, 0);
		add(fc);

		// option panel2
		addImage(new TextureRegion(st, 8,0,8,8), 500, 0, 0,1280, 720, 0, 128,255,255,255, 0, 0, 0, 0, 0, 0, OPTION_PANEL2, 0, 0);
		addImage(new TextureRegion(pt, 298, 0, 300, 159), 0, 50, 370, 600, 318, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL2, 0, 0);

		TextureRegion[][] aot = new TextureRegion[2][];
		for(int i = 0;i < aot.length;i++) {
			aot[i] = new TextureRegion[]{new TextureRegion(pt, 299,175 - i * 15 ,67,15)};
		}
		SkinImage ac = new SkinImage();
		ac.setImage(aot, 0);
		ac.setReferenceID(BUTTON_ASSIST_EXJUDGE);
		setDestination(ac, 0, 52, 368, 134, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL2, 0, 0);
		add(ac);
		ac = new SkinImage();
		ac.setImage(aot, 0);
		ac.setReferenceID(BUTTON_ASSIST_CONSTANT);
		setDestination(ac, 0, 124, 596, 134, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL2, 0, 0);
		add(ac);
		ac = new SkinImage();
		ac.setImage(aot, 0);
		ac.setReferenceID(BUTTON_ASSIST_JUDGEAREA);
		setDestination(ac, 0, 192, 368, 134, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL2, 0, 0);
		add(ac);
		ac = new SkinImage();
		ac.setImage(aot, 0);
		ac.setReferenceID(BUTTON_ASSIST_LEGACY);
		setDestination(ac, 0, 264, 596, 134, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL2, 0, 0);
		add(ac);
		ac = new SkinImage();
		ac.setImage(aot, 0);
		ac.setReferenceID(BUTTON_ASSIST_MARKNOTE);
		setDestination(ac, 0, 332, 368, 134, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL2, 0, 0);
		add(ac);
		ac = new SkinImage();
		ac.setImage(aot, 0);
		ac.setReferenceID(BUTTON_ASSIST_BPMGUIDE);
		setDestination(ac, 0, 404, 596, 134, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL2, 0, 0);
		add(ac);
		ac = new SkinImage(aot, 0);
		ac.setReferenceID(BUTTON_ASSIST_NOMINE);
		setDestination(ac, 0, 472, 368, 134, 30, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL2, 0, 0);
		add(ac);
		// option panel3
		addImage(new TextureRegion(st, 8,0,8,8), 500, 0, 0,1280, 720, 0, 128,255,255,255, 0, 0, 0, 0, 0, 0, OPTION_PANEL3, 0, 0);
		addImage(new TextureRegion(pt, 598, 0, 300, 188), 0, 50, 314, 600, 374, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL3, 0, 0);

		ac = new SkinImage(fct, 0);
		ac.setReferenceID(BUTTON_BGA);
		setDestination(ac, 0, 52, 312, 134, 90, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL3, 0, 0);
		add(ac);
		ac = new SkinImage(fct, 0);
		ac.setReferenceID(BUTTON_JUDGEDETAIL);
		setDestination(ac, 0, 120, 594, 134, 90, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL3, 0, 0);
		add(ac);

		addNumber(new SkinNumber(ntr[0], 0, 4, 0, NUMBER_DURATION), 0, 440, 598, 12, 24, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, OPTION_PANEL3, 0, 0);
		addNumber(new SkinNumber(ntr[1], ntr[2], 0, 3, 0, NUMBER_JUDGETIMING), 0, 512, 376, 12, 24, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, OPTION_PANEL3, 0, 0);

		// mode,sort,lnmode
		TextureRegion[][] modet = new TextureRegion[6][];
		final int[] mode_lr2 = {5,3,0,4,1,2};
		for(int i = 0;i < modet.length;i++) {
			modet[i] = new TextureRegion[]{new TextureRegion(st, 240 ,600 + mode_lr2[i] * 24 ,144,24)};
		}
		SkinImage mode = new SkinImage();
		mode.setImage(modet, 0);
		mode.setReferenceID(BUTTON_MODE);
		setDestination(mode, 0, 120, 10, 108, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(mode);
		addImage(new TextureRegion(st, 240,744 ,144,24), 0, 10, 10,108, 18, 0, 128,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		TextureRegion[][] sortt = new TextureRegion[5][];
		for(int i = 0;i < sortt.length;i++) {
			sortt[i] = new TextureRegion[]{new TextureRegion(st, 400 ,600 + i * 24 ,120,24)};
		}
		SkinImage sort = new SkinImage();
		sort.setImage(sortt, 0);
		sort.setReferenceID(BUTTON_SORT);
		setDestination(sort, 0, 390, 10, 108, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(sort);
		addImage(new TextureRegion(st, 400,720 ,96,24), 0, 280, 10,72, 18, 0, 128,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		TextureRegion[][] lnt = new TextureRegion[3][];
		for(int i = 0;i < lnt.length;i++) {
			lnt[i] = new TextureRegion[]{new TextureRegion(st, 550 ,600 + i * 24 ,72,24)};
		}
		SkinImage lnmode = new SkinImage();
		lnmode.setImage(lnt, 0);
		lnmode.setReferenceID(BUTTON_LNMODE);
		setDestination(lnmode, 0, 700, 10, 54, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(lnmode);
		addImage(new TextureRegion(st, 550,672 ,144,24), 0, 540, 10,108, 18, 0, 128,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);


		// timer
		addNumber(new SkinNumber(ntr[0], 0, 4, 1, NUMBER_TIME_YEAR), 0, 1028, 2, 12, 12, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, NUMBER_TIME_MONTH), 0, 1088, 2, 12, 12, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, NUMBER_TIME_DAY), 0, 1130, 2, 12, 12, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, NUMBER_TIME_HOUR), 0, 1178, 2, 12, 12, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, NUMBER_TIME_MINUTE), 0, 1214, 2, 12, 12, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, NUMBER_TIME_SECOND), 0, 1250, 2, 12, 12, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
	}

	public int[] getClickableBar() {
		return clickableBar;
	}

	public void setClickableBar(int[] clickableBar) {
		this.clickableBar = clickableBar;
	}

	public int getCenterBar() {
		return centerBar;
	}

	public void setCenterBar(int centerBar) {
		this.centerBar = centerBar;
	}

}
