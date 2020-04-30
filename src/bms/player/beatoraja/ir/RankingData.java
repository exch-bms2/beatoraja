package bms.player.beatoraja.ir;

import java.util.Arrays;
import java.util.logging.Logger;

import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.MainController.IRStatus;
import bms.player.beatoraja.song.SongData;

/**
 * IRのランキングデータ
 *
 * @author exch
 */
public class RankingData {
	/**
	 * 選択されている楽曲の現在のIR順位
	 */
	private int irrank;
	/**
	 * 選択されている楽曲の以前のIR順位
	 */
	private int prevrank;
	/**
	 * 選択されている楽曲のローカルスコアでの想定IR順位
	 */	
	private int localrank;

	/**
	 * IR総プレイ数
	 */
	private int irtotal;
	/**
	 * 各クリアランプ総数
	 */
	private int[] lamps = new int[11];
	/**
	 * 全スコアデータ
	 */
	private IRScoreData[] scores;
	
	/**
	 * IRアクセス状態
	 */
	private int state = NONE;
	public static final int NONE = 0;
	public static final int ACCESS = 1;
	public static final int FINISH = 2;
	public static final int FAIL = 3;
	
	/**
	 * 最終更新時間
	 */
	private long lastUpdateTime;
	
	public void load(MainState mainstate, SongData song) {
		state = NONE;
		Thread irprocess = new Thread(() -> {
			state = ACCESS;
			final IRStatus[] ir = mainstate.main.getIRStatus();
	        IRResponse<IRScoreData[]> response = ir[0].connection.getPlayData(null, song);
	        if(response.isSucceeded()) {
	        	updateScore(response.getData(), mainstate.getScoreDataProperty().getScoreData());
	        	
	            Logger.getGlobal().warning("IRからのスコア取得成功 : " + response.getMessage());
	        } else {
	            Logger.getGlobal().warning("IRからのスコア取得失敗 : " + response.getMessage());
				state = FAIL;
		        lastUpdateTime = System.currentTimeMillis();
	        }
		});
		irprocess.start();

	}
	
	public void updateScore(IRScoreData[] scores, IRScoreData localscore) {
		if(scores == null) {
			return;
		}
		boolean firstUpdate = this.scores == null;
		
		if(!firstUpdate) {
			prevrank = irrank;	
		}
		this.scores = scores;
        irtotal = scores.length;
        Arrays.fill(lamps, 0);
        irrank = 0;
        localrank = 0;
        for(int i = 0;i < scores.length;i++) {
            if(irrank == 0 && scores[i].getPlayer().length() == 0) {
            	irrank = i + 1;
            }
            if(localscore != null && localrank == 0 && scores[i].getExscore() <=  localscore.getExscore()) {
            	localrank = i + 1;
            }
            lamps[scores[i].getClear()]++;
        }
        
        if(firstUpdate && localrank != 0) {
        	prevrank = Math.max(irrank, localrank);
        }
        
		state = FINISH;
        lastUpdateTime = System.currentTimeMillis();
	}
	
	public int getRank() {
		return irrank;
	}
	
	public int getPreviousRank() {
		return prevrank;
	}
	
	public int getLocalRank() {
		return localrank;
	}
	
	public int getTotalPlayer() {
		return irtotal;
	}

	public IRScoreData[] getScores() {
		return scores;
	}
	
	public int getClearCount(int clearType) {
		return lamps[clearType];
	}
	
	public int getState() {
		return state;
	}
	
	public long getLastUpdateTime() {
		return lastUpdateTime;
	}
}