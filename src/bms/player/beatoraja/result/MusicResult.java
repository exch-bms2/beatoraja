package bms.player.beatoraja.result;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

import javafx.scene.canvas.GraphicsContext;

import org.lwjgl.opengl.GL11;

import bms.model.BMSModel;
import bms.model.Note;
import bms.model.TimeLine;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.gauge.GrooveGauge;
import bms.player.lunaticrave2.IRScoreData;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
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
public class MusicResult extends ApplicationAdapter {

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

	private Sound clear;
	private Sound fail;

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

	private long time = 0;

	public void create(PlayerResource resource) {
		this.resource = resource;
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
		title = "result";
		parameter.characters = title + resource.getBMSModel().getFullTitle() + parameter.characters;
		titlefont = generator.generateFont(parameter);
		time = System.currentTimeMillis();
		layout = new GlyphLayout(titlefont, resource.getBMSModel().getFullTitle());
		updateScoreDatabase();

		// TODO 後でJUDGERANK反映
		final int[] judgetable = { 20, 60, 165, 315, 1000 };

		data = new int[resource.getBMSModel().getLastTime() / 1000 + 1][6];
		fdata = new int[resource.getBMSModel().getLastTime() / 1000 + 1][3];
		for (TimeLine tl : resource.getBMSModel().getAllTimeLines()) {
			for (int i = 0; i < 18; i++) {
				Note n = tl.getNote(i);
				if (n != null) {
					if (n.getState() == 0) {
						data[tl.getTime() / 1000][0]++;
						fdata[tl.getTime() / 1000][0]++;
					} else {
						int dtime = n.getState() > 0 ? n.getState() - 1 : n.getState();
						for (int j = 0; j < judgetable.length; j++) {
							if (Math.abs(dtime) <= judgetable[j]) {
								data[tl.getTime() / 1000][j + 1]++;
								fdata[tl.getTime() / 1000][dtime >= 0 ? 1 : 2]++;
								break;
							}
						}
					}
				}
			}
		}
	}

	private int[][] data;
	private int[][] fdata;

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

		if (resource.getBGAManager().getStagefileData() != null) {
			sprite.begin();
			sprite.draw(resource.getBGAManager().getStagefileData(), 0, 0, w, h);
			sprite.end();
		}

		IRScoreData score = resource.getScoreData();
		// ゲージグラフ描画
		int gaugetype = resource.getConfig().getGauge();
		shape.begin(ShapeType.Filled);
		shape.setColor(graph_back[gaugetype]);
		shape.rect(graph.x, graph.y, graph.width, graph.height);
		if (resource.getGrooveGauge().getBorder() > 0) {
			shape.setColor(graph_back[3]);
			shape.rect(graph.x, graph.y + graph.height * resource.getGrooveGauge().getBorder() / 100, graph.width,
					graph.height * (100 - resource.getGrooveGauge().getBorder()) / 100);
		}
		shape.setColor(graph_back[gaugetype]);
		shape.end();

		Gdx.gl.glLineWidth(4);
		shape.begin(ShapeType.Line);
		shape.setColor(Color.WHITE);
		shape.rect(graph.x, graph.y, graph.width, graph.height);

		Float f1 = null;
		for (int i = 0; i < resource.getGauge().size(); i++) {
			Float f2 = resource.getGauge().get(i);
			if (f1 != null) {
				shape.setColor(graph_line[gaugetype]);
				shape.line(graph.x + graph.width * (i - 1) / resource.getGauge().size(), graph.y + (f1 / 100.0f)
						* graph.height, graph.x + graph.width * i / resource.getGauge().size(), graph.y + (f2 / 100.0f)
						* graph.height);
			}
			f1 = f2;
		}
		shape.end();
		Gdx.gl.glLineWidth(1);

		Gdx.gl.glEnable(GL11.GL_BLEND);
		Gdx.gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		shape.begin(ShapeType.Filled);
		shape.setColor(0, 0, 0, 0.8f);
		shape.rect(80, 100, 1120, 350);
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

			titlefont.draw(sprite, "PGREAT : " + score.getPg(), 100, 250);
			titlefont.draw(sprite, "GREAT  : " + score.getGr(), 100, 220);
			titlefont.draw(sprite, "GOOD   : " + score.getGd(), 100, 190);
			titlefont.draw(sprite, "BAD    : " + score.getBd(), 100, 160);
			titlefont.draw(sprite, "POOR : " + score.getPr(), 100, 130);
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

		if (((System.currentTimeMillis() - time) / 5000) % 2 == 0) {
			drawGraph(data, JGRAPH);
			sprite.begin();
			titlefont.setColor(Color.GREEN);
			titlefont.draw(sprite, "JUDGE DETAIL", 500, 700);
			sprite.end();
		} else {
			drawGraph(fdata, FGRAPH);
			sprite.begin();
			titlefont.setColor(Color.CYAN);
			titlefont.draw(sprite, "FAST/SLOW", 500, 700);
			sprite.end();
		}

		boolean[] keystate = main.getInputProcessor().getKeystate();
		long[] keytime = main.getInputProcessor().getTime();
		if (resource.getScoreData() == null
				|| ((System.currentTimeMillis() > time + 500 && (keystate[0] || keystate[2] || keystate[4] || keystate[6])))) {
			if (resource.getCourseBMSModels() != null) {
				if (resource.getGauge().get(resource.getGauge().size() - 1) <= 0) {
					// 不合格リザルト
					main.changeState(MainController.STATE_GRADE_RESULT, resource);
				} else if (resource.nextCourse()) {
					main.changeState(MainController.STATE_PLAYBMS, resource);
				} else {
					// 合格リザルト
					main.changeState(MainController.STATE_GRADE_RESULT, resource);
				}
			} else {
				if (keystate[4]) {
					keytime[4] = 0;
					// オプションを変更せず同じ譜面でリプレイ
					resource.setPatternModifyLog(null);
					resource.reloadBMSFile();
					main.changeState(MainController.STATE_PLAYBMS, resource);
				} else if (keystate[6]) {
					keytime[6] = 0;
					// 同じ譜面でリプレイ
					resource.reloadBMSFile();
					main.changeState(MainController.STATE_PLAYBMS, resource);
				} else {
					keytime[0] = keytime[2] = 0;
					main.changeState(MainController.STATE_SELECTMUSIC, resource);
				}
			}
		}
	}

	public void updateScoreDatabase() {
		BMSModel model = resource.getBMSModel();
		IRScoreData newscore = resource.getScoreData();
		String hash = model.getHash();
		boolean ln = model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
				+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
		if (ln && resource.getConfig().getLnmode() > 0) {
			hash = "C" + hash;
		}
		if (newscore == null) {
			return;
		}
		IRScoreData score = main.getScoreDatabase().getScoreData("Player", hash, false);
		if (score == null) {
			score = new IRScoreData();
		}
		int clear;
		if (ln && resource.getConfig().getLnmode() == 2) {
			clear = oldclear = score.getExclear();
		} else {
			clear = oldclear = score.getClear();
		}
		oldexscore = score.getExscore();
		oldmisscount = score.getMinbp();
		score.setHash(hash);
		score.setNotes(model.getTotalNotes());

		if (newscore.getClear() != GrooveGauge.CLEARTYPE_FAILED) {
			score.setClearcount(score.getClearcount() + 1);
		}
		if (clear < newscore.getClear()) {
			if (ln && resource.getConfig().getLnmode() == 2) {
				score.setExclear(newscore.getClear());				
			} else {
				score.setClear(newscore.getClear());				
			}
			score.setOption(resource.getConfig().getRandom());
		}

		final int pgreat = newscore.getPg();
		final int great = newscore.getGr();
		final int good = newscore.getGd();
		final int bad = newscore.getBd();
		final int poor = newscore.getPr();
		int exscore = pgreat * 2 + great;
		if (score.getExscore() < exscore && resource.isUpdateScore()) {
			score.setPg(pgreat);
			score.setGr(great);
			score.setGd(good);
			score.setBd(bad);
			score.setPr(poor);
		}
		if (score.getMinbp() > newscore.getMinbp() && resource.isUpdateScore()) {
			score.setMinbp(newscore.getMinbp());
		}
		score.setPlaycount(score.getPlaycount() + 1);
		score.setLastupdate(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis() / 1000L);
		main.getScoreDatabase().setScoreData("Player", score);

		Logger.getGlobal().info("スコアデータベース更新完了 ");

		// コースモードの場合はコーススコアに加算・累積する
		if (resource.getCourseBMSModels() != null) {
			IRScoreData cscore = resource.getCourseScoreData();
			if (cscore == null) {
				cscore = new IRScoreData();
				cscore.setMinbp(0);
				resource.setCourseScoreData(cscore);
			}
			cscore.setPg(cscore.getPg() + newscore.getPg());
			cscore.setGr(cscore.getGr() + newscore.getGr());
			cscore.setGd(cscore.getGd() + newscore.getGd());
			cscore.setBd(cscore.getBd() + newscore.getBd());
			cscore.setPr(cscore.getPr() + newscore.getPr());
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

	private static final String[] JGRAPH = { "555555", "0088ff", "00ff88", "ffff00", "ff8800", "ff0000" };
	private static final String[] FGRAPH = { "555555", "0088ff", "ff8800" };

	/**
	 * 密度分布グラフの描画
	 * 
	 * @param data
	 */
	private void drawGraph(int[][] data, String[] GRAPH) {

		final ShapeRenderer shape = main.getShapeRenderer();

		float x = 500;
		float y = 500;
		float w = 700;
		float h = 200;

		int max = 40;

		Gdx.gl.glEnable(GL11.GL_BLEND);
		Gdx.gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		shape.begin(ShapeType.Filled);
		shape.setColor(0, 0, 0, 0.8f);
		shape.rect(x, y, w, h);
		shape.end();
		Gdx.gl.glDisable(GL11.GL_BLEND);

		shape.begin(ShapeType.Filled);
		for (int i = 0; i < data.length; i++) {
			// BPM変化地点描画
			int[] n = data[i];
			// x軸補助線描画
			if (i % 30 == 0) {
				shape.setColor(Color.valueOf("666666"));
				shape.line(x + i * w / data.length, y, x + i * w / data.length, y + h);
			}

			for (int j = 0, k = n[0], index = 0; index < GRAPH.length;) {
				if (k > 0) {
					k--;
					shape.setColor(Color.valueOf(GRAPH[index]));
					shape.rect(x + w * i / data.length, y + j * (h / max), w / data.length - 1, (h / max) - 1);
					j++;
				} else {
					index++;
					if (index == GRAPH.length) {
						break;
					}
					k = n[index];
				}
			}
		}
		shape.end();
	}

}
