package bms.player.beatoraja.ir;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import bms.player.beatoraja.*;
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
	 * 各スコアデータの順位
	 */
	private int[] scorerankings;
	/**
	 * 各プレイヤーのタイプ
	 */
	private int[] playertypes;
	public static final int PLAYER_NONE = 0;
	public static final int PLAYER_YOU = 1;
	public static final int PLAYER_RIVAL = 2;

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
	
	public void load(final MainState mainstate, Object song) {
		if(!(song instanceof SongData || song instanceof CourseData)) {
			return;
		}		
		state = ACCESS;
		Thread irprocess = new Thread(() -> {
			final IRStatus[] ir = mainstate.main.getIRStatus();
	        IRResponse<IRScoreData[]> response = null;
	        if(song instanceof SongData) {
	        	 response = ir[0].connection.getPlayData(null, new IRChartData((SongData) song));
	        } else if(song instanceof CourseData) {
		        response = ir[0].connection.getCoursePlayData(null, new IRCourseData((CourseData) song, mainstate.main.getPlayerConfig().getLnmode()));
	        }
	        if(response.isSucceeded()) {
	        	updateScore(ir[0].player, mainstate.main.getRivalDataAccessor(), response.getData(), mainstate.getScoreDataProperty().getScoreData());
	            Logger.getGlobal().fine("IRからのスコア取得成功 : " + response.getMessage());
				state = FINISH;
	        } else {
	            Logger.getGlobal().warning("IRからのスコア取得失敗 : " + response.getMessage());
				state = FAIL;
	        }
	        lastUpdateTime = System.currentTimeMillis();
		});
		irprocess.start();

	}
	
	public void updateScore(IRPlayerData player, RivalDataAccessor rivals, IRScoreData[] scores, ScoreData localscore) {
		if(scores == null) {
			return;
		}
		boolean firstUpdate = this.scores == null;
		
		List<String> rivalid = IntStream.range(0, rivals.getRivalCount()).mapToObj(index -> rivals.getRivalInformation(index).getId()).collect(Collectors.toList());
		
		Arrays.sort(scores, (s1, s2) -> (s2.getExscore() - s1.getExscore()));
		int[] scorerankings = new int[scores.length];
		int[] playertypes = new int[scores.length];
		for(int i = 0;i < scorerankings.length;i++) {
			scorerankings[i] = (i > 0 && scores[i].getExscore() == scores[i - 1].getExscore()) ? scorerankings[i - 1] : i + 1;
		}
		
		if(!firstUpdate) {
			prevrank = irrank;	
		}
		this.scores = scores;
		this.scorerankings = scorerankings;
        irtotal = scores.length;
        Arrays.fill(lamps, 0);
        irrank = 0;
        localrank = 0;
        for(int i = 0;i < scores.length;i++) {
			playertypes[i] = rivalid.contains(scores[i].id) ? PLAYER_RIVAL : PLAYER_NONE;
        	if(Objects.equals(player.id, scores[i].id)) {
            	irrank = scorerankings[i];
    			playertypes[i] = PLAYER_YOU;
        	} else if(irrank == 0 && scores[i].player.length() == 0) {
            	// TODO 旧方式のため後で削除
            	irrank = scorerankings[i];
    			playertypes[i] = PLAYER_YOU;
            }
            if(localscore != null && localrank == 0 && scores[i].getExscore() <=  localscore.getExscore()) {
            	localrank = scorerankings[i];
            }
            lamps[scores[i].clear.id]++;
        }
        
        if(firstUpdate && localrank != 0) {
        	prevrank = Math.max(irrank, localrank);
        }
        
		state = FINISH;
        lastUpdateTime = System.currentTimeMillis();
	}
	
	/**
	 * 選択されている楽曲の現在のIR順位を返す
	 * 
	 * @return 現在のIR順位
	 */
	public int getRank() {
		return irrank;
	}

	/**
	 * 選択されている楽曲の以前のIR順位を返す
	 * 
	 * @return 以前のIR順位
	 */
	public int getPreviousRank() {
		return prevrank;
	}
	
	/**
	 * 選択されている楽曲のローカルスコアでの想定IR順位を返す
	 * 
	 * @return ローカルスコアでの想定IR順位
	 */
	public int getLocalRank() {
		return localrank;
	}
	
	/**
	 * IR上の総プレイ人数を返す
	 * 
	 * @return 総プレイ人数
	 */
	public int getTotalPlayer() {
		return irtotal;
	}

	/**
	 * IR上のindexに対応したプレイヤーのスコアデータを返す

	 * @param index インデックス
	 * @return 対応するスコアデータ。indexに対応したスコアデータが存在しない場合はnull
	 */
	public IRScoreData getScore(int index) {
		if(scores != null && index >= 0 && index < scores.length) {
			return scores[index];			
		}
		return null;
	}
	
	/**
	 * IR上のindexに対応したプレイヤーの順位を返す

	 * @param index インデックス
	 * @return 対応するスコアデータの順位。indexに対応したスコアデータが存在しない場合はInteger.MIN_VALUEl
	 */
	public int getScoreRanking(int index) {
		if(scorerankings != null && index >= 0 && index < scorerankings.length) {
			return scorerankings[index];			
		}
		return Integer.MIN_VALUE;
	}
	
	public int getPlayerType(int index) {
		if(playertypes != null && index >= 0 && index < playertypes.length) {
			return playertypes[index];			
		}
		return Integer.MIN_VALUE;
	}
	
	public int getClearCount(int clearType) {
		return lamps[clearType];
	}
	
	public int getState() {
		return state;
	}
	
	/**
	 * RankingDataの最終更新時間を返す
	 * 
	 * @return RankingDataの最終更新時間(ms)
	 */
	public long getLastUpdateTime() {
		return lastUpdateTime;
	}
}