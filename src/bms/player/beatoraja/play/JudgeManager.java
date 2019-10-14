package bms.player.beatoraja.play;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Arrays;

import com.badlogic.gdx.utils.FloatArray;

import bms.model.BMSModel;
import bms.model.Lane;
import bms.model.LongNote;
import bms.model.MineNote;
import bms.model.Mode;
import bms.model.NormalNote;
import bms.model.Note;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.audio.AudioDriver;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.play.JudgeProperty.MissCondition;
import bms.player.beatoraja.skin.SkinPropertyMapper;

/**
 * ノーツ判定管理用クラス
 *
 * @author exch
 */
public class JudgeManager {

	// TODO HCN押し直しの発音はどうする？

	private final BMSPlayer main;
	private final AudioDriver keysound;
	/**
	 * LN type
	 */
	private int lntype;
	private Lane[] lanes;

	/**
	 * 現在の判定カウント内訳
	 */
	private IRScoreData score = new IRScoreData();

	/**
	 * 現在のコンボ数
	 */
	private int combo;
	/**
	 * コース時の現在のコンボ数
	 */
	private int coursecombo;
	/**
	 * コース時の最大コンボ数
	 */
	private int coursemaxcombo;
	/**
	 * 判定連動レーザーの色
	 */
	private int[][] judge;
	/**
	 * 現在表示中の判定
	 */
	private int[] judgenow;
	private int[] judgecombo;
	/**
	 * ゴースト記録用の判定ログ
	 */
	private int[] ghost;
	/**
	 * 判定差時間(ms , +は早押しで-は遅押し)
	 */
	private long[] judgefast;
	/**
	 * 処理中のLN
	 */
	private LongNote[] processing;
	/**
	 * 通過中のHCN
	 */
	private LongNote[] passing;
	/**
	 * HCN増加判定
	 */
	private boolean[] inclease = new boolean[8];
	private boolean[] next_inclease = new boolean[8];
	private int[] passingcount;

	private int[] keyassign;

	private int[] sckeyassign;
	private int[] sckey;
	private int[] offset;
	private int[] player;
	private int[][] laneassign;
	/**
	 * HCNの増減間隔(ms)
	 */
	private static final int hcnduration = 200;
	/**
	 * ノーツ判定テーブル
	 */
	private int[][] njudge;
	private long judgestart;
	private long judgeend;
	/**
	 * CN終端判定テーブル
	 */
	private int[][] cnendjudge;
	/**
	 * スクラッチ判定テーブル
	 */
	private int[][] sjudge;
	private int[][] scnendjudge;
	/**
	 * PMS用判定システム(空POORでコンボカット、1ノーツにつき1空POORまで)の有効/無効
	 */
	private boolean[] combocond;

	private MissCondition miss;
	/**
	 * 各判定毎のノートの判定を消失するかどうか。PG, GR, GD, BD, PR, MSの順
	 */
	private boolean[] judgeVanish;

	private long prevtime;

	private boolean autoplay = false;
	private long[] auto_presstime;
	/**
	 * オートプレイでキーを押下する最小時間(ms)
	 */
	private final int auto_minduration = 80;

	private JudgeAlgorithm algorithm;

	/**
	 * 処理済ノート数
	 */
	private int pastNotes = 0;

	/**
	 * PMS キャラ用 判定
	 */
	private int PMcharaJudge = 0;

	/**
	 * 直近100ノーツの判定差時間
	 */
	private long[] recentJudges = new long[100];
	/**
	 * 判定差時間のヘッド
	 */
	private int recentJudgesIndex = 0;

	public JudgeManager(BMSPlayer main) {
		this.main = main;
		this.keysound = main.main.getAudioProcessor();
	}

	public void init(BMSModel model, PlayerResource resource) {
		final Mode orgmode = resource.getOriginalMode();
		prevtime = 0;
		final int  judgeregion = main.getSkin() instanceof PlaySkin ? ((PlaySkin) main.getSkin()).getJudgeregion() : 0;
		judgenow = new int[judgeregion];
		judgecombo = new int[judgeregion];
		judgefast = new long[judgeregion];
		score = new IRScoreData(orgmode);
		score.setNotes(model.getTotalNotes());
		score.setSha256(model.getSHA256());
		ghost = new int[model.getTotalNotes()];
		for (int i=0; i<ghost.length; i++) {
			ghost[i] = 4;
		}

		this.lntype = model.getLntype();
		lanes = model.getLanes();

		algorithm = JudgeAlgorithm.valueOf(resource.getPlayerConfig().getPlayConfig(orgmode).getPlayconfig().getJudgetype());
		JudgeProperty rule = BMSPlayerRule.getBMSPlayerRule(orgmode).judge;
		combocond = rule.combo;
		miss = rule.miss;
		judgeVanish = rule.judgeVanish;
		pastNotes = 0;
		PMcharaJudge = 0;

		keyassign = main.getLaneProperty().getKeyLaneAssign();
		offset = main.getLaneProperty().getLaneSkinOffset();
		player = main.getLaneProperty().getLanePlayer();
		sckeyassign = main.getLaneProperty().getLaneScratchAssign();
		laneassign = main.getLaneProperty().getLaneKeyAssign();
		sckey = new int[model.getMode().scratchKey.length];

		judge = new int[model.getMode().player][model.getMode().key / model.getMode().player + 1];
		processing = new LongNote[sckeyassign.length];
		passing = new LongNote[sckeyassign.length];
		passingcount = new int[sckeyassign.length];
		inclease = new boolean[sckeyassign.length];
		next_inclease = new boolean[sckeyassign.length];
		auto_presstime = new long[keyassign.length];

		for (int key = 0; key < keyassign.length; key++) {
			auto_presstime[key] = Long.MIN_VALUE;
		}

		final int judgerank = model.getJudgerank();
		final int judgeWindowRate = resource.getPlayerConfig().getJudgewindowrate();
		int constraint = 2;
		for (CourseData.CourseDataConstraint mode : resource.getConstraint()) {
			if (mode == CourseData.CourseDataConstraint.NO_GREAT) {
				constraint = 0;
			} else if (mode == CourseData.CourseDataConstraint.NO_GOOD) {
				constraint = 1;
			}
		}
		njudge = rule.getNoteJudge(judgerank, judgeWindowRate, constraint);
		cnendjudge = rule.getLongNoteEndJudge(judgerank, judgeWindowRate, constraint);
		sjudge = rule.getScratchJudge(judgerank, judgeWindowRate, constraint);
		scnendjudge = rule.getLongScratchEndJudge(judgerank, judgeWindowRate, constraint);
		judgestart = judgeend = 0;
		for (int[] i : njudge) {
			judgestart = Math.min(judgestart, i[0]);
			judgeend = Math.max(judgeend, i[1]);
		}
		for (int[] i : sjudge) {
			judgestart = Math.min(judgestart, i[0]);
			judgeend = Math.max(judgeend, i[1]);
		}

		this.autoplay = resource.getPlayMode().isAutoPlayMode();

		FloatArray[] f = resource.getGauge();
		if (f != null) {
			setCourseCombo(resource.getCombo());
			setCourseMaxcombo(resource.getMaxcombo());
		}

		Arrays.fill(recentJudges, Long.MIN_VALUE);
		this.recentJudgesIndex = 0;
	}

	public void update(final long time) {
		final MainController mc = main.main;
		final BMSPlayerInputProcessor input = mc.getInputProcessor();
		final Config config = mc.getPlayerResource().getConfig();
		final long[] keytime = input.getTime();
		final boolean[] keystate = input.getKeystate();
		final long now = mc.getNowTime();
		// 通過系の判定
		Arrays.fill(next_inclease, false);

		for (int lane = 0; lane < laneassign.length; lane++) {
			final Lane lanemodel = lanes[lane];
			lanemodel.mark((int) (prevtime + judgestart - 100));
			boolean pressed = false;
			for (int key : laneassign[lane]) {
				if (keystate[key]) {
					pressed = true;
					break;
				}
			}
			for (Note note = lanemodel.getNote(); note != null && note.getTime() <= time; note = lanemodel.getNote()) {
				if (note.getTime() <= prevtime) {
					continue;
				}
				if (note instanceof LongNote) {
					// HCN判定
					final LongNote lnote = (LongNote) note;
					if ((lnote.getType() == LongNote.TYPE_UNDEFINED && lntype == BMSModel.LNTYPE_HELLCHARGENOTE)
							|| lnote.getType() == LongNote.TYPE_HELLCHARGENOTE) {
						if (lnote.isEnd()) {
							passing[lane] = null;
							passingcount[lane] = 0;
						} else {
							passing[lane] = lnote;
						}
					}
				} else if (note instanceof MineNote && pressed) {
					final MineNote mnote = (MineNote) note;
					// 地雷ノート判定
					main.getGauge().addValue(-mnote.getDamage());
					System.out.println("Mine Damage : " + mnote.getWav());
				}

				if (autoplay) {
					// ここにオートプレイ処理を入れる
					if (note instanceof NormalNote && note.getState() == 0) {
						auto_presstime[laneassign[lane][0]] = now;
						keysound.play(note, config.getKeyvolume(), 0);
						this.update(lane, note, time, 0, 0);
					}
					if (note instanceof LongNote) {
						final LongNote ln = (LongNote) note;
						if (!ln.isEnd() && ln.getState() == 0 && processing[lane] == null) {
							auto_presstime[laneassign[lane][0]] = now;
							keysound.play(note, config.getKeyvolume(), 0);
							if ((lntype == BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
									|| ln.getType() == LongNote.TYPE_LONGNOTE) {
								passingcount[lane] = 0;
								//LN時のレーザー色変更処理
								this.judge[player[lane]][offset[lane]] = 8;
							} else {
								this.update(lane, ln, time, 0, 0);
							}
							processing[lane] = ln.getPair();
						}
						if (ln.isEnd() && ln.getState() == 0) {
							if ((lntype != BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
									|| ln.getType() == LongNote.TYPE_CHARGENOTE
									|| ln.getType() == LongNote.TYPE_HELLCHARGENOTE) {
								if (sckeyassign[lane] >= 0 && laneassign[lane].length >= 2) {
									auto_presstime[laneassign[lane][0]] = Long.MIN_VALUE;
									auto_presstime[laneassign[lane][1]] = now;
								}
								this.update(lane, ln, time, 0, 0);
								keysound.play(processing[lane], config.getKeyvolume(), 0);
								processing[lane] = null;
							}
						}
					}
				}
			}
			// HCNゲージ増減判定
			if (passing[lane] != null
					&& (pressed || (passing[lane].getPair().getState() > 0 && passing[lane].getPair().getState() <= 3)
							|| autoplay)) {
				next_inclease[lane] = true;
			}

			if (autoplay) {
				for (int key : laneassign[lane]) {
					if (auto_presstime[key] != Long.MIN_VALUE && now - auto_presstime[key] > auto_minduration
							&& processing[lane] == null) {
						auto_presstime[key] = Long.MIN_VALUE;
					}
				}
			}
		}

		final boolean[] b = inclease;
		inclease = next_inclease;
		next_inclease = b;

		for (int lane = 0; lane < passing.length; lane++) {
			final int offset = main.getLaneProperty().getLaneSkinOffset()[lane];
			final int timerActive = SkinPropertyMapper.hcnActiveTimerId(main.getLaneProperty().getLanePlayer()[lane],
					offset);
			final int timerDamage = SkinPropertyMapper.hcnDamageTimerId(main.getLaneProperty().getLanePlayer()[lane],
					offset);

			if (passing[lane] == null || passing[lane].getState() == 0) {
				mc.setTimerOff(timerActive);
				mc.setTimerOff(timerDamage);
				continue;
			}

			if (inclease[lane]) {
				passingcount[lane] += (time - prevtime);
				if (passingcount[lane] > hcnduration) {
					main.getGauge().update(1, 0.5f);
					// System.out.println("HCN : Gauge increase");
					passingcount[lane] -= hcnduration;
				}
				mc.switchTimer(timerActive, true);
				mc.setTimerOff(timerDamage);
			} else {
				passingcount[lane] -= (time - prevtime);
				if (passingcount[lane] < -hcnduration) {
					main.getGauge().update(3, 0.5f);
					// System.out.println("HCN : Gauge decrease");
					passingcount[lane] += hcnduration;
				}
				mc.setTimerOff(timerActive);
				mc.switchTimer(timerDamage, true);
			}
		}
		prevtime = time;

		for (int key = 0; key < keyassign.length; key++) {
			final int lane = keyassign[key];
			if (lane == -1) {
				continue;
			}
			final long ptime = keytime[key];
			if (ptime == 0) {
				continue;
			}
			final Lane lanemodel = lanes[lane];
			lanemodel.reset();
			final int sc = sckeyassign[lane];
			if (keystate[key]) {
				// キーが押されたときの処理
				if (processing[lane] != null) {
					// BSS終端処理
					if (((lntype != BMSModel.LNTYPE_LONGNOTE && processing[lane].getType() == LongNote.TYPE_UNDEFINED)
							|| processing[lane].getType() == LongNote.TYPE_CHARGENOTE
							|| processing[lane].getType() == LongNote.TYPE_HELLCHARGENOTE) && sc >= 0
							&& key != sckey[sc]) {
						final int[][] judge = scnendjudge;
						final int dtime = (int) (processing[lane].getTime() - ptime);
						int j = 0;
						for (; j < judge.length && !(dtime >= judge[j][0] && dtime <= judge[j][1]); j++)
							;

						keysound.play(processing[lane], config.getKeyvolume(), 0);
						this.update(lane, processing[lane], time, j, dtime);
						//						 System.out.println("BSS終端判定 - Time : " + ptime + " Judge : " + j + " LN : " + processing[lane].hashCode());
						processing[lane] = null;
						sckey[sc] = 0;
					} else {
						// ここに来るのはマルチキーアサイン以外ありえないはず
					}
				} else {
					final int[][] judge = sc >= 0 ? sjudge : njudge;
					// 対象ノーツの抽出
					lanemodel.reset();
					Note tnote = null;
					int j = 0;
					for (Note judgenote = lanemodel.getNote(); judgenote != null; judgenote = lanemodel.getNote()) {
						final long dtime = judgenote.getTime() - ptime;
						if (dtime >= judgeend) {
							break;
						}
						if (dtime < judgestart) {
							continue;
						}
						if (judgenote instanceof MineNote || (judgenote instanceof LongNote
								&& ((LongNote) judgenote).isEnd())) {
							continue;
						}
						if (tnote == null || tnote.getState() != 0
								|| algorithm.compare(tnote, judgenote, ptime, judge)) {
							if (!(miss == MissCondition.ONE && (judgenote.getState() != 0
									|| (judgenote.getState() == 0 && judgenote.getPlayTime() != 0
											&& (dtime > judge[2][1] || dtime < judge[2][0]))))) {
								if (judgenote.getState() != 0) {
									j = (dtime >= judge[4][0] && dtime <= judge[4][1]) ? 5 : 6;
								} else {
									for (j = 0; j < judge.length
											&& !(dtime >= judge[j][0] && dtime <= judge[j][1]); j++) {
									}
									j = (j >= 4 ? j + 1 : j);
								}
								
								if(j < 6) {
									if (j < 6 && (j < 4 || tnote == null
											|| Math.abs(tnote.getTime() - ptime) > Math.abs(judgenote.getTime() - ptime))) {
										tnote = judgenote;
									}									
								} else {
									tnote = null;
								}
							}
						}
					}

					if (tnote != null) {
						// TODO この時点で空POOR処理を分岐させるべきか
						if (tnote instanceof LongNote) {
							// ロングノート処理
							final LongNote ln = (LongNote) tnote;
							keysound.play(tnote, config.getKeyvolume(), 0);
							if (((lntype == BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
									|| ln.getType() == LongNote.TYPE_LONGNOTE)
									&& j < 4) {
								passingcount[lane] = (int) (tnote.getTime() - ptime);
								//LN時のレーザー色変更処理
								this.judge[player[lane]][offset[lane]] = 8;
							} else {
								final int dtime = (int) (tnote.getTime() - ptime);
								this.update(lane, ln, time, j, dtime);
							}
							if (j < 4) {
								processing[lane] = ln.getPair();
								if (sc >= 0) {
									// BSS処理開始
									//									 System.out.println("BSS開始判定 - Time : " + ptime + " Judge : " + j + " KEY : " + key + " LN : " + ln.getPair().hashCode());
									sckey[sc] = key;
								}
							}
						} else {
							keysound.play(tnote, config.getKeyvolume(), 0);
							// 通常ノート処理
							final int dtime = (int) (tnote.getTime() - ptime);
							this.update(lane, tnote, time, j, dtime);
						}
					} else {
						// 空POOR判定がないときのレーザー色変更処理
						this.judge[player[lane]][offset[lane]] = 0;

						// 空POOR判定がないときのキー音処理
						final Note[] notes = lanemodel.getNotes();
						Note n = notes.length > 0 ? notes[0] : null;
						for (Note note : lanemodel.getHiddens()) {
							if (note.getTime() >= ptime) {
								break;
							}
							n = note;
						}

						for (Note note : notes) {
							if (note.getTime() >= ptime) {
								break;
							}
							if ((n == null || n.getTime() <= note.getTime())
									&& !(note instanceof LongNote && note.getState() != 0)) {
								n = note;
							}
						}

						if (n != null) {
							keysound.play(n, config.getKeyvolume(), 0);
						}
					}
				}
				main.getKeyinput().inputKeyOn(lane);
			} else {
				// キーが離されたときの処理
				if (processing[lane] != null) {
					final int[][] judge = sc >= 0 ? scnendjudge : cnendjudge;
					int dtime = (int) (processing[lane].getTime() - ptime);
					int j = 0;
					for (; j < judge.length && !(dtime >= judge[j][0] && dtime <= judge[j][1]); j++)
						;

					if ((lntype != BMSModel.LNTYPE_LONGNOTE
							&& processing[lane].getType() == LongNote.TYPE_UNDEFINED)
							|| processing[lane].getType() == LongNote.TYPE_CHARGENOTE
							|| processing[lane].getType() == LongNote.TYPE_HELLCHARGENOTE) {
						// CN, HCN離し処理
						boolean release = true;
						if (sc >= 0) {
							if (j != 4 || key != sckey[sc]) {
								release = false;
							} else {
								//								 System.out.println("BSS途中離し判定 - Time : " + ptime + " Judge : " + j + " LN : " + processing[lane]);
								sckey[sc] = 0;
							}
						}
						if (release) {
							if (j >= 3) {
								keysound.stop(processing[lane].getPair());
							}
							this.update(lane, processing[lane], time, j, dtime);
							keysound.play(processing[lane], config.getKeyvolume(), 0);
							processing[lane] = null;
						}
					} else {
						boolean release = true;
						if (sc >= 0) {
							if (key != sckey[sc]) {
								release = false;
							} else {
								//								 System.out.println("BSS途中離し判定 - Time : " + ptime + " Judge : " + j + " LN : " + processing[lane]);
								sckey[sc] = 0;
							}
						}
						if (release) {
							// LN離し処理
							if (Math.abs(passingcount[lane]) > Math.abs(dtime)) {
								dtime = passingcount[lane];
								for (; j < judge.length && !(dtime >= judge[j][0] && dtime <= judge[j][1]); j++)
									;
							}
							if (j >= 3) {
								keysound.stop(processing[lane].getPair());
							}
							this.update(lane, processing[lane].getPair(), time, j, dtime);
							keysound.play(processing[lane], config.getKeyvolume(), 0);
							processing[lane] = null;
//							System.out.println("LN途中離し判定 - Time : " + ptime + " Judge : " + j + " LN : " + processing[lane]);	
						}
					}
				}
			}
			keytime[key] = 0;
		}

		for (int lane = 0; lane < sckeyassign.length; lane++) {
			final int sc = sckeyassign[lane];
			final int[][] judge = sc >= 0 ? sjudge : njudge;

			// LN終端判定
			if (processing[lane] != null
					&& ((lntype == BMSModel.LNTYPE_LONGNOTE && processing[lane].getType() == LongNote.TYPE_UNDEFINED)
							|| processing[lane].getType() == LongNote.TYPE_LONGNOTE)
					&& processing[lane].getTime() < time) {
				int j = 0;
				for (; j < judge.length; j++) {
					if (passingcount[lane] >= judge[j][0] && passingcount[lane] <= judge[j][1]) {
						break;
					}
				}
				this.update(lane, processing[lane].getPair(), time, j, passingcount[lane]);
				keysound.play(processing[lane], config.getKeyvolume(), 0);
				processing[lane] = null;
			}
			// 見逃しPOOR判定
			final Lane lanemodel = lanes[lane];
			lanemodel.reset();
			for (Note note = lanemodel.getNote(); note != null
					&& note.getTime() < time + judge[3][0]; note = lanemodel.getNote()) {
				final long jud = note.getTime() - time;
				if (note instanceof NormalNote && note.getState() == 0) {
					this.update(lane, note, time, 4, jud);
				} else if (note instanceof LongNote) {
					final LongNote ln = (LongNote) note;
					if (!ln.isEnd() && note.getState() == 0) {
						if ((lntype != BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
								|| ln.getType() == LongNote.TYPE_CHARGENOTE
								|| ln.getType() == LongNote.TYPE_HELLCHARGENOTE) {
							// System.out.println("CN start poor");
							this.update(lane, note, time, 4, jud);
							this.update(lane, ((LongNote) note).getPair(), time, 4, jud);
						}
						if (((lntype == BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
								|| ln.getType() == LongNote.TYPE_LONGNOTE) && processing[lane] != ln.getPair()) {
							// System.out.println("LN start poor");
							this.update(lane, note, time, 4, jud);
						}

					}
					if (((lntype != BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
							|| ln.getType() == LongNote.TYPE_CHARGENOTE || ln.getType() == LongNote.TYPE_HELLCHARGENOTE)
							&& ((LongNote) note).isEnd() && ((LongNote) note).getState() == 0) {
						// System.out.println("CN end poor");
						this.update(lane, ((LongNote) note), time, 4, jud);
						processing[lane] = null;
						if (sc >= 0) {
							sckey[sc] = 0;
						}
					}
				}
			}
			// LN処理タイマー
			// TODO processing値の変化のときのみ実行したい
			// TODO HCNは別タイマーにするかも
			mc.switchTimer(SkinPropertyMapper.holdTimerId(player[lane], offset[lane]),
					processing[lane] != null || (passing[lane] != null && inclease[lane]));
		}
	}

	private final int[] JUDGE_TIMER = { TIMER_JUDGE_1P, TIMER_JUDGE_2P, TIMER_JUDGE_3P };
	private final int[] COMBO_TIMER = { TIMER_COMBO_1P, TIMER_COMBO_2P, TIMER_COMBO_3P };

	private void update(int lane, Note n, long time, int judge, long fast) {
		if (judgeVanish[judge]) {
			if (pastNotes < ghost.length) {
				ghost[pastNotes] = judge;
			}
			n.setState(judge + 1);
			pastNotes++;
		}
		if (miss == MissCondition.ONE && judge == 4 && n.getPlayTime() != 0) {
			main.setPastNotes(pastNotes);
			return;
		}
		n.setPlayTime(fast);
		score.addJudgeCount(judge, fast >= 0, 1);

		if (judge < 4) {
			if (recentJudgesIndex == recentJudges.length - 1) {
				recentJudgesIndex = 0;
			} else {
				recentJudgesIndex++;
			}
			recentJudges[recentJudgesIndex] = fast;
		}

		if (combocond[judge] && judge < 5) {
			combo++;
			score.setCombo(Math.max(score.getCombo(), combo));
			coursecombo++;
			coursemaxcombo = coursemaxcombo > coursecombo ? coursemaxcombo : coursecombo;
		}
		if (!combocond[judge]) {
			combo = 0;
			coursecombo = 0;
		}

		if (judge != 4)
			this.judge[player[lane]][offset[lane]] = judge == 0 ? 1 : judge * 2 + (fast > 0 ? 0 : 1);
		if (judge <= ((PlaySkin) main.getSkin()).getJudgetimer()) {
			main.main.setTimerOn(SkinPropertyMapper.bombTimerId(player[lane], offset[lane]));
		}
		PMcharaJudge = judge + 1;

		final int lanelength = sckeyassign.length;
		if (judgenow.length > 0) {
			main.main.setTimerOn(JUDGE_TIMER[lane / (lanelength / judgenow.length)]);
			if (judgenow.length >= 3) {
				for (int i = 0; i < COMBO_TIMER.length; i++) {
					if (i != lane / (lanelength / judgenow.length))
						main.main.setTimerOff(COMBO_TIMER[i]);
				}
			}
			main.main.setTimerOn(COMBO_TIMER[lane / (lanelength / judgenow.length)]);
			judgenow[lane / (lanelength / judgenow.length)] = judge + 1;
			judgecombo[lane / (lanelength / judgenow.length)] = main.getJudgeManager().getCourseCombo();
			judgefast[lane / (lanelength / judgenow.length)] = fast;
		}
		main.update(judge, time);
		keysound.play(judge, fast >= 0);
	}

	public long[] getRecentJudges() {
		return recentJudges;
	}

	public int getRecentJudgesIndex() {
		return recentJudgesIndex;
	}

	public long[] getRecentJudgeTiming() {
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

	public long[] getAutoPresstime() {
		return auto_presstime;
	}

	/**
	 * 現在の1曲内のコンボ数を取得する
	 *
	 * @return 現在のコンボ数
	 */
	public int getCombo() {
		return combo;
	}

	/**
	 * 現在のコース内のコンボ数を取得する
	 *
	 * @return 現在のコンボ数
	 */
	public int getCourseCombo() {
		return coursecombo;
	}

	public void setCourseCombo(int combo) {
		this.coursecombo = combo;
	}

	public int getCourseMaxcombo() {
		return coursemaxcombo;
	}

	public void setCourseMaxcombo(int combo) {
		this.coursemaxcombo = combo;
	}

	public int[][] getJudgeTimeRegion(int lane) {
		return sckeyassign[lane] >= 0 ? sjudge : njudge;
	}

	public IRScoreData getScoreData() {
		return score;
	}

	/**
	 * 指定の判定のカウント数を返す
	 *
	 * @param judge
	 *            0:PG, 1:GR, 2:GD, 3:BD, 4:PR, 5:MS
	 * @return 判定のカウント数
	 */
	public int getJudgeCount(int judge) {
		return score.getJudgeCount(judge);
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
		return score.getJudgeCount(judge, fast);
	}

	public int getJudge(int timerId) {
		int player = SkinPropertyMapper.getKeyJudgeValuePlayer(timerId);
		int offset = SkinPropertyMapper.getKeyJudgeValueOffset(timerId);
		if (player >= judge.length || offset >= judge[player].length)
			return -1;
		return judge[player][offset];
	}

	public int[] getNowJudge() {
		return judgenow;
	}

	public int[] getNowCombo() {
		return judgecombo;
	}

	public int[][] getJudgeTable(boolean sc) {
		return sc ? sjudge : njudge;
	}

	public int getPastNotes() {
		return pastNotes;
	}

	public int getPMcharaJudge() {
		return PMcharaJudge;
	}

	public int[] getGhost() {
		return ghost;
	}
}
