package bms.player.beatoraja.result;

import bms.player.beatoraja.skin.Skin;
import bms.player.beatoraja.skin.SkinNumber;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * リサルトスキン
 */
public class MusicResultSkin extends Skin{

    private Rectangle gaugeregion;

    private Rectangle judgeregion;

    public MusicResultSkin() {
        this.setSkinNumbers(new SkinNumber[28]);
        
        gaugeregion = new Rectangle(20, 500, 400, 200);
        judgeregion = new Rectangle(500,500,700,200);        
		// 数字
		Texture nt = new Texture("skin/number.png");
		TextureRegion[][] ntr = TextureRegion.split(nt, 24, 24);

		for(int i =0;i < 6;i++) {
			for(int j = 0;j < 3;j++) {
				SkinNumber sn = new SkinNumber(ntr[j],0,4,0);
				sn.setDestination(0, 230 + j * 90, 260 - i * 30, 18, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0,
						0, 0, 0, 0, 0);
				setJudgeCount(i, j, sn);
			}
		}
		
		for(int i = 0;i < 2;i++) {
			SkinNumber sn = new SkinNumber(ntr[i + 1],0,4,0);
			sn.setDestination(0, 330 + i * 90, 80, 18, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0,
					0, 0, 0, 0, 0);
			setJudgeCount(i == 0, sn);			
		}
		
		SkinNumber score = new SkinNumber(ntr[0],0,5,0);
		score.setDestination(0, 240, 350, 24, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0,
				0, 0, 0, 0, 0);
		setScore(0, score);
		SkinNumber nscore = new SkinNumber(ntr[0],0,5,0);
		nscore.setDestination(0, 410, 350, 24, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0,
				0, 0, 0, 0, 0);
		setScore(1, nscore);
		SkinNumber dscore = new SkinNumber(ntr[1],0,5,0);
		dscore.setDestination(0, 550, 350, 12, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0,
				0, 0, 0, 0, 0);
		setScore(2, dscore);
		dscore = new SkinNumber(ntr[2],0,5,0);
		dscore.setDestination(0, 550, 350, 12, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0,
				0, 0, 0, 0, 0);
		setScore(3, dscore);

		SkinNumber minbp = new SkinNumber(ntr[0],0,5,0);
		minbp.setDestination(0, 240, 320, 24, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0,
				0, 0, 0, 0, 0);
		setMisscount(0, minbp);
		SkinNumber nminbp = new SkinNumber(ntr[0],0,5,0);
		nminbp.setDestination(0, 410, 320, 24, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0,
				0, 0, 0, 0, 0);
		setMisscount(1, nminbp);
		SkinNumber dminbp = new SkinNumber(ntr[1],0,5,0);
		dminbp.setDestination(0, 550, 320, 12, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0,
				0, 0, 0, 0, 0);
		setMisscount(2, dminbp);
		dminbp = new SkinNumber(ntr[2],0,5,0);
		dminbp.setDestination(0, 550, 320, 12, 24, 0, 1, 1, 1, 1, 0, 0, 0, 0,
				0, 0, 0, 0, 0);
		setMisscount(3, dminbp);
    }

    public Rectangle getGaugeRegion() {
        return gaugeregion;
    }

    public Rectangle getJudgeRegion() {
        return judgeregion;
    }

    public SkinNumber getJudgeCount(int judge, int status) {
    	return this.getSkinNumbers()[judge * 3 + status];
    }
    
    public void setJudgeCount(int judge, int status, SkinNumber number) {
    	this.getSkinNumbers()[judge * 3 + status] = number;
    }
    
    public SkinNumber getJudgeCount(boolean fast) {
    	return this.getSkinNumbers()[18 + (fast ? 0 : 1)];
    }
    
    public void setJudgeCount(boolean fast, SkinNumber number) {
    	this.getSkinNumbers()[18 + (fast ? 0 : 1)] = number;
    }
    
    public SkinNumber getScore(int status) {
    	return this.getSkinNumbers()[20 + status];
    }
    
    public void setScore(int status, SkinNumber number) {
    	this.getSkinNumbers()[20 + status] = number;
    }
    
    public SkinNumber getMisscount(int status) {
    	return this.getSkinNumbers()[24 + status];
    }
    
    public void setMisscount(int status, SkinNumber number) {
    	this.getSkinNumbers()[24 + status] = number;
    }    

}
