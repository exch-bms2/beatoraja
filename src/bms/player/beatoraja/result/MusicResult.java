package bms.player.beatoraja.result;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import bms.player.beatoraja.gauge.*;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.select.MusicSelector;
import com.badlogic.gdx.utils.GdxRuntimeException;
import org.lwjgl.opengl.GL11;

import bms.model.BMSModel;
import bms.player.beatoraja.*;
import bms.player.beatoraja.gauge.GrooveGauge;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.skin.LR2ResultSkinLoader;
import bms.player.beatoraja.skin.SkinImage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
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

	private static final String[] LAMP = { "000000", "808080", "800080", "ff00ff", "40ff40", "f0c000", "ffffff",
			"ffff88", "88ffff", "ff8888", "ff0000" };
	private static final String[] CLEAR = { "NO PLAY", "FAILED", "ASSIST CLEAR", "L-ASSIST CLEAR", "EASY CLEAR",
			"CLEAR", "HARD CLEAR", "EX-HARD CLEAR", "FULL COMBO", "PERFECT", "MAX" };

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

		if (clear == null) {
			if (new File("skin/clear.wav").exists()) {
				clear = Gdx.audio.newSound(Gdx.files.internal("skin/clear.wav"));
			}
		}
		if (fail == null) {
			if (new File("skin/fail.wav").exists()) {
				fail = Gdx.audio.newSound(Gdx.files.internal("skin/fail.wav"));
			}
		}
		
	}

	public void create() {
		final PlayerResource resource = getMainController().getPlayerResource();
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
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
		if(resource.getCourseBMSModels() != null) {
			resource.addCourseReplay(resource.getReplayData());
			resource.addCourseGauge(resource.getGauge());
		}

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

		detail = new DetailGraphRenderer(resource.getBMSModel());
		gaugegraph = new GaugeGraphRenderer();
	}

	public void render() {
		int time = getNowTime();
		final MainController main = getMainController();

		final SpriteBatch sprite = main.getSpriteBatch();
		final ShapeRenderer shape = main.getShapeRenderer();
		final PlayerResource resource = getMainController().getPlayerResource();

		final float w = MainController.RESOLUTION[resource.getConfig().getResolution()].width;
		final float h = MainController.RESOLUTION[resource.getConfig().getResolution()].height;

		Rectangle graph = skin.getGaugeRegion();

		if (resource.getBGAManager().getStagefileData() != null) {
			sprite.begin();
			sprite.draw(resource.getBGAManager().getStagefileData(), 0, 0, w, h);
			sprite.end();
		}

		IRScoreData score = resource.getScoreData();
		// ゲージグラフ描画
		gaugegraph.render(shape, time, resource, graph, resource.getGauge());

		Gdx.gl.glEnable(GL11.GL_BLEND);
		Gdx.gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		shape.begin(ShapeType.Filled);
		shape.setColor(0, 0, 0, 0.8f);
		shape.rect(w / 40, h / 8, w * 0.95f, h / 2);
		shape.end();
		Gdx.gl.glDisable(GL11.GL_BLEND);

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

		Gdx.gl.glEnable(GL11.GL_BLEND);
		Gdx.gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		shape.begin(ShapeType.Filled);
		shape.setColor(0, 0, 0, 0.8f);
		shape.rect(0, 0, w, 25);
		shape.end();
		Gdx.gl.glDisable(GL11.GL_BLEND);

		detail.render(sprite, titlefont, shape, time, skin.getJudgeRegion());

		if(getTimer()[BMSPlayer.TIMER_FADEOUT] != -1) {
			if (time > getTimer()[BMSPlayer.TIMER_FADEOUT] + getSkin().getFadeoutTime()) {
				boolean[] keystate = main.getInputProcessor().getKeystate();
				long[] keytime = main.getInputProcessor().getTime();
				keytime[0] = keytime[2] = keytime[4] = keytime[6] = 0;

				if (resource.getCourseBMSModels() != null) {
					if (resource.getGauge().get(resource.getGauge().size() - 1) <= 0) {
						if(resource.getCourseScoreData() != null) {
							// 未達曲のノーツをPOORとして加算
							final List<List<Float>> coursegauge = resource.getCourseGauge();
							final int cg = resource.getCourseBMSModels().length;
							for(int i = 0;i < cg;i++) {
								if (coursegauge.size() <= i) {
									resource.getCourseScoreData().setMinbp(resource.getCourseScoreData().getMinbp() + resource.getCourseBMSModels()[i].getTotalNotes());
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
			if(time > getSkin().getInputTime()) {
				boolean[] keystate = main.getInputProcessor().getKeystate();
				if (resource.getScoreData() == null
						|| (keystate[0] || keystate[2] || keystate[4] || keystate[6])) {
					getTimer()[BMSPlayer.TIMER_FADEOUT] = time;					
				}

				for(int i = 0;i < MusicSelector.REPLAY;i++) {
					if (resource.getAutoplay() == 0 && main.getInputProcessor().getNumberState()[i + 1]) {
						saveReplayData(i);
						break;
					}
				}
			}
			if(time > getSkin().getSceneTime()) {
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
				getMainController().getPlayDataAccessor()
						.wrireReplayData(rd, resource.getBMSModel(), resource.getConfig().getLnmode(), index);
				saveReplay = true;
			}
		}
	}

	private void updateScoreDatabase() {
		saveReplay = false;
		final PlayerResource resource = getMainController().getPlayerResource();
		BMSModel model = resource.getBMSModel();
		IRScoreData newscore = resource.getScoreData();
		boolean ln = model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
				+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		if (newscore == null) {
			if (resource.getCourseScoreData() != null) {
				resource.getCourseScoreData().setMinbp(resource.getCourseScoreData().getMinbp() + resource.getBMSModel().getTotalNotes());
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
				for(BMSModel mo : resource.getCourseBMSModels()) {
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
		if(score != null) {
			switch(judge) {
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
	public int getClear() {
		final PlayerResource resource = getMainController().getPlayerResource();
		if(resource.getScoreData() != null) {
			return resource.getScoreData().getClear();
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public int getTargetClear() {
		return oldclear;
	}

	@Override
	public int getScore() {
		final PlayerResource resource = getMainController().getPlayerResource();
		if(resource.getScoreData() != null) {
			return resource.getScoreData().getExscore();			
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public int getTargetScore() {
		return oldexscore;
	}

	@Override
	public int getMaxcombo() {
		final PlayerResource resource = getMainController().getPlayerResource();
		if(resource.getScoreData() != null) {
			return resource.getScoreData().getCombo();			
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public int getTargetMaxcombo() {
		if(oldcombo > 0) {
			return oldcombo;			
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public int getMisscount() {
		final PlayerResource resource = getMainController().getPlayerResource();
		if(resource.getScoreData() != null) {
			return resource.getScoreData().getMinbp();			
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public int getTargetMisscount() {
		if(oldmisscount == Integer.MAX_VALUE) {
			return Integer.MIN_VALUE;
		}
		return oldmisscount;
	}

	@Override
	public void dispose() {
		if(clear != null) {
			clear.dispose();
			clear = null;
		}
		if(fail != null) {
			fail.dispose();
			fail = null;
		}
		if(titlefont != null) {
			titlefont.dispose();
			titlefont = null;
		}
		if(skin != null) {
			skin.dispose();
			skin = null;
		}
	}

	public String getTitle() {
		final PlayerResource resource = getMainController().getPlayerResource();
		return resource.getBMSModel().getTitle();
	}

	public String getSubtitle() {
		final PlayerResource resource = getMainController().getPlayerResource();
		return resource.getBMSModel().getSubTitle();
	}

	public int getTotalNotes() {
		final PlayerResource resource = getMainController().getPlayerResource();
		return resource.getBMSModel().getTotalNotes();
	}
}
