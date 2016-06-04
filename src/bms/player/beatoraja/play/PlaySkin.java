package bms.player.beatoraja.play;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bms.player.beatoraja.skin.NumberResourceAccessor;
import bms.player.beatoraja.skin.Skin;
import bms.player.beatoraja.skin.SkinImage;
import bms.player.beatoraja.skin.SkinNumber;

import bms.player.beatoraja.skin.SkinText;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * プレイスキン
 * 
 * @author exch
 */
public class PlaySkin extends Skin {

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
	/**
	 * ボム画像
	 */
	private Animation[] bomb;
	/**
	 * キービーム画像
	 */
	private Sprite[] keybeam = new Sprite[0];
	/**
	 * レーンカバー画像
	 */
	private Sprite lanecover;

	private Texture background;

	private Sprite[] gauge;

	private Sprite[] judge;

	private Sprite[][] judgenum;

	/**
	 * レーン描画エリア
	 */
	private Rectangle[] laneregion;

	private Rectangle[] lanegroupregion;

	private Rectangle bgaregion;

	private Rectangle gaugeregion;

	private Rectangle[] judgeregion;

	private Rectangle judgecountregion;

	private Rectangle graphregion;

	private Rectangle progressregion;

	private Sprite slider;

	private final NumberResourceAccessor[] judgecount = { NumberResourceAccessor.FAST_PERFECT,
			NumberResourceAccessor.SLOW_PERFECT, NumberResourceAccessor.FAST_GREAT, NumberResourceAccessor.SLOW_GREAT,
			NumberResourceAccessor.FAST_GOOD, NumberResourceAccessor.SLOW_GOOD, NumberResourceAccessor.FAST_BAD,
			NumberResourceAccessor.SLOW_BAD, NumberResourceAccessor.FAST_POOR, NumberResourceAccessor.SLOW_POOR,
			NumberResourceAccessor.FAST_MISS, NumberResourceAccessor.SLOW_MISS };

	public PlaySkin(int mode) {
		List<SkinNumber> numbers = new ArrayList();
		makeCommonSkin(numbers);
		if (mode == 5 || mode == 7) {
			make7KeySkin(numbers);
		} else if (mode == 10 || mode == 14) {
			make14KeySkin(numbers);
		} else {
			make9KeySkin(numbers);
		}
		this.setSkinNumbers(numbers.toArray(new SkinNumber[0]));
	}

	private void makeCommonSkin(List<SkinNumber> numbers) {
		// ボムのスプライト作成
		Texture bombt = new Texture("skin/bomb.png");
		TextureRegion[][] bombtr = TextureRegion.split(bombt, 181, 191);
		bomb = new Animation[bombtr.length];
		for (int i = 0; i < bombtr.length; i++) {
			bomb[i] = new Animation(1 / 60f, bombtr[i]);
		}
		bomb[0].setPlayMode(Animation.PlayMode.NORMAL);
		bomb[1].setPlayMode(Animation.PlayMode.LOOP);
		bomb[2].setPlayMode(Animation.PlayMode.LOOP);
		bomb[3].setPlayMode(Animation.PlayMode.LOOP);
		// ゲージ
		Texture gt = new Texture("skin/gauge.png");
		gauge = new Sprite[4];
		gauge[0] = new Sprite(gt, 5, 0, 5, 17);
		gauge[1] = new Sprite(gt, 0, 0, 5, 17);
		gauge[2] = new Sprite(gt, 5, 17, 5, 17);
		gauge[3] = new Sprite(gt, 0, 17, 5, 17);
		// 判定文字
		Texture jt = new Texture("skin/judge.png");
		judge = new Sprite[5];
		judge[0] = new Sprite(jt, 0, 0, 115, 52);
		judge[1] = new Sprite(jt, 0, 52 * 3, 115, 52);
		judge[2] = new Sprite(jt, 25 * 16, 0, 25 * 5, 52);
		judge[3] = new Sprite(jt, 25 * 22, 0, 25 * 4, 52);
		judge[4] = new Sprite(jt, 25 * 26, 0, 25 * 4, 52);
		judgenum = new Sprite[3][10];
		for (int j = 0; j < 2; j++) {
			for (int i = 0; i < 10; i++) {
				judgenum[j][i] = new Sprite(jt, 28 * i + 115, j * 52 * 3, 28, 52);
			}
			for (int i = 0; i < 10; i++) {
				judgenum[2][i] = new Sprite(jt, 28 * i + 115, 52 * 3, 28, 52);
			}
		}
		// 数字
		Texture nt = new Texture("skin/number.png");
		TextureRegion[][] ntr = TextureRegion.split(nt, 24, 24);

		Texture lct = new Texture("skin/lanecover.png");
		lanecover = new Sprite(lct, 0, 0, 390, 580);
		// bpm
		SkinNumber[] bpm = new SkinNumber[3];
		bpm[0] = new SkinNumber(ntr[0], 0, 4, 0, NumberResourceAccessor.MIN_BPM);
		bpm[0].setDestination(0, 520, 2, 18, 18, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		bpm[1] = new SkinNumber(ntr[0], 0, 4, 0, NumberResourceAccessor.NOW_BPM);
		bpm[1].setDestination(0, 592, 2, 24, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		bpm[2] = new SkinNumber(ntr[0], 0, 4, 0, NumberResourceAccessor.MAX_BPM);
		bpm[2].setDestination(0, 688, 2, 18, 18, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		numbers.addAll(Arrays.asList(bpm));
		// 残り時間
		SkinNumber[] timecount = new SkinNumber[2];
		timecount[0] = new SkinNumber(ntr[0], 0, 2, 1, NumberResourceAccessor.TIMELEFT_MINUTE);
		timecount[0].setDestination(0, 1148, 2, 24, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		timecount[1] = new SkinNumber(ntr[0], 0, 2, 1, NumberResourceAccessor.TIMELEFT_SECOND);
		timecount[1].setDestination(0, 1220, 2, 24, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		numbers.addAll(Arrays.asList(timecount));

		SkinNumber[] hispeed = new SkinNumber[2];
		hispeed[0] = new SkinNumber(ntr[0], 0, 2, 0, NumberResourceAccessor.HISPEED);
		hispeed[0].setDestination(0, 116, 2, 12, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		hispeed[1] = new SkinNumber(ntr[0], 0, 2, 1, NumberResourceAccessor.HISPEED_AFTERDOT);
		hispeed[1].setDestination(0, 154, 2, 10, 20, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		numbers.addAll(Arrays.asList(hispeed));

		SkinNumber duration = new SkinNumber(ntr[0], 0, 4, 0, NumberResourceAccessor.DURATION);
		duration.setDestination(0, 318, 2, 12, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		numbers.add(duration);

		Texture bg = new Texture("skin/playbg.png");
		SkinImage[] images = new SkinImage[1];
		images[0] = new SkinImage();
		images[0].setImage(new TextureRegion[] { new TextureRegion(bg) }, 0);
		images[0].setDestination(0, 0, 0, 1280, 720, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.setSkinPart(images);
	}

	private void make7KeySkin(List<SkinNumber> numbers) {
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

		Texture kbt = new Texture("skin/keybeam.png");
		keybeam = new Sprite[8];
		keybeam[0] = keybeam[2] = keybeam[4] = keybeam[6] = new Sprite(kbt, 75, 0, 21, 255);
		keybeam[1] = keybeam[3] = keybeam[5] = new Sprite(kbt, 47, 0, 28, 255);
		keybeam[7] = new Sprite(kbt, 0, 0, 47, 255);

		judgeregion = new Rectangle[] { new Rectangle(20, 240, 390, 20) };

		bgaregion = new Rectangle(500, 50, 740, 650);

		SkinText title = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24);
		title.setDestination(0, 502, 698, 18, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		title.setDestination(1000, 502, 698, 18, 18, 0, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		title.setDestination(2000, 502, 698, 18, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.setTitle(title);

		graphregion = new Rectangle(410, 220, 90, 480);

		judgecountregion = new Rectangle(500, 50, 144, 108);
		// judge count
		Texture nt = new Texture("skin/number.png");
		TextureRegion[][] ntr = TextureRegion.split(nt, 24, 24);
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 2; j++) {
				SkinNumber number = new SkinNumber(ntr[j + 1], 0, 4, 2, judgecount[i * 2 + j]);
				number.setDestination(0, 536 + j * 60, 50 + (5 - i) * 18, 12, 18, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0,
						0, 0);
				numbers.add(number);
			}
		}

		laneregion = new Rectangle[8];
		laneregion[0] = new Rectangle(90, 140, 50, 580);
		laneregion[1] = new Rectangle(140, 140, 40, 580);
		laneregion[2] = new Rectangle(180, 140, 50, 580);
		laneregion[3] = new Rectangle(230, 140, 40, 580);
		laneregion[4] = new Rectangle(270, 140, 50, 580);
		laneregion[5] = new Rectangle(320, 140, 40, 580);
		laneregion[6] = new Rectangle(360, 140, 50, 580);
		laneregion[7] = new Rectangle(20, 140, 70, 580);

		lanegroupregion = new Rectangle[] { new Rectangle(20, 140, 390, 580) };

		gaugeregion = new Rectangle(20, 30, 390, 30);
		
		SkinNumber[] gaugecount = new SkinNumber[2];
		gaugecount[0] = new SkinNumber(ntr[0], 0, 3, 0, NumberResourceAccessor.GROOVEGAUGE);
		gaugecount[1] = new SkinNumber(ntr[0], 0, 1, 0, NumberResourceAccessor.GROOVEGAUGE_AFTERDOT);
		gaugecount[0].setDestination(0, 314, 60, 24, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		gaugecount[1].setDestination(0, 386, 60, 18, 18, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		numbers.addAll(Arrays.asList(gaugecount));
		
		progressregion = new Rectangle(4, 140, 12, 540);
	}

	private void make9KeySkin(List<SkinNumber> numbers) {
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
				longnote[3][i] = new Sprite(notet, 0, 38, 36, 1);
				longnote[4][i] = new Sprite(notet, 0, 18, 36, 18);
				longnote[5][i] = new Sprite(notet, 0, 38, 36, 18);
				longnote[6][i] = new Sprite(notet, 0, 38, 36, 1);
				longnote[7][i] = new Sprite(notet, 0, 38, 36, 1);
				longnote[8][i] = new Sprite(notet, 0, 38, 36, 1);
				longnote[9][i] = new Sprite(notet, 0, 38, 36, 1);
				minenote[i] = new Sprite(notet, 0, 18, 36, 18);
			}
			if (i == 1 || i == 7) {
				note[i] = new Sprite(notet, 38, 0, 28, 18);
				longnote[0][i] = new Sprite(notet, 38, 18, 28, 18);
				longnote[1][i] = new Sprite(notet, 38, 38, 28, 18);
				longnote[2][i] = new Sprite(notet, 38, 38, 28, 1);
				longnote[3][i] = new Sprite(notet, 38, 38, 28, 1);
				longnote[4][i] = new Sprite(notet, 38, 18, 28, 18);
				longnote[5][i] = new Sprite(notet, 38, 38, 28, 18);
				longnote[6][i] = new Sprite(notet, 38, 38, 28, 1);
				longnote[7][i] = new Sprite(notet, 38, 38, 28, 1);
				longnote[8][i] = new Sprite(notet, 38, 38, 28, 1);
				longnote[9][i] = new Sprite(notet, 38, 38, 28, 1);
				minenote[i] = new Sprite(notet, 38, 0, 28, 18);
			}
			if (i == 2 || i == 6) {
				note[i] = new Sprite(notet, 68, 0, 36, 18);
				longnote[0][i] = new Sprite(notet, 68, 18, 36, 18);
				longnote[1][i] = new Sprite(notet, 68, 38, 36, 18);
				longnote[2][i] = new Sprite(notet, 68, 38, 36, 1);
				longnote[3][i] = new Sprite(notet, 68, 38, 36, 1);
				longnote[4][i] = new Sprite(notet, 68, 18, 36, 18);
				longnote[5][i] = new Sprite(notet, 68, 38, 36, 18);
				longnote[6][i] = new Sprite(notet, 68, 38, 36, 1);
				longnote[7][i] = new Sprite(notet, 68, 38, 36, 1);
				longnote[8][i] = new Sprite(notet, 68, 38, 36, 1);
				longnote[9][i] = new Sprite(notet, 68, 38, 36, 1);
				minenote[i] = new Sprite(notet, 68, 18, 36, 18);
			}
			if (i == 3 || i == 5) {
				note[i] = new Sprite(notet, 106, 0, 28, 18);
				longnote[0][i] = new Sprite(notet, 106, 18, 28, 18);
				longnote[1][i] = new Sprite(notet, 106, 38, 28, 18);
				longnote[2][i] = new Sprite(notet, 106, 38, 28, 1);
				longnote[3][i] = new Sprite(notet, 106, 38, 28, 1);
				longnote[4][i] = new Sprite(notet, 106, 18, 28, 18);
				longnote[5][i] = new Sprite(notet, 106, 38, 28, 18);
				longnote[6][i] = new Sprite(notet, 106, 38, 28, 1);
				longnote[7][i] = new Sprite(notet, 106, 38, 28, 1);
				longnote[8][i] = new Sprite(notet, 106, 38, 28, 1);
				longnote[9][i] = new Sprite(notet, 106, 38, 28, 1);
				minenote[i] = new Sprite(notet, 106, 0, 28, 18);
			}
			if (i == 4) {
				note[i] = new Sprite(notet, 136, 0, 36, 18);
				longnote[0][i] = new Sprite(notet, 136, 18, 36, 18);
				longnote[1][i] = new Sprite(notet, 136, 38, 36, 18);
				longnote[2][i] = new Sprite(notet, 136, 38, 36, 1);
				longnote[3][i] = new Sprite(notet, 136, 38, 36, 1);
				longnote[4][i] = new Sprite(notet, 136, 18, 36, 18);
				longnote[5][i] = new Sprite(notet, 136, 38, 36, 18);
				longnote[6][i] = new Sprite(notet, 136, 38, 36, 1);
				longnote[7][i] = new Sprite(notet, 136, 38, 36, 1);
				longnote[8][i] = new Sprite(notet, 136, 38, 36, 1);
				longnote[9][i] = new Sprite(notet, 136, 38, 36, 1);
				minenote[i] = new Sprite(notet, 136, 18, 36, 18);
			}
		}

		Texture kbt = new Texture("skin/keybeam.png");
		keybeam = new Sprite[9];
		keybeam[0] = keybeam[2] = keybeam[4] = keybeam[6] = keybeam[8] = new Sprite(kbt, 75, 0, 21, 255);
		keybeam[1] = keybeam[3] = keybeam[5] = keybeam[7] = new Sprite(kbt, 47, 0, 28, 255);

		judgeregion = new Rectangle[] { new Rectangle(300, 240, 680, 20) };
		bgaregion = new Rectangle(10, 390, 330, 330);
		SkinText title = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24);
		title.setDestination(0, 12, 720, 18, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		title.setDestination(1000, 12, 720, 18, 18, 0, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		title.setDestination(2000, 12, 720, 18, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.setTitle(title);

		graphregion = new Rectangle(1090, 220, 90, 480);

		judgecountregion = new Rectangle(1090, 40, 144, 108);
		// judge count
		Texture nt = new Texture("skin/number.png");
		TextureRegion[][] ntr = TextureRegion.split(nt, 24, 24);
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 2; j++) {
				SkinNumber number = new SkinNumber(ntr[j + 1], 0, 4, 2, judgecount[i * 2 + j]);
				number.setDestination(0, 1126 + j * 60, 40 + (5 - i) * 18, 12, 18, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0,
						0, 0);
				numbers.add(number);
			}
		}

		laneregion = new Rectangle[9];
		laneregion[0] = new Rectangle(345, 140, 70, 580);
		laneregion[1] = new Rectangle(415, 140, 60, 580);
		laneregion[2] = new Rectangle(475, 140, 70, 580);
		laneregion[3] = new Rectangle(545, 140, 60, 580);
		laneregion[4] = new Rectangle(605, 140, 70, 580);
		laneregion[5] = new Rectangle(675, 140, 60, 580);
		laneregion[6] = new Rectangle(735, 140, 70, 580);
		laneregion[7] = new Rectangle(805, 140, 60, 580);
		laneregion[8] = new Rectangle(865, 140, 70, 580);

		lanegroupregion = new Rectangle[] { new Rectangle(345, 140, 590, 580) };

		gaugeregion = new Rectangle(345, 30, 590, 30);

		SkinNumber[] gaugecount = new SkinNumber[2];
		gaugecount[0] = new SkinNumber(ntr[0], 0, 3, 0, NumberResourceAccessor.GROOVEGAUGE);
		gaugecount[1] = new SkinNumber(ntr[0], 0, 1, 0, NumberResourceAccessor.GROOVEGAUGE_AFTERDOT);
		gaugecount[0].setDestination(0, 600, 60, 24, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		gaugecount[1].setDestination(0, 672, 60, 18, 18, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		numbers.addAll(Arrays.asList(gaugecount));

		progressregion = new Rectangle(990, 140, 10, 540);
	}

	private void make14KeySkin(List<SkinNumber> numbers) {
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
		keybeam = new Sprite[16];
		keybeam[0] = keybeam[2] = keybeam[4] = keybeam[6] = keybeam[8] = keybeam[10] = keybeam[12] = keybeam[14] = new Sprite(
				kbt, 75, 0, 21, 255);
		keybeam[1] = keybeam[3] = keybeam[5] = keybeam[9] = keybeam[11] = keybeam[13] = new Sprite(kbt, 47, 0, 28, 255);
		keybeam[7] = keybeam[15] = new Sprite(kbt, 0, 0, 47, 255);

		judgeregion = new Rectangle[] { new Rectangle(210, 240, 390, 20), new Rectangle(680, 240, 390, 20) };

		bgaregion = new Rectangle(10, 500, 180, 220);
		SkinText title = new SkinText("skin/VL-Gothic-Regular.ttf", 0, 24);
		title.setDestination(0, 12, 678, 18, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		title.setDestination(1000, 12, 678, 18, 18, 0, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		title.setDestination(2000, 12, 678, 18, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.setTitle(title);

		graphregion = new Rectangle(1090, 220, 90, 480);

		judgecountregion = new Rectangle(1090, 40, 144, 108);
		// judge count
		Texture nt = new Texture("skin/number.png");
		TextureRegion[][] ntr = TextureRegion.split(nt, 24, 24);
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 2; j++) {
				SkinNumber number = new SkinNumber(ntr[j + 1], 0, 4, 2, judgecount[i * 2 + j]);
				number.setDestination(0, 1126 + j * 60, 40 + (5 - i) * 18, 12, 18, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0,
						0, 0);
				numbers.add(number);
			}
		}

		laneregion = new Rectangle[16];
		laneregion[0] = new Rectangle(280, 140, 50, 580);
		laneregion[1] = new Rectangle(330, 140, 40, 580);
		laneregion[2] = new Rectangle(370, 140, 50, 580);
		laneregion[3] = new Rectangle(420, 140, 40, 580);
		laneregion[4] = new Rectangle(460, 140, 50, 580);
		laneregion[5] = new Rectangle(510, 140, 40, 580);
		laneregion[6] = new Rectangle(550, 140, 50, 580);
		laneregion[7] = new Rectangle(210, 140, 70, 580);
		laneregion[8] = new Rectangle(680, 140, 50, 580);
		laneregion[9] = new Rectangle(730, 140, 40, 580);
		laneregion[10] = new Rectangle(770, 140, 50, 580);
		laneregion[11] = new Rectangle(820, 140, 40, 580);
		laneregion[12] = new Rectangle(860, 140, 50, 580);
		laneregion[13] = new Rectangle(910, 140, 40, 580);
		laneregion[14] = new Rectangle(950, 140, 50, 580);
		laneregion[15] = new Rectangle(1000, 140, 70, 580);

		lanegroupregion = new Rectangle[] { new Rectangle(210, 140, 390, 580), new Rectangle(680, 140, 390, 580) };

		gaugeregion = new Rectangle(445, 30, 390, 30);

		SkinNumber[] gaugecount = new SkinNumber[2];
		gaugecount[0] = new SkinNumber(ntr[0], 0, 3, 0, NumberResourceAccessor.GROOVEGAUGE);
		gaugecount[1] = new SkinNumber(ntr[0], 0, 1, 0, NumberResourceAccessor.GROOVEGAUGE_AFTERDOT);
		gaugecount[0].setDestination(0, 600, 60, 24, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		gaugecount[1].setDestination(0, 672, 60, 18, 18, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		numbers.addAll(Arrays.asList(gaugecount));

		progressregion = new Rectangle(1072, 140, 6, 540);
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

	public Animation[] getBomb() {
		return bomb;
	}

	public Sprite[] getKeybeam() {
		return keybeam;
	}

	public Sprite getLanecover() {
		return lanecover;
	}

	public Sprite[] getGauge() {
		return gauge;
	}

	public Sprite[] getJudge() {
		return judge;
	}

	public Sprite[][] getJudgenum() {
		return judgenum;
	}

	public Rectangle getBGAregion() {
		return bgaregion;
	}

	public void setBGAregion(Rectangle r) {
		bgaregion = r;
	}

	public Rectangle getGaugeRegion() {
		return gaugeregion;
	}

	public Rectangle[] getLaneregion() {
		return laneregion;
	}

	public Rectangle[] getLaneGroupRegion() {
		return lanegroupregion;
	}

	public void setLaneregion(Rectangle[] laneregion) {
		this.laneregion = laneregion;
	}

	public Texture getBackground() {
		return background;
	}

	public Rectangle[] getJudgeregion() {
		return judgeregion;
	}

	public void setJudgeregion(Rectangle[] judgeregion) {
		this.judgeregion = judgeregion;
	}

	public Rectangle getJudgecountregion() {
		return judgecountregion;
	}

	public Rectangle getGraphregion() {
		return graphregion;
	}

	public Rectangle getProgressRegion() {
		return progressregion;
	}
}
