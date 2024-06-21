package bms.player.beatoraja.result;

import static bms.player.beatoraja.ClearType.*;
import static bms.player.beatoraja.skin.SkinProperty.*;
import static bms.player.beatoraja.SystemSoundManager.SoundType.*;

import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.input.KeyCommand;
import com.badlogic.gdx.utils.FloatArray;

import bms.model.BMSModel;
import bms.player.beatoraja.*;
import bms.player.beatoraja.MainController.IRStatus;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyBoardInputProcesseor.ControlKeys;
import bms.player.beatoraja.ir.*;
import bms.player.beatoraja.skin.SkinType;
import bms.player.beatoraja.skin.property.EventFactory.EventType;

/**
 * コースリザルト
 *
 * @author exch
 */
public class CourseResult extends AbstractResult {

	private List<IRSendStatus> irSendStatus = new ArrayList<IRSendStatus>();

	private ResultKeyProperty property;

	public CourseResult(MainController main) {
		super(main);
	}

	public void create() {
		for(int i = 0;i < REPLAY_SIZE;i++) {
			saveReplay[i] = main.getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
					resource.getPlayerConfig().getLnmode(), i ,resource.getConstraint()) ? ReplayStatus.EXIST : ReplayStatus.NOT_EXIST ;
		}

		for(int i = resource.getCourseGauge().size;i < resource.getCourseBMSModels().length;i++) {
			FloatArray[] list = new FloatArray[resource.getGrooveGauge().getGaugeTypeLength()];
			for(int type = 0; type < list.length; type++) {
				list[type] = new FloatArray();
				for(int l = 0;l < (resource.getCourseBMSModels()[i].getLastNoteTime() + 500) / 500;l++) {
					list[type].add(0f);
				}
			}
			resource.getCourseGauge().add(list);
		}

		property = ResultKeyProperty.get(resource.getBMSModel().getMode());
		if(property == null) {
			property = ResultKeyProperty.BEAT_7K;
		}

		updateScoreDatabase();

		// リプレイの自動保存
		if(resource.getPlayMode().mode == BMSPlayerMode.Mode.PLAY){
			for(int i=0;i<REPLAY_SIZE;i++){
				if(MusicResult.ReplayAutoSaveConstraint.get(resource.getPlayerConfig().getAutoSaveReplay()[i]).isQualified(oldscore ,getNewScore())) {
					saveReplayData(i);
				}
			}
		}

		gaugeType = resource.getGrooveGauge().getType();

		loadSkin(SkinType.COURSE_RESULT);
	}
	
	public void prepare() {
		state = STATE_OFFLINE;
		final PlayerConfig config = resource.getPlayerConfig();
		final ScoreData newscore = getNewScore();

		ranking = resource.getRankingData() != null && resource.getCourseBMSModels() != null ? resource.getRankingData() : new RankingData();
		rankingOffset = 0;
		final IRStatus[] ir = main.getIRStatus();
		if (ir.length > 0 && resource.getPlayMode().mode == BMSPlayerMode.Mode.PLAY) {
			state = STATE_IR_PROCESSING;
			
			boolean uln = false;
			for(BMSModel model : resource.getCourseBMSModels()) {
				if(model.containsUndefinedLongNote()) {
					uln = true;
					break;
				}
			}
			final int lnmode = uln ? config.getLnmode() : 0;
			
        	for(IRStatus irc : ir) {
    			boolean send = resource.isUpdateCourseScore() && resource.getCourseData().isRelease();
    			switch(irc.config.getIrsend()) {
    			case IRConfig.IR_SEND_ALWAYS:
    				break;
    			case IRConfig.IR_SEND_COMPLETE_SONG:
//    				FloatArray gauge = resource.getGauge()[resource.getGrooveGauge().getType()];
//    				send &= gauge.get(gauge.size - 1) > 0.0;
    				break;
    			case IRConfig.IR_SEND_UPDATE_SCORE:
//    				send &= (newscore.getExscore() > oldscore.getExscore() || newscore.getClear() > oldscore.getClear()
//    						|| newscore.getCombo() > oldscore.getCombo() || newscore.getMinbp() < oldscore.getMinbp());
    				break;
    			}
    			
    			if(send) {
    				irSendStatus.add(new IRSendStatus(irc.connection, resource.getCourseData(), lnmode, newscore));
    			}
        	}

			Thread irprocess = new Thread(() -> {
				int irsend = 0;
				boolean succeed = true;
				List<IRSendStatus> removeIrSendStatus = new ArrayList<>();

				for (IRSendStatus irc : irSendStatus) {
					try {
						if (irsend == 0) {
							timer.switchTimer(TIMER_IR_CONNECT_BEGIN, true);
						}
						irsend++;
						succeed &= irc.send();
						if (irc.retry < 0 || irc.retry > main.getConfig().getIrSendCount()) {
							removeIrSendStatus.add(irc);
						}
					} catch (Exception e) {
						Logger.getGlobal().warning("IR送信時の例外:" + e.getMessage());
						e.printStackTrace();
						// remove from queue
						removeIrSendStatus.add(irc);
					}
				}
				irSendStatus.removeAll(removeIrSendStatus);

				if (irsend > 0) {
					timer.switchTimer(succeed ? TIMER_IR_CONNECT_SUCCESS : TIMER_IR_CONNECT_FAIL, true);
					try {
						IRResponse<bms.player.beatoraja.ir.IRScoreData[]> response = ir[0].connection.getCoursePlayData(null, new IRCourseData(resource.getCourseData(), lnmode));
						if (response.isSucceeded()) {
							ranking.updateScore(response.getData(), newscore.getExscore() > oldscore.getExscore() ? newscore : oldscore);
							rankingOffset = ranking.getRank() > 10 ? ranking.getRank() - 5 : 0;
							Logger.getGlobal().info("IRからのスコア取得成功 : " + response.getMessage());
						} else {
							Logger.getGlobal().warning("IRからのスコア取得失敗 : " + response.getMessage());
						}
					} catch (Exception e) {
						Logger.getGlobal().warning("IRからのスコア取得時例外:" + e.getMessage());
						e.printStackTrace();
					}
				}
				state = STATE_IR_FINISHED;
			});
			irprocess.start();
		}

		play(newscore.getClear() != Failed.id ? (getSound(COURSE_CLEAR) != null ? COURSE_CLEAR : RESULT_CLEAR)
				: (getSound(COURSE_FAIL) != null ? COURSE_FAIL : RESULT_FAIL), resource.getConfig().getAudioConfig().isLoopCourseResultSound());
	}

	public void shutdown() {
		stop(getSound(COURSE_CLEAR) != null ? COURSE_CLEAR : RESULT_CLEAR);
		stop(getSound(COURSE_FAIL) != null ? COURSE_FAIL : RESULT_FAIL);
		stop(getSound(COURSE_CLOSE) != null ? COURSE_CLOSE : RESULT_CLOSE);
	}

	public void render() {
		long time = timer.getNowTime();
		timer.switchTimer(TIMER_RESULTGRAPH_BEGIN, true);
		timer.switchTimer(TIMER_RESULTGRAPH_END, true);
		timer.switchTimer(TIMER_RESULT_UPDATESCORE, true);

		if(time > getSkin().getInput()){
			timer.switchTimer(TIMER_STARTINPUT, true);
		}

		if (timer.isTimerOn(TIMER_FADEOUT)) {
			if (timer.getNowTime(TIMER_FADEOUT) > getSkin().getFadeout()) {
				resource.getPlayerConfig().setGauge(resource.getOrgGaugeOption());
				main.changeState(MainStateType.MUSICSELECT);
			}
		} else if (time > getSkin().getScene()) {
			timer.switchTimer(TIMER_FADEOUT, true);
			if(getSound(COURSE_CLOSE) != null || getSound(RESULT_CLOSE) != null) {
				stop(getSound(COURSE_CLEAR) != null ? COURSE_CLEAR : RESULT_CLEAR);
				stop(getSound(COURSE_FAIL) != null ? COURSE_FAIL : RESULT_FAIL);
				play(getSound(COURSE_CLOSE) != null ? COURSE_CLOSE : RESULT_CLOSE);
			}
		}

	}

	public void input() {
		super.input();
		final BMSPlayerInputProcessor inputProcessor = main.getInputProcessor();

		if (!timer.isTimerOn(TIMER_FADEOUT) && timer.isTimerOn(TIMER_STARTINPUT)) {
			boolean ok = false;
			for (int i = 0; i < property.getAssignLength(); i++) {
				if (property.getAssign(i) == ResultKeyProperty.ResultKey.CHANGE_GRAPH && inputProcessor.getKeyState(i) && inputProcessor.resetKeyChangedTime(i)) {
					gaugeType = (gaugeType - 5) % 3 + 6;
				} else if (property.getAssign(i) != null && inputProcessor.getKeyState(i) && inputProcessor.resetKeyChangedTime(i)) {
					ok = true;
				}
			}

			if (inputProcessor.isControlKeyPressed(ControlKeys.ESCAPE) || inputProcessor.isControlKeyPressed(ControlKeys.ENTER)) {
				ok = true;
			} 

			if (resource.getScoreData() == null || ok) {
				if (((CourseResultSkin) getSkin()).getRankTime() != 0 && !timer.isTimerOn(TIMER_RESULT_UPDATESCORE)) {
					timer.switchTimer(TIMER_RESULT_UPDATESCORE, true);
				} else if (state == STATE_OFFLINE || state == STATE_IR_FINISHED){
					timer.switchTimer(TIMER_FADEOUT, true);
					if(getSound(COURSE_CLOSE) != null || getSound(RESULT_CLOSE) != null) {
						stop(getSound(COURSE_CLEAR) != null ? COURSE_CLEAR : RESULT_CLEAR);
						stop(getSound(COURSE_FAIL) != null ? COURSE_FAIL : RESULT_FAIL);
						play(getSound(COURSE_CLOSE) != null ? COURSE_CLOSE : RESULT_CLOSE);
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

	public void updateScoreDatabase() {
		final PlayerConfig config = resource.getPlayerConfig();
		BMSModel[] models = resource.getCourseBMSModels();
		final ScoreData newscore = getNewScore();
		if (newscore == null) {
			return;
		}
		boolean dp = false;
		for (BMSModel model : models) {
			dp |= model.getMode().player == 2;
		}
		newscore.setCombo(resource.getMaxcombo());
		newscore.setAvgjudge(newscore.getTotalDuration() / newscore.getNotes());
		int random = 0;
		if (config.getRandom() > 0
				|| (dp && (config.getRandom2() > 0 || config.getDoubleoption() > 0))) {
			random = 2;
		}
		if (config.getRandom() == 1
				&& (!dp || (config.getRandom2() == 1 && config.getDoubleoption() == 1))) {
			random = 1;
		}
		final ScoreData score = main.getPlayDataAccessor().readScoreData(models,
				config.getLnmode(), random, resource.getConstraint());
		oldscore = score != null ? score : new ScoreData();

		getScoreDataProperty().setTargetScore(oldscore.getExscore(), resource.getTargetScoreData() != null ? resource.getTargetScoreData().getExscore() : 0,
				Arrays.asList(resource.getCourseData().getSong()).stream().mapToInt(sd -> sd.getNotes()).sum());
		getScoreDataProperty().update(newscore);

		main.getPlayDataAccessor().writeScoreData(newscore, models, config.getLnmode(),
				random, resource.getConstraint(), resource.isUpdateCourseScore());


		Logger.getGlobal().info("スコアデータベース更新完了 ");
	}

	public int getJudgeCount(int judge, boolean fast) {
		ScoreData score = resource.getCourseScoreData();
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

	public void saveReplayData(int index) {
		if (resource.getPlayMode().mode == BMSPlayerMode.Mode.PLAY && resource.getCourseScoreData() != null) {
			if (saveReplay[index] != ReplayStatus.SAVED && resource.isUpdateCourseScore()) {
				// 保存されているリプレイデータがない場合は、EASY以上で自動保存
				ReplayData[] rd = resource.getCourseReplay();
				for(int i = 0; i < rd.length; i++) {
					rd[i].gauge = resource.getPlayerConfig().getGauge();
				}
				main.getPlayDataAccessor().wrireReplayData(rd, resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), index, resource.getConstraint());
				saveReplay[index] = ReplayStatus.SAVED;
			}
		}
	}

	public ScoreData getNewScore() {
		return resource.getCourseScoreData();
	}

	static class IRSendStatus {
		public final IRConnection ir;
		public final CourseData course;
		public final int lnmode;
		public final ScoreData score;
		public int retry = 0;
		
		public IRSendStatus(IRConnection ir, CourseData course, int lnmode, ScoreData score) {
			this.ir = ir;
			this.course = course;
			this.lnmode = lnmode;
			this.score = score;
		}
		
		public boolean send() {
			Logger.getGlobal().info("IRへスコア送信中 : " + course.getName());
            IRResponse<Object> send1 = ir.sendCoursePlayData(new IRCourseData(course, lnmode), new bms.player.beatoraja.ir.IRScoreData(score));
            if(send1.isSucceeded()) {
                Logger.getGlobal().info("IRスコア送信完了 : " + course.getName());
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
