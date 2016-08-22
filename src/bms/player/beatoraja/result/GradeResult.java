package bms.player.beatoraja.result;

import java.io.File;
import java.util.*;

import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.select.MusicSelector;
import com.badlogic.gdx.math.Rectangle;
import java.util.logging.Logger;

import bms.player.beatoraja.skin.SkinImage;
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
import bms.player.beatoraja.gauge.GrooveGauge;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class GradeResult extends MainState {

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

	private MusicResultSkin skin;

	private boolean saveReplay = false;

	private GaugeGraphRenderer gaugegraph;

	private Sound clear;
	private Sound fail;

	public GradeResult(MainController main) {
		super(main);
	}

	public void create() {
		final PlayerResource resource = getMainController().getPlayerResource();
		
		if(resource.getConfig().getSoundpath().length() > 0) {
			final File soundfolder = new File(resource.getConfig().getSoundpath());
			if(soundfolder.exists() && soundfolder.isDirectory()) {
				for(File f : soundfolder.listFiles()) {
					if (clear == null) {
						if (f.getName().equals("course_clear.wav")) {
							clear = Gdx.audio.newSound(Gdx.files.internal(f.getPath()));
						}
					}
					if (fail == null) {
						if (f.getName().equals("course_fail.wav")) {
							fail = Gdx.audio.newSound(Gdx.files.internal(f.getPath()));
						}
					}
				}
			}
		}

		if(skin == null) {
			skin = new MusicResultSkin(MainController.RESOLUTION[resource.getConfig().getResolution()]);
			this.setSkin(skin);			
		}
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
		title = "result";
		parameter.characters = title + parameter.characters + "段位認定 " + resource.getCoursetitle() + "不合格";
		titlefont = generator.generateFont(parameter);
		updateScoreDatabase();

		if (resource.getAutoplay() == 0
				&& resource.getCourseScoreData() != null
				&& resource.getCourseScoreData().getClear() >= GrooveGauge.CLEARTYPE_EASY
				&& !getMainController().getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(),
						resource.getConfig().getLnmode(), 0, resource.getConstraint())) {
			saveReplayData(0);
		}

		gaugegraph = new GaugeGraphRenderer();
	}

	public void render() {
		int time = getNowTime();
		final MainController main = getMainController();
		final SpriteBatch sprite = main.getSpriteBatch();
		final ShapeRenderer shape = main.getShapeRenderer();
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

			// ゲージグラフ描画
			final List<List<Float>> coursegauge = resource.getCourseGauge();
			final int cg = resource.getCourseBMSModels().length;
			for (int i = 0; i < cg; i++) {
				Rectangle graph = new Rectangle(40 + i * (1200 / cg), 500, 1200 / cg, 200);
				if (coursegauge.size() <= i) {
					shape.begin(ShapeRenderer.ShapeType.Filled);
					shape.setColor(Color.DARK_GRAY);
					shape.rect(graph.x, graph.y, graph.width, graph.height);
					shape.end();
					Gdx.gl.glLineWidth(4);
					shape.begin(ShapeRenderer.ShapeType.Line);
					shape.setColor(Color.WHITE);
					shape.rect(graph.x, graph.y, graph.width, graph.height);
					shape.end();
					Gdx.gl.glLineWidth(1);
				} else {
					gaugegraph.render(shape, time, resource, graph, coursegauge.get(i));
				}
			}

			sprite.begin();
			titlefont.setColor(Color.WHITE);
			titlefont.draw(sprite, resource.getCoursetitle()
					+ (score.getClear() > GrooveGauge.CLEARTYPE_FAILED ? "  合格" : "  不合格"), w * 3 / 4, h / 2);
			if (saveReplay) {
				titlefont.draw(sprite, "Replay Saved", w * 3 / 4, h / 4);
			}

			sprite.end();
		}
		
		if(getTimer()[BMSPlayer.TIMER_FADEOUT] != -1) {
			if (time > getTimer()[BMSPlayer.TIMER_FADEOUT] + getSkin().getFadeoutTime()) {
				main.changeState(MainController.STATE_SELECTMUSIC);				
			}
		} else {
			if(time > getSkin().getInputTime()) {
				boolean[] keystate = main.getInputProcessor().getKeystate();
				if (resource.getScoreData() == null
						|| (keystate[0] || keystate[2] || keystate[4] || keystate[6])) {
					getTimer()[BMSPlayer.TIMER_FADEOUT] = time;					
				}

				for (int i = 0; i < MusicSelector.REPLAY; i++) {
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

	public void updateScoreDatabase() {
		saveReplay = false;
		final PlayerResource resource = getMainController().getPlayerResource();
		BMSModel[] models = resource.getCourseBMSModels();
		IRScoreData newscore = resource.getCourseScoreData();
		if (newscore == null) {
			return;
		}
		boolean ln = false;
		boolean dp = false;
		for (BMSModel model : models) {
			ln |= model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
					+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
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

		getMainController().getPlayDataAccessor().writeScoreDara(newscore, models, resource.getConfig().getLnmode(),
				random, resource.getConstraint(), resource.isUpdateScore());

		if (newscore.getClear() != GrooveGauge.CLEARTYPE_FAILED) {
			if (this.clear != null) {
				this.clear.play();
			}
		} else {
			if (fail != null) {
				fail.play();
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

	public int getNumberValue(int id) {
		final PlayerResource resource = getMainController().getPlayerResource();
		switch(id) {
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
				if(oldmisscount == Integer.MAX_VALUE) {
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
				if(oldcombo == 0) {
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
				for(int i = 1;i < 6;i++) {
					ecount += getJudgeCount(i,true);
				}
				return ecount;
			case NUMBER_TOTALLATE:
				int count = 0;
				for(int i = 1;i < 6;i++) {
					count += getJudgeCount(i,false);
				}
				return count;
		}
		return super.getNumberValue(id);
	}

	@Override
	public void dispose() {
		if (titlefont != null) {
			titlefont.dispose();
			titlefont = null;
		}
		if (skin != null) {
			skin.dispose();
			skin = null;
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
}
