package bms.player.beatoraja.skin;

import java.util.Optional;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.play.BMSPlayerRule;
import bms.player.beatoraja.play.JudgeProperty;
import bms.player.beatoraja.result.MusicResult;
import bms.player.beatoraja.result.MusicResult.TimingDistribution;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

/**
 * 判定タイミング分布のグラフ
 *
 * @author keh
 */
public class SkinTimingDistributionGraph extends SkinObject {

	private TextureRegion tex = null;
	private Pixmap shape = null;

	private final boolean drawAverage;

	private int max = 10;

	private static final Color[] JColor = new Color[] {
			Color.valueOf("000088"),
			Color.valueOf("008800"),
			Color.valueOf("666600"),
			Color.valueOf("880000"),
			Color.valueOf("000000") };

	public SkinTimingDistributionGraph() {
		this(1);
	}

	public SkinTimingDistributionGraph(int drawAverage) {
		this.drawAverage = drawAverage == 1 ? true : false;
	}

	@Override
	public void draw(SkinObjectRenderer sprite, long time, MainState state) {

		if (state instanceof MusicResult) {
			draw(sprite, time, (MusicResult) state, getDestination(time, state));
		}

	}

	private void draw(SkinObjectRenderer sprite, long time, MusicResult state, Rectangle r) {
		if (r == null) {
			return;
		}

		// Texture生成は一度だけ
		if (tex == null) {
			TimingDistribution td = state.getTimingDistribution();
			int[] dist = td.getTimingDistribution();
			final int center = td.getArrayCenter();
			int[][] judgeArea = getJudgeArea(state.main.getPlayerResource());

			for (int d : dist) {
				if (max < d) {
					max = (d / 10) * 10 + 10;
				}
			}

			Pixmap shape = new Pixmap(dist.length, max, Pixmap.Format.RGBA8888);
			for (int i = JColor.length - 1; i >= 0; i--) {
				shape.setColor(JColor[i]);
				int x = center + Math.max(-center, Math.min(judgeArea[i][0], center));
				int width = Math.min(dist.length - x, Math.abs(judgeArea[i][0]) + Math.abs(judgeArea[i][1]) + 1);
				shape.fillRectangle(x, 0, width, max);
			}

			for (int i = 0; i < dist.length; i++) {
				shape.setColor(Color.valueOf("dddddd"));
				shape.drawLine(i, max - dist[i], i, max);
			}

			if (drawAverage && td.getAverage() != Float.MAX_VALUE) {
				int avg = Math.round(td.getAverage());
				shape.setColor(Color.RED);
				shape.drawLine(center + avg, 0, center + avg, max);
			}

			tex = new TextureRegion(new Texture(shape));
			shape.dispose();
		}

		draw(sprite, tex, r.x, r.y, r.width, r.height);

	}

	private int[][] getJudgeArea(PlayerResource resource) {
		BMSModel model = resource.getBMSModel();
		JudgeProperty rule = BMSPlayerRule.getBMSPlayerRule(model.getMode()).judge;

		final int judgerank = model.getJudgerank();
		final int judgeWindowRate = resource.getPlayerConfig().getJudgewindowrate();
		int constraint = 2;
		for (CourseData.CourseDataConstraint mode : resource.getConstraint()) {
			if (mode == CourseData.CourseDataConstraint.NO_GREAT) {
				constraint = 0;
			} else if (mode == CourseData.CourseDataConstraint.NO_GOOD) {
				constraint = 1;
			}
		}

		return rule.getNoteJudge(judgerank, judgeWindowRate, constraint,
				model.getMode() == Mode.POPN_9K && !BMSPlayerRule.isSevenToNine());
	}

	@Override
	public void dispose() {
		Optional.ofNullable(tex.getTexture()).ifPresent(Texture::dispose);
		Optional.ofNullable(shape).ifPresent(Pixmap::dispose);
	}

}
