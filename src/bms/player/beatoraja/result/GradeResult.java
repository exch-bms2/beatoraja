package bms.player.beatoraja.result;

import static bms.player.beatoraja.ClearType.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;

import com.badlogic.gdx.utils.FloatArray;

import bms.model.BMSModel;
import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.ReplayData;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.skin.SkinType;

public class GradeResult extends MainState {

	private int oldclear;
	private int oldexscore;
	private int oldmisscount;
	private int oldcombo;

	private int saveReplay[] = new int[4];
	private static final int replay= 4;

	public static final int SOUND_CLEAR = 0;
	public static final int SOUND_FAIL = 1;

	public GradeResult(MainController main) {
		super(main);
	}

	public void create() {
		final PlayerResource resource = getMainController().getPlayerResource();

		setSound(SOUND_CLEAR, resource.getConfig().getSoundpath() + File.separatorChar + "course_clear.wav", false);
		setSound(SOUND_FAIL, resource.getConfig().getSoundpath() + File.separatorChar + "course_fail.wav", false);

		loadSkin(SkinType.COURSE_RESULT);

        for(int i = resource.getCourseGauge().size();i < resource.getCourseBMSModels().length;i++) {
        	FloatArray list = new FloatArray();
            for(int l = 0;l < (resource.getCourseBMSModels()[i].getLastNoteTime() + 500) / 500;l++) {
                list.add(0f);
            }
            resource.getCourseGauge().add(list);
        }

		updateScoreDatabase();

		// リプレイの自動保存
		if(resource.getAutoplay() == 0){
			for(int i=0;i<replay;i++){
				/*
				 * コンフィグ値:0=保存しない 1=スコア更新時 2=スコアが自己ベスト以上 3=BP更新時 4=BPが自己ベスト以下
				 * 5=COMBO更新時 6=COMBOが自己ベスト以上 7=ランプ更新時 8=ランプが自己ベスト以上 9=毎回
				 */
				switch(resource.getConfig().getAutoSaveReplay()[i]){
				case 0:
					break;
				case 1:
					if(resource.getScoreData().getExscore() > oldexscore)
						saveReplayData(i);
					break;
				case 2:
					if(resource.getScoreData().getExscore() >= oldexscore)
						saveReplayData(i);
					break;
				case 3:
					if(resource.getScoreData().getMinbp() > oldmisscount || oldclear == NoPlay.id)
						saveReplayData(i);
					break;
				case 4:
					if(resource.getScoreData().getMinbp() >= oldmisscount || oldclear == NoPlay.id)
						saveReplayData(i);
					break;
				case 5:
					if(resource.getScoreData().getCombo() > oldcombo)
						saveReplayData(i);
					break;
				case 6:
					if(resource.getScoreData().getCombo() >= oldcombo)
						saveReplayData(i);
					break;
				case 7:
					if(resource.getScoreData().getClear() > oldclear)
						saveReplayData(i);
					break;
				case 8:
					if(resource.getScoreData().getClear() >= oldclear)
						saveReplayData(i);
					break;
				case 9:
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

		final MainController main = getMainController();

		if (getTimer()[TIMER_FADEOUT] != Long.MIN_VALUE) {
			if (time > getTimer()[TIMER_FADEOUT] + getSkin().getFadeout()) {
				stop(SOUND_CLEAR);
				stop(SOUND_FAIL);
				main.changeState(MainController.STATE_SELECTMUSIC);
			}
		} else if (time > getSkin().getScene()) {
            getTimer()[TIMER_FADEOUT] = time;
		}

	}

    public void input() {
        int time = getNowTime();
        final MainController main = getMainController();
        final PlayerResource resource = getMainController().getPlayerResource();

        if (getTimer()[TIMER_FADEOUT] == Long.MIN_VALUE && time > getSkin().getInput()) {
            boolean[] keystate = main.getInputProcessor().getKeystate();
            long[] keytime = main.getInputProcessor().getTime();
            if (resource.getScoreData() == null
                    || ((keystate[0] && keytime[0] != 0) || (keystate[2] && keytime[2] != 0)
                    || (keystate[4] && keytime[4] != 0) || (keystate[6] && keytime[6] != 0))) {
                keytime[0] = keytime[2] = keytime[4] = keytime[6] = 0;
                if (((MusicResultSkin) getSkin()).getRankTime() != 0 && getTimer()[TIMER_RESULT_UPDATESCORE] == Long.MIN_VALUE) {
                    getTimer()[TIMER_RESULT_UPDATESCORE] = time;
                } else {
                    getTimer()[TIMER_FADEOUT] = time;
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
		IRScoreData newscore = resource.getCourseScoreData();
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