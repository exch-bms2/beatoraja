package bms.player.beatoraja.result;

import static bms.player.beatoraja.ClearType.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.input.KeyCommand;
import bms.player.beatoraja.input.KeyBoardInputProcesseor.ControlKeys;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.MainController.IRStatus;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.ir.*;
import bms.player.beatoraja.play.GrooveGauge;
import bms.player.beatoraja.skin.SkinType;
import bms.player.beatoraja.skin.property.EventFactory.EventType;
import bms.player.beatoraja.song.SongData;

/**
 * リザルト
 *
 * @author exch
 */
public class MusicResult extends AbstractResult {

	private ResultKeyProperty property;
	
	private List<IRSendStatus> irSendStatus = new ArrayList<IRSendStatus>();

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
		if (resource.getPlayMode().mode == BMSPlayerMode.Mode.PLAY) {
			for (int i = 0; i < REPLAY_SIZE; i++) {
				if (ReplayAutoSaveConstraint.get(resource.getPlayerConfig().getAutoSaveReplay()[i]).isQualified(oldscore,
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
		final PlayerResource resource = main.getPlayerResource();
		final ScoreData newscore = getNewScore();

		ranking = resource.getRankingData() != null && resource.getCourseBMSModels() == null ? resource.getRankingData() : new RankingData();
		rankingOffset = 0;
		// TODO スコアハッシュがあり、有効期限が切れていないものを送信する？
		final IRStatus[] ir = main.getIRStatus();
		if (ir.length > 0 && resource.getPlayMode().mode == BMSPlayerMode.Mode.PLAY) {
			state = STATE_IR_PROCESSING;
			
        	for(IRStatus irc : ir) {
    			boolean send = resource.isUpdateScore();
    			switch(irc.config.getIrsend()) {
    			case IRConfig.IR_SEND_ALWAYS:
    				break;
    			case IRConfig.IR_SEND_COMPLETE_SONG:
    				FloatArray gauge = resource.getGauge()[resource.getGrooveGauge().getType()];
    				send &= gauge.get(gauge.size - 1) > 0.0;
    				break;
    			case IRConfig.IR_SEND_UPDATE_SCORE:
    				send &= (newscore.getExscore() > oldscore.getExscore() || newscore.getClear() > oldscore.getClear()
    						|| newscore.getCombo() > oldscore.getCombo() || newscore.getMinbp() < oldscore.getMinbp());
    				break;
    			}
    			
    			if(send) {
    				irSendStatus.add(new IRSendStatus(irc.connection, resource.getSongdata(), newscore));
    			}
        	}
			
			Thread irprocess = new Thread(() -> {
                try {
                	int irsend = 0;
                	boolean succeed = true;
                	List<IRSendStatus> removeIrSendStatus = new ArrayList<IRSendStatus>();
                	
                	for(IRSendStatus irc : irSendStatus) {
        				if(irsend == 0) {
        					main.switchTimer(TIMER_IR_CONNECT_BEGIN, true);                					
        				}
        				irsend++;
                        succeed &= irc.send();
                        if(irc.retry < 0 || irc.retry > main.getConfig().getIrSendCount()) {
                        	removeIrSendStatus.add(irc);
                        }
                	}
                	irSendStatus.removeAll(removeIrSendStatus);
                	                	
                	if(irsend > 0) {
                        main.switchTimer(succeed ? TIMER_IR_CONNECT_SUCCESS : TIMER_IR_CONNECT_FAIL, true);
                        
                        IRResponse<bms.player.beatoraja.ir.IRScoreData[]> response = ir[0].connection.getPlayData(null, new IRChartData(resource.getSongdata()));
                        if(response.isSucceeded()) {
                    		ranking.updateScore(response.getData(), newscore.getExscore() > oldscore.getExscore() ? newscore : oldscore);
                    		rankingOffset = ranking.getRank() > 10 ? ranking.getRank() - 5 : 0;
                            Logger.getGlobal().warning("IRからのスコア取得成功 : " + response.getMessage());
                        } else {
                            Logger.getGlobal().warning("IRからのスコア取得失敗 : " + response.getMessage());
                        }                    		
                	}
                } catch (Exception e) {
                    Logger.getGlobal().severe(e.getMessage());
                } finally {
                    state = STATE_IR_FINISHED;
                }
            });
			irprocess.start();
		}

		final ScoreData cscore = resource.getCourseScoreData();
		play(newscore.getClear() != Failed.id && (cscore == null || cscore.getClear() != Failed.id) ? SOUND_CLEAR : SOUND_FAIL);
	}

	public void shutdown() {
		stop(SOUND_CLEAR);
		stop(SOUND_FAIL);
		stop(SOUND_CLOSE);
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
				main.getAudioProcessor().stop((Note) null);

				final BMSPlayerInputProcessor input = main.getInputProcessor();
				main.getInputProcessor().resetAllKeyChangedTime();

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
									resource.getCourseScoreData().setTotalDuration(resource.getCourseScoreData().getTotalDuration()
											+ 1000000L * resource.getCourseBMSModels()[i].getTotalNotes());
								}
							}
							// 不合格リザルト
							main.changeState(MainStateType.COURSERESULT);
						} else {
							// コーススコアがない場合は選曲画面へ
							main.changeState(MainStateType.MUSICSELECT);
						}
					} else if (resource.nextCourse()) {
						RankingData songrank = main.getRankingDataCache().get(resource.getSongdata(), main.getPlayerConfig().getLnmode());
						if(main.getIRStatus().length > 0 && songrank == null) {
							songrank = new RankingData();
							main.getRankingDataCache().put(resource.getSongdata(), main.getPlayerConfig().getLnmode(), songrank);
						}
						resource.setRankingData(songrank);

						main.changeState(MainStateType.PLAY);
					} else {
						// 合格リザルト
						main.changeState(MainStateType.COURSERESULT);
					}
				} else {
					main.getPlayerResource().getPlayerConfig().setGauge(main.getPlayerResource().getOrgGaugeOption());
					ResultKeyProperty.ResultKey key = null;
					for (int i = 0; i < property.getAssignLength(); i++) {
						if (property.getAssign(i) == ResultKeyProperty.ResultKey.REPLAY_DIFFERENT && input.getKeyState(i)) {
							key = ResultKeyProperty.ResultKey.REPLAY_DIFFERENT;
							break;
						}
						if (property.getAssign(i) == ResultKeyProperty.ResultKey.REPLAY_SAME && input.getKeyState(i)) {
							key = ResultKeyProperty.ResultKey.REPLAY_SAME;
							break;
						}
					}
					if (resource.getPlayMode().mode == BMSPlayerMode.Mode.PLAY
							&& key == ResultKeyProperty.ResultKey.REPLAY_DIFFERENT) {
						Logger.getGlobal().info("オプションを変更せずリプレイ");
						// オプションを変更せず同じ譜面でリプレイ
						resource.getReplayData().randomoptionseed = -1;
						resource.reloadBMSFile();
						main.changeState(MainStateType.PLAY);
					} else if (resource.getPlayMode().mode == BMSPlayerMode.Mode.PLAY
							&& key == ResultKeyProperty.ResultKey.REPLAY_SAME) {
						// 同じ譜面でリプレイ
						if(resource.isUpdateScore()) {
							Logger.getGlobal().info("同じ譜面でリプレイ");							
						} else {
							Logger.getGlobal().info("アシストモード時は同じ譜面でリプレイできません");
							resource.getReplayData().randomoptionseed = -1;
						}
						resource.reloadBMSFile();
						main.changeState(MainStateType.PLAY);
					} else {
						main.changeState(MainStateType.MUSICSELECT);
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
		super.input();
		long time = main.getNowTime();
		final PlayerResource resource = main.getPlayerResource();
		final BMSPlayerInputProcessor inputProcessor = main.getInputProcessor();

		if (!main.isTimerOn(TIMER_FADEOUT) && main.isTimerOn(TIMER_STARTINPUT)) {
			if (time > getSkin().getInput()) {
				boolean ok = false;
				for (int i = 0; i < property.getAssignLength(); i++) {
					if (property.getAssign(i) == ResultKeyProperty.ResultKey.CHANGE_GRAPH && inputProcessor.getKeyState(i) && inputProcessor.resetKeyChangedTime(i)) {
						if(gaugeType >= GrooveGauge.ASSISTEASY && gaugeType <= GrooveGauge.HAZARD) {
							gaugeType = (gaugeType + 1) % 6;
						} else {
							gaugeType = (gaugeType - 5) % 3 + 6;
						}
					} else if (property.getAssign(i) != null && inputProcessor.getKeyState(i) && inputProcessor.resetKeyChangedTime(i)) {
						ok = true;
					}
				}

				if (inputProcessor.isControlKeyPressed(ControlKeys.ESCAPE) || inputProcessor.isControlKeyPressed(ControlKeys.ENTER)) {
					ok = true;
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

				if(inputProcessor.isControlKeyPressed(ControlKeys.NUM1)) {
					saveReplayData(0);				
				} else if(inputProcessor.isControlKeyPressed(ControlKeys.NUM2)) {
					saveReplayData(1);				
				} else if(inputProcessor.isControlKeyPressed(ControlKeys.NUM3)) {
					saveReplayData(2);				
				} else if(inputProcessor.isControlKeyPressed(ControlKeys.NUM4)) {
					saveReplayData(3);				
				}

				if(inputProcessor.isActivated(KeyCommand.OPEN_IR)) {
					this.executeEvent(EventType.open_ir);
				}
			}
		}
	}

	public void saveReplayData(int index) {
		final PlayerResource resource = main.getPlayerResource();
		if (resource.getPlayMode().mode == BMSPlayerMode.Mode.PLAY && resource.getCourseBMSModels() == null
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
		ScoreData newscore = resource.getScoreData();
		if (newscore == null) {
			if (resource.getCourseScoreData() != null) {
				resource.getCourseScoreData()
						.setMinbp(resource.getCourseScoreData().getMinbp() + resource.getBMSModel().getTotalNotes());
				resource.getCourseScoreData().setClear(Failed.id);
			}
			return;
		}
		final ScoreData oldsc = main.getPlayDataAccessor().readScoreData(resource.getBMSModel(),
				resource.getPlayerConfig().getLnmode());
		oldscore = oldsc != null ? oldsc : new ScoreData();

		getScoreDataProperty().setTargetScore(oldscore.getExscore(), resource.getRivalScoreData() != null ? resource.getRivalScoreData().getExscore() : 0, resource.getBMSModel().getTotalNotes());
		getScoreDataProperty().update(newscore);
		// duration average
		int count = 0;
		avgduration = newscore.getAvgjudge();
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
						timingDistribution.add(time);
					}
				}
			}
		}
		timingDistribution.statisticValueCalcuate();

		// コースモードの場合はコーススコアに加算・累積する
		if (resource.getCourseBMSModels() != null) {
			if (resource.getScoreData().getClear() == Failed.id) {
				resource.getScoreData().setClear(NoPlay.id);
			}
			ScoreData cscore = resource.getCourseScoreData();
			if (cscore == null) {
				cscore = new ScoreData();
				cscore.setMinbp(0);
				int notes = 0;
				for (BMSModel mo : resource.getCourseBMSModels()) {
					notes += mo.getTotalNotes();
				}
				cscore.setNotes(notes);
				cscore.setDeviceType(newscore.getDeviceType());
				cscore.setOption(newscore.getOption());
				cscore.setJudgeAlgorithm(newscore.getJudgeAlgorithm());
				cscore.setRule(newscore.getRule());
				resource.setCourseScoreData(cscore);
			}
			cscore.setPassnotes(cscore.getPassnotes() + newscore.getPassnotes());
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
			cscore.setTotalDuration(cscore.getTotalDuration() + newscore.getTotalDuration());
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

		if (resource.getPlayMode().mode == BMSPlayerMode.Mode.PLAY) {
			main.getPlayDataAccessor().writeScoreData(resource.getScoreData(), resource.getBMSModel(),
					resource.getPlayerConfig().getLnmode(), resource.isUpdateScore());
		} else {
			Logger.getGlobal().info("プレイモードが" + resource.getPlayMode().mode.name() + "のため、スコア登録はされません");
		}

	}

	public int getJudgeCount(int judge, boolean fast) {
		ScoreData score = main.getPlayerResource().getScoreData();
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

	public ScoreData getNewScore() {
		return main.getPlayerResource().getScoreData();
	}

	static class IRSendStatus {
		public final IRConnection ir;
		public final SongData song;
		public final ScoreData score;
		public int retry = 0;
		
		public IRSendStatus(IRConnection ir, SongData song, ScoreData score) {
			this.ir = ir;
			this.song = song;
			this.score = score;
		}
		
		public boolean send() {
			Logger.getGlobal().info("IRへスコア送信中 : " + song.getTitle());
            IRResponse<Object> send1 = ir.sendPlayData(new IRChartData(song), new bms.player.beatoraja.ir.IRScoreData(score));
            if(send1.isSucceeded()) {
                Logger.getGlobal().info("IRスコア送信完了 : " + song.getTitle());
                retry = -255;
                return true;
            } else {
                Logger.getGlobal().warning("IRスコア送信失敗 : " + send1.getMessage());
                retry++;
                return false;
            }

		}
	}
}
