package bms.player.beatoraja;

import java.util.Arrays;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.MineNote;
import bms.model.Note;
import bms.model.TimeLine;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;

/**
 * ノーツ判定管理用クラス
 * 
 * @author exch
 */
public class JudgeManager {

	private BMSPlayer main;
	private BMSModel model;

	public static final int JUDGE_ALGORITHM_LR2 = 0;
	public static final int JUDGE_ALGORITHM_IIDX = 1;
	public static final int JUDGE_ALGORITHM_LOWEST_NOTE = 2;
	/**
	 * 現在の判定カウント内訳
	 */
	private int[][] count = new int[6][2];

	/**
	 * 現在のコンボ数
	 */
	private int combo;
	/**
	 * 最大コンボ数
	 */
	private int maxcombo;
	/**
	 * 現在表示中の判定
	 */
	private int[] judgenow;
	/**
	 * 判定の最終更新時間
	 */
	private int[] judgenowt;

	private int[] judgecombo;

	/**
	 * 判定差時間(ms , +は早押しで-は遅押し)
	 */
	private int judgefast;
	/**
	 * ボムの表示開始時間
	 */
	private long[] bomb = new long[8];
	/**
	 * 処理中のLN
	 */
	private LongNote[] processing = new LongNote[8];
	private LongNote[] passing = new LongNote[8];
	private boolean[] inclease = new boolean[8];
	private int[] passingcount;

	private int[] keyassign;
	private int[] noteassign;

	private int[] sckeyassign;
	private int[] sckey;
	/**
	 * ミスレイヤー表示開始時間
	 */
	private int misslayer;

	private final int[][] judgetable = new int[][] { { 8, 24, 70, 130, 0, 1000 }, { 14, 42, 115, 220, 0, 1000 },
			{ 18, 54, 150, 285, 0, 1000 }, { 20, 60, 165, 315, 0, 1000 } };

	public JudgeManager(BMSPlayer main, BMSModel model) {
		this.main = main;
		this.model = model;
		switch (model.getUseKeys()) {
		case 5:
		case 7:
			bomb = new long[8];
			processing = new LongNote[8];
			passing = new LongNote[8];
			passingcount = new int[8];
			inclease = new boolean[8];
			keyassign = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 7 };
			noteassign = new int[] { 0, 1, 2, 3, 4, 5, 6, 7 };
			sckeyassign = new int[] { 7 };
			sckey = new int[1];
			judgenow = new int[1];
			judgenowt = new int[1];
			judgecombo = new int[1];
			break;
		case 10:
		case 14:
			bomb = new long[16];
			processing = new LongNote[16];
			passing = new LongNote[16];
			passingcount = new int[16];
			inclease = new boolean[16];
			keyassign = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 7, 8, 9, 10, 11, 12, 13, 14, 15, 15 };
			noteassign = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16 };
			sckeyassign = new int[] { 7, 15 };
			sckey = new int[2];
			judgenow = new int[2];
			judgenowt = new int[2];
			judgecombo = new int[2];
			break;
		case 9:
			bomb = new long[9];
			processing = new LongNote[9];
			passing = new LongNote[9];
			passingcount = new int[9];
			inclease = new boolean[9];
			keyassign = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
			noteassign = new int[] { 0, 1, 2, 3, 4, 10, 11, 12, 13 };
			sckeyassign = new int[] {};
			sckey = new int[0];
			judgenow = new int[1];
			judgenowt = new int[1];
			judgecombo = new int[1];
			break;
		}
		Arrays.fill(bomb, -1000);
		if (model.getJudgerank() > 3) {
			judge = judgetable[3];
		} else {
			judge = judgetable[model.getJudgerank()];
		}
	}

	private int[] judge;

	private int pos = 0;
	private int judgetype = 0;

	private int prevtime;

	public void update(TimeLine[] timelines, int time) {
		BMSPlayerInputProcessor input = main.getBMSPlayerInputProcessor();
		long[] keytime = input.getTime();
		boolean[] keystate = input.getKeystate();
		for (int i = pos; timelines[i].getTime() <= time; i++) {
			if (timelines[i].getTime() > prevtime) {
				for (int key = 0; key < keyassign.length; key++) {
					final Note note = timelines[i].getNote(noteassign[keyassign[key]]);
					if (note != null) {
						if (note instanceof MineNote && keystate[key]) {
							final MineNote mnote = (MineNote) note;
							// 地雷ノート判定
							main.getGauge().addValue(-mnote.getDamage());
							System.out.println("Mine Damage : " + mnote.getWav());
						}
						if (model.getLntype() == BMSModel.LNTYPE_HELLCHARGENOTE && note instanceof LongNote) {
							// HCN判定
							final LongNote lnote = (LongNote) note;
							if (lnote.getStart() == timelines[i]) {
								passing[keyassign[key]] = lnote;
							}
							if (lnote.getEnd() == timelines[i]) {
								passing[keyassign[key]] = null;
								passingcount[keyassign[key]] = 0;
							}
						}
					}
				}

			}
		}

		for (int key = 0; key < keyassign.length; key++) {
			if (passing[keyassign[key]] != null) {
				if (keystate[key]) {
					passingcount[keyassign[key]] += (time - prevtime);
					inclease[keyassign[key]] = true;
					if (passingcount[keyassign[key]] > 100) {
						main.getGauge().addValue(main.getGauge().getGaugeValue(1));
						System.out.println("HCN : Gauge increase");
						passingcount[keyassign[key]] -= 100;
					}
				} else {
					passingcount[keyassign[key]] -= (time - prevtime);
					inclease[keyassign[key]] = false;
					if (passingcount[keyassign[key]] < -100) {
						main.getGauge().addValue(main.getGauge().getGaugeValue(4) / 5);
						System.out.println("HCN : Gauge decrease");
						passingcount[keyassign[key]] += 100;
					}
				}
			}
		}

		for (int key = 0; key < keyassign.length; key++) {
			if (keytime[key] != 0) {
				long ptime = keytime[key];
				int lane = keyassign[key];
				int sc = -1;
				for (int i = 0; i < sckeyassign.length; i++) {
					if (sckeyassign[i] == lane) {
						sc = i;
						break;
					}
				}
				if (keystate[key]) {
					// キーが押されたときの処理
					if (processing[lane] != null) {
						if (sc != -1 && key != sckey[sc]) {
							for (int j = 0; j < judge.length; j++) {
								if (j > 3) {
									j = 4;
								}
								if (j == 4 || (ptime > processing[lane].getEnd().getTime() - judge[j]
										&& ptime < processing[lane].getEnd().getTime() + judge[j])) {
									if (j < 2) {
										bomb[lane] = ptime;
									}
									this.update(lane, j < 4 ? j : 4, time,
											(int) (processing[lane].getEnd().getTime() - ptime));
									main.update(j);
									processing[lane].setState(j + 1);
									// System.out.println("打鍵:" + time +
									// " ノーツ位置 > " + tl.getTime() + " -
									// " +
									// (lane + 1) + " : " +
									// judgename[j]);
									System.out.println("BSS終端判定 - Time : " + ptime + " Judge : " + j + " LN : "
											+ processing[lane].hashCode());
									processing[lane] = null;
									sckey[sc] = 0;
									break;
								}
							}

						} else {
							// ここに来るのはマルチキーアサイン以外ありえないはず
						}
					} else {
						TimeLine tl = null;
						int j = 0;
						// 対象ノーツの抽出
						for (int i = pos; i < timelines.length && timelines[i].getTime() < ptime + judge[5]; i++) {
							if (timelines[i].getTime() >= ptime - judge[5]) {
								Note judgenote = timelines[i].getNote(noteassign[lane]);
								if (judgenote != null && !(judgenote instanceof MineNote)
										&& (judgenote.getState() == 0 || timelines[i].getTime() < ptime - judge[3])) {
									if (tl == null) {
										tl = timelines[i];
										for (j = 0; j < judge.length && !(ptime >= timelines[i].getTime() - judge[j]
												&& ptime <= timelines[i].getTime() + judge[j]); j++) {
										}
									} else {
										switch (judgetype) {
										case JUDGE_ALGORITHM_LR2:
											// 判定ラインより下にある判定ラインに最も近いノーツを選ぶ(LR2式)
											if (tl.getTime() < ptime - judge[3] || timelines[i].getTime() <= ptime) {
												tl = timelines[i];
												for (j = 0; j < judge.length
														&& !(ptime >= timelines[i].getTime() - judge[j]
																&& ptime <= timelines[i].getTime() + judge[j]); j++) {
												}
											}
											break;
										case JUDGE_ALGORITHM_IIDX:
											// 最も判定ラインに近いノーツを選ぶ(本家式)
											if (Math.abs(tl.getTime() - ptime) > Math
													.abs(timelines[i].getTime() - ptime)) {
												tl = timelines[i];
												for (j = 0; j < judge.length
														&& !(ptime >= timelines[i].getTime() - judge[j]
																&& ptime <= timelines[i].getTime() + judge[j]); j++) {
												}
											}
											break;
										case JUDGE_ALGORITHM_LOWEST_NOTE:
											// 最も下にあるノーツを選ぶ
											if (tl.getTime() < ptime - judge[3]) {
												tl = timelines[i];
												for (j = 0; j < judge.length
														&& !(ptime >= timelines[i].getTime() - judge[j]
																&& ptime <= timelines[i].getTime() + judge[j]); j++) {
												}
											}
											break;
										}
									}
								}
							} else {
								pos = i;
							}
						}
						if (tl != null) {
							Note note = tl.getNote(noteassign[lane]);
							if (note instanceof LongNote) {
								// ロングノート処理
								LongNote ln = (LongNote) note;
								if (ln.getStart() == tl) {
									main.play(note.getWav(), note.getStarttime());
									if (j < 2) {
										bomb[lane] = ptime;
									}
									this.update(lane, j, time, (int) (tl.getTime() - ptime));
									main.update(j);
									if (j < 4) {
										processing[lane] = ln;
										if (sc != -1) {
											// BSS処理開始
											System.out.println("BSS開始判定 - Time : " + ptime + " Judge : " + j + " KEY : "
													+ key + " LN : " + note.hashCode());
											sckey[sc] = key;

										}
									}
								}
							} else {
								main.play(note.getWav(), note.getStarttime());
								// 通常ノート処理
								if (j < 2) {
									bomb[lane] = ptime;
								}
								this.update(lane, j, time, (int) (tl.getTime() - ptime));
								main.update(j);
								if (j < 4) {
									note.setState(j + 1);
								}
								// System.out.println("打鍵:" + time +
								// " ノーツ位置 > " + tl.getTime() + " - " +
								// (lane + 1) + " : " + judgename[j]);
							}
						} else {
							Note n = null;
							boolean sound = false;
							for (TimeLine tl2 : timelines) {
								if (tl2.getNote(lane) != null) {
									n = tl2.getNote(lane);
									if (tl2.getTime() >= ptime) {
										main.play(n.getWav(), n.getStarttime());
										sound = true;
										break;
									}
								}
							}
							if (!sound && n != null) {
								main.play(n.getWav(), n.getStarttime());
							}
						}
					}
				} else {
					if (processing[lane] != null) {
						// キーが離されたときの処理
						for (int j = 0; j < judge.length; j++) {
							if (j > 3) {
								j = 4;
							}
							if (j == 4 || (ptime > processing[lane].getEnd().getTime() - judge[j]
									&& ptime < processing[lane].getEnd().getTime() + judge[j])) {
								if (sc != -1) {
									if (j != 4) {
										break;
									}
									System.out.println("BSS途中離し判定 - Time : " + ptime + " Judge : " + j + " LN : "
											+ processing[lane]);
									sckey[sc] = 0;
								}
								if (j < 2) {
									bomb[lane] = ptime;
								}
								this.update(lane, j, time, (int) (processing[lane].getEnd().getTime() - ptime));
								main.update(j);
								processing[lane].setState(j + 1);
								// System.out.println("打鍵:" + time +
								// " ノーツ位置 > " + tl.getTime() + " - " +
								// (lane + 1) + " : " + judgename[j]);
								j = judge.length;
								processing[lane] = null;
								break;
							}
						}
					}
				}
				keytime[key] = 0;
			}
			prevtime = time;
		}
		for (int lane = 0; lane < noteassign.length; lane++) {
			// 見逃しPOOR判定
			int sc = -1;
			for (int i = 0; i < sckeyassign.length; i++) {
				if (sckeyassign[i] == lane) {
					sc = i;
					break;
				}
			}
			for (int i = 0; i < timelines.length && timelines[i].getTime() < time - judge[3]; i++) {
				if (timelines[i].getTime() >= time - judge[3] - 500) {
					Note note = timelines[i].getNote(noteassign[lane]);
					if (note != null && !(note instanceof MineNote) && note.getState() == 0) {
						int judge = timelines[i].getTime() - time;
						if (note instanceof LongNote) {
							LongNote ln = (LongNote) note;
							if (ln.getStart() == timelines[i]) {
								if (processing[lane] != ln) {
									// System.out.println("ln start poor");
									this.update(lane, 4, time, judge);
									this.update(lane, 4, time, judge);
									main.update(4);
									main.update(4);
									note.setState(5);
								}
							} else {
								// System.out.println("ln end poor");
								this.update(lane, 4, time, judge);
								main.update(4);
								note.setState(5);
								processing[lane] = null;
								if (sc != -1) {
									sckey[sc] = 0;
								}
							}
						} else {
							this.update(lane, 4, time, judge);
							main.update(4);
							note.setState(5);
						}
					}
				}
			}
		}
	}

	private void update(int lane, int j, int time, int fast) {
		judgenow[lane / (noteassign.length / judgenow.length)] = j + 1;
		judgenowt[lane / (noteassign.length / judgenow.length)] = time;
		count[j][fast >= 0 ? 0 : 1]++;
		judgefast = fast;
		if (j < 3) {
			combo++;
			maxcombo = maxcombo > combo ? maxcombo : combo;
		} else if (j >= 3 && j < 5) {
			combo = 0;
			misslayer = time;
		}
		judgecombo[lane / (noteassign.length / judgenow.length)] = combo;
	}

	public int[] getJudgeNow() {
		return judgenow;
	}

	public int[] getJudgeTime() {
		return judgenowt;
	}

	public int[] getJudgeCombo() {
		return judgecombo;
	}

	public int getRecentJudgeTiming() {
		return judgefast;
	}

	public int getCombo() {
		return combo;
	}

	public long[] getBomb() {
		return bomb;
	}

	public LongNote[] getProcessingLongNotes() {
		return processing;
	}

	public LongNote[] getPassingLongNotes() {
		return passing;
	}

	public boolean[] getHellChargeJudges() {
		return inclease;
	}

	public int getMisslayer() {
		return misslayer;
	}

	public int getMaxcombo() {
		return maxcombo;
	}

	public int getJudgeCount() {
		return count[0][0] + count[0][1] + count[1][0] + count[1][1] + count[2][0] + count[2][1] + count[3][0]
				+ count[3][1] + count[4][0] + count[4][1] + count[5][0] + count[5][1];

	}

	public int getJudgeCount(int judge) {
		return count[judge][0] + count[judge][1];
	}

	public int getJudgeCount(int judge, boolean fast) {
		return fast ? count[judge][0] : count[judge][1];
	}

	public void setExpandJudge() {
		judge[0] = judge[1];
		judge[1] = judge[2];
		judge[2] = judge[3];
	}

}
