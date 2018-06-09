package bms.player.beatoraja.result;

import static bms.player.beatoraja.ClearType.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import com.badlogic.gdx.utils.FloatArray;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.Note;
import bms.model.TimeLine;
import bms.player.beatoraja.*;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.keyData;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.ir.IRResponse;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.skin.SkinType;

/**
 * �꺁�궣�꺂�깉
 *
 * @author exch
 */
public class MusicResult extends AbstractResult {

	private IRScoreData oldscore = new IRScoreData();

	/**
	 * �뀲�깕�꺖�깂�겗亮녑쓦�궨�꺃
	 */
	private float avgduration;

	/**
	 * �궭�궎�깱�꺍�궛�늽躍�
	 */
	private TimingDistribution timingDistribution;

	/**
	 * �궭�궎�깱�꺍�궛�늽躍껁꺃�꺍�궦
	 */
	final int distRange = 150;

	private ResultKeyProperty property;

	public MusicResult(MainController main) {
		super(main);

		timingDistribution = new TimingDistribution(distRange);
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
		// �꺁�깤�꺃�궎�겗�눎�땿岳앭춼
		if (resource.getPlayMode() == PlayMode.PLAY) {
			for (int i = 0; i < REPLAY_SIZE; i++) {
				if (ReplayAutoSaveConstraint.get(resource.getConfig().getAutoSaveReplay()[i]).isQualified(oldscore,
						resource.getScoreData())) {
					saveReplayData(i);
				}
			}
		}
		// �궠�꺖�궧�깴�꺖�깋�겗�졃�릦�겘�꺁�깤�꺃�궎�깈�꺖�궭�굮�궧�깉�긿�궚�걲�굥
		if (resource.getCourseBMSModels() != null) {
			resource.addCourseReplay(resource.getReplayData());
			resource.addCourseGauge(resource.getGauge());
		}

		loadSkin(SkinType.RESULT);
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

				BMSPlayerInputProcessor input = main.getInputProcessor();
				keyData.resetKeyTime();

				if (resource.getCourseBMSModels() != null) {
					if (resource.getGauge()[resource.getGrooveGauge().getType()]
							.get(resource.getGauge()[resource.getGrooveGauge().getType()].size - 1) <= 0) {
						if (resource.getCourseScoreData() != null) {
							// �쑋�걫�쎊�겗�깕�꺖�깂�굮POOR�겏�걮�겍�뒥嶸�
							final List<FloatArray[]> coursegauge = resource.getCourseGauge();
							final int cg = resource.getCourseBMSModels().length;
							for (int i = 0; i < cg; i++) {
								if (coursegauge.size() <= i) {
									resource.getCourseScoreData().setMinbp(resource.getCourseScoreData().getMinbp()
											+ resource.getCourseBMSModels()[i].getTotalNotes());
								}
							}
							// 訝띶릦�졏�꺁�궣�꺂�깉
							main.changeState(MainController.STATE_GRADE_RESULT);
						} else {
							// �궠�꺖�궧�궧�궠�궋�걣�겒�걚�졃�릦�겘�겦�쎊�뵽�씊�겦
							main.changeState(MainController.STATE_SELECTMUSIC);
						}
					} else if (resource.nextCourse()) {
						main.changeState(MainController.STATE_PLAYBMS);
					} else {
						// �릦�졏�꺁�궣�꺂�깉
						if (resource.getPlayerConfig().isContinueUntilEndOfSong()) {
							int changedGaugeType = resource.getGrooveGauge()
									.changeTypeOfClear(resource.getGrooveGauge().getType());
							if (resource.getPlayMode() == PlayMode.PLAY)
								resource.getPlayerConfig().setGauge(changedGaugeType + resource.getGrooveGauge().NORMAL
										- resource.getGrooveGauge().CLASS);
						}
						main.changeState(MainController.STATE_GRADE_RESULT);
					}
				} else {
					main.getPlayerResource().getPlayerConfig().setGauge(main.getPlayerResource().getOrgGaugeOption());
					ResultKeyProperty.ResultKey key = null;
					for (int i = 0; i < property.getAssignLength(); i++) {
						if (property.getAssign(i) == ResultKeyProperty.ResultKey.REPLAY_DIFFERENT && keyData.getKeyState(i)) {
							key = ResultKeyProperty.ResultKey.REPLAY_DIFFERENT;
							break;
						}
						if (property.getAssign(i) == ResultKeyProperty.ResultKey.REPLAY_SAME && keyData.getKeyState(i)) {
							key = ResultKeyProperty.ResultKey.REPLAY_SAME;
							break;
						}
					}
					if (resource.getPlayMode() == PlayMode.PLAY
							&& key == ResultKeyProperty.ResultKey.REPLAY_DIFFERENT) {
						Logger.getGlobal().info("�궕�깤�궥�깾�꺍�굮鸚됪쎍�걵�걳�꺁�깤�꺃�궎");
						// �궕�깤�궥�깾�꺍�굮鸚됪쎍�걵�걳�릪�걯鈺쒒씊�겎�꺁�깤�꺃�궎
						resource.getReplayData().pattern = null;
						resource.reloadBMSFile();
						main.changeState(MainController.STATE_PLAYBMS);
					} else if (resource.getPlayMode() == PlayMode.PLAY
							&& key == ResultKeyProperty.ResultKey.REPLAY_SAME) {
						// �릪�걯鈺쒒씊�겎�꺁�깤�꺃�궎
						Logger.getGlobal().info("�릪�걯鈺쒒씊�겎�꺁�깤�꺃�궎");
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
				boolean ok = false;
				for (int i = 0; i < property.getAssignLength(); i++) {
					if (property.getAssign(i) != null && keyData.checkIfKeyPressed(i)) {
						keyData.resetKeyTime(i);
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
					if (keyData.getNumberState(i + 1)) {
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
		state = STATE_OFFLINE;
		irrank = irprevrank = irtotal = 0;
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
		next = 0;
		for (int i = 2; i < 9; i++) {
			if (newscore.getExscore() < i * 1111 * (resource.getBMSModel().getTotalNotes() * 2) / 10000) {
				next = newscore.getExscore() - i * 1111 * (resource.getBMSModel().getTotalNotes() * 2) / 10000;
				break;
			}
		}
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

		// �궠�꺖�궧�깴�꺖�깋�겗�졃�릦�겘�궠�꺖�궧�궧�궠�궋�겓�뒥嶸쀣꺕榮�令띲걲�굥
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
				int orgGaugeType = resource.getGrooveGauge().getType();
				if(resource.getPlayerConfig().isContinueUntilEndOfSong() && resource.getCourseIndex() == resource.getCourseBMSModels().length - 1) resource.getGrooveGauge().changeTypeOfClear(resource.getGrooveGauge().getType());
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
				if(resource.getPlayerConfig().isContinueUntilEndOfSong() && resource.getCourseIndex() == resource.getCourseBMSModels().length - 1) resource.getGrooveGauge().setType(orgGaugeType);
			} else {
				cscore.setClear(Failed.id);

				boolean b = false;
				// 餘뗣굤�겗�쎊�걣�걗�굥�졃�릦�겘totalnotes�굮BP�겓�뒥嶸쀣걲�굥
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
			// TODO �궧�궠�궋�깗�긿�궥�깷�걣�걗�굤�곫쐣�듅�쐿�솏�걣�늾�굦�겍�걚�겒�걚�굚�겗�굮�곦에�걲�굥竊�
			IRConnection ir = main.getIRConnection();
			if (ir != null) {
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
					Logger.getGlobal().info("IR�겦�궧�궠�궋�곦에訝�");
					main.switchTimer(TIMER_IR_CONNECT_BEGIN, true);
					state = STATE_IR_PROCESSING;
					Thread irprocess = new Thread() {

						@Override
						public void run() {
							ir.sendPlayData(resource.getBMSModel(), resource.getScoreData());
							IRResponse<IRScoreData[]> response = ir.getPlayData(null, resource.getBMSModel());
							if(response.isSuccessed()) {
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
								main.switchTimer(TIMER_IR_CONNECT_SUCCESS, true);
								Logger.getGlobal().info("IR�겦�궧�궠�궋�곦에若뚥틙");
							} else {
								main.switchTimer(TIMER_IR_CONNECT_FAIL, true);
								Logger.getGlobal().warning("IR�걢�굢�겗�궧�궠�궋�룚孃쀥ㅁ�븮 : " + response.getMessage());
							}

							state = STATE_IR_FINISHED;
						}
					};
					irprocess.start();
				}
			}
		}

		if (newscore.getClear() != Failed.id) {
			play(SOUND_CLEAR);
		} else {
			play(SOUND_FAIL);
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

	public TimingDistribution getTimingDistribution() {
		return timingDistribution;
	}

	public int getTotalNotes() {
		final PlayerResource resource = main.getPlayerResource();
		return resource.getBMSModel().getTotalNotes();
	}

	public int getNumberValue(int id) {
		final PlayerResource resource = main.getPlayerResource();
		switch (id) {
		case NUMBER_CLEAR:
			if (resource.getScoreData() != null) {
				return resource.getScoreData().getClear();
			}
			return Integer.MIN_VALUE;
		case NUMBER_TARGET_CLEAR:
			return oldscore.getClear();
		case NUMBER_HIGHSCORE:
		case NUMBER_HIGHSCORE2:
			return oldscore.getExscore();
		case NUMBER_TARGET_SCORE:
		case NUMBER_TARGET_SCORE2:
			return resource.getRivalScoreData();
		case NUMBER_DIFF_TARGETSCORE:
			return resource.getScoreData().getExscore() - resource.getRivalScoreData();
		case NUMBER_SCORE:
		case NUMBER_SCORE2:
		case NUMBER_SCORE3:
			if (resource.getScoreData() != null) {
				return resource.getScoreData().getExscore();
			}
			return Integer.MIN_VALUE;
		case NUMBER_DIFF_HIGHSCORE:
		case NUMBER_DIFF_HIGHSCORE2:
			return resource.getScoreData().getExscore() - oldscore.getExscore();
		case NUMBER_DIFF_NEXTRANK:
			return next;
		case NUMBER_MISSCOUNT:
		case NUMBER_MISSCOUNT2:
			if (resource.getScoreData() != null) {
				return resource.getScoreData().getMinbp();
			}
			return Integer.MIN_VALUE;
		case NUMBER_TARGET_MISSCOUNT:
			if (oldscore.getMinbp() == Integer.MAX_VALUE) {
				return Integer.MIN_VALUE;
			}
			return oldscore.getMinbp();
		case NUMBER_DIFF_MISSCOUNT:
			if (oldscore.getMinbp() == Integer.MAX_VALUE) {
				return Integer.MIN_VALUE;
			}
			return resource.getScoreData().getMinbp() - oldscore.getMinbp();
		case NUMBER_TARGET_MAXCOMBO:
			if (oldscore.getCombo() > 0) {
				return oldscore.getCombo();
			}
			return Integer.MIN_VALUE;
		case NUMBER_MAXCOMBO:
		case NUMBER_MAXCOMBO2:
			if (resource.getScoreData() != null) {
				return resource.getScoreData().getCombo();
			}
			return Integer.MIN_VALUE;
		case NUMBER_DIFF_MAXCOMBO:
			if (oldscore.getCombo() == 0) {
				return Integer.MIN_VALUE;
			}
			return resource.getScoreData().getCombo() - oldscore.getCombo();
		case NUMBER_GROOVEGAUGE:
			return (int) resource.getGauge()[resource.getGrooveGauge().getType()]
					.get(resource.getGauge()[resource.getGrooveGauge().getType()].size - 1);
		case NUMBER_AVERAGE_DURATION:
			return (int) avgduration;
		case NUMBER_AVERAGE_DURATION_AFTERDOT:
			return ((int) (avgduration * 100)) % 100;
		case NUMBER_IR_RANK:
			if (state != STATE_OFFLINE) {
				return irrank;
			}
			return Integer.MIN_VALUE;
		case NUMBER_IR_PREVRANK:
			if (state != STATE_OFFLINE) {
				return irprevrank;
			}
			return Integer.MIN_VALUE;
		case NUMBER_IR_TOTALPLAYER:
			if (state != STATE_OFFLINE) {
				return irtotal;
			}
			return Integer.MIN_VALUE;
		case NUMBER_AVERAGE_TIMING:
			return (int) timingDistribution.getAverage();
		case NUMBER_AVERAGE_TIMING_AFTERDOT:
			if (timingDistribution.getAverage() >= 0.0) {
				return (int) (timingDistribution.getAverage() * 100) % 100;
			} else {
				return (int) ( -1 * ((Math.abs(timingDistribution.getAverage()) * 100) % 100));
			}
		case NUMBER_STDDEV_TIMING:
			return (int) timingDistribution.getStdDev();
		case NUMBER_STDDEV_TIMING_AFTERDOT:
			return (int) (timingDistribution.getStdDev() * 100) % 100;
		}

		return super.getNumberValue(id);
	}

	public boolean getBooleanValue(int id) {
		final PlayerResource resource = main.getPlayerResource();
		final IRScoreData score = resource.getScoreData();
		final IRScoreData cscore = resource.getCourseScoreData();
		switch (id) {
		case OPTION_DISABLE_SAVE_SCORE:
			return !resource.isUpdateScore();
		case OPTION_ENABLE_SAVE_SCORE:
			return resource.isUpdateScore();
		case OPTION_RESULT_CLEAR:
			return score.getClear() != Failed.id
					&& (cscore == null || cscore.getClear() != Failed.id);
		case OPTION_RESULT_FAIL:
			return score.getClear() == Failed.id
					|| (cscore != null && cscore.getClear() == Failed.id);
		case OPTION_UPDATE_SCORE:
			return score.getExscore() > oldscore.getExscore();
		case OPTION_DRAW_SCORE:
			return score.getExscore() == oldscore.getExscore();
		case OPTION_UPDATE_MAXCOMBO:
			return score.getCombo() > oldscore.getCombo();
		case OPTION_DRAW_MAXCOMBO:
			return score.getCombo() == oldscore.getCombo();
		case OPTION_UPDATE_MISSCOUNT:
			return score.getMinbp() < oldscore.getMinbp();
		case OPTION_DRAW_MISSCOUNT:
			return score.getMinbp() == oldscore.getMinbp();
		case OPTION_UPDATE_SCORERANK:
			return getScoreDataProperty().getNowRate() > getScoreDataProperty().getBestScoreRate();
		case OPTION_DRAW_SCORERANK:
			return getScoreDataProperty().getNowRate() == getScoreDataProperty().getBestScoreRate();
		case OPTION_UPDATE_TARGET:
			return score.getExscore() > resource.getRivalScoreData();
		case OPTION_DRAW_TARGET:
			return score.getExscore() == resource.getRivalScoreData();
		}
		return super.getBooleanValue(id);
	}

	public int getImageIndex(int id) {
		switch (id) {
		case NUMBER_CLEAR:
			final PlayerResource resource = main.getPlayerResource();
			if (resource.getScoreData() != null) {
				return resource.getScoreData().getClear();
			}
			return Integer.MIN_VALUE;
		case NUMBER_TARGET_CLEAR:
			return oldscore.getClear();
		}
		return super.getImageIndex(id);
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

	public class TimingDistribution {
		private final int arrayCenter;
		private int[] dist;
		private float average;
		private float stdDev;

		public TimingDistribution(int range) {
			this.arrayCenter = range;
			this.dist = new int[range * 2 + 1];
		}

		public void statisticValueCalcuate() {
			int count = 0;
			int sum = 0;
			float sumf = 0;

			for (int i = 0; i < dist.length; i++) {
				count += dist[i];
				sum += dist[i] * (i - arrayCenter);
			}

			if (count == 0) {
				return;
			}

			average = sum * 1.0f / count;

			for (int i = 0; i < dist.length; i++) {
				sumf += dist[i] * (i - arrayCenter - average) * (i - arrayCenter - average);
			}

			stdDev = (float) Math.sqrt(sumf / count);
		}

		public void init() {
			Arrays.fill(dist, 0);
			average = Float.MAX_VALUE;
			stdDev = -1.0f;
		}

		public void add(int timing) {
			if (-arrayCenter <= timing && timing <= arrayCenter) {
				dist[timing + arrayCenter]++;
			}
		}

		public int[] getTimingDistribution() {
			return dist;
		}

		public float getAverage() {
			return average;
		}

		public float getStdDev() {
			return stdDev;
		}

		public int getArrayCenter() {
			return arrayCenter;
		}

	}
}
