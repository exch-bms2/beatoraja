package bms.player.beatoraja.play;

import java.util.Arrays;

import bms.model.*;
import bms.player.beatoraja.TableData;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;

/**
 * ノーツ判定管理用クラス
 * 
 * @author exch
 */
public class JudgeManager {
	
	// TODO HCN押し直しの発音はどうする？

	private final BMSPlayer main;
	/**
	 * LN type
	 */
	private final int lntype;
	private final TimeLine[] timelines;

	private final JudgeAlgorithm[] judgeAlgorithms = { new JudgeAlgorithmLR2(), new JudgeAlgorithm2DX(),
			new JudgeAlgorithmLowestNote() };
	/**
	 * 判定アルゴリズム:LR2風
	 */
	public static final int JUDGE_ALGORITHM_LR2 = 0;
	/**
	 * 判定アルゴリズム:本家風
	 */
	public static final int JUDGE_ALGORITHM_IIDX = 1;
	/**
	 * 判定アルゴリズム:最下ノーツ優先判定
	 */
	public static final int JUDGE_ALGORITHM_LOWEST_NOTE = 2;
	/**
	 * 現在の判定カウント内訳
	 */
	private final int[][] count = new int[6][2];

	/**
	 * 現在のコンボ数
	 */
	private int combo;
	/**
	 * 最大コンボ数
	 */
	private int maxcombo;
	/**
	 * コース時の現在のコンボ数
	 */
	private int coursecombo;
	/**
	 * コース時の最大コンボ数
	 */
	private int coursemaxcombo;
	/**
	 * 判定差時間(ms , +は早押しで-は遅押し)
	 */
	private int judgefast;
	/**
	 * 処理中のLN
	 */
	private LongNote[] processing = new LongNote[8];
	/**
	 * 通過中のHCN
	 */
	private LongNote[] passing = new LongNote[8];
	/**
	 * HCN増加判定
	 */
	private boolean[] inclease = new boolean[8];
	private boolean[] next_inclease = new boolean[8];
	private int[] passingcount;

	private int[] keyassign;
	private int[] noteassign;

	private int[] sckeyassign;
	private int[] sckey;
	/**
	 * 各判定の範囲(+-ms)。PGREAT, GREAT, GOOD, BAD, POOR, MISS空POORの順
	 */
	// private static final int[] judgetable = {20, 60, 165, 315, 0, 1000};
	private static final int[] judgetable = { 20, 60, 160, 250, 0, 1000 };
	/**
	 * CN終端の各判定の範囲(+-ms)。PGREAT, GREAT, GOOD, BAD, POOR, MISS空POORの順
	 */
	private static final int[] cnendjudgetable = { 100, 150, 200, 250, 0, 1000 };
	/**
	 * スクラッチの各判定の範囲(+-ms)。PGREAT, GREAT, GOOD, BAD, POOR, MISS空POORの順
	 */
	private static final int[] sjudgetable = { 30, 75, 200, 300, 0, 1000 };
	/**
	 * BSS終端の各判定の範囲(+-ms)。PGREAT, GREAT, GOOD, BAD, POOR, MISS空POORの順
	 */
	private static final int[] scnendjudgetable = {100, 150, 250, 300, 0, 1000 };
	/**
	 * PMSの各判定の範囲(+-ms)。PGREAT, GREAT, GOOD, BAD, POOR, MISS空POORの順
	 */
	private static final int[] pjudgetable = { 25, 75, 175, 200, 0, 1000 };
	/**
	 * PMSのCN終端の各判定の範囲(+-ms)。PGREAT, GREAT, GOOD, BAD, POOR, MISS空POORの順
	 */
	private static final int[] pcnendjudgetable = { 100, 150, 175, 200, 0, 1000 };
	/**
	 * HCNの増減間隔(ms)
	 */
	private static final int hcnduration = 200;
	/**
	 * ノーツ判定テーブル
	 */
	private int[] njudge;
	/**
	 * CN終端判定テーブル
	 */
	private int[] cnendjudge;
	/**
	 * スクラッチ判定テーブル
	 */
	private int[] sjudge;
	private int[] scnendjudge;
	/**
	 * PMS用判定システム(空POORでコンボカット、1ノーツにつき1空POORまで)の有効/無効
	 */
	private boolean pmsjudge = false;

	private int pos = 0;
	private int judgetype = 0;

	private int prevtime;

	private void prepareAttr() {
		processing = new LongNote[noteassign.length];
		passing = new LongNote[noteassign.length];
		passingcount = new int[noteassign.length];
		inclease = new boolean[noteassign.length];
		next_inclease = new boolean[noteassign.length];
		sckey = new int[sckeyassign.length];
	}

	public JudgeManager(BMSPlayer main, BMSModel model, int[] constraint) {
		this.main = main;
		this.lntype = model.getLntype();
		this.timelines = model.getAllTimeLines();
		switch (model.getUseKeys()) {
		case 5:
		case 7:
			keyassign = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 7 };
			noteassign = new int[] { 0, 1, 2, 3, 4, 5, 6, 7 };
			sckeyassign = new int[] { 7 };
			pmsjudge = false;
			break;
		case 10:
		case 14:
			keyassign = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 7, 8, 9, 10, 11, 12, 13, 14, 15, 15 };
			noteassign = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16 };
			sckeyassign = new int[] { 7, 15 };
			pmsjudge = false;
			break;
		case 9:
			keyassign = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
			noteassign = new int[] { 0, 1, 2, 3, 4, 10, 11, 12, 13 };
			sckeyassign = new int[] {};
			pmsjudge = true;
			break;
		}
		prepareAttr();

		njudge = new int[6];
		sjudge = new int[6];
		cnendjudge = new int[6];
		scnendjudge = new int[6];
		for (int i = 0; i < judgetable.length; i++) {
			if (i < 4) {
				njudge[i] = (model.getUseKeys() == 9 ? pjudgetable[i] : judgetable[i]) * model.getJudgerank() / 100;
				sjudge[i] = sjudgetable[i] * model.getJudgerank() / 100;
				cnendjudge[i] = (model.getUseKeys() == 9 ? pcnendjudgetable[i] : cnendjudgetable[i]) * model.getJudgerank() / 100;
				scnendjudge[i] = scnendjudgetable[i] * model.getJudgerank() / 100;
			} else {
				sjudge[i] = njudge[i] = cnendjudge[i] = scnendjudge[i] = (model.getUseKeys() == 9 ? pjudgetable[i] : judgetable[i]);

			}
		}

		for (int mode : constraint) {
			if (mode == TableData.NO_GREAT) {
				setJudgeMode(NO_GREAT_JUDGE);
			}
			if (mode == TableData.NO_GOOD) {
				setJudgeMode(NO_GOOD_JUDGE);
			}
		}
	}

	public void update(final int time) {
		final BMSPlayerInputProcessor input = main.getBMSPlayerInputProcessor();
		final long[] keytime = input.getTime();
		final boolean[] keystate = input.getKeystate();

		for (int i = pos; i < timelines.length && timelines[i].getTime() <= time; i++) {
			if (timelines[i].getTime() > prevtime) {
				for (int key = 0; key < keyassign.length; key++) {
					final Note note = timelines[i].getNote(noteassign[keyassign[key]]);
					if (note == null) {
						continue;
					}
					if (note instanceof MineNote && keystate[key]) {
						final MineNote mnote = (MineNote) note;
						// 地雷ノート判定
						main.getGauge().addValue(-mnote.getDamage());
						System.out.println("Mine Damage : " + mnote.getWav());
					}
					if (note instanceof LongNote) {
						// HCN判定
						final LongNote lnote = (LongNote) note;
						if ((lnote.getType() == LongNote.TYPE_UNDEFINED && lntype == BMSModel.LNTYPE_HELLCHARGENOTE)
								|| lnote.getType() == LongNote.TYPE_HELLCHARGENOTE) {
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
			if (pos < i && timelines[i].getTime() < prevtime - njudge[5]) {
				pos = i;
				// System.out.println("judge first position : " +
				// timelines[i].getTime() + " time : " + time);
			}
		}
		// HCNゲージ増減判定
		Arrays.fill(next_inclease, false);
		for (int key = 0; key < keyassign.length; key++) {
			if (passing[keyassign[key]] != null && keystate[key]) {
				next_inclease[keyassign[key]] = true;
			}
		}
		final boolean[] b = inclease;
		inclease = next_inclease;
		next_inclease = b;

		for (int key = 0; key < keyassign.length; key++) {
			if (passing[keyassign[key]] == null) {
				continue;
			}
			if (inclease[keyassign[key]]) {
				passingcount[keyassign[key]] += (time - prevtime);
				if (passingcount[keyassign[key]] > hcnduration) {
					main.getGauge().update(1, 2f);
					// System.out.println("HCN : Gauge increase");
					passingcount[keyassign[key]] -= hcnduration;
				}
			} else {
				passingcount[keyassign[key]] -= (time - prevtime);
				if (passingcount[keyassign[key]] < -hcnduration) {
					main.getGauge().update(4, 0.5f);
					// System.out.println("HCN : Gauge decrease");
					passingcount[keyassign[key]] += hcnduration;
				}
			}
		}
		prevtime = time;

		for (int key = 0; key < keyassign.length; key++) {
			final long ptime = keytime[key];
			if (ptime == 0) {
				continue;
			}
			final int lane = keyassign[key];
			final int sc = Arrays.binarySearch(sckeyassign, lane);
			if (keystate[key]) {
				// キーが押されたときの処理
				if (processing[lane] != null) {
					if (((lntype != BMSModel.LNTYPE_LONGNOTE && processing[lane].getType() == LongNote.TYPE_UNDEFINED)
							|| processing[lane].getType() == LongNote.TYPE_CHARGENOTE || processing[lane].getType() == LongNote.TYPE_HELLCHARGENOTE)
							&& sc >= 0 && key != sckey[sc]) {
						final int[] judge = scnendjudge;
						for (int j = 0; j < judge.length; j++) {
							if (j > 3) {
								j = 4;
							}
							if (j == 4
									|| (ptime > processing[lane].getEnd().getTime() - judge[j] && ptime < processing[lane]
											.getEnd().getTime() + judge[j])) {
								final int dtime = (int) (processing[lane].getEnd().getTime() - ptime);
								this.update(lane, j < 4 ? j : 4, time, dtime);
								processing[lane].setEndstate(j + 1);
								processing[lane].setEndtime(dtime);
								// System.out.println("BSS終端判定 - Time : " +
								// ptime + " Judge : " + j + " LN : "
								// + processing[lane].hashCode());
								processing[lane] = null;
								sckey[sc] = 0;
								break;
							}
						}

					} else {
						// ここに来るのはマルチキーアサイン以外ありえないはず
					}
				} else {
					final int[] judge = sc >= 0 ? sjudge : njudge;
					// 対象ノーツの抽出
					final TimeLine tl = judgeAlgorithms[judgetype].getNote(pos, timelines, ptime, judge,
							noteassign[lane], pmsjudge);
					final int j = judgeAlgorithms[judgetype].getJudge();

					if (tl != null) {
						final Note note = tl.getNote(noteassign[lane]);
						// TODO この時点で空POOR処理を分岐させるべきか
						if (note instanceof LongNote) {
							// ロングノート処理
							final LongNote ln = (LongNote) note;
							if (ln.getStart() == tl) {
								main.play(note);
								if ((lntype == BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
										|| ln.getType() == LongNote.TYPE_LONGNOTE) {
									passingcount[lane] = (int) (tl.getTime() - ptime);
								} else {
									final int dtime = (int) (tl.getTime() - ptime);
									this.update(lane, j, time, dtime);
									if (ln.getState() == 0) {
										ln.setTime(dtime);
									}
									if (j < 4) {
										ln.setState(j + 1);
									}
								}
								if (j < 4) {
									processing[lane] = ln;
									if (sc >= 0) {
										// BSS処理開始
										// System.out.println("BSS開始判定 - Time : "
										// + ptime + " Judge : " + j
										// + " KEY : " + key + " LN : " +
										// note.hashCode());
										sckey[sc] = key;

									}
								}
							}
						} else {
							main.play(note);
							// 通常ノート処理
							final int dtime = (int) (tl.getTime() - ptime);
							this.update(lane, j, time, dtime);
							if (note.getState() == 0) {
								note.setTime(dtime);
							}
							if (j < 4) {
								note.setState(j + 1);
							}
						}
					} else {
						Note n = null;
						boolean sound = false;
						for (TimeLine tl2 : timelines) {
							final Note note = tl2.getNote(lane);
							if (note != null && !(note instanceof LongNote && note.getState() != 0)) {
								n = note;
							}
							if (tl2.getHiddenNote(lane) != null) {
								n = tl2.getHiddenNote(lane);
							}
							if (n != null && tl2.getTime() >= ptime) {
								main.play(n);
								sound = true;
								break;
							}
						}
						if (!sound && n != null) {
							main.play(n);
						}
					}
				}
			} else {
				// キーが離されたときの処理
				if (processing[lane] != null) {
					final int[] judge = sc >= 0 ? scnendjudge : cnendjudge;
					for (int j = 0; j < judge.length; j++) {
						if (j > 3) {
							j = 4;
						}
						if (j == 4
								|| (ptime > processing[lane].getEnd().getTime() - judge[j] && ptime < processing[lane]
										.getEnd().getTime() + judge[j])) {
							int dtime = (int) (processing[lane].getEnd().getTime() - ptime);
							if ((lntype != BMSModel.LNTYPE_LONGNOTE && processing[lane].getType() == LongNote.TYPE_UNDEFINED)
									|| processing[lane].getType() == LongNote.TYPE_CHARGENOTE
									|| processing[lane].getType() == LongNote.TYPE_HELLCHARGENOTE) {
								if (sc >= 0) {
									if (j != 4 || key != sckey[sc]) {
										break;
									}
									// System.out.println("BSS途中離し判定 - Time : "
									// + ptime + " Judge : " + j + " LN : "
									// + processing[lane]);
									sckey[sc] = 0;
								}
								main.stop(processing[lane]);
								this.update(lane, j, time, dtime);
								processing[lane].setEndstate(j + 1);
								processing[lane].setEndtime(dtime);
								processing[lane] = null;
							} else {
								if (Math.abs(passingcount[lane]) > Math.abs(dtime)) {
									dtime = passingcount[lane];
									for (; j < 4; j++) {
										if (Math.abs(passingcount[lane]) <= judge[j]) {
											break;
										}
									}
								}
								if(j >= 3) {
									main.stop(processing[lane]);
								}
								this.update(lane, j, time, dtime);
								processing[lane].setState(j + 1);
								processing[lane].setTime(dtime);
								processing[lane] = null;
							}
							j = judge.length;
							break;
						}
					}
				}
			}
			keytime[key] = 0;
		}

		for (int lane = 0; lane < noteassign.length; lane++) {
			final int sc = Arrays.binarySearch(sckeyassign, lane);
			final int[] judge = sc >= 0 ? sjudge : njudge;

			// LN終端判定
			if (processing[lane] != null
					&& ((lntype == BMSModel.LNTYPE_LONGNOTE && processing[lane].getType() == LongNote.TYPE_UNDEFINED) || processing[lane]
							.getType() == LongNote.TYPE_LONGNOTE) && processing[lane].getEnd().getTime() < time) {
				int j = 0;
				for (; j < judge.length; j++) {
					if (Math.abs(passingcount[lane]) <= judge[j]) {
						break;
					}
				}
				this.update(lane, j, time, passingcount[lane]);
				processing[lane].setState(j + 1);
				processing[lane].setTime(passingcount[lane]);
				processing[lane] = null;
			}
			// 見逃しPOOR判定
			for (int i = pos; i < timelines.length && timelines[i].getTime() < time - judge[3]; i++) {
				final Note note = timelines[i].getNote(noteassign[lane]);
				if (note == null) {
					continue;
				}
				final int jud = timelines[i].getTime() - time;
				if (note instanceof NormalNote && note.getState() == 0) {
					this.update(lane, 4, time, jud);
					note.setState(5);
					note.setTime(jud);
				}
				if(note instanceof LongNote) {
					final LongNote ln = (LongNote) note;
					if (((LongNote) note).getStart() == timelines[i] && note.getState() == 0) {
						if ((lntype != BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
								|| ln.getType() == LongNote.TYPE_CHARGENOTE || ln.getType() == LongNote.TYPE_HELLCHARGENOTE) {
							// System.out.println("CN start poor");
							this.update(lane, 4, time, jud);
							note.setState(5);
							note.setTime(jud);
							this.update(lane, 4, time, jud);
							((LongNote) note).setEndstate(5);
							((LongNote) note).setEndtime(jud);
						}
						if (((lntype == BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
								|| ln.getType() == LongNote.TYPE_LONGNOTE) && processing[lane] != note) {
							// System.out.println("LN start poor");
							this.update(lane, 4, time, jud);
							note.setState(5);
							note.setTime(jud);
						}

					}
					if (((lntype != BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
							|| ln.getType() == LongNote.TYPE_CHARGENOTE || ln.getType() == LongNote.TYPE_HELLCHARGENOTE)
							&& ((LongNote) note).getEnd() == timelines[i] && ((LongNote) note).getEndstate() == 0) {
						// System.out.println("CN end poor");
						this.update(lane, 4, time, jud);
						((LongNote) note).setEndstate(5);
						((LongNote) note).setEndtime(jud);
						processing[lane] = null;
						if (sc >= 0) {
							sckey[sc] = 0;
						}
					}					
				}
			}
		}
	}

	private void update(int lane, int j, int time, int fast) {
		count[j][fast >= 0 ? 0 : 1]++;
		judgefast = fast;
		if (j < 3) {
			combo++;
			maxcombo = maxcombo > combo ? maxcombo : combo;
			coursecombo++;
			coursemaxcombo = coursemaxcombo > coursecombo ? coursemaxcombo : coursecombo;
		} else if ((j >= 3 && j < 5) || (pmsjudge && j >= 3)) {
			combo = 0;
			coursecombo = 0;
		}
		main.update(lane, j, time, fast);
	}

	public int getRecentJudgeTiming() {
		return judgefast;
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

	public int getCombo() {
		return combo;
	}

	public int getCourseCombo() {
		return coursecombo;
	}

	public void setCourseCombo(int combo) {
		this.coursecombo = combo;
	}

	public int getMaxcombo() {
		return maxcombo;
	}

	public int getCourseMaxcombo() {
		return coursemaxcombo;
	}

	public void setCourseMaxcombo(int combo) {
		this.coursemaxcombo = combo;
	}

	public int getJudgeCount() {
		return count[0][0] + count[0][1] + count[1][0] + count[1][1] + count[2][0] + count[2][1] + count[3][0]
				+ count[3][1] + count[4][0] + count[4][1] + count[5][0] + count[5][1];

	}

	public int[] getJudgeTimeRegion() {
		return njudge;
	}

	public int[] getScratchJudgeTimeRegion() {
		return sjudge;
	}

	/**
	 * 指定の判定のカウント数を返す
	 * 
	 * @param judge
	 *            0:PG, 1:GR, 2:GD, 3:BD, 4:PR, 5:MS
	 * @return 判定のカウント数
	 */
	public int getJudgeCount(int judge) {
		return count[judge][0] + count[judge][1];
	}

	/**
	 * 指定の判定のカウント数を返す
	 * 
	 * @param judge
	 *            0:PG, 1:GR, 2:GD, 3:BD, 4:PR, 5:MS
	 * @param fast
	 *            true:FAST, flase:SLOW
	 * @return 判定のカウント数
	 */
	public int getJudgeCount(int judge, boolean fast) {
		return fast ? count[judge][0] : count[judge][1];
	}

	public static final int EXPAND_JUDGE = 0;
	public static final int NO_GREAT_JUDGE = 1;
	public static final int NO_GOOD_JUDGE = 2;

	public void setJudgeMode(int mode) {
		switch (mode) {
		case EXPAND_JUDGE:
			njudge[0] = njudge[1];
			njudge[1] = njudge[2];
			njudge[2] = njudge[3];
			sjudge[0] = sjudge[1];
			sjudge[1] = sjudge[2];
			sjudge[2] = sjudge[3];
			break;
		case NO_GREAT_JUDGE:
			njudge[1] = njudge[0];
			sjudge[1] = sjudge[0];
			njudge[2] = njudge[0];
			sjudge[2] = sjudge[0];
			break;
		case NO_GOOD_JUDGE:
			njudge[2] = njudge[0];
			sjudge[2] = sjudge[0];
			break;
		}
	}
}

/**
 * 判定アルゴリズム
 * 
 * @author exch
 */
abstract class JudgeAlgorithm {

	private int judge;

	public TimeLine getNote(int pos, TimeLine[] timelines, long ptime, int[] judge, int lane, boolean pmsjudge) {
		TimeLine tl = null;
		int j = 0;
		for (int i = pos; i < timelines.length && timelines[i].getTime() < ptime + judge[5]; i++) {
			if (timelines[i].getTime() >= ptime - judge[5]) {
				Note judgenote = timelines[i].getNote(lane);
				if (judgenote != null && !(judgenote instanceof MineNote)
						&& !(judgenote instanceof LongNote && ((LongNote) judgenote).getEnd() == timelines[i])) {
					if (tl == null) {
						if (!(pmsjudge && (judgenote.getState() != 0 || (judgenote.getState() == 0
								&& judgenote.getTime() != 0 && ptime <= timelines[i].getTime() - judge[2])))) {
							tl = timelines[i];
							if (judgenote.getState() != 0) {
								j = 5;
							} else {
								for (j = 0; j < judge.length
										&& !(ptime >= timelines[i].getTime() - judge[j] && ptime <= timelines[i]
												.getTime() + judge[j]); j++) {
								}
							}
						}
					} else {
						if (compare(tl, timelines[i], lane, ptime) == timelines[i]) {
							if (!(pmsjudge && (judgenote.getState() != 0 || (judgenote.getState() == 0
									&& judgenote.getTime() != 0 && ptime <= timelines[i].getTime() - judge[2])))) {
								tl = timelines[i];
								if (judgenote.getState() != 0) {
									j = 5;
								} else {
									for (j = 0; j < judge.length
											&& !(ptime >= timelines[i].getTime() - judge[j] && ptime <= timelines[i]
													.getTime() + judge[j]); j++) {
									}
								}
							}
						}
					}
				}
			}
		}
		this.judge = j;
		return tl;
	}

	public int getJudge() {
		return judge;
	}

	public abstract TimeLine compare(TimeLine t1, TimeLine t2, int lane, long ptime);
}

/**
 * 判定アルゴリズム:LR2
 * 
 * @author exch
 */
class JudgeAlgorithmLR2 extends JudgeAlgorithm {

	@Override
	public TimeLine compare(TimeLine t1, TimeLine t2, int lane, long ptime) {
		if (t1.getNote(lane).getState() != 0) {
			return t2;
		}
		if (t1.getTime() < t2.getTime() && t2.getNote(lane).getState() == 0 && t2.getTime() <= ptime) {
			return t2;
		}
		return t1;
	}
}

/**
 * 判定アルゴリズム:2DX
 * 
 * @author exch
 */
class JudgeAlgorithm2DX extends JudgeAlgorithm {

	@Override
	public TimeLine compare(TimeLine t1, TimeLine t2, int lane, long ptime) {
		if (t1.getNote(lane).getState() != 0) {
			return t2;
		}
		if (Math.abs(t1.getTime() - ptime) < Math.abs(t2.getTime() - ptime) && t2.getNote(lane).getState() == 0) {
			return t2;
		}
		return t1;
	}
}

/**
 * 判定アルゴリズム:最下ノーツ優先
 * 
 * @author exch
 */
class JudgeAlgorithmLowestNote extends JudgeAlgorithm {

	@Override
	public TimeLine compare(TimeLine t1, TimeLine t2, int lane, long ptime) {
		if (t1.getNote(lane).getState() != 0) {
			return t2;
		}
		if (t1.getNote(lane).getState() != 0 && t2.getNote(lane).getState() == 0) {
			return t2;
		}
		return t1;
	}
}