package bms.player.beatoraja.result;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.*;

import bms.player.beatoraja.*;
import bms.player.beatoraja.PlayerResource.GaugeHistory;
import bms.player.beatoraja.play.AbstractGrooveGauge.Gauge;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.SkinObject;

/**
 * ゲージ遷移描画オブジェクト
 *
 * @author exch
 */
public class SkinGaugeGraphObject extends SkinObject {

	/**
	 * 背景テクスチャ
	 */
	private TextureRegion backtex;
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

	/**
	 * ボーダー下背景色
	 */
	private final Color[] graphcolor = new Color[6];
	/**
	 * ボーダー下グラフ色
	 */
	private final Color[] graphline = new Color[6];
	/**
	 * ボーダー上背景色
	 */
	private final Color[] borderline = new Color[6];
	/**
	 * ボーダー上グラフ色
	 */
	private final Color[] bordercolor = new Color[6];

	private final int[] typetable = {0,1,2,3,4,5,3,4,5,3};

	private int currentType = -1;
	private int color;
	private FloatArray gaugehistory;
	private FloatArray[] currentSource;
	private GaugeHistory timedHistory;
	private IntArray section;
	private Gauge gg;

	private float render;
	private boolean redraw;

	public SkinGaugeGraphObject() {
		this(new Color[][]{{Color.valueOf("ff0000"),Color.valueOf("440000"),Color.valueOf("ff00ff"),Color.valueOf("440044")},
			{Color.valueOf("ff0000"),Color.valueOf("440000"),Color.valueOf("00ffff"),Color.valueOf("004444")},
			{Color.valueOf("ff0000"),Color.valueOf("440000"),Color.valueOf("00ff00"),Color.valueOf("004400")},
			{Color.valueOf("ff0000"),Color.valueOf("440000")},
			{Color.valueOf("ffff00"),Color.valueOf("444400")},
			{Color.valueOf("cccccc"),Color.valueOf("444444")}
			});
	}

	public SkinGaugeGraphObject(Color[][] colors) {
		for(int i = 0;i < 6;i++) {
			if(colors.length > i) {
				borderline[i] = colors[i].length > 0 && colors[i][0] != null ? colors[i][0] : Color.valueOf("000000");
				bordercolor[i] = colors[i].length > 1 && colors[i][1] != null ? colors[i][1] : Color.valueOf("000000");
				graphline[i] = colors[i].length > 2 && colors[i][2] != null ? colors[i][2] : Color.valueOf("000000");
				graphcolor[i] = colors[i].length > 3 && colors[i][3] != null ? colors[i][3] : Color.valueOf("000000");
			} else {
				graphline[i] = graphcolor[i] = borderline[i] = bordercolor[i] = Color.valueOf("000000");
			}
		}
	}

	public SkinGaugeGraphObject(String assistClearBGColor, String assistAndEasyFailBGColor, String grooveFailBGColor, String grooveClearAndHardBGColor, String exHardBGColor, String hazardBGColor,
	String assistClearLineColor, String assistAndEasyFailLineColor, String grooveFailLineColor, String grooveClearAndHardLineColor, String exHardLineColor, String hazardLineColor,
	String borderlineColor, String borderColor) {
		graphcolor[0] = Color.valueOf(assistClearBGColor);
		graphcolor[1] = Color.valueOf(assistAndEasyFailBGColor);
		graphcolor[2] = Color.valueOf(grooveFailBGColor);
		bordercolor[3] = Color.valueOf(grooveClearAndHardBGColor);
		bordercolor[4] = Color.valueOf(exHardBGColor);
		bordercolor[5] = Color.valueOf(hazardBGColor);
		graphline[0] = Color.valueOf(assistClearLineColor);
		graphline[1] = Color.valueOf(assistAndEasyFailLineColor);
		graphline[2] = Color.valueOf(grooveFailLineColor);
		borderline[3] = Color.valueOf(grooveClearAndHardLineColor);
		borderline[4] = Color.valueOf(exHardLineColor);
		borderline[5] = Color.valueOf(hazardLineColor);

		for(int i = 0;i < 3;i++) {
			borderline[i] = Color.valueOf(borderlineColor);
			bordercolor[i] = Color.valueOf(borderColor);
		}
		for(int i = 3;i < 6;i++) {
			graphline[i] = borderline[i];
			graphcolor[i] = bordercolor[i];
		}
	}

	public void prepare(long time, MainState state) {
		render = time >= delay ? 1.0f : (float) time / delay;
		updateGraphData(state);
		super.prepare(time, state);
	}

	boolean updateGraphData(MainState state) {
		final PlayerResource resource = state.resource;
		var timed = state instanceof MusicResult result ? result.getDisplayGaugeHistory() : null;
		var gauge = timed == null ? resource.getGrooveGauge() : timed.gauge();
		var source = timed == null ? resource.getGauge() : timed.values();
		int type = state instanceof AbstractResult result ? result.getGaugeType() : gauge.getType();

		if (currentType != type || currentSource != source || timedHistory != timed) {
			redraw = true;
			currentType = type;
			currentSource = source;
			timedHistory = timed;
			gaugehistory = source[currentType];
			section = new IntArray();
			if (state instanceof CourseResult) {
				gaugehistory = new FloatArray();
				for (FloatArray[] l : resource.getCourseGauge()) {
					gaugehistory.addAll(l[currentType]);
					section.add((section.size > 0 ? section.get(section.size - 1) : 0) + l[currentType].size);
				}
			}
			gg = gauge.getGauge(currentType);
			return true;
		}
		return false;
	}

	@Override
	public void draw(SkinObjectRenderer sprite) {

		if (shapetex != null) {
			if (!redraw && shapetex.getTexture().getWidth() == (int) region.getWidth() && shapetex.getTexture().getHeight() == (int) region.getHeight()) {
				// shape.setColor(Color.BLACK);
				// shape.fill();
			} else {
				backtex.getTexture().dispose();
				backtex = null;
				shapetex.getTexture().dispose();
				shapetex = null;
			}
		}
		if (shapetex == null) {
			redraw = false;
			Pixmap shape = new Pixmap((int) region.width, (int) region.height, Pixmap.Format.RGBA8888);
			// ゲージグラフ描画
			color = typetable[currentType];
			shape.setColor(graphcolor[color]);
			shape.fill();
			final float border = gg.getProperty().border;
			final float max = gg.getProperty().max;
			shape.setColor(bordercolor[color]);
			shape.fillRectangle(0, (int) (region.height * border / max), (int) (region.width),
					(int) (region.height * (max - border) / max));

			backtex = new TextureRegion(new Texture(shape));
			shape.dispose();

			shape = new Pixmap((int) region.width, (int) region.height, Pixmap.Format.RGBA8888);
			if (timedHistory != null) {
				drawTimedHistory(shape);
			} else {
				drawLegacyHistory(shape, border, max);
			}
			shapetex = new TextureRegion(new Texture(shape));
			shape.dispose();
		}

		draw(sprite, backtex, region.x, region.y + region.height, region.width, -region.height);
		// setRegionにfloatを渡すと表示がおかしくなる
		shapetex.setRegion(0, 0, (int)(region.width * render), (int)region.height);
		draw(sprite, shapetex, region.x, region.y + region.height, (int)(region.width * render), -region.height);
	}

	private void drawLegacyHistory(Pixmap shape, float border, float max) {
		Float f1 = null;
		float lastGauge = -1;
		int lastX = -1;
		int lastY = -1;

		for (int i = 0; i < gaugehistory.size; i++) {
			if (section.contains(i)) {
				shape.setColor(Color.valueOf("ffffff"));
				shape.drawLine((int) (region.width * (i - 1) / gaugehistory.size), 0,
						(int) (region.width * (i - 1) / gaugehistory.size), (int) region.height);
			}
			Float f2 = gaugehistory.get(i);
			if (f1 != null) {
				final int x1 = (int) (region.width * (i - 1) / gaugehistory.size);
				final int y1 = (int) ((f1 / max) * (region.height - lineWidth));
				final int x2 = (int) (region.width * i / gaugehistory.size);
				final int y2 = (int) ((f2 / max) * (region.height - lineWidth));
				final int yb = (int) ((border / max) * (region.height - lineWidth));
				lastGauge = f2;
				lastX = x2;
				lastY = y2;
				if (f1 < border) {
					if (f2 < border) {
						shape.setColor(graphline[color]);
						shape.fillRectangle(x1, Math.min(y1, y2), lineWidth, Math.abs(y2 - y1) + lineWidth);
						shape.fillRectangle(x1, y2, x2 - x1, lineWidth);
					} else {
						shape.setColor(graphline[color]);
						shape.fillRectangle(x1, y1, lineWidth, yb - y1);
						shape.setColor(borderline[color]);
						shape.fillRectangle(x1, yb, lineWidth, y2 - yb + lineWidth);
						shape.fillRectangle(x1, y2, x2 - x1, lineWidth);
					}
				} else {
					if (f2 >= border) {
						shape.setColor(borderline[color]);
						shape.fillRectangle(x1, Math.min(y1, y2), lineWidth, Math.abs(y2 - y1) + lineWidth);
						shape.fillRectangle(x1, y2, x2 - x1, lineWidth);
					} else {
						shape.setColor(borderline[color]);
						shape.fillRectangle(x1, yb, lineWidth, y1 - yb + lineWidth);
						shape.setColor(graphline[color]);
						shape.fillRectangle(x1, y2, lineWidth, yb - y2);
						shape.fillRectangle(x1, y2, x2 - x1, lineWidth);
					}
				}
			}
			f1 = f2;
		}

		if (lastGauge != -1) {
			shape.setColor(lastGauge < border ? graphline[color] : borderline[color]);
			shape.fillRectangle(lastX, lastY, (int) (region.width - lastX), lineWidth);
		}

	}

	void drawTimedHistory(Pixmap shape) {
		if (gaugehistory.size == 0) return;
		int width = shape.getWidth();
		int height = shape.getHeight();
		int color = typetable[currentType];
		float max = gg.getProperty().max;
		float border = gg.getProperty().border;
		int borderY = (int) (border / max * (height - lineWidth));
		int lastX = 0;
		float last = gaugehistory.first();
		int lastY = (int) (last / max * (height - lineWidth));
		shape.setColor(last < border ? graphline[color] : borderline[color]);
		shape.fillRectangle(0, lastY, 1, lineWidth);

		for (int i = 1; i < gaugehistory.size; i++) {
			int x = (int) (timedHistory.timesUs().get(i) / (double) timedHistory.durationUs() * (width - 1));
			float value = gaugehistory.get(i);
			int y = (int) (value / max * (height - lineWidth));
			shape.setColor(last < border ? graphline[color] : borderline[color]);
			shape.fillRectangle(lastX, lastY, x - lastX, lineWidth);
			int left = Math.max(0, x - lineWidth + 1);
			int thickness = x - left + 1;
			int low = Math.min(y, lastY);
			int high = Math.max(y, lastY) + lineWidth;
			if (low < borderY) {
				shape.setColor(graphline[color]);
				shape.fillRectangle(left, low, thickness, Math.min(high, borderY) - low);
			}
			if (high > borderY) {
				shape.setColor(borderline[color]);
				shape.fillRectangle(left, Math.max(low, borderY), thickness, high - Math.max(low, borderY));
			}
			lastX = x;
			lastY = y;
			last = value;
		}
	}

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

	@Override
	public void dispose() {
		if (shapetex != null) {
			shapetex.getTexture().dispose();
			shapetex = null;
		}
		if (backtex != null) {
			backtex.getTexture().dispose();
			backtex = null;
		}
	}
}
