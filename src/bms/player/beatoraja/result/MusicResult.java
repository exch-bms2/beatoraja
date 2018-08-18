package bms.player.beatoraja.result;

import static bms.player.beatoraja.ClearType.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Arrays;
import java.util.logging.Logger;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.Note;
import bms.model.TimeLine;
import bms.player.beatoraja.*;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.ir.IRResponse;
import bms.player.beatoraja.play.GrooveGauge;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.skin.SkinType;

/**
 * リザルト
 *
 * @author exch
 */
public class MusicResult extends AbstractResult {

	private ResultKeyProperty property;

	public MusicResult(MainController main) {
		super(main);
	}

	public void create() {
		final PlayerResource resource = main.getPlayerResource();
		for(int i = 0;i < REPLAY_SIZE;i++) {
			saveReplay[i] = main.getPlayDataAccessor().existsReplayData(resource.getBMSModel(),
					resource.getPlayerConfig().getLnmode(), i) ? ReplayStatus.EXIST : ReplayStatus.NOT_EXIST ;			
		}

		setSound(SOUND_CLEAR, "clear.wav", SoundType.SOUND, false);
		setSound(SOUND_FAIL, "fail.wav", SoundType.SOUND, false);
		setSound(SOUND_CLOSE, "resultclose.wav", SoundType.SOUND, false);

		property = ResultKeyProperty.get(resource.getBMSModel().getMode());
		if (property == null) {
			property = ResultKeyProperty.BEAT_7K;
		}

		updateScoreDatabase();
		// リプレイの自動保存
		if (resource.getPlayMode() == PlayMode.PLAY) {
			for (int i = 0; i < REPLAY_SIZE; i++) {
				if (ReplayAutoSaveConstraint.get(resource.getConfig().getAutoSaveReplay()[i]).isQualified(oldscore,
						resource.getScoreData())) {
					saveReplayData(i);
				}
			}
		}
		// コースモードの場合はリプレイデータをストックする
		if (resource.getCourseBMSModels() != null) {
			resource.addCourseReplay(resource.getReplayData());
			resource.addCourseGauge(resource.getGauge());
		}
		
		gaugeType = resource.getGrooveGauge().getType();

		loadSkin(SkinType.RESULT);
	}
	
	public void prepare() {
		state = STATE_OFFLINE;
		irrank = irprevrank = irtotal = 0;
		final PlayerResource resource = main.getPlayerResource();
		final IRScoreData newscore = resource.getScoreData();

		// TODO スコアハッシュがあり、有効期限が切れていないものを送信する？
		final IRConnection ir = main.getIRConnection();
		if (ir != null && resource.getPlayMode() == PlayMode.PLAY) {
			boolean send = resource.isUpdateScore();
			switch(main.getPlayerConfig().getIrsend()) {
			case PlayerConfig.IR_SEND_ALWAYS:
				break;
			case PlayerConfig.IR_SEND_COMPLETE_SONG:
				FloatArray gauge = resource.getGauge()[resource.getGrooveGauge().getType()];
				send &= gauge.get(gauge.size - 1) > 0.0;
				break;
			case PlayerConfig.IR_SEND_UPDATE_SCORE:
				IRScoreData current = resource.getScoreData();
				send &= (current.getExscore() > oldscore.getExscore() || current.getClear() > oldscore.getClear()
						|| current.getCombo() > oldscore.getCombo() || current.getMinbp() < oldscore.getMinbp());
				break;
			}

			if(send) {
				Logger.getGlobal().info("IRへスコア送信中");
				main.switchTimer(TIMER_IR_CONNECT_BEGIN, true);
				state = STATE_IR_PROCESSING;
				Thread irprocess = new Thread(() -> {
                    try {
                        IRResponse<Object> send1 = ir.sendPlayData(resource.getSongdata(), resource.getScoreData());
                        if(send1.isSucceeded()) {
                            main.switchTimer(TIMER_IR_CONNECT_SUCCESS, true);
                            Logger.getGlobal().info("IRスコア送信完了");
                        } else {
                            main.switchTimer(TIMER_IR_CONNECT_FAIL, true);
                            Logger.getGlobal().warning("IRスコア送信失敗 : " + send1.getMessage());
                        }
                        IRResponse<IRScoreData[]> response = ir.getPlayData(null, resource.getSongdata());
                        if(response.isSucceeded()) {
                            IRScoreData[] scores = response.getData();
                            irtotal = scores.length;

                            for(int i = 0;i < scores.length;i++) {
                                if(irrank == 0 && scores[i].getExscore() <= resource.getScoreData().getExscore() ) {
                                    irrank = i + 1;
                                }
                                if(irprevrank == 0 && scores[i].getExscore() <= oldscore.getExscore() ) {
                                    irprevrank = i + 1;
                                    if(irrank == 0) {
                                        irrank = irprevrank;
                                    }
                                }
                            }
                            Logger.getGlobal().warning("IRからのスコア取得成功 : " + response.getMessage());
                        } else {
                            Logger.getGlobal().warning("IRからのスコア取得失敗 : " + response.getMessage());
                        }
                    } catch (Exception e) {
                        Logger.getGlobal().severe(e.getMessage());
                    } finally {
                        state = STATE_IR_FINISHED;
                    }
                });
				irprocess.start();
			}
		}

		final IRScoreData cscore = resource.getCourseScoreData();
		if (newscore.getClear() != Failed.id && (cscore == null || cscore.getClear() != Failed.id)) {
			play(SOUND_CLEAR);
		} else {
			play(SOUND_FAIL);
		}

	}

	public void render() {
		long time = main.getNowTime();
		main.switchTimer(TIMER_RESULTGRAPH_BEGIN, true);
		main.switchTimer(TIMER_RESULTGRAPH_END, true);

		if (((MusicResultSkin) getSkin()).getRankTime() == 0) {
			main.switchTimer(TIMER_RESULT_UPDATESCORE, true);
		}
		if (time > getSkin().getInput()) {
			main.switchTimer(TIMER_STARTINPUT, true);
		}

		final PlayerResource resource = main.getPlayerResource();

		if (main.isTimerOn(TIMER_FADEOUT)) {
			if (main.getNowTime(TIMER_FADEOUT) > getSkin().getFadeout()) {
				stop(SOUND_CLEAR);
				stop(SOUND_FAIL);
				stop(SOUND_CLOSE);
				main.getAudioProcessor().stop((Note) null);

				boolean[] keystate = main.getInputProcessor().getKeystate();
				//				System.out.println(Arrays.toString(keystate));
				long[] keytime = main.getInputProcessor().getTime();
				Arrays.fill(keytime, 0);

				if (resource.getCourseBMSModels() != null) {
					if (resource.getGauge()[resource.getGrooveGauge().getType()]
							.get(resource.getGauge()[resource.getGrooveGauge().getType()].size - 1) <= 0) {
						if (resource.getCourseScoreData() != null) {
							// 未達曲のノーツをPOORとして加算
							final Array<FloatArray[]> coursegauge = resource.getCourseGauge();
							final int cg = resource.getCourseBMSModels().length;
							for (int i = 0; i < cg; i++) {
								if (coursegauge.size <= i) {
									resource.getCourseScoreData().setMinbp(resource.getCourseScoreData().getMinbp()
											+ resource.getCourseBMSModels()[i].getTotalNotes());
								}
							}
							// 不合格リザルト
							main.changeState(MainController.STATE_GRADE_RESULT);
						} else {
							// コーススコアがない場合は選曲画面へ
							main.changeState(MainController.STATE_SELECTMUSIC);
						}
					} else if (resource.nextCourse()) {
						main.changeState(MainController.STATE_PLAYBMS);
					} else {
						// 合格リザルト
						main.changeState(MainController.STATE_GRADE_RESULT);
					}
				} else {
					main.getPlayerResource().getPlayerConfig().setGauge(main.getPlayerResource().getOrgGaugeOption());
					ResultKeyProperty.ResultKey key = null;
					for (int i = 0; i < property.getAssignLength(); i++) {
						if (property.getAssign(i) == ResultKeyProperty.ResultKey.REPLAY_DIFFERENT && keystate[i]) {
							key = ResultKeyProperty.ResultKey.REPLAY_DIFFERENT;
							break;
						}
						if (property.getAssign(i) == ResultKeyProperty.ResultKey.REPLAY_SAME && keystate[i]) {
							key = ResultKeyProperty.ResultKey.REPLAY_SAME;
							break;
						}
					}
					if (resource.getPlayMode() == PlayMode.PLAY
							&& key == ResultKeyProperty.ResultKey.REPLAY_DIFFERENT) {
						Logger.getGlobal().info("オプションを変更せずリプレイ");
						// オプションを変更せず同じ譜面でリプレイ
						resource.getReplayData().pattern = null;
						resource.reloadBMSFile();
						main.changeState(MainController.STATE_PLAYBMS);
					} else if (resource.getPlayMode() == PlayMode.PLAY
							&& key == ResultKeyProperty.ResultKey.REPLAY_SAME) {
						// 同じ譜面でリプレイ
						Logger.getGlobal().info("同じ譜面でリプレイ");
						resource.reloadBMSFile();
						main.changeState(MainController.STATE_PLAYBMS);
					} else {
						main.changeState(MainController.STATE_SELECTMUSIC);
					}
				}
			}
		} else {
			if (time > getSkin().getScene()) {
				main.switchTimer(TIMER_FADEOUT, true);
				if (getSound(SOUND_CLOSE) != null) {
					stop(SOUND_CLEAR);
					stop(SOUND_FAIL);
					play(SOUND_CLOSE);
				}
			}
		}

	}

	public void input() {
		long time = main.getNowTime();
		final PlayerResource resource = main.getPlayerResource();
		final BMSPlayerInputProcessor inputProcessor = main.getInputProcessor();

		if (!main.isTimerOn(TIMER_FADEOUT) && main.isTimerOn(TIMER_STARTINPUT)) {
			if (time > getSkin().getInput()) {
				boolean[] keystate = inputProcessor.getKeystate();
				long[] keytime = inputProcessor.getTime();
				boolean ok = false;
				for (int i = 0; i < property.getAssignLength(); i++) {
					if (property.getAssign(i) == ResultKeyProperty.ResultKey.CHANGE_GRAPH && keystate[i] && keytime[i] != 0) {
						if(gaugeType >= GrooveGauge.ASSISTEASY && gaugeType <= GrooveGauge.HAZARD) {
							gaugeType = (gaugeType + 1) % 6;
						} else {
							gaugeType = (gaugeType - 5) % 3 + 6;
						}
						keytime[i] = 0;
					} else if (property.getAssign(i) != null && keystate[i] && keytime[i] != 0) {
						keytime[i] = 0;
						ok = true;
					}
				}

				if (inputProcessor.isEnterPressed()) {
					ok = true;
					inputProcessor.setEnterPressed(false);
				}

				if (inputProcessor.isExitPressed()) {
					ok = true;
					inputProcessor.setExitPressed(false);
				}

				if (resource.getScoreData() == null || ok) {
					if (((MusicResultSkin) getSkin()).getRankTime() != 0
							&& !main.isTimerOn(TIMER_RESULT_UPDATESCORE)) {
						main.switchTimer(TIMER_RESULT_UPDATESCORE, true);
					} else if (state == STATE_OFFLINE || state == STATE_IR_FINISHED) {
						main.switchTimer(TIMER_FADEOUT, true);
						if (getSound(SOUND_CLOSE) != null) {
							stop(SOUND_CLEAR);
							stop(SOUND_FAIL);
							play(SOUND_CLOSE);
						}
					}
				}

				for (int i = 0; i < MusicSelector.REPLAY; i++) {
					if (inputProcessor.getNumberState()[i + 1]) {
						saveReplayData(i);
						break;
					}
				}
			}
		}
	}

	private void saveReplayData(int index) {
		final PlayerResource resource = main.getPlayerResource();
		if (resource.getPlayMode() == PlayMode.PLAY && resource.getCourseBMSModels() == null
				&& resource.getScoreData() != null) {
			if (saveReplay[index] != ReplayStatus.SAVED && resource.isUpdateScore()) {
				ReplayData rd = resource.getReplayData();
				main.getPlayDataAccessor().wrireReplayData(rd, resource.getBMSModel(),
						resource.getPlayerConfig().getLnmode(), index);
				saveReplay[index] = ReplayStatus.SAVED;
			}
		}
	}

	private void updateScoreDatabase() {
		final PlayerResource resource = main.getPlayerResource();
		IRScoreData newscore = resource.getScoreData();
		if (newscore == null) {
			if (resource.getCourseScoreData() != null) {
				resource.getCourseScoreData()
						.setMinbp(resource.getCourseScoreData().getMinbp() + resource.getBMSModel().getTotalNotes());
				resource.getCourseScoreData().setClear(Failed.id);
			}
			return;
		}
		IRScoreData oldsc = main.getPlayDataAccessor().readScoreData(resource.getBMSModel(),
				resource.getPlayerConfig().getLnmode());
		if (oldsc != null) {
			oldscore = oldsc;
		}else{
			oldscore = new IRScoreData();
		}

		getScoreDataProperty().setTargetScore(oldscore.getExscore(), resource.getRivalScoreData(), resource.getBMSModel().getTotalNotes());
		getScoreDataProperty().update(newscore);
		// duration average
		int count = 0;
		avgduration = 0;
		timingDistribution.init();
		final int lanes = resource.getBMSModel().getMode().key;
		for (TimeLine tl : resource.getBMSModel().getAllTimeLines()) {
			for (int i = 0; i < lanes; i++) {
				Note n = tl.getNote(i);
				if (n != null && !(resource.getBMSModel().getLntype() == BMSModel.LNTYPE_LONGNOTE
						&& n instanceof LongNote && ((LongNote) n).isEnd())) {
					int state = n.getState();
					int time = n.getPlayTime();
					if (state >= 1) {
						count++;
						avgduration += Math.abs(time);
						timingDistribution.add(time);
					}
				}
			}
		}
		avgduration /= count;
		timingDistribution.statisticValueCalcuate();

		// コースモードの場合はコーススコアに加算・累積する
		if (resource.getCourseBMSModels() != null) {
			if (resource.getScoreData().getClear() == Failed.id) {
				resource.getScoreData().setClear(NoPlay.id);
			}
			IRScoreData cscore = resource.getCourseScoreData();
			if (cscore == null) {
				cscore = new IRScoreData();
				cscore.setMinbp(0);
				int notes = 0;
				for (BMSModel mo : resource.getCourseBMSModels()) {
					notes += mo.getTotalNotes();
				}
				cscore.setNotes(notes);
				cscore.setDeviceType(newscore.getDeviceType());
				cscore.setOption(newscore.getOption());
				resource.setCourseScoreData(cscore);
			}
			cscore.setEpg(cscore.getEpg() + newscore.getEpg());
			cscore.setLpg(cscore.getLpg() + newscore.getLpg());
			cscore.setEgr(cscore.getEgr() + newscore.getEgr());
			cscore.setLgr(cscore.getLgr() + newscore.getLgr());
			cscore.setEgd(cscore.getEgd() + newscore.getEgd());
			cscore.setLgd(cscore.getLgd() + newscore.getLgd());
			cscore.setEbd(cscore.getEbd() + newscore.getEbd());
			cscore.setLbd(cscore.getLbd() + newscore.getLbd());
			cscore.setEpr(cscore.getEpr() + newscore.getEpr());
			cscore.setLpr(cscore.getLpr() + newscore.getLpr());
			cscore.setEms(cscore.getEms() + newscore.getEms());
			cscore.setLms(cscore.getLms() + newscore.getLms());
			cscore.setMinbp(cscore.getMinbp() + newscore.getMinbp());
			if (resource.getGauge()[resource.getGrooveGauge().getType()].get(resource.getGauge()[resource.getGrooveGauge().getType()].size - 1) > 0) {
				if (resource.getAssist() > 0) {
					if(resource.getAssist() == 1 && cscore.getClear() != ClearType.AssistEasy.id) cscore.setClear(ClearType.LightAssistEasy.id);
					else cscore.setClear(ClearType.AssistEasy.id);
				} else if(!(cscore.getClear() == ClearType.LightAssistEasy.id || cscore.getClear() == ClearType.AssistEasy.id)) {
					if(resource.getCourseIndex() == resource.getCourseBMSModels().length - 1) {
						int courseTotalNotes = 0;
						for(int i = 0; i < resource.getCourseBMSModels().length; i++) {
							courseTotalNotes += resource.getCourseBMSModels()[i].getTotalNotes();
						}
						if (courseTotalNotes == resource.getMaxcombo()) {
							if (cscore.getJudgeCount(2) == 0) {
								if (cscore.getJudgeCount(1) == 0) {
									cscore.setClear(ClearType.Max.id);
								} else {
									cscore.setClear(ClearType.Perfect.id);
								}
							} else {
								cscore.setClear(ClearType.FullCombo.id);
							}
						} else {
							cscore.setClear(resource.getGrooveGauge().getClearType().id);
						}
					}
				}
			} else {
				cscore.setClear(Failed.id);

				boolean b = false;
				// 残りの曲がある場合はtotalnotesをBPに加算する
				for (BMSModel m : resource.getCourseBMSModels()) {
					if (b) {
						cscore.setMinbp(cscore.getMinbp() + m.getTotalNotes());
					}
					if (m == resource.getBMSModel()) {
						b = true;
					}
				}
			}
			newscore = cscore;
		}

		if (resource.getPlayMode() == PlayMode.PLAY) {
			main.getPlayDataAccessor().writeScoreDara(resource.getScoreData(), resource.getBMSModel(),
					resource.getPlayerConfig().getLnmode(), resource.isUpdateScore());
		}

	}

	public int getJudgeCount(int judge, boolean fast) {
		IRScoreData score = main.getPlayerResource().getScoreData();
		if (score != null) {
			switch (judge) {
			case 0:
				return fast ? score.getEpg() : score.getLpg();
			case 1:
				return fast ? score.getEgr() : score.getLgr();
			case 2:
				return fast ? score.getEgd() : score.getLgd();
			case 3:
				return fast ? score.getEbd() : score.getLbd();
			case 4:
				return fast ? score.getEpr() : score.getLpr();
			case 5:
				return fast ? score.getEms() : score.getLms();
			}
		}
		return 0;
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	public int getTotalNotes() {
		final PlayerResource resource = main.getPlayerResource();
		return resource.getBMSModel().getTotalNotes();
	}

	public void executeClickEvent(int id, int arg) {
		switch (id) {
		case BUTTON_REPLAY:
			saveReplayData(0);
			break;
		case BUTTON_REPLAY2:
			saveReplayData(1);
			break;
		case BUTTON_REPLAY3:
			saveReplayData(2);
			break;
		case BUTTON_REPLAY4:
			saveReplayData(3);
			break;
		}
	}
	
	public IRScoreData getNewScore() {
		return main.getPlayerResource().getScoreData();
	}
}
