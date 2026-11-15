package bms.player.beatoraja.play;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.play.JudgeProperty.MissCondition;
import bms.player.beatoraja.play.JudgeProperty.NoteType;
import bms.player.beatoraja.skin.SkinPropertyMapper;

/** The standard beatoraja judgment engine, including CN, HCN and autoplay. */
public final class JudgeManager extends AbstractJudgeManager {
	// TODO HCN押し直しの発音はどうする？
	/**
	 * LN type
	 */
	private int lntype;
	private StandardLane[] states;
	private boolean passingAdvanced;
	private int[] sckey;
	/**
	 * HCNの増減間隔(ms)
	 */
	private static final int hcnmduration = 200000;

	private long mjudgestart;
	private long mjudgeend;
	private long nreleasemargin;
	private long sreleasemargin;
	private MissCondition miss;
	/**
	 * 各判定毎のノートの判定を消失するかどうか。PG, GR, GD, BD, PR, MSの順
	 */
	private boolean[] judgeVanish;
	private boolean autoplay = false;
	/**
	 * オートプレイでキーを押下する最小時間(ms)
	 */
	private final int auto_minduration = 80;

	private JudgeAlgorithm algorithm;
	JudgeManager(BMSPlayer main) {
		super(main);
	}

	@Override
	BMSPlayerRule ruleFor(Mode mode) {
		return BMSPlayerRule.getBMSPlayerRule(mode);
	}

	@Override
	JudgeAlgorithm judgeAlgorithm(PlayerResource resource) {
		return JudgeAlgorithm.valueOf(resource.getPlayerConfig().getPlayConfig(resource.getOriginalMode())
				.getPlayconfig().getJudgetype());
	}

	@Override
	LaneState[] initLanes(LaneProperty property, Lane[] lanes) {
		states = new StandardLane[lanes.length];
		for (int i = 0; i < lanes.length; i++) states[i] = new StandardLane(i, property, lanes[i]);
		return states;
	}

	@Override
	void initRule(BMSModel model, PlayerResource resource, JudgeProperty rule,
			int judgerank, int[] keyJudgeWindowRate, int[] scratchJudgeWindowRate) {
		lntype = model.getLntype();
		passingAdvanced = false;
		sckey = new int[model.getMode().scratchKey.length];
		algorithm = getScoreData().getJudgeAlgorithm();
		miss = rule.miss;
		judgeVanish = rule.judgeVanish;
		final PlayerConfig config = resource.getPlayerConfig();
		final long[] nmjudge = rule.getJudgeWindow(NoteType.NOTE, judgerank, keyJudgeWindowRate);
		nreleasemargin = config.isCustomJudge() ? rule.longnoteMargin * config.getLongnoteMarginRate() / 100L : rule.longnoteMargin;
		final long[] smjudge = rule.getJudgeWindow(NoteType.SCRATCH, judgerank, scratchJudgeWindowRate);
		sreleasemargin = rule.longscratchMargin;
		mjudgestart = mjudgeend = 0;
		for (int i = 0;i < nmjudge.length;i+=2) {
			mjudgestart = Math.min(mjudgestart, nmjudge[i]);
			mjudgeend = Math.max(mjudgeend, nmjudge[i + 1]);
		}
		for (int i = 0;i < smjudge.length;i+=2) {
			mjudgestart = Math.min(mjudgestart, smjudge[i]);
			mjudgeend = Math.max(mjudgeend, smjudge[i + 1]);
		}
		this.autoplay = resource.getPlayMode().mode == BMSPlayerMode.Mode.AUTOPLAY;
	}

	@Override
	boolean suppressMiss(Note note, int judgement) {
		return miss == MissCondition.ONE && judgement == 4 && note.getPlayTime() != 0;
	}

	@Override
	void advancePassing(final long mtime) {
		if (passingAdvanced && mtime == prevmtime) return;
		passingAdvanced = true;
		final MainController mc = main.main;
		final TimerManager timer = main.timer;
		final BMSPlayerInputProcessor input = mc.getInputProcessor();
		final Config config = main.resource.getConfig();
		final long now = timer.getNowTime();
		// 通過系の判定
		for (StandardLane state : states) {
			state.lanemodel.mark((int) ((prevmtime + mjudgestart - 100000) / 1000));
			boolean next_inclease = false;
			boolean pressed = false;
			for (int key : state.laneassign) {
				if (input.getJudgeKeyState(key)) {
					pressed = true;
					break;
				}
			}
			for (Note note = state.lanemodel.getNote(); note != null && note.getMicroTime() <= mtime; note = state.lanemodel.getNote()) {
				if (note.getMicroTime() <= prevmtime) {
					continue;
				}
				if (note instanceof LongNote lnote) {
					// HCN判定
					if ((lnote.getType() == LongNote.TYPE_UNDEFINED && lntype == BMSModel.LNTYPE_HELLCHARGENOTE)
							|| lnote.getType() == LongNote.TYPE_HELLCHARGENOTE) {
						if (lnote.isEnd()) {
							state.passing = null;
							state.mpassingcount = 0;
						} else {
							state.passing = lnote;
						}
					}
				} else if (note instanceof MineNote mnote && pressed) {
					// 地雷ノート判定
					main.getGauge().addValue((float) -mnote.getDamage());
//					System.out.println("Mine Damage : " + (float) mnote.getDamage());
					keysound.play(note, config.getAudioConfig().getKeyvolume(), 0);
				}

				if (autoplay) {
					// ここにオートプレイ処理を入れる
					if (note instanceof NormalNote && note.getState() == 0) {
						setAutoPresstime(state.laneassign[0], now);
						keysound.play(note, config.getAudioConfig().getKeyvolume(), 0);
						this.updateMicro(state, note, mtime, 0, 0, true);
					}
					if (note instanceof LongNote ln) {
						if (!ln.isEnd() && ln.getState() == 0 && state.processing == null) {
							setAutoPresstime(state.laneassign[0], now);
							keysound.play(note, config.getAudioConfig().getKeyvolume(), 0);
							if ((lntype == BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
									|| ln.getType() == LongNote.TYPE_LONGNOTE) {
								state.mpassingcount = 0;
								//LN時のレーザー色変更処理
								setLaneJudgement(state, 8);
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
									setAutoPresstime(state.laneassign[0], Long.MIN_VALUE);
									setAutoPresstime(state.laneassign[1], now);
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
					if (getAutoPresstime()[key] != Long.MIN_VALUE && now - getAutoPresstime()[key] > auto_minduration
							&& state.processing == null) {
						setAutoPresstime(key, Long.MIN_VALUE);
					}
				}
			}
			state.inclease = next_inclease;
		}

		for (StandardLane state : states) {
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
	}
	@Override
	void processKeyEvent(int key, long pmtime, boolean pressed, long mtime) {
		final Config config = main.resource.getConfig();
		if (key >= 0 && key < keyassign.length) {
			final int lane = keyassign[key];
			if (lane == -1) {
				return;
			}
			final StandardLane state = states[lane];
			state.lanemodel.reset();
			final int sc = state.sckey;
			if (pressed) {
				// キーが押されたときの処理
				if (state.processing != null) {
					if (((lntype != BMSModel.LNTYPE_LONGNOTE && state.processing.getType() == LongNote.TYPE_UNDEFINED)
							|| state.processing.getType() == LongNote.TYPE_CHARGENOTE
							|| state.processing.getType() == LongNote.TYPE_HELLCHARGENOTE)) {
						if(sc >= 0 && key != sckey[sc]) {
							// BSS終端処理
							final long dmtime = state.processing.getMicroTime() - pmtime;
							final int judge = getJudgeWindow().getJudge(NoteType.LONGSCRATCH_END, dmtime);

							keysound.play(state.processing, config.getAudioConfig().getKeyvolume(), 0);
							this.updateMicro(state, state.processing, mtime, judge, dmtime, true);
//							 System.out.println("BSS終端判定 - Time : " + ptime + " Judge : " + j + " LN : " + processing[lane].hashCode());
							state.processing = null;
							state.releasetime = Long.MIN_VALUE;
							state.lnendJudge = Integer.MIN_VALUE;
							sckey[sc] = 0;
						} else {
							// 押し直し処理
							state.releasetime = Long.MIN_VALUE;
							state.lnendJudge = Integer.MIN_VALUE;
						}
					} else {
						// 押し直し処理
						state.releasetime = Long.MIN_VALUE;
						state.lnendJudge = Integer.MIN_VALUE;
					}
				} else {
					final NoteType noteType = sc >= 0 ? NoteType.SCRATCH : NoteType.NOTE;
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
						if (judgenote instanceof MineNote || (judgenote instanceof LongNote ln && ln.isEnd())) {
							continue;
						}
						if (tnote == null || tnote.getState() != 0 || algorithm.function.compare(tnote, judgenote, pmtime, getJudgeWindow(), noteType)) {
							if (!(miss == MissCondition.ONE && (judgenote.getState() != 0
									|| (judgenote.getState() == 0 && judgenote.getPlayTime() != 0
											&& (dmtime > getJudgeWindow().getTime(noteType, 2, true) || dmtime < getJudgeWindow().getTime(noteType, 2, false)))))) {
								if (judgenote.getState() != 0) {
									judge = (dmtime >= getJudgeWindow().getTime(noteType, 4, false) && dmtime <= getJudgeWindow().getTime(noteType, 4, true)) ? 5 : 6;
								} else {
									judge = getJudgeWindow().getJudge(noteType, dmtime);
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
						if (tnote instanceof LongNote ln) {
							// ロングノート処理
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
									setLaneJudgement(state, 8);
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
						setLaneJudgement(state, 0);

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
					long dmtime = state.processing.getMicroTime() - pmtime;
					int judge = getJudgeWindow().getJudge(sc >= 0 ? NoteType.LONGSCRATCH_END : NoteType.LONGNOTE_END, dmtime);

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
							if(dmtime > 0) {
								state.releasetime = pmtime;
								state.lnendJudge = judge >= 3 ? 3 : judge;
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
		}
	}

	@Override
	void finishTime(long mtime) {
		final TimerManager timer = main.timer;
		final Config config = main.resource.getConfig();
		// ノート通過系判定
		for (StandardLane state : states) {
			final NoteType noteType = state.sckey >= 0 ? NoteType.SCRATCH : NoteType.NOTE;
			final long releasemargin = state.sckey >= 0 ? sreleasemargin : nreleasemargin;

			// LN終端判定
			if (state.processing != null) {
				if((lntype == BMSModel.LNTYPE_LONGNOTE && state.processing.getType() == LongNote.TYPE_UNDEFINED)
					|| state.processing.getType() == LongNote.TYPE_LONGNOTE) {
					if(state.releasetime != Long.MIN_VALUE && state.lnendJudge >= 3 && state.releasetime + releasemargin <= mtime) {
						keysound.setVolume(state.processing.getPair(), 0.0f);
						this.updateMicro(state, state.processing.getPair(), mtime, state.lnendJudge, state.processing.getMicroTime() - state.releasetime, true);
						keysound.play(state.processing, config.getAudioConfig().getKeyvolume(), 0);
						state.processing = null;
						state.releasetime = Long.MIN_VALUE;
						state.lnendJudge = Integer.MIN_VALUE;
					} else if(state.processing.getMicroTime() < mtime) {
						final int judge = state.lnendJudge != Integer.MIN_VALUE ? state.lnendJudge : state.lnstartJudge;
						final long duration = state.lnendJudge != Integer.MIN_VALUE ? state.processing.getMicroTime() - state.releasetime : state.lnstartDuration;
						this.updateMicro(state, state.processing.getPair(), mtime, judge, duration, true);
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
					&& note.getMicroTime() < mtime + getJudgeWindow().getTime(noteType, 3, false); note = state.lanemodel.getNote()) {
				final int mjud = (int) (note.getMicroTime() - mtime);
				if (note instanceof NormalNote && note.getState() == 0) {
					this.updateMicro(state, note, mtime, 4, mjud, true);
				} else if (note instanceof LongNote ln) {
					if (!ln.isEnd() && ln.getState() == 0) {
						if ((lntype != BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
								|| ln.getType() == LongNote.TYPE_CHARGENOTE
								|| ln.getType() == LongNote.TYPE_HELLCHARGENOTE) {
							// System.out.println("CN start poor");
							this.updateMicro(state, note, mtime, 4, mjud, true);
							this.updateMicro(state, ln.getPair(), mtime, 4, mjud, true);
						}
						if (((lntype == BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
								|| ln.getType() == LongNote.TYPE_LONGNOTE) && state.processing != ln.getPair()) {
							// System.out.println("LN start poor");
							this.updateMicro(state, note, mtime, 4, mjud, true);
						}

					}
					if (((lntype != BMSModel.LNTYPE_LONGNOTE && ln.getType() == LongNote.TYPE_UNDEFINED)
							|| ln.getType() == LongNote.TYPE_CHARGENOTE || ln.getType() == LongNote.TYPE_HELLCHARGENOTE)
							&& ln.isEnd() && ln.getState() == 0) {
						// System.out.println("CN end poor");
						this.updateMicro(state, ln, mtime, 4, mjud, true);
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
			timer.switchTimer(state.timerHold, state.processing != null || (state.passing != null && state.inclease));
		}
	}
	@Override
	public LongNote getPassingLongNote(int lane) {
		return states[lane].passing;
	}

	@Override
	public boolean getHellChargeJudge(int lane) {
		return states[lane].inclease;
	}

	private static final class StandardLane extends LaneState {
		final int timerActive;
		final int timerDamage;
		LongNote passing;
		boolean inclease;
		long mpassingcount;
		int lnendJudge = Integer.MIN_VALUE;

		StandardLane(int lane, LaneProperty property, Lane lanemodel) {
			super(lane, property, lanemodel);
			timerActive = SkinPropertyMapper.hcnActiveTimerId(player, offset);
			timerDamage = SkinPropertyMapper.hcnDamageTimerId(player, offset);
		}
	}
}
