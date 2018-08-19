package bms.player.beatoraja.result;

import static bms.player.beatoraja.ClearType.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.logging.Logger;

import com.badlogic.gdx.utils.FloatArray;

import bms.model.BMSModel;
import bms.player.beatoraja.*;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.ir.IRResponse;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.skin.SkinType;

/**
 * コースリザルト
 *
 * @author exch
 */
public class CourseResult extends AbstractResult {

	private IRScoreData newscore;

	private ResultKeyProperty property;

	public CourseResult(MainController main) {
		super(main);
	}

	public void create() {
		final PlayerResource resource = main.getPlayerResource();

		for(int i = 0;i < REPLAY_SIZE;i++) {
			saveReplay[i] = main.getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
					resource.getPlayerConfig().getLnmode(), i ,resource.getConstraint()) ? ReplayStatus.EXIST : ReplayStatus.NOT_EXIST ;
		}

		setSound(SOUND_CLEAR, "course_clear.wav", SoundType.SOUND,false);
		setSound(SOUND_FAIL, "course_fail.wav", SoundType.SOUND, false);
		setSound(SOUND_CLOSE, "course_close.wav", SoundType.SOUND, false);

		loadSkin(SkinType.COURSE_RESULT);

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
		if(resource.getPlayMode() == PlayMode.PLAY){
			for(int i=0;i<REPLAY_SIZE;i++){
				if(MusicResult.ReplayAutoSaveConstraint.get(resource.getConfig().getAutoSaveReplay()[i]).isQualified(oldscore ,newscore)) {
					saveReplayData(i);
				}
			}
		}

		gaugeType = resource.getGrooveGauge().getType();
	}
	
	public void prepare() {
		state = STATE_OFFLINE;
		final PlayerResource resource = main.getPlayerResource();
		final PlayerConfig config = resource.getPlayerConfig();

		final IRConnection ir = main.getIRConnection();
		if (ir != null && resource.getPlayMode() == PlayMode.PLAY) {
			boolean send = resource.isUpdateScore() && resource.getCourseData().isRelease();
			switch(main.getPlayerConfig().getIrsend()) {
			case PlayerConfig.IR_SEND_ALWAYS:
				break;
			case PlayerConfig.IR_SEND_COMPLETE_SONG:
//				FloatArray gauge = resource.getG.getGauge()[resource.getGrooveGauge().getType()];
//				send &= gauge.get(gauge.size - 1) > 0.0;
				break;
			case PlayerConfig.IR_SEND_UPDATE_SCORE:
//				IRScoreData current = resource.getScoreData();
//				send &= (current.getExscore() > oldexscore || current.getClear() > oldclear
//						|| current.getCombo() > oldcombo || current.getMinbp() < oldmisscount);
				break;
			}

			if(send) {
				Logger.getGlobal().info("IRへスコア送信中(未実装)");
				main.switchTimer(TIMER_IR_CONNECT_BEGIN, true);
				state = STATE_IR_PROCESSING;
				Thread irprocess = new Thread() {

					@Override
					public void run() {
						try {
							Logger.getGlobal().info("IRへスコア送信");
							int lnmode = 0;
							for(BMSModel model : resource.getCourseBMSModels()) {
								if(model.containsUndefinedLongNote()) {
									lnmode = config.getLnmode();
									break;
								}
							}
							IRResponse<Object> send = ir.sendCoursePlayData(resource.getCourseData(), lnmode, resource.getCourseScoreData());
							if(send.isSucceeded()) {
								main.switchTimer(TIMER_IR_CONNECT_SUCCESS, true);
								Logger.getGlobal().info("IRスコア送信完了");
							} else {
								main.switchTimer(TIMER_IR_CONNECT_FAIL, true);
								Logger.getGlobal().warning("IRスコア送信失敗 : " + send.getMessage());
							}
							IRResponse<IRScoreData[]> response = ir.getCoursePlayData(null, resource.getCourseData(), lnmode);
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
					}
				};
				irprocess.start();
			}
		}

		if (newscore.getClear() != Failed.id) {
			play(SOUND_CLEAR);
		} else {
			play(SOUND_FAIL);
		}
	}

	public void render() {
		long time = main.getNowTime();
		main.switchTimer(TIMER_RESULTGRAPH_BEGIN, true);
		main.switchTimer(TIMER_RESULTGRAPH_END, true);
		main.switchTimer(TIMER_RESULT_UPDATESCORE, true);

		if(time > getSkin().getInput()){
			main.switchTimer(TIMER_STARTINPUT, true);
		}

		if (main.isTimerOn(TIMER_FADEOUT)) {
			if (main.getNowTime(TIMER_FADEOUT) > getSkin().getFadeout()) {
				main.getPlayerResource().getPlayerConfig().setGauge(main.getPlayerResource().getOrgGaugeOption());
				stop(SOUND_CLEAR);
				stop(SOUND_FAIL);
				stop(SOUND_CLOSE);
				main.changeState(MainController.STATE_SELECTMUSIC);
			}
		} else if (time > getSkin().getScene()) {
			main.switchTimer(TIMER_FADEOUT, true);
			if(getSound(SOUND_CLOSE) != null) {
				stop(SOUND_CLEAR);
				stop(SOUND_FAIL);
				play(SOUND_CLOSE);
			}
		}

	}

	public void input() {
		final PlayerResource resource = main.getPlayerResource();
		final BMSPlayerInputProcessor inputProcessor = main.getInputProcessor();

		if (!main.isTimerOn(TIMER_FADEOUT) && main.isTimerOn(TIMER_STARTINPUT)) {
			boolean[] keystate = inputProcessor.getKeystate();
			long[] keytime = inputProcessor.getTime();

			boolean ok = false;
			for (int i = 0; i < property.getAssignLength(); i++) {
				if (property.getAssign(i) == ResultKeyProperty.ResultKey.CHANGE_GRAPH && keystate[i] && keytime[i] != 0) {
					gaugeType = (gaugeType - 5) % 3 + 6;
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
				if (((CourseResultSkin) getSkin()).getRankTime() != 0 && !main.isTimerOn(TIMER_RESULT_UPDATESCORE)) {
					main.switchTimer(TIMER_RESULT_UPDATESCORE, true);
				} else if (state == STATE_OFFLINE || state == STATE_IR_FINISHED){
					main.switchTimer(TIMER_FADEOUT, true);
					if(getSound(SOUND_CLOSE) != null) {
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

	public void updateScoreDatabase() {
		final PlayerResource resource = main.getPlayerResource();
		final PlayerConfig config = resource.getPlayerConfig();
		BMSModel[] models = resource.getCourseBMSModels();
		newscore = resource.getCourseScoreData();
		if (newscore == null) {
			return;
		}
		boolean dp = false;
		for (BMSModel model : models) {
			dp |= model.getMode().player == 2;
		}
		newscore.setCombo(resource.getMaxcombo());
		int random = 0;
		if (config.getRandom() > 0
				|| (dp && (config.getRandom2() > 0 || config.getDoubleoption() > 0))) {
			random = 2;
		}
		if (config.getRandom() == 1
				&& (!dp || (config.getRandom2() == 1 && config.getDoubleoption() == 1))) {
			random = 1;
		}
		IRScoreData score = main.getPlayDataAccessor().readScoreData(models,
				config.getLnmode(), random, resource.getConstraint());
		if (score != null) {
			oldscore = score;
		}else{
			oldscore = new IRScoreData();
		}

		getScoreDataProperty().setTargetScore(oldscore.getExscore(), resource.getRivalScoreData(), resource.getBMSModel().getTotalNotes());
		getScoreDataProperty().update(newscore);

		main.getPlayDataAccessor().writeScoreDara(newscore, models, config.getLnmode(),
				random, resource.getConstraint(), resource.isUpdateScore());


		Logger.getGlobal().info("スコアデータベース更新完了 ");
	}

	public int getJudgeCount(int judge, boolean fast) {
		final PlayerResource resource = main.getPlayerResource();
		IRScoreData score = resource.getCourseScoreData();
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

	private void saveReplayData(int index) {
		final PlayerResource resource = main.getPlayerResource();
		if (resource.getPlayMode() == PlayMode.PLAY && resource.getCourseScoreData() != null) {
			if (saveReplay[index] != ReplayStatus.SAVED && resource.isUpdateScore()) {
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
		return main.getPlayerResource().getCourseScoreData();
	}
}
