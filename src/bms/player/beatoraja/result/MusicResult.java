package bms.player.beatoraja.result;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.ReplayData;
import bms.player.beatoraja.MainController.PlayerResource;
import bms.player.beatoraja.gauge.GrooveGauge;
import bms.player.beatoraja.input.KeyInputLog;
import bms.player.beatoraja.pattern.PatternModifyLog;
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
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

/**
 * リザルト
 * 
 * @author exch
 */
public class MusicResult extends ApplicationAdapter {

	private MainController main;

	private BitmapFont titlefont;
	private String title;

	private PlayerResource resource;

	public MusicResult(MainController main) {
		this.main = main;
	}

	private long time = 0;

	public void create(PlayerResource resource) {
		this.resource = resource;
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
				Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
		title = "result";
		parameter.characters = title + parameter.characters;
		titlefont = generator.generateFont(parameter);
		time = System.currentTimeMillis();

		updateScoreDatabase();
	}

	public void render() {
		final SpriteBatch sprite = main.getSpriteBatch();
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		final float w = 1280;
		final float h = 720;

		sprite.begin();
		titlefont.setColor(Color.WHITE);
		titlefont.draw(sprite, title, w / 2, h / 2);

		IRScoreData score = resource.getScoreData();
		if (score != null) {
			titlefont.draw(sprite, "PGREAT : " + score.getPg(), 100, 250);
			titlefont.draw(sprite, "GREAT  : " + score.getGr(), 100, 220);
			titlefont.draw(sprite, "GOOD   : " + score.getGd(), 100, 190);
			titlefont.draw(sprite, "BAD    : " + score.getBd(), 100, 160);
			titlefont.draw(sprite, "POOR : " + score.getPr(), 100, 130);
		}
		sprite.end();
		// TODO キー入力で移行、および入力したキーでリプレイ、同じ譜面でリプレイ、等を分けたい
		if (resource.getScoreData() == null
				|| System.currentTimeMillis() > time + 1500) {
			main.changeState(MainController.STATE_SELECTMUSIC, null);
		}
	}

	public void updateScoreDatabase() {
		BMSModel model = resource.getBMSModel();
		IRScoreData newscore = resource.getScoreData();
		if (newscore == null) {
			return;
		}
		IRScoreData score = main.getScoreDatabase().getScoreData("Player",
				model.getHash(), false);
		if (score == null) {
			score = new IRScoreData();
		}
		score.setHash(model.getHash());
		score.setNotes(model.getTotalNotes()
				+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
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
		score.setLastupdate(Calendar.getInstance(TimeZone.getDefault())
				.getTimeInMillis() / 1000L);
		main.getScoreDatabase().setScoreData("Player", score);

		Logger.getGlobal().info("スコアデータベース更新完了 ");
	}
}
