package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;

public interface NumberResourceAccessor {
	
	public abstract int getValue(MainState state);


	public static NumberResourceAccessor CLEAR = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getClear();
		}
	};

	public static NumberResourceAccessor TARGET_CLEAR = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTargetClear();
		}
	};

	public static NumberResourceAccessor SCORE = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getScore();
		}
	};

	public static NumberResourceAccessor BEST_SCORE = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getBestScore();
		}
	};

	public static NumberResourceAccessor TARGET_SCORE = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTargetScore();
		}
	};

	public static NumberResourceAccessor DIFF_SCORE = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			if(state.getTargetScore() == Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}
 			return state.getScore() - state.getTargetScore();
		}
	};

	public static NumberResourceAccessor MAX_SCORE = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTotalNotes() * 2;
		}
	};
	
	public static NumberResourceAccessor MISSCOUNT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getMisscount();
		}
	};
	
	public static NumberResourceAccessor TARGET_MISSCOUNT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTargetMisscount();
		}
	};

	public static NumberResourceAccessor DIFF_MISSCOUNT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			if(state.getTargetMisscount() == Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}
			return state.getMisscount() - state.getTargetMisscount();
		}
	};

	public static NumberResourceAccessor MAXCOMBO = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getMaxcombo();
		}
	};
	
	public static NumberResourceAccessor TARGET_MAXCOMBO = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTargetMaxcombo();
		}
	};

	public static NumberResourceAccessor DIFF_MAXCOMBO = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			if(state.getTargetMaxcombo() == Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}
			return state.getMaxcombo() - state.getTargetMaxcombo();
		}
	};

	public static NumberResourceAccessor PERFECT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(0, true) + state.getJudgeCount(0, false);
		}
	};
	
	public static NumberResourceAccessor FAST_PERFECT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(0, true);
		}
	};
	
	public static NumberResourceAccessor SLOW_PERFECT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(0, false);
		}
	};
	
	public static NumberResourceAccessor GREAT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(1, true) + state.getJudgeCount(1, false);
		}
	};
	
	public static NumberResourceAccessor FAST_GREAT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(1, true);
		}
	};
	
	public static NumberResourceAccessor SLOW_GREAT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(1, false);
		}
	};
	
	public static NumberResourceAccessor GOOD = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(2, true) + state.getJudgeCount(2, false);
		}
	};
	
	public static NumberResourceAccessor FAST_GOOD = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(2, true);
		}
	};
	
	public static NumberResourceAccessor SLOW_GOOD = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(2, false);
		}
	};
	
	public static NumberResourceAccessor BAD = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(3, true) + state.getJudgeCount(3, false);
		}
	};
	
	public static NumberResourceAccessor FAST_BAD = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(3, true);
		}
	};
	
	public static NumberResourceAccessor SLOW_BAD = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(3, false);
		}
	};
	
	public static NumberResourceAccessor POOR = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(4, true) + state.getJudgeCount(4, false);
		}
	};
	
	public static NumberResourceAccessor FAST_POOR = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(4, true);
		}
	};
	
	public static NumberResourceAccessor SLOW_POOR = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(4, false);
		}
	};
	
	public static NumberResourceAccessor MISS = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(5, true) + state.getJudgeCount(5, false);
		}
	};
	
	public static NumberResourceAccessor FAST_TOTAL = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			int total = 0;
			for(int i = 1;i < 6;i++) {
				total += state.getJudgeCount(i, true);
			}
			return total;
		}
	};
	
	public static NumberResourceAccessor SLOW_TOTAL = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			int total = 0;
			for(int i = 1;i < 6;i++) {
				total += state.getJudgeCount(i, false);
			}
			return total;
		}
	};

	public static NumberResourceAccessor FAST_MISS = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(5, true);
		}
	};

	public static NumberResourceAccessor SLOW_MISS = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getJudgeCount(5, false);
		}
	};

	public static NumberResourceAccessor TOTALNOTES = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTotalNotes();
		}
	};
	
	public static NumberResourceAccessor MIN_BPM = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getMinBPM();
		}
	};
	
	public static NumberResourceAccessor NOW_BPM = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getBPM();
		}
	};
	
	public static NumberResourceAccessor MAX_BPM = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getMaxBPM();
		}
	};
	
	public static NumberResourceAccessor HISPEED = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return (int) state.getHispeed();
		}
	};
	
	public static NumberResourceAccessor HISPEED_AFTERDOT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return ((int)(state.getHispeed() * 100)) % 100;
		}
	};
	
	public static NumberResourceAccessor DURATION = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getDuration();
		}
	};	
}
