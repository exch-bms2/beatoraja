package bms.player.beatoraja;

import java.util.Arrays;
import java.util.Calendar;

import bms.player.beatoraja.skin.Skin;

public abstract class MainState {

	private final MainController main;
	
	private long starttime;
	
	private long[] timer = new long[256];
	private boolean[] option = new boolean[1000];
	
	private Skin skin;
	
	public static final int TIMER_FADEOUT = 2;
	public static final int TIMER_FAILED = 3;
	public static final int TIMER_READY = 40;
	public static final int TIMER_PLAY = 41;
	public static final int TIMER_FULLCOMBO1 = 48;
	public static final int TIMER_FULLCOMBO2 = 49;
	public static final int TIMER_BOMB = 50;
	public static final int TIMER_HOLD = 70;
	public static final int TIMER_KEYON = 100;
	public static final int TIMER_KEYOFF = 120;
	
	// 選曲専用
	public static final int SLIDER_MUSICSELECT_POSITION = 1;
	// プレイ専用
	public static final int SLIDER_MUSIC_PROGRESS = 6;
	public static final int OFFSET_LIFT = 100;
	
	public static final int STRING_TITLE = 10;
	public static final int STRING_SUBTITLE = 11;
	public static final int STRING_FULLTITLE = 12;
	public static final int STRING_GENRE = 13;
	public static final int STRING_ARTIST = 14;
	public static final int STRING_SUBARTIST = 15;
	public static final int STRING_DIRECTORY = 1000;
	
	public static final int NUMBER_TIME_YEAR = 21;
	public static final int NUMBER_TIME_MONTH = 22;
	public static final int NUMBER_TIME_DAY = 23;
	public static final int NUMBER_TIME_HOUR = 24;
	public static final int NUMBER_TIME_MINUTE = 25;
	public static final int NUMBER_TIME_SECOND = 26;

	public static final int NUMBER_TOTALPLAYCOUNT = 30;
	public static final int NUMBER_TOTALCLEARCOUNT = 31;
	public static final int NUMBER_TOTALFAILCOUNT = 32;
	public static final int NUMBER_TOTALPERFECT = 33;
	public static final int NUMBER_TOTALGREAT = 34;
	public static final int NUMBER_TOTALGOOD = 35;
	public static final int NUMBER_TOTALBAD = 36;
	public static final int NUMBER_TOTALPOOR = 37;
	public static final int NUMBER_TOTALPLAYNOTES = 1037;
	public static final int NUMBER_PLAYCOUNT = 77;
	public static final int NUMBER_CLEARCOUNT = 78;
	public static final int NUMBER_FAILCOUNT = 79;
	public static final int NUMBER_GROOVEGAUGE = 107;
	public static final int NUMBER_GROOVEGAUGE_AFTERDOT = 207;
	public static final int NUMBER_TIMELEFT_MINUTE = 163;
	public static final int NUMBER_TIMELEFT_SECOND = 164;

	public MainState() {
		this(null);
	}
	
	public MainState(MainController main) {
		this.main = main;
		Arrays.fill(timer, -1);
		Arrays.fill(option, false);
	}
	
	public MainController getMainController() {
		return main;
	}
	
	public abstract void create();
	
	public abstract void render();
	
	public void pause() {
		
	}
	
	public void resume() {
		
	}
	
	public void resize(int width, int height) {
		
	}
	
	public abstract void dispose();

	public long getStartTime() {
		return starttime;
	}

	public void setStartTime(long starttime) {
		this.starttime = starttime;
	}

	public int getNowTime() {
		return (int) (System.currentTimeMillis() - starttime);
	}

	public long[] getTimer() {
		return timer;
	}
	
	public boolean[] getOption() {
		return option;
	}
	
	public Skin getSkin() {
		return skin;
	}

	public void setSkin(Skin skin) {
		this.skin = skin;
	}

	public int getClear() {
		return 0;
	}

	public int getTargetClear() {
		return 0;
	}

	public int getScore() {
		return 0;
	}
	
	public int getBestScore() {
		return 0;
	}
	
	public int getTargetScore() {
		return 0;
	}
	
	public int getMaxcombo() {
		return 0;
	}
	
	public int getTargetMaxcombo() {
		return 0;
	}

	public int getJudgeCount(int judge, boolean fast) {
		return 0;
	}

	public int getMinBPM() {
		return 0;
	}
	
	public int getBPM() {
		return 0;
	}
	
	public int getMaxBPM() {
		return 0;
	}
	
	public int getTotalNotes() {
		return 0;
	}
	
	public float getHispeed() {
		return 0;
	}

	public int getDuration() {
		return 0;
	}

	public int getMisscount() {
		return 0;
	}

	public int getTargetMisscount() {
		return 0;
	}

	private Calendar cl = Calendar.getInstance();
	public int getNumberValue(int id) {
		switch(id) {
		case NUMBER_TIME_YEAR:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.YEAR);
		case NUMBER_TIME_MONTH:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.MONTH) +1;
		case NUMBER_TIME_DAY:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.DATE);
		case NUMBER_TIME_HOUR:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.HOUR_OF_DAY);
		case NUMBER_TIME_MINUTE:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.MINUTE);
		case NUMBER_TIME_SECOND:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.SECOND);
		}
		return 0;
	}
	
	public float getSliderValue(int id) {
		return 0;
	}
	
	public String getTextValue(int id) {
		if(getMainController().getPlayerResource() != null) {
			switch(id) {
			case STRING_TITLE:
				return getMainController().getPlayerResource().getBMSModel().getTitle();
			case STRING_SUBTITLE:
				return getMainController().getPlayerResource().getBMSModel().getSubTitle();
			case STRING_FULLTITLE:
				return getMainController().getPlayerResource().getBMSModel().getFullTitle();
			case STRING_ARTIST:
				return getMainController().getPlayerResource().getBMSModel().getArtist();
			case STRING_SUBARTIST:
				return getMainController().getPlayerResource().getBMSModel().getSubTitle();
			case STRING_GENRE:
				return getMainController().getPlayerResource().getBMSModel().getGenre();
			}
		}
		return "";
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
