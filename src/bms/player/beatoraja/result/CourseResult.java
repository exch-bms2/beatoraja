package bms.player.beatoraja.result;

import static bms.player.beatoraja.ClearType.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Arrays;
import java.util.logging.Logger;

import com.badlogic.gdx.utils.FloatArray;

import bms.model.BMSModel;
import bms.player.beatoraja.*;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.skin.SkinType;

public class CourseResult extends MainState {

	private IRScoreData oldscore = new IRScoreData();

	/**
	 * 状態
	 */
	private int state;

	public static final int STATE_OFFLINE = 0;
	public static final int STATE_IR_PROCESSING = 1;
	public static final int STATE_IR_FINISHED = 2;

	private int next;

	private int irrank;
	private int irprevrank;
	private int irtotal;

	private int saveReplay[] = new int[4];
	private static final int replay= 4;

	public static final int SOUND_CLEAR = 0;
	public static final int SOUND_FAIL = 1;
	public static final int SOUND_CLOSE = 2;

	private IRScoreData newscore;

	private ResultKeyProperty property;

	public CourseResult(MainController main) {
		super(main);
	}

	public void create() {
		final PlayerResource resource = main.getPlayerResource();

		setSound(SOUND_CLEAR, "course_clear.wav", SoundType.SOUND,false);
		setSound(SOUND_FAIL, "course_fail.wav", SoundType.SOUND, false);
		setSound(SOUND_CLOSE, "course_close.wav", SoundType.SOUND, false);

		loadSkin(SkinType.COURSE_RESULT);

        for(int i = resource.getCourseGauge().size();i < resource.getCourseBMSModels().length;i++) {
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
			for(int i=0;i<replay;i++){
				if(MusicResult.ReplayAutoSaveConstraint.get(resource.getConfig().getAutoSaveReplay()[i]).isQualified(oldscore ,newscore)) {
					saveReplayData(i);
				}
			}
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

        if (!main.isTimerOn(TIMER_FADEOUT) && main.isTimerOn(TIMER_STARTINPUT)) {
            boolean[] keystate = main.getInputProcessor().getKeystate();
            long[] keytime = main.getInputProcessor().getTime();

			boolean ok = false;
			for(int i = 0; i < property.getAssignLength(); i++) {
				if(property.getAssign(i) != null && keystate[i] && keytime[i] != 0) {
					keytime[i] = 0;
					ok = true;
				}
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
                if (main.getInputProcessor().getNumberState()[i + 1]) {
                    saveReplayData(i);
                    break;
                }
            }
        }
    }

    public void updateScoreDatabase() {
    	Arrays.fill(saveReplay, -1);
		state = STATE_OFFLINE;
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
		newscore.setMaxCombo(resource.getMaxcombo());
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

		IRConnection ir = main.getIRConnection();
		if (ir != null) {
			boolean send = resource.isUpdateScore();
			switch(main.getPlayerConfig().getIrsend()) {
			case PlayerConfig.IR_SEND_ALWAYS:
				break;
			case PlayerConfig.IR_SEND_COMPLETE_SONG:
//				FloatArray gauge = resource.getGauge();
//				send &= gauge.get(gauge.size - 1) > 0.0;
				break;
			case PlayerConfig.IR_SEND_UPDATE_SCORE:
//				IRScoreData current = resource.getScoreData();
//				send &= (current.getExscore() > oldexscore || current.getClear() > oldclear
//						|| current.getCombo() > oldcombo || current.getMinBP() < oldmisscount);
				break;
			}
			
			if(send) {
				Logger.getGlobal().info("IRへスコア送信中(未実装)");
				main.switchTimer(TIMER_IR_CONNECT_BEGIN, true);
				state = STATE_IR_PROCESSING;
				final IRScoreData oldscore = score;
				Thread irprocess = new Thread() {

					@Override
					public void run() {
						main.switchTimer(TIMER_IR_CONNECT_SUCCESS, true);
						Logger.getGlobal().info("IRへスコア送信完了(未実装)");
//						ir.sendPlayData(resource.getBMSModel(), resource.getScoreData());
//						IRResponse<IRScoreData[]> response = ir.getPlayData(null, resource.getBMSModel());
//						if(response.isSuccessed()) {
//							IRScoreData[] scores = response.getData();
//							irtotal = scores.length;
//
//							for(int i = 0;i < scores.length;i++) {
//								if(irrank == 0 && scores[i].getExscore() <= resource.getScoreData().getExscore() ) {
//									irrank = i + 1;
//								}
//								if(irprevrank == 0 && scores[i].getExscore() <= oldscore.getExscore() ) {
//									irprevrank = i + 1;
//									if(irrank == 0) {
//										irrank = irprevrank;
//									}
//								}
//							}
//							setTimerOn(TIMER_IR_CONNECT_SUCCESS, true);
//							Logger.getGlobal().info("IRへスコア送信完了");
//						} else {
//							setTimerOn(TIMER_IR_CONNECT_FAIL, true);
//							Logger.getGlobal().warning("IRからのスコア取得失敗 : " + response.getMessage());
//						}

						state = STATE_IR_FINISHED;
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

	public String getTextValue(int id) {
		final PlayerResource resource = main.getPlayerResource();
		switch (id) {
			case STRING_TITLE:
			case STRING_FULLTITLE:
				return resource.getCoursetitle();
		}
		return super.getTextValue(id);
	}

	public int getNumberValue(int id) {
		final PlayerResource resource = main.getPlayerResource();
		switch (id) {
			case NUMBER_CLEAR:
				if (resource.getCourseScoreData() != null) {
					return resource.getCourseScoreData().getClear();
				}
				return Integer.MIN_VALUE;
			case NUMBER_TARGET_CLEAR:
				return oldscore.getClear();
			case NUMBER_TARGET_SCORE:
				return oldscore.getExscore();
			case NUMBER_SCORE:
				if (resource.getCourseScoreData() != null) {
					return resource.getCourseScoreData().getExscore();
				}
				return Integer.MIN_VALUE;
			case NUMBER_DIFF_HIGHSCORE:
				return resource.getCourseScoreData().getExscore() - oldscore.getExscore();
			case NUMBER_MISSCOUNT:
				if (resource.getCourseScoreData() != null) {
					return resource.getCourseScoreData().getMinBP();
				}
				return Integer.MIN_VALUE;
			case NUMBER_TARGET_MISSCOUNT:
				if (oldscore.getMinBP() == Integer.MAX_VALUE) {
					return Integer.MIN_VALUE;
				}
				return oldscore.getMinBP();
			case NUMBER_DIFF_MISSCOUNT:
				if (oldscore.getMinBP() == Integer.MAX_VALUE) {
					return Integer.MIN_VALUE;
				}
				return resource.getCourseScoreData().getMinBP() - oldscore.getMinBP();
			case NUMBER_TARGET_MAXCOMBO:
				if (oldscore.getMaxCombo() > 0) {
					return oldscore.getMaxCombo();
				}
				return Integer.MIN_VALUE;
			case NUMBER_MAXCOMBO:
				if (resource.getCourseScoreData() != null) {
					return resource.getCourseScoreData().getMaxCombo();
				}
				return Integer.MIN_VALUE;
			case NUMBER_DIFF_MAXCOMBO:
				if (oldscore.getMaxCombo() == 0) {
					return Integer.MIN_VALUE;
				}
				return resource.getCourseScoreData().getMaxCombo() - oldscore.getMaxCombo();
			case NUMBER_TOTALNOTES:
				int notes = 0;
				for (BMSModel model : resource.getCourseBMSModels()) {
					notes += model.getTotalNotes();
				}
				return notes;
		}
		return super.getNumberValue(id);
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	private void saveReplayData(int index) {
		final PlayerResource resource = main.getPlayerResource();
		if (resource.getPlayMode() == PlayMode.PLAY && resource.getCourseScoreData() != null) {
			if (saveReplay[index] == -1 && resource.isUpdateScore()) {
				// 保存されているリプレイデータがない場合は、EASY以上で自動保存
				ReplayData[] rd = resource.getCourseReplay();
				for(int i = 0; i < rd.length; i++) {
					rd[i].gauge = resource.getPlayerConfig().getGauge();
				}
				main.getPlayDataAccessor().wrireReplayData(rd, resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), index, resource.getConstraint());
				saveReplay[index] = 1;
			}
		}
	}

	public boolean getBooleanValue(int id) {
		final PlayerResource resource = main.getPlayerResource();
		final IRScoreData score = resource.getCourseScoreData();
		switch (id) {
			case OPTION_RESULT_CLEAR:
				return score.getClear() != Failed.id;
			case OPTION_RESULT_FAIL:
				return score.getClear() == Failed.id;
			case OPTION_UPDATE_SCORE:
				return score.getExscore() > oldscore.getExscore();
			case OPTION_DRAW_SCORE:
				return score.getExscore() == oldscore.getExscore();
			case OPTION_UPDATE_MAXCOMBO:
				return score.getMaxCombo() > oldscore.getMaxCombo();
			case OPTION_DRAW_MAXCOMBO:
				return score.getMaxCombo() == oldscore.getMaxCombo();
			case OPTION_UPDATE_MISSCOUNT:
				return score.getMinBP() < oldscore.getMinBP();
			case OPTION_DRAW_MISSCOUNT:
				return score.getMinBP() == oldscore.getMinBP();
			case OPTION_UPDATE_SCORERANK:
				return getScoreDataProperty().getNowRate() > getScoreDataProperty().getBestScoreRate();
			case OPTION_DRAW_SCORERANK:
				return getScoreDataProperty().getNowRate() == getScoreDataProperty().getBestScoreRate();
			case OPTION_NO_REPLAYDATA:
				return !main.getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), 0,resource.getConstraint());
			case OPTION_NO_REPLAYDATA2:
				return !main.getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), 1,resource.getConstraint());
			case OPTION_NO_REPLAYDATA3:
				return !main.getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), 2,resource.getConstraint());
			case OPTION_NO_REPLAYDATA4:
				return !main.getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), 3,resource.getConstraint());
			case OPTION_REPLAYDATA:
				return main.getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), 0,resource.getConstraint());
			case OPTION_REPLAYDATA2:
				return main.getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), 1,resource.getConstraint());
			case OPTION_REPLAYDATA3:
				return main.getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), 2,resource.getConstraint());
			case OPTION_REPLAYDATA4:
				return main.getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), 3,resource.getConstraint());
			case OPTION_REPLAYDATA_SAVED:
				return saveReplay[0] == 1;
			case OPTION_REPLAYDATA2_SAVED:
				return saveReplay[1] == 1;
			case OPTION_REPLAYDATA3_SAVED:
				return saveReplay[2] == 1;
			case OPTION_REPLAYDATA4_SAVED:
				return saveReplay[3] == 1;
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

	public void executeClickEvent(int id) {
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
}