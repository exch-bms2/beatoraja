package bms.player.beatoraja;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
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
public class PlaySkin {
	
	/**
	 * ノーツ画像
	 */
	private Sprite[] note = new Sprite[8];
	/**
	 * ロングノーツ画像
	 */
	private Sprite[][] longnote = new Sprite[4][8];
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
	 * レーンカバーは像
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

	private Rectangle bgaregion;
	
	private Rectangle gaugeregion;
	
	private Rectangle judgeregion;

	private Rectangle judgecountregion;

	private Rectangle graphregion;
	
	private Sprite slider;

	private SkinPart[] skinparts = new SkinPart[0];
			
	public PlaySkin() {
		// 背景
//		background = new Texture("skin/bg.jpg");		
		// ノーツ
		Texture notet = new Texture("skin/note.png");
		for(int i = 0;i < 8;i++) {
			if(i % 2 == 0) {
				note[i] = new Sprite(notet, 99, 5, 27, 8);
				longnote[0][i] = new Sprite(notet, 99, 43, 27, 13);
				longnote[1][i] = new Sprite(notet, 99, 57, 27, 13);
				longnote[2][i] = new Sprite(notet, 99, 80, 27, 1);
				longnote[3][i] = new Sprite(notet, 99, 76, 27, 1);
				minenote[i] = new Sprite(notet, 99, 23, 27, 8);
			} else if(i == 7) {
				note[i] = new Sprite(notet, 50, 5, 46, 8);				
				longnote[0][i] = new Sprite(notet, 50, 43, 46, 13);
				longnote[1][i] = new Sprite(notet, 50, 57, 46, 13);
				longnote[2][i] = new Sprite(notet, 50, 80, 46, 1);
				longnote[3][i] = new Sprite(notet, 50, 76, 46, 1);
				minenote[i] = new Sprite(notet, 50, 23, 46, 8);
			} else {
				note[i] = new Sprite(notet, 127, 5, 21, 8);
				longnote[0][i] = new Sprite(notet, 127, 43, 21, 13);
				longnote[1][i] = new Sprite(notet, 127, 57, 21, 13);
				longnote[2][i] = new Sprite(notet, 127, 80, 21, 1);
				longnote[3][i] = new Sprite(notet, 127, 76, 21, 1);
				minenote[i] = new Sprite(notet, 127, 23, 21, 8);
			}
		}
		
		// ボムのスプライト作成
		Texture bombt = new Texture("skin/bomb.png");
		TextureRegion[][] bombtr = TextureRegion.split(bombt, 181, 191);
		bomb = new Animation[bombtr.length];
		for(int i = 0;i < bombtr.length;i++) {
			bomb[i] = new Animation(1/60f, bombtr[i]);
		}
		bomb[0].setPlayMode(Animation.PlayMode.NORMAL);		
		bomb[1].setPlayMode(Animation.PlayMode.LOOP);		
		bomb[2].setPlayMode(Animation.PlayMode.LOOP);		
		bomb[3].setPlayMode(Animation.PlayMode.LOOP);		
		
		Texture kbt = new Texture("skin/keybeam.png");
		keybeam = new Sprite[3];
		keybeam[0] = new Sprite(kbt, 75, 0, 21, 255);
		keybeam[1] = new Sprite(kbt, 47, 0, 28, 255);
		keybeam[2] = new Sprite(kbt, 0, 0, 47, 255);

		Texture lct = new Texture("skin/lanecover.png");
		lanecover = new Sprite(lct, 0, 0, 194, 342);
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
		for(int j = 0;j < 2;j++) {
			for(int i = 0;i < 10;i++) {
				judgenum[j][i] = new Sprite(jt, 28 * i + 115 , j * 52 * 3, 28, 52);
			}			
			for(int i = 0;i < 10;i++) {
				judgenum[2][i] = new Sprite(jt, 28 * i + 115 , 52 * 3, 28, 52);
			}			
		}
		judgeregion = new Rectangle(20, 240, 390, 20);
		
		bgaregion = new Rectangle(500, 50, 740, 650);
		
		graphregion = new Rectangle(410, 220, 90, 480);
		
		judgecountregion = new Rectangle(410, 100, 90, 120);
		
		laneregion = new Rectangle[8];
		laneregion[0] = new Rectangle(90, 140, 50, 580);
		laneregion[1] = new Rectangle(140, 140, 40, 580);
		laneregion[2] = new Rectangle(180, 140, 50, 580);
		laneregion[3] = new Rectangle(230, 140, 40, 580);
		laneregion[4] = new Rectangle(270, 140, 50, 580);
		laneregion[5] = new Rectangle(320, 140, 40, 580);
		laneregion[6] = new Rectangle(360, 140, 50, 580);
		laneregion[7] = new Rectangle(20, 140, 70, 580);
		
		gaugeregion = new Rectangle(20, 30, 390, 30);

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

	public void setLaneregion(Rectangle[] laneregion) {
		this.laneregion = laneregion;
	}

	public Texture getBackground() {
		return background;
	}

	public Rectangle getJudgeregion() {
		return judgeregion;
	}

	public void setJudgeregion(Rectangle judgeregion) {
		this.judgeregion = judgeregion;
	}
	
	public Rectangle getJudgecountregion() {
		return judgecountregion;
	}

	public Rectangle getGraphregion() {
		return graphregion;
	}
	
	public SkinPart[] getSkinPart() {
		return skinparts;
	}
	
	public void setSkinPart(SkinPart[] parts) {
		skinparts = parts;
	}
	
	public static class SkinPart {
		
		public TextureRegion image;
		
		public Rectangle dst;
		
		public int timing;
		
		public int[] op = new int[3];
	}
}
