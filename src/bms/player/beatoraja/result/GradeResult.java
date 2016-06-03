package bms.player.beatoraja.result;

import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

import bms.model.BMSModel;
import bms.player.beatoraja.*;
import bms.player.beatoraja.gauge.GrooveGauge;

public class GradeResult extends MainState {
	
	// TODO 段位リプレイの保存
	// TODO 段位ゲージ繊維の表示

	private static final String[] LAMP = { "000000", "808080", "800080", "ff00ff", "40ff40", "f0c000", "ffffff",
			"ffff88", "88ffff", "ff8888", "ff0000" };
	private static final String[] CLEAR = { "NO PLAY", "FAILED", "ASSIST CLEAR", "L-ASSIST CLEAR", "EASY CLEAR",
			"CLEAR", "HARD CLEAR", "EX-HARD CLEAR", "FULL COMBO", "PERFECT", "MAX" };

	private MainController main;

	private BitmapFont titlefont;
	private String title;

	private PlayerResource resource;

	private int oldclear;
	private int oldexscore;
	private int oldmisscount;

    private MusicResultSkin skin;

	public GradeResult(MainController main) {
		this.main = main;
		
        skin = new MusicResultSkin();
		this.setSkin(skin);
	}

	public void create(PlayerResource resource) {
		this.resource = resource;
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
		title = "result";
		parameter.characters = title + parameter.characters + "段位認定 " + resource.getCoursetitle() + "不合格";
		titlefont = generator.generateFont(parameter);
		updateScoreDatabase();
	}

	public void render() {
		int time = getNowTime();
		final SpriteBatch sprite = main.getSpriteBatch();
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

			sprite.begin();
			if (score != null) {
				titlefont.setColor(Color.WHITE);
				titlefont.draw(sprite, resource.getCoursetitle()
						+ (score.getClear() > GrooveGauge.CLEARTYPE_FAILED ? "  合格" : "  不合格"), w * 3 / 4, h / 2);
			}
			titlefont.draw(sprite, "CLEAR : ", 100, 400);
			if (oldclear != 0) {
				titlefont.setColor(Color.valueOf(LAMP[oldclear]));
				titlefont.draw(sprite, CLEAR[oldclear] + " -> ", 240, 400);
			}
			titlefont.setColor(Color.valueOf(LAMP[score.getClear()]));
			titlefont.draw(sprite, CLEAR[score.getClear()], 440, 400);
			titlefont.setColor(Color.WHITE);

			titlefont.draw(sprite, "SCORE : ", 100, 370);
			if (oldexscore != 0) {
				titlefont.draw(sprite, oldexscore + " -> ", 240, 370);
			}
			titlefont.draw(sprite,
					score.getExscore() + " ( " + (score.getExscore() > oldexscore ? "+" : "")
							+ (score.getExscore() - oldexscore) + " )", 440, 370);
			titlefont.setColor(Color.WHITE);

			titlefont.draw(sprite, "MISS COUNT : ", 100, 340);
			if (oldmisscount < 65535) {
				titlefont.draw(sprite, oldmisscount + " -> ", 240, 340);
				titlefont.draw(sprite,
						score.getMinbp() + " ( " + (score.getMinbp() > oldmisscount ? "+" : "")
								+ (score.getMinbp() - oldmisscount) + " )", 440, 340);
			} else {
				titlefont.draw(sprite, String.valueOf(score.getMinbp()), 440, 340);
			}

            titlefont.draw(sprite, "PGREAT : ", 100, 280);
            titlefont.draw(sprite, "GREAT  : ", 100, 250);
            titlefont.draw(sprite, "GOOD   : ", 100, 220);
            titlefont.draw(sprite, "BAD    : ",  100, 190);
            titlefont.draw(sprite, "POOR   : ", 100, 160);
            titlefont.draw(sprite, "MISS   : ", 100, 130);
            titlefont.draw(sprite, "FAST / SLOW  :  ", 100, 100);
            
    		skin.getJudgeCount(true).draw(sprite, time, score.getFgr() + score.getFgd() + score.getFbd() + score.getFpr() + score.getFms());
    		skin.getJudgeCount(false).draw(sprite, time, score.getSgr() + score.getSgd() + score.getSbd() + score.getSpr() + score.getSms());
			sprite.end();
		}
		boolean[] keystate = main.getInputProcessor().getKeystate();
		long[] keytime = main.getInputProcessor().getTime();
		if (score == null
				|| ((System.currentTimeMillis() > time + 500 && (keystate[0] || keystate[2] || keystate[4] || keystate[6])))) {
			keytime[0] = keytime[2] = keytime[4] = keytime[6] = 0;
			main.changeState(MainController.STATE_SELECTMUSIC);
		}
	}

	public void updateScoreDatabase() {
		BMSModel[] models = resource.getCourseBMSModels();
		IRScoreData newscore = resource.getCourseScoreData();
		if (newscore == null) {
			return;
		}
		IRScoreData score = main.getPlayDataAccessor().readScoreData(models, resource.getConfig().getLnmode(),
				resource.getConfig().getRandom() == 1);
		if (score == null) {
			score = new IRScoreData();
		}
		boolean ln = false;
		for(BMSModel model : models) {
			ln |= model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
					+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;			
		}
		if (ln && resource.getConfig().getLnmode() == 2) {
			oldclear = score.getExclear();
		} else {
			oldclear = score.getClear();
		}
		oldexscore = score.getExscore();
		oldmisscount = score.getMinbp();

		main.getPlayDataAccessor().writeScoreDara(newscore, models, resource.getConfig().getLnmode(),
				resource.getConfig().getRandom() == 1, resource.isUpdateScore());

		Logger.getGlobal().info("スコアデータベース更新完了 ");
	}

	public int getJudgeCount(int judge, boolean fast) {
		IRScoreData score = resource.getCourseScoreData();
		if(score != null) {
			switch(judge) {
			case 0:
				return fast ? score.getFpg() : score.getSpg();
			case 1:
				return fast ? score.getFgr() : score.getSgr();
			case 2:
				return fast ? score.getFgd() : score.getSgd();
			case 3:
				return fast ? score.getFbd() : score.getSbd();
			case 4:
				return fast ? score.getFpr() : score.getSpr();
			case 5:
				return fast ? score.getFms() : score.getSms();
			}
		}
		return 0;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
}
