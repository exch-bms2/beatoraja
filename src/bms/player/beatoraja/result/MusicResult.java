package bms.player.beatoraja.result;

import java.io.File;

import org.lwjgl.opengl.GL11;

import bms.model.BMSModel;
import bms.model.Note;
import bms.model.TimeLine;
import bms.player.beatoraja.*;
import bms.player.beatoraja.gauge.GrooveGauge;

import com.badlogic.gdx.ApplicationAdapter;
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
public class MusicResult extends ApplicationAdapter {

    private static final String[] LAMP = {"000000", "808080", "800080", "ff00ff", "40ff40", "f0c000", "ffffff",
            "ffff88", "88ffff", "ff8888", "ff0000"};
    private static final String[] CLEAR = {"NO PLAY", "FAILED", "ASSIST CLEAR", "L-ASSIST CLEAR", "EASY CLEAR",
            "CLEAR", "HARD CLEAR", "EX-HARD CLEAR", "FULL COMBO", "PERFECT", "MAX"};

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

    private MusicResultSkin skin;

    private DetailGraphRenderer detail;

    private long time = 0;

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
        skin = new MusicResultSkin();
    }

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
        // 保存されているリプレイデータがない場合は、EASY以上で自動保存
        if (resource.getAutoplay() == 0 && resource.getScoreData() != null
                && resource.getScoreData().getClear() >= GrooveGauge.CLEARTYPE_EASY
                && !main.getPlayDataAccessor().existsReplayData(resource.getBMSModel(), resource.getConfig().getLnmode())) {
            saveReplayData();
        }

        detail = new DetailGraphRenderer(resource.getBMSModel());
    }

    private final Color[] graph_back = {Color.valueOf("440044"), Color.valueOf("000044"), Color.valueOf("004400"),
            Color.valueOf("440000"), Color.valueOf("444400"), Color.valueOf("222222")};
    private final Color[] graph_line = {Color.valueOf("ff00ff"), Color.valueOf("0000ff"), Color.valueOf("00ff00"),
            Color.valueOf("ff0000"), Color.valueOf("ffff00"), Color.valueOf("cccccc")};

    public void render() {
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

            titlefont.draw(sprite, String.format("PGREAT : %4d ( %4d / %4d )", score.getPg(), score.getFpg(), score.getSpg()), 100, 280);
            titlefont.draw(sprite, String.format("GREAT  : %4d ( %4d / %4d )", score.getGr(), score.getFgr(), score.getSgr()), 100, 250);
            titlefont.draw(sprite, String.format("GOOD   : %4d ( %4d / %4d )", score.getGd(), score.getFgd(), score.getSgd()), 100, 220);
            titlefont.draw(sprite, String.format("GBAD   : %4d ( %4d / %4d )", score.getBd(), score.getFbd(), score.getSbd()), 100, 190);
            titlefont.draw(sprite, String.format("POOR   : %4d ( %4d / %4d )", score.getPr(), score.getFpr(), score.getSpr()), 100, 160);
            titlefont.draw(sprite, String.format("MISS   : %4d ( %4d / %4d )", score.getFms() + score.getSms(), score.getFms(), score.getSms()), 100, 130);
            titlefont.draw(sprite, String.format("FAST / SLOW  :  %4d / %4d", score.getFgr() + score.getFgd() + score.getFbd() + score.getFpr() + score.getFms()
                    , score.getSgr() + score.getSgd() + score.getSbd() + score.getSpr() + score.getSms()), 100, 100);
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

        detail.render(sprite, titlefont, shape, System.currentTimeMillis() - time, skin.getJudgeRegion());

        boolean[] keystate = main.getInputProcessor().getKeystate();
        long[] keytime = main.getInputProcessor().getTime();
        if (resource.getScoreData() == null
                || ((System.currentTimeMillis() > time + 500 && (keystate[0] || keystate[2] || keystate[4] || keystate[6])))) {
            if (resource.getCourseBMSModels() != null) {
                if (resource.getGauge().get(resource.getGauge().size() - 1) <= 0) {
                    // 不合格リザルト
                    main.changeState(MainController.STATE_GRADE_RESULT);
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
            return;
        }
        IRScoreData score = main.getPlayDataAccessor().readScoreData(resource.getBMSModel(),
                resource.getConfig().getLnmode());
        if (score == null) {
            score = new IRScoreData();
        }
        if (ln && resource.getConfig().getLnmode() == 2) {
            oldclear = score.getExclear();
        } else {
            oldclear = score.getClear();
        }
        oldexscore = score.getExscore();
        oldmisscount = score.getMinbp();
        // コースモードの場合はコーススコアに加算・累積する
        if (resource.getCourseBMSModels() != null) {
            if (resource.getScoreData().getClear() == GrooveGauge.CLEARTYPE_FAILED) {
                resource.getScoreData().setClear(GrooveGauge.CLEARTYPE_NOPLAY);
            }
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

        if(resource.getAutoplay() == 0) {
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

    private static final String[] JGRAPH = {"555555", "0088ff", "00ff88", "ffff00", "ff8800", "ff0000"};
    private static final String[] FGRAPH = {"555555", "0088ff", "ff8800"};

    /**
     * 密度分布グラフの描画
     *
     * @param data
     */
    private void drawGraph(int[][] data, String[] GRAPH) {

        final ShapeRenderer shape = main.getShapeRenderer();

        int max = 40;

        Rectangle judge = skin.getJudgeRegion();

        Gdx.gl.glEnable(GL11.GL_BLEND);
        Gdx.gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeType.Filled);
        shape.setColor(0, 0, 0, 0.8f);
        shape.rect(judge.x, judge.y, judge.width, judge.height);
        shape.end();
        Gdx.gl.glDisable(GL11.GL_BLEND);

        shape.begin(ShapeType.Filled);
        for (int i = 0; i < data.length; i++) {
            // BPM変化地点描画
            int[] n = data[i];
            // x軸補助線描画
            if (i % 30 == 0) {
                shape.setColor(Color.valueOf("666666"));
                shape.line(judge.x + i * judge.width / data.length, judge.y, judge.x + i * judge.width / data.length,
                        judge.y + judge.height);
            }

            for (int j = 0, k = n[0], index = 0; index < GRAPH.length; ) {
                if (k > 0) {
                    k--;
                    shape.setColor(Color.valueOf(GRAPH[index]));
                    shape.rect(judge.x + judge.width * i / data.length, judge.y + j * (judge.height / max),
                            judge.width / data.length - 1, (judge.height / max) - 1);
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
