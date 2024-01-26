package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.play.GrooveGauge.Gauge;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import java.util.Arrays;

/**
 * 判定オブジェクト
 * 
 * @author exch
 */
public final class SkinJudge extends SkinObject {

	/**
	 * 文字イメージ
	 */
    private final SkinImage[] judge = new SkinImage[7];
    /**
     * 数字イメージ
     */
    private final SkinNumber[] count = new SkinNumber[7];
    private final int player;
    private final boolean shift;
    
    private SkinImage nowJudge;
    private SkinNumber nowCount;

    public SkinJudge(int index, boolean shift) {
        this(null, null, index, shift);
    }

    public SkinJudge(SkinImage[] judge, SkinNumber[] count, int player, boolean shift) {
    	if(judge == null) {
    		Arrays.fill(this.judge, null);
    	} else {
        	for(int i = 0; i < this.judge.length && i < judge.length;i++) {
        		this.judge[i] = judge[i];
        	}
    	}
    	if(count == null) {
    		Arrays.fill(this.count, null);
    	} else {
        	for(int i = 0; i < this.count.length && i < count.length;i++) {
        		this.count[i] = count[i];
        	}
    	}
        this.player = player;
        this.shift = shift;
        
        this.setDestination(0, 0, 0, 0, 0, 0, 0, 255, 255, 255, 0, 0, 0, 0, 0, 0, new int[0]);
    }

    public SkinImage getJudge(int index) {
        return  index >= 0 && index < judge.length ? judge[index] : null;
    }

    public void setJudge(int index, SkinImage judge) {
    	if(index >= 0 && index < this.judge.length) {
    		this.judge[index] = judge;
    	}
    }

    public SkinNumber getJudgeCount(int index) {
        return  index >= 0 && index < count.length ? count[index] : null;
    }
    
    public void setJudgeCount(int index, SkinNumber count) {
    	if(index >= 0 && index < this.count.length) {
    		this.count[index] = count;
    	}
    }

    public boolean isShift() {
    	return shift;
    }

	@Override
	public void prepare(long time, MainState state) {
        final int judgenow = ((BMSPlayer)state).getJudgeManager().getNowJudge(player) - 1;
        if(judgenow < 0) {
        	draw = false;
            return;
        }
		super.prepare(time, state);
		
        final Gauge gauge = ((BMSPlayer)state).getGauge().getGauge();
        
        if(judgenow == 0 && gauge.isMax()) {
        	nowJudge = judge[6] != null ? judge[6] : judge[0];
        	nowCount = count[6] != null ? count[6] : count[0];
        } else {
        	nowJudge = judge[judgenow];
        	nowCount = judgenow < 3 ? count[judgenow] : null;        	
        }
        
        if(nowJudge != null) {
        	nowJudge.prepare(time, state);
        } else {
        	draw = false;
        	return;
        }
        
    	if(nowJudge.draw) {
            if(nowCount != null) {
            	nowCount.prepare(time, state, ((BMSPlayer)state).getJudgeManager().getNowCombo(player), nowJudge.region.x, nowJudge.region.y);
            	nowJudge.region.x += shift ? -nowCount.getLength() / 2 : 0;
            }        		
    	} else {
        	draw = false;
        	return;    		
    	}
	}

    @Override
    public void draw(SkinObjectRenderer sprite) {
        if (nowCount != null && nowCount.draw) {
        	nowCount.draw(sprite);
        }
        nowJudge.draw(sprite);
    }

    @Override
    public void dispose() {
    	disposeAll(judge);
    	disposeAll(count);
    }
}
