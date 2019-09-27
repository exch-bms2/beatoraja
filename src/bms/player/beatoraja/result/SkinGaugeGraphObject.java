package bms.player.beatoraja.result;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.*;

import bms.player.beatoraja.*;
import bms.player.beatoraja.play.GrooveGauge.Gauge;
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

		final PlayerResource resource = state.main.getPlayerResource();
		int type = resource.getGrooveGauge().getType();
		if(state instanceof AbstractResult) {
			type = ((AbstractResult) state).gaugeType;
		}

		if(currentType != type) {
			redraw = true;
			currentType = type;
			gaugehistory = resource.getGauge()[currentType];
			section = new IntArray();
			if (state instanceof CourseResult) {
				gaugehistory = new FloatArray();
				for (FloatArray[] l : resource.getCourseGauge()) {
					gaugehistory.addAll(l[currentType]);
					section.add((section.size > 0 ? section.get(section.size - 1) : 0) + l[currentType].size);
				}
			}
			gg = resource.getGrooveGauge().getGauge(currentType);
		}
		super.prepare(time, state);
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
			Float f1 = null;

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
			shapetex = new TextureRegion(new Texture(shape));
			shape.dispose();
		}

		draw(sprite, backtex, region.x, region.y + region.height, region.width, -region.height);
		// setRegionにfloatを渡すと表示がおかしくなる
		shapetex.setRegion(0, 0, (int)(region.width * render), (int)region.height);
		draw(sprite, shapetex, region.x, region.y + region.height, (int)(region.width * render), -region.height);
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
