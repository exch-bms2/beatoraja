package bms.player.beatoraja.result;

import static bms.player.beatoraja.ClearType.NoPlay;

import java.util.Arrays;

import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainState;

public abstract class AbstractResult extends MainState {

	/**
	 * 状態
	 */
	protected int state;

	public static final int STATE_OFFLINE = 0;
	public static final int STATE_IR_PROCESSING = 1;
	public static final int STATE_IR_FINISHED = 2;

	protected int irrank;
	protected int irprevrank;
	protected int irtotal;
	/**
	 * 全ノーツの平均ズレ
	 */
	protected float avgduration;
	/**
	 * タイミング分布
	 */
	protected TimingDistribution timingDistribution;

	/**
	 * タイミング分布レンジ
	 */
	final int distRange = 150;

	protected ReplayStatus[] saveReplay = new ReplayStatus[REPLAY_SIZE];
	protected static final int REPLAY_SIZE = 4;

	public static final int SOUND_CLEAR = 0;
	public static final int SOUND_FAIL = 1;
	public static final int SOUND_CLOSE = 2;
	
	protected int gaugeType;
	
	protected IRScoreData oldscore = new IRScoreData();

	public AbstractResult(MainController main) {
		super(main);
		timingDistribution = new TimingDistribution(distRange);
	}
	
	public ReplayStatus getReplayStatus(int index) {
		return saveReplay[index];
	}
	
	public enum ReplayAutoSaveConstraint {

		NOTHING {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return false;
			}
		},
		SCORE_UPDATE {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getExscore() > oldscore.getExscore();
			}
		},
		SCORE_UPDATE_OR_EQUAL {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getExscore() >= oldscore.getExscore();
			}
		},
		MISSCOUNT_UPDATE {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getMinbp() < oldscore.getMinbp() || oldscore.getClear() == NoPlay.id;
			}
		},
		MISSCOUNT_UPDATE_OR_EQUAL {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getMinbp() <= oldscore.getMinbp() || oldscore.getClear() == NoPlay.id;
			}
		},
		MAXCOMBO_UPDATE {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getCombo() > oldscore.getCombo();
			}
		},
		MAXCOMBO_UPDATE_OR_EQUAL {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getCombo() >= oldscore.getCombo();
			}
		},
		CLEAR_UPDATE {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getClear() > oldscore.getClear();
			}
		},
		CLEAR_UPDATE_OR_EQUAL {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getClear() >= oldscore.getClear();
			}
		},
		ANYONE_UPDATE {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return newscore.getClear() > oldscore.getClear() || newscore.getCombo() > oldscore.getCombo() ||
						newscore.getMinbp() < oldscore.getMinbp() || newscore.getExscore() > oldscore.getExscore();
			}
		},
		ALWAYS {
			@Override
			public boolean isQualified(IRScoreData oldscore, IRScoreData newscore) {
				return true;
			}
		};

		public abstract boolean isQualified(IRScoreData oldscore, IRScoreData newscore);

		public static ReplayAutoSaveConstraint get(int index) {
			if (index < 0 || index >= values().length) {
				return NOTHING;
			}
			return values()[index];
		}
	}
	
	public enum ReplayStatus {
		EXIST, NOT_EXIST, SAVED;
	}

	public int getGaugeType() {
		return gaugeType;
	}
	
	public int getState() {
		return state;
	}
	
	public int getIRRank() {
		return irrank;
	}
	
	public int getOldIRRank() {
		return irprevrank;
	}
	
	public int getIRTotalPlayer() {
		return irtotal;
	}
	
	public float getAverageDuration() {
		return avgduration;
	}
	
	public abstract IRScoreData getNewScore();
	
	public IRScoreData getOldScore() {
		return oldscore;
	}	
	
	public TimingDistribution getTimingDistribution() {
		return timingDistribution;
	}

	/**
	 *
	 *
	 * @author KEH
	 */
	public static class TimingDistribution {
		private final int arrayCenter;
		private int[] dist;
		private float average;
		private float stdDev;

		public TimingDistribution(int range) {
			this.arrayCenter = range;
			this.dist = new int[range * 2 + 1];
		}

		public void statisticValueCalcuate() {
			int count = 0;
			int sum = 0;
			float sumf = 0;

			for (int i = 0; i < dist.length; i++) {
				count += dist[i];
				sum += dist[i] * (i - arrayCenter);
			}

			if (count == 0) {
				return;
			}

			average = sum * 1.0f / count;

			for (int i = 0; i < dist.length; i++) {
				sumf += dist[i] * (i - arrayCenter - average) * (i - arrayCenter - average);
			}

			stdDev = (float) Math.sqrt(sumf / count);
		}

		public void init() {
			Arrays.fill(dist, 0);
			average = Float.MAX_VALUE;
			stdDev = -1.0f;
		}

		public void add(int timing) {
			if (-arrayCenter <= timing && timing <= arrayCenter) {
				dist[timing + arrayCenter]++;
			}
		}

		public int[] getTimingDistribution() {
			return dist;
		}

		public float getAverage() {
			return average;
		}

		public float getStdDev() {
			return stdDev;
		}

		public int getArrayCenter() {
			return arrayCenter;
		}

	}
}
