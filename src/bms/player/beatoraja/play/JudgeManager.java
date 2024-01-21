package bms.player.beatoraja.play;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Arrays;
import java.util.stream.IntStream;

import com.badlogic.gdx.utils.FloatArray;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.audio.AudioDriver;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.play.JudgeProperty.MissCondition;
import bms.player.beatoraja.play.JudgeProperty.NoteType;
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

	/**
	 * 現在の判定カウント内訳
	 */
	private ScoreData score = new ScoreData();

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
	
	private long[] mjudgefast;
	
	private LaneState[] states;

	private int[] keyassign;

	private int[] sckey;
	/**
	 * HCNの増減間隔(ms)
	 */
	private static final int hcnmduration = 200000;
	/**
	 * ノーツ判定テーブル
	 */
	private long[][] nmjudge;
	private long mjudgestart;
	private long mjudgeend;
	/**
	 * CN終端判定テーブル
	 */
	private long[][] cnendmjudge;
	private long nreleasemargin;
	/**
	 * スクラッチ判定テーブル
	 */
	private long[][] smjudge;
	private long[][] scnendmjudge;
	private long sreleasemargin;
	/**
	 * PMS用判定システム(空POORでコンボカット、1ノーツにつき1空POORまで)の有効/無効
	 */
	private boolean[] combocond;

	private MissCondition miss;
	/**
	 * 各判定毎のノートの判定を消失するかどうか。PG, GR, GD, BD, PR, MSの順
	 */
	private boolean[] judgeVanish;

	private long prevmtime;

	private boolean autoplay = false;
	private long[] auto_presstime;
	/**
	 * オートプレイでキーを押下する最小時間(ms)
	 */
	private final int auto_minduration = 80;

	private JudgeAlgorithm algorithm;

	/**
	 * 直近100ノーツの判定差時間
	 */
	private long[] recentJudges = new long[100];
	private long[] microrecentJudges = new long[100];
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
		prevmtime = 0;
		final int  judgeregion = main.getSkin() instanceof PlaySkin ? ((PlaySkin) main.getSkin()).getJudgeregion() : 0;
		judgenow = new int[judgeregion];
		judgecombo = new int[judgeregion];
		judgefast = new long[judgeregion];
		mjudgefast = new long[judgeregion];
		score = new ScoreData(orgmode);
		score.setNotes(model.getTotalNotes());
		score.setSha256(model.getSHA256());
		ghost = new int[model.getTotalNotes()];
		for (int i=0; i<ghost.length; i++) {
			ghost[i] = 4;
		}

		this.lntype = model.getLntype();
		Lane[] lanes = model.getLanes();

		algorithm = JudgeAlgorithm.valueOf(resource.getPlayerConfig().getPlayConfig(orgmode).getPlayconfig().getJudgetype());
		JudgeProperty rule = BMSPlayerRule.getBMSPlayerRule(orgmode).judge;
		score.setJudgeAlgorithm(algorithm);
		score.setRule(BMSPlayerRule.getBMSPlayerRule(orgmode));
		
		combocond = rule.combo;
		miss = rule.miss;
		judgeVanish = rule.judgeVanish;

		keyassign = main.getLaneProperty().getKeyLaneAssign();
		sckey = new int[model.getMode().scratchKey.length];

		judge = new int[model.getMode().player][model.getMode().key / model.getMode().player + 1];
		auto_presstime = new long[keyassign.length];

		states = IntStream.range(0, lanes.length).mapToObj(i -> new LaneState(i, main.getLaneProperty(), lanes[i])).toArray(LaneState[]::new);
		
		for (int key = 0; key < keyassign.length; key++) {
			auto_presstime[key] = Long.MIN_VALUE;
		}

		final int judgerank = model.getJudgerank();
		final PlayerConfig config = resource.getPlayerConfig();
		final int[] keyJudgeWindowRate = config.isCustomJudge()
				? new int[]{config.getKeyJudgeWindowRatePerfectGreat(), config.getKeyJudgeWindowRateGreat(), config.getKeyJudgeWindowRateGood()}
				: new int[]{100, 100, 100};
		final int[] scratchJudgeWindowRate = config.isCustomJudge()
				? new int[]{config.getScratchJudgeWindowRatePerfectGreat(), config.getScratchJudgeWindowRateGreat(), config.getScratchJudgeWindowRateGood()}
				: new int[]{100, 100, 100};
		for (CourseData.CourseDataConstraint mode : resource.getConstraint()) {
			if (mode == CourseData.CourseDataConstraint.NO_GREAT) {
				keyJudgeWindowRate[1] = keyJudgeWindowRate[2] = 0;
				scratchJudgeWindowRate[1] = scratchJudgeWindowRate[2] = 0;
			} else if (mode == CourseData.CourseDataConstraint.NO_GOOD) {
				keyJudgeWindowRate[2] = 0;
				scratchJudgeWindowRate[2] = 0;
			}
		}
		
		nmjudge = rule.getJudge(NoteType.NOTE, judgerank, keyJudgeWindowRate);
		cnendmjudge = rule.getJudge(NoteType.LONGNOTE_END, judgerank, keyJudgeWindowRate);
		nreleasemargin = rule.longnoteMargin;
		smjudge = rule.getJudge(NoteType.SCRATCH, judgerank, scratchJudgeWindowRate);
		scnendmjudge = rule.getJudge(NoteType.LONGSCRATCH_END, judgerank, scratchJudgeWindowRate);
		sreleasemargin = rule.longscratchMargin;
		mjudgestart = mjudgeend = 0;
		for (long[] l : nmjudge) {
			mjudgestart = Math.min(mjudgestart, l[0]);
			mjudgeend = Math.max(mjudgeend, l[1]);
		}
		for (long[] l : smjudge) {
			mjudgestart = Math.min(mjudgestart, l[0]);
			mjudgeend = Math.max(mjudgeend, l[1]);
		}

		this.autoplay = resource.getPlayMode().mode == BMSPlayerMode.Mode.AUTOPLAY;

		FloatArray[] f = resource.getGauge();
		if (f != null) {
			setCourseCombo(resource.getCombo());
			setCourseMaxcombo(resource.getMaxcombo());
		}

		Arrays.fill(recentJudges, Long.MIN_VALUE);
		this.recentJudgesIndex = 0;
	}

	public void update(final long mtime) {
		final MainController mc = main.main;
		final TimerManager timer = main.timer;
		final BMSPlayerInputProcessor input = mc.getInputProcessor();
		final Config config = main.resource.getConfig();
		final long now = timer.getNowTime();
		// 通過系の判定
		for (LaneState state : states) {
			state.lanemodel.mark((int) ((prevmtime + mjudgestart - 100000) / 1000));
			boolean next_inclease = false;
			boolean pressed = false;
			for (int key : state.laneassign) {
				if (input.getKeyState(key)) {
					pressed = true;
					break;
				}
			}
			for (Note note = state.lanemodel.getNote(); note != null && note.getMicroTime() <= mtime; note = state.lanemodel.getNote()) {
				if (note.getMicroTime() <= prevmtime) {
					continue;
				}
				if (note instanceof LongNote) {
					// HCN判定
					final LongNote lnote = (LongNote) note;
					if ((lnote.getType() == LongNote.TYPE_UNDEFINED && lntype == BMSModel.LNTYPE_HELLCHARGENOTE)
							|| lnote.getType() == LongNote.TYPE_HELLCHARGENOTE) {
						if (lnote.isEnd()) {
							state.passing = null;
							state.mpassingcount = 0;
						} else {
							state.passing = lnote;
						}
					}
				} else if (note instanceof MineNote && pressed) {
					final MineNote mnote = (MineNote) note;
					// 地雷ノート判定
					main.getGauge().addValue((float) -mnote.getDamage());
//					System.out.println("Mine Damage : " + (float) mnote.getDamage());
					keysound.play(note, config.getAudioConfig().getKeyvolume(), 0);
				}

				if (autoplay) {
					// ここにオートプレイ処理を入れる
					if (note instanceof NormalNote && note.getState() == 0) {
						auto_presstime[state.laneassign[0]] = now;
						keysound.play(note, config.getAudioConfig().getKeyvolume(), 0);
						this.updateMicro(state, note, mtime, 0, 0, true);
					}
					if (note instanceof LongNote) {
						final LongNote ln = (LongNote) note;
						if (!ln.isEnd() && ln.getState() == 0 && state.processing == null) {
							auto_presstime[state.laneassign[0]] = now;
							keysound.play(note, config.getAudioConfig().getKeyvolume(), 0);
							if ((lntype == BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
									|| ln.getType() == LongNote.TYPE_LONGNOTE) {
								state.mpassingcount = 0;
								//LN時のレーザー色変更処理
								this.judge[state.player][state.offset] = 8;
							} else {
								this.updateMicro(state, ln, mtime, 0, 0, true);
							}
							state.processing = ln.getPair();
						}
						if (ln.isEnd() && ln.getState() == 0) {
							if ((lntype != BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
									|| ln.getType() == LongNote.TYPE_CHARGENOTE
									|| ln.getType() == LongNote.TYPE_HELLCHARGENOTE) {
								if (state.sckey >= 0 && state.laneassign.length >= 2) {
									auto_presstime[state.laneassign[0]] = Long.MIN_VALUE;
									auto_presstime[state.laneassign[1]] = now;
								}
								this.updateMicro(state, ln, mtime, 0, 0, true);
								keysound.play(state.processing, config.getAudioConfig().getKeyvolume(), 0);
								state.processing = null;
							}
						}
					}
				}
			}
			// HCNゲージ増減判定
			if (state.passing != null
					&& (pressed || (state.passing.getPair().getState() > 0 && state.passing.getPair().getState() <= 3)
							|| autoplay)) {
				next_inclease = true;
			}

			if (autoplay) {
				for (int key : state.laneassign) {
					if (auto_presstime[key] != Long.MIN_VALUE && now - auto_presstime[key] > auto_minduration
							&& state.processing == null) {
						auto_presstime[key] = Long.MIN_VALUE;
					}
				}
			}
			state.inclease = next_inclease;
		}

		for (LaneState state : states) {
			if (state.passing == null || state.passing.getState() == 0) {
				timer.setTimerOff(state.timerActive);
				timer.setTimerOff(state.timerDamage);
				continue;
			}

			if (state.inclease) {
				state.mpassingcount += (mtime - prevmtime);
				if (state.mpassingcount > hcnmduration) {
					main.getGauge().update(1, 0.5f);
//					 System.out.println("HCN : Gauge increase");
					state.mpassingcount -= hcnmduration;
				}
				timer.switchTimer(state.timerActive, true);
				timer.setTimerOff(state.timerDamage);
				if(state.passing.getPair().getState() > 3) {
					keysound.setVolume(state.passing, config.getAudioConfig().getKeyvolume());
				}
			} else {
				state.mpassingcount -= (mtime - prevmtime);
				if (state.mpassingcount < -hcnmduration) {
					main.getGauge().update(3, 0.5f);
//					 System.out.println("HCN : Gauge decrease");
					state.mpassingcount += hcnmduration;
				}
				timer.setTimerOff(state.timerActive);
				timer.switchTimer(state.timerDamage, true);
				if(state.passing.getPair().getState() > 3) {
					keysound.setVolume(state.passing, 0.0f);
				}
			}
		}
		prevmtime = mtime;

		for (int key = 0; key < keyassign.length; key++) {
			final int lane = keyassign[key];
			if (lane == -1) {
				continue;
			}
			final long pmtime = input.getKeyChangedTime(key);
			if (pmtime == Long.MIN_VALUE) {
				continue;
			}
			final LaneState state = states[lane];
			state.lanemodel.reset();
			final int sc = state.sckey;
			if (input.getKeyState(key)) {
				// キーが押されたときの処理
				if (state.processing != null) {
					if (((lntype != BMSModel.LNTYPE_LONGNOTE && state.processing.getType() == LongNote.TYPE_UNDEFINED)
							|| state.processing.getType() == LongNote.TYPE_CHARGENOTE
							|| state.processing.getType() == LongNote.TYPE_HELLCHARGENOTE)) {
						if(sc >= 0 && key != sckey[sc]) {
							// BSS終端処理
							final long[][] mjudge = scnendmjudge;
							final long dmtime = state.processing.getMicroTime() - pmtime;
							int j = 0;
							for (; j < mjudge.length && !(dmtime >= mjudge[j][0] && dmtime <= mjudge[j][1]); j++)
								;

							keysound.play(state.processing, config.getAudioConfig().getKeyvolume(), 0);
							this.updateMicro(state, state.processing, mtime, j, dmtime, true);
//							 System.out.println("BSS終端判定 - Time : " + ptime + " Judge : " + j + " LN : " + processing[lane].hashCode());
							state.processing = null;
							state.releasetime = Long.MIN_VALUE;
							state.lnendJudge = Integer.MIN_VALUE;
							sckey[sc] = 0;							
						} else {
							// 押し直し処理
							state.releasetime = Long.MIN_VALUE;
						}
					} else {
						// 押し直し処理
						state.releasetime = Long.MIN_VALUE;
					}
				} else {
					final long[][] mjudge = sc >= 0 ? smjudge : nmjudge;
					// 対象ノーツの抽出
					state.lanemodel.reset();
					Note tnote = null;
					int judge = 0;
					for (Note judgenote = state.lanemodel.getNote(); judgenote != null; judgenote = state.lanemodel.getNote()) {
						final long dmtime = judgenote.getMicroTime() - pmtime;
						if (dmtime >= mjudgeend) {
							break;
						}
						if (dmtime < mjudgestart) {
							continue;
						}
						if (judgenote instanceof MineNote || (judgenote instanceof LongNote
								&& ((LongNote) judgenote).isEnd())) {
							continue;
						}
						if (tnote == null || tnote.getState() != 0 || algorithm.compare(tnote, judgenote, pmtime, mjudge)) {
							if (!(miss == MissCondition.ONE && (judgenote.getState() != 0
									|| (judgenote.getState() == 0 && judgenote.getPlayTime() != 0
											&& (dmtime > mjudge[2][1] || dmtime < mjudge[2][0]))))) {
								if (judgenote.getState() != 0) {
									judge = (dmtime >= mjudge[4][0] && dmtime <= mjudge[4][1]) ? 5 : 6;
								} else {
									for (judge = 0; judge < mjudge.length
											&& !(dmtime >= mjudge[judge][0] && dmtime <= mjudge[judge][1]); judge++) {
									}
									judge = (judge >= 4 ? judge + 1 : judge);
								}
								
								if(judge < 6) {
									if (judge < 6 && (judge < 4 || tnote == null
											|| Math.abs(tnote.getMicroTime() - pmtime) > Math.abs(judgenote.getMicroTime() - pmtime))) {
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
							final long dmtime = tnote.getMicroTime() - pmtime;
							keysound.play(tnote, config.getAudioConfig().getKeyvolume(), 0);
							if ((lntype == BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
									|| ln.getType() == LongNote.TYPE_LONGNOTE) {
								// LN処理
								if(judgeVanish[judge]) {
									state.lnstartJudge = judge;
									state.lnstartDuration = dmtime;
									state.processing = ln.getPair();
									state.releasetime = Long.MIN_VALUE;
									state.lnendJudge = Integer.MIN_VALUE;
									if (sc >= 0) {
										sckey[sc] = key;
									}
									//LN時のレーザー色変更処理
									this.judge[state.player][state.offset] = 8;									
								} else {
									this.updateMicro(state, tnote, mtime, judge, dmtime, false);
								}
							} else {
								// CN,HCN処理
								if (judgeVanish[judge]) {
									state.processing = ln.getPair();
									state.releasetime = Long.MIN_VALUE;
									state.lnendJudge = Integer.MIN_VALUE;
									if (sc >= 0) {
										sckey[sc] = key;
									}
								}
								this.updateMicro(state, ln, mtime, judge, dmtime, judgeVanish[judge]);
							}
						} else {
							keysound.play(tnote, config.getAudioConfig().getKeyvolume(), 0);
							// 通常ノート処理
							final long dmtime = tnote.getMicroTime() - pmtime;
							this.updateMicro(state, tnote, mtime, judge, dmtime, judgeVanish[judge]);
						}
					} else {
						// 空POOR判定がないときのレーザー色変更処理
						this.judge[state.player][state.offset] = 0;

						// 空POOR判定がないときのキー音処理
						final Note[] notes = state.lanemodel.getNotes();
						Note n = notes.length > 0 ? notes[0] : null;
						for (Note note : state.lanemodel.getHiddens()) {
							if (note.getMicroTime() >= pmtime) {
								break;
							}
							n = note;
						}

						for (Note note : notes) {
							if (note.getMicroTime() >= pmtime) {
								break;
							}
							if ((n == null || n.getMicroTime() <= note.getMicroTime())
									&& !(note instanceof LongNote && note.getState() != 0)) {
								n = note;
							}
						}

						if (n != null && state.passing == null && !(n instanceof MineNote)) {
							keysound.play(n, config.getAudioConfig().getKeyvolume(), 0);
						}
					}
				}
				main.getKeyinput().inputKeyOn(lane);
			} else {
				// キーが離されたときの処理
				if (state.processing != null) {
					final long[][] mjudge = sc >= 0 ? scnendmjudge : cnendmjudge;
					long dmtime = state.processing.getMicroTime() - pmtime;
					int judge = 0;
					for (; judge < mjudge.length && !(dmtime >= mjudge[judge][0] && dmtime <= mjudge[judge][1]); judge++)
						;

					if ((lntype != BMSModel.LNTYPE_LONGNOTE
							&& state.processing.getType() == LongNote.TYPE_UNDEFINED)
							|| state.processing.getType() == LongNote.TYPE_CHARGENOTE
							|| state.processing.getType() == LongNote.TYPE_HELLCHARGENOTE) {
						// CN, HCN離し処理
						boolean release = true;
						if (sc >= 0) {
							if (judge != 4 || key != sckey[sc]) {
								release = false;
							} else {
//								 System.out.println("BSS途中離し判定 - Time : " + ptime + " Judge : " + j + " LN : " + processing[lane]);
								sckey[sc] = 0;
							}
						}
						if (release) {
							if(judge >= 3 && dmtime > 0) {
								state.releasetime = mtime;
								state.lnendJudge = judge;
							} else {
								this.updateMicro(state, state.processing, mtime, judge, dmtime, true);
								keysound.play(state.processing, config.getAudioConfig().getKeyvolume(), 0);
								state.processing = null;
								state.releasetime = Long.MIN_VALUE;
								state.lnendJudge = Integer.MIN_VALUE;
							}
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
							judge = Math.max(judge, state.lnstartJudge);
							if (Math.abs(state.lnstartDuration) > Math.abs(dmtime)) {
								dmtime = state.lnstartDuration;
							}
							if(judge >= 3 && dmtime > 0) {
								state.releasetime = mtime;								
								state.lnendJudge = 3;
							} else {
								this.updateMicro(state, state.processing.getPair(), mtime, judge, dmtime, true);
								keysound.play(state.processing, config.getAudioConfig().getKeyvolume(), 0);
								state.processing = null;
								state.releasetime = Long.MIN_VALUE;
								state.lnendJudge = Integer.MIN_VALUE;
//								System.out.println("LN途中離し判定 - Time : " + ptime + " Judge : " + j + " LN : " + processing[lane]);									
							}
						}
					}
				}
			}
			input.resetKeyChangedTime(key);
		}

		for (LaneState state : states) {
			final long[][] mjudge = state.sckey >= 0 ? smjudge : nmjudge;
			final long releasemargin = state.sckey >= 0 ? sreleasemargin : nreleasemargin;

			// LN終端判定
			if (state.processing != null) {
				if((lntype == BMSModel.LNTYPE_LONGNOTE && state.processing.getType() == LongNote.TYPE_UNDEFINED)
					|| state.processing.getType() == LongNote.TYPE_LONGNOTE) {
					if(state.releasetime != Long.MIN_VALUE && state.releasetime + releasemargin <= mtime) {
						keysound.setVolume(state.processing.getPair(), 0.0f);
						this.updateMicro(state, state.processing.getPair(), mtime, state.lnendJudge, state.processing.getMicroTime() - state.releasetime, true);
						keysound.play(state.processing, config.getAudioConfig().getKeyvolume(), 0);
						state.processing = null;
						state.releasetime = Long.MIN_VALUE;
						state.lnendJudge = Integer.MIN_VALUE;
					} else if(state.processing.getMicroTime() < mtime) {
						this.updateMicro(state, state.processing.getPair(), mtime, state.lnstartJudge, state.lnstartDuration, true);
						keysound.play(state.processing, config.getAudioConfig().getKeyvolume(), 0);
						state.processing = null;						
						state.releasetime = Long.MIN_VALUE;
						state.lnendJudge = Integer.MIN_VALUE;
					}
				} else {
					if(state.releasetime != Long.MIN_VALUE && state.releasetime + releasemargin <= mtime) {
						if (state.lnendJudge >= 3) {
							keysound.setVolume(state.processing.getPair(), 0.0f);
						}
						this.updateMicro(state, state.processing, mtime, state.lnendJudge, state.processing.getMicroTime() - state.releasetime, true);
						keysound.play(state.processing, config.getAudioConfig().getKeyvolume(), 0);
						state.processing = null;
						state.releasetime = Long.MIN_VALUE;
						state.lnendJudge = Integer.MIN_VALUE;
					}
				}
			}
			// 見逃しPOOR判定
			state.lanemodel.reset();
			for (Note note = state.lanemodel.getNote(); note != null
					&& note.getMicroTime() < mtime + mjudge[3][0]; note = state.lanemodel.getNote()) {
				final int mjud = (int) (note.getMicroTime() - mtime);
				if (note instanceof NormalNote && note.getState() == 0) {
					this.updateMicro(state, note, mtime, 4, mjud, true);
				} else if (note instanceof LongNote) {
					final LongNote ln = (LongNote) note;
					if (!ln.isEnd() && ln.getState() == 0) {
						if ((lntype != BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
								|| ln.getType() == LongNote.TYPE_CHARGENOTE
								|| ln.getType() == LongNote.TYPE_HELLCHARGENOTE) {
							// System.out.println("CN start poor");
							this.updateMicro(state, note, mtime, 4, mjud, true);
							this.updateMicro(state, ((LongNote) note).getPair(), mtime, 4, mjud, true);
						}
						if (((lntype == BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
								|| ln.getType() == LongNote.TYPE_LONGNOTE) && state.processing != ln.getPair()) {
							// System.out.println("LN start poor");
							this.updateMicro(state, note, mtime, 4, mjud, true);
						}

					}
					if (((lntype != BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
							|| ln.getType() == LongNote.TYPE_CHARGENOTE || ln.getType() == LongNote.TYPE_HELLCHARGENOTE)
							&& ((LongNote) note).isEnd() && ((LongNote) note).getState() == 0) {
						// System.out.println("CN end poor");
						this.updateMicro(state, ((LongNote) note), mtime, 4, mjud, true);
						state.processing = null;
						state.releasetime = Long.MIN_VALUE;
						state.lnendJudge = Integer.MIN_VALUE;
						if (state.sckey >= 0) {
							sckey[state.sckey] = 0;
						}
					}
				}
			}
			// LN処理タイマー
			// TODO processing値の変化のときのみ実行したい
			// TODO HCNは別タイマーにするかも
			timer.switchTimer(SkinPropertyMapper.holdTimerId(state.player, state.offset),
					state.processing != null || (state.passing != null && state.inclease));
		}
	}

	private final int[] JUDGE_TIMER = { TIMER_JUDGE_1P, TIMER_JUDGE_2P, TIMER_JUDGE_3P };
	private final int[] COMBO_TIMER = { TIMER_COMBO_1P, TIMER_COMBO_2P, TIMER_COMBO_3P };

	private void updateMicro(LaneState state, Note n, long mtime, int judge, long mfast, boolean judgeVanish) {
		if (judgeVanish) {
			if (score.getPassnotes() < ghost.length) {
				ghost[score.getPassnotes()] = judge;
			}
			n.setState(judge + 1);
			score.setPassnotes(score.getPassnotes() + 1);
		}
		if (miss == MissCondition.ONE && judge == 4 && n.getPlayTime() != 0) {
			return;
		}
		n.setMicroPlayTime(mfast);
		score.addJudgeCount(judge, mfast >= 0, 1);

		if (judge < 4) {
			recentJudgesIndex = (recentJudgesIndex + 1) % recentJudges.length;
			recentJudges[recentJudgesIndex] = mfast / 1000;
			microrecentJudges[recentJudgesIndex] = mfast;
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
			this.judge[state.player][state.offset] = judge == 0 ? 1 : judge * 2 + (mfast > 0 ? 0 : 1);
		if (judge <= ((PlaySkin) main.getSkin()).getJudgetimer()) {
			main.timer.setTimerOn(SkinPropertyMapper.bombTimerId(state.player, state.offset));
		}

		final int lanelength = states.length;
		if (judgenow.length > 0) {
			final int judgeindex = state.lane / (lanelength / judgenow.length);
			main.timer.setTimerOn(JUDGE_TIMER[judgeindex]);
			if (judgenow.length >= 3) {
				for (int i = 0; i < COMBO_TIMER.length; i++) {
					if (i != judgeindex)
						main.timer.setTimerOff(COMBO_TIMER[i]);
				}
			}
			main.timer.setTimerOn(COMBO_TIMER[judgeindex]);
			judgenow[judgeindex] = judge + 1;
			judgecombo[judgeindex] = main.getJudgeManager().getCourseCombo();
			judgefast[judgeindex] = mfast / 1000;
			mjudgefast[judgeindex] = mfast;
		}
		main.update(judge, mtime / 1000);
		keysound.play(judge, mfast >= 0);

		final PlayerConfig player = main.main.getPlayerConfig();
		if(player.isNotesDisplayTimingAutoAdjust()) {
			final BMSPlayerMode autoplay = main.resource.getPlayMode();
			if(autoplay.mode == BMSPlayerMode.Mode.PLAY || autoplay.mode == BMSPlayerMode.Mode.PRACTICE) {
				if (judge <= 2 && mfast >= -150000 && mfast <= 150000) {
					player.setJudgetiming(player.getJudgetiming() - (int)((mfast >= 0 ? mfast + 15000 : mfast - 15000) / 30000));
				}			
			}			
		}
	}

	public long[] getRecentJudges() {
		return recentJudges;
	}
	
	public long[] getMicroRecentJudges() {
		return microrecentJudges;
	}	

	public int getRecentJudgesIndex() {
		return recentJudgesIndex;
	}

	public long getRecentJudgeTiming(int player) {
		return player >= 0 && player < judgefast.length ? judgefast[player] : 0;
	}

	public long getRecentJudgeMicroTiming(int player) {
		return player >= 0 && player < mjudgefast.length ? mjudgefast[player] : 0;
	}

	public LongNote getProcessingLongNote(int lane) {
		return states[lane].processing;
	}

	public LongNote getPassingLongNote(int lane) {
		return states[lane].passing;
	}

	public boolean getHellChargeJudge(int lane) {
		return states[lane].inclease;
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

	public long[][] getJudgeTimeRegion(int lane) {
		return states[lane].sckey >= 0 ? smjudge : nmjudge;
	}

	public ScoreData getScoreData() {
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

	public int getNowJudge(int player) {
		return player >= 0 && player < judgenow.length ? judgenow[player] : 0;
	}

	public int getNowCombo(int player) {
		return player >= 0 && player < judgecombo.length ? judgecombo[player] : 0;
	}

	public long[][] getJudgeTable(boolean sc) {
		return sc ? smjudge : nmjudge;
	}

	public int getPastNotes() {
		return score.getPassnotes();
	}

	public int[] getGhost() {
		return ghost;
	}
	
	private static class LaneState {
		
		public final int lane;
		
		public final int player;
		
		public final int offset;
		
		public final int sckey;
		
		public final int[] laneassign;
		
		public final int timerActive;
		
		public final int timerDamage;
		
		public final Lane lanemodel;
		/**
		 * 処理中のLN
		 */
		public LongNote processing;
		/**
		 * 通過中のHCN
		 */
		public LongNote passing;
		/**
		 * HCN増加判定
		 */
		public boolean inclease;
		public long mpassingcount;
		
		public int lnstartJudge;
		public long lnstartDuration;
		
		public long releasetime = Long.MIN_VALUE;
		public int lnendJudge = Integer.MIN_VALUE;
		
		public LaneState(int lane, LaneProperty property, Lane lanemodel) {
			this.lane = lane;
			this.lanemodel = lanemodel;
			this.player = property.getLanePlayer()[lane];
			this.offset = property.getLaneSkinOffset()[lane];
			this.sckey = property.getLaneScratchAssign()[lane];
			this.laneassign = property.getLaneKeyAssign()[lane];
			final int offset = property.getLaneSkinOffset()[lane];
			this.timerActive = SkinPropertyMapper.hcnActiveTimerId(property.getLanePlayer()[lane], offset);
			this.timerDamage = SkinPropertyMapper.hcnDamageTimerId(property.getLanePlayer()[lane], offset);
		}
	}
}
