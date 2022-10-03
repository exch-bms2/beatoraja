package bms.player.beatoraja.play;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.ScoreData;
import bms.player.beatoraja.ir.RankingData;

import java.util.*;

/**
 * Created by exch on 2017/02/28.
 */
public abstract class TargetProperty {

	public final String id;
	
    private String name;

    /**
     * 旧属性値
     */
    private static TargetProperty[] available;

    private static final Map<String, TargetProperty> properties = new HashMap();
    
    public TargetProperty(String id) {
    	this.id = id;
    }
    
    public TargetProperty(String id, String name) {
    	this.id = id;
    	this.name = name;
    }
    
    /**
     * 旧メソッド
     */
    public static TargetProperty[] getAllTargetProperties() {
        if(available == null) {
            List<TargetProperty> targets = new ArrayList<TargetProperty>();
            targets.add(new StaticTargetProperty("RANK_A-", "RANK A-",   100.0f * 18.0f / 27.0f));
            targets.add(new StaticTargetProperty("RANK_A", "RANK A",    100.0f * 19.0f / 27.0f));
            targets.add(new StaticTargetProperty("RANK_A+", "RANK A+",   100.0f * 20.0f / 27.0f));
            targets.add(new StaticTargetProperty("RANK_AA-", "RANK AA-",  100.0f * 21.0f / 27.0f));
            targets.add(new StaticTargetProperty("RANK_AA", "RANK AA",   100.0f * 22.0f / 27.0f));
            targets.add(new StaticTargetProperty("RANK_AA+", "RANK AA+",  100.0f * 23.0f / 27.0f));
            targets.add(new StaticTargetProperty("RANK_AAA-", "RANK AAA-", 100.0f * 24.0f / 27.0f));
            targets.add(new StaticTargetProperty("RANK_AAA", "RANK AAA",  100.0f * 25.0f / 27.0f));
            targets.add(new StaticTargetProperty("RANK_AAA+", "RANK AAA+", 100.0f * 26.0f / 27.0f));
            targets.add(new StaticTargetProperty("MAX", "MAX", 100.0f));
            targets.add(new NextRankTargetProperty());
            available = targets.toArray(new TargetProperty[targets.size()]);
        }
        return available;
    }

    public static TargetProperty[] getTargetProperties() {
    	putTargetProperty(new StaticTargetProperty("RATE_A-", "RANK A-",   100.0f * 18.0f / 27.0f));
    	putTargetProperty(new StaticTargetProperty("RATE_A", "RANK A",    100.0f * 19.0f / 27.0f));
    	putTargetProperty(new StaticTargetProperty("RATE_A+", "RANK A+",   100.0f * 20.0f / 27.0f));
    	putTargetProperty(new StaticTargetProperty("RATE_AA-", "RANK AA-",  100.0f * 21.0f / 27.0f));
    	putTargetProperty(new StaticTargetProperty("RATE_AA", "RANK AA",   100.0f * 22.0f / 27.0f));
    	putTargetProperty(new StaticTargetProperty("RATE_AA+", "RANK AA+",  100.0f * 23.0f / 27.0f));
    	putTargetProperty(new StaticTargetProperty("RATE_AAA-", "RANK AAA-", 100.0f * 24.0f / 27.0f));
    	putTargetProperty(new StaticTargetProperty("RATE_AAA", "RANK AAA",  100.0f * 25.0f / 27.0f));
    	putTargetProperty(new StaticTargetProperty("RATE_AAA+", "RANK AAA+", 100.0f * 26.0f / 27.0f));
    	putTargetProperty(new StaticTargetProperty("MAX", "MAX", 100.0f));
    	putTargetProperty(new NextRankTargetProperty());
    	putTargetProperty(InternetRankingTargetProperty.getTargetProperty("IR_NEXT_1"));
    	putTargetProperty(InternetRankingTargetProperty.getTargetProperty("IR_NEXT_5"));
    	putTargetProperty(InternetRankingTargetProperty.getTargetProperty("IR_RANK_1"));
    	putTargetProperty(InternetRankingTargetProperty.getTargetProperty("IR_RANK_10"));
    	putTargetProperty(InternetRankingTargetProperty.getTargetProperty("IR_RANKRATE_10"));
    	putTargetProperty(InternetRankingTargetProperty.getTargetProperty("IR_RANKRATE_25"));
    	putTargetProperty(InternetRankingTargetProperty.getTargetProperty("IR_RANKRATE_50"));
        return properties.values().toArray(new TargetProperty[properties.size()]);
    }
    
    private static void putTargetProperty(TargetProperty target) {
    	if(!properties.containsKey(target.id)) {
    		properties.put(target.id, target);
    	}
    }
    
    public static TargetProperty getTargetProperty(String id) {
    	TargetProperty target = properties.get(id);
    	if(target == null) {
    		target = InternetRankingTargetProperty.getTargetProperty(id);
    	}
    	if(target == null) {
    		target = getTargetProperty("MAX");
    	}
    	return target;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract int getTarget(MainController main);
}

class StaticTargetProperty extends TargetProperty{

    private float rate;

    public StaticTargetProperty(String id, String name, float rate) {
    	super(id, name);
        this.rate = rate;
    }

    @Override
    public int getTarget(MainController main) {
        return (int) (main.getPlayerResource().getBMSModel().getTotalNotes() * 2 * rate / 100f);
    }
}

class RivalTargetProperty extends TargetProperty{

    private String rivalid;

    public RivalTargetProperty(String rivalid) {
    	super("RIVAL_" + rivalid);
        this.rivalid = rivalid;
    }

    @Override
    public int getTarget(MainController main) {
        // TODO 指定プレイヤーのスコアデータ取得
        return 0;
    }
}

class NextRankTargetProperty extends TargetProperty{

    public NextRankTargetProperty() {
        super("RANK_NEXT", "NEXT RANK");
    }

    @Override
    public int getTarget(MainController main) {
        ScoreData now = main.getPlayDataAccessor().readScoreData(main.getPlayerResource().getBMSModel()
                , main.getPlayerConfig().getLnmode());
        final int nowscore = now != null ? now.getExscore() : 0;
        final int max = main.getPlayerResource().getBMSModel().getTotalNotes() * 2;
        for(int i = 15;i < 27;i++) {
            int target = max * i / 27;
            if(nowscore < target) {
                return target;
            }
        }
        return max;
    }
}

class InternetRankingTargetProperty extends TargetProperty{

    private Target target;
    
    private int value;
    
    private InternetRankingTargetProperty(Target target, int value) {
    	super("IR_" + target.name(), "IR " + target.name() + " " + value);
        this.target = target;
        this.value = value;
    }

    @Override
    public int getTarget(MainController main) {
    	final RankingData ranking = main.getPlayerResource().getRankingData();
    	if(ranking.getState() == RankingData.FINISH) {
    		return getTargetScore(main, ranking);
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
	    		System.out.println(getTargetScore(main, ranking));
				main.getCurrentState().getScoreDataProperty().updateTargetScore(getTargetScore(main, ranking));	    		
	    	}			
		});
		irprocess.start();
        return 0;
    }
    
    private int getTargetScore(MainController main, RankingData ranking) {
        ScoreData now = main.getPlayDataAccessor().readScoreData(main.getPlayerResource().getBMSModel()
                , main.getPlayerConfig().getLnmode());
        final int nowscore = now != null ? now.getExscore() : 0;
		switch(target) {
		case NEXT:
			for(int i = 0;i  < ranking.getTotalPlayer(); i++) {
				if(ranking.getScore(i).getExscore() > nowscore) { 
					return ranking.getScore(Math.max(i - (value - 1) , 0)).getExscore();
				}
			}
			return nowscore;
		case RANK:
			return ranking.getTotalPlayer() > (value - 1) ? ranking.getScore(value - 1).getExscore() : 0;
		case RANKRATE:
			return ranking.getTotalPlayer() > 0 ? ranking.getScore(ranking.getTotalPlayer() * value / 100).getExscore() : 0;
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
    
    enum Target {
    	NEXT, RANK, RANKRATE
    }
}

