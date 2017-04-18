package bms.player.beatoraja.result;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import bms.player.beatoraja.play.gauge.GrooveGauge;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.lr2.*;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.FloatArray;

import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.player.beatoraja.*;
import bms.player.beatoraja.Config.SkinConfig;

import static bms.player.beatoraja.Resolution.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

public class GradeResult extends MainState {

	private int oldclear;
	private int oldexscore;
	private int oldmisscount;
	private int oldcombo;

	private int saveReplay = -1;

	public static final int SOUND_CLEAR = 0;
	public static final int SOUND_FAIL = 1;

	public GradeResult(MainController main) {
		super(main);
	}

	public void create() {
		final PlayerResource resource = getMainController().getPlayerResource();

		setSound(SOUND_CLEAR, resource.getConfig().getSoundpath() + File.separatorChar + "course_clear.wav", false);
		setSound(SOUND_FAIL, resource.getConfig().getSoundpath() + File.separatorChar + "course_fail.wav", false);

		try {
			SkinConfig sc = resource.getConfig().getSkin()[15];
			if (sc.getPath().endsWith(".json")) {
				SkinLoader sl = new SkinLoader(RESOLUTION[resource.getConfig().getResolution()]);
				setSkin(sl.loadResultSkin(Paths.get(sc.getPath()), sc.getProperty()));
			} else {
				LR2SkinHeaderLoader loader = new LR2SkinHeaderLoader();
				SkinHeader header = loader.loadSkin(Paths.get(sc.getPath()), this, sc.getProperty());
				Rectangle srcr = RESOLUTION[header.getResolution()];
				Rectangle dstr = RESOLUTION[resource.getConfig().getResolution()];
				LR2ResultSkinLoader dloader = new LR2ResultSkinLoader(srcr.width, srcr.height, dstr.width, dstr.height);
				setSkin(dloader.loadResultSkin(Paths.get(sc.getPath()).toFile(), this, header, loader.getOption(),
						sc.getProperty()));
			}
		} catch (Throwable e) {
			e.printStackTrace();
			SkinLoader sl = new SkinLoader(RESOLUTION[resource.getConfig().getResolution()]);
			setSkin(sl.loadResultSkin(Paths.get("skin/default/graderesult.json"), new HashMap()));
		}
		
        for(int i = resource.getCourseGauge().size();i < resource.getCourseBMSModels().length;i++) {
        	FloatArray list = new FloatArray();
            for(int l = 0;l < (resource.getCourseBMSModels()[i].getLastNoteTime() + 500) / 500;l++) {
                list.add(0f);
            }
            resource.getCourseGauge().add(list);
        }

		updateScoreDatabase();

		if (resource.getAutoplay() == 0
				&& resource.getCourseScoreData() != null
				&& resource.getCourseScoreData().getClear() >= GrooveGauge.CLEARTYPE_EASY
				&& !getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
				resource.getConfig().getLnmode(), 0, resource.getConstraint())) {
			saveReplayData(0);
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
		final PlayerResource resource = getMainController().getPlayerResource();

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
		saveReplay = -1;
		final PlayerResource resource = getMainController().getPlayerResource();
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
		if (resource.getConfig().getRandom() > 0
				|| (dp && (resource.getConfig().getRandom2() > 0 || resource.getConfig().getDoubleoption() > 0))) {
			random = 2;
		}
		if (resource.getConfig().getRandom() == 1
				&& (!dp || (resource.getConfig().getRandom2() == 1 && resource.getConfig().getDoubleoption() == 1))) {
			random = 1;
		}
		IRScoreData score = getMainController().getPlayDataAccessor().readScoreData(models,
				resource.getConfig().getLnmode(), random, resource.getConstraint());
		if (score == null) {
			score = new IRScoreData();
		}
		oldclear = score.getClear();
		oldexscore = score.getExscore();
		oldmisscount = score.getMinbp();
		oldcombo = score.getCombo();
		int notes = 0;
		for (BMSModel model : resource.getCourseBMSModels()) {
			notes += model.getTotalNotes();
		}

		getScoreDataProperty().setTargetScore(oldexscore, resource.getRivalScoreData(), resource.getBMSModel().getTotalNotes());
		getScoreDataProperty().update(newscore);

		getMainController().getPlayDataAccessor().writeScoreDara(newscore, models, resource.getConfig().getLnmode(),
				random, resource.getConstraint(), resource.isUpdateScore());

		if (newscore.getClear() != GrooveGauge.CLEARTYPE_FAILED) {
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
			case NUMBER_TOTALEARLY:
				int ecount = 0;
				for (int i = 1; i < 6; i++) {
					ecount += getJudgeCount(i, true);
				}
				return ecount;
			case NUMBER_TOTALLATE:
				int count = 0;
				for (int i = 1; i < 6; i++) {
					count += getJudgeCount(i, false);
				}
				return count;
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
			if (saveReplay == -1 && resource.isUpdateScore()) {
				// 保存されているリプレイデータがない場合は、EASY以上で自動保存
				ReplayData[] rd = resource.getCourseReplay();
				getMainController().getPlayDataAccessor().wrireReplayData(rd, resource.getCourseBMSModels(),
						resource.getConfig().getLnmode(), index, resource.getConstraint());
				saveReplay = index;
			}
		}
	}

	public boolean getBooleanValue(int id) {
		final PlayerResource resource = getMainController().getPlayerResource();
		final IRScoreData score = resource.getCourseScoreData();
		switch (id) {
			case OPTION_RESULT_CLEAR:
				return score.getClear() != GrooveGauge.CLEARTYPE_FAILED;
			case OPTION_RESULT_FAIL:
				return score.getClear() == GrooveGauge.CLEARTYPE_FAILED;
			case OPTION_UPDATE_SCORE:
				return score.getExscore() > oldexscore;
			case OPTION_UPDATE_MAXCOMBO:
				return score.getCombo() > oldcombo;
			case OPTION_UPDATE_MISSCOUNT:
				return score.getMinbp() < oldmisscount;
			case OPTION_UPDATE_SCORERANK:
				return getScoreDataProperty().getNowRate() > getScoreDataProperty().getBestScoreRate();
			case OPTION_NO_REPLAYDATA:
				return !getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getConfig().getLnmode(), 0,resource.getConstraint());
			case OPTION_NO_REPLAYDATA2:
				return !getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getConfig().getLnmode(), 1,resource.getConstraint());
			case OPTION_NO_REPLAYDATA3:
				return !getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getConfig().getLnmode(), 2,resource.getConstraint());
			case OPTION_NO_REPLAYDATA4:
				return !getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getConfig().getLnmode(), 3,resource.getConstraint());
			case OPTION_REPLAYDATA:
				return getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getConfig().getLnmode(), 0,resource.getConstraint());
			case OPTION_REPLAYDATA2:
				return getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getConfig().getLnmode(), 1,resource.getConstraint());
			case OPTION_REPLAYDATA3:
				return getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getConfig().getLnmode(), 2,resource.getConstraint());
			case OPTION_REPLAYDATA4:
				return getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getConfig().getLnmode(), 3,resource.getConstraint());
			case OPTION_REPLAYDATA_SAVED:
				return saveReplay == 0;
			case OPTION_REPLAYDATA2_SAVED:
				return saveReplay == 1;
			case OPTION_REPLAYDATA3_SAVED:
				return saveReplay == 2;
			case OPTION_REPLAYDATA4_SAVED:
				return saveReplay == 3;
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