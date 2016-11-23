package bms.player.beatoraja.result;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import bms.player.beatoraja.play.audio.SoundProcessor;
import bms.player.beatoraja.play.gauge.GrooveGauge;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.skin.SkinLoader;
import com.badlogic.gdx.math.Rectangle;
import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

import bms.model.BMSModel;
import bms.player.beatoraja.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import static bms.player.beatoraja.Resolution.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

public class GradeResult extends MainState {

	private int oldclear;
	private int oldexscore;
	private int oldmisscount;
	private int oldcombo;

	private boolean saveReplay = false;

	private Sound clear;
	private Sound fail;

	public GradeResult(MainController main) {
		super(main);
	}

	public void create() {
		final PlayerResource resource = getMainController().getPlayerResource();

		if (resource.getConfig().getSoundpath().length() > 0) {
			final File soundfolder = new File(resource.getConfig().getSoundpath());
			if (soundfolder.exists() && soundfolder.isDirectory()) {
				for (File f : soundfolder.listFiles()) {
					if (clear == null && f.getName().startsWith("course_clear.")) {
						clear = SoundProcessor.getSound(f.getPath());
					}
					if (fail == null && f.getName().startsWith("course_fail.")) {
						fail = SoundProcessor.getSound(f.getPath());
					}
				}
			}
		}

		if (getSkin() != null) {
			getSkin().dispose();
		}
		SkinLoader sl = new SkinLoader(RESOLUTION[resource.getConfig().getResolution()]);
		setSkin(sl.loadResultSkin(Paths.get("skin/default/graderesult.json")));

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
		IRScoreData score = resource.getCourseScoreData();

		if (score != null) {
			if (score.getClear() > GrooveGauge.CLEARTYPE_FAILED) {
				Gdx.gl.glClearColor(0, 0, 0.4f, 1);
			} else {
				Gdx.gl.glClearColor(0.4f, 0, 0, 1);
			}
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

			final float w = 1280;
			final float h = 720;
		}

		if (getTimer()[TIMER_FADEOUT] != Long.MIN_VALUE) {
			if (time > getTimer()[TIMER_FADEOUT] + getSkin().getFadeout()) {
				if (clear != null) {
					clear.stop();
				}
				if (fail != null) {
					fail.stop();
				}

				main.changeState(MainController.STATE_SELECTMUSIC);
			}
		} else {
			if (time > getSkin().getInput()) {
				boolean[] keystate = main.getInputProcessor().getKeystate();
				if (resource.getScoreData() == null || (keystate[0] || keystate[2] || keystate[4] || keystate[6])) {
					getTimer()[TIMER_FADEOUT] = time;
				}

				for (int i = 0; i < MusicSelector.REPLAY; i++) {
					if (resource.getAutoplay() == 0 && main.getInputProcessor().getNumberState()[i + 1]) {
						saveReplayData(i);
						break;
					}
				}
			}
			if (time > getSkin().getScene()) {
				getTimer()[TIMER_FADEOUT] = time;
			}
		}

	}

	public void updateScoreDatabase() {
		saveReplay = false;
		final PlayerResource resource = getMainController().getPlayerResource();
		BMSModel[] models = resource.getCourseBMSModels();
		IRScoreData newscore = resource.getCourseScoreData();
		if (newscore == null) {
			return;
		}
		boolean dp = false;
		for (BMSModel model : models) {
			dp |= (model.getUseKeys() == 10 || model.getUseKeys() == 14);
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
		rate = score.getExscore() * 10000 / (notes * 2);
		oldrate = oldexscore * 10000 / (notes * 2);

		getMainController().getPlayDataAccessor().writeScoreDara(newscore, models, resource.getConfig().getLnmode(),
				random, resource.getConstraint(), resource.isUpdateScore());

		if (newscore.getClear() != GrooveGauge.CLEARTYPE_FAILED) {
			if (this.clear != null) {
				this.clear.loop();
			}
		} else {
			if (fail != null) {
				fail.loop();
			}
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
		if (getSkin() != null) {
			getSkin().dispose();
			setSkin(null);
		}
	}

	private void saveReplayData(int index) {
		final PlayerResource resource = getMainController().getPlayerResource();
		if (resource.getCourseScoreData() != null) {
			if (!saveReplay && resource.isUpdateScore()) {
				// 保存されているリプレイデータがない場合は、EASY以上で自動保存
				ReplayData[] rd = resource.getCourseReplay();
				getMainController().getPlayDataAccessor().wrireReplayData(rd, resource.getCourseBMSModels(),
						resource.getConfig().getLnmode(), index, resource.getConstraint());
				saveReplay = true;
			}
		}
	}

	private int rate;
	private int oldrate;

	public boolean getBooleanValue(int id) {
		final PlayerResource resource = getMainController().getPlayerResource();
		final IRScoreData score = resource.getCourseScoreData();
		switch (id) {
			case OPTION_RESULT_CLEAR:
				return score.getClear() != GrooveGauge.CLEARTYPE_FAILED;
			case OPTION_RESULT_FAIL:
				return score.getClear() == GrooveGauge.CLEARTYPE_FAILED;
			case OPTION_RESULT_F_1P:
			case OPTION_NOW_F_1P:
				return rate <= 2222;
			case OPTION_RESULT_E_1P:
			case OPTION_NOW_E_1P:
				return rate > 2222 && rate <= 3333;
			case OPTION_RESULT_D_1P:
			case OPTION_NOW_D_1P:
				return rate > 3333 && rate <= 4444;
			case OPTION_RESULT_C_1P:
			case OPTION_NOW_C_1P:
				return rate > 4444 && rate <= 5555;
			case OPTION_RESULT_B_1P:
			case OPTION_NOW_B_1P:
				return rate > 5555 && rate <= 6666;
			case OPTION_RESULT_A_1P:
			case OPTION_NOW_A_1P:
				return rate > 6666 && rate <= 7777;
			case OPTION_RESULT_AA_1P:
			case OPTION_NOW_AA_1P:
				return rate > 7777 && rate <= 8888;
			case OPTION_RESULT_AAA_1P:
			case OPTION_NOW_AAA_1P:
				return rate > 8888;
			case OPTION_BEST_F_1P:
				return oldrate <= 2222;
			case OPTION_BEST_E_1P:
				return oldrate > 2222 && oldrate <= 3333;
			case OPTION_BEST_D_1P:
				return oldrate > 3333 && oldrate <= 4444;
			case OPTION_BEST_C_1P:
				return oldrate > 4444 && oldrate <= 5555;
			case OPTION_BEST_B_1P:
				return oldrate > 5555 && oldrate <= 6666;
			case OPTION_BEST_A_1P:
				return oldrate > 6666 && oldrate <= 7777;
			case OPTION_BEST_AA_1P:
				return oldrate > 7777 && oldrate <= 8888;
			case OPTION_BEST_AAA_1P:
				return oldrate > 8888;
			case OPTION_UPDATE_SCORE:
				return score.getExscore() > oldexscore;
			case OPTION_UPDATE_MAXCOMBO:
				return score.getCombo() > oldcombo;
			case OPTION_UPDATE_MISSCOUNT:
				return score.getMinbp() < oldmisscount;
			case OPTION_UPDATE_SCORERANK:
				return rate / 1111 > oldrate / 1111;

		}
		return super.getBooleanValue(id);

	}
}