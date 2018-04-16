package bms.player.beatoraja.result;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.play.GrooveGauge.Gauge;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.SkinObject;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;

/**
 * ゲージ遷移描画オブジェクト
 * 
 * @author exch
 */
public class SkinGaugeGraphObject extends SkinObject {

	/**
	 * 背景テクスチャ
	 */
	private Texture backtex;
	/**
	 * グラフテクスチャ
	 */
	private TextureRegion shapetex;
	/**
	 * ゲージ描画を完了するまでの時間(ms)
	 */
	private int delay = 1500;
	/**
	 * グラフ線の太さ
	 */
	private int lineWidth = 2;
	
	private int currentType;

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

	private final Color[] graphcolor = { Color.valueOf("440044"), Color.valueOf("004444"), Color.valueOf("004400"),
			Color.valueOf("440000"), Color.valueOf("444400"), Color.valueOf("444444") };
	private final Color[] graphline = { Color.valueOf("ff00ff"), Color.valueOf("00ffff"), Color.valueOf("00ff00"),
			Color.valueOf("ff0000"), Color.valueOf("ffff00"), Color.valueOf("cccccc") };
	private final Color borderline = Color.valueOf("ff0000");
	private final Color bordercolor = Color.valueOf("440000");
	private final int[] typetable = {0,1,2,3,4,5,3,4,5,3};

	private int color;
	private FloatArray gauge;

	@Override
	public void draw(SkinObjectRenderer sprite, long time, MainState state) {
		Rectangle graph = getDestination(time, state);
		if (graph == null) {
			return;
		}
		
		final PlayerResource resource = state.main.getPlayerResource();
		int type = resource.getGrooveGauge().getType();
		if(state instanceof AbstractResult) {
			type = ((AbstractResult) state).gaugeType;
		}

		if (shapetex != null) {
			if (currentType == type && shapetex.getTexture().getWidth() == (int) graph.getWidth() && shapetex.getTexture().getHeight() == (int) graph.getHeight()) {
				// shape.setColor(Color.BLACK);
				// shape.fill();
			} else {
				backtex.dispose();
				backtex = null;
				shapetex.getTexture().dispose();
				shapetex = null;
			}
		}
		if (shapetex == null) {
			currentType = type;
			Pixmap shape = new Pixmap((int) graph.width, (int) graph.height, Pixmap.Format.RGBA8888);
			// ゲージグラフ描画
			color = typetable[currentType];
			gauge = resource.getGauge()[currentType];
			IntArray section = new IntArray();
			if (state instanceof CourseResult) {
				gauge = new FloatArray();
				for (FloatArray[] l : resource.getCourseGauge()) {
					gauge.addAll(l[currentType]);
					section.add((section.size > 0 ? section.get(section.size - 1) : 0) + l[currentType].size);
				}
			}
			shape.setColor(graphcolor[color]);
			shape.fill();
			final Gauge gg = resource.getGrooveGauge().getGauge(currentType);
			final float border = gg.getProperty().border;
			final float max = gg.getProperty().max;
			Color borderline = this.borderline;
			if (border > 0) {
				shape.setColor(bordercolor);
				shape.fillRectangle(0, (int) (graph.height * border / max), (int) (graph.width),
						(int) (graph.height * (max - border) / max));
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
					final int y1 = (int) ((f1 / max) * (graph.height - lineWidth));
					final int x2 = (int) (graph.width * i / gauge.size);
					final int y2 = (int) ((f2 / max) * (graph.height - lineWidth));
					final int yb = (int) ((border / max) * (graph.height - lineWidth));
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
			shapetex = new TextureRegion(new Texture(shape));
			shape.dispose();
		}

		sprite.draw(backtex, graph.x, graph.y + graph.height, graph.width, -graph.height);
		final float render = time >= delay ? 1.0f : (float) time / delay;
		// setRegionにfloatを渡すと表示がおかしくなる
		shapetex.setRegion(0, 0, (int)(graph.width * render), (int)graph.height);
		sprite.draw(shapetex, graph.x, graph.y + graph.height, (int)(graph.width * render), -graph.height);
	}

	@Override
	public void dispose() {
		if (shapetex != null) {
			shapetex.getTexture().dispose();
			shapetex = null;
		}
		if (backtex != null) {
			backtex.dispose();
			backtex = null;
		}
	}
}
