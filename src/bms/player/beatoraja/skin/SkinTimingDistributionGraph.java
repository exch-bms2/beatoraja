package bms.player.beatoraja.skin;

import java.util.Optional;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.result.MusicResult;
import bms.player.beatoraja.result.AbstractResult.TimingDistribution;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

/**
 * 判定タイミング分布のグラフ
 *
 * @author keh
 */
public class SkinTimingDistributionGraph extends SkinObject {

	private TextureRegion tex = null;
	private Pixmap shape = null;

	private final int gx;
	private final int c;
	private final boolean drawAverage;
	private final boolean drawDev;
	private int max = 10;
	private Color[] JColor;
	private Color graphColor;
	private Color averageColor;
	private Color devColor;

	private MusicResult state;

	public SkinTimingDistributionGraph(int width, int lineWidth,
			String graphColor, String averageColor, String devColor, String PGColor, String GRColor, String GDColor, String BDColor,
			String PRColor,
			int drawAverage, int drawDev) {
		int w = 1 < width ? width : 1;
		int lw = MathUtils.clamp(lineWidth, 1, width);
		this.gx = w / lw;
		this.c = gx / 2;
		this.graphColor = Color.valueOf(SkinTimingVisualizer.colorStringValidation(graphColor));
		this.averageColor = Color.valueOf(SkinTimingVisualizer.colorStringValidation(averageColor));
		this.devColor = Color.valueOf(SkinTimingVisualizer.colorStringValidation(devColor));
		JColor = new Color[] {
				Color.valueOf(SkinTimingVisualizer.colorStringValidation(PGColor)),
				Color.valueOf(SkinTimingVisualizer.colorStringValidation(GRColor)),
				Color.valueOf(SkinTimingVisualizer.colorStringValidation(GDColor)),
				Color.valueOf(SkinTimingVisualizer.colorStringValidation(BDColor)),
				Color.valueOf(SkinTimingVisualizer.colorStringValidation(PRColor))
		};
		this.drawAverage = (drawAverage == 1);
		this.drawDev = (drawDev == 1);
	}

	public void prepare(long time, MainState state) {
		if(!(state instanceof MusicResult)) {
			draw = false;
			return;
		}
		this.state = (MusicResult) state;
		super.prepare(time, state);
		
	}
	
	public void draw(SkinObjectRenderer sprite) {
		// Texture生成は一度だけ
		if (tex == null) {
			TimingDistribution td = state.getTimingDistribution();
			int[] dist = td.getTimingDistribution();
			final int center = td.getArrayCenter();
			int[][] judgeArea = SkinTimingVisualizer.getJudgeArea(state.main.getPlayerResource());

			for (int d : dist) {
				if (max < d) {
					max = (d / 10) * 10 + 10;
				}
			}

			Pixmap shape = new Pixmap(gx, max, Pixmap.Format.RGBA8888);
			//グラフエリア描画
			shape.setColor(JColor[0]);
			shape.fillRectangle(c, 0, 1, max);// ジャスト
			int beforex1 = c;
			int beforex2 = c + 1;
			for (int i = 0; i < JColor.length; i++) {
				shape.setColor(JColor[i]);
				int x1 = c + MathUtils.clamp(judgeArea[i][0], -c, c);
				int x2 = c + MathUtils.clamp(judgeArea[i][1], -c, c) + 1;

				if (beforex1 > x1) {
					shape.fillRectangle(x1, 0, Math.abs(x1 - beforex1), max);
					beforex1 = x1;
				}

				if (x2 > beforex2) {
					shape.fillRectangle(beforex2, 0, Math.abs(x2 - beforex2), max);
					beforex2 = x2;
				}
			}
			
			shape.setColor(0f, 0f, 0f, 0.25f);
			for(int x = c % 10;x < c * 2 + 1;x += 10) {
				shape.drawLine(x, 0, x, 1);
			}

			//平均描画
			if (drawAverage && td.getAverage() != Float.MAX_VALUE) {
				int avg = Math.round(td.getAverage());
				shape.setColor(averageColor);
				shape.drawLine(c + avg, 0, c + avg, max);
			}

			//偏差エリア描画
			if (drawDev && td.getStdDev() != -1.0f) {
				int avg = Math.round(td.getAverage());
				int dev = Math.round(td.getStdDev());
				shape.setColor(devColor);
				shape.drawLine(c + avg + dev, 0, c + avg + dev, max);
				shape.drawLine(c + avg - dev, 0, c + avg - dev, max);
			}

			//グラフ描画
			shape.setColor(graphColor);
			for (int i = -c; i < gx - c ; i++) {
				if (-center < i && i < center) {
					shape.fillRectangle(c + i, max - dist[center + i], 1, dist[center + i]);
				}
			}


			tex = new TextureRegion(new Texture(shape));
			shape.dispose();
		}

		draw(sprite, tex);

	}

	@Override
	public void dispose() {
		Optional.ofNullable(tex).ifPresent(t -> t.getTexture().dispose());
		Optional.ofNullable(shape).ifPresent(Pixmap::dispose);
	}

}
