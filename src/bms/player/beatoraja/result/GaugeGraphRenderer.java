package bms.player.beatoraja.result;

import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.gauge.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

import java.util.List;

/**
 * Created by exch on 2016/06/05.
 */
public class GaugeGraphRenderer {

    public void render(ShapeRenderer shape, long time, PlayerResource resource, Rectangle graph, List<Float> gauge) {
        // ゲージグラフ描画
        String graphcolor = "444444";
        String graphline = "cccccc";
        if(resource.getGrooveGauge() instanceof AssistEasyGrooveGauge) {
            graphcolor = "440044";
            graphline = "ff00ff";
        }
        if(resource.getGrooveGauge() instanceof EasyGrooveGauge) {
            graphcolor = "004444";
            graphline = "00ffff";
        }
        if(resource.getGrooveGauge() instanceof NormalGrooveGauge) {
            graphcolor = "004400";
            graphline = "00ff00";
        }
        if(resource.getGrooveGauge() instanceof HardGrooveGauge || resource.getGrooveGauge() instanceof GradeGrooveGauge) {
            graphcolor = "440000";
            graphline = "ff0000";
        }
        if(resource.getGrooveGauge() instanceof ExhardGrooveGauge || resource.getGrooveGauge() instanceof ExgradeGrooveGauge) {
            graphcolor = "444400";
            graphline = "ffff00";
        }

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(Color.valueOf(graphcolor));
        shape.rect(graph.x, graph.y, graph.width, graph.height);
        if (resource.getGrooveGauge().getBorder() > 0) {
            shape.setColor(Color.valueOf("440000"));
            shape.rect(graph.x, graph.y + graph.height * resource.getGrooveGauge().getBorder() / 100, graph.width,
                    graph.height * (100 - resource.getGrooveGauge().getBorder()) / 100);
        }
        shape.setColor(Color.valueOf(graphcolor));
        shape.end();

        Gdx.gl.glLineWidth(4);
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.WHITE);
        shape.rect(graph.x, graph.y, graph.width, graph.height);

        Float f1 = null;
        for (int i = 0; i < gauge.size(); i++) {
            Float f2 = gauge.get(i);
            if (f1 != null) {
                shape.setColor(Color.valueOf(graphline));
                shape.line(graph.x + graph.width * (i - 1) / gauge.size(), graph.y + (f1 / 100.0f)
                        * graph.height, graph.x + graph.width * i / gauge.size(), graph.y + (f2 / 100.0f)
                        * graph.height);
            }
            f1 = f2;
        }
        shape.end();
        Gdx.gl.glLineWidth(1);

    }

}
