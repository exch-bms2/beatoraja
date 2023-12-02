package bms.player.beatoraja.result;

import static bms.player.beatoraja.ClearType.NoPlay;

import java.util.Arrays;

import com.badlogic.gdx.math.MathUtils;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.ScoreData;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.ir.RankingData;

public abstract class AbstractResult extends MainState {

	/**
	 * 状態
	 */
	protected int state;

	public static final int STATE_OFFLINE = 0;
	/**
	 * 状態:IR送信中
	 */
	public static final int STATE_IR_PROCESSING = 1;
	/**
	 * 状態:IR処理完了
	 */
	public static final int STATE_IR_FINISHED = 2;

	/**
	 * ランキングデータ
	 */
	protected RankingData ranking;
	/**
	 * ランキング表示位置
	 */
	protected int rankingOffset = 0;
	/**
	 * 全ノーツの平均ズレ(us)
	 */
	protected long avgduration;
	/**
	 * タイミング分布
	 */
	protected TimingDistribution timingDistribution;

	/**
	 * タイミング分布レンジ
	 */
	final int distRange = 150;
	/**
	 * 各リプレイデータ状態
	 */
	protected ReplayStatus[] saveReplay = new ReplayStatus[REPLAY_SIZE];
	protected static final int REPLAY_SIZE = 4;
	
	protected int gaugeType;

	/**
	 * 旧スコアデータ
	 */
	protected ScoreData oldscore = new ScoreData();

	public AbstractResult(MainController main) {
		super(main);
		timingDistribution = new TimingDistribution(distRange);
	}
	
	public ReplayStatus getReplayStatus(int index) {
		return saveReplay[index];
	}

	/**
	 * リプレイ自動保存条件
	 *
	 * @author exch
	 */
	public enum ReplayAutoSaveConstraint {

		/**
		 * 保存しない
		 */
		NOTHING {
			@Override
			public boolean isQualified(ScoreData oldscore, ScoreData newscore) {
				return false;
			}
		},
		/**
		 * スコア更新時に保存
		 */
		SCORE_UPDATE {
			@Override
			public boolean isQualified(ScoreData oldscore, ScoreData newscore) {
				return newscore.getExscore() > oldscore.getExscore();
			}
		},
		/**
		 * スコア同数以上時に保存
		 */
		SCORE_UPDATE_OR_EQUAL {
			@Override
			public boolean isQualified(ScoreData oldscore, ScoreData newscore) {
				return newscore.getExscore() >= oldscore.getExscore();
			}
		},
		/**
		 * ミスカウント更新時に保存
		 */
		MISSCOUNT_UPDATE {
			@Override
			public boolean isQualified(ScoreData oldscore, ScoreData newscore) {
				return newscore.getMinbp() < oldscore.getMinbp() || oldscore.getClear() == NoPlay.id;
			}
		},
		/**
		 * ミスカウント同数以下時に保存
		 */
		MISSCOUNT_UPDATE_OR_EQUAL {
			@Override
			public boolean isQualified(ScoreData oldscore, ScoreData newscore) {
				return newscore.getMinbp() <= oldscore.getMinbp() || oldscore.getClear() == NoPlay.id;
			}
		},
		/**
		 * 最大コンボ数更新時に保存
		 */
		MAXCOMBO_UPDATE {
			@Override
			public boolean isQualified(ScoreData oldscore, ScoreData newscore) {
				return newscore.getCombo() > oldscore.getCombo();
			}
		},
		/**
		 * 最大コンボ数同数以上時に保存
		 */
		MAXCOMBO_UPDATE_OR_EQUAL {
			@Override
			public boolean isQualified(ScoreData oldscore, ScoreData newscore) {
				return newscore.getCombo() >= oldscore.getCombo();
			}
		},
		/**
		 * クリアランプ更新時に保存
		 */
		CLEAR_UPDATE {
			@Override
			public boolean isQualified(ScoreData oldscore, ScoreData newscore) {
				return newscore.getClear() > oldscore.getClear();
			}
		},
		/**
		 * クリアランプ同等以上時に保存
		 */
		CLEAR_UPDATE_OR_EQUAL {
			@Override
			public boolean isQualified(ScoreData oldscore, ScoreData newscore) {
				return newscore.getClear() >= oldscore.getClear();
			}
		},
		/**
		 * 何かしらの更新があれば保存
		 */
		ANYONE_UPDATE {
			@Override
			public boolean isQualified(ScoreData oldscore, ScoreData newscore) {
				return newscore.getClear() > oldscore.getClear() || newscore.getCombo() > oldscore.getCombo() ||
						newscore.getMinbp() < oldscore.getMinbp() || newscore.getExscore() > oldscore.getExscore();
			}
		},
		/**
		 * 常時保存
		 */
		ALWAYS {
			@Override
			public boolean isQualified(ScoreData oldscore, ScoreData newscore) {
				return true;
			}
		};

		/**
		 * リプレイ保存条件を満たしているかどうか判定する
		 *
		 * @param oldscore 旧スコアデータ
		 * @param newscore 新スコアデータ
		 * @return リプレイ保存条件を満たしていればtrue
		 */
		public abstract boolean isQualified(ScoreData oldscore, ScoreData newscore);

		public static ReplayAutoSaveConstraint get(int index) {
			if (index < 0 || index >= values().length) {
				return NOTHING;
			}
			return values()[index];
		}
	}

	/**
	 * リプレイデータ状態
	 *
	 * @author exch
	 */
	public enum ReplayStatus {
		EXIST, NOT_EXIST, SAVED;
	}

	public int getGaugeType() {
		return gaugeType;
	}
	
	public int getState() {
		return state;
	}
	
	public RankingData getRankingData() {
		return ranking;
	}
	
	public int getIRRank() {
		return ranking != null ? ranking.getRank() : 0;
	}
	
	public int getOldIRRank() {
		return ranking != null ? ranking.getPreviousRank() : 0;
	}
	
	public int getIRTotalPlayer() {
		return ranking != null ? ranking.getTotalPlayer() : 0;
	}
	
	public long getAverageDuration() {
		return avgduration;
	}
	
	public abstract ScoreData getNewScore();
	
	public ScoreData getOldScore() {
		return oldscore;
	}	
	
	public TimingDistribution getTimingDistribution() {
		return timingDistribution;
	}

	public void input() {
		BMSPlayerInputProcessor input = main.getInputProcessor();
		int mov = -input.getScroll();
		input.resetScroll();
		if (mov != 0 && ranking != null) {
			final int rankingMax = Math.max(1, ranking.getTotalPlayer());
			rankingOffset = MathUtils.clamp(rankingOffset + mov, 0, rankingMax - 1);
		}
	}
	
	public int getRankingOffset() {
		return rankingOffset;
	}
	
	public float getRankingPosition() {
		final int rankingMax = ranking != null ? Math.max(1, ranking.getTotalPlayer()) : 1;
		return (float)rankingOffset / rankingMax;		
	}
	
	public void setRankingPosition(float value) {
		if (value >= 0 && value < 1) {
			final int rankingMax = ranking != null ? Math.max(1, ranking.getTotalPlayer()) : 1;
			rankingOffset = (int) (rankingMax * value);
		}
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
