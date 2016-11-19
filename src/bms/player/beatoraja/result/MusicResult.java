package bms.player.beatoraja.result;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import bms.model.LongNote;
import bms.model.Note;
import bms.model.TimeLine;
import bms.player.beatoraja.play.audio.SoundProcessor;
import bms.player.beatoraja.play.gauge.GrooveGauge;
import bms.player.beatoraja.select.MusicSelector;

import bms.model.BMSModel;
import bms.player.beatoraja.*;
import bms.player.beatoraja.Config.SkinConfig;
import bms.player.beatoraja.skin.LR2ResultSkinLoader;
import bms.player.beatoraja.skin.LR2SkinHeader;
import bms.player.beatoraja.skin.LR2SkinHeaderLoader;
import bms.player.beatoraja.skin.SkinLoader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

import static bms.player.beatoraja.Resolution.*;
import static bms.player.beatoraja.skin.SkinProperty.*;

/**
 * リザルト
 * 
 * @author exch
 */
public class MusicResult extends MainState {

	public static final int NUMBER_AVERAGE_DURATION = 5555;
	public static final int NUMBER_AVERAGE_DURATION_AFTERDOT = 5556;

	private BitmapFont titlefont;
	private String title;

	private int oldclear;
	private int oldexscore;
	private int oldmisscount;
	private int oldcombo;

	private float avgduration;

	private Sound clear;
	private Sound fail;

	public MusicResult(MainController main) {
		super(main);
	}

	public void create() {
		final PlayerResource resource = getMainController().getPlayerResource();

		if (resource.getConfig().getSoundpath().length() > 0) {
			final File soundfolder = new File(resource.getConfig().getSoundpath());
			if (soundfolder.exists() && soundfolder.isDirectory()) {
				for (File f : soundfolder.listFiles()) {
					if (clear == null && f.getName().startsWith("clear.")) {
						clear = SoundProcessor.getSound(f.getPath());
					}
					if (fail == null && f.getName().startsWith("fail.")) {
						fail = SoundProcessor.getSound(f.getPath());
					}
				}
			}
		}

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 30;
		title = "result";
		parameter.characters = title + resource.getBMSModel().getFullTitle() + parameter.characters;
		titlefont = generator.generateFont(parameter);
		updateScoreDatabase();
		// 保存されているリプレイデータがない場合は、EASY以上で自動保存
		if (resource.getAutoplay() == 0
				&& resource.getScoreData() != null
				&& resource.getScoreData().getClear() >= GrooveGauge.CLEARTYPE_EASY
				&& !getMainController().getPlayDataAccessor().existsReplayData(resource.getBMSModel(),
						resource.getConfig().getLnmode(), 0)) {
			saveReplayData(0);
		}
		// コースモードの場合はリプレイデータをストックする
		if (resource.getCourseBMSModels() != null) {
			resource.addCourseReplay(resource.getReplayData());
			resource.addCourseGauge(resource.getGauge());
		}

		if (getSkin() != null) {
			getSkin().dispose();
		}
		if (resource.getConfig().getSkin()[7] != null) {
			try {
				SkinConfig sc = resource.getConfig().getSkin()[7];
				LR2SkinHeaderLoader loader = new LR2SkinHeaderLoader();
				LR2SkinHeader header = loader.loadSkin(Paths.get(sc.getPath()), this, sc.getProperty());
				Rectangle srcr = RESOLUTION[header.getResolution()];
				Rectangle dstr = RESOLUTION[resource.getConfig().getResolution()];
				LR2ResultSkinLoader dloader = new LR2ResultSkinLoader(srcr.width, srcr.height, dstr.width, dstr.height);
				setSkin(dloader.loadResultSkin(Paths.get(sc.getPath()).toFile(), this, header, loader.getOption(),
						sc.getProperty()));
			} catch (IOException e) {
				e.printStackTrace();
				SkinLoader sl = new SkinLoader(RESOLUTION[resource.getConfig().getResolution()]);
				setSkin(sl.loadResultSkin(Paths.get("skin/default/result.json")));
			}
		} else {
			SkinLoader sl = new SkinLoader(RESOLUTION[resource.getConfig().getResolution()]);
			setSkin(sl.loadResultSkin(Paths.get("skin/default/result.json")));
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
		if (getTimer()[TIMER_RESULT_UPDATESCORE] == Long.MIN_VALUE && ((MusicResultSkin) getSkin()).getRankTime() == 0) {
			getTimer()[TIMER_RESULT_UPDATESCORE] = time;
		}
		final MainController main = getMainController();

		final SpriteBatch sprite = main.getSpriteBatch();
		final PlayerResource resource = getMainController().getPlayerResource();

		final float w = RESOLUTION[resource.getConfig().getResolution()].width;
		final float h = RESOLUTION[resource.getConfig().getResolution()].height;

		IRScoreData score = resource.getScoreData();
		// ゲージグラフ描画

		sprite.begin();
		if (resource.getCourseBMSModels() != null) {
			titlefont.setColor(Color.WHITE);
			titlefont.draw(sprite, resource.getGauge().get(resource.getGauge().size() - 1) > 0 ? "Stage Passed"
					: "Stage Failed", w * 3 / 4, h / 2);
		} else {
			if (score != null) {
				titlefont.setColor(Color.WHITE);
				titlefont.draw(sprite,
						resource.getScoreData().getClear() > GrooveGauge.CLEARTYPE_FAILED ? "Stage Cleared"
								: "Stage Failed", w * 3 / 4, h / 2);
			}
			if (saveReplay) {
				titlefont.draw(sprite, "Replay Saved", w * 3 / 4, h / 4);
			}
		}
		sprite.end();

		if (getTimer()[TIMER_FADEOUT] != Long.MIN_VALUE) {
			if (time > getTimer()[TIMER_FADEOUT] + getSkin().getFadeout()) {
				if (this.clear != null) {
					this.clear.stop();
				}
				if (this.fail != null) {
					this.fail.stop();
				}

				boolean[] keystate = main.getInputProcessor().getKeystate();
				long[] keytime = main.getInputProcessor().getTime();
				keytime[0] = keytime[2] = keytime[4] = keytime[6] = 0;

				if (resource.getCourseBMSModels() != null) {
					if (resource.getGauge().get(resource.getGauge().size() - 1) <= 0) {
						if (resource.getCourseScoreData() != null) {
							// 未達曲のノーツをPOORとして加算
							final List<List<Float>> coursegauge = resource.getCourseGauge();
							final int cg = resource.getCourseBMSModels().length;
							for (int i = 0; i < cg; i++) {
								if (coursegauge.size() <= i) {
									resource.getCourseScoreData().setMinbp(
											resource.getCourseScoreData().getMinbp()
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
					if (keystate[4]) {
						// オプションを変更せず同じ譜面でリプレイ
						resource.getReplayData().pattern = null;
						resource.reloadBMSFile();
						main.changeState(MainController.STATE_PLAYBMS);
					} else if (keystate[6]) {
						// 同じ譜面でリプレイ
						resource.reloadBMSFile();
						main.changeState(MainController.STATE_PLAYBMS);
					} else {
						main.changeState(MainController.STATE_SELECTMUSIC);
					}
				}
			}
		} else {
			if (time > getSkin().getInput()) {
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

	private boolean saveReplay = false;

	private void saveReplayData(int index) {
		final PlayerResource resource = getMainController().getPlayerResource();
		if (resource.getCourseBMSModels() == null && resource.getScoreData() != null) {
			if (!saveReplay && resource.isUpdateScore()) {
				ReplayData rd = resource.getReplayData();
				getMainController().getPlayDataAccessor().wrireReplayData(rd, resource.getBMSModel(),
						resource.getConfig().getLnmode(), index);
				saveReplay = true;
			}
		}
	}

	private void updateScoreDatabase() {
		saveReplay = false;
		final PlayerResource resource = getMainController().getPlayerResource();
		IRScoreData newscore = resource.getScoreData();
		if (newscore == null) {
			if (resource.getCourseScoreData() != null) {
				resource.getCourseScoreData().setMinbp(
						resource.getCourseScoreData().getMinbp() + resource.getBMSModel().getTotalNotes());
				resource.getCourseScoreData().setClear(GrooveGauge.CLEARTYPE_FAILED);
			}
			return;
		}
		IRScoreData score = getMainController().getPlayDataAccessor().readScoreData(resource.getBMSModel(),
				resource.getConfig().getLnmode());
		if (score == null) {
			score = new IRScoreData();
		}
		oldclear = score.getClear();
		oldexscore = score.getExscore();
		oldmisscount = score.getMinbp();
		oldcombo = score.getCombo();
		rate = newscore.getExscore() * 10000 / (resource.getBMSModel().getTotalNotes() * 2);
		next = 0;
		for (int i = 2; i < 9; i++) {
			if (newscore.getExscore() < i * 1111 * (resource.getBMSModel().getTotalNotes() * 2) / 10000) {
				next = newscore.getExscore() - i * 1111 * (resource.getBMSModel().getTotalNotes() * 2) / 10000;
				break;
			}
		}
		oldrate = oldexscore * 10000 / (resource.getBMSModel().getTotalNotes() * 2);
		// duration average
		int count = 0;
		avgduration = 0;
		for (TimeLine tl : resource.getBMSModel().getAllTimeLines()) {
			for (int i = 0; i < 18; i++) {
				Note n = tl.getNote(i);
				if (n != null
						&& !(resource.getBMSModel().getLntype() == BMSModel.LNTYPE_LONGNOTE && n instanceof LongNote && ((LongNote) n)
								.getEndnote().getSection() == tl.getSection())) {
					int state = n.getState();
					int time = n.getTime();
					if (n instanceof LongNote && ((LongNote) n).getEndnote().getSection() == tl.getSection()) {
						state = ((LongNote) n).getEndnote().getState();
						time = ((LongNote) n).getEndnote().getTime();
					}
					if (state >= 1) {
						count++;
						avgduration += Math.abs(time);
					}
				}
			}
		}
		avgduration /= count;

		// コースモードの場合はコーススコアに加算・累積する
		if (resource.getCourseBMSModels() != null) {
			if (resource.getScoreData().getClear() == GrooveGauge.CLEARTYPE_FAILED) {
				resource.getScoreData().setClear(GrooveGauge.CLEARTYPE_NOPLAY);
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
			if (resource.getGauge().get(resource.getGauge().size() - 1) > 0) {
				cscore.setClear(resource.getGrooveGauge().getClearType());
			} else {
				cscore.setClear(GrooveGauge.CLEARTYPE_FAILED);

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

		if (resource.getAutoplay() == 0) {
			getMainController().getPlayDataAccessor().writeScoreDara(resource.getScoreData(), resource.getBMSModel(),
					resource.getConfig().getLnmode(), resource.isUpdateScore());
		}

		if (newscore.getClear() != GrooveGauge.CLEARTYPE_FAILED) {
			if (this.clear != null) {
				this.clear.play();
			}
		} else {
			if (fail != null) {
				fail.play();
			}
		}
	}

	public int getJudgeCount(int judge, boolean fast) {
		IRScoreData score = getMainController().getPlayerResource().getScoreData();
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
		if (clear != null) {
			clear.dispose();
			clear = null;
		}
		if (fail != null) {
			fail.dispose();
			fail = null;
		}
		if (titlefont != null) {
			titlefont.dispose();
			titlefont = null;
		}
		if (getSkin() != null) {
			getSkin().dispose();
			setSkin(null);
		}
	}

	public int getTotalNotes() {
		final PlayerResource resource = getMainController().getPlayerResource();
		return resource.getBMSModel().getTotalNotes();
	}

	public int getNumberValue(int id) {
		final PlayerResource resource = getMainController().getPlayerResource();
		switch (id) {
		case NUMBER_CLEAR:
			if (resource.getScoreData() != null) {
				return resource.getScoreData().getClear();
			}
			return Integer.MIN_VALUE;
		case NUMBER_TARGET_CLEAR:
			return oldclear;
		case NUMBER_HIGHSCORE:
		case NUMBER_HIGHSCORE2:
			return oldexscore;
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
			return resource.getScoreData().getExscore() - oldexscore;
		case NUMBER_DIFF_NEXTRANK:
			return next;
		case NUMBER_MISSCOUNT:
		case NUMBER_MISSCOUNT2:
			if (resource.getScoreData() != null) {
				return resource.getScoreData().getMinbp();
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
			return resource.getScoreData().getMinbp() - oldmisscount;
		case NUMBER_TARGET_MAXCOMBO:
			if (oldcombo > 0) {
				return oldcombo;
			}
			return Integer.MIN_VALUE;
		case NUMBER_MAXCOMBO:
		case NUMBER_MAXCOMBO2:
			if (resource.getScoreData() != null) {
				return resource.getScoreData().getCombo();
			}
			return Integer.MIN_VALUE;
		case NUMBER_DIFF_MAXCOMBO:
			if (oldcombo == 0) {
				return Integer.MIN_VALUE;
			}
			return resource.getScoreData().getCombo() - oldcombo;
		case NUMBER_GROOVEGAUGE:
			return resource.getGauge().get(resource.getGauge().size() - 1).intValue();
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
		case NUMBER_AVERAGE_DURATION:
			return (int) avgduration;
		case NUMBER_AVERAGE_DURATION_AFTERDOT:
			return ((int) (avgduration * 100)) % 100;
		}
		return super.getNumberValue(id);
	}

	private int rate;
	private int oldrate;
	private int next;

	public boolean getBooleanValue(int id) {
		final PlayerResource resource = getMainController().getPlayerResource();
		final IRScoreData score = resource.getScoreData();
		switch (id) {
		case OPTION_DISABLE_SAVE_SCORE:
			return !resource.isUpdateScore();
		case OPTION_ENABLE_SAVE_SCORE:
			return resource.isUpdateScore();
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
