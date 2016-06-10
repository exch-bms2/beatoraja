package bms.player.beatoraja.skin;

import java.util.Calendar;

import bms.player.beatoraja.MainState;

public interface NumberResourceAccessor {
	
	public abstract int getValue(MainState state);

	
	public static NumberResourceAccessor SCORE = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return (state.getJudgeCount(0, true) + state.getJudgeCount(0, false)) * 2 + state.getJudgeCount(1, true) + state.getJudgeCount(1, false);
		}
	};
	
	public static NumberResourceAccessor TARGET_SCORE = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTargetScore();
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
	
	public static NumberResourceAccessor PLAYCOUNT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getPlayCount(true) + state.getPlayCount(false);
		}
	};
	
	public static NumberResourceAccessor CLEARCOUNT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getPlayCount(true);
		}
	};
	
	public static NumberResourceAccessor FAILCOUNT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getPlayCount(false);
		}
	};
	
	public static NumberResourceAccessor PLAYER_PLAYCOUNT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTotalPlayCount(true) + state.getTotalPlayCount(false);
		}
	};
	
	public static NumberResourceAccessor PLAYER_CLEARCOUNT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTotalPlayCount(true);
		}
	};
	
	public static NumberResourceAccessor PLAYER_FAILCOUNT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTotalPlayCount(false);
		}
	};
	
	public static NumberResourceAccessor PLAYER_PERFECT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTotalJudgeCount(0);
		}
	};
	
	public static NumberResourceAccessor PLAYER_GREAT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTotalJudgeCount(1);
		}
	};
	
	public static NumberResourceAccessor PLAYER_GOOD = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTotalJudgeCount(2);
		}
	};
	
	public static NumberResourceAccessor PLAYER_BAD = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTotalJudgeCount(3);
		}
	};
	
	public static NumberResourceAccessor PLAYER_POOR = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTotalJudgeCount(4);
		}
	};
	
	public static NumberResourceAccessor PLAYER_MISS = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTotalJudgeCount(5);
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
	
	public static NumberResourceAccessor GROOVEGAUGE = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return (int) state.getGrooveGauge();
		}
	};
	
	public static NumberResourceAccessor GROOVEGAUGE_AFTERDOT = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return ((int)(state.getGrooveGauge() * 10)) % 10;
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
	
	public static NumberResourceAccessor TIMELEFT_MINUTE = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTimeleftMinute();
		}
	};
	
	public static NumberResourceAccessor TIMELEFT_SECOND = new NumberResourceAccessor() {
		@Override
		public int getValue(MainState state) {
			return state.getTimeleftSecond();
		}
	};

	public static NumberResourceAccessor TIME_YEAR = new TimeAccessor(Calendar.YEAR);	
	public static NumberResourceAccessor TIME_MONTH = new TimeAccessor(Calendar.MONTH);	
	public static NumberResourceAccessor TIME_DAY = new TimeAccessor(Calendar.DATE);	
	public static NumberResourceAccessor TIME_HOUR = new TimeAccessor(Calendar.HOUR_OF_DAY);	
	public static NumberResourceAccessor TIME_MINUTE = new TimeAccessor(Calendar.MINUTE);	
	public static NumberResourceAccessor TIME_SECOND = new TimeAccessor(Calendar.SECOND);
	
}

class TimeAccessor implements NumberResourceAccessor {

	protected Calendar cl = Calendar.getInstance();
	
	private int field;
	
	public TimeAccessor(int field) {
		this.field = field;
	}
	
	@Override
	public int getValue(MainState state) {
		cl.setTimeInMillis(System.currentTimeMillis());
		return cl.get(field) + (field == Calendar.MONTH ? 1 : 0);
	}	
}

/*
LR2のIDと参照値 (RED氏スキン仕様書より)
//プレイオプションとか

10 HS-1P
11 HS-2P

12 JUDGE TIMING
13 DEFAULT TARGET RATE

14 SUD+ / 1P
15 SUD+ / 2P


//なんかいろいろ
20 fps
21 年
22 月
23 日
24 時
25 分
26 秒

//プレイヤーステータスとか

30 TOTAL PLAY COUNT
31 TOTAL CLEAR COUNT
32 TOTAL FAIL COUNT

33 TOTAL PERFECT
34 TOTAL GREAT
35 TOTAL GOOD
36 TOTAL BAD
37 TOTAL POOR

38 RUNNING COMBO
39 RUNNING COMBO(MAX)

40 TRIAL LEVEL
41 TRIAL LEVEL-1 (更新表示のときに使うかも)


45 同フォルダのbeginner譜面の曲レベル
46 同フォルダのnormal譜面の曲レベル
47 同フォルダのhyper譜面の曲レベル
48 同フォルダのanother譜面の曲レベル
49 同フォルダのinsane譜面の曲レベル


//エフェクタとか
50 EQ0
51 EQ1
52 EQ2
53 EQ3
54 EQ4
55 EQ5
56 EQ6

57 MASTER VOLUME
58 KEY VOLUME
59 BGM VOLUME

60 FX0 P1
61 FX0 P2
62 FX1 P1
63 FX1 P2
64 FX2 P1
65 FX2 P2

66 PITCH

//選曲時
70 score
71 exscore
72 exscore理論値
73 rate
74 totalnotes
75 maxcombo
76 min b+p
77 playcount
78 clear
79 fail
//
80 perfect
81 great
82 good
83 bad
84 poor
85 perfect %
86 great %
87 good %
88 bad %
89 poor %

90 bpm max
91 bpm min

92 IR rank
93 IR totalplayer
94 IR clearrate

95 IR ライバルとの差分


//bmsプレイ時

//1P
100 score
101 exscore
102 rate
103 rate(小数点下二桁
104 nowcombo
105 maxcombo
106 totalnotes
107 groovegauge
108 exscore2pとの差
110 perfect
111 great
112 good
113 bad
114 poor
115 total rate
116 total rate(小数点下二桁

//対戦相手orゴースト
120 score
121 exscore
122 rate
123 rate(小数点下二桁
124 nowcombo
125 maxcombo
126 totalnotes
127 groovegauge
128 exscore2pとの差
130 perfect
131 great
132 good
133 bad
134 poor
135 total rate
136 total rate(小数点下二桁

//120-139は設定によってハイスコアかゴーストか不定なので、スコアグラフ上で指定して表示する場合
//リザルトで使用する場合は#DISABLEFLIP必須
150 ハイスコア現在値
151 ターゲット現在値
152 ハイスコアと1pスコアの差
153 ターゲットと1pスコアの差
154 次のランクとの差
155 ハイスコアrate
156 ハイスコアrate小数点2桁
157 ターゲットrate
158 ターゲットrate小数点2桁



//BMSの状態
160 bpm
161 分
162 秒
163 残り時間分
164 残り時間秒
165 ロード状況(%)

//リザのハイスコア表示用
170 EXSCORE更新前
171 EXSCORE今回
172 EXSCORE差分

173 MAXCOMBO更新前
174 MAXCOMBO今回
175 MAXCOMBO差分

176 最小BP更新前
177 最小BP更新後
178 最小BP差分

179 IR rank
180 IR totalplayer
181 IR clearrate
182 IR rank (更新前)

183 rate更新前
184 rate更新前(小数点下二桁



//IR(beta3以降用)

200 IR TOTALPLAYER
201 IR TOTALプレイ回数

210 FAILED人数
211 FAILED割合
212 EASY人数
213 EASY割合
214 CLEAR人数
215 CLEAR割合
216 HARD人数
217 HARD割合
218 FULLCOMBO人数
219 FULLCOMBO割合

220 IR自動更新までの残り時間

250 コースのレベルstage1
251 stage2
252 stage3
253 stage4
254 stage5


//選曲時ライバル
270 score
271 exscore
272 exscore理論値
273 rate
274 totalnotes
275 maxcombo
276 min b+p
277 playcount
278 clear
279 fail
//
280 perfect
281 great
282 good
283 bad
284 poor
285 perfect %
286 great %
287 good %
288 bad %
289 poor %

290 bpm max
291 bpm min

292 IR rank
293 IR totalplayer
294 IR clearrate

*/
