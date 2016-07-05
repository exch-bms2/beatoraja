package bms.player.beatoraja.result;

import java.util.ArrayList;
import java.util.List;

import bms.player.beatoraja.skin.NumberResourceAccessor;
import bms.player.beatoraja.skin.Skin;
import bms.player.beatoraja.skin.SkinImage;
import bms.player.beatoraja.skin.SkinNumber;
import bms.player.beatoraja.skin.SkinObject;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * リサルトスキン
 */
public class MusicResultSkin extends Skin {

	private Rectangle gaugeregion;

	private Rectangle judgeregion;

	private SkinNumber[] fastslow = new SkinNumber[2];
	private SkinNumber[] exscore = new SkinNumber[4];
	private SkinNumber[] misscount = new SkinNumber[4];
	private SkinNumber[] maxcombo = new SkinNumber[4];
	private SkinNumber totalnotes;

	private final NumberResourceAccessor[] judgecount = { NumberResourceAccessor.PERFECT,
			NumberResourceAccessor.FAST_PERFECT, NumberResourceAccessor.SLOW_PERFECT, NumberResourceAccessor.GREAT,
			NumberResourceAccessor.FAST_GREAT, NumberResourceAccessor.SLOW_GREAT, NumberResourceAccessor.GOOD,
			NumberResourceAccessor.FAST_GOOD, NumberResourceAccessor.SLOW_GOOD, NumberResourceAccessor.BAD,
			NumberResourceAccessor.FAST_BAD, NumberResourceAccessor.SLOW_BAD, NumberResourceAccessor.POOR,
			NumberResourceAccessor.FAST_POOR, NumberResourceAccessor.SLOW_POOR, NumberResourceAccessor.MISS,
			NumberResourceAccessor.FAST_MISS, NumberResourceAccessor.SLOW_MISS };

	public SkinNumber getTotalnotes() {
		return totalnotes;
	}

	public void setTotalnotes(SkinNumber totalnotes) {
		this.totalnotes = totalnotes;
	}

	private float dw;
	private float dh;

	public MusicResultSkin() {
		
	}
	
	public MusicResultSkin(Rectangle r) {
		dw = r.width / 1280.0f;
		dh = r.height / 720.0f;
		List<SkinNumber> numbers = new ArrayList<SkinNumber>();
		gaugeregion = new Rectangle(20 * dw, 500 * dh, 400 * dw, 200 * dh);
		judgeregion = new Rectangle(500 * dw, 500 * dh, 700 * dw, 200 * dh);

		Texture bg = new Texture("skin/resultbg.png");
		SkinImage[] images = new SkinImage[1];
		images[0] = new SkinImage();
		images[0].setImage(new TextureRegion[] { new TextureRegion(bg) }, 0);
		setDestination(images[0], 0, 0, 0, 1280, 720, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		this.setSkinPart(images);
		// 数字
		Texture nt = new Texture("skin/number.png");
		TextureRegion[][] ntr = TextureRegion.split(nt, 24, 24);

		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 3; j++) {
				SkinNumber sn = new SkinNumber(ntr[0], 0, 4, 0, judgecount[i * 3 + j]);
				setDestination(sn, 0, 230 + j * 90, 255 - i * 30, 18, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
				numbers.add(sn);
			}
		}

		for (int i = 0; i < 2; i++) {
			SkinNumber sn = new SkinNumber(ntr[0], 0, 4, 0);
			setDestination(sn, 0, 320 + i * 90, 75, 18, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			setJudgeCount(i == 0, sn);
		}

		SkinNumber score = new SkinNumber(ntr[0], 0, 5, 0, NumberResourceAccessor.TARGET_SCORE);
		setDestination(score, 0, 240, 375, 24, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		numbers.add(score);
		SkinNumber nscore = new SkinNumber(ntr[0], 0, 5, 0, NumberResourceAccessor.SCORE);
		setDestination(nscore, 0, 410, 375, 24, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		numbers.add(nscore);

		SkinNumber dscore = new SkinNumber(ntr[1], 0, 5, 0);
		setDestination(dscore, 0, 550, 375, 12, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setScore(2, dscore);
		dscore = new SkinNumber(ntr[2], 0, 5, 0);
		setDestination(dscore, 0, 550, 375, 12, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setScore(3, dscore);

		SkinNumber minbp = new SkinNumber(ntr[0], 0, 5, 0, NumberResourceAccessor.TARGET_MISSCOUNT);
		setDestination(minbp, 0, 240, 345, 24, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		numbers.add(minbp);
		SkinNumber nminbp = new SkinNumber(ntr[0], 0, 5, 0, NumberResourceAccessor.MISSCOUNT);
		setDestination(nminbp, 0, 410, 345, 24, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		numbers.add(nminbp);
		
		SkinNumber dminbp = new SkinNumber(ntr[1], 0, 5, 0);
		setDestination(dminbp, 0, 550, 345, 12, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setMisscount(2, dminbp);
		dminbp = new SkinNumber(ntr[2], 0, 5, 0);
		setDestination(dminbp, 0, 550, 345, 12, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setMisscount(3, dminbp);

		SkinNumber combo = new SkinNumber(ntr[0], 0, 5, 0, NumberResourceAccessor.TARGET_MAXCOMBO);
		setDestination(combo, 0, 240, 315, 24, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		numbers.add(combo);
		SkinNumber ncombo = new SkinNumber(ntr[0], 0, 5, 0, NumberResourceAccessor.MAXCOMBO);
		setDestination(ncombo, 0, 410, 315, 24, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		numbers.add(ncombo);
		
		SkinNumber dcombo = new SkinNumber(ntr[1], 0, 5, 0);
		setDestination(dcombo, 0, 550, 315, 12, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setMaxcombo(2, dcombo);
		dcombo = new SkinNumber(ntr[2], 0, 5, 0);
		setDestination(dcombo, 0, 550, 315, 12, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		setMaxcombo(3, dcombo);

		totalnotes = new SkinNumber(ntr[0], 0, 5, 0);
		setDestination(totalnotes, 0, 360, 486, 12, 12, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		
		this.setSkinNumbers(numbers.toArray(new SkinNumber[0]));
	}

	public Rectangle getGaugeRegion() {
		return gaugeregion;
	}

	public Rectangle getJudgeRegion() {
		return judgeregion;
	}

	public SkinNumber getJudgeCount(boolean fast) {
		return fastslow[(fast ? 0 : 1)];
	}

	public void setJudgeCount(boolean fast, SkinNumber number) {
		fastslow[(fast ? 0 : 1)] = number;
	}

	public SkinNumber getScore(int status) {
		return exscore[status];
	}

	public void setScore(int status, SkinNumber number) {
		exscore[status] = number;
	}

	public SkinNumber getMisscount(int status) {
		return misscount[status];
	}

	public void setMisscount(int status, SkinNumber number) {
		misscount[status] = number;
	}

	public SkinNumber getMaxcombo(int status) {
		return maxcombo[status];
	}

	public void setMaxcombo(int status, SkinNumber number) {
		maxcombo[status] = number;
	}
	
    private void setDestination(SkinObject object, long time, float x, float y, float w, float h, int acc, int a, int r,
			int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3) {
    	object.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center, loop, timer, op1, op2, op3);
    }
}
