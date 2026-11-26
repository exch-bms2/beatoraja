package bms.player.beatoraja.play;

import java.util.Arrays;
import java.util.stream.Stream;

import bms.player.beatoraja.ir.IRScoreData;
import com.badlogic.gdx.utils.Array;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.PlayerInformation;
import bms.player.beatoraja.RivalDataAccessor;
import bms.player.beatoraja.ScoreData;
import bms.player.beatoraja.ir.RankingData;
import bms.player.beatoraja.select.ScoreDataCache;

/**
 * スコアターゲット
 * 
 * @author exch
 */
public abstract class TargetProperty {

    /** A response may only update the exact play and target that requested it. */
    record PendingTarget(MainController main, bms.player.beatoraja.PlayerResource resource, bms.model.BMSModel model,
            bms.player.beatoraja.ReplayData replay, ScoreData target) {
        boolean active() {
            return main.getPlayerResource() == resource && resource.getBMSModel() == model && resource.getReplayData() == replay
                    && resource.getTargetScoreData() == target && resource.getPrimaryScoringSelection().isStandard();
        }
        boolean apply(Runnable update) {
            if (!active()) return false;
            update.run();
            return true;
        }
    }

    public static boolean isTargetAvailable(bms.player.beatoraja.MainState state) {
        if (state instanceof bms.player.beatoraja.select.MusicSelector selector)
            return selector.getRival() != null && selector.canCompareRival();
        return state.resource.getTargetScoreData() != null
                && !(state instanceof bms.player.beatoraja.result.MusicResult result && result.isShowingSecondaryScore());
    }

	/**
	 * Target ID
	 */
	public final String id;
	
    /**
     * ターゲットスコア
     */
    protected final ScoreData targetScore = new ScoreData();
    protected ScoreData scoring;

    public ScoreData getTarget(MainController main, ScoreData scoring) {
        this.scoring = scoring;
        return getTarget(main);
    }

    protected int maximum(MainController main) {
        return scoring != null ? scoring.getRuleMaxScore() : main.getPlayerResource().getBMSModel().getTotalNotes() * 2;
    }

    protected void assignTarget(int value, MainController main) {
        targetScore.setNotes(scoring != null ? scoring.getNotes() : main.getPlayerResource().getBMSModel().getTotalNotes());
        targetScore.setEpg(value / 2); targetScore.setEgr(value % 2);
        if (scoring != null && scoring.getNativeScore() >= 0) {
            targetScore.setNativeScore(value); targetScore.setNativeMaxScore(scoring.getRuleMaxScore());
        }
    }
    
    private static String[] targets = new String[0];
    private static String[] targetNames =  new String[0];
    
    public TargetProperty(String id) {
    	this.id = id;
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
    
    public static void setTargets(String[] s, MainController main) {
    	if(s != null) {
    		targets = s;
    	}
    	targetNames = Stream.of(s).map(TargetProperty::getTargetProperty)
    			.map(target -> target != null ? target.getName(main) : "").toArray(String[]::new);
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

    public abstract String getName(MainController main);
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
    
    private String name;

    public StaticTargetProperty(String id, String name, float rate) {
    	super(id);
    	this.name = name;
        this.rate = rate;
    }

	@Override
	public String getName(MainController main) {
		return name;
	}

    @Override
    public ScoreData getTarget(MainController main) {
        int rivalscore = (int)Math.ceil(maximum(main) * rate / 100f);
		targetScore.setPlayer(getName(main));
		assignTarget(rivalscore, main);
        return targetScore;
    }
    
    public static TargetProperty getTargetProperty(String id) {
		switch(id) {
			case "RATE_A-":
				return new StaticTargetProperty("RATE_A-", "RANK A-",   100.0f * 17.0f / 27.0f);
			case "RATE_A":
				return new StaticTargetProperty("RATE_A", "RANK A",   100.0f * 18.0f / 27.0f);
			case "RATE_A+":
				return new StaticTargetProperty("RATE_A+", "RANK A+",   100.0f * 19.0f / 27.0f);
			case "RATE_AA-":
				return new StaticTargetProperty("RATE_AA-", "RANK AA-",   100.0f * 20.0f / 27.0f);
			case "RATE_AA":
				return new StaticTargetProperty("RATE_AA", "RANK AA",   100.0f * 21.0f / 27.0f);
			case "RATE_AA+":
				return new StaticTargetProperty("RATE_AA+", "RANK AA+",   100.0f * 22.0f / 27.0f);
			case "RATE_AAA-":
				return new StaticTargetProperty("RATE_AAA-", "RANK AAA-",   100.0f * 23.0f / 27.0f);
			case "RATE_AAA":
				return new StaticTargetProperty("RATE_AAA", "RANK AAA",   100.0f * 24.0f / 27.0f);
			case "RATE_AAA+":
				return new StaticTargetProperty("RATE_AAA+", "RANK AAA+",   100.0f * 25.0f / 27.0f);
			case "RATE_MAX-":
				return new StaticTargetProperty("RATE_MAX-", "RANK MAX-",   100.0f * 26.0f / 27.0f);
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

	private final Target target;
   
    private final int index;

    public RivalTargetProperty(Target target, int index) {
    	super("RIVAL_" + (index + 1));
    	this.target = target;
        this.index = index;
    }

	@Override
	public String getName(MainController main) {
    	final PlayerInformation info = main.getRivalDataAccessor().getRivalInformation(index);
    	switch(target) {
    	case INDEX:
    		return info != null ? "RIVAL " + info.getName() : "NO RIVAL";
    	case RANK:
    		return index > 0 ? "RIVAL RANK " + (index + 1) : "RIVAL TOP";
    	case NEXT:
    		return "RIVAL NEXT " + (index + 1);
    	}
		return "NO RIVAL";
	}

    @Override
    public ScoreData getTarget(MainController main) {
        if (!main.getPlayerResource().getPrimaryScoringSelection().isStandard()) return null;
    	final PlayerInformation info = main.getRivalDataAccessor().getRivalInformation(index);
    	final ScoreDataCache cache = main.getRivalDataAccessor().getRivalScoreDataCache(index);
    	
    	String name = null;
    	ScoreData score = null;
    	ScoreData[] scores = null;
    	switch(target) {
    	case INDEX:
    		name = info != null ? info.getName() : name;
    		score = cache != null ? cache.readScoreData(main.getPlayerResource().getSongdata(), main.getPlayerConfig().getLnmode()) : score;
        	break;
    	case RANK:
    		scores = createScoreArray(main);
    		if(scores.length > 0) {
        		Arrays.sort(scores, (s1, s2) -> (s2.getExscore() - s1.getExscore()));
        		score = scores[index < scores.length ? index : scores.length - 1];
        		name = score.getPlayer();
    		}
    		
    		break;
    	case NEXT:
    		scores = createScoreArray(main);
    		if(scores.length > 0) {
        		Arrays.sort(scores, (s1, s2) -> (s2.getExscore() - s1.getExscore()));
        		
        		int rank = Math.max(scores.length -1 - index , 0);
    			for(int i = 0;i  < scores.length; i++) {
    				if(scores[i].getPlayer().length() == 0) {
    					rank = Math.max(i - index , 0);
    				}
    			}
        		score = scores[rank];
        		name = score.getPlayer();
    		}
    		break;
    		
    	}
    	
    	if(score != null) {
    		targetScore.setPlayer(name);
    		targetScore.setEpg(score.getEpg());
    		targetScore.setLpg(score.getLpg());
    		targetScore.setEgr(score.getEgr());
    		targetScore.setLgr(score.getLgr());
    		targetScore.setOption(score.getOption());
    	} else if(name != null) {
    		targetScore.setPlayer("NO DATA");
    		targetScore.setOption(0);
    	} else {
    		targetScore.setPlayer("NO RIVAL");
    		targetScore.setOption(0);
    	}
    	
        return targetScore;
    }
    
    private ScoreData[] createScoreArray(MainController main) {
    	final RivalDataAccessor rivals = main.getRivalDataAccessor();
		Array<ScoreData> scorearray = new Array<ScoreData>();
		for(int i = 0;i < rivals.getRivalCount();i++) {
			ScoreData sd = rivals.getRivalScoreDataCache(i).readScoreData(main.getPlayerResource().getSongdata(), main.getPlayerConfig().getLnmode());
			if(sd != null) {
				sd.setPlayer(rivals.getRivalInformation(i).getName());
				scorearray.add(sd);
			}
		}
		
		ScoreData myscore = main.getPlayDataAccessor().readScoreData(main.getPlayerResource().getSongdata().getBMSModel(), main.getPlayerConfig().getLnmode());
		if(myscore != null) {
			myscore.setPlayer("");
			scorearray.add(myscore);
		}
		return scorearray.toArray(ScoreData.class);
    }
    
    public static TargetProperty getTargetProperty(String id) {
    	if(id.startsWith("RIVAL_NEXT_")) {
    		try {
        		int index = Integer.parseInt(id.substring(11));
        		if(index > 0) {
        			return new RivalTargetProperty(Target.NEXT, index - 1);
        		}
    		} catch (NumberFormatException e) {
    			
    		}
    	} else if(id.startsWith("RIVAL_RANK_")) {
    		try {
        		int index = Integer.parseInt(id.substring(11));
        		if(index > 0) {
        			return new RivalTargetProperty(Target.RANK, index - 1);
        		}
    		} catch (NumberFormatException e) {
    			
    		}
    	} else if(id.startsWith("RIVAL_")) {
    		try {
        		int index = Integer.parseInt(id.substring(6));
        		if(index > 0) {
        			return new RivalTargetProperty(Target.INDEX, index - 1);
        		}
    		} catch (NumberFormatException e) {
    			
    		}
    	}
    	return null;
    }
    
    enum Target {
    	INDEX, NEXT, RANK
    }
}

class NextRankTargetProperty extends TargetProperty {

    public NextRankTargetProperty() {
        super("RANK_NEXT");
    }

	@Override
	public String getName(MainController main) {
		return "NEXT RANK";
	}

    @Override
    public ScoreData getTarget(MainController main) {
        var resource = main.getPlayerResource();
        var model = resource.getBMSModel();
        ScoreData now = resource.getPrimaryScoringSelection().isStandard()
                ? main.getPlayDataAccessor().readScoreData(model, main.getPlayerConfig().getLnmode())
                : main.getPlayDataAccessor().readRuleBest(resource.getPrimaryScoringSelection().effective(), model.getSHA256(),
                        model.containsUndefinedLongNote() ? model.getLntype() : 0);
        final int nowscore = now != null ? now.getRuleScore() : 0;
        final int max = maximum(main);
        int targetscore = max;
        for(int i = 15;i < 27;i++) {
            int target = (int)Math.ceil(max * i / 27f);
            if(nowscore < target) {
            	targetscore = target;
            	break;
            }
        }
		targetScore.setPlayer(getName(main));
		assignTarget(targetscore, main);
        return targetScore;

    }
}

/**
 * ターゲット:IR
 *
 * @author exch
 */
class InternetRankingTargetProperty extends TargetProperty {

    private final Target target;
    
    private final int value;
    
    private InternetRankingTargetProperty(Target target, int value) {
    	super("IR_" + target.name() + "_" + value);
        this.target = target;
        this.value = value;
    }

	@Override
	public String getName(MainController main) {
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

    @Override
    public ScoreData getTarget(MainController main) {
        var resource = main.getPlayerResource();
        if (!resource.getPrimaryScoringSelection().isStandard()) return null;
        final RankingData ranking = resource.getRankingData();
        targetScore.setPlayer("NO DATA");
        targetScore.setOption(0);
        if (ranking == null) return targetScore;
        var best = main.getPlayDataAccessor().readScoreData(resource.getBMSModel(), main.getPlayerConfig().getLnmode());
        final int localBest = best != null ? best.getExscore() : 0;
        if (ranking.getState() == RankingData.FINISH) {
            fillTarget(ranking, localBest);
            return targetScore;
        }
        var request = new PendingTarget(main, resource, resource.getBMSModel(), resource.getReplayData(), targetScore);
        final var owner = main.getCurrentState();
        final var song = resource.getSongdata();
        Thread irprocess = new Thread(() -> {
            ranking.load(owner, song, best);
            while (ranking.getState() == RankingData.NONE || ranking.getState() == RankingData.ACCESS) {
                if (resource.getBMSModel() != request.model() || resource.getReplayData() != request.replay()) return;
                try { Thread.sleep(500); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
            if (ranking.getState() != RankingData.FINISH) return;
            Runnable apply = () -> request.apply(() -> {
                fillTarget(ranking, localBest);
                main.getCurrentState().getScoreDataProperty().updateTargetScore(targetScore.getExscore());
            });
            if (com.badlogic.gdx.Gdx.app != null) com.badlogic.gdx.Gdx.app.postRunnable(apply);
            else apply.run();
        }, "IR-target");
        irprocess.start();
        return targetScore;
    }

    private void fillTarget(RankingData ranking, int localBest) {
        if (ranking.getTotalPlayer() == 0) return;
        var irScore = ranking.getScore(getTargetRank(localBest, ranking));
        targetScore.setPlayer(irScore.player.isEmpty() ? "YOU" : irScore.player);
        targetScore.setEpg(irScore.getExscore() / 2);
        targetScore.setEgr(irScore.getExscore() % 2);
        targetScore.setOption(irScore.option);
    }

    private int getTargetRank(int nowscore, RankingData ranking) {
        return switch (target) {
            case NEXT -> {
                int rank = 0;
                for (int i = 0; i < ranking.getTotalPlayer(); i++) {
                    if (ranking.getScore(i).getExscore() <= nowscore) { rank = Math.max(i - value, 0); break; }
                }
                yield rank;
            }
            case RANK -> Math.min(ranking.getTotalPlayer(), value) - 1;
            case RANKRATE -> ranking.getTotalPlayer() * value / 100;
        };
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

