package bms.player.beatoraja.result;

import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.gauge.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
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
        final GrooveGauge gg = resource.getGrooveGauge();
        if (gg.getBorder() > 0) {
            shape.setColor(Color.valueOf("440000"));
            shape.rect(graph.x, graph.y + graph.height * gg.getBorder() / gg.getMaxValue(), graph.width,
                    graph.height * (gg.getMaxValue() - gg.getBorder()) / gg.getMaxValue());
        }
        shape.setColor(Color.valueOf(graphcolor));
        shape.end();

        Gdx.gl.glLineWidth(2);
        shape.begin(ShapeRenderer.ShapeType.Line);

        Float f1 = null;
        for (int i = 0; i < gauge.size(); i++) {
            Float f2 = gauge.get(i);
            if (f1 != null) {
                shape.setColor(Color.valueOf(graphline));
                shape.line(graph.x + graph.width * (i - 1) / gauge.size(), graph.y + (f1 / gg.getMaxValue())
                        * graph.height, graph.x + graph.width * i / gauge.size(), graph.y + (f2 / gg.getMaxValue())
                        * graph.height);
            }
            f1 = f2;
        }
        shape.end();
        Gdx.gl.glLineWidth(1);

    }

}
