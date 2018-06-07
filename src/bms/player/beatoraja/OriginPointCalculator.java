package bms.player.beatoraja;

public class OriginPointCalculator implements IPointCalculator {

	@Override
	public int calcutaePoint(IRScoreData score) {
		int newPoint;	
		switch (score.getPlaymode()) {
	    case BEAT_5K:
	    case BEAT_10K:
	    	newPoint = (int)((long)100000 * score.getJudgeCount(0) + 100000 * score.getJudgeCount(1) + 50000 * score.getJudgeCount(2))
	                / score.getNotes();
	        break;
	    case BEAT_7K:
	    case BEAT_14K:
	    	newPoint = (int)((long)150000 * score.getJudgeCount(0) + 100000 * score.getJudgeCount(1) + 20000 * score.getJudgeCount(2))
	                / score.getNotes() + (int)((long)50000 * score.getCombo() / score.getNotes());
	        break;
	    case POPN_5K:
	    case POPN_9K:
	    	newPoint = (int)((long)100000 * score.getJudgeCount(0) + 70000 * score.getJudgeCount(1) + 40000 * score.getJudgeCount(2))
	                / score.getNotes();
	        break;
	    default:
	    	newPoint = (int)((long)1000000 * score.getJudgeCount(0) + 700000 * score.getJudgeCount(1) + 400000 * score.getJudgeCount(2))
	                / score.getNotes();
	        break;	
		}
		return newPoint;
	}
	
}
