package bms.player.beatoraja.result;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Logger;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;

import bms.model.BMSModel;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.gauge.GrooveGauge;
import bms.player.lunaticrave2.IRScoreData;

public class GradeResult extends ApplicationAdapter {

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

	private long time = 0;
	
	public GradeResult(MainController main) {
		this.main = main;
	}

	public void create(PlayerResource resource) {
		this.resource = resource;
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
		title = "result";
		parameter.characters = title + parameter.characters;
		titlefont = generator.generateFont(parameter);
		time = System.currentTimeMillis();

		updateScoreDatabase();
	}

	private Rectangle graph = new Rectangle(20, 500, 400, 200);

	private final Color[] graph_back = { Color.valueOf("440044"), Color.valueOf("000044"), Color.valueOf("004400"),
			Color.valueOf("440000"), Color.valueOf("444400"), Color.valueOf("222222") };
	private final Color[] graph_line = { Color.valueOf("ff00ff"), Color.valueOf("0000ff"), Color.valueOf("00ff00"),
			Color.valueOf("ff0000"), Color.valueOf("ffff00"), Color.valueOf("cccccc") };

	public void render() {
		final SpriteBatch sprite = main.getSpriteBatch();
		final ShapeRenderer shape = main.getShapeRenderer();

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		final float w = 1280;
		final float h = 720;

		IRScoreData score = resource.getCourseScoreData();

		sprite.begin();
		if (score != null) {
			titlefont.setColor(Color.WHITE);
			titlefont.draw(sprite, resource.getScoreData().getClear() > GrooveGauge.CLEARTYPE_FAILED ? "Stage Cleared"
					: "Stage Failed", w * 3 / 4, h / 2);
		}

		if (score != null) {
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
			titlefont.draw(sprite, score.getExscore() + " ( " + (score.getExscore() > oldexscore ? "+" : "")
					+ (score.getExscore() - oldexscore) + " )", 440, 370);
			titlefont.setColor(Color.WHITE);

			titlefont.draw(sprite, "MISS COUNT : ", 100, 340);
			if (oldmisscount < 65535) {
				titlefont.draw(sprite, oldmisscount + " -> ", 240, 340);
				titlefont.draw(sprite, score.getMinbp() + " ( " + (score.getMinbp() > oldmisscount ? "+" : "")
						+ (score.getMinbp() - oldmisscount) + " )", 440, 340);
			} else {
				titlefont.draw(sprite, String.valueOf(score.getMinbp()), 440, 340);
			}

			titlefont.draw(sprite, "PGREAT : " + score.getPg(), 100, 250);
			titlefont.draw(sprite, "GREAT  : " + score.getGr(), 100, 220);
			titlefont.draw(sprite, "GOOD   : " + score.getGd(), 100, 190);
			titlefont.draw(sprite, "BAD    : " + score.getBd(), 100, 160);
			titlefont.draw(sprite, "POOR : " + score.getPr(), 100, 130);
		}
		sprite.end();
		boolean[] keystate = main.getInputProcessor().getKeystate();
		long[] keytime = main.getInputProcessor().getTime();
		if (resource.getScoreData() == null || ((System.currentTimeMillis() > time + 500
				&& (keystate[0] || keystate[2] || keystate[4] || keystate[6])))) {
			keytime[0] = keytime[2] = keytime[4] = keytime[6] = 0;
					main.changeState(MainController.STATE_SELECTMUSIC, resource);					
		}
	}

	public void updateScoreDatabase() {
		BMSModel[] models = resource.getCourseBMSModels();
		String hash = "";
		int totalnotes = 0;
		for(BMSModel model : models) {
			hash += model.getHash();
			totalnotes += model.getTotalNotes();
		}
		IRScoreData newscore = resource.getCourseScoreData();
		if (newscore == null) {
			return;
		}
		IRScoreData score = main.getScoreDatabase().getScoreData("Player", hash, false);
		if (score == null) {
			score = new IRScoreData();
		}
		oldclear = score.getClear();
		oldexscore = score.getExscore();
		oldmisscount = score.getMinbp();
		score.setHash(hash);
		score.setNotes(totalnotes);

		if (newscore.getClear() != GrooveGauge.CLEARTYPE_FAILED) {
			score.setClearcount(score.getClearcount() + 1);
		}
		if (score.getClear() < newscore.getClear()) {
			score.setClear(newscore.getClear());
			score.setOption(resource.getConfig().getRandom());
		}

		final int pgreat = newscore.getPg();
		final int great = newscore.getGr();
		final int good = newscore.getGd();
		final int bad = newscore.getBd();
		final int poor = newscore.getPr();
		int exscore = pgreat * 2 + great;
		if (score.getExscore() < exscore) {
			score.setPg(pgreat);
			score.setGr(great);
			score.setGd(good);
			score.setBd(bad);
			score.setPr(poor);
		}
		if (score.getMinbp() > newscore.getMinbp()) {
			score.setMinbp(newscore.getMinbp());
		}
		score.setPlaycount(score.getPlaycount() + 1);
		score.setLastupdate(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis() / 1000L);
		main.getScoreDatabase().setScoreData("Player", score);

		Logger.getGlobal().info("スコアデータベース更新完了 ");
	}
}
