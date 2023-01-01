package bms.player.beatoraja.play;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.PlayerInformation;
import bms.player.beatoraja.ScoreData;
import bms.player.beatoraja.ir.RankingData;
import bms.player.beatoraja.select.ScoreDataCache;

import java.util.*;

/**
 * スコアターゲット
 * 
 * @author exch
 */
public abstract class TargetProperty {

	/**
	 * Target ID
	 */
	public final String id;
	
	/**
	 * Target名称
	 */
    private String name;

    /**
     * ターゲットスコア
     */
    protected final ScoreData targetScore = new ScoreData();
    
    private static String[] targets = new String[0];
    private static String[] targetNames =  new String[0];
    
    public TargetProperty(String id) {
    	this.id = id;
    }
    
    public TargetProperty(String id, String name) {
    	this.id = id;
    	this.name = name;
    }
    
    public static String[] getTargets() {
        return targets;
    }
    
    public static String getTargetName(String target) {
    	for(int i = 0;i <targets.length;i++) {
    		if(targets[i].equals(target)) {
    			return targetNames[i];
    		}
    	}
        return "";
    }
    
    public static void setTargets(String[] s) {
    	if(s != null) {
    		targets = s;
    	}
    	targetNames = new String[targets.length];
    	for(int i = 0;i < targets.length;i++) {
    		TargetProperty target = getTargetProperty(targets[i]);
    		targetNames[i] = target != null ? target.getName() : "";
    	}
    }
    
    public static TargetProperty getTargetProperty(String id) {
    	TargetProperty target = StaticTargetProperty.getTargetProperty(id);
    	if(target == null) {
    		target = RivalTargetProperty.getTargetProperty(id);
    	}
    	if(target == null) {
    		target = InternetRankingTargetProperty.getTargetProperty(id);
    	}
    	if(target == null && "RANK_NEXT".equals(id)) {
    		target = new NextRankTargetProperty();
    	}
    	if(target == null) {
    		target = StaticTargetProperty.getTargetProperty("MAX");
    	}
    	return target;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract ScoreData getTarget(MainController main);    
}

/**
 * ターゲット:固定レート
 *
 * @author exch
 */
class StaticTargetProperty extends TargetProperty {

	/**
	 * スコアレート(0%-100%)
	 */
    private float rate;

    public StaticTargetProperty(String id, String name, float rate) {
    	super(id, name);
        this.rate = rate;
    }

    @Override
    public ScoreData getTarget(MainController main) {
    	int rivalscore = (int) (main.getPlayerResource().getBMSModel().getTotalNotes() * 2 * rate / 100f);
		targetScore.setPlayer(getName());
		targetScore.setEpg(rivalscore / 2);
		targetScore.setEgr(rivalscore % 2);
        return targetScore;
    }
    
    public static TargetProperty getTargetProperty(String id) {
    	switch(id) {
    	case "RATE_A-":
    		return new StaticTargetProperty("RATE_A-", "RANK A-",   100.0f * 18.0f / 27.0f);
    	case "RATE_A":
    		return new StaticTargetProperty("RATE_A", "RANK A",   100.0f * 19.0f / 27.0f);
    	case "RATE_A+":
    		return new StaticTargetProperty("RATE_A+", "RANK A+",   100.0f * 20.0f / 27.0f);
    	case "RATE_AA-":
    		return new StaticTargetProperty("RATE_AA-", "RANK AA-",   100.0f * 21.0f / 27.0f);
    	case "RATE_AA":
    		return new StaticTargetProperty("RATE_AA", "RANK AA",   100.0f * 22.0f / 27.0f);
    	case "RATE_AA+":
    		return new StaticTargetProperty("RATE_AA+", "RANK AA+",   100.0f * 23.0f / 27.0f);
    	case "RATE_AAA-":
    		return new StaticTargetProperty("RATE_AAA-", "RANK AAA-",   100.0f * 24.0f / 27.0f);
    	case "RATE_AAA":
    		return new StaticTargetProperty("RATE_AAA", "RANK AAA",   100.0f * 25.0f / 27.0f);
    	case "RATE_AAA+":
    		return new StaticTargetProperty("RATE_AAA+", "RANK AAA+",   100.0f * 26.0f / 27.0f);
    	case "MAX":
    		return new StaticTargetProperty(id, "MAX", 100.0f);
    	}

		if(id.startsWith("RATE_")) {
			try {
				float index = Float.parseFloat(id.substring(5));
				if(index >= 0f && index <= 100f) {
					return new StaticTargetProperty("RATE_" + index, "SCORE RATE " + index + "%",   index);
				}
			} catch (NumberFormatException e) {

			}
		}
		return null;
	}
}

/**
 * ターゲット:ライバル
 *
 * @author exch
 */
class RivalTargetProperty extends TargetProperty {

    private int index;

    public RivalTargetProperty(int index) {
    	super("RIVAL_" + (index + 1), "RIVAL No." + (index + 1));
        this.index = index;
    }

    @Override
    public ScoreData getTarget(MainController main) {
    	PlayerInformation[] info = main.getRivalDataAccessor().getRivals();
    	ScoreDataCache[] cache = main.getRivalDataAccessor().getRivalScoreDataCaches();
    	if(index < info.length) {
    		targetScore.setPlayer(info[index].getName());
    		ScoreData score = cache[index].readScoreData(main.getPlayerResource().getSongdata(), main.getPlayerConfig().getLnmode());
    		if(score != null) {
        		targetScore.setPlayer(info[index].getName());    			
        		targetScore.setEpg(score.getEpg());
        		targetScore.setLpg(score.getLpg());
        		targetScore.setEgr(score.getEgr());
        		targetScore.setLgr(score.getLgr());
    		} else {
        		targetScore.setPlayer("NO DATA");    			
    		}
    	} else {
    		targetScore.setPlayer("NO RIVAL");    		
    	}
        return targetScore;
    }
    
    public static TargetProperty getTargetProperty(String id) {
    	if(id.startsWith("RIVAL_")) {
    		try {
        		int index = Integer.parseInt(id.substring(6));
        		if(index > 0) {
        			return new RivalTargetProperty(index - 1);
        		}
    		} catch (NumberFormatException e) {
    			
    		}
    	}
    	return null;
    }
}

class NextRankTargetProperty extends TargetProperty {

    public NextRankTargetProperty() {
        super("RANK_NEXT", "NEXT RANK");
    }

    @Override
    public ScoreData getTarget(MainController main) {
        ScoreData now = main.getPlayDataAccessor().readScoreData(main.getPlayerResource().getBMSModel()
                , main.getPlayerConfig().getLnmode());
        final int nowscore = now != null ? now.getExscore() : 0;
        final int max = main.getPlayerResource().getBMSModel().getTotalNotes() * 2;
        int targetscore = max;
        for(int i = 15;i < 27;i++) {
            int target = max * i / 27;
            if(nowscore < target) {
            	targetscore = target;
            	break;
            }
        }
		targetScore.setPlayer(getName());
		targetScore.setEpg(targetscore / 2);
		targetScore.setEgr(targetscore % 2);
        return targetScore;

    }
}

/**
 * ターゲット:IR
 *
 * @author exch
 */
class InternetRankingTargetProperty extends TargetProperty {

    private Target target;
    
    private int value;
    
    private InternetRankingTargetProperty(Target target, int value) {
    	super("IR_" + target.name() + "_" + value, getTargetName(target, value));
        this.target = target;
        this.value = value;
    }

    @Override
    public ScoreData getTarget(MainController main) {
    	final RankingData ranking = main.getPlayerResource().getRankingData();
    	if(ranking == null) {
			targetScore.setPlayer("NO DATA");
			return targetScore;    		
    	}
    	
    	if(ranking.getState() == RankingData.FINISH) {
    		if(ranking.getTotalPlayer() > 0) {
    			int index = getTargetRank(main, ranking);
    			int targetscore = ranking.getScore(index).getExscore();
    			targetScore.setPlayer(ranking.getScore(index).player.length() > 0 ? ranking.getScore(index).player : "YOU");
        		targetScore.setEpg(targetscore / 2);
        		targetScore.setEgr(targetscore % 2);
    		} else {
    			targetScore.setPlayer("NO DATA");    			
    		}
    		return targetScore;
    	}
		Thread irprocess = new Thread(() -> {
    		ranking.load(main.getCurrentState(), main.getPlayerResource().getSongdata());
			while(ranking.getState() == RankingData.NONE  || ranking.getState() == RankingData.ACCESS) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
	    	if(ranking.getState() == RankingData.FINISH) {
	    		if(ranking.getTotalPlayer() > 0) {
	    			int index = getTargetRank(main, ranking);
	    			int targetscore = ranking.getScore(index).getExscore();
	    			targetScore.setPlayer(ranking.getScore(index).player.length() > 0 ? ranking.getScore(index).player : "YOU");
	        		targetScore.setEpg(targetscore / 2);
	        		targetScore.setEgr(targetscore % 2);
	    		} else {
	    			targetScore.setPlayer("NO DATA");    			
	    		}
	    		
				main.getCurrentState().getScoreDataProperty().updateTargetScore(targetScore.getExscore());	    		
	    	}
		});
		irprocess.start();
        return targetScore;
    }
    
    private int getTargetRank(MainController main, RankingData ranking) {
        ScoreData now = main.getPlayDataAccessor().readScoreData(main.getPlayerResource().getBMSModel()
                , main.getPlayerConfig().getLnmode());
        final int nowscore = now != null ? now.getExscore() : 0;
		switch(target) {
		case NEXT:
			// n位上のプレイヤー
			for(int i = 0;i  < ranking.getTotalPlayer(); i++) {
				if(ranking.getScore(i).getExscore() <= nowscore) { 
					return Math.max(i - value , 0);
				}
			}
			return 0;
			
		case RANK:
			// n位のプレイヤー
			return Math.min(ranking.getTotalPlayer(), value) - 1;
		case RANKRATE:
			// 上位n％のプレイヤー
			return ranking.getTotalPlayer() * value / 100;
		}
    	return 0;
    }
    
    public static TargetProperty getTargetProperty(String id) {
    	if(id.startsWith("IR_NEXT_")) {
    		try {
        		int index = Integer.parseInt(id.substring(8));
        		if(index > 0) {
        			return new InternetRankingTargetProperty(Target.NEXT, index);
        		}
    		} catch (NumberFormatException e) {
    			
    		}
    	}
    	if(id.startsWith("IR_RANK_")) {
    		try {
        		int index = Integer.parseInt(id.substring(8));
        		if(index > 0) {
        			return new InternetRankingTargetProperty(Target.RANK, index);
        		}
    		} catch (NumberFormatException e) {
    			
    		}
    	}
    	if(id.startsWith("IR_RANKRATE_")) {
    		try {
        		int index = Integer.parseInt(id.substring(12));
        		if(index > 0 && index < 100) {
        			return new InternetRankingTargetProperty(Target.RANKRATE, index);
        		}
    		} catch (NumberFormatException e) {
    			
    		}
    	}
    	return null;
    }
    
    public static String getTargetName(Target target, int value) {
    	switch(target) {
    	case NEXT:
    		return "IR NEXT " + value + "RANK";
    	case RANK:
    		return "IR RANK " + value;
    	case RANKRATE:
    		return "IR RANK TOP " + value + "%";
    	}
    	return "";
    }
    
    enum Target {
    	NEXT, RANK, RANKRATE
    }
}

