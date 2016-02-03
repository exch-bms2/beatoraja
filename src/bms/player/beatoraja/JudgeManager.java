package bms.player.beatoraja;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import bms.model.BMSModel;
import bms.model.LongNote;
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
	private int judgenow;
	/**
	 * 判定の最終更新時間
	 */
	private int judgenowt;
	private boolean judgefast;
	/**
	 * ボムの表示開始時間
	 */
	private long[] bomb = new long[8];
	/**
	 * 処理中のLN
	 */
	private LongNote[] processing = new LongNote[8];
	private int sckey;
	/**
	 * ミスレイヤー表示開始時間
	 */
	private int misslayer;

	private final int[][] judgetable = new int[][] { { 8, 24, 70, 130, 0, 1000 }, { 14, 42, 115, 220, 0, 1000 },
			{ 18, 54, 150, 285, 0, 1000 }, { 20, 60, 165, 315, 0, 1000 } };

	public JudgeManager(BMSPlayer main, BMSModel model) {
		this.main = main;
		this.model = model;
		Arrays.fill(bomb, -1000);
		if (model.getJudgerank() > 3) {
			judge = judgetable[3];
		} else {
			judge = judgetable[model.getJudgerank()];
		}
	}

	private int[] judge;

	public void update(TimeLine[] timelines, int time) {
		BMSPlayerInputProcessor input = main.getBMSPlayerInputProcessor();
		long[] keytime = input.getTime();
		boolean[] keystate = input.getKeystate();

		for (int key = 0; key < 9; key++) {
			if (keytime[key] != 0) {
				long ptime = keytime[key];
				int lane = key == 8 ? 7 : key;
				if (keystate[key]) {
					boolean sound = false;
					// キーが押されたときの処理
					for (int j = 0; j < judge.length; j++) {
						for (int i = 0; i < timelines.length && timelines[i].getTime() < ptime + judge[j]; i++) {
							if (timelines[i].getTime() >= ptime - judge[j]) {
								Note note = timelines[i].getNote(lane);
								if (note != null && note.getState() == 0) {
									main.getAudioManager().play(note.getWav());
									sound = true;
									if (note instanceof LongNote) {
										LongNote ln = (LongNote) note;
										if (ln.getStart() == timelines[i]) {
											if (j < 2) {
												bomb[lane] = ptime;
											}
											judgefast = ptime <= timelines[i].getTime();
											main.update(j, judgefast);
											this.update(j, time, judgefast);
											if (j < 4) {
												processing[lane] = ln;
												if (lane == 7) {
													// BSS処理開始
													System.out.println("BSS開始判定 - Time : " + ptime + " Judge : " + j);
													sckey = key;

												}
											}
											j = judge.length;
											break;
										}
										if (lane == 7 && key != sckey && processing[lane] != null
												&& ln.getEnd() == timelines[i]) {
											judgefast = ptime < processing[lane].getEnd().getTime();
											if (j < 2) {
												bomb[lane] = ptime;
											}
											main.update(j, judgefast);
											this.update(j < 4 ? j : 4, time, judgefast);
											processing[lane].setState(j + 1);
											// System.out.println("打鍵:" + time +
											// " ノーツ位置 > " + tl.getTime() + " -
											// " +
											// (lane + 1) + " : " +
											// judgename[j]);
											j = judge.length;
											processing[lane] = null;
											sckey = 0;
											System.out.println(
													"BSS終端判定 - Time : " + ptime + " Judge : " + (judgenow - 1));
											break;
										}
									} else {
										if (j < 2) {
											bomb[lane] = ptime;
										}
										judgefast = ptime <= timelines[i].getTime();
										main.update(j, judgefast);
										this.update(j, time, judgefast);
										if (j < 4) {
											note.setState(j + 1);
										}
										// System.out.println("打鍵:" + time +
										// " ノーツ位置 > " + tl.getTime() + " - " +
										// (lane + 1) + " : " + judgename[j]);
										j = judge.length;
										break;
									}
								}
							}
						}
					}
					if (!sound) {
						Note n = null;
						for (TimeLine tl : timelines) {
							if (tl.getNote(lane) != null) {
								n = tl.getNote(lane);
								if (tl.getTime() >= ptime) {
									main.getAudioManager().play(n.getWav());
									sound = true;
									break;
								}
							}
						}
						if (!sound && n != null) {
							main.getAudioManager().play(n.getWav());
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
								judgefast = ptime < processing[lane].getEnd().getTime();
								if (lane == 7 && j != 4) {
									break;
								}
								if (j < 2) {
									bomb[lane] = ptime;
								}
								main.update(j, judgefast);
								this.update(j, time, judgefast);
								processing[lane].setState(j + 1);
								// System.out.println("打鍵:" + time +
								// " ノーツ位置 > " + tl.getTime() + " - " +
								// (lane + 1) + " : " + judgename[j]);
								j = judge.length;
								processing[lane] = null;
								sckey = 0;
								break;
							}
						}
					}
				}
				keytime[key] = 0;
			}
		}
		for (int lane = 0; lane < 8; lane++) {
			// 見逃しPOOR判定
			for (int i = 0; i < timelines.length && timelines[i].getTime() < time - judge[3]; i++) {
				if (timelines[i].getTime() >= time - judge[3] - 500) {
					Note note = timelines[i].getNote(lane);
					if (note != null && note.getState() == 0) {
						if (note instanceof LongNote) {
							LongNote ln = (LongNote) note;
							if (ln.getStart() == timelines[i]) {
								if (processing[lane] != ln) {
									// System.out.println("ln start poor");
									main.update(4, false);
									main.update(4, false);
									this.update(4, time, false);
									judgefast = false;
									note.setState(5);
								}
							} else {
								// System.out.println("ln end poor");
								main.update(4, false);
								this.update(4, time, false);
								judgefast = false;
								note.setState(5);
								processing[lane] = null;
								sckey = 0;
							}
						} else {
							main.update(4, false);
							this.update(4, time, false);
							judgefast = false;
							note.setState(5);
						}
					}
				}
			}
		}
	}

	private void update(int j, int time, boolean fast) {
		judgenow = j + 1;
		judgenowt = time;
		count[j][fast ? 0 : 1]++;
		if (j < 3) {
			combo++;
			maxcombo = maxcombo > combo ? maxcombo : combo;
		} else if (j >= 3 && j < 5) {
			combo = 0;
			misslayer = time;
		}
	}

	public int getJudgeNow() {
		return judgenow;
	}

	public int getJudgeTime() {
		return judgenowt;
	}

	public boolean getJudgeTimingIsFast() {
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

}
