package bms.player.beatoraja.skin;

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

	private TextureRegion backtex;
	private TextureRegion graphtex;

	private int max = 10;

	private static final Color[] JColor = new Color[] {
					Color.valueOf("000088"),
					Color.valueOf("008800"),
					Color.valueOf("666600"),
					Color.valueOf("880000"),
					Color.valueOf("000000")};

	@Override
	public void draw(SkinObjectRenderer sprite, long time, MainState state) {

		if (state instanceof MusicResult) {
			draw(sprite, time, (MusicResult)state, getDestination(time, state));
		}

	}

	private void draw(SkinObjectRenderer sprite, long time, MusicResult state, Rectangle r) {
		if (backtex != null) {
			backtex.getTexture().dispose();
		}
		if (graphtex != null) {
			graphtex.getTexture().dispose();
		}

		if (r == null) {
			return;
		}

		TimingDistribution td = state.getTimingDistribution();
		int[] dist = td.getTimingDistribution();
		final int center = td.getArrayCenter();
		int[][] judgeArea = getJudgeArea(state.main.getPlayerResource());

		for (int d : dist) {
			if (max < d) {
				max = (d / 10) * 10 + 10;
			}
		}

		Pixmap shape = new Pixmap(dist.length , 1, Pixmap.Format.RGBA8888);
		for (int i = JColor.length - 1; i >= 0; i--) {
			shape.setColor(JColor[i]);
			int x = center + Math.max(-center, Math.min(judgeArea[i][0], center));
			int width = Math.min(dist.length - x, Math.abs(judgeArea[i][0]) + Math.abs(judgeArea[i][1]));
			shape.fillRectangle(x, 0, width, 1);
		}

		backtex = new TextureRegion(new Texture(shape));
		shape.dispose();
		
		shape = new Pixmap(dist.length, max, Pixmap.Format.RGBA8888);
		for (int i = 0; i < dist.length; i++) {
			shape.setColor(Color.valueOf("dddddd"));
			shape.fillRectangle(i, max - dist[i], 1, dist[i]);
		}
		
		graphtex = new TextureRegion(new Texture(shape));
		shape.dispose();

		draw(sprite, backtex, r.x, r.y, r.width, r.height);
		draw(sprite, graphtex, r.x, r.y, r.width, r.height);
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

		return rule.getNoteJudge(judgerank, judgeWindowRate, constraint, model.getMode() == Mode.POPN_9K && !BMSPlayerRule.isSevenToNine());
	}

	@Override
	public void dispose() {
		// TODO 自動生成されたメソッド・スタブ

	}

}
