package bms.player.beatoraja.result;

import java.io.File;
import java.io.IOException;
import java.util.List;

import bms.player.beatoraja.gauge.*;
import org.lwjgl.opengl.GL11;

import bms.model.BMSModel;
import bms.player.beatoraja.*;
import bms.player.beatoraja.gauge.GrooveGauge;
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

	private MainController main;

	private BitmapFont titlefont;
	private GlyphLayout layout;
	private String title;

	private PlayerResource resource;

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
		this.main = main;

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

	public void create(PlayerResource resource) {
		this.resource = resource;
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
		title = "result";
		parameter.characters = title + resource.getBMSModel().getFullTitle() + parameter.characters;
		titlefont = generator.generateFont(parameter);
		layout = new GlyphLayout(titlefont, resource.getBMSModel().getFullTitle());
		updateScoreDatabase();
		// 保存されているリプレイデータがない場合は、EASY以上で自動保存
		if (resource.getAutoplay() == 0
				&& resource.getScoreData() != null
				&& resource.getScoreData().getClear() >= GrooveGauge.CLEARTYPE_EASY
				&& !main.getPlayDataAccessor().existsReplayData(resource.getBMSModel(),
						resource.getConfig().getLnmode())) {
			saveReplayData();
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
				skin = new MusicResultSkin();
			}
		} else {
			skin = new MusicResultSkin();
		}
		this.setSkin(skin);

		detail = new DetailGraphRenderer(resource.getBMSModel());
		gaugegraph = new GaugeGraphRenderer();
	}

	public void render() {
		int time = getNowTime();
		final SpriteBatch sprite = main.getSpriteBatch();
		final ShapeRenderer shape = main.getShapeRenderer();

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		final float w = 1280;
		final float h = 720;

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
		shape.rect(80, 70, 1120, 380);
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
		
		for(SkinImage img : skin.getSkinPart()) {
			if(img.getTiming() != 2) {
				img.draw(sprite, time);				
			}
		}

		if (score != null) {
			// totalnotes
			skin.getTotalnotes().draw(sprite, time, resource.getScoreData().getNotes());
			
			if (oldclear != 0) {
				titlefont.setColor(Color.valueOf(LAMP[oldclear]));
				titlefont.draw(sprite, CLEAR[oldclear] + " -> ", 240, 425);
			}
			titlefont.setColor(Color.valueOf(LAMP[score.getClear()]));
			titlefont.draw(sprite, CLEAR[score.getClear()], 440, 425);
			titlefont.setColor(Color.WHITE);

			if (oldexscore != 0) {
				titlefont.setColor(Color.WHITE);
				titlefont.draw(sprite, " -> ", 360, 395);
				skin.getScore(score.getExscore() > oldexscore ? 2 : 3).draw(sprite, time, Math.abs(score.getExscore() - oldexscore));
			}

			if (oldmisscount < 65535) {
				titlefont.draw(sprite, " -> ", 360, 365);
				skin.getMisscount(score.getMinbp() > oldmisscount ? 3 : 2).draw(sprite, time, Math.abs(score.getMinbp() - oldmisscount));
			}
			
			if(oldcombo > 0) {
				titlefont.draw(sprite, " -> ", 360, 335);
				skin.getMaxcombo(score.getCombo() > oldcombo ? 2 : 3).draw(sprite, time, Math.abs(score.getCombo() - oldcombo));
			}

			titlefont.draw(sprite, "FAST / SLOW  :  ", 100, 100);

			skin.getJudgeCount(true).draw(sprite, time,
					score.getEgr() + score.getEgd() + score.getEbd() + score.getEpr() + score.getEms());
			skin.getJudgeCount(false).draw(sprite, time,
					score.getLgr() + score.getLgd() + score.getLbd() + score.getLpr() + score.getLms());
		}
		sprite.end();

		Gdx.gl.glEnable(GL11.GL_BLEND);
		Gdx.gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		shape.begin(ShapeType.Filled);
		shape.setColor(0, 0, 0, 0.8f);
		shape.rect(0, 0, 1280, 25);
		shape.end();
		Gdx.gl.glDisable(GL11.GL_BLEND);
		sprite.begin();
		titlefont.draw(sprite, resource.getBMSModel().getFullTitle(), w / 2 - layout.width / 2, 23);
		sprite.end();

		detail.render(sprite, titlefont, shape, time, skin.getJudgeRegion());

		boolean[] keystate = main.getInputProcessor().getKeystate();
		long[] keytime = main.getInputProcessor().getTime();
		if (resource.getScoreData() == null
				|| ((time > 500 && (keystate[0] || keystate[2] || keystate[4] || keystate[6])))) {
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
					keytime[4] = 0;
					// オプションを変更せず同じ譜面でリプレイ
					resource.getReplayData().pattern = null;
					resource.reloadBMSFile();
					main.changeState(MainController.STATE_PLAYBMS);
				} else if (keystate[6]) {
					keytime[6] = 0;
					// 同じ譜面でリプレイ
					resource.reloadBMSFile();
					main.changeState(MainController.STATE_PLAYBMS);
				} else {
					keytime[0] = keytime[2] = 0;
					main.changeState(MainController.STATE_SELECTMUSIC);
				}
			}
		}

		if (resource.getAutoplay() == 0 && main.getInputProcessor().getNumberState()[1]) {
			saveReplayData();
		}
	}

	private boolean saveReplay = false;

	private void saveReplayData() {
		if (resource.getCourseBMSModels() == null && resource.getScoreData() != null) {
			if (!saveReplay && resource.isUpdateScore()) {
				ReplayData rd = resource.getReplayData();
				main.getPlayDataAccessor()
						.wrireReplayData(rd, resource.getBMSModel(), resource.getConfig().getLnmode());
				saveReplay = true;
			}
		}
	}

	private void updateScoreDatabase() {
		saveReplay = false;
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
		IRScoreData score = main.getPlayDataAccessor().readScoreData(resource.getBMSModel(),
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
			main.getPlayDataAccessor().writeScoreDara(resource.getScoreData(), resource.getBMSModel(),
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
		IRScoreData score = resource.getScoreData();
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
	public int getScore() {
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
		if(resource.getScoreData() != null) {
			return resource.getScoreData().getMinbp();			
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public int getTargetMisscount() {
		return oldmisscount;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
}
