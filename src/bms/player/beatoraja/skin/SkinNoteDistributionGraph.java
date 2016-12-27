package bms.player.beatoraja.skin;

import bms.model.*;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.pattern.LongNoteModifier;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import org.lwjgl.opengl.GL11;

/**
 * Created by exch on 2016/12/19.
 */
public class SkinNoteDistributionGraph extends SkinObject {

    private ShapeRenderer shape = new ShapeRenderer();

    private int[][] data;

    private static final String[] JGRAPH = { "cccccc", "4444ff", "ff4444", "44ff44", "880000"};

    private int max = 20;

    public void draw(SpriteBatch sprite, long time, MainState state) {
        draw(sprite, time, state, getDestination(time,state), -1, -1);
    }

    public void draw(SpriteBatch sprite, long time, MainState state, Rectangle r, int starttime, int endtime) {
        if(data == null) {
            BMSModel model = state.getMainController().getPlayerResource().getBMSModel();
            data = new int[model.getLastTime() / 1000 + 1][5];
            int pos = 0;
            int count = 0;
            max = 20;
            for (TimeLine tl : model.getAllTimeLines()) {
                if (tl.getTime() / 1000 != pos) {
                    if (max < count) {
                        max = (count / 10) * 10 + 10;
                    }
                    pos = tl.getTime() / 1000;
                    count = 0;
                }
                for (int i = 0; i < 18; i++) {
                    Note n = tl.getNote(i);
                    if (n != null
                            && !(model.getLntype() == BMSModel.LNTYPE_LONGNOTE && n instanceof LongNote && ((LongNote) n)
                            .getEndnote().getSection() == tl.getSection())) {

                        if(n instanceof NormalNote) {
                            data[tl.getTime() / 1000][model.getUseKeys() != 9 && (i == 7 || i == 16) ? 2 : 0]++;
                        }
                        if(n instanceof LongNote) {
                            data[tl.getTime() / 1000][model.getUseKeys() != 9 && (i == 7 || i == 16) ? 3 : 1]++;
                        }
                        if(n instanceof MineNote) {
                            data[tl.getTime() / 1000][4]++;
                        }
                        count++;
                    }
                }
            }
        }
        sprite.end();
        drawGraph(r, data, JGRAPH);
        if(starttime != -1) {
            BMSModel model = state.getMainController().getPlayerResource().getBMSModel();
            shape.begin(ShapeRenderer.ShapeType.Line);
            shape.setColor(Color.valueOf("88ff88"));
            float dx = starttime * r.width / (data.length * 1000);
            shape.line(r.x + dx, r.y, r.x + dx, r.y + r.height);
            shape.end();
        }
        if(endtime != -1) {
            BMSModel model = state.getMainController().getPlayerResource().getBMSModel();
            shape.begin(ShapeRenderer.ShapeType.Line);
            shape.setColor(Color.valueOf("8888ff"));
            float dx = endtime * r.width / (data.length * 1000);
            shape.line(r.x + dx, r.y, r.x + dx, r.y + r.height);
            shape.end();
        }
        sprite.begin();
    }

    private void drawGraph(Rectangle judge, int[][] data, String[] GRAPH) {
        if (judge == null) {
            return;
        }
        Gdx.gl.glEnable(GL11.GL_BLEND);
        Gdx.gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0, 0, 0, 0.8f);
        shape.rect(judge.x, judge.y, judge.width, judge.height);
        shape.end();
        Gdx.gl.glDisable(GL11.GL_BLEND);

        shape.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 10; i < max; i += 10) {
            shape.setColor(Color.valueOf("444444"));
            shape.line(judge.x, judge.y + judge.height * i / max, judge.x + judge.width, judge.y + judge.height * i
                    / max);
        }
        shape.end();
        for (int i = 0; i < data.length; i++) {
            // BPM変化地点描画
            int[] n = data[i];
            // x軸補助線描画
            if (i % 60 == 0) {
                shape.begin(ShapeRenderer.ShapeType.Line);
                shape.setColor(Color.valueOf("444444"));
                shape.line(judge.x + i * judge.width / data.length, judge.y, judge.x + i * judge.width / data.length,
                        judge.y + judge.height);
                shape.end();
            } else if (i % 10 == 0) {
                shape.begin(ShapeRenderer.ShapeType.Line);
                shape.setColor(Color.valueOf("222222"));
                shape.line(judge.x + i * judge.width / data.length, judge.y, judge.x + i * judge.width / data.length,
                        judge.y + judge.height);
                shape.end();
            }

            shape.begin(ShapeRenderer.ShapeType.Filled);
            for (int j = 0, k = n[0], index = 0; index < GRAPH.length;) {
                if (k > 0) {
                    k--;
                    shape.setColor(Color.valueOf(GRAPH[index]));
                    shape.rect(judge.x + judge.width * i / data.length, judge.y + j * (judge.height / max), judge.width
                            / data.length - 1, (judge.height / max) - 1);
                    j++;
                } else {
                    index++;
                    if (index == GRAPH.length) {
                        break;
                    }
                    k = n[index];
                }
            }
            shape.end();
        }
    }

    @Override
    public void dispose() {
        shape.dispose();
    }

}
