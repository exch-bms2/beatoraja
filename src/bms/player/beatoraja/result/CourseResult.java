package bms.player.beatoraja.result;

import static bms.player.beatoraja.ClearType.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Arrays;
import java.util.logging.Logger;

import com.badlogic.gdx.utils.FloatArray;

import bms.model.BMSModel;
import bms.player.beatoraja.*;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.skin.SkinType;

public class CourseResult extends MainState {

	private int oldclear;
	private int oldexscore;
	private int oldmisscount;
	private int oldcombo;

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
		final PlayerResource resource = getMainController().getPlayerResource();

		setSound(SOUND_CLEAR, "course_clear.wav", SoundType.SOUND,false);
		setSound(SOUND_FAIL, "course_fail.wav", SoundType.SOUND, false);
		setSound(SOUND_CLOSE, "course_close.wav", SoundType.SOUND, false);

		loadSkin(SkinType.COURSE_RESULT);

        for(int i = resource.getCourseGauge().size();i < resource.getCourseBMSModels().length;i++) {
        	FloatArray list = new FloatArray();
            for(int l = 0;l < (resource.getCourseBMSModels()[i].getLastNoteTime() + 500) / 500;l++) {
                list.add(0f);
            }
            resource.getCourseGauge().add(list);
        }

		property = ResultKeyProperty.get(resource.getBMSModel().getMode());
		if(property == null) {
			property = ResultKeyProperty.BEAT_7K;
		}

		updateScoreDatabase();

		// リプレイの自動保存
		if(resource.getAutoplay() == 0){
			for(int i=0;i<replay;i++){
				/*
				 * コンフィグ値:0=保存しない 1=スコア更新時 2=スコアが自己ベスト以上 3=BP更新時 4=BPが自己ベスト以下
				 * 5=COMBO更新時 6=COMBOが自己ベスト以上 7=ランプ更新時 8=ランプが自己ベスト以上 9=何か更新した時 10=毎回
				 */
				switch(resource.getConfig().getAutoSaveReplay()[i]){
				case 0:
					break;
				case 1:
					if(newscore.getExscore() > oldexscore)
						saveReplayData(i);
					break;
				case 2:
					if(newscore.getExscore() >= oldexscore)
						saveReplayData(i);
					break;
				case 3:
					if(newscore.getMinbp() < oldmisscount || oldclear == NoPlay.id)
						saveReplayData(i);
					break;
				case 4:
					if(newscore.getMinbp() <= oldmisscount || oldclear == NoPlay.id)
						saveReplayData(i);
					break;
				case 5:
					if(newscore.getCombo() > oldcombo)
						saveReplayData(i);
					break;
				case 6:
					if(newscore.getCombo() >= oldcombo)
						saveReplayData(i);
					break;
				case 7:
					if(newscore.getClear() > oldclear)
						saveReplayData(i);
					break;
				case 8:
					if(newscore.getClear() >= oldclear)
						saveReplayData(i);
					break;
				case 9:
					if(newscore.getClear() > oldclear || newscore.getCombo() > oldcombo||
						newscore.getMinbp() < oldmisscount || newscore.getExscore() > oldexscore)
						saveReplayData(i);
					break;
				case 10:
						saveReplayData(i);
					break;
				}
			}
		}
	}

	public void render() {
		int time = getNowTime();
		if (getTimer()[TIMER_RESULTGRAPH_BEGIN] == Long.MIN_VALUE) {
			getTimer()[TIMER_RESULTGRAPH_BEGIN] = time;
		}
		if (getTimer()[TIMER_RESULTGRAPH_END] == Long.MIN_VALUE) {
			getTimer()[TIMER_RESULTGRAPH_END] = time;
		}
		if (getTimer()[TIMER_RESULT_UPDATESCORE] == Long.MIN_VALUE) {
			getTimer()[TIMER_RESULT_UPDATESCORE] = time;
		}

        if(getTimer()[TIMER_STARTINPUT] == Long.MIN_VALUE && time > getSkin().getInput()){
        	getTimer()[TIMER_STARTINPUT] =  time;
        }

		final MainController main = getMainController();

		if (getTimer()[TIMER_FADEOUT] != Long.MIN_VALUE) {
			if (time > getTimer()[TIMER_FADEOUT] + getSkin().getFadeout()) {
				stop(SOUND_CLEAR);
				stop(SOUND_FAIL);
				stop(SOUND_CLOSE);
				main.changeState(MainController.STATE_SELECTMUSIC);
			}
		} else if (time > getSkin().getScene()) {
            getTimer()[TIMER_FADEOUT] = time;
			if(getSound(SOUND_CLOSE) != null) {
				stop(SOUND_CLEAR);
				stop(SOUND_FAIL);
				play(SOUND_CLOSE);
			}
		}

	}

    public void input() {
        int time = getNowTime();
        final MainController main = getMainController();
        final PlayerResource resource = getMainController().getPlayerResource();

        if (getTimer()[TIMER_FADEOUT] == Long.MIN_VALUE && getTimer()[TIMER_STARTINPUT] != Long.MIN_VALUE) {
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
                if (((CourseResultSkin) getSkin()).getRankTime() != 0 && getTimer()[TIMER_RESULT_UPDATESCORE] == Long.MIN_VALUE) {
                    getTimer()[TIMER_RESULT_UPDATESCORE] = time;
                } else {
                    getTimer()[TIMER_FADEOUT] = time;
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
		final PlayerResource resource = getMainController().getPlayerResource();
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
		IRScoreData score = getMainController().getPlayDataAccessor().readScoreData(models,
				config.getLnmode(), random, resource.getConstraint());
		if (score == null) {
			score = new IRScoreData();
		}
		oldclear = score.getClear();
		oldexscore = score.getExscore();
		oldmisscount = score.getMinbp();
		oldcombo = score.getCombo();

		getScoreDataProperty().setTargetScore(oldexscore, resource.getRivalScoreData(), resource.getBMSModel().getTotalNotes());
		getScoreDataProperty().update(newscore);

		getMainController().getPlayDataAccessor().writeScoreDara(newscore, models, config.getLnmode(),
				random, resource.getConstraint(), resource.isUpdateScore());

		if (newscore.getClear() != Failed.id) {
			play(SOUND_CLEAR);
		} else {
			play(SOUND_FAIL);
		}

		Logger.getGlobal().info("スコアデータベース更新完了 ");
	}

	public int getJudgeCount(int judge, boolean fast) {
		final PlayerResource resource = getMainController().getPlayerResource();
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
		final PlayerResource resource = getMainController().getPlayerResource();
		switch (id) {
			case STRING_TITLE:
			case STRING_FULLTITLE:
				return resource.getCoursetitle();
		}
		return super.getTextValue(id);
	}

	public int getNumberValue(int id) {
		final PlayerResource resource = getMainController().getPlayerResource();
		switch (id) {
			case NUMBER_CLEAR:
				if (resource.getCourseScoreData() != null) {
					return resource.getCourseScoreData().getClear();
				}
				return Integer.MIN_VALUE;
			case NUMBER_TARGET_CLEAR:
				return oldclear;
			case NUMBER_TARGET_SCORE:
				return oldexscore;
			case NUMBER_SCORE:
				if (resource.getCourseScoreData() != null) {
					return resource.getCourseScoreData().getExscore();
				}
				return Integer.MIN_VALUE;
			case NUMBER_DIFF_HIGHSCORE:
				return resource.getCourseScoreData().getExscore() - oldexscore;
			case NUMBER_MISSCOUNT:
				if (resource.getCourseScoreData() != null) {
					return resource.getCourseScoreData().getMinbp();
				}
				return Integer.MIN_VALUE;
			case NUMBER_TARGET_MISSCOUNT:
				if (oldmisscount == Integer.MAX_VALUE) {
					return Integer.MIN_VALUE;
				}
				return oldmisscount;
			case NUMBER_DIFF_MISSCOUNT:
				if (oldmisscount == Integer.MAX_VALUE) {
					return Integer.MIN_VALUE;
				}
				return resource.getCourseScoreData().getMinbp() - oldmisscount;
			case NUMBER_TARGET_MAXCOMBO:
				if (oldcombo > 0) {
					return oldcombo;
				}
				return Integer.MIN_VALUE;
			case NUMBER_MAXCOMBO:
				if (resource.getCourseScoreData() != null) {
					return resource.getCourseScoreData().getCombo();
				}
				return Integer.MIN_VALUE;
			case NUMBER_DIFF_MAXCOMBO:
				if (oldcombo == 0) {
					return Integer.MIN_VALUE;
				}
				return resource.getCourseScoreData().getCombo() - oldcombo;
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
		final PlayerResource resource = getMainController().getPlayerResource();
		if (resource.getAutoplay() == 0 && resource.getCourseScoreData() != null) {
			if (saveReplay[index] == -1 && resource.isUpdateScore()) {
				// 保存されているリプレイデータがない場合は、EASY以上で自動保存
				ReplayData[] rd = resource.getCourseReplay();
				getMainController().getPlayDataAccessor().wrireReplayData(rd, resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), index, resource.getConstraint());
				saveReplay[index] = 1;
			}
		}
	}

	public boolean getBooleanValue(int id) {
		final PlayerResource resource = getMainController().getPlayerResource();
		final IRScoreData score = resource.getCourseScoreData();
		switch (id) {
			case OPTION_RESULT_CLEAR:
				return score.getClear() != Failed.id;
			case OPTION_RESULT_FAIL:
				return score.getClear() == Failed.id;
			case OPTION_UPDATE_SCORE:
				return score.getExscore() > oldexscore;
			case OPTION_DRAW_SCORE:
				return score.getExscore() == oldexscore;
			case OPTION_UPDATE_MAXCOMBO:
				return score.getCombo() > oldcombo;
			case OPTION_DRAW_MAXCOMBO:
				return score.getCombo() == oldcombo;
			case OPTION_UPDATE_MISSCOUNT:
				return score.getMinbp() < oldmisscount;
			case OPTION_DRAW_MISSCOUNT:
				return score.getMinbp() == oldmisscount;
			case OPTION_UPDATE_SCORERANK:
				return getScoreDataProperty().getNowRate() > getScoreDataProperty().getBestScoreRate();
			case OPTION_DRAW_SCORERANK:
				return getScoreDataProperty().getNowRate() == getScoreDataProperty().getBestScoreRate();
			case OPTION_NO_REPLAYDATA:
				return !getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), 0,resource.getConstraint());
			case OPTION_NO_REPLAYDATA2:
				return !getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), 1,resource.getConstraint());
			case OPTION_NO_REPLAYDATA3:
				return !getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), 2,resource.getConstraint());
			case OPTION_NO_REPLAYDATA4:
				return !getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), 3,resource.getConstraint());
			case OPTION_REPLAYDATA:
				return getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), 0,resource.getConstraint());
			case OPTION_REPLAYDATA2:
				return getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), 1,resource.getConstraint());
			case OPTION_REPLAYDATA3:
				return getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getPlayerConfig().getLnmode(), 2,resource.getConstraint());
			case OPTION_REPLAYDATA4:
				return getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
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
				final PlayerResource resource = getMainController().getPlayerResource();
				if (resource.getScoreData() != null) {
					return resource.getScoreData().getClear();
				}
				return Integer.MIN_VALUE;
			case NUMBER_TARGET_CLEAR:
				return oldclear;
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