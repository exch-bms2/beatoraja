package bms.player.beatoraja.result;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainController.PlayerResource;
import bms.player.beatoraja.gauge.GrooveGauge;
import bms.player.beatoraja.input.MusicResultInputProcessor;
import bms.player.lunaticrave2.IRScoreData;

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

/**
 * リザルト
 * 
 * @author exch
 */
public class MusicResult extends ApplicationAdapter {

	private static final String[] LAMP = { "000000", "808080", "800080", "ff00ff", "40ff40", "f0c000", "ffffff",
			"ffff88", "88ffff", "ff8888", "ff0000" };
	private static final String[] CLEAR = { "NO PLAY", "FAILED", "ASSIST CLEAR", "L-ASSIST CLEAR", "EASY CLEAR",
			"CLEAR", "HARD CLEAR", "EX-HARD CLEAR", "FULL COMBO", "PERFECT", "MAX" };

	private MainController main;

	private BitmapFont titlefont;
	private String title;

	private PlayerResource resource;

	private IRScoreData oldscore;

	private MusicResultInputProcessor input;

	public MusicResult(MainController main) {
		this.main = main;
	}

	private long time = 0;

	public void create(PlayerResource resource) {
		this.resource = resource;
		input = new MusicResultInputProcessor(this);
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

	public void render() {
		final SpriteBatch sprite = main.getSpriteBatch();
		final ShapeRenderer shape = main.getShapeRenderer();

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		final float w = 1280;
		final float h = 720;

		IRScoreData score = resource.getScoreData();
		// ゲージグラフ描画
		shape.begin(ShapeType.Line);
		shape.setColor(Color.WHITE);
		shape.rect(graph.x, graph.y, graph.width, graph.height);
		Float f1 = null;
		for (int i = 0; i < resource.getGauge().size(); i++) {
			Float f2 = resource.getGauge().get(i);
			if (f1 != null) {
				shape.setColor(Color.GREEN);
				shape.line(graph.x + graph.width * (i - 1) / resource.getGauge().size(),
						graph.y + (f1 / 100.0f) * graph.height, graph.x + graph.width * i / resource.getGauge().size(),
						graph.y + (f2 / 100.0f) * graph.height);
			}
			f1 = f2;
		}
		shape.end();

		sprite.begin();
		if (score != null) {
			titlefont.setColor(Color.WHITE);
			titlefont.draw(sprite, resource.getScoreData().getClear() > GrooveGauge.CLEARTYPE_FAILED ? "Stage Cleared"
					: "Stage Failed", w / 2, h / 2);
		}

		if (score != null) {
			titlefont.draw(sprite, "CLEAR : ", 100, 300);
			if (oldscore != null) {
				titlefont.setColor(Color.valueOf(LAMP[oldscore.getClear()]));
				titlefont.draw(sprite, CLEAR[oldscore.getClear()] + " -> ", 240, 300);
			}
			titlefont.setColor(Color.valueOf(LAMP[score.getClear()]));
			titlefont.draw(sprite, CLEAR[score.getClear()], 440, 300);
			titlefont.setColor(Color.WHITE);

			titlefont.draw(sprite, "PGREAT : " + score.getPg(), 100, 250);
			titlefont.draw(sprite, "GREAT  : " + score.getGr(), 100, 220);
			titlefont.draw(sprite, "GOOD   : " + score.getGd(), 100, 190);
			titlefont.draw(sprite, "BAD    : " + score.getBd(), 100, 160);
			titlefont.draw(sprite, "POOR : " + score.getPr(), 100, 130);
		}
		sprite.end();
		// TODO キー入力で移行、および入力したキーでリプレイ、同じ譜面でリプレイ、等を分けたい
		boolean[] keystate = input.getKeystate();
		if (resource.getScoreData() == null || ((System.currentTimeMillis() > time + 500
				&& (keystate[0] || keystate[2] || keystate[4] || keystate[6])))) {
			if (resource.getCourseBMSModels() != null) {
				if (resource.getGauge().get(resource.getGauge().size() - 1) <= 0) {
					// TODO 不合格リザルト
				}
				if (resource.nextCourse()) {
					main.changeState(MainController.STATE_PLAYBMS, resource);
				} else {
					// TODO 合格リザルト
					main.changeState(MainController.STATE_SELECTMUSIC, null);
				}
			} else {
				main.changeState(MainController.STATE_SELECTMUSIC, null);
			}
		}
	}

	public void updateScoreDatabase() {
		BMSModel model = resource.getBMSModel();
		IRScoreData newscore = resource.getScoreData();
		if (newscore == null) {
			return;
		}
		IRScoreData score = main.getScoreDatabase().getScoreData("Player", model.getHash(), false);
		oldscore = score;
		if (score == null) {
			score = new IRScoreData();
		}
		score.setHash(model.getHash());
		score.setNotes(model.getTotalNotes() + model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
				+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH));

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
