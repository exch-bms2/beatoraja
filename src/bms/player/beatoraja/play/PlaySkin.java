package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.*;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * プレイスキン
 * 
 * @author exch
 */
public class PlaySkin extends Skin {

	private int playstart;
	/**
	 * ノーツ画像
	 */
	private Sprite[] note = new Sprite[8];
	/**
	 * ロングノーツ画像
	 */
	private Sprite[][] longnote = new Sprite[10][8];
	/**
	 * 地雷ノーツ画像
	 */
	private Sprite[] minenote = new Sprite[8];

	private Sprite[] gauge;

	private TextureRegion[][] judge;

	private TextureRegion[][][] judgenum;

	private SkinGraph[] graph;

	private SkinImage[] line = new SkinImage[0];
	/**
	 * レーン描画エリア
	 */
	private Rectangle[] laneregion;

	private Rectangle[] lanegroupregion;

	private Rectangle gaugeregion;

	private JudgeRegion[] judgeregion = new JudgeRegion[0];

	private float dw;
	private float dh;

	private int close;

	private final int[] judgecount = { MainState.NUMBER_EARLY_PERFECT,
			MainState.NUMBER_LATE_PERFECT, MainState.NUMBER_EARLY_GREAT,
			MainState.NUMBER_LATE_GREAT,
			MainState.NUMBER_EARLY_GOOD,
			MainState.NUMBER_LATE_GOOD, MainState.NUMBER_EARLY_BAD,
			MainState.NUMBER_LATE_BAD, MainState.NUMBER_EARLY_POOR,
			MainState.NUMBER_LATE_POOR,
			MainState.NUMBER_EARLY_MISS,
			MainState.NUMBER_LATE_MISS };

	private BMSPlayer player;

	public PlaySkin(int mode) {
		super(640, 480, 1280, 720);
	}

	public PlaySkin(int mode, boolean use2p, Rectangle r) {
		super(1280, 720, r.width, r.height);
		dw = r.width / 1280f;
		dh = r.height / 720f;

		makeCommonSkin();
		if (mode == 5 || mode == 7) {
			if(use2p) {
				make7KeySkin2();
			} else {
				make7KeySkin();
			}
		} else if (mode == 10 || mode == 14) {
			make14KeySkin();
		} else {
			make9KeySkin();
		}

		// 閉店
		Texture close = new Texture("skin/close.png");
		SkinImage ci = new SkinImage(new TextureRegion[] { new TextureRegion(close, 0, 500, 640, 240) }, 0);
		setDestination(ci, 0, 0, -360, 1280, 360, 0, 255, 255, 255, 255, 0, 0, 0, 0, 700, BMSPlayer.TIMER_FAILED, 0, 0, 0);
		setDestination(ci, 500, 0, 0, 1280, 360, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(ci, 600, 0, -40, 1280, 360, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(ci, 700, 0, 0, 1280, 360, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(ci);
		ci = new SkinImage(new TextureRegion[] { new TextureRegion(close, 0, 740, 640, 240) }, 0);
		setDestination(ci, 0, 0, 720, 1280, 360, 0, 255, 255, 255, 255, 0, 0, 0, 0, 700, BMSPlayer.TIMER_FAILED, 0, 0, 0);
		setDestination(ci, 500, 0, 360, 1280, 360, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(ci, 600, 0, 400, 1280, 360, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(ci, 700, 0, 360, 1280, 360, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(ci);

		Texture nt = new Texture("skin/system.png");
		SkinImage fi = new SkinImage(new TextureRegion[] { new TextureRegion(nt, 0, 0, 8, 8) }, 0);
		setDestination(fi, 0, 0, 0, 1280, 720, 0, 0, 255, 255, 255, 0, 0, 0, 0, 500, BMSPlayer.TIMER_FADEOUT, 0, 0, 0);
		setDestination(fi, 500, 0, 0, 1280, 720, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(fi);

		setPlaystartTime(1000);
		setCloseTime(1500);
		setFadeoutTime(1000);
	}

	private void makeCommonSkin() {
		SkinImage back = new SkinImage(new TextureRegion[] { new TextureRegion(new Texture("skin/play.png")) }, 0);
		setDestination(back, 0, 0, 0, 1280, 720, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(back);
		Texture bg = new Texture("skin/playbg.png");
		SkinImage images = new SkinImage(new TextureRegion[] { new TextureRegion(bg) }, 0);
		setDestination(images, 0, 0, 0, 1280, 720, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(images);
		// ゲージ
		Texture gt = new Texture("skin/gauge.png");
		gauge = new Sprite[8];
		gauge[0] = new Sprite(gt, 0, 0, 5, 17);
		gauge[1] = new Sprite(gt, 5, 0, 5, 17);
		gauge[2] = new Sprite(gt, 10, 0, 5, 17);
		gauge[3] = new Sprite(gt, 15, 0, 5, 17);
		gauge[4] = new Sprite(gt, 0, 17, 5, 17);
		gauge[5] = new Sprite(gt, 5, 17, 5, 17);
		gauge[6] = new Sprite(gt, 10, 17, 5, 17);
		gauge[7] = new Sprite(gt, 15, 17, 5, 17);
		// 判定文字
		Texture jt = new Texture("skin/judge.png");
		judge = new TextureRegion[6][];
		judge[0] = new TextureRegion[]{new TextureRegion(jt, 0, 0, 180, 50),new TextureRegion(jt, 0, 50, 180, 50)};
		judge[1] = new TextureRegion[]{new TextureRegion(jt, 0, 150, 180, 50),new TextureRegion(jt, 0, 350, 180, 50)};
		judge[2] = new TextureRegion[]{new TextureRegion(jt, 0, 200, 180, 50),new TextureRegion(jt, 0, 350, 180, 50)};
		judge[3] = new TextureRegion[]{new TextureRegion(jt, 0, 250, 180, 50),new TextureRegion(jt, 0, 350, 180, 50)};
		judge[4] = new TextureRegion[]{new TextureRegion(jt, 0, 300, 180, 50),new TextureRegion(jt, 0, 350, 180, 50)};
		judgenum = new Sprite[3][2][10];
		for (int i = 0; i < 10; i++) {
			judgenum[0][0][i] = new Sprite(jt, 30 * i + 200, 0, 30, 50);
			judgenum[0][1][i] = new Sprite(jt, 30 * i + 200, 50, 30, 50);
			judgenum[1][0][i] = new Sprite(jt, 30 * i + 200, 150, 30, 50);
			judgenum[1][1][i] = new Sprite(jt, 30 * i + 200, 350, 30, 50);
			judgenum[2][0][i] = new Sprite(jt, 30 * i + 200, 200, 30, 50);
			judgenum[2][1][i] = new Sprite(jt, 30 * i + 200, 350, 30, 50);
		}
		// 数字
		Texture nt = new Texture("skin/number.png");
		TextureRegion[][] ntr = TextureRegion.split(nt, 24, 24);

		// bpm
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, MainState.NUMBER_MINBPM), 0, 520, 2, 18, 18, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, MainState.OPTION_BPMCHANGE, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, MainState.NUMBER_NOWBPM), 0, 592, 2, 24, 24, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 4, 0, MainState.NUMBER_MAXBPM), 0, 688, 2, 18, 18, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, MainState.OPTION_BPMCHANGE, 0, 0);
		// 残り時間
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, MainState.NUMBER_TIMELEFT_MINUTE), 0, 1148, 2, 24, 24, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, MainState.NUMBER_TIMELEFT_SECOND), 0, 1220, 2, 24, 24, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		addNumber(new SkinNumber(ntr[0], 0, 2, 0, MainState.NUMBER_HISPEED), 0, 116, 2, 12, 24, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 2, 1, MainState.NUMBER_HISPEED_AFTERDOT), 0, 154, 2, 10, 20, 0, 255,
				255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		addNumber(new SkinNumber(ntr[0], 0, 4, 0, MainState.NUMBER_DURATION), 0, 318, 2, 12, 24, 0, 255, 255,
				255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		Texture grapht = new Texture("skin/scoregraph.png");
		TextureRegion[] graphtr = new TextureRegion[3];
		graphtr[0] = new TextureRegion(grapht, 0, 0, 100, 296);
		graphtr[1] = new TextureRegion(grapht, 100, 0, 100, 296);
		graphtr[2] = new TextureRegion(grapht, 200, 0, 100, 296);

		graph = new SkinGraph[3];
		graph[0] = new SkinGraph(new TextureRegion[] { new TextureRegion(grapht, 0, 0, 100, 296) }, 0);
		graph[0].setReferenceID(MainState.BARGRAPH_SCORERATE);
		graph[1] = new SkinGraph(new TextureRegion[] { new TextureRegion(grapht, 100, 0, 100, 296) }, 0);
		graph[1].setReferenceID(MainState.BARGRAPH_BESTSCORERATE);
		graph[2] = new SkinGraph(new TextureRegion[] { new TextureRegion(grapht, 200, 0, 100, 296) }, 0);
		graph[2].setReferenceID(MainState.BARGRAPH_TARGETSCORERATE);

	}

	private void make7KeySkin() {
		// 背景
		// background = new Texture("skin/bg.jpg");
		// ノーツ
		note = new Sprite[8];
		longnote = new Sprite[10][8];
		minenote = new Sprite[8];
		Texture notet = new Texture("skin/note.png");
		for (int i = 0; i < 8; i++) {
			if (i % 2 == 0) {
				note[i] = new Sprite(notet, 99, 5, 27, 8);
				longnote[0][i] = new Sprite(notet, 99, 43, 27, 13);
				longnote[1][i] = new Sprite(notet, 99, 57, 27, 13);
				longnote[2][i] = new Sprite(notet, 99, 80, 27, 1);
				longnote[3][i] = new Sprite(notet, 99, 76, 27, 1);
				longnote[4][i] = new Sprite(notet, 99, 94, 27, 13);
				longnote[5][i] = new Sprite(notet, 99, 108, 27, 13);
				longnote[6][i] = new Sprite(notet, 99, 131, 27, 1);
				longnote[7][i] = new Sprite(notet, 99, 127, 27, 1);
				longnote[8][i] = new Sprite(notet, 99, 128, 27, 1);
				longnote[9][i] = new Sprite(notet, 99, 129, 27, 1);
				minenote[i] = new Sprite(notet, 99, 23, 27, 8);
			} else if (i == 7) {
				note[i] = new Sprite(notet, 50, 5, 46, 8);
				longnote[0][i] = new Sprite(notet, 50, 43, 46, 13);
				longnote[1][i] = new Sprite(notet, 50, 57, 46, 13);
				longnote[2][i] = new Sprite(notet, 50, 80, 46, 1);
				longnote[3][i] = new Sprite(notet, 50, 76, 46, 1);
				longnote[4][i] = new Sprite(notet, 50, 94, 46, 13);
				longnote[5][i] = new Sprite(notet, 50, 108, 46, 13);
				longnote[6][i] = new Sprite(notet, 50, 131, 46, 1);
				longnote[7][i] = new Sprite(notet, 50, 127, 46, 1);
				longnote[8][i] = new Sprite(notet, 50, 128, 46, 1);
				longnote[9][i] = new Sprite(notet, 50, 129, 46, 1);
				minenote[i] = new Sprite(notet, 50, 23, 46, 8);
			} else {
				note[i] = new Sprite(notet, 127, 5, 21, 8);
				longnote[0][i] = new Sprite(notet, 127, 43, 21, 13);
				longnote[1][i] = new Sprite(notet, 127, 57, 21, 13);
				longnote[2][i] = new Sprite(notet, 127, 80, 21, 1);
				longnote[3][i] = new Sprite(notet, 127, 76, 21, 1);
				longnote[4][i] = new Sprite(notet, 127, 94, 21, 13);
				longnote[5][i] = new Sprite(notet, 127, 108, 21, 13);
				longnote[6][i] = new Sprite(notet, 127, 131, 21, 1);
				longnote[7][i] = new Sprite(notet, 127, 127, 21, 1);
				longnote[8][i] = new Sprite(notet, 127, 128, 21, 1);
				longnote[9][i] = new Sprite(notet, 127, 129, 21, 1);
				minenote[i] = new Sprite(notet, 127, 23, 21, 8);
			}
		}

		SkinImage[] images = new SkinImage[6];
		SkinNumber[] number = new SkinNumber[6];
		for (int i = 0; i < 6; i++) {
			images[i] = new SkinImage();
			images[i].setImage(judge[i == 5 ? 4 : i], 100);
			setDestination(images[i], 0, 115, 240, 180, 40, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			images[i].setOffsetYReferenceID(MainState.OFFSET_LIFT);
			number[i] = new SkinNumber(judgenum[i > 2 ? 2 : i], 100, 6, 0, MainState.NUMBER_MAXCOMBO);
			setDestination(number[i], 0, 200, 0, 40, 40, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		}
		judgeregion = new JudgeRegion[] { new JudgeRegion(images, number, true) };

		SkinBGAObject bga = new SkinBGAObject(this);
		setDestination(bga,0,500,50,740,650,0,255,255,255,255,0,0,0,0,0,0,0,0,0);
		add(bga);
		add(new SkinGaugeObject(this));

		SkinText title = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24, 2);
		title.setReferenceID(MainState.STRING_FULLTITLE);
		setDestination(title, 0, 502, 698, 24, 24, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(title, 1000, 502, 698, 24, 24, 0, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(title, 2000, 502, 698, 24, 24, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(title);

		laneregion = new Rectangle[8];
		laneregion[0] = rect(90, 140, 50, 580);
		laneregion[1] = rect(140, 140, 40, 580);
		laneregion[2] = rect(180, 140, 50, 580);
		laneregion[3] = rect(230, 140, 40, 580);
		laneregion[4] = rect(270, 140, 50, 580);
		laneregion[5] = rect(320, 140, 40, 580);
		laneregion[6] = rect(360, 140, 50, 580);
		laneregion[7] = rect(20, 140, 70, 580);
		Texture st = new Texture("skin/system.png");
		SkinImage si = new SkinImage(new TextureRegion[] { new TextureRegion(st, 30, 0, 390, 10) }, 0);
		setDestination(si, 0, 20, 140, 390, 0, 0, 0, 255, 255, 255, 0, 0, 0, 0, 1000, 0, 0, 0, 0);
		setDestination(si, 1000, 20, 140, 390, 580, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(si);

		// graph
		SkinImage gbi = new SkinImage(new TextureRegion[] { new TextureRegion(st, 168, 108, 126, 303) }, 0);
		setDestination(gbi, 0, 410, 220, 90,480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 1000, 0, 0, 0, 0);
		add(gbi);

		setDestination(graph[0], 0, 411, 220, 28, 480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(graph[0]);
		setDestination(graph[1], 0, 441, 220, 28, 480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(graph[1]);
		setDestination(graph[2], 0, 471, 220, 28, 480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(graph[2]);

		SkinImage gi = new SkinImage(new TextureRegion[] { new TextureRegion(st, 40, 108, 126, 303) }, 0);
		setDestination(gi, 0, 410, 220, 90,480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 1000, 0, 0, 0, 0);
		add(gi);

		// progress
		SkinImage progress = new SkinImage(new TextureRegion[] { new TextureRegion(st, 10, 10, 10, 251) }, 0);
		setDestination(progress, 0, 4, 140, 12, 540, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(progress);
		SkinSlider pseek = new SkinSlider(new TextureRegion[] { new TextureRegion(st, 0, 289, 14, 20) }, 0, 2,
				(int) (520 * dh), MainState.SLIDER_MUSIC_PROGRESS);
		setDestination(pseek, 0, 2, 660, 16, 20, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0, 0, 0, 0);
		add(pseek);
		pseek = new SkinSlider(new TextureRegion[] { new TextureRegion(st, 15, 289, 14, 20) }, 0, 2,
				(int) (520 * dh), MainState.SLIDER_MUSIC_PROGRESS);
		setDestination(pseek, 0, 2, 660, 16, 20, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, MainState.TIMER_ENDOFNOTE_1P, 0, 0, 0);
		add(pseek);

		Texture kbt = new Texture("skin/keybeam.png");
		Sprite[] keybeam = new Sprite[8];
		keybeam[0] = keybeam[2] = keybeam[4] = keybeam[6] = new Sprite(kbt, 47, 0, 28, 255);
		keybeam[1] = keybeam[3] = keybeam[5] = new Sprite(kbt, 75, 0, 21, 255);
		keybeam[7] = new Sprite(kbt, 0, 0, 47, 255);
		TextureRegion[] keybeaml = new Sprite[8];
		keybeaml[0] = keybeaml[2] = keybeaml[4] = keybeaml[6] = new Sprite(kbt, 144, 0, 28, 255);
		keybeaml[1] = keybeaml[3] = keybeaml[5] = new Sprite(kbt, 172, 0, 21, 255);
		keybeaml[7] = new Sprite(kbt, 97, 0, 47, 255);
		Texture bombt = new Texture("skin/bomb.png");
		TextureRegion[][] bombtr = TextureRegion.split(bombt, 181, 192);

		for (int i = 0; i < laneregion.length; i++) {
			SkinImage ri = new SkinImage(new TextureRegion[][] { { keybeam[i] }, { keybeaml[i] } }, 0);
			setDestination(ri, 0, laneregion[i].x / dw  + laneregion[i].width / dw / 4, laneregion[i].y / dh , laneregion[i].width / dw / 2,
					laneregion[i].height / dh , 0, 255, 255, 255, 255, 0, 0, 0, 0, 100, BMSPlayer.TIMER_KEYON_1P_KEY1 + (i % 8 == 7 ? -1 : i), 0, 0, 0);
			setDestination(ri, 100, laneregion[i].x / dw , laneregion[i].y / dh , laneregion[i].width / dw , laneregion[i].height / dh , 0,
					255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			ri.setOffsetYReferenceID(MainState.OFFSET_LIFT);
			ri.setReferenceID(BMSPlayer.VALUE_JUDGE_1P_KEY1 + (i % 8 == 7 ? -1 : i));
			add(ri);
		}
		line = new SkinImage[1];
		line[0] = new SkinImage(new TextureRegion[]{new TextureRegion(st,0,0,1,1)},0);
		setDestination(line[0], 0, 20, 140, 390, 1, 0, 255,255,255,255,0,0,0,0,0,0,0,0,0);
		add(new SkinLaneObject(this));
		Texture lct = new Texture("skin/lanecover.png");
		SkinSlider lanecover = new SkinSlider(new TextureRegion[]{new TextureRegion(lct)},0,2, (int) (580*dh), BMSPlayer.SLIDER_LANECOVER);
		setDestination(lanecover, 0, 20, 720, 390, 580, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(lanecover);

		for (int i = 0; i < laneregion.length; i++) {
			SkinImage bombi = new SkinImage(new TextureRegion[][] { {}, bombtr[3], bombtr[0], bombtr[1] }, 160);
			bombi.setTiming(BMSPlayer.TIMER_BOMB_1P_KEY1 + (i % 8 == 7 ? -1 : i));
			setDestination(bombi, 0, laneregion[i].x / dw + laneregion[i].width / dw / 2 - 141, laneregion[i].y / dh - 202, 322, 344,
					0, 255, 255, 255, 255, 2, 0, 0, 0, 161, BMSPlayer.TIMER_BOMB_1P_KEY1 + (i % 8 == 7 ? -1 : i), 0, 0, 0);
			setDestination(bombi, 160, laneregion[i].x / dw  + laneregion[i].width /dw / 2 - 141, laneregion[i].y / dh - 202, 322,
					344, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0, 0, 0, 0);
			setDestination(bombi, 161, laneregion[i].x / dw , laneregion[i].y / dh , 0, 0, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0,
					0, 0, 0);
			bombi.setOffsetYReferenceID(MainState.OFFSET_LIFT);
			bombi.setReferenceID(BMSPlayer.VALUE_JUDGE_1P_KEY1 + (i % 8 == 7 ? -1 : i));
			add(bombi);

			SkinImage hbombi = new SkinImage(bombtr[2], 160);
			hbombi.setTiming(BMSPlayer.TIMER_HOLD_1P_KEY1 + (i % 8 == 7 ? -1 : i));
			setDestination(hbombi, 0, laneregion[i].x / dw + laneregion[i].width / dw / 2 - 141, laneregion[i].y / dh - 202, 322, 344,
					0, 255, 255, 255, 255, 2, 0, 0, 0, 0, BMSPlayer.TIMER_HOLD_1P_KEY1 + (i % 8 == 7 ? -1 : i), 0, 0, 0);
			hbombi.setOffsetYReferenceID(MainState.OFFSET_LIFT);
			add(hbombi);
		}

		// judge count
		Texture nt = new Texture("skin/number.png");
		TextureRegion[][] ntr = TextureRegion.split(nt, 24, 24);
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 2; j++) {
				addNumber(new SkinNumber(ntr[j + 1], 0, 4, 2, judgecount[i * 2 + j]), 0, 536 + j * 60,
						50 + (5 - i) * 18, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			}
		}
		addNumber(new SkinNumber(ntr[0], 100, 4, 0, MainState.NUMBER_SCORE_RATE), 0, 420,
				200, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 100, 1, 0, MainState.NUMBER_SCORE_RATE_AFTERDOT), 0, 468,
				200, 8, 12, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 100, 5, 0, MainState.NUMBER_SCORE), 0, 420,
				180, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[1], 100, 5, 0, MainState.NUMBER_HIGHSCORE), 0, 420,
				160, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[2], 100, 5, 0, MainState.NUMBER_TARGET_SCORE), 0, 420,
				140, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		lanegroupregion = new Rectangle[] { rect(20, 140, 390, 580) };

		SkinSlider seek = new SkinSlider(new TextureRegion[] { new TextureRegion(st, 0, 265, 17, 24) }, 0, 1,
				(int) (360 * dh), MainState.BARGRAPH_LOAD_PROGRESS);
		setDestination(seek, 0, 20, 440, 30, 24, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0, 0, 0, 0);
		add(seek);
		// READY
		Texture ready = new Texture("skin/ready.png");
		SkinImage ri = new SkinImage(new TextureRegion[] { new TextureRegion(ready) }, 0);
		setDestination(ri, 0, 40, 250, 350, 60, 0, 0, 255, 255, 255, 0, 0, 0, 0, -1, BMSPlayer.TIMER_READY, 0, 0, 0);
		setDestination(ri, 750, 40, 300, 350, 60, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(ri, 1000, 40, 300, 350, 60, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(ri);

		gaugeregion = rect(20, 30, 390, 30);

		addNumber(new SkinNumber(ntr[0], 0, 3, 0, MainState.NUMBER_GROOVEGAUGE), 0, 314, 60, 24, 24, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 1, 0, MainState.NUMBER_GROOVEGAUGE_AFTERDOT), 0, 386, 60, 18, 18, 0, 255,
				255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

	}

	private void make7KeySkin2() {
		// 背景
		// background = new Texture("skin/bg.jpg");
		// ノーツ
		note = new Sprite[8];
		longnote = new Sprite[10][8];
		minenote = new Sprite[8];
		Texture notet = new Texture("skin/note.png");
		for (int i = 0; i < 8; i++) {
			if (i % 2 == 0) {
				note[i] = new Sprite(notet, 99, 5, 27, 8);
				longnote[0][i] = new Sprite(notet, 99, 43, 27, 13);
				longnote[1][i] = new Sprite(notet, 99, 57, 27, 13);
				longnote[2][i] = new Sprite(notet, 99, 80, 27, 1);
				longnote[3][i] = new Sprite(notet, 99, 76, 27, 1);
				longnote[4][i] = new Sprite(notet, 99, 94, 27, 13);
				longnote[5][i] = new Sprite(notet, 99, 108, 27, 13);
				longnote[6][i] = new Sprite(notet, 99, 131, 27, 1);
				longnote[7][i] = new Sprite(notet, 99, 127, 27, 1);
				longnote[8][i] = new Sprite(notet, 99, 128, 27, 1);
				longnote[9][i] = new Sprite(notet, 99, 129, 27, 1);
				minenote[i] = new Sprite(notet, 99, 23, 27, 8);
			} else if (i == 7) {
				note[i] = new Sprite(notet, 50, 5, 46, 8);
				longnote[0][i] = new Sprite(notet, 50, 43, 46, 13);
				longnote[1][i] = new Sprite(notet, 50, 57, 46, 13);
				longnote[2][i] = new Sprite(notet, 50, 80, 46, 1);
				longnote[3][i] = new Sprite(notet, 50, 76, 46, 1);
				longnote[4][i] = new Sprite(notet, 50, 94, 46, 13);
				longnote[5][i] = new Sprite(notet, 50, 108, 46, 13);
				longnote[6][i] = new Sprite(notet, 50, 131, 46, 1);
				longnote[7][i] = new Sprite(notet, 50, 127, 46, 1);
				longnote[8][i] = new Sprite(notet, 50, 128, 46, 1);
				longnote[9][i] = new Sprite(notet, 50, 129, 46, 1);
				minenote[i] = new Sprite(notet, 50, 23, 46, 8);
			} else {
				note[i] = new Sprite(notet, 127, 5, 21, 8);
				longnote[0][i] = new Sprite(notet, 127, 43, 21, 13);
				longnote[1][i] = new Sprite(notet, 127, 57, 21, 13);
				longnote[2][i] = new Sprite(notet, 127, 80, 21, 1);
				longnote[3][i] = new Sprite(notet, 127, 76, 21, 1);
				longnote[4][i] = new Sprite(notet, 127, 94, 21, 13);
				longnote[5][i] = new Sprite(notet, 127, 108, 21, 13);
				longnote[6][i] = new Sprite(notet, 127, 131, 21, 1);
				longnote[7][i] = new Sprite(notet, 127, 127, 21, 1);
				longnote[8][i] = new Sprite(notet, 127, 128, 21, 1);
				longnote[9][i] = new Sprite(notet, 127, 129, 21, 1);
				minenote[i] = new Sprite(notet, 127, 23, 21, 8);
			}
		}

		SkinImage[] images = new SkinImage[6];
		SkinNumber[] number = new SkinNumber[6];
		for (int i = 0; i < 6; i++) {
			images[i] = new SkinImage();
			images[i].setImage(judge[i == 5 ? 4 : i], 100);
			setDestination(images[i], 0, 965, 240, 180, 40, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			images[i].setOffsetYReferenceID(MainState.OFFSET_LIFT);
			number[i] = new SkinNumber(judgenum[i > 2 ? 2 : i], 100, 6, 0, MainState.NUMBER_MAXCOMBO);
			setDestination(number[i], 0, 200, 0, 40, 40, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		}
		judgeregion = new JudgeRegion[] { new JudgeRegion(images, number, true) };

		SkinBGAObject bga = new SkinBGAObject(this);
		setDestination(bga,0,40,50,740,650,0,255,255,255,255,0,0,0,0,0,0,0,0,0);
		add(bga);
		add(new SkinGaugeObject(this));

		SkinText title = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24, 2);
		title.setReferenceID(MainState.STRING_FULLTITLE);
		setDestination(title, 0, 42, 698, 24, 24, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(title, 1000, 42, 698, 24, 24, 0, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(title, 2000, 42, 698, 24, 24, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(title);

		laneregion = new Rectangle[8];
		laneregion[0] = rect(870, 140, 50, 580);
		laneregion[1] = rect(920, 140, 40, 580);
		laneregion[2] = rect(960, 140, 50, 580);
		laneregion[3] = rect(1010, 140, 40, 580);
		laneregion[4] = rect(1050, 140, 50, 580);
		laneregion[5] = rect(1100, 140, 40, 580);
		laneregion[6] = rect(1140, 140, 50, 580);
		laneregion[7] = rect(1190, 140, 70, 580);
		Texture st = new Texture("skin/system.png");
		SkinImage si = new SkinImage(new TextureRegion[] { new TextureRegion(st, 30, 0, 390, 10) }, 0);
		setDestination(si, 0, 1260, 140, -390, 0, 0, 0, 255, 255, 255, 0, 0, 0, 0, 1000, 0, 0, 0, 0);
		setDestination(si, 1000, 1260, 140, -390, 580, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(si);

		// graph
		SkinImage gbi = new SkinImage(new TextureRegion[] { new TextureRegion(st, 168, 108, 126, 303) }, 0);
		setDestination(gbi, 0, 780, 220, 90,480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 1000, 0, 0, 0, 0);
		add(gbi);

		setDestination(graph[0], 0, 841, 220, 28, 480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(graph[0]);
		setDestination(graph[1], 0, 811, 220, 28, 480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(graph[1]);
		setDestination(graph[2], 0, 781, 220, 28, 480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(graph[2]);

		SkinImage gi = new SkinImage(new TextureRegion[] { new TextureRegion(st, 40, 108, 126, 303) }, 0);
		setDestination(gi, 0, 780, 220, 90,480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 1000, 0, 0, 0, 0);
		add(gi);

		// progress
		SkinImage progress = new SkinImage(new TextureRegion[] { new TextureRegion(st, 10, 10, 10, 251) }, 0);
		setDestination(progress, 0, 1264, 140, 12, 540, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(progress);
		SkinSlider pseek = new SkinSlider(new TextureRegion[] { new TextureRegion(st, 0, 289, 14, 20) }, 0, 2,
				(int) (520 * dh), MainState.SLIDER_MUSIC_PROGRESS);
		setDestination(pseek, 0, 1264, 660, 16, 20, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0, 0, 0, 0);
		add(pseek);
		pseek = new SkinSlider(new TextureRegion[] { new TextureRegion(st, 15, 289, 14, 20) }, 0, 2,
				(int) (520 * dh), MainState.SLIDER_MUSIC_PROGRESS);
		setDestination(pseek, 0, 1264, 660, 16, 20, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, MainState.TIMER_ENDOFNOTE_1P, 0, 0, 0);
		add(pseek);

		Texture kbt = new Texture("skin/keybeam.png");
		Sprite[] keybeam = new Sprite[8];
		keybeam[0] = keybeam[2] = keybeam[4] = keybeam[6] = new Sprite(kbt, 47, 0, 28, 255);
		keybeam[1] = keybeam[3] = keybeam[5] = new Sprite(kbt, 75, 0, 21, 255);
		keybeam[7] = new Sprite(kbt, 0, 0, 47, 255);
		TextureRegion[] keybeaml = new Sprite[8];
		keybeaml[0] = keybeaml[2] = keybeaml[4] = keybeaml[6] = new Sprite(kbt, 144, 0, 28, 255);
		keybeaml[1] = keybeaml[3] = keybeaml[5] = new Sprite(kbt, 172, 0, 21, 255);
		keybeaml[7] = new Sprite(kbt, 97, 0, 47, 255);
		Texture bombt = new Texture("skin/bomb.png");
		TextureRegion[][] bombtr = TextureRegion.split(bombt, 181, 192);

		for (int i = 0; i < laneregion.length; i++) {
			SkinImage ri = new SkinImage(new TextureRegion[][] { { keybeam[i] }, { keybeaml[i] } }, 0);
			setDestination(ri, 0, laneregion[i].x / dw  + laneregion[i].width / dw / 4, laneregion[i].y / dh , laneregion[i].width / dw / 2,
					laneregion[i].height / dh , 0, 255, 255, 255, 255, 0, 0, 0, 0, 100, BMSPlayer.TIMER_KEYON_1P_KEY1 + (i % 8 == 7 ? -1 : i), 0, 0, 0);
			setDestination(ri, 100, laneregion[i].x / dw , laneregion[i].y / dh , laneregion[i].width / dw , laneregion[i].height / dh , 0,
					255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			ri.setOffsetYReferenceID(MainState.OFFSET_LIFT);
			ri.setReferenceID(BMSPlayer.VALUE_JUDGE_1P_KEY1 + (i % 8 == 7 ? -1 : i));
			add(ri);
		}
		line = new SkinImage[1];
		line[0] = new SkinImage(new TextureRegion[]{new TextureRegion(st,0,0,1,1)},0);
		setDestination(line[0], 0, 870, 140, 390, 1, 0, 255,255,255,255,0,0,0,0,0,0,0,0,0);
		add(new SkinLaneObject(this));
		Texture lct = new Texture("skin/lanecover.png");
		SkinSlider lanecover = new SkinSlider(new TextureRegion[]{new TextureRegion(lct)},0,2, (int) (580*dh), BMSPlayer.SLIDER_LANECOVER);
		setDestination(lanecover, 0, 870, 720, 390, 580, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(lanecover);

		for (int i = 0; i < laneregion.length; i++) {
			SkinImage bombi = new SkinImage(new TextureRegion[][] { {}, bombtr[3], bombtr[0], bombtr[1] }, 160);
			bombi.setTiming(BMSPlayer.TIMER_BOMB_1P_KEY1 + (i % 8 == 7 ? -1 : i));
			setDestination(bombi, 0, laneregion[i].x / dw + laneregion[i].width / dw / 2 - 141, laneregion[i].y / dh - 202, 322, 344,
					0, 255, 255, 255, 255, 2, 0, 0, 0, 161, BMSPlayer.TIMER_BOMB_1P_KEY1 + (i % 8 == 7 ? -1 : i), 0, 0, 0);
			setDestination(bombi, 160, laneregion[i].x / dw  + laneregion[i].width /dw / 2 - 141, laneregion[i].y / dh - 202, 322,
					344, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0, 0, 0, 0);
			setDestination(bombi, 161, laneregion[i].x / dw , laneregion[i].y / dh , 0, 0, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0,
					0, 0, 0);
			bombi.setOffsetYReferenceID(MainState.OFFSET_LIFT);
			bombi.setReferenceID(BMSPlayer.VALUE_JUDGE_1P_KEY1 + (i % 8 == 7 ? -1 : i));
			add(bombi);

			SkinImage hbombi = new SkinImage(bombtr[2], 160);
			hbombi.setTiming(BMSPlayer.TIMER_HOLD_1P_KEY1 + (i % 8 == 7 ? -1 : i));
			setDestination(hbombi, 0, laneregion[i].x / dw + laneregion[i].width / dw / 2 - 141, laneregion[i].y / dh - 202, 322, 344,
					0, 255, 255, 255, 255, 2, 0, 0, 0, 0, BMSPlayer.TIMER_HOLD_1P_KEY1 + (i % 8 == 7 ? -1 : i), 0, 0, 0);
			hbombi.setOffsetYReferenceID(MainState.OFFSET_LIFT);
			add(hbombi);
		}

		// judge count
		Texture nt = new Texture("skin/number.png");
		TextureRegion[][] ntr = TextureRegion.split(nt, 24, 24);
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 2; j++) {
				addNumber(new SkinNumber(ntr[j + 1], 0, 4, 2, judgecount[i * 2 + j]), 0, 660 + j * 60,
						50 + (5 - i) * 18, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			}
		}
		addNumber(new SkinNumber(ntr[0], 100, 4, 0, MainState.NUMBER_SCORE_RATE), 0, 790,
				200, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 100, 1, 0, MainState.NUMBER_SCORE_RATE_AFTERDOT), 0, 838,
				200, 8, 12, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 100, 5, 0, MainState.NUMBER_SCORE), 0, 790,
				180, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[1], 100, 5, 0, MainState.NUMBER_HIGHSCORE), 0, 790,
				160, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[2], 100, 5, 0, MainState.NUMBER_TARGET_SCORE), 0, 790,
				140, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		lanegroupregion = new Rectangle[] { rect(870, 140, 390, 580) };

		SkinSlider seek = new SkinSlider(new TextureRegion[] { new TextureRegion(st, 0, 265, 17, 24) }, 0, 1,
				(int) (360 * dh), MainState.BARGRAPH_LOAD_PROGRESS);
		setDestination(seek, 0, 870, 440, 30, 24, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0, 0, 0, 0);
		add(seek);
		// READY
		Texture ready = new Texture("skin/ready.png");
		SkinImage ri = new SkinImage(new TextureRegion[] { new TextureRegion(ready) }, 0);
		setDestination(ri, 0, 870, 250, 350, 60, 0, 0, 255, 255, 255, 0, 0, 0, 0, -1, BMSPlayer.TIMER_READY, 0, 0, 0);
		setDestination(ri, 750, 870, 300, 350, 60, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(ri, 1000, 870, 300, 350, 60, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(ri);

		gaugeregion = rect(1260, 30, -390, 30);

		addNumber(new SkinNumber(ntr[0], 0, 3, 0, MainState.NUMBER_GROOVEGAUGE), 0, 870, 60, 24, 24, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 1, 0, MainState.NUMBER_GROOVEGAUGE_AFTERDOT), 0, 942, 60, 18, 18, 0, 255,
				255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

	}

	private void make9KeySkin() {
		// 背景
		// background = new Texture("skin/bg.jpg");
		// ノーツ
		note = new Sprite[9];
		longnote = new Sprite[10][9];
		minenote = new Sprite[9];
		Texture notet = new Texture("skin/pop.png");
		for (int i = 0; i < 9; i++) {
			if (i == 0 || i == 8) {
				note[i] = new Sprite(notet, 0, 0, 36, 18);
				longnote[0][i] = new Sprite(notet, 0, 18, 36, 18);
				longnote[1][i] = new Sprite(notet, 0, 38, 36, 18);
				longnote[2][i] = new Sprite(notet, 0, 38, 36, 1);
				longnote[3][i] = new Sprite(notet, 0, 36, 36, 1);
				longnote[4][i] = new Sprite(notet, 0, 18, 36, 18);
				longnote[5][i] = new Sprite(notet, 0, 38, 36, 18);
				longnote[6][i] = new Sprite(notet, 0, 38, 36, 1);
				longnote[7][i] = new Sprite(notet, 0, 36, 36, 1);
				longnote[8][i] = new Sprite(notet, 0, 38, 36, 1);
				longnote[9][i] = new Sprite(notet, 0, 64, 36, 1);
				minenote[i] = new Sprite(notet, 0, 56, 36, 18);
			}
			if (i == 1 || i == 7) {
				note[i] = new Sprite(notet, 38, 0, 28, 18);
				longnote[0][i] = new Sprite(notet, 38, 18, 28, 18);
				longnote[1][i] = new Sprite(notet, 38, 38, 28, 18);
				longnote[2][i] = new Sprite(notet, 38, 38, 28, 1);
				longnote[3][i] = new Sprite(notet, 38, 36, 28, 1);
				longnote[4][i] = new Sprite(notet, 38, 18, 28, 18);
				longnote[5][i] = new Sprite(notet, 38, 38, 28, 18);
				longnote[6][i] = new Sprite(notet, 38, 38, 28, 1);
				longnote[7][i] = new Sprite(notet, 38, 36, 28, 1);
				longnote[8][i] = new Sprite(notet, 38, 38, 28, 1);
				longnote[9][i] = new Sprite(notet, 38, 64, 28, 1);
				minenote[i] = new Sprite(notet, 38, 56, 28, 18);
			}
			if (i == 2 || i == 6) {
				note[i] = new Sprite(notet, 68, 0, 36, 18);
				longnote[0][i] = new Sprite(notet, 68, 18, 36, 18);
				longnote[1][i] = new Sprite(notet, 68, 38, 36, 18);
				longnote[2][i] = new Sprite(notet, 68, 38, 36, 1);
				longnote[3][i] = new Sprite(notet, 68, 36, 36, 1);
				longnote[4][i] = new Sprite(notet, 68, 18, 36, 18);
				longnote[5][i] = new Sprite(notet, 68, 38, 36, 18);
				longnote[6][i] = new Sprite(notet, 68, 38, 36, 1);
				longnote[7][i] = new Sprite(notet, 68, 36, 36, 1);
				longnote[8][i] = new Sprite(notet, 68, 38, 36, 1);
				longnote[9][i] = new Sprite(notet, 68, 64, 36, 1);
				minenote[i] = new Sprite(notet, 68, 56, 36, 18);
			}
			if (i == 3 || i == 5) {
				note[i] = new Sprite(notet, 106, 0, 28, 18);
				longnote[0][i] = new Sprite(notet, 106, 18, 28, 18);
				longnote[1][i] = new Sprite(notet, 106, 38, 28, 18);
				longnote[2][i] = new Sprite(notet, 106, 38, 28, 1);
				longnote[3][i] = new Sprite(notet, 106, 36, 28, 1);
				longnote[4][i] = new Sprite(notet, 106, 18, 28, 18);
				longnote[5][i] = new Sprite(notet, 106, 38, 28, 18);
				longnote[6][i] = new Sprite(notet, 106, 38, 28, 1);
				longnote[7][i] = new Sprite(notet, 106, 36, 28, 1);
				longnote[8][i] = new Sprite(notet, 106, 38, 28, 1);
				longnote[9][i] = new Sprite(notet, 106, 64, 28, 1);
				minenote[i] = new Sprite(notet, 106, 56, 28, 18);
			}
			if (i == 4) {
				note[i] = new Sprite(notet, 136, 0, 36, 18);
				longnote[0][i] = new Sprite(notet, 136, 18, 36, 18);
				longnote[1][i] = new Sprite(notet, 136, 38, 36, 18);
				longnote[2][i] = new Sprite(notet, 136, 38, 36, 1);
				longnote[3][i] = new Sprite(notet, 136, 36, 36, 1);
				longnote[4][i] = new Sprite(notet, 136, 18, 36, 18);
				longnote[5][i] = new Sprite(notet, 136, 38, 36, 18);
				longnote[6][i] = new Sprite(notet, 136, 38, 36, 1);
				longnote[7][i] = new Sprite(notet, 136, 36, 36, 1);
				longnote[8][i] = new Sprite(notet, 136, 38, 36, 1);
				longnote[9][i] = new Sprite(notet, 136, 64, 36, 1);
				minenote[i] = new Sprite(notet, 136, 56, 36, 18);
			}
		}

		SkinImage[] images = new SkinImage[6];
		SkinNumber[] number = new SkinNumber[6];
		for (int i = 0; i < 6; i++) {
			images[i] = new SkinImage();
			images[i].setImage(judge[i == 5 ? 4 : i], 100);
			setDestination(images[i], 0, 375, 240, 140, 20, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			images[i].setOffsetYReferenceID(MainState.OFFSET_LIFT);
			number[i] = new SkinNumber(judgenum[i > 2 ? 2 : i], 100, 6, 0, MainState.NUMBER_MAXCOMBO);
			setDestination(number[i], 0, 70, -30, 20, 20, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		}
		JudgeRegion jr1 = new JudgeRegion(images, number, false);
		images = new SkinImage[6];
		number = new SkinNumber[6];
		for (int i = 0; i < 6; i++) {
			images[i] = new SkinImage();
			images[i].setImage(judge[i == 5 ? 4 : i], 100);
			setDestination(images[i], 0, 570, 240, 140, 20, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			images[i].setOffsetYReferenceID(MainState.OFFSET_LIFT);
			number[i] = new SkinNumber(judgenum[i > 2 ? 2 : i], 100, 6, 0, MainState.NUMBER_MAXCOMBO);
			setDestination(number[i], 0, 70, -30, 20, 20, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		}
		JudgeRegion jr2 = new JudgeRegion(images, number, false);
		images = new SkinImage[6];
		number = new SkinNumber[6];
		for (int i = 0; i < 6; i++) {
			images[i] = new SkinImage();
			images[i].setImage(judge[i == 5 ? 4 : i], 100);
			setDestination(images[i], 0, 765, 240, 140, 20, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			images[i].setOffsetYReferenceID(MainState.OFFSET_LIFT);
			number[i] = new SkinNumber(judgenum[i > 2 ? 2 : i], 100, 6, 0, MainState.NUMBER_MAXCOMBO);
			setDestination(number[i], 0, 70, -30, 20, 20, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		}
		JudgeRegion jr3 = new JudgeRegion(images, number, false);

		judgeregion = new JudgeRegion[] { jr1, jr2, jr3 };

		SkinBGAObject bga = new SkinBGAObject(this);
		setDestination(bga,0,10,390,330,330,0,255,255,255,255,0,0,0,0,0,0,0,0,0);
		add(bga);
		SkinBGAObject bga2 = new SkinBGAObject(this);
		setDestination(bga2,0,10,50,330,330,0,255,255,255,255,0,0,0,0,0,0,0,0,0);
		add(bga2);
		add(new SkinGaugeObject(this));

		SkinText title = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24);
		title.setReferenceID(MainState.STRING_FULLTITLE);
		setDestination(title, 0, 12, 720, 18, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(title, 1000, 12, 720, 18, 18, 0, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(title, 2000, 12, 720, 18, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(title);

		laneregion = new Rectangle[9];
		laneregion[0] = rect(345, 140, 70, 580);
		laneregion[1] = rect(415, 140, 60, 580);
		laneregion[2] = rect(475, 140, 70, 580);
		laneregion[3] = rect(545, 140, 60, 580);
		laneregion[4] = rect(605, 140, 70, 580);
		laneregion[5] = rect(675, 140, 60, 580);
		laneregion[6] = rect(735, 140, 70, 580);
		laneregion[7] = rect(805, 140, 60, 580);
		laneregion[8] = rect(865, 140, 70, 580);
		Texture st = new Texture("skin/system.png");
		SkinImage si = new SkinImage(new TextureRegion[] { new TextureRegion(st, 30, 30, 590, 10) }, 0);
		setDestination(si, 0, 345, 140, 590, 0, 0, 0, 255, 255, 255, 0, 0, 0, 0, 1000, 0, 0, 0, 0);
		setDestination(si, 1000, 345, 140, 590, 580, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(si);

		// graph
		SkinImage gbi = new SkinImage(new TextureRegion[] { new TextureRegion(st, 168, 108, 126, 303) }, 0);
		setDestination(gbi, 0, 960, 220, 180,480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 1000, 0, 0, 0, 0);
		add(gbi);
		setDestination(graph[0], 0, 962, 220, 56, 480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(graph[0]);
		setDestination(graph[1], 0, 1022, 220, 56, 480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(graph[1]);
		setDestination(graph[2], 0, 1082, 220, 56, 480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(graph[2]);

		SkinImage gi = new SkinImage(new TextureRegion[] { new TextureRegion(st, 40, 108, 126, 303) }, 0);
		setDestination(gi, 0, 960, 220, 180,480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 1000, 0, 0, 0, 0);
		add(gi);

		Texture kbt = new Texture("skin/keybeaml.png");
		Sprite[] keybeam = new Sprite[9];
		keybeam[0] = keybeam[2] = keybeam[4] = keybeam[6] = keybeam[8] = new Sprite(kbt, 75, 0, 21, 255);
		keybeam[1] = keybeam[3] = keybeam[5] = keybeam[7] = new Sprite(kbt, 47, 0, 28, 255);
		TextureRegion[] keybeaml = new Sprite[9];
		keybeaml[0] = keybeaml[2] = keybeaml[4] = keybeaml[6] = keybeaml[8] = new Sprite(kbt, 172, 0, 21, 255);
		keybeaml[1] = keybeaml[3] = keybeaml[5] = keybeaml[7] = new Sprite(kbt, 144, 0, 28, 255);
		TextureRegion[] keybeamg = new Sprite[9];
		keybeamg[0] = keybeamg[2] = keybeamg[4] = keybeamg[6] = keybeamg[8] = new Sprite(kbt, 269, 0, 21, 255);
		keybeamg[1] = keybeamg[3] = keybeamg[5] = keybeamg[7] = new Sprite(kbt, 241, 0, 28, 255);

		Texture bombt = new Texture("skin/bomb.png");
		TextureRegion[][] bombtr = TextureRegion.split(bombt, 181, 192);

		for (int i = 0; i < laneregion.length; i++) {
			SkinImage bi = new SkinImage(new TextureRegion[][] { { keybeam[i] }, { keybeaml[i] }, { keybeamg[i] },
					{ keybeamg[i] } }, 0);
			setDestination(bi, 0, laneregion[i].x + laneregion[i].width / 4, laneregion[i].y, laneregion[i].width / 2,
					laneregion[i].height, 0, 255, 255, 255, 255, 0, 0, 0, 0, 100, BMSPlayer.TIMER_KEYON_1P_KEY1 + i, 0, 0, 0);
			setDestination(bi, 100, laneregion[i].x, laneregion[i].y, laneregion[i].width, laneregion[i].height, 0,
					255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			bi.setOffsetYReferenceID(MainState.OFFSET_LIFT);
			bi.setReferenceID(BMSPlayer.VALUE_JUDGE_1P_KEY1 + i);
			add(bi);

		}

		add(new SkinLaneObject(this));
		Texture lct = new Texture("skin/lanecover.png");
		SkinSlider lanecover = new SkinSlider(new TextureRegion[]{new TextureRegion(lct)},0,2, (int) (580*dh), BMSPlayer.SLIDER_LANECOVER);
		setDestination(lanecover, 0, 345, 720, 590, 580, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(lanecover);

		for (int i = 0; i < laneregion.length; i++) {
			SkinImage bombi = new SkinImage(bombtr[0], 160);
			bombi.setTiming(BMSPlayer.TIMER_BOMB_1P_KEY1 + i);
			setDestination(bombi, 0, laneregion[i].x + laneregion[i].width / 2 - 156, laneregion[i].y - 222, 362, 384,
					0, 255, 255, 255, 255, 2, 0, 0, 0, 161, BMSPlayer.TIMER_BOMB_1P_KEY1 + i, 0, 0, 0);
			setDestination(bombi, 160, laneregion[i].x + laneregion[i].width / 2 - 156, laneregion[i].y - 222, 362,
					384, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0, 0, 0, 0);
			setDestination(bombi, 161, laneregion[i].x, laneregion[i].y, 0, 0, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0,
					0, 0, 0);
			bombi.setOffsetYReferenceID(MainState.OFFSET_LIFT);
			add(bombi);

			SkinImage hbombi = new SkinImage(bombtr[2], 160);
			hbombi.setTiming(BMSPlayer.TIMER_HOLD_1P_KEY1 + i);
			setDestination(hbombi, 0, laneregion[i].x + laneregion[i].width / 2 - 156, laneregion[i].y - 222, 362, 384,
					0, 255, 255, 255, 255, 2, 0, 0, 0, 0, BMSPlayer.TIMER_HOLD_1P_KEY1 + i, 0, 0, 0);
			hbombi.setOffsetYReferenceID(MainState.OFFSET_LIFT);
			add(hbombi);
		}

		// judge count
		Texture nt = new Texture("skin/number.png");
		TextureRegion[][] ntr = TextureRegion.split(nt, 24, 24);
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 2; j++) {
				addNumber(new SkinNumber(ntr[j + 1], 0, 4, 2, judgecount[i * 2 + j]), 0, 1126 + j * 60,
						30 + (5 - i) * 18, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			}
		}
		addNumber(new SkinNumber(ntr[0], 100, 4, 0, MainState.NUMBER_SCORE_RATE), 0, 970,
				200, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 100, 1, 0, MainState.NUMBER_SCORE_RATE_AFTERDOT), 0, 1018,
				200, 8, 12, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 100, 5, 0, MainState.NUMBER_SCORE), 0, 970,
				180, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[1], 100, 5, 0, MainState.NUMBER_HIGHSCORE), 0, 970,
				160, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[2], 100, 5, 0, MainState.NUMBER_TARGET_SCORE), 0, 970,
				140, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		SkinSlider seek = new SkinSlider(new TextureRegion[] { new TextureRegion(st, 0, 265, 17, 24) }, 0, 1,
				(int) (560 * dh), MainState.BARGRAPH_LOAD_PROGRESS);
		setDestination(seek, 0, 345, 440, 30, 24, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0, 0, 0, 0);
		add(seek);
		// READY
		Texture ready = new Texture("skin/ready.png");
		SkinImage ri = new SkinImage(new TextureRegion[] { new TextureRegion(ready) }, 0);
		setDestination(ri, 0, 465, 250, 350, 60, 0, 0, 255, 255, 255, 0, 0, 0, 0, -1, BMSPlayer.TIMER_READY, 0, 0, 0);
		setDestination(ri, 750, 465, 300, 350, 60, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(ri, 1000, 465, 300, 350, 60, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(ri);

		lanegroupregion = new Rectangle[] { rect(345, 140, 590, 580) };

		gaugeregion = rect(345, 30, 590, 30);

		addNumber(new SkinNumber(ntr[0], 0, 3, 0, MainState.NUMBER_GROOVEGAUGE), 0, 600, 60, 24, 24, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 1, 0, MainState.NUMBER_GROOVEGAUGE_AFTERDOT), 0, 672, 60, 18, 18, 0, 255,
				255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		// progress
		SkinImage progress = new SkinImage(new TextureRegion[] { new TextureRegion(st, 10, 10, 10, 251) }, 0);
		setDestination(progress, 0, 940, 140, 12, 540, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(progress);
		SkinSlider pseek = new SkinSlider(new TextureRegion[] { new TextureRegion(st, 0, 289, 14, 20) }, 0, 2,
				(int) (520 * dh), MainState.SLIDER_MUSIC_PROGRESS);
		setDestination(pseek, 0, 938, 660, 16, 20, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0, 0, 0, 0);
		add(pseek);
		pseek = new SkinSlider(new TextureRegion[] { new TextureRegion(st, 15, 289, 14, 20) }, 0, 2,
				(int) (520 * dh), MainState.SLIDER_MUSIC_PROGRESS);
		setDestination(pseek, 0, 938, 660, 16, 20, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, MainState.TIMER_ENDOFNOTE_1P, 0, 0, 0);
		add(pseek);
	}

	private void make14KeySkin() {
		// 背景
		// background = new Texture("skin/bg.jpg");
		// ノーツ
		note = new Sprite[16];
		longnote = new Sprite[10][16];
		minenote = new Sprite[16];
		Texture notet = new Texture("skin/note.png");
		for (int i = 0; i < 16; i++) {
			if (i % 2 == 0) {
				note[i] = new Sprite(notet, 99, 5, 27, 8);
				longnote[0][i] = new Sprite(notet, 99, 43, 27, 13);
				longnote[1][i] = new Sprite(notet, 99, 57, 27, 13);
				longnote[2][i] = new Sprite(notet, 99, 80, 27, 1);
				longnote[3][i] = new Sprite(notet, 99, 76, 27, 1);
				longnote[4][i] = new Sprite(notet, 99, 94, 27, 13);
				longnote[5][i] = new Sprite(notet, 99, 108, 27, 13);
				longnote[6][i] = new Sprite(notet, 99, 131, 27, 1);
				longnote[7][i] = new Sprite(notet, 99, 127, 27, 1);
				longnote[8][i] = new Sprite(notet, 99, 128, 27, 1);
				longnote[9][i] = new Sprite(notet, 99, 129, 27, 1);
				minenote[i] = new Sprite(notet, 99, 23, 27, 8);
			} else if (i == 7 || i == 15) {
				note[i] = new Sprite(notet, 50, 5, 46, 8);
				longnote[0][i] = new Sprite(notet, 50, 43, 46, 13);
				longnote[1][i] = new Sprite(notet, 50, 57, 46, 13);
				longnote[2][i] = new Sprite(notet, 50, 80, 46, 1);
				longnote[3][i] = new Sprite(notet, 50, 76, 46, 1);
				longnote[4][i] = new Sprite(notet, 50, 94, 46, 13);
				longnote[5][i] = new Sprite(notet, 50, 108, 46, 13);
				longnote[6][i] = new Sprite(notet, 50, 131, 46, 1);
				longnote[7][i] = new Sprite(notet, 50, 127, 46, 1);
				longnote[8][i] = new Sprite(notet, 50, 128, 46, 1);
				longnote[9][i] = new Sprite(notet, 50, 129, 46, 1);
				minenote[i] = new Sprite(notet, 50, 23, 46, 8);
			} else {
				note[i] = new Sprite(notet, 127, 5, 21, 8);
				longnote[0][i] = new Sprite(notet, 127, 43, 21, 13);
				longnote[1][i] = new Sprite(notet, 127, 57, 21, 13);
				longnote[2][i] = new Sprite(notet, 127, 80, 21, 1);
				longnote[3][i] = new Sprite(notet, 127, 76, 21, 1);
				longnote[4][i] = new Sprite(notet, 127, 94, 21, 13);
				longnote[5][i] = new Sprite(notet, 127, 108, 21, 13);
				longnote[6][i] = new Sprite(notet, 127, 131, 21, 1);
				longnote[7][i] = new Sprite(notet, 127, 127, 21, 1);
				longnote[8][i] = new Sprite(notet, 127, 128, 21, 1);
				longnote[9][i] = new Sprite(notet, 127, 129, 21, 1);
				minenote[i] = new Sprite(notet, 127, 23, 21, 8);
			}
		}

		Texture kbt = new Texture("skin/keybeam.png");
		Sprite[] keybeam = new Sprite[16];
		keybeam[0] = keybeam[2] = keybeam[4] = keybeam[6] = keybeam[8] = keybeam[10] = keybeam[12] = keybeam[14] = new Sprite(
				kbt, 75, 0, 21, 255);
		keybeam[1] = keybeam[3] = keybeam[5] = keybeam[9] = keybeam[11] = keybeam[13] = new Sprite(kbt, 47, 0, 28, 255);
		keybeam[7] = keybeam[15] = new Sprite(kbt, 0, 0, 47, 255);

		SkinImage[] images = new SkinImage[6];
		SkinNumber[] number = new SkinNumber[6];
		for (int i = 0; i < 6; i++) {
			images[i] = new SkinImage();
			images[i].setImage(judge[i == 5 ? 4 : i], 100);
			setDestination(images[i], 0, 315, 240, 180, 40, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			images[i].setOffsetYReferenceID(MainState.OFFSET_LIFT);
			number[i] = new SkinNumber(judgenum[i > 2 ? 2 : i], 100, 6, 0, MainState.NUMBER_MAXCOMBO);
			setDestination(number[i], 0, 200, 0, 40, 40, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		}
		JudgeRegion jr1 = new JudgeRegion(images, number, true);
		images = new SkinImage[6];
		number = new SkinNumber[6];
		for (int i = 0; i < 6; i++) {
			images[i] = new SkinImage();
			images[i].setImage(judge[i == 5 ? 4 : i], 100);
			setDestination(images[i], 0, 785, 240, 180, 40, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			images[i].setOffsetYReferenceID(MainState.OFFSET_LIFT);
			number[i] = new SkinNumber(judgenum[i > 2 ? 2 : i], 100, 6, 0, MainState.NUMBER_MAXCOMBO);
			setDestination(number[i], 0, 200, 0, 40, 40, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		}
		JudgeRegion jr2 = new JudgeRegion(images, number, true);

		judgeregion = new JudgeRegion[] { jr1, jr2 };

		SkinBGAObject bga = new SkinBGAObject(this);
		setDestination(bga,0,10,500,180,220,0,255,255,255,255,0,0,0,0,0,0,0,0,0);
		add(bga);
		SkinBGAObject bga2 = new SkinBGAObject(this);
		setDestination(bga2,0,10,270,180,220,0,255,255,255,255,0,0,0,0,0,0,0,0,0);
		add(bga2);
		SkinBGAObject bga3 = new SkinBGAObject(this);
		setDestination(bga3,0,10,40,180,220,0,255,255,255,255,0,0,0,0,0,0,0,0,0);
		add(bga3);
		add(new SkinGaugeObject(this));

		SkinText title = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24);
		title.setReferenceID(MainState.STRING_FULLTITLE);
		setDestination(title, 0, 12, 720, 18, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(title, 1000, 12, 720, 18, 18, 0, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(title, 2000, 12, 720, 18, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(title);

		laneregion = new Rectangle[16];
		laneregion[0] = rect(280, 140, 50, 580);
		laneregion[1] = rect(330, 140, 40, 580);
		laneregion[2] = rect(370, 140, 50, 580);
		laneregion[3] = rect(420, 140, 40, 580);
		laneregion[4] = rect(460, 140, 50, 580);
		laneregion[5] = rect(510, 140, 40, 580);
		laneregion[6] = rect(550, 140, 50, 580);
		laneregion[7] = rect(210, 140, 70, 580);
		laneregion[8] = rect(680, 140, 50, 580);
		laneregion[9] = rect(730, 140, 40, 580);
		laneregion[10] = rect(770, 140, 50, 580);
		laneregion[11] = rect(820, 140, 40, 580);
		laneregion[12] = rect(860, 140, 50, 580);
		laneregion[13] = rect(910, 140, 40, 580);
		laneregion[14] = rect(950, 140, 50, 580);
		laneregion[15] = rect(1000, 140, 70, 580);

		Texture st = new Texture("skin/system.png");
		SkinImage si = new SkinImage(new TextureRegion[] { new TextureRegion(st, 30, 0, 390, 10) }, 0);
		setDestination(si, 0, 210, 140, 390, 0, 0, 0, 255, 255, 255, 0, 0, 0, 0, 1000, 0, 0, 0, 0);
		setDestination(si, 1000, 210, 140, 390, 580, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(si);
		si = new SkinImage(new TextureRegion[] { new TextureRegion(st, 30, 15, 390, 10) }, 0);
		setDestination(si, 0, 680, 140, 390, 0, 0, 0, 255, 255, 255, 0, 0, 0, 0, 1000, 0, 0, 0, 0);
		setDestination(si, 1000, 680, 140, 390, 580, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(si);

		// graph
		SkinImage gbi = new SkinImage(new TextureRegion[] { new TextureRegion(st, 168, 108, 126, 303) }, 0);
		setDestination(gbi, 0, 1090, 220, 180,480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 1000, 0, 0, 0, 0);
		add(gbi);

		setDestination(graph[0], 0, 1092, 220, 56, 480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(graph[0]);
		setDestination(graph[1], 0, 1152, 220, 56, 480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(graph[1]);
		setDestination(graph[2], 0, 1212, 220, 56, 480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(graph[2]);

		SkinImage gi = new SkinImage(new TextureRegion[] { new TextureRegion(st, 40, 108, 126, 303) }, 0);
		setDestination(gi, 0, 1090, 220, 180,480, 0, 255, 255, 255, 255, 0, 0, 0, 0, 1000, 0, 0, 0, 0);
		add(gi);

		Texture bombt = new Texture("skin/bomb.png");
		TextureRegion[][] bombtr = TextureRegion.split(bombt, 181, 192);
		for (int i = 0; i < laneregion.length; i++) {
			SkinImage ri = new SkinImage(new TextureRegion[] { keybeam[i] }, 0);
			setDestination(ri, 0, laneregion[i].x + laneregion[i].width / 4, laneregion[i].y, laneregion[i].width / 2,
					laneregion[i].height, 0, 255, 255, 255, 255, 0, 0, 0, 0, 100, BMSPlayer.TIMER_KEYON_1P_KEY1 + (i % 8 == 7 ? -1 : (i % 8)) + (i >= 8 ? 10 : 0), 0, 0, 0);
			setDestination(ri, 100, laneregion[i].x, laneregion[i].y, laneregion[i].width, laneregion[i].height, 0,
					255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			ri.setOffsetYReferenceID(MainState.OFFSET_LIFT);
			add(ri);
		}
		line = new SkinImage[2];
		line[0] = new SkinImage(new TextureRegion[]{new TextureRegion(st,0,0,1,1)},0);
		setDestination(line[0], 0, 210, 140, 390, 1, 0, 255,255,255,255,0,0,0,0,0,0,0,0,0);
		line[1] = new SkinImage(new TextureRegion[]{new TextureRegion(st,0,0,1,1)},0);
		setDestination(line[1], 0, 680, 140, 390, 1, 0, 255,255,255,255,0,0,0,0,0,0,0,0,0);
		add(new SkinLaneObject(this));
		Texture lct = new Texture("skin/lanecover.png");
		SkinSlider lanecover = new SkinSlider(new TextureRegion[]{new TextureRegion(lct)},0,2, (int) (580*dh), BMSPlayer.SLIDER_LANECOVER);
		setDestination(lanecover, 0, 210, 720, 390, 580, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(lanecover);
		SkinSlider lanecover2 = new SkinSlider(new TextureRegion[]{new TextureRegion(lct)},0,2, (int) (580*dh), BMSPlayer.SLIDER_LANECOVER);
		setDestination(lanecover2, 0, 680, 720, 390, 580, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(lanecover2);

		for (int i = 0; i < laneregion.length; i++) {
			SkinImage bombi = new SkinImage(new TextureRegion[][] { {}, bombtr[3], bombtr[0], bombtr[1] }, 160);
			bombi.setTiming(BMSPlayer.TIMER_BOMB_1P_KEY1 + (i % 8 == 7 ? -1 : (i % 8)) + (i >= 8 ? 10 : 0));
			setDestination(bombi, 0, laneregion[i].x + laneregion[i].width / 2 - 141, laneregion[i].y - 202, 322, 344,
					0, 255, 255, 255, 255, 2, 0, 0, 0, 161, BMSPlayer.TIMER_BOMB_1P_KEY1 + (i % 8 == 7 ? -1 : (i % 8)) + (i >= 8 ? 10 : 0), 0, 0, 0);
			setDestination(bombi, 160, laneregion[i].x + laneregion[i].width / 2 - 141, laneregion[i].y - 202, 322,
					344, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0, 0, 0, 0);
			setDestination(bombi, 161, laneregion[i].x, laneregion[i].y, 0, 0, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0,
					0, 0, 0);
			bombi.setOffsetYReferenceID(MainState.OFFSET_LIFT);
			bombi.setReferenceID(BMSPlayer.VALUE_JUDGE_1P_KEY1 + (i % 8 == 7 ? -1 : (i % 8)) + (i >= 8 ? 10 : 0));
			add(bombi);

			SkinImage hbombi = new SkinImage(bombtr[2], 160);
			hbombi.setTiming(BMSPlayer.TIMER_HOLD_1P_KEY1 + (i % 8 == 7 ? -1 : (i % 8)) + (i >= 8 ? 10 : 0));
			setDestination(hbombi, 0, laneregion[i].x + laneregion[i].width / 2 - 141, laneregion[i].y - 202, 322, 344,
					0, 255, 255, 255, 255, 2, 0, 0, 0, 0, BMSPlayer.TIMER_HOLD_1P_KEY1 + (i % 8 == 7 ? -1 : (i % 8)) + (i >= 8 ? 10 : 0), 0, 0, 0);
			hbombi.setOffsetYReferenceID(MainState.OFFSET_LIFT);
			add(hbombi);
		}

		// judge count
		Texture nt = new Texture("skin/number.png");
		TextureRegion[][] ntr = TextureRegion.split(nt, 24, 24);
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 2; j++) {
				addNumber(new SkinNumber(ntr[j + 1], 0, 4, 2, judgecount[i * 2 + j]), 0, 1166 + j * 60,
						40 + (5 - i) * 18, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			}
		}
		addNumber(new SkinNumber(ntr[0], 100, 4, 0, MainState.NUMBER_SCORE_RATE), 0, 1100,
				200, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 100, 1, 0, MainState.NUMBER_SCORE_RATE_AFTERDOT), 0, 1148,
				200, 8, 12, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 100, 5, 0, MainState.NUMBER_SCORE), 0, 1100,
				180, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[1], 100, 5, 0, MainState.NUMBER_HIGHSCORE), 0, 1100,
				160, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[2], 100, 5, 0, MainState.NUMBER_TARGET_SCORE), 0, 1100,
				140, 12, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		SkinSlider seek = new SkinSlider(new TextureRegion[] { new TextureRegion(st, 0, 265, 17, 24) }, 0, 1,
				(int) (360 * dh), MainState.BARGRAPH_LOAD_PROGRESS);
		setDestination(seek, 0, 210, 440, 30, 24, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0, 0, 0, 0);
		add(seek);

		lanegroupregion = new Rectangle[] { rect(210, 140, 390, 580), rect(680, 140, 390, 580) };
		// READY
		Texture ready = new Texture("skin/ready.png");
		SkinImage ri = new SkinImage(new TextureRegion[] { new TextureRegion(ready) }, 0);
		setDestination(ri, 0, 230, 250, 350, 60, 0, 0, 255, 255, 255, 0, 0, 0, 0, -1, BMSPlayer.TIMER_READY, 0, 0, 0);
		setDestination(ri, 750, 230, 300, 350, 60, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(ri, 1000, 230, 300, 350, 60, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(ri);
		ri = new SkinImage(new TextureRegion[] { new TextureRegion(ready) }, 0);
		setDestination(ri, 0, 700, 250, 350, 60, 0, 0, 255, 255, 255, 0, 0, 0, 0, -1, BMSPlayer.TIMER_READY, 0, 0, 0);
		setDestination(ri, 750, 700, 300, 350, 60, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setDestination(ri, 1000, 700, 300, 350, 60, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(ri);

		gaugeregion = rect(445, 30, 390, 30);

		addNumber(new SkinNumber(ntr[0], 0, 3, 0, MainState.NUMBER_GROOVEGAUGE), 0, 600, 60, 24, 24, 0, 255, 255, 255,
				255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		addNumber(new SkinNumber(ntr[0], 0, 1, 0, MainState.NUMBER_GROOVEGAUGE_AFTERDOT), 0, 672, 60, 18, 18, 0, 255,
				255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		// progress
		SkinImage progress = new SkinImage(new TextureRegion[] { new TextureRegion(st, 10, 10, 10, 251) }, 0);
		setDestination(progress, 0, 1075, 140, 12, 540, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		add(progress);
		SkinSlider pseek = new SkinSlider(new TextureRegion[] { new TextureRegion(st, 0, 289, 14, 20) }, 0, 2,
				(int) (520 * dh), MainState.SLIDER_MUSIC_PROGRESS);
		setDestination(pseek, 0, 1073, 660, 16, 20, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, 0, 0, 0, 0);
		add(pseek);
		pseek = new SkinSlider(new TextureRegion[] { new TextureRegion(st, 15, 289, 14, 20) }, 0, 2,
				(int) (520 * dh), MainState.SLIDER_MUSIC_PROGRESS);
		setDestination(pseek, 0, 1073, 660, 16, 20, 0, 255, 255, 255, 255, 2, 0, 0, 0, 0, MainState.TIMER_ENDOFNOTE_1P, 0, 0, 0);
		add(pseek);

	}

	public Sprite[] getNote() {
		return note;
	}

	public void setNote(Sprite[] note) {
		this.note = note;
	}

	public Sprite[][] getLongnote() {
		return longnote;
	}

	public void setLongnote(Sprite[][] longnote) {
		this.longnote = longnote;
	}

	public Sprite[] getMinenote() {
		return minenote;
	}

	public void setMinenote(Sprite[] mine) {
		this.minenote = mine;
	}

	public Sprite[] getGauge() {
		return gauge;
	}

	public void setGauge(Sprite[] gauge) {
		this.gauge = gauge;
	}

	public Rectangle getGaugeRegion() {
		return gaugeregion;
	}

	public void setGaugeRegion(Rectangle r) {
		gaugeregion = r;
	}

	public Rectangle[] getLaneregion() {
		return laneregion;
	}

	public Rectangle[] getLaneGroupRegion() {
		return lanegroupregion;
	}

	public void setLaneGroupRegion(Rectangle[] r) {
		lanegroupregion = r;
	}

	public void setLaneregion(Rectangle[] laneregion) {
		this.laneregion = laneregion;
	}

	public void setJudgeregion(JudgeRegion[] jr) {
		judgeregion = jr;
	}

	public JudgeRegion[] getJudgeregion() {
		return judgeregion;
	}

	private Rectangle rect(float x, float y, float width, float height) {
		return new Rectangle(x * dw, y * dh, width * dw, height * dh);
	}

	public int getCloseTime() {
		return close;
	}

	public void setCloseTime(int close) {
		this.close = close;
	}

	public int getPlayStartTime() {
		return playstart;
	}

	public void setPlaystartTime(int playstart) {
		this.playstart = playstart;
	}

	public SkinImage[] getLine() {
		return line;
	}

	public void setLine(SkinImage[] line) {
		this.line = line;
	}

	public static class JudgeRegion {

		public SkinImage[] judge;
		public SkinNumber[] count;
		public boolean shift;

		public JudgeRegion(SkinImage[] judge, SkinNumber[] count, boolean shift) {
			this.judge = judge;
			this.count = count;
			this.shift = shift;
		}
	}

	public void setBMSPlayer(BMSPlayer player) {
		this.player = player;
	}

	public static class SkinLaneObject extends SkinObject {

		private PlaySkin skin;

		public SkinLaneObject(PlaySkin skin) {
			this.skin = skin;
		}

		@Override
		public void draw(SpriteBatch sprite, long time, MainState state) {
			if (skin.player.getLanerender() != null) {
				skin.player.getLanerender().drawLane();
			}
		}

		@Override
		public void dispose() {

		}
	}

	public static class SkinGaugeObject extends SkinObject {

		private PlaySkin skin;

		private Texture backtex;

		public SkinGaugeObject(PlaySkin skin) {
			this.skin = skin;
			Pixmap back = new Pixmap(1,1, Pixmap.Format.RGBA8888);
			back.setColor(0,0,0,0.7f);
			back.fill();
			backtex = new Texture(back);
		}

		@Override
		public void draw(SpriteBatch sprite, long time, MainState state) {
			if (skin.player.getGauge() != null) {
				Rectangle gr = skin.getGaugeRegion();
				sprite.end();
				skin.player.getGauge().draw(skin, sprite, gr.x, gr.y, gr.width, gr.height);
				sprite.begin();
			}
		}

		@Override
		public void dispose() {

		}
	}

	public static class SkinBGAObject extends SkinObject {

		private PlaySkin skin;

		public SkinBGAObject(PlaySkin skin) {
			this.skin = skin;
		}

		@Override
		public void draw(SpriteBatch sprite, long time, MainState state) {
			if (skin.player.getMainController().getPlayerResource().getBGAManager() != null) {
				BMSPlayer player = (BMSPlayer) state;
				skin.player.getMainController().getPlayerResource().getBGAManager().drawBGA(sprite, getDestination(time, state), player.getState() == BMSPlayer.STATE_PRELOAD || player.getState() == BMSPlayer.STATE_READY ? -1 : (int)(player.getNowTime() - player.getTimer()[MainState.TIMER_PLAY]));
			}
		}

		@Override
		public void dispose() {

		}
	}

}
