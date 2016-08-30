package bms.player.beatoraja.result;

import java.io.File;
import java.io.IOException;
import java.util.List;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.play.audio.SoundProcessor;
import bms.player.beatoraja.select.MusicSelector;

import org.lwjgl.opengl.GL11;

import bms.model.BMSModel;
import bms.player.beatoraja.*;
import bms.player.beatoraja.gauge.GrooveGauge;
import bms.player.beatoraja.skin.LR2ResultSkinLoader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;

/**
 * リザルト
 * 
 * @author exch
 */
public class MusicResult extends MainState {

	public static final int OPTION_RESULT_CLEAR = 90;
	public static final int OPTION_RESULT_FAIL = 91;

	private BitmapFont titlefont;
	private String title;

	private int oldclear;
	private int oldexscore;
	private int oldmisscount;
	private int oldcombo;

	private Sound clear;
	private Sound fail;

	private MusicResultSkin skin;

	private DetailGraphRenderer detail;
	private GaugeGraphRenderer gaugegraph;

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

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
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

		if (skin == null) {
			if (resource.getConfig().getLr2resultskin() != null) {
				LR2ResultSkinLoader loader = new LR2ResultSkinLoader();
				try {
					skin = loader.loadResultSkin(new File(resource.getConfig().getLr2resultskin()), resource
							.getConfig().getLr2resultskinoption());
				} catch (IOException e) {
					e.printStackTrace();
					skin = new MusicResultSkin(MainController.RESOLUTION[resource.getConfig().getResolution()]);
				}
			} else {
				skin = new MusicResultSkin(MainController.RESOLUTION[resource.getConfig().getResolution()]);
			}
			this.setSkin(skin);
		}

		detail = new DetailGraphRenderer(resource.getBMSModel());
		gaugegraph = new GaugeGraphRenderer();
	}

	public void render() {
		int time = getNowTime();
		if (getTimer()[TIMER_RESULTGRAPH_BEGIN] == -1) {
			getTimer()[TIMER_RESULTGRAPH_BEGIN] = time;
		}
		if (getTimer()[TIMER_RESULTGRAPH_END] == -1) {
			getTimer()[TIMER_RESULTGRAPH_END] = time;
		}
		final MainController main = getMainController();

		final SpriteBatch sprite = main.getSpriteBatch();
		final PlayerResource resource = getMainController().getPlayerResource();

		final float w = MainController.RESOLUTION[resource.getConfig().getResolution()].width;
		final float h = MainController.RESOLUTION[resource.getConfig().getResolution()].height;

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

		if (getTimer()[BMSPlayer.TIMER_FADEOUT] != -1) {
			if (time > getTimer()[BMSPlayer.TIMER_FADEOUT] + getSkin().getFadeoutTime()) {
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
			if (time > getSkin().getInputTime()) {
				boolean[] keystate = main.getInputProcessor().getKeystate();
				if (resource.getScoreData() == null || (keystate[0] || keystate[2] || keystate[4] || keystate[6])) {
					getTimer()[BMSPlayer.TIMER_FADEOUT] = time;
				}

				for (int i = 0; i < MusicSelector.REPLAY; i++) {
					if (resource.getAutoplay() == 0 && main.getInputProcessor().getNumberState()[i + 1]) {
						saveReplayData(i);
						break;
					}
				}
			}
			if (time > getSkin().getSceneTime()) {
				getTimer()[BMSPlayer.TIMER_FADEOUT] = time;
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
		rate = score.getExscore() * 10000 / (resource.getBMSModel().getTotalNotes() * 2);
		oldrate = oldexscore * 10000 / (resource.getBMSModel().getTotalNotes() * 2);

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
		if (skin != null) {
			skin.dispose();
			skin = null;
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
		case NUMBER_TOTALNOTES:
		case NUMBER_TOTALNOTES2:
			return resource.getBMSModel().getTotalNotes();
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
		}
		return super.getNumberValue(id);
	}

	public void renderGraph(long time) {
		Rectangle graph = skin.getGaugeRegion();
		final ShapeRenderer shape = getMainController().getShapeRenderer();
		final PlayerResource resource = getMainController().getPlayerResource();
		gaugegraph.render(shape, time, resource, graph, resource.getGauge());
	}

	public void renderDetail(long time) {
		final ShapeRenderer shape = getMainController().getShapeRenderer();
		final SpriteBatch sprite = getMainController().getSpriteBatch();
		detail.render(sprite, titlefont, shape, time, skin.getJudgeRegion());
	}
	
	private int rate;
	private int oldrate;
	
	public boolean getBooleanValue(int id) {
		final PlayerResource resource = getMainController().getPlayerResource();
		final IRScoreData score = resource.getScoreData();
		switch(id) {
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
