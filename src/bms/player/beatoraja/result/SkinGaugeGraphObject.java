package bms.player.beatoraja.result;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.play.gauge.*;
import bms.player.beatoraja.skin.SkinObject;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.FloatArray;

import java.util.ArrayList;
import java.util.List;

/**
 * ゲージ遷移描画オブジェクト
 * 
 * @author exch
 */
public class SkinGaugeGraphObject extends SkinObject {

	private Texture backtex;
	private Texture shapetex;

	private int delay = 1500;

	private int lineWidth = 2;

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public int getLineWidth() {
		return lineWidth;
	}

	public void setLineWidth(int lineWidth) {
		this.lineWidth = lineWidth;
	}

	private int count;
	private long timecount;

	private final Color[] graphcolor = { Color.valueOf("440044"), Color.valueOf("004444"), Color.valueOf("004400"),
			Color.valueOf("440000"), Color.valueOf("444400"), Color.valueOf("444444") };
	private final Color[] graphline = { Color.valueOf("ff00ff"), Color.valueOf("00ffff"), Color.valueOf("00ff00"),
			Color.valueOf("ff0000"), Color.valueOf("ffff00"), Color.valueOf("cccccc") };
	private final Color borderline = Color.valueOf("ff0000");
	private final Color bordercolor = Color.valueOf("440000");

	private int color;
	private FloatArray gauge;
	private List<Integer> section;

	@Override
	public void draw(SpriteBatch sprite, long time, MainState state) {
		Rectangle graph = getDestination(time, state);
		if (graph == null) {
			return;
		}

		if (shapetex != null) {
			if (shapetex.getWidth() == (int) graph.getWidth() && shapetex.getHeight() == (int) graph.getHeight()) {
				// shape.setColor(Color.BLACK);
				// shape.fill();
			} else {
				backtex.dispose();
				shapetex.dispose();
				shapetex = null;
			}
		}
		if (shapetex == null) {
			PlayerResource resource = state.getMainController().getPlayerResource();
			Pixmap shape = new Pixmap((int) graph.width, (int) graph.height, Pixmap.Format.RGBA8888);
			// ゲージグラフ描画
			if (resource.getGrooveGauge() instanceof AssistEasyGrooveGauge) {
				color = 0;
			}
			if (resource.getGrooveGauge() instanceof EasyGrooveGauge) {
				color = 1;
			}
			if (resource.getGrooveGauge() instanceof NormalGrooveGauge) {
				color = 2;
			}
			if (resource.getGrooveGauge() instanceof HardGrooveGauge
					|| resource.getGrooveGauge() instanceof GradeGrooveGauge) {
				color = 3;
			}
			if (resource.getGrooveGauge() instanceof ExhardGrooveGauge
					|| resource.getGrooveGauge() instanceof ExgradeGrooveGauge) {
				color = 4;
			}
			gauge = resource.getGauge();
			section = new ArrayList<Integer>();
			if (state instanceof GradeResult) {
				gauge = new FloatArray();
				for (FloatArray l : resource.getCourseGauge()) {
					gauge.addAll(l);
					section.add((section.size() > 0 ? section.get(section.size() - 1) : 0) + l.size);
				}
			}
			shape.setColor(graphcolor[color]);
			shape.fill();
			final GrooveGauge gg = resource.getGrooveGauge();
			final float border = gg.getBorder();
			Color borderline = this.borderline;
			if (border > 0) {
				shape.setColor(bordercolor);
				shape.fillRectangle(0, (int) (graph.height * gg.getBorder() / gg.getMaxValue()), (int) (graph.width),
						(int) (graph.height * (gg.getMaxValue() - gg.getBorder()) / gg.getMaxValue()));
			} else {
				borderline = graphline[color];
			}
			backtex = new Texture(shape);
			shape.dispose();

			shape = new Pixmap((int) graph.width, (int) graph.height, Pixmap.Format.RGBA8888);
			Float f1 = null;

			for (int i = 0; i < gauge.size; i++) {
				if (section.contains(i)) {
					shape.setColor(Color.valueOf("ffffff"));
					shape.drawLine((int) (graph.width * (i - 1) / gauge.size), 0,
							(int) (graph.width * (i - 1) / gauge.size), (int) graph.height);
				}
				Float f2 = gauge.get(i);
				if (f1 != null) {
					final int x1 = (int) (graph.width * (i - 1) / gauge.size);
					final int y1 = (int) ((f1 / gg.getMaxValue()) * (graph.height - lineWidth));
					final int x2 = (int) (graph.width * i / gauge.size);
					final int y2 = (int) ((f2 / gg.getMaxValue()) * (graph.height - lineWidth));
					final int yb = (int) ((border / gg.getMaxValue()) * (graph.height - lineWidth));
					if (f1 < border) {
						if (f2 < border) {
							shape.setColor(graphline[color]);
							shape.fillRectangle(x1, Math.min(y1, y2), lineWidth, Math.abs(y2 - y1) + lineWidth);
							shape.fillRectangle(x1, y2, x2 - x1, lineWidth);
						} else {
							shape.setColor(graphline[color]);
							shape.fillRectangle(x1, y1, lineWidth, yb - y1);
							shape.setColor(borderline);
							shape.fillRectangle(x1, yb, lineWidth, y2 - yb + lineWidth);
							shape.fillRectangle(x1, y2, x2 - x1, lineWidth);
						}
					} else {
						if (f2 >= border) {
							shape.setColor(borderline);
							shape.fillRectangle(x1, Math.min(y1, y2), lineWidth, Math.abs(y2 - y1) + lineWidth);
							shape.fillRectangle(x1, y2, x2 - x1, lineWidth);
						} else {
							shape.setColor(borderline);
							shape.fillRectangle(x1, yb, lineWidth, y1 - yb + lineWidth);
							shape.setColor(graphline[color]);
							shape.fillRectangle(x1, y2, lineWidth, yb - y2);
							shape.fillRectangle(x1, y2, x2 - x1, lineWidth);
						}
					}
				}
				f1 = f2;
			}
			shapetex = new Texture(shape);
			shape.dispose();
		}

		sprite.draw(backtex, graph.x, graph.y + graph.height, graph.width, -graph.height);
		final float render = time >= delay ? 1.0f : (float) time / delay;
		sprite.draw(new TextureRegion(shapetex, 0, 0, (int) (graph.width * render), (int) graph.height), graph.x,
				graph.y + graph.height, graph.width * render, -graph.height);
	}

	@Override
	public void dispose() {
		if (shapetex != null) {
			shapetex.dispose();
			shapetex = null;
		}
	}
}
